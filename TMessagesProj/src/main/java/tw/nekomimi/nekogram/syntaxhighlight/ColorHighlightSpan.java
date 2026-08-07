package tw.nekomimi.nekogram.syntaxhighlight;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.CharacterStyle;
import android.text.style.ReplacementSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ColorHighlightSpan extends ReplacementSpan {

    private static final Paint colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int color;

    public ColorHighlightSpan(int color) {
        this.color = color;
    }

    @Override
    public int getSize(@NonNull Paint paint, CharSequence text,
                       int start, int end,
                       @Nullable Paint.FontMetricsInt fm) {
        var size = paint.getTextSize() * 0.9f;
        var spacing = size / 7.2f;
        return Math.round(paint.measureText(text, start, end) + spacing + size);
    }

    @Override
    public void draw(@NonNull Canvas canvas, CharSequence text,
                     int start, int end, float x,
                     int top, int y, int bottom, @NonNull Paint paint) {
        var textPaint = (TextPaint) paint;
        if (text instanceof Spanned spanned) {
            var spans = spanned.getSpans(start, end, CharacterStyle.class);
            for (var span : spans) {
                span.updateDrawState(textPaint);
            }
        }
        canvas.drawText(text, start, end, x, y, textPaint);

        colorPaint.setColor(color);
        var size = textPaint.getTextSize() * 0.9f;
        var spacing = size / 7.2f;
        var paddingTop = top + (bottom - top - size) / 2.0f;
        var paddingLeft = x + textPaint.measureText(text, start, end) + spacing;
        canvas.drawRoundRect(paddingLeft, paddingTop, paddingLeft + size, paddingTop + size, size * 0.285f, size * 0.285f, colorPaint);
    }
}
