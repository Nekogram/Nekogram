package tw.nekomimi.nekogram;

import android.os.SystemClock;
import android.util.TypedValue;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.utils.proxy.ProxySettings;

import java.util.ArrayList;

import tw.nekomimi.nekogram.helpers.UserHelper;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class DatacenterPopupWrapper {

    private static final ArrayList<DatacenterInfo> datacenterInfos = new ArrayList<>(5) {{
        for (int a = 1; a <= 5; a++) {
            add(new DatacenterInfo(a));
        }
    }};

    public static void fillDcItems(ItemOptions options, Runnable closeSwipeBack, Theme.ResourcesProvider resourcesProvider) {
        if (closeSwipeBack != null) {
            options.add(R.drawable.msg_arrow_back, LocaleController.getString(R.string.Back), closeSwipeBack);
            options.addGap();
        }

        for (var datacenterInfo : datacenterInfos) {
            options.add(UserHelper.formatDCString(datacenterInfo.id), () -> {
            });
            var item = options.getLast();
            item.setTag(datacenterInfo);
            item.setOnClickListener(view -> {
                if (datacenterInfo.checking) {
                    return;
                }
                checkDatacenter(item, true, resourcesProvider);
            });
            updateStatus(item, resourcesProvider, false);
            checkDatacenter(item, false, resourcesProvider);
        }

        options.addGap();

        var textView = new LinkSpanDrawable.LinksTextView(options.getContext());
        textView.setTag(R.id.fit_width_tag, 1);
        textView.setPadding(AndroidUtilities.dp(13), 0, AndroidUtilities.dp(13), AndroidUtilities.dp(8));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText));
        textView.setText(BaseNekoSettingsActivity.getSpannedString(R.string.DatacenterStatusAbout, "https://core.telegram.org/api/datacenter"));
        options.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8, 0, 0));
    }

    private static void checkDatacenter(ActionBarMenuSubItem item, boolean force, Theme.ResourcesProvider resourcesProvider) {
        var datacenterInfo = (DatacenterInfo) item.getTag();
        if (datacenterInfo.checking) {
            return;
        }
        if (!force && SystemClock.elapsedRealtime() - datacenterInfo.availableCheckTime < 2 * 60 * 1000) {
            return;
        }
        datacenterInfo.checking = true;
        updateStatus(item, resourcesProvider, true);
        var proxySettings = ProxySettings.builder().setAddress("ping.neko").setPort(datacenterInfo.id).setType(ProxySettings.Type.SOCKS5).build();
        datacenterInfo.pingId = ConnectionsManager.getInstance(UserConfig.selectedAccount).checkProxy(proxySettings, time -> AndroidUtilities.runOnUIThread(() -> {
            datacenterInfo.availableCheckTime = SystemClock.elapsedRealtime();
            datacenterInfo.checking = false;
            if (time == -1) {
                datacenterInfo.available = false;
                datacenterInfo.ping = 0;
            } else {
                datacenterInfo.ping = time;
                datacenterInfo.available = true;
            }
            updateStatus(item, resourcesProvider, true);
        }));
    }

    private static void updateStatus(ActionBarMenuSubItem item, Theme.ResourcesProvider resourcesProvider, boolean animated) {
        var datacenterInfo = (DatacenterInfo) item.getTag();
        int colorKey;
        if (datacenterInfo.checking) {
            item.setAnimatedSubtext(LocaleController.getString(R.string.Checking), animated);
            colorKey = Theme.key_windowBackgroundWhiteGrayText2;
        } else if (datacenterInfo.available) {
            if (datacenterInfo.ping >= 1000) {
                item.setAnimatedSubtext(LocaleController.formatString(R.string.Ping, datacenterInfo.ping), animated);
                colorKey = Theme.key_text_RedRegular;
            } else if (datacenterInfo.ping != 0) {
                item.setAnimatedSubtext(LocaleController.formatString(R.string.Ping, datacenterInfo.ping), animated);
                colorKey = Theme.key_windowBackgroundWhiteGreenText;
            } else {
                item.setAnimatedSubtext(LocaleController.getString(R.string.Available), animated);
                colorKey = Theme.key_windowBackgroundWhiteGreenText;
            }
        } else {
            item.setAnimatedSubtext(LocaleController.getString(R.string.Unavailable), animated);
            colorKey = Theme.key_text_RedRegular;
        }
        item.setAnimatedSubtextColor(Theme.getColor(colorKey, resourcesProvider));
    }

    private static class DatacenterInfo {

        public int id;

        public long pingId;
        public long ping;
        public boolean checking;
        public boolean available;
        public long availableCheckTime;

        public DatacenterInfo(int i) {
            id = i;
        }
    }
}
