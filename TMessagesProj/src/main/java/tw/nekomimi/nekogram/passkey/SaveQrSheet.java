package tw.nekomimi.nekogram.passkey;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.InputType;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BotWebViewVibrationEffect;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.ImageLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaController;
import org.telegram.messenger.R;
import org.telegram.messenger.TelegramQRCodeWriter;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.EditTextCell;
import org.telegram.ui.Components.AvatarDrawable;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TextHelper;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;

public class SaveQrSheet extends BottomSheet {

    private SaveQrSheet(Context context, int currentAccount, Encryptable qr, String title, String qrTitle, Runnable onSaved) {
        super(context, true);
        this.currentAccount = currentAccount;

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
        subTitleView.setText(LocaleController.getString(R.string.PassQRNewPassword));
        linearLayout.addView(subTitleView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 16, 6, 16, 6));

        var passwordField = new EditTextCell(context, LocaleController.getString(R.string.NewPassword), false, false, -1, null);
        passwordField.editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordField.editText.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        passwordField.setBackground(Theme.createRoundRectDrawable(dp(16), Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider)));
        linearLayout.addView(passwordField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.BOTTOM, 12, 12, 12, 6));

        var passwordConfirmField = new EditTextCell(context, LocaleController.getString(R.string.ConfirmNewPassword), false, false, -1, null);
        passwordConfirmField.editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        passwordConfirmField.setBackground(Theme.createRoundRectDrawable(dp(16), Theme.getColor(Theme.key_windowBackgroundWhite, resourcesProvider)));
        linearLayout.addView(passwordConfirmField, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 52, Gravity.BOTTOM, 12, 6, 12, 12));

        passwordField.editText.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_NEXT) {
                passwordConfirmField.editText.requestFocus();
                return true;
            }
            return false;
        });

        var saveView = new ButtonWithCounterView(context, true, resourcesProvider).setRound();
        saveView.setText(LocaleController.getString(R.string.SaveToGallery), false);
        saveView.setOnClickListener(v -> {
            var password = passwordField.editText.getText();
            if (TextUtils.isEmpty(password) || password.length() < 8) {
                AndroidUtilities.shakeViewSpring(passwordField, -6);
                BotWebViewVibrationEffect.APP_ERROR.vibrate();
                return;
            } else if (!TextUtils.equals(password, passwordConfirmField.editText.getText())) {
                AndroidUtilities.shakeViewSpring(passwordConfirmField, -6);
                BotWebViewVibrationEffect.APP_ERROR.vibrate();
                getBulletinFactory().createErrorBulletin(LocaleController.getString(R.string.PasswordDoNotMatch)).show();
                return;
            }
            if ((Build.VERSION.SDK_INT <= 28 || BuildVars.NO_SCOPED_STORAGE) && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (context instanceof Activity activity) {
                    activity.requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4);
                }
                return;
            }
            Utilities.globalQueue.postRunnable(() -> {
                try {
                    var bitmap = createQRBitmap(qrTitle, qr.encryptToUrl(password.toString().getBytes(StandardCharsets.UTF_8)));
                    var cacheFile = new File(AndroidUtilities.getCacheDir(), AndroidUtilities.generateFileName(0, "png"));
                    try (var stream = new FileOutputStream(cacheFile)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    }
                    MediaController.saveFile(cacheFile.toString(), context, 0, null, null, arg -> {
                        dismiss();
                        if (onSaved != null) onSaved.run();
                    });
                } catch (Exception e) {
                    FileLog.e(e);
                    AndroidUtilities.runOnUIThread(() -> getBulletinFactory().createErrorBulletin(e.getMessage()).show());
                }
            });
        });
        linearLayout.addView(saveView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.BOTTOM, 12, 12, 12, 12));

        var scrollView = new ScrollView(context);
        scrollView.addView(linearLayout);
        setCustomView(scrollView);
    }

    public static void show(Context context, int currentAccount, Encryptable qr, String title, String qrTitle, Runnable onSaved) {
        new SaveQrSheet(context, currentAccount, qr, title, qrTitle, onSaved).show();
    }

    private final static int QR_OUTER_PADDING = 80;
    private final static int QR_INNER_PADDING = 48;
    private final static int QR_CARD_CORNER = 32;

    private final static int QR_WIDTH = 1280;
    private final static int QR_CONTENT_WIDTH = QR_WIDTH - (QR_OUTER_PADDING * 2);
    private final static int QR_SIZE = QR_CONTENT_WIDTH;
    private final static int QR_TOP_BOX_HEIGHT = 180;
    private final static int QR_BOTTOM_BOX_HEIGHT = 280;
    private final static int QR_SPACING = 40;
    private final static int QR_HEIGHT = QR_OUTER_PADDING
            + QR_TOP_BOX_HEIGHT
            + QR_SPACING
            + QR_SIZE
            + QR_SPACING
            + QR_BOTTOM_BOX_HEIGHT
            + QR_OUTER_PADDING;

    private final static int QR_AVATAR_SIZE = 120;
    private final static int QR_AVATAR_SPACING = 32;

    private final static int QR_TEXT_SIZE_NAME = 48;
    private final static int QR_TEXT_SIZE_ID = 36;
    private final static int QR_TEXT_SIZE_TITLE = 56;
    private final static int QR_TEXT_SIZE_DESC = 40;
    private final static int QR_TEXT_SIZE_TIME = 32;

    private final TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint strokePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint fillPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US).withZone(ZoneId.systemDefault());

    private Bitmap createQRBitmap(String title, String url) throws WriterException {
        var user = UserConfig.getInstance(currentAccount).getCurrentUser();
        var name = UserObject.getUserName(user);
        var username = UserObject.getPublicUsername(user);

        var bitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT, Bitmap.Config.ARGB_8888);
        var canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(0x1F000000);
        strokePaint.setStrokeWidth(1f);

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(0x06000000);

        var currentY = QR_OUTER_PADDING;

        var topBoxRect = new RectF(QR_OUTER_PADDING, currentY, QR_OUTER_PADDING + QR_CONTENT_WIDTH, currentY + QR_TOP_BOX_HEIGHT);
        canvas.drawRoundRect(topBoxRect, QR_CARD_CORNER, QR_CARD_CORNER, fillPaint);
        canvas.drawRoundRect(topBoxRect, QR_CARD_CORNER, QR_CARD_CORNER, strokePaint);

        var avatarLeft = QR_OUTER_PADDING + QR_INNER_PADDING;
        var avatarTop = currentY + (QR_TOP_BOX_HEIGHT - QR_AVATAR_SIZE) / 2f;

        var avatarBitmap = getAvatarBitmap(user, QR_AVATAR_SIZE);
        if (avatarBitmap != null) {
            drawCircleBitmap(canvas, avatarBitmap, avatarLeft, avatarTop, QR_AVATAR_SIZE);
        } else {
            drawFallbackAvatar(canvas, avatarLeft, avatarTop, QR_AVATAR_SIZE, user);
        }

        var headerTextLeft = avatarLeft + QR_AVATAR_SIZE + QR_AVATAR_SPACING;
        var headerMaxTextWidth = QR_OUTER_PADDING + QR_CONTENT_WIDTH - QR_INNER_PADDING - headerTextLeft;

        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setTextSize(QR_TEXT_SIZE_NAME);
        var nameLayout = createLayout(name, headerMaxTextWidth, 1);
        canvas.save();
        canvas.translate(headerTextLeft, currentY + QR_INNER_PADDING - 12f);
        nameLayout.draw(canvas);
        canvas.restore();

        var subtitle = new StringBuilder();
        if (username != null && !username.isEmpty()) {
            subtitle.append("@").append(username).append("  •  ");
        }
        subtitle.append("ID: ").append(user.id);

        textPaint.setColor(0xFF8E8E93);
        textPaint.setTypeface(null);
        textPaint.setTextSize(QR_TEXT_SIZE_ID);
        var subLayout = createLayout(subtitle.toString(), headerMaxTextWidth, 1);
        canvas.save();
        canvas.translate(headerTextLeft, currentY + QR_TOP_BOX_HEIGHT - QR_INNER_PADDING + 12f - subLayout.getHeight());
        subLayout.draw(canvas);
        canvas.restore();

        currentY += QR_TOP_BOX_HEIGHT + QR_SPACING;

        var hints = new HashMap<EncodeHintType, Object>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);

        var writer = new TelegramQRCodeWriter();
        var qrBitmap = writer.encode(url, QR_SIZE, QR_SIZE, hints, null, 0f, Color.WHITE, Color.BLACK, false);
        drawScaledBitmap(canvas, qrBitmap, QR_OUTER_PADDING, currentY, QR_SIZE, QR_SIZE);

        currentY += QR_SIZE + QR_SPACING;

        var bottomBoxRect = new RectF(QR_OUTER_PADDING, currentY, QR_OUTER_PADDING + QR_CONTENT_WIDTH, currentY + QR_BOTTOM_BOX_HEIGHT);
        canvas.drawRoundRect(bottomBoxRect, QR_CARD_CORNER, QR_CARD_CORNER, fillPaint);
        canvas.drawRoundRect(bottomBoxRect, QR_CARD_CORNER, QR_CARD_CORNER, strokePaint);

        var bottomContentWidth = QR_CONTENT_WIDTH - (QR_INNER_PADDING * 2);

        textPaint.setColor(Color.BLACK);
        textPaint.setTypeface(AndroidUtilities.bold());
        textPaint.setTextSize(QR_TEXT_SIZE_TITLE);
        var titleLayout = createLayout(title, bottomContentWidth, 1);
        canvas.save();
        canvas.translate(QR_OUTER_PADDING + QR_INNER_PADDING, currentY + QR_INNER_PADDING);
        titleLayout.draw(canvas);
        canvas.restore();

        textPaint.setColor(0xFF333333);
        textPaint.setTypeface(null);
        textPaint.setTextSize(QR_TEXT_SIZE_DESC);
        var descLayout = createLayout(LocaleController.getString(R.string.PassQREncryption), bottomContentWidth, 1);
        canvas.save();
        canvas.translate(QR_OUTER_PADDING + QR_INNER_PADDING, currentY + QR_INNER_PADDING + titleLayout.getHeight() + 10f); // 10px spacing
        descLayout.draw(canvas);
        canvas.restore();

        textPaint.setColor(0xFF8E8E93);
        textPaint.setTextSize(QR_TEXT_SIZE_TIME);
        textPaint.setTypeface(Typeface.MONOSPACE);
        String timeText = formatter.format(Instant.now());
        var timeLayout = createLayout(timeText, bottomContentWidth, 1);
        canvas.save();
        canvas.translate(QR_OUTER_PADDING + QR_INNER_PADDING, currentY + QR_BOTTOM_BOX_HEIGHT - QR_INNER_PADDING + 4f - timeLayout.getHeight());
        timeLayout.draw(canvas);
        canvas.restore();

        return bitmap;
    }

    private StaticLayout createLayout(CharSequence text, int width, int maxLines) {
        return StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, Math.max(1, width))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.0f)
                .setIncludePad(false)
                .setMaxLines(maxLines)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
    }

    private void drawCircleBitmap(Canvas canvas, Bitmap source, float x, float y, int size) {
        bitmapPaint.setShader(new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        var radius = size / 2f;
        canvas.save();
        canvas.translate(x, y);
        canvas.drawCircle(radius, radius, radius, bitmapPaint);
        canvas.restore();
    }

    private void drawScaledBitmap(Canvas canvas, Bitmap source, float x, float y, int width, int height) {
        var qrShader = new BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        var matrix = new Matrix();

        var scaleX = (float) width / source.getWidth();
        var scaleY = (float) height / source.getHeight();

        matrix.setScale(scaleX, scaleY);
        matrix.postTranslate(x, y);
        qrShader.setLocalMatrix(matrix);

        bitmapPaint.setShader(qrShader);

        canvas.drawRect(x, y, x + width, y + height, bitmapPaint);
    }

    private void drawFallbackAvatar(Canvas canvas, float x, float y, int size, TLRPC.User user) {
        var avatarDrawable = new AvatarDrawable();
        avatarDrawable.setInfo(user);
        avatarDrawable.setTextSize(Math.round(size * 0.55f));
        avatarDrawable.setBounds(Math.round(x), Math.round(y), Math.round(x + size), Math.round(y + size));
        avatarDrawable.draw(canvas);
    }

    @Nullable
    private Bitmap getAvatarBitmap(TLRPC.User user, int size) {
        var photoPath = user.photo.photo_small;
        if (photoPath != null) {
            var path = FileLoader.getInstance(currentAccount).getPathToAttach(photoPath, true);
            if (path.exists()) {
                return ImageLoader.loadBitmap(path.getAbsolutePath(), null, size, size, false);
            }
        }
        return null;
    }
}
