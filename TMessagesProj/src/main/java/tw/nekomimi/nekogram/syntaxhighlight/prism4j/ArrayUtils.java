package tw.nekomimi.nekogram.syntaxhighlight.prism4j;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

abstract class ArrayUtils {

    @SafeVarargs
    @NonNull
    static <T> List<T> toList(T... args) {
        final int length = args != null
                ? args.length
                : 0;
        final List<T> list = new ArrayList<>(length);
        if (length > 0) {
            Collections.addAll(list, args);
        }
        return list;
    }

    private ArrayUtils() {
    }
}
