package tw.nekomimi.nekogram.folder;

import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.Components.LayoutHelper;

import java.util.function.Consumer;

public class IconSelectorAlert {

    public static void show(BaseFragment fragment, View view, String selectedIcon, Consumer<String> onIconSelectedListener) {
        var options = ItemOptions.makeOptions(fragment, view);

        var context = fragment.getParentActivity();
        var gridLayout = new GridLayout(context);
        gridLayout.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        gridLayout.setColumnCount(6);

        for (var icon : FolderIconHelper.folderIcons.keySet()) {
            var imageView = new ImageView(context);
            boolean selected = icon.equals(selectedIcon);
            imageView.setScaleType(ImageView.ScaleType.CENTER);
            imageView.setBackground(Theme.AdaptiveRipple.filledRect(selected ? Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhiteValueText), .1f) : Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), 12));
            imageView.setColorFilter(new PorterDuffColorFilter(Theme.getColor(selected ? Theme.key_chat_messagePanelSend : Theme.key_chat_messagePanelIcons), PorterDuff.Mode.MULTIPLY));
            imageView.setImageResource(FolderIconHelper.getTabIcon(icon));
            imageView.setSelected(selected);
            imageView.setOnClickListener(v -> {
                if (selectedIcon.equals(icon)) {
                    return;
                }
                options.dismiss();
                onIconSelectedListener.accept(icon);
            });
            gridLayout.addView(imageView, LayoutHelper.createFrame(48, 48, Gravity.CENTER));
        }
        options.addView(gridLayout, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        options.setScrimViewBackground(Theme.createCircleDrawable(AndroidUtilities.dp(40), Theme.getColor(Theme.key_windowBackgroundWhite)));
        options.show();
    }
}
