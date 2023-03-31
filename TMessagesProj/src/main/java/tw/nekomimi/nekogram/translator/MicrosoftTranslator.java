package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class MicrosoftTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "sq", "ar", "az", "ga", "et", "or", "mww", "bg", "is", "pl", "bs", "fa", "ko",
            "da", "de", "ru", "fr", "zh-TW", "fil", "fj", "fi", "gu", "kk", "ht", "nl",
            "ca", "zh-CN", "cs", "kn", "otq", "hr", "lv", "lt", "ro", "mg", "mt", "mr",
            "ml", "ms", "mi", "bn", "af", "ne", "nb", "pa", "pt", "pt-PT", "ja", "sv", "sm",
            "sr-Latn", "sr-Cyrl", "sk", "sl", "sw", "ty", "te", "ta", "th", "to", "tr", "cy",
            "ur", "uk", "es", "he", "el", "hu", "hy", "it", "hi", "id", "en", "yua", "yue",
            "vi", "am", "as", "prs", "fr-CA", "iu", "km", "tlh-Latn", "ku", "kmr", "lo", "my", "ps", "ti");

    private static final class InstanceHolder {
        private static final MicrosoftTranslator instance = new MicrosoftTranslator();
    }

    static MicrosoftTranslator getInstance() {
        return InstanceHolder.instance;
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

    private static Result getResult(String response) throws JSONException, IOException {
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject;
        try {
            jsonObject = new JSONArray(response).getJSONObject(0);
        } catch (JSONException e) {
            jsonObject = new JSONObject(response);
        }
        if (!jsonObject.has("translations") && jsonObject.has("message")) {
            throw new IOException(jsonObject.getString("message"));
        }
        JSONArray array = jsonObject.getJSONArray("translations");
        String sourceLang = null;
        try {
            sourceLang = jsonObject.getJSONObject("detectedLanguage").getString("language");
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new Result(array.getJSONObject(0).getString("text"), sourceLang);
    }

    @Override
    public String convertLanguageCode(String code, boolean reverse) {
        if (reverse) {
            if (code.equals("zh-Hans")) {
                return "zh-CN";
            } else if (code.equals("zh-Hant")) {
                return "zh-TW";
            }
        } else {
            if (code.equals("zh-CN")) {
                return "zh-Hans";
            } else if (code.equals("zh-TW")) {
                return "zh-Hant";
            }
        }
        return code;
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("Text", query);
        String url = "api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=&to=" + tl;
        return getResult(Http.url("https://" + url)
                .header("X-Mt-Signature", Extra.signMicrosoft(url))
                .header("User-Agent", "okhttp/4.5.0")
                .data(new JSONArray().put(new JSONObject().put("Text", query)).toString(), "application/json; charset=UTF-8")
                .request());
    }
}
