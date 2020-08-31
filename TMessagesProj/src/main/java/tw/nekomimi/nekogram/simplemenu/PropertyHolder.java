package tw.nekomimi.nekogram.simplemenu;

import android.annotation.TargetApi;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;

/**
 * Holder class holds background drawable and content view.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class PropertyHolder {

    private final CustomBoundsDrawable mBackground;
    private final View mContentView;

    public PropertyHolder(CustomBoundsDrawable background, View contentView) {
        mBackground = background;
        mContentView = contentView;
    }

    private CustomBoundsDrawable getBackground() {
        return mBackground;
    }

    private View getContentView() {
        return mContentView;
    }

    public Rect getBounds() {
        return getBackground().getBounds();
    }

    public void setBounds(Rect value) {
        getBackground().setCustomBounds(value);
        getContentView().invalidateOutline();
    }
}
