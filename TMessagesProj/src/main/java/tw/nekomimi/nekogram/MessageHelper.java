package tw.nekomimi.nekogram;

import android.annotation.SuppressLint;
import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Cells.ChatMessageCell;

import java.util.ArrayList;

import tw.nekomimi.nekogram.settings.NekoGeneralSettingsActivity;
import tw.nekomimi.nekogram.translator.TranslateBottomSheet;
import tw.nekomimi.nekogram.translator.Translator;

public class MessageHelper extends BaseController {

    private static volatile MessageHelper[] Instance = new MessageHelper[UserConfig.MAX_ACCOUNT_COUNT];
    @SuppressLint("StaticFieldLeak")
    private static AlertDialog progressDialog;
    private int lastReqId;

    public MessageHelper(int num) {
        super(num);
    }

    public static void setMessageContent(MessageObject messageObject, ChatMessageCell chatMessageCell, String message) {
        messageObject.messageOwner.message = message;
        if (messageObject.caption != null) {
            messageObject.caption = null;
            messageObject.generateCaption();
            messageObject.forceUpdate = true;
        }
        messageObject.applyNewText();
        messageObject.resetLayout();
        chatMessageCell.requestLayout();
        chatMessageCell.invalidate();
    }

    public static void showTranslateDialog(Context context, String query) {
        if (NekoConfig.translationProvider < 0) {
            TranslateBottomSheet.show(context, query);
        } else {
            if (progressDialog != null) {
                progressDialog.dismiss();
            }
            progressDialog = new AlertDialog(context, 3);
            progressDialog.showDelayed(400);
            Translator.translate(query, new Translator.TranslateCallBack() {
                @Override
                public void onSuccess(String translation) {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(translation);
                    builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
                    builder.setNeutralButton(LocaleController.getString("Copy", R.string.Copy), (dialog, which) -> AndroidUtilities.addToClipboard(translation));
                    builder.show();
                }

                @Override
                public void onError() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(LocaleController.getString("TranslateFailed", R.string.TranslateFailed));
                    builder.setNeutralButton(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), (dialog, which) -> NekoGeneralSettingsActivity.getTranslationProviderAlert(context).show());
                    builder.setPositiveButton(LocaleController.getString("Retry", R.string.Retry), (dialog, which) -> showTranslateDialog(context, query));
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.show();
                }

                @Override
                public void onUnsupported() {
                    if (progressDialog != null) {
                        progressDialog.dismiss();
                    }
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(LocaleController.getString("TranslateApiUnsupported", R.string.TranslateApiUnsupported));
                    builder.setPositiveButton(LocaleController.getString("TranslationProvider", R.string.TranslationProvider), (dialog, which) -> NekoGeneralSettingsActivity.getTranslationProviderAlert(context).show());
                    builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
                    builder.show();
                }
            });
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
