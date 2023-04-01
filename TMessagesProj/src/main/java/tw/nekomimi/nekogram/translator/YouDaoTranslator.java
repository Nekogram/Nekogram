package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class YouDaoTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "en", "ja", "ko", "fr", "de", "ru", "es",
            "pt", "it", "vi", "id", "ar", "nl", "th",
            "zh-CN", "zh-TW", "zh");

    private static final class InstanceHolder {
        private static final YouDaoTranslator instance = new YouDaoTranslator();
    }

    static YouDaoTranslator getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public String convertLanguageCode(String language, String country) {
        String languageLowerCase = language.toLowerCase();
        String code;
        if (!TextUtils.isEmpty(country)) {
            String countryUpperCase = country.toUpperCase();
            if (targetLanguages.contains(languageLowerCase + "-" + countryUpperCase)) {
                code = languageLowerCase + "-" + countryUpperCase;
            } else if (languageLowerCase.equals("zh")) {
                if (countryUpperCase.equals("DG")) {
                    code = "zh-CN";
                } else if (countryUpperCase.equals("HK")) {
                    code = "zh-TW";
                } else {
                    code = languageLowerCase;
                }
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    private static Result getResult(String string) {
        Response response = GSON.fromJson(string, Response.class);
        if (response.fanyi == null) {
            return null;
        }
        return new Result(response.fanyi.trans, response.lang);
    }

    @Override
    public String convertLanguageCode(String code, boolean reverse) {
        return reverse ? code.replace("_", "-") : code.replace("-", "_").toUpperCase();
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException {
        var time = System.currentTimeMillis() + Math.round(Math.random() * 10);
        var sign = Extra.signYouDao(query, time);
        return getResult(Http.url("https://fanyi.youdao.com/appapi/tran?&product=fanyiguan&appVersion=4.1.16&keyfrom=fanyi.4.1.16.android&network=wifi")
                .header("User-Agent", "okhttp/4.9.1")
                .data("q=" + URLEncode(query) + "&salt=" + time + "&sign=" + sign + "&needad=false&category=Android&type=AUTO2" + tl + "&needdict=true&version=4.1.16&needfanyi=true&needsentences=false&scene=realtime")
                .request());
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    public static class Fanyi {
        @SerializedName("trans")
        @Expose
        public String trans;
    }

    public static class Response {
        @SerializedName("fanyi")
        @Expose
        public Fanyi fanyi;
        @SerializedName("lang")
        @Expose
        public String lang;
    }
}
