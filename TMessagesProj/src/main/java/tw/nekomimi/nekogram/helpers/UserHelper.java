package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.TopicsFragment;

import java.util.Locale;
import java.util.function.Consumer;

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

    public void openByDialogId(long dialogId, Activity activity, Consumer<BaseFragment> callback, Browser.Progress progress) {
        if (dialogId == 0 || activity == null) {
            return;
        }
        AlertDialog progressDialog = progress != null ? null : new AlertDialog(activity, AlertDialog.ALERT_TYPE_SPINNER);
        searchPeer(dialogId, (peer) -> {
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
            if (peer instanceof TLRPC.User user) {
                args.putLong("user_id", user.id);
                callback.accept(new ProfileActivity(args));
            } else if (peer instanceof TLRPC.Chat chat) {
                args.putLong("chat_id", chat.id);
                if (ChatObject.isForum(chat)) {
                    callback.accept(new TopicsFragment(args));
                } else {
                    callback.accept(new ChatActivity(args));
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

    public void searchPeer(long dialogId, Consumer<Object> callback) {
        if (dialogId < 0) {
            searchChat(-dialogId, callback::accept);
        } else {
            searchUser(dialogId, callback::accept);
        }
    }

    public void searchUser(long userId, Consumer<TLRPC.User> callback) {
        var user = getMessagesController().getUser(userId);
        if (user != null) {
            callback.accept(user);
            return;
        }
        searchUser(userId, callback, null);
    }

    public void searchChat(long chatId, Consumer<TLRPC.Chat> callback) {
        var chat = getMessagesController().getChat(chatId);
        if (chat != null) {
            callback.accept(chat);
            return;
        }
        searchChat(chatId, callback, null);
    }

    void resolveUser(String username, long userId, Consumer<TLRPC.User> callback) {
        resolvePeer(username, resolved -> callback.accept(getMessagesController().getUser(userId)));
    }

    private void resolvePeer(String username, Consumer<Boolean> callback) {
        var req = new TLRPC.TL_contacts_resolveUsername();
        req.username = username;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (response instanceof TLRPC.TL_contacts_resolvedPeer res) {
                getMessagesController().putUsers(res.users, false);
                getMessagesController().putChats(res.chats, false);
                getMessagesStorage().putUsersAndChats(res.users, res.chats, true, true);
                callback.accept(true);
            } else {
                callback.accept(false);
            }
        }));
    }

    private void searchUser(long userId, Consumer<TLRPC.User> callback, ParsedPeer fallback) {
        searchPeer(Extra.getUserInfoBot(fallback != null), userId, fakeUser -> {
            if (fakeUser == null) {
                callback.accept(fallback != null ? fallback.toUser() : null);
                return;
            }
            var user = getMessagesController().getUser(userId);
            if (user != null) {
                callback.accept(user);
            } else if (fallback != null) {
                callback.accept(fallback.toUser());
            } else {
                searchUser(userId, callback, fakeUser);
            }
        });
    }

    private void searchChat(long chatId, Consumer<TLRPC.Chat> callback, ParsedPeer fallback) {
        searchPeer(Extra.getUserInfoBot(fallback != null), -1000000000000L - chatId, fakeChat -> {
            if (fakeChat == null) {
                callback.accept(null);
                return;
            }
            var chat = getMessagesController().getChat(fakeChat.id);
            if (chat != null) {
                callback.accept(chat);
            } else if (fallback != null) {
                callback.accept(fallback.toChat());
            } else {
                searchChat(chatId, callback, fakeChat);
            }
        });
    }

    private void searchPeer(UserInfoBot botInfo, long id, Consumer<ParsedPeer> callback) {
        getInlineBotHelper().query(botInfo, String.valueOf(id), (results, error) -> {
            if (results == null || results.isEmpty()) {
                callback.accept(null);
                return;
            }
            var result = results.get(0);
            if (result.send_message == null || TextUtils.isEmpty(result.send_message.message)) {
                callback.accept(null);
                return;
            }
            var lines = result.send_message.message.split("\n");
            if (lines.length < 3) {
                callback.accept(null);
                return;
            }
            var peer = botInfo.parsePeer(lines);
            if (peer == null || peer.id != id) {
                callback.accept(null);
                return;
            }
            if (peer.username != null) {
                resolvePeer(peer.username, resolved -> callback.accept(peer));
            } else {
                callback.accept(peer);
            }
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

    public static long getOwnerFromStickerSetId(long id) {
        var ownerId = id >> 32;
        //long seqByOwner = id & 0xffff;
        var extByte = (id >> 24) & 0xff;
        // (ownerId < int 32) => 0x00
        // (ownerId > int 32) (int 64) => 0x7f (old) or 0x3f (current)
        var sepByte = (id >> 16) & 0xff;
        if (sepByte == 0x3f) { // elif sepByte == 0x7f nothing needs to be done here.
            ownerId |= 0x80000000L;
        }
        if (extByte != 0) {
            //seqByOwner = 0x10000 - seqByOwner;
            ownerId += 0x100000000L;
        }
        return ownerId;
    }

    public interface BotInfo {
        long getId();

        String getUsername();
    }

    public static class ParsedPeer {
        public long id;
        public String username;
        public String first_name;
        public String last_name;
        public String title;

        public TLRPC.User toUser() {
            if (first_name == null) return null;
            var user = new TLRPC.TL_user();
            user.id = id;
            user.first_name = first_name;
            user.last_name = last_name;
            return user;
        }

        public TLRPC.Chat toChat() {
            if (title == null) return null;
            var chat = new TLRPC.TL_chat();
            chat.id = id;
            chat.title = title;
            return chat;
        }
    }

    abstract public static class UserInfoBot implements BotInfo {
        abstract public ParsedPeer parsePeer(String[] lines);
    }
}
