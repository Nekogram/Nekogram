package tw.nekomimi.nekogram.syntaxhighlight;

import android.text.Spannable;

import androidx.annotation.NonNull;

import io.noties.prism4j.Grammar;
import io.noties.prism4j.Prism4j;

public class Prism4jSyntaxHighlight {

    @NonNull
    public static Prism4jSyntaxHighlight create(
            @NonNull Prism4j prism4j,
            @NonNull Prism4jTheme theme) {
        return new Prism4jSyntaxHighlight(prism4j, theme);
    }

    private final Prism4j prism4j;
    private final Prism4jTheme theme;

    protected Prism4jSyntaxHighlight(
            @NonNull Prism4j prism4j,
            @NonNull Prism4jTheme theme) {
        this.prism4j = prism4j;
        this.theme = theme;
    }

    public void highlight(@NonNull String info, @NonNull Spannable spannable, int start, int end) {
        final Grammar grammar = prism4j.grammar(info);
        if (grammar != null) {
            final Prism4jSyntaxVisitor visitor = new Prism4jSyntaxVisitor(info, theme, spannable, start);
            visitor.visit(prism4j.tokenize(spannable.subSequence(start, end).toString(), grammar));
        }
    }

    @NonNull
    protected Prism4j prism4j() {
        return prism4j;
    }

    @NonNull
    protected Prism4jTheme theme() {
        return theme;
    }
}
