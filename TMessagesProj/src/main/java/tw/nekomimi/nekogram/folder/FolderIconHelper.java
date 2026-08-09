package tw.nekomimi.nekogram.folder;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_ALL_CHATS;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_BOTS;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_CHANNELS;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_CONTACTS;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_MUTED;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_EXCLUDE_READ;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_GROUPS;
import static org.telegram.messenger.MessagesController.DIALOG_FILTER_FLAG_NON_CONTACTS;

import androidx.core.util.Pair;

import org.telegram.messenger.R;

import java.util.LinkedHashMap;

import me.vkryl.core.BitwiseUtils;

public class FolderIconHelper {

    public static final String EMOTICON_ALL = "\uD83D\uDCAC";
    public static final String EMOTICON_CUSTOM = "\uD83D\uDCC1";
    public static final String EMOTICON_UNREAD = "\u2705";
    public static final String EMOTICON_UNMUTED = "\uD83D\uDD14";
    public static final String EMOTICON_PRIVATE = "\uD83D\uDC64";
    public static final String EMOTICON_GROUPS = "\uD83D\uDC65";
    public static final String EMOTICON_CHANNELS = "\uD83D\uDCE2";
    public static final String EMOTICON_BOTS = "\uD83E\uDD16";

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
        put(EMOTICON_PRIVATE, R.drawable.filter_private);
        put(EMOTICON_GROUPS, R.drawable.filter_group);
        put(EMOTICON_ALL, R.drawable.filter_all);
        put(EMOTICON_UNREAD, R.drawable.filter_unread);
        //put("\u2611", R.drawable.filter_check);
        put(EMOTICON_BOTS, R.drawable.filter_bots);
        //put("\uD83D\uDDC2", R.drawable.filter_folders);
        put("\uD83D\uDC51", R.drawable.filter_crown);
        put("\uD83C\uDF39", R.drawable.filter_flower);
        put("\uD83C\uDFE0", R.drawable.filter_home);
        put("\u2764", R.drawable.filter_love);
        put("\uD83C\uDFAD", R.drawable.filter_mask);
        put("\uD83C\uDF78", R.drawable.filter_party);
        put("\uD83D\uDCC8", R.drawable.filter_trade);
        put("\uD83D\uDCBC", R.drawable.filter_work);
        put(EMOTICON_UNMUTED, R.drawable.filter_unmuted);
        put(EMOTICON_CHANNELS, R.drawable.filter_channels);
        put(EMOTICON_CUSTOM, R.drawable.filter_custom);
        put("\uD83D\uDCCB", R.drawable.filter_setup);
        //put("\uD83D\uDCA9", R.drawable.filter_poo);
    }};

    public static Pair<String, String> getEmoticonFromFlags(int newFilterFlags) {
        var flags = newFilterFlags & DIALOG_FILTER_FLAG_ALL_CHATS;
        if (BitwiseUtils.hasAllFlags(flags, DIALOG_FILTER_FLAG_ALL_CHATS)) {
            if (BitwiseUtils.hasFlag(newFilterFlags, DIALOG_FILTER_FLAG_EXCLUDE_READ)) {
                return Pair.create(getString(R.string.FilterNameUnread), EMOTICON_UNREAD);
            } else if (BitwiseUtils.hasFlag(newFilterFlags, DIALOG_FILTER_FLAG_EXCLUDE_MUTED)) {
                return Pair.create(getString(R.string.FilterNameNonMuted), EMOTICON_UNMUTED);
            }
        } else if (BitwiseUtils.hasFlag(flags, DIALOG_FILTER_FLAG_CONTACTS)) {
            flags = BitwiseUtils.setFlag(flags, DIALOG_FILTER_FLAG_CONTACTS, false);
            if (flags == 0) {
                return Pair.create(getString(R.string.FilterContacts), EMOTICON_PRIVATE);
            } else if (BitwiseUtils.hasFlag(flags, DIALOG_FILTER_FLAG_NON_CONTACTS)) {
                flags = BitwiseUtils.setFlag(flags, DIALOG_FILTER_FLAG_NON_CONTACTS, false);
                if (flags == 0) {
                    return Pair.create(getString(R.string.FilterContacts), EMOTICON_PRIVATE);
                }
            }
        } else if (BitwiseUtils.hasFlag(flags, DIALOG_FILTER_FLAG_NON_CONTACTS)) {
            flags = BitwiseUtils.setFlag(flags, DIALOG_FILTER_FLAG_NON_CONTACTS, false);
            if (flags == 0) {
                return Pair.create(getString(R.string.FilterNonContacts), EMOTICON_PRIVATE);
            }
        } else if (BitwiseUtils.hasFlag(flags, DIALOG_FILTER_FLAG_GROUPS)) {
            flags = BitwiseUtils.setFlag(flags, DIALOG_FILTER_FLAG_GROUPS, false);
            if (flags == 0) {
                return Pair.create(getString(R.string.FilterGroups), EMOTICON_GROUPS);
            }
        } else if (BitwiseUtils.hasFlag(flags, DIALOG_FILTER_FLAG_BOTS)) {
            flags = BitwiseUtils.setFlag(flags, DIALOG_FILTER_FLAG_BOTS, false);
            if (flags == 0) {
                return Pair.create(getString(R.string.FilterBots), EMOTICON_BOTS);
            }
        } else if (BitwiseUtils.hasFlag(flags, DIALOG_FILTER_FLAG_CHANNELS)) {
            flags = BitwiseUtils.setFlag(flags, DIALOG_FILTER_FLAG_CHANNELS, false);
            if (flags == 0) {
                return Pair.create(getString(R.string.FilterChannels), EMOTICON_CHANNELS);
            }
        }
        return Pair.create("", EMOTICON_CUSTOM);
    }

    public static int getIconWidth() {
        return dp(24);
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
