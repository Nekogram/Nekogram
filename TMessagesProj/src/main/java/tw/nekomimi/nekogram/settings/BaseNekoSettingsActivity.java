package tw.nekomimi.nekogram.settings;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.ViewGroupPartRenderer;
import org.telegram.ui.Components.blur3.capture.IBlur3Capture;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;

import java.util.ArrayList;
import java.util.Locale;

public abstract class BaseNekoSettingsActivity extends BaseFragment {

    protected static final Object PARTIAL = new Object();

    protected SizeNotifierFrameLayout contentView;
    protected UniversalRecyclerView listView;
    protected LinearLayoutManager layoutManager;
    protected Theme.ResourcesProvider resourcesProvider;
    protected View actionBarBackground;
    protected FrameLayout actionBarContainer;
    protected ActionBarMenuItem searchItem;

    protected int rowId = 1;

    public BaseNekoSettingsActivity() {
        this(null);
    }

    public BaseNekoSettingsActivity(Bundle args) {
        super(args);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            scrollableViewNoiseSuppressor = new DownscaleScrollableNoiseSuppressor();
            iBlur3SourceGlassFrosted = new BlurredBackgroundSourceRenderNode(null);
        } else {
            scrollableViewNoiseSuppressor = null;
            iBlur3SourceGlassFrosted = null;
        }
    }

    @Override
    public View createView(Context context) {
        contentView = new SizeNotifierFrameLayout(context) {
            @Override
            protected void dispatchDraw(Canvas canvas) {
                if (Build.VERSION.SDK_INT >= 31 && scrollableViewNoiseSuppressor != null) {
                    blur3_InvalidateBlur();

                    final int width = getMeasuredWidth();
                    final int height = getMeasuredHeight();
                    if (iBlur3SourceGlassFrosted != null && !iBlur3SourceGlassFrosted.inRecording()) {
                        final Canvas c = iBlur3SourceGlassFrosted.beginRecording(width, height);
                        c.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
                        if (SharedConfig.chatBlurEnabled()) {
                            scrollableViewNoiseSuppressor.draw(c, DownscaleScrollableNoiseSuppressor.DRAW_FROSTED_GLASS);
                        }
                        iBlur3SourceGlassFrosted.endRecording();
                    }
                }
                super.dispatchDraw(canvas);
            }

            @Override
            public void drawBlurRect(Canvas canvas, float y, Rect rectTmp, Paint blurScrimPaint, boolean top) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || !SharedConfig.chatBlurEnabled() || iBlur3SourceGlassFrosted == null) {
                    canvas.drawRect(rectTmp, blurScrimPaint);
                    return;
                }

                canvas.save();
                canvas.translate(0, -y);
                iBlur3SourceGlassFrosted.draw(canvas, rectTmp.left, rectTmp.top + y, rectTmp.right, rectTmp.bottom + y);
                canvas.restore();

                final int oldScrimAlpha = blurScrimPaint.getAlpha();
                blurScrimPaint.setAlpha(ChatActivity.ACTION_BAR_BLUR_ALPHA);
                canvas.drawRect(rectTmp, blurScrimPaint);
                blurScrimPaint.setAlpha(oldScrimAlpha);
            }
        };
        contentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));

        listView = new UniversalRecyclerView(this, this::fillItems, this::onItemClick, (item, view, position, x, y) -> {
            if (onItemLongClick(item, view, position, x, y)) {
                return true;
            }
            var slug = item.slug;
            var key = getKey();
            if (key != null && item.enabled && !TextUtils.isEmpty(slug)) {
                ItemOptions.makeOptions(this, view)
                        .setScrimViewBackground(listView.getClipBackground(view))
                        .add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), () -> {
                            if ("copyReportId".equals(slug)) {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/nekosettings/%s", getMessagesController().linkPrefix, "reportId"));
                            } else if ("checkUpdate".equals(slug)) {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/nekosettings/%s", getMessagesController().linkPrefix, "update"));
                            } else {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/nekosettings/%s?r=%s", getMessagesController().linkPrefix, key, slug));
                            }
                            BulletinFactory.of(this).createCopyLinkBulletin().show();
                        })
                        .setMinWidth(190)
                        .show();
                return true;
            }
            return false;
        }) {

            @Override
            public Integer getSelectorColor(int position) {
                return BaseNekoSettingsActivity.this.getSelectorColor(position);
            }
        };
        listView.adapter.setApplyBackground(false);
        listView.setClipToPadding(false);
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && scrollableViewNoiseSuppressor != null) {
                    scrollableViewNoiseSuppressor.onScrolled(dx, dy);
                    blur3_InvalidateBlur();
                }
            }
        });
        iBlur3Capture = new ViewGroupPartRenderer(listView, contentView, listView::drawChild);
        listView.addEdgeEffectListener(() -> listView.postOnAnimation(this::blur3_InvalidateBlur));
        listView.setSections();
        listView.setPadding(0, needActionBarPadding() ? ActionBar.getCurrentActionBarHeight() : AndroidUtilities.dp(12), 0, 0);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        actionBarBackground = new View(context) {
            private final Paint blurScrimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(@NonNull Canvas canvas) {
                var top = actionBarContainer.getHeight();
                AndroidUtilities.rectTmp2.set(0, 0, getMeasuredWidth(), top);
                blurScrimPaint.setColor(Theme.getColor(Theme.key_actionBarDefault, resourceProvider));
                contentView.drawBlurRect(canvas, 0, AndroidUtilities.rectTmp2, blurScrimPaint, true);
                if (getParentLayout() != null) {
                    getParentLayout().drawHeaderShadow(canvas, top);
                }
            }
        };
        contentView.addView(actionBarBackground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.TOP));
        actionBarContainer = new FrameLayout(context);
        actionBarContainer.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.BOTTOM));
        contentView.addView(actionBarContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                updateActionBarVisible();
            }
        });

        updateActionBarVisible(true, false);

        return fragmentView = contentView;
    }

    protected void createSearchItem(ActionBarMenu menu, ActionBarMenuItem.ActionBarMenuItemSearchListener searchListener) {
        searchItem = menu.addItem(0, R.drawable.outline_header_search, resourceProvider).setIsSearchField(true).setActionBarMenuItemSearchListener(searchListener);
        searchItem.setSearchFieldHint(LocaleController.getString(R.string.Search));
        searchItem.setContentDescription(LocaleController.getString(R.string.Search));
    }

    protected boolean isSearchFieldVisible() {
        return searchItem != null && searchItem.isSearchFieldVisible2();
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (isSearchFieldVisible()) {
            if (invoked) actionBar.closeSearchField();
            return false;
        }
        return super.onBackPressed(invoked);
    }

    private boolean actionBarVisible;
    private ValueAnimator actionBarVisibleAnimator;

    protected void updateActionBarVisible() {
        updateActionBarVisible(false, true);
    }

    private void updateActionBarVisible(boolean force, boolean animated) {
        final boolean visible;
        if (isSearchFieldVisible()) {
            visible = true;
        } else if (listView.getChildCount() > 0) {
            var firstChild = listView.getChildAt(0);
            visible = needActionBarPadding() ? listView.canScrollVertically(-1) : (
                    listView.getChildAdapterPosition(firstChild) > 0 ||
                    firstChild.getY() + firstChild.getHeight() < actionBar.getHeight()
            );
        } else {
            visible = false;
        }
        if (actionBarVisible == visible && !force) return;

        actionBarVisible = visible;
        if (actionBarVisibleAnimator != null) {
            actionBarVisibleAnimator.cancel();
            actionBarVisibleAnimator = null;
        }
        if (!animated) {
            if (!needActionBarPadding())
                actionBar.getTitlesContainer().setAlpha(visible ? 1.0f : 0.0f);
            actionBarBackground.setAlpha(visible ? 1.0f : 0.0f);
        } else {
            actionBarVisibleAnimator = ValueAnimator.ofFloat(actionBarBackground.getAlpha(), visible ? 1.0f : 0.0f);
            actionBarVisibleAnimator.addUpdateListener(a -> {
                final float t = (float) a.getAnimatedValue();
                if (!needActionBarPadding()) actionBar.getTitlesContainer().setAlpha(t);
                actionBarBackground.setAlpha(t);
            });
            actionBarVisibleAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            actionBarVisibleAnimator.setDuration(420);
            actionBarVisibleAnimator.start();
        }
    }


    @Override
    public void setParentLayout(INavigationLayout layout) {
        if (layout != null && layout.getLastFragment() != null) {
            resourcesProvider = layout.getLastFragment().getResourceProvider();
        }
        super.setParentLayout(layout);
    }

    @Override
    public ActionBar createActionBar(Context context) {
        var actionBar = super.createActionBar(context);
        actionBar.setBackgroundColor(Color.TRANSPARENT);
        actionBar.setAddToContainer(false);
        actionBar.setUseContainerForTitles();
        actionBar.setOccupyStatusBar(false);
        actionBar.setTitle(getActionBarTitle());
        actionBar.setTitleColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setItemsColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        return actionBar;
    }

    protected String getKey() {
        return null;
    }

    protected abstract void fillItems(ArrayList<UItem> items, UniversalAdapter adapter);

    protected abstract void onItemClick(UItem item, View view, int position, float x, float y);

    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    protected abstract String getActionBarTitle();

    public Integer getSelectorColor(int position) {
        return getThemedColor(Theme.key_listSelector);
    }

    protected boolean needActionBarPadding() {
        return true;
    }

    protected void showRestartBulletin() {
        BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.RestartAppToTakeEffect)).show();
    }

    protected void updateRows() {
        listView.adapter.updateWithoutNotify();
    }

    protected void notifyItemChanged(int itemId) {
        notifyItemChanged(itemId, null);
    }

    protected void notifyItemChanged(int itemId, Object payload) {
        listView.adapter.notifyItemChanged(listView.findPositionByItemId(itemId), payload);
    }

    protected void notifyItemRemoved(int itemId) {
        listView.adapter.notifyItemRemoved(listView.findPositionByItemId(itemId));
    }

    protected void notifyItemInserted(int itemId) {
        listView.adapter.notifyItemInserted(listView.findPositionByItemId(itemId));
    }

    protected void notifyItemRangeRemoved(int itemId, int itemCount) {
        listView.adapter.notifyItemRangeRemoved(listView.findPositionByItemId(itemId), itemCount);
    }

    protected void notifyItemRangeInserted(int itemId, int itemCount) {
        listView.adapter.notifyItemRangeInserted(listView.findPositionByItemId(itemId), itemCount);
    }

    public static CharSequence getSpannedString(int id, String url) {
        var text = LocaleController.getString(id);
        var builder = new SpannableStringBuilder(text);
        int index1 = text.indexOf("**");
        int index2 = text.lastIndexOf("**");
        if (index1 >= 0 && index2 >= 0 && index1 != index2) {
            builder.replace(index2, index2 + 2, "");
            builder.replace(index1, index1 + 2, "");
            builder.setSpan(new URLSpanNoUnderline(url), index1, index2 - 2, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return builder;
    }

    public void scrollToRow(String key, Runnable unknown) {
        if (listView == null) return;
        var position = listView.findPositionByItemSlug(key);
        if (position != -1) {
            var layoutManager = (LinearLayoutManager) listView.getLayoutManager();
            layoutManager.scrollToPositionWithOffset(position, AndroidUtilities.dp(60));
            listView.post(() -> {
                if (listView == null) return;
                var currentPosition = listView.findPositionByItemSlug(key);
                if (currentPosition != -1) {
                    listView.highlightRow(() -> currentPosition);
                } else {
                    unknown.run();
                }
            });
        } else {
            unknown.run();
        }
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        var topPadding = needActionBarPadding() ? ActionBar.getCurrentActionBarHeight() : AndroidUtilities.dp(12);
        listView.setPadding(0, top + topPadding, 0, bottom);
        actionBarContainer.setPadding(0, top, 0, 0);
        super.onInsets(left, top, right, bottom);
    }

    /* Blur */

    private final @Nullable DownscaleScrollableNoiseSuppressor scrollableViewNoiseSuppressor;
    private final @Nullable BlurredBackgroundSourceRenderNode iBlur3SourceGlassFrosted;

    private IBlur3Capture iBlur3Capture;

    private final ArrayList<RectF> iBlur3Positions = new ArrayList<>();
    private final RectF iBlur3PositionActionBar = new RectF();

    {
        iBlur3Positions.add(iBlur3PositionActionBar);
    }

    private void blur3_InvalidateBlur() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || scrollableViewNoiseSuppressor == null) {
            return;
        }

        final int additionalList = AndroidUtilities.dp(48);
        iBlur3PositionActionBar.set(0, -additionalList, fragmentView.getMeasuredWidth(), actionBarContainer.getMeasuredHeight() + additionalList);

        scrollableViewNoiseSuppressor.setupRenderNodes(iBlur3Positions, 1);
        scrollableViewNoiseSuppressor.invalidateResultRenderNodes(iBlur3Capture, fragmentView.getMeasuredWidth(), fragmentView.getMeasuredHeight());
    }

    protected static class TextSettingsCellFactory extends UItem.UItemFactory<TextSettingsCell> {
        static {
            setup(new TextSettingsCellFactory());
        }

        private Theme.ResourcesProvider resourcesProvider;

        @Override
        public TextSettingsCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            this.resourcesProvider = resourcesProvider;
            return new TextSettingsCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var textCell = (TextSettingsCell) view;
            textCell.setCanDisable(true);
            if (!TextUtils.isEmpty(item.textValue)) {
                var valueText = textCell.getValueTextView().getText();
                var animated = !TextUtils.isEmpty(valueText) && !item.textValue.equals(valueText);
                textCell.setTextAndValue(item.text, item.textValue, animated, divider);
            } else {
                textCell.setText(item.text, divider);
            }
            if (item.red) {
                textCell.setTextColor(Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
            } else if (item.accent) {
                textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText2, resourcesProvider));
            } else {
                textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            }
        }

        public static UItem of(int id, CharSequence title) {
            return of(id, title, null);
        }

        public static UItem of(int id, CharSequence title, CharSequence value) {
            var item = UItem.ofFactory(TextSettingsCellFactory.class);
            item.id = id;
            item.text = title;
            item.textValue = value;
            return item;
        }
    }

    protected static class TextDetailSettingsCellFactory extends UItem.UItemFactory<TextDetailSettingsCell> {
        static {
            setup(new TextDetailSettingsCellFactory());
        }

        @Override
        public TextDetailSettingsCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new TextDetailSettingsCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var textCell = (TextDetailSettingsCell) view;
            textCell.setMultilineDetail(true);
            textCell.setTextAndValue(item.text, item.subtext, divider);
            textCell.setEnabled(item.enabled);
        }

        public static UItem of(int id, CharSequence title, CharSequence subtitle) {
            var item = UItem.ofFactory(TextDetailSettingsCellFactory.class);
            item.id = id;
            item.text = title;
            item.subtext = subtitle;
            return item;
        }
    }

    protected static class TextCheckbox2CellFactory extends UItem.UItemFactory<TextCheckbox2Cell> {
        static {
            setup(new TextCheckbox2CellFactory());
        }

        @Override
        public TextCheckbox2Cell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new TextCheckbox2Cell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var textCell = (TextCheckbox2Cell) view;
            if (TextUtils.isEmpty(item.subtext)) {
                textCell.setTextAndCheck(item.text, item.checked, divider);
            } else {
                textCell.setTextAndValueAndCheck(item.text, item.subtext, item.checked, true, divider);
            }
        }

        public static UItem of(int id, CharSequence title) {
            var item = UItem.ofFactory(TextCheckbox2CellFactory.class);
            item.id = id;
            item.text = title;
            return item;
        }
    }
}
