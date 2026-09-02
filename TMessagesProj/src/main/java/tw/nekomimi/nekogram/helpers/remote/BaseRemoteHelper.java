package tw.nekomimi.nekogram.helpers.remote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.helpers.InlineBotHelper;

public abstract class BaseRemoteHelper {
    protected static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoremoteconfig", Activity.MODE_PRIVATE);
    public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    protected MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    protected ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(UserConfig.selectedAccount);
    }

    protected MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(UserConfig.selectedAccount);
    }

    protected FileLoader getFileLoader() {
        return FileLoader.getInstance(UserConfig.selectedAccount);
    }

    protected InlineBotHelper getInlineBotHelper() {
        return InlineBotHelper.getInstance(UserConfig.selectedAccount);
    }

    abstract protected void onError(String text, Delegate delegate);

    abstract protected String getRequestMethod();

    abstract protected String getRequestParams();

    public static String getRequestExtra() {
        return " " +
                BuildConfig.VERSION_CODE +
                " " +
                BuildConfig.BUILD_TYPE +
                " " +
                LocaleController.getSystemLocaleStringIso639() +
                " " +
                NekoConfig.userMcc +
                " " +
                SharedConfig.pushString;
    }

    protected String getJSON() {
        var tag = getRequestMethod();
        var json = preferences.getString(tag, "");
        if (TextUtils.isEmpty(json)) {
            load();
            return null;
        }
        var updateTime = preferences.getLong(tag + "_update_time", 0);
        if (Math.abs(System.currentTimeMillis() - updateTime) > 24 * 60 * 60 * 1000) {
            load();
        }
        return json;
    }

    public static String getTextFromInlineResult(TLRPC.BotInlineResult result) {
        return result.send_message != null ? result.send_message.message : result.description;
    }

    protected void onLoadSuccess(ArrayList<TLRPC.BotInlineResult> results, Delegate delegate) {
        var result = !results.isEmpty() ? results.get(0) : null;
        if (result == null) {
            onLoadSuccess(null);
        } else {
            onLoadSuccess(getTextFromInlineResult(result));
        }
    }

    public void onLoadSuccess(String result) {
        var tag = getRequestMethod();
        if (result == null) {
            preferences.edit()
                    .remove(tag + "_update_time")
                    .remove(tag)
                    .apply();
        } else {
            preferences.edit()
                    .putLong(tag + "_update_time", System.currentTimeMillis())
                    .putString(tag, result)
                    .apply();
        }
    }

    public void load() {
        load(null);
    }

    private boolean loading;

    public void load(Delegate delegate) {
        var botInfo = Extra.getHelperBot();
        if (botInfo == null) {
            return;
        }
        if (!UserConfig.getInstance(UserConfig.selectedAccount).isClientActivated()) {
            return;
        }
        if (loading) return;
        loading = true;
        getInlineBotHelper().query(botInfo,
                getRequestMethod() + getRequestParams() + getRequestExtra(),
                (results, error) -> {
                    loading = false;
                    if (error == null) {
                        onLoadSuccess(results, delegate);
                    } else {
                        onError(error, delegate);
                    }
                });
    }

    public interface Delegate {
        void onTLResponse(TLRPC.TL_help_appUpdate res, String error);
    }

    protected static List<TLRPC.MessageEntity> parseBotAPIEntities(MessageEntity[] botEntities, boolean emojiOnly) {
        if (botEntities == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(botEntities)
                .filter(e -> !emojiOnly || e.customEmojiId != null)
                .map(e -> {
                    var entity = switch (e.type) {
                        case "mention" -> new TLRPC.TL_messageEntityMention();
                        case "hashtag" -> new TLRPC.TL_messageEntityHashtag();
                        case "cashtag" -> new TLRPC.TL_messageEntityCashtag();
                        case "bot_command" -> new TLRPC.TL_messageEntityBotCommand();
                        case "url" -> new TLRPC.TL_messageEntityUrl();
                        case "email" -> new TLRPC.TL_messageEntityEmail();
                        case "phone_number" -> new TLRPC.TL_messageEntityPhone();
                        case "bold" -> new TLRPC.TL_messageEntityBold();
                        case "italic" -> new TLRPC.TL_messageEntityItalic();
                        case "underline" -> new TLRPC.TL_messageEntityUnderline();
                        case "strikethrough" -> new TLRPC.TL_messageEntityStrike();
                        case "spoiler" -> new TLRPC.TL_messageEntitySpoiler();
                        //case "blockquote", "expandable_blockquote" -> new TLRPC.TL_messageEntityBlockquote();
                        //case "code" -> new TLRPC.TL_messageEntityCode();
                        //case "pre" -> new TLRPC.TL_messageEntityPre();
                        //case "date_time" -> new TL_messageEntityFormattedDate();
                        case "text_link" -> new TLRPC.TL_messageEntityTextUrl();
                        case "custom_emoji" -> {
                            var emoji = new TLRPC.TL_messageEntityCustomEmoji();
                            emoji.document_id = e.customEmojiId;
                            yield emoji;
                        }
                        default -> new TLRPC.TL_messageEntityUnknown();
                    };
                    entity.offset = e.offset;
                    entity.length = e.length;
                    entity.url = e.url;
                    return entity;
                })
                .toList();
    }

    public static class MessageEntity {
        @SerializedName("type")
        @Expose
        public String type;
        @SerializedName("offset")
        @Expose
        public Integer offset;
        @SerializedName("length")
        @Expose
        public Integer length;
        @SerializedName("url")
        @Expose
        public String url;
        @SerializedName("custom_emoji_id")
        @Expose
        public Long customEmojiId;
    }
}