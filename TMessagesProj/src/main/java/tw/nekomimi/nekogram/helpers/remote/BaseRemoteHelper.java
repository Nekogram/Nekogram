package tw.nekomimi.nekogram.helpers.remote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

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
                NekoConfig.isChineseUser;
    }

    protected String getJSON() {
        var tag = getRequestMethod();
        var json = preferences.getString(tag, "");
        if (TextUtils.isEmpty(json)) {
            load();
            return null;
        }
        return json;
    }

    public static String getTextFromInlineResult(TLRPC.BotInlineResult result) {
        return result.send_message != null ? result.send_message.message : result.description;
    }

    protected void onLoadSuccess(ArrayList<TLRPC.BotInlineResult> results, Delegate delegate) {
        var tag = getRequestMethod();
        var result = !results.isEmpty() ? results.get(0) : null;
        if (result == null) {
            preferences.edit()
                    .remove(tag + "_update_time")
                    .remove(tag)
                    .apply();
        } else {
            var json = getTextFromInlineResult(result);
            preferences.edit()
                    .putLong(tag + "_update_time", System.currentTimeMillis())
                    .putString(tag, json)
                    .apply();
        }
    }

    public void load() {
        load(null);
    }

    public void load(Delegate delegate) {
        var botInfo = Extra.getHelperBot();
        if (botInfo == null) {
            return;
        }
        getInlineBotHelper().query(botInfo,
                getRequestMethod() + getRequestParams() + getRequestExtra(),
                (results, error) -> {
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
}