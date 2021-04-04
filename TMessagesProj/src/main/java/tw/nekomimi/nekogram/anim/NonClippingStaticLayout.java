package tw.nekomimi.nekogram.anim;

import android.graphics.Canvas;
import android.text.Spannable;
import android.text.StaticLayout;
import android.text.TextPaint;

import org.telegram.messenger.Emoji;

/**
 * Draws a single line that never clips
 */
public class NonClippingStaticLayout extends StaticLayout {
    public NonClippingStaticLayout(CharSequence source, TextPaint paint, int width, Alignment align, float spacingmult, float spacingadd, boolean includepad) {
        super(source, paint, width, align, spacingmult, spacingadd, includepad);
        if (source instanceof Spannable) {
            Emoji.EmojiSpan[] spans = ((Spannable) source).getSpans(0, source.length(), Emoji.EmojiSpan.class);
            for (Emoji.EmojiSpan span : spans) {
                ((Emoji.EmojiDrawable) span.getDrawable()).setForceDraw(true);
            }
        }
    }

    public long getLineRangeForDraw(Canvas canvas) {
        return 0L;
    }
}
