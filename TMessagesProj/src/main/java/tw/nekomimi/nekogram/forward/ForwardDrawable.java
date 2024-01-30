package tw.nekomimi.nekogram.forward;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.Components.RLottieDrawable;

public class ForwardDrawable extends Drawable {

    private final Drawable drawable;

    public ForwardDrawable(int type, boolean share) {
        super();

        if (type == ForwardItem.ID_FORWARD) {
            drawable = ApplicationLoader.applicationContext.getResources().getDrawable(
                    share ? R.drawable.msg_header_share : R.drawable.msg_forward
            ).mutate();
        } else {
            RLottieDrawable drawable = new RLottieDrawable(
                    type == ForwardItem.ID_FORWARD_NOCAPTION ? R.raw.caption_hide : R.raw.name_hide,
                    type == ForwardItem.ID_FORWARD_NOCAPTION ? "caption_hide" : "name_hide",
                    dp(24), dp(24));
            drawable.setAllowDecodeSingleFrame(true);
            drawable.setPlayInDirectionOfCustomEndFrame(true);
            drawable.setAutoRepeat(0);
            this.drawable = drawable;
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        drawable.setBounds(getBounds());
        drawable.draw(canvas);
    }

    @Override
    public void setAlpha(int alpha) {
        drawable.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        drawable.setColorFilter(colorFilter);
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
