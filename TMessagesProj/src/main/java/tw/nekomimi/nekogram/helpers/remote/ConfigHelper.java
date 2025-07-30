package tw.nekomimi.nekogram.helpers.remote;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_bots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.NekoConfig;

public class ConfigHelper extends BaseRemoteHelper {
    private static final String NEWS_METHOD = "get_config";

    private static final List<Long> DEFAULT_VERIFY_LIST = Arrays.asList(
            1349472891L,
            1339737452L,
            1302242053L,
            1715773134L
    );

    private static final class InstanceHolder {
        private static final ConfigHelper instance = new ConfigHelper();
    }

    public static ConfigHelper getInstance() {
        return InstanceHolder.instance;
    }

    public static boolean isChatCat(TLRPC.Chat chat) {
        return getVerify().stream().anyMatch(id -> id == chat.id || id == -2000000000000L - chat.id);
    }

    public static List<Long> getVerify() {
        Config config = getInstance().getConfig();
        if (config == null || config.verify == null) {
            return DEFAULT_VERIFY_LIST;
        }
        return config.verify;
    }

    public static List<Crypto> getCryptos() {
        Config config = getInstance().getConfig();
        if (config == null || config.cryptos == null) {
            return Collections.emptyList();
        }
        return config.cryptos;
    }

    public static List<News> getNews() {
        Config config = getInstance().getConfig();
        if (config == null || config.news == null) {
            return Collections.emptyList();
        }
        ArrayList<News> newsItems = new ArrayList<>();
        config.news.forEach(news -> {
            if (news.chineseOnly != null && news.chineseOnly && !NekoConfig.isChineseUser) {
                return;
            }
            if (news.direct != null && news.direct && !Extra.isDirectApp()) {
                return;
            }
            if (news.source != null && news.source.equals(BuildConfig.BUILD_TYPE)) {
                return;
            }
            if (news.maxVersion != null && news.maxVersion < BuildConfig.VERSION_CODE) {
                return;
            }
            if (news.minVersion != null && news.minVersion > BuildConfig.VERSION_CODE) {
                return;
            }
            newsItems.add(news);
        });
        return newsItems;
    }

    public static void overrideChat(TLRPC.Chat chat) {
        Config config = getInstance().getConfig();
        if (config == null || config.chatOverrides == null) {
            return;
        }
        config.chatOverrides.forEach(chatOverride -> {
            if (chatOverride.id == chat.id) {
                if (chatOverride.statusEmojiId != null) {
                    var status = new TLRPC.TL_emojiStatus();
                    status.document_id = chatOverride.statusEmojiId;
                    chat.flags |= 512;
                    chat.emoji_status = status;
                }
                if (chatOverride.colorId != null || chatOverride.backgroundEmojiId != null) {
                    var color = new TLRPC.TL_peerColor();
                    if (chatOverride.colorId != null) {
                        color.flags |= 1;
                        color.color = chatOverride.colorId;
                    }
                    if (chatOverride.backgroundEmojiId != null) {
                        color.flags |= 2;
                        color.background_emoji_id = chatOverride.backgroundEmojiId;
                    }
                    chat.flags |= 128;
                    chat.color = color;
                }
                if (chatOverride.profileColorId != null || chatOverride.profileBackgroundEmojiId != null) {
                    var color = new TLRPC.TL_peerColor();
                    if (chatOverride.profileColorId != null) {
                        color.flags |= 1;
                        color.color = chatOverride.profileColorId;
                    }
                    if (chatOverride.profileBackgroundEmojiId != null) {
                        color.flags |= 2;
                        color.background_emoji_id = chatOverride.profileBackgroundEmojiId;
                    }
                    chat.flags |= 256;
                    chat.profile_color = color;
                }
                if (chatOverride.botVerificationEmojiId != null) {
                    chat.flags2 |= 8192;
                    chat.bot_verification_icon = chatOverride.botVerificationEmojiId;
                }
            }
        });
    }

    public static void overrideChatFull(TLRPC.ChatFull chatFull) {
        Config config = getInstance().getConfig();
        if (config == null || config.chatOverrides == null) {
            return;
        }
        config.chatOverrides.forEach(chatOverride -> {
            if (chatOverride.id == chatFull.id) {
                if (chatOverride.botVerificationEmojiId != null) {
                    var verification = new TL_bots.botVerification();
                    verification.icon = chatOverride.botVerificationEmojiId;
                    verification.description = chatOverride.botVerificationDescription;
                    chatFull.flags2 |= 131072;
                    chatFull.bot_verification = verification;
                }
            }
        });
    }

    private Config getConfig() {
        String string = getInstance().getJSON();
        try {
            return GSON.fromJson(string, Config.class);
        } catch (Throwable t) {
            FileLog.e(t);
            getInstance().load();
            return null;
        }
    }

    @Override
    protected void onError(String text, Delegate delegate) {
        FileLog.e("ConfigHelper error = " + text);
    }

    @Override
    protected String getRequestMethod() {
        return NEWS_METHOD;
    }

    @Override
    protected String getRequestParams() {
        return "";
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

    public static class ChatOverride {
        @SerializedName("id")
        @Expose
        public long id;
        @SerializedName("status_emoji_id")
        @Expose
        public Long statusEmojiId;
        @SerializedName("color_id")
        @Expose
        public Integer colorId;
        @SerializedName("background_emoji_id")
        @Expose
        public Long backgroundEmojiId;
        @SerializedName("profile_color_id")
        @Expose
        public Integer profileColorId;
        @SerializedName("profile_background_emoji_id")
        @Expose
        public Long profileBackgroundEmojiId;
        @SerializedName("bot_verification_emoji_id")
        @Expose
        public Long botVerificationEmojiId;
        @SerializedName("bot_verification_description")
        @Expose
        public String botVerificationDescription;
    }

    public static class Crypto {
        @SerializedName("currency")
        @Expose
        public String currency;
        @SerializedName("chain")
        @Expose
        public String chain;
        @SerializedName("address")
        @Expose
        public String address;
    }

    public static class Config {
        @SerializedName("verify")
        @Expose
        public List<Long> verify;
        @SerializedName("newsv3")
        @Expose
        public List<News> news;
        @SerializedName("chat_overrides")
        @Expose
        public List<ChatOverride> chatOverrides;
        @SerializedName("cryptos")
        @Expose
        public List<Crypto> cryptos;
    }
}
