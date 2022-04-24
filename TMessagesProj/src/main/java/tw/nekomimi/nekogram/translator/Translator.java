package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.View;

import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TranslateAlert;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.settings.NekoLanguagesSelectActivity;

public class Translator {

    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_LINGO = "lingo";
    public static final String PROVIDER_YANDEX = "yandex";
    public static final String PROVIDER_DEEPL = "deepl";
    public static final String PROVIDER_MICROSOFT = "microsoft";
    public static final String PROVIDER_YOUDAO = "youdao";

    public static void showTranslateDialog(Context context, String query, boolean noforwards) {
        showTranslateDialog(context, query, noforwards, null, null, null);
    }

    public static void showTranslateDialog(Context context, String query, boolean noforwards, BaseFragment fragment, TranslateAlert.OnLinkPress onLinkPress, String sourceLanguage) {
        if (NekoConfig.transType == NekoConfig.TRANS_TYPE_EXTERNAL) {
            Translator.startExternalTranslator(context, query);
        } else {
            TranslateAlert.showAlert(context, fragment, sourceLanguage, NekoConfig.translationTarget, query, noforwards, onLinkPress, null);
        }
    }

    public static void handleTranslationError(Context context, final Exception e, final Runnable onRetry, Theme.ResourcesProvider resourcesProvider) {
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        if (e instanceof UnsupportedTargetLanguageException) {
            builder.setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported));
            builder.setPositiveButton(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(context, null, null, resourcesProvider));
        } else {
            if (e instanceof BaseTranslator.Http429Exception) {
                builder.setTitle(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
                builder.setMessage(LocaleController.getString("FloodWait", R.string.FloodWait));
            } else if (e != null && e.getLocalizedMessage() != null) {
                builder.setTitle(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
                builder.setMessage(e.getLocalizedMessage());
            } else {
                builder.setMessage(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
            }
            if (onRetry != null) {
                builder.setPositiveButton(LocaleController.getString("Retry", R.string.Retry), (dialog, which) -> onRetry.run());
            }
            builder.setNeutralButton(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(context, null, null, resourcesProvider));
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    public static String stripLanguageCode(String language) {
        if (language.contains("-")) {
            return language.substring(0, language.indexOf("-"));
        }
        return language;
    }

    public static boolean isLanguageRestricted(String lang) {
        if (lang == null || lang.equals("und")) {
            return false;
        }
        String toLang = stripLanguageCode(Translator.getCurrentTranslator().getCurrentTargetLanguage());
        lang = stripLanguageCode(lang);
        if (lang.equals(toLang)) {
            return true;
        }
        boolean restricted = false;
        for (String language : NekoConfig.restrictedLanguages) {
            if (language.contains("_")) {
                language = language.substring(0, language.indexOf("_"));
            }
            if (language.equals(lang)) {
                restricted = true;
                break;
            }
        }
        return restricted;
    }

    public static Pair<ArrayList<String>, ArrayList<String>> getProviders() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> types = new ArrayList<>();
        names.add(LocaleController.getString("ProviderGoogleTranslate", R.string.ProviderGoogleTranslate));
        types.add(Translator.PROVIDER_GOOGLE);
        names.add(LocaleController.getString("ProviderLingocloud", R.string.ProviderLingocloud));
        types.add(Translator.PROVIDER_LINGO);
        names.add(LocaleController.getString("ProviderYandex", R.string.ProviderYandex));
        types.add(Translator.PROVIDER_YANDEX);
        names.add(LocaleController.getString("ProviderDeepLTranslate", R.string.ProviderDeepLTranslate));
        types.add(Translator.PROVIDER_DEEPL);
        names.add(LocaleController.getString("ProviderMicrosoftTranslator", R.string.ProviderMicrosoftTranslator));
        types.add(Translator.PROVIDER_MICROSOFT);
        names.add(LocaleController.getString("ProviderYouDaoTranslate", R.string.ProviderYouDaoTranslate));
        types.add(Translator.PROVIDER_YOUDAO);
        return new Pair<>(names, types);
    }

    public static void showTranslationTargetSelector(BaseFragment fragment, View view, Runnable callback) {
        showTranslationTargetSelector(fragment, view, callback, true, null);
    }

    public static void showTranslationTargetSelector(BaseFragment fragment, View view, Runnable callback, boolean whiteActionBar, Theme.ResourcesProvider resourcesProvider) {
        if (Translator.getCurrentTranslator().getTargetLanguages().size() <= 30) {
            ArrayList<String> targetLanguages = new ArrayList<>(Translator.getCurrentTranslator().getTargetLanguages());
            ArrayList<CharSequence> names = new ArrayList<>();
            for (String language : targetLanguages) {
                Locale locale = Locale.forLanguageTag(language);
                if (!TextUtils.isEmpty(locale.getScript())) {
                    names.add(HtmlCompat.fromHtml(String.format("%s - %s", locale.getDisplayScript(), locale.getDisplayScript(locale)), HtmlCompat.FROM_HTML_MODE_LEGACY));
                } else {
                    names.add(String.format("%s - %s", locale.getDisplayName(), locale.getDisplayName(locale)));
                }
            }
            targetLanguages.add(0, "app");
            names.add(0, LocaleController.getString("TranslationTargetApp", R.string.TranslationTargetApp));

            PopupHelper.show(names, LocaleController.getString("TranslationTarget", R.string.TranslationTarget), targetLanguages.indexOf(NekoConfig.translationTarget), fragment.getParentActivity(), view, i -> {
                NekoConfig.setTranslationTarget(targetLanguages.get(i));
                if (callback != null) callback.run();
            }, resourcesProvider);
        } else {
            fragment.presentFragment(new NekoLanguagesSelectActivity(NekoLanguagesSelectActivity.TYPE_TARGET, whiteActionBar));
        }
    }

    public static void showTranslatorTypeSelector(Context context, View view, Runnable callback) {
        showTranslatorTypeSelector(context, view, callback, null);
    }

    public static void showTranslatorTypeSelector(Context context, View view, Runnable callback, Theme.ResourcesProvider resourcesProvider) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        arrayList.add(LocaleController.getString("TranslatorTypeNeko", R.string.TranslatorTypeNeko));
        types.add(NekoConfig.TRANS_TYPE_NEKO);
        arrayList.add(LocaleController.getString("TranslatorTypeTG", R.string.TranslatorTypeTG));
        types.add(NekoConfig.TRANS_TYPE_TG);
        arrayList.add(LocaleController.getString("TranslatorTypeExternal", R.string.TranslatorTypeExternal));
        types.add(NekoConfig.TRANS_TYPE_EXTERNAL);
        PopupHelper.show(arrayList, LocaleController.getString("TranslatorType", R.string.TranslatorType), types.indexOf(NekoConfig.transType), context, view, i -> {
            NekoConfig.setTransType(types.get(i));
            if (callback != null) callback.run();
        });
    }

    public static void showTranslationProviderSelector(Context context, View view, MessagesStorage.BooleanCallback callback) {
        showTranslationProviderSelector(context, view, callback, null);
    }

    public static void showTranslationProviderSelector(Context context, View view, MessagesStorage.BooleanCallback callback, Theme.ResourcesProvider resourcesProvider) {
        Pair<ArrayList<String>, ArrayList<String>> providers = getProviders();
        ArrayList<String> names = providers.first;
        ArrayList<String> types = providers.second;
        if (names == null || types == null) {
            return;
        }
        PopupHelper.show(names, LocaleController.getString("TranslationProvider", R.string.TranslationProvider), types.indexOf(NekoConfig.translationProvider), context, view, i -> {
            BaseTranslator translator = getTranslator(types.get(i));
            String targetLanguage = translator.getTargetLanguage(NekoConfig.translationTarget);

            if (translator.supportLanguage(targetLanguage)) {
                NekoConfig.setTranslationProvider(types.get(i));
                if (callback != null) callback.run(true);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider)
                        .setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported));
                if ("app".equals(NekoConfig.translationTarget)) {
                    builder.setPositiveButton(LocaleController.getString("UseGoogleTranslate", R.string.UseGoogleTranslate), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(Translator.PROVIDER_GOOGLE);
                        if (callback != null) callback.run(true);
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                } else if (translator.supportLanguage(translator.getCurrentAppLanguage())) {
                    builder.setPositiveButton(LocaleController.getString("ResetLanguage", R.string.ResetLanguage), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(types.get(i));
                        NekoConfig.setTranslationTarget("app");
                        if (callback != null) callback.run(false);
                    });
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                } else {
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                }
                builder.show();
            }
        }, resourcesProvider);
    }

    public static BaseTranslator getCurrentTranslator() {
        return getTranslator(NekoConfig.translationProvider);
    }

    public static BaseTranslator getTranslator(String type) {
        switch (type) {
            case PROVIDER_YANDEX:
                return YandexTranslator.getInstance();
            case PROVIDER_LINGO:
                return LingoTranslator.getInstance();
            case PROVIDER_DEEPL:
                return DeepLTranslator.getInstance();
            case PROVIDER_MICROSOFT:
                return MicrosoftTranslator.getInstance();
            case PROVIDER_YOUDAO:
                return YouDaoTranslator.getInstance();
            case PROVIDER_GOOGLE:
            default:
                return GoogleAppTranslator.getInstance();
        }
    }

    public static void translate(Object query, TranslateCallBack translateCallBack) {
        translate(query, null, translateCallBack);
    }

    public static void translate(Object query, String tl, TranslateCallBack translateCallBack) {
        BaseTranslator translator = getCurrentTranslator();

        String language = tl != null ? tl : translator.getCurrentTargetLanguage();

        if (!translator.supportLanguage(language)) {
            translateCallBack.onError(new UnsupportedTargetLanguageException());
        } else {
            translator.startTask(query, language, translateCallBack);
        }
    }

    public static void startExternalTranslator(Context context, String text) {
        @SuppressLint("InlinedApi") var intent = new Intent(Intent.ACTION_TRANSLATE);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            new AlertDialog.Builder(context)
                    .setTitle(LocaleController.getString("AppName", R.string.AppName))
                    .setMessage(LocaleController.getString("NoTranslatorAppInstalled", R.string.NoTranslatorAppInstalled))
                    .show();
        }
    }

    public interface TranslateCallBack {
        void onSuccess(Object translation, String sourceLanguage);

        void onError(Exception e);
    }

    private static class UnsupportedTargetLanguageException extends IllegalArgumentException {
    }


}
