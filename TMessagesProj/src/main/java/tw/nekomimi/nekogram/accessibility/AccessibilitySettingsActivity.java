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
    private static final ArrayList<String> SEEKBAR_TIME_VALUES = new ArrayList<>();

    private ListAdapter listAdapter;

    private int rowCount = 0;

    private final int seekbarHeadingRow = rowCount++;
    private final int showNumbersOfItemsRow = rowCount++;
    private final int showIndexOfItemRow = rowCount++;
    private final int showValueChangesRow = rowCount++;
    private final int timeBeforeAnnouncingOfSeekbarRow = rowCount++;
    private final int seekbarHeading2Row = rowCount++;

    static {
        SEEKBAR_TIME_VALUES.add(LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay));
        for (int a = 1; a <= 4; a++) {
            SEEKBAR_TIME_VALUES.add(LocaleController.formatString(R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, 50 * a));
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
            if (position == timeBeforeAnnouncingOfSeekbarRow) {
                PopupHelper.show(SEEKBAR_TIME_VALUES, LocaleController.getString("AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarHeading", R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarHeading),

                        AccConfig.DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE / 50,
                        context, view, i -> {

                            AccConfig.setDelayBetweenAnnouncingOfChangingOfSeekbarValue(i * 50);
                            listAdapter.notifyItemChanged(position);
                        }, null);
            } else if (position == showNumbersOfItemsRow || position == showIndexOfItemRow || position == showValueChangesRow) {
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
                    if (position == seekbarHeading2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == timeBeforeAnnouncingOfSeekbarRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbar), AccConfig.DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE > 0 ? LocaleController.formatString("AccTimeBeforeAnnouncingOfChangesOfSeekbarValue", R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, AccConfig.DELAY_BETWEEN_ANNOUNCING_OF_CHANGING_OF_SEEKBAR_VALUE) : LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay), false);
                    }
                    break;
                }
                case 3: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == showNumbersOfItemsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccNumberOfItems), AccConfig.SHOW_NUMBERS_OF_ITEMS, true);
                    } else if (position == showIndexOfItemRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccIndexOfItem), AccConfig.SHOW_INDEX_OF_ITEM, true);
                    } else if (position == showValueChangesRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccShowValueChanges), AccConfig.SHOW_SEEKBAR_VALUE_CHANGES, true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == seekbarHeadingRow) {
                        headerCell.setText(LocaleController.getString(R.string.AccSeekbarHeading));
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
                    view.setBackground(Theme.getThemedDrawableByKey(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == seekbarHeadingRow) {
                return 4;
            } else if (position == timeBeforeAnnouncingOfSeekbarRow) {
                return 2;
            } else if (position == seekbarHeading2Row) {
                return 1;
            } else {
                return 3;
            }
        }
    }
}
