package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;

import tw.nekomimi.nekogram.plugins.IrongramPluginsController;
import tw.nekomimi.nekogram.plugins.PluginMetadata;

public final class IrongramPluginSettingsActivity extends BaseNekoSettingsActivity {
    private static final int SETTING_ROW_BASE = 310000;
    private static final int STATE_ROW = 1;

    private final String pluginId;
    private final String pluginName;
    private final ArrayList<IrongramPluginsController.PluginSetting> settings = new ArrayList<>();

    private String pageId = "root";
    private boolean loading = true;
    private boolean hasResumedOnce;
    private String loadError;
    private int loadGeneration;

    public IrongramPluginSettingsActivity(PluginMetadata metadata) {
        this(metadata.id, metadata.name, null);
    }

    private IrongramPluginSettingsActivity(String pluginId, String pluginName,
                                           IrongramPluginsController.PluginSettingsPage initialPage) {
        this.pluginId = pluginId;
        this.pluginName = pluginName;
        if (initialPage != null) {
            pageId = initialPage.pageId;
            settings.addAll(initialPage.settings);
            loading = false;
        }
    }

    @Override
    public boolean onFragmentCreate() {
        boolean created = super.onFragmentCreate();
        if (created && loading) {
            loadSettings();
        }
        return created;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasResumedOnce && !loading) {
            loadSettings();
        }
        hasResumedOnce = true;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (loading) {
            items.add(TextDetailSettingsCellFactory.of(
                    STATE_ROW,
                    LocaleController.getString(R.string.PluginSettingLoading),
                    ""
            ).setEnabled(false));
            return;
        }
        if (loadError != null) {
            items.add(TextDetailSettingsCellFactory.of(
                    STATE_ROW,
                    LocaleController.getString(R.string.PluginSettingsLoadFailed),
                    loadError
            ).setEnabled(false));
            return;
        }
        if (settings.isEmpty()) {
            items.add(TextDetailSettingsCellFactory.of(
                    STATE_ROW,
                    LocaleController.getString(R.string.PluginNoSettings),
                    LocaleController.getString(R.string.PluginNoSettingsDetail)
            ).setEnabled(false));
            return;
        }

