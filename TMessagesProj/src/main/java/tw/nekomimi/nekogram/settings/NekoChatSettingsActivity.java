package tw.nekomimi.nekogram.settings;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.ThemePreviewMessagesCell;
import org.telegram.ui.Components.AnimatedTextView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.EntitiesHelper;
import tw.nekomimi.nekogram.helpers.VoiceEnhancementsHelper;
import tw.nekomimi.nekogram.helpers.WhisperHelper;

public class NekoChatSettingsActivity extends BaseNekoSettingsActivity {

    private ActionBarMenuItem resetItem;

    private final int stickerSizeRow = rowId++;
    private final int hideTimeOnStickerRow = rowId++;
    private final int showTimeHintRow = rowId++;
    private final int reducedColorsRow = rowId++;

    private final int ignoreBlockedRow = rowId++;
    private final int quickForwardRow = rowId++;
    private final int hideKeyboardOnChatScrollRow = rowId++;
    private final int tryToOpenAllLinksInIVRow = rowId++;
    private final int disableJumpToNextRow = rowId++;
    private final int disableGreetingStickerRow = rowId++;
    private final int hideChannelBottomButtonsRow = rowId++;
    private final int doubleTapActionRow = rowId++;
    private final int maxRecentStickersRow = rowId++;

    private final int transcribeProviderRow = rowId++;
    private final int cfCredentialsRow = rowId++;

    private final int markdownEnableRow = rowId++;
    private final int markdownParserRow = rowId++;
    private final int markdownParseLinksRow = rowId++;
    private final int markdown2Row = rowId++;

    private final int voiceEnhancementsRow = rowId++;
    private final int confirmAVRow = rowId++;
    private final int disableProximityEventsRow = rowId++;
    private final int disableVoiceMessageAutoPlayRow = rowId++;
    private final int unmuteVideosWithVolumeButtonsRow = rowId++;
    private final int autoPauseVideoRow = rowId++;
    private final int preferOriginalQualityRow = rowId++;
    private final int cameraInVideoMessagesRow = rowId++;

    private final int messageMenuRow = 100;

    @Override
    public View createView(Context context) {
        var fragmentView = super.createView(context);

        var menu = actionBar.createMenu();
        resetItem = menu.addItem(0, R.drawable.msg_reset);
        resetItem.setContentDescription(LocaleController.getString(R.string.ResetStickerSize));
        resetItem.setTag(null);
        resetItem.setOnClickListener(v -> {
            AndroidUtilities.updateViewVisibilityAnimated(resetItem, false, 0.5f, true);
            var item = listView.findItemByItemId(stickerSizeRow);
            var stickerCell = (StickerSizeCell) listView.findViewByItemId(stickerSizeRow);
            if (stickerCell != null) {
                ValueAnimator animator = ValueAnimator.ofFloat(NekoConfig.stickerSize, 14.0f);
                animator.setDuration(150);
                animator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                animator.addUpdateListener(valueAnimator -> {
                    var floatValue = (float) valueAnimator.getAnimatedValue();
                    NekoConfig.setStickerSize(floatValue);
                    stickerCell.setValue(floatValue);
                });
                animator.start();
            } else {
                NekoConfig.setStickerSize(14.0f);
            }
            item.floatValue = 14.0f;
        });
        AndroidUtilities.updateViewVisibilityAnimated(resetItem, Float.compare(NekoConfig.stickerSize, 14.0f) != 0, 1f, false);

        return fragmentView;
    }

