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
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.URLSpanNoUnderline;
import org.telegram.ui.PasscodeActivity;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.helpers.PasscodeHelper;

public class NekoPasscodeSettingsActivity extends BaseNekoSettingsActivity {

    private boolean passcodeSet;

    private int showInSettingsRow;
    private int showInSettings2Row;

    private int accountsStartRow;
    private int accountsEndRow;

    private int panicCodeRow;
    private int setPanicCodeRow;
    private int removePanicCodeRow;
    private int panicCode2Row;

    private int clearPasscodesRow;
    private int clearPasscodes2Row;

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
    protected void onItemClick(View view, int position, float x, float y) {
        if (!passcodeSet) {
            makePasscodeBulletin();
            return;
        }
        if (position > accountsStartRow && position < accountsEndRow) {
            var account = accounts.get(position - accountsStartRow - 1);
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
                                listAdapter.notifyItemChanged(position);
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
        } else if (position == clearPasscodesRow) {
            PasscodeHelper.clearAll();
            finishFragment();
        } else if (position == setPanicCodeRow) {
            presentFragment(new PasscodeActivity(PasscodeActivity.TYPE_SETUP_CODE, Integer.MAX_VALUE));
        } else if (position == removePanicCodeRow) {
            AlertDialog alertDialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                    .setTitle(LocaleController.getString(R.string.PasscodePanicCodeRemove))
                    .setMessage(LocaleController.getString(R.string.PasscodePanicCodeRemoveConfirmMessage))
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null)
                    .setPositiveButton(LocaleController.getString(R.string.DisablePasscodeTurnOff), (dialog, which) -> {
                        PasscodeHelper.removePasscodeForAccount(Integer.MAX_VALUE);
                        listAdapter.notifyItemChanged(setPanicCodeRow);
                        listAdapter.notifyItemRemoved(removePanicCodeRow);
                        updateRows();
                    }).create();
            showDialog(alertDialog);
            ((TextView) alertDialog.getButton(Dialog.BUTTON_POSITIVE)).setTextColor(getThemedColor(Theme.key_text_RedBold));
        } else if (position == showInSettingsRow) {
            PasscodeHelper.setHideSettings(!PasscodeHelper.isSettingsHidden());
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(!PasscodeHelper.isSettingsHidden());
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
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
        passcodeSet = SharedConfig.passcodeHash.length() > 0;
        if (!passcodeSet) {
            makePasscodeBulletin();
        }
        updateRows();
        super.onResume();
    }

    private void makePasscodeBulletin() {
        BulletinFactory.of(this).createSimpleBulletin(R.raw.info, LocaleController.getString(R.string.PasscodeNeeded), LocaleController.getString(R.string.Passcode), () -> presentFragment(PasscodeActivity.determineOpenFragment())).show();
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        showInSettingsRow = rowCount++;
        showInSettings2Row = rowCount++;

        accountsStartRow = rowCount++;
        rowCount += accounts.size();
        accountsEndRow = rowCount++;

        panicCodeRow = rowCount++;
        setPanicCodeRow = rowCount++;
        if (!PasscodeHelper.hasPanicCode()) {
            removePanicCodeRow = -1;
        } else {
            removePanicCodeRow = rowCount++;
        }
        panicCode2Row = rowCount++;

        if (false) {
            clearPasscodesRow = rowCount++;
            clearPasscodes2Row = rowCount++;
        } else {
            clearPasscodesRow = -1;
            clearPasscodes2Row = -1;
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
                    textCell.setCanDisable(true);
                    textCell.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == setPanicCodeRow) {
                        textCell.setText(PasscodeHelper.hasPanicCode() ? LocaleController.getString(R.string.PasscodePanicCodeEdit) : LocaleController.getString(R.string.PasscodePanicCodeSet), removePanicCodeRow != -1);
                    } else if (position == clearPasscodesRow) {
                        textCell.setTextColor(getThemedColor(Theme.key_text_RedRegular));
                        textCell.setText("Clear passcodes", divider);
                    } else if (position == removePanicCodeRow) {
                        textCell.setTextColor(getThemedColor(Theme.key_text_RedRegular));
                        textCell.setText(LocaleController.getString(R.string.PasscodePanicCodeRemove), divider);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(passcodeSet, null);
                    if (position == showInSettingsRow) {
                        textCell.setTextAndCheck(LocaleController.getString(R.string.PasscodeShowInSettings), !PasscodeHelper.isSettingsHidden(), divider);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell cell = (HeaderCell) holder.itemView;
                    cell.setEnabled(passcodeSet, null);
                    if (position == accountsStartRow) {
                        cell.setText(LocaleController.getString(R.string.Account));
                    } else if (position == panicCodeRow) {
                        cell.setText(LocaleController.getString(R.string.PasscodePanicCode));
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    cell.setEnabled(passcodeSet, null);
                    if (position == accountsEndRow) {
                        cell.setText(LocaleController.getString(R.string.PasscodeAbout));
                    } else if (position == panicCode2Row) {
                        cell.setText(LocaleController.getString(R.string.PasscodePanicCodeAbout));
                    } else if (position == showInSettings2Row) {
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
                        cell.setText(stringBuilder);
                    }
                    break;
                }
                case TYPE_ACCOUNT: {
                    AccountCell cell = (AccountCell) holder.itemView;
                    cell.setEnabled(passcodeSet);
                    int account = accounts.get(position - accountsStartRow - 1);
                    cell.setAccount(account, PasscodeHelper.hasPasscodeForAccount(account), divider);
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return passcodeSet && super.isEnabled(holder);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == clearPasscodes2Row) {
                return TYPE_SHADOW;
            } else if (position == clearPasscodesRow || position == setPanicCodeRow || position == removePanicCodeRow) {
                return TYPE_SETTINGS;
            } else if (position == showInSettingsRow) {
                return TYPE_CHECK;
            } else if (position == accountsStartRow || position == panicCodeRow) {
                return TYPE_HEADER;
            } else if (position == showInSettings2Row || position == accountsEndRow || position == panicCode2Row) {
                return TYPE_INFO_PRIVACY;
            } else if (position > accountsStartRow && position < accountsEndRow) {
                return TYPE_ACCOUNT;
            }
            return TYPE_SETTINGS;
        }
    }

}
