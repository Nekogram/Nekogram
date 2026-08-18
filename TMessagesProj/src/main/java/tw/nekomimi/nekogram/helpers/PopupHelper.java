package tw.nekomimi.nekogram.helpers;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.RadioColorCell;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

import tw.nekomimi.nekogram.DatacenterPopupWrapper;
import tw.nekomimi.nekogram.tlv.TlViewer;

public class PopupHelper {

    public static void show(List<? extends CharSequence> entries, String title, int checkedIndex, BaseFragment fragment, View itemView, IntConsumer listener) {
        if (itemView == null) {
            var context = fragment.getParentActivity();
            var resourcesProvider = fragment.getResourceProvider();
            var builder = new AlertDialog.Builder(context, resourcesProvider);
            builder.setTitle(title);
            var linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            builder.setView(linearLayout);

            for (int a = 0; a < entries.size(); a++) {
                var cell = new RadioColorCell(context, resourcesProvider);
                cell.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), 0);
                cell.setTag(a);
                cell.setTextAndValue(entries.get(a), checkedIndex == a);
                cell.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), Theme.RIPPLE_MASK_ALL));
                linearLayout.addView(cell);
                cell.setOnClickListener(v -> {
                    Integer which = (Integer) v.getTag();
                    builder.getDismissRunnable().run();
                    listener.accept(which);
                });
            }
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            fragment.showDialog(builder.create());
        } else {
            var popup = ItemOptions.makeOptions(fragment, itemView);
            var parent = itemView.getParent();
            if (parent instanceof RecyclerListView listView) {
                popup.setScrimViewBackground(listView.getClipBackground(itemView));
            }
            popup.setGravity(LocaleController.isRTL ? Gravity.LEFT : Gravity.RIGHT);
            for (int i = 0; i < entries.size(); i++) {
                var entry = entries.get(i);
                var finalI = i;
                popup.addChecked(checkedIndex == i, entry, () -> listener.accept(finalI));
            }
            popup.show();
        }
    }

    public static void showIdPopup(BaseFragment fragment, View anchorView, long id, int dc, long did, long userId, float x, float y) {
        Context context = fragment.getParentActivity();
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, R.drawable.popup_fixed_alert4, fragment.getResourceProvider(), ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
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
                var messagesController = fragment.getMessagesController();
                var peer = did > 0 ? messagesController.getUser(did) : messagesController.getChat(-did);
                var peerFull = did > 0 ? messagesController.getUserFull(did) : messagesController.getChatFull(-did);
                if (peer == null) {
                    return;
                }
                if (peerFull == null) {
                    TlViewer.openTlViewer(fragment, peer);
                } else {
                    TlViewer.openTlViewer(fragment, peer, peerFull);
                }
            });
        }
        popupLayout.setParentWindow(popupWindow);
    }

    public static void showCopyPopup(BaseFragment fragment, CharSequence title, View anchorView, float x, float y, Runnable callback) {
        Context context = fragment.getParentActivity();
        ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, R.drawable.popup_fixed_alert4, fragment.getResourceProvider());
        popupLayout.setFitItems(true);
        ActionBarPopupWindow popupWindow = AlertsCreator.createSimplePopup(fragment, popupLayout, anchorView, x, y);
        ActionBarMenuItem.addItem(popupLayout, R.drawable.msg_copy, title, false, fragment.getResourceProvider()).setOnClickListener(v -> {
            popupWindow.dismiss();
            callback.run();
        });
        popupLayout.setParentWindow(popupWindow);
    }

    private static final Set<String> TELEGRAM_PACKAGES = Set.of("org.telegram.messenger", "org.telegram.messenger.web", "org.telegram.messenger.beta");

    private static Intent findOfficialTelegram(Context context, String uri) {
        var pm = context.getPackageManager();
        var intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(uri));
        var activities = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (var info : activities) {
            if (TELEGRAM_PACKAGES.contains(info.activityInfo.packageName)) {
                intent.setPackage(info.activityInfo.packageName);
                return intent;
            }
        }
        return null;
    }

    public static void showBlameAlert(Context context, @StringRes int text, String uri) {
        var linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        var scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);

        var title = new TextView(context);
        title.setGravity(Gravity.START);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setText(AndroidUtilities.replaceTags(LocaleController.getString(R.string.SubscribeToPremiumOfficialAppNeeded)));
        linearLayout.addView(title, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 16, 21, 0));

        var description = new TextView(context);
        description.setGravity(Gravity.START);
        description.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        description.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        description.setText(AndroidUtilities.replaceTags(LocaleController.getString(text)));
        linearLayout.addView(description, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 21, 15, 21, 16));

        var buttonTextView = new ButtonWithCounterView(context, true, null).setRound();
        var officialIntent = findOfficialTelegram(context, uri);
        if (officialIntent != null) {
            buttonTextView.setText(LocaleController.getString(R.string.OpenOfficialApp));
            buttonTextView.setOnClickListener(v -> {
                try {
                    context.startActivity(officialIntent);
                } catch (ActivityNotFoundException e) {
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger")));
                }
            });
        } else {
            buttonTextView.setText(LocaleController.getString(R.string.InstallOfficialApp));
            buttonTextView.setOnClickListener(v -> context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger"))));
        }
        linearLayout.addView(buttonTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 21, 0, 21, 16));

        var sheet = new BottomSheet(context, false);
        sheet.setCustomView(scrollView);
        sheet.show();
    }
}
