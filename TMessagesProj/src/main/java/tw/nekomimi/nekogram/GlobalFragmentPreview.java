package tw.nekomimi.nekogram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;

import java.util.function.Consumer;

public abstract class GlobalFragmentPreview {

    private final WindowManager windowManager;

    private final View previewView;
    private BitmapDrawable previewBlurDrawable;
    private float startY;

    private boolean visible;

    public GlobalFragmentPreview(Context context) {
        windowManager = ContextCompat.getSystemService(context, WindowManager.class);
        previewView = new View(context) {
            @Override
            public void draw(@NonNull Canvas canvas) {
                super.draw(canvas);
                var actionBarLayout = getActionBarLayout();
                if (visible && actionBarLayout != null) {
                    if (previewBlurDrawable != null) {
                        previewBlurDrawable.setAlpha((int) (actionBarLayout.getCurrentPreviewFragmentAlpha() * 255));
                        previewBlurDrawable.draw(canvas);
                    }
                    actionBarLayout.drawCurrentPreviewFragment(canvas, null);
                }
            }
        };
    }

    public void show() {
        createBlurDrawable();
        var params = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                0, PixelFormat.TRANSLUCENT
        );
        params.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        params.flags |= WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                | WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
        AndroidUtilities.applyEdgeToEdgeLayoutParams(params);
        AndroidUtilities.setPreferredMaxRefreshRate(windowManager, previewView, params);

        windowManager.addView(previewView, params);
        visible = true;

        invalidate();
    }

    public void hide() {
        windowManager.removeView(previewView);
        visible = false;
        startY = 0;
    }

    public void invalidate() {
        if (visible && previewView != null) {
            previewView.invalidate();
        }
    }

    public boolean dispatchTouchEvent(MotionEvent ev, Consumer<MotionEvent> superMethod) {
        var actionBarLayout = getActionBarLayout();
        if (visible && actionBarLayout != null) {
            final int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_MOVE) {
                if (startY == 0) {
                    startY = ev.getY();
                    MotionEvent event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_CANCEL, 0, 0, 0);
                    superMethod.accept(event);
                    event.recycle();
                } else {
                    actionBarLayout.movePreviewFragment(startY - ev.getY());
                }
            } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP || action == MotionEvent.ACTION_CANCEL) {
                actionBarLayout.finishPreviewFragment();
            }
            return true;
        }
        return false;
    }

    public boolean isVisible() {
        return visible;
    }

    public void createBlurDrawable() {
        var amount = 15;
        var measuredWidth = getContainerWidth();
        var measuredHeight = getContainerHeight();
        var w = measuredWidth / amount;
        var h = measuredHeight / amount;
        var bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        var canvas = new Canvas(bitmap);
        canvas.scale(1.0f / amount, 1.0f / amount);
        drawContainer(canvas);
        Utilities.stackBlurBitmap(bitmap, Math.max(amount, Math.max(w, h) / 180));
        var colorMatrixBg = new ColorMatrix();
        AndroidUtilities.adjustSaturationColorMatrix(colorMatrixBg, Theme.isCurrentThemeDark() ? .04f : +.25f);
        AndroidUtilities.adjustBrightnessColorMatrix(colorMatrixBg, Theme.isCurrentThemeDark() ? -.04f : -.07f);
        var bitmapBg = AndroidUtilities.applyColorMatrix(bitmap, colorMatrixBg);
        bitmapBg.setHasAlpha(false);
        previewBlurDrawable = new BitmapDrawable(bitmapBg);
        previewBlurDrawable.setBounds(0, 0, measuredWidth, measuredHeight);
    }

    protected abstract int getContainerWidth();

    protected abstract int getContainerHeight();

    protected abstract void drawContainer(Canvas canvas);

    public abstract INavigationLayout getActionBarLayout();
}
