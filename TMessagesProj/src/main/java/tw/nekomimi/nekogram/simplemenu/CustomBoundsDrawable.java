package tw.nekomimi.nekogram.simplemenu;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * A wrapped {@link Drawable} that force use its own bounds to draw.
 * <p>
 * It maybe a little dirty. But if we don't do that, during the expanding animation, there will be
 * one or two frame using wrong bounds because of parent view sets bounds.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class CustomBoundsDrawable extends DrawableWrapper {

    public CustomBoundsDrawable(Drawable wrappedDrawable) {
        super(wrappedDrawable);
    }

    public void setCustomBounds(@NonNull Rect bounds) {
        setCustomBounds(bounds.left, bounds.top, bounds.right, bounds.bottom);
    }

    public void setCustomBounds(int left, int top, int right, int bottom) {
        setBounds(left, top, right, bottom);
        getWrappedDrawable().setBounds(left, top, right, bottom);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
    }

    @Override
    public void setBounds(@NonNull Rect bounds) {
    }
}
