package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.deepl.DeepLTranslater;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.FileLog;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class DeepLTranslator extends BaseTranslator {

    public static final int FORMALITY_DEFAULT = 0;
    public static final int FORMALITY_MORE = 1;
    public static final int FORMALITY_LESS = 2;

    private static DeepLTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "bg", "cs", "pl", "da", "de", "el", "en-GB", "en-US", "en",
            "es", "et", "fi", "fr", "hu", "it", "ja", "lt", "lv", "nl",
            "pl", "pt-BR", "pt-PT", "pt", "ro", "ru", "sk", "sl", "sv",
            "zh");
    private final DeepLTranslater deeplTranslater = new DeepLTranslater();

    private boolean triedApi = false;
    private String lastKey = NekoConfig.lastDeepLKey;

    static DeepLTranslator getInstance() {
        if (instance == null) {
            synchronized (DeepLTranslator.class) {
                if (instance == null) {
                    instance = new DeepLTranslator();
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
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    @Override
    protected Result translate(String query, String tl) throws Exception {
        Result result = null;
        if (!triedApi) {
            if (!TextUtils.isEmpty(lastKey)) {
                try {
                    result = translateApiImpl(lastKey, query, tl);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            if (result == null) {
                for (String key : ConfigHelper.getDeepLKeys()) {
                    try {
                        result = translateApiImpl(key, query, tl);
                        if (result != null) {
                            lastKey = key;
                            NekoConfig.setLastDeepLKey(lastKey);
                            break;
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            }

        }
        if (result == null) {
            if (!triedApi) {
                triedApi = true;
                ConfigHelper.getInstance().load();
            }
            result = translateWebImpl(query, tl);
        }
        return result;
    }

    private Result translateApiImpl(String key, String query, String tl) throws IOException, JSONException {
        var params = "text=" + URLEncoder.encode(query, "UTF-8") +
                "&target_lang=" + tl.toUpperCase() +
                "&formality=" + getFormalityStringApi();
        String response = Http.url("https://api" + (key.endsWith(":fx") ? "-free" : "") + ".deepl.com/v2/translate")
                .header("Authorization", "DeepL-Auth-Key " + key)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.82 Safari/537.36")
                .data(params)
                .request();
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        var jsonObject = new JSONObject(response);
        if (!jsonObject.has("translations") && jsonObject.has("message")) {
            throw new IOException(jsonObject.getString("message"));
        }
        var translation = jsonObject.getJSONArray("translations").getJSONObject(0);
        String sourceLang = null;
        if (translation.has("detected_source_language")) {
            sourceLang = translation.getString("detected_source_language");
        }
        return new Result(translation.getString("text"), sourceLang);
    }

    private Result translateWebImpl(String query, String tl) throws Exception {
        var result = deeplTranslater.translate(query, "auto", tl.toUpperCase(), getFormalityStringWeb());
        return new Result(result[1], result[0]);
    }

    private String getFormalityStringApi() {
        switch (NekoConfig.deepLFormality) {
            case FORMALITY_DEFAULT:
            default:
                return "default";
            case FORMALITY_MORE:
                return "more";
            case FORMALITY_LESS:
                return "less";
        }
    }

    private String getFormalityStringWeb() {
        switch (NekoConfig.deepLFormality) {
            case FORMALITY_DEFAULT:
            default:
                return null;
            case FORMALITY_MORE:
                return "formal";
            case FORMALITY_LESS:
                return "informal";
        }
    }
}
