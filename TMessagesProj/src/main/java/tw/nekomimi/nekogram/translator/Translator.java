package tw.nekomimi.nekogram.translator;

import android.content.Context;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.core.util.Pair;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TranslateAlert2;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import app.nekogram.translator.BaiduTranslator;
import app.nekogram.translator.BaseTranslator;
import app.nekogram.translator.DeepLTranslator;
import app.nekogram.translator.GoogleAppTranslator;
import app.nekogram.translator.Http429Exception;
import app.nekogram.translator.LingoTranslator;
import app.nekogram.translator.MicrosoftTranslator;
import app.nekogram.translator.Result;
import app.nekogram.translator.SogouTranslator;
import app.nekogram.translator.TranSmartTranslator;
import app.nekogram.translator.YandexTranslator;
import app.nekogram.translator.YouDaoTranslator;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.settings.NekoLanguagesSelectActivity;

public class Translator {

    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_YANDEX = "yandex";
    public static final String PROVIDER_MICROSOFT = "microsoft";
    public static final String PROVIDER_DEEPL = "deepl";
    public static final String PROVIDER_LINGO = "lingo";
    public static final String PROVIDER_YOUDAO = "youdao";
    public static final String PROVIDER_BAIDU = "baidu";
    public static final String PROVIDER_SOGOU = "sogou";
    public static final String PROVIDER_TENCENT = "tencent";

    private static final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    private static final LruCache<Pair<String, String>, Pair<String, String>> cache = new LruCache<>(200);

    public static ListeningExecutorService getExecutorService() {
        return executorService;
    }

    public static void showTranslateDialog(Context context, String query, boolean noforwards, BaseFragment fragment, Utilities.CallbackReturn<URLSpan, Boolean> onLinkPress, String sourceLanguage, View anchorView, Theme.ResourcesProvider resourcesProvider) {
        if (NekoConfig.transType == NekoConfig.TRANS_TYPE_EXTERNAL) {
            TranslatorApps.showExternalTranslateDialog(context, query, sourceLanguage, anchorView, resourcesProvider);
        } else {
            TranslateAlert2.showAlert(context, fragment, UserConfig.selectedAccount, sourceLanguage, NekoConfig.translationTarget, query, null, noforwards, onLinkPress, null, resourcesProvider);
        }
    }

