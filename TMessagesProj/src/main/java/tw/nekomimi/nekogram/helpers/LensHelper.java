package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

public class LensHelper {

    private static final String QUICK_SEARCH_BOX = "com.google.android.googlequicksearchbox";

    private static boolean hasLens = false;
    private static CharSequence lensName;
    private static String lensClass;

    public static boolean hasLens() {
        return hasLens;
    }

    public static CharSequence getLensName() {
        return lensName;
    }

    public static void checkLensSupportAsync() {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                checkLensSupport();
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    private static void checkLensSupport() {
        var pm = ApplicationLoader.applicationContext.getPackageManager();
        var intent = new Intent(Intent.ACTION_SEND);
        intent.setDataAndType(Uri.parse("content://" + ApplicationLoader.getApplicationId() + ".provider"), "image/jpeg");
        intent.setPackage(QUICK_SEARCH_BOX);
        var activities = pm.queryIntentActivities(intent, 0);
        for (var info : activities) {
            if (info.activityInfo.name.contains("Lens")) {
                hasLens = true;
                lensClass = info.activityInfo.name;
                lensName = info.loadLabel(pm);
                break;
            }
        }
    }

    public static void launchLens(Activity activity, Uri uri) {
        var intent = new Intent(Intent.ACTION_SEND);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, "image/jpeg");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.setClassName(QUICK_SEARCH_BOX, lensClass);
        try {
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            FileLog.e(e);
        }
    }

}
