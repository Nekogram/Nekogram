package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.deepl.DeepLTranslater;

import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;

public class DeepLTranslator extends BaseTranslator {

    public static final int FORMALITY_DEFAULT = 0;
    public static final int FORMALITY_MORE = 1;
    public static final int FORMALITY_LESS = 2;

    private static DeepLTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "bg", "pl", "da", "de", "ru", "fr", "fi", "nl", "cs", "lv", "lt", "ro",
            "pt", "pt-PT", "pt-BR", "ja", "sv", "sk", "sl", "es", "el", "hu", "it", "en", "en-GB", "en-US", "zh");
    private final DeepLTranslater deeplTranslater = new DeepLTranslater();

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
        return new Result(deeplTranslater.translate(query, "auto", tl.toUpperCase(), getFormalityString()), null);
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
