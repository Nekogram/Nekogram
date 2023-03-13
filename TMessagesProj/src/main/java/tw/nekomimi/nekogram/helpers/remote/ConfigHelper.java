package tw.nekomimi.nekogram.helpers.remote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;

public class ConfigHelper extends BaseRemoteHelper {
    private static final String NEWS_TAG = "config";

    private static volatile ConfigHelper Instance;
    private static final ArrayList<NewsItem> DEFAULT_NEWS_LIST = new ArrayList<>();
    private static final ArrayList<Long> DEFAULT_VERIFY_LIST = new ArrayList<>();
    private static final String DEFAULT_WS_DOMAIN = "nekoe.eu.org";

    static {
        DEFAULT_VERIFY_LIST.add(1302242053L);
        DEFAULT_VERIFY_LIST.add(1406090861L);
        DEFAULT_VERIFY_LIST.add(1221673407L);
        DEFAULT_VERIFY_LIST.add(1339737452L);
        DEFAULT_VERIFY_LIST.add(1349472891L);
        DEFAULT_VERIFY_LIST.add(1676383632L);
    }

    public static ConfigHelper getInstance() {
        ConfigHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (ConfigHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ConfigHelper();
                }
                return localInstance;
            }
        }
        return localInstance;
    }

    public static String getWsDomain() {
        JSONObject jsonObject = getInstance().getJSON(false);
        if (jsonObject == null) {
            return DEFAULT_WS_DOMAIN;
        }
        try {
            return jsonObject.getString("wsdomain");
        } catch (JSONException e) {
            FileLog.e(e);
            return DEFAULT_WS_DOMAIN;
        }
    }

    public static ArrayList<Long> getVerify() {
        JSONObject jsonObject = getInstance().getJSON();
        if (jsonObject == null) {
            return DEFAULT_VERIFY_LIST;
        }
        ArrayList<Long> verifyItems = new ArrayList<>();
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("verify");
            for (int i = 0; i < jsonArray.length(); i++) {
                verifyItems.add(jsonArray.getLong(i));
            }
            return verifyItems;
        } catch (JSONException e) {
            FileLog.e(e);
            getInstance().load();
            return DEFAULT_VERIFY_LIST;
        }
    }

    public static ArrayList<NewsItem> getNews() {
        JSONObject jsonObject = getInstance().getJSON();
        if (jsonObject == null) {
            return DEFAULT_NEWS_LIST;
        }
        ArrayList<NewsItem> newsItems = new ArrayList<>();
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("newsv2");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                if (object.has("chineseOnly") && object.getBoolean("chineseOnly") && !NekoConfig.isChineseUser) {
                    continue;
                }
                if (object.has("direct") && object.getBoolean("direct") && !NekoConfig.isDirectApp()) {
                    continue;
                }
                if (object.has("source") && !object.getString("source").equals(BuildConfig.BUILD_TYPE)) {
                    continue;
                }
                if (object.has("maxVersion") && object.getLong("maxVersion") > BuildConfig.VERSION_CODE) {
                    continue;
                }
                if (object.has("minVersion") && object.getLong("minVersion") < BuildConfig.VERSION_CODE) {
                    continue;
                }
                if (!object.getString("language").equals("ALL") && !object.getString("language").equals(LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername))) {
                    continue;
                }
                newsItems.add(new NewsItem(
                        object.getInt("type"),
                        object.getString("title"),
                        object.getString("summary"),
                        object.getString("url")
                ));
            }
            return newsItems;
        } catch (JSONException e) {
            FileLog.e(e);
            getInstance().load();
            return DEFAULT_NEWS_LIST;
        }
    }

    @Override
    protected void onError(String text, Delegate delegate) {
        FileLog.e("ConfigHelper error = " + text);
    }

    @Override
    protected String getTag() {
        return NEWS_TAG;
    }

    public static class NewsItem {
        public int type;
        public String title;
        public String summary;
        public String url;

        public NewsItem(int type, String title, String summary, String url) {
            this.type = type;
            this.title = title;
            this.summary = summary;
            this.url = url;
        }
    }
}
