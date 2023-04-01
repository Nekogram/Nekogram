package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GoogleAppTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "sq", "ar", "am", "az", "ga", "et", "or", "eu", "be", "bg", "is", "pl", "bs",
            "fa", "af", "tt", "da", "de", "ru", "fr", "tl", "fi", "fy", "km", "ka", "gu",
            "kk", "ht", "ko", "ha", "nl", "ky", "gl", "ca", "cs", "kn", "co", "hr", "ku",
            "la", "lv", "lo", "lt", "lb", "rw", "ro", "mg", "mt", "mr", "ml", "ms", "mk",
            "mi", "mn", "bn", "my", "hmn", "xh", "zu", "ne", "no", "pa", "pt", "ps", "ny",
            "ja", "sv", "sm", "sr", "st", "si", "eo", "sk", "sl", "sw", "gd", "ceb", "so",
            "tg", "te", "ta", "th", "tr", "tk", "cy", "ug", "ur", "uk", "uz", "es", "iw",
            "el", "haw", "sd", "hu", "sn", "hy", "ig", "it", "yi", "hi", "su", "id", "jw",
            "en", "yo", "vi", "zh-TW", "zh-CN", "zh");

    private static final class InstanceHolder {
        private static final GoogleAppTranslator instance = new GoogleAppTranslator();
    }

    static GoogleAppTranslator getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException {
        String url = "https://translate.google.com/translate_a/single?dj=1" +
                "&q=" + URLEncode(query) +
                "&sl=auto" +
                "&tl=" + tl +
                "&ie=UTF-8&oe=UTF-8&client=at&dt=t&otf=2";
        var userAgent = "GoogleTranslate/6.28.0.05.421483610 (Linux; U; Android 13; Pixel 7 Pro)";
        String response = Http.url(url)
                .header("User-Agent", userAgent)
                .request();
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        return getResult(response);
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

    private Result getResult(String string) {
        StringBuilder sb = new StringBuilder();
        Response response = GSON.fromJson(string, Response.class);
        if (response.sentences == null) {
            return null;
        }
        response.sentences.forEach(sentence -> sb.append(sentence.trans));
        return new Result(sb.toString(), response.src);
    }

    public static class Response {
        @SerializedName("sentences")
        @Expose
        public List<Sentence> sentences;
        @SerializedName("src")
        @Expose
        public String src;
    }

    public static class Sentence {
        @SerializedName("trans")
        @Expose
        public String trans;
    }
}
