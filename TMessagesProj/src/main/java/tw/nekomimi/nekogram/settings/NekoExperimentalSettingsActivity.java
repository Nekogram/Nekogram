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
import org.telegram.tgnet.tl.TL_account;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.AnalyticsHelper;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.helpers.SettingsHelper;

public class NekoExperimentalSettingsActivity extends BaseNekoSettingsActivity {

    private int experimentRow;
    private int downloadSpeedBoostRow;
    private int keepFormattingRow;
    private int autoInlineBotRow;
    private int forceFontWeightFallbackRow;
    private int mapDriftingFixRow;
    private int contentRestrictionRow;
    private int showRPCErrorRow;
    private int experiment2Row;

    private int dataRow;
    private int sendBugReportRow;
    private int deleteDataRow;
    private int copyReportIdRow;
    private int data2Row;

    private int deleteAccountRow;
    private int deleteAccount2Row;

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (false) {
            var builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
            var message = new TextView(getParentActivity());
            message.setText(getSpannedString(R.string.SoonRemovedOption, "https://t.me/" + LocaleController.getString(R.string.OfficialChannelUsername)));
            message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            message.setLinkTextColor(getThemedColor(Theme.key_dialogTextLink));
            message.setHighlightColor(getThemedColor(Theme.key_dialogLinkSelection));
            message.setPadding(AndroidUtilities.dp(23), 0, AndroidUtilities.dp(23), 0);
            message.setMovementMethod(new AndroidUtilities.LinkMovementMethodMy());
            message.setTextColor(getThemedColor(Theme.key_dialogTextBlack));
            builder.setView(message);
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
            showDialog(builder.create());
        }
        if (position == deleteAccountRow) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
            builder.setMessage(LocaleController.getString(R.string.TosDeclineDeleteAccount));
            builder.setTitle(LocaleController.getString(R.string.DeleteAccount));
            builder.setPositiveButton(LocaleController.getString(R.string.Deactivate), (dialog, which) -> {
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
                            getMessageHelper().deleteUserHistoryWithSearch(NekoExperimentalSettingsActivity.this, TLdialog.id);
                        }
                    }
                    if (peer.user_id != 0) {
                        getMessagesController().deleteDialog(TLdialog.id, 0, true);
                    }
                }

                Utilities.globalQueue.postRunnable(() -> {
                    TL_account.deleteAccount req = new TL_account.deleteAccount();
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
                            String errorText = LocaleController.getString(R.string.ErrorOccurred);
                            if (error != null) {
                                errorText += "\n" + error.text;
                            }
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
                            builder1.setTitle(LocaleController.getString(R.string.AppName));
                            builder1.setMessage(errorText);
                            builder1.setPositiveButton(LocaleController.getString(R.string.OK), null);
                            builder1.show();
                        }
                    }));
                }, 20000);
                progressDialog.show();
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            AlertDialog dialog = builder.create();
            dialog.setOnShowListener(dialog1 -> {
                var button = (TextView) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                button.setTextColor(getThemedColor(Theme.key_text_RedBold));
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
        } else if (position == downloadSpeedBoostRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.DownloadSpeedBoostNone));
            types.add(NekoConfig.BOOST_NONE);
            arrayList.add(LocaleController.getString(R.string.DownloadSpeedBoostAverage));
            types.add(NekoConfig.BOOST_AVERAGE);
            arrayList.add(LocaleController.getString(R.string.DownloadSpeedBoostExtreme));
            types.add(NekoConfig.BOOST_EXTREME);
            PopupHelper.show(arrayList, LocaleController.getString(R.string.DownloadSpeedBoost), types.indexOf(NekoConfig.downloadSpeedBoost), getParentActivity(), view, i -> {
                NekoConfig.setDownloadSpeedBoost(types.get(i));
                listAdapter.notifyItemChanged(downloadSpeedBoostRow, PARTIAL);
            }, resourcesProvider);
        } else if (position == sendBugReportRow) {
            if (AnalyticsHelper.analyticsDisabled) {
                return;
            }
            AnalyticsHelper.toggleSendBugReport();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(AnalyticsHelper.sendBugReport);
            }
            listAdapter.notifyItemChanged(copyReportIdRow);
        } else if (position == deleteDataRow) {
            if (AnalyticsHelper.analyticsDisabled) {
                return;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);
            builder.setTitle(LocaleController.getString(R.string.AnonymousDataDelete));
            builder.setMessage(LocaleController.getString(R.string.AnonymousDataDeleteDesc));
            builder.setPositiveButton(LocaleController.getString(R.string.Delete), (dialog, which) -> {
                AnalyticsHelper.setAnalyticsDisabled();
                listAdapter.notifyItemRangeChanged(sendBugReportRow, 3);
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            AlertDialog dialog = builder.create();
            showDialog(dialog);
            dialog.redPositive();
        } else if (position == contentRestrictionRow) {
            NekoConfig.toggleIgnoreContentRestriction();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.ignoreContentRestriction);
            }
        } else if (position == copyReportIdRow) {
            if (AnalyticsHelper.analyticsDisabled || !AnalyticsHelper.sendBugReport) {
                return;
            }
            SettingsHelper.copyReportId();
        } else if (position == autoInlineBotRow) {
            NekoConfig.toggleAutoInlineBot();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.autoInlineBot);
            }
        } else if (position == forceFontWeightFallbackRow) {
            NekoConfig.toggleForceFontWeightFallback();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.forceFontWeightFallback);
            }
            showRestartBulletin();
        } else if (position == keepFormattingRow) {
            NekoConfig.toggleKeepFormatting();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.keepFormatting);
            }
        }
    }

    @Override
    public Integer getSelectorColor(int position) {
        if (position == deleteAccountRow) {
            return Theme.multAlpha(getThemedColor(Theme.key_text_RedRegular), .1f);
        }
        return super.getSelectorColor(position);
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
        return LocaleController.getString(R.string.NotificationsOther);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        experimentRow = addRow("experiment");
        downloadSpeedBoostRow = MessagesController.getInstance(currentAccount).getfileExperimentalParams ? -1 : addRow("downloadSpeedBoost");
        keepFormattingRow = addRow("keepFormatting");
        autoInlineBotRow = addRow("autoInlineBot");
        forceFontWeightFallbackRow = addRow("forceFontWeightFallback");
        mapDriftingFixRow = addRow("mapDriftingFix");
        contentRestrictionRow = Extra.isDirectApp() ? addRow("contentRestriction") : -1;
        showRPCErrorRow = addRow("showRPCError");
        experiment2Row = addRow();

        if (AnalyticsHelper.isSettingsAvailable()) {
            dataRow = addRow();
            sendBugReportRow = addRow();
            deleteDataRow = addRow();
        } else {
            dataRow = -1;
            sendBugReportRow = -1;
            deleteDataRow = -1;
        }
        copyReportIdRow = addRow("copyReportId");
        data2Row = addRow();

        deleteAccountRow = addRow("deleteAccount");
        deleteAccount2Row = addRow();
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == deleteAccountRow) {
                        textCell.setText(LocaleController.getString(R.string.DeleteAccount), divider);
                        textCell.setTextColor(getThemedColor(Theme.key_text_RedRegular));
                    } else if (position == downloadSpeedBoostRow) {
                        String value = switch (NekoConfig.downloadSpeedBoost) {
                            case NekoConfig.BOOST_NONE ->
                                    LocaleController.getString(R.string.DownloadSpeedBoostNone);
                            case NekoConfig.BOOST_EXTREME ->
                                    LocaleController.getString(R.string.DownloadSpeedBoostExtreme);
                            default ->
                                    LocaleController.getString(R.string.DownloadSpeedBoostAverage);
                        };
                        textCell.setTextAndValue(LocaleController.getString(R.string.DownloadSpeedBoost), value, partial, divider);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == mapDriftingFixRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.MapDriftingFix), NekoConfig.mapDriftingFix, divider);
                    } else if (position == showRPCErrorRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.ShowRPCError), LocaleController.formatString(R.string.ShowRPCErrorException, "FILE_REFERENCE_EXPIRED"), NekoConfig.showRPCError, true, divider);
                    } else if (position == sendBugReportRow) {
                        textCell.setEnabled(!AnalyticsHelper.analyticsDisabled, null);
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.SendBugReport), LocaleController.getString(R.string.SendBugReportDesc), !AnalyticsHelper.analyticsDisabled && AnalyticsHelper.sendBugReport, true, divider);
                    } else if (position == contentRestrictionRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.IgnoreContentRestriction), NekoConfig.ignoreContentRestriction, divider);
                    } else if (position == autoInlineBotRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.AutoInlineBot), LocaleController.getString(R.string.AutoInlineBotDesc), NekoConfig.autoInlineBot, true, divider);
                    } else if (position == forceFontWeightFallbackRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.ForceFontWeightFallback), NekoConfig.forceFontWeightFallback, divider);
                    } else if (position == keepFormattingRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.TranslationKeepFormatting), NekoConfig.keepFormatting, divider);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == experimentRow) {
                        headerCell.setText(LocaleController.getString(R.string.Experiment));
                    } else if (position == dataRow) {
                        headerCell.setText(LocaleController.getString(R.string.SendAnonymousData));
                    }
                    break;
                }
                case TYPE_DETAIL_SETTINGS: {
                    TextDetailSettingsCell cell = (TextDetailSettingsCell) holder.itemView;
                    cell.setEnabled(true);
                    cell.setMultilineDetail(true);
                    if (position == deleteDataRow) {
                        cell.setEnabled(!AnalyticsHelper.analyticsDisabled);
                        cell.setTextAndValue(LocaleController.getString(R.string.AnonymousDataDelete), LocaleController.getString(R.string.AnonymousDataDeleteDesc), divider);
                    } else if (position == copyReportIdRow) {
                        cell.setEnabled(!AnalyticsHelper.analyticsDisabled && AnalyticsHelper.sendBugReport);
                        cell.setTextAndValue(LocaleController.getString(R.string.CopyReportId), LocaleController.getString(R.string.CopyReportIdDescription), divider);
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == data2Row) {
                        cell.setText(LocaleController.formatString(R.string.SendAnonymousDataDesc, "Sentry", "Functional Software"));
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
            if (position == copyReportIdRow) {
                return !AnalyticsHelper.analyticsDisabled && AnalyticsHelper.sendBugReport;
            }
            return super.isEnabled(holder);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == experiment2Row || position == deleteAccount2Row || (position == data2Row && !AnalyticsHelper.isSettingsAvailable())) {
                return TYPE_SHADOW;
            } else if (position == deleteAccountRow || position == downloadSpeedBoostRow) {
                return TYPE_SETTINGS;
            } else if (position > experimentRow && position <= showRPCErrorRow || position == sendBugReportRow) {
                return TYPE_CHECK;
            } else if (position == experimentRow || position == dataRow) {
                return TYPE_HEADER;
            } else if (position == deleteDataRow || position == copyReportIdRow) {
                return TYPE_DETAIL_SETTINGS;
            } else if (position == data2Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }
    }
}
