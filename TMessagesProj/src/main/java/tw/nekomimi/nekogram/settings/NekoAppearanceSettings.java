package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.transition.TransitionManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.DrawerProfileCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.EmojiHelper;
import tw.nekomimi.nekogram.helpers.PopupHelper;

public class NekoAppearanceSettings extends BaseNekoSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private DrawerProfilePreviewCell profilePreviewCell;

    private int drawerRow;
    private int avatarAsDrawerBackgroundRow;
    private int avatarBackgroundBlurRow;
    private int avatarBackgroundDarkenRow;
    private int hidePhoneRow;
    private int drawer2Row;

    private int appearanceRow;
    private int emojiSetsRow;
    private int mediaPreviewRow;
    private int appBarShadowRow;
    private int formatTimeWithSecondsRow;
    private int disableNumberRoundingRow;
    private int tabletModeRow;
    private int eventTypeRow;
    private int appearance2Row;

    private int foldersRow;
    private int hideAllTabRow;
    private int tabsTitleTypeRow;
    private int folders2Row;

    @Override
    public boolean onFragmentCreate() {
        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);
        return super.onFragmentCreate();
    }

    @Override
    public void onFragmentDestroy() {
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
        super.onFragmentDestroy();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == hidePhoneRow) {
            NekoConfig.toggleHidePhone();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hidePhone);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
        } else if (position == tabletModeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TabletModeAuto));
            types.add(NekoConfig.TABLET_AUTO);
            arrayList.add(LocaleController.getString(R.string.Enable));
            types.add(NekoConfig.TABLET_ENABLE);
            arrayList.add(LocaleController.getString(R.string.Disable));
            types.add(NekoConfig.TABLET_DISABLE);
            PopupHelper.show(arrayList, LocaleController.getString(R.string.TabletMode), types.indexOf(NekoConfig.tabletMode), getParentActivity(), view, i -> {
                NekoConfig.setTabletMode(types.get(i));
                listAdapter.notifyItemChanged(tabletModeRow, PARTIAL);
                AndroidUtilities.resetTabletFlag();
                if (getParentActivity() instanceof LaunchActivity) {
                    ((LaunchActivity) getParentActivity()).invalidateTabletMode();
                }
            }, resourcesProvider);
        } else if (position == emojiSetsRow) {
            presentFragment(new NekoEmojiSettingsActivity());
        } else if (position == eventTypeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.DependsOnDate));
            arrayList.add(LocaleController.getString(R.string.Christmas));
            arrayList.add(LocaleController.getString(R.string.Valentine));
            arrayList.add(LocaleController.getString(R.string.Halloween));
            PopupHelper.show(arrayList, LocaleController.getString(R.string.EventType), NekoConfig.eventType, getParentActivity(), view, i -> {
                NekoConfig.setEventType(i);
                listAdapter.notifyItemChanged(eventTypeRow, PARTIAL);
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            }, resourcesProvider);
        } else if (position == avatarAsDrawerBackgroundRow) {
            NekoConfig.toggleAvatarAsDrawerBackground();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.avatarAsDrawerBackground);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            TransitionManager.beginDelayedTransition(profilePreviewCell);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
            if (NekoConfig.avatarAsDrawerBackground) {
                updateRows();
                listAdapter.notifyItemRangeInserted(avatarBackgroundBlurRow, 2);
            } else {
                listAdapter.notifyItemRangeRemoved(avatarBackgroundBlurRow, 2);
                updateRows();
            }
        } else if (position == avatarBackgroundBlurRow) {
            NekoConfig.toggleAvatarBackgroundBlur();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.avatarBackgroundBlur);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
        } else if (position == avatarBackgroundDarkenRow) {
            NekoConfig.toggleAvatarBackgroundDarken();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.avatarBackgroundDarken);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
            listAdapter.notifyItemChanged(drawerRow, PARTIAL);
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
            parentLayout.setHeaderShadow(NekoConfig.disableAppBarShadow ? null : parentLayout.getParentActivity().getDrawable(R.drawable.header_shadow).mutate());
            parentLayout.rebuildAllFragmentViews(false, false);
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
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (position == hideAllTabRow) {
            NekoConfig.toggleHideAllTab();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideAllTab);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (position == tabsTitleTypeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TabTitleTypeText));
            types.add(NekoConfig.TITLE_TYPE_TEXT);
            arrayList.add(LocaleController.getString(R.string.TabTitleTypeIcon));
            types.add(NekoConfig.TITLE_TYPE_ICON);
            arrayList.add(LocaleController.getString(R.string.TabTitleTypeMix));
            types.add(NekoConfig.TITLE_TYPE_MIX);
            PopupHelper.show(arrayList, LocaleController.getString(R.string.TabTitleType), types.indexOf(NekoConfig.tabsTitleType), getParentActivity(), view, i -> {
                NekoConfig.setTabsTitleType(types.get(i));
                listAdapter.notifyItemChanged(tabsTitleTypeRow, PARTIAL);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            }, resourcesProvider);
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.ChangeChannelNameColor2);
    }

    @Override
    protected String getKey() {
        return "a";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        drawerRow = addRow("drawer");
        avatarAsDrawerBackgroundRow = addRow("avatarAsDrawerBackground");
        if (NekoConfig.avatarAsDrawerBackground) {
            avatarBackgroundBlurRow = addRow("avatarBackgroundBlur");
            avatarBackgroundDarkenRow = addRow("avatarBackgroundDarken");
        } else {
            avatarBackgroundBlurRow = -1;
            avatarBackgroundDarkenRow = -1;
        }
        hidePhoneRow = addRow("hidePhone");
        drawer2Row = addRow();

        appearanceRow = addRow("appearance");
        emojiSetsRow = addRow("emojiSets");
        mediaPreviewRow = addRow("mediaPreview");
        appBarShadowRow = addRow("appBarShadow");
        formatTimeWithSecondsRow = addRow("formatTimeWithSeconds");
        disableNumberRoundingRow = addRow("disableNumberRounding");
        eventTypeRow = addRow("eventType");
        tabletModeRow = addRow("tabletMode");
        appearance2Row = addRow();

        foldersRow = addRow("folders");
        hideAllTabRow = addRow("hideAllTab");
        tabsTitleTypeRow = addRow("tabsTitleType");
        folders2Row = addRow();
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.emojiLoaded && listAdapter != null) {
            listAdapter.notifyItemChanged(emojiSetsRow, PARTIAL);
        }
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
                    if (position == eventTypeRow) {
                        String value = switch (NekoConfig.eventType) {
                            case 1 -> LocaleController.getString(R.string.Christmas);
                            case 2 -> LocaleController.getString(R.string.Valentine);
                            case 3 -> LocaleController.getString(R.string.Halloween);
                            default -> LocaleController.getString(R.string.DependsOnDate);
                        };
                        textCell.setTextAndValue(LocaleController.getString(R.string.EventType), value, partial, divider);
                    } else if (position == tabsTitleTypeRow) {
                        String value = switch (NekoConfig.tabsTitleType) {
                            case NekoConfig.TITLE_TYPE_TEXT ->
                                    LocaleController.getString(R.string.TabTitleTypeText);
                            case NekoConfig.TITLE_TYPE_ICON ->
                                    LocaleController.getString(R.string.TabTitleTypeIcon);
                            default -> LocaleController.getString(R.string.TabTitleTypeMix);
                        };
                        textCell.setTextAndValue(LocaleController.getString(R.string.TabTitleType), value, partial, divider);
                    } else if (position == tabletModeRow) {
                        String value = switch (NekoConfig.tabletMode) {
                            case NekoConfig.TABLET_AUTO ->
                                    LocaleController.getString(R.string.TabletModeAuto);
                            case NekoConfig.TABLET_ENABLE ->
                                    LocaleController.getString(R.string.Enable);
                            default -> LocaleController.getString(R.string.Disable);
                        };
                        textCell.setTextAndValue(LocaleController.getString(R.string.TabletMode), value, partial, divider);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == hidePhoneRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.HidePhone), NekoConfig.hidePhone, divider);
                    } else if (position == avatarAsDrawerBackgroundRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AvatarAsBackground), NekoConfig.avatarAsDrawerBackground, divider);
                    } else if (position == disableNumberRoundingRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.DisableNumberRounding), "4.8K -> 4777", NekoConfig.disableNumberRounding, divider, divider);
                    } else if (position == appBarShadowRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DisableAppBarShadow), NekoConfig.disableAppBarShadow, divider);
                    } else if (position == mediaPreviewRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.MediaPreview), NekoConfig.mediaPreview, divider);
                    } else if (position == formatTimeWithSecondsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.FormatWithSeconds), NekoConfig.formatTimeWithSeconds, divider);
                    } else if (position == avatarBackgroundBlurRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.BlurAvatarBackground), NekoConfig.avatarBackgroundBlur, divider);
                    } else if (position == avatarBackgroundDarkenRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DarkenAvatarBackground), NekoConfig.avatarBackgroundDarken, divider);
                    } else if (position == hideAllTabRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.HideAllTab), NekoConfig.hideAllTab, divider);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == appearanceRow) {
                        headerCell.setText(LocaleController.getString(R.string.ChangeChannelNameColor2));
                    } else if (position == foldersRow) {
                        headerCell.setText(LocaleController.getString(R.string.Filters));
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == folders2Row) {
                        cell.setText(LocaleController.getString(R.string.TabTitleTypeTip));
                    }
                    break;
                }
                case TYPE_EMOJI: {
                    EmojiSetCell emojiPackSetCell = (EmojiSetCell) holder.itemView;
                    if (position == emojiSetsRow) {
                        emojiPackSetCell.setData(EmojiHelper.getInstance().getCurrentEmojiPackInfo(), partial, divider);
                    }
                    break;
                }
                case Integer.MAX_VALUE: {
                    DrawerProfilePreviewCell cell = (DrawerProfilePreviewCell) holder.itemView;
                    cell.setUser(getUserConfig().getCurrentUser(), false);
                    break;
                }
            }
        }

        @Override
        public View createCustomView(int viewType) {
            if (viewType == Integer.MAX_VALUE) {
                return profilePreviewCell = new DrawerProfilePreviewCell(mContext);
            } else {
                return super.createCustomView(viewType);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == appearance2Row || position == drawer2Row) {
                return TYPE_SHADOW;
            } else if (position == eventTypeRow || position == tabsTitleTypeRow || position == tabletModeRow) {
                return TYPE_SETTINGS;
            } else if (position == hideAllTabRow ||
                    (position > emojiSetsRow && position <= disableNumberRoundingRow) ||
                    (position > drawerRow && position < drawer2Row)) {
                return TYPE_CHECK;
            } else if (position == appearanceRow || position == foldersRow) {
                return TYPE_HEADER;
            } else if (position == folders2Row) {
                return TYPE_INFO_PRIVACY;
            } else if (position == emojiSetsRow) {
                return TYPE_EMOJI;
            } else if (position == drawerRow) {
                return Integer.MAX_VALUE;
            }
            return TYPE_SETTINGS;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ThemeDescription.ThemeDescriptionDelegate cellDelegate = () -> {
            if (listView != null) {
                for (int i = 0; i < listView.getChildCount(); i++) {
                    View child = listView.getChildAt(i);
                    if (child instanceof DrawerProfileCell profileCell) {
                        profileCell.applyBackground(true);
                        profileCell.updateColors();
                    }
                }
            }
        };
        ArrayList<ThemeDescription> themeDescriptions = super.getThemeDescriptions();
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuName));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhone));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuPhoneCats));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chat_serviceBackground));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadow));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{DrawerProfileCell.class}, null, null, null, Theme.key_chats_menuTopShadowCats));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_IMAGECOLOR, new Class[]{DrawerProfileCell.class}, new String[]{"darkThemeView"}, null, null, null, Theme.key_chats_menuName));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{DrawerProfileCell.class}, null, null, cellDelegate, Theme.key_chats_menuTopBackgroundCats));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR | ThemeDescription.FLAG_CHECKTAG, new Class[]{DrawerProfileCell.class}, null, null, cellDelegate, Theme.key_chats_menuTopBackground));
        return themeDescriptions;
    }
}
