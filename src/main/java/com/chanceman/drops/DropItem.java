package com.chanceman.drops;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Setter
@Getter
public class DropItem
{
    private int itemId;
    private String name;
    private String rarity;
    private static final Pattern PCT    = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final Pattern MULT   = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[xX]\\s*(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern FRAC   = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)");
    private static final Pattern PAREN  = Pattern.compile("\\s*\\([^)]*\\)$", Pattern.UNICODE_CASE);
    private static final Pattern IN_SYNT = Pattern.compile("\\bin\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRACKETS = Pattern.compile("\\[[^\\]]*\\]");

    public DropItem(int itemId, String name, String rarity)
    {
        this.itemId = itemId;
        this.name = name;
        this.rarity = rarity;
    }

    /**
     * Convert a raw rarity string like "2/128" to a simplified
     * one-over form, e.g. "1/64". If no fraction is detected,
     * return the original string.
     */
    public String getOneOverRarity() {
        if (rarity == null) return "";
        String[] parts = rarity.split("\\s*;\\s*|,\\s+");
        return Arrays.stream(parts)
                .map(this::normalizeSegment)
                .collect(Collectors.joining("; "));
    }

    /**
     * Attempt to parse the rarity into a numeric one-over value.
     * <p>
     * For example a rarity of "1/128" will return {@code 128}. Unknown or
     * non-numeric rarities return {@link Double#POSITIVE_INFINITY} so they are
     * treated as the rarest drops when sorting.
     */
    public double getRarityValue()
    {
        String oneOver = getOneOverRarity();
        if (oneOver.isEmpty())
        {
            return Double.POSITIVE_INFINITY;
        }

        Matcher m = Pattern.compile("1/(\\d+(?:\\.\\d+)?)").matcher(oneOver);
        if (m.find())
        {
            try
            {
                return Double.parseDouble(m.group(1));
            }
            catch (NumberFormatException ex)
            {
                return Double.POSITIVE_INFINITY;
            }
        }

        if (oneOver.equalsIgnoreCase("Always"))
        {
            return 0d;
        }

        return Double.POSITIVE_INFINITY;
    }

    private String normalizeSegment(String raw) {
        String cleaned = raw;
        cleaned = BRACKETS.matcher(cleaned).replaceAll("");
        cleaned = cleaned
                .replace("×", "x")
                .replace(",", "")
                .replace("≈", "")
                .replace("~", "")
                .replaceAll(PAREN.pattern(), "")
                .replaceAll(IN_SYNT.pattern(), "/")
                .trim();

        String[] range = cleaned.split("\\s*[–—-]\\s*");
        if (range.length > 1) {
            return Arrays.stream(range)
                    .map(this::simplifySingle)
                    .collect(Collectors.joining("–"));
        }

        return simplifySingle(cleaned);
    }

    private String simplifySingle(String s) {
        Matcher m;

        m = PCT.matcher(s);
        if (m.matches()) {
            double pct = Double.parseDouble(m.group(1));
            if (pct == 0) return "0";
            return formatOneOver(100.0 / pct);
        }

        m = MULT.matcher(s);
        if (m.find()) {
            double factor = Double.parseDouble(m.group(1));
            double a = Double.parseDouble(m.group(2));
            double b = Double.parseDouble(m.group(3));
            return formatOneOver(b / (a * factor));
        }

        m = FRAC.matcher(s);
        if (m.find()) {
            double a = Double.parseDouble(m.group(1));
            double b = Double.parseDouble(m.group(2));
            if (a != 0) {
                return formatOneOver(b / a);
            }
        }

        // fallback to cleaned input
        return s;
    }

    private String formatOneOver(double val) {
        if (Math.abs(val - Math.round(val)) < 0.01) {
            return "1/" + Math.round(val);
        }
        return String.format(Locale.ROOT, "1/%.2f", val);
    }
}