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

import org.apache.commons.lang3.function.BooleanConsumer;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLObject;
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

import app.nekogram.translator.Http429Exception;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;
import tw.nekomimi.nekogram.settings.NekoLanguagesSelectActivity;

public class Translator {

    public static final String PROVIDER_GOOGLE = "google";
    public static final String PROVIDER_YANDEX = "yandex";
    public static final String PROVIDER_MICROSOFT = "microsoft";
    public static final String PROVIDER_DEEPL = "deepl";
    public static final String PROVIDER_TELEGRAM = "telegram";
    public static final String PROVIDER_LINGO = "lingo";
    public static final String PROVIDER_YOUDAO = "youdao";
    public static final String PROVIDER_BAIDU = "baidu";
    public static final String PROVIDER_SOGOU = "sogou";
    public static final String PROVIDER_TENCENT = "tencent";

    public static final String TRANSLATION_SEPARATOR = "\n--------\n";

    private static final ListeningExecutorService executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    private static final LruCache<Pair<String, String>, TranslationResult> cache = new LruCache<>(200);

    public static ListeningExecutorService getExecutorService() {
        return executorService;
    }

    public static void showTranslateDialog(Context context, String query, boolean noforwards, BaseFragment fragment, Utilities.CallbackReturn<URLSpan, Boolean> onLinkPress, String sourceLanguage, View anchorView, Theme.ResourcesProvider resourcesProvider) {
        showTranslateDialog(context, query, null, noforwards, fragment, onLinkPress, sourceLanguage, anchorView, resourcesProvider);
    }

    public static void showTranslateDialog(Context context, String query, ArrayList<TLRPC.MessageEntity> entities, boolean noforwards, BaseFragment fragment, Utilities.CallbackReturn<URLSpan, Boolean> onLinkPress, String sourceLanguage, View anchorView, Theme.ResourcesProvider resourcesProvider) {
        if (NekoConfig.transType == NekoConfig.TRANS_TYPE_EXTERNAL) {
            TranslatorApps.showExternalTranslateDialog(context, query, sourceLanguage, anchorView, resourcesProvider);
        } else {
            TranslateAlert2.showAlert(context, fragment, UserConfig.selectedAccount, sourceLanguage, NekoConfig.translationTarget, query, entities, noforwards, onLinkPress, null, resourcesProvider);
        }
    }

    public static void handleTranslationError(BaseFragment fragment, String t, final Runnable onRetry) {
        var exception = "QUOTA_EXCEEDED".equals(t) ? new Http429Exception() : new RuntimeException(t);
        handleTranslationError(fragment, exception, onRetry);
    }

