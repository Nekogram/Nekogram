package tw.nekomimi.nekogram;

import android.os.SystemClock;
import android.util.TypedValue;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import tw.nekomimi.nekogram.helpers.MessageHelper;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class DatacenterPopupWrapper {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;

    public DatacenterPopupWrapper(BaseFragment fragment, PopupSwipeBackLayout swipeBackLayout, Theme.ResourcesProvider resourcesProvider) {
        var context = fragment.getParentActivity();
        windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, swipeBackLayout != null ? 0 : R.drawable.popup_fixed_alert2, resourcesProvider, ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
        windowLayout.setFitItems(true);

        if (swipeBackLayout != null) {
            var backItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_arrow_back, LocaleController.getString("Back", R.string.Back), false, resourcesProvider);
            backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());

            ActionBarMenuItem.addColoredGap(windowLayout, resourcesProvider);
        }

        for (var datacenterInfo : NekoConfig.datacenterInfos) {
            var item = ActionBarMenuItem.addItem(windowLayout, 0, MessageHelper.formatDCString(datacenterInfo.id), false, resourcesProvider);
            item.setTag(datacenterInfo);
            item.setOnClickListener(view -> {
                if (datacenterInfo.checking) {
                    return;
                }
                checkDatacenter(item, true, resourcesProvider);
            });
            updateStatus(item, resourcesProvider);
            checkDatacenter(item, false, resourcesProvider);
        }

        ActionBarMenuItem.addColoredGap(windowLayout, resourcesProvider);

        var textView = new LinkSpanDrawable.LinksTextView(context);
        textView.setTag(R.id.fit_width_tag, 1);
        textView.setPadding(AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13), AndroidUtilities.dp(8));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView.setText(BaseNekoSettingsActivity.getSpannedString("DatacenterStatusAbout", R.string.DatacenterStatusAbout, "https://core.telegram.org/api/datacenter"));
        windowLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8, 0, 0));
    }

    private void checkDatacenter(ActionBarMenuSubItem item, boolean force, Theme.ResourcesProvider resourcesProvider) {
        var datacenterInfo = (NekoConfig.DatacenterInfo) item.getTag();
        if (datacenterInfo.checking) {
            return;
        }
        if (!force && SystemClock.elapsedRealtime() - datacenterInfo.availableCheckTime < 2 * 60 * 1000) {
            return;
        }
        datacenterInfo.checking = true;
        updateStatus(item, resourcesProvider);
        datacenterInfo.pingId = ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy("ping.neko", datacenterInfo.id, null, null, null, time -> AndroidUtilities.runOnUIThread(() -> {
            datacenterInfo.availableCheckTime = SystemClock.elapsedRealtime();
            datacenterInfo.checking = false;
            if (time == -1) {
                datacenterInfo.available = false;
                datacenterInfo.ping = 0;
            } else {
                datacenterInfo.ping = time;
                datacenterInfo.available = true;
            }
            updateStatus(item, resourcesProvider);
        }));
    }

    public void updateStatus(ActionBarMenuSubItem item, Theme.ResourcesProvider resourcesProvider) {
        var datacenterInfo = (NekoConfig.DatacenterInfo) item.getTag();
        int colorKey;
        if (datacenterInfo.checking) {
            item.setSubtext(LocaleController.getString("Checking", R.string.Checking));
            colorKey = Theme.key_windowBackgroundWhiteGrayText2;
        } else if (datacenterInfo.available) {
            if (datacenterInfo.ping >= 1000) {
                item.setSubtext(LocaleController.formatString("Ping", R.string.Ping, datacenterInfo.ping));
                colorKey = Theme.key_text_RedRegular;
            } else if (datacenterInfo.ping != 0) {
                item.setSubtext(LocaleController.formatString("Ping", R.string.Ping, datacenterInfo.ping));
                colorKey = Theme.key_windowBackgroundWhiteGreenText;
            } else {
                item.setSubtext(LocaleController.getString("Available", R.string.Available));
                colorKey = Theme.key_windowBackgroundWhiteGreenText;
            }
        } else {
            item.setSubtext(LocaleController.getString("Unavailable", R.string.Unavailable));
            colorKey = Theme.key_text_RedRegular;
        }
        item.setSubtextColor(Theme.getColor(colorKey, resourcesProvider));
    }
}
