package tw.nekomimi.nekogram.accessibility;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;

import tw.nekomimi.nekogram.helpers.PopupHelper;

public class AccessibilitySettingsActivity extends BaseFragment {
    private static final ArrayList<String> REWIND_TYPES = new ArrayList<>();
    private static final ArrayList<String> ADD_TYPES = new ArrayList<>();
    private static final ArrayList<String> SEEKBAR_TIME_VALUES = new ArrayList<>();

    private ListAdapter listAdapter;

    private int rowCount = 0;

    private final int rewindHeadingRow = rowCount++;
    private final int typeOfRewindRow = rowCount++;
    private final int typeOfRewindVideoRow = rowCount++;
    private final int rewindHeading2Row = rowCount++;
    private final int seekbarHeadingRow = rowCount++;
    private final int showNumbersOfItemsRow = rowCount++;
    private final int showIndexOfItemRow = rowCount++;
    private final int showValueChangesRow = rowCount++;
    private final int timeBeforeAnnouncingOfSeekbarRow = rowCount++;
    private final int seekbarHeading2Row = rowCount++;
    private final int differentHeadingRow = rowCount++;
    private final int addTypeOfChatToDescriptionRow = rowCount++;
    private final int showLinkNodesRow = rowCount++;
    private final int hideLinksRow = rowCount++;
    private final int differentHeading2Row = rowCount++;

    static {
        REWIND_TYPES.add(LocaleController.getString("AccRewindTypesWithout", R.string.AccRewindTypesWithout));
        REWIND_TYPES.add(LocaleController.getString("AccRewindTypesPercent", R.string.AccRewindTypesPercent));
        REWIND_TYPES.add(LocaleController.getString("AccRewindTypesSecond", R.string.AccRewindTypesSecond));
        REWIND_TYPES.add(LocaleController.getString("AccRewindTypesSelect", R.string.AccRewindTypesSelect));

        ADD_TYPES.add(LocaleController.getString("AccTypeOfAnnouncingTypeOfChatStart", R.string.AccTypeOfAnnouncingTypeOfChatStart));
        ADD_TYPES.add(LocaleController.getString("AccTypeOfAnnouncingTypeOfChatMiddle", R.string.AccTypeOfAnnouncingTypeOfChatMiddle));
        ADD_TYPES.add(LocaleController.getString("AccTypeOfAnnouncingTypeOfChatEnd", R.string.AccTypeOfAnnouncingTypeOfChatEnd));
        ADD_TYPES.add(LocaleController.getString("AccTypeOfAnnouncingTypeOfChatNo", R.string.AccTypeOfAnnouncingTypeOfChatNo));

        SEEKBAR_TIME_VALUES.add(LocaleController.getString("AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay", R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay));
        for (int a = 1; a <= 4; a++) {
            SEEKBAR_TIME_VALUES.add(LocaleController.formatString("AccTimeBeforeAnnouncingOfChangesOfSeekbarValue", R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, 50 * a));
        }
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("AccessibilitySettings", R.string.AccessibilitySettings));

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

