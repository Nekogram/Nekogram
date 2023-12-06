package tw.nekomimi.nekogram.helpers;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FingerprintController;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

@SuppressWarnings("deprecation")
@RequiresApi(23)
public class BiometricPromptHelper {

    private final Activity parentActivity;
    private CancellationSignal cancellationSignal;
    private AlertDialog alertDialog;
    private ImageView iconImageView;
    private TextView errorTextView;
    private boolean selfCancelled;

    private int currentState;

    //private static final int STATE_IDLE = 0;
    private static final int STATE_AUTHENTICATING = 1;
    private static final int STATE_ERROR = 2;
    private static final int STATE_PENDING_CONFIRMATION = 3;
    private static final int STATE_AUTHENTICATED = 4;

    private final Runnable resetRunnable = this::handleResetMessage;

    public BiometricPromptHelper(Activity activity) {
        parentActivity = activity;
    }

    public void prompt(Runnable successCallback) {
        if (parentActivity == null || !hasBiometricEnrolled()) {
            return;
        }
        if (!FingerprintController.isKeyReady() || FingerprintController.checkDeviceFingerprintsChanged()) {
            return;
        }
        Activity activity = parentActivity;
        if (Build.VERSION.SDK_INT >= 28) {
            cancellationSignal = new CancellationSignal();
            BiometricPrompt.Builder builder = new BiometricPrompt.Builder(activity);
            builder.setTitle(LocaleController.getString(R.string.AppName));
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), activity.getMainExecutor(), (dialog, which) -> {
            });
            if (Build.VERSION.SDK_INT >= 29) {
                builder.setConfirmationRequired(false);
            }
            builder.build().authenticate(cancellationSignal, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {

                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    successCallback.run();
                }
            });
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (alertDialog != null && alertDialog.isShowing()) {
                return;
            }
            FingerprintManager fingerprintManager = ApplicationLoader.applicationContext.getSystemService(FingerprintManager.class);
            if (fingerprintManager == null || !fingerprintManager.isHardwareDetected() || !fingerprintManager.hasEnrolledFingerprints()) {
                return;
            }
            LinearLayout linearLayout = new LinearLayout(activity);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 4, 4, 4, 4));

            TextView titleTextView = new TextView(activity);
            titleTextView.setTypeface(AndroidUtilities.getTypeface(AndroidUtilities.TYPEFACE_ROBOTO_MEDIUM));
            titleTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
            titleTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            titleTextView.setText(LocaleController.getString(R.string.AppName));
            linearLayout.addView(titleTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 24, 24, 0));

            TextView descriptionTextView = new TextView(activity);
            descriptionTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            descriptionTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            descriptionTextView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            descriptionTextView.setText(LocaleController.getString(R.string.FingerprintInfo));
            descriptionTextView.setPadding(0, AndroidUtilities.dp(8), 0, 0);
            linearLayout.addView(descriptionTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));

            iconImageView = new ImageView(activity);
            iconImageView.setScaleType(ImageView.ScaleType.FIT_XY);
            linearLayout.addView(iconImageView, LayoutHelper.createLinear(64, 64, Gravity.CENTER_HORIZONTAL, 0, 48, 0, 0));

            errorTextView = new TextView(activity);
            errorTextView.setGravity(Gravity.CENTER_HORIZONTAL);
            errorTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            errorTextView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
            errorTextView.setText(LocaleController.getString(R.string.AppName));
            errorTextView.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(24));
            linearLayout.addView(errorTextView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 0, 24, 0));

            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setView(linearLayout);
            builder.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
            builder.setOnDismissListener(dialog -> {
                if (cancellationSignal != null) {
                    selfCancelled = true;
                    try {
                        cancellationSignal.cancel();
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                    cancellationSignal = null;
                }
                alertDialog = null;
            });
            if (alertDialog != null) {
                try {
                    if (alertDialog.isShowing()) {
                        alertDialog.dismiss();
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            alertDialog = builder.show();

            selfCancelled = false;
            cancellationSignal = new CancellationSignal();
            fingerprintManager.authenticate(null, cancellationSignal, 0, new FingerprintManager.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    if (errorCode == FingerprintManager.FINGERPRINT_ERROR_USER_CANCELED) {
                        alertDialog.dismiss();
                    } else if (!selfCancelled && errorCode != FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                        updateState(STATE_ERROR);
                        showTemporaryMessage(errString);
                    }
                }

                @Override
                public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                    updateState(STATE_ERROR);
                    showTemporaryMessage(helpString);
                }

                @Override
                public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
                    builder.getDismissRunnable().run();
                    successCallback.run();
                }

                @Override
                public void onAuthenticationFailed() {
                    updateState(STATE_ERROR);
                    showTemporaryMessage(LocaleController.getString(R.string.FingerprintNotRecognized));
                }
            }, null);

            updateState(STATE_AUTHENTICATING);
            errorTextView.setText(LocaleController.getString(R.string.FingerprintHelp));
            errorTextView.setVisibility(View.VISIBLE);
        }
    }

    private void updateState(int newState) {
        if (newState == STATE_PENDING_CONFIRMATION) {
            AndroidUtilities.cancelRunOnUIThread(resetRunnable);
            errorTextView.setVisibility(View.INVISIBLE);
        } else if (newState == STATE_AUTHENTICATED) {
            errorTextView.setVisibility(View.INVISIBLE);
        }

        updateIcon(currentState, newState);
        currentState = newState;
    }

    private void showTemporaryMessage(CharSequence message) {
        AndroidUtilities.cancelRunOnUIThread(resetRunnable);
        errorTextView.setText(message);
        errorTextView.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        errorTextView.setContentDescription(message);
        Vibrator v = (Vibrator) parentActivity.getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null) {
            v.vibrate(100);
        }
        AndroidUtilities.shakeViewSpring(errorTextView);
        AndroidUtilities.runOnUIThread(resetRunnable, 2000);
    }

    private void handleResetMessage() {
        if (errorTextView == null) {
            return;
        }
        updateState(STATE_AUTHENTICATING);
        errorTextView.setText(LocaleController.getString(R.string.FingerprintHelp));
        errorTextView.setTextColor(Theme.getColor(Theme.key_dialogButton));
    }

    private void updateIcon(int lastState, int newState) {
        final Drawable icon = getAnimationForTransition(lastState, newState);
        if (icon == null) {
            return;
        }

        final AnimatedVectorDrawable animation = icon instanceof AnimatedVectorDrawable ? (AnimatedVectorDrawable) icon : null;

        iconImageView.setImageDrawable(icon);

        if (animation != null && shouldAnimateForTransition(lastState, newState)) {
            animation.start();
        }
    }

    private boolean shouldAnimateForTransition(int oldState, int newState) {
        if (newState == STATE_ERROR) {
            return true;
        } else if (oldState == STATE_ERROR && newState == STATE_AUTHENTICATING) {
            return true;
        } else if (oldState == STATE_AUTHENTICATING && newState == STATE_AUTHENTICATED) {
            return false;
        } else if (oldState == STATE_ERROR && newState == STATE_AUTHENTICATED) {
            return false;
        } else if (newState == STATE_AUTHENTICATING) {
            return false;
        }
        return false;
    }

    private Drawable getAnimationForTransition(int oldState, int newState) {
        if (parentActivity == null) {
            return null;
        }
        int iconRes;
        if (newState == STATE_ERROR) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (oldState == STATE_ERROR && newState == STATE_AUTHENTICATING) {
            iconRes = R.drawable.fingerprint_dialog_error_to_fp;
        } else if (oldState == STATE_AUTHENTICATING && newState == STATE_AUTHENTICATED) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (oldState == STATE_ERROR && newState == STATE_AUTHENTICATED) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else if (newState == STATE_AUTHENTICATING) {
            iconRes = R.drawable.fingerprint_dialog_fp_to_error;
        } else {
            return null;
        }
        return parentActivity.getDrawable(iconRes);
    }

    public void dismiss() {
        if (alertDialog != null) {
            alertDialog.dismiss();
            alertDialog = null;
        }
        if (cancellationSignal != null) {
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    public static boolean hasBiometricEnrolled() {
        if (Build.VERSION.SDK_INT >= 29) {
            BiometricManager biometricManager = ApplicationLoader.applicationContext.getSystemService(BiometricManager.class);
            if (biometricManager == null) {
                return false;
            }
            if (Build.VERSION.SDK_INT >= 30) {
                return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS;
            } else {
                return biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            FingerprintManager fingerprintManager = ApplicationLoader.applicationContext.getSystemService(FingerprintManager.class);
            if (fingerprintManager == null) {
                return false;
            }
            return fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
        }
        return false;
    }
}
