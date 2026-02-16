package tw.nekomimi.nekogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.R;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.SerializedData;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.MainTabsActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.TopicsFragment;

import java.util.ArrayList;
import java.util.LinkedList;

public class BackButtonMenuRecent {

    private static final int MAX_RECENT_DIALOGS = 25;

    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekorecentdialogs", Context.MODE_PRIVATE);
    private static final SparseArray<LinkedList<Long>> recentDialogs = new SparseArray<>();

    public static void show(int currentAccount, BaseFragment fragment, View button, DialogsActivity.DialogsActivityDelegate delegate) {
        var context = fragment.getParentActivity();
        if (context == null) {
            return;
        }
        var dialogs = getRecentDialogs(fragment.getCurrentAccount());
        if (dialogs.isEmpty()) {
            return;
        }
        var options = ItemOptions.makeOptions(fragment, button);
        options.add(R.drawable.menu_clear_recent, LocaleController.getString(R.string.ClearButton), () -> {
            var builder = new AlertDialog.Builder(context);
            builder.setTitle(LocaleController.getString(R.string.ClearRecentChats));
            builder.setMessage(LocaleController.getString(R.string.ClearRecentChatAlert));
            builder.setPositiveButton(LocaleController.getString(R.string.ClearButton).toUpperCase(), (dialogInterface, i) -> clearRecentDialogs(currentAccount));
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            fragment.showDialog(builder.create());
        });
        options.addGap();
        for (var dialogId : dialogs) {
            final TLRPC.Chat chat;
            final TLRPC.User user;
            if (dialogId < 0) {
                chat = MessagesController.getInstance(currentAccount).getChat(-dialogId);
                user = null;
            } else {
                chat = null;
                user = MessagesController.getInstance(currentAccount).getUser(dialogId);
            }
            if (chat == null && user == null) {
                continue;
            }
            var cell = new FrameLayout(context);

            var imageView = new BackupImageView(context);
            imageView.setRoundRadius(chat != null && chat.forum ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16));
            cell.addView(imageView, LayoutHelper.createFrameRelatively(32, 32, Gravity.START | Gravity.CENTER_VERTICAL, 13, 0, 1, 0));

            var titleView = new TextView(context);
            titleView.setLines(1);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(titleView, LayoutHelper.createFrameRelatively(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, 59, 0, 12, 0));

            var avatarDrawable = new AvatarDrawable();
            avatarDrawable.setScaleSize(.8f);
            Drawable thumb = avatarDrawable;

            if (chat != null) {
                avatarDrawable.setInfo(chat);
                if (chat.photo != null && chat.photo.strippedBitmap != null) {
                    thumb = chat.photo.strippedBitmap;
                }
                imageView.setImage(ImageLocation.getForChat(chat, ImageLocation.TYPE_SMALL), "50_50", thumb, chat);
                titleView.setText(chat.title);
            } else {
                String name;
                if (user.photo != null && user.photo.strippedBitmap != null) {
                    thumb = user.photo.strippedBitmap;
                }
                if (UserObject.isReplyUser(user)) {
                    name = LocaleController.getString(R.string.RepliesTitle);
                    avatarDrawable.setAvatarType(AvatarDrawable.AVATAR_TYPE_REPLIES);
                    imageView.setImageDrawable(avatarDrawable);
                } else if (UserObject.isDeleted(user)) {
                    name = LocaleController.getString(R.string.HiddenName);
                    avatarDrawable.setInfo(user);
                    imageView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", avatarDrawable, user);
                } else {
                    name = UserObject.getUserName(user);
                    avatarDrawable.setInfo(user);
                    imageView.setImage(ImageLocation.getForUser(user, ImageLocation.TYPE_SMALL), "50_50", thumb, user);
                }
                titleView.setText(name);
            }

            cell.setBackground(Theme.getSelectorDrawable(Theme.getColor(Theme.key_listSelector), false));
            cell.setOnClickListener(e2 -> {
                options.dismiss();
                if (fragment instanceof DialogsActivity dialogsActivity && delegate != null) {
                    ArrayList<MessagesStorage.TopicKey> keys = new ArrayList<>();
                    keys.add(MessagesStorage.TopicKey.of(dialogId, 0));
                    delegate.didSelectDialogs(dialogsActivity, keys, null, false, true, 0, 0, null);
                    return;
                }
                var bundle = new Bundle();
                if (dialogId < 0) {
                    bundle.putLong("chat_id", -dialogId);
                    if (MessagesController.getInstance(currentAccount).isForum(dialogId)) {
                        fragment.presentFragment(new TopicsFragment(bundle));
                    } else {
                        fragment.presentFragment(new ChatActivity(bundle));
                    }
                } else {
                    bundle.putLong("user_id", dialogId);
                    fragment.presentFragment(new ChatActivity(bundle));
                }
            });
            cell.setOnLongClickListener(e2 -> {
                options.dismiss();
                var bundle = new Bundle();
                if (dialogId < 0) {
                    bundle.putLong("chat_id", -dialogId);
                } else {
                    bundle.putLong("user_id", dialogId);
                }
                fragment.presentFragment(new ProfileActivity(bundle));
                return true;
            });
            options.addView(cell, LayoutHelper.createLinear(230, 48));
        }

