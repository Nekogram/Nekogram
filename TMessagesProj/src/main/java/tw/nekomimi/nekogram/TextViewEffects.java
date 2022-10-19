package tw.nekomimi.nekogram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Region;
import android.text.Layout;
import android.text.Spanned;
import android.view.MotionEvent;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.LinkSpanDrawable;
import org.telegram.ui.Components.spoilers.SpoilerEffect;
import org.telegram.ui.Components.spoilers.SpoilersClickDetector;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class TextViewEffects extends LinkSpanDrawable.LinksTextView {

    private final SpoilersClickDetector clickDetector;
    private final List<SpoilerEffect> spoilers = new ArrayList<>();
    private final Stack<SpoilerEffect> spoilersPool = new Stack<>();
    private boolean isSpoilersRevealed;
    private final Path path = new Path();
    private Paint xRefPaint;

    private int emojiOnlyCount;
    private AnimatedEmojiSpan.EmojiGroupedSpans animatedEmojiDrawables;
    private Layout lastLayout = null;
    private int lastTextLength;

    public TextViewEffects(Context context) {
        this(context, null);
    }

    public TextViewEffects(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);

        clickDetector = new SpoilersClickDetector(this, spoilers, (eff, x, y) -> {
            if (isSpoilersRevealed) return;

            eff.setOnRippleEndCallback(() -> post(() -> {
                isSpoilersRevealed = true;
                invalidateSpoilers();
            }));

            float rad = (float) Math.sqrt(Math.pow(getWidth(), 2) + Math.pow(getHeight(), 2));
            for (SpoilerEffect ef : spoilers)
                ef.startRipple(x, y, rad);
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (clickDetector.onTouchEvent(event))
            return true;
        return super.dispatchTouchEvent(event);
    }

    public void setText(CharSequence text, int emojiOnlyCount) {
        this.emojiOnlyCount = emojiOnlyCount;
        super.setText(text);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        isSpoilersRevealed = false;
        super.setText(text, type);
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);
        invalidateSpoilers();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        invalidateSpoilers();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        AnimatedEmojiSpan.release(this, animatedEmojiDrawables);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateAnimatedEmoji(false);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        updateAnimatedEmoji(false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int pl = getPaddingLeft(), pt = getPaddingTop();

        canvas.save();
        path.rewind();
        for (SpoilerEffect eff : spoilers) {
            Rect bounds = eff.getBounds();
            path.addRect(bounds.left + pl, bounds.top + pt, bounds.right + pl, bounds.bottom + pt, Path.Direction.CW);
        }
        canvas.clipPath(path, Region.Op.DIFFERENCE);
        updateAnimatedEmoji(false);
        super.onDraw(canvas);
        if (animatedEmojiDrawables != null) {
            AnimatedEmojiSpan.drawAnimatedEmojis(canvas, getLayout(), animatedEmojiDrawables, 0, spoilers, computeVerticalScrollOffset() - AndroidUtilities.dp(6), computeVerticalScrollOffset() + computeVerticalScrollExtent(), 0, 1f);
        }
        canvas.restore();

        canvas.save();
        canvas.clipPath(path);
        path.rewind();
        if (!spoilers.isEmpty()) {
            spoilers.get(0).getRipplePath(path);
        }
        canvas.clipPath(path);
        super.onDraw(canvas);
        canvas.restore();

        if (!spoilers.isEmpty()) {
            boolean useAlphaLayer = spoilers.get(0).getRippleProgress() != -1;
            if (useAlphaLayer) {
                canvas.saveLayer(0, 0, getMeasuredWidth(), getMeasuredHeight(), null, Canvas.ALL_SAVE_FLAG);
            } else {
                canvas.save();
            }
            canvas.translate(getPaddingLeft(), getPaddingTop() + AndroidUtilities.dp(2));
            for (SpoilerEffect eff : spoilers) {
                eff.setColor(getPaint().getColor());
                eff.draw(canvas);
            }

            if (useAlphaLayer) {
                path.rewind();
                spoilers.get(0).getRipplePath(path);
                if (xRefPaint == null) {
                    xRefPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    xRefPaint.setColor(0xff000000);
                    xRefPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
                }
                canvas.drawPath(path, xRefPaint);
            }
            canvas.restore();
        }
    }

    public void updateAnimatedEmoji(boolean force) {
        int newTextLength = (getLayout() == null || getLayout().getText() == null) ? 0 : getLayout().getText().length();
        if (force || lastLayout != getLayout() || lastTextLength != newTextLength) {
            int cacheType = -1;
            switch (emojiOnlyCount) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES_LARGE;
                    break;
                case 5:
                case 6:
                    cacheType = AnimatedEmojiDrawable.CACHE_TYPE_KEYBOARD;
                    break;
                case 7:
                case 8:
                case 9:
                default:
                    if (emojiOnlyCount > 9) {
                        cacheType = AnimatedEmojiDrawable.CACHE_TYPE_MESSAGES;
                    }
                    break;
            }
            animatedEmojiDrawables = AnimatedEmojiSpan.update(cacheType, this, animatedEmojiDrawables, getLayout());
            lastLayout = getLayout();
            lastTextLength = newTextLength;
        }
    }

    private void invalidateSpoilers() {
        if (spoilers == null) return;
        spoilersPool.addAll(spoilers);
        spoilers.clear();

        if (isSpoilersRevealed) {
            invalidate();
            return;
        }

        Layout layout = getLayout();
        if (layout != null && layout.getText() instanceof Spanned) {
            if (animatedEmojiDrawables != null) {
                animatedEmojiDrawables.recordPositions(false);
            }
            SpoilerEffect.addSpoilers(this, spoilersPool, spoilers);
            if (animatedEmojiDrawables != null) {
                animatedEmojiDrawables.recordPositions(true);
            }
        }
        invalidate();
    }
}