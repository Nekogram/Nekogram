package tw.nekomimi.nekogram.helpers;

import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.LocaleSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LinkifyPort;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.URLSpanMono;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.URLSpanUserMention;

import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Pattern;

import tw.nekomimi.nekogram.NekoConfig;
import tw.nekomimi.nekogram.syntaxhighlight.SyntaxHighlight;

public class EntitiesHelper {
    private static final Pattern[] PATTERNS = new Pattern[]{
            Pattern.compile("^`{3}(.*?)[\\n\\r](.*?[\\n\\r]?)`{3}", Pattern.MULTILINE | Pattern.DOTALL), // pre
            Pattern.compile("^`{3}[\\n\\r]?(.*?)[\\n\\r]?`{3}", Pattern.MULTILINE | Pattern.DOTALL), // pre
            Pattern.compile("[`]{3}([^`]+)[`]{3}"), // pre
            Pattern.compile("[`]([^`\\n]+)[`]"), // code
            Pattern.compile("[*]{2}([^*\\n]+)[*]{2}"), // bold
            Pattern.compile("[_]{2}([^_\\n]+)[_]{2}"), // italic
            Pattern.compile("[~]{2}([^~\\n]+)[~]{2}"), // strike
            Pattern.compile("[|]{2}([^|\\n]+)[|]{2}"), // spoiler
            Pattern.compile("\\[([^]]+?)]\\(([^\\n]+)\\)")}; // link

    public static CharSequence parseMarkdown(CharSequence text) {
        var message = new CharSequence[]{text};
        parseMarkdown(message, true);
        return message[0];
    }