        if (fragment instanceof MainTabsActivity) {
            options.setBlur(true);
            options.translate(0, -AndroidUtilities.dp(4));
            var bg = Theme.createRoundRectDrawable(AndroidUtilities.dp(28), Theme.getColor(Theme.key_windowBackgroundWhite));
            bg.getPaint().setShadowLayer(AndroidUtilities.dp(6), 0, AndroidUtilities.dp(1), Theme.multAlpha(0xFF000000, 0.15f));
            options.setScrimViewBackground(bg);
        } else {
            options.setScrimViewBackground(Theme.createCircleDrawable(AndroidUtilities.dp(40), Theme.getColor(Theme.key_windowBackgroundWhite)));
        }
        options.show();
    }

    private static LinkedList<Long> getRecentDialogs(int currentAccount) {
        LinkedList<Long> recentDialog = recentDialogs.get(currentAccount);
        if (recentDialog == null) {
            recentDialog = new LinkedList<>();
            String list = preferences.getString("recents_" + currentAccount, null);
            if (!TextUtils.isEmpty(list)) {
                byte[] bytes = Base64.decode(list, Base64.NO_WRAP | Base64.NO_PADDING);
                SerializedData data = new SerializedData(bytes);
                int count = data.readInt32(false);
                for (int a = 0; a < count; a++) {
                    recentDialog.add(data.readInt64(false));
                }
                data.cleanup();
            }
            recentDialogs.put(currentAccount, recentDialog);
        }
        return recentDialog;
    }

    public static void addToRecentDialogs(int currentAccount, long dialogId) {
        LinkedList<Long> recentDialog = getRecentDialogs(currentAccount);
        for (int i = 0; i < recentDialog.size(); i++) {
            if (recentDialog.get(i) == dialogId) {
                recentDialog.remove(i);
                break;
            }
        }

        if (recentDialog.size() > MAX_RECENT_DIALOGS) {
            recentDialog.removeLast();
        }
        recentDialog.addFirst(dialogId);
        LinkedList<Long> finalRecentDialog = new LinkedList<>(recentDialog);
        Utilities.globalQueue.postRunnable(() -> saveRecentDialogs(currentAccount, finalRecentDialog));
    }

    private static void saveRecentDialogs(int currentAccount, LinkedList<Long> recentDialog) {
        SerializedData serializedData = new SerializedData();
        int count = recentDialog.size();
        serializedData.writeInt32(count);
        for (Long dialog : recentDialog) {
            serializedData.writeInt64(dialog);
        }
        preferences.edit().putString("recents_" + currentAccount, Base64.encodeToString(serializedData.toByteArray(), Base64.NO_WRAP | Base64.NO_PADDING)).apply();
        serializedData.cleanup();
    }

    public static void clearRecentDialogs(int currentAccount) {
        getRecentDialogs(currentAccount).clear();
        preferences.edit().putString("recents_" + currentAccount, "").apply();
    }
}
