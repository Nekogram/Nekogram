package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;

import androidx.core.util.Pair;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

public class Translator {

    public static final int PROVIDER_GOOGLE = 1;
    public static final int PROVIDER_GOOGLE_CN = 2;
    public static final int PROVIDER_LINGO = 3;
    public static final int PROVIDER_YANDEX = 4;
    public static final int PROVIDER_DEEPL = 5;
    public static final int PROVIDER_YOUDAO = 6;
    public static final int PROVIDER_MICROSOFT = 7;

    @SuppressLint("StaticFieldLeak")
    private static AlertDialog progressDialog;

    public static void showTranslateDialog(Context context, String query) {
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
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage((String) translation);
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                builder.setNeutralButton(LocaleController.getString("Copy", R.string.Copy), (dialog, which) -> AndroidUtilities.addToClipboard((String) translation));
                builder.show();
            }

            @Override
            public void onError(Exception e) {
                handleTranslationError(context, e, () -> showTranslateDialog(context, query));
            }
        });
    }

    public static void handleTranslationError(Context context, final Exception e, final RetryCallback onRetry) {
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
            builder.setPositiveButton(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), (dialog, which) -> getTranslationProviderAlert(context).show());
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
        }
        builder.setNeutralButton(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), (dialog, which) -> getTranslationProviderAlert(context).show());
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        builder.show();
    }

    public static Pair<ArrayList<String>, ArrayList<Integer>> getProviders() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        names.add(LocaleController.getString("ProviderGoogleTranslate", R.string.ProviderGoogleTranslate));
        types.add(Translator.PROVIDER_GOOGLE);
        names.add(LocaleController.getString("ProviderGoogleTranslateCN", R.string.ProviderGoogleTranslateCN));
        types.add(Translator.PROVIDER_GOOGLE_CN);
        names.add(LocaleController.getString("ProviderLingocloud", R.string.ProviderLingocloud));
        types.add(Translator.PROVIDER_LINGO);
        names.add(LocaleController.getString("ProviderYandex", R.string.ProviderYandex));
        types.add(Translator.PROVIDER_YANDEX);
        names.add(LocaleController.getString("ProviderDeepLTranslate", R.string.ProviderDeepLTranslate));
        types.add(Translator.PROVIDER_DEEPL);
        names.add(LocaleController.getString("ProviderYouDao", R.string.ProviderYouDao));
        types.add(Translator.PROVIDER_YOUDAO);
        names.add(LocaleController.getString("ProviderMicrosoftTranslator", R.string.ProviderMicrosoftTranslator));
        types.add(Translator.PROVIDER_MICROSOFT);
        return new Pair<>(names, types);
    }

    public static AlertDialog getTranslationProviderAlert(Context context) {
        Pair<ArrayList<String>, ArrayList<Integer>> providers = getProviders();
        ArrayList<String> names = providers.first;
        ArrayList<Integer> types = providers.second;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("TranslationProvider", R.string.TranslationProvider));
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(linearLayout);
        if (names == null || types == null) {
            return builder.create();
        }
        for (int a = 0; a < names.size(); a++) {
            RadioColorCell cell = new RadioColorCell(context);
            cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            cell.setTag(a);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(names.get(a), NekoConfig.translationProvider == types.get(a));
            linearLayout.addView(cell);
            cell.setOnClickListener(v -> {
                Integer which = (Integer) v.getTag();
                NekoConfig.setTranslationProvider(types.get(which));
                builder.getDismissRunnable().run();
            });
        }
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        return builder.create();
    }

    public static void translate(Object query, TranslateCallBack translateCallBack) {
        BaseTranslator translator;
        int provider = NekoConfig.translationProvider;
        switch (provider) {
            case PROVIDER_YANDEX:
                translator = YandexTranslator.getInstance();
                break;
            case PROVIDER_LINGO:
                translator = LingoTranslator.getInstance();
                break;
            case PROVIDER_DEEPL:
                translator = DeepLTranslator.getInstance();
                break;
            case PROVIDER_YOUDAO:
                translator = YouDaoTranslator.getInstance();
                break;
            case PROVIDER_MICROSOFT:
                translator = MicrosoftTranslator.getInstance();
                break;
            case PROVIDER_GOOGLE:
            case PROVIDER_GOOGLE_CN:
            default:
                translator = GoogleAppTranslator.getInstance();
                break;
        }

        List<String> targetLanguages = translator.getTargetLanguages();
        Locale locale = LocaleController.getInstance().currentLocale;
        String toLang = convertLanguageCode(provider, locale.getLanguage(), locale.getCountry());
        if (!targetLanguages.contains(toLang)) {
            toLang = convertLanguageCode(provider, LocaleController.getString("LanguageCode", R.string.LanguageCode), null);
        }
        if (!targetLanguages.contains(toLang)) {
            translateCallBack.onError(new UnsupportedTargetLanguageException());
        } else {
            translator.startTask(query, toLang, translateCallBack);
        }
    }

    private static String convertLanguageCode(int provider, String language, String country) {
        String toLang;
        switch (provider) {
            case PROVIDER_YANDEX:
            case PROVIDER_LINGO:
                toLang = language;
                break;
            case PROVIDER_DEEPL:
                toLang = language.toUpperCase();
                break;
            case PROVIDER_YOUDAO:
                if (language.equals("zh")) {
                    toLang = "zh-CHS";
                } else {
                    toLang = language;
                }
                break;
            case PROVIDER_MICROSOFT:
            case PROVIDER_GOOGLE:
            case PROVIDER_GOOGLE_CN:
            default:
                if (country != null && language.equals("zh")) {
                    String countryUpperCase = country.toUpperCase();
                    if (countryUpperCase.equals("CN") || countryUpperCase.equals("DUANG")) {
                        toLang = provider == PROVIDER_MICROSOFT ? "zh-Hans" : "zh-CN";
                    } else if (countryUpperCase.equals("TW") || countryUpperCase.equals("HK")) {
                        toLang = provider == PROVIDER_MICROSOFT ? "zh-HanT" : "zh-TW";
                    } else {
                        toLang = language;
                    }
                } else {
                    toLang = language;
                }
                break;
        }
        return toLang;
    }

    public interface RetryCallback {
        void run();
    }

    public interface TranslateCallBack {
        void onSuccess(Object translation);

        void onError(Exception e);
    }

    private static class UnsupportedTargetLanguageException extends IllegalArgumentException {
    }


}
