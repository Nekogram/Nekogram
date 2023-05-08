package tw.nekomimi.nekogram.folder;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

import java.util.concurrent.atomic.AtomicReference;

public class IconSelectorAlert {
    private final static Paint selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public static void show(BaseFragment fragment, View view, String selectedIcon, OnIconSelectedListener onIconSelectedListener) {
        selectedPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
        selectedPaint.setAlpha(40);

        Context context = fragment.getParentActivity();

        ActionBarPopupWindow.ActionBarPopupWindowLayout layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context);
        Rect backgroundPaddings = new Rect();
        Drawable shadowDrawable = fragment.getParentActivity().getResources().getDrawable(R.drawable.popup_fixed_alert).mutate();
        shadowDrawable.getPadding(backgroundPaddings);
        layout.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        int[] location = new int[2];
        view.getLocationInWindow(location);

        int popupX = location[0] - AndroidUtilities.dp(8) - backgroundPaddings.left + view.getMeasuredWidth();
        int popupY = location[1] - AndroidUtilities.dp(8) - backgroundPaddings.top + view.getMeasuredHeight();

        AtomicReference<ActionBarPopupWindow> scrimPopupWindowRef = new AtomicReference<>();

        GridLayout gridLayout = new GridLayout(context);
        int columnCount = 6;
        while (AndroidUtilities.displaySize.x - popupX < 48 * columnCount + AndroidUtilities.dp(8)) {
            columnCount--;
        }
        gridLayout.setColumnCount(columnCount);

        for (String icon : FolderIconHelper.folderIcons.keySet().toArray(new String[0])) {
            var imageView = new ImageView(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    if (isSelected()) {
                        AndroidUtilities.rectTmp.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                        canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(4), AndroidUtilities.dp(4), selectedPaint);
                    }
                    super.onDraw(canvas);
                }
            };
            boolean selected = icon.equals(selectedIcon);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setBackground(Theme.createRadSelectorDrawable(selected ? Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteValueText), .1f) : Theme.getColor(Theme.key_listSelector), AndroidUtilities.dp(2), AndroidUtilities.dp(2)));
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(selected ? Theme.key_windowBackgroundWhiteValueText : Theme.key_windowBackgroundWhiteGrayIcon), PorterDuff.Mode.MULTIPLY));
            imageView.setImageResource(FolderIconHelper.getTabIcon(icon));
            imageView.setSelected(selected);
            imageView.setOnClickListener(v -> {
                if (selectedIcon.equals(icon)) {
                    return;
                }
                if (scrimPopupWindowRef.get() != null) {
                    scrimPopupWindowRef.getAndSet(null).dismiss();
                }
                onIconSelectedListener.onIconSelected(icon);
            });
            gridLayout.addView(imageView, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
        }
        layout.addView(gridLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 4, 4));

        ActionBarPopupWindow scrimPopupWindow = new ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        scrimPopupWindowRef.set(scrimPopupWindow);
        scrimPopupWindow.setPauseNotifications(true);
        scrimPopupWindow.setDismissAnimationDuration(220);
        scrimPopupWindow.setOutsideTouchable(true);
        scrimPopupWindow.setClippingEnabled(true);
        scrimPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        scrimPopupWindow.setFocusable(true);
        layout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        scrimPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        scrimPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        scrimPopupWindow.getContentView().setFocusableInTouchMode(true);
        scrimPopupWindow.showAtLocation(view, Gravity.LEFT | Gravity.TOP, popupX, popupY);
        scrimPopupWindow.dimBehind();
    }

    public interface OnIconSelectedListener {
        void onIconSelected(String emoticon);
    }
}
