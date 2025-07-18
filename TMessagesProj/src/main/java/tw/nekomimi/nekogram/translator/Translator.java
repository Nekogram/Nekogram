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
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.TranslateAlert2;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private static final LruCache<Pair<String, String>, Result> cache = new LruCache<>(200);

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
        var restrictedLanguages = getRestrictedLanguages();
        for (String language : restrictedLanguages) {
            if (language.equals(lang)) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<String> getRestrictedLanguages() {
        var languages = new ArrayList<String>();
        if (NekoConfig.restrictedLanguages == null) {
            languages.add(stripLanguageCode(getCurrentTargetLanguage()));
        } else {
            languages.addAll(NekoConfig.restrictedLanguages);
        }
        return languages;
    }

    public static void saveRestrictedLanguages(List<String> restrictedLanguages) {
        var currentTargetLanguage = stripLanguageCode(getCurrentTargetLanguage());
        var languages = restrictedLanguages.stream().filter(s -> !s.equals(currentTargetLanguage)).collect(Collectors.toSet());
        if (!restrictedLanguages.isEmpty() && languages.isEmpty()) {
            NekoConfig.saveRestrictedLanguages(null);
            return;
        }
        NekoConfig.saveRestrictedLanguages(new HashSet<>(restrictedLanguages));
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

    public static void translate(String query, TranslateController.PollText poll, String fl, String tl, TranslateCallBack translateCallBack) {
        var translator = getCurrentTranslator();

        var language = tl == null ? getCurrentTargetLanguage() : tl;

        if (!translator.supportLanguage(language)) {
            translateCallBack.onError(new UnsupportedTargetLanguageException());
        } else if (poll != null) {
            var task = PollTranslateTask.obtain(translator, poll, fl, language, translateCallBack);
            var future = getExecutorService().submit(task);
            Futures.addCallback(future, task, ContextCompat.getMainExecutor(ApplicationLoader.applicationContext));
        } else {
            var task = TranslateTask.obtain(translator, query, fl, language, translateCallBack);
            var future = getExecutorService().submit(task);
            Futures.addCallback(future, task, ContextCompat.getMainExecutor(ApplicationLoader.applicationContext));
        }
    }

    public static void translate(String query, String fl, TranslateCallBack translateCallBack) {
        translate(query, null, fl, null, translateCallBack);
    }

    public interface TranslateCallBack {
        default void onSuccess(String translation, TranslateController.PollText poll, String sourceLanguage, String targetLanguage) {
            onSuccess(translation, sourceLanguage, targetLanguage);
        }

        default void onSuccess(String translation, String sourceLanguage, String targetLanguage) {

        }

        void onError(Throwable t);
    }

    private static class UnsupportedTargetLanguageException extends IllegalArgumentException {
    }

    public static TLRPC.TL_textWithEntities getTLResult(String translation, String message, ArrayList<TLRPC.MessageEntity> entities) {
        TLRPC.TL_textWithEntities text = new TLRPC.TL_textWithEntities();
        text.text = translation;
        text.entities = new ArrayList<>();
        return text;
    }

    public static String normalizeLanguageCode(BaseTranslator translator, String language, String country) {
        String languageLowerCase = language.toLowerCase();
        String code;
        if (!TextUtils.isEmpty(country)) {
            String countryUpperCase = language.equals("zh") ? normalizeCountry(country) : country.toUpperCase();
            if (translator.supportLanguage(languageLowerCase + "-" + countryUpperCase)) {
                code = languageLowerCase + "-" + countryUpperCase;
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    private static String normalizeCountry(String country) {
        var countryUpperCase = country.toUpperCase();
        if (countryUpperCase.equals("DG")) {
            return "CN";
        } else if (countryUpperCase.equals("HK")) {
            return "TW";
        } else {
            return countryUpperCase;
        }
    }

    private static String getCurrentAppLanguage(BaseTranslator translator) {
        String toLang;
        Locale locale = LocaleController.getInstance().getCurrentLocale();
        toLang = normalizeLanguageCode(translator, locale.getLanguage(), locale.getCountry());
        if (!translator.supportLanguage(toLang)) {
            toLang = normalizeLanguageCode(translator, LocaleController.getString(R.string.LanguageCode), null);
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

    private record PollTranslateTask(BaseTranslator translator, TranslateController.PollText query,
                                     String fl,
                                     String tl,
                                     TranslateCallBack translateCallBack) implements Callable<Pair<TranslateController.PollText, String>>,
            FutureCallback<Pair<TranslateController.PollText, String>> {

        @Override
        public Pair<TranslateController.PollText, String> call() throws Exception {
            var translated = new TranslateController.PollText();
            String sourceLanguage = null;
            if (query.question != null) {
                var result = TranslateTask.obtain(translator, query.question.text, fl, tl, null).call();
                translated.question = getTLResult(result.translation, query.question.text, query.question.entities);
                sourceLanguage = result.sourceLanguage;
            }
            for (var answer : query.answers) {
                var result = TranslateTask.obtain(translator, answer.text.text, fl, tl, null).call();
                var resultAnswer = new TLRPC.TL_pollAnswer();
                resultAnswer.text = getTLResult(result.translation, answer.text.text, answer.text.entities);
                resultAnswer.option = answer.option;
                translated.answers.add(resultAnswer);
                if (sourceLanguage == null) sourceLanguage = result.sourceLanguage;
            }
            if (query.solution != null) {
                var result = TranslateTask.obtain(translator, query.solution.text, fl, tl, null).call();
                translated.solution = getTLResult(result.translation, query.question.text, query.question.entities);
                if (sourceLanguage == null) sourceLanguage = result.sourceLanguage;
            }
            return Pair.create(translated, sourceLanguage);
        }

        public static PollTranslateTask obtain(BaseTranslator translator, TranslateController.PollText query, String fl, String tl, TranslateCallBack callback) {
            return new PollTranslateTask(translator, query, fl, tl, callback);
        }

        @Override
        public void onSuccess(Pair<TranslateController.PollText, String> result) {
            translateCallBack.onSuccess(null, result.first, result.second == null ? fl : result.second, tl);
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            translateCallBack.onError(t);
        }
    }

    private record TranslateTask(BaseTranslator translator, String query, String fl,
                                 String tl,
                                 TranslateCallBack translateCallBack) implements Callable<Result>, FutureCallback<Result> {

        @Override
        public Result call() throws Exception {
            var key = Pair.create(query, tl + "|" + NekoConfig.translationProvider);
            var cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            var result = translator.translate(query, null, tl);
            cache.put(key, result);
            return result;
        }

        @Override
        public void onSuccess(Result result) {
            translateCallBack.onSuccess(result.translation, null, result.sourceLanguage == null ? fl : result.sourceLanguage, tl);
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            translateCallBack.onError(t);
        }

        public static TranslateTask obtain(BaseTranslator translator, String query, String fl, String tl, TranslateCallBack callback) {
            return new TranslateTask(translator, query, fl, tl, callback);
        }
    }
}