        RecyclerListView listView = new RecyclerListView(context);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        ((DefaultItemAnimator) listView.getItemAnimator()).setDelayAnimations(false);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == typeOfRewindRow || position == typeOfRewindVideoRow || position == addTypeOfChatToDescriptionRow || position == timeBeforeAnnouncingOfSeekbarRow) {
                var values = new ArrayList<String>();
                if (position == typeOfRewindRow || position == typeOfRewindVideoRow) {
                    values.addAll(REWIND_TYPES);
                } else if (position == addTypeOfChatToDescriptionRow) {
                    values.addAll(ADD_TYPES);
                } else {
                    values.addAll(SEEKBAR_TIME_VALUES);
                }
                // Second and auto rewind not implemented yet,delete this values from ArrayList. Auto rewind mean,what message have <= 100 seconds length,we use percent rewind,else second rewind. When this two rewind will be implemented for audio and video,remove this part of code.
                if (position == typeOfRewindRow || position == typeOfRewindVideoRow) {
                    values.remove(2);
                    values.remove(2);
                }
                PopupHelper.show(values, position == typeOfRewindRow ?
                                LocaleController.getString("AccTypeOfRewindHeading", R.string.AccTypeOfRewindHeading) :
                                position == typeOfRewindVideoRow ?
                                        LocaleController.getString("AccTypeOfRewindVideoHeading", R.string.AccTypeOfRewindVideoHeading) :
                                        position == addTypeOfChatToDescriptionRow ?
                                                LocaleController.getString("AccTypeOfAddingHeading", R.string.AccTypeOfAddingHeading) :
                                                LocaleController.getString("AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarHeading", R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarHeading),
                        position == typeOfRewindRow ?
                                AccConfig.TYPE_OF_REWIND :
                                position == typeOfRewindVideoRow ?
                                        AccConfig.TYPE_OF_REWIND_VIDEO :
                                        position == addTypeOfChatToDescriptionRow ?
                                                AccConfig.ADD_TYPE_OF_CHAT_TO_DESCRIPTION :
                                                AccConfig.DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE / 50,
                        context, view, i -> {
                            if (position == typeOfRewindRow) {
                                AccConfig.setTypeOfRewind(i);
                            } else if (position == typeOfRewindVideoRow) {
                                AccConfig.setTypeOfRewindVideo(i);
                            } else if (position == addTypeOfChatToDescriptionRow) {
                                AccConfig.setAddTypeOfChatToDescription(i);
                            } else {
                                AccConfig.setDelayBetweenAnnouncingOfChangingOfSeekbarValue(i * 50);
                            }
                            listAdapter.notifyItemChanged(position);
                        });
            } else if (position == showNumbersOfItemsRow || position == showIndexOfItemRow || position == showValueChangesRow || position == showLinkNodesRow || position == hideLinksRow) {
                TextCheckCell cell = (TextCheckCell) view;
                if (position == showNumbersOfItemsRow) {
                    AccConfig.saveShowNumbersOfItems();
                    cell.setChecked(AccConfig.SHOW_NUMBERS_OF_ITEMS);
                } else if (position == showIndexOfItemRow) {
                    AccConfig.saveShowIndexOfItem();
                    cell.setChecked(AccConfig.SHOW_INDEX_OF_ITEM);
                } else if (position == showValueChangesRow) {
                    AccConfig.saveShowSeekbarValueChanges();
                    cell.setChecked(AccConfig.SHOW_SEEKBAR_VALUE_CHANGES);
                } else if (position == showLinkNodesRow) {
                    AccConfig.saveShowLinkNodes();
                    cell.setChecked(AccConfig.SHOW_LINK_NODES);
                } else {
                    AccConfig.saveHideLinks();
                    cell.setChecked(AccConfig.HIDE_LINKS);
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
                    if (position == differentHeading2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == typeOfRewindRow) {
                        textCell.setTextAndValue(LocaleController.getString("AccTypeOfRewind", R.string.AccTypeOfRewind), REWIND_TYPES.get(AccConfig.TYPE_OF_REWIND), true);
                    } else if (position == typeOfRewindVideoRow) {
                        textCell.setTextAndValue(LocaleController.getString("AccTypeOfRewindVideo", R.string.AccTypeOfRewindVideo), REWIND_TYPES.get(AccConfig.TYPE_OF_REWIND_VIDEO), false);
                    } else if (position == addTypeOfChatToDescriptionRow) {
                        textCell.setTextAndValue(LocaleController.getString("AccTypeOfAdding", R.string.AccTypeOfAdding), ADD_TYPES.get(AccConfig.ADD_TYPE_OF_CHAT_TO_DESCRIPTION), true);
                    } else if (position == timeBeforeAnnouncingOfSeekbarRow) {
                        textCell.setTextAndValue(LocaleController.getString("AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbar", R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbar), AccConfig.DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE > 0 ? LocaleController.formatString("AccTimeBeforeAnnouncingOfChangesOfSeekbarValue", R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, AccConfig.DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE) : LocaleController.getString("AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay", R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay), false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == showNumbersOfItemsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccNumberOfItems", R.string.AccNumberOfItems), AccConfig.SHOW_NUMBERS_OF_ITEMS, true);
                    } else if (position == showIndexOfItemRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccIndexOfItem", R.string.AccIndexOfItem), AccConfig.SHOW_INDEX_OF_ITEM, true);
                    } else if (position == showValueChangesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccShowValueChanges", R.string.AccShowValueChanges), AccConfig.SHOW_SEEKBAR_VALUE_CHANGES, true);
                    } else if (position == showLinkNodesRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccShowLinksForNodes", R.string.AccShowLinksForNodes), AccConfig.SHOW_LINK_NODES, true);
                    } else if (position == hideLinksRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccHideLinks", R.string.AccHideLinks), AccConfig.HIDE_LINKS, false);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == rewindHeadingRow) {
                        headerCell.setText(LocaleController.getString("AccRewindHeading", R.string.AccRewindHeading));
                    } else if (position == seekbarHeadingRow) {
                        headerCell.setText(LocaleController.getString("AccSeekbarHeading", R.string.AccSeekbarHeading));
                    } else if (position == differentHeadingRow) {
                        headerCell.setText(LocaleController.getString("AccDifferentHeading", R.string.AccDifferentHeading));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 2 || type == 3 || type == 6 || type == 5;
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
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == seekbarHeadingRow || position == rewindHeadingRow || position == differentHeadingRow) {
                return 4;
            } else if (position == typeOfRewindRow || position == typeOfRewindVideoRow || position == addTypeOfChatToDescriptionRow || position == timeBeforeAnnouncingOfSeekbarRow) {
                return 2;
            } else if (position == seekbarHeading2Row || position == rewindHeading2Row || position == differentHeading2Row) {
                return 1;
            } else {
                return 3;
            }
        }
    }
}
