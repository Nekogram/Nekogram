package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
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

    private int appearanceRow;
    private int emojiSetsRow;
    private int mediaPreviewRow;
    private int predictiveBackAnimationRow;
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
        if (position == tabletModeRow) {
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
        } else if (position == predictiveBackAnimationRow) {
            NekoConfig.togglePredictiveBackAnimation();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.predictiveBackAnimation);
            }
            showRestartBulletin();
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

        appearanceRow = addRow("appearance");
        emojiSetsRow = addRow("emojiSets");
        mediaPreviewRow = addRow("mediaPreview");
        predictiveBackAnimationRow = addRow("predictiveBackAnimation");
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
                    if (position == disableNumberRoundingRow) {
                        textCell.setTextAndValueAndCheck(LocaleController.getString(R.string.DisableNumberRounding), "4.8K -> 4777", NekoConfig.disableNumberRounding, divider, divider);
                    } else if (position == appBarShadowRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.DisableAppBarShadow), NekoConfig.disableAppBarShadow, divider);
                    } else if (position == mediaPreviewRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.MediaPreview), NekoConfig.mediaPreview, divider);
                    } else if (position == formatTimeWithSecondsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.FormatWithSeconds), NekoConfig.formatTimeWithSeconds, divider);
                    } else if (position == hideAllTabRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.HideAllTab), NekoConfig.hideAllTab, divider);
                    } else if (position == predictiveBackAnimationRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.PredictiveBackAnimation), NekoConfig.predictiveBackAnimation, divider);
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
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == appearance2Row) {
                return TYPE_SHADOW;
            } else if (position == eventTypeRow || position == tabsTitleTypeRow || position == tabletModeRow) {
                return TYPE_SETTINGS;
            } else if (position == hideAllTabRow ||
                    (position > emojiSetsRow && position <= disableNumberRoundingRow)) {
                return TYPE_CHECK;
            } else if (position == appearanceRow || position == foldersRow) {
                return TYPE_HEADER;
            } else if (position == folders2Row) {
                return TYPE_INFO_PRIVACY;
            } else if (position == emojiSetsRow) {
                return TYPE_EMOJI;
            }
            return TYPE_SETTINGS;
        }
    }
}
