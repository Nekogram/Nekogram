package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.EmojiHelper;

public class NekoAppearanceSettingsActivity extends BaseNekoSettingsActivity {

    private final int emojiSetsRow = rowId++;
    private final int predictiveBackAnimationRow = rowId++;
    private final int appBarShadowRow = rowId++;
    private final int gooeyAvatarAnimationRow = rowId++;
    private final int formatTimeWithSecondsRow = rowId++;
    private final int disableNumberRoundingRow = rowId++;
    private final int hideBottomNavigationBarRow = rowId++;
    private final int tabletModeRow = rowId++;

    private final int hideStoriesRow = rowId++;
    private final int mediaPreviewRow = rowId++;
    private final int miniSenderAvatarRow = rowId++;

    private final int hideAllTabRow = rowId++;
    private final int tabsTitleTypeRow = rowId++;
    private final int tabsPositionRow = rowId++;

    private final int strokeOnViewsRow = rowId++;

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.ChangeChannelNameColor2)));
        items.add(EmojiSetCellFactory.of(emojiSetsRow, LocaleController.getString(R.string.EmojiSets)).slug("emojiSets"));
        items.add(UItem.asCheck(predictiveBackAnimationRow, LocaleController.getString(R.string.PredictiveBackAnimation)).slug("predictiveBackAnimation").setChecked(NekoConfig.predictiveBackAnimation));
        items.add(UItem.asCheck(gooeyAvatarAnimationRow, LocaleController.getString(R.string.GooeyAvatarAnimation)).slug("gooeyAvatarAnimation").setChecked(NekoConfig.gooeyAvatarAnimation));
        items.add(UItem.asCheck(appBarShadowRow, LocaleController.getString(R.string.DisableAppBarShadow)).slug("appBarShadow").setChecked(NekoConfig.disableAppBarShadow));
        items.add(UItem.asCheck(formatTimeWithSecondsRow, LocaleController.getString(R.string.FormatWithSeconds)).slug("formatTimeWithSeconds").setChecked(NekoConfig.formatTimeWithSeconds));
        items.add(UItem.asCheck(disableNumberRoundingRow, LocaleController.getString(R.string.DisableNumberRounding), "4.8K -> 4777").slug("disableNumberRounding").setChecked(NekoConfig.disableNumberRounding));
        items.add(UItem.asCheck(hideBottomNavigationBarRow, LocaleController.getString(R.string.HideBottomNavigationBar)).setChecked(NekoConfig.hideBottomNavigationBar).slug("hideBottomNavigationBar"));
        items.add(TextSettingsCellFactory.of(tabletModeRow, LocaleController.getString(R.string.TabletMode), switch (NekoConfig.tabletMode) {
            case NekoConfig.TABLET_AUTO -> LocaleController.getString(R.string.TabletModeAuto);
            case NekoConfig.TABLET_ENABLE -> LocaleController.getString(R.string.Enable);
            default -> LocaleController.getString(R.string.Disable);
        }).slug("tabletMode"));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.SavedDialogsTab)));
        items.add(UItem.asCheck(hideStoriesRow, LocaleController.getString(R.string.HideStories)).slug("hideStories").setChecked(NekoConfig.hideStories));
        items.add(UItem.asCheck(mediaPreviewRow, LocaleController.getString(R.string.MediaPreview)).slug("mediaPreview").setChecked(NekoConfig.mediaPreview));
        items.add(UItem.asCheck(miniSenderAvatarRow, LocaleController.getString(R.string.MiniSenderAvatar)).slug("miniSenderAvatar").setChecked(NekoConfig.miniSenderAvatar));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.Filters)));
        items.add(UItem.asCheck(hideAllTabRow, LocaleController.getString(R.string.HideAllTab)).slug("hideAllTab").setChecked(NekoConfig.hideAllTab));
        items.add(TextSettingsCellFactory.of(tabsTitleTypeRow, LocaleController.getString(R.string.TabTitleType), switch (NekoConfig.tabsTitleType) {
            case NekoConfig.TITLE_TYPE_TEXT ->
                    LocaleController.getString(R.string.TabTitleTypeText);
            case NekoConfig.TITLE_TYPE_ICON ->
                    LocaleController.getString(R.string.TabTitleTypeIcon);
            default -> LocaleController.getString(R.string.TabTitleTypeMix);
        }).slug("tabsTitleType"));
        items.add(TextSettingsCellFactory.of(tabsPositionRow, LocaleController.getString(R.string.TabsPosition), LocaleController.getString(NekoConfig.bottomFilterTabs ? R.string.TabsPositionBottom : R.string.TabsPositionTop)).slug("tabsPosition"));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.LiteOptionsBlur2)));
        items.add(UItem.asCheck(strokeOnViewsRow, LocaleController.getString(R.string.StrokeOnViews)).setChecked(NekoConfig.strokeOnViews).slug("strokeOnViews"));
        items.add(UItem.asShadow(null));

    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id == tabletModeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TabletModeAuto));
            types.add(NekoConfig.TABLET_AUTO);
            arrayList.add(LocaleController.getString(R.string.Enable));
            types.add(NekoConfig.TABLET_ENABLE);
            arrayList.add(LocaleController.getString(R.string.Disable));
            types.add(NekoConfig.TABLET_DISABLE);
            showPopup(arrayList, types.indexOf(NekoConfig.tabletMode), item, view, i -> {
                NekoConfig.setTabletMode(types.get(i));
                listView.adapter.notifyItemChanged(position, PARTIAL);
                AndroidUtilities.resetTabletFlag();
                if (getParentActivity() instanceof LaunchActivity) {
                    ((LaunchActivity) getParentActivity()).invalidateTabletMode();
                }
            });
        } else if (id == emojiSetsRow) {
            presentFragment(new NekoEmojiSettingsActivity());
        } else if (id == disableNumberRoundingRow) {
            NekoConfig.toggleDisableNumberRounding();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableNumberRounding);
            }
        } else if (id == appBarShadowRow) {
            NekoConfig.toggleDisableAppBarShadow();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableAppBarShadow);
            }
            parentLayout.setHeaderShadow(NekoConfig.disableAppBarShadow ? null : parentLayout.getParentActivity().getDrawable(R.drawable.header_shadow).mutate());
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (id == mediaPreviewRow) {
            NekoConfig.toggleMediaPreview();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.mediaPreview);
            }
        } else if (id == hideStoriesRow) {
            NekoConfig.toggleHideStories();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideStories);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.storiesEnabledUpdate);
        } else if (id == formatTimeWithSecondsRow) {
            NekoConfig.toggleFormatTimeWithSeconds();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.formatTimeWithSeconds);
            }
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (id == hideAllTabRow) {
            NekoConfig.toggleHideAllTab();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideAllTab);
            }
            getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
        } else if (id == tabsTitleTypeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TabTitleTypeText));
            types.add(NekoConfig.TITLE_TYPE_TEXT);
            arrayList.add(LocaleController.getString(R.string.TabTitleTypeIcon));
            types.add(NekoConfig.TITLE_TYPE_ICON);
            arrayList.add(LocaleController.getString(R.string.TabTitleTypeMix));
            types.add(NekoConfig.TITLE_TYPE_MIX);
            showPopup(arrayList, types.indexOf(NekoConfig.tabsTitleType), item, view, i -> {
                NekoConfig.setTabsTitleType(types.get(i));
                listView.adapter.notifyItemChanged(position, PARTIAL);
                getNotificationCenter().postNotificationName(NotificationCenter.dialogFiltersUpdated);
            });
        } else if (id == predictiveBackAnimationRow) {
            NekoConfig.togglePredictiveBackAnimation();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.predictiveBackAnimation);
            }
            showRestartBulletin();
        } else if (id == hideBottomNavigationBarRow) {
            NekoConfig.toggleHideBottomNavigationBar();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.hideBottomNavigationBar);
            }
            parentLayout.rebuildAllFragmentViews(false, false);
        } else if (id == tabsPositionRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            arrayList.add(LocaleController.getString(R.string.TabsPositionTop));
            arrayList.add(LocaleController.getString(R.string.TabsPositionBottom));
            showPopup(arrayList, NekoConfig.bottomFilterTabs ? 1 : 0, item, view, i -> {
                NekoConfig.setBottomFilterTabs(i == 1);
                listView.adapter.notifyItemChanged(position, PARTIAL);
                parentLayout.rebuildAllFragmentViews(false, false);
            });
        } else if (id == strokeOnViewsRow) {
            NekoConfig.toggleStrokeOnViews();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.strokeOnViews);
            }
        } else if (id == gooeyAvatarAnimationRow) {
            NekoConfig.toggleGooeyAvatarAnimation();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.gooeyAvatarAnimation);
            }
        } else if (id == miniSenderAvatarRow) {
            NekoConfig.toggleMiniSenderAvatar();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.miniSenderAvatar);
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.ChangeChannelNameColor2);
    }

    @Override
    protected String getKey() {
        return "a";
    }

    private static class EmojiSetCellFactory extends UItem.UItemFactory<EmojiSetCell> {
        static {
            setup(new EmojiSetCellFactory());
        }

        @Override
        public EmojiSetCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new EmojiSetCell(context, false, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var cell = (EmojiSetCell) view;
            var pack = cell.getPack();
            var newPack = EmojiHelper.getInstance().getCurrentEmojiPackInfo();
            cell.setData(newPack, pack != null && !pack.getPackId().equals(newPack.getPackId()), divider);
        }

        public static UItem of(int id, String title) {
            var item = UItem.ofFactory(EmojiSetCellFactory.class);
            item.id = id;
            item.text = title;
            return item;
        }
    }
}
