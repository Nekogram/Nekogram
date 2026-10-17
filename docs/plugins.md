# Irongram Python plugins (compatibility layer in progress)

Irongram can import a single `.py` file from Settings → Plugins. Imported files start
**disabled**. Enabling a plugin executes its Python code inside the Irongram process; install
only code and dependencies you trust. Plugins are not sandboxed.

The runtime is Python 3.11 via Chaquopy. The app minimum Android version is API 24 because
that is the minimum supported by this Python runtime.

## Metadata and example

A plugin needs literal top-level `__id__` and `__name__` values. `__version__`, `__author__`,
`__description__`, and `__requirements__` are also read:

```python
__id__ = "example_rewriter"
__name__ = "Example rewriter"
__version__ = "1.0.0"
__author__ = "Example"
__description__ = "Rewrites an outgoing message prefix."
__requirements__ = []

from base_plugin import BasePlugin, HookResult, HookStrategy


class ExampleRewriter(BasePlugin):
    def on_plugin_load(self):
        self.add_on_send_message_hook(priority=0)

    def on_plugin_unload(self):
        self.logger.info("Plugin stopped")

    def on_send_message_hook(self, account, params):
        if params.message and params.message.startswith(".hello"):
            params.message = params.message.replace(".hello", "Hello from Irongram", 1)
            return HookResult(strategy=HookStrategy.MODIFY, params=params)
        return HookResult()
```

## Implemented API surface

- Install, enable, disable, remove, and restore enabled `.py` plugins at app startup.
- `on_plugin_load` / `on_plugin_unload`, per-plugin JSON settings (`get_setting`, `set_setting`,
  `export_settings`, `import_settings`), and Logcat logger methods.
- Event hooks registered by `add_hook(name, match_substring=False, priority=0)`:
  - `pre_request_hook` and `post_request_hook` for Telegram RPC request classes.
  - `on_update_hook` for individual update classes and `on_updates_hook` for update containers.
  - `HookResult` supports default, cancel, modify, and modify-final outcomes. Objects passed by the
    app should normally be mutated in place.
- `add_on_send_message_hook(priority=0)` and `on_send_message_hook` for outgoing message params.
- `__requirements__` downloads dependencies from PyPI. The installer verifies wheel SHA-256 and
  accepts only pure-Python wheels (`none-any`). Native extensions, direct URLs, and optional
  dependency extras are not supported. Requirements share one app-wide library directory, so
  incompatible versions can prevent a plugin from enabling. Network access is required the first
  time a dependency is installed.
- Basic `ui.settings` declaration classes are included so plugins can import and define settings.
  Persistent values work, but Irongram does not currently render plugin-defined settings screens.

## Not yet compatible

This is **not full AyuGram/exteraGram compatibility yet**. In particular:

- Arbitrary Xposed-style Java method hooking (`hook_method`, `MethodHook`, `MethodReplacement`) is
  not implemented. Calling `BasePlugin.hook_method` raises `NotImplementedError`.
- Plugin menu injection, custom plugin settings screens, application lifecycle events, and the
  exteraGram convenience modules (client, Android, dialog, bulletin, reflection helpers, etc.) are
  not implemented.
- `.elyx` / `.eaf` structured plugin formats and app/SDK version constraints are not implemented.

The compatibility boundary is also shown in the Plugins screen. Do not describe this build as
fully compatible until these APIs are implemented and exercised on Android devices.
