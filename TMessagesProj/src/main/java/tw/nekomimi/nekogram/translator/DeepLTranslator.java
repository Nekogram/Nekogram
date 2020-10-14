package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class DeepLTranslator extends BaseTranslator {

    private static DeepLTranslator instance;
    private List<String> targetLanguages = Arrays.asList("DE", "EN", "ES", "FR", "IT", "JA", "NL", "PL", "PT", "RU", "ZH");

    static DeepLTranslator getInstance() {
        if (instance == null) {
            synchronized (DeepLTranslator.class) {
                if (instance == null) {
                    instance = new DeepLTranslator();
                }
            }
        }
        return instance;
    }

    @Override
    protected List<String> getTargetLanguages() {
        return targetLanguages;
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        String url = "https://api.deepl.com/v2/translate?auth_key=73221642-fa70-fc56-aa9f-234c5139c173&text=" + URLEncoder.encode(query, "UTF-8") + "&target_lang=" + tl;
        String response = request(url);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(response);
        if (!response.contains("translations") && response.contains("message")) {
            throw new IOException(jsonObject.getString("message"));
        }
        JSONArray translations = jsonObject.getJSONArray("translations");
        return translations.getJSONObject(0).getString("text");
    }

    private String request(String url) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(url);
        URLConnection httpConnection = downloadUrl.openConnection();
        httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
        httpConnection.setConnectTimeout(1000);
        httpConnection.setReadTimeout(2000);
        httpConnection.setDoOutput(true);
        httpConnection.connect();
        httpConnectionStream = httpConnection.getInputStream();
        outbuf = new ByteArrayOutputStream();

        byte[] data = new byte[1024 * 32];
        while (true) {
            int read = httpConnectionStream.read(data);
            if (read > 0) {
                outbuf.write(data, 0, read);
            } else if (read == -1) {
                break;
            } else {
                break;
            }
        }
        String result = new String(outbuf.toByteArray());
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }
}
