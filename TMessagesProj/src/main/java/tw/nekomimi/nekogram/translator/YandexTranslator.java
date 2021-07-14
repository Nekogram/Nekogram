package tw.nekomimi.nekogram.translator;

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
import java.util.UUID;

public class YandexTranslator extends BaseTranslator {

    private static YandexTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "en", "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "ceb", "cs", "cy",
            "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "ga", "gd", "gl", "gu",
            "he", "hi", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "jv", "ka", "kk", "km",
            "kn", "ko", "ky", "la", "lb", "lo", "lt", "lv", "mg", "mi", "mk", "ml", "mn", "mr",
            "ms", "mt", "my", "ne", "nl", "no", "pa", "pl", "pt", "ro", "ru", "si", "sk", "sl",
            "sq", "sr", "su", "sv", "sw", "ta", "te", "tg", "th", "tl", "tr", "tt", "uk", "ur",
            "uz", "vi", "xh", "yi", "zh");
    private final String uuid = UUID.randomUUID().toString().replace("-", "");

    static YandexTranslator getInstance() {
        if (instance == null) {
            synchronized (YandexTranslator.class) {
                if (instance == null) {
                    instance = new YandexTranslator();
                }
            }
        }
        return instance;
    }

    private static String getResult(String string) throws JSONException, IOException {
        JSONObject json = new JSONObject(string);
        int code = json.getInt("code");
        if (code != 200) {
            throw new IOException(json.getString("message"));
        }
        JSONArray array = json.getJSONArray("text");
        StringBuilder sb = new StringBuilder();
        int length = array.length();
        for (int i = 0; i < length; i++) {
            sb.append(array.getString(i));
        }
        return sb.toString();
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        String url = "https://translate.yandex.net/api/v1/tr.json/translate?srv=android&lang=" + tl + "&uuid=" + uuid;
        return getResult(request(url, "text=" + URLEncoder.encode(query, "UTF-8")));
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    private String request(String url, String param) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(url);
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        httpConnection.addRequestProperty("User-Agent", "ru.yandex.translate/21.4.2.21291931 (Xiaomi Redmi K20 Pro; Android 11)");
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
        String result = outbuf.toString();
        httpConnectionStream.close();
        outbuf.close();
        return result;

    }
}
