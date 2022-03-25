package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

public class NekoExperimentalSettingsActivity extends BaseNekoSettingsActivity {

    private final boolean sensitiveCanChange;
    private boolean sensitiveEnabled;

    private int experimentRow;
    private int emojiRow;
    private int mapDriftingFixRow;
    private int codeSyntaxHighlightRow;
    private int saveCacheToExternalFilesDirRow;
    private int disableFilteringRow;
    private int unlimitedFavedStickersRow;
    private int unlimitedPinnedDialogsRow;
    private int keepFormattingRow;
    private int showRPCErrorRow;
    private int experiment2Row;

    private int deleteAccountRow;
    private int deleteAccount2Row;

    NekoExperimentalSettingsActivity(boolean sensitiveCanChange, boolean sensitiveEnabled) {
        this.sensitiveCanChange = sensitiveCanChange;
        this.sensitiveEnabled = sensitiveEnabled;
    }

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);
        actionBar.setTitle(LocaleController.getString("Experiment", R.string.Experiment));

        listAdapter = new ListAdapter(context);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == saveCacheToExternalFilesDirRow) {
                NekoConfig.toggleSaveCacheToExternalFilesDir();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.saveCacheToExternalFilesDir);
                }
                showRestartBulletin();
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
                    if (BuildConfig.DEBUG) return;
                    final AlertDialog progressDialog = new AlertDialog(getParentActivity(), 3);
                    progressDialog.setCanCancel(false);

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

                    Utilities.globalQueue.postRunnable(() -> {
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
                    }, 20000);
                    progressDialog.show();
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialog1 -> {
                    var button = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                    button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
                    button.setEnabled(false);
                    var buttonText = button.getText();
                    new CountDownTimer(60000, 100) {
                        @Override
                        public void onTick(long millisUntilFinished) {
                            button.setText(String.format(Locale.getDefault(), "%s (%d)", buttonText, millisUntilFinished / 1000 + 1));
                        }

                        @Override
                        public void onFinish() {
                            button.setText(buttonText);
                            button.setEnabled(true);
                        }
                    }.start();
                });
                showDialog(dialog);
            } else if (position == unlimitedPinnedDialogsRow) {
                NekoConfig.toggleUnlimitedPinnedDialogs();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.unlimitedPinnedDialogs);
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
            } else if (position == codeSyntaxHighlightRow) {
                NekoConfig.toggleCodeSyntaxHighlight();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.codeSyntaxHighlight);
                }
            } else if (position == showRPCErrorRow) {
                NekoConfig.toggleShowRPCError();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.showRPCError);
                }
            } else if (position == keepFormattingRow) {
                NekoConfig.toggleKeepFormatting();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.keepFormatting);
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

    @SuppressLint("Range")
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

    @Override
    protected void updateRows() {
        rowCount = 0;

        experimentRow = rowCount++;
        emojiRow = rowCount++;
        mapDriftingFixRow = rowCount++;
        codeSyntaxHighlightRow = rowCount++;
        saveCacheToExternalFilesDirRow = BuildVars.NO_SCOPED_STORAGE ? rowCount++ : -1;
        disableFilteringRow = sensitiveCanChange ? rowCount++ : -1;
        unlimitedFavedStickersRow = rowCount++;
        unlimitedPinnedDialogsRow = rowCount++;
        keepFormattingRow = rowCount++;
        showRPCErrorRow = rowCount++;
        experiment2Row = rowCount++;
        deleteAccountRow = rowCount++;
        deleteAccount2Row = rowCount++;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if (position == deleteAccount2Row) {
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
                        textCell.setText(LocaleController.getString("DeleteAccount", R.string.DeleteAccount), false);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText));
                    } else if (position == emojiRow) {
                        textCell.setText(LocaleController.getString("CustomEmojiTypeface", R.string.CustomEmojiTypeface), true);
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
                    } else if (position == codeSyntaxHighlightRow) {
                        textCell.setTextAndCheck(LocaleController.getString("CodeSyntaxHighlight", R.string.CodeSyntaxHighlight), NekoConfig.codeSyntaxHighlight, true);
                    } else if (position == showRPCErrorRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("ShowRPCError", R.string.ShowRPCError), LocaleController.formatString("ShowRPCErrorException", R.string.ShowRPCErrorException, "FILE_REFERENCE_EXPIRED"), NekoConfig.showRPCError, true, false);
                    } else if (position == keepFormattingRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("KeepFormatting", R.string.KeepFormatting), LocaleController.getString("KeepFormattingAbout", R.string.KeepFormattingAbout), NekoConfig.keepFormatting, true, true);
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
        public int getItemViewType(int position) {
            if (position == experiment2Row || position == deleteAccount2Row) {
                return 1;
            } else if (position == deleteAccountRow) {
                return 2;
            } else if (position > emojiRow && position <= showRPCErrorRow) {
                return 3;
            } else if (position == experimentRow) {
                return 4;
            } else if (position == emojiRow) {
                return TextUtils.isEmpty(NekoConfig.customEmojiFontPath) ? 2 : 5;
            }
            return 2;
        }
    }
}
