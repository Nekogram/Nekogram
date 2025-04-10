package tw.nekomimi.nekogram.settings;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.ThemePreviewMessagesCell;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.EntitiesHelper;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.helpers.VoiceEnhancementsHelper;
import tw.nekomimi.nekogram.helpers.WhisperHelper;

public class NekoChatSettingsActivity extends BaseNekoSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private ActionBarMenuItem resetItem;
    private StickerSizeCell stickerSizeCell;

    private int stickerSizeRow;
    private int hideTimeOnStickerRow;
    private int showTimeHintRow;
    private int reducedColorsRow;
    private int stickerSize2Row;

    private int chatRow;
    private int ignoreBlockedRow;
    private int quickForwardRow;
    private int hideKeyboardOnChatScrollRow;
    private int tryToOpenAllLinksInIVRow;
    private int disableJumpToNextRow;
    private int disableGreetingStickerRow;
    private int doubleTapActionRow;
    private int maxRecentStickersRow;
    private int chat2Row;

    private int transcribeRow;
    private int transcribeProviderRow;
    private int cfCredentialsRow;
    private int transcribe2Row;

    private int markdownRow;
    private int markdownEnableRow;
    private int markdownParserRow;
    private int markdownParseLinksRow;
    private int markdown2Row;

    private int mediaRow;
    private int voiceEnhancementsRow;
    private int rearVideoMessagesRow;
    private int confirmAVRow;
    private int disableProximityEventsRow;
    private int disableVoiceMessageAutoPlayRow;
    private int unmuteVideosWithVolumeButtonsRow;
    private int autoPauseVideoRow;
    private int preferOriginalQualityRow;
    private int media2Row;

    private int messageMenuRow;
    private int messageMenu2Row;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);

        return true;
    }

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        ActionBarMenu menu = actionBar.createMenu();
        resetItem = menu.addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString(R.string.ResetStickerSize));
        resetItem.setTag(null);
        resetItem.setOnClickListener(v -> {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
            ValueAnimator animator = ValueAnimator.ofFloat(NekoConfig.stickerSize, 14.0f);
            animator.setDuration(150);
            animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
            animator.addUpdateListener(valueAnimator -> {
                if (stickerSizeCell != null) {
                    stickerSizeCell.setValue((float) valueAnimator.getAnimatedValue());
                }
            });
            animator.start();
        });
        AndroidUtilities.updateViewVisibilityAnimated(resetItem, NekoConfig.stickerSize != 14.0f, 1f, false);

        return fragmentView;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == ignoreBlockedRow) {
            NekoConfig.toggleIgnoreBlocked();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.ignoreBlocked);
            }
        } else if (position == hideKeyboardOnChatScrollRow) {
            NekoConfig.toggleHideKeyboardOnChatScroll();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideKeyboardOnChatScroll);
            }
        } else if (position == rearVideoMessagesRow) {
            NekoConfig.toggleRearVideoMessages();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.rearVideoMessages);
            }
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
            showRestartBulletin();
        } else if (position == tryToOpenAllLinksInIVRow) {
            NekoConfig.toggleTryToOpenAllLinksInIV();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.tryToOpenAllLinksInIV);
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
        } else if (position == unmuteVideosWithVolumeButtonsRow) {
            NekoConfig.toggleUnmuteVideosWithVolumeButtons();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.unmuteVideosWithVolumeButtons);
            }
        } else if (position == doubleTapActionRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.Disable));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_NONE);
            arrayList.add(LocaleController.getString(R.string.Reactions));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_REACTION);
            arrayList.add(LocaleController.getString(R.string.TranslateMessage));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_TRANSLATE);
            arrayList.add(LocaleController.getString(R.string.Reply));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_REPLY);
            arrayList.add(LocaleController.getString(R.string.AddToSavedMessages));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_SAVE);
            arrayList.add(LocaleController.getString(R.string.Repeat));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_REPEAT);
            arrayList.add(LocaleController.getString(R.string.Edit));
            types.add(NekoConfig.DOUBLE_TAP_ACTION_EDIT);

            var context = getParentActivity();
            var builder = new AlertDialog.Builder(context, resourcesProvider);
            builder.setTitle(LocaleController.getString(R.string.DoubleTapAction));

            var linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            builder.setView(linearLayout);

            var messagesCell = new ThemePreviewMessagesCell(context, parentLayout, 0);
            messagesCell.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            linearLayout.addView(messagesCell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            var hLayout = new LinearLayout(context);
            hLayout.setOrientation(LinearLayout.HORIZONTAL);
            hLayout.setPadding(0, AndroidUtilities.dp(8), 0, 0);
            linearLayout.addView(hLayout);

            for (int i = 0; i < 2; i++) {
                var out = i == 1;
                var layout = new LinearLayout(context);
                layout.setOrientation(LinearLayout.VERTICAL);
                hLayout.addView(layout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, .5f));

                for (int a = 0; a < arrayList.size(); a++) {

                    var cell = new RadioColorCell(context, resourcesProvider);
                    cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                    cell.setTag(a);
                    cell.setTextAndValue(arrayList.get(a), a == types.indexOf(out ? NekoConfig.doubleTapOutAction : NekoConfig.doubleTapInAction));
                    cell.setBackground(Theme.createRadSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), out ? AndroidUtilities.dp(6) : 0, out ? 0 : AndroidUtilities.dp(6), out ? 0 : AndroidUtilities.dp(6), out ? AndroidUtilities.dp(6) : 0));
                    layout.addView(cell);
                    cell.setOnClickListener(v -> {
                        var which = (Integer) v.getTag();
                        var old = out ? NekoConfig.doubleTapOutAction : NekoConfig.doubleTapInAction;
                        if (types.get(which) == old) {
                            return;
                        }
                        if (out) {
                            NekoConfig.setDoubleTapOutAction(types.get(which));
                        } else {
                            NekoConfig.setDoubleTapInAction(types.get(which));
                        }
                        ((RadioColorCell) layout.getChildAt(types.indexOf(old))).setChecked(false, true);
                        cell.setChecked(true, true);
                        listAdapter.notifyItemChanged(doubleTapActionRow, PARTIAL);
                    });
                }
            }

            builder.setOnPreDismissListener(dialog -> listAdapter.notifyItemChanged(doubleTapActionRow, PARTIAL));
            builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
            builder.show();
        } else if (position == markdownEnableRow) {
            NekoConfig.toggleDisableMarkdownByDefault();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(!NekoConfig.disableMarkdownByDefault);
            }
        } else if (position > messageMenuRow && position < messageMenu2Row) {
            TextCheckbox2Cell cell = ((TextCheckbox2Cell) view);
            int menuPosition = position - messageMenuRow - 1;
            if (menuPosition == 0) {
                NekoConfig.toggleShowDeleteDownloadedFile();
                cell.setChecked(NekoConfig.showDeleteDownloadedFile);
            } else if (menuPosition == 1) {
                NekoConfig.toggleShowNoQuoteForward();
                cell.setChecked(NekoConfig.showNoQuoteForward);
            } else if (menuPosition == 2) {
                NekoConfig.toggleShowAddToSavedMessages();
                cell.setChecked(NekoConfig.showAddToSavedMessages);
            } else if (menuPosition == 3) {
                NekoConfig.toggleShowRepeat();
                cell.setChecked(NekoConfig.showRepeat);
            } else if (menuPosition == 4) {
                NekoConfig.toggleShowPrPr();
                cell.setChecked(NekoConfig.showPrPr);
            } else if (menuPosition == 5) {
                NekoConfig.toggleShowViewHistory();
                cell.setChecked(NekoConfig.showViewHistory);
            } else if (menuPosition == 6) {
                NekoConfig.toggleShowTranslate();
                cell.setChecked(NekoConfig.showTranslate);
            } else if (menuPosition == 7) {
                NekoConfig.toggleShowReport();
                cell.setChecked(NekoConfig.showReport);
            } else if (menuPosition == 8) {
                NekoConfig.toggleShowMessageDetails();
                cell.setChecked(NekoConfig.showMessageDetails);
            } else if (menuPosition == 9) {
                NekoConfig.toggleShowCopyPhoto();
                cell.setChecked(NekoConfig.showCopyPhoto);
            } else if (menuPosition == 10) {
                NekoConfig.toggleShowSetReminder();
                cell.setChecked(NekoConfig.showSetReminder);
            } else if (menuPosition == 11) {
                NekoConfig.toggleShowQrCode();
                cell.setChecked(NekoConfig.showQrCode);
            } else if (menuPosition == 12) {
                NekoConfig.toggleShowOpenIn();
                cell.setChecked(NekoConfig.showOpenIn);
            }
        } else if (position == voiceEnhancementsRow) {
            NekoConfig.toggleVoiceEnhancements();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.voiceEnhancements);
            }
        } else if (position == maxRecentStickersRow) {
            int[] counts = {20, 30, 40, 50, 80, 100, 120, 150, 180, 200};
            ArrayList<String> types = new ArrayList<>();
            for (int count : counts) {
                if (count <= getMessagesController().maxRecentStickersCount) {
                    types.add(String.valueOf(count));
                }
            }
            PopupHelper.show(types, LocaleController.getString(R.string.MaxRecentStickers), types.indexOf(String.valueOf(NekoConfig.maxRecentStickers)), getParentActivity(), view, i -> {
                NekoConfig.setMaxRecentStickers(Integer.parseInt(types.get(i)));
                listAdapter.notifyItemChanged(maxRecentStickersRow, PARTIAL);
            }, resourcesProvider);
        } else if (position == hideTimeOnStickerRow) {
            NekoConfig.toggleHideTimeOnSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideTimeOnSticker);
            }
            stickerSizeCell.invalidate();
        } else if (position == markdownParserRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("Nekogram");
            arrayList.add("Telegram");
            boolean oldParser = NekoConfig.newMarkdownParser;
            PopupHelper.show(arrayList, LocaleController.getString(R.string.MarkdownParser), NekoConfig.newMarkdownParser ? 0 : 1, getParentActivity(), view, i -> {
                NekoConfig.setNewMarkdownParser(i == 0);
                listAdapter.notifyItemChanged(markdownParserRow, PARTIAL);
                if (oldParser != NekoConfig.newMarkdownParser) {
                    if (oldParser) {
                        listAdapter.notifyItemRemoved(markdownParseLinksRow);
                        updateRows();
                    } else {
                        updateRows();
                        listAdapter.notifyItemInserted(markdownParseLinksRow);
                    }
                    listAdapter.notifyItemChanged(markdown2Row);
                }
            }, resourcesProvider);
        } else if (position == markdownParseLinksRow) {
            NekoConfig.toggleMarkdownParseLinks();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.markdownParseLinks);
            }
            listAdapter.notifyItemChanged(markdown2Row);
        } else if (position == quickForwardRow) {
            NekoConfig.toggleQuickForward();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.quickForward);
            }
        } else if (position == reducedColorsRow) {
            NekoConfig.toggleReducedColors();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.reducedColors);
            }
            stickerSizeCell.invalidate();
        } else if (position == showTimeHintRow) {
            NekoConfig.toggleShowTimeHint();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.showTimeHint);
            }
        } else if (position == transcribeProviderRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TranscribeProviderAuto));
            types.add(NekoConfig.TRANSCRIBE_AUTO);
            arrayList.add(LocaleController.getString(R.string.TelegramPremium));
            types.add(NekoConfig.TRANSCRIBE_PREMIUM);
            arrayList.add(LocaleController.getString(R.string.TranscribeProviderWorkersAI));
            types.add(NekoConfig.TRANSCRIBE_WORKERSAI);
            PopupHelper.show(arrayList, LocaleController.getString(R.string.TranscribeProviderShort), types.indexOf(NekoConfig.transcribeProvider), getParentActivity(), view, i -> {
                NekoConfig.setTranscribeProvider(types.get(i));
                listAdapter.notifyItemChanged(transcribeProviderRow, PARTIAL);
            }, resourcesProvider);
        } else if (position == cfCredentialsRow) {
            WhisperHelper.showCfCredentialsDialog(this);
        } else if (position == preferOriginalQualityRow) {
            NekoConfig.togglePreferOriginalQuality();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.preferOriginalQuality);
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.Chat);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        stickerSizeRow = addRow("stickerSize");
        hideTimeOnStickerRow = addRow("hideTimeOnSticker");
        showTimeHintRow = addRow("showTimeHint");
        reducedColorsRow = addRow("reducedColors");
        stickerSize2Row = addRow();

        chatRow = addRow("chat");
        ignoreBlockedRow = addRow("ignoreBlocked");
        quickForwardRow = addRow("quickForward");
        hideKeyboardOnChatScrollRow = addRow("hideKeyboardOnChatScroll");
        tryToOpenAllLinksInIVRow = addRow("tryToOpenAllLinksInIV");
        disableJumpToNextRow = addRow("disableJumpToNext");
        disableGreetingStickerRow = addRow("disableGreetingSticker");
        doubleTapActionRow = addRow("doubleTapAction");
        maxRecentStickersRow = addRow("maxRecentStickers");
        chat2Row = addRow();

        transcribeRow = addRow("transcribe");
        transcribeProviderRow = addRow("transcribeProvider");
        cfCredentialsRow = addRow("cfCredentials");
        transcribe2Row = addRow();

        markdownRow = addRow("markdown");
        markdownEnableRow = addRow("markdownEnableRow");
        markdownParserRow = addRow("markdownParserRow");
        markdownParseLinksRow = NekoConfig.newMarkdownParser ? addRow("markdownParseLinksRow") : -1;
        markdown2Row = addRow();

        mediaRow = addRow("media");
        voiceEnhancementsRow = VoiceEnhancementsHelper.isAvailable() ? addRow("voiceEnhancements") : -1;
        rearVideoMessagesRow = addRow("rearVideoMessages");
        confirmAVRow = addRow("confirmAV");
        disableProximityEventsRow = addRow("disableProximityEvents");
        disableVoiceMessageAutoPlayRow = addRow("disableVoiceMessageAutoPlay");
        unmuteVideosWithVolumeButtonsRow = addRow("unmuteVideosWithVolumeButtons");
        autoPauseVideoRow = addRow("autoPauseVideo");
        preferOriginalQualityRow = addRow("preferOriginalQuality");
        media2Row = addRow();

        messageMenuRow = addRow("messageMenu");
        addRow("showDeleteDownloadedFile");
        addRow("showNoQuoteForward");
        addRow("showAddToSavedMessages");
        addRow("showRepeat");
        addRow("showPrPr");
        addRow("showViewHistory");
        addRow("showTranslate");
        addRow("showReport");
        addRow("showMessageDetails");
        addRow("showCopyPhoto");
        addRow("showSetReminder");
        addRow("showQrCode");
        addRow("showOpenIn");
        messageMenu2Row = addRow();
    }

    @Override
    protected String getKey() {
        return "c";
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
        private final AltSeekbar sizeBar;

        public StickerSizeCell(Context context) {
            super(context);

            setWillNotDraw(false);

            sizeBar = new AltSeekbar(context, (float p) -> {
                setValue(p);
                if (resetItem.getVisibility() != VISIBLE) {
                    AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
                }
            }, 2, 20, LocaleController.getString(R.string.StickerSize), LocaleController.getString(R.string.StickerSizeLeft), LocaleController.getString(R.string.StickerSizeRight), resourcesProvider);
            sizeBar.setValue(NekoConfig.stickerSize);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            messagesCell = new StickerSizePreviewMessagesCell(context, NekoChatSettingsActivity.this);
            messagesCell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 112, 0, 0));
        }

        public void setValue(float value) {
            NekoConfig.setStickerSize(value);
            sizeBar.setValue(value);
            messagesCell.invalidate();
        }

        @Override
        public void invalidate() {
            super.invalidate();
            messagesCell.invalidate();
        }
    }

    @SuppressLint("ViewConstructor")
    public static class AltSeekbar extends FrameLayout {

        private final AnimatedTextView headerValue;
        private final TextView leftTextView;
        private final TextView rightTextView;
        private final SeekBarView seekBarView;
        private final Theme.ResourcesProvider resourcesProvider;

        private final int min, max;
        private float currentValue;
        private int roundedValue;

        public interface OnDrag {
            void run(float progress);
        }

        public AltSeekbar(Context context, OnDrag onDrag, int min, int max, String title, String left, String right, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            this.resourcesProvider = resourcesProvider;

            this.max = max;
            this.min = min;

            LinearLayout headerLayout = new LinearLayout(context);
            headerLayout.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);

            TextView headerTextView = new TextView(context);
            headerTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            headerTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            headerTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
            headerTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            headerTextView.setText(title);
            headerLayout.addView(headerTextView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

            headerValue = new AnimatedTextView(context, false, true, true) {
                final Drawable backgroundDrawable = Theme.createRoundRectDrawable(AndroidUtilities.dp(4), Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider), 0.15f));

                @Override
                protected void onDraw(Canvas canvas) {
                    backgroundDrawable.setBounds(0, 0, (int) (getPaddingLeft() + getDrawable().getCurrentWidth() + getPaddingRight()), getMeasuredHeight());
                    backgroundDrawable.draw(canvas);

                    super.onDraw(canvas);
                }
            };
            headerValue.setAnimationProperties(.45f, 0, 240, CubicBezierInterpolator.EASE_OUT_QUINT);
            headerValue.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            headerValue.setPadding(AndroidUtilities.dp(5.33f), AndroidUtilities.dp(2), AndroidUtilities.dp(5.33f), AndroidUtilities.dp(2));
            headerValue.setTextSize(AndroidUtilities.dp(12));
            headerValue.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
            headerLayout.addView(headerValue, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, 17, Gravity.CENTER_VERTICAL, 6, 1, 0, 0));

            addView(headerLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 21, 17, 21, 0));

            seekBarView = new SeekBarView(context, true, resourcesProvider);
            seekBarView.setReportChanges(true);
            seekBarView.setDelegate((stop, progress) -> {
                currentValue = min + (max - min) * progress;
                onDrag.run(currentValue);
                if (Math.round(currentValue) != roundedValue) {
                    roundedValue = Math.round(currentValue);
                    updateText();
                }
            });
            addView(seekBarView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 38 + 6, Gravity.TOP, 6, 68, 6, 0));

            FrameLayout valuesView = new FrameLayout(context);

            leftTextView = new TextView(context);
            leftTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            leftTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            leftTextView.setGravity(Gravity.LEFT);
            leftTextView.setText(left);
            valuesView.addView(leftTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.CENTER_VERTICAL));

            rightTextView = new TextView(context);
            rightTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            rightTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            rightTextView.setGravity(Gravity.RIGHT);
            rightTextView.setText(right);
            valuesView.addView(rightTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

            addView(valuesView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.FILL_HORIZONTAL, 21, 52, 21, 0));
        }

        private void updateValues() {
            int middle = (max - min) / 2 + min;
            if (currentValue >= middle * 1.5f - min * 0.5f) {
                rightTextView.setTextColor(ColorUtils.blendARGB(
                        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider),
                        Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, resourcesProvider),
                        (currentValue - (middle * 1.5f - min * 0.5f)) / (max - (middle * 1.5f - min * 0.5f))
                ));
                leftTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            } else if (currentValue <= (middle + min) * 0.5f) {
                leftTextView.setTextColor(ColorUtils.blendARGB(
                        Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider),
                        Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, resourcesProvider),
                        (currentValue - (middle + min) * 0.5f) / (min - (middle + min) * 0.5f)
                ));
                rightTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            } else {
                leftTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
                rightTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            }
        }

        public void setValue(float value) {
            currentValue = value;
            seekBarView.setProgress((value - min) / (float) (max - min));
            if (Math.round(currentValue) != roundedValue) {
                roundedValue = Math.round(currentValue);
                updateText();
            }
        }

        private void updateText() {
            headerValue.cancelAnimation();
            headerValue.setText(getTextForHeader(), true);
            updateValues();
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(
                    MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(112), MeasureSpec.EXACTLY)
            );
        }

        public CharSequence getTextForHeader() {
            CharSequence text;
            if (roundedValue == min) {
                text = leftTextView.getText();
            } else if (roundedValue == max) {
                text = rightTextView.getText();
            } else {
                text = String.valueOf(roundedValue);
            }
            return text.toString().toUpperCase();
        }
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        public String getDoubleTapActionText(int action) {
            return switch (action) {
                case NekoConfig.DOUBLE_TAP_ACTION_REACTION ->
                        LocaleController.getString(R.string.Reactions);
                case NekoConfig.DOUBLE_TAP_ACTION_TRANSLATE ->
                        LocaleController.getString(R.string.TranslateMessage);
                case NekoConfig.DOUBLE_TAP_ACTION_REPLY ->
                        LocaleController.getString(R.string.Reply);
                case NekoConfig.DOUBLE_TAP_ACTION_SAVE ->
                        LocaleController.getString(R.string.AddToSavedMessages);
                case NekoConfig.DOUBLE_TAP_ACTION_REPEAT ->
                        LocaleController.getString(R.string.Repeat);
                case NekoConfig.DOUBLE_TAP_ACTION_EDIT -> LocaleController.getString(R.string.Edit);
                default -> LocaleController.getString(R.string.Disable);
            };
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == doubleTapActionRow) {
                        var value = NekoConfig.doubleTapInAction == NekoConfig.doubleTapOutAction ?
                                getDoubleTapActionText(NekoConfig.doubleTapInAction) :
                                getDoubleTapActionText(NekoConfig.doubleTapInAction) + ", " + getDoubleTapActionText(NekoConfig.doubleTapOutAction);
                        textCell.setTextAndValue(LocaleController.getString(R.string.DoubleTapAction), value, partial, divider);
                    } else if (position == maxRecentStickersRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MaxRecentStickers), String.valueOf(NekoConfig.maxRecentStickers), partial, divider);
                    } else if (position == markdownParserRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.MarkdownParser), NekoConfig.newMarkdownParser ? "Nekogram" : "Telegram", partial, divider);
                    } else if (position == transcribeProviderRow) {
                        String value = switch (NekoConfig.transcribeProvider) {
                            case NekoConfig.TRANSCRIBE_AUTO ->
                                    LocaleController.getString(R.string.TranscribeProviderAuto);
                            case NekoConfig.TRANSCRIBE_WORKERSAI ->
                                    LocaleController.getString(R.string.TranscribeProviderWorkersAI);
                            default -> LocaleController.getString(R.string.TelegramPremium);
                        };
                        textCell.setTextAndValue(LocaleController.getString(R.string.TranscribeProviderShort), value, partial, divider);
                    } else if (position == cfCredentialsRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.CloudflareCredentials), "", partial, divider);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ignoreBlockedRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.IgnoreBlocked), LocaleController.getString(R.string.IgnoreBlockedAbout), NekoConfig.ignoreBlocked, true, divider);
                    } else if (position == hideKeyboardOnChatScrollRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.HideKeyboardOnChatScroll), NekoConfig.hideKeyboardOnChatScroll, divider);
                    } else if (position == rearVideoMessagesRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.RearVideoMessages), NekoConfig.rearVideoMessages, divider);
                    } else if (position == confirmAVRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.ConfirmAVMessage), NekoConfig.confirmAVMessage, divider);
                    } else if (position == disableProximityEventsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DisableProximityEvents), NekoConfig.disableProximityEvents, divider);
                    } else if (position == tryToOpenAllLinksInIVRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.OpenAllLinksInInstantView), NekoConfig.tryToOpenAllLinksInIV, divider);
                    } else if (position == autoPauseVideoRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.AutoPauseVideo), LocaleController.getString(R.string.AutoPauseVideoAbout), NekoConfig.autoPauseVideo, true, divider);
                    } else if (position == disableJumpToNextRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DisableJumpToNextChannel), NekoConfig.disableJumpToNextChannel, divider);
                    } else if (position == disableGreetingStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DisableGreetingSticker), NekoConfig.disableGreetingSticker, divider);
                    } else if (position == disableVoiceMessageAutoPlayRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DisableVoiceMessagesAutoPlay), NekoConfig.disableVoiceMessageAutoPlay, divider);
                    } else if (position == unmuteVideosWithVolumeButtonsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.UnmuteVideosWithVolumeButtons), NekoConfig.unmuteVideosWithVolumeButtons, divider);
                    } else if (position == voiceEnhancementsRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.VoiceEnhancements), LocaleController.getString(R.string.VoiceEnhancementsAbout), NekoConfig.voiceEnhancements, true, divider);
                    } else if (position == hideTimeOnStickerRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.HideTimeOnSticker), NekoConfig.hideTimeOnSticker, divider);
                    } else if (position == markdownEnableRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.MarkdownEnableByDefault), !NekoConfig.disableMarkdownByDefault, divider);
                    } else if (position == markdownParseLinksRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.MarkdownParseLinks), NekoConfig.markdownParseLinks, divider);
                    } else if (position == quickForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.QuickForward), NekoConfig.quickForward, divider);
                    } else if (position == reducedColorsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.ReducedColors), NekoConfig.reducedColors, divider);
                    } else if (position == showTimeHintRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.ShowTimeHint), LocaleController.getString(R.string.ShowTimeHintDesc), NekoConfig.showTimeHint, true, divider);
                    } else if (position == preferOriginalQualityRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.PreferOriginalQuality), LocaleController.getString(R.string.PreferOriginalQualityDesc), NekoConfig.preferOriginalQuality, true, divider);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == chatRow) {
                        headerCell.setText(LocaleController.getString(R.string.Chat));
                    } else if (position == messageMenuRow) {
                        headerCell.setText(LocaleController.getString(R.string.MessageMenu));
                    } else if (position == mediaRow) {
                        headerCell.setText(LocaleController.getString(R.string.SharedMediaTab2));
                    } else if (position == markdownRow) {
                        headerCell.setText(LocaleController.getString(R.string.Markdown));
                    } else if (position == transcribeRow) {
                        headerCell.setText(LocaleController.getString(R.string.PremiumPreviewVoiceToText));
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == markdown2Row) {
                        cell.getTextView().setMovementMethod(null);
                        cell.setText(TextUtils.expandTemplate(EntitiesHelper.parseMarkdown(NekoConfig.newMarkdownParser && NekoConfig.markdownParseLinks ? LocaleController.getString(R.string.MarkdownAbout) : LocaleController.getString(R.string.MarkdownAbout2)), "**", "__", "~~", "`", "||", "[", "](", ")"));
                    } else if (position == transcribe2Row) {
                        cell.setText(LocaleController.formatString(R.string.TranscribeProviderDesc, LocaleController.getString(R.string.TranscribeProviderWorkersAI)));
                    }
                    break;
                }
                case TYPE_CHECKBOX: {
                    TextCheckbox2Cell cell = (TextCheckbox2Cell) holder.itemView;
                    int menuPosition = position - messageMenuRow - 1;
                    if (menuPosition == 0) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.DeleteDownloadedFile), NekoConfig.showDeleteDownloadedFile, divider);
                    } else if (menuPosition == 1) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.NoQuoteForward), NekoConfig.showNoQuoteForward, divider);
                    } else if (menuPosition == 2) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.AddToSavedMessages), NekoConfig.showAddToSavedMessages, divider);
                    } else if (menuPosition == 3) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.Repeat), NekoConfig.showRepeat, divider);
                    } else if (menuPosition == 4) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.Prpr), NekoConfig.showPrPr, divider);
                    } else if (menuPosition == 5) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.ViewHistory), NekoConfig.showViewHistory, divider);
                    } else if (menuPosition == 6) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.TranslateMessage), NekoConfig.showTranslate, divider);
                    } else if (menuPosition == 7) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.ReportChat), NekoConfig.showReport, divider);
                    } else if (menuPosition == 8) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.MessageDetails), NekoConfig.showMessageDetails, divider);
                    } else if (menuPosition == 9) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.CopyPhoto), NekoConfig.showCopyPhoto, divider);
                    } else if (menuPosition == 10) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.SetReminder), NekoConfig.showSetReminder, divider);
                    } else if (menuPosition == 11) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.QrCode), NekoConfig.showQrCode, divider);
                    } else if (menuPosition == 12) {
                        cell.setTextAndCheck(LocaleController.getString(R.string.OpenInExternalApp), NekoConfig.showOpenIn, divider);
                    }
                    break;
                }
            }
        }

        @Override
        public View createCustomView(int viewType) {
            if (viewType == Integer.MAX_VALUE) {
                return stickerSizeCell = new StickerSizeCell(mContext);
            } else {
                return super.createCustomView(viewType);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == chat2Row || position == stickerSize2Row || position == messageMenu2Row || position == media2Row) {
                return TYPE_SHADOW;
            } else if (position == doubleTapActionRow || position == maxRecentStickersRow || position == markdownParserRow ||
                    position == transcribeProviderRow || position == cfCredentialsRow) {
                return TYPE_SETTINGS;
            } else if ((position > chatRow && position < doubleTapActionRow) ||
                    (position > mediaRow && position < media2Row) ||
                    (position > markdownRow && position < markdown2Row) ||
                    (position > stickerSizeRow && position < stickerSize2Row)
            ) {
                return TYPE_CHECK;
            } else if (position == chatRow || position == messageMenuRow || position == mediaRow || position == markdownRow ||
                    position == transcribeRow) {
                return TYPE_HEADER;
            } else if (position == markdown2Row || position == transcribe2Row) {
                return TYPE_INFO_PRIVACY;
            } else if (position > messageMenuRow && position < messageMenu2Row) {
                return TYPE_CHECKBOX;
            } else if (position == stickerSizeRow) {
                return Integer.MAX_VALUE;
            }
            return TYPE_SETTINGS;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = super.getThemeDescriptions();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_PROGRESSBAR, new Class[]{StickerSizeCell.class}, new String[]{"sizeBar"}, null, null, null, Theme.key_player_progress));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, new String[]{"sizeBar"}, null, null, null, Theme.key_player_progressBackground));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgInDrawable, Theme.chat_msgInMediaDrawable}, null, Theme.key_chat_inBubble));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgInSelectedDrawable, Theme.chat_msgInMediaSelectedDrawable}, null, Theme.key_chat_inBubbleSelected));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, Theme.chat_msgInDrawable.getShadowDrawables(), null, Theme.key_chat_inBubbleShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, Theme.chat_msgInMediaDrawable.getShadowDrawables(), null, Theme.key_chat_inBubbleShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutDrawable, Theme.chat_msgOutMediaDrawable}, null, Theme.key_chat_outBubble));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutDrawable, Theme.chat_msgOutMediaDrawable}, null, Theme.key_chat_outBubbleGradient1));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutDrawable, Theme.chat_msgOutMediaDrawable}, null, Theme.key_chat_outBubbleGradient2));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutDrawable, Theme.chat_msgOutMediaDrawable}, null, Theme.key_chat_outBubbleGradient3));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutSelectedDrawable, Theme.chat_msgOutMediaSelectedDrawable}, null, Theme.key_chat_outBubbleSelected));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutDrawable, Theme.chat_msgOutMediaDrawable}, null, Theme.key_chat_outBubbleShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgInDrawable, Theme.chat_msgInMediaDrawable}, null, Theme.key_chat_inBubbleShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_messageTextIn));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_messageTextOut));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutCheckDrawable}, null, Theme.key_chat_outSentCheck));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutCheckSelectedDrawable}, null, Theme.key_chat_outSentCheckSelected));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutCheckReadDrawable, Theme.chat_msgOutHalfCheckDrawable}, null, Theme.key_chat_outSentCheckRead));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgOutCheckReadSelectedDrawable, Theme.chat_msgOutHalfCheckSelectedDrawable}, null, Theme.key_chat_outSentCheckReadSelected));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, new Drawable[]{Theme.chat_msgMediaCheckDrawable, Theme.chat_msgMediaHalfCheckDrawable}, null, Theme.key_chat_mediaSentCheck));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_inReplyLine));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_outReplyLine));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_inReplyNameText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_outReplyNameText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_inReplyMessageText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_outReplyMessageText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_inReplyMediaMessageSelectedText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_outReplyMediaMessageSelectedText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_inTimeText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_outTimeText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_inTimeSelectedText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{StickerSizeCell.class}, null, null, null, Theme.key_chat_outTimeSelectedText));
        return themeDescriptions;
    }
}
