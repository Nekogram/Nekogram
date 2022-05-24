package tw.nekomimi.nekogram.helpers;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.core.content.FileProvider;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildConfig;
import org.telegram.messenger.FileLog;

import java.io.File;

public class ClipboardHelper {

    private static final ClipboardManager clipboardManager;

    static {
        clipboardManager = (ClipboardManager) ApplicationLoader.applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
    }

    public static boolean hasPrimaryClip() {
        return clipboardManager.hasPrimaryClip();
    }

    public static void addFileToClipboard(File file, Runnable callback) {
        try {
            var uri = FileProvider.getUriForFile(ApplicationLoader.applicationContext, BuildConfig.APPLICATION_ID + ".provider", file);
            var clip = ClipData.newUri(ApplicationLoader.applicationContext.getContentResolver(), "label", uri);
            clipboardManager.setPrimaryClip(clip);
            callback.run();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static String getClipboardText() {
        var clip = clipboardManager.getPrimaryClip();
        String clipText;
        if (clip != null && clip.getItemCount() > 0) {
            try {
                clipText = clip.getItemAt(0).coerceToText(ApplicationLoader.applicationContext).toString();
            } catch (Exception e) {
                clipText = null;
            }
        } else {
            clipText = null;
        }
        return clipText;
    }
}
