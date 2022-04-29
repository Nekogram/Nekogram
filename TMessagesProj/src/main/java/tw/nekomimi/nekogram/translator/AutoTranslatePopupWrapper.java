package tw.nekomimi.nekogram.translator;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import tw.nekomimi.nekogram.DialogConfig;

public class AutoTranslatePopupWrapper {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;
    private final long dialogId;
    private final ActionBarMenuSubItem defaultItem;
    private final ActionBarMenuSubItem enableItem;
    private final ActionBarMenuSubItem disableItem;

    public AutoTranslatePopupWrapper(Context context, PopupSwipeBackLayout swipeBackLayout, long dialogId, Theme.ResourcesProvider resourcesProvider) {
        windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, resourcesProvider);
        windowLayout.setFitItems(true);
        this.dialogId = dialogId;

        if (swipeBackLayout != null) {
            var backItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_arrow_back, LocaleController.getString("Back", R.string.Back), false, resourcesProvider);
            backItem.setOnClickListener(view -> {
                swipeBackLayout.closeForeground();
            });
        }

        defaultItem = ActionBarMenuItem.addItem(windowLayout, 0, LocaleController.getString("Default", R.string.Default), true, resourcesProvider);

        defaultItem.setOnClickListener(view -> {
            DialogConfig.removeAutoTranslateConfig(dialogId);
            updateItems();
        });

        enableItem = ActionBarMenuItem.addItem(windowLayout, 0, LocaleController.getString("Enable", R.string.Enable), true, resourcesProvider);
        enableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId) && DialogConfig.isAutoTranslateEnable(dialogId));
        enableItem.setOnClickListener(view -> {
            DialogConfig.setAutoTranslateEnable(dialogId, true);
            updateItems();
        });

        disableItem = ActionBarMenuItem.addItem(windowLayout, 0, LocaleController.getString("Disable", R.string.Disable), true, resourcesProvider);
        disableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId) && !DialogConfig.isAutoTranslateEnable(dialogId));
        disableItem.setOnClickListener(view -> {
            DialogConfig.setAutoTranslateEnable(dialogId, false);
            updateItems();
        });
        updateItems();

        View gap = new FrameLayout(context);
        gap.setBackgroundColor(Theme.getColor(Theme.key_graySection));
        gap.setTag(R.id.fit_width_tag, 1);
        windowLayout.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));

        TextView textView = new TextView(context);
        textView.setTag(R.id.fit_width_tag, 1);
        textView.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(8), AndroidUtilities.dp(13), AndroidUtilities.dp(8));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem));
        textView.setText(LocaleController.getString("AutoTranslateAbout", R.string.AutoTranslateAbout));
        windowLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public void updateItems() {
        defaultItem.setChecked(!DialogConfig.hasAutoTranslateConfig(dialogId));
        enableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId) && DialogConfig.isAutoTranslateEnable(dialogId));
        disableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId) && !DialogConfig.isAutoTranslateEnable(dialogId));
    }
}
