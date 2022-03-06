package tw.nekomimi.nekogram.syntaxhighlight;

import android.text.Spannable;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public abstract class Prism4jThemeBase implements Prism4jTheme {

    @ColorInt
    protected static int applyAlpha(@IntRange(from = 0, to = 255) int alpha, @ColorInt int color) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @ColorInt
    protected static int applyAlpha(@FloatRange(from = .0F, to = 1.F) float alpha, @ColorInt int color) {
        return applyAlpha((int) (255 * alpha + .5F), color);
    }

    protected static boolean isOfType(@NonNull String expected, @NonNull String type, @Nullable String alias) {
        return expected.equals(type) || expected.equals(alias);
    }

    private ColorHashMap colorHashMap;

    protected Prism4jThemeBase() {
        updateColors();
    }

    public void updateColors() {
        this.colorHashMap = init();
    }

    @NonNull
    protected abstract ColorHashMap init();

    @ColorInt
    protected int color(@NonNull String language, @NonNull String type, @Nullable String alias) {

        Color color = colorHashMap.get(type);
        if (color == null
                && alias != null) {
            color = colorHashMap.get(alias);
        }

        return color != null
                ? color.color
                : 0;
    }

    @Override
    public void apply(
            @NonNull String language,
            @NonNull Prism4j.Syntax syntax,
            @NonNull Spannable spannable,
            int start,
            int end) {

        final String type = syntax.type();
        final String alias = syntax.alias();

        final int color = color(language, type, alias);
        if (color != 0) {
            applyColor(language, type, alias, color, spannable, start, end);
        }
    }

    @SuppressWarnings("unused")
    protected void applyColor(
            @NonNull String language,
            @NonNull String type,
            @Nullable String alias,
            @ColorInt int color,
            @NonNull Spannable spannable,
            int start,
            int end) {
        spannable.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    protected static class Color {

        @NonNull
        public static Color of(@ColorInt int color) {
            return new Color(color);
        }

        @ColorInt
        protected final int color;

        protected Color(@ColorInt int color) {
            this.color = color;
        }
    }

    protected static class ColorHashMap extends HashMap<String, Color> {

        @NonNull
        protected ColorHashMap add(@ColorInt int color, String name) {
            put(name, Color.of(color));
            return this;
        }

        @NonNull
        protected ColorHashMap add(
                @ColorInt int color,
                @NonNull String name1,
                @NonNull String name2) {
            final Color c = Color.of(color);
            put(name1, c);
            put(name2, c);
            return this;
        }

        @NonNull
        protected ColorHashMap add(
                @ColorInt int color,
                @NonNull String name1,
                @NonNull String name2,
                @NonNull String name3) {
            final Color c = Color.of(color);
            put(name1, c);
            put(name2, c);
            put(name3, c);
            return this;
        }

        @NonNull
        protected ColorHashMap add(@ColorInt int color, String... names) {
            final Color c = Color.of(color);
            for (String name : names) {
                put(name, c);
            }
            return this;
        }
    }
}
