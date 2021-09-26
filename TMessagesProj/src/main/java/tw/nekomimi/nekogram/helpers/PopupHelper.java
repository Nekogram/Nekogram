package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;

import java.util.ArrayList;

import tw.nekomimi.nekogram.simplemenu.SimpleMenuPopupWindow;

public class PopupHelper {
    private static SimpleMenuPopupWindow mPopupWindow;

    public static void show(ArrayList<? extends CharSequence> entries, String title, int checkedIndex, Context context, View itemView, SimpleMenuPopupWindow.OnItemClickListener listener) {
        show(entries, title, checkedIndex, context, itemView, listener, null);
    }

    public static void show(ArrayList<? extends CharSequence> entries, String title, int checkedIndex, Context context, View itemView, SimpleMenuPopupWindow.OnItemClickListener listener, Theme.ResourcesProvider resourcesProvider) {
        if (itemView == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
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
}
