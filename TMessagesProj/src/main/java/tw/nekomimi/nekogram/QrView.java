package tw.nekomimi.nekogram;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.text.TextUtils;
import android.view.View;

import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.R;
import org.telegram.messenger.SvgHelper;
import org.telegram.messenger.Utilities;
import org.telegram.ui.Components.AnimatedFloat;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.RLottieDrawable;

import java.util.HashMap;

public class QrView extends View {

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AnimatedFloat contentBitmapAlpha = new AnimatedFloat(1f, this, 0, 2000, CubicBezierInterpolator.EASE_OUT_QUINT);
    private final Paint crossfadeFromPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint crossfadeToPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int crossfadeWidthDp = 120;
    private RLottieDrawable loadingMatrix;
    private Bitmap contentBitmap, oldContentBitmap;
    private String link;
    private final float[] radii = new float[8];

    protected QrView(Context context) {
        super(context);

        crossfadeFromPaint.setShader(new LinearGradient(0, 0, 0, AndroidUtilities.dp(crossfadeWidthDp), new int[]{0xffffffff, 0}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        crossfadeFromPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
        crossfadeToPaint.setShader(new LinearGradient(0, 0, 0, AndroidUtilities.dp(crossfadeWidthDp), new int[]{0, 0xffffffff}, new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        crossfadeToPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        loadingMatrix = new RLottieDrawable(R.raw.qr_matrix, "qr_matrix", AndroidUtilities.dp(200), AndroidUtilities.dp(200));
        loadingMatrix.setMasterParent(this);
        loadingMatrix.setAutoRepeat(1);
        loadingMatrix.setColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY);
        loadingMatrix.start();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            Utilities.themeQueue.postRunnable(() -> prepareContent(w, h));
        }
    }

    private void drawLoading(Canvas canvas) {
        if (loadingMatrix != null) {
            int width = getWidth();
            loadingMatrix.setBounds(16, 16, width - 16, width - 16);
            loadingMatrix.draw(canvas);
            int qrSize = 37;
            int multiple = width / qrSize;
            int size = multiple * qrSize + 32;
            int imageBloks = Math.round((size - 32) / 4.65f / multiple);
            if (imageBloks % 2 != qrSize % 2) {
                imageBloks++;
            }
            int imageSize = imageBloks * multiple - 24;
            int imageX = (size - imageSize) / 2;
            paint.setColor(Color.BLACK);
            QRCodeWriter.drawSideQuads(canvas, 0, 0, paint, 7, multiple, 16, size, .75f, radii, true);
            String svg = RLottieDrawable.readRes(null, R.raw.qr_logo);
            Bitmap icon = SvgHelper.getBitmap(svg, imageSize, imageSize, false);
            canvas.drawBitmap(icon, imageX, imageX, null);
            icon.recycle();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.WHITE);
        AndroidUtilities.rectTmp.set(0, 0, getWidth(), getHeight());
        canvas.drawRoundRect(AndroidUtilities.rectTmp, AndroidUtilities.dp(20), AndroidUtilities.dp(20), paint);

        float crossfadeAlpha = contentBitmapAlpha.set(1f);
        boolean crossfading = crossfadeAlpha > 0 && crossfadeAlpha < 1;

        if (crossfadeAlpha < 1f) {
            if (crossfading) {
                AndroidUtilities.rectTmp.set(0, 0, getWidth(), getHeight());
                canvas.saveLayerAlpha(AndroidUtilities.rectTmp, 255, Canvas.ALL_SAVE_FLAG);
            }
            if (oldContentBitmap != null) {
                canvas.drawBitmap(oldContentBitmap, 0, 0, null);
            } else {
                drawLoading(canvas);
            }
            if (crossfading) {
                float h = AndroidUtilities.dp(crossfadeWidthDp);
                canvas.save();
                canvas.translate(0, -h + (getHeight() + h) * (1f - crossfadeAlpha));
                canvas.drawRect(0, 0, getWidth(), getHeight() + h, crossfadeToPaint);
                canvas.restore();
                canvas.restore();
            }
        }
        if (crossfadeAlpha > 0f) {
            if (crossfading) {
                AndroidUtilities.rectTmp.set(0, 0, getWidth(), getHeight());
                canvas.saveLayerAlpha(AndroidUtilities.rectTmp, 255, Canvas.ALL_SAVE_FLAG);
            }
            if (contentBitmap != null) {
                canvas.drawBitmap(contentBitmap, 0f, 0f, null);
            } else {
                drawLoading(canvas);
            }
            if (crossfading) {
                float h = AndroidUtilities.dp(crossfadeWidthDp);
                canvas.save();
                canvas.translate(0, -h + (getHeight() + h) * (1f - crossfadeAlpha));
                canvas.drawRect(0, -h - getHeight(), getWidth(), getHeight() + h, crossfadeFromPaint);
                canvas.restore();
                canvas.restore();
            }
        }
    }

    public void setData(String link) {
        this.link = link;
        final int w = getWidth(), h = getHeight();
        Utilities.themeQueue.postRunnable(() -> prepareContent(w, h));
        invalidate();
    }

    private Integer hadWidth, hadHeight;
    private String hadLink;
    private boolean firstPrepare = true;

    private void prepareContent(int w, int h) {
        if (w == 0 || h == 0) {
            return;
        }
        if (TextUtils.isEmpty(link)) {
            AndroidUtilities.runOnUIThread(() -> {
                firstPrepare = false;
                if (contentBitmap != null) {
                    Bitmap oldBitmap = contentBitmap;
                    contentBitmap = null;
                    contentBitmapAlpha.set(0, true);
                    if (oldContentBitmap != null) {
                        oldContentBitmap.recycle();
                    }
                    oldContentBitmap = oldBitmap;
                    this.invalidate();
                }
            });
            return;
        }

        if (TextUtils.equals(link, hadLink) && hadWidth != null && hadHeight != null && hadWidth == w && hadHeight == h) {
            return;
        }

        Bitmap qrBitmap = null;
        HashMap<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.MARGIN, 0);
        QRCodeWriter writer = new QRCodeWriter();
        try {
            qrBitmap = writer.encode(link, w, h, hints, null, 0.75f, 0, Color.BLACK);
        } catch (Exception e) {
            FileLog.e(e);
        }
        if (qrBitmap == null) {
            return;
        }

        Bitmap bitmap = qrBitmap;

        hadWidth = w;
        hadHeight = h;
        hadLink = link;

        AndroidUtilities.runOnUIThread(() -> {
            Bitmap oldBitmap = contentBitmap;
            contentBitmap = bitmap;
            if (!firstPrepare) {
                contentBitmapAlpha.set(0, true);
            }
            firstPrepare = false;
            if (oldContentBitmap != null) {
                oldContentBitmap.recycle();
            }
            oldContentBitmap = oldBitmap;

            this.invalidate();
        });
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (loadingMatrix != null) {
            loadingMatrix.stop();
            loadingMatrix.recycle(false);
            loadingMatrix = null;
        }
    }
}