package tw.nekomimi.nekogram.translator;

import androidx.core.text.HtmlCompat;

import app.nekogram.translator.Result;
import app.nekogram.translator.TencentTranslator;

public class TencentTranslatorNewLine extends TencentTranslator {

    @Override
    public synchronized Result translate(String sourceText, String source, String target) {
        var result = super.translate(sourceText.replace("\n", "<br/>"), source, target);
        result.translation = HtmlCompat.fromHtml(result.translation, HtmlCompat.FROM_HTML_MODE_LEGACY).toString();
        return result;
    }
}
