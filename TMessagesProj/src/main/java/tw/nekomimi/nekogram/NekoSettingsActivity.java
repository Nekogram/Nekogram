package tw.nekomimi.nekogram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

public class NekoSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;

    private int connectionRow;
    private int ipv6Row;
    private int connection2Row;

    private int emojiRow;
    private int useSystemEmojiRow;
    private int singleBigEmojiRow;
    private int emoji2Row;

    private int chatRow;
    private int ignoreBlockedRow;
    private int nyaRow;
    private int nyaSuffixRow;
    private int chat2Row;

    private int settingsRow;
    private int hidePhoneRow;
    private int inappCameraRow;
    private int nameOrderRow;
    private int transparentStatusBarRow;
    private int navigationBarTintRow;
    private int navigationBarColorRow;
    private int forceTabletRow;
    private int settings2Row;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows(false);

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("NekoSettings", R.string.NekoSettings));

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
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setGlowColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        listView.setAdapter(listAdapter);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == ipv6Row) {
                NekoConfig.toggleIPv6();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.useIPv6);
                }
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager.native_setUseIpv6(a, NekoConfig.useIPv6);
                    }
                }
            } else if (position == hidePhoneRow) {
                NekoConfig.toggleHidePhone();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hidePhone);
                }
            } else if (position == inappCameraRow) {
                SharedConfig.toggleInappCamera();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.inappCamera);
                }
            } else if (position == forceTabletRow) {
                NekoConfig.toggleForceTablet();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.forceTablet);
                }
            } else if (position == ignoreBlockedRow) {
                NekoConfig.toggleIgnoreBlocked();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.ignoreBlocked);
                }
            } else if (position == nyaRow) {
                NekoConfig.toggleNya();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.nya);
                }
                updateRows(true);
            } else if (position == transparentStatusBarRow) {
                if (!(NekoConfig.navigationBarTint || Build.VERSION.SDK_INT < Build.VERSION_CODES.O))
                    return;
                NekoConfig.toggleTransparentStatusBar();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.transparentStatusBar);
                }
                UIHelper.updateStatusBarColor(getParentActivity());
            } else if (position == navigationBarTintRow) {
                NekoConfig.toggleNavigationBarTint();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.navigationBarTint);
                }
                updateRows(true);
                UIHelper.updateStatusBarColor(getParentActivity());
                UIHelper.updateNavigationBarColor(getParentActivity());
            } else if (position == navigationBarColorRow) {
                if (!NekoConfig.navigationBarTint)
                    return;
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("NavigationBarColor", R.string.NavigationBarColor));
                CharSequence[] items = new CharSequence[]{
                        LocaleController.getString("NavigationBarColorBlack", R.string.NavigationBarColorBlack),
                        LocaleController.getString("NavigationBarColorActionBar", R.string.NavigationBarColorActionBar),
                        LocaleController.getString("NavigationBarColorMessagePanel", R.string.NavigationBarColorMessagePanel),
                };
                builder.setItems(items, (dialog, which) -> {
                    NekoConfig.setNavigationBarColor(which + 1);
                    listAdapter.notifyItemChanged(navigationBarColorRow);
                    UIHelper.updateNavigationBarColor(getParentActivity());
                });
                showDialog(builder.create());
            } else if (position == useSystemEmojiRow) {
                SharedConfig.useSystemEmoji = !SharedConfig.useSystemEmoji;
                SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
                editor.putBoolean("useSystemEmoji", SharedConfig.useSystemEmoji);
                editor.commit();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.useSystemEmoji);
                }
            } else if (position == singleBigEmojiRow) {
                SharedConfig.allowBigEmoji = !SharedConfig.allowBigEmoji;
                SharedPreferences.Editor editor = MessagesController.getGlobalMainSettings().edit();
                editor.putBoolean("allowBigEmoji", SharedConfig.allowBigEmoji);
                editor.commit();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.allowBigEmoji);
                }
            } else if (position == nameOrderRow) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
                builder.setTitle(LocaleController.getString("NameOrder", R.string.NameOrder));
                CharSequence[] items = new CharSequence[]{
                        LocaleController.getString("FirstLast", R.string.FirstLast),
                        LocaleController.getString("LastFirst", R.string.LastFirst),
                };
                builder.setItems(items, (dialog, which) -> {
                    NekoConfig.setNameOrder(which + 1);
                    listAdapter.notifyItemChanged(nameOrderRow);
                });
                showDialog(builder.create());
            } else if (position == nyaSuffixRow) {
                if (!NekoConfig.nya)
                    return;

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(LocaleController.getString("NyaSuffix", R.string.NyaSuffix));

                final EditTextBoldCursor editText = new EditTextBoldCursor(context) {
                    @Override
                    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
                    }
                };
                editText.setText(NekoConfig.nyaSuffix);
                editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                editText.setHintText(LocaleController.getString("NyaSuffixHint", R.string.NyaSuffixHint));
                editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
                editText.setSingleLine(true);
                editText.setFocusable(true);
                editText.setTransformHintToHeader(true);
                editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated), Theme.getColor(Theme.key_windowBackgroundWhiteRedText3));
                editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                editText.setBackgroundDrawable(null);
                editText.requestFocus();
                editText.setPadding(0, 0, 0, 0);
                builder.setView(editText);


                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (getParentActivity() == null) {
                            return;
                        }
                        String suffix = editText.getText().toString();
                        NekoConfig.setNyaSuffix(suffix);
                        listAdapter.notifyItemChanged(nyaSuffixRow);
                    }
                });
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show().setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        editText.requestFocus();
                        AndroidUtilities.showKeyboard(editText);
                    }
                });
                if (editText != null) {
                    ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) editText.getLayoutParams();
                    if (layoutParams != null) {
                        if (layoutParams instanceof FrameLayout.LayoutParams) {
                            ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
                        }
                        layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
                        layoutParams.height = AndroidUtilities.dp(36);
                        editText.setLayoutParams(layoutParams);
                    }
                }
            }
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

    private void updateRows(boolean notify) {
        rowCount = 0;
        connectionRow = rowCount++;
        ipv6Row = rowCount++;
        connection2Row = rowCount++;
        emojiRow = rowCount++;
        useSystemEmojiRow = rowCount++;
        singleBigEmojiRow = rowCount++;
        emoji2Row = rowCount++;
        chatRow = rowCount++;
        ignoreBlockedRow = rowCount++;
        nyaRow = rowCount++;
        nyaSuffixRow = rowCount++;
        chat2Row = rowCount++;
        settingsRow = rowCount++;
        hidePhoneRow = rowCount++;
        inappCameraRow = rowCount++;
        navigationBarTintRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? rowCount++ : -1;
        transparentStatusBarRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? rowCount++ : -1;
        navigationBarColorRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? rowCount++ : -1;
        forceTabletRow = rowCount++;
        nameOrderRow = rowCount++;
        settings2Row = rowCount++;
        if (notify && listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        return new ThemeDescription[]{
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite),
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow),

                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText),

                new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack),
                new ThemeDescription(listView, 0, new Class[]{NotificationsCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked),

                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrack),
                new ThemeDescription(listView, 0, new Class[]{TextCheckCell.class}, new String[]{"checkBox"}, null, null, null, Theme.key_switchTrackChecked),

                new ThemeDescription(listView, 0, new Class[]{HeaderCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlueHeader),

                new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
        };
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

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
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == nameOrderRow) {
                        String value;
                        switch (NekoConfig.nameOrder) {
                            case 2:
                                value = LocaleController.getString("LastFirst", R.string.LastFirst);
                                break;
                            case 1:
                            default:
                                value = LocaleController.getString("FirstLast", R.string.FirstLast);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("NameOrder", R.string.NameOrder), value, false);
                    } else if (position == navigationBarColorRow) {
                        String value;
                        switch (NekoConfig.navigationBarColor) {
                            case 3:
                                value = LocaleController.getString("NavigationBarColorMessagePanel", R.string.NavigationBarColorMessagePanel);
                                break;
                            case 2:
                                value = LocaleController.getString("NavigationBarColorActionBar", R.string.NavigationBarColorActionBar);
                                break;
                            case 1:
                            default:
                                value = LocaleController.getString("NavigationBarColorBlack", R.string.NavigationBarColorBlack);
                        }
                        textCell.setTextAndValue(LocaleController.getString("NavigationBarColor", R.string.NavigationBarColor), value, true);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == ipv6Row) {
                        textCell.setTextAndCheck(LocaleController.getString("IPv6", R.string.IPv6), NekoConfig.useIPv6, false);
                    } else if (position == hidePhoneRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HidePhone", R.string.HidePhone), NekoConfig.hidePhone, true);
                    } else if (position == inappCameraRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DebugMenuEnableCamera", R.string.DebugMenuEnableCamera), SharedConfig.inappCamera, true);
                    } else if (position == transparentStatusBarRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TransparentStatusBar", R.string.TransparentStatusBar), NekoConfig.transparentStatusBar, true);
                    } else if (position == navigationBarTintRow) {
                        textCell.setTextAndCheck(LocaleController.getString("NavigationBarTint", R.string.NavigationBarTint), NekoConfig.navigationBarTint, true);
                    } else if (position == useSystemEmojiRow) {
                        textCell.setTextAndCheck(LocaleController.getString("EmojiUseDefault", R.string.EmojiUseDefault), SharedConfig.useSystemEmoji, true);
                    } else if (position == singleBigEmojiRow) {
                        textCell.setTextAndCheck(LocaleController.getString("EmojiBigSize", R.string.EmojiBigSize), SharedConfig.allowBigEmoji, false);
                    } else if (position == ignoreBlockedRow) {
                        textCell.setTextAndCheck(LocaleController.getString("IgnoreBlocked", R.string.IgnoreBlocked), NekoConfig.ignoreBlocked, true);
                    } else if (position == forceTabletRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ForceTabletMode", R.string.ForceTabletMode), NekoConfig.forceTablet, true);
                    } else if (position == nyaRow) {
                        textCell.setTextAndCheck(LocaleController.getString("EnableNya", R.string.EnableNya), NekoConfig.nya, true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == settingsRow) {
                        headerCell.setText(LocaleController.getString("Settings", R.string.Settings));
                    } else if (position == emojiRow) {
                        headerCell.setText(LocaleController.getString("Emoji", R.string.Emoji));
                    } else if (position == connectionRow) {
                        headerCell.setText(LocaleController.getString("Connection", R.string.Connection));
                    } else if (position == chatRow) {
                        headerCell.setText(LocaleController.getString("Chat", R.string.Chat));
                    }
                    break;
                }
                case 6: {
                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
                    if (position == nyaSuffixRow) {
                        textCell.setTextAndValue(LocaleController.getString("NyaSuffix", R.string.NyaSuffix), NekoConfig.nyaSuffix, false);
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position == hidePhoneRow || position == inappCameraRow || position == ignoreBlockedRow || position == navigationBarTintRow ||
                    position == useSystemEmojiRow || position == singleBigEmojiRow || position == ipv6Row ||
                    position == nameOrderRow || position == forceTabletRow || position == nyaRow ||
                    (position == nyaSuffixRow && NekoConfig.nya) ||
                    (position == transparentStatusBarRow && (NekoConfig.navigationBarTint || Build.VERSION.SDK_INT < Build.VERSION_CODES.O)) ||
                    (position == navigationBarColorRow && NekoConfig.navigationBarTint);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
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
            }
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == settings2Row || position == emoji2Row || position == connection2Row || position == chat2Row) {
                return 1;
            } else if (position == nameOrderRow || position == navigationBarColorRow) {
                return 2;
            } else if (position == ipv6Row || position == hidePhoneRow || position == inappCameraRow ||
                    position == transparentStatusBarRow || position == navigationBarTintRow ||
                    position == ignoreBlockedRow || position == useSystemEmojiRow || position == singleBigEmojiRow ||
                    position == forceTabletRow || position == nyaRow) {
                return 3;
            } else if (position == settingsRow || position == connectionRow || position == emojiRow || position == chatRow) {
                return 4;
            }
            return 6;
        }
    }
}
