package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.deepl.DeeplTranslater;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DeepLTranslator extends BaseTranslator {

    private static DeepLTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "bg", "pl", "da", "de", "ru", "fr", "fi", "nl", "cs", "lv", "lt", "ro",
            "pt", "pt-PT", "pt-BR", "ja", "sv", "sk", "sl", "es", "el", "hu", "it", "en", "en-GB", "en-US", "zh");
    private final DeeplTranslater deeplTranslater = new DeeplTranslater();

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
    protected String translate(String query, String tl) throws IOException {
        return deeplTranslater.translate(query, "auto", tl.toUpperCase());
    }
}
