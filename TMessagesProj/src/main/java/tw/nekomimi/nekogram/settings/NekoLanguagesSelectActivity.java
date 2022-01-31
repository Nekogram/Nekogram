package tw.nekomimi.nekogram.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.LanguageCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckbox2Cell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.ui.Components.EmptyTextProgressView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.translator.Translator;

@SuppressLint("NotifyDataSetChanged")
public class NekoLanguagesSelectActivity extends BaseFragment {

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

    private ListAdapter listAdapter;
    private RecyclerListView listView;
    private ListAdapter searchListViewAdapter;
    private EmptyTextProgressView emptyView;

    private ArrayList<LocaleInfo> searchResult;
    private ArrayList<LocaleInfo> sortedLanguages;

    public NekoLanguagesSelectActivity(int type) {
        this.currentType = type;
    }

    @Override
    public boolean onFragmentCreate() {
        fillLanguages();
        return super.onFragmentCreate();
    }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(currentType == TYPE_RESTRICTED ? LocaleController.getString("DoNotTranslate", R.string.DoNotTranslate) : LocaleController.getString("TranslationTarget", R.string.TranslationTarget));

        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

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
        item.setSearchFieldHint(LocaleController.getString("Search", R.string.Search));

        listAdapter = new ListAdapter(context, false);
        searchListViewAdapter = new ListAdapter(context, true);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        emptyView = new EmptyTextProgressView(context);
        emptyView.setText(LocaleController.getString("NoResult", R.string.NoResult));
        emptyView.showTextView();
        emptyView.setShowAtCenter(true);
        frameLayout.addView(emptyView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView = new RecyclerListView(context);
        listView.setEmptyView(emptyView);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
        listView.setVerticalScrollBarEnabled(false);
        listView.setAdapter(listAdapter);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        listView.setOnItemClickListener((view, position) -> {
            if (view instanceof ShadowSectionCell || view instanceof HeaderCell) {
                return;
            }
            boolean search = listView.getAdapter() == searchListViewAdapter;
            if (!search) position--;
            LocaleInfo localeInfo;
            if (search) {
                localeInfo = searchResult.get(position);
            } else {
                localeInfo = sortedLanguages.get(position);
            }
            if (localeInfo != null) {
                if (currentType == TYPE_RESTRICTED) {
                    TextCheckbox2Cell cell = (TextCheckbox2Cell) view;
                    if (localeInfo.langCode.equals(getCurrentTargetLanguage())) {
                        AndroidUtilities.shakeView(((TextCheckbox2Cell) view).checkbox, 2, 0);
                        return;
                    }
                    boolean remove = NekoConfig.restrictedLanguages.contains(localeInfo.langCode);
                    if (remove) {
                        NekoConfig.restrictedLanguages.removeIf(s -> s != null && s.equals(localeInfo.langCode));
                    } else {
                        NekoConfig.restrictedLanguages.add(localeInfo.langCode);
                    }
                    NekoConfig.saveRestrictedLanguages();
                    cell.setChecked(!remove);
                } else {
                    NekoConfig.setTranslationTarget(localeInfo.langCode);
                    finishFragment();
                }
            }
        });

        listView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    AndroidUtilities.hideKeyboard(getParentActivity().getCurrentFocus());
                }
            }
        });

        return fragmentView;
    }

    private String getCurrentTargetLanguage() {
        var language = Translator.getCurrentTranslator().getCurrentTargetLanguage();
        if (currentType == TYPE_RESTRICTED) {
            language = Translator.stripLanguageCode(language);
        }
        return language;
    }

    private void fillLanguages() {
        sortedLanguages = new ArrayList<>();
        Locale localeEn = Locale.forLanguageTag("en");
        for (String languageCode : currentType == TYPE_RESTRICTED ? RESTRICTED_LIST : Translator.getCurrentTranslator().getTargetLanguages()) {
            var localeInfo = new LocaleInfo();
            localeInfo.langCode = languageCode;
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
            sortedLanguages.add(localeInfo);
        }
        if (currentType == TYPE_TARGET) {
            var localeInfo = new LocaleInfo();
            localeInfo.langCode = "app";
            sortedLanguages.add(0, localeInfo);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
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

        for (int a = 0, N = sortedLanguages.size(); a < N; a++) {
            LocaleInfo c = sortedLanguages.get(a);
            if (c.langCode.equals("app")) {
                continue;
            }
            if (c.name.toString().toLowerCase().startsWith(query) || c.nameEnglish.toString().toLowerCase().startsWith(query) || c.nameLocalized.toString().toLowerCase().startsWith(query)) {
                resultArray.add(c);
            }
        }

        updateSearchResults(resultArray);
    }

    private void updateSearchResults(final ArrayList<LocaleInfo> arrCounties) {
        AndroidUtilities.runOnUIThread(() -> {
            searchResult = arrCounties;
            searchListViewAdapter.notifyDataSetChanged();
        });
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private final Context mContext;
        private final boolean search;

        public ListAdapter(Context context, boolean isSearch) {
            mContext = context;
            search = isSearch;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int type = holder.getItemViewType();
            return type == 3 || type == 4;
        }

        @Override
        public int getItemCount() {
            if (search) {
                if (searchResult == null || searchResult.size() == 0) {
                    return 0;
                }
                return searchResult.size() + 1;
            } else {
                return sortedLanguages.size() + 2;
            }
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 1: {
                    view = new ShadowSectionCell(mContext);
                    break;
                }
                case 2:
                    HeaderCell header = new HeaderCell(mContext);
                    header.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    header.setText(LocaleController.getString("ChooseLanguages", R.string.ChooseLanguages));
                    view = header;
                    break;
                case 3:
                    view = new TextRadioCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 4:
                    view = new TextCheckbox2Cell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    holder.itemView.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                    break;
                }
                case 3: {
                    if (!search) position--;
                    TextRadioCell cell = (TextRadioCell) holder.itemView;
                    LocaleInfo localeInfo;
                    boolean last;
                    if (search) {
                        localeInfo = searchResult.get(position);
                        last = position == searchResult.size() - 1;
                    } else {
                        localeInfo = sortedLanguages.get(position);
                        last = position == sortedLanguages.size() - 1;
                    }
                    if (localeInfo.langCode.equals("app")) {
                        cell.setTextAndCheck(LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp), NekoConfig.translationTarget.equals(localeInfo.langCode), !last);
                    } else {
                        cell.setTextAndValueAndCheck(localeInfo.name, localeInfo.nameLocalized, NekoConfig.translationTarget.equals(localeInfo.langCode), false, !last);
                    }
                    break;
                }
                case 4: {
                    if (!search) position--;
                    TextCheckbox2Cell cell = (TextCheckbox2Cell) holder.itemView;
                    LocaleInfo localeInfo;
                    boolean last;
                    if (search) {
                        localeInfo = searchResult.get(position);
                        last = position == searchResult.size() - 1;
                    } else {
                        localeInfo = sortedLanguages.get(position);
                        last = position == sortedLanguages.size() - 1;
                    }
                    boolean checked = NekoConfig.restrictedLanguages.contains(localeInfo.langCode) || localeInfo.langCode.equals(getCurrentTargetLanguage());
                    cell.setTextAndValueAndCheck(localeInfo.name, localeInfo.nameLocalized, checked, false, !last);
                    break;
                }
            }
        }

        @Override
        public int getItemViewType(int i) {
            if (!search) i--;
            if (i == -1) return 2;
            if (i == (search ? searchResult : sortedLanguages).size()) return 1;
            return currentType == TYPE_TARGET ? 3 : 4;
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = new ArrayList<>();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{LanguageCell.class}, null, null, null, Theme.key_windowBackgroundWhite));
        themeDescriptions.add(new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray));

        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_actionBarDefault));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_actionBarDefaultIcon));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_actionBarDefaultSelector));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCH, null, null, null, null, Theme.key_actionBarDefaultSearch));
        themeDescriptions.add(new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SEARCHPLACEHOLDER, null, null, null, null, Theme.key_actionBarDefaultSearchPlaceholder));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector));

        themeDescriptions.add(new ThemeDescription(emptyView, ThemeDescription.FLAG_TEXTCOLOR, null, null, null, null, Theme.key_emptyListPlaceholder));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider));

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{LanguageCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{LanguageCell.class}, new String[]{"textView2"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText3));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{LanguageCell.class}, new String[]{"checkImage"}, null, null, null, Theme.key_featuredStickers_addedIcon));

        return themeDescriptions;
    }


    public static class LocaleInfo {

        public CharSequence name;
        public CharSequence nameEnglish;
        public CharSequence nameLocalized;
        public String langCode;
    }
}
