package tw.nekomimi.nekogram.forward;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class ForwardDrawable extends Drawable {

    private static final int BADGE_LEFT = AndroidUtilities.dp(5f);
    private static final int BADGE_TOP = AndroidUtilities.dp(3f);

    private final Drawable drawable;
    private Drawable badgeDrawable = null;
    private int badgeHeight;
    private int badgeWidth;

    public ForwardDrawable(int type) {
        super();

        switch (type) {
            case ForwardItem.ID_FORWARD_NOCAPTION:
                drawable = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.msg_remove).mutate();
                break;
            default:
            case ForwardItem.ID_FORWARD:
                badgeDrawable = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.msg_forward_badge).mutate();
                badgeHeight = Math.round(badgeDrawable.getIntrinsicHeight() / 3.4f);
                badgeWidth = Math.round(badgeDrawable.getIntrinsicWidth() / 3.4f);
            case ForwardItem.ID_FORWARD_NOQUOTE:
                drawable = ApplicationLoader.applicationContext.getResources().getDrawable(R.drawable.msg_forward).mutate();
                break;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (badgeDrawable != null) {
            badgeDrawable.setBounds(bounds.left + BADGE_LEFT, bounds.top + BADGE_TOP, bounds.left + BADGE_LEFT + badgeWidth, bounds.top + BADGE_TOP + badgeHeight);
            badgeDrawable.draw(canvas);
        }
        drawable.setBounds(getBounds());
        drawable.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        drawable.setAlpha(alpha);
        if (badgeDrawable != null) {
            badgeDrawable.setAlpha(alpha);
        }
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        drawable.setColorFilter(colorFilter);
        if (badgeDrawable != null) {
            badgeDrawable.setColorFilter(colorFilter);
        }
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public int getIntrinsicHeight() {
        return drawable.getIntrinsicHeight();
    }

    @Override
    public int getIntrinsicWidth() {
        return drawable.getIntrinsicWidth();
    }
}
