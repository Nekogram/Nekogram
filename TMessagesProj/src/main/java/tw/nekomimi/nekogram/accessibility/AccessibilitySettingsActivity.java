package tw.nekomimi.nekogram.accessibility;

import android.view.View;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

public class AccessibilitySettingsActivity extends BaseNekoSettingsActivity {
    private static final ArrayList<String> SEEKBAR_TIME_VALUES = new ArrayList<>();

    private final int showNumbersOfItemsRow = rowId++;
    private final int showIndexOfItemRow = rowId++;
    private final int showValueChangesRow = rowId++;
    private final int timeBeforeAnnouncingOfSeekbarRow = rowId++;

    private final int announceDialogTypeRow = rowId++;
    private final int announceDialogMuted = rowId++;

    private final int announceFileProgressRow = rowId++;
    private final int showTranslatedLanguageRow = rowId++;

    static {
        SEEKBAR_TIME_VALUES.add(LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay));
        for (int a = 1; a <= 4; a++) {
            SEEKBAR_TIME_VALUES.add(LocaleController.formatString(R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, 50 * a));
        }
    }

    private CharSequence getTimeBeforeAnnouncingOfSeekbar() {
        return AccConfig.delayBetweenAnnouncingOfChangingOfSeekbarValue > 0 ?
                LocaleController.formatString(R.string.AccTimeBeforeAnnouncingOfChangesOfSeekbarValue, AccConfig.delayBetweenAnnouncingOfChangingOfSeekbarValue) :
                LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbarWithoutDelay);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(LocaleController.getString(R.string.AccSeekbarHeading)));
        items.add(UItem.asCheck(showNumbersOfItemsRow, LocaleController.getString(R.string.AccNumberOfItems)).setChecked(AccConfig.showNumbersOfItems));
        items.add(UItem.asCheck(showIndexOfItemRow, LocaleController.getString(R.string.AccIndexOfItem)).setChecked(AccConfig.showIndexOfItem));
        items.add(UItem.asCheck(showValueChangesRow, LocaleController.getString(R.string.AccShowValueChanges)).setChecked(AccConfig.showSeekbarValueChanges));
        items.add(TextSettingsCellFactory.of(timeBeforeAnnouncingOfSeekbarRow, LocaleController.getString(R.string.AccTimeBeforeAnnouncingOfChangingOfValueOfSeekbar), getTimeBeforeAnnouncingOfSeekbar()));
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.Chats)));
        items.add(UItem.asCheck(announceDialogTypeRow, LocaleController.getString(R.string.AccAnnounceDialogType)).setChecked(AccConfig.announceDialogType));
        items.add(UItem.asCheck(announceDialogMuted, LocaleController.getString(R.string.AccAnnounceDialogMuted)).setChecked(AccConfig.announceDialogType));
        items.add(UItem.asShadow(null));

        items.add(UItem.asCheck(announceFileProgressRow, LocaleController.getString(R.string.AccAnnounceFileProgress)).setChecked(AccConfig.announceFileProgress));
        items.add(UItem.asCheck(showTranslatedLanguageRow, LocaleController.getString(R.string.AccShowTranslatedLanguage)).setChecked(AccConfig.showTranslatedLanguage));
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id == timeBeforeAnnouncingOfSeekbarRow) {
            showPopup(SEEKBAR_TIME_VALUES,
                    AccConfig.delayBetweenAnnouncingOfChangingOfSeekbarValue / 50,
                    item, view, i -> {
                        AccConfig.setDelayBetweenAnnouncingOfChangingOfSeekbarValue(i * 50);
                        listView.adapter.notifyItemChanged(position);
                    });
        } else if (view instanceof TextCheckCell cell) {
            if (id == showNumbersOfItemsRow) {
                AccConfig.saveShowNumbersOfItems();
                cell.setChecked(AccConfig.showNumbersOfItems);
            } else if (id == showIndexOfItemRow) {
                AccConfig.saveShowIndexOfItem();
                cell.setChecked(AccConfig.showIndexOfItem);
            } else if (id == showValueChangesRow) {
                AccConfig.saveShowSeekbarValueChanges();
                cell.setChecked(AccConfig.showSeekbarValueChanges);
            } else if (id == announceFileProgressRow) {
                AccConfig.toggleAnnounceFileProgress();
                cell.setChecked(AccConfig.announceFileProgress);
            } else if (id == showTranslatedLanguageRow) {
                AccConfig.toggleShowTranslatedLanguage();
                cell.setChecked(AccConfig.showTranslatedLanguage);
            } else if (id == announceDialogTypeRow) {
                AccConfig.toggleAnnounceDialogType();
                cell.setChecked(AccConfig.announceDialogType);
            } else if (id == announceDialogMuted) {
                AccConfig.toggleAnnounceDialogMuted();
                cell.setChecked(AccConfig.announceDialogMuted);
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.AccessibilitySettings);
    }
}