        for (IrongramPluginsController.PluginSetting setting : settings) {
            int rowId = SETTING_ROW_BASE + setting.index;
            String title = setting.text;
            switch (setting.type) {
                case "header":
                    items.add(UItem.asHeader(title));
                    break;
                case "divider":
                    items.add(UItem.asShadow(TextUtils.isEmpty(title) ? null : title));
                    break;
                case "switch":
                    if (!setting.supported || TextUtils.isEmpty(setting.key)) {
                        addUnsupportedItem(items, rowId, setting);
                        break;
                    }
                    items.add(UItem.asCheck(rowId, title, setting.subtext)
                            .setChecked(setting.checked));
                    break;
                case "selector":
                    if (!setting.supported || TextUtils.isEmpty(setting.key) || setting.options.isEmpty()) {
                        addUnsupportedItem(items, rowId, setting);
                        break;
                    }
                    items.add(TextSettingsCellFactory.of(
                            rowId,
                            title,
                            getSelectorValue(setting)
                    ));
                    break;
                case "input":
                    if (!setting.supported || TextUtils.isEmpty(setting.key)) {
                        addUnsupportedItem(items, rowId, setting);
                        break;
                    }
                    String inputValue = getInputValue(setting);
                    if (TextUtils.isEmpty(setting.subtext)) {
                        items.add(TextSettingsCellFactory.of(rowId, title, inputValue));
                    } else {
                        items.add(TextDetailSettingsCellFactory.of(
                                rowId,
                                title,
                                setting.subtext + "\n" + inputValue
                        ));
                    }
                    break;
                case "edittext": {
                    if (!setting.supported || TextUtils.isEmpty(setting.key)) {
                        addUnsupportedItem(items, rowId, setting);
                        break;
                    }
                    String editTitle = TextUtils.isEmpty(setting.hint)
                            ? LocaleController.getString(R.string.PluginTextInput)
                            : setting.hint;
                    items.add(TextSettingsCellFactory.of(
                            rowId,
                            editTitle,
                            getInputValue(setting)
                    ));
                    break;
                }
                case "text":
                    if (!setting.supported) {
                        addUnsupportedItem(items, rowId, setting);
                        break;
                    }
                    UItem textItem = TextUtils.isEmpty(setting.subtext)
                            ? TextSettingsCellFactory.of(rowId, title)
                            : TextDetailSettingsCellFactory.of(rowId, title, setting.subtext);
                    if (setting.red) {
                        textItem.red();
                    } else if (setting.accent) {
                        textItem.accent();
                    }
                    items.add(textItem);
                    break;
                default:
                    addUnsupportedItem(items, rowId, setting);
                    break;
            }
        }
    }

    private void addUnsupportedItem(ArrayList<UItem> items, int rowId,
                                    IrongramPluginsController.PluginSetting setting) {
        String title = TextUtils.isEmpty(setting.text)
                ? LocaleController.getString(R.string.PluginCustomSetting)
                : setting.text;
        items.add(TextDetailSettingsCellFactory.of(
                rowId,
                title,
                LocaleController.getString(R.string.PluginSettingUnsupported)
        ).setEnabled(false));
    }

    private String getSelectorValue(IrongramPluginsController.PluginSetting setting) {
        int index = setting.selectedIndex;
        return index >= 0 && index < setting.options.size()
                ? setting.options.get(index)
                : LocaleController.getString(R.string.PluginSettingNotSet);
    }

    private String getInputValue(IrongramPluginsController.PluginSetting setting) {
        if (TextUtils.isEmpty(setting.value)) {
            return LocaleController.getString(R.string.PluginSettingNotSet);
        }
        String value = setting.value.replace('\n', ' ');
        return value.length() > 70 ? value.substring(0, 67) + "…" : value;
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        int index = item.id - SETTING_ROW_BASE;
        if (index < 0) {
            return;
        }
        IrongramPluginsController.PluginSetting setting = findSetting(index);
        if (setting == null) {
            return;
        }
        switch (setting.type) {
            case "switch":
                updateSetting(setting, !setting.checked);
                break;
            case "selector":
                showSelector(setting);
                break;
            case "input":
            case "edittext":
                showTextInput(setting);
                break;
            case "text":
                activateTextSetting(setting, view);
                break;
        }
    }

    private IrongramPluginsController.PluginSetting findSetting(int index) {
        for (IrongramPluginsController.PluginSetting setting : settings) {
            if (setting.index == index) {
                return setting;
            }
        }
        return null;
    }

    private void showSelector(IrongramPluginsController.PluginSetting setting) {
        if (setting.options.isEmpty()) {
            return;
        }
        CharSequence[] options = setting.options.toArray(new CharSequence[0]);
        AlertDialog dialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                .setTitle(setting.text)
                .setItems(options, (dialogInterface, which) -> updateSetting(setting, which))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .create();
        showDialog(dialog);
    }

    private void showTextInput(IrongramPluginsController.PluginSetting setting) {
        Context context = getParentActivity();
        if (context == null) {
            return;
        }
        EditTextBoldCursor editText = new EditTextBoldCursor(context);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        editText.setHintText(setting.type.equals("edittext") ? setting.hint : setting.text);
        editText.setHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText, resourcesProvider));
        editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
        editText.setLineColors(
                Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider),
                Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider),
                Theme.getColor(Theme.key_text_RedRegular, resourcesProvider)
        );
        editText.setBackground(null);
        editText.setText(setting.value);
        editText.setSelection(editText.getText().length());
        editText.setFocusable(true);
        editText.setFocusableInTouchMode(true);
        editText.setTransformHintToHeader(true);

        boolean multiline = setting.type.equals("edittext") && setting.multiline;
        if (multiline) {
            editText.setSingleLine(false);
            editText.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            editText.setGravity(Gravity.TOP | Gravity.START);
            editText.setMinLines(3);
            editText.setMaxLines(8);
        } else {
            editText.setSingleLine(true);
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
        if (setting.maxLength > 0) {
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(setting.maxLength)});
        }

        LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.addView(editText, LayoutHelper.createLinear(
                LayoutHelper.MATCH_PARENT,
                multiline ? 112 : 56,
                0,
                24,
                0,
                24,
                0
        ));

        String title = setting.type.equals("edittext")
                ? LocaleController.getString(R.string.PluginTextInput)
                : setting.text;
        AlertDialog dialog = new AlertDialog.Builder(context, resourcesProvider)
                .setTitle(title)
                .setView(container)
                .setPositiveButton(LocaleController.getString(R.string.OK),
                        (dialogInterface, which) -> updateSetting(setting, editText.getText().toString()))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .create();
        showDialog(dialog);
        editText.requestFocus();
        AndroidUtilities.showKeyboard(editText);
    }

    private void updateSetting(IrongramPluginsController.PluginSetting setting, Object value) {
        IrongramPluginsController.getInstance().setPluginSetting(
                pluginId,
                pageId,
                setting.index,
                value,
                (success, error) -> {
                    if (!success) {
                        showToast(error == null
                                ? LocaleController.getString(R.string.PluginOperationFailed)
                                : error);
                    }
                    loadSettings();
                }
        );
    }

    private void activateTextSetting(IrongramPluginsController.PluginSetting setting, View view) {
        if (!setting.clickable) {
            return;
        }
        IrongramPluginsController.getInstance().clickPluginSetting(
                pluginId,
                pageId,
                setting.index,
                view,
                (success, error) -> {
                    if (!success) {
                        showToast(error == null
                                ? LocaleController.getString(R.string.PluginOperationFailed)
                                : error);
                        loadSettings();
                        return;
                    }
                    if (setting.hasSubpage) {
                        IrongramPluginsController.getInstance().openPluginSettingsSubpage(
                                pluginId,
                                pageId,
                                setting.index,
                                (page, pageError) -> {
                                    if (page == null) {
                                        showToast(pageError == null
                                                ? LocaleController.getString(R.string.PluginSettingsLoadFailed)
                                                : pageError);
                                        return;
                                    }
                                    String title = TextUtils.isEmpty(setting.text) ? pluginName : setting.text;
                                    presentFragment(new IrongramPluginSettingsActivity(pluginId, title, page));
                                }
                        );
                    }
                }
        );
    }

    private void loadSettings() {
        int generation = ++loadGeneration;
        if (settings.isEmpty()) {
            loading = true;
        }
        IrongramPluginsController.getInstance().getPluginSettings(pluginId, pageId, (page, error) -> {
            if (isFinished || generation != loadGeneration) {
                return;
            }
            loading = false;
            loadError = error;
            settings.clear();
            if (page != null) {
                settings.addAll(page.settings);
                pageId = page.pageId;
            }
            if (listView != null && listView.adapter != null) {
                listView.adapter.update(true);
            }
        });
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void showToast(String message) {
        if (getParentActivity() != null && message != null) {
            Toast.makeText(getParentActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String getActionBarTitle() {
        return pluginName;
    }
}
