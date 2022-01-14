package tw.nekomimi.nekogram.folder;


import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;

import java.util.LinkedHashMap;

import tw.nekomimi.nekogram.NekoConfig;


public class FolderIconHelper {
    public static LinkedHashMap<String, FolderIcon> folderIcons = new LinkedHashMap<>();

    static {
        folderIcons.put("\uD83D\uDC31", FolderIcon.obtain(R.drawable.filter_cat, R.drawable.filter_cat_active));
        folderIcons.put("\uD83D\uDC51", FolderIcon.obtain(R.drawable.filter_crown, R.drawable.filter_crown_active));
        folderIcons.put("\u2B50", FolderIcon.obtain(R.drawable.filter_favorite, R.drawable.filter_favorite_active));
        folderIcons.put("\uD83C\uDF39", FolderIcon.obtain(R.drawable.filter_flower, R.drawable.filter_flower_active));
        folderIcons.put("\uD83C\uDFAE", FolderIcon.obtain(R.drawable.filter_game, R.drawable.filter_game_active));
        folderIcons.put("\uD83C\uDFE0", FolderIcon.obtain(R.drawable.filter_home, R.drawable.filter_home_active));
        folderIcons.put("\u2764", FolderIcon.obtain(R.drawable.filter_love, R.drawable.filter_love_active));
        folderIcons.put("\uD83C\uDFAD", FolderIcon.obtain(R.drawable.filter_mask, R.drawable.filter_mask_active));
        folderIcons.put("\uD83C\uDF78", FolderIcon.obtain(R.drawable.filter_party, R.drawable.filter_party_active));
        folderIcons.put("\u26BD", FolderIcon.obtain(R.drawable.filter_sport, R.drawable.filter_sport_active));
        folderIcons.put("\uD83C\uDF93", FolderIcon.obtain(R.drawable.filter_study, R.drawable.filter_study_active));
        folderIcons.put("\uD83D\uDCC8", FolderIcon.obtain(R.drawable.filter_trade, R.drawable.filter_trade));
        folderIcons.put("\u2708", FolderIcon.obtain(R.drawable.filter_travel, R.drawable.filter_travel_active));
        folderIcons.put("\uD83D\uDCBC", FolderIcon.obtain(R.drawable.filter_work, R.drawable.filter_work_active));
        folderIcons.put("\uD83D\uDCAC", FolderIcon.obtain(R.drawable.filter_all, R.drawable.filter_all_active));
        folderIcons.put("\u2705", FolderIcon.obtain(R.drawable.filter_unread, R.drawable.filter_unread_active));
        folderIcons.put("\uD83D\uDD14", FolderIcon.obtain(R.drawable.filter_unmuted, R.drawable.filter_unmuted_active));
        folderIcons.put("\uD83E\uDD16", FolderIcon.obtain(R.drawable.filter_bot, R.drawable.filter_bot_active));
        folderIcons.put("\uD83D\uDCE2", FolderIcon.obtain(R.drawable.filter_channel, R.drawable.filter_channel_active));
        folderIcons.put("\uD83D\uDC65", FolderIcon.obtain(R.drawable.filter_groups, R.drawable.filter_groups_active));
        folderIcons.put("\uD83D\uDC64", FolderIcon.obtain(R.drawable.filter_private, R.drawable.filter_private_active));
        folderIcons.put("\uD83D\uDCC1", FolderIcon.obtain(R.drawable.filter_custom, R.drawable.filter_custom_active));
        folderIcons.put("\uD83D\uDCCB", FolderIcon.obtain(R.drawable.filter_setup, R.drawable.filter_setup));
    }

    public static class FolderIcon {
        public int icon;
        public int iconActive;

        public static FolderIcon obtain(int icon, int iconActive) {
            var folderIcon = new FolderIcon();
            folderIcon.icon = icon;
            folderIcon.iconActive = iconActive;
            return folderIcon;
        }
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

    public static int getTabIcon(String emoji, boolean active) {
        if (emoji != null) {
            var folderIcon = folderIcons.get(emoji);
            if (folderIcon != null) {
                return active ? folderIcon.iconActive : folderIcon.icon;
            }
        }
        if (active) {
            return R.drawable.filter_custom_active;
        } else {
            return R.drawable.filter_custom;
        }
    }
}
