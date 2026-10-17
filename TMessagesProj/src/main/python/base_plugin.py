"""A small compatibility layer for exteraGram-style Python plugins.

The host deliberately runs plugin code inside Irongram's process. Plugins therefore have
full process privileges and should only be installed from a source the user trusts.
"""

import json
import os
import tempfile
import traceback
from enum import Enum

try:
    from java import jclass

    _Log = jclass("android.util.Log")
except Exception:
    _Log = None


class HookStrategy:
    DEFAULT = "default"
    CANCEL = "cancel"
    MODIFY = "modify"
    MODIFY_FINAL = "modify_final"


class HookResult:
    def __init__(self, strategy=HookStrategy.DEFAULT, request=None, response=None,
                 update=None, updates=None, params=None):
        self.strategy = strategy
        self.request = request
        self.response = response
        self.update = update
        self.updates = updates
        self.params = params


class AppEvent(Enum):
    START = "start"
    STOP = "stop"
    PAUSE = "pause"
    RESUME = "resume"


class MenuItemType(Enum):
    MESSAGE_CONTEXT_MENU = "message_context_menu"
    DRAWER_MENU = "drawer_menu"
    MAIN_MENU = "main_menu"
    CHAT_ACTION_MENU = "chat_action_menu"
    PROFILE_ACTION_MENU = "profile_action_menu"


class MenuItemData:
    def __init__(self, menu_type, text, on_click, item_id=None, icon=None,
                 subtext=None, condition=None, priority=0):
        self.menu_type = menu_type
        self.text = text
        self.on_click = on_click
        self.item_id = item_id
        self.icon = icon
        self.subtext = subtext
        self.condition = condition
        self.priority = priority


class _PluginLogger:
    def __init__(self, plugin):
        self._plugin = plugin

    @property
    def _tag(self):
        return "IrongramPlugin/" + (getattr(self._plugin, "_plugin_id", None) or "unknown")

    @staticmethod
    def _format(message, args):
        if not args:
            return str(message)
        try:
            return str(message) % args
        except Exception:
            return "{} {}".format(message, " ".join(map(str, args)))

    def _write(self, level, message, *args):
        text = self._format(message, args)
        if _Log is None:
            print("[{}] {}".format(self._tag, text))
            return
        getattr(_Log, level)(self._tag, text)

    def debug(self, message, *args):
        self._write("d", message, *args)

    def info(self, message, *args):
        self._write("i", message, *args)

    def warning(self, message, *args):
        self._write("w", message, *args)

    warn = warning

    def error(self, message, *args):
        self._write("e", message, *args)

    def exception(self, message, *args):
        self._write("e", "{}\n{}".format(self._format(message, args), traceback.format_exc()))

    def dump(self, value):
        self.info("%r", value)


