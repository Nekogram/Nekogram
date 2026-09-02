package tw.nekomimi.nekogram;

import android.content.Context;
import android.graphics.Paint;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewSwitcher;

import org.telegram.ui.ActionBar.SimpleTextView;

public class SimpleTextViewSwitcher extends ViewSwitcher {

    public SimpleTextViewSwitcher(Context context) {
        super(context);
    }

    public void setText(CharSequence text, boolean animated) {
        if (!TextUtils.equals(text, getCurrentView().getText())) {
            if (animated) {
                getNextView().setText(text);
                showNext();
            } else {
                getCurrentView().setText(text);
            }
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (!(child instanceof SimpleTextView)) {
            throw new IllegalArgumentException();
        }
        super.addView(child, index, params);
    }

    @Override
    public SimpleTextView getCurrentView() {
        return (SimpleTextView) super.getCurrentView();
    }

    @Override
    public SimpleTextView getNextView() {
        return (SimpleTextView) super.getNextView();
    }

    public void invalidateViews() {
        getCurrentView().invalidate();
        getNextView().invalidate();
    }

    public void setTextColor(int color) {
        getCurrentView().setTextColor(color);
        getNextView().setTextColor(color);
    }

    public Paint getPaint() {
        return getCurrentView().getPaint();
    }

    public CharSequence getText() {
        return getCurrentView().getText();
    }

    public float getExactWidth() {
        return getCurrentView().getExactWidth();
    }
}
