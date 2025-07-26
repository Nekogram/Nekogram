package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;

import java.util.HashMap;
import java.util.function.BiConsumer;

import tw.nekomimi.nekogram.Extra;
import tw.nekomimi.nekogram.helpers.remote.BaseRemoteHelper;

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
        InlineBotHelper.getInstance(UserConfig.selectedAccount).query(Extra.getHelperBot(), "get_regdate " + userId + BaseRemoteHelper.getRequestExtra(), (results, error) -> {
            if (error != null) {
                callback.accept(0, error);
                return;
            }
            var result = !results.isEmpty() ? results.get(0) : null;
            if (result == null) {
                callback.accept(0, "EMPTY_RESULT");
                return;
            }
            int date;
            try {
                date = Integer.parseInt(BaseRemoteHelper.getTextFromInlineResult(result));
            } catch (NumberFormatException e) {
                callback.accept(0, "INVALID_RESULT");
                return;
            }
            regDates.put(userId, date);
            callback.accept(date, null);
        });
    }
}
