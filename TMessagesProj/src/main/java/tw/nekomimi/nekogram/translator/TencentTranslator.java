package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
    protected Result translate(String query, String tl) throws IOException, JSONException {
        String response = TmtClient.translate(query, tl);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(response).getJSONObject("Response");
        if (!jsonObject.has("TargetText") && jsonObject.has("Error")) {
            throw new IOException(jsonObject.getJSONObject("Error").getString("Message"));
        }
        String sourceLang = null;
        try {
            sourceLang = jsonObject.getString("Source");
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new Result(jsonObject.getString("TargetText"), sourceLang);
    }

    private static class TmtClient {

        private static String sign(String param) {
            try {
                SecretKeySpec secretKeySpec = new SecretKeySpec(Extra.TENCENT_SECRET_KEY.getBytes(), "HmacSHA1");
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
            hashMap.put("SecretId", Extra.TENCENT_SECRET_ID);
            hashMap.put("Version", "2018-03-21");
            hashMap.put("SourceText", query);
            hashMap.put("Source", "auto");
            hashMap.put("Target", tl);
            hashMap.put("ProjectId", "0");
            hashMap.put("Signature", sign("POSTtmt.tencentcloudapi.com/?" + buildQuery(hashMap, true)));
            return Http.url("https://tmt.tencentcloudapi.com")
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("User-Agent", "Mozilla/5.0 (Linux; U; Android 11; zh-cn; Redmi K20 Pro Build/RQ3A.210705.001) AppleWebKit/533.1 (KHTML, like Gecko) Mobile Safari/533.1")
                    .data(buildQuery(hashMap, false))
                    .request();
        }
    }
}
