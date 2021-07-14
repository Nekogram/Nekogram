package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.os.AsyncTask;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

import java.util.List;
import java.util.Locale;

import tw.nekomimi.nekogram.NekoConfig;

abstract public class BaseTranslator {

    abstract protected String translate(String query, String tl) throws Exception;

    abstract public List<String> getTargetLanguages();

    public String convertLanguageCode(String language, String country) {
        return language;
    }

    void startTask(Object query, String toLang, Translator.TranslateCallBack translateCallBack) {
        new MyAsyncTask().request(query, toLang, translateCallBack).execute();
    }

    public boolean supportLanguage(String language) {
        return getTargetLanguages().contains(language);
    }

    public String getCurrentAppLanguage() {
        String toLang;
        Locale locale = LocaleController.getInstance().currentLocale;
        toLang = convertLanguageCode(locale.getLanguage(), locale.getCountry());
        if (!supportLanguage(toLang)) {
            toLang = convertLanguageCode(LocaleController.getString("LanguageCode", R.string.LanguageCode), null);
        }
        return toLang;
    }

    public String getTargetLanguage(String language) {
        String toLang;
        if (language.equals("app")) {
            toLang = getCurrentAppLanguage();
        } else {
            toLang = language;
        }
        return toLang;
    }

    public String getCurrentTargetLanguage() {
        return getTargetLanguage(NekoConfig.translationTarget);
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("StaticFieldLeak")
    private class MyAsyncTask extends AsyncTask<Void, Integer, Object> {
        Translator.TranslateCallBack translateCallBack;
        Object query;
        String tl;

        public MyAsyncTask request(Object query, String tl, Translator.TranslateCallBack translateCallBack) {
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
                e.printStackTrace();
                FileLog.e(e);
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            if (result == null) {
                translateCallBack.onError(null);
            } else if (result instanceof Exception) {
                translateCallBack.onError((Exception) result);
            } else {
                translateCallBack.onSuccess(result);
            }
        }

    }
}
