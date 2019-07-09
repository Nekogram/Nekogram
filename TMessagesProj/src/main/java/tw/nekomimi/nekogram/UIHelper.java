package tw.nekomimi.nekogram;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

public class UIHelper {

    static private void setStatusBarColor(Activity parentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (NekoConfig.transparentStatusBar &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || NekoConfig.navigationBarTint)) {
                parentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                parentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                parentActivity.getWindow().setStatusBarColor(Color.TRANSPARENT);
                setUIFlagLight(parentActivity);
            }
        }
    }

    static private void setUIFlagLight(Activity parentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = parentActivity.getWindow().getDecorView();
            int colorCode = Theme.getColor(Theme.key_actionBarDefault);
            int flags = view.getSystemUiVisibility();
            if (ColorUtils.calculateLuminance(colorCode) > 0.8 && NekoConfig.transparentStatusBar &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || NekoConfig.navigationBarTint)) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                view.setSystemUiVisibility(flags);
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                view.setSystemUiVisibility(flags);
            }
        }
    }

    static private void setNavigationBarColor(Activity parentActivity) {
        int color;
        switch (NekoConfig.navigationBarColor) {
            case 3:
                color = Theme.getColor(Theme.key_chat_messagePanelBackground);
                break;
            case 2:
                color = Theme.getColor(Theme.key_actionBarDefault);
                break;
            case 1:
            default:
                color = 0xff000000;
        }
        setNavigationBarColor(parentActivity, color);
    }

    static private void setNavigationBarColor(Activity parentActivity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (NekoConfig.navigationBarTint) {
                parentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                parentActivity.getWindow().setNavigationBarColor(color);
                setUIFlagLightNavBar(parentActivity, color);
            }
        }
    }

    static private void setUIFlagLightNavBar(Activity parentActivity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View view = parentActivity.getWindow().getDecorView();
            int flags = view.getSystemUiVisibility();
            if (ColorUtils.calculateLuminance(color) > 0.8 && NekoConfig.navigationBarTint) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                view.setSystemUiVisibility(flags);
            } else {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                view.setSystemUiVisibility(flags);
            }
        }
    }

    static private void clearColor(Activity parentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            parentActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            parentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
    }

    static public void updateStatusBarColor(Activity parentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (NekoConfig.transparentStatusBar) {
                setStatusBarColor(parentActivity);
            } else {
                clearColor(parentActivity);
                setNavigationBarColor(parentActivity);
            }
            setUIFlagLight(parentActivity);
        }
    }

    public static void updateNavigationBarColor(Activity parentActivity) {
        int color;
        switch (NekoConfig.navigationBarColor) {
            case 3:
                color = Theme.getColor(Theme.key_chat_messagePanelBackground);
                break;
            case 2:
                color = Theme.getColor(Theme.key_actionBarDefault);
                break;
            case 1:
            default:
                color = 0xff000000;
        }
        updateNavigationBarColor(parentActivity, color);
    }

    public static void updateNavigationBarColor(Activity parentActivity, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (NekoConfig.navigationBarTint) {
                setNavigationBarColor(parentActivity, color);
            } else {
                clearColor(parentActivity);
                setStatusBarColor(parentActivity);
            }
            setUIFlagLightNavBar(parentActivity, color);
        }
    }

}