    public static void handleTranslationError(Context context, final Throwable t, final Runnable onRetry, Theme.ResourcesProvider resourcesProvider) {
        if (context == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        if (t instanceof UnsupportedTargetLanguageException) {
            builder.setMessage(LocaleController.getString(R.string.TranslateApiUnsupported));
            builder.setPositiveButton(LocaleController.getString(R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(context, null, null, resourcesProvider));
        } else {
            if (t instanceof Http429Exception) {
                builder.setTitle(LocaleController.getString(R.string.TranslateFailed));
                builder.setMessage(LocaleController.getString(R.string.FloodWait));
            } else if (t != null && t.getLocalizedMessage() != null) {
                builder.setTitle(LocaleController.getString(R.string.TranslateFailed));
                builder.setMessage(t.getLocalizedMessage());
            } else {
                builder.setMessage(LocaleController.getString(R.string.TranslateFailed));
            }
            if (onRetry != null) {
                builder.setPositiveButton(LocaleController.getString(R.string.Retry), (dialog, which) -> onRetry.run());
            }
            builder.setNeutralButton(LocaleController.getString(R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(context, null, null, resourcesProvider));
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
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
        String toLang = stripLanguageCode(getCurrentTargetLanguage());
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
        names.add(LocaleController.getString(R.string.ProviderGoogleTranslate));
        types.add(PROVIDER_GOOGLE);
        names.add(LocaleController.getString(R.string.ProviderYandex));
        types.add(PROVIDER_YANDEX);
        names.add(LocaleController.getString(R.string.ProviderMicrosoftTranslator));
        types.add(PROVIDER_MICROSOFT);
        names.add(LocaleController.getString(R.string.ProviderDeepLTranslate));
        types.add(PROVIDER_DEEPL);
        names.add(LocaleController.getString(R.string.ProviderLingocloud));
        types.add(PROVIDER_LINGO);
        names.add(LocaleController.getString(R.string.ProviderYouDaoTranslate));
        types.add(PROVIDER_YOUDAO);
        names.add(LocaleController.getString(R.string.ProviderBaiduTranslate));
        types.add(PROVIDER_BAIDU);
        names.add(LocaleController.getString(R.string.ProviderSogouTranslate));
        types.add(PROVIDER_SOGOU);
        names.add(LocaleController.getString(R.string.ProviderTencentTranslator));
        types.add(PROVIDER_TENCENT);
        return new Pair<>(names, types);
    }

    public static void showTranslationTargetSelector(BaseFragment fragment, View view, Runnable callback, boolean whiteActionBar, Theme.ResourcesProvider resourcesProvider) {
        if (getCurrentTranslator().getTargetLanguages().size() <= 30) {
            ArrayList<String> targetLanguages = new ArrayList<>(getCurrentTranslator().getTargetLanguages());
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
            names.add(0, LocaleController.getString(R.string.TranslationTargetApp));

            PopupHelper.show(names, LocaleController.getString(R.string.TranslationTarget), targetLanguages.indexOf(NekoConfig.translationTarget), fragment.getParentActivity(), view, i -> {
                NekoConfig.setTranslationTarget(targetLanguages.get(i));
                if (callback != null) callback.run();
            }, resourcesProvider);
        } else {
            fragment.presentFragment(new NekoLanguagesSelectActivity(NekoLanguagesSelectActivity.TYPE_TARGET, whiteActionBar));
        }
    }

    public static void showTranslatorTypeSelector(Context context, View view, Runnable callback, Theme.ResourcesProvider resourcesProvider) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        arrayList.add(LocaleController.getString(R.string.TranslatorTypeNeko));
        types.add(NekoConfig.TRANS_TYPE_NEKO);
        arrayList.add(LocaleController.getString(R.string.TranslatorTypeTG));
        types.add(NekoConfig.TRANS_TYPE_TG);
        arrayList.add(LocaleController.getString(R.string.TranslatorTypeExternal));
        types.add(NekoConfig.TRANS_TYPE_EXTERNAL);
        PopupHelper.show(arrayList, LocaleController.getString(R.string.TranslatorType), types.indexOf(NekoConfig.transType), context, view, i -> {
            NekoConfig.setTransType(types.get(i));
            if (callback != null) callback.run();
        }, resourcesProvider);
    }

    public static void showTranslationProviderSelector(Context context, View view, MessagesStorage.BooleanCallback callback, Theme.ResourcesProvider resourcesProvider) {
        if (NekoConfig.transType == NekoConfig.TRANS_TYPE_EXTERNAL) {
            var app = TranslatorApps.getTranslatorApp();
            var apps = TranslatorApps.getTranslatorApps();
            if (apps.isEmpty()) {
                return;
            }
            var list = apps.stream().map(a -> a.title).collect(Collectors.toList());
            PopupHelper.show(list, LocaleController.getString(R.string.TranslationProvider), apps.indexOf(app), context, view, i -> {
                TranslatorApps.setTranslatorApp(apps.get(i));
                if (callback != null) callback.run(true);
            }, resourcesProvider);
            return;
        }
        Pair<ArrayList<String>, ArrayList<String>> providers = getProviders();
        ArrayList<String> names = providers.first;
        ArrayList<String> types = providers.second;
        if (names == null || types == null) {
            return;
        }
        PopupHelper.show(names, LocaleController.getString(R.string.TranslationProvider), types.indexOf(NekoConfig.translationProvider), context, view, i -> {
            BaseTranslator translator = getTranslator(types.get(i));
            String targetLanguage = getTargetLanguage(translator, NekoConfig.translationTarget);

            if (translator.supportLanguage(targetLanguage)) {
                NekoConfig.setTranslationProvider(types.get(i));
                if (callback != null) callback.run(true);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider)
                        .setMessage(LocaleController.getString(R.string.TranslateApiUnsupported));
                if ("app".equals(NekoConfig.translationTarget)) {
                    builder.setPositiveButton(LocaleController.getString(R.string.UseGoogleTranslate), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(PROVIDER_GOOGLE);
                        if (callback != null) callback.run(true);
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                } else if (translator.supportLanguage(getCurrentAppLanguage(translator))) {
                    builder.setPositiveButton(LocaleController.getString(R.string.ResetLanguage), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(types.get(i));
                        NekoConfig.setTranslationTarget("app");
                        if (callback != null) callback.run(false);
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                } else {
                    builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                }
                builder.show();
            }
        }, resourcesProvider);
    }

    public static BaseTranslator getCurrentTranslator() {
        return getTranslator(NekoConfig.translationProvider);
    }

    private static BaseTranslator getTranslator(String type) {
        return switch (type) {
            case PROVIDER_YANDEX -> YandexTranslator.getInstance();
            case PROVIDER_LINGO -> LingoTranslator.getInstance();
            case PROVIDER_DEEPL -> {
                DeepLTranslator.setFormality(NekoConfig.deepLFormality);
                yield DeepLTranslator.getInstance();
            }
            case PROVIDER_MICROSOFT -> MicrosoftTranslator.getInstance();
            case PROVIDER_YOUDAO -> YouDaoTranslator.getInstance();
            case PROVIDER_BAIDU -> BaiduTranslator.getInstance();
            case PROVIDER_SOGOU -> SogouTranslator.getInstance();
            case PROVIDER_TENCENT -> TranSmartTranslator.getInstance();
            default -> GoogleAppTranslator.getInstance();
        };
    }

    public static void translate(String query, String fl, String tl, TranslateCallBack translateCallBack) {
        BaseTranslator translator = getCurrentTranslator();

        String language = tl == null ? getCurrentTargetLanguage() : tl;

        if (!translator.supportLanguage(language)) {
            translateCallBack.onError(new UnsupportedTargetLanguageException());
        } else {
            startTask(translator, query, fl, language, translateCallBack);
        }
    }

    public static void translate(String query, String fl, TranslateCallBack translateCallBack) {
        translate(query, fl, null, translateCallBack);
    }

    public interface TranslateCallBack {
        void onSuccess(String translation, String sourceLanguage, String targetLanguage);

        void onError(Throwable t);
    }

    private static class UnsupportedTargetLanguageException extends IllegalArgumentException {
    }

    private static void startTask(BaseTranslator translator, String query, String fromLang, String toLang, TranslateCallBack translateCallBack) {
        var result = cache.get(Pair.create(query, toLang + "|" + NekoConfig.translationProvider));
        if (result != null) {
            translateCallBack.onSuccess(result.first, result.second == null ? fromLang : translator.convertLanguageCode(result.second, true), toLang);
        } else {
            TranslateTask translateTask = new TranslateTask(translator, query, fromLang, toLang, translateCallBack);
            ListenableFuture<Pair<String, String>> future = getExecutorService().submit(translateTask);
            Futures.addCallback(future, translateTask, ContextCompat.getMainExecutor(ApplicationLoader.applicationContext));
        }
    }

    public static TLRPC.TL_textWithEntities getTLResult(String translation, String message, ArrayList<TLRPC.MessageEntity> entities) {
        TLRPC.TL_textWithEntities text = new TLRPC.TL_textWithEntities();
        text.text = translation;
        text.entities = new ArrayList<>();
        return text;
    }

    private static String getCurrentAppLanguage(BaseTranslator translator) {
        String toLang;
        Locale locale = LocaleController.getInstance().getCurrentLocale();
        toLang = translator.convertLanguageCode(locale.getLanguage(), locale.getCountry());
        if (!translator.supportLanguage(toLang)) {
            toLang = translator.convertLanguageCode(LocaleController.getString(R.string.LanguageCode), null);
        }
        return toLang;
    }

    private static String getTargetLanguage(BaseTranslator translator, String language) {
        String toLang;
        if (language.equals("app")) {
            toLang = getCurrentAppLanguage(translator);
        } else {
            toLang = language;
        }
        return toLang;
    }

    public static String getTargetLanguage(String language) {
        return getTargetLanguage(getCurrentTranslator(), language);
    }

    public static String getCurrentTargetLanguage() {
        return getTargetLanguage(getCurrentTranslator(), NekoConfig.translationTarget);
    }

    private static class TranslateTask implements Callable<Pair<String, String>>, FutureCallback<Pair<String, String>> {
        private final TranslateCallBack translateCallBack;
        private final BaseTranslator translator;
        private final String query;
        private final String fl;
        private final String tl;

        public TranslateTask(BaseTranslator translator, String query, String fl, String tl, TranslateCallBack translateCallBack) {
            this.translator = translator;
            this.query = query;
            this.fl = fl;
            this.tl = tl;
            this.translateCallBack = translateCallBack;
        }

        @Override
        public Pair<String, String> call() throws Exception {
            var from = "";//translator.convertLanguageCode(TextUtils.isEmpty(fl) || "und".equals(fl) ? "auto" : fl, false);
            var to = translator.convertLanguageCode(tl, false);
            Result result = translator.translate(query, from, to);
            return Pair.create(result.translation, result.sourceLanguage);
        }

        @Override
        public void onSuccess(Pair<String, String> result) {
            translateCallBack.onSuccess(result.first, result.second == null ? fl : translator.convertLanguageCode(result.second, true), tl);
            cache.put(Pair.create(query, tl + "|" + NekoConfig.translationProvider), result);
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            translateCallBack.onError(t);
        }
    }
}
