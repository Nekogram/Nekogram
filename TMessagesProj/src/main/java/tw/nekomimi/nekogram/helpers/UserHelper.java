package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;

import java.util.HashMap;
import java.util.Locale;

import tw.nekomimi.nekogram.Extra;

public class UserHelper extends BaseController {

    private static final UserHelper[] Instance = new UserHelper[UserConfig.MAX_ACCOUNT_COUNT];

    public UserHelper(int num) {
        super(num);
    }

    public static UserHelper getInstance(int num) {
        UserHelper localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (UserHelper.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new UserHelper(num);
                }
            }
        }
        return localInstance;
    }

    public interface UserCallback {
        void onResult(TLRPC.User user);
    }

    public void openById(Long userId, Activity activity, Runnable runnable, Browser.Progress progress) {
        if (userId == 0 || activity == null) {
            return;
        }
        TLRPC.User user = getMessagesController().getUser(userId);
        if (user != null) {
            runnable.run();
        } else {
            AlertDialog progressDialog = progress != null ? null : new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);

            searchUser(userId, user1 -> {
                if (progress != null) {
                    progress.end();
                }
                if (progressDialog != null) {
                    try {
                        progressDialog.dismiss();
                    } catch (Exception ignored) {

                    }
                }
                if (user1 != null && user1.access_hash != 0) {
                    runnable.run();
                }
            });
            if (progress != null) {
                progress.init();
            } else {
                try {
                    progressDialog.showDelayed(300);
                } catch (Exception ignore) {

                }
            }
        }
    }

    public void searchUser(long userId, UserCallback callback) {
        var user = getMessagesController().getUser(userId);
        if (user != null) {
            callback.onResult(user);
            return;
        }
        searchUser(userId, true, true, callback);
    }

    private void resolveUser(String userName, long userId, UserCallback callback) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = userName;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response != null) {
                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                getMessagesController().putUsers(res.users, false);
                getMessagesController().putChats(res.chats, false);
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                callback.onResult(res.peer.user_id == userId ? getMessagesController().getUser(userId) : null);
            } else {
                callback.onResult(null);
            }
        }));
    }

    protected void searchUser(long userId, boolean searchUser, boolean cache, UserCallback callback) {
        var bot = getMessagesController().getUser(Extra.USER_INFO_BOT_ID);
        if (bot == null) {
            if (searchUser) {
                resolveUser(Extra.USER_INFO_BOT, Extra.USER_INFO_BOT_ID, user -> searchUser(userId, false, false, callback));
            } else {
                callback.onResult(null);
            }
            return;
        }

        var key = "user_search_" + userId;
        RequestDelegate requestDelegate = (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (cache && (!(response instanceof TLRPC.messages_BotResults) || ((TLRPC.messages_BotResults) response).results.isEmpty())) {
                searchUser(userId, searchUser, false, callback);
                return;
            }

            if (response instanceof TLRPC.messages_BotResults) {
                TLRPC.messages_BotResults res = (TLRPC.messages_BotResults) response;
                if (!cache && res.cache_time != 0) {
                    getMessagesStorage().saveBotCache(key, res);
                }
                if (res.results.isEmpty()) {
                    callback.onResult(null);
                    return;
                }
                var result = res.results.get(0);
                if (result.send_message == null || TextUtils.isEmpty(result.send_message.message)) {
                    callback.onResult(null);
                    return;
                }
                var lines = result.send_message.message.split("\n");
                if (lines.length < 3) {
                    callback.onResult(null);
                    return;
                }
                var fakeUser = new TLRPC.TL_user();
                for (var line : lines) {
                    line = line.replaceAll("\\p{C}", "").trim();
                    if (line.startsWith("\uD83D\uDC64")) {
                        fakeUser.id = Utilities.parseLong(line.replace("\uD83D\uDC64", ""));
                    } else if (line.startsWith("\uD83D\uDC66\uD83C\uDFFB")) {
                        fakeUser.first_name = line.replace("\uD83D\uDC66\uD83C\uDFFB", "").trim();
                    } else if (line.startsWith("\uD83D\uDC6A")) {
                        fakeUser.last_name = line.replace("\uD83D\uDC6A", "").trim();
                    } else if (line.startsWith("\uD83C\uDF10")) {
                        fakeUser.username = line.replace("\uD83C\uDF10", "").replace("@", "").trim();
                    }
                }
                if (fakeUser.id == 0) {
                    callback.onResult(null);
                    return;
                }
                if (fakeUser.username != null) {
                    resolveUser(fakeUser.username, fakeUser.id, user -> {
                        if (user != null) {
                            callback.onResult(user);
                        } else {
                            fakeUser.username = null;
                            callback.onResult(fakeUser);
                        }
                    });
                } else {
                    callback.onResult(fakeUser);
                }
            } else {
                callback.onResult(null);
            }
        });

        if (cache) {
            getMessagesStorage().getBotCache(key, requestDelegate);
        } else {
            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.query = String.valueOf(userId);
            req.bot = getMessagesController().getInputUser(bot);
            req.offset = "";
            req.peer = new TLRPC.TL_inputPeerEmpty();
            getConnectionsManager().sendRequest(req, requestDelegate, ConnectionsManager.RequestFlagFailOnServerErrors);
        }
    }

    private static String getDCLocation(int dc) {
        switch (dc) {
            case 1:
            case 3:
                return "Miami";
            case 2:
            case 4:
                return "Amsterdam";
            case 5:
                return "Singapore";
            default:
                return "Unknown";
        }
    }

    private static String getDCName(int dc) {
        switch (dc) {
            case 1:
                return "Pluto";
            case 2:
                return "Venus";
            case 3:
                return "Aurora";
            case 4:
                return "Vesta";
            case 5:
                return "Flora";
            default:
                return "Unknown";
        }
    }

    public static String formatDCString(int dc) {
        return String.format(Locale.US, "DC%d %s, %s", dc, UserHelper.getDCName(dc), UserHelper.getDCLocation(dc));
    }

    private static final HashMap<Long, RegDate> regDates = new HashMap<>();

    public static String formatRegDate(RegDate regDate) {
        if (regDate.error == null) {
            switch (regDate.type) {
                case 0:
                    return LocaleController.formatString("RegistrationDateApproximately", R.string.RegistrationDateApproximately, regDate.date);
                case 2:
                    return LocaleController.formatString("RegistrationDateNewer", R.string.RegistrationDateNewer, regDate.date);
                case 3:
                    return LocaleController.formatString("RegistrationDateOlder", R.string.RegistrationDateOlder, regDate.date);
                default:
                    return regDate.date;
            }
        } else {
            return regDate.error;
        }
    }

    public static RegDate getRegDate(long userId) {
        return regDates.get(userId);
    }

    public static void getRegDate(long userId, Utilities.Callback<RegDate> callback) {
        RegDate regDate = regDates.get(userId);
        if (regDate != null) {
            callback.run(regDate);
            return;
        }
        Extra.getRegDate(userId, arg -> {
            if (arg != null && arg.error == null) {
                regDates.put(userId, arg);
            }
            callback.run(arg);
        });
    }

    public static class RegDate {
        public int type;
        public String date;
        public String error;
    }
}
