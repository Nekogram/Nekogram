package tw.nekomimi.nekogram.settings;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;

import java.util.ArrayList;
import java.util.List;

import tw.nekomimi.nekogram.plugins.IrongramPluginsController;
import tw.nekomimi.nekogram.plugins.PluginMetadata;

public final class IrongramPluginsActivity extends BaseNekoSettingsActivity {
    private static final int PICK_PLUGIN_FILE = 19742;
    private static final int PLUGIN_ROW_BASE = 200000;

    private final int addPluginRow = rowId++;
    private final int warningRow = rowId++;
    private final ArrayList<IrongramPluginsController.InstalledPlugin> pluginRows = new ArrayList<>();

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(TextDetailSettingsCellFactory.of(
                warningRow,
                LocaleController.getString(R.string.PluginManagerWarningTitle),
                LocaleController.getString(R.string.PluginManagerWarning)
        ));
        items.add(UItem.asButton(
                addPluginRow,
                R.drawable.msg_add,
                LocaleController.getString(R.string.InstallPlugin)
        ));
        items.add(UItem.asShadow(null));

        pluginRows.clear();
        List<IrongramPluginsController.InstalledPlugin> installed =
                IrongramPluginsController.getInstance().getInstalledPlugins();
        pluginRows.addAll(installed);
        if (pluginRows.isEmpty()) {
            items.add(TextDetailSettingsCellFactory.of(
                    PLUGIN_ROW_BASE,
                    LocaleController.getString(R.string.NoPluginsInstalled),
                    LocaleController.getString(R.string.NoPluginsInstalledDetail)
            ).setEnabled(false));
            return;
        }

        for (int index = 0; index < pluginRows.size(); index++) {
            IrongramPluginsController.InstalledPlugin plugin = pluginRows.get(index);
            String subtitle = plugin.enabled
                    ? LocaleController.getString(R.string.PluginEnabled)
                    : LocaleController.getString(R.string.PluginDisabled);
            if (plugin.error != null && !plugin.error.isEmpty()) {
                subtitle += " · " + plugin.error;
            } else if (!plugin.metadata.description.isEmpty()) {
                subtitle += " · " + plugin.metadata.description;
            }
            items.add(TextDetailSettingsCellFactory.of(
                    PLUGIN_ROW_BASE + index,
                    plugin.metadata.name,
                    subtitle
            ));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.id == addPluginRow) {
            openPluginPicker();
            return;
        }
        int pluginIndex = item.id - PLUGIN_ROW_BASE;
        if (pluginIndex >= 0 && pluginIndex < pluginRows.size()) {
            showPluginActions(pluginRows.get(pluginIndex));
        }
    }

    @Override
    public void onActivityResultFragment(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_PLUGIN_FILE || resultCode != Activity.RESULT_OK || data == null) {
            return;
        }
        Uri uri = data.getData();
        if (uri == null) {
            showToast(R.string.PluginInstallFailed);
            return;
        }
        IrongramPluginsController.getInstance().importPlugin(uri, (metadata, error) -> {
            listView.adapter.update(true);
            if (metadata == null) {
                showToast(error == null ? LocaleController.getString(R.string.PluginInstallFailed) : error);
                return;
            }
            showToast(LocaleController.formatString(R.string.PluginInstalled, metadata.name));
        });
    }

    private void openPluginPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "text/x-python",
                "application/x-python-code",
                "text/plain"
        });
        startActivityForResult(intent, PICK_PLUGIN_FILE);
    }

    private void showPluginActions(IrongramPluginsController.InstalledPlugin entry) {
        PluginMetadata metadata = entry.metadata;
        StringBuilder details = new StringBuilder();
        details.append(LocaleController.formatString(R.string.PluginVersion, metadata.version));
        if (!metadata.author.isEmpty()) {
            details.append("\n").append(LocaleController.formatString(R.string.PluginAuthor, metadata.author));
        }
        if (!metadata.requirements.isEmpty()) {
            details.append("\n").append(LocaleController.formatString(
                    R.string.PluginDependencies,
                    TextUtils.join(", ", metadata.requirements)
            ));
        }
        if (!metadata.description.isEmpty()) {
            details.append("\n\n").append(metadata.description);
        }
        if (entry.error != null && !entry.error.isEmpty()) {
            details.append("\n\n").append(LocaleController.getString(R.string.PluginLastError))
                    .append(": ").append(entry.error);
        }

        CharSequence[] actions = entry.enabled
                ? new CharSequence[]{
                        LocaleController.getString(R.string.PluginSettings),
                        LocaleController.getString(R.string.DisablePlugin),
                        LocaleController.getString(R.string.RemovePlugin)
                }
                : new CharSequence[]{
                        LocaleController.getString(R.string.EnablePlugin),
                        LocaleController.getString(R.string.RemovePlugin)
                };
        new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                .setTitle(metadata.name)
                .setMessage(details.toString())
                .setItems(actions, (dialog, which) -> {
                    if (entry.enabled) {
                        if (which == 0) {
                            dialog.dismiss();
                            presentFragment(new IrongramPluginSettingsActivity(metadata));
                        } else if (which == 1) {
                            setPluginEnabled(metadata, false);
                        } else if (which == 2) {
                            confirmRemove(metadata);
                        }
                    } else if (which == 0) {
                        confirmEnable(metadata);
                    } else if (which == 1) {
                        confirmRemove(metadata);
                    }
                })
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
    }

    private void confirmEnable(PluginMetadata metadata) {
        new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                .setTitle(LocaleController.getString(R.string.EnablePlugin))
                .setMessage(LocaleController.getString(R.string.PluginTrustWarning))
                .setPositiveButton(LocaleController.getString(R.string.EnablePlugin), (dialog, which) ->
                        setPluginEnabled(metadata, true))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
    }

    private void confirmRemove(PluginMetadata metadata) {
        new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                .setTitle(LocaleController.getString(R.string.RemovePlugin))
                .setMessage(LocaleController.formatString(R.string.RemovePluginConfirmation, metadata.name))
                .setPositiveButton(LocaleController.getString(R.string.RemovePlugin), (dialog, which) ->
                        IrongramPluginsController.getInstance().removePlugin(metadata, (success, error) -> {
                            listView.adapter.update(true);
                            showToast(success ? LocaleController.getString(R.string.PluginRemoved) : error);
                        }))
                .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                .show();
    }

    private void setPluginEnabled(PluginMetadata metadata, boolean enabled) {
        IrongramPluginsController.getInstance().setPluginEnabled(metadata, enabled, (success, error) -> {
            listView.adapter.update(true);
            if (success) {
                showToast(LocaleController.getString(enabled ? R.string.PluginEnabled : R.string.PluginDisabled));
            } else {
                showToast(error == null ? LocaleController.getString(R.string.PluginOperationFailed) : error);
            }
        });
    }

    private void showToast(int stringId) {
        showToast(LocaleController.getString(stringId));
    }

    private void showToast(String message) {
        if (getParentActivity() != null && message != null) {
            Toast.makeText(getParentActivity(), message, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.Plugins);
    }
}
