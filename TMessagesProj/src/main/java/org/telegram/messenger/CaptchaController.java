package org.telegram.messenger;

import org.telegram.tgnet.ConnectionsManager;

public class CaptchaController {

    private static class Request {
        private final int currentAccount;
        private final int requestToken;
        private final int generation;
        private boolean completed;

        private Request(int currentAccount, int requestToken, int generation) {
            this.currentAccount = currentAccount;
            this.requestToken = requestToken;
            this.generation = generation;
        }

        private void done(String token) {
            if (completed) {
                return;
            }
            completed = true;
            ConnectionsManager.native_receivedCaptchaResult(currentAccount, requestToken, generation, token);
        }
    }

    public static void request(int currentAccount, int requestToken, int generation, String action, String key_id) {
        final Request finalRequest = new Request(currentAccount, requestToken, generation);

        finalRequest.done("RECAPTCHA_FAILED_NO_ACTIVITY");
    }

    private static String formatException(Exception e) {
        if (e == null) return "NULL";
        if (e.getMessage() == null) return "MSG_NULL";
        return e.getMessage().replaceAll(" ", "_").toUpperCase();
    }

}
