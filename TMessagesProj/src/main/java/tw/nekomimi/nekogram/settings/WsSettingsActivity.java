package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;

public class WsSettingsActivity extends BaseNekoSettingsActivity {

    private int settingsRow;
    private int enableTLSRow;
    private int localProxyRow;
    private int enableDoHRow;
    private int settings2Row;

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableTLSRow) {
            NekoConfig.toggleWsEnableTLS();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.wsEnableTLS);
            }
            NekoConfig.wsReloadConfig();
        } else if (position == localProxyRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(LocaleController.getString("UseProxySocks5", R.string.UseProxySocks5));
            arrayList.add(LocaleController.getString("UseProxyTelegram", R.string.UseProxyTelegram));
            PopupHelper.show(arrayList, LocaleController.getString("WsLocalProxy", R.string.WsLocalProxy), NekoConfig.wsUseMTP ? 1 : 0, getParentActivity(), view, i -> {
                NekoConfig.setWsUseMTP(i == 1);
                listAdapter.notifyItemChanged(localProxyRow, PARTIAL);
                NekoConfig.wsReloadConfig();
            });
        } else if (position == enableDoHRow) {
            NekoConfig.toggleWsEnableDoH();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.wsUseDoH);
            }
            NekoConfig.wsReloadConfig();
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return NekoConfig.WS_ADDRESS;
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        settingsRow = rowCount++;
        enableTLSRow = rowCount++;
        localProxyRow = rowCount++;
        enableDoHRow = rowCount++;
        settings2Row = rowCount++;
    }

    @Override
    protected boolean hasWhiteActionBar() {
        return false;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_SHADOW: {
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == localProxyRow) {
                        String value = NekoConfig.wsUseMTP ? LocaleController.getString("UseProxyTelegram", R.string.UseProxyTelegram) : LocaleController.getString("UseProxySocks5", R.string.UseProxySocks5);
                        textCell.setTextAndValue(LocaleController.getString("WsLocalProxy", R.string.WsLocalProxy), value, partial, true);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == enableTLSRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableTls", R.string.WsEnableTls), NekoConfig.wsEnableTLS, true);
                    } else if (position == enableDoHRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableDoh", R.string.WsEnableDoh), NekoConfig.wsUseDoH, false);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == settingsRow) {
                        headerCell.setText(LocaleController.getString("Settings", R.string.Settings));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == settings2Row) {
                return TYPE_SHADOW;
            } else if (position == settingsRow) {
                return TYPE_HEADER;
            } else if (position == enableTLSRow || position == enableDoHRow) {
                return TYPE_CHECK;
            }
            return TYPE_SETTINGS;
        }
    }
}
