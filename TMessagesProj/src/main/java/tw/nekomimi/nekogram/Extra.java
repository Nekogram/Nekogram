package tw.nekomimi.nekogram;

import android.util.Base64;

import org.telegram.messenger.BuildConfig;
import org.telegram.tgnet.SerializedData;

import java.util.HashMap;

public class Extra {
    private static final HashMap<String, Object> map = new HashMap<>();

    static {
        SerializedData data = new SerializedData(Base64.decode(BuildConfig.EXTRA, Base64.NO_WRAP | Base64.NO_PADDING));
        map.put("tencent.SecretId", data.readString(false));
        map.put("tencent.SecretKey", data.readString(false));
        map.put("lingo.token", data.readString(false));
        map.put("neko.update_channel", data.readString(false));
        data.cleanup();
    }

    public static String getString(String key) {
        if (map.containsKey(key)) {
            return (String) map.get(key);
        } else {
            return "";
        }
    }
}
