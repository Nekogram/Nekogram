package tw.nekomimi.nekogram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;

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
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.AnimatedEmojiDrawable;
import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import tw.nekomimi.nekogram.helpers.MessageHelper;
import tw.nekomimi.nekogram.helpers.UserHelper;
import tw.nekomimi.nekogram.helpers.WebAppHelper;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;

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

    private int idRow;
    private int messageRow;
    private int captionRow;
    private int groupRow;
    private int channelRow;
    private int fromRow;
    private int botRow;
    private int dateRow;
    private int editedRow;
    private int forwardRow;
    private int restrictionReasonRow;
    private int viewsAndForwardsRow;

    private int fileNameRow;
    private int filePathRow;
    private int fileSizeRow;
    private int fileMimeTypeRow;
    private int mediaRow;
    private int dcRow;

    private int stickerSetRow;
    private int emojiSetRow;
    private int shouldBlockMessageRow;
    private int languageRow;
    private int linkOrEmojiOnlyRow;
    private int emptyRow;

    private int exportRow;
    private int endRow;

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

        noforwards = getMessagesController().isChatNoForwards(toChat) ||
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
        if (position == exportRow) {
            return Theme.multAlpha(getThemedColor(Theme.key_switchTrackChecked), .1f);
        }
        return super.getSelectorColor(position);
    }

    @Override
    protected boolean hasWhiteActionBar() {
        return false;
    }

    private void showNoForwards() {
        if (getMessagesController().isChatNoForwards(toChat)) {
            BulletinFactory.of(this).createErrorBulletin(toChat.broadcast ?
                    LocaleController.getString(R.string.ForwardsRestrictedInfoChannel) :
                    LocaleController.getString(R.string.ForwardsRestrictedInfoGroup)
            ).show();
        } else {
            BulletinFactory.of(this).createErrorBulletin(
                    LocaleController.getString(R.string.ForwardsRestrictedInfoBot)).show();
        }
    }

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        flagSecure = new FlagSecureReason(getParentActivity().getWindow(), () -> noforwards);

        return fragmentView;
    }

    @Override
    protected void onItemClick(View view, int position, float x, float y) {
        if (position == dcRow) {
            AlertsCreator.createSimplePopup(this, new DatacenterPopupWrapper(this, null, resourcesProvider).windowLayout, view, Math.round(x), Math.round(y));
        } else if (position == filePathRow) {
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
        } else if (position == channelRow || position == groupRow) {
            if (toChat != null) {
                Bundle args = new Bundle();
                args.putLong("chat_id", toChat.id);
                ProfileActivity fragment = new ProfileActivity(args);
                presentFragment(fragment);
            }
        } else if (position == fromRow) {
            Bundle args = new Bundle();
            if (fromChat != null) {
                args.putLong("chat_id", fromChat.id);
            } else if (fromUser != null) {
                args.putLong("user_id", fromUser.id);
            }
            ProfileActivity fragment = new ProfileActivity(args);
            presentFragment(fragment);
        } else if (position == forwardRow) {
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
        } else if (position == restrictionReasonRow) {
            ArrayList<TLRPC.RestrictionReason> reasons = messageObject.messageOwner.restriction_reason;
            LinearLayout ll = new LinearLayout(getParentActivity());
            ll.setOrientation(LinearLayout.VERTICAL);

            AlertDialog dialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                    .setView(ll)
                    .create();

            for (TLRPC.RestrictionReason reason : reasons) {
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
        } else if (position == stickerSetRow) {
            if (stickerSetOwner != 0) {
                Bundle args = new Bundle();
                args.putLong("user_id", stickerSetOwner);
                ProfileActivity fragment = new ProfileActivity(args);
                presentFragment(fragment);
            }
        } else if (position == emojiSetRow) {
            LinearLayout ll = new LinearLayout(getParentActivity());
            ll.setOrientation(LinearLayout.VERTICAL);

            AlertDialog dialog = new AlertDialog.Builder(getParentActivity(), resourcesProvider)
                    .setView(ll)
                    .create();

            for (Long emojiSetOwner : emojiSetOwners) {
                TextDetailSettingsCell cell = new TextDetailSettingsCell(getParentActivity(), true, resourcesProvider);
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

                StringBuilder builder = new StringBuilder();
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
        } else if (position == exportRow) {
            WebAppHelper.openTLViewer(this,
                    messageObject.currentEvent != null ? messageObject.currentEvent : messageObject.messageOwner);
        }
    }

    @Override
    protected boolean onItemLongClick(View view, int position, float x, float y) {
        var type = listAdapter.getItemViewType(position);
        if (type != TYPE_SHADOW && position != exportRow) {
            if (!noforwards || !(position == messageRow || position == captionRow || position == filePathRow)) {
                CharSequence text;
                if (view instanceof TextDetailSettingsCell textCell) {
                    text = textCell.getValueTextView().getText();
                } else {
                    TextDetailSimpleCell textCell = (TextDetailSimpleCell) view;
                    text = textCell.getValueTextView().getText();
                }
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
    protected BaseListAdapter createAdapter(Context context) {
        return new ListAdapter(context);
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.MessageDetails);
    }

    @Override
    protected void updateRows() {
        rowCount = 0;
        idRow = messageObject.isSponsored() ? -1 : rowCount++;
        messageRow = TextUtils.isEmpty(messageObject.messageText) ? -1 : rowCount++;
        captionRow = TextUtils.isEmpty(messageObject.caption) ? -1 : rowCount++;
        groupRow = toChat != null && !toChat.broadcast ? rowCount++ : -1;
        channelRow = toChat != null && toChat.broadcast ? rowCount++ : -1;
        fromRow = fromUser != null || fromChat != null || messageObject.messageOwner.post_author != null ? rowCount++ : -1;
        botRow = fromUser != null && fromUser.bot ? rowCount++ : -1;
        dateRow = messageObject.messageOwner.date != 0 ? rowCount++ : -1;
        editedRow = messageObject.messageOwner.edit_date != 0 ? rowCount++ : -1;
        forwardRow = messageObject.isForwarded() ? rowCount++ : -1;
        restrictionReasonRow = messageObject.messageOwner.restriction_reason.isEmpty() ? -1 : rowCount++;
        viewsAndForwardsRow = messageObject.messageOwner.views > 0 || messageObject.messageOwner.forwards > 0 ? rowCount++ : -1;

        fileNameRow = TextUtils.isEmpty(fileName) ? -1 : rowCount++;
        filePathRow = TextUtils.isEmpty(filePath) ? -1 : rowCount++;
        fileSizeRow = messageObject.getSize() > 0 ? rowCount++ : -1;
        fileMimeTypeRow = !TextUtils.isEmpty(messageObject.getMimeType()) ? rowCount++ : -1;
        mediaRow = width > 0 && height > 0 ? rowCount++ : -1;
        dcRow = dc != 0 ? rowCount++ : -1;

        stickerSetRow = stickerSetOwner > 0 ? rowCount++ : -1;
        emojiSetRow = emojiSetOwners.isEmpty() ? -1 : rowCount++;
        shouldBlockMessageRow = messageObject.shouldBlockMessage() ? rowCount++ : -1;
        languageRow = TextUtils.isEmpty(getMessageHelper().getMessagePlainText(messageObject)) ? -1 : rowCount++;
        linkOrEmojiOnlyRow = !TextUtils.isEmpty(messageObject.messageOwner.message) && MessageHelper.isLinkOrEmojiOnlyMessage(messageObject) ? rowCount++ : -1;
        emptyRow = rowCount++;

        exportRow = rowCount++;
        endRow = rowCount++;
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

    private class ListAdapter extends BaseListAdapter {

        public ListAdapter(Context context) {
            super(context);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, boolean partial, boolean divider) {
            switch (holder.getItemViewType()) {
                case TYPE_SETTINGS: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == exportRow) {
                        textCell.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteBlueText2));
                        textCell.setTextAndValue(LocaleController.getString(R.string.ViewAsJson), null, false);
                    }
                    break;
                }
                case TYPE_DETAIL_SETTINGS: {
                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
                    textCell.setMultilineDetail(true);
                    if (position == idRow) {
                        textCell.setTextAndValue("ID", String.valueOf(messageObject.messageOwner.id), divider);
                    } else if (position == channelRow || position == groupRow) {
                        StringBuilder builder = new StringBuilder();
                        appendUserOrChat(toChat, builder);
                        textCell.setTextAndValueWithEmoji(position == channelRow ? "Channel" : "Group", builder, divider);
                    } else if (position == fromRow) {
                        StringBuilder builder = new StringBuilder();
                        if (fromUser != null) {
                            appendUserOrChat(fromUser, builder);
                        } else if (fromChat != null) {
                            appendUserOrChat(fromChat, builder);
                        } else if (!TextUtils.isEmpty(messageObject.messageOwner.post_author)) {
                            builder.append(messageObject.messageOwner.post_author);
                        }
                        textCell.setTextAndValueWithEmoji("From", builder, divider);
                    } else if (position == botRow) {
                        textCell.setTextAndValue("Bot", "Yes", divider);
                    } else if (position == dateRow) {
                        textCell.setTextAndValue(messageObject.scheduled ? "Scheduled date" : "Date", formatTime(messageObject.messageOwner.date), divider);
                    } else if (position == editedRow) {
                        textCell.setTextAndValue("Edited", formatTime(messageObject.messageOwner.edit_date), divider);
                    } else if (position == forwardRow) {
                        StringBuilder builder = new StringBuilder();
                        if (forwardFromPeer != null) {
                            if (forwardFromPeer.channel_id != 0 || forwardFromPeer.chat_id != 0) {
                                TLRPC.Chat chat = getMessagesController().getChat(forwardFromPeer.channel_id != 0 ? forwardFromPeer.channel_id : forwardFromPeer.chat_id);
                                appendUserOrChat(chat, builder);
                            } else if (forwardFromPeer.user_id != 0) {
                                TLRPC.User user = getMessagesController().getUser(forwardFromPeer.user_id);
                                appendUserOrChat(user, builder);
                            }
                        } else if (!TextUtils.isEmpty(messageObject.messageOwner.fwd_from.from_name)) {
                            builder.append(messageObject.messageOwner.fwd_from.from_name);
                        }
                        builder.append("\n").append(formatTime(messageObject.messageOwner.fwd_from.date));
                        textCell.setTextAndValueWithEmoji("Forward from", builder, divider);
                    } else if (position == fileNameRow) {
                        textCell.setTextAndValue("File name", fileName, divider);
                    } else if (position == filePathRow) {
                        textCell.setTextAndValue("File path", filePath, divider);
                    } else if (position == fileSizeRow) {
                        textCell.setTextAndValue("File size", AndroidUtilities.formatFileSize(messageObject.getSize()), divider);
                    } else if (position == fileMimeTypeRow) {
                        textCell.setTextAndValue("Mime type", messageObject.getMimeType(), divider);
                    } else if (position == mediaRow) {
                        textCell.setTextAndValue("Media", String.format(Locale.US, "%dx%d", width, height) +
                                (TextUtils.isEmpty(video_codec) ? "" : (", " + video_codec)), divider);
                    } else if (position == dcRow) {
                        textCell.setTextAndValue("DC", UserHelper.formatDCString(dc), divider);
                    } else if (position == restrictionReasonRow) {
                        ArrayList<TLRPC.RestrictionReason> reasons = messageObject.messageOwner.restriction_reason;
                        StringBuilder value = new StringBuilder();
                        for (TLRPC.RestrictionReason reason : reasons) {
                            value.append(reason.reason);
                            value.append("-");
                            value.append(reason.platform);
                            if (reasons.indexOf(reason) != reasons.size() - 1) {
                                value.append(", ");
                            }
                        }
                        textCell.setTextAndValue("Restriction reason", value, divider);
                    } else if (position == viewsAndForwardsRow) {
                        textCell.setTextAndValue("Views and forwards", String.format(Locale.US, "%d views, %d forwards", messageObject.messageOwner.views, messageObject.messageOwner.forwards), divider);
                    } else if (position == shouldBlockMessageRow) {
                        textCell.setTextAndValue("Blocked", "Yes", divider);
                    } else if (position == languageRow) {
                        textCell.setTextAndValue("Language", "Loading...", divider);
                        LanguageDetector.detectLanguage(
                                getMessageHelper().getMessagePlainText(messageObject),
                                lang -> textCell.setTextAndValue("Language", lang, divider),
                                e -> textCell.setTextAndValue("Language", e.getLocalizedMessage(), divider));
                    } else if (position == linkOrEmojiOnlyRow) {
                        textCell.setTextAndValue("Link or emoji only", "Yes", divider);
                    } else if (position == stickerSetRow) {
                        StringBuilder builder = new StringBuilder();
                        TLRPC.User user = getMessagesController().getUser(stickerSetOwner);
                        if (user != null) {
                            appendUserOrChat(user, builder);
                        } else {
                            getUserHelper().searchUser(stickerSetOwner, user1 -> {
                                StringBuilder builder1 = new StringBuilder();
                                if (user1 != null) {
                                    appendUserOrChat(user1, builder1);
                                } else {
                                    builder1.append(stickerSetOwner);
                                }
                                textCell.setTextAndValueWithEmoji("Sticker Pack creator", builder1, divider);
                            });
                            builder.append("Loading...");
                            builder.append("\n");
                            builder.append(stickerSetOwner);
                        }
                        textCell.setTextAndValueWithEmoji("Sticker Pack creator", builder, divider);
                    } else if (position == emojiSetRow) {
                        textCell.setTextAndValue("Emoji Pack creators", TextUtils.join(", ", emojiSetOwners), divider);
                    }
                    break;
                }
                case Integer.MAX_VALUE: {
                    TextDetailSimpleCell textCell = (TextDetailSimpleCell) holder.itemView;
                    if (position == messageRow) {
                        textCell.setTextAndValue("Message", AnimatedEmojiSpan.cloneSpans(messageObject.messageText), messageObject.getEmojiOnlyCount(), divider);
                    } else if (position == captionRow) {
                        textCell.setTextAndValue("Caption", AnimatedEmojiSpan.cloneSpans(messageObject.caption), messageObject.getEmojiOnlyCount(), divider);
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return super.isEnabled(holder) || holder.getItemViewType() == Integer.MAX_VALUE;
        }

        @Override
        public View createCustomView(int viewType) {
            if (viewType == Integer.MAX_VALUE) {
                return new TextDetailSimpleCell(mContext, resourcesProvider);
            } else {
                return super.createCustomView(viewType);
            }
        }

        @Override
        public int getItemViewType(int position) {
            if (position == emptyRow || position == endRow) {
                return TYPE_SHADOW;
            } else if (position == exportRow) {
                return TYPE_SETTINGS;
            } else if (position == messageRow || position == captionRow) {
                return Integer.MAX_VALUE;
            } else {
                return TYPE_DETAIL_SETTINGS;
            }
        }

        private String formatTime(int timestamp) {
            if (timestamp == 0x7ffffffe) {
                return "When online";
            } else {
                return timestamp + "\n" + LocaleController.formatString(R.string.formatDateAtTime, LocaleController.getInstance().getFormatterYear().format(new Date(timestamp * 1000L)), LocaleController.getInstance().getFormatterDayWithSeconds().format(new Date(timestamp * 1000L)));
            }
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

    @SuppressLint("ViewConstructor")
    public static class TextDetailSimpleCell extends FrameLayout {

        private final TextView textView;
        private final TextViewEffects valueTextView;
        private boolean needDivider;

        public TextDetailSimpleCell(Context context, Theme.ResourcesProvider resourcesProvider) {
            super(context);
            setClipChildren(false);

            textView = new TextView(context);
            textView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            textView.setLines(1);
            textView.setMaxLines(1);
            textView.setSingleLine(true);
            textView.setEllipsize(TextUtils.TruncateAt.END);
            textView.setGravity((LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.CENTER_VERTICAL);
            addView(textView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 21, 10, 21, 0));

            valueTextView = new TextViewEffects(context, resourcesProvider);
            valueTextView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2, resourcesProvider));
            valueTextView.setTextSize(13);
            valueTextView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            valueTextView.setSingleLine(false);
            valueTextView.setPadding(0, 0, 0, AndroidUtilities.dp(12));
            valueTextView.setLinkTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteLinkText, resourcesProvider));
            addView(valueTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, (LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT) | Gravity.TOP, 21, 35, 21, 0));
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        }

        public TextViewEffects getValueTextView() {
            return valueTextView;
        }

        public void setTextAndValue(String text, CharSequence value, int emojiOnlyCount, boolean divider) {
            textView.setText(text);
            valueTextView.setText(value, emojiOnlyCount);
            needDivider = divider;
            setWillNotDraw(!divider);
        }

        @Override
        public void invalidate() {
            super.invalidate();
            textView.invalidate();
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            if (needDivider && Theme.dividerPaint != null) {
                canvas.drawLine(LocaleController.isRTL ? 0 : AndroidUtilities.dp(20), getMeasuredHeight() - 1, getMeasuredWidth() - (LocaleController.isRTL ? AndroidUtilities.dp(20) : 0), getMeasuredHeight() - 1, Theme.dividerPaint);
            }
        }
    }

    @Override
    public ArrayList<ThemeDescription> getThemeDescriptions() {
        ArrayList<ThemeDescription> themeDescriptions = super.getThemeDescriptions();

        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{TextDetailSimpleCell.class}, null, null, null, Theme.key_windowBackgroundWhite));

        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSimpleCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText));
        themeDescriptions.add(new ThemeDescription(listView, 0, new Class[]{TextDetailSimpleCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2));
        themeDescriptions.add(new ThemeDescription(listView, ThemeDescription.FLAG_LINKCOLOR, new Class[]{TextDetailSimpleCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteLinkText));

        return themeDescriptions;
    }
}
