package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon)) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        for (LauncherIcon i : LauncherIcon.values()) {
            pm.setComponentEnabledSetting(i.getComponentName(ctx), i == icon ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED :
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.color.ic_launcher_background, R.drawable.ic_launcher_foreground, R.string.AppIconDefault),
        //VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.drawable.ic_launcher_foreground, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.drawable.ic_launcher_foreground, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.drawable.ic_launcher_foreground, R.string.AppIconPremium),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.drawable.ic_launcher_foreground, R.string.AppIconTurbo),
        NOX("NoxIcon", R.drawable.icon_2_background_sa, R.drawable.ic_launcher_foreground, R.string.AppIconNox),
        MERIO("MerioIcon", R.mipmap.icon_7_launcher_background, R.mipmap.icon_7_launcher_foreground, R.string.AppIconMerio),
        RAINBOW("RainbowIcon", R.color.icon_8_launcher_background, R.mipmap.icon_8_launcher_foreground, R.string.AppIconRainbow),
        SCHOOL("OldSchoolIcon", R.mipmap.icon_9_launcher_background, R.mipmap.icon_9_launcher_foreground, R.string.AppIconOldSchool),
        MUSHEEN("MusheenIcon", R.color.ic_launcher_background, R.mipmap.icon_10_launcher_foreground, R.string.AppIconMusheen),
        SPACE("SpaceIcon", R.mipmap.icon_11_launcher_background, R.mipmap.icon_11_launcher_foreground, R.string.AppIconSpace),
        CLOUD("CloudIcon", R.color.ic_launcher_background, R.mipmap.icon_12_launcher_foreground, R.string.AppIconCloud),
        NEON("NeonIcon", R.mipmap.icon_13_launcher_background, R.mipmap.icon_13_launcher_foreground, R.string.AppIconNeon),
        MATERIAL("MaterialIcon", R.mipmap.icon_14_launcher_background, R.mipmap.icon_14_launcher_foreground, R.string.AppIconMaterial);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
