package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;

import tw.nekomimi.nekogram.NekoConfig;

public class WsSettingsActivity extends BaseNekoSettingsActivity {

    private int settingsRow;
    private int enableTLSRow;
    private int settings2Row;

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableTLSRow) {
            NekoConfig.toggleWsEnableTLS();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.wsEnableTLS);
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
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == enableTLSRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableTls", R.string.WsEnableTls), NekoConfig.wsEnableTLS, false);
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
            } else if (position == enableTLSRow) {
                return TYPE_CHECK;
            }
            return TYPE_SETTINGS;
        }
    }
}
