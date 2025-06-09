package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.LaunchActivity;

import java.util.List;

import tw.nekomimi.nekogram.accessibility.AccessibilitySettingsActivity;
import tw.nekomimi.nekogram.helpers.CloudSettingsHelper;
import tw.nekomimi.nekogram.helpers.PasscodeHelper;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class NekoSettingsActivity extends BaseNekoSettingsActivity {

    private final List<ConfigHelper.News> news = ConfigHelper.getNews();
    private boolean checkingUpdate = false;

    private int categoriesRow;
    private int generalRow;
    private int appearanceRow;
    private int chatRow;
    private int passcodeRow;
    private int experimentRow;
    private int accessibilityRow;
    private int categories2Row;

    private int aboutRow;
    private int channelRow;
    private int websiteRow;
    private int sourceCodeRow;
    private int translationRow;
    private int donateRow;
    private int checkUpdateRow;
    private int about2Row;

    private int sponsorRow;
    private int sponsor2Row;

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        actionBar.createMenu()
                .addItem(0, R.drawable.cloud_sync)
                .setOnClickListener(v -> CloudSettingsHelper.getInstance().showDialog(NekoSettingsActivity.this));

        return fragmentView;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == chatRow) {
            presentFragment(new NekoChatSettingsActivity());
        } else if (position == generalRow) {
            presentFragment(new NekoGeneralSettingsActivity());
        } else if (position == appearanceRow) {
            presentFragment(new NekoAppearanceSettings());
        } else if (position == passcodeRow) {
            presentFragment(new NekoPasscodeSettingsActivity());
        } else if (position == experimentRow) {
            presentFragment(new NekoExperimentalSettingsActivity());
        } else if (position == accessibilityRow) {
            presentFragment(new AccessibilitySettingsActivity());
        } else if (position == channelRow) {
            getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1);
        } else if (position == donateRow) {
            presentFragment(new NekoDonateActivity());
        } else if (position == translationRow) {
            Browser.openUrl(getParentActivity(), "https://neko.crowdin.com/nekogram");
        } else if (position == websiteRow) {
            Browser.openUrl(getParentActivity(), "https://nekogram.app");
        } else if (position == sourceCodeRow) {
            Browser.openUrl(getParentActivity(), "https://github.com/Nekogram/Nekogram");
        } else if (position == checkUpdateRow) {
            ((LaunchActivity) getParentActivity()).checkAppUpdate(true, new Browser.Progress() {
                @Override
                public void end() {
                    checkingUpdate = false;
                    listAdapter.notifyItemChanged(checkUpdateRow);
                }
            });
            checkingUpdate = true;
            listAdapter.notifyItemChanged(checkUpdateRow);
        } else if (position >= sponsorRow && position < sponsor2Row) {
            ConfigHelper.News item = news.get(position - sponsorRow);
            Browser.openUrl(getParentActivity(), item.url);
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.NekoSettings);
    }

    @Override
    protected String getKey() {
        return "";
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        categoriesRow = addRow("categories");
        generalRow = addRow("general");
        appearanceRow = addRow("appearance");
        chatRow = addRow("chat");
        if (!PasscodeHelper.isSettingsHidden()) {
            passcodeRow = addRow("passcode");
        } else {
            passcodeRow = -1;
        }
        experimentRow = addRow("experiment");
        AccessibilityManager am = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isTouchExplorationEnabled()) {
            accessibilityRow = addRow("accessibility");
        } else {
            accessibilityRow = -1;
        }
        categories2Row = addRow();

        aboutRow = addRow("about");
        channelRow = addRow("channel");
        websiteRow = addRow("website");
        sourceCodeRow = addRow("sourceCode");
        translationRow = addRow("translation");
        donateRow = addRow("donate");
        checkUpdateRow = addRow("checkUpdate");
        about2Row = addRow();

        if (!news.isEmpty()) {
            sponsorRow = addRow();
            rowCount += news.size() - 1;
            sponsor2Row = addRow();
        } else {
            sponsorRow = -1;
            sponsor2Row = -1;
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
                    if (position == channelRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.OfficialChannel), "@" + LocaleController.getString(R.string.OfficialChannelUsername), divider);
                    } else if (position == websiteRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.OfficialSite), "nekogram.app", divider);
                    } else if (position == sourceCodeRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.ViewSourceCode), "GitHub", divider);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == categoriesRow) {
                        headerCell.setText(LocaleController.getString(R.string.Categories));
                    } else if (position == aboutRow) {
                        headerCell.setText(LocaleController.getString(R.string.About));
                    }
                    break;
                }
                case TYPE_DETAIL_SETTINGS: {
                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
                    textCell.setMultilineDetail(true);
                    if (position == translationRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.Translation), LocaleController.getString(R.string.TranslationAbout), divider);
                    } else if (position == donateRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.Donate), LocaleController.getString(R.string.DonateAbout), divider);
                    } else if (position == checkUpdateRow) {
                        textCell.setTextAndValue(LocaleController.getString(R.string.CheckUpdate),
                                checkingUpdate ? LocaleController.getString(R.string.CheckingUpdate) :
                                        UpdateHelper.formatDateUpdate(SharedConfig.lastUpdateCheckTime), divider);
                    } else if (position >= sponsorRow && position < sponsor2Row) {
                        ConfigHelper.News item = news.get(position - sponsorRow);
                        textCell.setTextAndValue(item.title, item.summary, divider);
                    }
                    break;
                }
                case TYPE_TEXT: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == chatRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.Chat), R.drawable.msg_discussion, divider);
                    } else if (position == generalRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.General), R.drawable.msg_media, divider);
                    } else if (position == appearanceRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.ChangeChannelNameColor2), R.drawable.msg_theme, divider);
                    } else if (position == passcodeRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.PasscodeNeko), R.drawable.msg_permissions, divider);
                    } else if (position == experimentRow) {
                        textCell.setTextAndIcon(LocaleController.getString(R.string.NotificationsOther), R.drawable.msg_fave, divider);
                    } else if (position == accessibilityRow) {
                        textCell.setText(LocaleController.getString(R.string.AccessibilitySettings), divider);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position >= sponsorRow && position < sponsor2Row) {
                return TYPE_DETAIL_SETTINGS;
            } else if (position == categories2Row || position == about2Row || position == sponsor2Row) {
                return TYPE_SHADOW;
            } else if (position >= channelRow && position < translationRow) {
                return TYPE_SETTINGS;
            } else if (position == categoriesRow || position == aboutRow) {
                return TYPE_HEADER;
            } else if (position >= translationRow && position < about2Row) {
                return TYPE_DETAIL_SETTINGS;
            } else if (position > categoriesRow && position < categories2Row) {
                return TYPE_TEXT;
            }
            return TYPE_SETTINGS;
        }
    }
}
