package tw.nekomimi.nekogram.helpers;

import android.content.pm.PackageInfo;
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

import tw.nekomimi.nekogram.NekoConfig;

public class UpdateHelper {

    private static volatile UpdateHelper Instance;
    private int currentVersion;
    private int currentAbi;

    UpdateHelper() {
        if (NekoConfig.installedFromPlay) return;
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            currentVersion = pInfo.versionCode / 10;
            currentAbi = pInfo.versionCode % 10;
        } catch (Exception ignore) {
            currentVersion = -1;
            currentAbi = -1;
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

    private JSONObject getShouldUpdateVersion(ArrayList<TLRPC.Message> messages) {
        int maxVersion = currentVersion;
        JSONObject ref = null;
        for (TLRPC.Message message : messages) {
            if (TextUtils.isEmpty(message.message) || !message.message.startsWith("#update")) {
                continue;
            }
            try {
                JSONObject json = new JSONObject(message.message.substring(7).trim());
                int version_code = json.getInt("version_code");
                if (version_code > maxVersion) {
                    maxVersion = version_code;
                    ref = json;
                }
            } catch (JSONException ignore) {

            }
        }
        return ref;
    }

    private void getNewVersionMessagesTLCallback(UpdateHelperDelegate delegate, int dialog_id, JSONObject json,
                                                 TLObject response, TLRPC.TL_error error) {

        if (error == null) {
            final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            getMessagesController().removeDeletedMessagesFromArray(dialog_id, res.messages);

            HashMap<Integer, TLRPC.Message> messages = new HashMap<>();
            for (TLRPC.Message message : res.messages) {
                messages.put(message.id, message);
            }
            try {
                TLRPC.TL_help_appUpdate update = new TLRPC.TL_help_appUpdate();
                update.version = json.getString("version");
                update.can_not_skip = json.getBoolean("can_not_skip");
                if (json.has("url")) {
                    update.url = json.getString("url");
                }
                if (json.has("files")) {
                    TLRPC.Message file = messages.get(json.getJSONArray("files").getInt(currentAbi));
                    if (file != null && file.media != null) {
                        update.document = file.media.document;
                    }
                }
                if (json.has("message")) {
                    JSONObject messageJson = json.getJSONObject("message");
                    String channel = LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername);
                    if (messageJson.has(channel)) {
                        TLRPC.Message message = messages.get(messageJson.getInt(channel));
                        if (message != null) {
                            update.text = message.message;
                            update.entities = message.entities;
                        }
                    }
                }
                if (json.has("sticker")) {
                    TLRPC.Message sticker = messages.get(json.getInt("sticker"));
                    if (sticker != null && sticker.media != null) {
                        update.sticker = sticker.media.document;
                    }
                }
                delegate.didCheckNewVersionAvailable(update, null);
            } catch (JSONException e) {
                delegate.didCheckNewVersionAvailable(null, e.getLocalizedMessage());
            }
        } else {
            delegate.didCheckNewVersionAvailable(null, error.text);
        }
    }

    private void checkNewVersionTLCallback(UpdateHelperDelegate delegate, int dialog_id,
                                           TLObject response, TLRPC.TL_error error) {
        if (error == null) {
            final TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
            getMessagesController().removeDeletedMessagesFromArray(dialog_id, res.messages);

            JSONObject json = getShouldUpdateVersion(res.messages);
            if (json == null) {
                delegate.didCheckNewVersionAvailable(null, null); // no available version
                return;
            }

            try {
                ArrayList<Integer> ids = new ArrayList<>();
                if (json.has("message")) {
                    JSONObject messageJson = json.getJSONObject("message");
                    String channel = LocaleController.getString("OfficialChannelUsername", R.string.OfficialChannelUsername);
                    if (messageJson.has(channel)) {
                        ids.add(messageJson.getInt(channel));
                    }
                }
                if (json.has("sticker")) {
                    ids.add(json.getInt("sticker"));
                }
                if (json.has("files")) {
                    ids.add(json.getJSONArray("files").getInt(currentAbi));
                }

                TLRPC.TL_channels_getMessages req = new TLRPC.TL_channels_getMessages();
                req.channel = getMessagesController().getInputChannel(-dialog_id);
                req.id = ids;
                getConnectionsManager().sendRequest(req, (response1, error1) -> getNewVersionMessagesTLCallback(delegate, dialog_id, json, response1, error1));
            } catch (Exception e) {
                delegate.didCheckNewVersionAvailable(null, e.getLocalizedMessage());
            }
        } else {
            delegate.didCheckNewVersionAvailable(null, error.text);
        }
    }

    public void checkNewVersionAvailable(UpdateHelperDelegate delegate) {
        checkNewVersionAvailable(delegate, false);
    }

    public void checkNewVersionAvailable(UpdateHelperDelegate delegate, boolean forceRefreshAccessHash) {
        if (NekoConfig.installedFromPlay) {
            delegate.didCheckNewVersionAvailable(null, null);
            return;
        }
        TLRPC.TL_contacts_resolveUsername req1 = new TLRPC.TL_contacts_resolveUsername();
        int dialog_id = -1232424156;
        req1.username = "n3e5bd600e33db09e6af702425c50335";
        TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.limit = 10;
        req.offset_id = 0;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        req.q = "#update";
        req.peer = getMessagesController().getInputPeer(dialog_id);
        if (req.peer == null || req.peer.access_hash == 0 || forceRefreshAccessHash) {
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
                getConnectionsManager().sendRequest(req, (response, error) -> checkNewVersionTLCallback(delegate, dialog_id, response, error));
            });
        } else {
            getConnectionsManager().sendRequest(req, (response, error) -> {
                if (error != null) {
                    checkNewVersionAvailable(delegate, true);
                    return;
                }
                checkNewVersionTLCallback(delegate, dialog_id, response, null);
            });
        }
    }
}
