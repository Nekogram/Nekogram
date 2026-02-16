package tw.nekomimi.nekogram.accessibility;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class AccConfig {

    private static final Object sync = new Object();

    public static int TIME_DEFAULT = 200;
    public static String DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY = "delay_between_changing_of_seekbar_value";
    public static String SHOW_NUMBERS_OF_ITEMS_KEY = "show_numbers_of_items";
    public static String SHOW_INDEX_OF_ITEM_KEY = "show_index_of_item";
    public static String SHOW_SEEKBAR_VALUE_CHANGES_KEY = "show_seekbar_value_changes";

    public static int delayBetweenAnnouncingOfChangingOfSeekbarValue = TIME_DEFAULT;
    public static boolean showNumbersOfItems = true;
    public static boolean showIndexOfItem = true;
    public static boolean showSeekbarValueChanges = true;

    public static boolean announceFileProgress = true;
    public static boolean showTranslatedLanguage = true;

    private static boolean configLoaded;

    static {
        loadConfig();
    }

    public static void loadConfig() {
        synchronized (sync) {
            if (configLoaded) {
                return;
            }

            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
            delayBetweenAnnouncingOfChangingOfSeekbarValue = preferences.getInt(DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY, TIME_DEFAULT);
            showNumbersOfItems = preferences.getBoolean(SHOW_NUMBERS_OF_ITEMS_KEY, true);
            showIndexOfItem = preferences.getBoolean(SHOW_INDEX_OF_ITEM_KEY, true);
            showSeekbarValueChanges = preferences.getBoolean("show_seekbar_value_changes", true);

            announceFileProgress = preferences.getBoolean("announceFileProgress", true);
            showTranslatedLanguage = preferences.getBoolean("showTranslatedLanguage", true);

            configLoaded = true;
        }
    }

    public static void setDelayBetweenAnnouncingOfChangingOfSeekbarValue(int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY, value).apply();
        delayBetweenAnnouncingOfChangingOfSeekbarValue = value;
    }

    public static void saveShowNumbersOfItems() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_NUMBERS_OF_ITEMS_KEY, !showIndexOfItem).apply();
        showNumbersOfItems = !showNumbersOfItems;
    }

    public static void saveShowIndexOfItem() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_INDEX_OF_ITEM_KEY, !showIndexOfItem).apply();
        showIndexOfItem = !showIndexOfItem;
    }

    public static void saveShowSeekbarValueChanges() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_SEEKBAR_VALUE_CHANGES_KEY, !showSeekbarValueChanges).apply();
        showSeekbarValueChanges = !showSeekbarValueChanges;
    }

    public static void toggleAnnounceFileProgress() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("announceFileProgress", !announceFileProgress).apply();
        announceFileProgress = !announceFileProgress;
    }

    public static void toggleShowTranslatedLanguage() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("showTranslatedLanguage", !showTranslatedLanguage).apply();
        showTranslatedLanguage = !showTranslatedLanguage;
    }
}
