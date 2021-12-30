/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.ui.Cells;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.palette.graphics.Palette;

import org.telegram.PhoneFormat.PhoneFormat;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ImageLocation;
import org.telegram.messenger.ImageReceiver;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.AudioPlayerAlert;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.BackupImageView;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.RLottieDrawable;
import org.telegram.ui.Components.RLottieImageView;
import org.telegram.ui.Components.SnowflakesEffect;

import tw.nekomimi.nekogram.NekoConfig;

public class DrawerProfileCell extends FrameLayout {

    private BackupImageView avatarImageView;
    private TextView nameTextView;
    private AudioPlayerAlert.ClippingTextViewSwitcher phoneTextView;
    private ImageView shadowView;
    private ImageView arrowView;
    private RLottieImageView darkThemeView;
    private RLottieDrawable sunDrawable;

    private Rect srcRect = new Rect();
    private Rect destRect = new Rect();
    private Paint paint = new Paint();
    private Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Integer currentColor;
    private Integer currentMoonColor;
    private SnowflakesEffect snowflakesEffect;
    private boolean accountsShown;
    private int darkThemeBackgroundColor;
    public static boolean switchingTheme;

    private final ImageReceiver imageReceiver;
    private Bitmap lastBitmap;
    private boolean avatarAsDrawerBackground = false;

    public DrawerProfileCell(Context context) {
        super(context);

        imageReceiver = new ImageReceiver(this);
        imageReceiver.setCrossfadeWithOldImage(true);
        imageReceiver.setForceCrossfade(true);
        imageReceiver.setDelegate((imageReceiver, set, thumb, memCache) -> {
            if (NekoConfig.avatarBackgroundDarken || NekoConfig.avatarBackgroundBlur) {
                if (thumb) {
                    return;
                }
                ImageReceiver.BitmapHolder bmp = imageReceiver.getBitmapSafe();
                if (bmp != null) {
                    new Thread(() -> {
                        int width = NekoConfig.avatarBackgroundBlur ? 150 : bmp.bitmap.getWidth();
                        int height = NekoConfig.avatarBackgroundBlur ? 150 : bmp.bitmap.getHeight();
                        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        Canvas canvas = new Canvas(bitmap);
                        canvas.drawBitmap(bmp.bitmap, null, new Rect(0, 0, width, height), new Paint(Paint.FILTER_BITMAP_FLAG));
                        if (NekoConfig.avatarBackgroundBlur) {
                            try {
                                Utilities.stackBlurBitmap(bitmap, 3);
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        }
                        if (NekoConfig.avatarBackgroundDarken) {
                            final Palette palette = Palette.from(bmp.bitmap).generate();
                            Paint paint = new Paint();
                            paint.setColor((palette.getDarkMutedColor(0xFF547499) & 0x00FFFFFF) | 0x44000000);
                            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
                        }
                        AndroidUtilities.runOnUIThread(() -> {
                            if (lastBitmap != null) {
                                imageReceiver.setCrossfadeWithOldImage(false);
                                imageReceiver.setImageBitmap(new BitmapDrawable(null, lastBitmap), false);
                            }
                            imageReceiver.setCrossfadeWithOldImage(true);
                            imageReceiver.setImageBitmap(new BitmapDrawable(null, bitmap));
                            lastBitmap = bitmap;
                        });
                    }).start();
                }
            } else {
                lastBitmap = null;
            }
        });

        shadowView = new ImageView(context);
        shadowView.setVisibility(INVISIBLE);
        shadowView.setScaleType(ImageView.ScaleType.FIT_XY);
        shadowView.setImageResource(R.drawable.bottom_shadow);
        addView(shadowView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 70, Gravity.LEFT | Gravity.BOTTOM));

        avatarImageView = new BackupImageView(context);
        avatarImageView.getImageReceiver().setRoundRadius(AndroidUtilities.dp(32));
        addView(avatarImageView, LayoutHelper.createFrame(64, 64, Gravity.LEFT | Gravity.BOTTOM, 16, 0, 0, 67));

        nameTextView = new TextView(context);
        nameTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        nameTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        nameTextView.setLines(1);
        nameTextView.setMaxLines(1);
        nameTextView.setSingleLine(true);
        nameTextView.setGravity(Gravity.LEFT);
        nameTextView.setEllipsize(TextUtils.TruncateAt.END);
        addView(nameTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 16, 0, 76, 28));

