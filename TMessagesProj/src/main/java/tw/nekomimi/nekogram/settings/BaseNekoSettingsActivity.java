package tw.nekomimi.nekogram.settings;

import android.animation.ValueAnimator;
import android.content.Context;
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
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.SimpleActionBarMenu;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeBind;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.utils.glass.BaseFragmentSimpleGlass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

import tw.nekomimi.nekogram.helpers.PopupHelper;

public abstract class BaseNekoSettingsActivity extends BaseFragment {

    protected static final Object PARTIAL = new Object();

    protected FrameLayout contentView;
    protected UniversalRecyclerView listView;
    protected LinearLayoutManager layoutManager;
    protected Theme.ResourcesProvider resourcesProvider;
    private View actionBarFadeView;
    protected ActionBarMenuItem searchItem;

    protected int rowId = 1;

    @Override
    protected boolean useSimpleActionBar() {
        return true;
    }

    @Override
    public View createView(Context context) {
        setHasOwnBackground(true);
        contentView = new FrameLayout(context);

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
        listView.setCaptureSectionsDecoratorAllowed(true);
        listView.adapter.setApplyBackground(false);
        listView.setClipToPadding(false);
        listView.setSections();
        listView.setSectionsDrawBackground(true);
        contentView.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.FILL));

        simpleActionBar.setTitle(getActionBarTitle());
        simpleActionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        simpleActionBar.setBackButtonImage(R.drawable.ic_ab_back);
        if (!needActionBarPadding()) {
            actionBarFadeView = new View(context);
            actionBarFadeView.setBackground(getBaseSimpleGlass().createActionBarFadeDrawable(actionBarFadeView));
            contentView.addView(actionBarFadeView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 0, Gravity.TOP));
            contentView.addView(simpleActionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));

            listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    updateActionBarVisible();
                }
            });

            updateActionBarVisible(true, false);
        } else {
            contentView.addView(simpleActionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));
        }

        return fragmentView = contentView;
    }

    protected void createSearchItem(SimpleActionBarMenu menu, ActionBarMenuItem.ActionBarMenuItemSearchListener searchListener) {
        searchItem = menu.addItem(0, R.drawable.outline_header_search, resourcesProvider).setIsSearchField(true).setActionBarMenuItemSearchListener(searchListener);
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
            visible = listView.getChildAdapterPosition(firstChild) > 0 ||
                    firstChild.getY() + firstChild.getHeight() < simpleActionBar.getHeight();
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
            setActionBarVisibilityFactor(visible ? 1f : 0f);
        } else {
            actionBarVisibleAnimator = ValueAnimator.ofFloat(simpleActionBar.getTitlesAlpha(), visible ? 1.0f : 0.0f);
            actionBarVisibleAnimator.addUpdateListener(a -> {
                final float t = (float) a.getAnimatedValue();
                setActionBarVisibilityFactor(t);
            });
            actionBarVisibleAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            actionBarVisibleAnimator.setDuration(420);
            actionBarVisibleAnimator.start();
        }
    }

    private void setActionBarVisibilityFactor(float factor) {
        simpleActionBar.setTitlesAlpha(factor);
        actionBarFadeView.setAlpha(factor);
    }

    private void updateActionBarFadeHeight(int insetTop) {
        var height = insetTop + ActionBar.getCurrentActionBarHeight();
        var params = actionBarFadeView.getLayoutParams();
        if (params.height != height) {
            params.height = height;
            actionBarFadeView.setLayoutParams(params);
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
    public Theme.ResourcesProvider getResourceProvider() {
        return resourcesProvider;
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

    public void scrollToRow(String key, Runnable unknown) {
        if (listView == null) return;
        var position = listView.findPositionByItemSlug(key);
        if (position != -1) {
            listView.highlightRow(() -> {
                var layoutManager = (LinearLayoutManager) listView.getLayoutManager();
                layoutManager.scrollToPositionWithOffset(position, AndroidUtilities.dp(60));
                return position;
            });
        } else {
            if (unknown != null) unknown.run();
        }
    }

    public void showPopup(List<? extends CharSequence> entries, int checkedIndex, UItem item, View itemView, IntConsumer listener) {
        PopupHelper.show(entries, null, checkedIndex, this, itemView, i -> {
            item.textValue = entries.get(i);
            listener.accept(i);
        });
    }

    @Override
    public boolean isSupportEdgeToEdge() {
        return true;
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        if (!needActionBarPadding()) {
            updateActionBarFadeHeight(top);
            BaseFragmentSimpleGlass.setListViewParams(listView, top, bottom, AndroidUtilities.dp(12), 0);
        }
    }

    @Override
    protected boolean useFadeDrawableForActionBar() {
        return needActionBarPadding();
    }

    @Nullable
    @Override
    protected RecyclerListView getListViewForSimpleGlass() {
        return listView;
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
            var textView = textCell.getTextView();
            ThemeBind.clear(textView);
            if (item.red) {
                ThemeBind.textColor(textView, Theme.key_text_RedRegular, resourcesProvider);
            } else if (item.accent) {
                ThemeBind.textColor(textView, Theme.key_windowBackgroundWhiteBlueText2, resourcesProvider);
            } else {
                ThemeBind.textColor(textView, Theme.key_windowBackgroundWhiteBlackText, resourcesProvider);
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
