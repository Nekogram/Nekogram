package tw.nekomimi.nekogram.syntaxhighlight.prism4j.languages;

import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.pattern;
import static tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j.token;

import androidx.annotation.NonNull;

import tw.nekomimi.nekogram.syntaxhighlight.prism4j.GrammarUtils;
import tw.nekomimi.nekogram.syntaxhighlight.prism4j.Prism4j;

public class Prism_cpp {

    @NonNull
    public static Prism4j.Grammar create(@NonNull Prism4j prism4j) {

        final Prism4j.Grammar cpp = GrammarUtils.extend(
                GrammarUtils.require(prism4j, "c"),
                "cpp",
                token("keyword", pattern(compile("\\b(?:alignas|alignof|asm|auto|bool|break|case|catch|char|char16_t|char32_t|class|compl|const|constexpr|const_cast|continue|decltype|default|delete|do|double|dynamic_cast|else|enum|explicit|export|extern|float|for|friend|goto|if|inline|int|int8_t|int16_t|int32_t|int64_t|uint8_t|uint16_t|uint32_t|uint64_t|long|mutable|namespace|new|noexcept|nullptr|operator|private|protected|public|register|reinterpret_cast|return|short|signed|sizeof|static|static_assert|static_cast|struct|switch|template|this|thread_local|throw|try|typedef|typeid|typename|union|unsigned|using|virtual|void|volatile|wchar_t|while)\\b"))),
                token("operator", pattern(compile("--?|\\+\\+?|!=?|<{1,2}=?|>{1,2}=?|->|:{1,2}|={1,2}|\\^|~|%|&{1,2}|\\|\\|?|\\?|\\*|\\/|\\b(?:and|and_eq|bitand|bitor|not|not_eq|or|or_eq|xor|xor_eq)\\b")))
        );

        // in prism-js cpp is extending c, but c has not booleans... (like classes)
        GrammarUtils.insertBeforeToken(cpp, "function",
                token("boolean", pattern(compile("\\b(?:true|false)\\b")))
        );

        GrammarUtils.insertBeforeToken(cpp, "keyword",
                token("class-name", pattern(compile("(class\\s+)\\w+", CASE_INSENSITIVE), true))
        );

        GrammarUtils.insertBeforeToken(cpp, "string",
                token("raw-string", pattern(compile("R\"([^()\\\\ ]{0,16})\\([\\s\\S]*?\\)\\1\""), false, true, "string"))
        );

        return cpp;
    }
}
