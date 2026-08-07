package tw.nekomimi.nekogram.syntaxhighlight;

import android.graphics.Color;
import android.text.Spannable;
import android.text.Spanned;

public class SyntaxHighlight {

    public static void highlightColor(Spannable spannable, int start, int end) {
        var length = end - start;
        if (length == 7 || length == 9) {
            try {
                int color = Color.parseColor(spannable.subSequence(start, end).toString());
                spannable.setSpan(new ColorHighlightSpan(color), end - 1, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } catch (IllegalArgumentException ignore) {
            }
        }
    }
}
