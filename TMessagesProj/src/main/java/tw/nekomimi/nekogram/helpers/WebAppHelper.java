package tw.nekomimi.nekogram.helpers;

import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.bots.BotWebViewAttachedSheet;
import org.telegram.ui.bots.BotWebViewSheet;
import org.telegram.ui.bots.WebViewRequestProps;

import tw.nekomimi.nekogram.Extra;

public class WebAppHelper {
    public static final int INTERNAL_BOT_TLV = 1;

    public static boolean isInternalBot(WebViewRequestProps props) {
        return props.internalType > 0;
    }

    public static String getInternalBotName(WebViewRequestProps props) {
        switch (props.internalType) {
            case INTERNAL_BOT_TLV:
                return LocaleController.getString(R.string.ViewAsJson);
            default:
                return "";
        }
    }

    public static void openTLViewer(BaseFragment fragment, TLObject object) {
        var serialized = "";
        try {
            var data = new SerializedData(object.getObjectSize());
            object.serializeToStream(data);
            serialized = Base64.encodeToString(data.toByteArray(), Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
            data.cleanup();
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (TextUtils.isEmpty(serialized)) {
            return;
        }
        var url = Extra.TLV_URL + "#m=" + serialized + "&l=" + TLRPC.LAYER;
        openInternalWebApp(fragment, url, INTERNAL_BOT_TLV, true);
    }

    private static void openInternalWebApp(BaseFragment fragment, String url, int type, boolean searchUser) {
        var botInfo = Extra.getHelperBot();
        var bot = fragment.getMessagesController().getUser(botInfo.getId());
        if (bot == null) {
            if (searchUser) {
                fragment.getUserHelper().resolveUser(botInfo.getUsername(), botInfo.getId(), user -> openInternalWebApp(fragment, url, type, false));
            }
            return;
        }
        var props = WebViewRequestProps.of(
                fragment.getCurrentAccount(),
                bot.id,
                bot.id,
                null,
                url,
                BotWebViewAttachedSheet.TYPE_WEB_VIEW_BUTTON,
                0, false, null, false, null, null, 0, false, false);
        props.internalType = type;
        var context = fragment.getParentActivity();
        if (context instanceof LaunchActivity activity) {
            if (activity.getBottomSheetTabs() != null && activity.getBottomSheetTabs().tryReopenTab(props) != null) {
                return;
            }
        }
        var webViewSheet = new BotWebViewSheet(context, fragment.getResourceProvider());
        webViewSheet.setDefaultFullsize(false);
        webViewSheet.setNeedsContext(false);
        webViewSheet.setParentActivity(context);
        webViewSheet.requestWebView(null, props);
        webViewSheet.show();
    }
}
