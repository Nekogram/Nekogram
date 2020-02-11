package tw.nekomimi.nekogram.translator;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONTokener;
import org.telegram.messenger.FileLog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;

public class GoogleWebTranslator {

    private static GoogleWebTranslator instance;
    private long[] tkk;
    /*private static List<String> targetLanguages = Arrays.asList(
            "auto", "en", "af", "am", "ar", "az", "be", "bg", "bn", "bs", "ca",
            "ceb", "co", "cs", "cy", "da", "de", "el", "eo", "es", "et", "eu", "fa",
            "fi", "fr", "fy", "ga", "gd", "gl", "gu", "ha", "haw", "hi", "hmn", "hr",
            "ht", "hu", "hy", "id", "ig", "is", "it", "iw", "ja", "jw", "ka", "kk",
            "km", "kn", "ko", "ku", "ky", "la", "lb", "lo", "lt", "lv", "mg", "mi",
            "mk", "ml", "mn", "mr", "ms", "mt", "my", "ne", "nl", "no", "ny", "pa",
            "pl", "ps", "pt", "ro", "ru", "sd", "si", "sk", "sl", "sm", "sn", "so",
            "sq", "sr", "st", "su", "sv", "sw", "ta", "te", "tg", "th", "tl", "tr",
            "uk", "ur", "uz", "vi", "xh", "yi", "yo", "zu", "zh-CN", "zh-TW");*/

    public static GoogleWebTranslator getInstance() {
        if (instance == null) {
            synchronized (GoogleWebTranslator.class) {
                if (instance == null) {
                    instance = new GoogleWebTranslator();
                }
            }
        }
        return instance;
    }

    private String translate(String query, String sl, String tl) {
        String result = translateImpl(query, sl, tl);
        if (result == null) {
            tkk = null;// 可能身份过期
            result = translateImpl(query, sl, tl);
        }
        if (result == null) { // 翻译失败
            FileLog.e("Translation failed");
        }
        return result;
    }

    public void translate(String query, String sl, String tl, TranslateCallBack translateCallBack) {
        new MyAsyncTask().request(query, sl, tl, translateCallBack).execute();
    }

    private String translateImpl(String query, String sl, String tl) {
        if (tkk == null) {
            initTkk();
        }
        if (tkk == null) {
            return null;
        }
        String tk = Utils.signWeb(query, tkk[0], tkk[1]);

        String url;
        if (NekoConfig.translationProvider == 2) {
            url = "https://translate.google.cn/translate_a/single?client=webapp&dt=t" +
                    "&sl=" + sl +
                    "&tl=" + tl +
                    "&tk=" + tk +
                    "&q=" + Utils.encodeURIComponent(query); // 不能用URLEncoder
        } else {
            url = "https://translate.google.com/translate_a/single?client=webapp&dt=t" +
                    "&sl=" + sl +
                    "&tl=" + tl +
                    "&tk=" + tk +
                    "&q=" + Utils.encodeURIComponent(query); // 不能用URLEncoder
        }
        String response = request(url);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        try {
            return getResult(response);
        } catch (JSONException e) {
            FileLog.e(e);
            return null;
        }
    }

    private String getResult(String string) throws JSONException {
        StringBuilder sb = new StringBuilder();
        JSONArray array = new JSONArray(new JSONTokener(string)).getJSONArray(0);
        for (int i = 0; i < array.length(); i++) {
            sb.append(array.getJSONArray(i).getString(0));
        }
        return sb.toString();
    }

    private void initTkk() {
        String response;
        if (NekoConfig.translationProvider == 2) {
            response = request("https://translate.google.cn/");
        } else {
            response = request("https://translate.google.com/");
        }
        if (TextUtils.isEmpty(response)) {
            FileLog.e("Tkk init failed");
            return;
        }
        tkk = matchTKK(response);
        if (tkk == null) {
            FileLog.e("Tkk init failed");
        }
    }

    private long[] matchTKK(String src) {
        Matcher matcher = Pattern.compile("tkk\\s*[:=]\\s*['\"]([0-9]+)\\.([0-9]+)['\"]",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(src);
        if (matcher.find()) {
            if (matcher.group(1) == null || matcher.group(2) == null) {
                return null;
            }
            //noinspection ConstantConditions
            return new long[]{Long.parseLong(matcher.group(1)), Long.parseLong(matcher.group(2))};
        }
        return null;
    }

    private String request(String url) {
        try {
            ByteArrayOutputStream outbuf;
            InputStream httpConnectionStream;
            URL downloadUrl = new URL(url);
            URLConnection httpConnection = downloadUrl.openConnection();
            httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
            httpConnection.setConnectTimeout(1000);
            httpConnection.setReadTimeout(2000);
            httpConnection.connect();
            httpConnectionStream = httpConnection.getInputStream();

            outbuf = new ByteArrayOutputStream();

            byte[] data = new byte[1024 * 32];
            while (true) {
                int read = httpConnectionStream.read(data);
                if (read > 0) {
                    outbuf.write(data, 0, read);
                } else if (read == -1) {
                    break;
                } else {
                    break;
                }
            }
            String result = new String(outbuf.toByteArray());
            try {
                httpConnectionStream.close();
            } catch (Throwable e) {
                FileLog.e(e);
            }
            try {
                outbuf.close();
            } catch (Exception ignore) {

            }
            return result;
        } catch (Throwable e) {
            FileLog.e(e);
            return null;
        }
    }

    public interface TranslateCallBack {
        void onSuccess(String translation);

        void onFailure();
    }

    @SuppressLint("StaticFieldLeak")
    private class MyAsyncTask extends AsyncTask<Void, Integer, String> {
        TranslateCallBack translateCallBack; // 回调接口
        String query;
        String sl;
        String tl;

        public MyAsyncTask request(String query, String sl, String tl, TranslateCallBack translateCallBack) {
            this.query = query;
            this.sl = sl;
            this.tl = tl;
            this.translateCallBack = translateCallBack;
            return this;
        }

        @Override
        protected String doInBackground(Void... params) {
            return translate(query, sl, tl);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result == null) {
                translateCallBack.onFailure();
            } else {
                translateCallBack.onSuccess(result);
            }
        }

    }
}
