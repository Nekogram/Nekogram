package tw.nekomimi.nekogram.translator.deepl;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import app.nekogram.translator.DeepLTranslator;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import tw.nekomimi.nekogram.translator.Translator;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public class DeepLOAuth {

    private static final String AUTH_URL = "https://auth.deepl.com/authorize";
    private static final String TOKEN_URL = "https://auth.deepl.com/token";
    private static final String SCOPES = "email idp offline_access openid profile service_level organization";
    private static final String CLIENT_ID = "\u0063\u0068\u0072\u006f\u006d\u0065\u0045\u0078\u0074\u0065\u006e\u0073\u0069\u006f\u006e";
    private static final String EXT_ID = "\u0063\u006f\u0066\u0064\u0062\u0070\u006f\u0065\u0067\u0065\u006d\u0070\u006a\u006c\u006f\u006f\u0067\u0062\u0061\u0067\u006b\u006e\u0063\u0065\u006b\u0069\u006e\u0066\u006c\u0063\u006e\u006a";
    private static final String REDIRECT_URL = "https://" + EXT_ID + ".chromiumapp.org/";

    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekodeepl", Context.MODE_PRIVATE);
    private static final OkHttpClient okHttpClient;
    private static final Gson gson = new Gson();

    static {
        var builder = new OkHttpClient.Builder();
        builder.connectTimeout(120, TimeUnit.SECONDS);
        builder.readTimeout(120, TimeUnit.SECONDS);
        builder.writeTimeout(120, TimeUnit.SECONDS);
        builder.addInterceptor(chain -> {
            var original = chain.request();
            var newBuilder = original.newBuilder();
            newBuilder.header("accept", "*/*");
            newBuilder.header("accept-language", "en-US,en;q=0.9");
            newBuilder.header("content-type", "application/x-www-form-urlencoded");
            newBuilder.header("origin", "chrome-extension://" + EXT_ID);
            newBuilder.header("priority", "u=1, i");
            newBuilder.header("sec-fetch-dest", "empty");
            newBuilder.header("sec-fetch-mode", "cors");
            newBuilder.header("sec-fetch-site", "none");
            newBuilder.header("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36");
            return chain.proceed(newBuilder.build());
        });
        okHttpClient = builder.build();
    }

    private static String generateNonce() {
        var bytes = new byte[28];
        Utilities.fastRandom.nextBytes(bytes);
        var sb = new StringBuilder(56);
        for (var b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String generateCodeChallenge(String codeVerifier) {
        var hash = Utilities.computeSHA256(codeVerifier.getBytes());
        return Base64.encodeToString(hash, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
    }

    private static AuthUrl buildAuthUrl() {
        var nonce = generateNonce();
        var codeVerifier = generateNonce();
        var codeChallenge = generateCodeChallenge(codeVerifier);

        var uriBuilder = Uri.parse(AUTH_URL).buildUpon()
                .appendQueryParameter("client_id", CLIENT_ID)
                .appendQueryParameter("response_type", "code")
                .appendQueryParameter("redirect_uri", REDIRECT_URL)
                .appendQueryParameter("scope", SCOPES)
                .appendQueryParameter("code_challenge", codeChallenge)
                .appendQueryParameter("code_challenge_method", "S256")
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("prompt", "select_account");

        return new AuthUrl(uriBuilder.build().toString(), codeVerifier);
    }

    public static boolean isRedirectUrl(String url) {
        return url.startsWith(DeepLOAuth.REDIRECT_URL);
    }

    private static void startOAuthActivity(BaseFragment fragment, AuthUrl authUrl, Consumer<String> callback) {
        var authFragment = new DeepLOAuthWebviewActivity(authUrl.url, url -> {
            var uri = Uri.parse(url);
            var code = uri.getQueryParameter("code");
            callback.accept(code);
        });
        fragment.presentFragment(authFragment);
    }

    private static void requestToken(AuthUrl auth, String code) throws Exception {
        var params = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("code_verifier", auth.codeVerifier)
                .add("grant_type", "authorization_code")
                .add("redirect_uri", REDIRECT_URL)
                .add("code", code)
                .build();
        var request = new Request.Builder()
                .url(TOKEN_URL)
                .post(params)
                .build();
        try (var response = okHttpClient.newCall(request).execute()) {
            var body = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException(body);
            }
            var token = gson.fromJson(body, Token.class);
            saveToken(token);
        }
    }

    private static void saveToken(Token token) {
        var accessToken = token.accessToken;
        var expiresAt = System.currentTimeMillis() / 1000 + token.expiresIn - 300;
        var refreshToken = token.refreshToken;
        var idToken = token.idToken;
        preferences.edit()
                .putString("access_token", accessToken)
                .putLong("expires_at", expiresAt)
                .putString("refresh_token", refreshToken)
                .putString("id_token", idToken)
                .apply();
    }

    public static void startOAuth(BaseFragment fragment, Runnable callback) {
        var authUrl = buildAuthUrl();
        startOAuthActivity(fragment, authUrl, code -> {
            if (code == null) {
                BulletinFactory.of(fragment).showForError("EMPTY_CODE");
            } else {
                var progressDialog = new AlertDialog(fragment.getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
                progressDialog.showDelayed(150);
                Translator.getExecutorService().execute(() -> {
                    try {
                        requestToken(authUrl, code);
                    } catch (Exception e) {
                        FileLog.e("DeepLOAuth failed to request token", e);
                        AndroidUtilities.runOnUIThread(() -> AlertsCreator.showSimpleAlert(fragment, LocaleController.getString(R.string.ErrorOccurred), e.getLocalizedMessage()));
                    }
                    AndroidUtilities.runOnUIThread(() -> {
                        progressDialog.dismiss();
                        callback.run();
                    });
                });
            }
        });
    }

    private static void refreshToken(String refreshToken) throws Exception {
        var params = new FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .build();
        var request = new Request.Builder()
                .url(TOKEN_URL)
                .post(params)
                .build();
        try (var response = okHttpClient.newCall(request).execute()) {
            var body = response.body().string();
            if (!response.isSuccessful()) {
                throw new IOException(body);
            }
            var token = gson.fromJson(body, Token.class);
            saveToken(token);
        }
    }

    private static String getRefreshToken() {
        return preferences.getString("refresh_token", null);
    }

    private static long getExpiresAt() {
        return preferences.getLong("expires_at", 0);
    }

    private static String getAccessToken() {
        return preferences.getString("access_token", null);
    }

    private static String getIdToken() {
        return preferences.getString("id_token", null);
    }

    public static void configureAccessToken() throws Exception {
        var expiresAt = getExpiresAt();
        if (expiresAt < System.currentTimeMillis() / 1000) {
            var refreshToken = getRefreshToken();
            if (refreshToken == null) {
                return;
            }
            refreshToken(refreshToken);
        }
        DeepLTranslator.setJwt(getAccessToken());
    }

    public static IdToken getIdInfo() {
        var idToken = getIdToken();
        if (idToken == null) return null;
        var parts = idToken.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        try {
            var payload = Base64.decode(parts[1], Base64.URL_SAFE);
            return gson.fromJson(new String(payload, StandardCharsets.UTF_8), IdToken.class);
        } catch (Exception e) {
            FileLog.e("DeepLOAuth failed to parse ID Token", e);
            return null;
        }
    }

    public static void clearToken() {
        preferences.edit()
                .remove("access_token")
                .remove("expires_at")
                .remove("refresh_token")
                .remove("id_token")
                .apply();
        DeepLTranslator.setJwt(null);
    }

    private record AuthUrl(String url, String codeVerifier) {
    }

    private static class Token {
        @SerializedName("access_token")
        public String accessToken;
        @SerializedName("expires_in")
        public int expiresIn;
        @SerializedName("id_token")
        public String idToken;
        @SerializedName("refresh_token")
        public String refreshToken;
    }

    public static class IdToken {
        @SerializedName("email")
        public String email;
    }
}
