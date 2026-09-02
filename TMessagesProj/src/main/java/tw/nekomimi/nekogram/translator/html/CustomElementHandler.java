/*
 * This is the source code of OctoGram for Android
 * It is licensed under GNU GPL v2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright OctoGram, 2023-2025.
 */

package tw.nekomimi.nekogram.translator.html;

import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;

import org.telegram.ui.Components.AnimatedEmojiSpan;
import org.xml.sax.Attributes;

public class CustomElementHandler implements HTMLTagAttributesHandler.TagHandler {

    private static class BlockquoteMarker {
    }

    private record PreMarker(String language) {
    }

    @Override
    public boolean handleTag(boolean opening, String tag, Editable output, Attributes attributes) {
        if (tag.equalsIgnoreCase("tg-emoji")) {
            if (opening) {
                String emojiIdString = HTMLTagAttributesHandler.getValue(attributes, "emoji-id");
                if (emojiIdString != null) {
                    long documentId = Long.parseLong(emojiIdString);
                    output.setSpan(new AnimatedEmojiSpan(documentId, null), output.length(), output.length(), Spanned.SPAN_MARK_MARK);
                    return true;
                }
            } else {
                AnimatedEmojiSpan obj = getLast(output, AnimatedEmojiSpan.class);
                if (obj != null) {
                    int where = output.getSpanStart(obj);
                    output.removeSpan(obj);
                    if (where != output.length()) {
                        output.setSpan(obj, where, output.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                    return true;
                }
            }
        } else if (tag.equalsIgnoreCase("blockquote")) {
            if (opening) {
                start(output, new BlockquoteMarker());
            } else {
                end(output, BlockquoteMarker.class, new HTMLKeeper.BlockquoteSpan());
            }
            return true;
        } else if (tag.equalsIgnoreCase("tg-pre")) {
            if (opening) {
                start(output, new PreMarker(HTMLTagAttributesHandler.getValue(attributes, "language")));
            } else {
                PreMarker marker = getLast(output, PreMarker.class);
                end(output, PreMarker.class, new HTMLKeeper.PreSpan(marker != null ? marker.language : ""));
            }
            return true;
        }
        return false;
    }

    private void start(Editable output, Object span) {
        output.setSpan(span, output.length(), output.length(), Spanned.SPAN_MARK_MARK);
    }

    private void end(Editable output, Class<?> kind, Object replacement) {
        Object obj = getLast(output, kind);
        if (obj != null) {
            int where = output.getSpanStart(obj);
            output.removeSpan(obj);
            if (where != output.length()) {
                output.setSpan(replacement, where, output.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private <T> T getLast(Editable text, Class<T> kind) {
        T[] objs = text.getSpans(0, text.length(), kind);
        if (objs.length != 0) {
            for (int i = objs.length; i > 0; i--) {
                if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
        }
        return null;
    }
}