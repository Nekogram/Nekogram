package tw.nekomimi.nekogram.helpers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.OutputSerializedData;
import org.telegram.tgnet.TLObject;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;

import java.util.List;

import tw.nekomimi.nekogram.DatacenterPopupWrapper;

public class PopupHelper {

    public static void show(List<? extends CharSequence> entries, String title, int checkedIndex, Context context, View itemView, Utilities.Callback<Integer> listener, Theme.ResourcesProvider resourcesProvider) {
        if (itemView == null) {
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
                    listener.run(which);
                });
            }
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            builder.show();
        } else {
            ViewGroup container = (ViewGroup) itemView.getRootView();
            if (container == null) {
                return;
            }
            var popup = ItemOptions.makeOptions(container, resourcesProvider, itemView);
            popup.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.get(i);
                var finalI = i;
                popup.add(0, entry, () -> listener.run(finalI));
                if (checkedIndex == i) {
                    popup.putCheck();
                }
            }
            popup.show();
        }
    }

    public static void showIdPopup(BaseFragment fragment, View anchorView, long id, int dc, long did, long userId, float x, float y) {
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
        if (userId != 0) {
            ActionBarMenuSubItem subItem = ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_calendar, LocaleController.getString(R.string.RegistrationDate), false, fragment.getResourceProvider());
            var regDate = RegDateHelper.getRegDate(userId);
            subItem.setSubtext(regDate != null ? RegDateHelper.formatRegDate(regDate, null) : LocaleController.getString(R.string.Loading));
            if (regDate == null) {
                RegDateHelper.getRegDate(userId, (date, error) -> subItem.setSubtext(RegDateHelper.formatRegDate(date, error), true));
            }
        }
        if (did != 0) {
            ActionBarMenuSubItem subItem = ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_stories_caption, LocaleController.getString(R.string.ViewAsJson), false, fragment.getResourceProvider());
            subItem.setOnClickListener(v -> {
                popupWindow.dismiss();
                WebAppHelper.openTLViewer(fragment, getPeerAndFull(fragment, did));
            });
        }
        popupLayout.setParentWindow(popupWindow);
    }

    public static TLObject getPeerAndFull(BaseFragment fragment, long peerId) {
        var messagesController = fragment.getMessagesController();
        var mediaDataController = fragment.getMediaDataController();
        TLObject peer;
        TLObject peerFull;
        TLObject info;
        if (peerId > 0) {
            peer = messagesController.getUser(peerId);
            peerFull = messagesController.getUserFull(peerId);
            info = mediaDataController.getBotInfoCached(peerId, peerId);
        } else {
            peer = messagesController.getChat(-peerId);
            peerFull = messagesController.getChatFull(-peerId);
            info = null;
        }
        if (peer == null) {
            return null;
        }
        return new TLObject() {
            @Override
            public void serializeToStream(OutputSerializedData stream) {
                stream.writeInt32(0x1cb5c415);
                var count = 1;
                if (peerFull != null) count++;
                if (info != null) count++;
                stream.writeInt32(count);
                peer.serializeToStream(stream);
                if (peerFull != null) {
                    peerFull.serializeToStream(stream);
                }
                if (info != null) {
                    info.serializeToStream(stream);
                }
            }
        };
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
