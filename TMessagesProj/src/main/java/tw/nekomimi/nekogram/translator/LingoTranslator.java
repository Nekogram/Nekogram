package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.Extra;

public class LingoTranslator extends BaseTranslator {

    private static LingoTranslator instance;
    private final List<String> targetLanguages = Arrays.asList("zh", "en", "es", "fr", "ja", "ru");

    static LingoTranslator getInstance() {
        if (instance == null) {
            synchronized (LingoTranslator.class) {
                if (instance == null) {
                    instance = new LingoTranslator();
                }
            }
        }
        return instance;
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("source", query);
        jsonObject.put("trans_type", "auto2" + tl);
        jsonObject.put("request_id", String.valueOf(System.currentTimeMillis()));
        jsonObject.put("detect", true);
        String response = new Http("https://api.interpreter.caiyunai.com/v1/translator")
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("X-Authorization", "token " + Extra.LINGO_TOKEN)
                .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1")
                .data(jsonObject.toString())
                .request();
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        jsonObject = new JSONObject(response);
        if (!jsonObject.has("target") && jsonObject.has("error")) {
            throw new IOException(jsonObject.getString("error"));
        }
        return jsonObject.getString("target");
    }
}
