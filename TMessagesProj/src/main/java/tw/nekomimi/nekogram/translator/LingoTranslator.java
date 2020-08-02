package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class LingoTranslator extends Translator {

    private static LingoTranslator instance;
    private List<String> targetLanguages = Arrays.asList("zh", "en", "es", "fr", "ja", "ru");

    static LingoTranslator getInstance() {
        if (instance == null) {
            synchronized (LingoTranslator.class) {
                if (instance == null) {
                    instance = new LingoTranslator();
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
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("source", query);
        jsonObject.put("trans_type", "auto2" + tl);
        jsonObject.put("request_id", String.valueOf(System.currentTimeMillis()));
        jsonObject.put("detect", true);
        FileLog.e(jsonObject.toString());
        String response = request(jsonObject.toString());
        FileLog.e(response);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        jsonObject = new JSONObject(response);
        if (!response.contains("target") && response.contains("error")) {
            throw new IOException(jsonObject.getString("error"));
        }
        return new JSONObject(response).getString("target");
    }

    private String request(String param) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL("https://api.interpreter.caiyunai.com/v1/translator");
        HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
        httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        httpConnection.addRequestProperty("X-Authorization", "token 9sdftiq37bnv410eon2l");//白嫖
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
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }
}
