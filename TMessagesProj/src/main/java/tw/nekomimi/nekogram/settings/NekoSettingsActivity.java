package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.accessibility.AccessibilitySettingsActivity;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class NekoSettingsActivity extends BaseNekoSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private final ArrayList<ConfigHelper.NewsItem> news = ConfigHelper.getNews();

    private boolean sensitiveCanChange = false;
    private boolean sensitiveEnabled = false;
    private boolean checkingUpdate = false;

    private int categoriesRow;
    private int generalRow;
    private int appearanceRow;
    private int chatRow;
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

    private void checkSensitive() {
        TLRPC.TL_account_getContentSettings req = new TLRPC.TL_account_getContentSettings();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                TLRPC.TL_account_contentSettings settings = (TLRPC.TL_account_contentSettings) response;
                sensitiveEnabled = settings.sensitive_enabled;
                sensitiveCanChange = settings.sensitive_can_change;
            } else {
                AndroidUtilities.runOnUIThread(() -> AlertsCreator.processError(currentAccount, error, this, req));
            }
        }));
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.appUpdateAvailable);

        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);
        actionBar.setTitle(LocaleController.getString("NekoSettings", R.string.NekoSettings));

        listAdapter = new ListAdapter(context);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == chatRow) {
                presentFragment(new NekoChatSettingsActivity());
            } else if (position == generalRow) {
                presentFragment(new NekoGeneralSettingsActivity());
            } else if (position == appearanceRow) {
                presentFragment(new NekoAppearanceSettings());
            } else if (position == experimentRow) {
                presentFragment(new NekoExperimentalSettingsActivity(sensitiveCanChange, sensitiveEnabled));
            } else if (position == accessibilityRow) {
                presentFragment(new AccessibilitySettingsActivity());
            } else if (position == channelRow) {
                getMessagesController().openByUserName(LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername), this, 1);
            } else if (position == donateRow) {
                if (NekoConfig.installedFromPlay) {
                    presentFragment(new NekoDonateActivity());
                } else {
                    Browser.openUrl(getParentActivity(), "https://www.buymeacoffee.com/nekogram");
                }
            } else if (position == translationRow) {
                Browser.openUrl(getParentActivity(), "https://neko.crowdin.com/nekogram");
            } else if (position == websiteRow) {
                Browser.openUrl(getParentActivity(), "https://nekogram.app");
            } else if (position == sourceCodeRow) {
                if (LocaleController.isRTL && x <= AndroidUtilities.dp(84) || !LocaleController.isRTL && x >= view.getMeasuredWidth() - AndroidUtilities.dp(84)) {
                    Browser.openUrl(getParentActivity(), String.format("https://gitlab.com/Nekogram/Nekogram/-/commit/%s", BuildConfig.COMMIT_ID));
                } else {
                    Browser.openUrl(getParentActivity(), "https://gitlab.com/Nekogram/Nekogram");
                }
            } else if (position == checkUpdateRow) {
                ((LaunchActivity) getParentActivity()).checkAppUpdate(true);
                checkingUpdate = true;
                listAdapter.notifyItemChanged(checkUpdateRow);
            } else if (position >= sponsorRow && position < sponsor2Row) {
                ConfigHelper.NewsItem item = news.get(position - sponsorRow);
                Browser.openUrl(getParentActivity(), item.url);
            }
        });

        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkSensitive();
    }

    @Override
    protected void updateRows() {
        rowCount = 0;
        categoriesRow = rowCount++;
        generalRow = rowCount++;
        appearanceRow = rowCount++;
        chatRow = rowCount++;
        experimentRow = rowCount++;
        AccessibilityManager am = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isTouchExplorationEnabled()) {
            accessibilityRow = rowCount++;
        } else {
            accessibilityRow = -1;
        }
        categories2Row = rowCount++;

        aboutRow = rowCount++;
        channelRow = rowCount++;
        websiteRow = rowCount++;
        sourceCodeRow = rowCount++;
        translationRow = rowCount++;
        donateRow = rowCount++;
        checkUpdateRow = rowCount++;
        about2Row = rowCount++;

        if (news.size() != 0) {
            sponsorRow = rowCount++;
            rowCount += news.size() - 1;
            sponsor2Row = rowCount++;
        } else {
            sponsorRow = -1;
            sponsor2Row = -1;
        }
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.appUpdateAvailable) {
            checkingUpdate = false;
            AndroidUtilities.runOnUIThread(() -> listAdapter.notifyItemChanged(checkUpdateRow));
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();

        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.appUpdateAvailable);
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    if ((position == about2Row && sponsor2Row == -1) || position == sponsor2Row) {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    } else {
                        holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                    }
                    break;
                }
                case 2: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == channelRow) {
                        textCell.setTextAndValue(LocaleController.getString("OfficialChannel", R.string.OfficialChannel), "@" + LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername), true);
                    } else if (position == websiteRow) {
                        textCell.setTextAndValue(LocaleController.getString("OfficialSite", R.string.OfficialSite), "nekogram.app", true);
                    } else if (position == sourceCodeRow) {
                        textCell.setTextAndValue(LocaleController.getString("ViewSourceCode", R.string.ViewSourceCode), BuildConfig.COMMIT_ID.substring(0, 7), true);
                    }
                    break;
                }
                case 4: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == categoriesRow) {
                        headerCell.setText(LocaleController.getString("Categories", R.string.Categories));
                    } else if (position == aboutRow) {
                        headerCell.setText(LocaleController.getString("About", R.string.About));
                    }
                    break;
                }
                case 6: {
                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
                    textCell.setMultilineDetail(true);
                    if (position == translationRow) {
                        textCell.setTextAndValue(LocaleController.getString("Translation", R.string.Translation), LocaleController.getString("TranslationAbout", R.string.TranslationAbout), true);
                    } else if (position == donateRow) {
                        textCell.setTextAndValue(LocaleController.getString("Donate", R.string.Donate), LocaleController.getString("DonateAbout", R.string.DonateAbout), position + 1 != about2Row);
                    } else if (position == checkUpdateRow) {
                        textCell.setTextAndValue(LocaleController.getString("CheckUpdate", R.string.CheckUpdate),
                                checkingUpdate ? LocaleController.getString("CheckingUpdate", R.string.CheckingUpdate) :
                                        UpdateHelper.formatDateUpdate(SharedConfig.lastUpdateCheckTime), position + 1 != about2Row);
                    } else if (position >= sponsorRow && position < sponsor2Row) {
                        ConfigHelper.NewsItem item = news.get(position - sponsorRow);
                        textCell.setTextAndValue(item.title, item.summary, position + 1 != sponsor2Row);
                    }
                    break;
                }
                case 8: {
                    TextCell textCell = (TextCell) holder.itemView;
                    if (position == chatRow) {
                        textCell.setTextAndIcon(LocaleController.getString("Chat", R.string.Chat), R.drawable.menu_chats, true);
                    } else if (position == generalRow) {
                        textCell.setTextAndIcon(LocaleController.getString("General", R.string.General), R.drawable.msg_media, true);
                    } else if (position == appearanceRow) {
                        textCell.setTextAndIcon(LocaleController.getString("Appearance", R.string.Appearance), R.drawable.msg_theme, true);
                    } else if (position == experimentRow) {
                        textCell.setTextAndIcon(LocaleController.getString("Experiment", R.string.Experiment), R.drawable.msg_fave, accessibilityRow != -1);
                    } else if (position == accessibilityRow) {
                        textCell.setText(LocaleController.getString("AccessibilitySettings", R.string.AccessibilitySettings), false);
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == categories2Row || position == about2Row || position == sponsor2Row) {
                return 1;
            } else if (position >= channelRow && position < translationRow) {
                return 2;
            } else if (position == categoriesRow || position == aboutRow) {
                return 4;
            } else if ((position >= translationRow && position < about2Row) || (position >= sponsorRow && position < sponsor2Row)) {
                return 6;
            } else if (position > categoriesRow && position < categories2Row) {
                return 8;
            }
            return 2;
        }
    }
}
