package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.NekoConfig;

public class NewsHelper {

    private static volatile NewsHelper Instance;
    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoconfig", Activity.MODE_PRIVATE);
    private static final ArrayList<NewsItem> DEFAULT_LIST = new ArrayList<>();

    static {
        if (NekoConfig.isChineseUser) {
            DEFAULT_LIST.add(new NewsItem(
                    0,
                    LocaleController.getString("YahagiTitle", R.string.YahagiTitle),
                    LocaleController.getString("YahagiSummary", R.string.YahagiSummary),
                    LocaleController.getString("YahagiLink", R.string.YahagiLink)
            ));
        }
    }


    public static NewsHelper getInstance() {
        NewsHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (NewsHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new NewsHelper();
                }
                return localInstance;
            }
        }
        return localInstance;
    }

    protected MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    protected ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(UserConfig.selectedAccount);
    }

    protected MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(UserConfig.selectedAccount);
    }

    private JSONArray getNews(ArrayList<TLRPC.Message> messages) {
        JSONArray ref = null;
        for (TLRPC.Message message : messages) {
            if (TextUtils.isEmpty(message.message) || !message.message.startsWith("#news")) {
                continue;
            }
            try {
                ref = new JSONArray(message.message.substring(5).trim());
            } catch (JSONException ignore) {

            }
        }
        return ref;
    }

    private void checkNewVersionTLCallback(int dialog_id,
                                           TLObject response, TLRPC.TL_error error) {
        if (error == null) {
            final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            getMessagesController().removeDeletedMessagesFromArray(dialog_id, res.messages);

            JSONArray json = getNews(res.messages);
            if (json == null) {
                preferences.edit()
                        .remove("news_update_time")
                        .remove("news")
                        .apply();
            } else {
                preferences.edit()
                        .putLong("news_update_time", System.currentTimeMillis())
                        .putString("news", json.toString())
                        .apply();
            }
        }
    }

    public static ArrayList<NewsItem> getNews() {
        String json = preferences.getString("news", "");
        if (TextUtils.isEmpty(json)) {
            getInstance().checkNews();
            return DEFAULT_LIST;
        }

        ArrayList<NewsItem> newsItems = new ArrayList<>();
        try {
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject object = jsonArray.getJSONObject(i);
                if (object.getBoolean("chineseOnly") && !NekoConfig.isChineseUser) {
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
            getInstance().checkNews();
            return DEFAULT_LIST;
        }
    }

    public void checkNews() {
        checkNews(false);
    }

    public void checkNews(boolean forceRefreshAccessHash) {
        TLRPC.TL_contacts_resolveUsername req1 = new TLRPC.TL_contacts_resolveUsername();
        int dialog_id = Extra.UPDATE_CHANNEL_ID;
        req1.username = Extra.UPDATE_CHANNEL;
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.limit = 10;
        req.offset_id = 0;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.q = "#news";
        req.peer = getMessagesController().getInputPeer(dialog_id);
        if (req.peer == null || req.peer.access_hash == 0 || forceRefreshAccessHash) {
            getConnectionsManager().sendRequest(req1, (response1, error1) -> {
                if (error1 != null) {
                    return;
                }
                if (!(response1 instanceof TLRPC.TL_contacts_resolvedPeer)) {
                    return;
                }
                TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response1;
                getMessagesController().putUsers(resolvedPeer.users, false);
                getMessagesController().putChats(resolvedPeer.chats, false);
                getMessagesStorage().putUsersAndChats(resolvedPeer.users, resolvedPeer.chats, false, true);
                if ((resolvedPeer.chats == null || resolvedPeer.chats.size() == 0)) {
                    return;
                }
                req.peer = new TLRPC.TL_inputPeerChannel();
                req.peer.channel_id = resolvedPeer.chats.get(0).id;
                req.peer.access_hash = resolvedPeer.chats.get(0).access_hash;
                getConnectionsManager().sendRequest(req, (response, error) -> checkNewVersionTLCallback(dialog_id, response, error));
            });
        } else {
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error != null) {
                    checkNews(true);
                    return;
                }
                checkNewVersionTLCallback(dialog_id, response, null);
            });
        }
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
