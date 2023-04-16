package tw.nekomimi.nekogram.helpers;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.SparseArray;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.CameraScanActivity;

import java.util.ArrayList;

public class QrHelper {

    public static void openCameraScanActivity(BaseFragment fragment) {
        CameraScanActivity.showAsSheet(fragment, true, CameraScanActivity.TYPE_QR, new CameraScanActivity.CameraScanActivityDelegate() {

            @Override
            public void didFindQr(String link) {
                Browser.openUrl(fragment.getParentActivity(), link, true, false);
            }

            @Override
            public boolean processQr(String link, Runnable onLoadEnd) {
                AndroidUtilities.runOnUIThread(onLoadEnd, 750);
                return true;
            }
        });
    }

    public static ArrayList<QrResult> readQr(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return new ArrayList<>();
        }
        ArrayList<QrResult> results = new ArrayList<>(readQrInternal(bitmap));
        Bitmap inverted = null;
        if (results.isEmpty()) {
            inverted = invert(bitmap);
            results.addAll(readQrInternal(inverted));
            AndroidUtilities.recycleBitmap(inverted);
        }
        if (results.isEmpty()) {
            Bitmap monochrome = monochrome(inverted);
            results.addAll(readQrInternal(monochrome));
            AndroidUtilities.recycleBitmap(monochrome);
        }
        return results;
    }

    private static QRCodeMultiReader qrReader;
    private static BarcodeDetector visionQrReader;

    private static ArrayList<QrResult> readQrInternal(Bitmap bitmap) {
        ArrayList<QrResult> results = new ArrayList<>();
        try {
            if (visionQrReader == null) {
                visionQrReader = new BarcodeDetector.Builder(ApplicationLoader.applicationContext).setBarcodeFormats(Barcode.QR_CODE).build();
            }
            if (visionQrReader.isOperational()) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                SparseArray<Barcode> codes = visionQrReader.detect(frame);
                for (int i = 0; i < codes.size(); i++) {
                    Barcode code = codes.valueAt(i);
                    String text = code.rawValue;
                    RectF bounds = new RectF();
                    if (code.cornerPoints.length > 0) {
                        float minX = Float.MAX_VALUE,
                                maxX = Float.MIN_VALUE,
                                minY = Float.MAX_VALUE,
                                maxY = Float.MIN_VALUE;
                        for (Point point : code.cornerPoints) {
                            minX = Math.min(minX, point.x);
                            maxX = Math.max(maxX, point.x);
                            minY = Math.min(minY, point.y);
                            maxY = Math.max(maxY, point.y);
                        }
                        bounds.set(minX, minY, maxX, maxY);
                    }
                    results.add(buildResult(text, bounds, width, height));
                }
            } else {
                if (qrReader == null) {
                    qrReader = new QRCodeMultiReader();
                }

                int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);
                int width = bitmap.getWidth();
                int height = bitmap.getWidth();

                Result[] codes;
                try {
                    codes = qrReader.decodeMultiple(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                } catch (NotFoundException e) {
                    codes = null;
                }
                if (codes != null) {
                    for (var code : codes) {
                        String text = code.getText();
                        RectF bounds = new RectF();
                        var resultPoints = code.getResultPoints();
                        if (resultPoints != null && resultPoints.length > 0) {
                            float minX = Float.MAX_VALUE,
                                    maxX = Float.MIN_VALUE,
                                    minY = Float.MAX_VALUE,
                                    maxY = Float.MIN_VALUE;
                            for (ResultPoint point : resultPoints) {
                                minX = Math.min(minX, point.getX());
                                maxX = Math.max(maxX, point.getX());
                                minY = Math.min(minY, point.getY());
                                maxY = Math.max(maxY, point.getY());
                            }
                            bounds = new RectF();
                            bounds.set(minX, minY, maxX, maxY);
                        }
                        results.add(buildResult(text, bounds, width, height));
                    }
                }

            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return results;
    }

    private static QrResult buildResult(String text, RectF bounds, int width, int height) {
        QrResult result = new QrResult();
        if (!bounds.isEmpty()) {
            int paddingX = AndroidUtilities.dp(25), paddingY = AndroidUtilities.dp(15);
            bounds.set(bounds.left - paddingX, bounds.top - paddingY, bounds.right + paddingX, bounds.bottom + paddingY);
            bounds.set(
                    bounds.left / (float) width, bounds.top / (float) height,
                    bounds.right / (float) width, bounds.bottom / (float) height
            );
        }
        result.bounds = bounds;
        result.text = text;
        return result;
    }

    private static Bitmap invert(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();

        ColorMatrix matrixGrayscale = new ColorMatrix();
        matrixGrayscale.setSaturation(0);
        ColorMatrix matrixInvert = new ColorMatrix();
        matrixInvert.set(new float[]{
                -1.0f, 0.0f, 0.0f, 0.0f, 255.0f,
                0.0f, -1.0f, 0.0f, 0.0f, 255.0f,
                0.0f, 0.0f, -1.0f, 0.0f, 255.0f,
                0.0f, 0.0f, 0.0f, 1.0f, 0.0f
        });
        matrixInvert.preConcat(matrixGrayscale);
        paint.setColorFilter(new ColorMatrixColorFilter(matrixInvert));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return newBitmap;
    }

    private static Bitmap monochrome(Bitmap bitmap) {
        int height = bitmap.getHeight();
        int width = bitmap.getWidth();

        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(newBitmap);
        Paint paint = new Paint();

        paint.setColorFilter(new ColorMatrixColorFilter(createThresholdMatrix(90)));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return newBitmap;
    }

    public static ColorMatrix createThresholdMatrix(int threshold) {
        return new ColorMatrix(new float[]{
                85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                85.f, 85.f, 85.f, 0.f, -255.f * threshold,
                0f, 0f, 0f, 1f, 0f
        });
    }

    public static class QrResult {
        public String text;
        public RectF bounds;
    }
}
