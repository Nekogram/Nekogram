package tw.nekomimi.nekogram.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PatternMatcher;

import androidx.annotation.RequiresApi;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;

import java.util.HashMap;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MonetHelper {
    private static final HashMap<String, Integer> ids = new HashMap<>();
    private static final String ACTION_OVERLAY_CHANGED = "android.intent.action.OVERLAY_CHANGED";
    private static final OverlayChangeReceiver overlayChangeReceiver = new OverlayChangeReceiver();

    static {
        ids.put("a1_0", android.R.color.system_accent1_0);
        ids.put("a1_10", android.R.color.system_accent1_10);
        ids.put("a1_50", android.R.color.system_accent1_50);
        ids.put("a1_100", android.R.color.system_accent1_100);
        ids.put("a1_200", android.R.color.system_accent1_200);
        ids.put("a1_300", android.R.color.system_accent1_300);
        ids.put("a1_400", android.R.color.system_accent1_400);
        ids.put("a1_500", android.R.color.system_accent1_500);
        ids.put("a1_600", android.R.color.system_accent1_600);
        ids.put("a1_700", android.R.color.system_accent1_700);
        ids.put("a1_800", android.R.color.system_accent1_800);
        ids.put("a1_900", android.R.color.system_accent1_900);
        ids.put("a1_1000", android.R.color.system_accent1_1000);
        ids.put("a2_0", android.R.color.system_accent2_0);
        ids.put("a2_10", android.R.color.system_accent2_10);
        ids.put("a2_50", android.R.color.system_accent2_50);
        ids.put("a2_100", android.R.color.system_accent2_100);
        ids.put("a2_200", android.R.color.system_accent2_200);
        ids.put("a2_300", android.R.color.system_accent2_300);
        ids.put("a2_400", android.R.color.system_accent2_400);
        ids.put("a2_500", android.R.color.system_accent2_500);
        ids.put("a2_600", android.R.color.system_accent2_600);
        ids.put("a2_700", android.R.color.system_accent2_700);
        ids.put("a2_800", android.R.color.system_accent2_800);
        ids.put("a2_900", android.R.color.system_accent2_900);
        ids.put("a2_1000", android.R.color.system_accent2_1000);
        ids.put("a3_0", android.R.color.system_accent3_0);
        ids.put("a3_10", android.R.color.system_accent3_10);
        ids.put("a3_50", android.R.color.system_accent3_50);
        ids.put("a3_100", android.R.color.system_accent3_100);
        ids.put("a3_200", android.R.color.system_accent3_200);
        ids.put("a3_300", android.R.color.system_accent3_300);
        ids.put("a3_400", android.R.color.system_accent3_400);
        ids.put("a3_500", android.R.color.system_accent3_500);
        ids.put("a3_600", android.R.color.system_accent3_600);
        ids.put("a3_700", android.R.color.system_accent3_700);
        ids.put("a3_800", android.R.color.system_accent3_800);
        ids.put("a3_900", android.R.color.system_accent3_900);
        ids.put("a3_1000", android.R.color.system_accent3_1000);
        ids.put("n1_0", android.R.color.system_neutral1_0);
        ids.put("n1_10", android.R.color.system_neutral1_10);
        ids.put("n1_50", android.R.color.system_neutral1_50);
        ids.put("n1_100", android.R.color.system_neutral1_100);
        ids.put("n1_200", android.R.color.system_neutral1_200);
        ids.put("n1_300", android.R.color.system_neutral1_300);
        ids.put("n1_400", android.R.color.system_neutral1_400);
        ids.put("n1_500", android.R.color.system_neutral1_500);
        ids.put("n1_600", android.R.color.system_neutral1_600);
        ids.put("n1_700", android.R.color.system_neutral1_700);
        ids.put("n1_800", android.R.color.system_neutral1_800);
        ids.put("n1_900", android.R.color.system_neutral1_900);
        ids.put("n1_1000", android.R.color.system_neutral1_1000);
        ids.put("n2_0", android.R.color.system_neutral2_0);
        ids.put("n2_10", android.R.color.system_neutral2_10);
        ids.put("n2_50", android.R.color.system_neutral2_50);
        ids.put("n2_100", android.R.color.system_neutral2_100);
        ids.put("n2_200", android.R.color.system_neutral2_200);
        ids.put("n2_300", android.R.color.system_neutral2_300);
        ids.put("n2_400", android.R.color.system_neutral2_400);
        ids.put("n2_500", android.R.color.system_neutral2_500);
        ids.put("n2_600", android.R.color.system_neutral2_600);
        ids.put("n2_700", android.R.color.system_neutral2_700);
        ids.put("n2_800", android.R.color.system_neutral2_800);
        ids.put("n2_900", android.R.color.system_neutral2_900);
        ids.put("n2_1000", android.R.color.system_neutral2_1000);
        ids.put("monetRedDark", R.color.monetRedDark);
        ids.put("monetRedLight", R.color.monetRedLight);
        ids.put("monetRedCall", R.color.monetRedCall);
        ids.put("monetGreenCall", R.color.monetGreenCall);
    }

    public static int getColor(String color) {
        return getColor(color, false);
    }

    public static int getColor(String color, boolean amoled) {
        try {
            //noinspection ConstantConditions
            int id = ids.getOrDefault(amoled && "n1_900".equals(color) ? "n1_1000" : color, 0);
            return ApplicationLoader.applicationContext.getColor(id);
        } catch (Exception e) {
            Log.e("Theme", "Error loading color " + color);
            e.printStackTrace();
            return 0;
        }
    }

    private static class OverlayChangeReceiver extends BroadcastReceiver {

        public void register(Context context) {
            IntentFilter packageFilter = new IntentFilter(ACTION_OVERLAY_CHANGED);
            packageFilter.addDataScheme("package");
            packageFilter.addDataSchemeSpecificPart("android", PatternMatcher.PATTERN_LITERAL);
            context.registerReceiver(this, packageFilter);
        }

        public void unregister(Context context) {
            context.unregisterReceiver(this);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_OVERLAY_CHANGED.equals(intent.getAction())) {
                if (Theme.getActiveTheme().isMonet()) {
                    Theme.applyTheme(Theme.getActiveTheme());
                }
            }
        }
    }

    public static void registerReceiver(Context context) {
        overlayChangeReceiver.register(context);
    }

    public static void unregisterReceiver(Context context) {
        overlayChangeReceiver.unregister(context);
    }
}
