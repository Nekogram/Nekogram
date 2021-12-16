package tw.nekomimi.nekogram.helpers;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

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
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import tw.nekomimi.nekogram.Extra;

public class UpdateHelper {
    public static final String UPDATE_TAG = "#updatev2";

    private static volatile UpdateHelper Instance;
    private int installedVersion;
    private String installedAbi;

    UpdateHelper() {
        try {
            var pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            installedVersion = pInfo.versionCode / 10;
            switch (pInfo.versionCode % 10) {
                case 1:
                case 3:
                    installedAbi = "arm-v7a";
                    break;
                case 2:
                case 4:
                    installedAbi = "x86";
                    break;
                case 5:
                case 7:
                    installedAbi = "arm64-v8a";
                    break;
                case 6:
                case 8:
                    installedAbi = "x86_64";
                    break;
                case 0:
                case 9:
                    installedAbi = "universal";
                    break;
            }
        } catch (Exception ignore) {
            installedVersion = -1;
            installedAbi = "universal";
        }
    }

    /**
     * @param date {long} - date in milliseconds
     */
    public static String formatDateUpdate(long date) {
        long epoch;
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            epoch = pInfo.lastUpdateTime;
        } catch (Exception e) {
            epoch = 0;
        }
        if (date <= epoch) {
            return LocaleController.formatString("LastUpdateNever", R.string.LastUpdateNever);
        }
        try {
            Calendar rightNow = Calendar.getInstance();
            int day = rightNow.get(Calendar.DAY_OF_YEAR);
            int year = rightNow.get(Calendar.YEAR);
            rightNow.setTimeInMillis(date);
            int dateDay = rightNow.get(Calendar.DAY_OF_YEAR);
            int dateYear = rightNow.get(Calendar.YEAR);

            if (dateDay == day && year == dateYear) {
                if (Math.abs(System.currentTimeMillis() - date) < 60000L) {
                    return LocaleController.formatString("LastUpdateRecently", R.string.LastUpdateRecently);
                }
                return LocaleController.formatString("LastUpdateFormatted", R.string.LastUpdateFormatted, LocaleController.formatString("TodayAtFormatted", R.string.TodayAtFormatted,
                        LocaleController.getInstance().formatterDay.format(new Date(date))));
            } else if (dateDay + 1 == day && year == dateYear) {
                return LocaleController.formatString("LastUpdateFormatted", R.string.LastUpdateFormatted, LocaleController.formatString("YesterdayAtFormatted", R.string.YesterdayAtFormatted,
                        LocaleController.getInstance().formatterDay.format(new Date(date))));
            } else if (Math.abs(System.currentTimeMillis() - date) < 31536000000L) {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                        LocaleController.getInstance().formatterDayMonth.format(new Date(date)),
                        LocaleController.getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LastUpdateDateFormatted", R.string.LastUpdateDateFormatted, format);
            } else {
                String format = LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime,
                        LocaleController.getInstance().formatterYear.format(new Date(date)),
                        LocaleController.getInstance().formatterDay.format(new Date(date)));
                return LocaleController.formatString("LastUpdateDateFormatted", R.string.LastUpdateDateFormatted, format);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "LOC_ERR";
    }

    public interface UpdateHelperDelegate {
        void didCheckNewVersionAvailable(TLRPC.TL_help_appUpdate res, String error);
    }

