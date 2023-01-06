package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.DispatchQueue;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipFile;

public class UnzipHelper {
    private static final DispatchQueue unzipQueue = new DispatchQueue("unzipQueue");

    public static void unzip(String path, File output, Runnable callback) {
        unzipQueue.postRunnable(() -> {
            try (var zip = new ZipFile(path)) {
                var entries = zip.entries();
                while (entries.hasMoreElements()) {
                    var entry = entries.nextElement();
                    var target = new File(output.getAbsolutePath(), entry.getName());
                    if (!entry.isDirectory()) {
                        var in = zip.getInputStream(entry);
                        AndroidUtilities.copyFile(in, target);
                        in.close();
                    } else {
                        //noinspection ResultOfMethodCallIgnored
                        target.mkdir();
                    }
                }
            } catch (IOException e) {
                FileLog.e(e);
            }
            AndroidUtilities.runOnUIThread(callback);
        });
    }
}