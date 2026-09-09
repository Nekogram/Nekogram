package tw.nekomimi.nekogram.helpers;

import android.content.Context;

import com.google.android.gms.net.CronetProviderInstaller;
import com.google.android.gms.tasks.Continuation;

import org.chromium.net.CronetEngine;
import org.telegram.messenger.FileLog;

import java.io.File;

public class CronetHelper {

    private static CronetEngine engine = null;

    public static void init(Context context) {
        CronetProviderInstaller.installProvider(context)
                .continueWith((Continuation<Void, Object>) task -> engine = createEngine(context))
                .addOnFailureListener(e -> FileLog.e("Failed to create cronet engine", e));
    }

    public static CronetEngine createEngine(Context context) {
        var builder = new CronetEngine.Builder(context);
        builder.enableBrotli(true);
        builder.enableHttp2(true);
        builder.enableQuic(true);
        var cacheDir = new File(context.getCacheDir(), "cronet");
        if (cacheDir.exists() || cacheDir.mkdirs()) {
            builder.setStoragePath(cacheDir.getAbsolutePath());
            builder.enableHttpCache(CronetEngine.Builder.HTTP_CACHE_DISK_NO_HTTP, 10 * 1024 * 1024);
        }
        return builder.build();
    }

    public static boolean isAvailable() {
        return engine != null;
    }

    public static CronetEngine getEngine() {
        return engine;
    }
}
