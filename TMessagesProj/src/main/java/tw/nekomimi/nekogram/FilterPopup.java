package tw.nekomimi.nekogram;

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

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.DialogsActivity;

import java.util.ArrayList;

public class FilterPopup extends BaseController {
    private static volatile FilterPopup[] Instance = new FilterPopup[UserConfig.MAX_ACCOUNT_COUNT];
    private ArrayList<TLRPC.Dialog> dialogsAdmin = new ArrayList<>();
    private ArrayList<TLRPC.Dialog> dialogsUsers = new ArrayList<>();
    private ArrayList<TLRPC.Dialog> dialogsGroups = new ArrayList<>();
    private ArrayList<TLRPC.Dialog> dialogsChannels = new ArrayList<>();
    private ArrayList<TLRPC.Dialog> dialogsBots = new ArrayList<>();
    private ActionBarPopupWindow scrimPopupWindow;

    public FilterPopup(int num) {
        super(num);
    }

    public static FilterPopup getInstance(int num) {
        FilterPopup localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (FilterPopup.class) {
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
                TLRPC.Chat chat = getMessagesController().getChat(-lower_id);
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
                TLRPC.User user = getMessagesController().getUser(lower_id);
                if (user != null) {
                    if (user.bot)
                        dialogsBots.add(dialog);
                    else
                        dialogsUsers.add(dialog);
                }
            }
        } else {
            TLRPC.EncryptedChat encryptedChat = getMessagesController().getEncryptedChat(high_id);
            if (encryptedChat != null)
                dialogsUsers.add(dialog);
        }
    }

    public boolean hasHiddenArchive(int type) {
        if (!SharedConfig.archiveHidden)
            return false;
        ArrayList<TLRPC.Dialog> dialogs = getDialogs(type, 0);
        if (dialogs == null)
            return getMessagesController().hasHiddenArchive();
        for (TLRPC.Dialog dialog : dialogs) {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                return true;
            }
        }
        return false;
    }

    private ArrayList<TLRPC.Dialog> filterUnmutedDialogs(ArrayList<TLRPC.Dialog> allDialogs) {
        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        for (TLRPC.Dialog dialog : allDialogs) {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                continue;
            }
            if (!getMessagesController().isDialogMuted(dialog.id)) {
                dialogs.add(dialog);
            }
        }
        return dialogs;
    }

    public ArrayList<TLRPC.Dialog> getDialogs(int type, int folderId) {
        ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(getMessagesController().getDialogs(folderId));
        ArrayList<TLRPC.Dialog> folders = new ArrayList<>();
        ArrayList<ArrayList<TLRPC.Dialog>> folderDialogs = new ArrayList<>();

        for (TLRPC.Dialog dialog : allDialogs) {
            if (dialog instanceof TLRPC.TL_dialogFolder) {
                folders.add(dialog);
                TLRPC.TL_dialogFolder dialogFolder = (TLRPC.TL_dialogFolder) dialog;
                folderDialogs.add(new ArrayList<>(getMessagesController().getDialogs(dialogFolder.folder.id)));
            }
        }

        ArrayList<TLRPC.Dialog> dialogs = new ArrayList<>();
        switch (type) {
            case DialogType.Unmuted:
                for (int i = 0; i < folders.size(); i++) {
                    folderDialogs.get(i).retainAll(filterUnmutedDialogs(folderDialogs.get(i)));
                    if (!folderDialogs.get(i).isEmpty())
                        dialogs.add(folders.get(i));
                }
                allDialogs.retainAll(filterUnmutedDialogs(allDialogs));
                break;
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
            allDialogs = new ArrayList<>(getMessagesController().getDialogs(folderId));
        }
        dialogs.addAll(allDialogs);
        return dialogs;
    }

    public int getTotalUnreadCount() {
        ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(getMessagesController().getDialogs(0));
        return getDialogsUnreadCount(allDialogs);
    }

    private int getDialogsUnreadCount(ArrayList<TLRPC.Dialog> dialogs) {
        int count = 0;
        for (TLRPC.Dialog dialog : dialogs) {
            if (!(dialog instanceof TLRPC.TL_dialogFolder)
                    && !getMessagesController().isDialogMuted(dialog.id)) {
                count += dialog.unread_count;
            }
        }
        return count;
    }

    public void createMenu(DialogsActivity dialogsActivity, int x, int y, int folderId, boolean fab) {
        ArrayList<CharSequence> items = new ArrayList<>();
        final ArrayList<Integer> options = new ArrayList<>();
        ArrayList<Integer> unreadCounts = new ArrayList<>();

        ArrayList<TLRPC.Dialog> allDialogs = new ArrayList<>(getMessagesController().getDialogs(folderId));

        items.add(LocaleController.getString("All", R.string.All));
        options.add(DialogType.All);
        unreadCounts.add(getDialogsUnreadCount(allDialogs));

        ArrayList<TLRPC.Dialog> temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsUsers);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Users", R.string.Users));
            options.add(DialogType.Users);
            unreadCounts.add(getDialogsUnreadCount(temp));
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsGroups);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Groups", R.string.Groups));
            options.add(DialogType.Groups);
            unreadCounts.add(getDialogsUnreadCount(temp));
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsChannels);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Channels", R.string.Channels));
            options.add(DialogType.Channels);
            unreadCounts.add(getDialogsUnreadCount(temp));
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsBots);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Bots", R.string.Bots));
            options.add(DialogType.Bots);
            unreadCounts.add(getDialogsUnreadCount(temp));
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(dialogsAdmin);
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("Admins", R.string.Admins));
            options.add(DialogType.Admin);
            unreadCounts.add(getDialogsUnreadCount(temp));
        }

        temp = new ArrayList<>(allDialogs);
        temp.retainAll(filterUnmutedDialogs(allDialogs));
        if (!temp.isEmpty()) {
            items.add(LocaleController.getString("NotificationsUnmuted", R.string.NotificationsUnmuted));
            options.add(DialogType.Unmuted);
            unreadCounts.add(getDialogsUnreadCount(temp));
        }

        if (scrimPopupWindow != null) {
            scrimPopupWindow.dismiss();
            scrimPopupWindow = null;
            return;
        }

        Rect rect = new Rect();

        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(dialogsActivity.getParentActivity());
        popupLayout.setOnTouchListener(new View.OnTouchListener() {

            private int[] pos = new int[2];

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    if (scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                        View contentView = scrimPopupWindow.getContentView();
                        contentView.getLocationInWindow(pos);
                        rect.set(pos[0], pos[1], pos[0] + contentView.getMeasuredWidth(), pos[1] + contentView.getMeasuredHeight());
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
            }
        });
        popupLayout.setDispatchKeyEventListener(keyEvent -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BACK && keyEvent.getRepeatCount() == 0 && scrimPopupWindow != null && scrimPopupWindow.isShowing()) {
                scrimPopupWindow.dismiss();
            }
        });
        Rect backgroundPaddings = new Rect();
        Drawable shadowDrawable = dialogsActivity.getParentActivity().getResources().getDrawable(R.drawable.popup_fixed_alert).mutate();
        shadowDrawable.getPadding(backgroundPaddings);
        popupLayout.setBackgroundDrawable(shadowDrawable);
        popupLayout.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground));

        LinearLayout linearLayout = new LinearLayout(dialogsActivity.getParentActivity());
        GridLayout gridLayout = new GridLayout(dialogsActivity.getParentActivity());
        RelativeLayout cascadeLayout = new RelativeLayout(dialogsActivity.getParentActivity()) {
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
            scrollView = new ScrollView(dialogsActivity.getParentActivity(), null, 0, R.style.scrollbarShapeStyle) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    setMeasuredDimension(cascadeLayout.getMeasuredWidth(), getMeasuredHeight());
                }
            };
        } else {
            scrollView = new ScrollView(dialogsActivity.getParentActivity());
        }
        scrollView.setClipToPadding(false);
        popupLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        gridLayout.setColumnCount(2);
        gridLayout.setMinimumWidth(AndroidUtilities.dp(200));
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        for (int a = 0, N = items.size(); a < N; a++) {
            ActionBarMenuSubItem cell = new ActionBarMenuSubItem(dialogsActivity.getParentActivity());
            cell.setText(items.get(a).toString());
            cell.setMinimumWidth(AndroidUtilities.dp(171));
            ActionBarMenuSubItem cell2 = new ActionBarMenuSubItem(dialogsActivity.getParentActivity());
            linearLayout.addView(cell2);
            gridLayout.addView(cell);
            UnreadCountBadgeView badge = new UnreadCountBadgeView(dialogsActivity.getParentActivity(), unreadCounts.get(a).toString());
            gridLayout.addView(badge);
            if (unreadCounts.get(a) == 0)
                badge.setVisibility(View.GONE);
            else
                badge.setVisibility(View.VISIBLE);
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
                    dialogsActivity.getParentActivity().getWindow().getDecorView().setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
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
        int popupX = x - popupLayout.getMeasuredWidth() + backgroundPaddings.left - AndroidUtilities.dp(28);
        if (popupX < AndroidUtilities.dp(6)) {
            popupX = AndroidUtilities.dp(6);
        } else if (popupX > dialogsActivity.getFragmentView().getMeasuredWidth() - AndroidUtilities.dp(6) - popupLayout.getMeasuredWidth()) {
            popupX = dialogsActivity.getFragmentView().getMeasuredWidth() - AndroidUtilities.dp(6) - popupLayout.getMeasuredWidth();
        }
        int totalHeight = dialogsActivity.getFragmentView().getHeight();
        int height = popupLayout.getMeasuredHeight();
        int popupY = height < totalHeight ? y - (fab ? height : 0) : AndroidUtilities.statusBarHeight;
        scrimPopupWindow.showAtLocation(dialogsActivity.getFragmentView(), Gravity.LEFT | Gravity.TOP, popupX, popupY);
    }

    public static class DialogType {
        public static final int All = 0;
        public static final int Users = 7;
        public static final int Groups = 8;
        public static final int Channels = 9;
        public static final int Bots = 10;
        public static final int Admin = 11;
        public static final int Unmuted = 12;

        public static boolean isDialogsType(int dialogsType) {
            return dialogsType == 0 || (dialogsType >= 7 && dialogsType <= 12);
        }
    }
}
