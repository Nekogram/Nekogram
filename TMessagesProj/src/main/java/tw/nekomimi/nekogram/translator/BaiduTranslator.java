package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.Utilities;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import tw.nekomimi.nekogram.Extra;

public class BaiduTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "zh", "en", "ja", "ko", "fr", "es", "th", "ar",
            "ru", "pt", "de", "it", "el", "nl", "pl", "bg",
            "et", "da", "fi", "cs", "ro", "sl", "sv", "hu",
            "zh-TW", "vi");
    private final List<String> baiduLanguages = Arrays.asList(
            "zh", "en", "jp", "kor", "fra", "spa", "th", "ara",
            "ru", "pt", "de", "it", "el", "nl", "pl", "bul",
            "est", "dan", "fin", "cs", "rom", "slo", "swe", "hu",
            "cht", "vie");
    private final String cuid = UUID.randomUUID().toString().toUpperCase().replace("-", "") + "|" + randomString();

    private static final class InstanceHolder {
        private static final BaiduTranslator instance = new BaiduTranslator();
    }

    static BaiduTranslator getInstance() {
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
                if (countryUpperCase.equals("HK")) {
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

    private static Result getResult(String string) throws IOException {
        Response response = GSON.fromJson(string, Response.class);
        if (response.fanyiList == null) {
            if (response.msg != null) {
                throw new IOException(response.msg);
            }
            return null;
        }
        StringBuilder sb = new StringBuilder();
        response.fanyiList.forEach(s -> sb.append(s).append("\n"));
        return new Result(sb.toString().trim(), response.detectLang);
    }

    @Override
    public String convertLanguageCode(String code, boolean reverse) {
        var index = reverse ? baiduLanguages.indexOf(code) : targetLanguages.indexOf(code);
        if (index < 0) {
            return code;
        }
        return reverse ? targetLanguages.get(index) : baiduLanguages.get(index);
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException {
        var time = System.currentTimeMillis();
        var sign = Extra.signBaidu(query, tl, time);
        var response = Http.url("https://fanyi-app.baidu.com/transapp/agent.php?product=transapp&type=json&version=153&plat=android&req=v2trans&cuid=" + cuid)
                .header("User-Agent", "BDTApp; Android 13; BaiduTranslate/10.2.1")
                .data("sign=" + sign + "&sofireId=&zhType=0&use_cache_response=1&from=auto&timestamp=" + time + "&query=" + URLEncode(query) + "&needfixl=1&lfixver=1&is_show_ad=1&appRecommendSwitch=1&to=" + tl + "&page=translate")
                .request();
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        return getResult(response);
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    public String randomString() {
        char[] symbols = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
        final char[] buf = new char[9];
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[Utilities.random.nextInt(symbols.length)];
        }
        return new String(buf);
    }

    public static class Response {
        @SerializedName("detect_lang")
        @Expose
        public String detectLang;
        @SerializedName("fanyi_list")
        @Expose
        public List<String> fanyiList;
        @SerializedName("msg")
        @Expose
        public String msg;
    }
}
