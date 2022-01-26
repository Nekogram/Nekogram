package tw.nekomimi.nekogram.syntaxhighlight.prism4j;

import androidx.annotation.NonNull;

import java.util.List;

public class GrammarImpl implements Prism4j.Grammar {

    private final String name;
    private final List<Prism4j.Token> tokens;

    public GrammarImpl(@NonNull String name, @NonNull List<Prism4j.Token> tokens) {
        this.name = name;
        this.tokens = tokens;
    }

    @NonNull
    @Override
    public String name() {
        return name;
    }

    @NonNull
    @Override
    public List<Prism4j.Token> tokens() {
        return tokens;
    }

    @Override
    public String toString() {
        return ToString.toString(this);
    }
}
