package tw.nekomimi.nekogram.simplemenu;

import android.graphics.Rect;
import android.view.View;

/**
 * Holder class holds background drawable and content view.
 */

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

    public View getContentView() {
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
