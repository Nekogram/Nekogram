package tw.nekomimi.nekogram.helpers;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;

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
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ChatAttachAlert;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

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
        saveStickerToGallery(activity, path, callback);
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
                Bitmap image;
                if (Build.VERSION.SDK_INT >= 19) {
                    image = BitmapFactory.decodeFile(path);
                } else {
                    RandomAccessFile file = new RandomAccessFile(path, "r");
                    ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, path.length());
                    BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                    bmOptions.inJustDecodeBounds = true;
                    Utilities.loadWebpImage(null, buffer, buffer.limit(), bmOptions, true);
                    image = Bitmaps.createBitmap(bmOptions.outWidth, bmOptions.outHeight, Bitmap.Config.ARGB_8888);
                    Utilities.loadWebpImage(image, buffer, buffer.limit(), null, true);
                    file.close();
                }
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
                    long channelId = 0;
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
                    getMessagesController().deleteMessages(ids, null, null, -channelId, true, true, false);
                    deleteUserChannelHistoryWithSearch(fragment, dialog_id, lastMessageId);
                }
            } else {
                AlertsCreator.showSimpleAlert(fragment, LocaleController.getString("SendWebFile", R.string.SendWebFile), LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred) + "\n" + error.text);
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
