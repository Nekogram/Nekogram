package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tw.nekomimi.nekogram.Extra;

public class MicrosoftTranslator extends BaseTranslator {

    private static MicrosoftTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "sq", "ar", "az", "ga", "et", "or", "mww", "bg", "is", "pl", "bs", "fa", "ko",
            "da", "de", "ru", "fr", "zh-TW", "fil", "fj", "fi", "gu", "kk", "ht", "nl",
            "ca", "zh-CN", "cs", "kn", "otq", "hr", "lv", "lt", "ro", "mg", "mt", "mr",
            "ml", "ms", "mi", "bn", "af", "ne", "nb", "pa", "pt", "pt-PT", "ja", "sv", "sm",
            "sr-Latn", "sr-Cyrl", "sk", "sl", "sw", "ty", "te", "ta", "th", "to", "tr", "cy",
            "ur", "uk", "es", "he", "el", "hu", "hy", "it", "hi", "id", "en", "yua", "yue",
            "vi", "am", "as", "prs", "fr-CA", "iu", "km", "tlh-Latn", "ku", "kmr", "lo", "my", "ps", "ti");

    static MicrosoftTranslator getInstance() {
        if (instance == null) {
            synchronized (MicrosoftTranslator.class) {
                if (instance == null) {
                    instance = new MicrosoftTranslator();
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

    @Override
    protected Result translate(String query, String tl) throws IOException, JSONException {
        if (tl.equals("zh-CN")) {
            tl = "zh-Hans";
        } else if (tl.equals("zh-TW")) {
            tl = "zh-hant";
        }
        String response = Cognitive.translate(query, tl);
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

    private static class Cognitive {
        private static String sign(String url) {
            String uuid = UUID.randomUUID().toString().replaceAll("-", "");
            try {
                String encode = URLEncoder.encode(url, "UTF-8");
                String time = formatTime();
                byte[] bytes = String.format("%s%s%s%s", "MSTranslatorAndroidApp", encode, time, uuid).toLowerCase().getBytes(Charset.defaultCharset());
                SecretKeySpec secretKeySpec = new SecretKeySpec(Base64.decode(Extra.MICROSOFT_SECRET_KEY, Base64.NO_WRAP | Base64.NO_PADDING), "HmacSHA256");
                Mac instance = Mac.getInstance("HmacSHA256");
                instance.init(secretKeySpec);
                return String.format("%s::%s::%s::%s", "MSTranslatorAndroidApp", Base64.encodeToString(instance.doFinal(bytes), 2), time, uuid);
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        private static String formatTime() {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.US);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            return simpleDateFormat.format(new Date(Calendar.getInstance(TimeZone.getTimeZone("GMT")).getTimeInMillis())).toLowerCase() + "GMT";
        }

        public static String translate(String query, String tl) throws JSONException, IOException {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Text", query);
            String url = "api.cognitive.microsofttranslator.com/translate?api-version=3.0&from=&to=" + tl;
            return Http.url("https://" + url)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("X-Mt-Signature", sign(url))
                    .header("User-Agent", "okhttp/4.5.0")
                    .data(new JSONArray().put(new JSONObject().put("Text", query)).toString())
                    .request();
        }
    }
}
