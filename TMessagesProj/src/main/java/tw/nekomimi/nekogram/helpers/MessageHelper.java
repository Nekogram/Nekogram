package tw.nekomimi.nekogram.helpers;

import android.net.Uri;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Cells.ChatMessageCell;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageHelper extends BaseController {

    private static volatile MessageHelper[] Instance = new MessageHelper[UserConfig.MAX_ACCOUNT_COUNT];
    private int lastReqId;

    public MessageHelper(int num) {
        super(num);
    }

    public static void resetMessageContent(MessageObject messageObject, ChatMessageCell chatMessageCell) {
        messageObject.forceUpdate = true;
        if (messageObject.caption != null) {
            messageObject.caption = null;
            messageObject.generateCaption();
        }
        messageObject.applyNewText();
        messageObject.resetLayout();
        chatMessageCell.requestLayout();
        chatMessageCell.invalidate();
    }

    public static String saveUriToCache(Uri uri) {
        try {
            InputStream inputStream = ApplicationLoader.applicationContext.getContentResolver().openInputStream(uri);
            String fileName = Integer.MIN_VALUE + "_" + SharedConfig.getLastLocalId();
            File fileDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_CACHE);
            final File cacheFile = new File(fileDir, fileName);
            if (inputStream != null) {
                AndroidUtilities.copyFile(inputStream, cacheFile);
                SharedConfig.saveConfig();
                return cacheFile.getAbsolutePath();
            } else {
                return null;
            }
        } catch (Exception e) {
            FileLog.e(e);
            return null;
        }
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

    public void processForwardFromMyName(ArrayList<MessageObject> messages, long did, boolean notify, int scheduleDate) {
        Long groupId = Utilities.random.nextLong();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            if (messageObject.messageOwner.media != null && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty) && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame) && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice)) {
                HashMap<String, String> params = null;
                if ((int) did == 0 && messageObject.messageOwner.to_id != null && (messageObject.messageOwner.media.photo instanceof TLRPC.TL_photo || messageObject.messageOwner.media.document instanceof TLRPC.TL_document)) {
                    params = new HashMap<>();
                    params.put("parentObject", "sent_" + messageObject.messageOwner.to_id.channel_id + "_" + messageObject.getId());
                }
                if (messageObject.messageOwner.grouped_id != 0) {
                    if (params == null) {
                        params = new HashMap<>();
                    }
                    params.put("groupId", groupId + "");
                    if (i == messages.size() - 1) {
                        params.put("final", "true");
                    }
                }
                if (messageObject.messageOwner.media.photo instanceof TLRPC.TL_photo) {
                    getSendMessagesHelper().sendMessage((TLRPC.TL_photo) messageObject.messageOwner.media.photo, null, did, null, messageObject.messageOwner.message, messageObject.messageOwner.entities, null, params, notify, scheduleDate, messageObject.messageOwner.media.ttl_seconds, messageObject);
                } else if (messageObject.messageOwner.media.document instanceof TLRPC.TL_document) {
                    getSendMessagesHelper().sendMessage((TLRPC.TL_document) messageObject.messageOwner.media.document, null, messageObject.messageOwner.attachPath, did, null, messageObject.messageOwner.message, messageObject.messageOwner.entities, null, params, notify, scheduleDate, messageObject.messageOwner.media.ttl_seconds, messageObject);
                } else if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaVenue || messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGeo) {
                    getSendMessagesHelper().sendMessage(messageObject.messageOwner.media, did, null, null, null, notify, scheduleDate);
                } else if (messageObject.messageOwner.media.phone_number != null) {
                    TLRPC.User user = new TLRPC.TL_userContact_old2();
                    user.phone = messageObject.messageOwner.media.phone_number;
                    user.first_name = messageObject.messageOwner.media.first_name;
                    user.last_name = messageObject.messageOwner.media.last_name;
                    user.id = messageObject.messageOwner.media.user_id;
                    getSendMessagesHelper().sendMessage(user, did, null, null, null, notify, scheduleDate);
                } else if ((int) did != 0) {
                    ArrayList<MessageObject> arrayList = new ArrayList<>();
                    arrayList.add(messageObject);
                    getSendMessagesHelper().sendMessage(arrayList, did, notify, scheduleDate);
                }
            } else if (messageObject.messageOwner.message != null) {
                TLRPC.WebPage webPage = null;
                if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) {
                    webPage = messageObject.messageOwner.media.webpage;
                }
                ArrayList<TLRPC.MessageEntity> entities;
                if (messageObject.messageOwner.entities != null && !messageObject.messageOwner.entities.isEmpty()) {
                    entities = new ArrayList<>();
                    for (int a = 0; a < messageObject.messageOwner.entities.size(); a++) {
                        TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(a);
                        if (entity instanceof TLRPC.TL_messageEntityBold ||
                                entity instanceof TLRPC.TL_messageEntityItalic ||
                                entity instanceof TLRPC.TL_messageEntityPre ||
                                entity instanceof TLRPC.TL_messageEntityCode ||
                                entity instanceof TLRPC.TL_messageEntityTextUrl) {
                            entities.add(entity);
                        }
                    }
                } else {
                    entities = null;
                }
                getSendMessagesHelper().sendMessage(messageObject.messageOwner.message, did, null, webPage, true, entities, null, null, notify, scheduleDate);
            } else if ((int) did != 0) {
                ArrayList<MessageObject> arrayList = new ArrayList<>();
                arrayList.add(messageObject);
                getSendMessagesHelper().sendMessage(arrayList, did, notify, scheduleDate);
            }
        }
    }

    public void deleteUserChannelHistoryWithSearch(final long dialog_id, final TLRPC.User user) {
        deleteUserChannelHistoryWithSearch(dialog_id, user, 0);
    }

    public void deleteUserChannelHistoryWithSearch(final long dialog_id, final TLRPC.User user, final int offset_id) {
        final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = getMessagesController().getInputPeer((int) dialog_id);
        if (req.peer == null) {
            return;
        }
        req.limit = 100;
        req.q = "";
        req.offset_id = offset_id;
        if (user != null) {
            req.from_id = getMessagesController().getInputUser(user);
            req.flags |= 1;
        }
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        final int currentReqId = ++lastReqId;
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
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
                            if (message.to_id.channel_id != 0) {
                                channelId = message.to_id.channel_id;
                            }
                            if (message.id > lastMessageId) {
                                lastMessageId = message.id;
                            }
                        }
                        getMessagesController().deleteMessages(ids, random_ids, null, dialog_id, channelId, true, false);
                        deleteUserChannelHistoryWithSearch(dialog_id, user, lastMessageId);
                    }
                }
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }
}
