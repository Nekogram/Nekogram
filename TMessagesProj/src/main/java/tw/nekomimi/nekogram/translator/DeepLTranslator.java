package tw.nekomimi.nekogram.translator;

import org.deepl.DeeplTranslater;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class DeepLTranslator extends BaseTranslator {

    private static DeepLTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "BG", "PL", "DA", "DE", "RU", "FR", "FI", "NL", "CS", "LV", "LT", "RO",
            "PT", "JA", "SV", "SK", "SL", "ES", "EL", "HU", "IT", "US", "ZH");
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
    protected List<String> getTargetLanguages() {
        return targetLanguages;
    }

    @Override
    protected String translate(String query, String tl) throws IOException {
        return deeplTranslater.translate(query, "auto", tl);
    }
}