    public static void parseMarkdown(CharSequence[] message, boolean allowStrike) {
        var spannable = message[0] instanceof Spannable ? (Spannable) message[0] : Spannable.Factory.getInstance().newSpannable(message[0]);
        for (int i = 0; i < PATTERNS.length; i++) {
            if (!allowStrike && i == 6 || !NekoConfig.markdownParseLinks && i == 8) {
                continue;
            }
            var m = PATTERNS[i].matcher(spannable);
            var sources = new ArrayList<String>();
            var destinations = new ArrayList<CharSequence>();
            find:
            while (m.find()) {
                var start = m.start();
                var end = m.end();
                var length = i < 3 ? 3 : i > 3 && i != 8 ? 2 : 1;
                var typefaceSpans = spannable.getSpans(start, end, TypefaceSpan.class);
                for (var typefaceSpan : typefaceSpans) {
                    if (!"monospace".equals(typefaceSpan.getFamily())) {
                        continue;
                    }
                    int spanStart = spannable.getSpanStart(typefaceSpan);
                    int spanEnd = spannable.getSpanEnd(typefaceSpan);
                    if (spanStart < start + length || spanEnd > end - length) {
                        continue find;
                    }
                }

                var destination = new SpannableStringBuilder(spannable.subSequence(m.start(i == 0 ? 2 : 1), m.end(i == 0 ? 2 : 1)));
                switch (i) {
                    case 0:
                    case 1:
                    case 2:
                    case 3:
                        if (i != 3) {
                            destination.setSpan(new LocaleSpan(languageToLocale(i == 0 ? m.group(1) : "")), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        destination.setSpan(new TypefaceSpan("monospace"), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 4:
                        destination.setSpan(new StyleSpan(Typeface.BOLD), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 5:
                        destination.setSpan(new StyleSpan(Typeface.ITALIC), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 6:
                        destination.setSpan(new StrikethroughSpan(), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 7:
                        destination.setSpan(new BackgroundColorSpan(Theme.getColor(Theme.key_chats_archivePullDownBackground)), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                    case 8:
                        destination.setSpan(new URLSpan(m.group(2)), 0, destination.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        break;
                }
                sources.add(m.group(0));
                destinations.add(destination);
            }
            for (int j = 0; j < sources.size(); j++) {
                spannable = (Spannable) TextUtils.replace(spannable, new String[]{sources.get(j)}, new CharSequence[]{destinations.get(j)});
            }
        }
        message[0] = spannable;
    }

    private static Locale languageToLocale(String language) {
        return Locale.forLanguageTag("ng-SH-x-lvariant-lang-" + (TextUtils.isEmpty(language) ? "none" : language));
    }

    private static String localeToLanguage(Locale locale) {
        var variant = locale.getVariant();
        if (TextUtils.isEmpty(variant) || !variant.startsWith("lang_") || variant.length() < 6) {
            return null;
        }
        if ("none".equals(variant.substring(5))) {
            return "";
        }
        return variant.substring(5);
    }

    public static void applyEntities(ArrayList<TLRPC.MessageEntity> entities, SpannableStringBuilder stringBuilder) {
        applyEntities(entities, stringBuilder, 0);
    }

    public static void applyEntities(ArrayList<TLRPC.MessageEntity> entities, SpannableStringBuilder stringBuilder, int start) {
        for (var entity : entities) {
            entity.offset += start;
            if (entity.offset > stringBuilder.length() || entity.offset + entity.length > stringBuilder.length()) {
                continue;
            }
            if (entity instanceof TLRPC.TL_inputMessageEntityMentionName) {
                stringBuilder.setSpan(new URLSpan("tg://user?id=" + ((TLRPC.TL_inputMessageEntityMentionName) entity).user_id.user_id), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityMentionName) {
                stringBuilder.setSpan(new URLSpan("tg://user?id=" + ((TLRPC.TL_messageEntityMentionName) entity).user_id), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityCode || entity instanceof TLRPC.TL_messageEntityPre) {
                if (!TextUtils.isEmpty(entity.language)) {
                    stringBuilder.setSpan(new LocaleSpan(languageToLocale(entity.language)), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    SyntaxHighlight.highlight(entity.language, entity.offset, entity.offset + entity.length, stringBuilder);
                }
                stringBuilder.setSpan(new TypefaceSpan("monospace"), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityBold) {
                stringBuilder.setSpan(new StyleSpan(Typeface.BOLD), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityItalic) {
                stringBuilder.setSpan(new StyleSpan(Typeface.ITALIC), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityStrike) {
                stringBuilder.setSpan(new StrikethroughSpan(), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityUnderline) {
                stringBuilder.setSpan(new UnderlineSpan(), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                stringBuilder.setSpan(new URLSpan(entity.url), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (entity instanceof TLRPC.TL_messageEntitySpoiler) {
                stringBuilder.setSpan(new BackgroundColorSpan(Theme.getColor(Theme.key_chats_archivePullDownBackground)), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    public static void getEntities(Spanned spannable, ArrayList<TLRPC.MessageEntity> entities) {
        var spans = spannable.getSpans(0, spannable.length(), CharacterStyle.class);
        for (var span : spans) {
            if ((spannable.getSpanFlags(span) & Spanned.SPAN_COMPOSING) != 0) {
                continue;
            }
            int spanStart = spannable.getSpanStart(span);
            int spanEnd = spannable.getSpanEnd(span);
            if (span instanceof URLSpan) {
                var urlSpan = (URLSpan) span;
                var url = urlSpan.getURL();
                if (url != null) {
                    if (url.startsWith("tg://user?id=")) {
                        String id = url.replace("tg://user?id=", "");
                        TLRPC.TL_inputMessageEntityMentionName entity = new TLRPC.TL_inputMessageEntityMentionName();
                        entity.user_id = MessagesController.getInstance(UserConfig.selectedAccount).getInputUser(Utilities.parseLong(id));
                        if (entity.user_id != null) {
                            entity.offset = spanStart;
                            entity.length = spanEnd - entity.offset;
                            if (spannable.charAt(entity.offset + entity.length - 1) == ' ') {
                                entity.length--;
                            }
                            entities.add(entity);
                        }
                    } else {
                        TLRPC.TL_messageEntityTextUrl entity = new TLRPC.TL_messageEntityTextUrl();
                        entity.offset = spanStart;
                        entity.length = spanEnd - entity.offset;
                        entity.url = url;
                        entities.add(entity);
                    }
                }
            } else if (span instanceof StyleSpan) {
                StyleSpan styleSpan = (StyleSpan) span;
                switch (styleSpan.getStyle()) {
                    case Typeface.BOLD:
                        entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityBold(), spanStart, spanEnd));
                        break;
                    case Typeface.ITALIC:
                        entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityItalic(), spanStart, spanEnd));
                        break;
                    case Typeface.BOLD_ITALIC:
                        entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityBold(), spanStart, spanEnd));
                        entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityItalic(), spanStart, spanEnd));
                        break;
                }
            } else if (span instanceof TypefaceSpan) {
                var typefaceSpan = (TypefaceSpan) span;
                if ("monospace".equals(typefaceSpan.getFamily())) {
                    var localeSpans = spannable.getSpans(spanStart, spanEnd, LocaleSpan.class);
                    if (localeSpans != null && localeSpans.length > 0 && localeSpans[0].getLocale() != null) {
                        TLRPC.TL_messageEntityPre entity = new TLRPC.TL_messageEntityPre();
                        entity.language = localeToLanguage(localeSpans[0].getLocale());
                        entities.add(setEntityStartEnd(entity, spanStart, spanEnd));
                    } else {
                        entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityCode(), spanStart, spanEnd));
                    }
                }
            } else if (span instanceof UnderlineSpan) {
                entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityUnderline(), spanStart, spanEnd));
            } else if (span instanceof StrikethroughSpan) {
                entities.add(setEntityStartEnd(new TLRPC.TL_messageEntityStrike(), spanStart, spanEnd));
            } else if (span instanceof BackgroundColorSpan) {
                entities.add(setEntityStartEnd(new TLRPC.TL_messageEntitySpoiler(), spanStart, spanEnd));
            }
        }
    }

    public static boolean isEnabled() {
        return NekoConfig.keepFormatting;
    }

    private static TLRPC.MessageEntity setEntityStartEnd(TLRPC.MessageEntity entity, int spanStart, int spanEnd) {
        entity.offset = spanStart;
        entity.length = spanEnd - spanStart;
        return entity;
    }

    public enum Style {
        BOLD,
        ITALIC,
        STRIKE,
        UNDERLINE,
        MONO,
        URL,
        MENTION,
        SPOILER,
        REGULAR,
    }

    public static void addStyleToText(Style style, EditTextCaption editTextCaption, EditTextCaption.EditTextCaptionDelegate delegate, int start, int end, boolean allowTextEntitiesIntersection) {
        var editable = editTextCaption.getText();
        if (editable == null) {
            return;
        }
        var resourcesProvider = editTextCaption.resourcesProvider;
        var context = editTextCaption.getContext();
        if (style == Style.MENTION || style == Style.URL || style == Style.MONO) {
            String title;
            String hint;
            String text;
            if (style == Style.MENTION) {
                title = LocaleController.getString("CreateMention", R.string.CreateMention);
                hint = "ID";
                text = "";
            } else if (style == Style.MONO) {
                title = LocaleController.getString("CreateMono", R.string.CreateMono);
                hint = LocaleController.getString("CreateMonoLanguage", R.string.CreateMonoLanguage);
                text = "";
            } else /*if (style == Style.URL) */ {
                title = LocaleController.getString("CreateLink", R.string.CreateLink);
                hint = LocaleController.getString("URL", R.string.URL);
                text = "https://";
            }
            var builder = new AlertDialog.Builder(context, resourcesProvider);
            builder.setTitle(title);

            var editText = new EditTextBoldCursor(context) {
                @Override
                protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                    super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(AndroidUtilities.dp(64), MeasureSpec.EXACTLY));
                }
            };
            if (style == Style.MENTION) {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
            }
            editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            editText.setText(text);
            editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
            editText.setHintText(hint);
            editText.setHeaderHintColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
            editText.setSingleLine(true);
            editText.setFocusable(true);
            editText.setTransformHintToHeader(true);
            editText.setLineColors(Theme.getColor(Theme.key_windowBackgroundWhiteInputField, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteInputFieldActivated, resourcesProvider), Theme.getColor(Theme.key_windowBackgroundWhiteRedText3, resourcesProvider));
            editText.setImeOptions(EditorInfo.IME_ACTION_DONE);
            editText.setBackground(null);
            editText.requestFocus();
            editText.setPadding(0, 0, 0, 0);
            builder.setView(editText);

            var oldSpans = editable.getSpans(start, end, CharacterStyle.class);
            if (oldSpans != null && oldSpans.length > 0) {
                for (var oldSpan : oldSpans) {
                    if (oldSpan instanceof URLSpan && (style == Style.MENTION || style == Style.URL)) {
                        var url = ((URLSpan) oldSpan).getURL();
                        if (!TextUtils.isEmpty(url)) {
                            if (style == Style.MENTION) {
                                if (url.startsWith("tg://user?id=")) {
                                    editText.setText(url.replace("tg://user?id=", ""));
                                    break;
                                }
                            } else {
                                editText.setText(url);
                                break;
                            }
                        }
                    } else if (oldSpan instanceof LocaleSpan && style == Style.MONO) {
                        var language = localeToLanguage(((LocaleSpan) oldSpan).getLocale());
                        if (!TextUtils.isEmpty(language)) {
                            editText.setText(language);
                            break;
                        }
                    }
                }
            }

            var hasPaste = ClipboardHelper.hasPrimaryClip();
            if (hasPaste) {
                builder.setNeutralButton(LocaleController.getString(android.R.string.paste), null);
            }
            builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
            builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
            AlertDialog dialog = builder.show();
            if (hasPaste) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                    var string = ClipboardHelper.getClipboardText();
                    if (!isValidInput(style, string)) {
                        AndroidUtilities.shakeView(view, 2, 0);
                        return;
                    }
                    editText.setText(string);
                });
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                var string = editText.getText().toString();
                if (!isValidInput(style, string)) {
                    AndroidUtilities.shakeView(editText, 2, 0);
                    return;
                }
                clearSpan(editable, start, end, allowTextEntitiesIntersection && style != Style.MONO);
                try {
                    if (style == Style.MENTION) {
                        editable.setSpan(new URLSpan("tg://user?id=" + string), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    } else if (style == Style.MONO) {
                        editable.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                        if (!TextUtils.isEmpty(string)) {
                            editable.setSpan(new LocaleSpan(languageToLocale(string)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            SyntaxHighlight.highlight(string, start, end, editable);
                        }
                    } else /*if (style == Style.URL) */ {
                        editable.setSpan(new URLSpan(string), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                } catch (Exception ignore) {

                }
                if (delegate != null) {
                    delegate.onSpansChanged();
                }
                dialog.dismiss();
            });
            dialog.setOnShowListener(d -> {
                editText.requestFocus();
                AndroidUtilities.showKeyboard(editText);
            });
            var layoutParams = (ViewGroup.MarginLayoutParams) editText.getLayoutParams();
            if (layoutParams != null) {
                if (layoutParams instanceof FrameLayout.LayoutParams) {
                    ((FrameLayout.LayoutParams) layoutParams).gravity = Gravity.CENTER_HORIZONTAL;
                }
                layoutParams.rightMargin = layoutParams.leftMargin = AndroidUtilities.dp(24);
                layoutParams.height = AndroidUtilities.dp(36);
                editText.setLayoutParams(layoutParams);
            }
            editText.setSelection(0, editText.getText().length());
        } else {
            if (!allowTextEntitiesIntersection) {
                clearSpan(editable, start, end, false);
            }
            if (style == Style.BOLD) {
                editable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style == Style.ITALIC) {
                editable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style == Style.STRIKE) {
                editable.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style == Style.UNDERLINE) {
                editable.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style == Style.SPOILER) {
                editable.setSpan(new BackgroundColorSpan(Theme.getColor(Theme.key_chats_archivePullDownBackground)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (style == Style.REGULAR) {
                clearSpan(editable, start, end, false);
            }
            if (delegate != null) {
                delegate.onSpansChanged();
            }
        }
    }

    private static boolean isValidInput(Style style, String string) {
        if (style == Style.MENTION) {
            return Utilities.parseLong(string) != 0L;
        } else if (style == Style.URL) {
            return LinkifyPort.WEB_URL.matcher(string).matches();
        } else {
            return true;
        }
    }

    private static void clearSpan(Editable editable, int start, int end, boolean url) {
        var spans = url ? editable.getSpans(start, end, URLSpan.class) : editable.getSpans(start, end, CharacterStyle.class);
        if (spans != null && spans.length > 0) {
            for (var span : spans) {
                int spanStart = editable.getSpanStart(span);
                int spanEnd = editable.getSpanEnd(span);
                editable.removeSpan(span);
                if (spanStart < start) {
                    editable.setSpan(span, spanStart, start, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (spanEnd > end) {
                    editable.setSpan(span, end, spanEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
    }

    public static CharSequence commonizeSpans(CharSequence text) {
        if (!(text instanceof Spanned)) {
            return text;
        }
        var spanned = (Spanned) text;
        var spannable = new SpannableStringBuilder(text.toString());
        var spans = spanned.getSpans(0, spanned.length(), CharacterStyle.class);
        for (var span : spans) {
            int start = spanned.getSpanStart(span);
            int end = spanned.getSpanEnd(span);
            if (span instanceof URLSpanMono) {
                var style = ((URLSpanMono) span).getStyle();
                if (style != null && style.urlEntity instanceof TLRPC.TL_messageEntityPre) {
                    spannable.setSpan(new LocaleSpan(languageToLocale(style.urlEntity.language)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                spannable.setSpan(new TypefaceSpan("monospace"), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (span instanceof URLSpanUserMention) {
                spannable.setSpan(new URLSpan("tg://user?id=" + Long.parseLong(((URLSpanUserMention) span).getURL())), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (span instanceof URLSpanReplacement) {
                var url = ((URLSpanReplacement) span).getURL();
                if (url.startsWith("mailto:")) {
                    continue;
                }
                spannable.setSpan(new URLSpan(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (span instanceof TextStyleSpan) {
                var styleFlags = ((TextStyleSpan) span).getStyleFlags();
                if ((styleFlags & TextStyleSpan.FLAG_STYLE_BOLD) > 0) {
                    spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if ((styleFlags & TextStyleSpan.FLAG_STYLE_ITALIC) > 0) {
                    spannable.setSpan(new StyleSpan(Typeface.ITALIC), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if ((styleFlags & TextStyleSpan.FLAG_STYLE_UNDERLINE) > 0) {
                    spannable.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if ((styleFlags & TextStyleSpan.FLAG_STYLE_STRIKE) > 0) {
                    spannable.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (((styleFlags & TextStyleSpan.FLAG_STYLE_SPOILER) > 0)) {
                    spannable.setSpan(new BackgroundColorSpan(Theme.getColor(Theme.key_chats_archivePullDownBackground)), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
        }
        return spannable;
    }
}
