package tw.nekomimi.nekogram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ActionBar.ThemeDescription;
import org.telegram.ui.Cells.EmptyCell;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.NotificationsCheckCell;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.ProfileActivity;

import java.io.File;
import java.util.Date;

public class MessageDetailsActivity extends BaseFragment {

    private RecyclerListView listView;
    private ListAdapter listAdapter;

    private MessageObject messageObject;
    private TLRPC.Chat fromChat;
    private TLRPC.User fromUser;
    private String filePath;
    private String fileName;

    private int rowCount;

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
    private int fileNameRow;
    private int filePathRow;
    private int fileSizeRow;
    private int dcRow;
    private int emptyRow;
    private int exportRow;
    private int endRow;

    public MessageDetailsActivity(MessageObject messageObject) {
        this.messageObject = messageObject;
        if (messageObject.messageOwner.to_id != null && messageObject.messageOwner.to_id.channel_id != 0) {
            fromChat = getMessagesController().getChat(messageObject.messageOwner.to_id.channel_id);
        } else if (messageObject.messageOwner.to_id != null && messageObject.messageOwner.to_id.chat_id != 0) {
            fromChat = getMessagesController().getChat(messageObject.messageOwner.to_id.chat_id);
        }
        if (messageObject.messageOwner.from_id != 0) {
            fromUser = getMessagesController().getUser(messageObject.messageOwner.from_id);
        }
        filePath = messageObject.messageOwner.attachPath;
        if (!TextUtils.isEmpty(filePath)) {
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = FileLoader.getPathToMessage(messageObject.messageOwner).toString();
            File temp = new File(filePath);
            if (!temp.exists()) {
                filePath = null;
            }
        }
        if (TextUtils.isEmpty(filePath)) {
            filePath = FileLoader.getPathToAttach(messageObject.getDocument(), true).toString();
            File temp = new File(filePath);
            if (!temp.isFile()) {
                filePath = null;
            }
        }
        if (messageObject.messageOwner.media != null && messageObject.messageOwner.media.document != null) {
            if (TextUtils.isEmpty(messageObject.messageOwner.media.document.file_name)) {
                for (int a = 0; a < messageObject.messageOwner.media.document.attributes.size(); a++) {
                    if (messageObject.messageOwner.media.document.attributes.get(a) instanceof TLRPC.TL_documentAttributeFilename) {
                        fileName = messageObject.messageOwner.media.document.attributes.get(a).file_name;
                    }
                }
            } else {
                fileName = messageObject.messageOwner.media.document.file_name;
            }
        }

    }

    @Override
    public boolean onFragmentCreate() {
        super.onFragmentCreate();

        updateRows();

        return true;
    }