    public String getDoubleTapActionText(int action) {
        return switch (action) {
            case NekoConfig.DOUBLE_TAP_ACTION_REACTION ->
                    LocaleController.getString(R.string.Reactions);
            case NekoConfig.DOUBLE_TAP_ACTION_TRANSLATE ->
                    LocaleController.getString(R.string.TranslateMessage);
            case NekoConfig.DOUBLE_TAP_ACTION_REPLY -> LocaleController.getString(R.string.Reply);
            case NekoConfig.DOUBLE_TAP_ACTION_SAVE ->
                    LocaleController.getString(R.string.AddToSavedMessages);
            case NekoConfig.DOUBLE_TAP_ACTION_REPEAT -> LocaleController.getString(R.string.Repeat);
            case NekoConfig.DOUBLE_TAP_ACTION_EDIT -> LocaleController.getString(R.string.Edit);
            default -> LocaleController.getString(R.string.Disable);
        };
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(StickerSizeCellFactory.of(stickerSizeRow, LocaleController.getString(R.string.StickerSize), NekoConfig.stickerSize, progress -> {
            NekoConfig.setStickerSize(progress);
            if (progress != 14.0f && resetItem.getVisibility() != View.VISIBLE) {
                AndroidUtilities.updateViewVisibilityAnimated(resetItem, true, 0.5f, true);
            }
        }).slug("stickerSize"));
        items.add(UItem.asCheck(hideTimeOnStickerRow, LocaleController.getString(R.string.HideTimeOnSticker)).slug("hideTimeOnSticker").setChecked(NekoConfig.hideTimeOnSticker));
        items.add(UItem.asCheck(showTimeHintRow, LocaleController.getString(R.string.ShowTimeHint), LocaleController.getString(R.string.ShowTimeHintDesc)).slug("showTimeHint").setChecked(NekoConfig.showTimeHint));
        items.add(UItem.asCheck(reducedColorsRow, LocaleController.getString(R.string.ReducedColors)).slug("reducedColors").setChecked(NekoConfig.reducedColors));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.Chat)));
        items.add(UItem.asCheck(ignoreBlockedRow, LocaleController.getString(R.string.IgnoreBlocked), LocaleController.getString(R.string.IgnoreBlockedAbout)).slug("ignoreBlocked").setChecked(NekoConfig.ignoreBlocked));
        items.add(UItem.asCheck(quickForwardRow, LocaleController.getString(R.string.QuickForward)).slug("quickForward").setChecked(NekoConfig.quickForward));
        items.add(UItem.asCheck(hideKeyboardOnChatScrollRow, LocaleController.getString(R.string.HideKeyboardOnChatScroll)).slug("hideKeyboardOnChatScroll").setChecked(NekoConfig.hideKeyboardOnChatScroll));
        items.add(UItem.asCheck(tryToOpenAllLinksInIVRow, LocaleController.getString(R.string.OpenAllLinksInInstantView)).slug("tryToOpenAllLinksInIV").setChecked(NekoConfig.tryToOpenAllLinksInIV));
        items.add(UItem.asCheck(disableJumpToNextRow, LocaleController.getString(R.string.DisableJumpToNextChannel)).slug("disableJumpToNext").setChecked(NekoConfig.disableJumpToNextChannel));
        items.add(UItem.asCheck(disableGreetingStickerRow, LocaleController.getString(R.string.DisableGreetingSticker)).slug("disableGreetingSticker").setChecked(NekoConfig.disableGreetingSticker));
        items.add(UItem.asCheck(hideChannelBottomButtonsRow, LocaleController.getString(R.string.HideChannelBottomButtons)).slug("hideChannelBottomButtons").setChecked(NekoConfig.hideChannelBottomButtons));
        items.add(TextSettingsCellFactory.of(doubleTapActionRow, LocaleController.getString(R.string.DoubleTapAction), NekoConfig.doubleTapInAction == NekoConfig.doubleTapOutAction ?
                getDoubleTapActionText(NekoConfig.doubleTapInAction) :
                getDoubleTapActionText(NekoConfig.doubleTapInAction) + ", " + getDoubleTapActionText(NekoConfig.doubleTapOutAction)).slug("doubleTapAction"));
        items.add(TextSettingsCellFactory.of(maxRecentStickersRow, LocaleController.getString(R.string.MaxRecentStickers), String.valueOf(NekoConfig.maxRecentStickers)).slug("maxRecentStickers"));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.PremiumPreviewVoiceToText)));
        items.add(TextSettingsCellFactory.of(transcribeProviderRow, LocaleController.getString(R.string.TranscribeProviderShort), switch (NekoConfig.transcribeProvider) {
            case NekoConfig.TRANSCRIBE_AUTO ->
                    LocaleController.getString(R.string.TranscribeProviderAuto);
            case NekoConfig.TRANSCRIBE_WORKERSAI ->
                    LocaleController.getString(R.string.TranscribeProviderWorkersAI);
            default -> LocaleController.getString(R.string.TelegramPremium);
        }).slug("transcribeProvider"));
        items.add(TextSettingsCellFactory.of(cfCredentialsRow, LocaleController.getString(R.string.CloudflareCredentials), "").slug("cfCredentials"));
        items.add(UItem.asShadow(LocaleController.formatString(R.string.TranscribeProviderDesc, LocaleController.getString(R.string.TranscribeProviderWorkersAI))));

        items.add(UItem.asHeader(LocaleController.getString(R.string.Markdown)));
        items.add(UItem.asCheck(markdownEnableRow, LocaleController.getString(R.string.MarkdownEnableByDefault)).slug("markdownEnable").setChecked(!NekoConfig.disableMarkdownByDefault));
        items.add(TextSettingsCellFactory.of(markdownParserRow, LocaleController.getString(R.string.MarkdownParser), NekoConfig.newMarkdownParser ? "Nekogram" : "Telegram").slug("markdownParser"));
        if (NekoConfig.newMarkdownParser) {
            items.add(UItem.asCheck(markdownParseLinksRow, LocaleController.getString(R.string.MarkdownParseLinks)).slug("markdownParseLinks").setChecked(NekoConfig.markdownParseLinks));
        }
        items.add(UItem.asShadow(markdown2Row, TextUtils.expandTemplate(EntitiesHelper.parseMarkdown(NekoConfig.newMarkdownParser && NekoConfig.markdownParseLinks ? LocaleController.getString(R.string.MarkdownAbout) : LocaleController.getString(R.string.MarkdownAbout2)), "**", "__", "~~", "`", "||", "[", "](", ")")));

        items.add(UItem.asHeader(LocaleController.getString(R.string.SharedMediaTab2)));
        if (VoiceEnhancementsHelper.isAvailable()) {
            items.add(UItem.asCheck(voiceEnhancementsRow, LocaleController.getString(R.string.VoiceEnhancements), LocaleController.getString(R.string.VoiceEnhancementsAbout)).slug("voiceEnhancements").setChecked(NekoConfig.voiceEnhancements));
        }
        items.add(UItem.asCheck(confirmAVRow, LocaleController.getString(R.string.ConfirmAVMessage)).slug("confirmAV").setChecked(NekoConfig.confirmAVMessage));
        items.add(UItem.asCheck(disableProximityEventsRow, LocaleController.getString(R.string.DisableProximityEvents)).slug("disableProximityEvents").setChecked(NekoConfig.disableProximityEvents));
        items.add(UItem.asCheck(disableVoiceMessageAutoPlayRow, LocaleController.getString(R.string.DisableVoiceMessagesAutoPlay)).slug("disableVoiceMessageAutoPlay").setChecked(NekoConfig.disableVoiceMessageAutoPlay));
        items.add(UItem.asCheck(unmuteVideosWithVolumeButtonsRow, LocaleController.getString(R.string.UnmuteVideosWithVolumeButtons)).slug("unmuteVideosWithVolumeButtons").setChecked(NekoConfig.unmuteVideosWithVolumeButtons));
        items.add(UItem.asCheck(autoPauseVideoRow, LocaleController.getString(R.string.AutoPauseVideo), LocaleController.getString(R.string.AutoPauseVideoAbout)).slug("autoPauseVideo").setChecked(NekoConfig.autoPauseVideo));
        items.add(UItem.asCheck(preferOriginalQualityRow, LocaleController.getString(R.string.PreferOriginalQuality), LocaleController.getString(R.string.PreferOriginalQualityDesc)).slug("preferOriginalQuality").setChecked(NekoConfig.preferOriginalQuality));
        items.add(TextSettingsCellFactory.of(cameraInVideoMessagesRow, LocaleController.getString(R.string.CameraInVideoMessages), switch (NekoConfig.cameraInVideoMessages) {
            case NekoConfig.CAMERA_ASK -> LocaleController.getString(R.string.AskCamera);
            case NekoConfig.CAMERA_REAR -> LocaleController.getString(R.string.RearCamera);
            default -> LocaleController.getString(R.string.FrontCamera);
        }).slug("cameraInVideoMessages"));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.MessageMenu)));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 1, LocaleController.getString(R.string.DeleteDownloadedFile)).slug("showDeleteDownloadedFile").setChecked(NekoConfig.showDeleteDownloadedFile));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 2, LocaleController.getString(R.string.NoQuoteForward)).slug("showNoQuoteForward").setChecked(NekoConfig.showNoQuoteForward));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 3, LocaleController.getString(R.string.AddToSavedMessages)).slug("showAddToSavedMessages").setChecked(NekoConfig.showAddToSavedMessages));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 4, LocaleController.getString(R.string.Repeat)).slug("showRepeat").setChecked(NekoConfig.showRepeat));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 5, LocaleController.getString(R.string.Prpr)).slug("showPrPr").setChecked(NekoConfig.showPrPr));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 6, LocaleController.getString(R.string.TranslateMessage)).slug("showTranslate").setChecked(NekoConfig.showTranslate));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 7, LocaleController.getString(R.string.ReportChat)).slug("showReport").setChecked(NekoConfig.showReport));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 8, LocaleController.getString(R.string.MessageDetails)).slug("showMessageDetails").setChecked(NekoConfig.showMessageDetails));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 9, LocaleController.getString(R.string.CopyPhoto)).slug("showCopyPhoto").setChecked(NekoConfig.showCopyPhoto));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 10, LocaleController.getString(R.string.SetReminder)).slug("showSetReminder").setChecked(NekoConfig.showSetReminder));
        items.add(TextCheckbox2CellFactory.of(messageMenuRow + 11, LocaleController.getString(R.string.QrCode)).slug("showQrCode").setChecked(NekoConfig.showQrCode));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            items.add(TextCheckbox2CellFactory.of(messageMenuRow + 12, LocaleController.getString(R.string.OpenInExternalApp)).slug("showOpenIn").setChecked(NekoConfig.showOpenIn));
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id == ignoreBlockedRow) {
            NekoConfig.toggleIgnoreBlocked();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.ignoreBlocked);
            }
        } else if (id == hideKeyboardOnChatScrollRow) {
            NekoConfig.toggleHideKeyboardOnChatScroll();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideKeyboardOnChatScroll);
            }
        } else if (id == cameraInVideoMessagesRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.AskCamera));
            types.add(NekoConfig.CAMERA_ASK);
            arrayList.add(LocaleController.getString(R.string.RearCamera));
            types.add(NekoConfig.CAMERA_REAR);
            arrayList.add(LocaleController.getString(R.string.FrontCamera));
            types.add(NekoConfig.CAMERA_FRONT);
            showPopup(arrayList, types.indexOf(NekoConfig.cameraInVideoMessages), item, view, i -> {
                NekoConfig.setCameraInVideoMessages(types.get(i));
                listView.adapter.notifyItemChanged(position, PARTIAL);
            });
        } else if (id == confirmAVRow) {
            NekoConfig.toggleConfirmAVMessage();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.confirmAVMessage);
            }
        } else if (id == disableProximityEventsRow) {
            NekoConfig.toggleDisableProximityEvents();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableProximityEvents);
            }
            showRestartBulletin();
        } else if (id == tryToOpenAllLinksInIVRow) {
            NekoConfig.toggleTryToOpenAllLinksInIV();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.tryToOpenAllLinksInIV);
            }
        } else if (id == autoPauseVideoRow) {
            NekoConfig.toggleAutoPauseVideo();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.autoPauseVideo);
            }
        } else if (id == disableJumpToNextRow) {
            NekoConfig.toggleDisableJumpToNextChannel();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableJumpToNextChannel);
            }
        } else if (id == disableGreetingStickerRow) {
            NekoConfig.toggleDisableGreetingSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableGreetingSticker);
            }
        } else if (id == disableVoiceMessageAutoPlayRow) {
            NekoConfig.toggleDisableVoiceMessageAutoPlay();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableVoiceMessageAutoPlay);
            }
        } else if (id == unmuteVideosWithVolumeButtonsRow) {
            NekoConfig.toggleUnmuteVideosWithVolumeButtons();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.unmuteVideosWithVolumeButtons);
            }
        } else if (id == doubleTapActionRow) {
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
                        item.textValue = NekoConfig.doubleTapInAction == NekoConfig.doubleTapOutAction ?
                                getDoubleTapActionText(NekoConfig.doubleTapInAction) :
                                getDoubleTapActionText(NekoConfig.doubleTapInAction) + ", " + getDoubleTapActionText(NekoConfig.doubleTapOutAction);
                        listView.adapter.notifyItemChanged(position, PARTIAL);
                    });
                }
            }

            builder.setOnPreDismissListener(dialog -> listView.adapter.notifyItemChanged(position, PARTIAL));
            builder.setNegativeButton(LocaleController.getString(R.string.OK), null);
            builder.show();
        } else if (id == markdownEnableRow) {
            NekoConfig.toggleDisableMarkdownByDefault();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(!NekoConfig.disableMarkdownByDefault);
            }
        } else if (id > messageMenuRow) {
            TextCheckbox2Cell cell = ((TextCheckbox2Cell) view);
            int menuPosition = id - messageMenuRow - 1;
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
                NekoConfig.toggleShowTranslate();
                cell.setChecked(NekoConfig.showTranslate);
            } else if (menuPosition == 6) {
                NekoConfig.toggleShowReport();
                cell.setChecked(NekoConfig.showReport);
            } else if (menuPosition == 7) {
                NekoConfig.toggleShowMessageDetails();
                cell.setChecked(NekoConfig.showMessageDetails);
            } else if (menuPosition == 8) {
                NekoConfig.toggleShowCopyPhoto();
                cell.setChecked(NekoConfig.showCopyPhoto);
            } else if (menuPosition == 9) {
                NekoConfig.toggleShowSetReminder();
                cell.setChecked(NekoConfig.showSetReminder);
            } else if (menuPosition == 10) {
                NekoConfig.toggleShowQrCode();
                cell.setChecked(NekoConfig.showQrCode);
            } else if (menuPosition == 11) {
                NekoConfig.toggleShowOpenIn();
                cell.setChecked(NekoConfig.showOpenIn);
            }
        } else if (id == voiceEnhancementsRow) {
            NekoConfig.toggleVoiceEnhancements();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.voiceEnhancements);
            }
        } else if (id == maxRecentStickersRow) {
            int[] counts = {20, 30, 40, 50, 80, 100, 120, 150, 180, 200};
            ArrayList<String> types = new ArrayList<>();
            for (int count : counts) {
                if (count <= getMessagesController().maxRecentStickersCount) {
                    types.add(String.valueOf(count));
                }
            }
            showPopup(types, types.indexOf(String.valueOf(NekoConfig.maxRecentStickers)), item, view, i -> {
                NekoConfig.setMaxRecentStickers(Integer.parseInt(types.get(i)));
                listView.adapter.notifyItemChanged(position, PARTIAL);
            });
        } else if (id == hideTimeOnStickerRow) {
            NekoConfig.toggleHideTimeOnSticker();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideTimeOnSticker);
            }
            var stickerCell = listView.findViewByItemId(stickerSizeRow);
            if (stickerCell != null) stickerCell.invalidate();
        } else if (id == markdownParserRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add("Nekogram");
            arrayList.add("Telegram");
            boolean oldParser = NekoConfig.newMarkdownParser;
            showPopup(arrayList, NekoConfig.newMarkdownParser ? 0 : 1, item, view, i -> {
                NekoConfig.setNewMarkdownParser(i == 0);
                listView.adapter.notifyItemChanged(position, PARTIAL);
                if (oldParser != NekoConfig.newMarkdownParser) {
                    if (oldParser) {
                        notifyItemRemoved(markdownParseLinksRow);
                        updateRows();
                    } else {
                        updateRows();
                        notifyItemInserted(markdownParseLinksRow);
                    }
                    notifyItemChanged(markdown2Row);
                }
            });
        } else if (id == markdownParseLinksRow) {
            NekoConfig.toggleMarkdownParseLinks();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.markdownParseLinks);
            }
            notifyItemChanged(markdown2Row);
        } else if (id == quickForwardRow) {
            NekoConfig.toggleQuickForward();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.quickForward);
            }
        } else if (id == reducedColorsRow) {
            NekoConfig.toggleReducedColors();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.reducedColors);
            }
            var stickerCell = listView.findViewByItemId(stickerSizeRow);
            stickerCell.invalidate();
        } else if (id == showTimeHintRow) {
            NekoConfig.toggleShowTimeHint();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.showTimeHint);
            }
        } else if (id == transcribeProviderRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TranscribeProviderAuto));
            types.add(NekoConfig.TRANSCRIBE_AUTO);
            arrayList.add(LocaleController.getString(R.string.TelegramPremium));
            types.add(NekoConfig.TRANSCRIBE_PREMIUM);
            arrayList.add(LocaleController.getString(R.string.TranscribeProviderWorkersAI));
            types.add(NekoConfig.TRANSCRIBE_WORKERSAI);
            showPopup(arrayList, types.indexOf(NekoConfig.transcribeProvider), item, view, i -> {
                NekoConfig.setTranscribeProvider(types.get(i));
                listView.adapter.notifyItemChanged(position, PARTIAL);
            });
        } else if (id == cfCredentialsRow) {
            WhisperHelper.showCfCredentialsDialog(this);
        } else if (id == preferOriginalQualityRow) {
            NekoConfig.togglePreferOriginalQuality();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.preferOriginalQuality);
            }
        } else if (id == hideChannelBottomButtonsRow) {
            NekoConfig.toggleHideChannelBottomButtons();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideChannelBottomButtons);
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.Chat);
    }

    @Override
    protected String getKey() {
        return "c";
    }

    private static class StickerSizeCellFactory extends UItem.UItemFactory<StickerSizeCell> {
        static {
            setup(new StickerSizeCellFactory());
        }

        @Override
        public StickerSizeCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new StickerSizeCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var cell = (StickerSizeCell) view;
            var frameLayout = (FrameLayout) listView.getParent();
            cell.setFragmentView(frameLayout);
            cell.setValue(item.floatValue);
            cell.setOnDragListener((AltSeekbar.OnDrag) item.object);
        }

        public static UItem of(int id, String title, float value, AltSeekbar.OnDrag onDrag) {
            var item = UItem.ofFactory(StickerSizeCellFactory.class);
            item.id = id;
            item.text = title;
            item.object = onDrag;
            item.floatValue = value;
            return item;
        }

        @Override
        public boolean isClickable() {
            return false;
        }
    }

    private static class StickerSizeCell extends FrameLayout {

        private final StickerSizePreviewMessagesCell messagesCell;
        private final AltSeekbar sizeBar;

        private AltSeekbar.OnDrag onDrag;

        public StickerSizeCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);

            setWillNotDraw(false);

            sizeBar = new AltSeekbar(context, progress -> {
                setValue(progress);
                if (onDrag != null) onDrag.run(progress);
            }, 2, 20, LocaleController.getString(R.string.StickerSize), LocaleController.getString(R.string.StickerSizeLeft), LocaleController.getString(R.string.StickerSizeRight), resourcesProvider);
            sizeBar.setValue(NekoConfig.stickerSize);
            addView(sizeBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            messagesCell = new StickerSizePreviewMessagesCell(context, resourcesProvider);
            messagesCell.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
            addView(messagesCell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP, 0, 112, 0, 0));
        }

        public void setOnDragListener(AltSeekbar.OnDrag onDrag) {
            this.onDrag = onDrag;
        }

        public void setFragmentView(FrameLayout fragmentView) {
            messagesCell.setFragmentView(fragmentView);
        }

        public void setValue(float value) {
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
    private static class AltSeekbar extends FrameLayout {

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

        public AltSeekbar(Context context, AltSeekbar.OnDrag onDrag, int min, int max, String title, String left, String right, Theme.ResourcesProvider resourcesProvider) {
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
}
