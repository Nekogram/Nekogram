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

    void resolveUser(String userName, long userId, Utilities.Callback<TLRPC.User> callback) {
        var user = getMessagesController().getUser(userId);
        if (user != null) {
            callback.run(user);
            return;
        }
        resolvePeer(userName, peer -> {
            if (peer instanceof TLRPC.TL_peerUser) {
                callback.run(peer.user_id == userId ? getMessagesController().getUser(userId) : null);
            } else {
                callback.run(null);
            }
        });
    }

    private void resolveChat(String userName, long chatId, Utilities.Callback<TLRPC.Chat> callback) {
        var chat = getMessagesController().getChat(chatId);
        if (chat != null) {
            callback.run(chat);
            return;
        }
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

    private void searchUser(UserInfoBot botInfo, long userId, Utilities.Callback<TLRPC.User> callback) {
        searchPeer(botInfo, userId, lines -> {
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

    private void searchChat(UserInfoBot botInfo, long chatId, Utilities.Callback<TLRPC.Chat> callback) {
        searchPeer(botInfo, -1000000000000L - chatId, lines -> {
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

    private void searchPeer(UserInfoBot botInfo, long id, Utilities.Callback<String[]> callback) {
        if (botInfo == null) {
            return;
        }
        getInlineBotHelper().query(botInfo, String.valueOf(id), (results, error) -> {
            if (results == null) {
                callback.run(null);
                return;
            }
            if (results.isEmpty()) {
                callback.run(null);
                return;
            }
            var result = results.get(0);
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
        });
    }

    private static String getDCLocation(int dc) {
        return switch (dc) {
            case 1, 3 -> "Miami";
            case 2, 4 -> "Amsterdam";
            case 5 -> "Singapore";
            default -> "Unknown";
        };
    }

    private static String getDCName(int dc) {
        return switch (dc) {
            case 1 -> "Pluto";
            case 2 -> "Venus";
            case 3 -> "Aurora";
            case 4 -> "Vesta";
            case 5 -> "Flora";
            default -> "Unknown";
        };
    }

    public static String formatDCString(int dc) {
        return String.format(Locale.US, "DC%d %s, %s", dc, UserHelper.getDCName(dc), UserHelper.getDCLocation(dc));
    }

    private static final HashMap<Long, RegDate> regDates = new HashMap<>();

    public static String formatRegDate(RegDate regDate) {
        if (regDate.error == null) {
            return switch (regDate.type) {
                case 0 ->
                        LocaleController.formatString(R.string.RegistrationDateApproximately, regDate.date);
                case 2 ->
                        LocaleController.formatString(R.string.RegistrationDateNewer, regDate.date);
                case 3 ->
                        LocaleController.formatString(R.string.RegistrationDateOlder, regDate.date);
                default -> regDate.date;
            };
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

    public interface BotInfo {
        long getId();

        String getUsername();
    }

    abstract public static class UserInfoBot implements BotInfo {
        abstract public TLRPC.TL_user parseUser(String[] lines);

        abstract public TLRPC.TL_chat parseChat(String[] lines);
    }

    public static class RegDate {
        public int type;
        public String date;
        public String error;
    }
}
