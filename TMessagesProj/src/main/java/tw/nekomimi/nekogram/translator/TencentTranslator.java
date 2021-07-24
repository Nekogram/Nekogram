package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.Utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class TencentTranslator extends BaseTranslator {

    private static TencentTranslator instance;
    private final List<String> targetLanguages = Arrays.asList("zh", "en", "jp", "kr", "fr", "es", "it", "de", "tr", "ru", "pt", "vi", "id", "th", "ms");
    // var languagePair = {
    //             "auto": ["zh", "en", "jp", "kr", "fr", "es", "it", "de", "tr", "ru", "pt", "vi", "id", "th", "ms"],
    //             "en": ["zh", "fr", "es", "it", "de", "tr", "ru", "pt", "vi", "id", "th", "ms", "ar", "hi"],
    //             "zh": ["en", "jp", "kr", "fr", "es", "it", "de", "tr", "ru", "pt", "vi", "id", "th", "ms"],
    //             "fr": ["zh", "en", "es", "it", "de", "tr", "ru", "pt"],
    //             "es": ["zh", "en", "fr", "it", "de", "tr", "ru", "pt"],
    //             "it": ["zh", "en", "fr", "es", "de", "tr", "ru", "pt"],
    //             "de": ["zh", "en", "fr", "es", "it", "tr", "ru", "pt"],
    //             "tr": ["zh", "en", "fr", "es", "it", "de", "ru", "pt"],
    //             "ru": ["zh", "en", "fr", "es", "it", "de", "tr", "pt"],
    //             "pt": ["zh", "en", "fr", "es", "it", "de", "tr", "ru"],
    //             "vi": ["zh", "en"],
    //             "id": ["zh", "en"],
    //             "ms": ["zh", "en"],
    //             "th": ["zh", "en"],
    //             "jp": ["zh"],
    //             "kr": ["zh"],
    //             "ar": ["en"],
    //             "hi": ["en"]
    //         };
    // 腾讯翻译君并不支持所有语言的互翻译
    private final String guid = generateGuid();

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

    private String generateGuid() {
        byte[] r = new byte[20];
        Utilities.random.nextBytes(r);
        return Base64.encodeToString(r, Base64.URL_SAFE);
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        String response = request("https://wxapp.translator.qq.com/api/translate?" +
                "&sourceText=" + URLEncoder.encode(query, "UTF-8") +
                "&source=auto" +
                "&target=" + tl +
                "&platform=WeChat_APP&candidateLangs=auto&guid=" + guid);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(response);
        if (!jsonObject.has("targetText") && jsonObject.has("errMsg")) {
            throw new IOException(jsonObject.getString("errMsg"));
        }
        return jsonObject.getString("targetText");
    }

    private String request(String url) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(url);
        URLConnection httpConnection = downloadUrl.openConnection();
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; Redmi K20 Pro Build/RQ3A.210705.001; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/78.0.3904.62 XWEB/2853 MMWEBSDK/20210601 Mobile Safari/537.36 MMWEBID/9726 MicroMessenger/8.0.7.1920(0x28000737) Process/appbrand0 WeChat/arm64 Weixin NetType/WIFI Language/en_US ABI/arm64 MiniProgramEnv/android");
        httpConnection.addRequestProperty("Content-Type", "application/json");
        httpConnection.addRequestProperty("Referer", "https://servicewechat.com/wxb1070eabc6f9107e/117/page-frame.html");
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
