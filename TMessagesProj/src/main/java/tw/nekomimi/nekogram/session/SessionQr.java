package tw.nekomimi.nekogram.session;

import android.content.Context;
import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.Vector;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

import java.security.GeneralSecurityException;
import java.util.function.BiConsumer;

import tw.nekomimi.nekogram.passkey.Encryptable;
import tw.nekomimi.nekogram.passkey.PasskeyQr;
import tw.nekomimi.nekogram.passkey.QrEncryption;
import tw.nekomimi.nekogram.passkey.SaveQrSheet;

public class SessionQr {

    public static void exportSession(BaseFragment fragment) {
        var currentAccount = fragment.getCurrentAccount();
        try {
            var sessionData = exportSession(currentAccount);
            var userConfig = UserConfig.getInstance(currentAccount);
            var userId = userConfig.clientUserId;
            var session = new Session(userId, sessionData, false);
            SaveQrSheet.show(fragment.getParentActivity(), currentAccount, session,
                    LocaleController.getString(R.string.CreateSessionQR),
                    LocaleController.getString(R.string.SessionQR),
                    () -> BulletinFactory.of(fragment).createDownloadBulletin(BulletinFactory.FileType.PHOTO).show());
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private static byte[] exportSession(int currentAccount) {
        var userConfig = UserConfig.getInstance(currentAccount);
        if (!userConfig.isClientActivated() || userConfig.getCurrentUser() == null) {
            throw new IllegalStateException("User is not logged in");
        }
        var dcId = ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId();
        var authKey = ConnectionsManager.getInstance(currentAccount).exportAuthKey(dcId);
        if (authKey == null || authKey.length != 256) {
            throw new IllegalStateException("Failed to export auth key");
        }
        var apiId = BuildConfig.API_ID;
        var testMode = ConnectionsManager.getInstance(currentAccount).isTestBackend();
        var userId = userConfig.clientUserId;

        return PyrogramSession.pack(dcId, apiId, testMode, authKey, userId, false);
    }

    public static Session decodeSessionUrl(String url) {
        var uri = Uri.parse(url);
        var id = uri.getQueryParameter("id");
        var encryptedSessionString = uri.getQueryParameter("d");
        var sessionString = encryptedSessionString != null ? encryptedSessionString : uri.getQueryParameter("ss");
        if (id == null || sessionString == null) {
            throw new IllegalStateException("Invalid session URL");
        }
        return new Session(Long.parseLong(id), PasskeyQr.fromBase64Url(sessionString), encryptedSessionString != null);
    }

    public static class Session implements Encryptable {
        public long userId;
        public byte[] sessionData;
        private boolean needDecryption;

        public Session(long userId, byte[] sessionData, boolean needDecryption) {
            this.userId = userId;
            this.sessionData = sessionData;
            this.needDecryption = needDecryption;
        }

        @Override
        public String encryptToUrl(byte[] password) throws GeneralSecurityException {
            var builder = new Uri.Builder();
            builder.scheme("tg");
            builder.authority("session");
            builder.appendQueryParameter("id", String.valueOf(userId));
            var ed = QrEncryption.encrypt(sessionData, password);
            builder.appendQueryParameter("d", PasskeyQr.toBase64Url(ed));
            return builder.build().toString();
        }

        @Override
        public void decrypt(byte[] password) throws GeneralSecurityException {
            if (needDecryption) {
                sessionData = QrEncryption.decrypt(sessionData, password);
                needDecryption = false;
            }
        }

        @Override
        public boolean needDecryption() {
            return needDecryption;
        }
    }

    public static void importSession(Context context, int currentAccount, Session session, BiConsumer<TLRPC.auth_Authorization, String> done) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            var userConfig = UserConfig.getInstance(a);
            if (!userConfig.isClientActivated()) {
                continue;
            }
            var userId = userConfig.getClientUserId();
            if (userId == session.userId) {
                var builder = new AlertDialog.Builder(context);
                builder.setTitle(LocaleController.getString(R.string.AppName));
                builder.setMessage(LocaleController.getString(R.string.AccountAlreadyLoggedIn));
                builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                builder.show();
                return;
            }
        }
        var progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.showDelayed(500);
        var sessionData = PyrogramSession.unpack(session.sessionData);
        importAuthKey(currentAccount, sessionData.testMode(), sessionData.dcId(), sessionData.authKey(), (authAuthorization, error) -> {
            progressDialog.dismiss();
            done.accept(authAuthorization, error);
        });
    }

    private static void importAuthKey(int currentAccount, boolean testBackend, int dcId, byte[] authKey, BiConsumer<TLRPC.auth_Authorization, String> done) {
        var currentTestBackend = ConnectionsManager.getInstance(currentAccount).isTestBackend();
        if (currentTestBackend != testBackend) {
            ConnectionsManager.getInstance(currentAccount).switchBackend(false);
        }
        ConnectionsManager.getInstance(currentAccount).cleanup(false);
        ConnectionsManager.getInstance(currentAccount).importAuthKey(dcId, authKey);
        var req = new TLRPC.TL_users_getUsers();
        req.id.add(new TLRPC.TL_inputUserSelf());
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error != null) {
                done.accept(null, error.text);
                return;
            }
            //noinspection unchecked
            var res = (Vector<TLRPC.User>) response;
            var user = res.objects.get(0);

            var fakeAuthorization = new TLRPC.TL_auth_authorization();
            fakeAuthorization.user = user;

            done.accept(fakeAuthorization, null);
        }), ConnectionsManager.RequestFlagFailOnServerErrors | ConnectionsManager.RequestFlagWithoutLogin);
    }
}
