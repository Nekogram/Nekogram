package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;

import java.util.ArrayList;

import tw.nekomimi.nekogram.DatacenterActivity;
import tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow;

public class PopupHelper {
    private static SimpleMenuPopupWindow mPopupWindow;

    public static void show(ArrayList<? extends CharSequence> entries, String title, int checkedIndex, Context context, View itemView, SimpleMenuPopupWindow.OnItemClickListener listener) {
        show(entries, title, checkedIndex, context, itemView, listener, null);
    }

    public static void show(ArrayList<? extends CharSequence> entries, String title, int checkedIndex, Context context, View itemView, SimpleMenuPopupWindow.OnItemClickListener listener, Theme.ResourcesProvider resourcesProvider) {
        if (itemView == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
            builder.setTitle(title);
            final LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            builder.setView(linearLayout);

            for (int a = 0; a < entries.size(); a++) {
                RadioColorCell cell = new RadioColorCell(context);
                cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                cell.setTag(a);
                cell.setCheckColor(Theme.getColor(Theme.key_radioBackground, resourcesProvider), Theme.getColor(Theme.key_dialogRadioBackgroundChecked, resourcesProvider));
                cell.setTextAndValue(entries.get(a), checkedIndex == a);
                linearLayout.addView(cell);
                cell.setOnClickListener(v -> {
                    Integer which = (Integer) v.getTag();
                    builder.getDismissRunnable().run();
                    listener.onClick(which);
                });
            }
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            builder.show();
        } else {
            View container = (View) itemView.getParent();
            if (container == null) {
                return;
            }
            if (mPopupWindow != null) {
                try {
                    if (mPopupWindow.isShowing()) mPopupWindow.dismiss();
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            mPopupWindow = new SimpleMenuPopupWindow(context);
            mPopupWindow.setOnItemClickListener(listener);
            mPopupWindow.setEntries(entries.toArray(new CharSequence[0]));
            mPopupWindow.setSelectedIndex(checkedIndex);

            mPopupWindow.show(itemView, container, 0);
        }
    }

    public static void showIdPopup(BaseFragment fragment, View anchorView, long id, int dc, boolean user, float x, float y) {
        Context context = fragment.getParentActivity();
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, fragment.getResourceProvider()) {
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
        popupLayout.setFitItems(true);
        ActionBarPopupWindow popupWindow = AlertsCreator.createSimplePopup(fragment, popupLayout, anchorView, x, y);
        if (id != 0) {
            ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, LocaleController.getString("CopyID", R.string.CopyID), false, fragment.getResourceProvider()).setOnClickListener(v -> {
                popupWindow.dismiss();
                AndroidUtilities.addToClipboard(String.valueOf(id));
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.formatString("TextCopied", R.string.TextCopied)).show();
            });
        }
        if (dc != 0) {
            ActionBarMenuSubItem subItem = ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_satellite, LocaleController.getString("DatacenterStatusShort", R.string.DatacenterStatusShort), false, fragment.getResourceProvider());
            subItem.setSubtext(MessageHelper.formatDCString(dc));
            subItem.setOnClickListener(v -> {
                popupWindow.dismiss();
                fragment.presentFragment(new DatacenterActivity(dc));
            });
        }
        if (id != 0 && user) {
            ActionBarMenuSubItem subItem = ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_calendar, LocaleController.getString("RegistrationDate", R.string.RegistrationDate), false, fragment.getResourceProvider());
            MessageHelper.RegDate regDate = MessageHelper.getRegDate(id);
            subItem.setSubtext(regDate != null ? MessageHelper.formatRegDate(regDate) : LocaleController.getString("Loading", R.string.Loading));
            if (regDate == null) {
                MessageHelper.getRegDate(id, arg -> {
                    if (arg != null) {
                        subItem.setSubtext(MessageHelper.formatRegDate(arg));
                    } else {
                        subItem.setSubtext(LocaleController.getString("ErrorOccurred", R.string.ErrorOccurred));
                    }
                });
            }
        }
        popupLayout.setParentWindow(popupWindow);
    }

    public static void showCopyPopup(BaseFragment fragment, CharSequence title, View anchorView, float x, float y, Runnable callback) {
        Context context = fragment.getParentActivity();
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, fragment.getResourceProvider()) {
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
        popupLayout.setFitItems(true);
        ActionBarPopupWindow popupWindow = AlertsCreator.createSimplePopup(fragment, popupLayout, anchorView, x, y);
        ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, title, false, fragment.getResourceProvider()).setOnClickListener(v -> {
            popupWindow.dismiss();
            callback.run();
        });
        popupLayout.setParentWindow(popupWindow);
    }
}
