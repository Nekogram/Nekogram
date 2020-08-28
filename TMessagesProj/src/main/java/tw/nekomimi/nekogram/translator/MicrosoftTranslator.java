package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class MicrosoftTranslator extends Translator {

    private static MicrosoftTranslator instance;
    private List<String> targetLanguages = Arrays.asList(
            "af", "ar", "bg", "bn", "bs", "ca", "cs", "cy", "da", "de", "el", "en", "es",
            "et", "fa", "fi", "fil", "fj", "fr", "ga", "gu", "he", "hi", "hr", "ht", "hu",
            "id", "is", "it", "ja", "kk", "kmr", "kn", "ko", "ku", "lt", "lv", "mg", "mi",
            "ml", "mr", "ms", "mt", "mww", "nb", "nl", "or", "otq", "pa", "pl", "prs", "ps",
            "pt", "pt-pt", "ro", "ru", "sk", "sl", "sm", "sr-Cyrl", "sr-Latn", "sv", "sw", "ta",
            "te", "th", "tlh", "tlh-Latn", "tlh-Piqd", "to", "tr", "ty", "uk", "ur", "vi", "yua", "yue", "zh-Hans", "zh-Hant", "zh");

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

    private String translateImpl(String query, String tl) throws IOException, JSONException {
        long t = System.currentTimeMillis();
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("to", tl);
        jsonObject.put("query", query);
        jsonObject.put("t", t);
        jsonObject.put("k", Hash(tl, query, String.valueOf(t)));
        String response = request(jsonObject.toString());
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        jsonObject = new JSONObject(response);
        if (!response.contains("target") && response.contains("error")) {
            throw new IOException(jsonObject.getString("error"));
        }
        return jsonObject.getString("target");
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        return translateImpl(query, tl);
    }

    @Override
    protected List<String> getTargetLanguages() {
        return targetLanguages;
    }

    private native String Hash(String to, String query, String t);


    private String request(String param) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL("https://cognitive.rectifier.tech/api/Translate");
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
        httpConnection.setConnectTimeout(5000);
        httpConnection.setReadTimeout(5000);
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
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }
}
