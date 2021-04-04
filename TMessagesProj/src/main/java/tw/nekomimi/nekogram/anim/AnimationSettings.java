package tw.nekomimi.nekogram.anim;

import android.view.animation.Interpolator;

import org.telegram.ui.Components.CubicBezierInterpolator;

public class AnimationSettings {

    static {
        setDefaults();
    }

    public static BubbleMessageAnimationParameters shortTextMessageParams;
    public static BubbleMessageAnimationParameters longTextMessageParams;
    public static BubbleMessageAnimationParameters linkPreviewMessageParams;
    public static BubbleMessageAnimationParameters photoMessageParams;
    public static StickerMessageAnimationParameters emojiMessageParams;
    public static BubbleMessageAnimationParameters voiceMessageParams;
    public static StickerMessageAnimationParameters stickerMessageParams;
    public static GifMessageAnimationParameters gifMessageAnimationParameters;

    public static void setDefaults() {
        shortTextMessageParams = new BubbleMessageAnimationParameters();
        longTextMessageParams = new BubbleMessageAnimationParameters();
        linkPreviewMessageParams = new BubbleMessageAnimationParameters();
        photoMessageParams = new BubbleMessageAnimationParameters();
        emojiMessageParams = new StickerMessageAnimationParameters();
        voiceMessageParams = new BubbleMessageAnimationParameters();
        stickerMessageParams = new StickerMessageAnimationParameters();
        gifMessageAnimationParameters = new GifMessageAnimationParameters();

        voiceMessageParams.duration = 700;
        voiceMessageParams.scaleTiming = new TimingParameters();

        photoMessageParams.duration = 1000;
        photoMessageParams.xPositionTiming = new TimingParameters();
        photoMessageParams.timeAppearTiming = new TimingParameters(.5f, 1f);
        photoMessageParams.colorChangeTiming = new TimingParameters(0f, .5f);
        photoMessageParams.scaleTiming = new TimingParameters();
        photoMessageParams.bubbleShapeTiming = new TimingParameters();
    }

    public static class TimingParameters {
        public float startDelayFraction, endTimeFraction;
        float easingStart, easingEnd;
        private Interpolator interpolator;

        public TimingParameters(float startDelayFraction, float endTimeFraction) {
            this.startDelayFraction = startDelayFraction;
            this.endTimeFraction = endTimeFraction;
            setEasing(.33f, 1f);
        }

        public TimingParameters() {
            startDelayFraction = 0f;
            endTimeFraction = 1f;
            setEasing(.33f, 1f);
        }

        public Interpolator getInterpolator() {
            return interpolator;
        }

        public void setEasing(float start, float end) {
            easingStart = start;
            easingEnd = end;
            interpolator = new CubicBezierInterpolator(easingStart, 0f, 1f - easingEnd, 1f);
        }

        public long scaledDuration(long duration) {
            return Math.round(duration * (endTimeFraction - startDelayFraction));
        }

        public long scaledStartDelay(long duration) {
            return Math.round(duration * startDelayFraction);
        }
    }

    public static class MessageAnimationParameters {
        public long duration = 500;
        public TimingParameters xPositionTiming = new TimingParameters(0f, .5f);
        public TimingParameters yPositionTiming = new TimingParameters();
        public TimingParameters timeAppearTiming = new TimingParameters(0f, .5f);
        public TimingParameters scaleTiming = new TimingParameters(0f, .5f);
    }

    public static class BubbleMessageAnimationParameters extends MessageAnimationParameters {

        public TimingParameters colorChangeTiming = new TimingParameters(0f, .5f);
        public TimingParameters bubbleShapeTiming = new TimingParameters(0f, .5f);
    }

    public static class StickerMessageAnimationParameters extends MessageAnimationParameters {

        public TimingParameters placeholderCrossfadeTiming = new TimingParameters(0f, .25f);
        public TimingParameters reappearTiming = new TimingParameters();

        public StickerMessageAnimationParameters() {
            timeAppearTiming = new TimingParameters(.5f, 1f);
        }
    }

    public static class GifMessageAnimationParameters extends BubbleMessageAnimationParameters {

        public TimingParameters placeholderCrossfadeTiming = new TimingParameters(0f, .25f);
        public TimingParameters reappearTiming = new TimingParameters();

        public GifMessageAnimationParameters() {
            timeAppearTiming = new TimingParameters(.5f, 1f);
        }
    }
}
