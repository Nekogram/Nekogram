package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tw.nekomimi.nekogram.Extra;

public class TencentTranslator extends BaseTranslator {

    private static TencentTranslator instance;
    private final List<String> targetLanguages = Arrays.asList("zh", "zh-TW", "en", "ja", "ko", "fr", "es", "it", "de", "tr", "ru", "pt", "vi", "id", "th", "ms", "ar", "hi");

    static TencentTranslator getInstance() {
        if (instance == null) {
            synchronized (TencentTranslator.class) {
                if (instance == null) {
                    instance = new TencentTranslator();
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
    protected String translate(String query, String tl) throws IOException, JSONException {
        String response = TmtClient.translate(query, tl);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(response).getJSONObject("Response");
        if (!jsonObject.has("TargetText") && jsonObject.has("Error")) {
            throw new IOException(jsonObject.getJSONObject("Error").getString("Message"));
        }
        return jsonObject.getString("TargetText");
    }

    private static class TmtClient {

        private static String sign(String param) {
            try {
                SecretKeySpec secretKeySpec = new SecretKeySpec(Extra.getString("tencent.SecretKey").getBytes(), "HmacSHA1");
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(secretKeySpec);
                return Base64.encodeToString(mac.doFinal(param.getBytes()), Base64.NO_WRAP);
            } catch (NullPointerException | InvalidKeyException | NoSuchAlgorithmException e2) {
                return null;
            }
        }

        private static String buildQuery(HashMap<String, String> hashMap, boolean encode) throws UnsupportedEncodingException {
            ArrayList<String> arrayList = new ArrayList<>(hashMap.keySet());
            Collections.sort(arrayList);
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < arrayList.size(); i++) {
                stringBuilder.append(arrayList.get(i));
                stringBuilder.append("=");
                String value = hashMap.get(arrayList.get(i));
                stringBuilder.append(encode ? value : URLEncoder.encode(value, "UTF-8"));
                if (i < arrayList.size() - 1) {
                    stringBuilder.append("&");
                }
            }
            return stringBuilder.toString();
        }

        public static String translate(String query, String tl) throws IOException {
            HashMap<String, String> hashMap = new HashMap<>();
            hashMap.put("Action", "TextTranslate");
            hashMap.put("Region", "ap-guangzhou");
            hashMap.put("Timestamp", String.valueOf(System.currentTimeMillis() / 1000));
            hashMap.put("Nonce", String.valueOf(new Random().nextInt(Integer.MAX_VALUE)));
            hashMap.put("SecretId", Extra.getString("tencent.SecretId"));
            hashMap.put("Version", "2018-03-21");
            hashMap.put("SourceText", query);
            hashMap.put("Source", "auto");
            hashMap.put("Target", tl);
            hashMap.put("ProjectId", "0");
            hashMap.put("Signature", sign("POSTtmt.tencentcloudapi.com/?" + buildQuery(hashMap, true)));
            return request(buildQuery(hashMap, false));
        }

        private static String request(String param) throws IOException {
            ByteArrayOutputStream outbuf;
            InputStream httpConnectionStream;
            URL downloadUrl = new URL("https://tmt.tencentcloudapi.com");
            HttpURLConnection httpConnection = (HttpURLConnection) downloadUrl.openConnection();
            httpConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Linux; U; Android 11; zh-cn; Redmi K20 Pro Build/RQ3A.210705.001) AppleWebKit/533.1 (KHTML, like Gecko) Mobile Safari/533.1");
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
}
