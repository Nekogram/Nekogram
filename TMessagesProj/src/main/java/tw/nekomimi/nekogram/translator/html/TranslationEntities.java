package tw.nekomimi.nekogram.translator.html;

import android.text.TextUtils;

import androidx.core.text.HtmlCompat;

import org.telegram.tgnet.TLObject;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.TranslateAlert2;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeSet;

public final class TranslationEntities {

    public enum Format {ID, BARE, HTML}

    private record Open(String tag, TLRPC.MessageEntity entity, int start) {
    }

    public static ArrayList<TLRPC.TL_textWithEntities> split(TLRPC.TL_textWithEntities source, int limit) {
        ArrayList<TLRPC.TL_textWithEntities> parts = new ArrayList<>();
        if (encode(source, Format.ID).length() <= limit) {
            parts.add(source);
            return parts;
        }
        int start = 0, end = 0;
        for (int i = source.text.indexOf('\n'); ; i = source.text.indexOf('\n', i + 1)) {
            int cut = i < 0 ? source.text.length() : i;
            if (source.entities.stream().noneMatch(e -> e.offset <= cut && cut < e.offset + e.length)) {
                if (end > start && encode(slice(source, start, cut), Format.ID).length() > limit) {
                    parts.add(slice(source, start, end));
                    start = end + 1;
                }
                end = cut;
            }
            if (i < 0) break;
        }
        if (!source.text.substring(start).trim().isEmpty()) {
            parts.add(slice(source, start, source.text.length()));
        }
        return parts;
    }

    private static TLRPC.TL_textWithEntities slice(TLRPC.TL_textWithEntities source, int start, int end) {
        TLRPC.TL_textWithEntities part = new TLRPC.TL_textWithEntities();
        part.text = source.text.substring(start, end);
        for (TLRPC.MessageEntity entity : source.entities) {
            if (entity.offset < start || entity.offset + entity.length > end) continue;
            TLRPC.MessageEntity copy = TLObject.deepCopy(entity, TLRPC.MessageEntity::TLdeserialize);
            copy.offset -= start;
            part.entities.add(copy);
        }
        return part;
    }

