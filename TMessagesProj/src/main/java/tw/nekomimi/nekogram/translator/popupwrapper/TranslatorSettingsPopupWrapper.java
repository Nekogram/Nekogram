package tw.nekomimi.nekogram.translator.popupwrapper;

import android.view.View;
import android.widget.FrameLayout;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.PopupSwipeBackLayout;

import tw.nekomimi.nekogram.translator.Translator;

public class TranslatorSettingsPopupWrapper {

    public ActionBarPopupWindow.ActionBarPopupWindowLayout windowLayout;

    public TranslatorSettingsPopupWrapper(BaseFragment fragment, PopupSwipeBackLayout swipeBackLayout, long dialogId, Theme.ResourcesProvider resourcesProvider) {
        var context = fragment.getParentActivity();
        windowLayout = new ActionBarPopupWindow.ActionBarPopupWindowLayout(context, 0, resourcesProvider, ActionBarPopupWindow.ActionBarPopupWindowLayout.FLAG_USE_SWIPEBACK);
        windowLayout.setFitItems(true);

        if (swipeBackLayout != null) {
            var backItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_arrow_back, LocaleController.getString("Back", R.string.Back), false, resourcesProvider);
            backItem.setOnClickListener(view -> {
                swipeBackLayout.closeForeground();
            });
        }

        var items = new String[]{
                LocaleController.getString("TranslatorType", R.string.TranslatorType),
                LocaleController.getString("TranslationTarget", R.string.TranslationTarget),
                LocaleController.getString("TranslationProvider", R.string.TranslationProvider),
        };
        for (int i = 0; i < items.length; i++) {
            var item = ActionBarMenuItem.addItem(windowLayout, 0, items[i], false, resourcesProvider);
            item.setTag(i);
            item.setOnClickListener(view -> {
                switch ((Integer) view.getTag()) {
                    case 0:
                        Translator.showTranslatorTypeSelector(context, null, null, resourcesProvider);
                        break;
                    case 1:
                        Translator.showTranslationTargetSelector(fragment, null, null, false, resourcesProvider);
                        break;
                    case 2:
                        Translator.showTranslationProviderSelector(context, null, null, resourcesProvider);
                        break;
                }
            });
        }
        var subSwipeBackLayout = windowLayout.getSwipeBack();
        if (subSwipeBackLayout != null) {
            subSwipeBackLayout.addOnSwipeBackProgressListener((layout, toProgress, progress) -> {
                if (swipeBackLayout != null) {
                    swipeBackLayout.setSwipeBackDisallowed(progress != 0);
                }
            });

            View gap = new FrameLayout(context);
            gap.setBackgroundColor(Theme.getColor(Theme.key_graySection, resourcesProvider));
            gap.setTag(R.id.fit_width_tag, 1);
            windowLayout.addView(gap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 8));

            var autoTranslatePopupWrapper = new AutoTranslatePopupWrapper(context, windowLayout.getSwipeBack(), dialogId, resourcesProvider);
            int autoTranslateSwipeBackIndex = windowLayout.addViewToSwipeBack(autoTranslatePopupWrapper.windowLayout);
            var autoTranslateItem = ActionBarMenuItem.addItem(windowLayout, R.drawable.msg_translate, LocaleController.getString("AutoTranslate", R.string.AutoTranslate), true, resourcesProvider);
            autoTranslateItem.setRightIcon(R.drawable.msg_arrowright);
            autoTranslateItem.setOnClickListener(view -> {
                subSwipeBackLayout.openForeground(autoTranslateSwipeBackIndex);
            });
        }
    }
}
