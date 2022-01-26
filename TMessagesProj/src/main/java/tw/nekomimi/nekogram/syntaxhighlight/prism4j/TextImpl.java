package tw.nekomimi.nekogram.syntaxhighlight.prism4j;

import androidx.annotation.NonNull;

public class TextImpl implements Prism4j.Text {

    private final String literal;

    public TextImpl(@NonNull String literal) {
        this.literal = literal;
    }

    @Override
    public int textLength() {
        return literal.length();
    }

    @Override
    public final boolean isSyntax() {
        return false;
    }

    @NonNull
    @Override
    public String literal() {
        return literal;
    }

    @Override
    public String toString() {
        return "TextImpl{" +
                "literal='" + literal + '\'' +
                '}';
    }
}
