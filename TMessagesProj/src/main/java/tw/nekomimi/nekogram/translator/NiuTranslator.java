package tw.nekomimi.nekogram.translator;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;

public class NiuTranslator extends BaseTranslator {

    private static NiuTranslator instance;
    private final List<String> targetLanguages = Arrays.asList(
            "sq", "ar", "am", "acu", "agr", "ake", "amu", "az", "ga", "et", "ee", "ojb", "om",
            "or", "os", "ifb", "aym", "knj", "ify", "acr", "amk", "bdu", "adh", "any", "cpb",
            "tpi", "bsn", "ba", "eu", "be", "mww", "ber", "bg", "is", "bi", "bem", "pl", "bs",
            "fa", "pot", "br", "kmr", "poh", "bam", "map", "bba", "bus", "bqp", "bnp", "bch",
            "bno", "bqj", "bdh", "ptu", "bfa", "cbl", "gbo", "cha", "cv", "tn", "ts", "che",
            "ccp", "cdf", "tt", "da", "de", "tet", "dv", "dik", "dyu", "tbz", "mps", "tih",
            "duo", "ru", "djk", "enx", "fr", "fo", "fil", "fj", "fi", "cfm", "gur", "km",
            "quw", "kg", "fy", "jy", "gu", "gub", "gof", "xsm", "krs", "ka", "kk", "ht", "ko",
            "ha", "nl", "me", "cnh", "hui", "hlb", "ky", "quc", "gbi", "gl", "ca", "cs", "gil",
            "kac", "kab", "cjp", "cak", "kn", "kek", "cni", "cop", "kbh", "co", "otq", "hr",
            "ku", "qxr", "ksd", "quz", "kpg", "crh", "xal", "kbo", "keo", "cki", "pss", "kle",
            "la", "lv", "lo", "rn", "lt", "ln", "lg", "dop", "lb", "rw", "ro", "rmn", "ngl", "rug",
            "lsi", "ond", "mg", "mt", "gv", "mr", "ml", "ms", "mhr", "mam", "mk", "mi", "mn", "my",
            "bn", "mni", "meu", "mah", "mrw", "mdy", "mad", "mos", "muv", "nhg", "af", "xh", "zu",
            "ne", "no", "azb", "quh", "lnd", "fuv", "nop", "ntm", "nyy", "pap", "pck", "pa", "pt",
            "ps", "ata", "ny", "tw", "chr", "chq", "cas", "ja", "sv", "sm", "sr", "crs", "st", "sg",
            "si", "mrj", "eo", "jiv", "sk", "sl", "sw", "gd", "so", "swp", "ssx", "spy", "huv", "jmc",
            "tg", "ty", "te", "ta", "th", "to", "tig", "tmh", "tr", "tk", "tpm", "ctd", "tyv", "iou",
            "tex", "lcm", "teo", "wal", "war", "cy", "ve", "wol", "udm", "ur", "uk", "uz", "ppk", "usp"
            , "wlx", "prk", "wsk", "wrs", "vun", "es", "he", "shi", "el", "haw", "sd", "hu", "sn", "ceb",
            "syc", "hwc", "hmo", "lcp", "sid", "mbb", "shp", "ssd", "gnw", "kyu", "hy", "jac", "ace",
            "ig", "it", "yi", "hi", "su", "id", "jv", "en", "yua", "yo", "vi", "yue", "ikk", "izz", "pil",
            "jae", "yon", "zyb", "byr", "dje", "dz", "ifa", "czt", "dtp", "zh-TW", "zh-CN");

    static NiuTranslator getInstance() {
        if (instance == null) {
            synchronized (NiuTranslator.class) {
                if (instance == null) {
                    instance = new NiuTranslator();
                }
            }
        }
        return instance;
    }

    @Override
    protected String translate(String query, String tl) throws IOException, JSONException {
        if (tl.equals("zh-CN")) {
            tl = "zh";
        } else if (tl.equals("zh-TW")) {
            tl = "cht";
        }
        String url = "https://test.niutrans.com/NiuTransServer/testaligntrans?" +
                "from=auto" +
                "&to=" + tl +
                "&src_text=" + URLEncoder.encode(query, "UTF-8") +
                "&source=text&dictNo=&memoryNo=&isUseDict=0&isUseMemory=0&time=" + System.currentTimeMillis();
        String response = request(url);
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        JSONObject jsonObject = new JSONObject(response);
        if (!jsonObject.has("tgt_text") && jsonObject.has("error_msg")) {
            throw new IOException(jsonObject.getString("error_msg"));
        }
        return jsonObject.getString("tgt_text");
    }

    @Override
    public List<String> getTargetLanguages() {
        return targetLanguages;
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

    private String request(String url) throws IOException {
        ByteArrayOutputStream outbuf;
        InputStream httpConnectionStream;
        URL downloadUrl = new URL(url);
        URLConnection httpConnection = downloadUrl.openConnection();
        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
        httpConnection.setConnectTimeout(1000);
        //httpConnection.setReadTimeout(2000);
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
        String result = outbuf.toString();
        httpConnectionStream.close();
        outbuf.close();
        return result;
    }
}
