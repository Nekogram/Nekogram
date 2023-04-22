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

    private final Drawable drawable;
    private Drawable badgeDrawable = null;
    private int badgeHeight;
    private int badgeWidth;
    private int badgeLeft;
    private int badgeTop;

    public ForwardDrawable(int type, boolean share) {
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
                badgeLeft = AndroidUtilities.dp(share ? 3f : 5f);
                badgeTop = AndroidUtilities.dp(share ? 1f : 3f);
            case ForwardItem.ID_FORWARD_NOQUOTE:
                drawable = ApplicationLoader.applicationContext.getResources().getDrawable(share ? R.drawable.msg_header_share : R.drawable.msg_forward).mutate();
                break;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if (badgeDrawable != null) {
            badgeDrawable.setBounds(bounds.left + badgeLeft, bounds.top + badgeTop, bounds.left + badgeLeft + badgeWidth, bounds.top + badgeTop + badgeHeight);
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
