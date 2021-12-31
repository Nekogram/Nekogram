package tw.nekomimi.nekogram.accessibility;

import android.app.Activity;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class AccConfig {

    private static final Object sync = new Object();

    public static int TYPE_NO_REWIND = 0;
    public static int TYPE_PERCENT_REWIND = 1;
    public static int TYPE_SECOND_REWIND = 2;
    public static int TYPE_AUTO_REWIND = 3;
    public static int TYPE_OF_REWIND = TYPE_AUTO_REWIND;
    public static int TYPE_OF_REWIND_VIDEO = TYPE_AUTO_REWIND;
    public static int TYPE_AT_FIRST = 0;
    public static int TYPE_AT_MIDDLE = 1;
    public static int TYPE_AT_SECOND = 2;
    public static int TYPE_NO = 3;
    public static int ADD_TYPE_OF_CHAT_TO_DESCRIPTION = TYPE_AT_FIRST;
    public static int TIME_DEFAULT = 200;
    public static int DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE = TIME_DEFAULT;
    public static String TYPE_OF_REWIND_KEY = "rewind";
    public static String TYPE_OF_REWIND_VIDEO_KEY = "rewind_video";
    public static String DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY = "delay_between_changing_of_seekbar_value";
    public static String SHOW_NUMBERS_OF_ITEMS_KEY = "show_numbers_of_items";
    public static String SHOW_INDEX_OF_ITEM_KEY = "show_index_of_item";
    public static String SHOW_SEEKBAR_VALUE_CHANGES_KEY = "show_seekbar_value_changes";
    public static String ADD_TYPE_OF_CHAT_TO_DESCRIPTION_KEY = "add_type_of_chat_to_description";
    public static String SHOW_LINK_NODES_KEY = "show_link_nodes";
    public static String HIDE_LINKS_KEY = "hide_links";
    public static boolean SHOW_NUMBERS_OF_ITEMS = true;
    public static boolean SHOW_INDEX_OF_ITEM = true;
    public static boolean SHOW_SEEKBAR_VALUE_CHANGES = true;
    public static boolean SHOW_LINK_NODES = false;
    public static boolean HIDE_LINKS = false;

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
            TYPE_OF_REWIND = preferences.getInt(TYPE_OF_REWIND_KEY, TYPE_PERCENT_REWIND);
            TYPE_OF_REWIND_VIDEO = preferences.getInt(TYPE_OF_REWIND_VIDEO_KEY, TYPE_PERCENT_REWIND);
            ADD_TYPE_OF_CHAT_TO_DESCRIPTION = preferences.getInt(ADD_TYPE_OF_CHAT_TO_DESCRIPTION_KEY, TYPE_AT_FIRST);
            DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE = preferences.getInt(DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY, TIME_DEFAULT);
            SHOW_NUMBERS_OF_ITEMS = preferences.getBoolean(SHOW_NUMBERS_OF_ITEMS_KEY, true);
            SHOW_INDEX_OF_ITEM = preferences.getBoolean(SHOW_INDEX_OF_ITEM_KEY, true);
            SHOW_SEEKBAR_VALUE_CHANGES = preferences.getBoolean(SHOW_SEEKBAR_VALUE_CHANGES_KEY, true);
            SHOW_LINK_NODES = preferences.getBoolean(SHOW_LINK_NODES_KEY, true);
            HIDE_LINKS = preferences.getBoolean(HIDE_LINKS_KEY, false);
            configLoaded = true;
        }
    }

    public static void setTypeOfRewind(int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(TYPE_OF_REWIND_KEY, value).commit();
        TYPE_OF_REWIND = value;
    }

    public static void setTypeOfRewindVideo(int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(TYPE_OF_REWIND_VIDEO_KEY, value).commit();
        TYPE_OF_REWIND_VIDEO = value;
    }

    public static void setAddTypeOfChatToDescription(int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(ADD_TYPE_OF_CHAT_TO_DESCRIPTION_KEY, value).commit();
        ADD_TYPE_OF_CHAT_TO_DESCRIPTION = value;
    }

    public static void setDelayBetweenAnnouncingOfChangingOfSeekbarValue(int value) {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE_KEY, value).commit();
        DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE = value;
    }

    public static void saveShowLinkNodes() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_LINK_NODES_KEY, !SHOW_LINK_NODES).commit();
        SHOW_LINK_NODES = !SHOW_LINK_NODES;
    }

    public static void saveHideLinks() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(HIDE_LINKS_KEY, !HIDE_LINKS).commit();
        HIDE_LINKS = !HIDE_LINKS;
    }

    public static void saveShowNumbersOfItems() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_NUMBERS_OF_ITEMS_KEY, !SHOW_NUMBERS_OF_ITEMS).commit();
        SHOW_NUMBERS_OF_ITEMS = !SHOW_NUMBERS_OF_ITEMS;
    }

    public static void saveShowIndexOfItem() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_INDEX_OF_ITEM_KEY, !SHOW_INDEX_OF_ITEM).commit();
        SHOW_INDEX_OF_ITEM = !SHOW_INDEX_OF_ITEM;
    }

    public static void saveShowSeekbarValueChanges() {
        SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("accconfig", Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(SHOW_SEEKBAR_VALUE_CHANGES_KEY, !SHOW_SEEKBAR_VALUE_CHANGES).commit();
        SHOW_SEEKBAR_VALUE_CHANGES = !SHOW_SEEKBAR_VALUE_CHANGES;
    }
}