        phoneTextView = new AudioPlayerAlert.ClippingTextViewSwitcher(context) {
            @Override
            protected TextView createTextView() {
                TextView textView = new TextView(context);
                textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
                textView.setLines(1);
                textView.setMaxLines(1);
                textView.setSingleLine(true);
                textView.setGravity(Gravity.LEFT);
                return textView;
            }
        };
        addView(phoneTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT | Gravity.BOTTOM, 16, 0, 76, 9));

        arrowView = new ImageView(context);
        arrowView.setScaleType(ImageView.ScaleType.CENTER);
        arrowView.setImageResource(R.drawable.menu_expand);
        addView(arrowView, LayoutHelper.createFrame(59, 59, Gravity.RIGHT | Gravity.BOTTOM));
        setArrowState(false);

        sunDrawable = new RLottieDrawable(R.raw.sun, "" + R.raw.sun, AndroidUtilities.dp(28), AndroidUtilities.dp(28), true, null);
        if (Theme.isCurrentThemeDay()) {
            sunDrawable.setCustomEndFrame(36);
        } else {
            sunDrawable.setCustomEndFrame(0);
            sunDrawable.setCurrentFrame(36);
        }
        sunDrawable.setPlayInDirectionOfCustomEndFrame(true);
        darkThemeView = new RLottieImageView(context) {
            @Override
            public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(info);
                if (sunDrawable.getCustomEndFrame() != 0) {
                    info.setText(LocaleController.getString("AccDescrSwitchToNightTheme", R.string.AccDescrSwitchToNightTheme));
                } else {
                    info.setText(LocaleController.getString("AccDescrSwitchToDayTheme", R.string.AccDescrSwitchToDayTheme));
                }
            }
        };
        sunDrawable.beginApplyLayerColors();
        int color = Theme.getColor(Theme.key_chats_menuName);
        sunDrawable.setLayerColor("Sunny.**", color);
        sunDrawable.setLayerColor("Path 6.**", color);
        sunDrawable.setLayerColor("Path.**", color);
        sunDrawable.setLayerColor("Path 5.**", color);
        sunDrawable.commitApplyLayerColors();
        darkThemeView.setScaleType(ImageView.ScaleType.CENTER);
        darkThemeView.setAnimation(sunDrawable);
        if (Build.VERSION.SDK_INT >= 21) {
            darkThemeView.setBackgroundDrawable(Theme.createSelectorDrawable(darkThemeBackgroundColor = Theme.getColor(Theme.key_listSelector), 1, AndroidUtilities.dp(17)));
            Theme.setRippleDrawableForceSoftware((RippleDrawable) darkThemeView.getBackground());
        }
        darkThemeView.setOnClickListener(v -> {
            if (switchingTheme) {
                return;
            }
            switchingTheme = true;
            SharedPreferences preferences = ApplicationLoader.applicationContext.getSharedPreferences("themeconfig", Activity.MODE_PRIVATE);
            String dayThemeName = preferences.getString("lastDayTheme", "Blue");
            if (Theme.getTheme(dayThemeName) == null || Theme.getTheme(dayThemeName).isDark()) {
                dayThemeName = "Blue";
            }
            String nightThemeName = preferences.getString("lastDarkTheme", "Dark Blue");
            if (Theme.getTheme(nightThemeName) == null || !Theme.getTheme(nightThemeName).isDark()) {
                nightThemeName = "Dark Blue";
            }
            Theme.ThemeInfo themeInfo = Theme.getActiveTheme();
            if (dayThemeName.equals(nightThemeName)) {
                if (themeInfo.isDark() || dayThemeName.equals("Dark Blue") || dayThemeName.equals("Night")) {
                    dayThemeName = "Blue";
                } else {
                    nightThemeName = "Dark Blue";
                }
            }

            boolean toDark;
            if (toDark = dayThemeName.equals(themeInfo.getKey())) {
                themeInfo = Theme.getTheme(nightThemeName);
                sunDrawable.setCustomEndFrame(36);
            } else {
                themeInfo = Theme.getTheme(dayThemeName);
                sunDrawable.setCustomEndFrame(0);
            }
            darkThemeView.playAnimation();
            if (Theme.selectedAutoNightType != Theme.AUTO_NIGHT_TYPE_NONE) {
                Toast.makeText(getContext(), LocaleController.getString("AutoNightModeOff", R.string.AutoNightModeOff), Toast.LENGTH_SHORT).show();
                Theme.selectedAutoNightType = Theme.AUTO_NIGHT_TYPE_NONE;
                Theme.saveAutoNightThemeConfig();
                Theme.cancelAutoNightThemeCallbacks();
            }
            switchTheme(themeInfo, toDark);
        });
        addView(darkThemeView, LayoutHelper.createFrame(48, 48, Gravity.RIGHT | Gravity.BOTTOM, 0, 0, 6, 90));

        if (Theme.getEventType() == 0 || NekoConfig.eventType == 1) {
            snowflakesEffect = new SnowflakesEffect(0);
            snowflakesEffect.setColorKey(Theme.key_chats_menuName);
        }
    }

    private void switchTheme(Theme.ThemeInfo themeInfo, boolean toDark) {
        int[] pos = new int[2];
        darkThemeView.getLocationInWindow(pos);
        pos[0] += darkThemeView.getMeasuredWidth() / 2;
        pos[1] += darkThemeView.getMeasuredHeight() / 2;
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.needSetDayNightTheme, themeInfo, false, pos, -1, toDark, darkThemeView);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateColors();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (Build.VERSION.SDK_INT >= 21) {
            super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(148) + AndroidUtilities.statusBarHeight, MeasureSpec.EXACTLY));
        } else {
            try {
                super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(148), MeasureSpec.EXACTLY));
            } catch (Exception e) {
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), AndroidUtilities.dp(148));
                FileLog.e(e);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable backgroundDrawable = Theme.getCachedWallpaper();
        String backgroundKey = applyBackground(false);
        boolean useImageBackground = !backgroundKey.equals(Theme.key_chats_menuTopBackground) && Theme.isCustomTheme() && !Theme.isPatternWallpaper() && backgroundDrawable != null && !(backgroundDrawable instanceof ColorDrawable) && !(backgroundDrawable instanceof GradientDrawable);
        boolean drawCatsShadow = false;
        int color;
        int darkBackColor = 0;
        if (!avatarAsDrawerBackground && !useImageBackground && Theme.hasThemeKey(Theme.key_chats_menuTopShadowCats)) {
            color = Theme.getColor(Theme.key_chats_menuTopShadowCats);
            drawCatsShadow = true;
        } else {
            if (Theme.hasThemeKey(Theme.key_chats_menuTopShadow)) {
                color = Theme.getColor(Theme.key_chats_menuTopShadow);
            } else {
                color = Theme.getServiceMessageColor() | 0xff000000;
            }
        }
        if (currentColor == null || currentColor != color) {
            currentColor = color;
            shadowView.getDrawable().setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
        }
        color = Theme.getColor(Theme.key_chats_menuName);
        if (currentMoonColor == null || currentMoonColor != color) {
            currentMoonColor = color;
            sunDrawable.beginApplyLayerColors();
            sunDrawable.setLayerColor("Sunny.**", currentMoonColor);
            sunDrawable.setLayerColor("Path 6.**", currentMoonColor);
            sunDrawable.setLayerColor("Path.**", currentMoonColor);
            sunDrawable.setLayerColor("Path 5.**", currentMoonColor);
            sunDrawable.commitApplyLayerColors();
        }
        nameTextView.setTextColor(Theme.getColor(Theme.key_chats_menuName));
        if (avatarAsDrawerBackground || useImageBackground) {
            phoneTextView.getTextView().setTextColor(Theme.getColor(Theme.key_chats_menuPhone));
            if (shadowView.getVisibility() != VISIBLE) {
                shadowView.setVisibility(VISIBLE);
            }
            if (avatarAsDrawerBackground) {
                imageReceiver.setImageCoords(0, 0, getWidth(), getHeight());
                imageReceiver.draw(canvas);
                darkBackColor = Theme.getColor(Theme.key_listSelector);
            } else if (backgroundDrawable instanceof ColorDrawable || backgroundDrawable instanceof GradientDrawable) {
                backgroundDrawable.setBounds(0, 0, getMeasuredWidth(), getMeasuredHeight());
                backgroundDrawable.draw(canvas);
                darkBackColor = Theme.getColor(Theme.key_listSelector);
            } else if (backgroundDrawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) backgroundDrawable).getBitmap();
                float scaleX = (float) getMeasuredWidth() / (float) bitmap.getWidth();
                float scaleY = (float) getMeasuredHeight() / (float) bitmap.getHeight();
                float scale = Math.max(scaleX, scaleY);
                int width = (int) (getMeasuredWidth() / scale);
                int height = (int) (getMeasuredHeight() / scale);
                int x = (bitmap.getWidth() - width) / 2;
                int y = (bitmap.getHeight() - height) / 2;
                srcRect.set(x, y, x + width, y + height);
                destRect.set(0, 0, getMeasuredWidth(), getMeasuredHeight());
                try {
                    canvas.drawBitmap(bitmap, srcRect, destRect, paint);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
                darkBackColor = (Theme.getServiceMessageColor() & 0x00ffffff) | 0x50000000;
            }
        } else {
            int visibility = drawCatsShadow? VISIBLE : INVISIBLE;
            if (shadowView.getVisibility() != visibility) {
                shadowView.setVisibility(visibility);
            }
            phoneTextView.getTextView().setTextColor(Theme.getColor(Theme.key_chats_menuPhoneCats));
            super.onDraw(canvas);
            darkBackColor = Theme.getColor(Theme.key_listSelector);
        }

        if (darkBackColor != 0) {
            if (darkBackColor != darkThemeBackgroundColor) {
                backPaint.setColor(darkThemeBackgroundColor = darkBackColor);
                if (Build.VERSION.SDK_INT >= 21) {
                    Theme.setSelectorDrawableColor(darkThemeView.getBackground(), darkThemeBackgroundColor = darkBackColor, true);
                }
            }
            if (!avatarAsDrawerBackground && useImageBackground && backgroundDrawable instanceof BitmapDrawable) {
                canvas.drawCircle(darkThemeView.getX() + darkThemeView.getMeasuredWidth() / 2, darkThemeView.getY() + darkThemeView.getMeasuredHeight() / 2, AndroidUtilities.dp(17), backPaint);
            }
        }

        if (snowflakesEffect != null) {
            snowflakesEffect.onDraw(this, canvas);
        }
    }

    public boolean isInAvatar(float x, float y) {
        if (avatarAsDrawerBackground) {
            return y <= arrowView.getTop();
        } else {
            return x >= avatarImageView.getLeft() && x <= avatarImageView.getRight() && y >= avatarImageView.getTop() && y <= avatarImageView.getBottom();
        }
    }

    public boolean hasAvatar() {
        return avatarImageView.getImageReceiver().hasNotThumb();
    }

    public boolean isAccountsShown() {
        return accountsShown;
    }

    public void setAccountsShown(boolean value, boolean animated) {
        if (accountsShown == value) {
            return;
        }
        accountsShown = value;
        setArrowState(animated);
    }

    public void setUser(TLRPC.User user, boolean accounts) {
        if (user == null) {
            return;
        }
        accountsShown = accounts;
        setArrowState(false);
        nameTextView.setText(UserObject.getUserName(user));
        if (!NekoConfig.hidePhone) {
            phoneTextView.setText(PhoneFormat.getInstance().format("+" + user.phone));
        } else if (!TextUtils.isEmpty(user.username)) {
            phoneTextView.setText("@" + user.username);
        } else {
            phoneTextView.setText(LocaleController.getString("MobileHidden",R.string.MobileHidden));
        }
        AvatarDrawable avatarDrawable = new AvatarDrawable(user);
        avatarDrawable.setColor(Theme.getColor(Theme.key_avatar_backgroundInProfileBlue));
        avatarImageView.setForUserOrChat(user, avatarDrawable);
        if (NekoConfig.avatarAsDrawerBackground) {
            ImageLocation imageLocation = ImageLocation.getForUser(user, ImageLocation.TYPE_BIG);
            avatarAsDrawerBackground = imageLocation != null;
            imageReceiver.setImage(imageLocation, "512_512", null, null, new ColorDrawable(0x00000000), 0, null, user, 1);
            avatarImageView.setVisibility(INVISIBLE);
        } else {
            avatarAsDrawerBackground = false;
            avatarImageView.setVisibility(VISIBLE);
        }

        applyBackground(true);
    }

    public String applyBackground(boolean force) {
        String currentTag = (String) getTag();
        String backgroundKey = Theme.hasThemeKey(Theme.key_chats_menuTopBackground) && Theme.getColor(Theme.key_chats_menuTopBackground) != 0 ? Theme.key_chats_menuTopBackground : Theme.key_chats_menuTopBackgroundCats;
        if (force || !backgroundKey.equals(currentTag)) {
            setBackgroundColor(Theme.getColor(backgroundKey));
            setTag(backgroundKey);
        }
        return backgroundKey;
    }

    public void updateColors() {
        if (snowflakesEffect != null) {
            snowflakesEffect.updateColors();
        }
    }

    private void setArrowState(boolean animated) {
        final float rotation = accountsShown ? 180.0f : 0.0f;
        if (animated) {
            arrowView.animate().rotation(rotation).setDuration(220).setInterpolator(CubicBezierInterpolator.EASE_OUT).start();
        } else {
            arrowView.animate().cancel();
            arrowView.setRotation(rotation);
        }
        arrowView.setContentDescription(accountsShown ? LocaleController.getString("AccDescrHideAccounts", R.string.AccDescrHideAccounts) : LocaleController.getString("AccDescrShowAccounts", R.string.AccDescrShowAccounts));
    }

}
