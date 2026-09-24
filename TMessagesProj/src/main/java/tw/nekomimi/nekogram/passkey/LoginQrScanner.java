package tw.nekomimi.nekogram.passkey;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.CameraScanActivity;

import java.util.function.BiConsumer;

import tw.nekomimi.nekogram.session.SessionQr;

public class LoginQrScanner {

    private static final int TYPE_UNKNOWN = -1;
    private static final int TYPE_PASSKEY = 0;
    private static final int TYPE_SESSION = 1;

    private static int getLinkType(String link) {
        if (link.startsWith("tg://passkey?")) {
            return TYPE_PASSKEY;
        } else if (link.startsWith("tg://session?")) {
            return TYPE_SESSION;
        } else {
            return TYPE_UNKNOWN;
        }
    }

    public static void showScanner(BaseFragment fragment, BiConsumer<TLRPC.auth_Authorization, String> done) {
        CameraScanActivity.showAsSheet(fragment, true, CameraScanActivity.TYPE_QR, new CameraScanActivity.CameraScanActivityDelegate() {
            @Override
            public void didFindQr(String link) {
                var type = getLinkType(link);
                if (type == TYPE_PASSKEY || type == TYPE_SESSION) {
                    try {
                        switch (type) {
                            case TYPE_PASSKEY -> {
                                var passkey = PasskeyQr.decodePasskeyUrl(link);
                                DecryptQrSheet.show(fragment.getParentActivity(), passkey,
                                        LocaleController.getString(R.string.PassQR),
                                        () -> PasskeyQr.login(fragment.getParentActivity(),
                                                fragment.getCurrentAccount(), passkey, done));
                            }
                            case TYPE_SESSION -> {
                                var session = SessionQr.decodeSessionUrl(link);
                                DecryptQrSheet.show(fragment.getParentActivity(), session,
                                        LocaleController.getString(R.string.SessionQR),
                                        () -> SessionQr.importSession(fragment.getParentActivity(),
                                                fragment.getCurrentAccount(), session, done));
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                        done.accept(null, LocaleController.getString(R.string.InvalidPassQr));
                    }
                }
            }

            @Override
            public boolean validateQr(String link) {
                return getLinkType(link) != TYPE_UNKNOWN;
            }
        });
    }
}
