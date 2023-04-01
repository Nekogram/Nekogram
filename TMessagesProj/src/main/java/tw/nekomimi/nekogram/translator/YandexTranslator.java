package tw.nekomimi.nekogram.translator;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class YandexTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "af", "sq", "am", "ar", "hy", "az", "ba", "eu", "be", "bn", "bs", "bg", "my",
            "ca", "ceb", "zh", "cv", "hr", "cs", "da", "nl", "sjn", "emj", "en", "eo",
            "et", "fi", "fr", "gl", "ka", "de", "el", "gu", "ht", "he", "mrj", "hi",
            "hu", "is", "id", "ga", "it", "ja", "jv", "kn", "kk", "kazlat", "km", "ko",
            "ky", "lo", "la", "lv", "lt", "lb", "mk", "mg", "ms", "ml", "mt", "mi", "mr",
            "mhr", "mn", "ne", "no", "pap", "fa", "pl", "pt", "pa", "ro", "ru", "gd", "sr",
            "si", "sk", "sl", "es", "su", "sw", "sv", "tl", "tg", "ta", "tt", "te", "th", "tr",
            "udm", "uk", "ur", "uz", "uzbcyr", "vi", "cy", "xh", "sah", "yi", "zu");
    private final String uuid = UUID.randomUUID().toString().replace("-", "");

    private static final class InstanceHolder {
        private static final YandexTranslator instance = new YandexTranslator();
    }

    static YandexTranslator getInstance() {
        return InstanceHolder.instance;
    }

    private static Result getResult(String string) throws IOException {
        Response response = GSON.fromJson(string, Response.class);
        if (response.text == null) {
            if (response.message != null) {
                throw new IOException(response.message);
            }
            return null;
        }
        StringBuilder sb = new StringBuilder();
        response.text.forEach(sb::append);
        String sourceLang;
        if (response.lang != null && response.lang.contains("-")) {
            sourceLang = response.lang.split("-")[0];
        } else {
            sourceLang = null;
        }
        return new Result(sb.toString(), sourceLang);
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException {
        return getResult(Http.url("https://translate.yandex.net/api/v1/tr.json/translate?id=" + uuid + "-2-0&srv=android")
                .header("User-Agent", "ru.yandex.translate/43.4.30430400 (Pixel 7 Pro; Android 13)")
                .data("lang=" + tl + "&text=" + URLEncode(query))
                .request());
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    public static class Response {
        @SerializedName("message")
        @Expose
        public String message;
        @SerializedName("lang")
        @Expose
        public String lang;
        @SerializedName("text")
        @Expose
        public List<String> text;
    }
}
