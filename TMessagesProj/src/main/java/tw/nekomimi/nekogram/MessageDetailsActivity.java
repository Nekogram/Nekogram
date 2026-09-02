package tw.nekomimi.nekogram;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.LinearLayout;

import androidx.core.content.FileProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FlagSecureReason;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import tw.nekomimi.nekogram.helpers.MessageHelper;
import tw.nekomimi.nekogram.helpers.UserHelper;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.tlv.TlViewer;

public class MessageDetailsActivity extends BaseNekoSettingsActivity implements NotificationCenter.NotificationCenterDelegate {

    private final MessageObject messageObject;
    private final boolean noforwards;

    private TLRPC.Chat toChat;
    private TLRPC.User fromUser;
    private TLRPC.Chat fromChat;
    private TLRPC.Peer forwardFromPeer;
    private String filePath;
    private String fileName;
    private int width;
    private int height;
    private String video_codec;
    private int dc;
    private long stickerSetOwner;
    private final ArrayList<Long> emojiSetOwners = new ArrayList<>();
    private FlagSecureReason flagSecure;

    private final int idRow = rowId++;
    private final int messageRow = rowId++;
    private final int captionRow = rowId++;
    private final int groupRow = rowId++;
    private final int channelRow = rowId++;
    private final int fromRow = rowId++;
    private final int botRow = rowId++;
    private final int dateRow = rowId++;
    private final int editedRow = rowId++;
    private final int forwardRow = rowId++;
    private final int restrictionReasonRow = rowId++;
    private final int viewsAndForwardsRow = rowId++;

    private final int fileNameRow = rowId++;
    private final int filePathRow = rowId++;
    private final int fileSizeRow = rowId++;
    private final int fileMimeTypeRow = rowId++;
    private final int mediaRow = rowId++;
    private final int dcRow = rowId++;

    private final int stickerSetRow = rowId++;
    private final int emojiSetRow = rowId++;
    private final int shouldBlockMessageRow = rowId++;
    private final int languageRow = rowId++;
    private final int linkOrEmojiOnlyRow = rowId++;

    private final int exportRow = rowId++;

    public MessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;

        if (messageObject.messageOwner.peer_id != null) {
            var peer = messageObject.messageOwner.peer_id;
            if (peer.channel_id != 0 || peer.chat_id != 0) {
                toChat = getMessagesController().getChat(peer.channel_id != 0 ? peer.channel_id : peer.chat_id);
            }
        }

        if (messageObject.messageOwner.fwd_from != null && messageObject.messageOwner.fwd_from.from_id != null) {
            forwardFromPeer = messageObject.messageOwner.fwd_from.from_id;
        }

        if (messageObject.messageOwner.from_id != null) {
            var peer = messageObject.messageOwner.from_id;
            if (peer.channel_id != 0 || peer.chat_id != 0) {
                fromChat = getMessagesController().getChat(peer.channel_id != 0 ? peer.channel_id : peer.chat_id);
            } else if (peer.user_id != 0) {
                fromUser = getMessagesController().getUser(peer.user_id);
            }
        }

        var media = MessageObject.getMedia(messageObject.messageOwner);
        if (media != null) {
            filePath = MessageHelper.getPathToMessage(messageObject);
            var photo = media.webpage != null ? media.webpage.photo : media.photo;
            if (photo != null) {
                dc = photo.dc_id;
                var photoSize = FileLoader.getClosestPhotoSizeWithSize(photo.sizes, Integer.MAX_VALUE);
                if (photoSize != null) {
                    width = photoSize.w;
                    height = photoSize.h;
                }
            }
            var document = media.webpage != null ? media.webpage.document : media.document;
            if (document != null) {
                dc = document.dc_id;
                for (var attribute : document.attributes) {
                    if (attribute instanceof TLRPC.TL_documentAttributeFilename) {
                        fileName = attribute.file_name;
                    }
                    if (attribute instanceof TLRPC.TL_documentAttributeSticker) {
                        stickerSetOwner = UserHelper.getOwnerFromStickerSetId(attribute.stickerset.id);
                    }
                    if (attribute instanceof TLRPC.TL_documentAttributeImageSize ||
                            attribute instanceof TLRPC.TL_documentAttributeVideo) {
                        width = attribute.w;
                        height = attribute.h;
                        video_codec = attribute.video_codec;
                    }
                }
            }
        }

