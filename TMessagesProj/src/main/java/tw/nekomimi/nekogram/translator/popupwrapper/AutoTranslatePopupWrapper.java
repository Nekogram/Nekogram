package tw.nekomimi.nekogram.translator.popupwrapper;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LanguageDetector;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import tw.nekomimi.nekogram.DialogConfig;

public class AutoTranslatePopupWrapper {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;
    private final long dialogId;
    private final int topicId;
    private final ActionBarMenuSubItem defaultItem;
    private final ActionBarMenuSubItem enableItem;
    private final ActionBarMenuSubItem disableItem;
    private final boolean supportLanguageDetector = LanguageDetector.hasSupport();

    public AutoTranslatePopupWrapper(BaseFragment fragment, PopupSwipeBackLayout swipeBackLayout, long dialogId, int topicId, Theme.ResourcesProvider resourcesProvider) {
        Context context = fragment.getParentActivity();
        windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, resourcesProvider);
        windowLayout.setFitItems(true);
        this.dialogId = dialogId;
        this.topicId = topicId;

        if (swipeBackLayout != null) {
            var backItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_arrow_back, LocaleController.getString("Back", R.string.Back), false, resourcesProvider);
            backItem.setOnClickListener(view -> swipeBackLayout.closeForeground());
        }

        defaultItem = ActionBarMenuItem.addItem(windowLayout, 0, LocaleController.getString("Default", R.string.Default), true, resourcesProvider);

        defaultItem.setOnClickListener(view -> {
            if (!supportLanguageDetector) {
                BulletinFactory.of(fragment).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            DialogConfig.removeAutoTranslateConfig(dialogId, topicId);
            updateItems();
        });
        defaultItem.setAlpha(supportLanguageDetector ? 1.0f : 0.5f);

        enableItem = ActionBarMenuItem.addItem(windowLayout, 0, LocaleController.getString("Enable", R.string.Enable), true, resourcesProvider);
        enableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId, topicId) && DialogConfig.isAutoTranslateEnable(dialogId, topicId));
        enableItem.setOnClickListener(view -> {
            if (!supportLanguageDetector) {
                BulletinFactory.of(fragment).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            DialogConfig.setAutoTranslateEnable(dialogId, topicId, true);
            updateItems();
        });
        enableItem.setAlpha(supportLanguageDetector ? 1.0f : 0.5f);

        disableItem = ActionBarMenuItem.addItem(windowLayout, 0, LocaleController.getString("Disable", R.string.Disable), true, resourcesProvider);
        disableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId, topicId) && !DialogConfig.isAutoTranslateEnable(dialogId, topicId));
        disableItem.setOnClickListener(view -> {
            if (!supportLanguageDetector) {
                BulletinFactory.of(fragment).createErrorBulletinSubtitle(LocaleController.getString("BrokenMLKit", R.string.BrokenMLKit), LocaleController.getString("BrokenMLKitDetail", R.string.BrokenMLKitDetail), null).show();
                return;
            }
            DialogConfig.setAutoTranslateEnable(dialogId, topicId, false);
            updateItems();
        });
        disableItem.setAlpha(supportLanguageDetector ? 1.0f : 0.5f);
        updateItems();

        FrameLayout gap = new FrameLayout(context);
        gap.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator, resourcesProvider));
        View gapShadow = new View(context);
        gapShadow.setBackground(Theme.getThemedDrawable(context, R.drawable.greydivider, Theme.key_windowBackgroundGrayShadow, resourcesProvider));
        gap.addView(gapShadow, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        gap.setTag(R.id.fit_width_tag, 1);
        windowLayout.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));

        TextView textView = new TextView(context);
        textView.setTag(R.id.fit_width_tag, 1);
        textView.setPadding(AndroidUtilities.dp(13), AndroidUtilities.dp(8), AndroidUtilities.dp(13), AndroidUtilities.dp(8));
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        textView.setTextColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider));
        textView.setText(LocaleController.getString("AutoTranslateAbout", R.string.AutoTranslateAbout));
        windowLayout.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
    }

    public void updateItems() {
        defaultItem.setChecked(!DialogConfig.hasAutoTranslateConfig(dialogId, topicId));
        enableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId, topicId) && DialogConfig.isAutoTranslateEnable(dialogId, topicId));
        disableItem.setChecked(DialogConfig.hasAutoTranslateConfig(dialogId, topicId) && !DialogConfig.isAutoTranslateEnable(dialogId, topicId));
    }
}
