package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.app.assist.AssistContent;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.HashBiMap;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.INavigationLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.CreationTextCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BlurredRecyclerView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FlickerLoadingView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.ArrayList;
import java.util.Locale;

public abstract class BaseNekoSettingsActivity extends BaseFragment {

    protected static final Object PARTIAL = new Object();

    public static final int TYPE_SHADOW = 1;
    public static final int TYPE_SETTINGS = 2;
    public static final int TYPE_CHECK = 3;
    public static final int TYPE_HEADER = 4;
    public static final int TYPE_NOTIFICATION_CHECK = 5;
    public static final int TYPE_DETAIL_SETTINGS = 6;
    public static final int TYPE_INFO_PRIVACY = 7;
    public static final int TYPE_TEXT = 8;
    public static final int TYPE_CHECKBOX = 9;
    public static final int TYPE_RADIO = 10;
    public static final int TYPE_ACCOUNT = 11;
    public static final int TYPE_EMOJI = 12;
    public static final int TYPE_EMOJI_SELECTION = 13;
    public static final int TYPE_CREATION = 14;
    public static final int TYPE_FLICKER = 15;

    protected BlurredRecyclerView listView;
    protected BaseListAdapter listAdapter;
    protected LinearLayoutManager layoutManager;
    protected Theme.ResourcesProvider resourcesProvider;

