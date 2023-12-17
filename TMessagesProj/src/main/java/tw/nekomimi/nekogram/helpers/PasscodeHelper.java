package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.LaunchActivity;

import java.nio.charset.StandardCharsets;

public class PasscodeHelper {
    private static final SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("nekopasscode", Context.MODE_PRIVATE);

    public static boolean checkPasscode(Activity activity, String passcode) {
        if (hasPasscodeForAccount(Integer.MAX_VALUE)) {
            String passcodeHash = preferences.getString("passcodeHash" + Integer.MAX_VALUE, "");
            String passcodeSaltString = preferences.getString("passcodeSalt" + Integer.MAX_VALUE, "");
            if (checkPasscodeHash(passcode, passcodeHash, passcodeSaltString)) {
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated() && isAccountAllowPanic(a)) {
                        MessagesController.getInstance(a).performLogout(1);
                    }
                }
                return false;
            }
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated() && hasPasscodeForAccount(a)) {
                String passcodeHash = preferences.getString("passcodeHash" + a, "");
                String passcodeSaltString = preferences.getString("passcodeSalt" + a, "");
                if (checkPasscodeHash(passcode, passcodeHash, passcodeSaltString)) {
                    if (activity instanceof LaunchActivity launchActivity) {
                        launchActivity.switchToAccount(a, true);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean checkPasscodeHash(String passcode, String passcodeHash, String passcodeSaltString) {
        try {
            byte[] passcodeSalt;
            if (passcodeSaltString.length() > 0) {
                passcodeSalt = Base64.decode(passcodeSaltString, Base64.DEFAULT);
            } else {
                passcodeSalt = new byte[0];
            }
            byte[] passcodeBytes = passcode.getBytes(StandardCharsets.UTF_8);
            byte[] bytes = new byte[32 + passcodeBytes.length];
            System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
            System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
            String hash = Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length));
            return passcodeHash.equals(hash);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return false;
    }

    public static void removePasscodeForAccount(int account) {
        preferences.edit()
                .remove("passcodeHash" + account)
                .remove("passcodeSalt" + account)
                .remove("hide" + account)
                .apply();
    }

    public static boolean isAccountAllowPanic(int account) {
        return preferences.getBoolean("allowPanic" + account, true);
    }

    public static boolean isAccountHidden(int account) {
        return hasPasscodeForAccount(account) && preferences.getBoolean("hide" + account, false);
    }

    public static void setAccountAllowPanic(int account, boolean panic) {
        preferences.edit()
                .putBoolean("allowPanic" + account, panic)
                .apply();
    }

    public static void setHideAccount(int account, boolean hide) {
        preferences.edit()
                .putBoolean("hide" + account, hide)
                .apply();
    }

    public static void setPasscodeForAccount(String firstPassword, int account) {
        try {
            byte[] passcodeSalt = new byte[16];
            Utilities.random.nextBytes(passcodeSalt);
            byte[] passcodeBytes = firstPassword.getBytes(StandardCharsets.UTF_8);
            byte[] bytes = new byte[32 + passcodeBytes.length];
            System.arraycopy(passcodeSalt, 0, bytes, 0, 16);
            System.arraycopy(passcodeBytes, 0, bytes, 16, passcodeBytes.length);
            System.arraycopy(passcodeSalt, 0, bytes, passcodeBytes.length + 16, 16);
            preferences.edit()
                    .putString("passcodeHash" + account, Utilities.bytesToHex(Utilities.computeSHA256(bytes, 0, bytes.length)))
                    .putString("passcodeSalt" + account, Base64.encodeToString(passcodeSalt, Base64.DEFAULT))
                    .apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static boolean hasPasscodeForAccount(int account) {
        return preferences.contains("passcodeHash" + account) && preferences.contains("passcodeSalt" + account);
    }

    public static boolean hasPanicCode() {
        return hasPasscodeForAccount(Integer.MAX_VALUE);
    }

    public static String getSettingsKey() {
        var settingsHash = preferences.getString("settingsHash", "");
        if (!TextUtils.isEmpty(settingsHash)) {
            return settingsHash;
        }
        byte[] bytes = new byte[8];
        Utilities.random.nextBytes(bytes);
        var hash = Base64.encodeToString(bytes, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
        preferences.edit().putString("settingsHash", hash).apply();
        return hash;
    }

    public static boolean isSettingsHidden() {
        return preferences.getBoolean("hideSettings", false);
    }

    public static void setHideSettings(boolean hide) {
        preferences.edit()
                .putBoolean("hideSettings", hide)
                .apply();
    }

    public static boolean isEnabled() {
        return !preferences.getAll().isEmpty();
    }

    public static void clearAll() {
        preferences.edit().clear().apply();
    }
}
