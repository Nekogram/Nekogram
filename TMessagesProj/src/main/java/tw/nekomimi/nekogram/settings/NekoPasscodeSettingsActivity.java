package tw.nekomimi.nekogram.settings;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalRecyclerView;
import org.telegram.ui.PasscodeActivity;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.helpers.PasscodeHelper;

public class NekoPasscodeSettingsActivity extends BaseNekoSettingsActivity {
    private boolean passcodeSet;

    private final int showInSettingsRow = rowId++;

    private final int accountsStartRow = 100;

    private final int setPanicCodeRow = rowId++;
    private final int removePanicCodeRow = rowId++;

    private final int clearPasscodesRow = rowId++;

    private final ArrayList<Integer> accounts = new ArrayList<>();

    @Override
    public boolean onFragmentCreate() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            var u = AccountInstance.getInstance(a).getUserConfig().getCurrentUser();
            if (u != null) {
                accounts.add(a);
            }
        }
        return super.onFragmentCreate();
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asCheck(showInSettingsRow, LocaleController.getString(R.string.PasscodeShowInSettings)).setEnabled(passcodeSet).setChecked(!PasscodeHelper.isSettingsHidden()));
        var link = String.format(Locale.ENGLISH, "https://t.me/nekosettings/%s", PasscodeHelper.getSettingsKey());
        var stringBuilder = new SpannableStringBuilder(AndroidUtilities.replaceTags(LocaleController.getString(R.string.PasscodeShowInSettingsAbout)));
        stringBuilder.append("\n").append(link);
        stringBuilder.setSpan(new URLSpanNoUnderline(null) {
            @Override
            public void onClick(@NonNull View view) {
                ClipboardManager clipboard = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", link);
                clipboard.setPrimaryClip(clip);
                BulletinFactory.of(NekoPasscodeSettingsActivity.this).createCopyLinkBulletin().show();
            }
        }, stringBuilder.length() - link.length(), stringBuilder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        items.add(UItem.asShadow(stringBuilder).setEnabled(passcodeSet));

        items.add(UItem.asHeader(LocaleController.getString(R.string.Account)).setEnabled(passcodeSet));
        for (var account : accounts) {
            items.add(AccountCellFactory.of(accountsStartRow + account, account).setEnabled(passcodeSet));
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.PasscodeAbout)).setEnabled(passcodeSet));

        items.add(UItem.asHeader(LocaleController.getString(R.string.PasscodePanicCode)).setEnabled(passcodeSet));
        items.add(TextSettingsCellFactory.of(setPanicCodeRow, PasscodeHelper.hasPanicCode() ? LocaleController.getString(R.string.PasscodePanicCodeEdit) : LocaleController.getString(R.string.PasscodePanicCodeSet)).setEnabled(passcodeSet));
        if (PasscodeHelper.hasPanicCode()) {
            items.add(TextSettingsCellFactory.of(removePanicCodeRow, LocaleController.getString(R.string.PasscodePanicCodeRemove)).red().setEnabled(passcodeSet));
        }
        items.add(UItem.asShadow(LocaleController.getString(R.string.PasscodePanicCodeAbout)).setEnabled(passcodeSet));

        if (BuildConfig.DEBUG) {
            items.add(TextSettingsCellFactory.of(clearPasscodesRow, "Clear passcodes").red());
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (!passcodeSet) {
            makePasscodeBulletin();
            return;
        }
        var id = item.id;
        if (id >= accountsStartRow) {
            var account = id - accountsStartRow;
            var builder = new AlertDialog.Builder(getParentActivity(), resourcesProvider);

            var linearLayout = new LinearLayout(getParentActivity());
            linearLayout.setOrientation(LinearLayout.VERTICAL);

            if (PasscodeHelper.hasPasscodeForAccount(account)) {
                TextCheckCell hideAccount = new TextCheckCell(getParentActivity(), 23, true, resourcesProvider);
                hideAccount.setTextAndCheck(LocaleController.getString(R.string.PasscodeHideAccount), PasscodeHelper.isAccountHidden(account), false);
                hideAccount.setOnClickListener(view13 -> {
                    boolean hide = !hideAccount.isChecked();
                    PasscodeHelper.setHideAccount(account, hide);
                    hideAccount.setChecked(hide);
                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                });
                hideAccount.setBackground(Theme.getSelectorDrawable(false));
                linearLayout.addView(hideAccount, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            TextCheckCell allowPanic = new TextCheckCell(getParentActivity(), 23, true, resourcesProvider);
            allowPanic.setTextAndCheck(LocaleController.getString(R.string.PasscodeAllowPanic), PasscodeHelper.isAccountAllowPanic(account), false);
            allowPanic.setOnClickListener(view13 -> {
                boolean hide = !allowPanic.isChecked();
                PasscodeHelper.setAccountAllowPanic(account, hide);
                allowPanic.setChecked(hide);
            });
            allowPanic.setBackground(Theme.getSelectorDrawable(false));
            linearLayout.addView(allowPanic, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            AlertDialog.AlertDialogCell editPasscode = new AlertDialog.AlertDialogCell(getParentActivity(), resourcesProvider);
            editPasscode.setTextAndIcon(PasscodeHelper.hasPasscodeForAccount(account) ? LocaleController.getString(R.string.PasscodeEdit) : LocaleController.getString(R.string.PasscodeSet), 0);
            editPasscode.setOnClickListener(view1 -> {
                builder.getDismissRunnable().run();
                presentFragment(new PasscodeActivity(PasscodeActivity.TYPE_SETUP_CODE, account));
            });
            editPasscode.setBackground(Theme.getSelectorDrawable(false));
            linearLayout.addView(editPasscode, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

            if (PasscodeHelper.hasPasscodeForAccount(account)) {
                AlertDialog.AlertDialogCell removePasscode = new AlertDialog.AlertDialogCell(getParentActivity(), resourcesProvider);
                removePasscode.setTextAndIcon(LocaleController.getString(R.string.PasscodeRemove), 0);
                removePasscode.setOnClickListener(view12 -> {
                    AlertDialog alertDialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                            .setTitle(LocaleController.getString(R.string.PasscodeRemove))
                            .setMessage(LocaleController.getString(R.string.PasscodeRemoveConfirmMessage))
                            .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                            .setPositiveButton(LocaleController.getString(R.string.DisablePasscodeTurnOff), (dialog, which) -> {
                                var hidden = PasscodeHelper.isAccountHidden(account);
                                PasscodeHelper.removePasscodeForAccount(account);
                                listView.adapter.notifyItemChanged(position);
                                if (hidden) {
                                    getNotificationCenter().postNotificationName(NotificationCenter.mainUserInfoChanged);
                                }
                            }).create();
                    showDialog(alertDialog);
                    ((TextView) alertDialog.getButton(Dialog.BUTTON_POSITIVE)).setTextColor(getThemedColor(Theme.key_text_RedBold));
                });
                removePasscode.setBackground(Theme.getSelectorDrawable(false));
                linearLayout.addView(removePasscode, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            builder.setView(linearLayout);
            showDialog(builder.create());
        } else if (id == clearPasscodesRow) {
            PasscodeHelper.clearAll();
            finishFragment();
        } else if (id == setPanicCodeRow) {
            presentFragment(new PasscodeActivity(PasscodeActivity.TYPE_SETUP_CODE, Integer.MAX_VALUE));
        } else if (id == removePanicCodeRow) {
            AlertDialog alertDialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                    .setTitle(LocaleController.getString(R.string.PasscodePanicCodeRemove))
                    .setMessage(LocaleController.getString(R.string.PasscodePanicCodeRemoveConfirmMessage))
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .setPositiveButton(LocaleController.getString(R.string.DisablePasscodeTurnOff), (dialog, which) -> {
                        PasscodeHelper.removePasscodeForAccount(Integer.MAX_VALUE);
                        listView.findItemByItemId(setPanicCodeRow).text = LocaleController.getString(R.string.PasscodePanicCodeSet);
                        notifyItemChanged(setPanicCodeRow);
                        notifyItemRemoved(removePanicCodeRow);
                        updateRows();
                    }).create();
            showDialog(alertDialog);
            ((TextView) alertDialog.getButton(Dialog.BUTTON_POSITIVE)).setTextColor(getThemedColor(Theme.key_text_RedBold));
        } else if (id == showInSettingsRow) {
            PasscodeHelper.setHideSettings(!PasscodeHelper.isSettingsHidden());
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(!PasscodeHelper.isSettingsHidden());
            }
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.PasscodeNeko);
    }

    @Override
    protected String getKey() {
        return PasscodeHelper.getSettingsKey();
    }

    @Override
    public void onResume() {
        super.onResume();
        passcodeSet = SharedConfig.passcodeHash.length() > 0;
        if (!passcodeSet) {
            makePasscodeBulletin();
        }
        listView.adapter.update(true);
    }

    private void makePasscodeBulletin() {
        BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.PasscodeNeeded), LocaleController.getString(R.string.Passcode), () -> presentFragment(PasscodeActivity.determineOpenFragment())).show();
    }

    protected static class AccountCellFactory extends UItem.UItemFactory<AccountCell> {
        static {
            setup(new AccountCellFactory());
        }

        @Override
        public AccountCell createView(Context context, RecyclerListView listView, int currentAccount, int classGuid, Theme.ResourcesProvider resourcesProvider) {
            return new AccountCell(context, resourcesProvider);
        }

        @Override
        public void bindView(View view, UItem item, boolean divider, UniversalAdapter adapter, UniversalRecyclerView listView) {
            var cell = (AccountCell) view;
            var account = item.intValue;
            cell.setAccount(account, PasscodeHelper.hasPasscodeForAccount(account), divider);
        }

        public static UItem of(int id, int account) {
            var item = UItem.ofFactory(AccountCellFactory.class);
            item.id = id;
            item.intValue = account;
            return item;
        }
    }
}
