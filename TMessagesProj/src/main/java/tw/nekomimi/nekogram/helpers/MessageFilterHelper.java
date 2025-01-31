package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

import tw.nekomimi.nekogram.NekoConfig;

public class MessageFilterHelper {

    public static ArrayList<TLRPC.MessageEntity> checkBlockedEntities(MessageObject messageObject, ArrayList<TLRPC.MessageEntity> original) {
        if (messageObject.shouldBlockMessage() && messageObject.messageOwner.message != null) {
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(original);
            var spoiler = new TLRPC.TL_messageEntitySpoiler();
            spoiler.offset = 0;
            spoiler.length = messageObject.messageOwner.message.length();
            entities.add(spoiler);
            var quote = new TLRPC.TL_messageEntityBlockquote();
            quote.offset = 0;
            quote.length = messageObject.messageOwner.message.length();
            quote.collapsed = true;
            entities.add(quote);
            return entities;
        } else {
            return original;
        }
    }

    public static ArrayList<TLRPC.MessageEntity> checkBlockedEntities(MessageObject messageObject) {
        return checkBlockedEntities(messageObject, messageObject.messageOwner.entities);
    }

    public static boolean shouldBlockMessage(MessageObject message) {
        if (message.messageOwner == null || message.storyItem != null) {
            return false;
        }
        if (!NekoConfig.ignoreBlocked) {
            return false;
        }
        if (isUserBlocked(message.currentAccount, message.getFromChatId())) {
            return true;
        }
        if (message.messageOwner.fwd_from == null || message.messageOwner.fwd_from.from_id == null) {
            return false;
        }
        return isUserBlocked(message.currentAccount, MessageObject.getPeerId(message.messageOwner.fwd_from.from_id));
    }

    private static boolean isUserBlocked(int currentAccount, long id) {
        var messagesController = MessagesController.getInstance(currentAccount);
        var userFull = messagesController.getUserFull(id);
        return (userFull != null && userFull.blocked) || messagesController.blockePeers.indexOfKey(id) >= 0;
    }

}
