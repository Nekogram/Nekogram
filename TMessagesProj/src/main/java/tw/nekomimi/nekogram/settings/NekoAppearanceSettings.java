package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.os.Build;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBarLayout;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;

public class NekoAppearanceSettings extends BaseNekoSettingsActivity {

    private DrawerProfilePreviewCell profilePreviewCell;

    private int drawerRow;
    private int avatarAsDrawerBackgroundRow;
    private int avatarBackgroundBlurRow;
    private int avatarBackgroundDarkenRow;
    private int hidePhoneRow;
    private int drawer2Row;

    private int appearanceRow;
    private int useSystemEmojiRow;
    private int transparentStatusBarRow;
    private int forceTabletRow;
    private int mediaPreviewRow;
    private int appBarShadowRow;
    private int formatTimeWithSecondsRow;
    private int disableNumberRoundingRow;
    private int newYearRow;
    private int eventTypeRow;
    private int appearance2Row;

    private int foldersRow;
    private int showTabsOnForwardRow;
    private int hideAllTabRow;
    private int tabsTitleTypeRow;
    private int folders2Row;

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);
        actionBar.setTitle(LocaleController.getString("Appearance", R.string.Appearance));

        listAdapter = new ListAdapter(context);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == hidePhoneRow) {
                NekoConfig.toggleHidePhone();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hidePhone);
                }
                parentLayout.rebuildAllFragmentViews(false, false);
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                listAdapter.notifyItemChanged(drawerRow, new Object());
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
            } else if (position == useSystemEmojiRow) {
                NekoConfig.toggleUseSystemEmoji();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.useSystemEmoji);
                }
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
            } else if (position == avatarAsDrawerBackgroundRow) {
                NekoConfig.toggleAvatarAsDrawerBackground();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.avatarAsDrawerBackground);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                TransitionManager.beginDelayedTransition(profilePreviewCell);
                listAdapter.notifyItemChanged(drawerRow, new Object());
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
                listAdapter.notifyItemChanged(drawerRow, new Object());
            } else if (position == avatarBackgroundDarkenRow) {
                NekoConfig.toggleAvatarBackgroundDarken();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.avatarBackgroundDarken);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                listAdapter.notifyItemChanged(drawerRow, new Object());
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
                LocaleController.getInstance().recreateFormatters();
                parentLayout.rebuildAllFragmentViews(false, false);
            } else if (position == showTabsOnForwardRow) {
                NekoConfig.toggleShowTabsOnForward();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.showTabsOnForward);
                }
            } else if (position == hideAllTabRow) {
                NekoConfig.toggleHideAllTab();
                if (view instanceof TextCheckCell) {
                    ((TextCheckCell) view).setChecked(NekoConfig.hideAllTab);
                }
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            } else if (position == tabsTitleTypeRow) {
                ArrayList<String> arrayList = new ArrayList<>();
                ArrayList<Integer> types = new ArrayList<>();
                arrayList.add(LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText));
                types.add(NekoConfig.TITLE_TYPE_TEXT);
                arrayList.add(LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon));
                types.add(NekoConfig.TITLE_TYPE_ICON);
                arrayList.add(LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix));
                types.add(NekoConfig.TITLE_TYPE_MIX);
                PopupHelper.show(arrayList, LocaleController.getString("TabTitleType", R.string.TabTitleType), types.indexOf(NekoConfig.tabsTitleType), context, view, i -> {
                    NekoConfig.setTabsTitleType(types.get(i));
                    listAdapter.notifyItemChanged(tabsTitleTypeRow);
                    getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
                });
            }
        });

        return fragmentView;
    }

    @Override
    protected void updateRows() {
        rowCount = 0;

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

        foldersRow = rowCount++;
        showTabsOnForwardRow = rowCount++;
        hideAllTabRow = rowCount++;
        tabsTitleTypeRow = rowCount++;
        folders2Row = rowCount++;
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
                    if (position == eventTypeRow) {
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
                    } else if (position == tabsTitleTypeRow) {
                        String value;
                        switch (NekoConfig.tabsTitleType) {
                            case NekoConfig.TITLE_TYPE_TEXT:
                                value = LocaleController.getString("TabTitleTypeText", R.string.TabTitleTypeText);
                                break;
                            case NekoConfig.TITLE_TYPE_ICON:
                                value = LocaleController.getString("TabTitleTypeIcon", R.string.TabTitleTypeIcon);
                                break;
                            case NekoConfig.TITLE_TYPE_MIX:
                            default:
                                value = LocaleController.getString("TabTitleTypeMix", R.string.TabTitleTypeMix);
                        }
                        textCell.setTextAndValue(LocaleController.getString("TabTitleType", R.string.TabTitleType), value, false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == hidePhoneRow) {
                        textCell.setTextAndCheck(LocaleController.getString("HidePhone", R.string.HidePhone), NekoConfig.hidePhone, false);
                    } else if (position == transparentStatusBarRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TransparentStatusBar", R.string.TransparentStatusBar), SharedConfig.noStatusBar, true);
                    } else if (position == useSystemEmojiRow) {
                        textCell.setTextAndCheck(LocaleController.getString("EmojiUseDefault", R.string.EmojiUseDefault), NekoConfig.useSystemEmoji, true);
                    } else if (position == forceTabletRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ForceTabletMode", R.string.ForceTabletMode), NekoConfig.forceTablet, true);
                    } else if (position == newYearRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ChristmasHat", R.string.ChristmasHat), NekoConfig.newYear, true);
                    } else if (position == avatarAsDrawerBackgroundRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AvatarAsBackground", R.string.AvatarAsBackground), NekoConfig.avatarAsDrawerBackground, true);
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
                    } else if (position == showTabsOnForwardRow) {
                        textCell.setTextAndCheck(LocaleController.getString("ShowTabsOnForward", R.string.ShowTabsOnForward), NekoConfig.showTabsOnForward, true);
                    } else if (position == hideAllTabRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString("HideAllTab", R.string.HideAllTab), LocaleController.getString("HideAllTabAbout", R.string.HideAllTabAbout), NekoConfig.hideAllTab, true, true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == appearanceRow) {
                        headerCell.setText(LocaleController.getString("Appearance", R.string.Appearance));
                    } else if (position == foldersRow) {
                        headerCell.setText(LocaleController.getString("Filters", R.string.Filters));
                    }
                    break;
                }
                case 7: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == folders2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("TabTitleTypeTip", R.string.TabTitleTypeTip));
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

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == Integer.MAX_VALUE) {
                profilePreviewCell = new DrawerProfilePreviewCell(mContext);
                profilePreviewCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                profilePreviewCell.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
                return new RecyclerListView.Holder(profilePreviewCell);
            } else {
                return super.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == appearance2Row || position == drawer2Row) {
                return 1;
            } else if (position == eventTypeRow || position == tabsTitleTypeRow) {
                return 2;
            } else if (position == newYearRow || position == showTabsOnForwardRow || position == hideAllTabRow ||
                    (position > appearanceRow && position <= disableNumberRoundingRow) ||
                    (position > drawerRow && position < drawer2Row)) {
                return 3;
            } else if (position == appearanceRow || position == foldersRow) {
                return 4;
            } else if (position == folders2Row) {
                return 7;
            } else if (position == drawerRow) {
                return Integer.MAX_VALUE;
            }
            return 2;
        }
    }
}
