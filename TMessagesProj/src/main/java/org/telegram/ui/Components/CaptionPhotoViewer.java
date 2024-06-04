package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.ui.ActionBar.Theme.RIPPLE_MASK_CIRCLE_20DP;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.google.android.exoplayer2.util.Util;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Stories.DarkThemeResourceProvider;
import org.telegram.ui.Stories.recorder.CaptionContainerView;
import org.telegram.ui.Stories.recorder.HintView2;

import java.util.Arrays;

public class CaptionPhotoViewer extends CaptionContainerView {

    private boolean addPhotoVisible;
    private final ImageView addPhotoButton;

    private boolean timerVisible;
    private final ImageView timerButton;
    private final PeriodDrawable timerDrawable;
    private ItemOptions timerPopup;

    private int timer = 0;
    private final int SHOW_ONCE = 0x7FFFFFFF;
    private final int[] values = new int[] { SHOW_ONCE, 3, 10, 30, 0, -1};

//    private final BlurringShader.StoryBlurDrawer hintBlur;
    private final HintView2 hint;
    private final Runnable applyCaption;

    @Override
    protected int getEditTextStyle() {
        return EditTextEmoji.STYLE_PHOTOVIEWER;
    }

    public CaptionPhotoViewer(Context context, FrameLayout rootView, SizeNotifierFrameLayout sizeNotifierFrameLayout, FrameLayout containerView, Theme.ResourcesProvider resourcesProvider, BlurringShader.BlurManager blurManager, Runnable applyCaption) {
        super(context, rootView, sizeNotifierFrameLayout, containerView, resourcesProvider, blurManager);
        this.applyCaption = applyCaption;

        addPhotoButton = new ImageView(context);
        addPhotoButton.setImageResource(R.drawable.filled_add_photo);
        addPhotoButton.setScaleType(ImageView.ScaleType.CENTER);
        addPhotoButton.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        addPhotoButton.setBackground(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, RIPPLE_MASK_CIRCLE_20DP, dp(18)));
        setAddPhotoVisible(false, false);
        addView(addPhotoButton, LayoutHelper.createFrame(44, 44, Gravity.LEFT | Gravity.BOTTOM, 14, 0, 0, 10));

        timerButton = new ImageView(context);
        timerButton.setImageDrawable(timerDrawable = new PeriodDrawable());
        timerButton.setBackground(Theme.createSelectorDrawable(Theme.ACTION_BAR_WHITE_SELECTOR_COLOR, RIPPLE_MASK_CIRCLE_20DP, dp(18)));
        timerButton.setScaleType(ImageView.ScaleType.CENTER);
        setTimerVisible(false, false);
        addView(timerButton, LayoutHelper.createFrame(44, 44, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 11, 10));

