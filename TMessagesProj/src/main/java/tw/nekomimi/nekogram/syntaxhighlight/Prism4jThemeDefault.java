package tw.nekomimi.nekogram.syntaxhighlight;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.Spanned;
import android.text.style.StyleSpan;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.ui.ActionBar.Theme;

public class Prism4jThemeDefault extends Prism4jThemeBase {

    @NonNull
    public static Prism4jThemeDefault create() {
        return new Prism4jThemeDefault();
    }

    @Override
    public int textColor() {
        return 0xdd000000;
    }

    @NonNull
    @Override
    protected ColorHashMap init() {
        return new ColorHashMap()
                .add(Theme.getColor(Theme.key_codehighlight_annotation), "annotation")
                .add(Theme.getColor(Theme.key_codehighlight_atrule), "atrule")
                .add(Theme.getColor(Theme.key_codehighlight_attr_name), "attr-name")
                .add(Theme.getColor(Theme.key_codehighlight_attr_value), "attr-value")
                .add(Theme.getColor(Theme.key_codehighlight_boolean), "boolean")
                .add(Theme.getColor(Theme.key_codehighlight_builtin), "builtin")
                .add(Theme.getColor(Theme.key_codehighlight_cdata), "cdata")
                .add(Theme.getColor(Theme.key_codehighlight_char), "char")
                .add(Theme.getColor(Theme.key_codehighlight_class_name), "class-name")
                .add(Theme.getColor(Theme.key_codehighlight_comment), "comment")
                .add(Theme.getColor(Theme.key_codehighlight_constant), "constant")
                .add(Theme.getColor(Theme.key_codehighlight_deleted), "deleted")
                .add(Theme.getColor(Theme.key_codehighlight_delimiter), "delimiter")
                .add(Theme.getColor(Theme.key_codehighlight_doctype), "doctype")
                .add(Theme.getColor(Theme.key_codehighlight_entity), "entity")
                .add(Theme.getColor(Theme.key_codehighlight_function), "function")
                .add(Theme.getColor(Theme.key_codehighlight_important), "important")
                .add(Theme.getColor(Theme.key_codehighlight_inserted), "inserted")
                .add(Theme.getColor(Theme.key_codehighlight_keyword), "keyword")
                .add(Theme.getColor(Theme.key_codehighlight_number), "number")
                .add(Theme.getColor(Theme.key_codehighlight_operator), "operator")
                .add(Theme.getColor(Theme.key_codehighlight_prolog), "prolog")
                .add(Theme.getColor(Theme.key_codehighlight_property), "property")
                .add(Theme.getColor(Theme.key_codehighlight_punctuation), "punctuation")
                .add(Theme.getColor(Theme.key_codehighlight_regex), "regex")
                .add(Theme.getColor(Theme.key_codehighlight_selector), "selector")
                .add(Theme.getColor(Theme.key_codehighlight_string), "string")
                .add(Theme.getColor(Theme.key_codehighlight_symbol), "symbol")
                .add(Theme.getColor(Theme.key_codehighlight_tag), "tag")
                .add(Theme.getColor(Theme.key_codehighlight_url), "url")
                .add(Theme.getColor(Theme.key_codehighlight_variable), "variable");
    }

    @Override
    protected void applyColor(
            @NonNull String language,
            @NonNull String type,
            @Nullable String alias,
            @ColorInt int color,
            @NonNull Spannable spannable,
            int start,
            int end) {

        super.applyColor(language, type, alias, color, spannable, start, end);

        if (isOfType("important", type, alias)
                || isOfType("bold", type, alias)) {
            spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (isOfType("italic", type, alias)) {
            spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
