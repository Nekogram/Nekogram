package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.LinearLayout;

import org.json.JSONException;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

abstract public class Translator {

    public static final int PROVIDER_GOOGLE = 1;
    public static final int PROVIDER_GOOGLE_CN = 2;
    public static final int PROVIDER_LINGO = 3;
    public static final int PROVIDER_YANDEX = 4;
    public static final int PROVIDER_MICROSOFT = 5;

    @SuppressLint("StaticFieldLeak")
    private static AlertDialog progressDialog;

    public static void showTranslateDialog(Context context, String query) {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        progressDialog = new AlertDialog(context, 3);
        progressDialog.showDelayed(400);
        translate(query, new TranslateCallBack() {
            @Override
            public void onSuccess(Object translation) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage((String) translation);
                builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                builder.setNeutralButton(LocaleController.getString("Copy", R.string.Copy), (dialog, which) -> AndroidUtilities.addToClipboard((String) translation));
                builder.show();
            }

            @Override
            public void onError(Throwable e) {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                if (e != null && e.getLocalizedMessage() != null) {
                    builder.setTitle(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
                    builder.setMessage(e.getLocalizedMessage());
                } else {
                    builder.setMessage(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
                }
                builder.setNeutralButton(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), (dialog, which) -> getTranslationProviderAlert(context).show());
                builder.setPositiveButton(LocaleController.getString("Retry", R.string.Retry), (dialog, which) -> showTranslateDialog(context, query));
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show();
            }

            @Override
            public void onUnsupported() {
                if (progressDialog != null) {
                    progressDialog.dismiss();
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported));
                builder.setPositiveButton(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), (dialog, which) -> getTranslationProviderAlert(context).show());
                builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                builder.show();
            }
        });
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
        arrayList.add(LocaleController.getString("ProviderMicrosoft", R.string.ProviderMicrosoft));
        types.add(Translator.PROVIDER_MICROSOFT);

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
        Translator translator;
        switch (NekoConfig.translationProvider) {
            case PROVIDER_YANDEX:
                toLang = locale.getLanguage();
                translator = YandexTranslator.getInstance();
                break;
            case PROVIDER_LINGO:
                toLang = locale.getLanguage();
                translator = LingoTranslator.getInstance();
                break;
            case PROVIDER_MICROSOFT:
                if (locale.getLanguage().equals("zh")) {
                    if (locale.getCountry().toUpperCase().equals("CN") || locale.getCountry().toUpperCase().equals("DUANG")) {
                        toLang = "zh-Hans";
                    } else if (locale.getCountry().toUpperCase().equals("TW") || locale.getCountry().toUpperCase().equals("HK")) {
                        toLang = "zh-Hant";
                    } else {
                        toLang = locale.getLanguage();
                    }
                } else {
                    toLang = locale.getLanguage();
                }
                translator = MicrosoftTranslator.getInstance();
                break;
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
            translateCallBack.onUnsupported();
        } else {
            translator.startTask(query, toLang, translateCallBack);
        }
    }

    private void startTask(Object query, String toLang, TranslateCallBack translateCallBack) {
        new MyAsyncTask().request(query, toLang, translateCallBack).execute();
    }

    abstract protected String translate(String query, String tl) throws IOException, JSONException;

    abstract protected List<String> getTargetLanguages();

    public interface TranslateCallBack {
        void onSuccess(Object translation);

        void onError(Throwable e);

        void onUnsupported();
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class MyAsyncTask extends AsyncTask<Void, Integer, Object> {
        TranslateCallBack translateCallBack;
        Object query;
        String tl;

        public MyAsyncTask request(Object query, String tl, TranslateCallBack translateCallBack) {
            this.query = query;
            this.tl = tl;
            this.translateCallBack = translateCallBack;
            return this;
        }

        @Override
        protected Object doInBackground(Void... params) {
            try {
                if (query instanceof String) {
                    return translate((String) query, tl);
                } else if (query instanceof TLRPC.Poll) {
                    TLRPC.TL_poll poll = new TLRPC.TL_poll();
                    TLRPC.TL_poll original = (TLRPC.TL_poll) query;
                    poll.question = original.question +
                            "\n" +
                            "--------" +
                            "\n" + translate(original.question, tl);
                    for (int i = 0; i < original.answers.size(); i++) {
                        TLRPC.TL_pollAnswer answer = new TLRPC.TL_pollAnswer();
                        answer.text = original.answers.get(i).text + " | " + translate(original.answers.get(i).text, tl);
                        answer.option = original.answers.get(i).option;
                        poll.answers.add(answer);
                    }
                    poll.close_date = original.close_date;
                    poll.close_period = original.close_period;
                    poll.closed = original.closed;
                    poll.flags = original.flags;
                    poll.id = original.id;
                    poll.multiple_choice = original.multiple_choice;
                    poll.public_voters = original.public_voters;
                    poll.quiz = original.quiz;
                    return poll;
                } else {
                    throw new UnsupportedOperationException("Unsupported translation query");
                }
            } catch (Throwable e) {
                FileLog.e(e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result == null) {
                translateCallBack.onError(null);
            } else if (result instanceof Throwable) {
                translateCallBack.onError((Throwable) result);
            } else {
                translateCallBack.onSuccess(result);
            }
        }

    }

}
