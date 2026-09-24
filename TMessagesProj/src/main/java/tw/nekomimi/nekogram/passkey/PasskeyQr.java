package tw.nekomimi.nekogram.passkey;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.interfaces.ECPublicKey;
import java.util.function.BiConsumer;

public class PasskeyQr {

    private final static SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekopassqr", Context.MODE_PRIVATE);

    public static boolean isKnownPassQR(String id) {
        return preferences.getBoolean("passqr_" + id, false);
    }

    private static void putKnownPassQR(String id) {
        preferences.edit().putBoolean("passqr_" + id, true).apply();
    }

    public static String toBase64Url(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    public static byte[] fromBase64Url(String base64Url) {
        return Base64.decode(base64Url, Base64.URL_SAFE);
    }

    public static Passkey decodePasskeyUrl(String url) {
        var uri = Uri.parse(url);
        var privateKey = uri.getQueryParameter("pk");
        var userId = uri.getQueryParameter("h");
        var credentialId = uri.getQueryParameter("cid");
        if (privateKey == null || userId == null || credentialId == null) {
            throw new IllegalStateException("Invalid session URL");
        }
        return new Passkey(userId, credentialId, fromBase64Url(privateKey), true);
    }

    public static void create(Context context, int currentAccount, BiConsumer<TL_account.Passkey, String> done) {
        var progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.showDelayed(500);
        var requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(
                new TL_account.initPasskeyRegistration(),
                AndroidUtilities::runOnUIThread,
                (res, err) -> {
                    progressDialog.dismiss();
                    if (err != null) {
                        done.accept(null, err.text);
                        return;
                    }
                    try {
                        var response = createPasskey(res.options.data);
                        SaveQrSheet.show(context, currentAccount, response.passkey, LocaleController.getString(R.string.CreatePassQR), LocaleController.getString(R.string.PassQR), () -> register(context, currentAccount, response, done));
                    } catch (Exception e) {
                        FileLog.e(e);
                        done.accept(null, e.getMessage());
                    }
                }
        );
        progressDialog.setOnCancelListener(d -> {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
            done.accept(null, "CANCELLED");
        });
    }

    private static void register(Context context, int currentAccount, CreationResponse response, BiConsumer<TL_account.Passkey, String> done) {
        var passkeyResponse = new TL_account.inputPasskeyResponseRegister();
        passkeyResponse.client_data = new TLRPC.TL_dataJSON();
        passkeyResponse.client_data.data = response.clientDataJSON;
        passkeyResponse.attestation_object = response.attestationObject;

        var credential = new TL_account.inputPasskeyCredentialPublicKey();
        credential.id = response.passkey.id;
        credential.raw_id = response.passkey.id;
        credential.response = passkeyResponse;

        var req = new TL_account.registerPasskey();
        req.credential = credential;

        var progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.showDelayed(500);

        var requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, AndroidUtilities::runOnUIThread, (passkey, err) -> {
            progressDialog.dismiss();
            if (err != null) {
                done.accept(null, err.text);
            } else {
                putKnownPassQR(response.passkey.id);
                done.accept(passkey, null);
            }
        });
        progressDialog.setOnCancelListener(d -> {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
            done.accept(null, "CANCELLED");
        });
    }

    public static void login(Context context, int currentAccount, Passkey passkey, BiConsumer<TLRPC.auth_Authorization, String> done) {
        var handle = new String(fromBase64Url(passkey.userHandle));
        var id = handle.split(":")[1];
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            var userConfig = UserConfig.getInstance(a);
            if (!userConfig.isClientActivated()) {
                continue;
            }
            var userId = userConfig.getClientUserId();
            if (String.valueOf(userId).equals(id)) {
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
        var req = new TL_account.initPasskeyLogin();
        req.api_id = BuildVars.APP_ID;
        req.api_hash = BuildVars.APP_HASH;
        var requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, AndroidUtilities::runOnUIThread, (res, err) -> {
            progressDialog.dismiss();
            if (err != null) {
                done.accept(null, err.text);
                return;
            }
            try {
                var response = signingPasskey(passkey, res.options.data);

                finishLogin(context, currentAccount, passkey, response, done);
            } catch (Exception e) {
                FileLog.e(e);
                done.accept(null, e.getMessage());
            }

        }, ConnectionsManager.RequestFlagWithoutLogin);
        progressDialog.setOnCancelListener(d -> {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
            done.accept(null, "CANCELLED");
        });
    }

    private static void finishLogin(Context context, int currentAccount, Passkey passkey, SigningResponse response, BiConsumer<TLRPC.auth_Authorization, String> done) {
        var req = new TL_account.finishPasskeyLogin();
        req.credential = new TL_account.inputPasskeyCredentialPublicKey();

        req.credential.id = passkey.id;
        req.credential.raw_id = passkey.id;

        var passkeyResponse = new TL_account.inputPasskeyResponseLogin();
        passkeyResponse.client_data = new TLRPC.TL_dataJSON();
        passkeyResponse.client_data.data = response.clientDataJSON;

        passkeyResponse.authenticator_data = response.authenticatorData;
        passkeyResponse.signature = response.signature;
        passkeyResponse.user_handle = new String(fromBase64Url(passkey.userHandle));

        req.credential.response = passkeyResponse;

        var progressDialog = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER);
        progressDialog.showDelayed(500);