class BasePlugin:
    """Base class matching the lifecycle and event-hook surface used by exteraGram."""

    def __init__(self):
        self._plugin_id = None
        self._plugin_path = None
        self._settings_path = None
        self._runtime = None
        self.logger = _PluginLogger(self)
        self._registered_hooks = []
        self._registered_send_hook = False

    def on_plugin_load(self):
        pass

    def on_plugin_unload(self):
        pass

    def on_app_event(self, event_type):
        pass

    def pre_request_hook(self, request_name, account, request):
        return HookResult()

    def post_request_hook(self, request_name, account, response, error):
        return HookResult()

    def on_update_hook(self, update_name, account, update):
        return HookResult()

    def on_updates_hook(self, container_name, account, updates):
        return HookResult()

    def on_send_message_hook(self, account, params):
        return HookResult()

    def create_settings(self):
        return []

    def add_hook(self, name, match_substring=False, priority=0):
        if self._runtime is None:
            raise RuntimeError("Plugin runtime is not initialized")
        token = self._runtime.register_request_hook(
            self, str(name), bool(match_substring), int(priority)
        )
        self._registered_hooks.append(token)
        return token

    def add_on_send_message_hook(self, priority=0):
        if self._runtime is None:
            raise RuntimeError("Plugin runtime is not initialized")
        token = self._runtime.register_send_message_hook(self, int(priority))
        self._registered_send_hook = True
        return token

    def hook_method(self, *args, **kwargs):
        raise NotImplementedError(
            "Low-level Xposed method hooks are not available in this Irongram build."
        )

    def unhook_all(self):
        if self._runtime is not None:
            self._runtime.unregister_plugin_hooks(self)
        self._registered_hooks.clear()
        self._registered_send_hook = False

    def add_menu_item(self, item):
        raise NotImplementedError(
            "Plugin menu injection is not available in this Irongram build."
        )

    def remove_menu_item(self, item_id):
        return False

    def get_setting(self, key, default=None):
        values = self._read_settings()
        return values.get(str(key), default)

    def set_setting(self, key, value, reload_settings=False):
        values = self._read_settings()
        values[str(key)] = value
        self._write_settings(values)

    def export_settings(self):
        return self._read_settings()

    def import_settings(self, settings, reload_settings=True):
        if not isinstance(settings, dict):
            raise TypeError("settings must be a dictionary")
        self._write_settings(settings)

    def get_plugin_dir(self):
        return os.path.dirname(self._plugin_path) if self._plugin_path else ""

    def get_data_dir(self):
        path = self._settings_path
        if not path:
            return ""
        directory = os.path.dirname(path)
        os.makedirs(directory, exist_ok=True)
        return directory

    def log(self, message, *args):
        self.logger.info(message, *args)

    def _read_settings(self):
        path = self._settings_path
        if not path or not os.path.isfile(path):
            return {}
        try:
            with open(path, "r", encoding="utf-8") as settings_file:
                value = json.load(settings_file)
            return value if isinstance(value, dict) else {}
        except Exception:
            self.logger.exception("Could not read plugin settings")
            return {}

    def _write_settings(self, values):
        path = self._settings_path
        if not path:
            raise RuntimeError("Plugin settings are unavailable before initialization")
        directory = os.path.dirname(path)
        os.makedirs(directory, exist_ok=True)
        fd, temp_path = tempfile.mkstemp(prefix="settings-", suffix=".tmp", dir=directory)
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as settings_file:
                json.dump(values, settings_file, ensure_ascii=False, sort_keys=True)
                settings_file.flush()
                os.fsync(settings_file.fileno())
            os.replace(temp_path, path)
        finally:
            if os.path.exists(temp_path):
                os.unlink(temp_path)


class MethodHook:
    def before_hooked_method(self, param):
        pass

    def after_hooked_method(self, param):
        pass


class MethodReplacement:
    def replace_hooked_method(self, param):
        raise NotImplementedError


class BaseHook(MethodHook):
    def __init__(self, before=None, after=None):
        self.before = before
        self.after = after

    def before_hooked_method(self, param):
        if self.before is not None:
            return self.before(param)

    def after_hooked_method(self, param):
        if self.after is not None:
            return self.after(param)


class HookFilter:
    RESULT_IS_NULL = object()
    RESULT_NOT_NULL = object()
    RESULT_IS_TRUE = object()
    RESULT_IS_FALSE = object()

    @staticmethod
    def ResultIsInstanceOf(clazz):
        return ("result_instance", clazz)

    @staticmethod
    def ResultEqual(value):
        return ("result_equal", value)

    @staticmethod
    def ResultNotEqual(value):
        return ("result_not_equal", value)

    @staticmethod
    def ArgumentIsNull(index):
        return ("argument_null", index)

    @staticmethod
    def ArgumentNotNull(index):
        return ("argument_not_null", index)

    @staticmethod
    def ArgumentIsFalse(index):
        return ("argument_false", index)

    @staticmethod
    def ArgumentIsTrue(index):
        return ("argument_true", index)

    @staticmethod
    def ArgumentIsInstanceOf(index, clazz):
        return ("argument_instance", index, clazz)

    @staticmethod
    def ArgumentEqual(index, value):
        return ("argument_equal", index, value)

    @staticmethod
    def ArgumentNotEqual(index, value):
        return ("argument_not_equal", index, value)

    @staticmethod
    def Condition(condition, object=None):
        return ("condition", condition, object)

    @staticmethod
    def Or(*filters):
        return ("or", filters)


def hook_filters(*filters):
    def decorate(function):
        function.__irongram_hook_filters__ = filters
        return function
    return decorate
