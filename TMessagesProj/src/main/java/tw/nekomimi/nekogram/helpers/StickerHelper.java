package tw.nekomimi.nekogram.helpers;

import android.graphics.Bitmap;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.utils.BitmapsCache;
import org.telegram.ui.Components.AnimatedFileDrawable;
import org.telegram.ui.Components.RLottieDrawable;

import java.io.File;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import app.nekogram.gifski.Gifski;

public class StickerHelper {
    private static final Executor rendererExecutor = Executors.newCachedThreadPool();

    public static void convertStickerFormat(String path, boolean animated, Consumer<String> callback) {
        rendererExecutor.execute(() -> {
            var resultPath = path + ".gif";
            var cacheOptions = new BitmapsCache.CacheOptions();
            var drawable = animated ?
                    new RLottieDrawable(new File(path), 512, 512, cacheOptions, false, null, 0) :
                    new AnimatedFileDrawable(new File(path), true, 0, 0, null, null, null, 0, 0, false, 0, 0, cacheOptions);
            var success = renderToGif(resultPath, drawable, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            if (animated) {
                ((RLottieDrawable) drawable).recycle(false);
            } else {
                ((AnimatedFileDrawable) drawable).recycle();
            }
            if (success) {
                callback.accept(resultPath);
            }
        });
    }

    private static boolean renderToGif(String path, BitmapsCache.Cacheable source, int width, int height) {
        try {
            var fps = source.getFps();
            FileLog.d("start gif rendering for path = " + path + ", width = " + width + ", height = " + height + ", fps = " + fps);
            source.prepareForGenerateCache();
            var settings = new Gifski.Settings();
            settings.setHeight(height);
            settings.setWidth(width);
            settings.setQuality(90);
            settings.setRepeat((short) 0);
            var gifski = new Gifski(settings);
            gifski.setFileOutput(path);
            var framePosition = 0;
            var bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            while (source.getNextFrame(bitmap) == 1) {
                var pts = (double) framePosition / fps;
                gifski.addFrameBitmap(framePosition, bitmap, pts);
                framePosition++;
            }
            bitmap.recycle();
            gifski.finish();
            return true;
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            source.releaseForGenerateCache();
        }
        return false;
    }
}
