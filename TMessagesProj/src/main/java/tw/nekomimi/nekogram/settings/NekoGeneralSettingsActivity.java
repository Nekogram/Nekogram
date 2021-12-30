package tw.nekomimi.nekogram.settings;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.DefaultItemAnimator;
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
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.translator.DeepLTranslator;
import tw.nekomimi.nekogram.translator.Translator;

@SuppressLint({"RtlHardcoded", "NotifyDataSetChanged"})
public class NekoGeneralSettingsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;
    private ValueAnimator statusBarColorAnimator;
    private DrawerProfilePreviewCell profilePreviewCell;

    private int rowCount;

    private int connectionRow;
    private int ipv6Row;
    private int connection2Row;

    private int drawerRow;
    private int avatarAsDrawerBackgroundRow;
    private int avatarBackgroundBlurRow;
    private int avatarBackgroundDarkenRow;
    private int drawer2Row;

    private int appearanceRow;
    private int useSystemEmojiRow;
    private int transparentStatusBarRow;
    private int forceTabletRow;
    private int mediaPreviewRow;
    private int appBarShadowRow;
    private int newYearRow;
    private int eventTypeRow;
    private int appearance2Row;

    private int notificationRow;
    private int accentAsNotificationColorRow;
    private int silenceNonContactsRow;
    private int notification2Row;

    private int translatorRow;
    private int translatorTypeRow;
    private int translationProviderRow;
    private int translationTargetRow;
    private int deepLFormalityRow;
    private int translator2Row;

    private int generalRow;
    private int hidePhoneRow;
    private int disabledInstantCameraRow;
    private int hideProxySponsorChannelRow;
    private int askBeforeCallRow;
    private int disableNumberRoundingRow;
    private int openArchiveOnPullRow;
    private int formatTimeWithSecondsRow;
    private int nameOrderRow;
    private int idTypeRow;
    private int general2Row;

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows(true);

        return true;
    }

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
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == ipv6Row) {
                NekoConfig.toggleIPv6();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.useIPv6);
                }
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager.getInstance(a).checkConnection();
                    }
                }
            } else if (position == hidePhoneRow) {
                NekoConfig.toggleHidePhone();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hidePhone);
                }
                parentLayout.rebuildAllFragmentViews(false, false);
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                listAdapter.notifyItemChanged(drawerRow, new Object());
            } else if (position == disabledInstantCameraRow) {
                NekoConfig.toggleDisabledInstantCamera();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.disableInstantCamera);
                }
            } else if (position == forceTabletRow) {
                NekoConfig.toggleForceTablet();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.forceTablet);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.formatString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect)).show();
            } else if (position == transparentStatusBarRow) {
                SharedConfig.toggleNoStatusBar();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(SharedConfig.noStatusBar);
                }
                int color = Theme.getColor(Theme.key_actionBarDefault, null, true);
                int alpha = AndroidUtilities.computePerceivedBrightness(color) >= 0.721f ? 0x0f : 0x33;
                if (statusBarColorAnimator != null && statusBarColorAnimator.isRunning()) {
                    statusBarColorAnimator.end();
                }
                statusBarColorAnimator = SharedConfig.noStatusBar ? ValueAnimator.ofInt(alpha, 0) : ValueAnimator.ofInt(0, alpha);
                statusBarColorAnimator.setDuration(300);
                statusBarColorAnimator.addUpdateListener(animation -> getParentActivity().getWindow().setStatusBarColor(ColorUtils.setAlphaComponent(0, (int) animation.getAnimatedValue())));
                statusBarColorAnimator.start();
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
            } else if (position == nameOrderRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("FirstLast", R.string.FirstLast));
                types.add(1);
                arrayList.add(LocaleController.getString("LastFirst", R.string.LastFirst));
                types.add(2);
                PopupHelper.show(arrayList, LocaleController.getString("NameOrder", R.string.NameOrder), types.indexOf(NekoConfig.nameOrder), context, view, i -> {
                    NekoConfig.setNameOrder(types.get(i));
                    listAdapter.notifyItemChanged(nameOrderRow);
                    parentLayout.rebuildAllFragmentViews(false, false);
                });
            } else if (position == eventTypeRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                arrayList.add(LocaleController.getString("DependsOnDate", R.string.DependsOnDate));
                arrayList.add(LocaleController.getString("Christmas", R.string.Christmas));
                arrayList.add(LocaleController.getString("Valentine", R.string.Valentine));
                arrayList.add(LocaleController.getString("Halloween", R.string.Halloween));
                PopupHelper.show(arrayList, LocaleController.getString("EventType", R.string.EventType), NekoConfig.eventType, context, view, i -> {
                    NekoConfig.setEventType(i);
                    listAdapter.notifyItemChanged(eventTypeRow);
                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                });
            } else if (position == newYearRow) {
                NekoConfig.toggleNewYear();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.newYear);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.chats_infotip, LocaleController.formatString("RestartAppToTakeEffect", R.string.RestartAppToTakeEffect)).show();
            } else if (position == translationProviderRow) {
                final int oldProvider = NekoConfig.translationProvider;
                Translator.showTranslationProviderSelector(context, view, param -> {
                    if (param) {
                        listAdapter.notifyItemChanged(translationProviderRow);
                    } else {
                        listAdapter.notifyItemRangeChanged(translationProviderRow, 2);
                    }
                    if (oldProvider != NekoConfig.translationProvider) {
                        if (oldProvider == Translator.PROVIDER_DEEPL) {
                            listAdapter.notifyItemChanged(translationTargetRow);
                            listAdapter.notifyItemRemoved(deepLFormalityRow);
                            updateRows(false);
                        } else if (NekoConfig.translationProvider == Translator.PROVIDER_DEEPL) {
                            updateRows(false);
                            listAdapter.notifyItemChanged(translationTargetRow);
                            listAdapter.notifyItemInserted(deepLFormalityRow);
                        }
                    }
                });
            } else if (position == translationTargetRow) {
                Translator.showTranslationTargetSelector(context, view, () -> listAdapter.notifyItemChanged(translationTargetRow));
            } else if (position == deepLFormalityRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("DeepLFormalityDefault", R.string.DeepLFormalityDefault));
                types.add(DeepLTranslator.FORMALITY_DEFAULT);
                arrayList.add(LocaleController.getString("DeepLFormalityMore", R.string.DeepLFormalityMore));
                types.add(DeepLTranslator.FORMALITY_MORE);
                arrayList.add(LocaleController.getString("DeepLFormalityLess", R.string.DeepLFormalityLess));
                types.add(DeepLTranslator.FORMALITY_LESS);
                PopupHelper.show(arrayList, LocaleController.getString("DeepLFormality", R.string.DeepLFormality), types.indexOf(NekoConfig.deepLFormality), context, view, i -> {
                    NekoConfig.setDeepLFormality(types.get(i));
                    listAdapter.notifyItemChanged(deepLFormalityRow);
                });
            } else if (position == openArchiveOnPullRow) {
                NekoConfig.toggleOpenArchiveOnPull();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.openArchiveOnPull);
                }
            } else if (position == avatarAsDrawerBackgroundRow) {
                NekoConfig.toggleAvatarAsDrawerBackground();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.avatarAsDrawerBackground);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                TransitionManager.beginDelayedTransition(profilePreviewCell);
                listAdapter.notifyItemChanged(drawerRow, new Object());
                if (NekoConfig.avatarAsDrawerBackground) {
                    updateRows(false);
                    listAdapter.notifyItemRangeInserted(avatarBackgroundBlurRow, 2);
                } else {
                    listAdapter.notifyItemRangeRemoved(avatarBackgroundBlurRow, 2);
                    updateRows(false);
                }
            } else if (position == avatarBackgroundBlurRow) {
                NekoConfig.toggleAvatarBackgroundBlur();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.avatarBackgroundBlur);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                listAdapter.notifyItemChanged(drawerRow, new Object());
            } else if (position == avatarBackgroundDarkenRow) {
                NekoConfig.toggleAvatarBackgroundDarken();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.avatarBackgroundDarken);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                listAdapter.notifyItemChanged(drawerRow, new Object());
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
                PopupHelper.show(arrayList, LocaleController.getString("IdType", R.string.IdType), types.indexOf(NekoConfig.idType), context, view, i -> {
                    NekoConfig.setIdType(types.get(i));
                    listAdapter.notifyItemChanged(idTypeRow);
                    parentLayout.rebuildAllFragmentViews(false, false);
                });
            } else if (position == mediaPreviewRow) {
                NekoConfig.toggleMediaPreview();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.mediaPreview);
                }
            } else if (position == formatTimeWithSecondsRow) {
                NekoConfig.toggleFormatTimeWithSeconds();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.formatTimeWithSeconds);
                }
                LocaleController.getInstance().recreateFormatters();
                parentLayout.rebuildAllFragmentViews(false, false);
            } else if (position == accentAsNotificationColorRow) {
                NekoConfig.toggleAccentAsNotificationColor();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.accentAsNotificationColor);
                }
            } else if (position == silenceNonContactsRow) {
                NekoConfig.toggleSilenceNonContacts();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.silenceNonContacts);
                }
            } else if (position == translatorTypeRow) {
                int oldType = NekoConfig.transType;
                Translator.showTranslatorTypeSelector(context, view, () -> {
                    listAdapter.notifyItemChanged(translatorTypeRow);
                    if (oldType != NekoConfig.transType) {
                        if (oldType == NekoConfig.TRANS_TYPE_EXTERNAL) {
                            updateRows(false);
                            listAdapter.notifyItemRangeInserted(translationProviderRow, NekoConfig.translationProvider == Translator.PROVIDER_DEEPL ? 3 : 2);
                        } else if (NekoConfig.transType == NekoConfig.TRANS_TYPE_EXTERNAL) {
                            listAdapter.notifyItemRangeRemoved(translationProviderRow, NekoConfig.translationProvider == Translator.PROVIDER_DEEPL ? 3 : 2);
                            updateRows(false);
                        }
                    }
                });
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

        drawerRow = rowCount++;
        avatarAsDrawerBackgroundRow = rowCount++;
        if (NekoConfig.avatarAsDrawerBackground) {
            avatarBackgroundBlurRow = rowCount++;
            avatarBackgroundDarkenRow = rowCount++;
        } else {
            avatarBackgroundBlurRow = -1;
            avatarBackgroundDarkenRow = -1;
        }
        hidePhoneRow = rowCount++;
        drawer2Row = rowCount++;

        appearanceRow = rowCount++;
        useSystemEmojiRow = rowCount++;
        transparentStatusBarRow = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? rowCount++ : -1;
        forceTabletRow = rowCount++;
        mediaPreviewRow = rowCount++;
        appBarShadowRow = rowCount++;
        formatTimeWithSecondsRow = rowCount++;
        disableNumberRoundingRow = rowCount++;
        newYearRow = NekoConfig.showHiddenFeature ? rowCount++ : -1;
        eventTypeRow = rowCount++;
        appearance2Row = rowCount++;

        notificationRow = rowCount++;
        accentAsNotificationColorRow = rowCount++;
        silenceNonContactsRow = rowCount++;
        notification2Row = rowCount++;

        translatorRow = rowCount++;
        translatorTypeRow = rowCount++;
        if (NekoConfig.transType != NekoConfig.TRANS_TYPE_EXTERNAL) {
            translationProviderRow = rowCount++;
            translationTargetRow = rowCount++;
            deepLFormalityRow = NekoConfig.translationProvider == Translator.PROVIDER_DEEPL ? rowCount++ : -1;
        } else {
            translationProviderRow = -1;
            translationTargetRow = -1;
            deepLFormalityRow = -1;
        }
        translator2Row = rowCount++;

        generalRow = rowCount++;
        disabledInstantCameraRow = rowCount++;
        hideProxySponsorChannelRow = rowCount++;
        askBeforeCallRow = rowCount++;
        openArchiveOnPullRow = rowCount++;
        nameOrderRow = rowCount++;
        idTypeRow = rowCount++;
        general2Row = rowCount++;

        if (notify && listAdapter != null) {
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

        private final Context mContext;

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
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
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
                            case 3:
                                value = LocaleController.getString("Halloween", R.string.Halloween);
                                break;
                            case 0:
                            default:
                                value = LocaleController.getString("DependsOnDate", R.string.DependsOnDate);
                        }
                        textCell.setTextAndValue(LocaleController.getString("EventType", R.string.EventType), value, false);
                    } else if (position == translationProviderRow) {
                        Pair<ArrayList<String>, ArrayList<Integer>> providers = Translator.getProviders();
                        ArrayList<String> names = providers.first;
                        ArrayList<Integer> types = providers.second;
                        if (names == null || types == null) {
                            return;
                        }
                        int index = types.indexOf(NekoConfig.translationProvider);
                        if (index < 0) {
                            textCell.setTextAndValue(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), "", true);
                        } else {
                            String value = names.get(index);
                            textCell.setTextAndValue(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), value, true);
                        }
                    } else if (position == translationTargetRow) {
                        String language = NekoConfig.translationTarget;
                        CharSequence value;
                        if (language.equals("app")) {
                            value = LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp);
                        } else {
                            Locale locale = Locale.forLanguageTag(language);
                            if (!TextUtils.isEmpty(locale.getScript())) {
                                value = HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
                            } else {
                                value = locale.getDisplayName();
                            }
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslationTarget", R.string.TranslationTarget), value, position + 1 != translator2Row);
                    } else if (position == deepLFormalityRow) {
                        String value;
                        switch (NekoConfig.deepLFormality) {
                            case DeepLTranslator.FORMALITY_DEFAULT:
                                value = LocaleController.getString("DeepLFormalityDefault", R.string.DeepLFormalityDefault);
                                break;
                            case DeepLTranslator.FORMALITY_MORE:
                                value = LocaleController.getString("DeepLFormalityMore", R.string.DeepLFormalityMore);
                                break;
                            case DeepLTranslator.FORMALITY_LESS:
                            default:
                                value = LocaleController.getString("DeepLFormalityLess", R.string.DeepLFormalityLess);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("DeepLFormality", R.string.DeepLFormality), value, false);
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
                    } else if (position == translatorTypeRow) {
                        String value;
                        switch (NekoConfig.transType) {
                            case NekoConfig.TRANS_TYPE_TG:
                                value = LocaleController.getString("TranslatorTypeTG", R.string.TranslatorTypeTG);
                                break;
                            case NekoConfig.TRANS_TYPE_EXTERNAL:
                                value = LocaleController.getString("TranslatorTypeExternal", R.string.TranslatorTypeExternal);
                                break;
                            case NekoConfig.TRANS_TYPE_NEKO:
                            default:
                                value = LocaleController.getString("TranslatorTypeNeko", R.string.TranslatorTypeNeko);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslatorType", R.string.TranslatorType), value, NekoConfig.transType != NekoConfig.TRANS_TYPE_EXTERNAL);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ipv6Row) {
                        textCell.setTextAndCheck(LocaleController.getString("IPv6", R.string.IPv6), NekoConfig.useIPv6, false);
                    } else if (position == hidePhoneRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HidePhone", R.string.HidePhone), NekoConfig.hidePhone, false);
                    } else if (position == disabledInstantCameraRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableInstantCamera", R.string.DisableInstantCamera), NekoConfig.disableInstantCamera, true);
                    } else if (position == transparentStatusBarRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TransparentStatusBar", R.string.TransparentStatusBar), SharedConfig.noStatusBar, true);
                    } else if (position == hideProxySponsorChannelRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HideProxySponsorChannel", R.string.HideProxySponsorChannel), NekoConfig.hideProxySponsorChannel, true);
                    } else if (position == useSystemEmojiRow) {
                        textCell.setTextAndCheck(LocaleController.getString("EmojiUseDefault", R.string.EmojiUseDefault), NekoConfig.useSystemEmoji, true);
                    } else if (position == forceTabletRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ForceTabletMode", R.string.ForceTabletMode), NekoConfig.forceTablet, true);
                    } else if (position == newYearRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ChristmasHat", R.string.ChristmasHat), NekoConfig.newYear, true);
                    } else if (position == openArchiveOnPullRow) {
                        textCell.setTextAndCheck(LocaleController.getString("OpenArchiveOnPull", R.string.OpenArchiveOnPull), NekoConfig.openArchiveOnPull, true);
                    } else if (position == avatarAsDrawerBackgroundRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AvatarAsBackground", R.string.AvatarAsBackground), NekoConfig.avatarAsDrawerBackground, true);
                    } else if (position == askBeforeCallRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AskBeforeCalling", R.string.AskBeforeCalling), NekoConfig.askBeforeCall, true);
                    } else if (position == disableNumberRoundingRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("DisableNumberRounding", R.string.DisableNumberRounding), "4.8K -> 4777", NekoConfig.disableNumberRounding, true, true);
                    } else if (position == appBarShadowRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableAppBarShadow", R.string.DisableAppBarShadow), NekoConfig.disableAppBarShadow, true);
                    } else if (position == mediaPreviewRow) {
                        textCell.setTextAndCheck(LocaleController.getString("MediaPreview", R.string.MediaPreview), NekoConfig.mediaPreview, true);
                    } else if (position == formatTimeWithSecondsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("FormatWithSeconds", R.string.FormatWithSeconds), NekoConfig.formatTimeWithSeconds, true);
                    } else if (position == avatarBackgroundBlurRow) {
                        textCell.setTextAndCheck(LocaleController.getString("BlurAvatarBackground", R.string.BlurAvatarBackground), NekoConfig.avatarBackgroundBlur, true);
                    } else if (position == avatarBackgroundDarkenRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DarkenAvatarBackground", R.string.DarkenAvatarBackground), NekoConfig.avatarBackgroundDarken, true);
                    } else if (position == accentAsNotificationColorRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccentAsNotificationColor", R.string.AccentAsNotificationColor), NekoConfig.accentAsNotificationColor, true);
                    } else if (position == silenceNonContactsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("SilenceNonContacts", R.string.SilenceNonContacts), NekoConfig.silenceNonContacts, false);
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
                    } else if (position == notificationRow) {
                        headerCell.setText(LocaleController.getString("Notifications", R.string.Notifications));
                    } else if (position == translatorRow) {
                        headerCell.setText(LocaleController.getString("Translator", R.string.Translator));
                    }
                    break;
                }
                case 7: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == general2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("IdTypeAbout", R.string.IdTypeAbout));
                    } else if (position == notification2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("SilenceNonContactsAbout", R.string.SilenceNonContactsAbout));
                    } else if (position == translator2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("TranslateMessagesInfo1", R.string.TranslateMessagesInfo1));
                    }
                    break;
                }
                case 8: {
                    DrawerProfilePreviewCell cell = (DrawerProfilePreviewCell) holder.itemView;
                    cell.setUser(getUserConfig().getCurrentUser(), false);
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
                case 8:
                    view = profilePreviewCell = new DrawerProfilePreviewCell(mContext);
                    view.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == connection2Row || position == appearance2Row || position == drawer2Row) {
                return 1;
            } else if (position == eventTypeRow || position == nameOrderRow || position == idTypeRow || position == translationProviderRow ||
                    position == translationTargetRow || position == deepLFormalityRow || position == translatorTypeRow) {
                return 2;
            } else if (position == ipv6Row || position == newYearRow ||
                    (position > appearanceRow && position <= disableNumberRoundingRow) ||
                    (position > generalRow && position < nameOrderRow) ||
                    (position > drawerRow && position < drawer2Row) ||
                    (position > notificationRow && position < notification2Row)) {
                return 3;
            } else if (position == generalRow || position == connectionRow || position == appearanceRow || position == notificationRow ||
                    position == translatorRow) {
                return 4;
            } else if (position == general2Row || position == notification2Row || position == translator2Row) {
                return 7;
            } else if (position == drawerRow) {
                return 8;
            }
            return 2;
        }
    }
}
