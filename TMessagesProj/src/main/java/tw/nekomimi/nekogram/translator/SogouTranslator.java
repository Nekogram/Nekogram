package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class SogouTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList(
            "ar", "cs", "da", "de", "de", "en", "es",
            "fi", "fr", "hu", "it", "ja", "ko", "nl",
            "pl", "pt", "ru", "sv", "th", "tr", "vi",
            "zh");

    private static final class InstanceHolder {
        private static final SogouTranslator instance = new SogouTranslator();
    }

    static SogouTranslator getInstance() {
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
                code = "zh-CHS";
            } else {
                code = languageLowerCase;
            }
        } else {
            code = languageLowerCase;
        }
        return code;
    }

    @Override
    public String convertLanguageCode(String code, boolean reverse) {
        if (reverse) {
            if (code.equals("zh-CHS")) return "zh";
        } else {
            if (code.equals("zh")) return "zh-CHS";
        }
        return code;
    }

    private static Result getResult(String string) throws IOException {
        Response response = GSON.fromJson(string, Response.class);
        if (response.data == null) {
            if (response.message != null) {
                throw new IOException(response.message);
            }
            return null;
        }
        Translation translation = response.data.translation;
        return new Result(translation.transText, translation.from);
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException {
        long currentTimeMillis = System.currentTimeMillis();
        var param = GSON.toJson(new Request(query, "auto", tl));
        return getResult(Http.url("https://fanyi.sogou.com/openapi/external/dictTranslation")
                .header("S-AppId", String.valueOf(Extra.SOGOU_APP_ID))
                .header("S-AppKey", Extra.SOGOU_APP_KEY)
                .header("S-CurTime", String.valueOf(currentTimeMillis))
                .header("S-Sign", Extra.signSogou(param, currentTimeMillis))
                .data("S-Param=" + URLEncode(param))
                .request());
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    public static class Request {
        @SerializedName("query")
        public String query;
        @SerializedName("from")
        public String from;
        @SerializedName("to")
        public String to;

        public Request(String query, String from, String to) {
            this.query = query;
            this.from = from;
            this.to = to;
        }
    }

    public static class Data {
        @SerializedName("translation")
        @Expose
        public Translation translation;
    }

    public static class Response {
        @SerializedName("message")
        @Expose
        public String message;
        @SerializedName("data")
        @Expose
        public Data data;
    }

    public static class Translation {
        @SerializedName("from")
        @Expose
        public String from;
        @SerializedName("trans_text")
        @Expose
        public String transText;
    }
}
