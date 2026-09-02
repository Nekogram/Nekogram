package tw.nekomimi.nekogram.forward;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import java.util.Arrays;

public class ForwardPopupWrapper {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;

    public ForwardPopupWrapper(ChatActivity fragment, MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup, PopupSwipeBackLayout swipeBackLayout, ActionBarMenuItem.ActionBarMenuItemDelegate delegate, Theme.ResourcesProvider resourcesProvider) {
        var context = fragment.getParentActivity();
        windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, resourcesProvider, ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
        windowLayout.setFitItems(true);

        if (swipeBackLayout != null) {
            var backItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_arrow_back, LocaleController.getString(R.string.Back), false, resourcesProvider);
            backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());

            ActionBarMenuItem.addColoredGap(windowLayout, resourcesProvider);
        }

        var hasCaption = ForwardItem.hasCaption(selectedObject, selectedObjectGroup);

        Arrays.stream(ForwardItem.ITEM_IDS).forEach(id -> {
            if (!hasCaption && id == ForwardItem.ID_FORWARD_NOCAPTION) {
                return;
            }
            var item = ActionBarMenuItem.addItem(false, false, windowLayout, 0, new ForwardDrawable(id, false), LocaleController.getString(ForwardItem.ITEM_TITLES.get(id)), false, resourcesProvider);
            item.setOnClickListener(view -> delegate.onItemClick(id));
        });
    }
}