        if (messageObject.messageOwner.entities != null) {
            for (var entity : messageObject.messageOwner.entities) {
                if (entity instanceof TLRPC.TL_messageEntityCustomEmoji) {
                    TLRPC.Document document = AnimatedEmojiDrawable.findDocument(currentAccount, ((TLRPC.TL_messageEntityCustomEmoji) entity).document_id);
                    TLRPC.InputStickerSet stickerSet = MessageObject.getInputStickerSet(document);
                    if (stickerSet == null) {
                        continue;
                    }
                    long owner = UserHelper.getOwnerFromStickerSetId(stickerSet.id);
                    if (owner != 0 && !emojiSetOwners.contains(owner)) {
                        emojiSetOwners.add(owner);
                    }
                }
            }
        }

        noforwards = isPeerNoForwards() ||
                messageObject.messageOwner.noforwards ||
                messageObject.type == MessageObject.TYPE_PAID_MEDIA;
    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        NotificationCenter.getGlobalInstance().addObserver(this, NotificationCenter.emojiLoaded);

        return true;
    }

    @Override
    public Integer getSelectorColor(int position) {
        var item = listView.adapter.getItem(position);
        if (item.id == exportRow) {
            return Theme.multAlpha(getThemedColor(Theme.key_switchTrackChecked), .1f);
        }
        return super.getSelectorColor(position);
    }

    private void showNoForwards() {
        if (isPeerNoForwards()) {
            BulletinFactory.of(this).createErrorBulletin(
                    LocaleController.getString(
                            toChat == null ? R.string.ForwardsRestrictedInfoUser :
                                    toChat.broadcast ?
                                            R.string.ForwardsRestrictedInfoChannel :
                                            R.string.ForwardsRestrictedInfoGroup)
            ).show();
        } else {
            BulletinFactory.of(this).createErrorBulletin(
                    LocaleController.getString(R.string.ForwardsRestrictedInfoBot)).show();
        }
    }

    public boolean isPeerNoForwards() {
        return toChat != null ?
                getMessagesController().isChatNoForwards(toChat) :
                fromUser != null && getMessagesController().isUserNoForwards(fromUser.id);
    }

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        flagSecure = new FlagSecureReason(getParentActivity().getWindow(), () -> noforwards);

        return fragmentView;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (!messageObject.isSponsored()) {
            items.add(TextDetailSettingsCellFactory.of(idRow, "ID", String.valueOf(messageObject.messageOwner.id)));
        }
        if (!TextUtils.isEmpty(messageObject.messageText)) {
            items.add(TextDetailSettingsCellFactory.of(messageRow, "Message", messageObject.messageText.toString()));
        }
        if (!TextUtils.isEmpty(messageObject.caption)) {
            items.add(TextDetailSettingsCellFactory.of(captionRow, "Caption", messageObject.caption.toString()));
        }
        if (toChat != null && !toChat.broadcast) {
            var builder = new StringBuilder();
            appendUserOrChat(toChat, builder);
            items.add(TextDetailSettingsCellFactory.of(channelRow, toChat.broadcast ? "Channel" : "Group", builder));
        }
        if (fromUser != null || fromChat != null || messageObject.messageOwner.post_author != null) {
            var builder = new StringBuilder();
            if (fromUser != null) {
                appendUserOrChat(fromUser, builder);
            } else if (fromChat != null) {
                appendUserOrChat(fromChat, builder);
            } else if (!TextUtils.isEmpty(messageObject.messageOwner.post_author)) {
                builder.append(messageObject.messageOwner.post_author);
            }
            items.add(TextDetailSettingsCellFactory.of(fromRow, "From", builder));
        }
        if (fromUser != null && fromUser.bot) {
            items.add(TextDetailSettingsCellFactory.of(botRow, "Bot", "Yes"));
        }
        if (messageObject.messageOwner.date != 0) {
            items.add(TextDetailSettingsCellFactory.of(dateRow, messageObject.scheduled ? "Scheduled date" : "Date", formatTime(messageObject.messageOwner.date)));
        }
        if (messageObject.messageOwner.edit_date != 0) {
            items.add(TextDetailSettingsCellFactory.of(editedRow, "Edited", formatTime(messageObject.messageOwner.edit_date)));
        }
        if (messageObject.isForwarded()) {
            var builder = new StringBuilder();
            if (forwardFromPeer != null) {
                if (forwardFromPeer.channel_id != 0 || forwardFromPeer.chat_id != 0) {
                    var chat = getMessagesController().getChat(forwardFromPeer.channel_id != 0 ? forwardFromPeer.channel_id : forwardFromPeer.chat_id);
                    appendUserOrChat(chat, builder);
                } else if (forwardFromPeer.user_id != 0) {
                    var user = getMessagesController().getUser(forwardFromPeer.user_id);
                    appendUserOrChat(user, builder);
                }
            } else if (!TextUtils.isEmpty(messageObject.messageOwner.fwd_from.from_name)) {
                builder.append(messageObject.messageOwner.fwd_from.from_name);
            }
            builder.append("\n").append(formatTime(messageObject.messageOwner.fwd_from.date));
            items.add(TextDetailSettingsCellFactory.of(forwardRow, "Forward from", builder));
        }
        if (!messageObject.messageOwner.restriction_reason.isEmpty()) {
            var reasons = messageObject.messageOwner.restriction_reason;
            var value = new StringBuilder();
            for (var reason : reasons) {
                value.append(reason.reason);
                value.append("-");
                value.append(reason.platform);
                if (reasons.indexOf(reason) != reasons.size() - 1) {
                    value.append(", ");
                }
            }
            items.add(TextDetailSettingsCellFactory.of(restrictionReasonRow, "Restriction reason", value));
        }
        if (messageObject.messageOwner.views > 0 || messageObject.messageOwner.forwards > 0) {
            items.add(TextDetailSettingsCellFactory.of(viewsAndForwardsRow, "Views and forwards", String.format(Locale.US, "%d views, %d forwards", messageObject.messageOwner.views, messageObject.messageOwner.forwards)));
        }

        if (!TextUtils.isEmpty(fileName)) {
            items.add(TextDetailSettingsCellFactory.of(fileNameRow, "File name", fileName));
        }
        if (!TextUtils.isEmpty(filePath)) {
            items.add(TextDetailSettingsCellFactory.of(filePathRow, "File path", filePath));
        }
        if (messageObject.getSize() > 0) {
            items.add(TextDetailSettingsCellFactory.of(fileSizeRow, "File size", AndroidUtilities.formatFileSize(messageObject.getSize())));
        }
        if (!TextUtils.isEmpty(messageObject.getMimeType())) {
            items.add(TextDetailSettingsCellFactory.of(fileMimeTypeRow, "MimeType", messageObject.getMimeType()));
        }
        if (width > 0 && height > 0) {
            items.add(TextDetailSettingsCellFactory.of(mediaRow, "Media", String.format(Locale.US, "%dx%d", width, height) +
                    (TextUtils.isEmpty(video_codec) ? "" : (", " + video_codec))));
        }
        if (dc != 0) {
            items.add(TextDetailSettingsCellFactory.of(dcRow, "DC", UserHelper.formatDCString(dc)));
        }

        if (stickerSetOwner > 0) {
            var builder = new StringBuilder();
            var user = getMessagesController().getUser(stickerSetOwner);
            if (user != null) {
                appendUserOrChat(user, builder);
                items.add(TextDetailSettingsCellFactory.of(stickerSetRow, "Sticker Pack creator", builder));
            } else {
                builder.append("Loading...");
                builder.append("\n");
                builder.append(stickerSetOwner);
                var item = TextDetailSettingsCellFactory.of(stickerSetRow, "Sticker Pack creator", builder);
                getUserHelper().searchUser(stickerSetOwner, user1 -> {
                    var builder1 = new StringBuilder();
                    if (user1 != null) {
                        appendUserOrChat(user1, builder1);
                    } else {
                        builder1.append(stickerSetOwner);
                    }
                    item.subtext = builder1;
                    notifyItemChanged(stickerSetRow);
                });
                items.add(item);
            }
        }
        if (!emojiSetOwners.isEmpty()) {
            items.add(TextDetailSettingsCellFactory.of(emojiSetRow, "Emoji Pack creators", TextUtils.join(", ", emojiSetOwners)));
        }
        if (messageObject.shouldBlockMessage()) {
            items.add(TextDetailSettingsCellFactory.of(shouldBlockMessageRow, "Blocked", "Yes"));
        }
        if (!TextUtils.isEmpty(getMessageHelper().getMessagePlainText(messageObject))) {
            var item = TextDetailSettingsCellFactory.of(languageRow, "Language", "Loading...");
            LanguageDetector.detectLanguage(
                    getMessageHelper().getMessagePlainText(messageObject),
                    lang -> {
                        item.subtext = lang;
                        notifyItemChanged(languageRow);
                    },
                    e -> {
                        item.subtext = e.getLocalizedMessage();
                        notifyItemChanged(languageRow);
                    });
            items.add(item);
        }
        if (!TextUtils.isEmpty(messageObject.messageOwner.message) && MessageHelper.isLinkOrEmojiOnlyMessage(messageObject)) {
            items.add(TextDetailSettingsCellFactory.of(linkOrEmojiOnlyRow, "Link or emoji only", "Yes"));
        }
        items.add(UItem.asShadow(null));


        items.add(TextSettingsCellFactory.of(exportRow, LocaleController.getString(R.string.ViewAsJson)).accent());
        items.add(UItem.asShadow(null));
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (id == dcRow) {
            AlertsCreator.createSimplePopup(this, new DatacenterPopupWrapper(this, null, resourcesProvider).windowLayout, view, Math.round(x), Math.round(y));
        } else if (id == filePathRow) {
            if (!noforwards) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                var uri = FileProvider.getUriForFile(getParentActivity(), ApplicationLoader.getApplicationId() + ".provider", new File(filePath));
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                intent.setDataAndType(uri, messageObject.getMimeType());
                startActivityForResult(Intent.createChooser(intent, LocaleController.getString(R.string.ShareFile)), 500);
            } else {
                showNoForwards();
            }
        } else if (id == channelRow || id == groupRow) {
            if (toChat != null) {
                Bundle args = new Bundle();
                args.putLong("chat_id", toChat.id);
                ProfileActivity fragment = new ProfileActivity(args);
                presentFragment(fragment);
            }
        } else if (id == fromRow) {
            Bundle args = new Bundle();
            if (fromChat != null) {
                args.putLong("chat_id", fromChat.id);
            } else if (fromUser != null) {
                args.putLong("user_id", fromUser.id);
            }
            ProfileActivity fragment = new ProfileActivity(args);
            presentFragment(fragment);
        } else if (id == forwardRow) {
            if (forwardFromPeer != null) {
                Bundle args = new Bundle();
                if (forwardFromPeer.channel_id != 0 || forwardFromPeer.chat_id != 0) {
                    args.putLong("chat_id", forwardFromPeer.channel_id != 0 ? forwardFromPeer.channel_id : forwardFromPeer.chat_id);
                } else if (forwardFromPeer.user_id != 0) {
                    args.putLong("user_id", forwardFromPeer.user_id);
                }
                ProfileActivity fragment = new ProfileActivity(args);
                presentFragment(fragment);
            }
        } else if (id == restrictionReasonRow) {
            var reasons = messageObject.messageOwner.restriction_reason;
            var ll = new LinearLayout(getParentActivity());
            ll.setOrientation(LinearLayout.VERTICAL);

            var dialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                    .setView(ll)
                    .create();

            for (var reason : reasons) {
                TextDetailSettingsCell cell = new TextDetailSettingsCell(getParentActivity(), resourcesProvider);
                cell.setBackground(Theme.getSelectorDrawable(false));
                cell.setMultilineDetail(true);
                cell.setOnClickListener(v1 -> {
                    dialog.dismiss();
                    AndroidUtilities.addToClipboard(cell.getValueTextView().getText());
                    BulletinFactory.of(this).createCopyBulletin(LocaleController.formatString(R.string.TextCopied)).show();
                });
                cell.setTextAndValue(reason.reason + "-" + reason.platform, reason.text, false);

                ll.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            }

            showDialog(dialog);
        } else if (id == stickerSetRow) {
            if (stickerSetOwner != 0) {
                Bundle args = new Bundle();
                args.putLong("user_id", stickerSetOwner);
                ProfileActivity fragment = new ProfileActivity(args);
                presentFragment(fragment);
            }
        } else if (id == emojiSetRow) {
            var ll = new LinearLayout(getParentActivity());
            ll.setOrientation(LinearLayout.VERTICAL);

            var dialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                    .setView(ll)
                    .create();

            for (var emojiSetOwner : emojiSetOwners) {
                var cell = new TextDetailSettingsCell(getParentActivity(), true, resourcesProvider);
                cell.setBackground(Theme.getSelectorDrawable(false));
                cell.setMultilineDetail(true);
                cell.setOnClickListener(v1 -> {
                    dialog.dismiss();
                    Bundle args = new Bundle();
                    args.putLong("user_id", emojiSetOwner);
                    ProfileActivity fragment = new ProfileActivity(args);
                    presentFragment(fragment);
                });
                ll.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

                var builder = new StringBuilder();
                TLRPC.User user = getMessagesController().getUser(emojiSetOwner);
                if (user != null) {
                    appendUserOrChat(user, builder);
                } else {
                    getUserHelper().searchUser(emojiSetOwner, user1 -> {
                        StringBuilder builder1 = new StringBuilder();
                        if (user1 != null) {
                            appendUserOrChat(user1, builder1);
                        } else {
                            builder1.append(emojiSetOwner);
                        }
                        cell.setTextAndValueWithEmoji("", builder1, false);
                    });
                    builder.append("Loading...");
                    builder.append("\n");
                    builder.append(emojiSetOwner);
                }
                cell.setTextAndValueWithEmoji("", builder, false);
            }

            showDialog(dialog);
        } else if (id == exportRow) {
            TlViewer.openTlViewer(this,
                    messageObject.currentEvent != null ? messageObject.currentEvent : messageObject.messageOwner);
        }
    }

    @Override
    protected boolean onItemLongClick(UItem item, View view, int position, float x, float y) {
        var id = item.id;
        if (item.viewType != UniversalAdapter.VIEW_TYPE_SHADOW && id != exportRow) {
            if (!noforwards || !(id == messageRow || id == captionRow || id == filePathRow)) {
                var text = ((TextDetailSettingsCell) view).getValueTextView().getText();
                AndroidUtilities.addToClipboard(text);
                BulletinFactory.of(this).createCopyBulletin(LocaleController.formatString(R.string.TextCopied)).show();
                return true;
            } else {
                showNoForwards();
                return true;
            }
        }
        return false;
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.MessageDetails);
    }

    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.emojiLoaded) {
            if (listView != null) {
                listView.invalidateViews();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        flagSecure.attach();
    }

    @Override
    public void onBecomeFullyHidden() {
        super.onBecomeFullyHidden();
        flagSecure.detach();
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        NotificationCenter.getGlobalInstance().removeObserver(this, NotificationCenter.emojiLoaded);
    }

    private String formatTime(int timestamp) {
        if (timestamp == 0x7ffffffe) {
            return "When online";
        } else {
            return timestamp + "\n" + LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterYear().format(new Date(timestamp * 1000L)), LocaleController.getInstance().getFormatterDayWithSeconds().format(new Date(timestamp * 1000L)));
        }
    }

    private void appendUserOrChat(TLObject object, StringBuilder builder) {
        if (object instanceof TLRPC.User user) {
            builder.append(ContactsController.formatName(user.first_name, user.last_name));
            builder.append("\n");
            var username = UserObject.getPublicUsername(user);
            if (!TextUtils.isEmpty(username)) {
                builder.append("@");
                builder.append(username);
                builder.append("\n");
            }
            builder.append(user.id);
        } else if (object instanceof TLRPC.Chat chat) {
            builder.append(chat.title);
            builder.append("\n");
            var username = ChatObject.getPublicUsername(chat);
            if (!TextUtils.isEmpty(username)) {
                builder.append("@");
                builder.append(username);
                builder.append("\n");
            }
            builder.append(chat.id);
        }
    }
}
