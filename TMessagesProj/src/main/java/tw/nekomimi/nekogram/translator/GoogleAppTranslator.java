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

import tw.nekomimi.nekogram.NekoConfig;

public class GoogleAppTranslator extends BaseTranslator {

    private static GoogleAppTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "sq", "ar", "am", "az", "ga", "et", "or", "eu", "be", "bg", "is", "pl", "bs",
            "fa", "af", "tt", "da", "de", "ru", "fr", "tl", "fi", "fy", "km", "ka", "gu",
            "kk", "ht", "ko", "ha", "nl", "ky", "gl", "ca", "cs", "kn", "co", "hr", "ku",
            "la", "lv", "lo", "lt", "lb", "rw", "ro", "mg", "mt", "mr", "ml", "ms", "mk",
            "mi", "mn", "bn", "my", "hmn", "xh", "zu", "ne", "no", "pa", "pt", "ps", "ny",
            "ja", "sv", "sm", "sr", "st", "si", "eo", "sk", "sl", "sw", "gd", "ceb", "so",
            "tg", "te", "ta", "th", "tr", "tk", "cy", "ug", "ur", "uk", "uz", "es", "iw",
            "el", "haw", "sd", "hu", "sn", "hy", "ig", "it", "yi", "hi", "su", "id", "jw",
            "en", "yo", "vi", "zh-TW", "zh-CN", "zh");

    static GoogleAppTranslator getInstance() {
        if (instance == null) {
            synchronized (GoogleAppTranslator.class) {
                if (instance == null) {
                    instance = new GoogleAppTranslator();
                }
            }
        }
        return instance;
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        String url = "https://translate.google." + (NekoConfig.translationProvider == Translator.PROVIDER_GOOGLE_CN ? "cn" : "com") + "/translate_a/single?dj=1" +
                "&q=" + URLEncoder.encode(query, "UTF-8") +
                "&sl=auto" +
                "&tl=" + tl +
                "&ie=UTF-8&oe=UTF-8&client=at&dt=t&otf=2";
        String response = request(url);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        return getResult(response);
    }

    @Override
    protected List<String> getTargetLanguages() {
        return targetLanguages;
    }

    private String getResult(String string) throws JSONException {
        StringBuilder sb = new StringBuilder();
        JSONArray array = new JSONObject(string).getJSONArray("sentences");
        for (int i = 0; i < array.length(); i++) {
            sb.append(array.getJSONObject(i).getString("trans"));
        }
        return sb.toString();
    }

    private String request(String url) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(url);
        URLConnection httpConnection = downloadUrl.openConnection();
        httpConnection.addRequestProperty("User-Agent", "GoogleTranslate/6.14.0.04.343003216 (Linux; U; Android 10; Redmi K20 Pro)");
        httpConnection.setConnectTimeout(1000);
        //httpConnection.setReadTimeout(2000);
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
        String result = outbuf.toString();
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }
}