    @SuppressLint({"NewApi", "RtlHardcoded"})
    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString("MessageDetails", R.string.MessageDetails));

        if (AndroidUtilities.isTablet()) {
            actionBar.setOccupyStatusBar(false);
        }
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        listAdapter = new ListAdapter(context);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        listView = new RecyclerListView(context);
        listView.setVerticalScrollBarEnabled(false);
        listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean supportsPredictiveItemAnimations() {
                return false;
            }
        });
        listView.setGlowColor(Theme.getColor(Theme.key_avatar_backgroundActionBarBlue));
        listView.setAdapter(listAdapter);
        listView.setItemAnimator(null);
        listView.setLayoutAnimation(null);
        frameLayout.addView(listView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT));
        listView.setOnItemClickListener((view, position, x, y) -> {
            if (position == exportRow) {
                Gson gson = new Gson();
                try {
                    AndroidUtilities.addToClipboard(gson.toJson(messageObject.messageOwner));
                    Toast.makeText(getParentActivity(), LocaleController.getString("TextCopied", R.string.TextCopied), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            } else if (position != endRow && position != emptyRow) {
                TextDetailSettingsCell textCell = (TextDetailSettingsCell) view;
                try {
                    AndroidUtilities.addToClipboard(textCell.getValueTextView().getText());
                    Toast.makeText(getParentActivity(), LocaleController.getString("TextCopied", R.string.TextCopied), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

        });
        listView.setOnItemLongClickListener((view, position) -> {
            if (position == filePathRow) {
                AndroidUtilities.runOnUIThread(() -> {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("application/octet-stream");
                    if (Build.VERSION.SDK_INT >= 24) {
                        try {
                            intent.putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(getParentActivity(), BuildConfig.APPLICATION_ID + ".provider", new File(filePath)));
                            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        } catch (Exception ignore) {
                            intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
                        }
                    } else {
                        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(filePath)));
                    }
                    startActivityForResult(Intent.createChooser(intent, LocaleController.getString("ShareFile", R.string.ShareFile)), 500);
                });
            } else if (position == channelRow || position == groupRow) {
                if (fromChat != null) {
                    Bundle args = new Bundle();
                    args.putInt("chat_id", fromChat.id);
                    ProfileActivity fragment = new ProfileActivity(args);
                    presentFragment(fragment);
                }
            } else if (position == fromRow) {
                if (fromUser != null) {
                    Bundle args = new Bundle();
                    args.putInt("user_id", fromUser.id);
                    ProfileActivity fragment = new ProfileActivity(args);
                    presentFragment(fragment);
                }
            } else {
                return false;
            }
            return true;
        });
        return fragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    private void updateRows() {
        rowCount = 0;
        idRow = rowCount++;
        messageRow = TextUtils.isEmpty(messageObject.messageText) ? -1 : rowCount++;
        captionRow = TextUtils.isEmpty(messageObject.caption) ? -1 : rowCount++;
        groupRow = fromChat != null && !fromChat.broadcast ? rowCount++ : -1;
        channelRow = fromChat != null && fromChat.broadcast ? rowCount++ : -1;
        fromRow = fromUser != null || messageObject.messageOwner.post_author != null ? rowCount++ : -1;
        botRow = fromUser != null && fromUser.bot ? rowCount++ : -1;
        dateRow = messageObject.messageOwner.date != 0 ? rowCount++ : -1;
        editedRow = messageObject.messageOwner.edit_date != 0 ? rowCount++ : -1;
        forwardRow = messageObject.isForwarded() ? rowCount++ : -1;
        fileNameRow = TextUtils.isEmpty(fileName) ? -1 : rowCount++;
        filePathRow = TextUtils.isEmpty(filePath) ? -1 : rowCount++;
        fileSizeRow = messageObject.getSize() != 0 ? rowCount++ : -1;
        if (messageObject.messageOwner.media != null && (
                (messageObject.messageOwner.media.photo != null && messageObject.messageOwner.media.photo.dc_id > 0) ||
                        (messageObject.messageOwner.media.document != null && messageObject.messageOwner.media.document.dc_id > 0)
        )) {
            dcRow = rowCount++;
        } else {
            dcRow = -1;
        }
        emptyRow = rowCount++;
        exportRow = rowCount++;
        endRow = rowCount++;
        if (listAdapter != null) {
            listAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public ThemeDescription[] getThemeDescriptions() {
        return new ThemeDescription[]{
                new ThemeDescription(listView, ThemeDescription.FLAG_CELLBACKGROUNDCOLOR, new Class[]{EmptyCell.class, TextSettingsCell.class, TextCheckCell.class, HeaderCell.class, TextDetailSettingsCell.class, NotificationsCheckCell.class}, null, null, null, Theme.key_windowBackgroundWhite),
                new ThemeDescription(fragmentView, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_windowBackgroundGray),

                new ThemeDescription(actionBar, ThemeDescription.FLAG_BACKGROUND, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(listView, ThemeDescription.FLAG_LISTGLOWCOLOR, null, null, null, null, Theme.key_avatar_backgroundActionBarBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_ITEMSCOLOR, null, null, null, null, Theme.key_avatar_actionBarIconBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_TITLECOLOR, null, null, null, null, Theme.key_actionBarDefaultTitle),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SELECTORCOLOR, null, null, null, null, Theme.key_avatar_actionBarSelectorBlue),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUBACKGROUND, null, null, null, null, Theme.key_actionBarDefaultSubmenuBackground),
                new ThemeDescription(actionBar, ThemeDescription.FLAG_AB_SUBMENUITEM, null, null, null, null, Theme.key_actionBarDefaultSubmenuItem),

                new ThemeDescription(listView, ThemeDescription.FLAG_SELECTOR, null, null, null, null, Theme.key_listSelector),

                new ThemeDescription(listView, 0, new Class[]{View.class}, Theme.dividerPaint, null, null, Theme.key_divider),

                new ThemeDescription(listView, ThemeDescription.FLAG_BACKGROUNDFILTER, new Class[]{ShadowSectionCell.class}, null, null, null, Theme.key_windowBackgroundGrayShadow),

                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteValueText),

                new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"textView"}, null, null, null, Theme.key_windowBackgroundWhiteBlackText),
                new ThemeDescription(listView, 0, new Class[]{TextDetailSettingsCell.class}, new String[]{"valueTextView"}, null, null, null, Theme.key_windowBackgroundWhiteGrayText2),
        };
    }

    private class ListAdapter extends RecyclerListView.SelectionAdapter {

        private Context mContext;

        public ListAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getItemCount() {
            return rowCount;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            switch (holder.getItemViewType()) {
                case 1: {
                    break;
                }
                case 2: {
                    TextDetailSettingsCell textCell = (TextDetailSettingsCell) holder.itemView;
                    textCell.setMultilineDetail(true);
                    boolean divider = position + 1 != emptyRow;
                    if (position == idRow) {
                        textCell.setTextAndValue("ID", String.valueOf(messageObject.messageOwner.id), divider);
                    } else if (position == messageRow) {
                        textCell.setTextWithEmojiAnd21Value("Message", messageObject.messageText, divider);
                    } else if (position == captionRow) {
                        textCell.setTextWithEmojiAnd21Value("Caption", messageObject.caption, divider);
                    } else if (position == channelRow || position == groupRow) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(fromChat.title);
                        builder.append("\n");
                        if (!TextUtils.isEmpty(fromChat.username)) {
                            builder.append("@");
                            builder.append(fromChat.username);
                            builder.append("\n");
                        }
                        builder.append(fromChat.id);
                        textCell.setTextWithEmojiAnd21Value(position == channelRow ? "Channel" : "Group", builder.toString(), divider);
                    } else if (position == fromRow) {
                        StringBuilder builder = new StringBuilder();
                        if (fromUser != null) {
                            builder.append(ContactsController.formatName(fromUser.first_name, fromUser.last_name));
                            builder.append("\n");
                            if (!TextUtils.isEmpty(fromUser.username)) {
                                builder.append("@");
                                builder.append(fromUser.username);
                                builder.append("\n");
                            }
                            builder.append(fromUser.id);
                        } else {
                            builder.append(messageObject.messageOwner.post_author);
                        }
                        textCell.setTextWithEmojiAnd21Value("From", builder.toString(), divider);
                    } else if (position == botRow) {
                        textCell.setTextWithEmojiAnd21Value("Bot", "Yes", divider);
                    } else if (position == dateRow) {
                        long date = (long) messageObject.messageOwner.date * 1000;
                        textCell.setTextWithEmojiAnd21Value("Date", LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().formatterYear.format(new Date(date)), LocaleController.getInstance().formatterDay.format(new Date(date))), divider);
                    } else if (position == editedRow) {
                        long date = (long) messageObject.messageOwner.date * 1000;
                        textCell.setTextWithEmojiAnd21Value("Edited", LocaleController.formatString("formatDateAtTime", R.string.formatDateAtTime, LocaleController.getInstance().formatterYear.format(new Date(date)), LocaleController.getInstance().formatterDay.format(new Date(date))), divider);
                    } else if (position == forwardRow) {
                        StringBuilder builder = new StringBuilder();
                        if (messageObject.messageOwner.fwd_from.channel_id != 0) {
                            TLRPC.Chat chat = getMessagesController().getChat(messageObject.messageOwner.fwd_from.channel_id);
                            builder.append(chat.title);
                            builder.append("\n");
                            if (!TextUtils.isEmpty(chat.username)) {
                                builder.append("@");
                                builder.append(chat.username);
                                builder.append("\n");
                            }
                            builder.append(chat.id);
                        } else if (messageObject.messageOwner.fwd_from.from_id != 0) {
                            TLRPC.User user = getMessagesController().getUser(messageObject.messageOwner.fwd_from.from_id);
                            builder.append(ContactsController.formatName(user.first_name, user.last_name));
                            builder.append("\n");
                            if (!TextUtils.isEmpty(user.username)) {
                                builder.append("@");
                                builder.append(user.username);
                                builder.append("\n");
                            }
                            builder.append(user.id);
                        } else if (!TextUtils.isEmpty(messageObject.messageOwner.fwd_from.from_name)) {
                            builder.append(messageObject.messageOwner.fwd_from.from_name);
                        }
                        textCell.setTextWithEmojiAnd21Value("Forward from", builder.toString(), divider);
                    } else if (position == fileNameRow) {
                        textCell.setTextWithEmojiAnd21Value("File name", fileName, divider);
                    } else if (position == filePathRow) {
                        textCell.setTextWithEmojiAnd21Value("File path", filePath, divider);
                    } else if (position == fileSizeRow) {
                        textCell.setTextWithEmojiAnd21Value("File size", AndroidUtilities.formatFileSize(messageObject.getSize()), divider);
                    } else if (position == dcRow) {
                        if (messageObject.messageOwner.media.photo != null && messageObject.messageOwner.media.photo.dc_id > 0) {
                            textCell.setTextWithEmojiAnd21Value("DC", String.valueOf(messageObject.messageOwner.media.photo.dc_id), divider);
                        } else if (messageObject.messageOwner.media.document != null && messageObject.messageOwner.media.document.dc_id > 0) {
                            textCell.setTextWithEmojiAnd21Value("DC", String.valueOf(messageObject.messageOwner.media.document.dc_id), divider);
                        }
                    }
                    break;
                }
                case 3: {
                    TextSettingsCell textCell = (TextSettingsCell) holder.itemView;
                    if (position == exportRow) {
                        textCell.setText(LocaleController.getString("ExportAsJson", R.string.ExportAsJson), false);
                        textCell.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
                    }
                    break;
                }
            }
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            int position = holder.getAdapterPosition();
            return position != endRow && position != emptyRow;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = null;
            switch (viewType) {
                case 1:
                    view = new ShadowSectionCell(mContext);
                    break;
                case 2:
                    view = new TextDetailSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
                case 3:
                    view = new TextSettingsCell(mContext);
                    view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
                    break;
            }
            //noinspection ConstantConditions
            view.setLayoutParams(new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT));
            return new RecyclerListView.Holder(view);
        }

        @Override
        public int getItemViewType(int position) {
            if (position == endRow || position == emptyRow) {
                return 1;
            } else if (position == exportRow) {
                return 3;
            } else {
                return 2;
            }
        }
    }
}
