package com.chanceman.drops;

import lombok.Value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Minimal parser for the Wiki templates
 */
final class WikiTemplateParser {
    private static final Pattern COMMENT = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private WikiTemplateParser() {
    }

    static Template findFirst(String wikitext, String name) {
        List<Template> matches = findAll(wikitext, name);
        return matches.isEmpty() ? null : matches.get(0);
    }

    static List<Template> findAll(String wikitext, String... names) {
        if (wikitext == null || wikitext.isEmpty() || names.length == 0) {
            return Collections.emptyList();
        }

        Set<String> wanted = new LinkedHashSet<>();
        Arrays.stream(names).map(WikiTemplateParser::normalizeName).forEach(wanted::add);

        String source = COMMENT.matcher(wikitext).replaceAll("");
        List<Template> matches = new ArrayList<>();
        for (int start = source.indexOf("{{"); start >= 0; start = source.indexOf("{{", start + 2)) {
            int nameEnd = findNameEnd(source, start + 2);
            if (!wanted.contains(normalizeName(source.substring(start + 2, nameEnd)))) {
                continue;
            }

            int end = findTemplateEnd(source, start);
            if (end < 0) {
                break;
            }

            Template template = parse(source.substring(start + 2, end - 2));
            if (template != null) {
                matches.add(template);
            }
        }
        return matches;
    }

    private static Template parse(String body) {
        List<String> parts = splitTopLevel(body, '|');
        if (parts.isEmpty()) {
            return null;
        }

        Map<String, String> named = new LinkedHashMap<>();
        List<String> positional = new ArrayList<>();
        for (int i = 1; i < parts.size(); i++) {
            String part = parts.get(i).trim();
            int equals = findTopLevelEquals(part);
            if (equals < 0) {
                positional.add(part);
            } else {
                named.put(normalizeParameterName(part.substring(0, equals)), part.substring(equals + 1).trim());
            }
        }
        return new Template(normalizeName(parts.get(0)), named, positional);
    }

    private static int findNameEnd(String source, int start) {
        int cursor = start;
        while (cursor < source.length()) {
            char c = source.charAt(cursor);
            if (c == '|' || c == '}' || c == '\n' || c == '\r') {
                break;
            }
            cursor++;
        }
        return cursor;
    }

    private static int findTemplateEnd(String source, int start) {
        int depth = 0;
        for (int i = start; i < source.length() - 1; i++) {
            char current = source.charAt(i);
            char next = source.charAt(i + 1);
            if (current == '{' && next == '{') {
                depth++;
                i++;
            } else if (current == '}' && next == '}') {
                depth--;
                i++;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return -1;
    }

    private static List<String> splitTopLevel(String input, char delimiter) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int end;
        while ((end = findTopLevelChar(input, delimiter, start)) >= 0) {
            parts.add(input.substring(start, end));
            start = end + 1;
        }
        parts.add(input.substring(start));
        return parts;
    }

    private static int findTopLevelEquals(String input) {
        return findTopLevelChar(input, '=', 0);
    }

    private static int findTopLevelChar(String input, char target, int start) {
        int templateDepth = 0;
        int linkDepth = 0;
        for (int i = start; i < input.length(); i++) {
            if (i + 1 < input.length()) {
                char current = input.charAt(i);
                char next = input.charAt(i + 1);
                if (current == '{' && next == '{') {
                    templateDepth++;
                    i++;
                    continue;
                }
                if (current == '}' && next == '}' && templateDepth > 0) {
                    templateDepth--;
                    i++;
                    continue;
                }
                if (current == '[' && next == '[') {
                    linkDepth++;
                    i++;
                    continue;
                }
                if (current == ']' && next == ']' && linkDepth > 0) {
                    linkDepth--;
                    i++;
                    continue;
                }
            }
            if (input.charAt(i) == target && templateDepth == 0 && linkDepth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static String normalizeName(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("template:")) {
            normalized = normalized.substring("template:".length());
        }
        return normalized.replace("_", "").replace(" ", "");
    }

    private static String normalizeParameterName(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT).replace("_", "").replace(" ", "");
    }

    @Value
    static class Template {
        String name;
        Map<String, String> named;
        List<String> positional;

        Template(String name, Map<String, String> named, List<String> positional) {
            this.name = name;
            this.named = Collections.unmodifiableMap(new LinkedHashMap<>(named));
            this.positional = Collections.unmodifiableList(new ArrayList<>(positional));
        }

        String get(String key) {
            return named.getOrDefault(normalizeParameterName(key), "");
        }
    }
}