package tw.nekomimi.nekogram.helpers;

import android.view.View;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LanguageDetector;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class LanguageDetectorTimeout {

    public static void detectLanguage(View parent, String text, LanguageDetector.StringCallback onSuccess, LanguageDetector.ExceptionCallback onFail, AtomicBoolean waitForLangDetection, AtomicReference<Runnable> onLangDetectionDone) {
        waitForLangDetection.set(true);
        LanguageDetector.detectLanguage(
                text,
                (String lang) -> {
                    onSuccess.run(lang);
                    waitForLangDetection.set(false);
                    if (onLangDetectionDone.get() != null) {
                        onLangDetectionDone.get().run();
                        onLangDetectionDone.set(null);
                    }
                },
                (Exception e) -> {
                    FileLog.e("mlkit: failed to detect language");
                    if (onFail != null) onFail.run(e);
                    waitForLangDetection.set(false);
                    if (onLangDetectionDone.get() != null) {
                        onLangDetectionDone.get().run();
                        onLangDetectionDone.set(null);
                    }
                }
        );
        parent.postDelayed(() -> {
            if (onLangDetectionDone.get() != null) {
                onLangDetectionDone.getAndSet(null).run();
            }
        }, 250);
    }
}
