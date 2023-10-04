package tw.nekomimi.nekogram.syntaxhighlight;

import android.graphics.Color;
import android.text.Spannable;
import android.text.Spanned;

import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.TextStyleSpan;

public class SyntaxHighlight {

    public static void highlight(TextStyleSpan.TextStyleRun run, Spannable spannable) {
        if (run.urlEntity instanceof TLRPC.TL_messageEntityHashtag) {
            var length = run.end - run.start;
            if (length == 7 || length == 9) {
                try {
                    int color = Color.parseColor(spannable.subSequence(run.start, run.end).toString());
                    spannable.setSpan(new ColorHighlightSpan(color, run), run.end - 1, run.end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } catch (IllegalArgumentException ignore) {
                }
            }
        }
    }
}
