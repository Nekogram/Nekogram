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
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
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
import tw.nekomimi.nekogram.helpers.PopupHelper;

public class NekoExperimentalSettingsActivity extends BaseNekoSettingsActivity {

    private final boolean sensitiveCanChange;
    private boolean sensitiveEnabled;

    private int experimentRow;
    private int emojiRow;
    private int downloadSpeedBoostRow;
    private int uploadSpeedBoostRow;
    private int mapDriftingFixRow;
    private int disableFilteringRow;
    private int showRPCErrorRow;
    private int experiment2Row;

    private int deleteAccountRow;
    private int deleteAccount2Row;

    public NekoExperimentalSettingsActivity(boolean sensitiveCanChange, boolean sensitiveEnabled) {
        this.sensitiveCanChange = sensitiveCanChange;
        this.sensitiveEnabled = sensitiveEnabled;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (false) {
            var builder = new AlertDialog.Builder(getParentActivity());
            var message = new TextView(getParentActivity());
            message.setText(getSpannedString("SoonRemovedOption", R.string.SoonRemovedOption, "https://t.me/" + LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername)));
            message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            message.setLinkTextColor(Theme.getColor(Theme.key_dialogTextLink));
            message.setHighlightColor(Theme.getColor(Theme.key_dialogLinkSelection));
            message.setPadding(AndroidUtilities.dp(23), 0, AndroidUtilities.dp(23), 0);
            message.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
            message.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            builder.setView(message);
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            showDialog(builder.create());
        }
        if (position == disableFilteringRow) {
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
        } else if (position == showRPCErrorRow) {
            NekoConfig.toggleShowRPCError();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.showRPCError);
            }
        } else if (position == uploadSpeedBoostRow) {
            NekoConfig.toggleUploadSpeedBoost();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.uploadSpeedBoost);
            }
        } else if (position == downloadSpeedBoostRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("DownloadSpeedBoostNone", R.string.DownloadSpeedBoostNone));
            types.add(NekoConfig.BOOST_NONE);
            arrayList.add(LocaleController.getString("DownloadSpeedBoostAverage", R.string.DownloadSpeedBoostAverage));
            types.add(NekoConfig.BOOST_AVERAGE);
            arrayList.add(LocaleController.getString("DownloadSpeedBoostExtreme", R.string.DownloadSpeedBoostExtreme));
            types.add(NekoConfig.BOOST_EXTREME);
            PopupHelper.show(arrayList, LocaleController.getString("DownloadSpeedBoost", R.string.DownloadSpeedBoost), types.indexOf(NekoConfig.downloadSpeedBoost), getParentActivity(), view, i -> {
                NekoConfig.setDownloadSpeedBoost(types.get(i));
                listAdapter.notifyItemChanged(downloadSpeedBoostRow);
            });
        }
    }

    @Override
    protected String getKey() {
        return "e";
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        if (position == emojiRow) {
            if (!TextUtils.isEmpty(NekoConfig.customEmojiFontPath)) {
                try {
                    if (NekoConfig.customEmojiFont) NekoConfig.toggleCustomEmojiFont();
                    //noinspection ResultOfMethodCallIgnored
                    new File(NekoConfig.customEmojiFontPath).delete();
                    NekoConfig.setCustomEmojiFontPath(null);
                } catch (Exception e) {
                    //
                }
                listAdapter.notifyItemChanged(emojiRow);
                return true;
            }
        }
        return false;
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("Experiment", R.string.Experiment);
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
        super.updateRows();

        experimentRow = addRow("experiment");
        emojiRow = addRow("emoji");
        downloadSpeedBoostRow = MessagesController.getInstance(currentAccount).getfileExperimentalParams ? -1 : addRow("downloadSpeedBoost");
        uploadSpeedBoostRow = addRow("uploadSpeedBoost");
        mapDriftingFixRow = addRow("mapDriftingFix");
        disableFilteringRow = sensitiveCanChange ? addRow("disableFiltering") : -1;
        showRPCErrorRow = addRow("showRPCError");
        experiment2Row = addRow();
        deleteAccountRow = addRow("deleteAccount");
        deleteAccount2Row = addRow();
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
                    } else if (position == downloadSpeedBoostRow) {
                        String value;
                        switch (NekoConfig.downloadSpeedBoost) {
                            case NekoConfig.BOOST_NONE:
                                value = LocaleController.getString("DownloadSpeedBoostNone", R.string.DownloadSpeedBoostNone);
                                break;
                            case NekoConfig.BOOST_EXTREME:
                                value = LocaleController.getString("DownloadSpeedBoostExtreme", R.string.DownloadSpeedBoostExtreme);
                                break;
                            default:
                            case NekoConfig.BOOST_AVERAGE:
                                value = LocaleController.getString("DownloadSpeedBoostAverage", R.string.DownloadSpeedBoostAverage);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("DownloadSpeedBoost", R.string.DownloadSpeedBoost), value, true);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == disableFilteringRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("SensitiveDisableFiltering", R.string.SensitiveDisableFiltering), LocaleController.getString("SensitiveAbout", R.string.SensitiveAbout), sensitiveEnabled, true, true);
                        textCell.setEnabled(sensitiveCanChange, null);
                    } else if (position == mapDriftingFixRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MapDriftingFix", R.string.MapDriftingFix), NekoConfig.mapDriftingFix, true);
                    } else if (position == showRPCErrorRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("ShowRPCError", R.string.ShowRPCError), LocaleController.formatString("ShowRPCErrorException", R.string.ShowRPCErrorException, "FILE_REFERENCE_EXPIRED"), NekoConfig.showRPCError, true, false);
                    } else if (position == uploadSpeedBoostRow) {
                        textCell.setTextAndCheck(LocaleController.getString("UploadloadSpeedBoost", R.string.UploadloadSpeedBoost), NekoConfig.uploadSpeedBoost, true);
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
            } else if (position == deleteAccountRow || position == downloadSpeedBoostRow) {
                return 2;
            } else if (position > downloadSpeedBoostRow && position <= showRPCErrorRow) {
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
