package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import androidx.annotation.Keep;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
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
    protected String translate(String query, String tl) {
        LingoRequest params = new LingoRequest(query, "auto2" + tl);
        Gson gson = new Gson();
        String response = request(gson.toJson(params));
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        try {
            LingoResponse lingoResponse = gson.fromJson(response, LingoResponse.class);
            if (TextUtils.isEmpty(lingoResponse.target)) {
                FileLog.e(response);
                return null;
            }
            return lingoResponse.target;
        } catch (Exception e) {
            FileLog.e(response + e);
            return null;
        }
    }

    private String request(String param) {
        try {
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
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public static class LingoRequest {

        @SerializedName("source")
        @Expose
        @Keep
        public String source;
        @SerializedName("trans_type")
        @Expose
        @Keep
        String transType;
        @SerializedName("request_id")
        @Expose
        @Keep
        String requestId;
        @SerializedName("detect")
        @Expose
        @Keep
        Boolean detect;

        LingoRequest(String source, String transType) {
            super();
            this.source = source;
            this.transType = transType;
            this.requestId = String.valueOf(System.currentTimeMillis());
            this.detect = true;
        }

    }

    static class LingoResponse {

        @SerializedName("target")
        @Expose
        String target;

    }
}
