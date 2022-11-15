package tw.nekomimi.nekogram.helpers;

import android.graphics.Bitmap;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.CameraScanActivity;

import java.util.HashMap;

public class QrHelper {

    public static void openCameraScanActivity(BaseFragment fragment) {
        CameraScanActivity.showAsSheet(fragment, false, CameraScanActivity.TYPE_QR, new CameraScanActivity.CameraScanActivityDelegate() {

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

    public static Bitmap createQR(String key) {
        try {
            HashMap<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            QRCodeWriter writer = new QRCodeWriter();
            return writer.encode(key, 768, 768, hints, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }
}
