package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.Bitmaps;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.AlertsCreator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;

public class MessageHelper extends BaseController {

    private static final MessageHelper[] Instance = new MessageHelper[UserConfig.MAX_ACCOUNT_COUNT];

    public MessageHelper(int num) {
        super(num);
    }

    public void saveStickerToGallery(Activity activity, MessageObject messageObject, Runnable callback) {
        String path = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(path)) {
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = FileLoader.getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = FileLoader.getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                return;
            }
        }
        if (!TextUtils.isEmpty(path)) {
            final String finalPath = path;
            Utilities.globalQueue.postRunnable(() -> {
                try {
                    Bitmap image;
                    if (Build.VERSION.SDK_INT >= 19) {
                        image = BitmapFactory.decodeFile(finalPath);
                    } else {
                        RandomAccessFile file = new RandomAccessFile(finalPath, "r");
                        ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, finalPath.length());
                        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                        bmOptions.inJustDecodeBounds = true;
                        Utilities.loadWebpImage(null, buffer, buffer.limit(), bmOptions, true);
                        image = Bitmaps.createBitmap(bmOptions.outWidth, bmOptions.outHeight, Bitmap.Config.ARGB_8888);
                        Utilities.loadWebpImage(image, buffer, buffer.limit(), null, true);
                        file.close();
                    }
                    if (image != null) {
                        File file = new File(finalPath.replace(".webp", ".png"));
                        FileOutputStream stream = new FileOutputStream(file);
                        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        stream.close();
                        MediaController.saveFile(file.toString(), activity, 0, null, null);
                        AndroidUtilities.runOnUIThread(callback);
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            });
        }
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

    public void resetMessageContent(long dialog_id, MessageObject messageObject, boolean translated) {
        TLRPC.Message message = messageObject.messageOwner;

        MessageObject obj = new MessageObject(currentAccount, message, true, true);
        obj.originalMessage = messageObject.originalMessage;
        obj.translated = translated;

        ArrayList<MessageObject> arrayList = new ArrayList<>();
        arrayList.add(obj);
        getNotificationCenter().postNotificationName(NotificationCenter.replaceMessagesObjects, dialog_id, arrayList, false);
    }

    public void processForwardFromMyName(ArrayList<MessageObject> messages, long did, boolean notify, int scheduleDate) {
        HashMap<Long, Long> map = new HashMap<>();
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            ArrayList<TLRPC.MessageEntity> entities;
            if (messageObject.messageOwner.entities != null && !messageObject.messageOwner.entities.isEmpty()) {
                entities = new ArrayList<>();
                for (int a = 0; a < messageObject.messageOwner.entities.size(); a++) {
                    TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(a);
                    if (entity instanceof TLRPC.TL_messageEntityBold ||
                            entity instanceof TLRPC.TL_messageEntityItalic ||
                            entity instanceof TLRPC.TL_messageEntityPre ||
                            entity instanceof TLRPC.TL_messageEntityCode ||
                            entity instanceof TLRPC.TL_messageEntityTextUrl ||
                            entity instanceof TLRPC.TL_messageEntityStrike ||
                            entity instanceof TLRPC.TL_messageEntityUnderline) {
                        entities.add(entity);
                    }
                    if (entity instanceof TLRPC.TL_messageEntityMentionName) {
                        TLRPC.TL_inputMessageEntityMentionName mention = new TLRPC.TL_inputMessageEntityMentionName();
                        mention.length = entity.length;
                        mention.offset = entity.offset;
                        mention.user_id = getMessagesController().getInputUser(((TLRPC.TL_messageEntityMentionName) entity).user_id);
                        entities.add(mention);
                    }
                }
            } else {
                entities = null;
            }
            if (messageObject.messageOwner.media != null && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty) && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaWebPage) && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGame) && !(messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaInvoice)) {
                HashMap<String, String> params = null;
                if ((int) did == 0 && messageObject.messageOwner.peer_id != null && (messageObject.messageOwner.media.photo instanceof TLRPC.TL_photo || messageObject.messageOwner.media.document instanceof TLRPC.TL_document)) {
                    params = new HashMap<>();
                    params.put("parentObject", "sent_" + messageObject.messageOwner.peer_id.channel_id + "_" + messageObject.getId());
                }
                long oldGroupId = messageObject.messageOwner.grouped_id;
                if (oldGroupId != 0) {
                    if (params == null) {
                        params = new HashMap<>();
                    }
                    Long groupId;
                    if (map.containsKey(oldGroupId)) {
                        groupId = map.get(oldGroupId);
                    } else {
                        groupId = Utilities.random.nextLong();
                        map.put(oldGroupId, groupId);
                    }
                    params.put("groupId", String.valueOf(groupId));
                    if (i == messages.size() - 1) {
                        params.put("final", "true");
                    } else {
                        long nextOldGroupId = messages.get(i + 1).messageOwner.grouped_id;
                        if (nextOldGroupId != oldGroupId) {
                            params.put("final", "true");
                        }
                    }
                }
                if (messageObject.messageOwner.media.photo instanceof TLRPC.TL_photo) {
                    getSendMessagesHelper().sendMessage((TLRPC.TL_photo) messageObject.messageOwner.media.photo, null, did, null, null, messageObject.messageOwner.message, entities, null, params, notify, scheduleDate, 0, messageObject);
                } else if (messageObject.messageOwner.media.document instanceof TLRPC.TL_document) {
                    getSendMessagesHelper().sendMessage((TLRPC.TL_document) messageObject.messageOwner.media.document, null, messageObject.messageOwner.attachPath, did, null, null, messageObject.messageOwner.message, entities, null, params, notify, scheduleDate, 0, messageObject, null);
                } else if (messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaVenue || messageObject.messageOwner.media instanceof TLRPC.TL_messageMediaGeo) {
                    getSendMessagesHelper().sendMessage(messageObject.messageOwner.media, did, null, null, null, null, notify, scheduleDate);
                } else if (messageObject.messageOwner.media.phone_number != null) {
                    TLRPC.User user = new TLRPC.TL_userContact_old2();
                    user.phone = messageObject.messageOwner.media.phone_number;
                    user.first_name = messageObject.messageOwner.media.first_name;
                    user.last_name = messageObject.messageOwner.media.last_name;
                    user.id = messageObject.messageOwner.media.user_id;
                    getSendMessagesHelper().sendMessage(user, did, null, null, null, null, notify, scheduleDate);
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
                getSendMessagesHelper().sendMessage(messageObject.messageOwner.message, did, null, null, webPage, webPage != null, entities, null, null, notify, scheduleDate, null);
            } else if ((int) did != 0) {
                ArrayList<MessageObject> arrayList = new ArrayList<>();
                arrayList.add(messageObject);
                getSendMessagesHelper().sendMessage(arrayList, did, notify, scheduleDate);
            }
        }
    }

    public void deleteUserChannelHistoryWithSearch(BaseFragment fragment, final long dialog_id) {
        deleteUserChannelHistoryWithSearch(fragment, dialog_id, 0);
    }

    public void deleteUserChannelHistoryWithSearch(BaseFragment fragment, final long dialog_id, final int offset_id) {
        final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = getMessagesController().getInputPeer((int) dialog_id);
        if (req.peer == null) {
            return;
        }
        req.limit = 100;
        req.q = "";
        req.offset_id = offset_id;
        req.from_id = MessagesController.getInputPeer(getUserConfig().getCurrentUser());
        req.flags |= 1;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                int lastMessageId = offset_id;
                if (response != null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    int size = res.messages.size();
                    if (size == 0) {
                        return;
                    }
                    FileLog.d("deleteUserChannelHistoryWithSearch size = " + size);
                    ArrayList<Integer> ids = new ArrayList<>();
                    int channelId = 0;
                    for (int a = 0; a < size; a++) {
                        TLRPC.Message message = res.messages.get(a);
                        if (message.id > lastMessageId) {
                            lastMessageId = message.id;
                        }
                        if (message.peer_id.channel_id != 0) {
                            channelId = message.peer_id.channel_id;
                        } else if (message.peer_id.chat_id == 0) {
                            continue;
                        }
                        ids.add(message.id);
                    }
                    if (ids.size() == 0) {
                        return;
                    }
                    getMessagesController().deleteMessages(ids, null, null, 0, channelId, true, false);
                    deleteUserChannelHistoryWithSearch(fragment, dialog_id, lastMessageId);
                }
            } else {
                AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("DeleteAllFromSelf", R.string.DeleteAllFromSelf), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text);
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    public String getDCLocation(int dc) {
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
}
