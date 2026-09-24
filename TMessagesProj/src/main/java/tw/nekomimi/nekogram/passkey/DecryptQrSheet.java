package tw.nekomimi.nekogram.passkey;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditTextCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TextHelper;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.nio.charset.StandardCharsets;

public class DecryptQrSheet extends BottomSheet {

    private DecryptQrSheet(Context context, Encryptable qr, String title, Runnable onDecrypted) {
        super(context, true);

        smoothKeyboardAnimationEnabled = true;
        setCanDismissWithSwipe(false);
        setCanDismissWithTouchOutside(false);
        setCanceledOnTouchOutside(false);

        fixNavigationBar(Theme.getColor(Theme.key_windowBackgroundGray));
        setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        var linearLayout = new LinearLayout(context);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        var titleView = TextHelper.makeTextView(context, 20, Theme.key_windowBackgroundWhiteBlackText, true);
        titleView.setText(title);
        linearLayout.addView(titleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 16, 6, 16, 0));

        var subTitleView = TextHelper.makeTextView(context, 14, Theme.key_windowBackgroundWhiteGrayText, false);
        subTitleView.setText(LocaleController.getString(R.string.EnterPassword));
        linearLayout.addView(subTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 16, 6, 16, 6));

        var passwordField = new EditTextCell(context, LocaleController.getString(R.string.LoginPassword), false, false, -1, null);
        passwordField.editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        passwordField.setBackground(Theme.createRoundRectDrawable(dp(16), Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider)));
        linearLayout.addView(passwordField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.BOTTOM, 12, 12, 12, 6));

        var saveView = new ButtonWithCounterView(context, true, resourcesProvider).setRound();
        saveView.setText(LocaleController.getString(R.string.Confirm), false);
        saveView.setOnClickListener(v -> {
            var password = passwordField.editText.getText();
            if (TextUtils.isEmpty(password) || password.length() < 8) {
                AndroidUtilities.shakeViewSpring(passwordField, -6);
                BotWebViewVibrationEffect.APP_ERROR.vibrate();
                return;
            }

            try {
                qr.decrypt(password.toString().getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                FileLog.e(e);
                AndroidUtilities.shakeViewSpring(passwordField, -6);
                BotWebViewVibrationEffect.APP_ERROR.vibrate();
                getBulletinFactory().createErrorBulletin(LocaleController.getString(R.string.CheckPasswordWrong)).show();
                return;
            }

            dismiss();
            onDecrypted.run();
        });
        linearLayout.addView(saveView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 12, 12, 12, 12));

        passwordField.editText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                saveView.callOnClick();
                return true;
            }
            return false;
        });

        var scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        setCustomView(scrollView);
    }

    public static void show(Context context, Encryptable qr, String title, Runnable onDecrypted) {
        if (!qr.needDecryption()) {
            onDecrypted.run();
            return;
        }
        new DecryptQrSheet(context, qr, title, onDecrypted).show();
    }
}
