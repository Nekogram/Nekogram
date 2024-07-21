package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;

import java.util.HashMap;
import java.util.function.BiConsumer;

import tw.nekomimi.nekogram.Extra;

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
        Extra.getRegDate(userId, (date, error) -> {
            if (date != 0) {
                regDates.put(userId, date);
            }
            callback.accept(date, error);
        });
    }
}
