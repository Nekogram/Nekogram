"""Runtime loader and event dispatcher for Irongram Python plugins."""

import importlib.util
import os
import sys
import traceback

from base_plugin import BasePlugin, HookStrategy, _PluginLogger
from dependency_manager import install_plugin_requirements

_PLUGINS = {}
_REQUEST_HOOKS = []
_SEND_MESSAGE_HOOKS = []
_ERRORS = []
_NEXT_TOKEN = 1

try:
    from java.lang.reflect import Modifier as _Modifier
except Exception:
    _Modifier = None


def _new_token():
    global _NEXT_TOKEN
    token = _NEXT_TOKEN
    _NEXT_TOKEN += 1
    return token


def _plugin_id(plugin):
    return getattr(plugin, "_plugin_id", None)


def register_request_hook(plugin, name, match_substring=False, priority=0):
    token = _new_token()
    _REQUEST_HOOKS.append({
        "token": token,
        "plugin": plugin,
        "name": name,
        "substring": bool(match_substring),
        "priority": int(priority),
    })
    _REQUEST_HOOKS.sort(key=lambda hook: hook["priority"], reverse=True)
    return token


def register_send_message_hook(plugin, priority=0):
    token = _new_token()
    _SEND_MESSAGE_HOOKS.append({
        "token": token,
        "plugin": plugin,
        "priority": int(priority),
    })
    _SEND_MESSAGE_HOOKS.sort(key=lambda hook: hook["priority"], reverse=True)
    return token


def unregister_plugin_hooks(plugin):
    _REQUEST_HOOKS[:] = [hook for hook in _REQUEST_HOOKS if hook["plugin"] is not plugin]
    _SEND_MESSAGE_HOOKS[:] = [hook for hook in _SEND_MESSAGE_HOOKS if hook["plugin"] is not plugin]


def _copy_java_object(target, source):
    if target is None or source is None or target is source:
        return
    try:
        target_class = target.getClass()
        source_class = source.getClass()
        if target_class != source_class:
            raise TypeError("Hook replacement must have the same Java class")
        current = source_class
        while current is not None:
            for field in current.getDeclaredFields():
                if _Modifier is not None and _Modifier.isStatic(field.getModifiers()):
                    continue
                field.setAccessible(True)
                field.set(target, field.get(source))
            current = current.getSuperclass()
    except Exception:
        # Java objects passed to plugins are normally mutated in place. Field copying is only
        # needed when a plugin returns a replacement object and is best-effort for vendor APIs.
        traceback.print_exc()


def _hook_outcome(plugin, result, replacement_attribute):
    if result is None:
        return HookStrategy.DEFAULT
    strategy = getattr(result, "strategy", HookStrategy.DEFAULT)
    if strategy == HookStrategy.CANCEL:
        return HookStrategy.CANCEL
    if strategy not in (HookStrategy.MODIFY, HookStrategy.MODIFY_FINAL):
        return HookStrategy.DEFAULT
    replacement = getattr(result, replacement_attribute, None)
    if replacement is not None:
        # The request/update/parameter objects are owned by Telegram's Java pipeline. Mutating
        # them in place preserves references held by existing callbacks and queues.
        target = getattr(result, "_irongram_target", None)
        if target is not None and replacement is not target:
            _copy_java_object(target, replacement)
    return strategy


def _copy_result_to_target(result, attribute, target):
    if result is None:
        return HookStrategy.DEFAULT
    strategy = getattr(result, "strategy", HookStrategy.DEFAULT)
    if strategy not in (HookStrategy.MODIFY, HookStrategy.MODIFY_FINAL):
        return HookStrategy.CANCEL if strategy == HookStrategy.CANCEL else HookStrategy.DEFAULT
    replacement = getattr(result, attribute, None)
    if replacement is not None and target is not None and replacement is not target:
        _copy_java_object(target, replacement)
    return strategy


def _fail_plugin(plugin, error):
    plugin_id = _plugin_id(plugin) or "unknown"
    message = "{}: {}".format(type(error).__name__, error)
    unregister_plugin_hooks(plugin)
    _PLUGINS.pop(plugin_id, None)
    try:
        plugin.on_plugin_unload()
    except Exception:
        traceback.print_exc()
    _ERRORS.append((plugin_id, message))
    try:
        plugin.logger.error("Plugin disabled after an exception: %s", message)
    except Exception:
        traceback.print_exc()


def install_requirements(plugin_path, library_directory):
    return install_plugin_requirements(str(plugin_path), str(library_directory))


