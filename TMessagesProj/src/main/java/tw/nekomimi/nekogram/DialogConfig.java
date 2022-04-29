package tw.nekomimi.nekogram;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class DialogConfig {
    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekodialogconfig", Context.MODE_PRIVATE);

    public static boolean isAutoTranslateEnable(long dialog_id) {
        return preferences.getBoolean("autoTranslate_" + dialog_id, NekoConfig.autoTranslate);
    }

    public static boolean hasAutoTranslateConfig(long dialog_id) {
        return preferences.contains("autoTranslate_" + dialog_id);
    }

    public static void setAutoTranslateEnable(long dialog_id, boolean enable) {
        preferences.edit().putBoolean("autoTranslate_" + dialog_id, enable).apply();
    }

    public static void removeAutoTranslateConfig(long dialog_id) {
        preferences.edit().remove("autoTranslate_" + dialog_id).apply();
    }

}
