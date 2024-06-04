package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.View;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;

@SuppressLint("InlinedApi")
public class TranslatorApps {

    private static final String XIAOAI_TRANSLATOR = "com.xiaomi.aiasst.vision";
    private static final String XIAOAI_TRANSLATOR_SERVICE = "com.xiaomi.aiasst.vision.control.translation.AiTranslateService";

    private static final ArrayList<TranslatorApp> translatorApps = new ArrayList<>();
    private static TranslatorApp translatorApp;
    private static boolean translatorsLoaded = false;

    public static class TranslatorApp {
        public String packageName;
        public String className;
        public CharSequence title;

        public boolean isXiaoAi() {
            return XIAOAI_TRANSLATOR_SERVICE.equals(className);
        }
    }

    public static void loadTranslatorAppsAsync() {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                loadTranslatorApps();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private static void loadTranslatorApps() {
        translatorApps.clear();
        var provider = NekoConfig.externalTranslationProvider;
        var pm = ApplicationLoader.applicationContext.getPackageManager();
        try {
            var info = pm.getPackageInfo(XIAOAI_TRANSLATOR, 0);
            if (info.versionCode >= 302) {
                var app = new TranslatorApp();
                app.packageName = XIAOAI_TRANSLATOR;
                app.className = XIAOAI_TRANSLATOR_SERVICE;
                app.title = info.applicationInfo.loadLabel(pm);
                translatorApps.add(app);
                if (provider.equals(app.packageName)) {
                    translatorApp = app;
                }
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        var activities = pm.queryIntentActivities(new Intent(Intent.ACTION_TRANSLATE), 0);
        for (var info : activities) {
            var app = new TranslatorApp();
            app.packageName = info.activityInfo.packageName;
            app.className = info.activityInfo.name;
            app.title = info.loadLabel(pm);
            translatorApps.add(app);
            if (provider.equals(app.packageName)) {
                translatorApp = app;
            }
        }
        if (translatorApp == null && !translatorApps.isEmpty()) {
            translatorApp = translatorApps.get(0);
        }
        translatorsLoaded = true;
    }

    public static ArrayList<TranslatorApp> getTranslatorApps() {
        return translatorApps;
    }

    public static TranslatorApp getTranslatorApp() {
        return translatorApp;
    }

    public static void setTranslatorApp(TranslatorApp app) {
        translatorApp = app;
        NekoConfig.setExternalTranslationProvider(app.packageName);
    }

    public static void showExternalTranslateDialog(Context context, String query, String from, View anchorView, Theme.ResourcesProvider resourcesProvider) {
        if (!translatorsLoaded || translatorApp == null) {
            return;
        }
        var app = translatorApp;
        var intent = new Intent();
        intent.setPackage(app.packageName);
        intent.setComponent(new ComponentName(app.packageName, app.className));
        if (app.isXiaoAi()) {
            intent.putExtra("text", query);
            if (!TextUtils.isEmpty(from)) {
                intent.putExtra("sourceLang", from);
            }
            intent.putExtra("from", "system_edit_box");
            intent.putExtra("floatingWindowType", "startDictionaryTranslationWindow");
            if (anchorView != null) {
                var temp = new int[2];
                var focusRect = new Rect();
                anchorView.getLocationOnScreen(temp);
                anchorView.getFocusedRect(focusRect);
                focusRect.offset(temp[0], temp[1]);
                intent.putExtra("rawX", focusRect.left);
                intent.putExtra("rawY", focusRect.top);
            }
            try {
                context.startService(intent);
            } catch (Exception e) {
                new AlertDialog.Builder(context, resourcesProvider)
                        .setTitle(LocaleController.getString(R.string.AppName))
                        .setMessage(LocaleController.getString(R.string.NoTranslatorAppInstalled))
                        .show();
            }
        } else {
            intent.setAction(Intent.ACTION_TRANSLATE);
            intent.putExtra(Intent.EXTRA_TEXT, query);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException e) {
                new AlertDialog.Builder(context, resourcesProvider)
                        .setTitle(LocaleController.getString(R.string.AppName))
                        .setMessage(LocaleController.getString(R.string.NoTranslatorAppInstalled))
                        .show();
            }
        }
    }

}
