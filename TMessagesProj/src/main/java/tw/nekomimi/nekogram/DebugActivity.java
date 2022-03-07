package tw.nekomimi.nekogram;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ShadowSectionCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public class DebugActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.Theme_TMessages);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);

        var contentView = new LinearLayout(this);
        contentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        contentView.setOrientation(LinearLayout.VERTICAL);

        var enableDebugItem = new TextCheckCell(this);
        enableDebugItem.setBackground(Theme.getSelectorDrawable(true));
        enableDebugItem.setTextAndCheck("Debugging features", NekoConfig.showHiddenFeature, true);
        enableDebugItem.setOnClickListener(view -> {
            NekoConfig.toggleShowHiddenFeature();
            enableDebugItem.setChecked(NekoConfig.showHiddenFeature);
        });
        contentView.addView(enableDebugItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        var trustItem = new TextCheckCell(this);
        trustItem.setBackground(Theme.getSelectorDrawable(true));
        trustItem.setTextAndCheck("Trust", !NekoConfig.shouldNOTTrustMe, false);
        trustItem.setOnClickListener(view -> {
            NekoConfig.toggleShouldNOTTrustMe();
            trustItem.setChecked(!NekoConfig.shouldNOTTrustMe);
        });
        contentView.addView(trustItem, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        var shadow = new ShadowSectionCell(this);
        shadow.setBackground(Theme.getThemedDrawable(this, R.drawable.greydivider_bottom, Theme.key_windowBackgroundGrayShadow));
        contentView.addView(shadow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        setContentView(contentView);

        new AlertDialog.Builder(this)
                .setMessage("This is only for debugging purposes!\n" +
                        "Take your own risks with these options.")
                .setPositiveButton("OK", null)
                .show();
    }
}