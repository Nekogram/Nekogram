package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

public class MicrosoftTranslator extends BaseTranslator {

    private static MicrosoftTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "ar", "as", "bn", "bs", "bg", "yue", "ca", "zh", "zh-Hans", "zh-Hant",
            "hr", "cs", "da", "prs", "nl", "en", "et", "fj", "fil", "fi",
            "fr", "de", "el", "gu", "ht", "he", "hi", "mww", "hu", "is",
            "id", "ga", "it", "ja", "kn", "kk", "tlh", "ko", "ku", "kmr",
            "lv", "lt", "mg", "ms", "ml", "mt", "mi", "mr", "nb", "or", "ps",
            "fa", "pl", "pt", "pa", "otq", "ro", "ru", "sm", "sr", "sk", "sl",
            "es", "sw", "sv", "ty", "ta", "te", "th", "to", "tr", "uk", "ur",
            "vi", "cy", "yua");

    static MicrosoftTranslator getInstance() {
        if (instance == null) {
            synchronized (MicrosoftTranslator.class) {
                if (instance == null) {
                    instance = new MicrosoftTranslator();
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
        String param = "fromLang=auto-detect&text=" + URLEncoder.encode(query, "UTF-8") +
                "&to=" + tl;
        String response = request(param);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject = new JSONArray(response).getJSONObject(0);
        if (!jsonObject.has("translations")) {
            throw new IOException(response);
        }
        JSONArray array = jsonObject.getJSONArray("translations");
        return array.getJSONObject(0).getString("text");
    }

    private String request(String param) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL("https://cn.bing.com/ttranslatev3");
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
        httpConnection.setConnectTimeout(1000);
        //httpConnection.setReadTimeout(2000);
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true);
        DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
        byte[] t = param.getBytes(Charset.defaultCharset());
        dataOutputStream.write(t);
        dataOutputStream.flush();
        dataOutputStream.close();
        httpConnection.connect();
        if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            httpConnectionStream = httpConnection.getErrorStream();
        } else {
            httpConnectionStream = httpConnection.getInputStream();
        }
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
