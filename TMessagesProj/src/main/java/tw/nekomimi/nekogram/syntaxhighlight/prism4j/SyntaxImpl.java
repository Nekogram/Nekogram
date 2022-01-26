package tw.nekomimi.nekogram.syntaxhighlight.prism4j;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SyntaxImpl implements Prism4j.Syntax {

    private final String type;
    private final List<? extends Prism4j.Node> children;
    private final String alias;
    private final String matchedString;
    private final boolean greedy;
    private final boolean tokenized;

    public SyntaxImpl(
            @NonNull String type,
            @NonNull List<? extends Prism4j.Node> children,
            @Nullable String alias,
            @NonNull String matchedString,
            boolean greedy,
            boolean tokenized) {
        this.type = type;
        this.children = children;
        this.alias = alias;
        this.matchedString = matchedString;
        this.greedy = greedy;
        this.tokenized = tokenized;
    }

    @Override
    public int textLength() {
        return matchedString.length();
    }

    @Override
    public final boolean isSyntax() {
        return true;
    }

    @NonNull
    @Override
    public String type() {
        return type;
    }

    @NonNull
    @Override
    public List<? extends Prism4j.Node> children() {
        return children;
    }

    @Nullable
    @Override
    public String alias() {
        return alias;
    }

    @NonNull
    @Override
    public String matchedString() {
        return matchedString;
    }

    @Override
    public boolean greedy() {
        return greedy;
    }

    @Override
    public boolean tokenized() {
        return tokenized;
    }

    @Override
    public String toString() {
        return "SyntaxImpl{" +
                "type='" + type + '\'' +
                ", children=" + children +
                ", alias='" + alias + '\'' +
                ", matchedString='" + matchedString + '\'' +
                ", greedy=" + greedy +
                ", tokenized=" + tokenized +
                '}';
    }
}
