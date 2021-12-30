package tw.nekomimi.nekogram.settings;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.text.TextPaint;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;

@SuppressLint({"RtlHardcoded", "NotifyDataSetChanged"})
public class NekoChatSettingsActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private ActionBarMenuItem resetItem;
    private StickerSizeCell stickerSizeCell;

    private int rowCount;

    private int stickerSizeHeaderRow;
    private int stickerSizeRow;
    private int stickerSize2Row;

    private int chatRow;
    private int ignoreBlockedRow;
    private int disablePhotoSideActionRow;
    private int hideKeyboardOnChatScrollRow;
    private int rearVideoMessagesRow;
    private int confirmAVRow;
    private int tryToOpenAllLinksInIVRow;
    private int disableProximityEventsRow;
    private int swipeToPiPRow;
    private int disableJumpToNextRow;
    private int disableGreetingStickerRow;
    private int disableVoiceMessageAutoPlayRow;
    private int autoPauseVideoRow;
    private int messageMenuRow;
    private int chat2Row;

    private int foldersRow;
    private int showTabsOnForwardRow;
    private int hideAllTabRow;
    private int tabsTitleTypeRow;
    private int folders2Row;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        updateRows();

        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("Chat", R.string.Chat));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }

        ActionBarMenu menu = actionBar.createMenu();
        resetItem = menu.addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString("ResetStickerSize", R.string.ResetStickerSize));
        resetItem.setVisibility(NekoConfig.stickerSize != 14.0f ? View.VISIBLE : View.GONE);
        resetItem.setTag(null);
        resetItem.setOnClickListener(v -> {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
            ValueAnimator animator = ValueAnimator.ofFloat(NekoConfig.stickerSize, 14.0f);
            animator.setDuration(150);
            animator.addUpdateListener(valueAnimator -> {
                NekoConfig.setStickerSize((Float) valueAnimator.getAnimatedValue());
                stickerSizeCell.invalidate();
            });
            animator.start();
        });

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == ignoreBlockedRow) {
                NekoConfig.toggleIgnoreBlocked();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.ignoreBlocked);
                }
            } else if (position == messageMenuRow) {
                showMessageMenuAlert();
            } else if (position == disablePhotoSideActionRow) {
                NekoConfig.toggleDisablePhotoSideAction();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disablePhotoSideAction);
                }
            } else if (position == hideKeyboardOnChatScrollRow) {
                NekoConfig.toggleHideKeyboardOnChatScroll();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideKeyboardOnChatScroll);
                }
            } else if (position == showTabsOnForwardRow) {
                NekoConfig.toggleShowTabsOnForward();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.showTabsOnForward);
                }
            } else if (position == rearVideoMessagesRow) {
                NekoConfig.toggleRearVideoMessages();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.rearVideoMessages);
                }
            } else if (position == hideAllTabRow) {
                NekoConfig.toggleHideAllTab();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideAllTab);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == tabsTitleTypeRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText));
                types.add(NekoConfig.TITLE_TYPE_TEXT);
                arrayList.add(LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon));
                types.add(NekoConfig.TITLE_TYPE_ICON);
                arrayList.add(LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix));
                types.add(NekoConfig.TITLE_TYPE_MIX);
                PopupHelper.show(arrayList, LocaleController.getString("TabTitleType", R.string.TabTitleType), types.indexOf(NekoConfig.tabsTitleType), context, view, i -> {
                    NekoConfig.setTabsTitleType(types.get(i));
                    listAdapter.notifyItemChanged(tabsTitleTypeRow);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                });
            } else if (position == confirmAVRow) {
                NekoConfig.toggleConfirmAVMessage();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.confirmAVMessage);
                }
            } else if (position == disableProximityEventsRow) {
                NekoConfig.toggleDisableProximityEvents();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableProximityEvents);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.formatString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect)).show();
            } else if (position == tryToOpenAllLinksInIVRow) {
                NekoConfig.toggleTryToOpenAllLinksInIV();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.tryToOpenAllLinksInIV);
                }
            } else if (position == swipeToPiPRow) {
                NekoConfig.toggleSwipeToPiP();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.swipeToPiP);
                }
            } else if (position == autoPauseVideoRow) {
                NekoConfig.toggleAutoPauseVideo();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.autoPauseVideo);
                }
            } else if (position == disableJumpToNextRow) {
                NekoConfig.toggleDisableJumpToNextChannel();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableJumpToNextChannel);
                }
            } else if (position == disableGreetingStickerRow) {
                NekoConfig.toggleDisableGreetingSticker();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableGreetingSticker);
                }
            } else if (position == disableVoiceMessageAutoPlayRow) {
                NekoConfig.toggleDisableVoiceMessageAutoPlay();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableVoiceMessageAutoPlay);
                }
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;
        stickerSizeHeaderRow = rowCount++;
        stickerSizeRow = rowCount++;
        stickerSize2Row = rowCount++;

        chatRow = rowCount++;
        ignoreBlockedRow = rowCount++;
        disablePhotoSideActionRow = rowCount++;
        hideKeyboardOnChatScrollRow = rowCount++;
        rearVideoMessagesRow = rowCount++;
        confirmAVRow = rowCount++;
        tryToOpenAllLinksInIVRow = rowCount++;
        disableProximityEventsRow = rowCount++;
        swipeToPiPRow = rowCount++;
        disableJumpToNextRow = rowCount++;
        disableGreetingStickerRow = rowCount++;
        disableVoiceMessageAutoPlayRow = rowCount++;
        autoPauseVideoRow = rowCount++;
        messageMenuRow = rowCount++;
        chat2Row = rowCount++;

        foldersRow = rowCount++;
        showTabsOnForwardRow = rowCount++;
        hideAllTabRow = rowCount++;
        tabsTitleTypeRow = rowCount++;
        folders2Row = rowCount++;

        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));

        return themeDescriptions;
    }

    private void showMessageMenuAlert() {
        if (getParentActivity() == null) {
            return;
        }
        Context context = getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("MessageMenu", R.string.MessageMenu));

        LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        LinearLayout linearLayoutInviteContainer = new LinearLayout(context);
        linearLayoutInviteContainer.setOrientation(LinearLayout.VERTICAL);
        linearLayout.addView(linearLayoutInviteContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        int count = 11;
        for (int a = 0; a < count; a++) {
            TextCheckCell textCell = new TextCheckCell(context);
            switch (a) {
                case 0: {
                    textCell.setTextAndCheck(LocaleController.getString("DeleteDownloadedFile", R.string.DeleteDownloadedFile), NekoConfig.showDeleteDownloadedFile, false);
                    break;
                }
                case 1: {
                    textCell.setTextAndCheck(LocaleController.getString("NoQuoteForward", R.string.NoQuoteForward), NekoConfig.showNoQuoteForward, false);
                    break;
                }
                case 2: {
                    textCell.setTextAndCheck(LocaleController.getString("AddToSavedMessages", R.string.AddToSavedMessages), NekoConfig.showAddToSavedMessages, false);
                    break;
                }
                case 3: {
                    textCell.setTextAndCheck(LocaleController.getString("Repeat", R.string.Repeat), NekoConfig.showRepeat, false);
                    break;
                }
                case 4: {
                    textCell.setTextAndCheck(LocaleController.getString("Prpr", R.string.Prpr), NekoConfig.showPrPr, false);
                    break;
                }
                case 5: {
                    textCell.setTextAndCheck(LocaleController.getString("ViewHistory", R.string.ViewHistory), NekoConfig.showViewHistory, false);
                    break;
                }
                case 6: {
                    textCell.setTextAndCheck(LocaleController.getString("TranslateMessage", R.string.TranslateMessage), NekoConfig.showTranslate, false);
                    break;
                }
                case 7: {
                    textCell.setTextAndCheck(LocaleController.getString("ReportChat", R.string.ReportChat), NekoConfig.showReport, false);
                    break;
                }
                case 8: {
                    textCell.setTextAndCheck(LocaleController.getString("EditAdminRights", R.string.EditAdminRights), NekoConfig.showAdminActions, false);
                    break;
                }
                case 9: {
                    textCell.setTextAndCheck(LocaleController.getString("ChangePermissions", R.string.ChangePermissions), NekoConfig.showChangePermissions, false);
                    break;
                }
                case 10: {
                    textCell.setTextAndCheck(LocaleController.getString("MessageDetails", R.string.MessageDetails), NekoConfig.showMessageDetails, false);
                    break;
                }
            }
            textCell.setTag(a);
            textCell.setBackground(Theme.getSelectorDrawable(false));
            linearLayoutInviteContainer.addView(textCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            textCell.setOnClickListener(v2 -> {
                Integer tag = (Integer) v2.getTag();
                switch (tag) {
                    case 0: {
                        NekoConfig.toggleShowDeleteDownloadedFile();
                        textCell.setChecked(NekoConfig.showDeleteDownloadedFile);
                        break;
                    }
                    case 1: {
                        NekoConfig.toggleShowNoQuoteForward();
                        textCell.setChecked(NekoConfig.showNoQuoteForward);
                        break;
                    }
                    case 2: {
                        NekoConfig.toggleShowAddToSavedMessages();
                        textCell.setChecked(NekoConfig.showAddToSavedMessages);
                        break;
                    }
                    case 3: {
                        NekoConfig.toggleShowRepeat();
                        textCell.setChecked(NekoConfig.showRepeat);
                        break;
                    }
                    case 4: {
                        NekoConfig.toggleShowPrPr();
                        textCell.setChecked(NekoConfig.showPrPr);
                        break;
                    }
                    case 5: {
                        NekoConfig.toggleShowViewHistory();
                        textCell.setChecked(NekoConfig.showViewHistory);
                        break;
                    }
                    case 6: {
                        NekoConfig.toggleShowTranslate();
                        textCell.setChecked(NekoConfig.showTranslate);
                        break;
                    }
                    case 7: {
                        NekoConfig.toggleShowReport();
                        textCell.setChecked(NekoConfig.showReport);
                        break;
                    }
                    case 8: {
                        NekoConfig.toggleShowAdminActions();
                        textCell.setChecked(NekoConfig.showAdminActions);
                        break;
                    }
                    case 9: {
                        NekoConfig.toggleShowChangePermissions();
                        textCell.setChecked(NekoConfig.showChangePermissions);
                        break;
                    }
                    case 10: {
                        NekoConfig.toggleShowMessageDetails();
                        textCell.setChecked(NekoConfig.showMessageDetails);
                        break;
                    }
                }
            });
        }
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.setView(linearLayout);
        showDialog(builder.create());
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.emojiLoaded) {
            if (listView != null) {
                listView.invalidateViews();
            }
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
    }

    private class StickerSizeCell extends FrameLayout {

        private final StickerSizePreviewMessagesCell messagesCell;
        private final SeekBarView sizeBar;
        private final int startStickerSize = 2;
        private final int endStickerSize = 20;

        private final TextPaint textPaint;
        private int lastWidth;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
            textPaint.setTextSize(AndroidUtilities.dp(16));

            sizeBar = new SeekBarView(context);
            sizeBar.setReportChanges(true);
            sizeBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
                @Override
                public void onSeekBarDrag(boolean stop, float progress) {
                    sizeBar.getSeekBarAccessibilityDelegate().postAccessibilityEventRunnable(StickerSizeCell.this);
                    NekoConfig.setStickerSize(startStickerSize + (endStickerSize - startStickerSize) * progress);
                    StickerSizeCell.this.invalidate();
                    if (resetItem.getVisibility() != VISIBLE) {
                        AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
                    }
                }

                @Override
                public void onSeekBarPressed(boolean pressed) {

                }
            });
            sizeBar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38, Gravity.LEFT | Gravity.TOP, 9, 5, 43, 11));

            messagesCell = new StickerSizePreviewMessagesCell(context, parentLayout);
            messagesCell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 53, 0, 0));
        }

        @Override
        protected void onDraw(Canvas canvas) {
            textPaint.setColor(Theme.getColor(Theme.key_windowBackgroundWhiteValueText));
            canvas.drawText(String.valueOf(Math.round(NekoConfig.stickerSize)), getMeasuredWidth() - AndroidUtilities.dp(39), AndroidUtilities.dp(28), textPaint);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            int width = MeasureSpec.getSize(widthMeasureSpec);
            if (lastWidth != width) {
                sizeBar.setProgress((NekoConfig.stickerSize - startStickerSize) / (float) (endStickerSize - startStickerSize));
                lastWidth = width;
            }
        }

        @Override
        public void invalidate() {
            super.invalidate();
            lastWidth = -1;
            messagesCell.invalidate();
            sizeBar.invalidate();
        }

        @Override
        public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
            super.onInitializeAccessibilityEvent(event);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityEvent(this, event);
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
            super.onInitializeAccessibilityNodeInfo(info);
            sizeBar.getSeekBarAccessibilityDelegate().onInitializeAccessibilityNodeInfoInternal(this, info);
        }

        @Override
        public boolean performAccessibilityAction(int action, Bundle arguments) {
            return super.performAccessibilityAction(action, arguments) || sizeBar.getSeekBarAccessibilityDelegate().performAccessibilityActionInternal(this, action, arguments);
        }
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == stickerSizeRow) {
                        textCell.setTextAndValue(LocaleController.getString("StickerSize", R.string.StickerSize), String.valueOf(Math.round(NekoConfig.stickerSize)), true);
                    } else if (position == messageMenuRow) {
                        textCell.setText(LocaleController.getString("MessageMenu", R.string.MessageMenu), false);
                    } else if (position == tabsTitleTypeRow) {
                        String value;
                        switch (NekoConfig.tabsTitleType) {
                            case NekoConfig.TITLE_TYPE_TEXT:
                                value = LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText);
                                break;
                            case NekoConfig.TITLE_TYPE_ICON:
                                value = LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon);
                                break;
                            case NekoConfig.TITLE_TYPE_MIX:
                            default:
                                value = LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix);
                        }
                        textCell.setTextAndValue(LocaleController.getString("TabTitleType", R.string.TabTitleType), value, false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ignoreBlockedRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("IgnoreBlocked", R.string.IgnoreBlocked), LocaleController.getString("IgnoreBlockedAbout", R.string.IgnoreBlockedAbout), NekoConfig.ignoreBlocked, true, true);
                    } else if (position == disablePhotoSideActionRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisablePhotoViewerSideAction", R.string.DisablePhotoViewerSideAction), NekoConfig.disablePhotoSideAction, true);
                    } else if (position == hideKeyboardOnChatScrollRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HideKeyboardOnChatScroll", R.string.HideKeyboardOnChatScroll), NekoConfig.hideKeyboardOnChatScroll, true);
                    } else if (position == showTabsOnForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ShowTabsOnForward", R.string.ShowTabsOnForward), NekoConfig.showTabsOnForward, true);
                    } else if (position == rearVideoMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("RearVideoMessages", R.string.RearVideoMessages), NekoConfig.rearVideoMessages, true);
                    } else if (position == hideAllTabRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("HideAllTab", R.string.HideAllTab), LocaleController.getString("HideAllTabAbout", R.string.HideAllTabAbout), NekoConfig.hideAllTab, true, true);
                    } else if (position == confirmAVRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ConfirmAVMessage", R.string.ConfirmAVMessage), NekoConfig.confirmAVMessage, true);
                    } else if (position == disableProximityEventsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableProximityEvents", R.string.DisableProximityEvents), NekoConfig.disableProximityEvents, true);
                    } else if (position == tryToOpenAllLinksInIVRow) {
                        textCell.setTextAndCheck(LocaleController.getString("OpenAllLinksInInstantView", R.string.OpenAllLinksInInstantView), NekoConfig.tryToOpenAllLinksInIV, true);
                    } else if (position == swipeToPiPRow) {
                        textCell.setTextAndCheck(LocaleController.getString("SwipeToPiP", R.string.SwipeToPiP), NekoConfig.swipeToPiP, true);
                    } else if (position == autoPauseVideoRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("AutoPauseVideo", R.string.AutoPauseVideo), LocaleController.getString("AutoPauseVideoAbout", R.string.AutoPauseVideoAbout), NekoConfig.autoPauseVideo, true, true);
                    } else if (position == disableJumpToNextRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableJumpToNextChannel", R.string.DisableJumpToNextChannel), NekoConfig.disableJumpToNextChannel, true);
                    } else if (position == disableGreetingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableGreetingSticker", R.string.DisableGreetingSticker), NekoConfig.disableGreetingSticker, true);
                    } else if (position == disableVoiceMessageAutoPlayRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableVoiceMessagesAutoPlay", R.string.DisableVoiceMessagesAutoPlay), NekoConfig.disableVoiceMessageAutoPlay, true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == chatRow) {
                        headerCell.setText(LocaleController.getString("Chat", R.string.Chat));
                    } else if (position == foldersRow) {
                        headerCell.setText(LocaleController.getString("Folder", R.string.Folder));
                    } else if (position == stickerSizeHeaderRow) {
                        headerCell.setText(LocaleController.getString("StickerSize", R.string.StickerSize));
                    }
                    break;
                }
                case 7: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == folders2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("TabTitleTypeTip", R.string.TabTitleTypeTip));
                    }

                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new HeaderCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 5:
                    view = new NotificationsCheckCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 6:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 7:
                    view = new TextInfoPrivacyCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
                case 8:
                    view = stickerSizeCell = new StickerSizeCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == chat2Row || position == stickerSize2Row) {
                return 1;
            } else if (position == messageMenuRow || position == tabsTitleTypeRow) {
                return 2;
            } else if (position == showTabsOnForwardRow || position == hideAllTabRow ||
                    (position > chatRow && position < messageMenuRow)) {
                return 3;
            } else if (position == chatRow || position == foldersRow || position == stickerSizeHeaderRow) {
                return 4;
            } else if (position == folders2Row) {
                return 7;
            } else if (position == stickerSizeRow) {
                return 8;
            }
            return 2;
        }
    }
}
