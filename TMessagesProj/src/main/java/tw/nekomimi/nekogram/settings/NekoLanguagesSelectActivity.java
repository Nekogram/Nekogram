package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.translator.Translator;

public class NekoLanguagesSelectActivity extends BaseNekoSettingsActivity {

    public static final int TYPE_RESTRICTED = 0;
    public static final int TYPE_TARGET = 1;

    private static final List<String> RESTRICTED_LIST = Arrays.asList(
            "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca", "ceb",
            "co", "cs", "cy", "da", "de", "el", "en", "eo", "es", "et",
            "eu", "fa", "fi", "fil", "fr", "fy", "ga", "gd", "gl", "gu",
            "ha", "haw", "he", "hi", "hmn", "hr", "ht", "hu", "hy", "id",
            "ig", "is", "it", "ja", "jv", "ka", "kk", "km", "kn", "ko",
            "ku", "ky", "la", "lb", "lo", "lt", "lv", "mg", "mi", "mk",
            "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "no", "ny",
            "pa", "pl", "ps", "pt", "ro", "ru", "sd", "si", "sk", "sl",
            "sm", "sn", "so", "sq", "sr", "st", "su", "sv", "sw", "ta",
            "te", "tg", "th", "tr", "uk", "ur", "uz", "vi", "xh", "yi",
            "yo", "zh", "zu");

    private final int currentType;
    private final boolean whiteActionBar;

    private ListAdapter searchListViewAdapter;
    private EmptyTextProgressView emptyView;

    private ArrayList<LocaleInfo> searchResult;
    private ArrayList<LocaleInfo> allLanguages;
    private ArrayList<LocaleInfo> sortedLanguages;

    private ArrayList<String> restrictedLanguages;

    public NekoLanguagesSelectActivity(int type, boolean whiteActionBar) {
        this.currentType = type;
        this.whiteActionBar = whiteActionBar;
    }

