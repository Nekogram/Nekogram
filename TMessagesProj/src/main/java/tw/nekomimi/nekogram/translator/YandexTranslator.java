package tw.nekomimi.nekogram.translator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class YandexTranslator extends BaseTranslator {

    private static YandexTranslator instance;
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

    static YandexTranslator getInstance() {
        if (instance == null) {
            synchronized (YandexTranslator.class) {
                if (instance == null) {
                    instance = new YandexTranslator();
                }
            }
        }
        return instance;
    }

    private static Result getResult(String string) throws JSONException, IOException {
        JSONObject json = new JSONObject(string);
        if (!json.has("text") && json.has("message")) {
            throw new IOException(json.getString("message"));
        }
        JSONArray array = json.getJSONArray("text");
        StringBuilder sb = new StringBuilder();
        int length = array.length();
        for (int i = 0; i < length; i++) {
            sb.append(array.getString(i));
        }
        String sourceLang = null;
        try {
            sourceLang = json.getString("lang").split("-")[0];
        } catch (Exception e) {
            FileLog.e(e);
        }
        return new Result(sb.toString(), sourceLang);
    }

    @Override
    protected Result translate(String query, String tl) throws IOException, JSONException {
        return getResult(Http.url("https://translate.yandex.net/api/v1/tr.json/translate?id=" + uuid + "-0-0&srv=android")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "ru.yandex.translate/21.15.4.21402814 (Xiaomi Redmi K20 Pro; Android 11)")
                .data("lang=" + tl + "&text=" + URLEncoder.encode(query, "UTF-8"))
                .request());
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }
}
