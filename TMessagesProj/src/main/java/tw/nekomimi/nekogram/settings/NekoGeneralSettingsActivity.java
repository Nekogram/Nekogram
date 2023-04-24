package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextInfoPrivacyCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;
import java.util.Locale;

import app.nekogram.translator.DeepLTranslator;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.translator.Translator;

public class NekoGeneralSettingsActivity extends BaseNekoSettingsActivity {

    private final boolean supportLanguageDetector;

    private int connectionRow;
    private int ipv6Row;
    private int connection2Row;

    private int translatorRow;
    private int showOriginalRow;
    private int translatorTypeRow;
    private int deepLFormalityRow;
    private int translationProviderRow;
    private int translationTargetRow;
    private int doNotTranslateRow;
    private int autoTranslateRow;
    private int translator2Row;

    private int notificationRow;
    private int accentAsNotificationColorRow;
    private int silenceNonContactsRow;
    private int notification2Row;
    private int generalRow;
    private int disabledInstantCameraRow;
    private int askBeforeCallRow;
    private int openArchiveOnPullRow;
    private int nameOrderRow;
    private int idTypeRow;
    private int general2Row;

    public NekoGeneralSettingsActivity() {
        supportLanguageDetector = LanguageDetector.hasSupport();
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == ipv6Row) {
            NekoConfig.toggleIPv6();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.useIPv6);
            }
            for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                if (UserConfig.getInstance(a).isClientActivated()) {
                    ConnectionsManager.getInstance(a).checkConnection();
                }
            }
        } else if (position == disabledInstantCameraRow) {
            NekoConfig.toggleDisabledInstantCamera();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.disableInstantCamera);
            }
        } else if (position == nameOrderRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("FirstLast", R.string.FirstLast));
            types.add(1);
            arrayList.add(LocaleController.getString("LastFirst", R.string.LastFirst));
            types.add(2);
            PopupHelper.show(arrayList, LocaleController.getString("NameOrder", R.string.NameOrder), types.indexOf(NekoConfig.nameOrder), getParentActivity(), view, i -> {
                NekoConfig.setNameOrder(types.get(i));
                listAdapter.notifyItemChanged(nameOrderRow, PARTIAL);
                parentLayout.rebuildAllFragmentViews(false, false);
            });
        } else if (position == translationProviderRow) {
            final String oldProvider = NekoConfig.translationProvider;
            Translator.showTranslationProviderSelector(getParentActivity(), view, param -> {
                if (param) {
                    listAdapter.notifyItemChanged(translationProviderRow, PARTIAL);
                } else {
                    listAdapter.notifyItemChanged(translationProviderRow, PARTIAL);
                    listAdapter.notifyItemChanged(translationTargetRow, PARTIAL);
                }
                if (!oldProvider.equals(NekoConfig.translationProvider)) {
                    if (oldProvider.equals(Translator.PROVIDER_DEEPL)) {
                        listAdapter.notifyItemRemoved(deepLFormalityRow);
                        updateRows();
                    } else if (NekoConfig.translationProvider.equals(Translator.PROVIDER_DEEPL)) {
                        updateRows();
                        listAdapter.notifyItemInserted(deepLFormalityRow);
                    }
                }
            });
        } else if (position == translationTargetRow) {
            Translator.showTranslationTargetSelector(this, view, () -> {
                listAdapter.notifyItemChanged(translationTargetRow, PARTIAL);
                if (getRestrictedLanguages().size() == 1) {
                    listAdapter.notifyItemChanged(doNotTranslateRow, PARTIAL);
                }
            });
        } else if (position == deepLFormalityRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("DeepLFormalityDefault", R.string.DeepLFormalityDefault));
            types.add(DeepLTranslator.FORMALITY_DEFAULT);
            arrayList.add(LocaleController.getString("DeepLFormalityMore", R.string.DeepLFormalityMore));
            types.add(DeepLTranslator.FORMALITY_MORE);
            arrayList.add(LocaleController.getString("DeepLFormalityLess", R.string.DeepLFormalityLess));
            types.add(DeepLTranslator.FORMALITY_LESS);
            PopupHelper.show(arrayList, LocaleController.getString("DeepLFormality", R.string.DeepLFormality), types.indexOf(NekoConfig.deepLFormality), getParentActivity(), view, i -> {
                NekoConfig.setDeepLFormality(types.get(i));
                listAdapter.notifyItemChanged(deepLFormalityRow, PARTIAL);
            });
        } else if (position == openArchiveOnPullRow) {
            NekoConfig.toggleOpenArchiveOnPull();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.openArchiveOnPull);
            }
        } else if (position == askBeforeCallRow) {
            NekoConfig.toggleAskBeforeCall();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.askBeforeCall);
            }
        } else if (position == idTypeRow) {
            ArrayList<String> arrayList = new ArrayList<>();
            ArrayList<Integer> types = new ArrayList<>();
            arrayList.add(LocaleController.getString("IdTypeHidden", R.string.IdTypeHidden));
            types.add(NekoConfig.ID_TYPE_HIDDEN);
            arrayList.add(LocaleController.getString("IdTypeAPI", R.string.IdTypeAPI));
            types.add(NekoConfig.ID_TYPE_API);
            arrayList.add(LocaleController.getString("IdTypeBOTAPI", R.string.IdTypeBOTAPI));
            types.add(NekoConfig.ID_TYPE_BOTAPI);
            PopupHelper.show(arrayList, LocaleController.getString("IdType", R.string.IdType), types.indexOf(NekoConfig.idType), getParentActivity(), view, i -> {
                NekoConfig.setIdType(types.get(i));
                listAdapter.notifyItemChanged(idTypeRow, PARTIAL);
                parentLayout.rebuildAllFragmentViews(false, false);
            });
        } else if (position == accentAsNotificationColorRow) {
            NekoConfig.toggleAccentAsNotificationColor();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.accentAsNotificationColor);
            }
        } else if (position == silenceNonContactsRow) {
            NekoConfig.toggleSilenceNonContacts();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.silenceNonContacts);
            }
        } else if (position == translatorTypeRow) {
            int oldType = NekoConfig.transType;
            Translator.showTranslatorTypeSelector(getParentActivity(), view, () -> {
                int newType = NekoConfig.transType;
                listAdapter.notifyItemChanged(translatorTypeRow, PARTIAL);
                if (oldType != newType) {
                    int count = 4;
                    if (NekoConfig.translationProvider.equals(Translator.PROVIDER_DEEPL)) {
                        count++;
                    }
                    if (oldType == NekoConfig.TRANS_TYPE_NEKO || newType == NekoConfig.TRANS_TYPE_NEKO) {
                        count++;
                    }
                    if (oldType == NekoConfig.TRANS_TYPE_EXTERNAL) {
                        updateRows();
                        listAdapter.notifyItemRangeInserted(translationProviderRow, count);
                    } else if (newType == NekoConfig.TRANS_TYPE_EXTERNAL) {
                        listAdapter.notifyItemRangeRemoved(translationProviderRow, count);
                        updateRows();
                    } else if (oldType == NekoConfig.TRANS_TYPE_NEKO) {
                        listAdapter.notifyItemRemoved(showOriginalRow);
                        updateRows();
                    } else if (newType == NekoConfig.TRANS_TYPE_NEKO) {
                        updateRows();
                        listAdapter.notifyItemInserted(showOriginalRow);
                    }
                }
            });
        } else if (position == doNotTranslateRow) {
            if (!supportLanguageDetector) {
                BulletinFactory.of(this).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            presentFragment(new NekoLanguagesSelectActivity(NekoLanguagesSelectActivity.TYPE_RESTRICTED, true));
        } else if (position == autoTranslateRow) {
            if (!supportLanguageDetector) {
                BulletinFactory.of(this).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            NekoConfig.toggleAutoTranslate();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.autoTranslate);
            }
        } else if (position == showOriginalRow) {
            NekoConfig.toggleShowOriginal();
            if (view instanceof TextCheckCell) {
                ((TextCheckCell) view).setChecked(NekoConfig.showOriginal);
            }
        }
    }

    @Override
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString("General", R.string.General);
    }

    @Override
    protected void updateRows() {
        super.updateRows();

        connectionRow = addRow("connection");
        ipv6Row = addRow("ipv6");
        connection2Row = addRow();

        translatorRow = addRow("translator");
        translatorTypeRow = addRow("translatorType");
        if (NekoConfig.transType != NekoConfig.TRANS_TYPE_EXTERNAL) {
            showOriginalRow = NekoConfig.transType == NekoConfig.TRANS_TYPE_NEKO ? addRow("showOriginalRow") : -1;
            translationProviderRow = addRow("translationProvider");
            deepLFormalityRow = NekoConfig.translationProvider.equals(Translator.PROVIDER_DEEPL) ? addRow("deepLFormality") : -1;
            translationTargetRow = addRow("translationTarget");
            doNotTranslateRow = addRow("doNotTranslate");
            autoTranslateRow = addRow("autoTranslate");
        } else {
            showOriginalRow = -1;
            translationProviderRow = -1;
            deepLFormalityRow = -1;
            translationTargetRow = -1;
            doNotTranslateRow = -1;
            autoTranslateRow = -1;
        }
        translator2Row = addRow();

        notificationRow = addRow("notification");
        accentAsNotificationColorRow = addRow("accentAsNotificationColor");
        silenceNonContactsRow = addRow("silenceNonContacts");
        notification2Row = addRow();

        generalRow = addRow("general");
        disabledInstantCameraRow = addRow("disabledInstantCamera");
        askBeforeCallRow = addRow("askBeforeCall");
        openArchiveOnPullRow = addRow("openArchiveOnPull");
        nameOrderRow = addRow("nameOrder");
        idTypeRow = addRow("idType");
        general2Row = addRow();
    }

    @Override
    protected String getKey() {
        return "g";
    }

    private ArrayList<String> getRestrictedLanguages() {
        String currentLang = Translator.stripLanguageCode(Translator.getCurrentTargetLanguage());
        ArrayList<String> langCodes = new ArrayList<>(NekoConfig.restrictedLanguages);
        if (!langCodes.contains(currentLang)) {
            langCodes.add(currentLang);
        }
        return langCodes;
    }

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    textCell.setCanDisable(true);
                    textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
                    if (position == nameOrderRow) {
                        String value;
                        switch (NekoConfig.nameOrder) {
                            case 2:
                                value = LocaleController.getString("LastFirst", R.string.LastFirst);
                                break;
                            case 1:
                            default:
                                value = LocaleController.getString("FirstLast", R.string.FirstLast);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("NameOrder", R.string.NameOrder), value, partial, true);
                    } else if (position == translationProviderRow) {
                        Pair<ArrayList<String>, ArrayList<String>> providers = Translator.getProviders();
                        ArrayList<String> names = providers.first;
                        ArrayList<String> types = providers.second;
                        if (names == null || types == null) {
                            return;
                        }
                        int index = types.indexOf(NekoConfig.translationProvider);
                        if (index < 0) {
                            textCell.setTextAndValue(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), "", partial, true);
                        } else {
                            String value = names.get(index);
                            textCell.setTextAndValue(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), value, partial, true);
                        }
                    } else if (position == translationTargetRow) {
                        String language = NekoConfig.translationTarget;
                        CharSequence value;
                        if (language.equals("app")) {
                            value = LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp);
                        } else {
                            Locale locale = Locale.forLanguageTag(language);
                            if (!TextUtils.isEmpty(locale.getScript())) {
                                value = HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
                            } else {
                                value = locale.getDisplayName();
                            }
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslationTarget", R.string.TranslationTarget), value, partial, true);
                    } else if (position == deepLFormalityRow) {
                        String value;
                        switch (NekoConfig.deepLFormality) {
                            case DeepLTranslator.FORMALITY_DEFAULT:
                                value = LocaleController.getString("DeepLFormalityDefault", R.string.DeepLFormalityDefault);
                                break;
                            case DeepLTranslator.FORMALITY_MORE:
                                value = LocaleController.getString("DeepLFormalityMore", R.string.DeepLFormalityMore);
                                break;
                            case DeepLTranslator.FORMALITY_LESS:
                            default:
                                value = LocaleController.getString("DeepLFormalityLess", R.string.DeepLFormalityLess);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("DeepLFormality", R.string.DeepLFormality), value, partial, true);
                    } else if (position == idTypeRow) {
                        String value;
                        switch (NekoConfig.idType) {
                            case NekoConfig.ID_TYPE_HIDDEN:
                                value = LocaleController.getString("IdTypeHidden", R.string.IdTypeHidden);
                                break;
                            case NekoConfig.ID_TYPE_BOTAPI:
                                value = LocaleController.getString("IdTypeBOTAPI", R.string.IdTypeBOTAPI);
                                break;
                            case NekoConfig.ID_TYPE_API:
                            default:
                                value = LocaleController.getString("IdTypeAPI", R.string.IdTypeAPI);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("IdType", R.string.IdType), value, partial, false);
                    } else if (position == translatorTypeRow) {
                        String value;
                        switch (NekoConfig.transType) {
                            case NekoConfig.TRANS_TYPE_TG:
                                value = LocaleController.getString("TranslatorTypeTG", R.string.TranslatorTypeTG);
                                break;
                            case NekoConfig.TRANS_TYPE_EXTERNAL:
                                value = LocaleController.getString("TranslatorTypeExternal", R.string.TranslatorTypeExternal);
                                break;
                            case NekoConfig.TRANS_TYPE_NEKO:
                            default:
                                value = LocaleController.getString("TranslatorTypeNeko", R.string.TranslatorTypeNeko);
                                break;
                        }
                        textCell.setTextAndValue(LocaleController.getString("TranslatorType", R.string.TranslatorType), value, partial, position + 1 != translator2Row);
                    } else if (position == doNotTranslateRow) {
                        ArrayList<String> langCodes = getRestrictedLanguages();
                        CharSequence value;
                        if (langCodes.size() == 1) {
                            Locale locale = Locale.forLanguageTag(langCodes.get(0));
                            if (!TextUtils.isEmpty(locale.getScript())) {
                                value = HtmlCompat.fromHtml(locale.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY);
                            } else {
                                value = locale.getDisplayName();
                            }
                        } else {
                            value = LocaleController.formatPluralString("Languages", langCodes.size());
                        }
                        textCell.setTextAndValue(LocaleController.getString("DoNotTranslate", R.string.DoNotTranslate), value, partial, true);
                    }
                    break;
                }
                case TYPE_CHECK: {
                    TextCheckCell textCell = (TextCheckCell) holder.itemView;
                    textCell.setEnabled(true, null);
                    if (position == ipv6Row) {
                        textCell.setTextAndCheck(LocaleController.getString("IPv6", R.string.IPv6), NekoConfig.useIPv6, false);
                    } else if (position == disabledInstantCameraRow) {
                        textCell.setTextAndCheck(LocaleController.getString("DisableInstantCamera", R.string.DisableInstantCamera), NekoConfig.disableInstantCamera, true);
                    } else if (position == openArchiveOnPullRow) {
                        textCell.setTextAndCheck(LocaleController.getString("OpenArchiveOnPull", R.string.OpenArchiveOnPull), NekoConfig.openArchiveOnPull, true);
                    } else if (position == askBeforeCallRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AskBeforeCalling", R.string.AskBeforeCalling), NekoConfig.askBeforeCall, true);
                    } else if (position == accentAsNotificationColorRow) {
                        textCell.setTextAndCheck(LocaleController.getString("AccentAsNotificationColor", R.string.AccentAsNotificationColor), NekoConfig.accentAsNotificationColor, true);
                    } else if (position == silenceNonContactsRow) {
                        textCell.setTextAndCheck(LocaleController.getString("SilenceNonContacts", R.string.SilenceNonContacts), NekoConfig.silenceNonContacts, false);
                    } else if (position == autoTranslateRow) {
                        textCell.setEnabled(supportLanguageDetector, null);
                        textCell.setTextAndValueAndCheck(LocaleController.getString("AutoTranslate", R.string.AutoTranslate), LocaleController.getString("AutoTranslateAbout", R.string.AutoTranslateAbout), NekoConfig.autoTranslate, true, false);
                    } else if (position == showOriginalRow) {
                        textCell.setTextAndCheck(LocaleController.getString("TranslatorShowOriginal", R.string.TranslatorShowOriginal), NekoConfig.showOriginal, true);
                    }
                    break;
                }
                case TYPE_HEADER: {
                    HeaderCell headerCell = (HeaderCell) holder.itemView;
                    if (position == generalRow) {
                        headerCell.setText(LocaleController.getString("General", R.string.General));
                    } else if (position == connectionRow) {
                        headerCell.setText(LocaleController.getString("Connection", R.string.Connection));
                    } else if (position == notificationRow) {
                        headerCell.setText(LocaleController.getString("Notifications", R.string.Notifications));
                    } else if (position == translatorRow) {
                        headerCell.setText(LocaleController.getString("Translator", R.string.Translator));
                    }
                    break;
                }
                case TYPE_INFO_PRIVACY: {
                    TextInfoPrivacyCell cell = (TextInfoPrivacyCell) holder.itemView;
                    if (position == general2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("IdTypeAbout", R.string.IdTypeAbout));
                    } else if (position == notification2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("SilenceNonContactsAbout", R.string.SilenceNonContactsAbout));
                    } else if (position == translator2Row) {
                        cell.setBackground(Theme.getThemedDrawable(mContext, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow));
                        cell.setText(LocaleController.getString("TranslateMessagesInfo1", R.string.TranslateMessagesInfo1));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            if (position == autoTranslateRow || position == doNotTranslateRow) {
                return supportLanguageDetector;
            }
            return super.isEnabled(holder);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == connection2Row) {
                return TYPE_SHADOW;
            } else if (position == nameOrderRow || position == idTypeRow || position == translatorTypeRow ||
                    (position >= translationProviderRow && position <= doNotTranslateRow)) {
                return TYPE_SETTINGS;
            } else if (position == ipv6Row || position == autoTranslateRow ||
                    (position > generalRow && position < nameOrderRow) ||
                    (position > notificationRow && position < notification2Row) ||
                    position == showOriginalRow) {
                return TYPE_CHECK;
            } else if (position == generalRow || position == connectionRow || position == notificationRow ||
                    position == translatorRow) {
                return TYPE_HEADER;
            } else if (position == general2Row || position == notification2Row || position == translator2Row) {
                return TYPE_INFO_PRIVACY;
            }
            return TYPE_SETTINGS;
        }
    }
}