    public static UpdateHelper getInstance() {
        UpdateHelper localInstance = Instance;
        if (localInstance == null) {
            synchronized (UpdateHelper.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new UpdateHelper();
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

    private int getPreferredAbiFile(JSONObject files) throws JSONException {
        for (String abi : Build.SUPPORTED_ABIS) {
            if (files.has(abi)) {
                return files.getInt(abi);
            }
        }
        if (files.has(installedAbi)) {
            return files.getInt(installedAbi);
        } else {
            return files.getInt("universal");
        }
    }

    private JSONObject getShouldUpdateVersion(ArrayList<TLRPC.Message> messages) {
        int maxVersion = installedVersion;
        JSONObject ref = null;
        for (var message : messages) {
            if (TextUtils.isEmpty(message.message) || !message.message.startsWith(UPDATE_TAG)) {
                continue;
            }
            try {
                JSONObject json = new JSONObject(message.message.substring(UPDATE_TAG.length()).trim());
                int version_code = json.getInt("version_code");
                if (version_code > maxVersion) {
                    maxVersion = version_code;
                    ref = json;
                }
            } catch (JSONException e) {
                FileLog.e(e);
            }
        }
        return ref;
    }

    private void getNewVersionMessagesCallback(UpdateHelperDelegate delegate, int dialog_id, JSONObject json,
                                               HashMap<String, Integer> ids, TLObject response) {
        try {
            var update = new TLRPC.TL_help_appUpdate();
            update.version = json.getString("version");
            update.can_not_skip = json.getBoolean("can_not_skip");
            if (json.has("url")) {
                update.url = json.getString("url");
                update.flags |= 4;
            }
            if (response != null) {
                var res = (TLRPC.messages_Messages) response;
                getMessagesController().removeDeletedMessagesFromArray(dialog_id, res.messages);
                var messages = new HashMap<Integer, TLRPC.Message>();
                for (var message : res.messages) {
                    messages.put(message.id, message);
                }

                if (ids.containsKey("file")) {
                    var file = messages.get(ids.get("file"));
                    if (file != null && file.media != null) {
                        update.document = file.media.document;
                        update.flags |= 2;
                    }
                }
                if (ids.containsKey("message")) {
                    var message = messages.get(ids.get("message"));
                    if (message != null) {
                        update.text = message.message;
                        update.entities = message.entities;
                    }
                }
                if (ids.containsKey("sticker")) {
                    var sticker = messages.get(ids.get("sticker"));
                    if (sticker != null && sticker.media != null) {
                        update.sticker = sticker.media.document;
                        update.flags |= 8;
                    }
                }
            }
            delegate.didCheckNewVersionAvailable(update, null);
        } catch (JSONException e) {
            FileLog.e(e);
            delegate.didCheckNewVersionAvailable(null, e.getLocalizedMessage());
        }
    }

    private void checkNewVersionCallback(UpdateHelperDelegate delegate, int dialog_id, TLObject response) {
        var res = (TLRPC.messages_Messages) response;
        getMessagesController().removeDeletedMessagesFromArray(dialog_id, res.messages);
        var json = getShouldUpdateVersion(res.messages);
        if (json == null) {
            delegate.didCheckNewVersionAvailable(null, null); // no available version
            return;
        }

        try {
            var ids = new HashMap<String, Integer>();
            if (json.has("messages")) {
                var messageJson = json.getJSONObject("messages");
                var channel = LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername);
                if (messageJson.has(channel)) {
                    ids.put("message", messageJson.getInt(channel));
                }
            }
            if (json.has("sticker")) {
                ids.put("sticker", json.getInt("sticker"));
            }
            if (json.has("files")) {
                ids.put("file", getPreferredAbiFile(json.getJSONObject("files")));
            }

            if (ids.isEmpty()) {
                getNewVersionMessagesCallback(delegate, dialog_id, json, null, null);
            } else {
                var req = new TLRPC.TL_channels_getMessages();
                req.channel = getMessagesController().getInputChannel(-dialog_id);
                req.id = new ArrayList<>(ids.values());
                getConnectionsManager().sendRequest(req, (response1, error1) -> {
                    if (error1 == null) {
                        getNewVersionMessagesCallback(delegate, dialog_id, json, ids, response1);
                    } else {
                        delegate.didCheckNewVersionAvailable(null, error1.text);
                    }
                });
            }
        } catch (JSONException e) {
            FileLog.e(e);
            delegate.didCheckNewVersionAvailable(null, e.getLocalizedMessage());
        }
    }

    public void checkNewVersionAvailable(UpdateHelperDelegate delegate) {
        NewsHelper.getInstance().checkNews();
        checkNewVersionAvailable(delegate, false);
    }

    public void checkNewVersionAvailable(UpdateHelperDelegate delegate, boolean forceRefreshAccessHash) {
        var dialog_id = Extra.UPDATE_CHANNEL_ID;
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.limit = 10;
        req.offset_id = 0;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.q = UPDATE_TAG;
        req.peer = getMessagesController().getInputPeer(dialog_id);
        if (req.peer == null || req.peer.access_hash == 0 || forceRefreshAccessHash) {
            var req1 = new TLRPC.TL_contacts_resolveUsername();
            req1.username = Extra.UPDATE_CHANNEL;
            getConnectionsManager().sendRequest(req1, (response1, error1) -> {
                if (error1 != null) {
                    delegate.didCheckNewVersionAvailable(null, error1.text);
                    return;
                }
                if (!(response1 instanceof TLRPC.TL_contacts_resolvedPeer)) {
                    delegate.didCheckNewVersionAvailable(null, "Unexpected TL_contacts_resolvedPeer response");
                    return;
                }
                TLRPC.TL_contacts_resolvedPeer resolvedPeer = (TLRPC.TL_contacts_resolvedPeer) response1;
                getMessagesController().putUsers(resolvedPeer.users, false);
                getMessagesController().putChats(resolvedPeer.chats, false);
                getMessagesStorage().putUsersAndChats(resolvedPeer.users, resolvedPeer.chats, false, true);
                if ((resolvedPeer.chats == null || resolvedPeer.chats.size() == 0)) {
                    delegate.didCheckNewVersionAvailable(null, "Unexpected TL_contacts_resolvedPeer chat size");
                    return;
                }
                req.peer = new TLRPC.TL_inputPeerChannel();
                req.peer.channel_id = resolvedPeer.chats.get(0).id;
                req.peer.access_hash = resolvedPeer.chats.get(0).access_hash;
                getConnectionsManager().sendRequest(req, (response, error) -> {
                    if (error == null) {
                        checkNewVersionCallback(delegate, dialog_id, response);
                    } else {
                        delegate.didCheckNewVersionAvailable(null, error.text);
                    }
                });
            });
        } else {
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error == null) {
                    checkNewVersionCallback(delegate, dialog_id, response);
                } else {
                    checkNewVersionAvailable(delegate, true);
                }
            });
        }
    }
}
