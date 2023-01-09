package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import androidx.core.text.HtmlCompat;

import org.translator.MrTranslatorWeb;

import java.util.Arrays;
import java.util.List;

public class TencentTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList("zh", "zh-TW", "en", "ja", "ko", "fr", "es", "it", "de", "tr", "ru", "pt", "vi", "id", "th", "ms", "ar", "hi");
    private final MrTranslatorWeb mrTranslatorWeb = new MrTranslatorWeb();

    private static final class InstanceHolder {
        private static final TencentTranslator instance = new TencentTranslator();
    }

    static TencentTranslator getInstance() {
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
                if (countryUpperCase.equals("HK")) {
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
    protected Result translate(String query, String fl, String tl) throws Exception {
        var result = mrTranslatorWeb.translate(query.replace("\n", "<br/>"), "auto", tl);
        return new Result(HtmlCompat.fromHtml(result[1], HtmlCompat.FROM_HTML_MODE_LEGACY).toString(), result[0]);
    }
}
