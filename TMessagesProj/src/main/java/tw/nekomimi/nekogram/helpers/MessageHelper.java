package tw.nekomimi.nekogram.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class MessageHelper extends BaseController {

    private static final MessageHelper[] Instance = new MessageHelper[UserConfig.MAX_ACCOUNT_COUNT];

    public MessageHelper(int num) {
        super(num);
    }

    public static void addFileToClipboard(File file, Runnable callback) {
        try {
            var context = ApplicationLoader.applicationContext;
            var clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            var uri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
            var clip = ClipData.newUri(context.getContentResolver(), "label", uri);
            clipboard.setPrimaryClip(clip);
            callback.run();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public void addMessageToClipboard(MessageObject selectedObject, Runnable callback) {
        var path = getPathToMessage(selectedObject);
        if (!TextUtils.isEmpty(path)) {
            addFileToClipboard(new File(path), callback);
        }
    }

    public Bitmap createQR(String key) {
        try {
            HashMap<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 0);
            QRCodeWriter writer = new QRCodeWriter();
            return writer.encode(key, 768, 768, hints, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }

    public String generateUpdateInfo(SparseArray<MessageObject>[] selectedMessagesIds) {
        ArrayList<MessageObject> messageObjects = new ArrayList<>();
        for (int a = 1; a >= 0; a--) {
            for (int b = 0; b < selectedMessagesIds[a].size(); b++) {
                messageObjects.add(selectedMessagesIds[a].valueAt(b));
            }
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("can_not_skip", false);
            Pattern regex = Pattern.compile("Nekogram-(.*)-([0-9]+)-(.*)\\.apk");
            JSONObject file = new JSONObject();
            JSONObject message = new JSONObject();
            for (MessageObject messageObject : messageObjects) {
                if (messageObject.isAnyKindOfSticker()) {
                    jsonObject.put("sticker", messageObject.getId());
                } else if (messageObject.getDocument() != null) {
                    Matcher m = regex.matcher(messageObject.getDocumentName());
                    if (m.find()) {
                        if (!jsonObject.has("version")) {
                            jsonObject.put("version", m.group(1));
                            jsonObject.put("version_code", m.group(2));
                        }
                        String abi = m.group(3);
                        if (abi != null) file.put(abi, messageObject.getId());
                    }
                } else {
                    if (containsHanScript(messageObject.messageOwner.message)) {
                        message.put("Zuragram", messageObject.getId());
                    } else {
                        message.put("nekoupdates", messageObject.getId());
                    }
                }
            }
            if (message.length() != 0) {
                jsonObject.put("messages", message);
                if (message.has("nekoupdates") && !message.has("Zuragram")) {
                    message.put("Zuragram", message.getInt("nekoupdates"));
                }
            }
            if (file.length() != 0) {
                jsonObject.put("files", file);
            }
            return "#" + UpdateHelper.UPDATE_TAG + jsonObject.toString();
        } catch (JSONException e) {
            FileLog.e(e);
            return "";
        }
    }

    public static boolean containsHanScript(String s) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return s.codePoints().anyMatch(Character::isIdeographic);
        } else {
            for (int i = 0; i < s.length(); i++) {
                if (Character.isIdeographic(s.codePointAt(i))) {
                    return true;
                }
            }
            return false;
        }
    }

    private MessageObject getTargetMessageObjectFromGroup(MessageObject.GroupedMessages selectedObjectGroup) {
        MessageObject messageObject = null;
        for (MessageObject object : selectedObjectGroup.messages) {
            if (!TextUtils.isEmpty(object.messageOwner.message)) {
                if (messageObject != null) {
                    messageObject = null;
                    break;
                } else {
                    messageObject = object;
                }
            }
        }
        return messageObject;
    }

    private boolean isEmoji(String message) {
        return message.matches("(?:[\uD83C\uDF00-\uD83D\uDDFF]|[\uD83E\uDD00-\uD83E\uDDFF]|" +
                "[\uD83D\uDE00-\uD83D\uDE4F]|[\uD83D\uDE80-\uD83D\uDEFF]|" +
                "[\u2600-\u26FF]\uFE0F?|[\u2700-\u27BF]\uFE0F?|\u24C2\uFE0F?|" +
                "[\uD83C\uDDE6-\uD83C\uDDFF]{1,2}|" +
                "[\uD83C\uDD70\uD83C\uDD71\uD83C\uDD7E\uD83C\uDD7F\uD83C\uDD8E\uD83C\uDD91-\uD83C\uDD9A]\uFE0F?|" +
                "[\u0023\u002A\u0030-\u0039]\uFE0F?\u20E3|[\u2194-\u2199\u21A9-\u21AA]\uFE0F?|[\u2B05-\u2B07\u2B1B\u2B1C\u2B50\u2B55]\uFE0F?|" +
                "[\u2934\u2935]\uFE0F?|[\u3030\u303D]\uFE0F?|[\u3297\u3299]\uFE0F?|" +
                "[\uD83C\uDE01\uD83C\uDE02\uD83C\uDE1A\uD83C\uDE2F\uD83C\uDE32-\uD83C\uDE3A\uD83C\uDE50\uD83C\uDE51]\uFE0F?|" +
                "[\u203C\u2049]\uFE0F?|[\u25AA\u25AB\u25B6\u25C0\u25FB-\u25FE]\uFE0F?|" +
                "[\u00A9\u00AE]\uFE0F?|[\u2122\u2139]\uFE0F?|\uD83C\uDC04\uFE0F?|\uD83C\uDCCF\uFE0F?|" +
                "[\u231A\u231B\u2328\u23CF\u23E9-\u23F3\u23F8-\u23FA]\uFE0F?)+");
    }

    public MessageObject getMessageForTranslate(MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup) {
        MessageObject messageObject = null;
        if (selectedObjectGroup != null && !selectedObjectGroup.isDocuments) {
            messageObject = getTargetMessageObjectFromGroup(selectedObjectGroup);
        } else if (!selectedObject.isAnimatedEmoji() && !TextUtils.isEmpty(selectedObject.messageOwner.message) || selectedObject.isPoll()) {
            messageObject = selectedObject;
        }
        if (messageObject != null && !TextUtils.isEmpty(selectedObject.messageOwner.message) && isEmoji(selectedObject.messageOwner.message)) {
            return null;
        }
        return messageObject;
    }

    public MessageObject getMessageForRepeat(MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup) {
        MessageObject messageObject = null;
        if (selectedObjectGroup != null && !selectedObjectGroup.isDocuments) {
            messageObject = getTargetMessageObjectFromGroup(selectedObjectGroup);
        } else if (!TextUtils.isEmpty(selectedObject.messageOwner.message) || selectedObject.isAnyKindOfSticker()) {
            messageObject = selectedObject;
        }
        return messageObject;
    }

    public String getPathToMessage(MessageObject messageObject) {
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
                return null;
            }
        }
        return path;
    }

    public void saveStickerToGallery(Activity activity, MessageObject messageObject, Runnable callback) {
        saveStickerToGallery(activity, getPathToMessage(messageObject), callback);
    }

    public static void saveStickerToGallery(Activity activity, TLRPC.Document document, Runnable callback) {
        String path = FileLoader.getPathToAttach(document, true).toString();
        File temp = new File(path);
        if (!temp.exists()) {
            return;
        }
        saveStickerToGallery(activity, path, callback);
    }

    private static void saveStickerToGallery(Activity activity, String path, Runnable callback) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                Bitmap image = BitmapFactory.decodeFile(path);
                if (image != null) {
                    File file = new File(path.replace(".webp", ".png"));
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

    public void createDeleteHistoryAlert(BaseFragment fragment, TLRPC.Chat chat, long mergeDialogId, Theme.ResourcesProvider resourcesProvider) {
        if (fragment == null || fragment.getParentActivity() == null || chat == null) {
            return;
        }

        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);

        CheckBoxCell cell = ChatObject.isChannel(chat) && ChatObject.canUserDoAction(chat, ChatObject.ACTION_DELETE_MESSAGES) ? new CheckBoxCell(context, 1, resourcesProvider) : null;

        TextView messageTextView = new TextView(context);
        messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        messageTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        messageTextView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP);

        FrameLayout frameLayout = new FrameLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                if (cell != null) {
                    setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight() + cell.getMeasuredHeight() + AndroidUtilities.dp(7));
                }
            }
        };
        builder.setView(frameLayout);

        AvatarDrawable avatarDrawable = new AvatarDrawable();
        avatarDrawable.setTextSize(AndroidUtilities.dp(12));
        avatarDrawable.setInfo(chat);

        BackupImageView imageView = new BackupImageView(context);
        imageView.setRoundRadius(AndroidUtilities.dp(20));
        imageView.setForUserOrChat(chat, avatarDrawable);
        frameLayout.addView(imageView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 22, 5, 22, 0));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        textView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        textView.setLines(1);
        textView.setMaxLines(1);
        textView.setSingleLine(true);
        textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
        textView.setEllipsize(TextUtils.TruncateAt.END);
        textView.setText(LocaleController.getString("DeleteAllFromSelf", R.string.DeleteAllFromSelf));

        frameLayout.addView(textView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, (LocaleController.isRTL ? 21 : 76), 11, (LocaleController.isRTL ? 76 : 21), 0));
        frameLayout.addView(messageTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 24, 57, 24, 9));

        if (cell != null) {
            boolean sendAs = ChatObject.getSendAsPeerId(chat, getMessagesController().getChatFull(chat.id), true) != getUserConfig().getClientUserId();
            cell.setBackground(Theme.getSelectorDrawable(false));
            cell.setText(LocaleController.getString("DeleteAllFromSelfAdmin", R.string.DeleteAllFromSelfAdmin), "", !ChatObject.shouldSendAnonymously(chat) && !sendAs, false);
            cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
            frameLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM | Gravity.LEFT, 0, 0, 0, 0));
            cell.setOnClickListener(v -> {
                CheckBoxCell cell1 = (CheckBoxCell) v;
                cell1.setChecked(!cell1.isChecked(), true);
            });
        }

        messageTextView.setText(AndroidUtilities.replaceTags(LocaleController.getString("DeleteAllFromSelfAlert", R.string.DeleteAllFromSelfAlert)));

        builder.setPositiveButton(LocaleController.getString("DeleteAll", R.string.DeleteAll), (dialogInterface, i) -> {
            if (cell != null && cell.isChecked()) {
                getMessagesController().deleteUserChannelHistory(chat, getUserConfig().getCurrentUser(), null, 0);
            } else {
                deleteUserChannelHistoryWithSearch(fragment, -chat.id, mergeDialogId);
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        fragment.showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_dialogTextRed2));
        }
    }

    public void resetMessageContent(long dialogId, MessageObject messageObject, boolean translated) {
        TLRPC.Message message = messageObject.messageOwner;

        MessageObject obj = new MessageObject(currentAccount, message, true, true);
        obj.originalMessage = messageObject.originalMessage;
        obj.translated = translated;
        if (messageObject.isSponsored()) {
            obj.sponsoredId = messageObject.sponsoredId;
            obj.botStartParam = messageObject.botStartParam;
        }

        ArrayList<MessageObject> arrayList = new ArrayList<>();
        arrayList.add(obj);
        getNotificationCenter().postNotificationName(NotificationCenter.replaceMessagesObjects, dialogId, arrayList, false);
    }

    public void deleteUserChannelHistoryWithSearch(BaseFragment fragment, final long dialogId, final long mergeDialogId) {
        deleteUserChannelHistoryWithSearch(fragment, dialogId, mergeDialogId, 0, -1);
    }

    public void deleteUserChannelHistoryWithSearch(BaseFragment fragment, final long dialogId, final long mergeDialogId, final int offsetId, int lastSize) {
        final TLRPC.TL_messages_search req = new TLRPC.TL_messages_search();
        req.peer = getMessagesController().getInputPeer(dialogId);
        if (req.peer == null) {
            return;
        }
        req.limit = 100;
        req.q = "";
        req.offset_id = offsetId;
        req.from_id = MessagesController.getInputPeer(getUserConfig().getCurrentUser());
        req.flags |= 1;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        getConnectionsManager().sendRequest(req, (response, error) -> AndroidUtilities.runOnUIThread(() -> {
            if (error == null) {
                if (response != null) {
                    TLRPC.messages_Messages res = (TLRPC.messages_Messages) response;
                    if (res.messages.size() == 0) {
                        return;
                    }
                    ArrayList<Integer> ids = new ArrayList<>();
                    int newOffsetId = res.messages.get(0).id;
                    for (TLRPC.Message message : res.messages) {
                        newOffsetId = Math.min(newOffsetId, message.id);
                        ids.add(message.id);
                    }
                    if (ids.size() == 0) {
                        return;
                    }
                    getMessagesController().deleteMessages(ids, null, null, dialogId, true, false);
                    if (offsetId == newOffsetId && lastSize == ids.size()) {
                        return;
                    }
                    deleteUserChannelHistoryWithSearch(fragment, dialogId, mergeDialogId, newOffsetId, ids.size());
                }
            } else {
                AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text);
            }
        }), ConnectionsManager.RequestFlagFailOnServerErrors);
        if (offsetId == 0 && mergeDialogId != 0) {
            deleteUserChannelHistoryWithSearch(fragment, mergeDialogId, 0, 0, -1);
        }
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

    public void sendWebFile(BaseFragment fragment, int did, String url, boolean isPhoto, Theme.ResourcesProvider resourcesProvider) {
        TLRPC.TL_messages_sendMedia req = new TLRPC.TL_messages_sendMedia();
        TLRPC.InputMedia media;
        if (isPhoto) {
            TLRPC.TL_inputMediaPhotoExternal photo = new TLRPC.TL_inputMediaPhotoExternal();
            photo.url = url;
            media = photo;
        } else {
            TLRPC.TL_inputMediaDocumentExternal document = new TLRPC.TL_inputMediaDocumentExternal();
            document.url = url;
            media = document;
        }
        req.media = media;
        req.random_id = Utilities.random.nextLong();
        req.peer = getMessagesController().getInputPeer(did);
        req.message = "";
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    if (error.text.equals("MEDIA_EMPTY")) {
                        BulletinFactory.of(fragment).createErrorBulletin(LocaleController.getString("SendWebFileInvalid", R.string.SendWebFileInvalid), resourcesProvider).show();
                    } else {
                        AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("SendWebFile", R.string.SendWebFile), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text, resourcesProvider);
                    }
                });
            }
        });
    }

    @SuppressLint("SetTextI18n")
    public void showSendWebFileDialog(ChatAttachAlert parentAlert, Theme.ResourcesProvider resourcesProvider) {
        ChatActivity fragment = (ChatActivity) parentAlert.getBaseFragment();
        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
        builder.setTitle(LocaleController.getString("SendWebFile", R.string.SendWebFile));
        builder.setMessage(LocaleController.getString("SendWebFileInfo", R.string.SendWebFileInfo));
        builder.setCustomViewOffset(0);

        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        final EditTextBoldCursor editText = new EditTextBoldCursor(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
            }
        };
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        editText.setText("http://");
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        editText.setHintText(LocaleController.getString("URL", R.string.URL));
        editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
        editText.setSingleLine(true);
        editText.setFocusable(true);
        editText.setTransformHintToHeader(true);
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteRedText3, resourcesProvider));
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setBackground(null);
        editText.requestFocus();
        editText.setPadding(0, 0, 0, 0);
        ll.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24, 0, 24, 0));

        CheckBoxCell cell = new CheckBoxCell(context, 1, resourcesProvider);
        cell.setBackground(Theme.getSelectorDrawable(false));
        cell.setText(LocaleController.getString("SendWithoutCompression", R.string.SendWithoutCompression), "", true, false);
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
        ll.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        cell.setOnClickListener(v -> {
            CheckBoxCell cell12 = (CheckBoxCell) v;
            cell12.setChecked(!cell12.isChecked(), true);
        });

        builder.setView(ll);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> sendWebFile(fragment, (int) fragment.getDialogId(), editText.getText().toString(), !cell.isChecked(), resourcesProvider));
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
        fragment.showDialog(alertDialog);
        editText.setSelection(0, editText.getText().length());
    }
}
