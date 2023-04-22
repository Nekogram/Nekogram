package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CreationTextCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class WsSettingsActivity extends BaseNekoSettingsActivity {

    private int settingsRow;
    private int providerRow;
    private int enableTLSRow;
    private int settings2Row;

    private int buyMeACoffeeRow;
    private int buyMeACoffee2Row;

    private int notWorkingRow;
    private int ownRow;
    private int appRow;
    private int endRow;

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == enableTLSRow) {
            NekoConfig.toggleWsEnableTLS();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.wsEnableTLS);
            }
            NekoConfig.wsReloadConfig();
        } else if (position == providerRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(LocaleController.getString("Nekogram", R.string.Nekogram));
            arrayList.add(LocaleController.getString("AutoDownloadCustom", R.string.AutoDownloadCustom));
            PopupHelper.show(arrayList, LocaleController.getString("WsProvider", R.string.WsProvider), TextUtils.isEmpty(NekoConfig.wsDomain) ? 0 : 1, getParentActivity(), view, i -> {
                if (i == 0) {
                    NekoConfig.setWsDomain("");
                    NekoConfig.wsReloadConfig();
                    listAdapter.notifyItemChanged(providerRow, PARTIAL);
                    showRestartBulletin();
                } else {
                    Context context = getParentActivity();
                    AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
                    builder.setTitle(LocaleController.getString("WsProvider", R.string.WsProvider));

                    LinearLayout ll = new LinearLayout(context);
                    ll.setOrientation(LinearLayout.VERTICAL);

                    final EditTextBoldCursor editText = new EditTextBoldCursor(context) {
                        @Override
                        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
                        }
                    };
                    editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    editText.setText("");
                    editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
                    editText.setHintText(LocaleController.getString("WsProvider", R.string.WsProvider));
                    editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
                    editText.setSingleLine(true);
                    editText.setFocusable(true);
                    editText.setTransformHintToHeader(true);
                    editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
                    editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    editText.setBackground(null);
                    editText.requestFocus();
                    editText.setPadding(0, 0, 0, 0);
                    ll.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24, 0, 24, 0));

                    builder.setView(ll);
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i2) -> {
                        NekoConfig.setWsDomain(editText.getText().toString());
                        listAdapter.notifyItemChanged(providerRow, PARTIAL);
                        showRestartBulletin();
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

                    AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(dialog -> {
                        editText.requestFocus();
                        AndroidUtilities.showKeyboard(editText);
                    });
                    showDialog(alertDialog);
                    editText.setSelection(0, editText.getText().length());
                }
            });
        } else if (position == notWorkingRow || position == ownRow || position == appRow) {
            Browser.openUrl(getParentActivity(), "https://t.me/WSProxy/8");
        } else if (position == buyMeACoffeeRow) {
            Browser.openUrl(getParentActivity(), "https://www.buymeacoffee.com/nekogram");
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
    public Integer getSelectorColor(int position) {
        if (position == ownRow || position == appRow) {
            return Theme.multAlpha(getThemedColor(Theme.key_switchTrackChecked), .1f);
        } else if (position == notWorkingRow) {
            return Theme.multAlpha(getThemedColor(Theme.key_text_RedRegular), .1f);
        }
        return super.getSelectorColor(position);
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

        settingsRow = rowCount++;
        providerRow = rowCount++;
        enableTLSRow = rowCount++;
        settings2Row = rowCount++;

        if (ConfigHelper.getCoffee()) {
            buyMeACoffeeRow = rowCount++;
            buyMeACoffee2Row = rowCount++;
        } else {
            buyMeACoffeeRow = -1;
            buyMeACoffee2Row = -1;
        }

        notWorkingRow = rowCount++;
        ownRow = rowCount++;
        appRow = rowCount++;
        endRow = rowCount++;
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
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == providerRow) {
                        String value;
                        if (TextUtils.isEmpty(NekoConfig.wsDomain)) {
                            value = LocaleController.getString("Nekogram", R.string.Nekogram);
                        } else {
                            value = NekoConfig.wsDomain;
                        }
                        textCell.setTextAndValue(LocaleController.getString("WsProvider", R.string.WsProvider), value, partial, true);
                    }
                    break;
                }
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
                case TYPE_CREATION: {
                    CreationTextCell creationTextCell = (CreationTextCell) holder.itemView;
                    if (position == notWorkingRow) {
                        Drawable drawable = creationTextCell.getContext().getResources().getDrawable(R.drawable.msg_report);
                        drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_text_RedRegular), PorterDuff.Mode.MULTIPLY));
                        creationTextCell.setTextColor(Theme.key_text_RedRegular);
                        creationTextCell.setTextAndIcon(LocaleController.getString("WsNotWorking", R.string.WsNotWorking), drawable, true);
                    } else if (position == ownRow) {
                        Drawable drawable = creationTextCell.getContext().getResources().getDrawable(R.drawable.msg_psa);
                        drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_switchTrackChecked), PorterDuff.Mode.MULTIPLY));
                        creationTextCell.setTextAndIcon(LocaleController.getString("WsUseMyOwnDomain", R.string.WsUseMyOwnDomain), drawable, true);
                    } else if (position == appRow) {
                        Drawable drawable = creationTextCell.getContext().getResources().getDrawable(R.drawable.msg_psa);
                        drawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_switchTrackChecked), PorterDuff.Mode.MULTIPLY));
                        creationTextCell.setTextAndIcon(LocaleController.getString("WsStandaloneApp", R.string.WsStandaloneApp), drawable, false);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == settings2Row || position == buyMeACoffee2Row || position == endRow) {
                return TYPE_SHADOW;
            } else if (position == settingsRow) {
                return TYPE_HEADER;
            } else if (position == enableTLSRow) {
                return TYPE_CHECK;
            } else if (position == providerRow) {
                return TYPE_SETTINGS;
            } else if (position == notWorkingRow || position == ownRow || position == appRow) {
                return TYPE_CREATION;
            } else if (position == buyMeACoffeeRow) {
                return TYPE_BUYMEACOFFEE;
            }
            return TYPE_SETTINGS;
        }
    }
}