def load_plugin(plugin_id, plugin_path, settings_path):
    plugin_id = str(plugin_id)
    if plugin_id in _PLUGINS:
        unload_plugin(plugin_id)

    plugin_path = os.path.abspath(str(plugin_path))
    module_name = "irongram_plugin_" + plugin_id.replace("-", "_")
    plugin_dir = os.path.dirname(plugin_path)
    if plugin_dir not in sys.path:
        sys.path.insert(0, plugin_dir)

    spec = importlib.util.spec_from_file_location(module_name, plugin_path)
    if spec is None or spec.loader is None:
        raise ImportError("Could not create a Python module for this plugin")
    module = importlib.util.module_from_spec(spec)
    sys.modules[module_name] = module
    try:
        spec.loader.exec_module(module)
        module_names = {module_name, getattr(module, "__name__", module_name)}
        classes = [
            candidate for candidate in vars(module).values()
            if isinstance(candidate, type)
            and candidate is not BasePlugin
            and issubclass(candidate, BasePlugin)
            and candidate.__module__ in module_names
        ]
        if not classes:
            raise TypeError("Plugin file must define a class inheriting from BasePlugin")
        if len(classes) != 1:
            raise TypeError("Plugin file must define exactly one BasePlugin subclass")

        plugin = classes[0]()
        plugin._plugin_id = plugin_id
        plugin._plugin_path = plugin_path
        plugin._settings_path = os.path.abspath(str(settings_path))
        plugin._runtime = sys.modules[__name__]
        plugin.logger = _PluginLogger(plugin)
        plugin._registered_hooks = []
        plugin._registered_send_hook = False
        plugin.on_plugin_load()
        _PLUGINS[plugin_id] = {
            "plugin": plugin,
            "module": module,
            "module_name": module_name,
            "plugin_dir": plugin_dir,
        }
        return True
    except BaseException:
        unregister_plugin_hooks(locals().get("plugin")) if "plugin" in locals() else None
        sys.modules.pop(module_name, None)
        raise


def unload_plugin(plugin_id):
    entry = _PLUGINS.pop(str(plugin_id), None)
    if entry is None:
        return False
    plugin = entry["plugin"]
    unregister_plugin_hooks(plugin)
    try:
        plugin.on_plugin_unload()
    except Exception:
        traceback.print_exc()
    sys.modules.pop(entry["module_name"], None)
    return True


def _matching_request_hooks(request_name):
    for hook in tuple(_REQUEST_HOOKS):
        name = hook["name"]
        if request_name == name or (hook["substring"] and name in request_name):
            yield hook


def dispatch_pre_request(account, request):
    request_name = request.getClass().getSimpleName()
    for hook in _matching_request_hooks(request_name):
        plugin = hook["plugin"]
        if _plugin_id(plugin) not in _PLUGINS:
            continue
        try:
            result = plugin.pre_request_hook(request_name, int(account), request)
            outcome = _copy_result_to_target(result, "request", request)
            if outcome == HookStrategy.CANCEL:
                return False
            if outcome == HookStrategy.MODIFY_FINAL:
                break
        except BaseException as error:
            _fail_plugin(plugin, error)
    return True


def dispatch_post_request(account, request, response, error):
    request_name = request.getClass().getSimpleName()
    for hook in _matching_request_hooks(request_name):
        plugin = hook["plugin"]
        if _plugin_id(plugin) not in _PLUGINS:
            continue
        try:
            result = plugin.post_request_hook(request_name, int(account), response, error)
            _copy_result_to_target(result, "response", response)
        except BaseException as failure:
            _fail_plugin(plugin, failure)


def dispatch_update(account, update):
    update_name = update.getClass().getSimpleName()
    for hook in _matching_request_hooks(update_name):
        plugin = hook["plugin"]
        if _plugin_id(plugin) not in _PLUGINS:
            continue
        try:
            result = plugin.on_update_hook(update_name, int(account), update)
            outcome = _copy_result_to_target(result, "update", update)
            if outcome == HookStrategy.CANCEL:
                return False
            if outcome == HookStrategy.MODIFY_FINAL:
                break
        except BaseException as error:
            _fail_plugin(plugin, error)
    return True


def dispatch_updates(account, updates):
    container_name = updates.getClass().getSimpleName()
    for hook in _matching_request_hooks(container_name):
        plugin = hook["plugin"]
        if _plugin_id(plugin) not in _PLUGINS:
            continue
        try:
            result = plugin.on_updates_hook(container_name, int(account), updates)
            outcome = _copy_result_to_target(result, "updates", updates)
            if outcome == HookStrategy.CANCEL:
                return False
            if outcome == HookStrategy.MODIFY_FINAL:
                break
        except BaseException as error:
            _fail_plugin(plugin, error)
    return True


def dispatch_send_message(account, params):
    for hook in tuple(_SEND_MESSAGE_HOOKS):
        plugin = hook["plugin"]
        if _plugin_id(plugin) not in _PLUGINS:
            continue
        try:
            result = plugin.on_send_message_hook(int(account), params)
            outcome = _copy_result_to_target(result, "params", params)
            if outcome == HookStrategy.CANCEL:
                return False
            if outcome == HookStrategy.MODIFY_FINAL:
                break
        except BaseException as error:
            _fail_plugin(plugin, error)
    return True


def drain_errors():
    errors = list(_ERRORS)
    _ERRORS.clear()
    return errors


def loaded_plugin_ids():
    return list(_PLUGINS.keys())


def is_plugin_loaded(plugin_id):
    return str(plugin_id) in _PLUGINS
