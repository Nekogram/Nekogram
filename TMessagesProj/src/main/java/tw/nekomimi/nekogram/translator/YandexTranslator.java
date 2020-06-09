package tw.nekomimi.nekogram.translator;

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
import java.util.Arrays;
import java.util.List;

public class YandexTranslator extends Translator {

    private static YandexTranslator instance;
    private List<String> targetLanguages = Arrays.asList(
            "en", "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "ceb", "cs", "cy",
            "da", "de", "el", "eo", "es", "et", "eu", "fa", "fi", "fr", "ga", "gd", "gl", "gu",
            "he", "hi", "hr", "ht", "hu", "hy", "id", "is", "it", "ja", "jv", "ka", "kk", "km",
            "kn", "ko", "ky", "la", "lb", "lo", "lt", "lv", "mg", "mi", "mk", "ml", "mn", "mr",
            "ms", "mt", "my", "ne", "nl", "no", "pa", "pl", "pt", "ro", "ru", "si", "sk", "sl",
            "sq", "sr", "su", "sv", "sw", "ta", "te", "tg", "th", "tl", "tr", "tt", "uk", "ur",
            "uz", "vi", "xh", "yi", "zh");

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

    private static String getResult(String string) throws JSONException {
        JSONObject json = new JSONObject(string);
        int code = json.getInt("code");
        if (code != 200) {
            return null;
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
        String result = translateImpl(query, tl);
        if (result == null) {
            return translateImpl(query, tl);
        }
        return result;
    }

    @Override
    protected List<String> getTargetLanguages() {
        return targetLanguages;
    }

    private String translateImpl(String query, String tl) throws IOException, JSONException {
        String url = "https://translate.yandex.net/api/v1.5/tr.json/translate"
                + "?key=trnsl.1.1.20160205T121943Z.0208eaff12c2747d.9526187390798b3098ec23e8f02073168e0b52c1"
                + "&lang=" + tl;
        return getResult(request(url, "text=" + URLEncoder.encode(query, "UTF-8")));

    }

    private String request(String url, String param) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(url);
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded charset=UTF-8");
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
        httpConnection.setConnectTimeout(1000);
        httpConnection.setReadTimeout(2000);
        httpConnection.setRequestMethod("POST");
        httpConnection.setDoOutput(true);
        DataOutputStream dataOutputStream = new DataOutputStream(httpConnection.getOutputStream());
        //noinspection CharsetObjectCanBeUsed
        byte[] t = param.getBytes("UTF-8");
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
        try {
            httpConnectionStream.close();
        } catch (Throwable e) {
            FileLog.e(e);
        }
        try {
            outbuf.close();
        } catch (Exception ignore) {

        }
        return result;

    }
}
