package tw.nekomimi.nekogram.helpers.remote;

import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tw.nekomimi.nekogram.NekoConfig;

public class ConfigHelper extends BaseRemoteHelper {
    private static final String NEWS_TAG = "config";

    private static volatile ConfigHelper Instance;
    private static final List<News> DEFAULT_NEWS_LIST = new ArrayList<>();
    private static final List<Long> DEFAULT_VERIFY_LIST = Arrays.asList(
            1349472891L,
            1339737452L,
            1302242053L,
            1715773134L
    );

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

    public static List<Long> getVerify() {
        Config config = getInstance().getConfig();
        if (config == null) {
            return DEFAULT_VERIFY_LIST;
        }
        return config.verify;
    }

    public static List<News> getNews() {
        Config config = getInstance().getConfig();
        if (config == null) {
            return DEFAULT_NEWS_LIST;
        }
        ArrayList<News> newsItems = new ArrayList<>();
        config.news.forEach(news -> {
            if (news.chineseOnly != null && news.chineseOnly && !NekoConfig.isChineseUser) {
                return;
            }
            if (news.direct != null && news.direct && !NekoConfig.isDirectApp()) {
                return;
            }
            if (news.source != null && news.source.equals(BuildConfig.BUILD_TYPE)) {
                return;
            }
            if (news.maxVersion != null && news.maxVersion > BuildConfig.VERSION_CODE) {
                return;
            }
            if (news.minVersion != null && news.minVersion < BuildConfig.VERSION_CODE) {
                return;
            }
            newsItems.add(news);
        });
        return newsItems;
    }

    private Config getConfig() {
        String string = getInstance().getJSON();
        try {
            return GSON.fromJson(string, Config.class);
        } catch (JsonSyntaxException e) {
            FileLog.e(e);
            getInstance().load();
            return null;
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

    public static class News {
        @SerializedName("title")
        @Expose
        public String title;
        @SerializedName("summary")
        @Expose
        public String summary;
        @SerializedName("type")
        @Expose
        public Integer type;
        @SerializedName("url")
        @Expose
        public String url;
        @SerializedName("language")
        @Expose
        public String language;
        @SerializedName("chineseOnly")
        @Expose
        public Boolean chineseOnly;
        @SerializedName("direct")
        @Expose
        public Boolean direct;
        @SerializedName("source")
        @Expose
        public String source;
        @SerializedName("maxVersion")
        @Expose
        public Integer maxVersion;
        @SerializedName("minVersion")
        @Expose
        public Integer minVersion;
    }

    public static class Config {
        @SerializedName("verify")
        @Expose
        public List<Long> verify;
        @SerializedName("newsv2")
        @Expose
        public List<News> news;
    }
}
