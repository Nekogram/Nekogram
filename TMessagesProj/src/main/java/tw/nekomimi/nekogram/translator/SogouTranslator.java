package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class SogouTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "ar", "cs", "da", "de", "de", "en", "es",
            "fi", "fr", "hu", "it", "ja", "ko", "nl",
            "pl", "pt", "ru", "sv", "th", "tr", "vi",
            "zh");

    private static final class InstanceHolder {
        private static final SogouTranslator instance = new SogouTranslator();
    }

    static SogouTranslator getInstance() {
        return InstanceHolder.instance;
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
                code = "zh-CHS";
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    @Override
    public String convertLanguageCode(String code, boolean reverse) {
        if (reverse) {
            if (code.equals("zh-CHS")) return "zh";
        } else {
            if (code.equals("zh")) return "zh-CHS";
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
                .header("S-AppId", String.valueOf(Extra.SOGOU_APP_ID))
                .header("S-AppKey", Extra.SOGOU_APP_KEY)
                .header("S-CurTime", String.valueOf(currentTimeMillis))
                .header("S-Sign", Extra.signSogou(param, currentTimeMillis))
                .data("S-Param=" + URLEncode(param))
                .request());
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

}
