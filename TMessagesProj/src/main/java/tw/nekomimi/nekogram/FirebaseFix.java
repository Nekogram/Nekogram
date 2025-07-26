package tw.nekomimi.nekogram;

import android.content.Context;

import org.telegram.messenger.FileLog;

public class FirebaseFix {

    public static void check(Context context) {
        var filesDir = context.getFilesDir();
        var files = filesDir.listFiles();
        if (files == null) return;
        for (var file : files) {
            if (file.getName().startsWith("PersistedInstallation.") && file.length() > 1024 * 128) {
                FileLog.e("delete large Firebase file: " + file.getName());
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }
}
