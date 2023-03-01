package org.telegram.messenger;

public class LanguageDetector {
    private static Boolean hasSupport = null;

    public interface StringCallback {
        void run(String str);
    }
    public interface ExceptionCallback {
        void run(Exception e);
    }

    public static boolean hasSupport() {
        return hasSupport(false);
    }

    public static boolean hasSupport(boolean initializeFirst) {
        if (hasSupport == null) {
            try {
                if (initializeFirst) {
                    com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(ApplicationLoader.applicationContext);
                }
                com.google.mlkit.nl.languageid.LanguageIdentification.getClient()
                        .identifyLanguage("apple")
                        .addOnSuccessListener(str -> {
                        })
                        .addOnFailureListener(e -> {
                        });
                hasSupport = true;
            } catch (Throwable t) {
                FileLog.e(t);
                if (initializeFirst) {
                    hasSupport = false;
                } else {
                    return hasSupport(true);
                }
            }
        }
        return hasSupport;
    }

    public static void detectLanguage(String text, StringCallback onSuccess, ExceptionCallback onFail) {
        detectLanguage(text, onSuccess, onFail, false);
    }

    public static void detectLanguage(String text, StringCallback onSuccess, ExceptionCallback onFail, boolean initializeFirst) {
        try {
            if (initializeFirst) {
                com.google.mlkit.common.sdkinternal.MlKitContext.initializeIfNeeded(ApplicationLoader.applicationContext);
            }
            com.google.mlkit.nl.languageid.LanguageIdentification.getClient()
                .identifyLanguage(text)
                .addOnSuccessListener(str -> {
                    if (onSuccess != null) {
                        onSuccess.run(str);
                    }
                })
                .addOnFailureListener(e -> {
                    if (onFail != null) {
                        onFail.run(e);
                    }
                });
        } catch (IllegalStateException e) {
            if (!initializeFirst) {
                detectLanguage(text, onSuccess, onFail, true);
            } else {
                if (onFail != null) {
                    onFail.run(e);
                }
                FileLog.e(e, false);
            }
        } catch (Exception e) {
            if (onFail != null) {
                onFail.run(e);
            }
            FileLog.e(e);
        } catch (Throwable t) {
            if (onFail != null) {
                onFail.run(null);
            }
            FileLog.e(t, false);
        }
    }
}
