package tw.nekomimi.nekogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.ChatActivity;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.ProfileActivity;
import org.telegram.ui.TopicsFragment;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicReference;

public class BackButtonMenuRecent {

    private static final int MAX_RECENT_DIALOGS = 25;

    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekorecentdialogs", Context.MODE_PRIVATE);
    private static final SparseArray<LinkedList<Long>> recentDialogs = new SparseArray<>();

    public static void show(int currentAccount, DialogsActivity fragment, View backButton, DialogsActivity.DialogsActivityDelegate delegate) {
        if (fragment == null) {
            return;
        }
        final Context context = fragment.getParentActivity();
        final View fragmentView = fragment.getFragmentView();
        if (context == null || fragmentView == null) {
            return;
        }
        LinkedList<Long> dialogs = getRecentDialogs(fragment.getCurrentAccount());
        if (dialogs.isEmpty()) {
            return;
        }

        ActionBarPopupWindow.ActionBarPopupWindowLayout layout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context) {
            final Path path = new Path();

            @Override
            protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
                canvas.save();
                path.rewind();
                AndroidUtilities.rectTmp.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
                path.addRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(6), AndroidUtilities.dp(6), Path.Direction.CW);
                canvas.clipPath(path);
                boolean draw = super.drawChild(canvas, child, drawingTime);
                canvas.restore();
                return draw;
            }
        };
        Rect backgroundPaddings = new Rect();
        Drawable shadowDrawable = fragment.getParentActivity().getResources().getDrawable(R.drawable.popup_fixed_alert2).mutate();
        shadowDrawable.getPadding(backgroundPaddings);
        layout.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        AtomicReference<ActionBarPopupWindow> scrimPopupWindowRef = new AtomicReference<>();

        FrameLayout headerView = new FrameLayout(context);
        headerView.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        TextView titleTextView = new TextView(context);
        titleTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlue));
        titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleTextView.setText(LocaleController.getString(R.string.RecentChats));
        titleTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
        headerView.addView(titleTextView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, 24, Gravity.LEFT));

        ImageView clearImageView = new ImageView(context);
        clearImageView.setScaleType(ImageView.ScaleType.CENTER);
        clearImageView.setColorFilter(Theme.getColor(Theme.key_actionBarDefaultSubmenuItemIcon));
        clearImageView.setImageResource(R.drawable.msg_close);
        clearImageView.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector)));
        clearImageView.setOnClickListener(e2 -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(LocaleController.getString(R.string.ClearRecentChats));
            builder.setMessage(LocaleController.getString(R.string.ClearRecentChatAlert));
            builder.setPositiveButton(LocaleController.getString(R.string.ClearButton).toUpperCase(), (dialogInterface, i) -> {
                if (scrimPopupWindowRef.get() != null) {
                    scrimPopupWindowRef.getAndSet(null).dismiss();
                }
                clearRecentDialogs(currentAccount);
            });
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            fragment.showDialog(builder.create());
        });
        headerView.addView(clearImageView, LayoutHelper.createFrame(24, 24, Gravity.RIGHT | Gravity.CENTER_VERTICAL));

        headerView.setPadding(AndroidUtilities.dp(9), AndroidUtilities.dp(8), AndroidUtilities.dp(8), AndroidUtilities.dp(8));
        layout.addView(headerView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 4, 4));

        for (Long dialogId : dialogs) {
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
            FrameLayout cell = new FrameLayout(context);
            cell.setMinimumWidth(AndroidUtilities.dp(200));

            BackupImageView imageView = new BackupImageView(context);
            imageView.setRoundRadius(chat != null && chat.forum ? AndroidUtilities.dp(8) : AndroidUtilities.dp(16));
            cell.addView(imageView, LayoutHelper.createFrameRelatively(32, 32, Gravity.START | Gravity.CENTER_VERTICAL, 13, 0, 0, 0));

            TextView titleView = new TextView(context);
            titleView.setLines(1);
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            titleView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
            titleView.setEllipsize(TextUtils.TruncateAt.END);
            cell.addView(titleView, LayoutHelper.createFrameRelatively(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.START | Gravity.CENTER_VERTICAL, 59, 0, 12, 0));

            AvatarDrawable avatarDrawable = new AvatarDrawable();
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
                if (scrimPopupWindowRef.get() != null) {
                    scrimPopupWindowRef.getAndSet(null).dismiss();
                }
                if (delegate != null) {
                    ArrayList<MessagesStorage.TopicKey> keys = new ArrayList<>();
                    keys.add(MessagesStorage.TopicKey.of(dialogId, 0));
                    delegate.didSelectDialogs(fragment, keys, null, false, true, 0, 0, null);
                    return;
                }
                Bundle bundle = new Bundle();
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
                if (scrimPopupWindowRef.get() != null) {
                    scrimPopupWindowRef.getAndSet(null).dismiss();
                }
                Bundle bundle = new Bundle();
                if (dialogId < 0) {
                    bundle.putLong("chat_id", -dialogId);
                } else {
                    bundle.putLong("user_id", dialogId);
                }
                fragment.presentFragment(new ProfileActivity(bundle));
                return true;
            });
            layout.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        }

        ActionBarPopupWindow scrimPopupWindow = new ActionBarPopupWindow(layout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        scrimPopupWindowRef.set(scrimPopupWindow);
        scrimPopupWindow.setPauseNotifications(true);
        scrimPopupWindow.setDismissAnimationDuration(220);
        scrimPopupWindow.setOutsideTouchable(true);
        scrimPopupWindow.setClippingEnabled(true);
        scrimPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        scrimPopupWindow.setFocusable(true);
        layout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        scrimPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        scrimPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        scrimPopupWindow.getContentView().setFocusableInTouchMode(true);
        layout.setFitItems(true);

        int popupX = AndroidUtilities.dp(8) - backgroundPaddings.left;
        if (AndroidUtilities.isTablet()) {
            int[] location = new int[2];
            fragmentView.getLocationInWindow(location);
            popupX += location[0];
        }
        int popupY = backButton.getBottom() - backgroundPaddings.top - AndroidUtilities.dp(8);
        scrimPopupWindow.showAtLocation(fragmentView, Gravity.LEFT | Gravity.TOP, popupX, popupY);
        scrimPopupWindow.dimBehind();
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
