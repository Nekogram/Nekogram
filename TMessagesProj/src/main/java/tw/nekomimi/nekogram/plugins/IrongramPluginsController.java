package tw.nekomimi.nekogram.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class IrongramPluginsController {
    private static final String PREFS_NAME = "irongram_plugins";
    private static final String ENABLED_PREFIX = "enabled_";
    private static final String ERROR_PREFIX = "error_";
    private static final int MAX_PLUGIN_SIZE = 5 * 1024 * 1024;

    private static volatile IrongramPluginsController instance;

    private final Context context;
    private final SharedPreferences preferences;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "IrongramPlugins");
        thread.setDaemon(true);
        return thread;
    });
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Object pythonLock = new Object();

    private volatile PyObject runtime;
    private volatile int loadedPluginCount;
    private volatile boolean startupLoadScheduled;

    public interface OperationCallback {
        void onComplete(boolean success, String error);
    }

    public interface ImportCallback {
        void onComplete(PluginMetadata metadata, String error);
    }

    public interface SettingsCallback {
        void onComplete(PluginSettingsPage page, String error);
    }

    public static final class PluginSettingsPage {
        public final String pageId;
        public final List<PluginSetting> settings;

        private PluginSettingsPage(String pageId, List<PluginSetting> settings) {
            this.pageId = pageId;
            this.settings = settings;
        }
    }

    public static final class PluginSetting {
        public final int index;
        public final String type;
        public final String key;
        public final String text;
        public final String subtext;
        public final String hint;
        public final String value;
        public final boolean checked;
        public final int selectedIndex;
        public final List<String> options;
        public final boolean multiline;
        public final int maxLength;
        public final boolean red;
        public final boolean accent;
        public final boolean clickable;
        public final boolean hasSubpage;
        public final boolean supported;

        private PluginSetting(int index, String type, String key, String text, String subtext,
                              String hint, String value, boolean checked, int selectedIndex,
                              List<String> options, boolean multiline, int maxLength,
                              boolean red, boolean accent, boolean clickable,
                              boolean hasSubpage, boolean supported) {
            this.index = index;
            this.type = type;
            this.key = key;
            this.text = text;
            this.subtext = subtext;
            this.hint = hint;
            this.value = value;
            this.checked = checked;
            this.selectedIndex = selectedIndex;
            this.options = options;
            this.multiline = multiline;
            this.maxLength = maxLength;
            this.red = red;
            this.accent = accent;
            this.clickable = clickable;
            this.hasSubpage = hasSubpage;
            this.supported = supported;
        }
    }

    public static final class InstalledPlugin {
        public final PluginMetadata metadata;
        public final boolean enabled;
        public final String error;

        private InstalledPlugin(PluginMetadata metadata, boolean enabled, String error) {
            this.metadata = metadata;
            this.enabled = enabled;
            this.error = error;
        }
    }

    private IrongramPluginsController(Context context) {
        this.context = context.getApplicationContext();
        preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static IrongramPluginsController getInstance() {
        IrongramPluginsController local = instance;
        if (local == null) {
            synchronized (IrongramPluginsController.class) {
                local = instance;
                if (local == null) {
                    Context appContext = org.telegram.messenger.ApplicationLoader.applicationContext;
                    if (appContext == null) {
                        throw new IllegalStateException("Application context is not initialized");
                    }
                    local = instance = new IrongramPluginsController(appContext);
                }
            }
        }
        return local;
    }

    public List<InstalledPlugin> getInstalledPlugins() {
        File[] files = getPluginsDirectory().listFiles((directory, name) -> name.endsWith(".py"));
        ArrayList<InstalledPlugin> result = new ArrayList<>();
        if (files == null) {
            return result;
        }
        Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        for (File file : files) {
            try {
                String source = readFile(file);
                PluginMetadata metadata = PluginMetadata.fromSource(source);
                if (!file.getName().equals(metadata.id + ".py")) {
                    continue;
                }
                result.add(new InstalledPlugin(
                        metadata,
                        preferences.getBoolean(ENABLED_PREFIX + metadata.id, false),
                        preferences.getString(ERROR_PREFIX + metadata.id, null)
                ));
            } catch (Exception error) {
                FileLog.e("Ignoring invalid plugin file " + file.getName() + ": " + error.getMessage());
            }
        }
        return result;
    }

    public void importPlugin(Uri uri, ImportCallback callback) {
        executor.execute(() -> {
            PluginMetadata metadata = null;
            String errorMessage = null;
            try {
                byte[] bytes;
                try (InputStream input = context.getContentResolver().openInputStream(uri)) {
                    if (input == null) {
                        throw new IllegalArgumentException("Could not open the selected file");
                    }
                    bytes = readLimited(input, MAX_PLUGIN_SIZE);
                }
                String source = new String(bytes, StandardCharsets.UTF_8);
                metadata = PluginMetadata.fromSource(source);
                File directory = getPluginsDirectory();
                if (!directory.exists() && !directory.mkdirs()) {
                    throw new IllegalStateException("Could not create the plugins directory");
                }
                File target = new File(directory, metadata.id + ".py");
                if (target.exists()) {
                    throw new IllegalArgumentException("A plugin with this ID is already installed");
                }
                File temporary = new File(directory, metadata.id + ".py.tmp");
                try (FileOutputStream output = new FileOutputStream(temporary)) {
                    output.write(bytes);
                    output.getFD().sync();
                }
                if (!temporary.renameTo(target)) {
                    temporary.delete();
                    throw new IllegalStateException("Could not install the plugin file");
                }
            } catch (Throwable error) {
                errorMessage = error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
                FileLog.e(error);
            }
            PluginMetadata result = metadata;
            String resultError = errorMessage;
            mainHandler.post(() -> callback.onComplete(result, resultError));
        });
    }

    public void setPluginEnabled(PluginMetadata metadata, boolean enabled, OperationCallback callback) {
        executor.execute(() -> {
            boolean success = false;
            String errorMessage = null;
            if (enabled) {
                try {
                    synchronized (pythonLock) {
                        PyObject pythonRuntime = ensureRuntimeLocked();
                        boolean wasLoaded = pythonRuntime.callAttr("is_plugin_loaded", metadata.id).toBoolean();
                        if (!wasLoaded) {
                            pythonRuntime.callAttr(
                                    "install_requirements",
                                    pluginFile(metadata.id).getAbsolutePath(),
                                    getPythonLibrariesDirectory().getAbsolutePath()
                            );
                            pythonRuntime.callAttr(
                                    "load_plugin",
                                    metadata.id,
                                    pluginFile(metadata.id).getAbsolutePath(),
                                    pluginSettingsFile(metadata.id).getAbsolutePath()
                            );
                            loadedPluginCount++;
                        }
                        collectRuntimeErrorsLocked();
                    }
                    preferences.edit()
                            .putBoolean(ENABLED_PREFIX + metadata.id, true)
                            .remove(ERROR_PREFIX + metadata.id)
                            .apply();
                    success = true;
                } catch (Throwable error) {
                    preferences.edit()
                            .putBoolean(ENABLED_PREFIX + metadata.id, false)
                            .putString(ERROR_PREFIX + metadata.id, errorSummary(error))
                            .apply();
                    errorMessage = errorSummary(error);
                    FileLog.e(error);
                }
            } else {
                preferences.edit().putBoolean(ENABLED_PREFIX + metadata.id, false).apply();
                try {
                    synchronized (pythonLock) {
                        unloadPluginLocked(metadata.id);
                    }
                    success = true;
                } catch (Throwable error) {
                    errorMessage = errorSummary(error);
                    FileLog.e(error);
                }
            }
            boolean result = success;
            String resultError = errorMessage;
            mainHandler.post(() -> callback.onComplete(result, resultError));
        });
    }

    public void getPluginSettings(String pluginId, String pageId, SettingsCallback callback) {
        executor.execute(() -> {
            PluginSettingsPage page = null;
            String errorMessage = null;
            try {
                synchronized (pythonLock) {
                    ensurePluginLoadedLocked(pluginId);
                    PyObject result = ensureRuntimeLocked().callAttr(
                            "get_plugin_settings_page", pluginId, pageId == null ? "root" : pageId
                    );
                    page = readPluginSettingsPage(result);
                    collectRuntimeErrorsLocked();
                }
            } catch (Throwable error) {
                errorMessage = errorSummary(error);
                FileLog.e(error);
            }
            PluginSettingsPage resultPage = page;
            String resultError = errorMessage;
            mainHandler.post(() -> callback.onComplete(resultPage, resultError));
        });
    }

    public void openPluginSettingsSubpage(String pluginId, String pageId, int settingIndex,
                                          SettingsCallback callback) {
        executor.execute(() -> {
            PluginSettingsPage page = null;
            String errorMessage = null;
            try {
                synchronized (pythonLock) {
                    ensurePluginLoadedLocked(pluginId);
                    PyObject pythonRuntime = ensureRuntimeLocked();
                    String childPageId = pythonRuntime.callAttr(
                            "open_plugin_settings_subpage",
                            pluginId,
                            pageId == null ? "root" : pageId,
                            settingIndex
                    ).toString();
                    PyObject result = pythonRuntime.callAttr(
                            "get_plugin_settings_page", pluginId, childPageId
                    );
                    page = readPluginSettingsPage(result);
                    collectRuntimeErrorsLocked();
                }
            } catch (Throwable error) {
                errorMessage = errorSummary(error);
                FileLog.e(error);
            }
            PluginSettingsPage resultPage = page;
            String resultError = errorMessage;
            mainHandler.post(() -> callback.onComplete(resultPage, resultError));
        });
    }

    public void setPluginSetting(String pluginId, String pageId, int settingIndex, Object value,
                                 OperationCallback callback) {
        executor.execute(() -> {
            String saveError = null;
            try {
                synchronized (pythonLock) {
                    ensurePluginLoadedLocked(pluginId);
                    ensureRuntimeLocked().callAttr(
                            "set_plugin_setting",
                            pluginId,
                            pageId == null ? "root" : pageId,
                            settingIndex,
                            value,
                            false
                    );
                }
            } catch (Throwable error) {
                saveError = errorSummary(error);
                FileLog.e(error);
                synchronized (pythonLock) {
                    collectRuntimeErrorsLocked();
                }
            }
            String resultSaveError = saveError;
            if (resultSaveError != null) {
                mainHandler.post(() -> callback.onComplete(false, resultSaveError));
                return;
            }

            mainHandler.post(() -> {
                boolean success = false;
                String errorMessage = null;
                try {
                    synchronized (pythonLock) {
                        requireLoadedPluginLocked(pluginId).callAttr(
                                "dispatch_plugin_setting_change",
                                pluginId,
                                pageId == null ? "root" : pageId,
                                settingIndex,
                                value
                        );
                        collectRuntimeErrorsLocked();
                    }
                    success = true;
                } catch (Throwable error) {
                    errorMessage = errorSummary(error);
                    FileLog.e(error);
                    synchronized (pythonLock) {
                        collectRuntimeErrorsLocked();
                    }
                }
                callback.onComplete(success, errorMessage);
            });
        });
    }

    public void clickPluginSetting(String pluginId, String pageId, int settingIndex, Object view,
                                   OperationCallback callback) {
        mainHandler.post(() -> {
            boolean success = false;
            String errorMessage = null;
            try {
                synchronized (pythonLock) {
                    requireLoadedPluginLocked(pluginId).callAttr(
                            "click_plugin_setting",
                            pluginId,
                            pageId == null ? "root" : pageId,
                            settingIndex,
                            view
                    );
                    collectRuntimeErrorsLocked();
                }
                success = true;
            } catch (Throwable error) {
                errorMessage = errorSummary(error);
                FileLog.e(error);
                synchronized (pythonLock) {
                    collectRuntimeErrorsLocked();
                }
            }
            callback.onComplete(success, errorMessage);
        });
    }

    public void removePlugin(PluginMetadata metadata, OperationCallback callback) {
        executor.execute(() -> {
            boolean success = false;
            String errorMessage = null;
            try {
                preferences.edit().putBoolean(ENABLED_PREFIX + metadata.id, false).apply();
                synchronized (pythonLock) {
                    unloadPluginLocked(metadata.id);
                }
                File plugin = pluginFile(metadata.id);
                if (plugin.exists() && !plugin.delete()) {
                    throw new IllegalStateException("Could not remove the plugin file");
                }
                deleteRecursively(new File(getDataDirectory(), metadata.id));
                preferences.edit()
                        .remove(ENABLED_PREFIX + metadata.id)
                        .remove(ERROR_PREFIX + metadata.id)
                        .apply();
                success = true;
            } catch (Throwable error) {
                errorMessage = errorSummary(error);
                FileLog.e(error);
            }
            boolean result = success;
            String resultError = errorMessage;
            mainHandler.post(() -> callback.onComplete(result, resultError));
        });
    }

    public void loadEnabledPlugins() {
        if (startupLoadScheduled) {
            return;
        }
        startupLoadScheduled = true;
        executor.execute(() -> {
            for (InstalledPlugin entry : getInstalledPlugins()) {
                if (!entry.enabled) {
                    continue;
                }
                try {
                    synchronized (pythonLock) {
                        PyObject pythonRuntime = ensureRuntimeLocked();
                        boolean wasLoaded = pythonRuntime.callAttr("is_plugin_loaded", entry.metadata.id).toBoolean();
                        if (!wasLoaded) {
                            pythonRuntime.callAttr(
                                    "install_requirements",
                                    pluginFile(entry.metadata.id).getAbsolutePath(),
                                    getPythonLibrariesDirectory().getAbsolutePath()
                            );
                            pythonRuntime.callAttr(
                                    "load_plugin",
                                    entry.metadata.id,
                                    pluginFile(entry.metadata.id).getAbsolutePath(),
                                    pluginSettingsFile(entry.metadata.id).getAbsolutePath()
                            );
                            loadedPluginCount++;
                        }
                        collectRuntimeErrorsLocked();
                    }
                    preferences.edit().remove(ERROR_PREFIX + entry.metadata.id).apply();
                } catch (Throwable error) {
                    preferences.edit()
                            .putBoolean(ENABLED_PREFIX + entry.metadata.id, false)
                            .putString(ERROR_PREFIX + entry.metadata.id, errorSummary(error))
                            .apply();
                    FileLog.e(error);
                }
            }
        });
    }

    public boolean onSendMessage(int account, SendMessagesHelper.SendMessageParams params) {
        return callBooleanHook("dispatch_send_message", account, params);
    }

    public boolean onPreRequest(int account, TLObject request) {
        return callBooleanHook("dispatch_pre_request", account, request);
    }

    public void onPostRequest(int account, TLObject request, TLObject response, TLRPC.TL_error error) {
        callVoidHook("dispatch_post_request", account, request, response, error);
    }

    public boolean onUpdate(int account, TLRPC.Update update) {
        return callBooleanHook("dispatch_update", account, update);
    }

    public boolean onUpdates(int account, TLRPC.Updates updates) {
        return callBooleanHook("dispatch_updates", account, updates);
    }

    private boolean callBooleanHook(String method, Object... args) {
        if (loadedPluginCount == 0 || runtime == null) {
            return true;
        }
        synchronized (pythonLock) {
            try {
                boolean result = ensureRuntimeLocked().callAttr(method, args).toBoolean();
                collectRuntimeErrorsLocked();
                return result;
            } catch (Throwable error) {
                FileLog.e(error);
                return true;
            }
        }
    }

    private void callVoidHook(String method, Object... args) {
        if (loadedPluginCount == 0 || runtime == null) {
            return;
        }
        synchronized (pythonLock) {
            try {
                ensureRuntimeLocked().callAttr(method, args);
                collectRuntimeErrorsLocked();
            } catch (Throwable error) {
                FileLog.e(error);
            }
        }
    }

    private PyObject ensureRuntimeLocked() {
        if (runtime == null) {
            if (!Python.isStarted()) {
                Python.start(new AndroidPlatform(context));
            }
            runtime = Python.getInstance().getModule("plugin_runtime");
        }
        return runtime;
    }

    private void ensurePluginLoadedLocked(String pluginId) {
        if (!preferences.getBoolean(ENABLED_PREFIX + pluginId, false)) {
            throw new IllegalStateException("Enable this plugin before opening its settings");
        }
        PyObject pythonRuntime = ensureRuntimeLocked();
        if (pythonRuntime.callAttr("is_plugin_loaded", pluginId).toBoolean()) {
            return;
        }
        try {
            pythonRuntime.callAttr(
                    "install_requirements",
                    pluginFile(pluginId).getAbsolutePath(),
                    getPythonLibrariesDirectory().getAbsolutePath()
            );
            pythonRuntime.callAttr(
                    "load_plugin",
                    pluginId,
                    pluginFile(pluginId).getAbsolutePath(),
                    pluginSettingsFile(pluginId).getAbsolutePath()
            );
            loadedPluginCount++;
            preferences.edit().remove(ERROR_PREFIX + pluginId).apply();
        } catch (Throwable error) {
            String message = errorSummary(error);
            preferences.edit()
                    .putBoolean(ENABLED_PREFIX + pluginId, false)
                    .putString(ERROR_PREFIX + pluginId, message)
                    .apply();
            throw new IllegalStateException(message, error);
        }
    }

    private PyObject requireLoadedPluginLocked(String pluginId) {
        if (!preferences.getBoolean(ENABLED_PREFIX + pluginId, false)) {
            throw new IllegalStateException("This plugin is disabled");
        }
        PyObject pythonRuntime = ensureRuntimeLocked();
        if (!pythonRuntime.callAttr("is_plugin_loaded", pluginId).toBoolean()) {
            throw new IllegalStateException("This plugin is not currently loaded");
        }
        return pythonRuntime;
    }

    private static PluginSettingsPage readPluginSettingsPage(PyObject pageObject) {
        String pageId = readStringAttribute(pageObject, "page_id", "root");
        ArrayList<PluginSetting> settings = new ArrayList<>();
        try {
            for (PyObject setting : pageObject.get("settings").asList()) {
                ArrayList<String> options = new ArrayList<>();
                try {
                    for (PyObject option : setting.get("options").asList()) {
                        options.add(option.toString());
                    }
                } catch (Throwable ignored) {
                    // Most setting rows do not have a choice list.
                }
                settings.add(new PluginSetting(
                        readIntAttribute(setting, "index", settings.size()),
                        readStringAttribute(setting, "kind", ""),
                        readStringAttribute(setting, "key", ""),
                        readStringAttribute(setting, "text", ""),
                        readStringAttribute(setting, "subtext", ""),
                        readStringAttribute(setting, "hint", ""),
                        readStringAttribute(setting, "value", ""),
                        readBooleanAttribute(setting, "checked", false),
                        readIntAttribute(setting, "selected_index", 0),
                        options,
                        readBooleanAttribute(setting, "multiline", false),
                        readIntAttribute(setting, "max_length", 0),
                        readBooleanAttribute(setting, "red", false),
                        readBooleanAttribute(setting, "accent", false),
                        readBooleanAttribute(setting, "clickable", false),
                        readBooleanAttribute(setting, "has_subpage", false),
                        readBooleanAttribute(setting, "supported", false)
                ));
            }
        } catch (Throwable error) {
            throw new IllegalStateException("Could not read plugin settings", error);
        }
        return new PluginSettingsPage(pageId, settings);
    }

    private static String readStringAttribute(PyObject object, String name, String fallback) {
        try {
            PyObject value = object.get(name);
            return value == null ? fallback : value.toString();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static boolean readBooleanAttribute(PyObject object, String name, boolean fallback) {
        try {
            PyObject value = object.get(name);
            return value == null ? fallback : value.toBoolean();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private static int readIntAttribute(PyObject object, String name, int fallback) {
        try {
            PyObject value = object.get(name);
            return value == null ? fallback : value.toInt();
        } catch (Throwable ignored) {
            return fallback;
        }
    }

    private void unloadPluginLocked(String pluginId) {
        if (runtime == null) {
            return;
        }
        PyObject unloaded = runtime.callAttr("unload_plugin", pluginId);
        if (unloaded.toBoolean()) {
            loadedPluginCount = Math.max(0, loadedPluginCount - 1);
        }
        collectRuntimeErrorsLocked();
    }

    private void collectRuntimeErrorsLocked() {
        if (runtime == null) {
            return;
        }
        try {
            List<PyObject> errors = runtime.callAttr("drain_errors").asList();
            for (PyObject error : errors) {
                List<PyObject> pair = error.asList();
                if (pair.size() < 2) {
                    continue;
                }
                String pluginId = pair.get(0).toString();
                String message = pair.get(1).toString();
                preferences.edit()
                        .putBoolean(ENABLED_PREFIX + pluginId, false)
                        .putString(ERROR_PREFIX + pluginId, message)
                        .apply();
                loadedPluginCount = Math.max(0, loadedPluginCount - 1);
                FileLog.e("Python plugin " + pluginId + " disabled: " + message);
            }
        } catch (Throwable error) {
            FileLog.e(error);
        }
    }

    private File getPluginsDirectory() {
        File directory = new File(context.getFilesDir(), "irongram/plugins");
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return directory;
    }

    private File getDataDirectory() {
        return new File(context.getFilesDir(), "irongram/plugin_data");
    }

    private File getPythonLibrariesDirectory() {
        return new File(context.getFilesDir(), "irongram/python-libs");
    }

    private File pluginFile(String pluginId) {
        return new File(getPluginsDirectory(), pluginId + ".py");
    }

    private File pluginSettingsFile(String pluginId) {
        return new File(new File(getDataDirectory(), pluginId), "settings.json");
    }

    private static byte[] readLimited(InputStream input, int maxSize) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > maxSize) {
                throw new IllegalArgumentException("Plugin file is larger than 5 MB");
            }
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static String readFile(File file) throws Exception {
        try (InputStream input = new FileInputStream(file)) {
            return new String(readLimited(input, MAX_PLUGIN_SIZE), StandardCharsets.UTF_8);
        }
    }

    private static String errorSummary(Throwable error) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? error.getClass().getSimpleName()
                : error.getClass().getSimpleName() + ": " + message;
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        if (file.exists()) {
            file.delete();
        }
    }
}
