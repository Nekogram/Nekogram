package tw.nekomimi.nekogram.helpers;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.AlertsCreator;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.LinkSpanDrawable;

import java.util.ArrayList;

public class QrHelper {

    public static void showQrDialog(BaseFragment fragment, Theme.ResourcesProvider resourcesProvider, ArrayList<String> qrResults) {
        showQrDialog(fragment, resourcesProvider, qrResults, false);
    }

    public static void showQrDialog(BaseFragment fragment, Theme.ResourcesProvider resourcesProvider, ArrayList<String> qrResults, boolean dark) {
        if (fragment == null || qrResults.isEmpty()) {
            return;
        }
        if (qrResults.size() == 1) {
            var text = qrResults.get(0);
            if (text.startsWith("http://") || text.startsWith("https://")) {
                AlertsCreator.showOpenUrlAlert(fragment, text, true, true, dark ? null : resourcesProvider);
                return;
            }
        }
        var context = fragment.getParentActivity();
        var ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        var dialog = new AlertDialog.Builder(context, resourcesProvider)
                .setView(ll)
                .create();
        if (dark) {
            dialog.setBackgroundColor(0xff1C2229);
        }

        for (int i = 0; i < qrResults.size(); i++) {
            var text = qrResults.get(i);
            var username = Browser.extractUsername(text);
            var linkOrUsername = username != null || text.startsWith("http://") || text.startsWith("https://");
            var textView = new LinkSpanDrawable.LinksTextView(context, dark ? null : resourcesProvider);
            textView.setDisablePaddingsOffsetY(true);
            textView.setTextColor(dark ? linkOrUsername ? 0xff79c4fc : 0xffffffff : linkOrUsername ? Theme.getColor(Theme.key_dialogTextLink, resourcesProvider) : Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            textView.setLinkTextColor(dark ? 0xff79c4fc : Theme.getColor(Theme.key_dialogTextLink, resourcesProvider));
            textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            textView.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            textView.setMaxLines(0);
            textView.setSingleLine(false);
            textView.setPadding(AndroidUtilities.dp(21), AndroidUtilities.dp(10), AndroidUtilities.dp(21), AndroidUtilities.dp(10));
            textView.setBackground(dark ? Theme.createSelectorDrawable(0x0fffffff, Theme.RIPPLE_MASK_ALL) : Theme.getSelectorDrawable(false));
            if (linkOrUsername) {
                SpannableStringBuilder sb = new SpannableStringBuilder(text);
                sb.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        if (text.startsWith("http://") || text.startsWith("https://")) {
                            AlertsCreator.showOpenUrlAlert(fragment, text, true, false, dark ? null : resourcesProvider);
                        }
                    }
                }, 0, text.length(), 0);
                textView.setOnLinkLongPressListener(span -> textView.performLongClick());
                textView.setText(sb);
            } else {
                textView.setText(text);
                textView.setOnClickListener(v1 -> {
                    AndroidUtilities.addToClipboard(text);
                    BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                });
            }
            textView.setOnLongClickListener(v -> {
                BottomSheet.Builder builder = new BottomSheet.Builder(context, false, resourcesProvider);
                builder.setTitle(username != null ? "@" + username : text);
                builder.setItems(linkOrUsername ? new CharSequence[]{LocaleController.getString(R.string.Open), LocaleController.getString(R.string.ShareFile), LocaleController.getString(R.string.Copy)} : new CharSequence[]{null, null, null, LocaleController.getString(R.string.Copy)}, (d, which) -> {
                    if (which == 0) {
                        AlertsCreator.showOpenUrlAlert(fragment, text, true, false, dark ? null : resourcesProvider);
                    } else if (which == 1 || which == 2) {
                        String url1 = text;
                        boolean tel = false;
                        if (url1.startsWith("mailto:")) {
                            url1 = url1.substring(7);
                        } else if (url1.startsWith("tel:")) {
                            url1 = url1.substring(4);
                            tel = true;
                        }
                        if (which == 2) {
                            AndroidUtilities.addToClipboard(url1);
                            String bulletinMessage;
                            if (tel) {
                                bulletinMessage = LocaleController.getString(R.string.PhoneCopied);
                            } else if (url1.startsWith("#")) {
                                bulletinMessage = LocaleController.getString(R.string.HashtagCopied);
                            } else if (url1.startsWith("@")) {
                                bulletinMessage = LocaleController.getString(R.string.UsernameCopied);
                            } else {
                                bulletinMessage = LocaleController.getString(R.string.LinkCopied);
                            }
                            if (AndroidUtilities.shouldShowClipboardToast()) {
                                BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createSimpleBulletin(R.raw.voip_invite, bulletinMessage).show();
                            }
                        } else {
                            Intent shareIntent = new Intent(Intent.ACTION_SEND);
                            shareIntent.setType("text/plain");
                            shareIntent.putExtra(Intent.EXTRA_TEXT, url1);
                            Intent chooserIntent = Intent.createChooser(shareIntent, LocaleController.getString(R.string.ShareFile));
                            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            ApplicationLoader.applicationContext.startActivity(chooserIntent);
                        }
                    } else {
                        BulletinFactory.of(Bulletin.BulletinWindow.make(context), resourcesProvider).createCopyBulletin(LocaleController.getString(R.string.TextCopied)).show();
                    }
                });
                BottomSheet bottomSheet = builder.create();
                if (dark) {
                    bottomSheet.scrollNavBar = true;
                    bottomSheet.show();
                    bottomSheet.setItemColor(0, 0xffffffff, 0xffffffff);
                    bottomSheet.setItemColor(1, 0xffffffff, 0xffffffff);
                    bottomSheet.setItemColor(2, 0xffffffff, 0xffffffff);
                    bottomSheet.setBackgroundColor(0xff1C2229);
                    bottomSheet.fixNavigationBar(0xff1C2229);
                    bottomSheet.setTitleColor(0xff8A8A8A);
                    bottomSheet.setCalcMandatoryInsets(true);
                    AndroidUtilities.setNavigationBarColor(bottomSheet.getWindow(), 0xff1C2229, false);
                    AndroidUtilities.setLightNavigationBar(bottomSheet.getWindow(), false);
                    bottomSheet.scrollNavBar = true;
                } else {
                    bottomSheet.show();
                }
                return true;
            });
            ll.addView(textView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        fragment.showDialog(dialog);
    }

