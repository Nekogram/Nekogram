package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.core.util.Pair;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.PopupHelper;

public class Translator {

    public static final int PROVIDER_GOOGLE = 1;
    public static final int PROVIDER_LINGO = 3;
    public static final int PROVIDER_YANDEX = 4;
    public static final int PROVIDER_DEEPL = 5;
    public static final int PROVIDER_MICROSOFT = 7;

    @SuppressLint("StaticFieldLeak")
    private static AlertDialog progressDialog;

    public static void showTranslateDialog(Context context, String query, Runnable callback) {
        try {
            progressDialog.dismiss();
        } catch (Exception ignore) {

        }
        progressDialog = new AlertDialog(context, 3);
        progressDialog.showDelayed(400);
        translate(query, new TranslateCallBack() {
            @Override
            public void onSuccess(Object translation) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignore) {

                }

                TextView messageTextView = new TextView(context);
                messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
                messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
                messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);
                messageTextView.setTextIsSelectable(true);
                messageTextView.setText((String) translation);
                messageTextView.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(4), AndroidUtilities.dp(24), AndroidUtilities.dp(4));

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setView(messageTextView);
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                builder.setNeutralButton(LocaleController.getString("Copy", R.string.Copy), (dialog, which) -> {
                    AndroidUtilities.addToClipboard((String) translation);
                    if (callback != null) {
                        callback.run();
                    }
                });
                builder.show();
            }

            @Override
            public void onError(Exception e) {
                handleTranslationError(context, e, () -> showTranslateDialog(context, query, callback));
            }
        });
    }

    public static void handleTranslationError(Context context, final Exception e, final Runnable onRetry) {
        if (context == null) {
            return;
        }
        try {
            progressDialog.dismiss();
        } catch (Exception ignore) {

        }
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (e instanceof UnsupportedTargetLanguageException) {
            builder.setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported));
            builder.setPositiveButton(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(context, null, null));
        } else {
            if (e != null && e.getLocalizedMessage() != null) {
                builder.setTitle(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
                builder.setMessage(e.getLocalizedMessage());
            } else {
                builder.setMessage(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
            }
            if (onRetry != null) {
                builder.setPositiveButton(LocaleController.getString("Retry", R.string.Retry), (dialog, which) -> onRetry.run());
            }
            builder.setNeutralButton(LocaleController.getString("TranslationProviderShort", R.string.TranslationProviderShort), (dialog, which) -> showTranslationProviderSelector(context, null, null));
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    public static Pair<ArrayList<String>, ArrayList<Integer>> getProviders() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
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
        return new Pair<>(names, types);
    }

    public static void showTranslationProviderSelector(Context context, View view, MessagesStorage.BooleanCallback callback) {
        Pair<ArrayList<String>, ArrayList<Integer>> providers = getProviders();
        ArrayList<String> names = providers.first;
        ArrayList<Integer> types = providers.second;
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
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                        .setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported));
                if ("app".equals(NekoConfig.translationTarget)) {
                    builder.setPositiveButton(LocaleController.getString("UseGoogleTranslate", R.string.UseGoogleTranslate), (dialog, which) -> {
                        NekoConfig.setTranslationProvider(Translator.PROVIDER_GOOGLE);
                        if (callback != null) callback.run(false);
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
        });
    }

    public static BaseTranslator getCurrentTranslator() {
        return getTranslator(NekoConfig.translationProvider);
    }

    public static BaseTranslator getTranslator(int type) {
        BaseTranslator translator;
        switch (type) {
            case PROVIDER_YANDEX:
                translator = YandexTranslator.getInstance();
                break;
            case PROVIDER_LINGO:
                translator = LingoTranslator.getInstance();
                break;
            case PROVIDER_DEEPL:
                translator = DeepLTranslator.getInstance();
                break;
            case PROVIDER_MICROSOFT:
                translator = MicrosoftTranslator.getInstance();
                break;
            case PROVIDER_GOOGLE:
            default:
                translator = GoogleAppTranslator.getInstance();
                break;
        }
        return translator;
    }

    public static void translate(Object query, TranslateCallBack translateCallBack) {
        BaseTranslator translator = getCurrentTranslator();

        String language = translator.getCurrentTargetLanguage();

        if (!translator.supportLanguage(language)) {
            translateCallBack.onError(new UnsupportedTargetLanguageException());
        } else {
            translator.startTask(query, language, translateCallBack);
        }
    }

    public interface TranslateCallBack {
        void onSuccess(Object translation);

        void onError(Exception e);
    }

    private static class UnsupportedTargetLanguageException extends IllegalArgumentException {
    }


}