    @Override
    public boolean onFragmentCreate() {
        fillLanguages();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        FrameLayout fragmentView = (FrameLayout) super.createView(context);

        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem item = menu.addItem(0, R.drawable.ic_ab_search).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            @Override
            public void onSearchCollapse() {
                search(null);
                if (listView != null) {
                    emptyView.setVisibility(View.GONE);
                    listView.setAdapter(listAdapter);
                }
            }

            @Override
            public void onTextChanged(EditText editText) {
                String text = editText.getText().toString();
                search(text);
                if (text.length() != 0) {
                    if (listView != null) {
                        listView.setAdapter(searchListViewAdapter);
                    }
                } else {
                    if (listView != null) {
                        emptyView.setVisibility(View.GONE);
                        listView.setAdapter(listAdapter);
                    }
                }
            }
        });
        item.setSearchFieldHint(LocaleController.getString(R.string.Search));

        if (whiteActionBar) {
            actionBar.setSearchTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText), true);
            actionBar.setSearchTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText), false);
            actionBar.setSearchCursorColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
        }

        searchListViewAdapter = new ListAdapter(context, true);

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString(R.string.NoResult));
        emptyView.showTextView();
        emptyView.setShowAtCenter(true);
        fragmentView.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setEmptyView(emptyView);

        listView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }
        });

        return fragmentView;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (view instanceof ShadowSectionCell) {
            return;
        }
        if (view instanceof TextInfoPrivacyCell) {
            BulletinFactory.of(this).createErrorBulletin(LocaleController.getString(R.string.Nya)).show();
            return;
        }
        boolean search = listView.getAdapter() == searchListViewAdapter;
        LocaleInfo localeInfo;
        if (search) {
            localeInfo = searchResult.get(position);
        } else {
            localeInfo = sortedLanguages.get(position);
        }
        if (localeInfo != null) {
            if (currentType == TYPE_RESTRICTED) {
                TextCheckbox2Cell cell = (TextCheckbox2Cell) view;
                boolean remove = restrictedLanguages.contains(localeInfo.langCode);
                if (remove) {
                    restrictedLanguages.removeIf(s -> s != null && s.equals(localeInfo.langCode));
                } else {
                    restrictedLanguages.add(localeInfo.langCode);
                }
                Translator.saveRestrictedLanguages(restrictedLanguages);
                cell.setChecked(!remove);
                getMessagesController().getTranslateController().checkRestrictedLanguagesUpdate();
            } else {
                NekoConfig.setTranslationTarget(localeInfo.langCode);
                finishFragment();
            }
        }
    }

    public static boolean toggleLanguage(String language, boolean doNotTranslate) {
        if (language == null) {
            return false;
        }
        var restrictedLanguages = Translator.getRestrictedLanguages();
        if (!doNotTranslate) {
            restrictedLanguages.removeIf(s -> s != null && s.equals(language));
        } else {
            restrictedLanguages.add(language);
        }
        Translator.saveRestrictedLanguages(restrictedLanguages);
        TranslateController.invalidateSuggestedLanguageCodes();
        return true;
    }


    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context, false);
    }

    @Override
    protected String getActionBarTitle() {
        return currentType == TYPE_RESTRICTED ? LocaleController.getString(R.string.DoNotTranslate) : LocaleController.getString(R.string.TranslationTarget);
    }

    @Override
    protected boolean hasWhiteActionBar() {
        return whiteActionBar;
    }

    private String getCurrentTargetLanguage() {
        var language = Translator.getCurrentTargetLanguage();
        if (currentType == TYPE_RESTRICTED) {
            language = Translator.stripLanguageCode(language);
        }
        return language;
    }

    private void fillLanguages() {
        if (currentType == TYPE_RESTRICTED) {
            restrictedLanguages = Translator.getRestrictedLanguages();
        }
        allLanguages = new ArrayList<>();
        Locale localeEn = Locale.forLanguageTag("en");
        for (String languageCode : currentType == TYPE_RESTRICTED ? RESTRICTED_LIST : Translator.getCurrentTranslator().getTargetLanguages()) {
            var localeInfo = new LocaleInfo(languageCode);
            Locale locale = Locale.forLanguageTag(languageCode);
            if (!TextUtils.isEmpty(locale.getScript())) {
                localeInfo.name = HtmlCompat.fromHtml(locale.getDisplayScript(locale), HtmlCompat.FROM_HTML_MODE_LEGACY);
                localeInfo.nameEnglish = HtmlCompat.fromHtml(locale.getDisplayScript(localeEn), HtmlCompat.FROM_HTML_MODE_LEGACY);
                localeInfo.nameLocalized = HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
            } else {
                localeInfo.name = locale.getDisplayName(locale);
                localeInfo.nameEnglish = locale.getDisplayName(localeEn);
                localeInfo.nameLocalized = locale.getDisplayName();
            }
            allLanguages.add(localeInfo);
        }
        sortedLanguages = new ArrayList<>(allLanguages);
        if (currentType == TYPE_TARGET) {
            sortedLanguages.add(0, new LocaleInfo("app"));
        }
        sortedLanguages.add(0, new LocaleInfo("shadow"));
        Collections.sort(sortedLanguages, (o1, o2) -> {
            if (currentType == TYPE_TARGET) {
                if (o1.langCode.equals("app")) {
                    return -1;
                } else if (o2.langCode.equals("app")) {
                    return 1;
                } else if (NekoConfig.translationTarget.equals(o1.langCode)) {
                    return -1;
                } else if (NekoConfig.translationTarget.equals(o2.langCode)) {
                    return 1;
                }
            } else if (currentType == TYPE_RESTRICTED) {
                if (o1.langCode.equals(getCurrentTargetLanguage())) {
                    return -1;
                } else if (o2.langCode.equals(getCurrentTargetLanguage())) {
                    return 1;
                } else if (restrictedLanguages.contains(o1.langCode) && !restrictedLanguages.contains(o2.langCode)) {
                    return -1;
                } else if (!restrictedLanguages.contains(o1.langCode) && restrictedLanguages.contains(o2.langCode)) {
                    return 1;
                } else if (o1.langCode.equals("shadow")) {
                    return -1;
                } else if (o2.langCode.equals("shadow")) {
                    return 1;
                }
            }
            return 0;
        });
    }

    @Override
    protected void updateRows() {
    }

    public void search(final String query) {
        if (query == null) {
            searchResult = null;
        } else {
            processSearch(query);
        }
    }

    private void processSearch(final String query) {
        String q = query.trim().toLowerCase();
        if (q.length() == 0) {
            updateSearchResults(new ArrayList<>());
            return;
        }
        ArrayList<LocaleInfo> resultArray = new ArrayList<>();

        for (int a = 0, N = allLanguages.size(); a < N; a++) {
            LocaleInfo c = allLanguages.get(a);
            if (c.name.toString().toLowerCase().startsWith(query) || c.nameEnglish.toString().toLowerCase().startsWith(query) || c.nameLocalized.toString().toLowerCase().startsWith(query)) {
                resultArray.add(c);
            }
        }

        updateSearchResults(resultArray);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateSearchResults(final ArrayList<LocaleInfo> arrCounties) {
        AndroidUtilities.runOnUIThread(() -> {
            searchResult = arrCounties;
            searchListViewAdapter.notifyDataSetChanged();
        });
    }

    private class ListAdapter extends BaseListAdapter {

        private final boolean search;

        public ListAdapter(Context context, boolean isSearch) {
            super(context);
            search = isSearch;
        }

        @Override
        public int getItemCount() {
            if (search) {
                if (searchResult == null || searchResult.isEmpty()) {
                    return 0;
                }
                return searchResult.size() + 1;
            } else {
                return sortedLanguages.size() + 1;
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return holder.getItemViewType() == TYPE_INFO_PRIVACY || super.isEnabled(holder);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            switch (holder.getItemViewType()) {
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.getTextView().setMovementMethod(null);
                    cell.setText("miaŭ");
                    break;
                }
                case TYPE_CHECKBOX:
                case TYPE_RADIO: {
                    var localeInfo = search ? searchResult.get(position) : sortedLanguages.get(position);
                    if (holder.getItemViewType() == TYPE_CHECKBOX) {
                        var checked = restrictedLanguages.contains(localeInfo.langCode);
                        TextCheckbox2Cell cell = (TextCheckbox2Cell) holder.itemView;
                        cell.setTextAndValueAndCheck(localeInfo.name, localeInfo.nameLocalized, checked, false, divider);
                    } else {
                        TextRadioCell cell = (TextRadioCell) holder.itemView;
                        if (localeInfo.langCode.equals("app")) {
                            cell.setTextAndCheck(LocaleController.getString(R.string.TranslationTargetApp), NekoConfig.translationTarget.equals(localeInfo.langCode), divider);
                        } else {
                            cell.setTextAndValueAndCheck(localeInfo.name, localeInfo.nameLocalized, NekoConfig.translationTarget.equals(localeInfo.langCode), false, divider);
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int i) {
            if (i == (search ? searchResult : sortedLanguages).size()) {
                return currentType == TYPE_RESTRICTED && !search ? TYPE_INFO_PRIVACY : TYPE_SHADOW;
            }
            if (!search && sortedLanguages.get(i).langCode.equals("shadow")) {
                return TYPE_SHADOW;
            }
            return currentType == TYPE_TARGET ? TYPE_RADIO : TYPE_CHECKBOX;
        }
    }

    public static class LocaleInfo {

        public CharSequence name;
        public CharSequence nameEnglish;
        public CharSequence nameLocalized;
        public String langCode;

        public LocaleInfo(String langCode) {
            this.langCode = langCode;
        }
    }
}
