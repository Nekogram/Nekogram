package tw.nekomimi.nekogram.folder;

import androidx.core.util.Pair;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;

import java.util.LinkedHashMap;

import tw.nekomimi.nekogram.NekoConfig;


public class FolderIconHelper {
    public static LinkedHashMap<String, Integer> folderIcons = new LinkedHashMap<>() {{
        put("\uD83D\uDC31", R.drawable.filter_cat);
        put("\uD83D\uDCD5", R.drawable.filter_book);
        put("\uD83D\uDCB0", R.drawable.filter_money);
        //put("\uD83D\uDCF8", R.drawable.filter_camera);
        put("\uD83C\uDFAE", R.drawable.filter_game);
        //put("\uD83C\uDFE1", R.drawable.filter_house);
        put("\uD83D\uDCA1", R.drawable.filter_light);
        put("\uD83D\uDC4C", R.drawable.filter_like);
        //put("\u2795", R.drawable.filter_plus);
        put("\uD83C\uDFB5", R.drawable.filter_note);
        put("\uD83C\uDFA8", R.drawable.filter_palette);
        put("\u2708", R.drawable.filter_travel);
        put("\u26BD", R.drawable.filter_sport);
        put("\u2B50", R.drawable.filter_favorite);
        put("\uD83C\uDF93", R.drawable.filter_study);
        put("\uD83D\uDEEB", R.drawable.filter_airplane);
        //put("\uD83E\uDDA0", R.drawable.filter_microbe);
        //put("\uD83D\uDC68\u200D\uD83D\uDCBC", R.drawable.filter_worker);
        put("\uD83D\uDC64", R.drawable.filter_private);
        put("\uD83D\uDC65", R.drawable.filter_group);
        put("\uD83D\uDCAC", R.drawable.filter_all);
        put("\u2705", R.drawable.filter_unread);
        //put("\u2611", R.drawable.filter_check);
        put("\uD83E\uDD16", R.drawable.filter_bots);
        //put("\uD83D\uDDC2", R.drawable.filter_folders);
        put("\uD83D\uDC51", R.drawable.filter_crown);
        put("\uD83C\uDF39", R.drawable.filter_flower);
        put("\uD83C\uDFE0", R.drawable.filter_home);
        put("\u2764", R.drawable.filter_love);
        put("\uD83C\uDFAD", R.drawable.filter_mask);
        put("\uD83C\uDF78", R.drawable.filter_party);
        put("\uD83D\uDCC8", R.drawable.filter_trade);
        put("\uD83D\uDCBC", R.drawable.filter_work);
        put("\uD83D\uDD14", R.drawable.filter_unmuted);
        put("\uD83D\uDCE2", R.drawable.filter_channels);
        put("\uD83D\uDCC1", R.drawable.filter_custom);
        put("\uD83D\uDCCB", R.drawable.filter_setup);
        //put("\uD83D\uDCA9", R.drawable.filter_poo);
    }};

    public static Pair<String, String> getEmoticonFromFlags(int newFilterFlags) {
        int flags = newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
        String newName = "";
        String newEmoticon = "";
        if ((flags & MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) == MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS) {
            if ((newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ) != 0) {
                newName = LocaleController.getString(R.string.FilterNameUnread);
                newEmoticon = "\u2705";
            } else if ((newFilterFlags & MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED) != 0) {
                newName = LocaleController.getString(R.string.FilterNameNonMuted);
                newEmoticon = "\uD83D\uDD14";
            }
        } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_CONTACTS) != 0) {
            flags &= ~MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
            if (flags == 0) {
                newName = LocaleController.getString(R.string.FilterContacts);
                newEmoticon = "\uD83D\uDC64";
            } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
                flags &= ~MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
                if (flags == 0) {
                    newName = LocaleController.getString(R.string.FilterContacts);
                    newEmoticon = "\uD83D\uDC64";
                }
            }
        } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS) != 0) {
            flags &= ~MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;
            if (flags == 0) {
                newName = LocaleController.getString(R.string.FilterNonContacts);
                newEmoticon = "\uD83D\uDC64";
            }
        } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_GROUPS) != 0) {
            flags &= ~MessagesController.DIALOG_FILTER_FLAG_GROUPS;
            if (flags == 0) {
                newName = LocaleController.getString(R.string.FilterGroups);
                newEmoticon = "\uD83D\uDC65";
            }
        } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_BOTS) != 0) {
            flags &= ~MessagesController.DIALOG_FILTER_FLAG_BOTS;
            if (flags == 0) {
                newName = LocaleController.getString(R.string.FilterBots);
                newEmoticon = "\uD83E\uDD16";
            }
        } else if ((flags & MessagesController.DIALOG_FILTER_FLAG_CHANNELS) != 0) {
            flags &= ~MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
            if (flags == 0) {
                newName = LocaleController.getString(R.string.FilterChannels);
                newEmoticon = "\uD83D\uDCE2";
            }
        }
        return Pair.create(newName, newEmoticon);
    }

    public static int getIconWidth() {
        return AndroidUtilities.dp(28);
    }

    public static int getPadding() {
        if (NekoConfig.tabsTitleType == NekoConfig.TITLE_TYPE_MIX) {
            return AndroidUtilities.dp(6);
        }
        return 0;
    }

    public static int getTotalIconWidth() {
        int result = 0;
        if (NekoConfig.tabsTitleType != NekoConfig.TITLE_TYPE_TEXT) {
            result = getIconWidth() + getPadding();
        }
        return result;
    }

    public static int getPaddingTab() {
        if (NekoConfig.tabsTitleType != NekoConfig.TITLE_TYPE_ICON) {
            return AndroidUtilities.dp(32);
        }
        return AndroidUtilities.dp(16);
    }

    public static int getTabIcon(String emoji) {
        if (emoji != null) {
            var folderIcon = folderIcons.get(emoji);
            if (folderIcon != null) {
                return folderIcon;
            }
        }
        return R.drawable.filter_custom;
    }
}
