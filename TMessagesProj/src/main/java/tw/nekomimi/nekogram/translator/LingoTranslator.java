package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class LingoTranslator extends BaseTranslator {

    private final List<String> targetLanguages = Arrays.asList("zh", "en", "ja", "ko", "es", "fr", "ru");

    private static final class InstanceHolder {
        private static final LingoTranslator instance = new LingoTranslator();
    }

    static LingoTranslator getInstance() {
        return InstanceHolder.instance;
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    @Override
    protected Result translate(String query, String fl, String tl) throws IOException {
        Request request = new Request(
                Arrays.asList(query.split("\n")),
                "auto2" + tl,
                String.valueOf(System.currentTimeMillis()),
                "true");
        String response = Http.url("https://interpreter.cyapi.cn/v1/translator")
                .header("X-Authorization", "token " + Extra.LINGO_TOKEN)
                .header("User-Agent", "okhttp/3.12.3")
                .data(GSON.toJson(request), "application/json; charset=UTF-8")
                .request();
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        return getResult(response);
    }

    private Result getResult(String string) throws IOException {
        Response response = GSON.fromJson(string, Response.class);
        if (response.target == null) {
            if (response.error != null) {
                throw new IOException(response.error);
            }
            return null;
        }
        StringBuilder sb = new StringBuilder();
        response.target.forEach(s -> sb.append(s).append("\n"));
        return new Result(sb.toString().trim(), null);
    }

    public static class Request {
        @SerializedName("source")
        @Expose
        public List<String> source;
        @SerializedName("trans_type")
        @Expose
        public String transType;
        @SerializedName("request_id")
        @Expose
        public String requestId;
        @SerializedName("detect")
        @Expose
        public String detect;

        public Request(List<String> source, String transType, String requestId, String detect) {
            this.source = source;
            this.transType = transType;
            this.requestId = requestId;
            this.detect = detect;
        }
    }

    public static class Response {
        @SerializedName("target")
        @Expose
        public List<String> target;
        @SerializedName("error")
        @Expose
        public String error;
    }
}
