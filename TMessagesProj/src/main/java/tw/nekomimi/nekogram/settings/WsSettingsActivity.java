package tw.nekomimi.nekogram.settings;

import android.app.assist.AssistContent;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.URLSpan;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.URLSpanNoUnderline;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;

public class WsSettingsActivity extends BaseNekoSettingsActivity {

    private int descriptionRow;
    private int settingsRow;
    private int enableTLSRow;
    private int localProxyRow;
    private int enableDoHRow;

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);
        actionBar.setTitle(NekoConfig.WS_ADDRESS);

        listAdapter = new ListAdapter(context);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
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
                PopupHelper.show(arrayList, LocaleController.getString("WsLocalProxy", R.string.WsLocalProxy), NekoConfig.wsUseMTP ? 1 : 0, context, view, i -> {
                    NekoConfig.setWsUseMTP(i == 1);
                    listAdapter.notifyItemChanged(localProxyRow);
                    NekoConfig.wsReloadConfig();
                });
            } else if (position == enableDoHRow) {
                NekoConfig.toggleWsEnableDoH();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.wsUseDoH);
                }
                NekoConfig.wsReloadConfig();
            }
        });
        return fragmentView;
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        settingsRow = rowCount++;
        enableTLSRow = rowCount++;
        localProxyRow = rowCount++;
        enableDoHRow = rowCount++;
        descriptionRow = rowCount++;
    }

    @Override
    protected boolean whiteStatusBar() {
        return false;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == localProxyRow) {
                        String value = NekoConfig.wsUseMTP ? LocaleController.getString("UseProxyTelegram", R.string.UseProxyTelegram) : LocaleController.getString("UseProxySocks5", R.string.UseProxySocks5);
                        textCell.setTextAndValue(LocaleController.getString("WsLocalProxy", R.string.WsLocalProxy), value, true);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == enableTLSRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableTls", R.string.WsEnableTls), NekoConfig.wsEnableTLS, true);
                    } else if (position == enableDoHRow) {
                        textCell.setTextAndCheck(LocaleController.getString("WsEnableDoh", R.string.WsEnableDoh), NekoConfig.wsUseDoH, false);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == settingsRow) {
                        headerCell.setText(LocaleController.getString("Settings", R.string.Settings));
                    }
                    break;
                }
                case 7: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    Spannable spanned = new SpannableString(Html.fromHtml(LocaleController.formatString("WsDescription2", R.string.WsDescription2, "https://nekogram.app/proxy").replace("\n", "<br>")));
                    URLSpan[] spans = spanned.getSpans(0, spanned.length(), URLSpan.class);
                    for (URLSpan span : spans) {
                        int start = spanned.getSpanStart(span);
                        int end = spanned.getSpanEnd(span);
                        spanned.removeSpan(span);
                        span = new URLSpanNoUnderline(span.getURL());
                        spanned.setSpan(span, start, end, 0);
                    }
                    cell.setText(spanned);
                    cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == descriptionRow) {
                return 7;
            } else if (position == settingsRow) {
                return 4;
            } else if (position == enableTLSRow || position == enableDoHRow) {
                return 3;
            }
            return 2;
        }
    }

    @Override
    public void onProvideAssistContent(AssistContent outContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            outContent.setWebUri(Uri.parse("https://nekogram.app/proxy"));
        }
    }
}
