package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

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
    public String convertLanguageCode(String language, String country) {
        String languageLowerCase = language.toLowerCase();
        String code;
        if (!TextUtils.isEmpty(country)) {
            String countryUpperCase = country.toUpperCase();
            if (targetLanguages.contains(languageLowerCase + "-" + countryUpperCase)) {
                code = languageLowerCase + "-" + countryUpperCase;
            } else if (languageLowerCase.equals("zh")) {
                if (countryUpperCase.equals("HK")) {
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

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException, JSONException {
        String response = Http.url(
                "https://app.translator.qq.com/api/translate?sourceText=" + URLEncoder.encode(query, "UTF-8") + "&source=" + fl + "&target=" + tl)
                .request();
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        var jsonObject = new JSONObject(response);
        if (!jsonObject.has("targetText") && jsonObject.has("errMsg")) {
            throw new IOException(jsonObject.getString("errMsg"));
        }
        return new Result(jsonObject.getString("targetText"), null);
    }
}