    public static void handleTranslationError(BaseFragment fragment, final Throwable t, final Runnable onRetry) {
        var context = fragment.getParentActivity();
        if (context == null) return;
        var resourcesProvider = fragment.getResourceProvider();
        var builder = new AlertDialog.Builder(context, resourcesProvider);
        if (t instanceof UnsupportedTargetLanguageException) {
            builder.setMessage(LocaleController.getString(R.string.TranslateApiUnsupported));
            builder.setPositiveButton(LocaleController.getString(R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(fragment, null, null));
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
            builder.setNeutralButton(LocaleController.getString(R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(fragment, null, null));
        }
        builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        fragment.showDialog(builder.create());
    }

    public static void handleTranslationError(Context context, final Throwable t, final Runnable onRetry, Theme.ResourcesProvider resourcesProvider) {
        if (context == null) return;
        var builder = new AlertDialog.Builder(context, resourcesProvider);
        if (t instanceof UnsupportedTargetLanguageException) {
            builder.setMessage(LocaleController.getString(R.string.TranslateApiUnsupported));
            builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
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
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        }
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
            NekoConfig.setRestrictedLanguages(null);
            return;
        }
        NekoConfig.setRestrictedLanguages(new HashSet<>(restrictedLanguages));
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
        names.add("Telegram");
        types.add(PROVIDER_TELEGRAM);
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

    public static void showTranslationTargetSelector(BaseFragment fragment, View view, Runnable callback) {
        if (getCurrentTargetLanguages().size() <= 30) {
            ArrayList<String> targetLanguages = new ArrayList<>(getCurrentTargetLanguages());
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

            PopupHelper.show(names, LocaleController.getString(R.string.TranslationTarget), targetLanguages.indexOf(NekoConfig.translationTarget), fragment, view, i -> {
                NekoConfig.setTranslationTarget(targetLanguages.get(i));
                if (callback != null) callback.run();
            });
        } else {
            fragment.presentFragment(new NekoLanguagesSelectActivity(NekoLanguagesSelectActivity.TYPE_TARGET));
        }
    }

    public static void showTranslatorTypeSelector(BaseFragment fragment, View view, Runnable callback) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        arrayList.add(LocaleController.getString(R.string.TranslatorTypeNeko));
        types.add(NekoConfig.TRANS_TYPE_NEKO);
        arrayList.add(LocaleController.getString(R.string.TranslatorTypeTG));
        types.add(NekoConfig.TRANS_TYPE_TG);
        arrayList.add(LocaleController.getString(R.string.TranslatorTypeExternal));
        types.add(NekoConfig.TRANS_TYPE_EXTERNAL);
        PopupHelper.show(arrayList, LocaleController.getString(R.string.TranslatorType), types.indexOf(NekoConfig.transType), fragment, view, i -> {
            NekoConfig.setTransType(types.get(i));
            if (callback != null) callback.run();
        });
    }

    public static void showTranslationProviderSelector(BaseFragment fragment, View view, BooleanConsumer callback) {
        if (NekoConfig.transType == NekoConfig.TRANS_TYPE_EXTERNAL) {
            var app = TranslatorApps.getTranslatorApp();
            var apps = TranslatorApps.getTranslatorApps();
            if (apps.isEmpty()) {
                return;
            }
            var list = apps.stream().map(a -> a.title).collect(Collectors.toList());
            PopupHelper.show(list, LocaleController.getString(R.string.TranslationProvider), apps.indexOf(app), fragment, view, i -> {
                TranslatorApps.setTranslatorApp(apps.get(i));
                if (callback != null) callback.accept(true);
            });
            return;
        }
        var providers = getProviders();
        var names = providers.first;
        var types = providers.second;
        if (names == null || types == null) {
            return;
        }
        PopupHelper.show(names, LocaleController.getString(R.string.TranslationProvider), types.indexOf(NekoConfig.translationProvider), fragment, view, i -> {
            var translator = getTranslator(types.get(i));
            String targetLanguage = getTargetLanguage(translator, NekoConfig.translationTarget);

            if (translator.supportLanguage(targetLanguage)) {
                NekoConfig.setTranslationProvider(types.get(i));
                if (callback != null) callback.accept(true);
            } else {
                var context = fragment.getParentActivity();
                var resourcesProvider = fragment.getResourceProvider();
                var builder = new AlertDialog.Builder(context, resourcesProvider)
                        .setMessage(LocaleController.getString(R.string.TranslateApiUnsupported));
                if ("app".equals(NekoConfig.translationTarget)) {
                    builder.setPositiveButton(LocaleController.getString(R.string.UseGoogleTranslate), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(PROVIDER_GOOGLE);
                        if (callback != null) callback.accept(true);
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                } else if (translator.supportLanguage(getCurrentAppLanguage(translator))) {
                    builder.setPositiveButton(LocaleController.getString(R.string.ResetLanguage), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(types.get(i));
                        NekoConfig.setTranslationTarget("app");
                        if (callback != null) callback.accept(false);
                    });
                    builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
                } else {
                    builder.setPositiveButton(LocaleController.getString(R.string.OK), null);
                }
                fragment.showDialog(builder.create());
            }
        });
    }

    public static List<String> getCurrentTargetLanguages() {
        return getCurrentTranslator().getTargetLanguages();
    }

    private static ITranslator getCurrentTranslator() {
        return getTranslator(NekoConfig.translationProvider);
    }

    private static ITranslator getTranslator(String type) {
        if (PROVIDER_TELEGRAM.equals(type)) {
            return TelegramTranslator.getInstance();
        }
        return TextWithEntitiesTranslator.of(type);
    }

    public static void translate(String text, ArrayList<TLRPC.MessageEntity> entities, TranslateController.PollText poll, String fl, String tl, TranslateCallBack translateCallBack) {
        translate(textWithEntities(text, entities), poll, fl, tl, translateCallBack);
    }

    public static void translate(TLRPC.TL_textWithEntities query, TranslateController.PollText poll, String fl, String tl, TranslateCallBack translateCallBack) {
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

    public interface TranslateCallBack {
        default void onSuccess(TLRPC.TL_textWithEntities translation, TranslateController.PollText poll, String sourceLanguage, String targetLanguage) {
            onSuccess(translation, sourceLanguage, targetLanguage);
        }

        default void onSuccess(TLRPC.TL_textWithEntities translation, String sourceLanguage, String targetLanguage) {

        }

        void onError(Throwable t);
    }

    private static class UnsupportedTargetLanguageException extends IllegalArgumentException {
    }

    public static TLRPC.TL_textWithEntities textWithEntities(String text, ArrayList<TLRPC.MessageEntity> entities) {
        var textWithEntities = new TLRPC.TL_textWithEntities();
        textWithEntities.text = text;
        if (entities != null) textWithEntities.entities = entities;
        return textWithEntities;
    }

    public static ArrayList<TLRPC.MessageEntity> mergeEntities(ArrayList<TLRPC.MessageEntity> entities1, ArrayList<TLRPC.MessageEntity> entities2, int offset) {
        ArrayList<TLRPC.MessageEntity> result = new ArrayList<>(entities1);
        if (entities2 != null) {
            for (TLRPC.MessageEntity entity : entities2) {
                var copy = TLObject.deepCopy(entity, TLRPC.MessageEntity::TLdeserialize);
                if (copy == null) continue;
                copy.offset += offset;
                result.add(copy);
            }
        }
        return result;
    }

    private static String normalizeLanguageCode(ITranslator translator, String language, String country) {
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
        } else {
            return countryUpperCase;
        }
    }

    private static String getCurrentAppLanguage(ITranslator translator) {
        String toLang;
        Locale locale = LocaleController.getInstance().getCurrentLocale();
        toLang = normalizeLanguageCode(translator, locale.getLanguage(), locale.getCountry());
        if (!translator.supportLanguage(toLang)) {
            toLang = normalizeLanguageCode(translator, LocaleController.getString(R.string.LanguageCode), null);
        }
        return toLang;
    }

    private static String getTargetLanguage(ITranslator translator, String language) {
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

    private record PollTranslateTask(ITranslator translator, TranslateController.PollText query,
                                     String fl,
                                     String tl,
                                     TranslateCallBack translateCallBack) implements Callable<Pair<TranslateController.PollText, String>>,
            FutureCallback<Pair<TranslateController.PollText, String>> {

        @Override
        public Pair<TranslateController.PollText, String> call() throws Exception {
            var translated = new TranslateController.PollText();
            String sourceLanguage = null;
            if (query.question != null) {
                var result = TranslateTask.obtain(translator, query.question, fl, tl, null).call();
                translated.question = result.translation;
                sourceLanguage = result.sourceLanguage;
            }
            for (var answer : query.answers) {
                var result = TranslateTask.obtain(translator, answer.text, fl, tl, null).call();
                var resultAnswer = new TLRPC.TL_pollAnswer();
                resultAnswer.text = result.translation;
                resultAnswer.option = answer.option;
                translated.answers.add(resultAnswer);
                if (sourceLanguage == null) sourceLanguage = result.sourceLanguage;
            }
            if (query.solution != null) {
                var result = TranslateTask.obtain(translator, query.solution, fl, tl, null).call();
                translated.solution = result.translation;
                if (sourceLanguage == null) sourceLanguage = result.sourceLanguage;
            }
            return Pair.create(translated, sourceLanguage);
        }

        public static PollTranslateTask obtain(ITranslator translator, TranslateController.PollText query, String fl, String tl, TranslateCallBack callback) {
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

    private record TranslateTask(ITranslator translator, TLRPC.TL_textWithEntities query,
                                 String fl,
                                 String tl,
                                 TranslateCallBack translateCallBack) implements Callable<TranslationResult>, FutureCallback<TranslationResult> {

        @Override
        public TranslationResult call() throws Exception {
            var key = Pair.create(query.text, tl + "|" + NekoConfig.translationProvider);
            var cached = cache.get(key);
            if (cached != null) {
                return cached;
            }
            var result = translator.translate(query, null, tl);
            cache.put(key, result);
            return result;
        }

        @Override
        public void onSuccess(TranslationResult result) {
            translateCallBack.onSuccess(result.translation, null, result.sourceLanguage == null ? fl : result.sourceLanguage, tl);
        }

        @Override
        public void onFailure(@NonNull Throwable t) {
            translateCallBack.onError(t);
        }

        public static TranslateTask obtain(ITranslator translator, TLRPC.TL_textWithEntities query, String fl, String tl, TranslateCallBack callback) {
            return new TranslateTask(translator, query, fl, tl, callback);
        }
    }

    interface ITranslator {
        Translator.TranslationResult translate(TLRPC.TL_textWithEntities query, String fl, String tl) throws Exception;

        boolean supportLanguage(String language);

        List<String> getTargetLanguages();
    }

    public record TranslationResult(TLRPC.TL_textWithEntities translation, String sourceLanguage) {
        public static TranslationResult of(TLRPC.TL_textWithEntities translation, String sourceLanguage) {
            return new TranslationResult(translation, sourceLanguage);
        }
    }
}
