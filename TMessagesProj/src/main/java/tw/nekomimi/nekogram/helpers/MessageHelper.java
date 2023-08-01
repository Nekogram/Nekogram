package tw.nekomimi.nekogram.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.core.text.HtmlCompat;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.Emoji;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.RequestDelegate;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.Forum.ForumUtilities;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranscribeButton;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.helpers.remote.BaseRemoteHelper;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class MessageHelper extends BaseController {

    private static final MessageHelper[] Instance = new MessageHelper[UserConfig.MAX_ACCOUNT_COUNT];
    private static final CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder();
    private static final SpannableStringBuilder[] spannedStrings = new SpannableStringBuilder[3];

    public MessageHelper(int num) {
        super(num);
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

    public static CharSequence createBlockedString(MessageObject messageObject) {
        if (spannedStrings[2] == null) {
            spannedStrings[2] = new SpannableStringBuilder("\u200B");
            spannedStrings[2].setSpan(new ColoredImageSpan(Theme.chat_blockDrawable), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        var spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder
                .append(spannedStrings[2])
                .append(' ')
                .append(LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000));
        return spannableStringBuilder;
    }

    public static CharSequence createEditedString(MessageObject messageObject) {
        if (spannedStrings[1] == null) {
            spannedStrings[1] = new SpannableStringBuilder("\u200B");
            spannedStrings[1].setSpan(new ColoredImageSpan(Theme.chat_editDrawable), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        var spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder
                .append(spannedStrings[1])
                .append(' ')
                .append(LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000));
        return spannableStringBuilder;
    }

    public static CharSequence createTranslateString(MessageObject messageObject) {
        var translatedLanguage = messageObject.translatedLanguage;
        if (translatedLanguage == null || "und".equals(translatedLanguage.first) || TextUtils.isEmpty(translatedLanguage.first) || TextUtils.isEmpty(translatedLanguage.second)) {
            return LocaleController.getString("Translated", R.string.Translated) + " " + LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000);
        }
        if (spannedStrings[0] == null) {
            spannedStrings[0] = new SpannableStringBuilder("\u200B");
            spannedStrings[0].setSpan(new ColoredImageSpan(Theme.chat_arrowDrawable), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        Locale from = Locale.forLanguageTag(translatedLanguage.first);
        Locale to = Locale.forLanguageTag(translatedLanguage.second);
        var spannableStringBuilder = new SpannableStringBuilder();
        spannableStringBuilder
                .append(!TextUtils.isEmpty(from.getScript()) ? HtmlCompat.fromHtml(from.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY) : from.getDisplayName())
                .append(' ')
                .append(spannedStrings[0])
                .append(' ')
                .append(!TextUtils.isEmpty(to.getScript()) ? HtmlCompat.fromHtml(to.getDisplayScript(), HtmlCompat.FROM_HTML_MODE_LEGACY) : to.getDisplayName())
                .append(' ')
                .append(LocaleController.getInstance().formatterDay.format((long) (messageObject.messageOwner.date) * 1000));
        return spannableStringBuilder;
    }

    public static ArrayList<TLRPC.MessageEntity> checkBlockedUserEntities(MessageObject messageObject) {
        if (messageObject.shouldBlockMessage() && messageObject.messageOwner.message != null) {
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(messageObject.messageOwner.entities);
            var spoiler = new TLRPC.TL_messageEntitySpoiler();
            spoiler.offset = 0;
            spoiler.length = messageObject.messageOwner.message.length();
            entities.add(spoiler);
            return entities;
        } else {
            return messageObject.messageOwner.entities;
        }
    }

    public static void addFileToClipboard(File file, Runnable callback) {
        try {
            var context = ApplicationLoader.applicationContext;
            var clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            var uri = FileProvider.getUriForFile(context, ApplicationLoader.getApplicationId() + ".provider", file);
            var clip = ClipData.newUri(context.getContentResolver(), "label", uri);
            clipboard.setPrimaryClip(clip);
            callback.run();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static String getTextOrBase64(byte[] data) {
        try {
            return utf8Decoder.decode(ByteBuffer.wrap(data)).toString();
        } catch (CharacterCodingException e) {
            return Base64.encodeToString(data, Base64.NO_PADDING | Base64.NO_WRAP);
        }
    }

    public void addMessageToClipboard(MessageObject selectedObject, Runnable callback) {
        var path = getPathToMessage(selectedObject);
        if (!TextUtils.isEmpty(path)) {
            addFileToClipboard(new File(path), callback);
        }
    }

    public void generateUpdateInfo(BaseFragment fragment, SparseArray<MessageObject>[] selectedMessagesIds, Runnable callback) {
        fragment.showDialog(new AlertDialog.Builder(fragment.getParentActivity())
                .setItems(new CharSequence[]{"direct", "play"}, (dialog, which) -> {
                    var tag = which == 0 ? "updatev3" : "updateplayv3";
                    ArrayList<MessageObject> messageObjects = new ArrayList<>();
                    for (int a = 1; a >= 0; a--) {
                        for (int b = 0; b < selectedMessagesIds[a].size(); b++) {
                            messageObjects.add(selectedMessagesIds[a].valueAt(b));
                        }
                    }
                    var update = new UpdateHelper.Update();
                    update.canNotSkip = false;
                    Pattern regex = Pattern.compile("Nekogram-(.*)-(\\d+)-(.*)\\.apk");
                    for (MessageObject messageObject : messageObjects) {
                        if (messageObject.isAnyKindOfSticker()) {
                            update.sticker = messageObject.getId();
                        } else if (messageObject.getDocument() != null) {
                            Matcher m = regex.matcher(messageObject.getDocumentName());
                            if (m.find()) {
                                if (update.version == null) {
                                    update.version = m.group(1);
                                    //noinspection ConstantConditions
                                    update.versionCode = Integer.valueOf(m.group(2));
                                }
                                if (which == 0) {
                                    String abi = m.group(3);
                                    if (abi != null) {
                                        if (update.files == null) {
                                            update.files = new HashMap<>();
                                        }
                                        update.files.put(abi, messageObject.getId());
                                    }
                                } else if (update.url == null) {
                                    update.url = "https://play.google.com/store/apps/details?id=tw.nekomimi.nekogram";
                                }
                            }
                        } else {
                            update.message = messageObject.getId();
                        }
                    }
                    AndroidUtilities.addToClipboard("#" + tag + BaseRemoteHelper.GSON.toJson(update));
                    callback.run();
                }).create());
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

    public String getMessagePlainText(MessageObject messageObject) {
        String message;
        if (messageObject.isPoll()) {
            TLRPC.Poll poll = ((TLRPC.TL_messageMediaPoll) messageObject.messageOwner.media).poll;
            StringBuilder pollText = new StringBuilder(poll.question).append("\n");
            for (TLRPC.TL_pollAnswer answer : poll.answers) {
                pollText.append("\n\uD83D\uDD18 ");
                pollText.append(answer.text);
            }
            message = pollText.toString();
        } else if (messageObject.isVoiceTranscriptionOpen()) {
            message = messageObject.messageOwner.voiceTranscription;
        } else {
            message = messageObject.messageOwner.message;
        }
        return message;
    }

    public MessageObject getMessageForTranslate(MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup) {
        MessageObject messageObject = null;
        if (selectedObjectGroup != null && !selectedObjectGroup.isDocuments) {
            messageObject = getTargetMessageObjectFromGroup(selectedObjectGroup);
        } else if (selectedObject.isPoll()) {
            messageObject = selectedObject;
        } else if (selectedObject.isVoiceTranscriptionOpen() && !TextUtils.isEmpty(selectedObject.messageOwner.voiceTranscription) && !TranscribeButton.isTranscribing(selectedObject)) {
            messageObject = selectedObject;
        } else if (!selectedObject.isVoiceTranscriptionOpen() && !TextUtils.isEmpty(selectedObject.messageOwner.message) && !isLinkOrEmojiOnlyMessage(selectedObject)) {
            messageObject = selectedObject;
        }
        if (messageObject != null && messageObject.translating) {
            return null;
        }
        return messageObject;
    }

    public boolean isLinkOrEmojiOnlyMessage(MessageObject messageObject) {
        if (messageObject.messageOwner.message.matches("[\\d -/:-@\\[-`{-~]")) {
            return true;
        }
        var entities = messageObject.messageOwner.entities;
        if (entities != null) {
            for (TLRPC.MessageEntity entity : entities) {
                if (entity instanceof TLRPC.TL_messageEntityBotCommand ||
                        entity instanceof TLRPC.TL_messageEntityEmail ||
                        entity instanceof TLRPC.TL_messageEntityUrl ||
                        entity instanceof TLRPC.TL_messageEntityMention ||
                        entity instanceof TLRPC.TL_messageEntityCashtag ||
                        entity instanceof TLRPC.TL_messageEntityHashtag ||
                        entity instanceof TLRPC.TL_messageEntityBankCard ||
                        entity instanceof TLRPC.TL_messageEntityPhone) {
                    if (entity.offset == 0 && entity.length == messageObject.messageOwner.message.length()) {
                        return true;
                    }
                }
            }
        }
        return Emoji.fullyConsistsOfEmojis(messageObject.messageOwner.message);
    }

    public boolean isMessageObjectAutoTranslatable(MessageObject messageObject) {
        if (messageObject.translated || messageObject.translating || messageObject.isOutOwner()) {
            return false;
        }
        if (messageObject.isPoll()) {
            return true;
        }
        return !TextUtils.isEmpty(messageObject.messageOwner.message) && !isLinkOrEmojiOnlyMessage(messageObject);
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

    public static String getPathToMessage(MessageObject messageObject) {
        String path = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(path)) {
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                path = null;
            }
        }
        if (TextUtils.isEmpty(path)) {
            path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(path);
            if (!temp.exists()) {
                return null;
            }
        }
        return path;
    }

    public void saveStickerToGallery(Activity activity, MessageObject messageObject, Utilities.Callback<Uri> callback) {
        saveStickerToGallery(activity, getPathToMessage(messageObject), messageObject.isVideoSticker(), callback);
    }

    public static void saveStickerToGallery(Activity activity, TLRPC.Document document, Utilities.Callback<Uri> callback) {
        String path = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(document, true).toString();
        File temp = new File(path);
        if (!temp.exists()) {
            return;
        }
        saveStickerToGallery(activity, path, MessageObject.isVideoSticker(document), callback);
    }

    private static void saveStickerToGallery(Activity activity, String path, boolean video, Utilities.Callback<Uri> callback) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                if (video) {
                    MediaController.saveFile(path, activity, 1, null, null, callback);
                } else {
                    Bitmap image = BitmapFactory.decodeFile(path);
                    if (image != null) {
                        File file = new File(path.endsWith(".webp") ? path.replace(".webp", ".png") : path + ".png");
                        FileOutputStream stream = new FileOutputStream(file);
                        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
                        stream.close();
                        MediaController.saveFile(file.toString(), activity, 0, null, null, callback);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    public static void readQrFromMessage(View parent, MessageObject selectedObject, MessageObject.GroupedMessages selectedObjectGroup, ViewGroup viewGroup, Utilities.Callback<ArrayList<QrHelper.QrResult>> callback, AtomicBoolean waitForQr, AtomicReference<Runnable> onQrDetectionDone) {
        waitForQr.set(true);
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<QrHelper.QrResult> qrResults = new ArrayList<>();
            ArrayList<MessageObject> messageObjects = new ArrayList<>();
            if (selectedObjectGroup != null) {
                messageObjects.addAll(selectedObjectGroup.messages);
            } else {
                messageObjects.add(selectedObject);
            }
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                if (child instanceof ChatMessageCell) {
                    var cell = (ChatMessageCell) child;
                    if (messageObjects.contains(cell.getMessageObject())) {
                        qrResults.addAll(QrHelper.readQr(cell.getPhotoImage().getBitmap()));
                    }
                }
            }
            AndroidUtilities.runOnUIThread(() -> {
                callback.run(qrResults);
                waitForQr.set(false);
                if (onQrDetectionDone.get() != null) {
                    onQrDetectionDone.get().run();
                    onQrDetectionDone.set(null);
                }
            });
        });
        parent.postDelayed(() -> {
            if (onQrDetectionDone.get() != null) {
                onQrDetectionDone.getAndSet(null).run();
            }
        }, 250);
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

    public void createDeleteHistoryAlert(BaseFragment fragment, TLRPC.Chat chat, TLRPC.TL_forumTopic forumTopic, long mergeDialogId, Theme.ResourcesProvider resourcesProvider) {
        if (fragment == null || fragment.getParentActivity() == null || chat == null) {
            return;
        }

        Context context = fragment.getParentActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);

        CheckBoxCell cell = forumTopic == null && ChatObject.isChannel(chat) && ChatObject.canUserDoAction(chat, ChatObject.ACTION_DELETE_MESSAGES) ? new CheckBoxCell(context, 1, resourcesProvider) : null;

        TextView messageTextView = new TextView(context);
        messageTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
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
        if (forumTopic != null) {
            if (forumTopic.id == 1) {
                imageView.setImageDrawable(ForumUtilities.createGeneralTopicDrawable(context, 0.75f, Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider)));
            } else {
                ForumUtilities.setTopicIcon(imageView, forumTopic, false, true, resourcesProvider);
            }
        } else {
            imageView.setForUserOrChat(chat, avatarDrawable);
        }
        frameLayout.addView(imageView, LayoutHelper.createFrame(40, 40, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 22, 5, 22, 0));

        TextView textView = new TextView(context);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        textView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
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
                showDeleteHistoryBulletin(fragment, 0, false, () -> getMessagesController().deleteUserChannelHistory(chat, getUserConfig().getCurrentUser(), null, 0), resourcesProvider);
            } else {
                deleteUserHistoryWithSearch(fragment, -chat.id, forumTopic != null ? forumTopic.id : 0, mergeDialogId, (count, deleteAction) -> showDeleteHistoryBulletin(fragment, count, true, deleteAction, resourcesProvider));
            }
        });
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        AlertDialog alertDialog = builder.create();
        fragment.showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold, resourcesProvider));
        }
    }

    public static void showDeleteHistoryBulletin(BaseFragment fragment, int count, boolean search, Runnable delayedAction, Theme.ResourcesProvider resourcesProvider) {
        if (fragment.getParentActivity() == null) {
            if (delayedAction != null) {
                delayedAction.run();
            }
            return;
        }
        Bulletin.ButtonLayout buttonLayout;
        if (search) {
            final Bulletin.TwoLineLottieLayout layout = new Bulletin.TwoLineLottieLayout(fragment.getParentActivity(), resourcesProvider);
            layout.titleTextView.setText(LocaleController.getString("DeleteAllFromSelfDone", R.string.DeleteAllFromSelfDone));
            layout.subtitleTextView.setText(LocaleController.formatPluralString("MessagesDeletedHint", count));
            layout.setTimer();
            buttonLayout = layout;
        } else {
            final Bulletin.LottieLayout layout = new Bulletin.LottieLayout(fragment.getParentActivity(), resourcesProvider);
            layout.textView.setText(LocaleController.getString("DeleteAllFromSelfDone", R.string.DeleteAllFromSelfDone));
            layout.setTimer();
            buttonLayout = layout;
        }
        buttonLayout.setButton(new Bulletin.UndoButton(fragment.getParentActivity(), true, resourcesProvider).setDelayedAction(delayedAction));
        Bulletin.make(fragment, buttonLayout, Bulletin.DURATION_PROLONG).show();
    }

    public void resetMessageContent(long dialogId, MessageObject messageObject, boolean translated) {
        resetMessageContent(dialogId, messageObject, translated, null, false, null);
    }

    public void resetMessageContent(long dialogId, MessageObject messageObject, boolean translated, boolean translating) {
        resetMessageContent(dialogId, messageObject, translated, null, translating, null);
    }

    public void resetMessageContent(long dialogId, MessageObject messageObject, boolean translated, Object original, boolean translating, Pair<String, String> translatedLanguage) {
        TLRPC.Message message = messageObject.messageOwner;

        MessageObject obj = new MessageObject(currentAccount, message, true, true);
        obj.originalMessage = original;
        obj.translating = translating;
        obj.translatedLanguage = translatedLanguage;
        obj.translated = translated;
        if (messageObject.isSponsored()) {
            obj.sponsoredId = messageObject.sponsoredId;
            obj.botStartParam = messageObject.botStartParam;
        }

        replaceMessagesObject(dialogId, obj);
    }

    private void replaceMessagesObject(long dialogId, MessageObject messageObject) {
        ArrayList<MessageObject> arrayList = new ArrayList<>();
        arrayList.add(messageObject);
        getNotificationCenter().postNotificationName(NotificationCenter.replaceMessagesObjects, dialogId, arrayList, false);
    }

    public void deleteUserHistoryWithSearch(BaseFragment fragment, final long dialogId, int replyMessageId, final long mergeDialogId, SearchMessagesResultCallback callback) {
        Utilities.globalQueue.postRunnable(() -> {
            ArrayList<Integer> messageIds = new ArrayList<>();
            var latch = new CountDownLatch(1);
            var peer = getMessagesController().getInputPeer(dialogId);
            var fromId = MessagesController.getInputPeer(getUserConfig().getCurrentUser());
            doSearchMessages(fragment, latch, messageIds, peer, replyMessageId, fromId, Integer.MAX_VALUE, 0);
            try {
                latch.await();
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (!messageIds.isEmpty()) {
                ArrayList<ArrayList<Integer>> lists = new ArrayList<>();
                final int N = messageIds.size();
                for (int i = 0; i < N; i += 100) {
                    lists.add(new ArrayList<>(messageIds.subList(i, Math.min(N, i + 100))));
                }
                var deleteAction = new Runnable() {
                    @Override
                    public void run() {
                        for (ArrayList<Integer> list : lists) {
                            getMessagesController().deleteMessages(list, null, null, dialogId, true, false);
                        }
                    }
                };
                AndroidUtilities.runOnUIThread(callback != null ? () -> callback.run(messageIds.size(), deleteAction) : deleteAction);
            }
            if (mergeDialogId != 0) {
                deleteUserHistoryWithSearch(fragment, mergeDialogId, 0, 0, null);
            }
        });
    }

    private interface SearchMessagesResultCallback {
        void run(int count, Runnable deleteAction);
    }

    public void doSearchMessages(BaseFragment fragment, CountDownLatch latch, ArrayList<Integer> messageIds, TLRPC.InputPeer peer, int replyMessageId, TLRPC.InputPeer fromId, int offsetId, long hash) {
        var req = new TLRPC.TL_messages_search();
        req.peer = peer;
        req.limit = 100;
        req.q = "";
        req.offset_id = offsetId;
        req.from_id = fromId;
        req.flags |= 1;
        req.filter = new TLRPC.TL_inputMessagesFilterEmpty();
        if (replyMessageId != 0) {
            req.top_msg_id = replyMessageId;
            req.flags |= 2;
        }
        req.hash = hash;
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (response instanceof TLRPC.messages_Messages) {
                var res = (TLRPC.messages_Messages) response;
                if (response instanceof TLRPC.TL_messages_messagesNotModified || res.messages.isEmpty()) {
                    latch.countDown();
                    return;
                }
                var newOffsetId = offsetId;
                for (TLRPC.Message message : res.messages) {
                    newOffsetId = Math.min(newOffsetId, message.id);
                    if (!message.out || message.post) {
                        continue;
                    }
                    messageIds.add(message.id);
                }
                doSearchMessages(fragment, latch, messageIds, peer, replyMessageId, fromId, newOffsetId, calcMessagesHash(res.messages));
            } else {
                if (error != null) {
                    AndroidUtilities.runOnUIThread(() -> AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text));
                }
                latch.countDown();
            }
        }, ConnectionsManager.RequestFlagFailOnServerErrors);
    }

    private long calcMessagesHash(ArrayList<TLRPC.Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        long acc = 0;
        for (TLRPC.Message message : messages) {
            acc = MediaDataController.calcHash(acc, message.id);
        }
        return acc;
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
        return String.format(Locale.US, "DC%d %s, %s", dc, MessageHelper.getDCName(dc), MessageHelper.getDCLocation(dc));
    }

    public void sendWebFile(BaseFragment fragment, int did, MessageObject thread, MessageObject reply_to, String url, boolean isPhoto, Theme.ResourcesProvider resourcesProvider) {
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
        var peer = getMessagesController().getInputPeer(did);
        req.peer = peer;
        if (peer instanceof TLRPC.TL_inputPeerChannel) {
            req.silent = MessagesController.getNotificationsSettings(currentAccount).getBoolean("silent_" + -peer.channel_id, false);
        } else if (peer instanceof TLRPC.TL_inputPeerChat) {
            req.silent = MessagesController.getNotificationsSettings(currentAccount).getBoolean("silent_" + -peer.chat_id, false);
        } else {
            req.silent = MessagesController.getNotificationsSettings(currentAccount).getBoolean("silent_" + peer.user_id, false);
        }
        req.message = "";
        if (reply_to != null) {
            req.reply_to = SendMessagesHelper.creteReplyInput(reply_to.getId(), thread.getId());
            req.flags |= 1;
        }
        getConnectionsManager().sendRequest(req, (response, error) -> {
            if (error == null) {
                getMessagesController().processUpdates((TLRPC.Updates) response, false);
            } else {
                AndroidUtilities.runOnUIThread(() -> {
                    if (BulletinFactory.canShowBulletin(fragment)) {
                        if (error.text.equals("MEDIA_EMPTY")) {
                            BulletinFactory.of(fragment).createErrorBulletin(LocaleController.getString("SendWebFileInvalid", R.string.SendWebFileInvalid), resourcesProvider).show();
                        } else {
                            AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("SendWebFile", R.string.SendWebFile), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text, resourcesProvider);
                        }
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
        editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_text_RedRegular, resourcesProvider));
        editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        editText.setBackground(null);
        editText.requestFocus();
        editText.setPadding(0, 0, 0, 0);
        ll.addView(editText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 36, 0, 24, 0, 24, 0));

        CheckBoxCell cell = new CheckBoxCell(context, 1, resourcesProvider);
        cell.setBackground(Theme.getSelectorDrawable(false));
        cell.setText(LocaleController.getString("SendWithoutCompression", R.string.SendWithoutCompression), "", false, false);
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(16) : AndroidUtilities.dp(8), 0, LocaleController.isRTL ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16), 0);
        ll.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        cell.setOnClickListener(v -> {
            CheckBoxCell cell12 = (CheckBoxCell) v;
            cell12.setChecked(!cell12.isChecked(), true);
        });

        builder.setView(ll);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> sendWebFile(fragment, (int) fragment.getDialogId(), fragment.getThreadMessage(), fragment.getReplyMessage(), editText.getText().toString(), !cell.isChecked(), resourcesProvider));
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            editText.requestFocus();
            AndroidUtilities.showKeyboard(editText);
        });
        fragment.showDialog(alertDialog);
        editText.setSelection(0, editText.getText().length());
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
