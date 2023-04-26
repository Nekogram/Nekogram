package tw.nekomimi.nekogram.simplemenu;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RecyclerListView;

import java.util.Arrays;

/**
 * Extension of {@link PopupWindow} that implements
 * <a href="https://material.io/guidelines/components/menus.html#menus-simple-menus">Simple Menus</a>
 * in Material Design.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class SimpleMenuPopupWindow extends PopupWindow {

    public static final int POPUP_MENU = 0;
    public static final int DIALOG = 1;

    public static final int HORIZONTAL = 0;
    public static final int VERTICAL = 1;
    protected final int[] elevation = new int[2];
    protected final int[][] margin = new int[2][2];
    protected final int[][] listPadding = new int[2][2];
    protected final int itemHeight;
    protected final int dialogMaxWidth;
    protected final int unit;
    protected final int maxUnits;
    private int mMode = POPUP_MENU;
    private boolean mRequestMeasure = true;
    private final SimpleMenuListAdapter mAdapter;
    private OnItemClickListener mOnItemClickListener;
    private CharSequence[] mEntries;
    private int mSelectedIndex;
    private int mMeasuredWidth;

    public SimpleMenuPopupWindow(Context context, Theme.ResourcesProvider resourcesProvider) {

        setFocusable(true);
        setOutsideTouchable(false);

        elevation[POPUP_MENU] = (int) AndroidUtilities.dpf2(8);
        elevation[DIALOG] = (int) AndroidUtilities.dpf2(24);
        margin[POPUP_MENU][HORIZONTAL] = (int) (AndroidUtilities.isTablet() ? AndroidUtilities.dpf2(23) : AndroidUtilities.dpf2(15));
        margin[POPUP_MENU][VERTICAL] = (int) AndroidUtilities.dpf2(8);
        margin[DIALOG][HORIZONTAL] = (int) AndroidUtilities.dpf2(16);
        margin[DIALOG][VERTICAL] = (int) AndroidUtilities.dpf2(24);
        listPadding[POPUP_MENU][HORIZONTAL] = (int) AndroidUtilities.dpf2(16);
        listPadding[DIALOG][HORIZONTAL] = (int) AndroidUtilities.dpf2(24);
        dialogMaxWidth = (int) AndroidUtilities.dpf2(600);
        unit = (int) AndroidUtilities.dpf2(56);
        maxUnits = AndroidUtilities.isTablet() ? 7 : 5;

        GradientDrawable backgroundDrawable = new GradientDrawable();
        backgroundDrawable.setCornerRadius(AndroidUtilities.dp(4));
        backgroundDrawable.setColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
        setBackgroundDrawable(backgroundDrawable);

        RecyclerListView mList = new RecyclerListView(context, resourcesProvider);
        mList.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        mList.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mList.setVerticalScrollBarEnabled(true);
        mList.setFocusable(true);
        mList.setLayoutManager(new LinearLayoutManager(context));
        mList.setItemAnimator(null);
        mList.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                getBackground().getOutline(outline);
            }
        });
        mList.setClipToPadding(false);
        mList.setClipToOutline(true);
        mList.setOnItemClickListener((view, position) -> {
            mOnItemClickListener.onClick(position);

            if (isShowing()) {
                dismiss();
            }
        });

        setContentView(mList);

        mAdapter = new SimpleMenuListAdapter(this, resourcesProvider);
        mList.setAdapter(mAdapter);

        // TODO do not hardcode
        itemHeight = AndroidUtilities.dpr(48);
        listPadding[POPUP_MENU][VERTICAL] = listPadding[DIALOG][VERTICAL] = AndroidUtilities.dpr(8);
    }

    public OnItemClickListener getOnItemClickListener() {
        return mOnItemClickListener;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    protected int getMode() {
        return mMode;
    }

    private void setMode(int mode) {
        mMode = mode;
    }

    protected CharSequence[] getEntries() {
        return mEntries;
    }

    public void setEntries(CharSequence[] entries) {
        mEntries = entries;
    }

    protected int getSelectedIndex() {
        return mSelectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        mSelectedIndex = selectedIndex;
    }

    @Override
    public RecyclerView getContentView() {
        return (RecyclerView) super.getContentView();
    }

    @Override
    public CustomBoundsDrawable getBackground() {
        Drawable background = super.getBackground();
        if (background != null
                && !(background instanceof CustomBoundsDrawable)) {
            setBackgroundDrawable(background);
        }
        return (CustomBoundsDrawable) super.getBackground();
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        if (background == null) {
            throw new IllegalStateException("SimpleMenuPopupWindow must have a background");
        }

        if (!(background instanceof CustomBoundsDrawable)) {
            background = new CustomBoundsDrawable(background);
        }
        super.setBackgroundDrawable(background);
    }

    /**
     * Show the PopupWindow
     *
     * @param anchor      View that will be used to calc the position of windows
     * @param container   View that will be used to calc the position of windows
     * @param extraMargin extra margin start
     */
    public void show(View anchor, View container, int extraMargin) {
        int maxMaxWidth = container.getWidth() - margin[POPUP_MENU][HORIZONTAL] * 2;
        int measuredWidth = measureWidth(maxMaxWidth, mEntries);
        if (measuredWidth == -1) {
            setMode(DIALOG);
        } else if (measuredWidth != 0) {
            setMode(POPUP_MENU);

            mMeasuredWidth = measuredWidth;
        }

        mAdapter.notifyDataSetChanged();

        // clear last bounds
        Rect zeroRect = new Rect();
        getBackground().setCustomBounds(zeroRect);
        getContentView().invalidateOutline();

        if (mMode == POPUP_MENU) {
            showPopupMenu(anchor, container, mMeasuredWidth, extraMargin);
        } else {
            showDialog(anchor, container);
        }
    }

    /**
     * Show popup window in dialog mode
     *
     * @param parent    a parent view to get the {@link View#getWindowToken()} token from
     * @param container Container view that holds preference list, also used to calc width
     */
    private void showDialog(View parent, View container) {
        final int index = Math.max(0, mSelectedIndex);
        final int count = mEntries.length;

        getContentView().setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);
        getContentView().scrollToPosition(index);

        setWidth(Math.min(dialogMaxWidth, container.getWidth() - margin[DIALOG][HORIZONTAL] * 2));
        setHeight(WRAP_CONTENT);
        setAnimationStyle(R.style.Animation_Preference_SimpleMenuCenter);
        setElevation(elevation[DIALOG]);

        super.showAtLocation(parent, Gravity.CENTER_VERTICAL, 0, 0);

        getContentView().post(() -> {
            // disable over scroll when no scroll
            LinearLayoutManager lm = (LinearLayoutManager) SimpleMenuPopupWindow.this.getContentView().getLayoutManager();
            if (lm != null && lm.findFirstCompletelyVisibleItemPosition() == 0
                    && lm.findLastCompletelyVisibleItemPosition() == count - 1) {
                SimpleMenuPopupWindow.this.getContentView().setOverScrollMode(View.OVER_SCROLL_NEVER);
            }

            int width = SimpleMenuPopupWindow.this.getContentView().getWidth();
            int height = SimpleMenuPopupWindow.this.getContentView().getHeight();
            Rect start = new Rect(width / 2, height / 2, width / 2, height / 2);

            SimpleMenuAnimation.startEnterAnimation(SimpleMenuPopupWindow.this.getBackground(), SimpleMenuPopupWindow.this.getContentView(),
                    width, height, width / 2, height / 2, start, itemHeight, elevation[DIALOG] / 4, index);
        });
    }

    /**
     * Show popup window in popup mode
     *
     * @param anchor    View that will be used to calc the position of the window
     * @param container Container view that holds preference list, also used to calc width
     * @param width     Measured width of this window
     */
    private void showPopupMenu(View anchor, View container, final int width, int extraMargin) {
        final boolean rtl = LocaleController.isRTL;

        final int index = Math.max(0, mSelectedIndex);
        final int count = mEntries.length;

        final int anchorTop = anchor.getTop() - container.getPaddingTop();
        final int anchorHeight = anchor.getHeight();
        final int measuredHeight = itemHeight * count + listPadding[POPUP_MENU][VERTICAL] * 2;

        int[] location = new int[2];
        container.getLocationInWindow(location);

        final int containerTopInWindow = location[1] + container.getPaddingTop();
        final int containerHeight = container.getHeight() - container.getPaddingTop() - container.getPaddingBottom();

        int y;

        final int height;
        int elevation = this.elevation[POPUP_MENU];
        final int centerX = rtl
                ? location[0] + extraMargin + listPadding[POPUP_MENU][HORIZONTAL]
                : location[0] + container.getWidth() - width - extraMargin - listPadding[POPUP_MENU][HORIZONTAL];
        final int centerY;
        final int animItemHeight = itemHeight + listPadding[POPUP_MENU][VERTICAL] * 2;
        final Rect animStartRect;

        if (measuredHeight > containerHeight) {
            // too high, use scroll
            y = containerTopInWindow + margin[POPUP_MENU][VERTICAL];

            // scroll to select item
            final int scroll = itemHeight * index
                    - anchorTop + listPadding[POPUP_MENU][VERTICAL] + margin[POPUP_MENU][VERTICAL]
                    - anchorHeight / 2 + itemHeight / 2;

            getContentView().post(() -> {
                SimpleMenuPopupWindow.this.getContentView().scrollBy(0, -measuredHeight); // to top
                SimpleMenuPopupWindow.this.getContentView().scrollBy(0, scroll);
            });
            getContentView().setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

            height = containerHeight - margin[POPUP_MENU][VERTICAL] * 2;

            centerY = itemHeight * index;
        } else {
            // calc align to selected
            y = containerTopInWindow + anchorTop + anchorHeight / 2 - itemHeight / 2
                    - listPadding[POPUP_MENU][VERTICAL] - index * itemHeight;

            // make sure window is in parent view
            int maxY = containerTopInWindow + containerHeight
                    - measuredHeight - margin[POPUP_MENU][VERTICAL];
            y = Math.min(y, maxY);

            int minY = containerTopInWindow + margin[POPUP_MENU][VERTICAL];
            y = Math.max(y, minY);

            getContentView().setOverScrollMode(View.OVER_SCROLL_NEVER);

            height = measuredHeight;

            // center of selected item
            centerY = (int) (listPadding[POPUP_MENU][VERTICAL] + index * itemHeight + itemHeight * 0.5);
        }

        setWidth(width);
        setHeight(height);
        setElevation(elevation);
        setAnimationStyle(R.style.Animation_Preference_SimpleMenuCenter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setEnterTransition(null);
            setExitTransition(null);
        }

        super.showAtLocation(anchor, Gravity.NO_GRAVITY, centerX, y);

        int startTop = centerY - (int) (itemHeight * 0.2);
        int startBottom = centerY + (int) (itemHeight * 0.2);
        int startLeft;
        int startRight;

        if (rtl) {
            startLeft = centerX;
            startRight = centerX + unit;
        } else {
            startLeft = centerX + width - unit;
            startRight = centerX + width;
        }

        animStartRect = new Rect(startLeft, startTop, startRight, startBottom);

        final int animElevation = (int) Math.round(elevation * 0.25);

        getContentView().post(() -> SimpleMenuAnimation.startEnterAnimation(SimpleMenuPopupWindow.this.getBackground(), SimpleMenuPopupWindow.this.getContentView(),
                width, height, centerX, centerY, animStartRect, animItemHeight, animElevation, index));
    }

    /**
     * Measure window width
     *
     * @param maxWidth max width for popup
     * @param entries  Entries of preference hold this window
     * @return 0: skip
     * -1: use dialog
     * other: measuredWidth
     */
    private int measureWidth(int maxWidth, CharSequence[] entries) {
        // skip if should not measure
        if (!mRequestMeasure) {
            return 0;
        }

        mRequestMeasure = false;

        entries = Arrays.copyOf(entries, entries.length);

        Arrays.sort(entries, (o1, o2) -> o2.length() - o1.length());

        Context context = getContentView().getContext();
        int width = 0;

        maxWidth = Math.min(unit * maxUnits, maxWidth);

        Rect bounds = new Rect();

        TextView view = new SimpleMenuItem(context, null);
        Paint textPaint = view.getPaint();

        for (CharSequence chs : entries) {
            textPaint.getTextBounds(chs.toString(), 0, chs.toString().length(), bounds);

            width = Math.max(width, bounds.right + 1 + Math.round(listPadding[POPUP_MENU][HORIZONTAL] * 2 + 1));

            // more than one line should use dialog
            if (width > maxWidth
                    || chs.toString().contains("\n")) {
                return -1;
            }
        }

        // width is a multiple of a unit
        int w = 0;
        while (width > w) {
            w += unit;
        }

        return w;
    }

    @Override
    public void showAtLocation(View parent, int gravity, int x, int y) {
        throw new UnsupportedOperationException("use show(anchor) to show the window");
    }

    @Override
    public void showAsDropDown(View anchor) {
        throw new UnsupportedOperationException("use show(anchor) to show the window");
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff) {
        throw new UnsupportedOperationException("use show(anchor) to show the window");
    }

    @Override
    public void showAsDropDown(View anchor, int xoff, int yoff, int gravity) {
        throw new UnsupportedOperationException("use show(anchor) to show the window");
    }

    public interface OnItemClickListener {
        void onClick(int i);
    }
}
