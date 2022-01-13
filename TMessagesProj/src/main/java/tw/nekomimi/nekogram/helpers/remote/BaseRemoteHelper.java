package tw.nekomimi.nekogram.helpers.remote;

import android.app.Activity;
import android.content.SharedPreferences;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

import tw.nekomimi.nekogram.Extra;

public abstract class BaseRemoteHelper {
    protected static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekoconfig", Activity.MODE_PRIVATE);

    protected MessagesController getMessagesController() {
        return MessagesController.getInstance(UserConfig.selectedAccount);
    }

    protected ConnectionsManager getConnectionsManager() {
        return ConnectionsManager.getInstance(UserConfig.selectedAccount);
    }

    protected MessagesStorage getMessagesStorage() {
        return MessagesStorage.getInstance(UserConfig.selectedAccount);
    }

    abstract protected void onError(String text, Delegate delegate);

    abstract protected String getTag();

    protected JSONObject getJSON() {
        var tag = getTag();
        var json = preferences.getString(tag, "");
        if (TextUtils.isEmpty(json)) {
            load();
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            FileLog.e(e);
            load();
            return null;
        }
    }

    protected void onLoadSuccess(ArrayList<JSONObject> responses, Delegate delegate) {
        var tag = getTag();
        var json = responses.size() > 0 ? responses.get(0) : null;
        if (json == null) {
            preferences.edit()
                    .remove(tag + "_update_time")
                    .remove(tag)
                    .apply();
        } else {
            preferences.edit()
                    .putLong(tag + "_update_time", System.currentTimeMillis())
                    .putString(tag, json.toString())
                    .apply();
        }
    }

    private void onGetMessageSuccess(TLObject response, Delegate delegate) {
        var tag = "#" + getTag();
        final var res = (TLRPC.messages_Messages) response;
        getMessagesController().removeDeletedMessagesFromArray(Extra.UPDATE_CHANNEL_ID, res.messages);
        ArrayList<JSONObject> responses = new ArrayList<>();
        for (var message : res.messages) {
            if (TextUtils.isEmpty(message.message) || !message.message.startsWith(tag)) {
                continue;
            }
            try {
                responses.add(new JSONObject(message.message.substring(tag.length()).trim()));
            } catch (JSONException e) {
                FileLog.e(e);
            }
        }
        onLoadSuccess(responses, delegate);
    }

    public void load() {
        load(false, null);
    }

    public void load(Delegate delegate) {
        load(false, delegate);
    }

    private void load(boolean forceRefreshAccessHash, Delegate delegate) {
        var tag = "#" + getTag();
        int dialog_id = Extra.UPDATE_CHANNEL_ID;
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.limit = 10;
        req.offset_id = 0;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.q = tag;
        req.peer = getMessagesController().getInputPeer(dialog_id);
        if (req.peer == null || req.peer.access_hash == 0 || forceRefreshAccessHash) {
            TLRPC.TL_contacts_resolveUsername req1 = new TLRPC.TL_contacts_resolveUsername();
            req1.username = Extra.UPDATE_CHANNEL;
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
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (error == null) {
                        onGetMessageSuccess(response, delegate);
                    } else {
                        onError(error.text, delegate);
                    }
                });
            });
        } else {
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    onGetMessageSuccess(response, delegate);
                } else {
                    load(true, delegate);
                }
            });
        }
    }

    public interface Delegate {
        void onTLResponse(TLRPC.TL_help_appUpdate res, String error);
    }
}