    public static TLRPC.TL_textWithEntities join(TLRPC.TL_textWithEntities[] parts) {
        if (parts.length == 1) return parts[0];
        TLRPC.TL_textWithEntities result = new TLRPC.TL_textWithEntities();
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                text.append('\n');
            }
            for (TLRPC.MessageEntity entity : parts[i].entities) {
                entity.offset += text.length();
                result.entities.add(entity);
            }
            text.append(parts[i].text);
        }
        result.text = text.toString();
        return result;
    }

    public static String encode(TLRPC.TL_textWithEntities source, Format format) {
        return encode(source, format, new ArrayList<>());
    }

    private static String encode(TLRPC.TL_textWithEntities source, Format format, ArrayList<TLRPC.MessageEntity> opened) {
        ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(source.entities);
        entities.removeIf(entity -> entity instanceof TLRPC.TL_messageEntityCustomEmoji);
        if (entities.isEmpty()) return source.text;
        TreeSet<Integer> boundaries = new TreeSet<>();
        boundaries.add(0);
        boundaries.add(source.text.length());
        for (TLRPC.MessageEntity entity : entities) {
            boundaries.add(entity.offset);
            boundaries.add(entity.offset + entity.length);
        }
        ArrayList<TLRPC.MessageEntity> ordered = new ArrayList<>(entities);
        ordered.sort(Comparator.comparingInt((TLRPC.MessageEntity e) -> e.offset).thenComparing((a, b) -> Integer.compare(b.length, a.length)));
        ArrayList<TLRPC.MessageEntity> active = new ArrayList<>();
        StringBuilder html = new StringBuilder();
        int previous = 0;
        for (int boundary : boundaries) {
            String text = TextUtils.htmlEncode(source.text.substring(previous, boundary));
            boolean code = active.stream().anyMatch(e -> e instanceof TLRPC.TL_messageEntityPre);
            if (format != Format.BARE) {
                html.append(format == Format.HTML && code ? text : text.replace("\n", "<br>"));
            } else if (!code) {
                html.append(text);
            }
            ArrayList<TLRPC.MessageEntity> next = new ArrayList<>();
            for (TLRPC.MessageEntity entity : ordered) {
                if (entity.offset <= boundary && boundary < entity.offset + entity.length) {
                    next.add(entity);
                }
            }
            int common = 0;
            while (common < active.size() && common < next.size() && active.get(common) == next.get(common)) {
                common++;
            }
            for (int i = active.size() - 1; i >= common; i--) {
                html.append("</").append(tag(active.get(i))).append('>');
            }
            for (int i = common; i < next.size(); i++) {
                TLRPC.MessageEntity entity = next.get(i);
                opened.add(entity);
                html.append('<').append(tag(entity));
                if (format != Format.BARE) {
                    html.append(" id=").append(source.entities.indexOf(entity));
                }
                html.append(format == Format.HTML && literal(entity) ? " translate=no>" : ">");
            }
            active = next;
            previous = boundary;
        }
        return html.toString();
    }

    private static String tag(TLRPC.MessageEntity e) {
        if (e instanceof TLRPC.TL_messageEntityBold) return "b";
        if (e instanceof TLRPC.TL_messageEntityItalic) return "i";
        if (e instanceof TLRPC.TL_messageEntityUnderline) return "u";
        if (e instanceof TLRPC.TL_messageEntityStrike) return "s";
        if (e instanceof TLRPC.TL_messageEntityCode) return "code";
        if (e instanceof TLRPC.TL_messageEntityPre) return "pre";
        if (e instanceof TLRPC.TL_messageEntityBlockquote) return "blockquote";
        if (e instanceof TLRPC.TL_messageEntitySpoiler) return "spoiler";
        return "a";
    }

    private static boolean literal(TLRPC.MessageEntity e) {
        return e instanceof TLRPC.TL_messageEntityCode || e instanceof TLRPC.TL_messageEntityPre || tag(e).equals("a") && !(e instanceof TLRPC.TL_messageEntityTextUrl);
    }

    private static int count(String text, String tag) {
        return (text.length() - text.replace(tag, "").length()) / tag.length();
    }

    public static TLRPC.TL_textWithEntities decode(TLRPC.TL_textWithEntities source, String translated, Format format) {
        TLRPC.TL_textWithEntities result = new TLRPC.TL_textWithEntities();
        if (source.entities.stream().allMatch(entity -> entity instanceof TLRPC.TL_messageEntityCustomEmoji)) {
            result.text = translated;
            return TranslateAlert2.preprocess(source, result);
        }
        HashMap<String, ArrayDeque<TLRPC.MessageEntity>> opened = new HashMap<>();
        if (format == Format.BARE) {
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
            encode(source, format, entities);
            for (TLRPC.MessageEntity entity : entities) {
                opened.computeIfAbsent(tag(entity), key -> new ArrayDeque<>()).add(entity);
            }
            ArrayDeque<TLRPC.MessageEntity> pres = opened.get("pre");
            if (pres != null && (count(translated, "<pre>") != pres.size() || count(translated, "</pre>") != pres.size()))
                return null;
            ArrayDeque<TLRPC.MessageEntity> links = opened.get("a");
            if (links != null && links.size() != count(translated, "<a>")) {
                opened.remove("a");
            }
        }
        StringBuilder text = new StringBuilder();
        ArrayList<Open> stack = new ArrayList<>();
        HtmlCompat.fromHtml("<nya>" + translated + "</nya>", HtmlCompat.FROM_HTML_MODE_COMPACT, null, (opening, tag, output, reader) -> {
            if (!opening || !tag.equals("nya")) return;
            reader.setContentHandler(new DefaultHandler() {
                @Override
                public void characters(char[] ch, int start, int length) {
                    text.append(ch, start, length);
                }

                @Override
                public void startElement(String uri, String local, String name, Attributes attributes) {
                    if (local.equals("br")) {
                        text.append('\n');
                        return;
                    }
                    String id = attributes.getValue("id");
                    ArrayDeque<TLRPC.MessageEntity> queue = opened.get(local);
                    TLRPC.MessageEntity original = queue != null ? queue.poll() : null;
                    if (id != null && id.matches("[0-9]+") && id.length() < 9 && Integer.parseInt(id) < source.entities.size()) {
                        original = source.entities.get(Integer.parseInt(id));
                    }
                    TLRPC.MessageEntity entity;
                    if (original != null) {
                        entity = TLObject.deepCopy(original, TLRPC.MessageEntity::TLdeserialize);
                    } else {
                        entity = switch (local) {
                            case "b", "strong" -> new TLRPC.TL_messageEntityBold();
                            case "i", "em" -> new TLRPC.TL_messageEntityItalic();
                            case "u" -> new TLRPC.TL_messageEntityUnderline();
                            case "s", "del", "strike" -> new TLRPC.TL_messageEntityStrike();
                            case "code" -> new TLRPC.TL_messageEntityCode();
                            case "pre" -> new TLRPC.TL_messageEntityPre();
                            case "blockquote" -> new TLRPC.TL_messageEntityBlockquote();
                            case "spoiler" -> new TLRPC.TL_messageEntitySpoiler();
                            case "a" -> new TLRPC.TL_messageEntityTextUrl();
                            default -> null;
                        };
                        if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                            entity.url = attributes.getValue("href");
                            if (entity.url == null) {
                                entity = null;
                            }
                        }
                        if (entity instanceof TLRPC.TL_messageEntityPre) {
                            entity.language = "";
                        }
                    }
                    stack.add(new Open(local, entity, text.length()));
                    if (format == Format.BARE && original instanceof TLRPC.TL_messageEntityPre) {
                        text.append(source.text, original.offset, original.offset + original.length);
                    }
                }

                @Override
                public void endElement(String uri, String local, String name) {
                    if (local.equals("br") || stack.isEmpty()) return;
                    Open open = stack.remove(stack.size() - 1);
                    if (open.entity != null && text.length() > open.start) {
                        open.entity.offset = open.start;
                        open.entity.length = text.length() - open.start;
                        result.entities.add(open.entity);
                    }
                }
            });
        });
        result.text = text.toString();
        result.entities.sort(Comparator.comparingInt(e -> e.offset));
        return TranslateAlert2.preprocess(source, result);
    }
}