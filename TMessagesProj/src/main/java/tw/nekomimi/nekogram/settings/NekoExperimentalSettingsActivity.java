package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
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
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;

@SuppressLint({"RtlHardcoded", "NotifyDataSetChanged"})
public class NekoExperimentalSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private final boolean sensitiveCanChange;
    private boolean sensitiveEnabled;

    private int rowCount;

    private int experimentRow;
    private int emojiRow;
    private int mapDriftingFixRow;
    private int increaseVoiceMessageQualityRow;
    private int codeSyntaxHighlightRow;
    private int saveCacheToExternalFilesDirRow;
    private int disableFilteringRow;
    private int unlimitedFavedStickersRow;
    private int unlimitedPinnedDialogsRow;
    private int maxRecentStickersRow;
    private int experiment2Row;

    private int deleteAccountRow;
    private int shouldNOTTrustMeRow;
    private int hidden2Row;

    NekoExperimentalSettingsActivity(boolean sensitiveCanChange, boolean sensitiveEnabled) {
        this.sensitiveCanChange = sensitiveCanChange;
        this.sensitiveEnabled = sensitiveEnabled;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("Experiment", R.string.Experiment));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
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
            if (position == saveCacheToExternalFilesDirRow) {
                NekoConfig.toggleSaveCacheToExternalFilesDir();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.saveCacheToExternalFilesDir);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.formatString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect)).show();
            } else if (position == disableFilteringRow) {
                sensitiveEnabled = !sensitiveEnabled;
                TLRPC.TL_account_setContentSettings req = new TLRPC.TL_account_setContentSettings();
                req.sensitive_enabled = sensitiveEnabled;
                AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                progressDialog.show();
                getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                    progressDialog.dismiss();
                    if (error == null) {
                        if (response instanceof TLRPC.TL_boolTrue && view instanceof TextCheckCell) {
                            ((TextCheckCell) view).setChecked(sensitiveEnabled);
                        }
                    } else {
                        AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, this, req));
                    }
                }));
            } else if (position == unlimitedFavedStickersRow) {
                NekoConfig.toggleUnlimitedFavedStickers();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.unlimitedFavedStickers);
                }
            } else if (position == deleteAccountRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setMessage(LocaleController.getString("TosDeclineDeleteAccount", R.string.TosDeclineDeleteAccount));
                builder.setTitle(LocaleController.getString("DeleteAccount", R.string.DeleteAccount));
                builder.setPositiveButton(LocaleController.getString("Deactivate", R.string.Deactivate), (dialog, which) -> {
                    ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>(getMessagesController().getAllDialogs());
                    for (TLRPC.Dialog TLdialog : dialogs) {
                        if (TLdialog instanceof TLRPC.TL_dialogFolder) {
                            continue;
                        }
                        TLRPC.Peer peer = getMessagesController().getPeer((int) TLdialog.id);
                        if (peer.channel_id != 0) {
                            TLRPC.Chat chat = getMessagesController().getChat(peer.channel_id);
                            if (!chat.broadcast) {
                                getMessageHelper().deleteUserHistoryWithSearch(NekoExperimentalSettingsActivity.this, TLdialog.id, 0, null);
                            }
                        }
                        if (peer.user_id != 0) {
                            getMessagesController().deleteDialog(TLdialog.id, 0, true);
                        }
                    }
                    AlertDialog.Builder builder12 = new AlertDialog.Builder(getParentActivity());
                    builder12.setMessage(LocaleController.getString("TosDeclineDeleteAccount", R.string.TosDeclineDeleteAccount));
                    builder12.setTitle(LocaleController.getString("DeleteAccount", R.string.DeleteAccount));
                    builder12.setPositiveButton(LocaleController.getString("Deactivate", R.string.Deactivate), (dialogInterface, i) -> {
                        final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                        progressDialog.setCanCacnel(false);

                        TLRPC.TL_account_deleteAccount req = new TLRPC.TL_account_deleteAccount();
                        req.reason = "Meow";
                        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
                            try {
                                progressDialog.dismiss();
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                            if (response instanceof TLRPC.TL_boolTrue) {
                                getMessagesController().performLogout(0);
                            } else if (error == null || error.code != -1000) {
                                String errorText = LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred);
                                if (error != null) {
                                    errorText += "\n" + error.text;
                                }
                                AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity());
                                builder1.setTitle(LocaleController.getString("AppName", R.string.AppName));
                                builder1.setMessage(errorText);
                                builder1.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                                builder1.show();
                            }
                        }));
                        progressDialog.show();
                    });
                    builder12.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    AlertDialog dialog12 = builder12.create();
                    showDialog(dialog12);
                    TextView button = (TextView) dialog12.getButton(DialogInterface.BUTTON_POSITIVE);
                    if (button != null) {
                        button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show();
                AlertDialog dialog = builder.create();
                showDialog(dialog);
                TextView button = (TextView) dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                if (button != null) {
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                }
            } else if (position == unlimitedPinnedDialogsRow) {
                NekoConfig.toggleUnlimitedPinnedDialogs();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.unlimitedPinnedDialogs);
                }
            } else if (position == shouldNOTTrustMeRow) {
                NekoConfig.toggleShouldNOTTrustMe();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.shouldNOTTrustMe);
                }
            } else if (position == emojiRow) {
                if (!TextUtils.isEmpty(NekoConfig.customEmojiFontPath) && (LocaleController.isRTL && x <= AndroidUtilities.dp(76) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(76))) {
                    NotificationsCheckCell checkCell = (NotificationsCheckCell) view;
                    NekoConfig.toggleCustomEmojiFont();
                    checkCell.setChecked(NekoConfig.customEmojiFont, 0);
                } else {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    startActivityForResult(intent, 36654);
                }
            } else if (position == mapDriftingFixRow) {
                NekoConfig.toggleMapDriftingFix();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.mapDriftingFix);
                }
            } else if (position == increaseVoiceMessageQualityRow) {
                NekoConfig.toggleIncreaseVoiceMessageQuality();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.increaseVoiceMessageQuality);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.formatString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect)).show();
            } else if (position == maxRecentStickersRow) {
                int[] counts = {20, 30, 40, 50, 80, 100, 120, 150, 180, 200};
                ArrayList<String> types = new ArrayList<>();
                for (int count : counts) {
                    if (count <= getMessagesController().maxRecentStickersCount) {
                        types.add(String.valueOf(count));
                    }
                }
                PopupHelper.show(types, LocaleController.getString("MaxRecentStickers", R.string.MaxRecentStickers), types.indexOf(String.valueOf(NekoConfig.maxRecentStickers)), context, view, i -> {
                    NekoConfig.setMaxRecentStickers(Integer.parseInt(types.get(i)));
                    listAdapter.notifyItemChanged(maxRecentStickersRow);
                });
            } else if (position == codeSyntaxHighlightRow) {
                NekoConfig.toggleCodeSyntaxHighlight();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.codeSyntaxHighlight);
                }
            }
        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (position == emojiRow) {
                try {
                    if (NekoConfig.customEmojiFont) NekoConfig.toggleCustomEmojiFont();
                    //noinspection ResultOfMethodCallIgnored
                    new File(NekoConfig.customEmojiFontPath).delete();
                    NekoConfig.setCustomEmojiFontPath(null);
                } catch (Exception e) {
                    //
                }
                listAdapter.notifyItemChanged(emojiRow);
            }
            return false;
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

    public String getFileName(Uri uri) {
        String result = null;
        try (Cursor cursor = getParentActivity().getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
            }
        }
        return result;
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == 36654) {
            if (data != null) {
                Uri uri = data.getData();
                if (uri != null) {
                    try {
                        InputStream os = getParentActivity().getContentResolver().openInputStream(uri);
                        String fileName = getFileName(uri);
                        File dest = new File(ApplicationLoader.applicationContext.getExternalFilesDir(null), fileName == null ? "emoji.ttf" : fileName);
                        AndroidUtilities.copyFile(os, dest);
                        if (NekoConfig.setCustomEmojiFontPath(dest.toString())) {
                            if (!NekoConfig.customEmojiFont) NekoConfig.toggleCustomEmojiFont();
                        } else {
                            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString("InvalidCustomEmojiTypeface", R.string.InvalidCustomEmojiTypeface)).show();
                            //noinspection ResultOfMethodCallIgnored
                            dest.delete();
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                        AlertsCreator.showSimpleAlert(this, e.getLocalizedMessage());
                    }
                }
            }
        }
    }

    private void updateRows() {
        rowCount = 0;

        experimentRow = rowCount++;
        emojiRow = rowCount++;
        mapDriftingFixRow = rowCount++;
        increaseVoiceMessageQualityRow = rowCount++;
        codeSyntaxHighlightRow = rowCount++;
        saveCacheToExternalFilesDirRow = BuildVars.NO_SCOPED_STORAGE ? rowCount++ : -1;
        disableFilteringRow = sensitiveCanChange ? rowCount++ : -1;
        unlimitedFavedStickersRow = rowCount++;
        unlimitedPinnedDialogsRow = rowCount++;
        maxRecentStickersRow = rowCount++;
        experiment2Row = rowCount++;
        if (NekoConfig.showHiddenFeature) {
            deleteAccountRow = rowCount++;
            shouldNOTTrustMeRow = rowCount++;
            hidden2Row = rowCount++;
        } else {
            deleteAccountRow = -1;
            shouldNOTTrustMeRow = -1;
            hidden2Row = -1;
        }
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
                    if (position == experiment2Row && hidden2Row == -1 || position == hidden2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == deleteAccountRow) {
                        textCell.setText(LocaleController.getString("DeleteAccount", R.string.DeleteAccount), true);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText));
                    } else if (position == emojiRow) {
                        textCell.setText(LocaleController.getString("CustomEmojiTypeface", R.string.CustomEmojiTypeface), true);
                    } else if (position == maxRecentStickersRow) {
                        textCell.setTextAndValue(LocaleController.getString("MaxRecentStickers", R.string.MaxRecentStickers), String.valueOf(NekoConfig.maxRecentStickers), false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == saveCacheToExternalFilesDirRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("SaveCacheToExternalFilesDir", R.string.SaveCacheToExternalFilesDir), LocaleController.getString("SaveCacheToExternalFilesDirAbout", R.string.SaveCacheToExternalFilesDirAbout), NekoConfig.saveCacheToExternalFilesDir, true, true);
                    } else if (position == disableFilteringRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("SensitiveDisableFiltering", R.string.SensitiveDisableFiltering), LocaleController.getString("SensitiveAbout", R.string.SensitiveAbout), sensitiveEnabled, true, true);
                        textCell.setEnabled(sensitiveCanChange, null);
                    } else if (position == unlimitedFavedStickersRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("UnlimitedFavoredStickers", R.string.UnlimitedFavoredStickers), LocaleController.getString("UnlimitedFavoredStickersAbout", R.string.UnlimitedFavoredStickersAbout), NekoConfig.unlimitedFavedStickers, true, true);
                    } else if (position == unlimitedPinnedDialogsRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("UnlimitedPinnedDialogs", R.string.UnlimitedPinnedDialogs), LocaleController.getString("UnlimitedPinnedDialogsAbout", R.string.UnlimitedPinnedDialogsAbout), NekoConfig.unlimitedPinnedDialogs, true, true);
                    } else if (position == mapDriftingFixRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MapDriftingFix", R.string.MapDriftingFix), NekoConfig.mapDriftingFix, true);
                    } else if (position == increaseVoiceMessageQualityRow) {
                        textCell.setTextAndCheck(LocaleController.getString("IncreaseVoiceMessageQuality", R.string.IncreaseVoiceMessageQuality), NekoConfig.increaseVoiceMessageQuality, true);
                    } else if (position == codeSyntaxHighlightRow) {
                        textCell.setTextAndCheck(LocaleController.getString("CodeSyntaxHighlight", R.string.CodeSyntaxHighlight), NekoConfig.codeSyntaxHighlight, true);
                    } else if (position == shouldNOTTrustMeRow) {
                        textCell.setTextAndCheck("", NekoConfig.shouldNOTTrustMe, false);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == experimentRow) {
                        headerCell.setText(LocaleController.getString("Experiment", R.string.Experiment));
                    }
                    break;
                }
                case 5: {
                    NotificationsCheckCell textCell = (NotificationsCheckCell) holder.itemView;
                    if (position == emojiRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("CustomEmojiTypeface", R.string.CustomEmojiTypeface), new File(NekoConfig.customEmojiFontPath).getName(), NekoConfig.customEmojiFont, true);
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3 || type == 6 || type == 5;
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
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == experiment2Row || position == hidden2Row) {
                return 1;
            } else if (position == deleteAccountRow) {
                return 2;
            } else if (position > emojiRow && position <= unlimitedPinnedDialogsRow) {
                return 3;
            } else if (position == experimentRow) {
                return 4;
            } else if (position == emojiRow) {
                return TextUtils.isEmpty(NekoConfig.customEmojiFontPath) ? 2 : 5;
            } else if (position == shouldNOTTrustMeRow) {
                return 3;
            }
            return 2;
        }
    }
}
