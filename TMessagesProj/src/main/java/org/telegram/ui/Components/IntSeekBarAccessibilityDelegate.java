package org.telegram.ui.Components;

import android.view.View;
import android.view.accessibility.AccessibilityEvent;

import tw.nekomimi.nekogram.accessibility.AccConfig;

public abstract class IntSeekBarAccessibilityDelegate extends SeekBarAccessibilityDelegate {

    @Override
    protected void doScroll(View host, boolean backward) {
        int delta = getDelta();
        if (backward) {
            delta *= -1;
        }
        setProgress(Math.min(getMaxValue(), Math.max(getMinValue(), getProgress() + delta)));
    }

    @Override
    protected boolean canScrollBackward(View host) {
        return getProgress() > getMinValue();
    }

    @Override
    protected boolean canScrollForward(View host) {
        return getProgress() < getMaxValue();
    }

    @Override
    public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(host, event);
        if (AccConfig.showNumbersOfItems || event.getEventType() == AccessibilityEvent.TYPE_ANNOUNCEMENT) event.setItemCount(getMaxValue() - getMinValue());
        if (AccConfig.showIndexOfItem || event.getEventType() == AccessibilityEvent.TYPE_ANNOUNCEMENT) event.setCurrentItemIndex(getProgress());
    }

    protected abstract int getProgress();

    protected abstract void setProgress(int progress);

    protected int getMinValue() {
        return 0;
    }

    protected  int getMaxValue() {
        return 100;
    }

    protected int getDelta() {
        return 1;
    }
}
