package tw.nekomimi.nekogram.syntaxhighlight;

import android.graphics.Color;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TextStyleSpan;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class SyntaxHighlight {

    private static boolean lastDark;
    private static Prism4jSyntaxHighlight highlight;

    public static void highlight(TextStyleSpan.TextStyleRun run, Spannable spannable) {
        if (!NekoConfig.codeSyntaxHighlight) {
            return;
        }
        if (run.urlEntity instanceof TLRPC.TL_messageEntityHashtag) {
            try {
                int color = Color.parseColor(spannable.subSequence(run.start, run.end).toString());
                var light = AndroidUtilities.computePerceivedBrightness(color) > 0.725f;
                spannable.setSpan(new ForegroundColorSpan(light ? Color.BLACK : Color.WHITE), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new BackgroundColorSpan(color), run.start, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (IllegalArgumentException ignore) {
            }
        } else if (!TextUtils.isEmpty(run.urlEntity.language)) {
            boolean dark = Theme.getActiveTheme().isDark();
            if (highlight == null || lastDark != dark) {
                lastDark = dark;
                highlight = Prism4jSyntaxHighlight.create(new Prism4j(new Prism4jGrammarLocator()), dark ? Prism4jThemeDarkula.create() : Prism4jThemeDefault.create());
            }
            highlight.highlight(run.urlEntity.language, spannable, run.start, run.end);
        }
    }
}
