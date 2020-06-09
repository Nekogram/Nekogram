package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import org.telegram.messenger.LocaleController;

import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

abstract public class Translator {

    public static final int PROVIDER_GOOGLE = 1;
    public static final int PROVIDER_GOOGLE_CN = 2;
    public static final int PROVIDER_LINGO = 3;
    public static final int PROVIDER_YANDEX = 4;
    public static final int PROVIDER_GOOGLE_WEB = -1;
    public static final int PROVIDER_GOOGLE_CN_WEB = -2;
    public static final int PROVIDER_BAIDU_WEB = -3;
    public static final int PROVIDER_DEEPL_WEB = -4;

    public static void translate(String query, TranslateCallBack translateCallBack) {
        Locale locale = LocaleController.getInstance().currentLocale;
        String toLang;
        if (NekoConfig.translationProvider != PROVIDER_LINGO && NekoConfig.translationProvider != PROVIDER_YANDEX && locale.getLanguage().equals("zh") && (locale.getCountry().toUpperCase().equals("CN") || locale.getCountry().toUpperCase().equals("TW"))) {
            toLang = locale.getLanguage() + "-" + locale.getCountry().toUpperCase();
        } else {
            toLang = locale.getLanguage();
        }
        Translator translator;
        switch (NekoConfig.translationProvider) {
            case PROVIDER_YANDEX:
                translator = YandexTranslator.getInstance();
                break;
            case PROVIDER_GOOGLE:
            case PROVIDER_GOOGLE_CN:
                translator = GoogleWebTranslator.getInstance();
                break;
            case PROVIDER_LINGO:
            default:
                translator = LingoTranslator.getInstance();
                break;
        }
        if (!translator.getTargetLanguages().contains(toLang)) {
            translateCallBack.onUnsupported();
        } else {
            translator.startTask(query, toLang, translateCallBack);
        }
    }

    private void startTask(String query, String toLang, TranslateCallBack translateCallBack) {
        new MyAsyncTask().request(query, toLang, translateCallBack).execute();
    }

    abstract protected String translate(String query, String tl);

    abstract protected List<String> getTargetLanguages();

    public interface TranslateCallBack {
        void onSuccess(String translation);

        void onError();

        void onUnsupported();
    }

    @SuppressLint("StaticFieldLeak")
    private class MyAsyncTask extends AsyncTask<Void, Integer, String> {
        TranslateCallBack translateCallBack;
        String query;
        String tl;

        public MyAsyncTask request(String query, String tl, TranslateCallBack translateCallBack) {
            this.query = query;
            this.tl = tl;
            this.translateCallBack = translateCallBack;
            return this;
        }

        @Override
        protected String doInBackground(Void... params) {
            return translate(query, tl);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                translateCallBack.onError();
            } else {
                translateCallBack.onSuccess(result);
            }
        }

    }

}
