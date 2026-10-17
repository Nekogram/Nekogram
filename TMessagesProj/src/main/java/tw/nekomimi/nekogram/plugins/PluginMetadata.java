package tw.nekomimi.nekogram.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginMetadata {
    private static final Pattern METADATA_PATTERN = Pattern.compile(
            "(?m)^\\s*__([a-zA-Z_]+)__\\s*=\\s*([\\\"'])(.*?)\\2\\s*(?:#.*)?$");
    private static final Pattern ID_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{1,31}$");
    private static final Pattern REQUIREMENTS_PATTERN = Pattern.compile("(?ms)^\\s*__requirements__\\s*=\\s*\\[(.*?)\\]");
    private static final Pattern REQUIREMENT_STRING_PATTERN = Pattern.compile("([\\\"'])(.*?)\\1");

    public final String id;
    public final String name;
    public final String version;
    public final String author;
    public final String description;
    public final List<String> requirements;

    private PluginMetadata(String id, String name, String version, String author, String description, List<String> requirements) {
        this.id = id;
        this.name = name;
        this.version = version;
        this.author = author;
        this.description = description;
        this.requirements = Collections.unmodifiableList(requirements);
    }

    public static PluginMetadata fromSource(String source) {
        if (source.startsWith("\uFEFF")) {
            source = source.substring(1);
        }
        String id = readValue(source, "id");
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Plugin must define a valid __id__ (2-32 letters, digits, '_' or '-').");
        }
        String name = readValue(source, "name");
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Plugin must define a non-empty __name__.");
        }
        return new PluginMetadata(
                id,
                name.trim(),
                nonEmpty(readValue(source, "version"), "0.0.0"),
                nonEmpty(readValue(source, "author"), ""),
                nonEmpty(readValue(source, "description"), ""),
                readRequirements(source)
        );
    }

    private static List<String> readRequirements(String source) {
        ArrayList<String> requirements = new ArrayList<>();
        Matcher listMatcher = REQUIREMENTS_PATTERN.matcher(source);
        if (!listMatcher.find()) {
            return requirements;
        }
        Matcher itemMatcher = REQUIREMENT_STRING_PATTERN.matcher(listMatcher.group(1));
        while (itemMatcher.find()) {
            String requirement = itemMatcher.group(2).trim();
            if (!requirement.isEmpty()) {
                requirements.add(requirement.replace("\\'", "'").replace("\\\"", "\""));
            }
        }
        return requirements;
    }

    private static String readValue(String source, String key) {
        Matcher matcher = METADATA_PATTERN.matcher(source);
        while (matcher.find()) {
            if (matcher.group(1).toLowerCase(Locale.ROOT).equals(key)) {
                return matcher.group(3).replace("\\'", "'").replace("\\\"", "\"");
            }
        }
        return null;
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
