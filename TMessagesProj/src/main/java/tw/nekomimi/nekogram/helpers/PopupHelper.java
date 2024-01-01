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

import tw.nekomimi.nekogram.DatacenterPopupWrapper;
import tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow;

public class PopupHelper {
    private static SimpleMenuPopupWindow mPopupWindow;

    public static void show(ArrayList<? extends CharSequence> entries, String title, int checkedIndex, Context context, View itemView, SimpleMenuPopupWindow.OnItemClickListener listener, Theme.ResourcesProvider resourcesProvider) {
        if (true) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
            builder.setTitle(title);
            final LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            builder.setView(linearLayout);

            for (int a = 0; a < entries.size(); a++) {
                RadioColorCell cell = new RadioColorCell(context, resourcesProvider);
                cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                cell.setTag(a);
                cell.setTextAndValue(entries.get(a), checkedIndex == a);
                cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), Theme.RIPPLE_MASK_ALL));
                linearLayout.addView(cell);
                cell.setOnClickListener(v -> {
                    Integer which = (Integer) v.getTag();
                    builder.getDismissRunnable().run();
                    listener.onClick(which);
                });
            }
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
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
            mPopupWindow = new SimpleMenuPopupWindow(context, resourcesProvider);
            mPopupWindow.setOnItemClickListener(listener);
            mPopupWindow.setEntries(entries.toArray(new CharSequence[0]));
            mPopupWindow.setSelectedIndex(checkedIndex);

            mPopupWindow.show(itemView, container, 0);
        }
    }

    public static void showIdPopup(BaseFragment fragment, View anchorView, long id, int dc, boolean user, float x, float y) {
        Context context = fragment.getParentActivity();
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, R.drawable.popup_fixed_alert2, fragment.getResourceProvider(), ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK) {
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
            ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, LocaleController.getString(R.string.CopyID), false, fragment.getResourceProvider()).setOnClickListener(v -> {
                popupWindow.dismiss();
                AndroidUtilities.addToClipboard(String.valueOf(id));
                BulletinFactory.of(fragment).createCopyBulletin(LocaleController.formatString(R.string.TextCopied)).show();
            });
        }
        if (dc != 0) {
            var dcPopupWrapper = new DatacenterPopupWrapper(fragment, popupLayout.getSwipeBack(), fragment.getResourceProvider());
            int swipeBackIndex = popupLayout.addViewToSwipeBack(dcPopupWrapper.windowLayout);
            ActionBarMenuSubItem subItem = ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_satellite, LocaleController.getString(R.string.DatacenterStatusShort), false, fragment.getResourceProvider());
            subItem.setSubtext(UserHelper.formatDCString(dc));
            subItem.setOnClickListener(v -> popupLayout.getSwipeBack().openForeground(swipeBackIndex));
        }
        if (id != 0 && user) {
            ActionBarMenuSubItem subItem = ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_calendar, LocaleController.getString(R.string.RegistrationDate), false, fragment.getResourceProvider());
            UserHelper.RegDate regDate = UserHelper.getRegDate(id);
            subItem.setSubtext(regDate != null ? UserHelper.formatRegDate(regDate) : LocaleController.getString(R.string.Loading));
            if (regDate == null) {
                UserHelper.getRegDate(id, arg -> {
                    if (arg != null) {
                        subItem.setSubtext(UserHelper.formatRegDate(arg), true);
                    } else {
                        subItem.setSubtext(LocaleController.getString(R.string.ErrorOccurred), true);
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
