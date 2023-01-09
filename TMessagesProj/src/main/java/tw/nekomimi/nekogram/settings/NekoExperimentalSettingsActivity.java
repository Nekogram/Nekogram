package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.TypedValue;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
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
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.helpers.remote.AnalyticsHelper;

public class NekoExperimentalSettingsActivity extends BaseNekoSettingsActivity {

    private final boolean sensitiveCanChange;
    private boolean sensitiveEnabled;

    private int experimentRow;
    private int downloadSpeedBoostRow;
    private int uploadSpeedBoostRow;
    private int mapDriftingFixRow;
    private int disableFilteringRow;
    private int sendLargePhotosRow;
    private int showRPCErrorRow;
    private int experiment2Row;

    private int dataRow;
    private int sendBugReportRow;
    private int deleteDataRow;
    private int data2Row;

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
            AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
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
                final AlertDialog progressDialog = new AlertDialog(getParentActivity(), AlertDialog.ALERT_TYPE_SPINNER);
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
                            getMessageHelper().deleteUserHistoryWithSearch(NekoExperimentalSettingsActivity.this, TLdialog.id, 0, 0, null);
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
                listAdapter.notifyItemChanged(downloadSpeedBoostRow, PARTIAL);
            });
        } else if (position == sendBugReportRow) {
            if (AnalyticsHelper.analyticsDisabled) {
                return;
            }
            AnalyticsHelper.toggleSendBugReport();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AnalyticsHelper.sendBugReport);
            }
        } else if (position == deleteDataRow) {
            if (AnalyticsHelper.analyticsDisabled) {
                return;
            }
            AnalyticsHelper.setAnalyticsDisabled();
            listAdapter.notifyItemRangeChanged(sendBugReportRow, 2);
        } else if (position == sendLargePhotosRow) {
            NekoConfig.toggleSendLargePhotos();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.sendLargePhotos);
            }
        }
    }

    @Override
    protected String getKey() {
        return "e";
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("NotificationsOther", R.string.NotificationsOther);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        experimentRow = addRow("experiment");
        downloadSpeedBoostRow = MessagesController.getInstance(currentAccount).getfileExperimentalParams ? -1 : addRow("downloadSpeedBoost");
        uploadSpeedBoostRow = addRow("uploadSpeedBoost");
        mapDriftingFixRow = addRow("mapDriftingFix");
        disableFilteringRow = sensitiveCanChange ? addRow("disableFiltering") : -1;
        sendLargePhotosRow = addRow("sendLargePhotosRow");
        showRPCErrorRow = addRow("showRPCError");
        experiment2Row = addRow();

        if (AnalyticsHelper.isSettingsAvailable()) {
            dataRow = addRow();
            sendBugReportRow = addRow();
            deleteDataRow = addRow();
            data2Row = addRow();
        } else {
            dataRow = -1;
            sendBugReportRow = -1;
            deleteDataRow = -1;
            data2Row = -1;
        }

        deleteAccountRow = addRow("deleteAccount");
        deleteAccount2Row = addRow();
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_SHADOW: {
                    if (position == deleteAccount2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == deleteAccountRow) {
                        textCell.setText(LocaleController.getString("DeleteAccount", R.string.DeleteAccount), false);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteRedText));
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
                        textCell.setTextAndValue(LocaleController.getString("DownloadSpeedBoost", R.string.DownloadSpeedBoost), value, partial, true);
                    }
                    break;
                }
                case TYPE_CHECK: {
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
                    } else if (position == sendBugReportRow) {
                        textCell.setEnabled(!AnalyticsHelper.analyticsDisabled, null);
                        textCell.setTextAndValueAndCheck(LocaleController.getString("SendBugReport", R.string.SendBugReport), LocaleController.getString("SendBugReportDesc", R.string.SendBugReportDesc), !AnalyticsHelper.analyticsDisabled && AnalyticsHelper.sendBugReport, true, true);
                    } else if (position == sendLargePhotosRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("SendLargePhotos", R.string.SendLargePhotos), LocaleController.getString("SendLargePhotosAbout", R.string.SendLargePhotosAbout), NekoConfig.sendLargePhotos, true, true);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == experimentRow) {
                        headerCell.setText(LocaleController.getString("Experiment", R.string.Experiment));
                    } else if (position == dataRow) {
                        headerCell.setText(LocaleController.getString("SendAnonymousData", R.string.SendAnonymousData));
                    }
                    break;
                }
                case TYPE_DETAIL_SETTINGS: {
                    TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                    cell.setEnabled(true);
                    if (position == deleteDataRow) {
                        cell.setEnabled(!AnalyticsHelper.analyticsDisabled);
                        cell.setMultilineDetail(true);
                        cell.setTextAndValue(LocaleController.getString("AnonymousDataDelete", R.string.AnonymousDataDelete), LocaleController.getString("AnonymousDataDeleteDesc", R.string.AnonymousDataDeleteDesc), false);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == data2Row) {
                        cell.setText(LocaleController.getString("SendAnonymousDataDesc", R.string.SendAnonymousDataDesc));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == sendBugReportRow || position == deleteDataRow) {
                return !AnalyticsHelper.analyticsDisabled;
            }
            return super.isEnabled(holder);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == experiment2Row || position == deleteAccount2Row) {
                return TYPE_SHADOW;
            } else if (position == deleteAccountRow || position == downloadSpeedBoostRow) {
                return TYPE_SETTINGS;
            } else if (position > downloadSpeedBoostRow && position <= showRPCErrorRow || position == sendBugReportRow) {
                return TYPE_CHECK;
            } else if (position == experimentRow || position == dataRow) {
                return TYPE_HEADER;
            } else if (position == deleteDataRow) {
                return TYPE_DETAIL_SETTINGS;
            } else if (position == data2Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }
    }
}
