package tw.nekomimi.nekogram;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

public class UIHelper {

    static private void setStatusBarColor(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (NekoConfig.transparentStatusBar &&
                    (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || NekoConfig.navigationBarTint)) {
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.TRANSPARENT);
                setUIFlagLight(window);
            }
        }
    }

    static private void setUIFlagLight(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View view = window.getDecorView();
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

    static private void setNavigationBarColor(Window window) {
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
        setNavigationBarColor(window, color);
    }

    static private void setNavigationBarColor(Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (NekoConfig.navigationBarTint) {
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setNavigationBarColor(color);
                setUIFlagLightNavBar(window, color);
            }
        }
    }

    static private void setUIFlagLightNavBar(Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            View view = window.getDecorView();
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

    static private void clearColor(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        }
    }

    static public void updateStatusBarColor(Activity parentActivity) {
        updateStatusBarColor(parentActivity.getWindow());
    }

    static public void updateStatusBarColor(Window window) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (NekoConfig.transparentStatusBar) {
                setStatusBarColor(window);
            } else {
                clearColor(window);
                setNavigationBarColor(window);
            }
            setUIFlagLight(window);
        }
    }

    public static void updateNavigationBarColor(Activity parentActivity) {
        updateNavigationBarColor(parentActivity.getWindow());
    }

    public static void updateNavigationBarColor(Window window) {
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
        updateNavigationBarColor(window, color);
    }

    public static void updateNavigationBarColor(Activity parentActivity, int color) {
        updateNavigationBarColor(parentActivity.getWindow(), color);
    }

    public static void updateNavigationBarColor(Window window, int color) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (NekoConfig.navigationBarTint) {
                setNavigationBarColor(window, color);
            } else {
                clearColor(window);
                setStatusBarColor(window);
            }
            setUIFlagLightNavBar(window, color);
        }
    }

}
