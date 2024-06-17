package tw.nekomimi.nekogram.helpers;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.FileLog;

public class ComponentsHelper {
    private static final String[] COMPONENTS = new String[]{
            "com.google.android.gms.measurement.AppMeasurementJobService",
            "com.google.android.gms.measurement.AppMeasurementService",
            "com.google.android.gms.measurement.AppMeasurementReceiver",
            "com.android.billingclient.api.ProxyBillingActivity",
            "com.android.billingclient.api.ProxyBillingActivityV2",
            "com.google.mlkit.common.internal.MlKitComponentDiscoveryService",
            "com.google.mlkit.common.internal.MlKitInitProvider"
    };

    public static void fixComponents(Context context) {
        var pm = context.getPackageManager();
        for (var cls : COMPONENTS) {
            var component = new ComponentName(context, cls);
            try {
                var state = pm.getComponentEnabledSetting(component);
                if (state != PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) {
                    FileLog.d("Fixing component: " + cls + ", old state = " + state);
                    pm.setComponentEnabledSetting(component,
                            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                            PackageManager.DONT_KILL_APP
                    );
                }
            } catch (Throwable t) {
                FileLog.e("Failed to fix component: " + cls, t);
            }
        }
    }
}
