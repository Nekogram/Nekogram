package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;

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

    private final ArrayList<NekoLanguagesSelectActivity.LocaleInfo> allLanguages = new ArrayList<>();
    private final ArrayList<NekoLanguagesSelectActivity.LocaleInfo> sortedLanguages = new ArrayList<>();
    private final ArrayList<NekoLanguagesSelectActivity.LocaleInfo> searchResult = new ArrayList<>();
    private boolean searchWas;
    private Runnable searchRunnable;
    private String lastSearchString;

    private ArrayList<String> restrictedLanguages;

    public NekoLanguagesSelectActivity(int type) {
        this.currentType = type;

        if (currentType == TYPE_RESTRICTED) {
            UItem.UItemFactory.setup(new TextCheckbox2CellFactory());
        } else {
            UItem.UItemFactory.setup(new TextRadioCellFactory());
        }
    }

    @Override
    public boolean onFragmentCreate() {
        fillLanguages();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        var fragmentView = super.createView(context);

        var menu = actionBar.createMenu();
        createSearchItem(menu, new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            @Override
            public void onSearchCollapse() {
                updateActionBarVisible();
                search(null);
            }

            @Override
            public void onSearchExpand() {
                updateActionBarVisible();
            }

            @Override
            public void onTextChanged(EditText editText) {
                search(editText.getText().toString());
            }
        });

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

    private void search(String text) {
        lastSearchString = text;
        if (searchRunnable != null) {
            Utilities.searchQueue.cancelRunnable(searchRunnable);
            searchRunnable = null;
        }
        if (TextUtils.isEmpty(text)) {
            searchWas = false;
            searchResult.clear();
            listView.adapter.update(true);
            return;
        }
        Utilities.searchQueue.postRunnable(searchRunnable = () -> {
            var results = new ArrayList<NekoLanguagesSelectActivity.LocaleInfo>();
            var lowerQuery = text.toLowerCase();
            for (var language : allLanguages) {
                if (language.name.toString().toLowerCase().startsWith(lowerQuery) ||
                        language.nameEnglish.toString().toLowerCase().startsWith(lowerQuery) ||
                        language.nameLocalized.toString().toLowerCase().startsWith(lowerQuery)) {
                    results.add(language);
                }
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (!text.equals(lastSearchString)) {
                    return;
                }
                searchWas = true;
                searchResult.clear();
                searchResult.addAll(results);
                listView.adapter.update(true);
            });
        }, 300);
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
        allLanguages.clear();
        Locale localeEn = Locale.forLanguageTag("en");
        for (String languageCode : currentType == TYPE_RESTRICTED ? RESTRICTED_LIST : Translator.getCurrentTargetLanguages()) {
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
        sortedLanguages.clear();
        sortedLanguages.addAll(allLanguages);
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
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        var factory = currentType == TYPE_TARGET ? TextRadioCellFactory.class : TextCheckbox2CellFactory.class;
        for (var locale : (searchWas ? searchResult : sortedLanguages)) {
            if (locale.langCode.equals("shadow")) {
                items.add(UItem.asShadow(null));
                continue;
            }
            var item = UItem.ofFactory(factory);
            item.id = sortedLanguages.indexOf(locale);
            if (locale.langCode.equals("app")) {
                item.text = LocaleController.getString(R.string.TranslationTargetApp);
            } else {
                item.text = locale.name;
                item.subtext = locale.nameLocalized;
            }
            item.checked = currentType == TYPE_RESTRICTED ? restrictedLanguages.contains(locale.langCode) : NekoConfig.translationTarget.equals(locale.langCode);
            items.add(item);
        }
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (view instanceof TextInfoPrivacyCell) {
            return;
        }
        LocaleInfo localeInfo;
        if (searchWas) {
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
                item.setChecked(!remove);
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
    protected String getActionBarTitle() {
        return currentType == TYPE_RESTRICTED ? LocaleController.getString(R.string.DoNotTranslate) : LocaleController.getString(R.string.TranslationTarget);
    }

    protected static class TextRadioCellFactory extends UItem.UItemFactory<TextRadioCell> {
        static {
            setup(new TextRadioCellFactory());
        }

        @Override
        public TextRadioCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new TextRadioCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var cell = (TextRadioCell) view;
            if (TextUtils.isEmpty(item.subtext)) {
                cell.setTextAndCheck(item.text, item.checked, divider);
            } else {
                cell.setTextAndValueAndCheck(item.text, item.subtext, item.checked, false, divider);
            }
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
