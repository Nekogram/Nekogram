package tw.nekomimi.nekogram.syntaxhighlight.prism4j;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Set;

/**
 * Basic class to locate grammars
 *
 * @see Prism4j#Prism4j(GrammarLocator)
 */
public interface GrammarLocator {

    @Nullable
    Prism4j.Grammar grammar(@NonNull Prism4j prism4j, @NonNull String language);

    /**
     * @return collection of languages included into this locator
     * @since 1.1.0
     */
    @NonNull
    Set<String> languages();
}
