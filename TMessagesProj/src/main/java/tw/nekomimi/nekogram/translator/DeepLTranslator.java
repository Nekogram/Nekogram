package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.translator.DeepLTranslatorAndroid;

import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;

public class DeepLTranslator extends BaseTranslator {

    public static final int FORMALITY_DEFAULT = 0;
    public static final int FORMALITY_MORE = 1;
    public static final int FORMALITY_LESS = 2;

    private final List<String> targetLanguages = Arrays.asList(
            "bg", "cs", "da", "de", "el", "en-GB", "en-US", "en", "es",
            "et", "fi", "fr", "hu", "id", "it", "ja", "lt", "lv", "nl",
            "pl", "pt-BR", "pt-PT", "pt", "ro", "ru", "sk", "sl", "sv",
            "tr", "uk", "zh");
    private final DeepLTranslatorAndroid deeplTranslater = new DeepLTranslatorAndroid();

    private static final class InstanceHolder {
        private static final DeepLTranslator instance = new DeepLTranslator();
    }

    static DeepLTranslator getInstance() {
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
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws Exception {
        var result = deeplTranslater.translate(query, tl, getFormalityString(), "newlines");
        return new Result(result[1], result[0]);
    }

    private String getFormalityString() {
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
