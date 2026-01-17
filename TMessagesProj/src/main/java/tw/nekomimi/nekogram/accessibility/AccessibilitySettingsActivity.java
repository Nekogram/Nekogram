package tw.nekomimi.nekogram.accessibility;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;

import java.util.ArrayList;

import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class AccessibilitySettingsActivity extends BaseNekoSettingsActivity {
    private static final ArrayList<String> SEEKBAR_TIME_VALUES = new ArrayList<>();

    private int seekbarHeadingRow;
    private int showNumbersOfItemsRow;
    private int showIndexOfItemRow;
    private int showValueChangesRow;
    private int timeBeforeAnnouncingOfSeekbarRow;
    private int seekbarHeading2Row;

    private int announceFileProgressRow;
    private int showTranslatedLanguageRow;
    private int endRow;

    static {
        SEEKBAR_TIME_VALUES.add(LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay));
        for (int a = 1; a <= 4; a++) {
            SEEKBAR_TIME_VALUES.add(LocaleController.formatString(R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, 50 * a));
        }
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        seekbarHeadingRow = addRow();
        showNumbersOfItemsRow = addRow("showNumbersOfItems");
        showIndexOfItemRow = addRow("showIndexOfItem");
        showValueChangesRow = addRow("showValueChanges");
        timeBeforeAnnouncingOfSeekbarRow = addRow("timeBeforeAnnouncingOfSeekbar");
        seekbarHeading2Row = addRow();

        announceFileProgressRow = addRow("announceFileProgress");
        showTranslatedLanguageRow = addRow("showTranslatedLanguage");
        endRow = addRow();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == timeBeforeAnnouncingOfSeekbarRow) {
            PopupHelper.show(SEEKBAR_TIME_VALUES, LocaleController.getString("AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarHeading", R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarHeading),
                    AccConfig.delayBetweenAnnouncingOfChangingOfSeekbarValue / 50,
                    getParentActivity(), view, i -> {
                        AccConfig.setDelayBetweenAnnouncingOfChangingOfSeekbarValue(i * 50);
                        listAdapter.notifyItemChanged(position);
                    }, resourcesProvider);
        } else if (position == showNumbersOfItemsRow ||
                position == showIndexOfItemRow ||
                position == showValueChangesRow ||
                position == announceFileProgressRow ||
                position == showTranslatedLanguageRow
        ) {
            TextCheckCell cell = (TextCheckCell) view;
            if (position == showNumbersOfItemsRow) {
                AccConfig.saveShowNumbersOfItems();
                cell.setChecked(AccConfig.showNumbersOfItems);
            } else if (position == showIndexOfItemRow) {
                AccConfig.saveShowIndexOfItem();
                cell.setChecked(AccConfig.showIndexOfItem);
            } else if (position == showValueChangesRow) {
                AccConfig.saveShowSeekbarValueChanges();
                cell.setChecked(AccConfig.showSeekbarValueChanges);
            } else if (position == announceFileProgressRow) {
                AccConfig.toggleAnnounceFileProgress();
                cell.setChecked(AccConfig.announceFileProgress);
            } else if (position == showTranslatedLanguageRow) {
                AccConfig.toggleShowTranslatedLanguage();
                cell.setChecked(AccConfig.showTranslatedLanguage);
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.AccessibilitySettings);
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
                    if (position == timeBeforeAnnouncingOfSeekbarRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbar), AccConfig.delayBetweenAnnouncingOfChangingOfSeekbarValue > 0 ? LocaleController.formatString(R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, AccConfig.delayBetweenAnnouncingOfChangingOfSeekbarValue) : LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay), divider);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    if (position == showNumbersOfItemsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccNumberOfItems), AccConfig.showNumbersOfItems, divider);
                    } else if (position == showIndexOfItemRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccIndexOfItem), AccConfig.showIndexOfItem, divider);
                    } else if (position == showValueChangesRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccShowValueChanges), AccConfig.showSeekbarValueChanges, divider);
                    } else if (position == announceFileProgressRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccAnnounceFileProgress), AccConfig.announceFileProgress, divider);
                    } else if (position == showTranslatedLanguageRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.AccShowTranslatedLanguage), AccConfig.showTranslatedLanguage, divider);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == seekbarHeadingRow) {
                        headerCell.setText(LocaleController.getString(R.string.AccSeekbarHeading));
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == seekbarHeading2Row || position == endRow) {
                return TYPE_SHADOW;
            } else if (position == seekbarHeadingRow) {
                return TYPE_HEADER;
            } else if (position == timeBeforeAnnouncingOfSeekbarRow) {
                return TYPE_SETTINGS;
            } else {
                return TYPE_CHECK;
            }
        }
    }
}
