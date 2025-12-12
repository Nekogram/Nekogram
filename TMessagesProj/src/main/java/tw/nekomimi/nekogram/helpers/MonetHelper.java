package tw.nekomimi.nekogram.helpers;

import android.os.Build;

import androidx.annotation.RequiresApi;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.HashMap;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MonetHelper {
    private static final HashMap<String, Integer> ids = new HashMap<>() {{
        put("a1_0", android.R.color.system_accent1_0);
        put("a1_10", android.R.color.system_accent1_10);
        put("a1_50", android.R.color.system_accent1_50);
        put("a1_100", android.R.color.system_accent1_100);
        put("a1_200", android.R.color.system_accent1_200);
        put("a1_300", android.R.color.system_accent1_300);
        put("a1_400", android.R.color.system_accent1_400);
        put("a1_500", android.R.color.system_accent1_500);
        put("a1_600", android.R.color.system_accent1_600);
        put("a1_700", android.R.color.system_accent1_700);
        put("a1_800", android.R.color.system_accent1_800);
        put("a1_900", android.R.color.system_accent1_900);
        put("a1_1000", android.R.color.system_accent1_1000);
        put("a2_0", android.R.color.system_accent2_0);
        put("a2_10", android.R.color.system_accent2_10);
        put("a2_50", android.R.color.system_accent2_50);
        put("a2_100", android.R.color.system_accent2_100);
        put("a2_200", android.R.color.system_accent2_200);
        put("a2_300", android.R.color.system_accent2_300);
        put("a2_400", android.R.color.system_accent2_400);
        put("a2_500", android.R.color.system_accent2_500);
        put("a2_600", android.R.color.system_accent2_600);
        put("a2_700", android.R.color.system_accent2_700);
        put("a2_800", android.R.color.system_accent2_800);
        put("a2_900", android.R.color.system_accent2_900);
        put("a2_1000", android.R.color.system_accent2_1000);
        put("a3_0", android.R.color.system_accent3_0);
        put("a3_10", android.R.color.system_accent3_10);
        put("a3_50", android.R.color.system_accent3_50);
        put("a3_100", android.R.color.system_accent3_100);
        put("a3_200", android.R.color.system_accent3_200);
        put("a3_300", android.R.color.system_accent3_300);
        put("a3_400", android.R.color.system_accent3_400);
        put("a3_500", android.R.color.system_accent3_500);
        put("a3_600", android.R.color.system_accent3_600);
        put("a3_700", android.R.color.system_accent3_700);
        put("a3_800", android.R.color.system_accent3_800);
        put("a3_900", android.R.color.system_accent3_900);
        put("a3_1000", android.R.color.system_accent3_1000);
        put("n1_0", android.R.color.system_neutral1_0);
        put("n1_10", android.R.color.system_neutral1_10);
        put("n1_50", android.R.color.system_neutral1_50);
        put("n1_100", android.R.color.system_neutral1_100);
        put("n1_200", android.R.color.system_neutral1_200);
        put("n1_300", android.R.color.system_neutral1_300);
        put("n1_400", android.R.color.system_neutral1_400);
        put("n1_500", android.R.color.system_neutral1_500);
        put("n1_600", android.R.color.system_neutral1_600);
        put("n1_700", android.R.color.system_neutral1_700);
        put("n1_800", android.R.color.system_neutral1_800);
        put("n1_900", android.R.color.system_neutral1_900);
        put("n1_1000", android.R.color.system_neutral1_1000);
        put("n2_0", android.R.color.system_neutral2_0);
        put("n2_10", android.R.color.system_neutral2_10);
        put("n2_50", android.R.color.system_neutral2_50);
        put("n2_100", android.R.color.system_neutral2_100);
        put("n2_200", android.R.color.system_neutral2_200);
        put("n2_300", android.R.color.system_neutral2_300);
        put("n2_400", android.R.color.system_neutral2_400);
        put("n2_500", android.R.color.system_neutral2_500);
        put("n2_600", android.R.color.system_neutral2_600);
        put("n2_700", android.R.color.system_neutral2_700);
        put("n2_800", android.R.color.system_neutral2_800);
        put("n2_900", android.R.color.system_neutral2_900);
        put("n2_1000", android.R.color.system_neutral2_1000);
        put("monetRedDark", R.color.monetRedDark);
        put("monetRedLight", R.color.monetRedLight);
        put("monetRedCall", R.color.monetRedCall);
        put("monetGreenCall", R.color.monetGreenCall);
    }};
    private static int lastMonetColor = 0;

    public static int getColor(String color) {
        return getColor(color, false);
    }

    public static int getColor(String color, boolean amoled) {
        try {
            //noinspection ConstantConditions
            int id = ids.getOrDefault(amoled && "n1_900".equals(color) ? "n1_1000" : color, 0);
            return ApplicationLoader.applicationContext.getColor(id);
        } catch (Exception e) {
            FileLog.e("Error loading color " + color, e);
            return 0;
        }
    }

    /**
     * Refresh Monet theme if the system color has changed.
     * Called in LaunchActivity.onResume()
     */
    public static void refreshMonetThemeIfChanged() {
        // Quick check: if the current theme is not a Monet theme, return directly
        Theme.ThemeInfo activeTheme = Theme.getActiveTheme();
        if (activeTheme == null || !activeTheme.isMonet()) {
            lastMonetColor = 0; // Reset to detect correctly when switching back to Monet theme
            return;
        }

        int currentColor = getColor("a1_600");

        // Record the color only on the first call, do not trigger refresh
        if (lastMonetColor == 0) {
            lastMonetColor = currentColor;
            return;
        }

        // Return directly if the color has not changed
        if (lastMonetColor == currentColor) {
            return;
        }

        // Refresh theme
        boolean isNight = Theme.isCurrentThemeNight();
        Theme.applyTheme(activeTheme, isNight);
        NotificationCenter.getGlobalInstance().postNotificationName(
                NotificationCenter.needSetDayNightTheme, activeTheme, isNight, null, -1
        );

        lastMonetColor = currentColor;
    }
}
