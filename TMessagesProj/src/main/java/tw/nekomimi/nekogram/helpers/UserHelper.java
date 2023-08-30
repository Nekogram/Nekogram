package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.TopicsFragment;

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

    public void openByDialogId(long dialogId, Activity activity, Utilities.Callback<BaseFragment> callback, Browser.Progress progress) {
        if (dialogId == 0 || activity == null) {
            return;
        }
        AlertDialog progressDialog = progress != null ? null : new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        searchPeer(dialogId, (user, chat) -> {
            if (progress != null) {
                progress.end();
            }
            if (progressDialog != null) {
                try {
                    progressDialog.dismiss();
                } catch (Exception ignored) {

                }
            }
            Bundle args = new Bundle();
            if (user != null) {
                args.putLong("user_id", user.id);
                callback.run(new ProfileActivity(args));
            } else if (chat != null) {
                args.putLong("chat_id", chat.id);
                if (ChatObject.isForum(chat)) {
                    callback.run(new TopicsFragment(args));
                } else {
                    callback.run(new ChatActivity(args));
                }
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

    public void searchPeer(long dialogId, Utilities.Callback2<TLRPC.User, TLRPC.Chat> callback) {
        if (dialogId < 0) {
            searchChat(-dialogId, chat -> callback.run(null, chat));
        } else {
            searchUser(dialogId, user -> callback.run(user, null));
        }
    }

    public void searchUser(long userId, Utilities.Callback<TLRPC.User> callback) {
        var user = getMessagesController().getUser(userId);
        if (user != null) {
            callback.run(user);
            return;
        }
        searchUser(Extra.getUserInfoBot(false), userId, user1 -> {
            if (user1 == null) {
                searchUser(Extra.getUserInfoBot(true), userId, callback);
            } else {
                callback.run(user1);
            }
        });
    }

    public void searchChat(long chatId, Utilities.Callback<TLRPC.Chat> callback) {
        var chat = getMessagesController().getChat(chatId);
        if (chat != null) {
            callback.run(chat);
            return;
        }
        searchChat(Extra.getUserInfoBot(false), chatId, chat1 -> {
            if (chat1 == null) {
                searchChat(Extra.getUserInfoBot(true), chatId, callback);
            } else {
                callback.run(chat1);
            }
        });
    }

    private void resolveUser(String userName, long userId, Utilities.Callback<TLRPC.User> callback) {
        resolvePeer(userName, peer -> {
            if (peer instanceof TLRPC.TL_peerUser) {
                callback.run(peer.user_id == userId ? getMessagesController().getUser(userId) : null);
            } else {
                callback.run(null);
            }
        });
    }

    private void resolveChat(String userName, long chatId, Utilities.Callback<TLRPC.Chat> callback) {
        resolvePeer(userName, peer -> {
            if (peer instanceof TLRPC.TL_peerChat || peer instanceof TLRPC.TL_peerChannel) {
                if (peer.chat_id == chatId || peer.channel_id == chatId) {
                    callback.run(getMessagesController().getChat(chatId));
                } else {
                    callback.run(null);
                }
            } else {
                callback.run(null);
            }
        });
    }

    private void resolvePeer(String userName, Utilities.Callback<TLRPC.Peer> callback) {
        TLRPC.TL_contacts_resolveUsername req = new TLRPC.TL_contacts_resolveUsername();
        req.username = userName;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response != null) {
                TLRPC.TL_contacts_resolvedPeer res = (TLRPC.TL_contacts_resolvedPeer) response;
                getMessagesController().putUsers(res.users, false);
                getMessagesController().putChats(res.chats, false);
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                callback.run(res.peer);
            } else {
                callback.run(null);
            }
        }));
    }

    private void searchUser(Extra.UserInfoBot botInfo, long userId, Utilities.Callback<TLRPC.User> callback) {
        searchPeer(botInfo, userId, true, true, lines -> {
            if (lines == null) {
                callback.run(null);
                return;
            }
            var fakeUser = botInfo.parseUser(lines);
            if (fakeUser.id == 0) {
                callback.run(null);
                return;
            }
            if (fakeUser.username != null) {
                resolveUser(fakeUser.username, fakeUser.id, user -> {
                    if (user != null) {
                        callback.run(user);
                    } else {
                        fakeUser.username = null;
                        callback.run(fakeUser);
                    }
                });
            } else {
                callback.run(fakeUser);
            }
        });
    }

    private void searchChat(Extra.UserInfoBot botInfo, long chatId, Utilities.Callback<TLRPC.Chat> callback) {
        searchPeer(botInfo, -1000000000000L - chatId, true, true, lines -> {
            if (lines == null) {
                callback.run(null);
                return;
            }
            var fakeChat = botInfo.parseChat(lines);
            if (fakeChat.id == 0) {
                callback.run(null);
                return;
            }
            if (fakeChat.username != null) {
                resolveChat(fakeChat.username, fakeChat.id, chat -> {
                    if (chat != null) {
                        callback.run(chat);
                    } else {
                        fakeChat.username = null;
                        callback.run(fakeChat);
                    }
                });
            } else {
                callback.run(fakeChat);
            }
        });
    }

    private void searchPeer(Extra.UserInfoBot botInfo, long id, boolean searchUser, boolean cache, Utilities.Callback<String[]> callback) {
        var bot = getMessagesController().getUser(botInfo.getId());
        if (bot == null) {
            if (searchUser) {
                resolveUser(botInfo.getUsername(), botInfo.getId(), user -> searchPeer(botInfo, id, false, false, callback));
            } else {
                callback.run(null);
            }
            return;
        }

        var key = "peer_search_" + id + "_" + botInfo.getId();
        RequestDelegate requestDelegate = (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (cache && (!(response instanceof TLRPC.messages_BotResults) || ((TLRPC.messages_BotResults) response).results.isEmpty())) {
                searchPeer(botInfo, id, searchUser, false, callback);
                return;
            }

            if (response instanceof TLRPC.messages_BotResults) {
                TLRPC.messages_BotResults res = (TLRPC.messages_BotResults) response;
                if (!cache && res.cache_time != 0) {
                    getMessagesStorage().saveBotCache(key, res);
                }
                if (res.results.isEmpty()) {
                    callback.run(null);
                    return;
                }
                var result = res.results.get(0);
                if (result.send_message == null || TextUtils.isEmpty(result.send_message.message)) {
                    callback.run(null);
                    return;
                }
                var lines = result.send_message.message.split("\n");
                if (lines.length < 3) {
                    callback.run(null);
                    return;
                }
                callback.run(lines);
            } else {
                callback.run(null);
            }
        });

        if (cache) {
            getMessagesStorage().getBotCache(key, requestDelegate);
        } else {
            TLRPC.TL_messages_getInlineBotResults req = new TLRPC.TL_messages_getInlineBotResults();
            req.query = String.valueOf(id);
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
