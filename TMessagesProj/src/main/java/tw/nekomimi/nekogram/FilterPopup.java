package tw.nekomimi.nekogram;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;

import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

public class FilterPopup {
    private static volatile FilterPopup[] Instance = new FilterPopup[UserConfig.MAX_ACCOUNT_COUNT];
    public ArrayList<TLRPC.Dialog> dialogsUsers = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsGroups = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsChannels = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsBots = new ArrayList<>();
    public ArrayList<TLRPC.Dialog> dialogsAdmin = new ArrayList<>();
    private ActionBarPopupWindow scrimPopupWindow;
    private int currentAccount;

    public FilterPopup(int num) {
        currentAccount = num;
    }

    public static FilterPopup getInstance(int num) {
        FilterPopup localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (MessagesController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new FilterPopup(num);
                }
            }
        }
        return localInstance;
    }

    public void cleanup() {
        dialogsUsers.clear();
        dialogsGroups.clear();
        dialogsChannels.clear();
        dialogsBots.clear();
        dialogsAdmin.clear();
    }

    public void remove(TLRPC.Dialog dialog) {
        dialogsUsers.remove(dialog);
        dialogsGroups.remove(dialog);
        dialogsChannels.remove(dialog);
        dialogsBots.remove(dialog);
        dialogsAdmin.remove(dialog);
    }

    public void sortDialogs(TLRPC.Dialog dialog, int high_id, int lower_id) {
        if (lower_id != 0 && high_id != 1) {
            if (DialogObject.isChannel(dialog)) {
                TLRPC.Chat chat = MessagesController.getInstance(currentAccount).getChat(-lower_id);
                if (chat != null) {
                    if (chat.megagroup) {
                        dialogsGroups.add(dialog);
                    } else {
                        dialogsChannels.add(dialog);
                    }
                }
                if (chat != null && (chat.creator || ChatObject.hasAdminRights(chat)))
                    dialogsAdmin.add(dialog);
            } else if (lower_id < 0) {
                dialogsGroups.add(dialog);
            } else {
                TLRPC.User user = MessagesController.getInstance(currentAccount).getUser((int) dialog.id);
                if (user != null) {
                    if (user.bot)
                        dialogsBots.add(dialog);
                    else
                        dialogsUsers.add(dialog);
                }
            }
        } else {
            TLRPC.EncryptedChat encryptedChat = MessagesController.getInstance(currentAccount).getEncryptedChat(high_id);
            if (encryptedChat != null)
                dialogsUsers.add(dialog);
        }
    }

    public boolean hasHiddenArchive(int type) {
        if (!SharedConfig.archiveHidden)
            return false;
        ArrayList<TLRPC.Dialog> dialogs = getDialogs(type, 0);
        if (dialogs == null)
            return MessagesController.getInstance(currentAccount).hasHiddenArchive();
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<TLRPC.Dialog> getDialogs(int type, int folderId) {
        MessagesController messagesController = AccountInstance.getInstance(currentAccount).getMessagesController();
        ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(messagesController.getDialogs(folderId));
        ArrayList<TLRPC.Dialog> folders = new ArrayList<>();
        ArrayList<ArrayList<TLRPC.Dialog>> folderDialogs = new ArrayList<>();

        for (TLRPC.Dialog dialog : allDialogs) {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                folders.add(dialog);
                TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder)dialog;
                folderDialogs.add(new ArrayList<>(messagesController.getDialogs(dialogFolder.folder.id)));
            }
        }

        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        switch (type) {
            case DialogType.Users:
                for (int i = 0; i < folders.size(); i++) {
                    folderDialogs.get(i).retainAll(dialogsUsers);
                    if (!folderDialogs.get(i).isEmpty())
                        dialogs.add(folders.get(i));
                }
                allDialogs.retainAll(dialogsUsers);
                break;
            case DialogType.Groups:
                for (int i = 0; i < folders.size(); i++) {
                    folderDialogs.get(i).retainAll(dialogsGroups);
                    if (!folderDialogs.get(i).isEmpty())
                        dialogs.add(folders.get(i));
                }
                allDialogs.retainAll(dialogsGroups);
                break;
            case DialogType.Channels:
                for (int i = 0; i < folders.size(); i++) {
                    folderDialogs.get(i).retainAll(dialogsChannels);
                    if (!folderDialogs.get(i).isEmpty())
                        dialogs.add(folders.get(i));
                }
                allDialogs.retainAll(dialogsChannels);
                break;
            case DialogType.Bots:
                for (int i = 0; i < folders.size(); i++) {
                    folderDialogs.get(i).retainAll(dialogsBots);
                    if (!folderDialogs.get(i).isEmpty())
                        dialogs.add(folders.get(i));
                }
                allDialogs.retainAll(dialogsBots);
                break;
            case DialogType.Admin:
                for (int i = 0; i < folders.size(); i++) {
                    folderDialogs.get(i).retainAll(dialogsAdmin);
                    if (!folderDialogs.get(i).isEmpty())
                        dialogs.add(folders.get(i));
                }
                allDialogs.retainAll(dialogsAdmin);
                break;
            default:
                return null;
        }
        if (folderId != 0 && allDialogs.isEmpty()) {
            allDialogs = new ArrayList<>(messagesController.getDialogs(folderId));
        }
        dialogs.addAll(allDialogs);
        return dialogs;
    }

    public void createMenu(DialogsActivity dialogsActivity, ActionBar actionBar, Activity parentActivity, RecyclerView listView, View fragmentView, int x, int y, int folderId) {
        if (actionBar.isActionModeShowed()) {
            return;
        }
        ArrayList<CharSequence> items = new ArrayList<>();
        final ArrayList<Integer> options = new ArrayList<>();

        MessagesController messagesController = AccountInstance.getInstance(currentAccount).getMessagesController();
        ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(messagesController.getDialogs(folderId));

        items.add(LocaleController.getString("All", R.string.All));
        options.add(DialogType.All);

        ArrayList<TLRPC.Dialog> temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsUsers);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Users", R.string.Users));
            options.add(DialogType.Users);
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsGroups);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Groups", R.string.Groups));
            options.add(DialogType.Groups);
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsChannels);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Channels", R.string.Channels));
            options.add(DialogType.Channels);
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsBots);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Bots", R.string.Bots));
            options.add(DialogType.Bots);
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsAdmin);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Admins", R.string.Admins));
            options.add(DialogType.Admin);
        }

        if (scrimPopupWindow != null) {
            scrimPopupWindow.dismiss();
            scrimPopupWindow = null;
            return;
        }

        Rect rect = new Rect();

        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(parentActivity);
        popupLayout.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                    actionBar.getHitRect(rect);
                    if (!rect.contains((int) event.getX(), (int) event.getY())) {
                        scrimPopupWindow.dismiss();
                    }
                }
            } else if (event.getActionMasked() == MotionEvent.ACTION_OUTSIDE) {
                if (scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                    scrimPopupWindow.dismiss();
                }
            }
            return false;
        });
        popupLayout.setDispatchKeyEventListener(keyEvent -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                scrimPopupWindow.dismiss();
            }
        });
        Rect backgroundPaddings = new Rect();
        Drawable shadowDrawable = parentActivity.getResources().getDrawable(R.drawable.popup_fixed_alert).mutate();
        shadowDrawable.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_dialogBackground), PorterDuff.Mode.MULTIPLY));
        shadowDrawable.getPadding(backgroundPaddings);
        popupLayout.setBackgroundDrawable(shadowDrawable);

        LinearLayout linearLayout = new LinearLayout(parentActivity);
        GridLayout gridLayout = new GridLayout(parentActivity);
        RelativeLayout cascadeLayout = new RelativeLayout(parentActivity) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                setMeasuredDimension(gridLayout.getMeasuredWidth(), getMeasuredHeight());
            }
        };
        cascadeLayout.addView(gridLayout, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        cascadeLayout.addView(linearLayout, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        ScrollView scrollView;
        if (Build.VERSION.SDK_INT >= 21) {
            scrollView = new ScrollView(parentActivity, null, 0, R.style.scrollbarShapeStyle) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    setMeasuredDimension(cascadeLayout.getMeasuredWidth(), getMeasuredHeight());
                }
            };
        } else {
            scrollView = new ScrollView(parentActivity);
        }
        scrollView.setClipToPadding(false);
        popupLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        gridLayout.setColumnCount(2);
        gridLayout.setMinimumWidth(AndroidUtilities.dp(200));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        for (int a = 0, N = items.size(); a < N; a++) {
            ActionBarMenuSubItem cell = new ActionBarMenuSubItem(parentActivity);
            cell.setText(items.get(a).toString());
            ActionBarMenuSubItem cell2 = new ActionBarMenuSubItem(parentActivity);
            linearLayout.addView(cell2);
            gridLayout.addView(cell);
            UnreadCountBadgeView badge = new UnreadCountBadgeView(parentActivity, "2333");
            gridLayout.addView(badge);
            final int i = a;
            cell2.setOnClickListener(v1 -> {
                dialogsActivity.updateDialogsType(options.get(i));
                if (scrimPopupWindow != null) {
                    scrimPopupWindow.dismiss();
                }
            });
        }

        scrollView.addView(cascadeLayout, LayoutHelper.createScroll(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.TOP));
        scrimPopupWindow = new ActionBarPopupWindow(popupLayout, LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT) {
            @Override
            public void dismiss() {
                super.dismiss();
                if (scrimPopupWindow != this) {
                    return;
                }
                scrimPopupWindow = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    parentActivity.getWindow().getDecorView().setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
                }
            }
        };
        scrimPopupWindow.setDismissAnimationDuration(220);
        scrimPopupWindow.setOutsideTouchable(true);
        scrimPopupWindow.setClippingEnabled(true);
        scrimPopupWindow.setAnimationStyle(R.style.PopupContextAnimation);
        scrimPopupWindow.setFocusable(true);
        popupLayout.measure(View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST), View.MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(1000), View.MeasureSpec.AT_MOST));
        scrimPopupWindow.setInputMethodMode(ActionBarPopupWindow.INPUT_METHOD_NOT_NEEDED);
        scrimPopupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_UNSPECIFIED);
        scrimPopupWindow.getContentView().setFocusableInTouchMode(true);
        int popupX = actionBar.getLeft() + x - popupLayout.getMeasuredWidth() + backgroundPaddings.left - AndroidUtilities.dp(28);
        if (popupX < AndroidUtilities.dp(6)) {
            popupX = AndroidUtilities.dp(6);
        } else if (popupX > listView.getMeasuredWidth() - AndroidUtilities.dp(6) - popupLayout.getMeasuredWidth()) {
            popupX = listView.getMeasuredWidth() - AndroidUtilities.dp(6) - popupLayout.getMeasuredWidth();
        }
        if (AndroidUtilities.isTablet()) {
            int[] location = new int[2];
            fragmentView.getLocationInWindow(location);
            popupX += location[0];
        }
        int popupY;
        popupY = actionBar.getTop() + y;
        scrimPopupWindow.showAtLocation(actionBar, Gravity.LEFT | Gravity.TOP, popupX, popupY);

    }

    public static class DialogType {
        public static final int All = 0;
        public static final int Users = 7;
        public static final int Groups = 8;
        public static final int Channels = 9;
        public static final int Bots = 10;
        public static final int Admin = 11;
    }
}
