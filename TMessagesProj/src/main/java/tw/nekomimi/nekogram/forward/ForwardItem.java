package tw.nekomimi.nekogram.forward;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.SparseIntArray;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;

import java.util.Arrays;
import java.util.Collection;

import tw.nekomimi.nekogram.NekoConfig;

public class ForwardItem {
    public static final int ID_FORWARD = -100;
    public static final int ID_FORWARD_NOQUOTE = -101;
    public static final int ID_FORWARD_NOCAPTION = -102;

    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoforward", Context.MODE_PRIVATE);
    public static int lastForwardOption = preferences.getInt("lastForwardOption", ForwardItem.ID_FORWARD);

    static final int[] ITEM_IDS = new int[]{
            ID_FORWARD,
            ID_FORWARD_NOQUOTE,
            ID_FORWARD_NOCAPTION,
    };
    static final SparseIntArray ITEM_TITLES = new SparseIntArray() {{
        put(ID_FORWARD, R.string.Forward);
        put(ID_FORWARD_NOQUOTE, R.string.NoQuoteForward);
        put(ID_FORWARD_NOCAPTION, R.string.NoCaptionForward);
    }};
    static final SparseIntArray ITEM_TITLES_SHORT = new SparseIntArray() {{
        put(ID_FORWARD, R.string.Forward);
        put(ID_FORWARD_NOQUOTE, R.string.NoQuoteForwardShort);
        put(ID_FORWARD_NOCAPTION, R.string.NoCaptionForwardShort);
    }};

    public static void setLastForwardOption(int option) {
        lastForwardOption = option;
        preferences.edit().putInt("lastForwardOption", lastForwardOption).apply();
    }

    public static void setupForwardItem(ActionBarMenuItem item, boolean hasCaption, Theme.ResourcesProvider resourcesProvider, ActionBarMenuItem.ActionBarMenuItemDelegate delegate) {
        setupForwardItem(item, true, false, hasCaption, resourcesProvider, delegate);
    }

    public static void setupForwardItem(ActionBarMenuItem item, boolean setIcon, boolean darkTheme, boolean hasCaption, Theme.ResourcesProvider resourcesProvider, ActionBarMenuItem.ActionBarMenuItemDelegate delegate) {
        setupForwardItem(item, setIcon, darkTheme, hasCaption, false, resourcesProvider, delegate);
    }

    public static void setupForwardItem(ActionBarMenuItem item, boolean setIcon, boolean darkTheme, boolean hasCaption, boolean share, Theme.ResourcesProvider resourcesProvider, ActionBarMenuItem.ActionBarMenuItemDelegate delegate) {
        if (setIcon) {
            item.setIcon(getLastForwardOptionIcon(hasCaption, share));
            item.setContentDescription(getLastForwardOptionTitle(hasCaption, false));
        }
        if (!item.hasSubMenu()) {
            Arrays.stream(ITEM_IDS).forEach(itemId -> {
                var subItem = item.addSubItem(itemId, new ForwardDrawable(itemId, share), LocaleController.getString(ITEM_TITLES.get(itemId)), resourcesProvider);
                if (darkTheme) subItem.setColors(0xfffafafa, 0xfffafafa);
            });
            if (darkTheme) {
                item.redrawPopup(0xf9222222);
                item.setPopupItemsSelectorColor(0x0fffffff);
            }
        }
        if (hasCaption) {
            item.showSubItem(ID_FORWARD_NOCAPTION);
        } else {
            item.hideSubItem(ID_FORWARD_NOCAPTION);
        }
        item.setOnClickListener(v -> delegate.onItemClick(getLastForwardOption(hasCaption)));
        item.setLongClickEnabled(true);
    }

    public static boolean hasCaption(Collection<MessageObject> messages) {
        return messages.stream().anyMatch(messageObject -> !TextUtils.isEmpty(messageObject.caption));
    }

    public static boolean hasCaption(MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup) {
        if (!TextUtils.isEmpty(selectedObject.caption)) {
            return true;
        } else if (selectedObjectGroup != null) {
            return selectedObjectGroup.messages.stream().anyMatch(messageObject -> !TextUtils.isEmpty(messageObject.caption));
        } else {
            return false;
        }
    }

    public static String getLastForwardOptionTitle(boolean hasCaption, boolean shortTitle) {
        return LocaleController.getString((shortTitle ? ITEM_TITLES_SHORT : ITEM_TITLES).get(getLastForwardOption(hasCaption)));
    }

    public static ForwardDrawable getLastForwardOptionIcon(boolean hasCaption) {
        return getLastForwardOptionIcon(hasCaption, false);
    }

    public static ForwardDrawable getLastForwardOptionIcon(boolean hasCaption, boolean share) {
        return new ForwardDrawable(getLastForwardOption(hasCaption), share);
    }

    public static int getLastForwardOption(boolean hasCaption) {
        var lastOption = ForwardItem.lastForwardOption;
        if (NekoConfig.showNoQuoteForward && lastOption == ID_FORWARD_NOQUOTE) {
            return ID_FORWARD;
        }
        if (!hasCaption && lastOption == ID_FORWARD_NOCAPTION) {
            return NekoConfig.showNoQuoteForward ? ID_FORWARD : ID_FORWARD_NOQUOTE;
        }
        return lastOption;
    }
}
