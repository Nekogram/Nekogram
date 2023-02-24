package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.CameraScanActivity;

public class QrHelper {

    public static void openCameraScanActivity(BaseFragment fragment) {
        CameraScanActivity.showAsSheet(fragment, true, CameraScanActivity.TYPE_QR, new CameraScanActivity.CameraScanActivityDelegate() {

            @Override
            public void didFindQr(String link) {
                Browser.openUrl(fragment.getParentActivity(), link, true, false);
            }

            @Override
            public boolean processQr(String link, Runnable onLoadEnd) {
                AndroidUtilities.runOnUIThread(onLoadEnd, 750);
                return true;
            }
        });
    }
}
