package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;

import java.util.Calendar;
import java.util.HashMap;
import java.util.function.BiConsumer;

public class RegDateHelper {
    private static final HashMap<Long, Integer> regDates = new HashMap<>();

    public static String formatRegDate(int regDate, String error) {
        if (error != null) return error;
        return LocaleController.formatString(R.string.RegistrationDateApproximately, LocaleController.getInstance().getFormatterMonthYear().format(regDate * 1000L));
    }

    public static Integer getRegDate(long userId) {
        return regDates.get(userId);
    }

    public static void getRegDate(long userId, BiConsumer<Integer, String> callback) {
        InlineBotHelper.queryText("get_regdate " + userId, (result, error) -> {
            if (error != null) {
                callback.accept(0, error);
                return;
            }
            int date;
            try {
                date = Integer.parseInt(result);
            } catch (NumberFormatException e) {
                callback.accept(0, "INVALID_RESULT");
                return;
            }
            regDates.put(userId, date);
            callback.accept(date, null);
        });
    }

    public static void setRegDate(long dialogId, TLRPC.PeerSettings settings) {
        if (settings == null || settings.registration_month == null) {
            return;
        }
        InlineBotHelper.queryText(String.format("set_regdate %s %s %s", dialogId, settings.registration_month, settings.phone_country), (result, error) -> {
            if (error != null) {
                FileLog.e("Failed to set reg date: " + error);
            }
        });
        var parts = settings.registration_month.split("\\.");
        if (parts.length != 2) return;
        var month = Integer.parseInt(parts[0]);
        var year = Integer.parseInt(parts[1]);
        var calendar = Calendar.getInstance();
        calendar.set(year, month - 1, 2, 0, 0, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        regDates.put(dialogId, (int) (calendar.getTimeInMillis() / 1000L));
    }
}
