package tw.nekomimi.nekogram.accessibility;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class AccConfig {

    private static final Object sync = new Object();

    public static int TIME_DEFAULT = 200;
    public static int DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE = TIME_DEFAULT;
    public static String DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY = "delay_between_changing_of_seekbar_value";
    public static String SHOW_NUMBERS_OF_ITEMS_KEY = "show_numbers_of_items";
    public static String SHOW_INDEX_OF_ITEM_KEY = "show_index_of_item";
    public static String SHOW_SEEKBAR_VALUE_CHANGES_KEY = "show_seekbar_value_changes";
    public static boolean SHOW_NUMBERS_OF_ITEMS = true;
    public static boolean SHOW_INDEX_OF_ITEM = true;
    public static boolean SHOW_SEEKBAR_VALUE_CHANGES = true;

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
            DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE = preferences.getInt(DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY, TIME_DEFAULT);
            SHOW_NUMBERS_OF_ITEMS = preferences.getBoolean(SHOW_NUMBERS_OF_ITEMS_KEY, true);
            SHOW_INDEX_OF_ITEM = preferences.getBoolean(SHOW_INDEX_OF_ITEM_KEY, true);
            SHOW_SEEKBAR_VALUE_CHANGES = preferences.getBoolean(SHOW_SEEKBAR_VALUE_CHANGES_KEY, true);
            configLoaded = true;
        }
    }

    public static void setDelayBetweenAnnouncingOfChangingOfSeekbarValue(int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY, value).apply();
        DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE = value;
    }

    public static void saveShowNumbersOfItems() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_NUMBERS_OF_ITEMS_KEY, !SHOW_NUMBERS_OF_ITEMS).apply();
        SHOW_NUMBERS_OF_ITEMS = !SHOW_NUMBERS_OF_ITEMS;
    }

    public static void saveShowIndexOfItem() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_INDEX_OF_ITEM_KEY, !SHOW_INDEX_OF_ITEM).apply();
        SHOW_INDEX_OF_ITEM = !SHOW_INDEX_OF_ITEM;
    }

    public static void saveShowSeekbarValueChanges() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_SEEKBAR_VALUE_CHANGES_KEY, !SHOW_SEEKBAR_VALUE_CHANGES).apply();
        SHOW_SEEKBAR_VALUE_CHANGES = !SHOW_SEEKBAR_VALUE_CHANGES;
    }
}
