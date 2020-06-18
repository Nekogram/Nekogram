package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UndoView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.translator.Translator;

@SuppressLint("RtlHardcoded")
public class NekoGeneralSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private int rowCount;

    private int connectionRow;
    private int ipv6Row;
    private int connection2Row;

    private int appearanceRow;
    private int typefaceRow;
    private int useSystemEmojiRow;
    private int transparentStatusBarRow;
    private int forceTabletRow;
    private int avatarAsDrawerBackgroundRow;
    private int appBarShadowRow;
    private int newYearRow;
    private int eventTypeRow;
    private int actionBarDecorationRow;
    private int appearance2Row;

    private int generalRow;
    private int hidePhoneRow;
    private int inappCameraRow;
    private int hideProxySponsorChannelRow;
    private int askBeforeCallRow;
    private int disableNumberRoundingRow;
    private int openArchiveOnPullRow;
    private int autoPauseVideoRow;
    private int translationProviderRow;
    private int nameOrderRow;
    private int idTypeRow;
    private int general2Row;

    private UndoView restartTooltip;

    static public AlertDialog getTranslationProviderAlert(Context context) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        arrayList.add(LocaleController.getString("ProviderGoogleTranslate", R.string.ProviderGoogleTranslate));
        types.add(Translator.PROVIDER_GOOGLE);
        arrayList.add(LocaleController.getString("ProviderGoogleTranslateCN", R.string.ProviderGoogleTranslateCN));
        types.add(Translator.PROVIDER_GOOGLE_CN);
        arrayList.add(LocaleController.getString("ProviderLingocloud", R.string.ProviderLingocloud));
        types.add(Translator.PROVIDER_LINGO);
        arrayList.add(LocaleController.getString("ProviderYandex", R.string.ProviderYandex));
        types.add(Translator.PROVIDER_YANDEX);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("TranslationProvider", R.string.TranslationProvider));
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(linearLayout);

        for (int a = 0; a < arrayList.size(); a++) {
            RadioColorCell cell = new RadioColorCell(context);
            cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            cell.setTag(a);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(arrayList.get(a), NekoConfig.translationProvider == types.get(a));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                Integer which = (Integer) v.getTag();
                NekoConfig.setTranslationProvider(types.get(which));
                builder.getDismissRunnable().run();
            });
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        return builder.create();
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("General", R.string.General));

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
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setAdapter(listAdapter);
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
                restartTooltip.showWithAction(0, UndoView.ACTION_CACHE_WAS_CLEARED, null, null);
            } else if (position == transparentStatusBarRow) {
                NekoConfig.toggleTransparentStatusBar();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.transparentStatusBar);
                }
                AndroidUtilities.runOnUIThread(() -> NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.didSetNewTheme, false));
            } else if (position == hideProxySponsorChannelRow) {
                NekoConfig.toggleHideProxySponsorChannel();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideProxySponsorChannel);
                }
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        MessagesController.getInstance(a).checkPromoInfo(true);
                    }
                }
            } else if (position == useSystemEmojiRow) {
                NekoConfig.toggleUseSystemEmoji();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.useSystemEmoji);
                }
            } else if (position == typefaceRow) {
                NekoConfig.toggleTypeface();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.typeface == 1);
                }
                parentLayout.rebuildAllFragmentViews(true, true);
            } else if (position == nameOrderRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("FirstLast", R.string.FirstLast));
                types.add(1);
                arrayList.add(LocaleController.getString("LastFirst", R.string.LastFirst));
                types.add(2);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(LocaleController.getString("NameOrder", R.string.NameOrder));
                final LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(linearLayout);

                for (int a = 0; a < arrayList.size(); a++) {
                    RadioColorCell cell = new RadioColorCell(context);
                    cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                    cell.setTag(a);
                    cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
                    cell.setTextAndValue(arrayList.get(a), NekoConfig.nameOrder == types.get(a));
                    linearLayout.addView(cell);
                    cell.setOnClickListener(v -> {
                        Integer which = (Integer) v.getTag();
                        NekoConfig.setNameOrder(types.get(which));
                        listAdapter.notifyItemChanged(nameOrderRow);
                        builder.getDismissRunnable().run();
                        parentLayout.rebuildAllFragmentViews(false, false);
                    });
                }
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            } else if (position == eventTypeRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("DependsOnDate", R.string.DependsOnDate));
                types.add(0);
                arrayList.add(LocaleController.getString("Christmas", R.string.Christmas));
                types.add(1);
                arrayList.add(LocaleController.getString("Valentine", R.string.Valentine));
                types.add(2);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(LocaleController.getString("EventType", R.string.EventType));
                final LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(linearLayout);

                for (int a = 0; a < arrayList.size(); a++) {
                    RadioColorCell cell = new RadioColorCell(context);
                    cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                    cell.setTag(a);
                    cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
                    cell.setTextAndValue(arrayList.get(a), NekoConfig.eventType == types.get(a));
                    linearLayout.addView(cell);
                    cell.setOnClickListener(v -> {
                        Integer which = (Integer) v.getTag();
                        NekoConfig.setEventType(types.get(which));
                        listAdapter.notifyItemChanged(eventTypeRow);
                        builder.getDismissRunnable().run();
                        restartTooltip.showWithAction(0, UndoView.ACTION_CACHE_WAS_CLEARED, null, null);
                    });
                }
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            } else if (position == actionBarDecorationRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("DependsOnDate", R.string.DependsOnDate));
                types.add(0);
                arrayList.add(LocaleController.getString("Snowflakes", R.string.Snowflakes));
                types.add(1);
                arrayList.add(LocaleController.getString("Fireworks", R.string.Fireworks));
                types.add(2);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(LocaleController.getString("ActionBarDecoration", R.string.ActionBarDecoration));
                final LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(linearLayout);

                for (int a = 0; a < arrayList.size(); a++) {
                    RadioColorCell cell = new RadioColorCell(context);
                    cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                    cell.setTag(a);
                    cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
                    cell.setTextAndValue(arrayList.get(a), NekoConfig.actionBarDecoration == types.get(a));
                    linearLayout.addView(cell);
                    cell.setOnClickListener(v -> {
                        Integer which = (Integer) v.getTag();
                        NekoConfig.setActionBarDecoration(types.get(which));
                        listAdapter.notifyItemChanged(actionBarDecorationRow);
                        builder.getDismissRunnable().run();
                        restartTooltip.showWithAction(0, UndoView.ACTION_CACHE_WAS_CLEARED, null, null);
                    });
                }
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            } else if (position == newYearRow) {
                NekoConfig.toggleNewYear();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.newYear);
                }
                restartTooltip.showWithAction(0, UndoView.ACTION_CACHE_WAS_CLEARED, null, null);
            } else if (position == translationProviderRow) {
                showDialog(getTranslationProviderAlert(context)).setOnDismissListener(dialog1 -> listAdapter.notifyItemChanged(translationProviderRow));
            } else if (position == openArchiveOnPullRow) {
                NekoConfig.toggleOpenArchiveOnPull();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.openArchiveOnPull);
                }
            } else if (position == avatarAsDrawerBackgroundRow) {
                NekoConfig.toggleAvatarAsDrawerBackground();
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.avatarAsDrawerBackground);
                }
            } else if (position == askBeforeCallRow) {
                NekoConfig.toggleAskBeforeCall();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.askBeforeCall);
                }
            } else if (position == disableNumberRoundingRow) {
                NekoConfig.toggleDisableNumberRounding();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableNumberRounding);
                }
            } else if (position == appBarShadowRow) {
                NekoConfig.toggleDisableAppBarShadow();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableAppBarShadow);
                }
                ActionBarLayout.headerShadowDrawable = NekoConfig.disableAppBarShadow ? null : parentLayout.getResources().getDrawable(R.drawable.header_shadow).mutate();
                parentLayout.rebuildAllFragmentViews(true, true);
            } else if (position == idTypeRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("IdTypeHidden", R.string.IdTypeHidden));
                types.add(NekoConfig.ID_TYPE_HIDDEN);
                arrayList.add(LocaleController.getString("IdTypeAPI", R.string.IdTypeAPI));
                types.add(NekoConfig.ID_TYPE_API);
                arrayList.add(LocaleController.getString("IdTypeBOTAPI", R.string.IdTypeBOTAPI));
                types.add(NekoConfig.ID_TYPE_BOTAPI);

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(LocaleController.getString("IdType", R.string.IdType));
                builder.setMessage(LocaleController.getString("IdTypeAbout", R.string.IdTypeAbout));
                final LinearLayout linearLayout = new LinearLayout(context);
                linearLayout.setOrientation(LinearLayout.VERTICAL);
                builder.setView(linearLayout);

                for (int a = 0; a < arrayList.size(); a++) {
                    RadioColorCell cell = new RadioColorCell(context);
                    cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                    cell.setTag(a);
                    cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
                    cell.setTextAndValue(arrayList.get(a), NekoConfig.idType == types.get(a));
                    linearLayout.addView(cell);
                    cell.setOnClickListener(v -> {
                        Integer which = (Integer) v.getTag();
                        NekoConfig.setIdType(types.get(which));
                        listAdapter.notifyItemChanged(idTypeRow);
                        builder.getDismissRunnable().run();
                    });
                }
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                showDialog(builder.create());
            } else if (position == autoPauseVideoRow) {
                NekoConfig.toggleAutoPauseVideo();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.autoPauseVideo);
                }
            }
        });

        restartTooltip = new UndoView(context);
        restartTooltip.setInfoText(LocaleController.formatString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect));
        frameLayout.addView(restartTooltip, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM | Gravity.LEFT, 8, 0, 8, 8));

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;

        connectionRow = rowCount++;
        ipv6Row = rowCount++;
        connection2Row = rowCount++;


        appearanceRow = rowCount++;
        typefaceRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? rowCount++ : -1;
        useSystemEmojiRow = rowCount++;
        transparentStatusBarRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? rowCount++ : -1;
        forceTabletRow = rowCount++;
        avatarAsDrawerBackgroundRow = rowCount++;
        appBarShadowRow = rowCount++;
        newYearRow = NekoConfig.showHiddenFeature ? rowCount++ : -1;
        eventTypeRow = NekoConfig.showHiddenFeature ? rowCount++ : -1;
        actionBarDecorationRow = NekoConfig.showHiddenFeature ? rowCount++ : -1;
        appearance2Row = rowCount++;

        generalRow = rowCount++;
        hidePhoneRow = rowCount++;
        inappCameraRow = rowCount++;
        hideProxySponsorChannelRow = rowCount++;
        askBeforeCallRow = rowCount++;
        disableNumberRoundingRow = rowCount++;
        openArchiveOnPullRow = rowCount++;
        autoPauseVideoRow = rowCount++;
        translationProviderRow = rowCount++;
        nameOrderRow = rowCount++;
        idTypeRow = rowCount++;
        general2Row = rowCount++;

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
                    if (position == general2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
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
                        textCell.setTextAndValue(LocaleController.getString("NameOrder", R.string.NameOrder), value, true);
                    } else if (position == eventTypeRow) {
                        String value;
                        switch (NekoConfig.eventType) {
                            case 1:
                                value = LocaleController.getString("Christmas", R.string.Christmas);
                                break;
                            case 2:
                                value = LocaleController.getString("Valentine", R.string.Valentine);
                                break;
                            case 0:
                            default:
                                value = LocaleController.getString("DependsOnDate", R.string.DependsOnDate);
                        }
                        textCell.setTextAndValue(LocaleController.getString("EventType", R.string.EventType), value, true);
                    } else if (position == actionBarDecorationRow) {
                        String value;
                        switch (NekoConfig.actionBarDecoration) {
                            case 1:
                                value = LocaleController.getString("Snowflakes", R.string.Snowflakes);
                                break;
                            case 2:
                                value = LocaleController.getString("Fireworks", R.string.Fireworks);
                                break;
                            case 0:
                            default:
                                value = LocaleController.getString("DependsOnDate", R.string.DependsOnDate);
                        }
                        textCell.setTextAndValue(LocaleController.getString("ActionBarDecoration", R.string.ActionBarDecoration), value, false);
                    } else if (position == translationProviderRow) {
                        String value;
                        switch (NekoConfig.translationProvider) {
                            case Translator.PROVIDER_GOOGLE:
                                value = LocaleController.getString("ProviderGoogleTranslate", R.string.ProviderGoogleTranslate);
                                break;
                            case Translator.PROVIDER_GOOGLE_CN:
                                value = LocaleController.getString("ProviderGoogleTranslateCN", R.string.ProviderGoogleTranslateCN);
                                break;
                            case Translator.PROVIDER_YANDEX:
                                value = LocaleController.getString("ProviderYandex", R.string.ProviderYandex);
                                break;
                            case Translator.PROVIDER_LINGO:
                            default:
                                value = LocaleController.getString("ProviderLingocloud", R.string.ProviderLingocloud);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), value, true);
                    } else if (position == idTypeRow) {
                        String value;
                        switch (NekoConfig.idType) {
                            case NekoConfig.ID_TYPE_HIDDEN:
                                value = LocaleController.getString("IdTypeHidden", R.string.IdTypeHidden);
                                break;
                            case NekoConfig.ID_TYPE_BOTAPI:
                                value = LocaleController.getString("IdTypeBOTAPI", R.string.IdTypeBOTAPI);
                                break;
                            case NekoConfig.ID_TYPE_API:
                            default:
                                value = LocaleController.getString("IdTypeAPI", R.string.IdTypeAPI);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("IdType", R.string.IdType), value, false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ipv6Row) {
                        textCell.setTextAndCheck(LocaleController.getString("IPv6", R.string.IPv6), NekoConfig.useIPv6, false);
                    } else if (position == hidePhoneRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HidePhone", R.string.HidePhone), NekoConfig.hidePhone, true);
                    } else if (position == inappCameraRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DebugMenuEnableCamera", R.string.DebugMenuEnableCamera), SharedConfig.inappCamera, true);
                    } else if (position == transparentStatusBarRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TransparentStatusBar", R.string.TransparentStatusBar), NekoConfig.transparentStatusBar, true);
                    } else if (position == hideProxySponsorChannelRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HideProxySponsorChannel", R.string.HideProxySponsorChannel), NekoConfig.hideProxySponsorChannel, true);
                    } else if (position == useSystemEmojiRow) {
                        textCell.setTextAndCheck(LocaleController.getString("EmojiUseDefault", R.string.EmojiUseDefault), NekoConfig.useSystemEmoji, true);
                    } else if (position == typefaceRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TypefaceUseDefault", R.string.TypefaceUseDefault), NekoConfig.typeface == 1, true);
                    } else if (position == forceTabletRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ForceTabletMode", R.string.ForceTabletMode), NekoConfig.forceTablet, true);
                    } else if (position == newYearRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ChristmasHat", R.string.ChristmasHat), NekoConfig.newYear, true);
                    } else if (position == openArchiveOnPullRow) {
                        textCell.setTextAndCheck(LocaleController.getString("OpenArchiveOnPull", R.string.OpenArchiveOnPull), NekoConfig.openArchiveOnPull, true);
                    } else if (position == avatarAsDrawerBackgroundRow) {
                        textCell.setTextAndCheck(LocaleController.getString("UseAvatarAsDrawerBackground", R.string.UseAvatarAsDrawerBackground), NekoConfig.avatarAsDrawerBackground, true);
                    } else if (position == askBeforeCallRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AskBeforeCalling", R.string.AskBeforeCalling), NekoConfig.askBeforeCall, true);
                    } else if (position == disableNumberRoundingRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("DisableNumberRounding", R.string.DisableNumberRounding), "4.8K -> 4777", NekoConfig.disableNumberRounding, true, true);
                    } else if (position == appBarShadowRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableAppBarShadow", R.string.DisableAppBarShadow), NekoConfig.disableAppBarShadow, eventTypeRow != -1);
                    } else if (position == autoPauseVideoRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("AutoPauseVideo", R.string.AutoPauseVideo), LocaleController.getString("AutoPauseVideoAbout", R.string.AutoPauseVideoAbout), NekoConfig.autoPauseVideo, true, true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalRow) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    } else if (position == connectionRow) {
                        headerCell.setText(LocaleController.getString("Connection", R.string.Connection));
                    } else if (position == appearanceRow) {
                        headerCell.setText(LocaleController.getString("Appearance", R.string.Appearance));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3;
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
            if (position == connection2Row || position == appearance2Row || position == general2Row) {
                return 1;
            } else if (position == eventTypeRow || position == actionBarDecorationRow ||
                    (position >= translationProviderRow && position <= idTypeRow)) {
                return 2;
            } else if (position == ipv6Row || position == newYearRow ||
                    (position > appearanceRow && position <= appBarShadowRow) ||
                    (position > generalRow && position <= autoPauseVideoRow)) {
                return 3;
            } else if (position == generalRow || position == connectionRow || position == appearanceRow) {
                return 4;
            }
            return 2;
        }
    }
}
