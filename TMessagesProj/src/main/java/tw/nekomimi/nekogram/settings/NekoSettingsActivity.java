package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.ProfileActivity.SearchAdapter.SearchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import tw.nekomimi.nekogram.accessibility.AccessibilitySettingsActivity;
import tw.nekomimi.nekogram.helpers.CloudSettingsHelper;
import tw.nekomimi.nekogram.helpers.PasscodeHelper;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;

public class NekoSettingsActivity extends BaseNekoSettingsActivity implements FactorAnimator.Target {

    private static final int ANIMATOR_ID_SEARCH_PAGE_VISIBLE = 0;

    private final BoolAnimator animatorSearchPageVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_PAGE_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);

    private final List<ConfigHelper.News> newsList = new ArrayList<>();

    private final int generalRow = rowId++;
    private final int appearanceRow = rowId++;
    private final int chatRow = rowId++;
    private final int passcodeRow = rowId++;
    private final int experimentRow = rowId++;
    private final int accessibilityRow = rowId++;

    private final int channelRow = rowId++;
    private final int websiteRow = rowId++;
    private final int sourceCodeRow = rowId++;
    private final int translationRow = rowId++;
    private final int donateRow = rowId++;

    private final int sponsorRow = 100;

    private ActionBarMenuItem syncItem;
    private final ArrayList<SearchResult> searchArray = createSearchArray();
    private final ArrayList<CharSequence> resultNames = new ArrayList<>();
    private final ArrayList<SearchResult> searchResults = new ArrayList<>();
    private boolean searchWas;
    private Runnable searchRunnable;
    private String lastSearchString;

    private FrameLayout topView;

    @Override
    public View createView(Context context) {
        if (parentLayout != null && parentLayout.isRightLayout()) {
            actionBar.setBackButtonImage(R.drawable.ic_ab_close);
        }
        topView = new FrameLayout(context);

        var logoContainer = new FrameLayout(context);
        var logoView = new BackupImageView(context);

        logoView.setImageDrawable(AppCompatResources.getDrawable(context, R.mipmap.ic_launcher));
        logoContainer.addView(logoView, LayoutHelper.createFrame(90, 90, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 15, 0, 0));
        topView.addView(logoContainer, LayoutHelper.createFrame(120, 120, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 23 - 12, 0, 0));

        var titleView = new TextView(context);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setGravity(Gravity.CENTER);
        titleView.setSingleLine();
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setText(LocaleController.getString(R.string.AppNameNeko));
        titleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        topView.addView(titleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 138.333f - 12, 0, 0));

        var subtitleView = new TextView(context);
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setSingleLine();
        subtitleView.setEllipsize(TextUtils.TruncateAt.END);
        subtitleView.setText(String.format(Locale.US, "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        subtitleView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        topView.addView(subtitleView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, 168 - 12, 0, 0));

        var fragmentView = super.createView(context);

        var menu = actionBar.createMenu();
        createSearchItem(menu, new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            @Override
            public void onSearchCollapse() {
                animatorSearchPageVisible.setValue(false, true);
                updateActionBarVisible();
                listView.adapter.update(true);
            }

            @Override
            public void onSearchExpand() {
                animatorSearchPageVisible.setValue(true, true);
                updateActionBarVisible();
                search("");
                listView.adapter.update(true);
            }

            @Override
            public void onTextChanged(EditText editText) {
                search(editText.getText().toString());
            }
        });
        syncItem = menu.addItem(1, R.drawable.cloud_sync);
        syncItem.setContentDescription(LocaleController.getString(R.string.CloudConfig));
        syncItem.setOnClickListener(v -> CloudSettingsHelper.getInstance().showDialog(this));

        return fragmentView;
    }

    @Override
    protected boolean needActionBarPadding() {
        return false;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (isSearchFieldVisible()) {
            items.add(UItem.asSpace(ActionBar.getCurrentActionBarHeight()));
            fillSearchItems(items);
            return;
        }

        items.add(UItem.asCustomShadow(topView, 200 - 12));

        items.add(UItem.asButton(generalRow, R.drawable.msg_media, LocaleController.getString(R.string.General)).slug("general"));
        items.add(UItem.asButton(appearanceRow, R.drawable.msg_theme, LocaleController.getString(R.string.ChangeChannelNameColor2)).slug("appearance"));
        items.add(UItem.asButton(chatRow, R.drawable.msg_discussion, LocaleController.getString(R.string.Chat)).slug("chat"));
        if (!PasscodeHelper.isSettingsHidden()) {
            items.add(UItem.asButton(passcodeRow, R.drawable.msg_secret, LocaleController.getString(R.string.PasscodeNeko)).slug("passcode"));
        }
        items.add(UItem.asButton(experimentRow, R.drawable.msg_fave, LocaleController.getString(R.string.NotificationsOther)).slug("experiment"));
        AccessibilityManager am = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isTouchExplorationEnabled()) {
            items.add(UItem.asButton(accessibilityRow, LocaleController.getString(R.string.AccessibilitySettings)).slug("accessibility"));
        }
        items.add(UItem.asShadow(null));

        items.add(UItem.asButton(channelRow, R.drawable.msg_channel, LocaleController.getString(R.string.OfficialChannel), "@" + LocaleController.getString(R.string.OfficialChannelUsername)).slug("channel"));
        items.add(UItem.asButton(websiteRow, R.drawable.msg_language, LocaleController.getString(R.string.OfficialSite), "nekogram.app").slug("website"));
        items.add(UItem.asButton(sourceCodeRow, R.drawable.msg_link, LocaleController.getString(R.string.ViewSourceCode), "GitHub").slug("sourceCode"));
        items.add(UItem.asButtonSubtext(translationRow, R.drawable.msg_translate, LocaleController.getString(R.string.Translation), LocaleController.getString(R.string.TranslationAbout)).slug("translation"));
        items.add(UItem.asButtonSubtext(donateRow, R.drawable.msg_input_like, LocaleController.getString(R.string.Donate), LocaleController.getString(R.string.DonateAbout)).slug("donate"));
        items.add(UItem.asShadow(null));

        newsList.clear();
        newsList.addAll(ConfigHelper.getNewsForSettings());
        if (!newsList.isEmpty()) {
            var newsId = 0;
            for (var news : newsList) {
                items.add(TextDetailSettingsCellFactory.of(sponsorRow + newsId++, news.title, news.summary));
            }
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.instanceOf(SettingsSearchCell.Factory.class)) {
            if (item.object instanceof SearchResult r) {
                r.open(null);
            }
            return;
        }
        var id = item.id;
        if (id == chatRow) {
            presentFragment(new NekoChatSettingsActivity());
        } else if (id == generalRow) {
            presentFragment(new NekoGeneralSettingsActivity());
        } else if (id == appearanceRow) {
            presentFragment(new NekoAppearanceSettingsActivity());
        } else if (id == passcodeRow) {
            presentFragment(new NekoPasscodeSettingsActivity());
        } else if (id == experimentRow) {
            presentFragment(new NekoExperimentalSettingsActivity());
        } else if (id == accessibilityRow) {
            presentFragment(new AccessibilitySettingsActivity());
        } else if (id == channelRow) {
            getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1);
        } else if (id == donateRow) {
            presentFragment(new NekoDonateActivity());
        } else if (id == translationRow) {
            Browser.openUrl(getParentActivity(), "https://neko.crowdin.com/nekogram");
        } else if (id == websiteRow) {
            Browser.openUrl(getParentActivity(), "https://nekogram.app");
        } else if (id == sourceCodeRow) {
            Browser.openUrl(getParentActivity(), "https://github.com/Nekogram/Nekogram");
        } else if (id >= sponsorRow) {
            var news = newsList.get(id - sponsorRow);
            Browser.openUrl(getParentActivity(), news.url);
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id >= sponsorRow) {
            var news = newsList.get(id - sponsorRow);
            if (news.id != null) {
                ItemOptions.makeOptions(this, view)
                        .setScrimViewBackground(listView.getClipBackground(view))
                        .add(R.drawable.msg_cancel, LocaleController.getString(R.string.Hide), () -> {
                            ConfigHelper.removeNews(news.id);
                            listView.adapter.update(true);
                        })
                        .setMinWidth(190)
                        .show();
                return true;
            }
        }
        return super.onItemLongClick(item, view, position, x, y);
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
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        if (id == ANIMATOR_ID_SEARCH_PAGE_VISIBLE) {
            FragmentFloatingButton.setAnimatedVisibility(syncItem, 1f - factor);
        }
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return !animatorSearchPageVisible.getValue();
    }

    private static BaseNekoSettingsActivity createFragment(int icon) {
        if (icon == R.drawable.msg_media) {
            return new NekoGeneralSettingsActivity();
        } else if (icon == R.drawable.msg_theme) {
            return new NekoAppearanceSettingsActivity();
        } else if (icon == R.drawable.msg_discussion) {
            return new NekoChatSettingsActivity();
        } else if (icon == R.drawable.msg_fave) {
            return new NekoExperimentalSettingsActivity();
        }
        return new NekoSettingsActivity();
    }

    private ArrayList<SearchResult> createSearchArray() {
        var searchResultList = new ArrayList<SearchResult>();
        var icons = new int[]{
                R.drawable.msg_media,
                R.drawable.msg_theme,
                R.drawable.msg_discussion,
                R.drawable.msg_fave,
        };
        for (var i = 0; i < icons.length; i++) {
            var icon = icons[i];
            var fragment = createFragment(icon);
            var items = new ArrayList<UItem>();
            fragment.fillItems(items, null);
            var fragmentTitle = fragment.getActionBarTitle();
            String headerText = null;
            for (var item : items) {
                if (item.viewType == UniversalAdapter.VIEW_TYPE_HEADER) {
                    headerText = item.text.toString();
                    continue;
                } else if (item.viewType == UniversalAdapter.VIEW_TYPE_SHADOW) {
                    headerText = null;
                    continue;
                }
                if (TextUtils.isEmpty(item.slug)) continue;
                searchResultList.add(new SearchResult(i * 1000 + item.id, item.text.toString(), null, fragmentTitle, fragmentTitle.equals(headerText) ? null : headerText, icon, () -> {
                    var fragment1 = createFragment(icon);
                    presentFragment(fragment1);
                    AndroidUtilities.runOnUIThread(() -> fragment1.scrollToRow(item.slug, () -> {
                    }));
                }));
            }
            searchResultList.add(new SearchResult(10000 + i, fragmentTitle, icon, () -> presentFragment(fragment)));
        }
        searchResultList.add(new SearchResult(8000, LocaleController.getString(R.string.EmojiUseDefault), null, LocaleController.getString(R.string.Chat), LocaleController.getString(R.string.EmojiSets), R.drawable.msg_theme, () -> {
            var fragment = new NekoEmojiSettingsActivity();
            presentFragment(fragment);
            AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow("useSystemEmoji", () -> {
            }));
        }));

        searchResultList.add(new SearchResult(20000, LocaleController.getString(R.string.OfficialChannel), "@" + LocaleController.getString(R.string.OfficialChannelUsername), R.drawable.msg2_help, () -> getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1)));
        searchResultList.add(new SearchResult(20001, LocaleController.getString(R.string.OfficialSite), "nekogram.app", R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), "https://nekogram.app")));
        searchResultList.add(new SearchResult(20002, LocaleController.getString(R.string.ViewSourceCode), "GitHub", R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), "https://github.com/Nekogram/Nekogram")));
        searchResultList.add(new SearchResult(20003, LocaleController.getString(R.string.Translation), LocaleController.getString(R.string.TranslationAbout), R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), "https://neko.crowdin.com/nekogram")));
        searchResultList.add(new SearchResult(20004, LocaleController.getString(R.string.Donate), LocaleController.getString(R.string.DonateAbout), R.drawable.msg2_help, () -> presentFragment(new NekoDonateActivity())));

        return searchResultList;
    }

    private void fillSearchItems(ArrayList<UItem> items) {
        if (searchWas) {
            for (int i = 0; i < searchResults.size(); i++) {
                items.add(SettingsSearchCell.Factory.of(resultNames.get(i), searchResults.get(i)));
            }
            if (!searchResults.isEmpty()) items.add(UItem.asShadow(null));
        }
    }

    private void search(String text) {
        lastSearchString = text;
        if (searchRunnable != null) {
            Utilities.searchQueue.cancelRunnable(searchRunnable);
            searchRunnable = null;
        }
        if (TextUtils.isEmpty(text)) {
            searchWas = false;
            searchResults.clear();
            resultNames.clear();
            listView.adapter.update(true);
            return;
        }
        Utilities.searchQueue.postRunnable(searchRunnable = () -> {
            var results = new ArrayList<SearchResult>();
            var names = new ArrayList<CharSequence>();
            var lowerQuery = text.toLowerCase();
            for (var result : searchArray) {
                var title = result.searchTitle.toLowerCase();
                var index = title.indexOf(lowerQuery);
                var matchLen = lowerQuery.length();
                if (index < 0) continue;
                var ssb = new SpannableStringBuilder(result.searchTitle);
                ssb.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, Math.min(index + matchLen, ssb.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                results.add(result);
                names.add(ssb);
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (!text.equals(lastSearchString)) {
                    return;
                }
                searchWas = true;
                searchResults.clear();
                resultNames.clear();
                searchResults.addAll(results);
                resultNames.addAll(names);
                listView.adapter.update(true);
            });
        }, 300);
    }
}
