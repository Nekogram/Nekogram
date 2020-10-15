package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

import java.util.ArrayList;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

public class Translator {

    public static final int PROVIDER_GOOGLE = 1;
    public static final int PROVIDER_GOOGLE_CN = 2;
    public static final int PROVIDER_LINGO = 3;
    public static final int PROVIDER_YANDEX = 4;
    //public static final int PROVIDER_DEEPL = 5;

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

    public static AlertDialog getTranslationProviderAlert(Context context) {
        ArrayList<String> arrayList = new ArrayList<>();
        ArrayList<Integer> types = new ArrayList<>();
        arrayList.add(LocaleController.getString("ProviderGoogleTranslate", R.string.ProviderGoogleTranslate));
        types.add(Translator.PROVIDER_GOOGLE);
        arrayList.add(LocaleController.getString("ProviderGoogleTranslateCN", R.string.ProviderGoogleTranslateCN));
        types.add(Translator.PROVIDER_GOOGLE_CN);
        arrayList.add(LocaleController.getString("ProviderLingocloud", R.string.ProviderLingocloud));
        types.add(Translator.PROVIDER_LINGO);
        arrayList.add(LocaleController.getString("ProviderYandex", R.string.ProviderYandex));
        types.add(Translator.PROVIDER_YANDEX);
        //arrayList.add(LocaleController.getString("ProviderDeepLTranslate", R.string.ProviderDeepLTranslate));
        //types.add(Translator.PROVIDER_DEEPL);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(LocaleController.getString("TranslationProvider", R.string.TranslationProvider));
        final LinearLayout linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        builder.setView(linearLayout);

        for (int a = 0; a < arrayList.size(); a++) {
            RadioColorCell cell = new RadioColorCell(context);
            cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
            cell.setTag(a);
            cell.setCheckColor(Theme.getColor(Theme.key_radioBackground), Theme.getColor(Theme.key_dialogRadioBackgroundChecked));
            cell.setTextAndValue(arrayList.get(a), NekoConfig.translationProvider == types.get(a));
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
        Locale locale = LocaleController.getInstance().currentLocale;
        String toLang;
        BaseTranslator translator;
        switch (NekoConfig.translationProvider) {
            case PROVIDER_YANDEX:
                toLang = locale.getLanguage();
                translator = YandexTranslator.getInstance();
                break;
            case PROVIDER_LINGO:
                toLang = locale.getLanguage();
                translator = LingoTranslator.getInstance();
                break;
            /*case PROVIDER_DEEPL:
                toLang = locale.getLanguage().toUpperCase();
                translator = DeepLTranslator.getInstance();
                break;*/
            case PROVIDER_GOOGLE:
            case PROVIDER_GOOGLE_CN:
            default:
                if (locale.getLanguage().equals("zh")) {
                    if (locale.getCountry().toUpperCase().equals("CN") || locale.getCountry().toUpperCase().equals("DUANG")) {
                        toLang = "zh-CN";
                    } else if (locale.getCountry().toUpperCase().equals("TW") || locale.getCountry().toUpperCase().equals("HK")) {
                        toLang = "zh-TW";
                    } else {
                        toLang = locale.getLanguage();
                    }
                } else {
                    toLang = locale.getLanguage();
                }
                translator = GoogleWebTranslator.getInstance();
                break;
        }
        if (!translator.getTargetLanguages().contains(toLang)) {
            translateCallBack.onError(new UnsupportedTargetLanguageException());
        } else {
            translator.startTask(query, toLang, translateCallBack);
        }
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