        var datacenterId = Integer.parseInt(passkeyResponse.user_handle.split(":")[0]);
        if (datacenterId != ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId()) {
            var from_dc_id = ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId();
            var from_auth_key_id = ConnectionsManager.getInstance(currentAccount).getCurrentAuthKeyId();

            ConnectionsManager.getInstance(currentAccount).setDefaultDatacenterId(datacenterId);

            req.flags |= TLObject.FLAG_0;
            req.from_dc_id = from_dc_id;
            req.from_auth_key_id = from_auth_key_id;
        }

        var requestId = ConnectionsManager.getInstance(currentAccount).sendRequestTyped(req, AndroidUtilities::runOnUIThread, (auth, err) -> {
            progressDialog.dismiss();
            if (err != null) {
                done.accept(null, err.text);
            } else {
                putKnownPassQR(passkey.id);
                done.accept(auth, null);
            }
        }, datacenterId, ConnectionsManager.RequestFlagWithoutLogin | ConnectionsManager.RequestFlagInvokeAfter);

        progressDialog.setOnCancelListener(d -> {
            ConnectionsManager.getInstance(currentAccount).cancelRequest(requestId, true);
            done.accept(null, "CANCELLED");
        });
    }

    private static SigningResponse signingPasskey(Passkey passkey, String requestJson) throws Exception {
        var obj = new JSONObject(requestJson);
        var options = obj.getJSONObject("publicKey");

        var rpId = options.getString("rpId");
        var challenge = options.getString("challenge");
        var authData = WebAuthnCrypto.buildAuthenticatorData(rpId, true, 0, null, null);

        var clientData = new JSONObject();
        clientData.put("type", "webauthn.get");
        clientData.put("challenge", challenge);
        clientData.put("origin", "https://" + rpId);
        clientData.put("androidPackageName", ApplicationLoader.getApplicationId());
        var clientDataBytes = clientData.toString().getBytes(StandardCharsets.UTF_8);

        var signature = WebAuthnCrypto.signAssertion(passkey.privateKey, authData, clientDataBytes);

        return new SigningResponse(authData, signature, clientData.toString());
    }

    public static class Passkey implements Encryptable {
        public String userHandle;
        public String id;
        public byte[] privateKey;
        private boolean needDecryption;

        public Passkey(String userHandle, String id, byte[] privateKey, boolean needDecryption) {
            this.userHandle = userHandle;
            this.id = id;
            this.privateKey = privateKey;
            this.needDecryption = needDecryption;
        }

        @Override
        public String encryptToUrl(byte[] password) throws GeneralSecurityException {
            var builder = new Uri.Builder();
            builder.scheme("tg");
            builder.authority("passkey");
            var epk = QrEncryption.encrypt(privateKey, password);
            builder.appendQueryParameter("pk", toBase64Url(epk));
            builder.appendQueryParameter("h", userHandle);
            builder.appendQueryParameter("cid", id);
            return builder.build().toString();
        }

        @Override
        public void decrypt(byte[] password) throws GeneralSecurityException {
            if (needDecryption) {
                privateKey = QrEncryption.decrypt(privateKey, password);
                needDecryption = false;
            }
        }

        @Override
        public boolean needDecryption() {
            return needDecryption;
        }
    }

    private static class SigningResponse {
        public byte[] authenticatorData;
        public byte[] signature;
        public String clientDataJSON;

        public SigningResponse(byte[] authenticatorData, byte[] signature, String clientDataJSON) {
            this.authenticatorData = authenticatorData;
            this.signature = signature;
            this.clientDataJSON = clientDataJSON;
        }
    }

    private static CreationResponse createPasskey(String requestJson) throws Exception {
        var obj = new JSONObject(requestJson);
        var options = obj.getJSONObject("publicKey");

        var userId = options.getJSONObject("user").getString("id");
        var rpId = options.getJSONObject("rp").getString("id");
        var challenge = options.getString("challenge");

        var keyPair = WebAuthnCrypto.generateP256KeyPair();
        var credentialId = new byte[32];
        Utilities.random.nextBytes(credentialId);

        var clientData = new JSONObject();
        clientData.put("type", "webauthn.create");
        clientData.put("challenge", challenge);
        clientData.put("origin", "https://" + rpId);
        clientData.put("androidPackageName", ApplicationLoader.getApplicationId());

        var authData = WebAuthnCrypto.buildAuthenticatorData(rpId, true, 0, credentialId, (ECPublicKey) keyPair.getPublic());
        var attestationObject = WebAuthnCrypto.buildAttestationObject(authData);

        return new CreationResponse(
                new Passkey(userId, toBase64Url(credentialId), keyPair.getPrivate().getEncoded(), false),
                attestationObject,
                clientData.toString()
        );
    }

    private static class CreationResponse {
        public Passkey passkey;
        public byte[] attestationObject;
        public String clientDataJSON;

        public CreationResponse(Passkey passkey, byte[] attestationObject, String clientDataJSON) {
            this.passkey = passkey;
            this.attestationObject = attestationObject;
            this.clientDataJSON = clientDataJSON;
        }
    }
}
