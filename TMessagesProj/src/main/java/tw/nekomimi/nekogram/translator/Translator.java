package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import org.telegram.messenger.LocaleController;

import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

abstract public class Translator {
    public static void translate(String query, TranslateCallBack translateCallBack) {
        Locale locale = LocaleController.getInstance().currentLocale;
        String toLang;
        if (NekoConfig.translationProvider != 3 && locale.getLanguage().equals("zh") && (locale.getCountry().toUpperCase().equals("CN") || locale.getCountry().toUpperCase().equals("TW"))) {
            toLang = locale.getLanguage() + "-" + locale.getCountry().toUpperCase();
        } else {
            toLang = locale.getLanguage();
        }
        Translator translator = NekoConfig.translationProvider == 3 ? LingoTranslator.getInstance() : GoogleWebTranslator.getInstance();
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