        hint = new HintView2(context, HintView2.DIRECTION_BOTTOM);
        hint.setRounding(12);
        hint.setPadding(dp(12), 0, dp(12), dp(8));
        hint.setJoint(1, -21);
        hint.setMultilineText(true);
        addView(hint, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 80, Gravity.RIGHT | Gravity.BOTTOM));

        timerButton.setOnClickListener(e -> {
            if (timerPopup != null && timerPopup.isShown()) {
                timerPopup.dismiss();
                timerPopup = null;
                return;
            }
            hint.hide();

            timerPopup = ItemOptions.makeOptions(rootView, new DarkThemeResourceProvider(), timerButton);
            timerPopup.setDimAlpha(0);
            timerPopup.addText(LocaleController.getString(R.string.TimerPeriodHint), 13, dp(200));
            timerPopup.addGap();
            for (int value : values) {
                String text;
                if (value == -1) {
                    text = LocaleController.getString("AutoDeleteCustom", R.string.AutoDeleteCustom);
                    if (Arrays.stream(values).noneMatch(v -> v == this.timer)) {
                        timerPopup.putCheck();
                    }
                } else if (value == 0) {
                    text = LocaleController.getString(R.string.TimerPeriodDoNotDelete);
                } else if (value == SHOW_ONCE) {
                    text = LocaleController.getString(R.string.TimerPeriodOnce);
                } else {
                    text = LocaleController.formatPluralString("Seconds", value);
                }
                timerPopup.add(0, text, () -> changeTimer(value));
                if (this.timer == value) {
                    timerPopup.putCheck();
                }
            }
            timerPopup.show();
        });
    }

    public void setOnAddPhotoClick(View.OnClickListener listener) {
        addPhotoButton.setOnClickListener(listener);
    }

    public void setAddPhotoVisible(boolean visible, boolean animated) {
        addPhotoVisible = visible;
        addPhotoButton.animate().cancel();
        if (animated) {
            addPhotoButton.setVisibility(View.VISIBLE);
            addPhotoButton.animate().alpha(visible ? 1f : 0f).translationX(visible ? 0 : dp(-8)).withEndAction(() -> {
                if (!visible) {
                    timerButton.setVisibility(View.GONE);
                }
            }).start();
        } else {
            addPhotoButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            addPhotoButton.setAlpha(visible ? 1f : 0f);
            addPhotoButton.setTranslationX(visible ? 0 : dp(-8));
        }
        updateEditTextLeft();

        MarginLayoutParams lp = (MarginLayoutParams) editText.getLayoutParams();
        lp.rightMargin = dp(12 + (addPhotoVisible && timerVisible ? 33 : 0));
        editText.setLayoutParams(lp);
    }

    @Override
    protected int getEditTextLeft() {
        return addPhotoVisible ? dp(31) : 0;
    }

    private boolean isVideo;
    public void setIsVideo(boolean isVideo) {
        this.isVideo = isVideo;
    }

    @Override
    protected void onTextChange() {
        if (applyCaption != null) {
            applyCaption.run();
        }
    }

    public void setTimerVisible(boolean visible, boolean animated) {
        timerVisible = visible;
        timerButton.animate().cancel();
        if (animated) {
            timerButton.setVisibility(View.VISIBLE);
            timerButton.animate().alpha(visible ? 1f : 0f).translationX(visible ? 0 : dp(8)).withEndAction(() -> {
                if (!visible) {
                    timerButton.setVisibility(View.GONE);
                }
            }).start();
        } else {
            timerButton.setVisibility(visible ? View.VISIBLE : View.GONE);
            timerButton.setAlpha(visible ? 1f : 0f);
            timerButton.setTranslationX(visible ? 0 : dp(8));
        }

        MarginLayoutParams lp = (MarginLayoutParams) editText.getLayoutParams();
        lp.rightMargin = dp(12 + (addPhotoVisible && timerVisible ? 33 : 0));
        editText.setLayoutParams(lp);
    }

    public boolean hasTimer() {
        return timerVisible && timer > 0;
    }

    public void setTimer(int value) {
        this.timer = value;
        timerDrawable.setValue(timer == SHOW_ONCE ? 1 : Math.max(1, timer), timer > 0, true);
        if (hint != null) {
            hint.hide();
        }
    }

    private void changeTimer(int value) {
        if (this.timer == value) {
            return;
        }
        if (value == -1) {
            Theme.ResourcesProvider resourcesProvider = new DarkThemeResourceProvider();
            Context context = getContext();

            final NumberPicker numberPicker = new NumberPicker(context, resourcesProvider);
            numberPicker.setMinValue(0);
            numberPicker.setMaxValue(28);
            if (timer != 0) {
                if (timer >= 0 && timer < 21) {
                    numberPicker.setValue(timer);
                } else {
                    numberPicker.setValue(21 + timer / 5 - 5);
                }
            }
            numberPicker.setFormatter(value1 -> {
                if (value1 == 0) {
                    return LocaleController.getString("ShortMessageLifetimeForever", R.string.ShortMessageLifetimeForever);
                } else if (value1 >= 1 && value1 < 21) {
                    return LocaleController.formatTTLString(value1);
                } else {
                    return LocaleController.formatTTLString((value1 - 16) * 5);
                }
            });
            AlertDialog.Builder builder = new AlertDialog.Builder(context, resourcesProvider);
            builder.setTitle(LocaleController.getString("MessageLifetime", R.string.MessageLifetime));
            builder.setView(numberPicker);
            builder.setPositiveButton(LocaleController.getString("Done", R.string.Done), (di, a) -> {
                int value1 = numberPicker.getValue();
                builder.getDismissRunnable().run();
                int seconds;
                if (value1 >= 0 && value1 < 21) {
                    seconds = value1;
                } else {
                    seconds = (value1 - 16) * 5;
                }
                changeTimer(seconds);
            });
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (di, a) -> builder.getDismissRunnable().run());
            builder.show();
            return;
        }
        setTimer(value);
        if (onTTLChange != null) {
            onTTLChange.run(value);
        }
        CharSequence text;
        if (value == 0) {
            text = LocaleController.getString(isVideo ? R.string.TimerPeriodVideoKeep : R.string.TimerPeriodPhotoKeep);
            hint.setMaxWidthPx(getMeasuredWidth());
            hint.setMultilineText(false);
            hint.setInnerPadding(13, 4, 10, 4);
            hint.setIconMargin(0);
            hint.setIconTranslate(0, -dp(1));
        } else if (value == SHOW_ONCE) {
            text = LocaleController.getString(isVideo ? R.string.TimerPeriodVideoSetOnce : R.string.TimerPeriodPhotoSetOnce);
            hint.setMaxWidthPx(getMeasuredWidth());
            hint.setMultilineText(false);
            hint.setInnerPadding(13, 4, 10, 4);
            hint.setIconMargin(0);
            hint.setIconTranslate(0, -dp(1));
        } else if (value > 0) {
            text = AndroidUtilities.replaceTags(LocaleController.formatPluralString(isVideo ? "TimerPeriodVideoSetSeconds" : "TimerPeriodPhotoSetSeconds", value));
            hint.setMultilineText(true);
            hint.setMaxWidthPx(HintView2.cutInFancyHalf(text, hint.getTextPaint()));
            hint.setInnerPadding(12, 7, 11, 7);
            hint.setIconMargin(2);
            hint.setIconTranslate(0, 0);
        } else {
            return;
        }
        hint.setTranslationY(-Math.min(dp(34), getEditTextHeight()) - dp(14));
        hint.setText(text);
        final int iconResId = value > 0 ? R.raw.fire_on : R.raw.fire_off;
        RLottieDrawable icon = new RLottieDrawable(iconResId, "" + iconResId, dp(34), dp(34));
        icon.start();
        hint.setIcon(icon);
        hint.show();
    }

    @Override
    protected void onEditHeightChange(int height) {
        hint.setTranslationY(-Math.min(dp(34), height) - dp(10));
    }

    @Override
    protected boolean clipChild(View child) {
        return child != hint;
    }

    private Utilities.Callback<Integer> onTTLChange;
    public void setOnTimerChange(Utilities.Callback<Integer> onTTLChange) {
        this.onTTLChange = onTTLChange;
    }

    @Override
    protected int getCaptionLimit() {
        return UserConfig.getInstance(currentAccount).isPremium() ? getCaptionPremiumLimit() : getCaptionDefaultLimit();
    }

    @Override
    protected int getCaptionDefaultLimit() {
        return MessagesController.getInstance(currentAccount).captionLengthLimitDefault;
    }

    @Override
    protected int getCaptionPremiumLimit() {
        return MessagesController.getInstance(currentAccount).captionLengthLimitPremium;
    }

    @Override
    protected void beforeUpdateShownKeyboard(boolean show) {
        if (!show) {
            timerButton.setVisibility(timerVisible ? View.VISIBLE : View.GONE);
            addPhotoButton.setVisibility(addPhotoVisible ? View.VISIBLE : View.GONE);
        }
        if (hint != null) {
            hint.hide();
        }
    }

    @Override
    protected void onUpdateShowKeyboard(float keyboardT) {
        timerButton.setAlpha(1f - keyboardT);
        addPhotoButton.setAlpha(1f - keyboardT);
    }

    @Override
    protected void afterUpdateShownKeyboard(boolean show) {
        timerButton.setVisibility(!show && timerVisible ? View.VISIBLE : View.GONE);
        addPhotoButton.setVisibility(!show && addPhotoVisible ? View.VISIBLE : View.GONE);
        if (show) {
            timerButton.setVisibility(View.GONE);
            addPhotoButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected int additionalKeyboardHeight() {
        return 0;
    }

    @Override
    public void updateColors(Theme.ResourcesProvider resourcesProvider) {
        super.updateColors(resourcesProvider);
        timerDrawable.updateColors(0xffffffff, Theme.getColor(Theme.key_chat_editMediaButton, resourcesProvider), 0xffffffff);
    }

    @Override
    protected void setupMentionContainer() {

    }
}
