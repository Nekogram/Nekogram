package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class SogouTranslator extends BaseTranslator {

    private static SogouTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "ar", "cs", "da", "de", "de", "en", "es",
            "fi", "fr", "hu", "it", "ja", "ko", "nl",
            "pl", "pt", "ru", "sv", "th", "tr", "vi",
            "zh", "zh-CN", "zh-TW");

    static SogouTranslator getInstance() {
        if (instance == null) {
            synchronized (SogouTranslator.class) {
                if (instance == null) {
                    instance = new SogouTranslator();
                }
            }
        }
        return instance;
    }

    @Override
    public String convertLanguageCode(String language, String country) {
        String languageLowerCase = language.toLowerCase();
        String code;
        if (!TextUtils.isEmpty(country)) {
            String countryUpperCase = country.toUpperCase();
            if (targetLanguages.contains(languageLowerCase + "-" + countryUpperCase)) {
                code = languageLowerCase + "-" + countryUpperCase;
            } else if (languageLowerCase.equals("zh")) {
                if (countryUpperCase.equals("DG")) {
                    code = "zh-CN";
                } else if (countryUpperCase.equals("HK")) {
                    code = "zh-TW";
                } else {
                    code = languageLowerCase;
                }
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    private static Result getResult(String string) throws JSONException, IOException {
        JSONObject json = new JSONObject(string);
        if (!json.has("data") && json.has("message")) {
            throw new IOException(json.getString("message"));
        }
        JSONObject translation = json.getJSONObject("data").getJSONObject("translation");
        String sourceLang = null;
        try {
            sourceLang = translation.getString("from");
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new Result(translation.getString("trans_text"), sourceLang);
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException, JSONException, GeneralSecurityException {
        long currentTimeMillis = System.currentTimeMillis();
        var jsonObject = new JSONObject();
        jsonObject.put("query", query);
        jsonObject.put("from", "auto");
        jsonObject.put("to", tl);
        var param = jsonObject.toString();
        return getResult(Http.url("https://fanyi.sogou.com/openapi/external/dictTranslation")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("S-AppId", String.valueOf(Extra.SOGOU_APP_ID))
                .header("S-AppKey", Extra.SOGOU_APP_KEY)
                .header("S-CurTime", String.valueOf(currentTimeMillis))
                .header("S-Sign", Extra.signSogou(param, currentTimeMillis))
                .data("S-Param=" + URLEncoder.encode(param, "UTF-8"))
                .request());
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

}
