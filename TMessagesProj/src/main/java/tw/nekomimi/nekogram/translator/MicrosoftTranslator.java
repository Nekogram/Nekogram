package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

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
            "sq", "ar", "az", "ga", "et", "or", "mww", "bg", "is", "pl", "bs", "fa", "ko",
            "da", "de", "ru", "fr", "zh-TW", "fil", "fj", "fi", "gu", "kk", "ht", "nl",
            "ca", "zh-CN", "cs", "kn", "otq", "hr", "lv", "lt", "ro", "mg", "mt", "mr",
            "ml", "ms", "mi", "bn", "af", "ne", "nb", "pa", "pt", "pt-PT", "ja", "sv", "sm",
            "sr-Latn", "sr-Cyrl", "sk", "sl", "sw", "ty", "te", "ta", "th", "to", "tr", "cy",
            "ur", "uk", "es", "he", "el", "hu", "hy", "it", "hi", "id", "en", "yua", "yue",
            "vi", "am", "as", "prs", "fr-CA", "iu", "km", "tlh-Latn", "ku", "kmr", "lo", "my", "ps", "ti");
    private boolean useCN = false;

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
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    @Override
    public String convertLanguageCode(String language, String country) {
        String code;
        if (country != null && language.equals("zh")) {
            String countryUpperCase = country.toUpperCase();
            if (countryUpperCase.equals("CN") || countryUpperCase.equals("DG")) {
                code = "zh-CN";
            } else if (countryUpperCase.equals("TW") || countryUpperCase.equals("HK")) {
                code = "zh-TW";
            } else {
                code = language;
            }
        } else {
            code = language;
        }
        return code;
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        if (tl.equals("zh-CN")) {
            tl = "zh-Hans";
        } else if (tl.equals("zh-TW")) {
            tl = "zh-hant";
        }
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
        URL downloadUrl = new URL("https://" + (useCN ? "cn" : "www") + ".bing.com/ttranslatev3");
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
        httpConnection.setConnectTimeout(1000);
        //httpConnection.setReadTimeout(2000);
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true);
        httpConnection.setInstanceFollowRedirects(false);
        DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
        byte[] t = param.getBytes(Charset.defaultCharset());
        dataOutputStream.write(t);
        dataOutputStream.flush();
        dataOutputStream.close();
        httpConnection.connect();
        if (httpConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            if (httpConnection.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP) {
                useCN = !useCN;
                FileLog.e("Move to " + (useCN ? "cn" : "www"));
                return request(param);
            }
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
        String result = outbuf.toString();
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }
}
