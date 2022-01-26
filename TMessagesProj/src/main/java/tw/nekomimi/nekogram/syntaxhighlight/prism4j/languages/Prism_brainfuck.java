package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.grammar;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class Prism_brainfuck {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {
        return grammar("brainfuck",
                token("pointer", pattern(compile("<|>"), false, false, "keyword")),
                token("increment", pattern(compile("\\+"), false, false, "inserted")),
                token("decrement", pattern(compile("-"), false, false, "deleted")),
                token("branching", pattern(compile("\\[|\\]"), false, false, "important")),
                token("operator", pattern(compile("[.,]"))),
                token("comment", pattern(compile("\\S+")))
        );
    }
}