    public static ArrayList<String> readQr(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            return new ArrayList<>();
        }
        ArrayList<String> results = new ArrayList<>(readQrInternal(bitmap));
        Bitmap inverted = null;
        try {
            if (results.isEmpty()) {
                inverted = invert(bitmap);
                results.addAll(readQrInternal(inverted));
                AndroidUtilities.recycleBitmap(inverted);
            }
        } catch (Throwable ignored) {
        }
        try {
            if (inverted != null && results.isEmpty()) {
                Bitmap monochrome = monochrome(inverted);
                results.addAll(readQrInternal(monochrome));
                AndroidUtilities.recycleBitmap(monochrome);
            }
        } catch (Throwable ignored) {
        }
        return results;
    }

    private static QRCodeMultiReader qrReader;
    private static BarcodeDetector visionQrReader;

    private static ArrayList<String> readQrInternal(Bitmap bitmap) {
        ArrayList<String> results = new ArrayList<>();
        try {
            if (visionQrReader == null) {
                visionQrReader = new BarcodeDetector.Builder(ApplicationLoader.applicationContext).setBarcodeFormats(Barcode.QR_CODE).build();
            }
            if (visionQrReader.isOperational()) {
                Frame frame = new Frame.Builder().setBitmap(bitmap).build();
                SparseArray<Barcode> codes = visionQrReader.detect(frame);
                for (int i = 0; i < codes.size(); i++) {
                    Barcode code = codes.valueAt(i);
                    results.add(code.rawValue);
                }
            } else {
                if (qrReader == null) {
                    qrReader = new QRCodeMultiReader();
                }

                int[] intArray = new int[bitmap.getWidth() * bitmap.getHeight()];
                bitmap.getPixels(intArray, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
                LuminanceSource source = new RGBLuminanceSource(bitmap.getWidth(), bitmap.getHeight(), intArray);

                Result[] codes;
                try {
                    codes = qrReader.decodeMultiple(new BinaryBitmap(new GlobalHistogramBinarizer(source)));
                } catch (NotFoundException e) {
                    codes = null;
                }
                if (codes != null) {
                    for (var code : codes) {
                        results.add(code.getText());
                    }
                }

            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        return results;
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
}
