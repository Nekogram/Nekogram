package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

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

    private static Result getResult(String string) throws IOException {
        if (TextUtils.isEmpty(string)) {
            return null;
        }
        Response[] responseArray;
        try {
            responseArray = GSON.fromJson(string, Response[].class);
        } catch (JsonSyntaxException e) {
            var responseError = GSON.fromJson(string, ResponseError.class);
            if (responseError != null && responseError.message != null) {
                throw new IOException(responseError.message);
            }
            return null;
        }
        Response response = responseArray[0];
        String sourceLang;
        if (response.detectedLanguage != null) {
            sourceLang = response.detectedLanguage.language;
        } else {
            sourceLang = null;
        }
        return new Result(response.translations.get(0).text, sourceLang);
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
    protected Result translate(String query, String fl, String tl) throws IOException {
        String url = "api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=&to=" + tl;
        return getResult(Http.url("https://" + url)
                .header("X-Mt-Signature", Extra.signMicrosoft(url))
                .header("User-Agent", "okhttp/4.5.0")
                .data(GSON.toJson(new Request[]{new Request(query)}), "application/json; charset=UTF-8")
                .request());
    }

    public static class Request {
        @SerializedName("Text")
        @Expose
        public String text;

        public Request(String text) {
            this.text = text;
        }
    }

    public static class DetectedLanguage {
        @SerializedName("language")
        @Expose
        public String language;
    }

    public static class Response {
        @SerializedName("detectedLanguage")
        @Expose
        public DetectedLanguage detectedLanguage;
        @SerializedName("translations")
        @Expose
        public List<Translation> translations;
    }

    public static class ResponseError {
        @SerializedName("message")
        @Expose
        public String message;
    }

    public static class Translation {
        @SerializedName("text")
        @Expose
        public String text;
    }
}