    protected int rowCount;
    protected HashBiMap<String, Integer> rowMap = HashBiMap.create(20);

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @Override
    public View createView(Context context) {
        fragmentView = new BlurContentView(context);
        fragmentView.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundGray));
        SizeNotifierFrameLayout frameLayout = (SizeNotifierFrameLayout) fragmentView;

        actionBar.setDrawBlurBackground(frameLayout);

        listView = new BlurredRecyclerView(context, resourcesProvider) {
            @Override
            public Integer getSelectorColor(int position) {
                return BaseNekoSettingsActivity.this.getSelectorColor(position);
            }
        };
        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                frameLayout.invalidateBlur();
            }
        });
        listView.additionalClipBottom = AndroidUtilities.dp(200);
        listView.setLayoutManager(layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        itemAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
        itemAnimator.setDelayAnimations(false);
        listView.setItemAnimator(itemAnimator);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listAdapter = createAdapter(context);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(this::onItemClick);
        listView.setOnItemLongClickListener((view, position, x, y) -> {
            if (onItemLongClick(view, position, x, y)) {
                return true;
            }
            var holder = listView.findViewHolderForAdapterPosition(position);
            var key = getKey();
            if (key != null && holder != null && listAdapter.isEnabled(holder) && rowMap.inverse().containsKey(position)) {
                ItemOptions.makeOptions(this, view)
                        .setScrimViewBackground(new ColorDrawable(getThemedColor(Theme.key_windowBackgroundWhite)))
                        .add(R.drawable.msg_copy, LocaleController.getString(R.string.CopyLink), () -> {
                            var rowKey = rowMap.inverse().get(position);
                            if ("copyReportId".equals(rowKey)) {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/nekosettings/%s", getMessagesController().linkPrefix, "reportId"));
                            } else if ("checkUpdate".equals(rowKey)) {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/nekosettings/%s", getMessagesController().linkPrefix, "update"));
                            } else {
                                AndroidUtilities.addToClipboard(String.format(Locale.getDefault(), "https://%s/nekosettings/%s?r=%s", getMessagesController().linkPrefix, getKey(), rowKey));
                            }
                            BulletinFactory.of(BaseNekoSettingsActivity.this).createCopyLinkBulletin().show();
                        })
                        .setMinWidth(190)
                        .show();
                return true;
            }
            return false;
        });
        return fragmentView;
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
        ActionBar actionBar;
        if (!hasWhiteActionBar()) {
            actionBar = super.createActionBar(context);
        } else {
            actionBar = new ActionBar(context);
            actionBar.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            actionBar.setItemsColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
            actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarActionModeDefaultSelector), true);
            actionBar.setItemsBackgroundColor(getThemedColor(Theme.key_actionBarWhiteSelector), false);
            actionBar.setItemsColor(getThemedColor(Theme.key_actionBarActionModeDefaultIcon), true);
            actionBar.setTitleColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
            actionBar.setCastShadows(false);
        }
        actionBar.setTitle(getActionBarTitle());
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        return actionBar;
    }

    protected String getKey() {
        return null;
    }

    protected abstract void onItemClick(View view, int position, float x, float y);

    protected boolean onItemLongClick(View view, int position, float x, float y) {
        return false;
    }

    protected abstract BaseListAdapter createAdapter(Context context);

    protected abstract String getActionBarTitle();

    public Integer getSelectorColor(int position) {
        return getThemedColor(Theme.key_listSelector);
    }

    protected void showRestartBulletin() {
        BulletinFactory.of(this).createErrorBulletin(LocaleController.formatString(R.string.RestartAppToTakeEffect)).show();
    }

    private class BlurContentView extends SizeNotifierFrameLayout {

        public BlurContentView(Context context) {
            super(context);
            needBlur = hasWhiteActionBar();
            blurBehindViews.add(this);
        }

        @Override
        protected void drawList(Canvas blurCanvas, boolean top, ArrayList<IViewWithInvalidateCallback> views) {
            super.drawList(blurCanvas, top, views);
            for (int j = 0; j < listView.getChildCount(); j++) {
                View child = listView.getChildAt(j);
                if (child.getY() < listView.blurTopPadding + AndroidUtilities.dp(100)) {
                    int restore = blurCanvas.save();
                    blurCanvas.translate(getX() + child.getX(), getY() + listView.getY() + child.getY());
                    child.draw(blurCanvas);
                    if (views != null && child instanceof SizeNotifierFrameLayout.IViewWithInvalidateCallback) {
                        views.add((SizeNotifierFrameLayout.IViewWithInvalidateCallback) child);
                    }
                    blurCanvas.restoreToCount(restore);
                }
            }
        }

        boolean divider = false;
        Paint paint = new Paint();
        Rect rectTmp = new Rect();

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (divider) {
                rectTmp.set(0, 0, getMeasuredWidth(), 1);
                paint.setColor(getThemedColor(Theme.key_divider));
                drawBlurRect(canvas, getY(), rectTmp, paint, true);
            }
        }

        @Override
        public void invalidateBlur() {
            if (hasWhiteActionBar()) {
                boolean divider = listView.canScrollVertically(-1);
                if (divider != this.divider) {
                    this.divider = divider;
                    if (!SharedConfig.chatBlurEnabled()) {
                        invalidate();
                    }
                }
                super.invalidateBlur();
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    protected boolean hasWhiteActionBar() {
        return true;
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

    @Override
    public boolean isLightStatusBar() {
        if (!hasWhiteActionBar()) return super.isLightStatusBar();
        int color = getThemedColor(Theme.key_windowBackgroundWhite);
        return ColorUtils.calculateLuminance(color) > 0.7f;
    }

    protected int addRow() {
        return rowCount++;
    }

    // TODO: refactor the whole settings
    protected int addRow(String... keys) {
        var row = rowCount++;
        for (var key : keys) {
            rowMap.put(key, row);
        }
        return row;
    }

    public void scrollToRow(String key, Runnable unknown) {
        if (rowMap.containsKey(key)) {
            listView.highlightRow(() -> {
                //noinspection ConstantConditions
                int position = rowMap.get(key);
                layoutManager.scrollToPositionWithOffset(position, AndroidUtilities.dp(60));
                return position;
            });
        } else {
            unknown.run();
        }
    }

    protected void updateRows() {
        rowCount = 0;
        rowMap.clear();
    }

    protected abstract class BaseListAdapter extends RecyclerListView.SelectionAdapter {

        protected final Context mContext;

        public BaseListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == TYPE_SETTINGS || type == TYPE_CHECK || type == TYPE_NOTIFICATION_CHECK || type == TYPE_DETAIL_SETTINGS || type == TYPE_TEXT | type == TYPE_CHECKBOX || type == TYPE_RADIO || type == TYPE_ACCOUNT || type == TYPE_EMOJI || type == TYPE_EMOJI_SELECTION || type == TYPE_CREATION;
        }

        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {

        }

        @Override
        public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            var partial = PARTIAL.equals(holder.getPayload());
            var top = position > 0;
            var bottom = position < getItemCount() - 1;
            var type = holder.getItemViewType();
            var nextType = position < getItemCount() - 1 ? getItemViewType(position + 1) : -1;
            var divider = nextType != -1 && nextType != TYPE_SHADOW && nextType != TYPE_INFO_PRIVACY;
            if (type == TYPE_SHADOW) {
                ShadowSectionCell shadowCell = (ShadowSectionCell) holder.itemView;
                shadowCell.setTopBottom(top, bottom);
                return;
            } else if (type == TYPE_INFO_PRIVACY) {
                TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                int drawable;
                if (top && bottom) {
                    drawable = R.drawable.greydivider;
                } else if (top) {
                    drawable = R.drawable.greydivider_bottom;
                } else if (bottom) {
                    drawable = R.drawable.greydivider_top;
                } else {
                    drawable = R.drawable.transparent;
                }
                cell.setBackground(Theme.getThemedDrawable(mContext, drawable, getThemedColor(Theme.key_windowBackgroundGrayShadow)));
            }
            onBindViewHolder(holder, position, partial, divider);
        }

        public View createCustomView(int viewType) {
            return null;
        }

        @NonNull
        @Override
        public final RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = switch (viewType) {
                case TYPE_SHADOW -> new ShadowSectionCell(mContext, resourcesProvider);
                case TYPE_SETTINGS -> new TextSettingsCell(mContext, resourcesProvider);
                case TYPE_CHECK -> new TextCheckCell(mContext, resourcesProvider);
                case TYPE_HEADER -> new HeaderCell(mContext, resourcesProvider);
                case TYPE_NOTIFICATION_CHECK ->
                        new NotificationsCheckCell(mContext, resourcesProvider);
                case TYPE_DETAIL_SETTINGS ->
                        new TextDetailSettingsCell(mContext, resourcesProvider);
                case TYPE_INFO_PRIVACY -> new TextInfoPrivacyCell(mContext, resourcesProvider);
                case TYPE_TEXT -> new TextCell(mContext, resourcesProvider);
                case TYPE_CHECKBOX -> new TextCheckbox2Cell(mContext, resourcesProvider);
                case TYPE_RADIO -> new TextRadioCell(mContext, resourcesProvider);
                case TYPE_ACCOUNT -> new AccountCell(mContext, resourcesProvider);
                case TYPE_EMOJI, TYPE_EMOJI_SELECTION ->
                        new EmojiSetCell(mContext, viewType == TYPE_EMOJI_SELECTION, resourcesProvider);
                case TYPE_CREATION -> new CreationTextCell(mContext, 71, resourcesProvider);
                case TYPE_FLICKER -> new FlickerLoadingView(mContext, resourcesProvider);
                default -> createCustomView(viewType);
            };
            if (viewType != TYPE_SHADOW && viewType != TYPE_INFO_PRIVACY) {
                view.setBackgroundColor(getThemedColor(Theme.key_windowBackgroundWhite));
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }
    }

    @Override
    public Theme.ResourcesProvider getResourceProvider() {
        return resourcesProvider;
    }

    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outContent.setWebUri(Uri.parse("https://nekogram.app"));
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate blurDelegate = () -> {
            if (fragmentView instanceof BlurContentView) {
                ((BlurContentView) fragmentView).invalidateBlurredViews();
            }
        };
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, NotificationsCheckCell.class, TextDetailSettingsCell.class, TextCell.class, TextCheckbox2Cell.class, TextRadioCell.class, AccountCell.class, EmojiSetCell.class, CreationTextCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        if (hasWhiteActionBar()) {
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundWhite));
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarWhiteSelector));
        } else {
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
            themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        }

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{TextInfoPrivacyCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText4));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LINKCOLOR, new Class[]{TextInfoPrivacyCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayIcon));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckbox2Cell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckbox2Cell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckbox2Cell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_radioBackgroundChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckbox2Cell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxDisabled));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckbox2Cell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextRadioCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextRadioCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextRadioCell.class}, new String[]{"radioButton"}, null, null, null, Theme.key_radioBackground));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextRadioCell.class}, new String[]{"radioButton"}, null, null, null, Theme.key_radioBackgroundChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AccountCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{AccountCell.class}, new String[]{"checkImageView"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{EmojiSetCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{EmojiSetCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_USEBACKGROUNDDRAWABLE | ThemeDescription.FLAG_DRAWABLESELECTEDSTATE, new Class[]{EmojiSetCell.class}, new String[]{"optionsButton"}, null, null, null, Theme.key_stickers_menuSelector));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{EmojiSetCell.class}, new String[]{"optionsButton"}, null, null, null, Theme.key_stickers_menu));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKBOX, new Class[]{EmojiSetCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{EmojiSetCell.class}, new String[]{"progressBar"}, null, null, null, Theme.key_featuredStickers_addedIcon));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CHECKBOXCHECK, new Class[]{EmojiSetCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_checkboxCheck));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{CreationTextCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueText2));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{CreationTextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_switchTrackChecked));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{CreationTextCell.class}, new String[]{"imageView"}, null, null, null, Theme.key_checkboxCheck));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(null, 0, null, null, null, blurDelegate, Theme.key_chat_BlurAlpha));
        return themeDescriptions;
    }
}
