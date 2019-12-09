package tw.nekomimi.nekogram;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class MessageHelper {

    private static volatile MessageHelper[] Instance = new MessageHelper[UserConfig.MAX_ACCOUNT_COUNT];
    private int currentAccount;
    private int lastReqId;

    public MessageHelper(int num) {
        currentAccount = num;
    }

    public static MessageHelper getInstance(int num) {
        MessageHelper localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MessageHelper.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new MessageHelper(num);
                }
            }
        }
        return localInstance;
    }

    public void deleteUserChannelHistoryWithSearch(final long dialog_id, final TLRPC.User user) {
        deleteUserChannelHistoryWithSearch(dialog_id, user, 0);
    }

    public void deleteUserChannelHistoryWithSearch(final long dialog_id, final TLRPC.User user, final int offset_id) {
        final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = MessagesController.getInstance(currentAccount).getInputPeer((int) dialog_id);
        if (req.peer == null) {
            return;
        }
        req.limit = 100;
        req.q = "";
        req.offset_id = offset_id;
        if (user != null) {
            req.from_id = MessagesController.getInstance(currentAccount).getInputUser(user);
            req.flags |= 1;
        }
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        final int currentReqId = ++lastReqId;
        ConnectionsManager.getInstance(currentAccount).sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            int lastMessageId = offset_id;
            if (currentReqId == lastReqId) {
                if (response != null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    int size = res.messages.size();
                    if (size == 0) {
                        return;
                    }
                    ArrayList<Integer> ids = new ArrayList<>();
                    ArrayList<Long> random_ids = new ArrayList<>();
                    int channelId = 0;
                    for (int a = 0; a < res.messages.size(); a++) {
                        TLRPC.Message message = res.messages.get(a);
                        ids.add(message.id);
                        if (message.random_id != 0) {
                            random_ids.add(message.random_id);
                        }
                        channelId = message.to_id.channel_id;
                        lastMessageId = message.id;
                    }
                    MessagesController.getInstance(currentAccount).deleteMessages(ids, random_ids, null, dialog_id, channelId, true, false);
                    deleteUserChannelHistoryWithSearch(dialog_id, user, lastMessageId);
                }
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }
}
