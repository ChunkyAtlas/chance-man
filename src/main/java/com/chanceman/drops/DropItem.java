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

    // Anchored patterns for correctness & speed
    private static final Pattern PCT       = Pattern.compile("^(\\d+(?:\\.\\d+)?)%$");
    private static final Pattern MULT      = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*[xX]\\s*(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)$");
    private static final Pattern FRAC      = Pattern.compile("^(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+(?:\\.\\d+)?)$");
    private static final Pattern PAREN     = Pattern.compile("\\s*\\([^)]*\\)$", Pattern.UNICODE_CASE);
    private static final Pattern IN_SYNT   = Pattern.compile("\\bin\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BRACKETS  = Pattern.compile("\\[[^\\]]*\\]");
    private static final Pattern ONE_OVER  = Pattern.compile("1/(\\d+(?:\\.\\d+)?)");

    public DropItem(int itemId, String name, String rarity)
    {
        this.itemId = itemId;
        this.name = name;
        this.rarity = rarity;
    }

    /** Convert raw rarity to normalized one-over form (preserves ranges like “1/64–1/32”). */
    public String getOneOverRarity()
    {
        if (rarity == null) return "";
        String[] parts = rarity.split("\\s*;\\s*|,\\s+");
        return Arrays.stream(parts)
                .map(this::normalizeSegment)
                .collect(Collectors.joining("; "));
    }

    /**
     * Parse rarity and return the denominator (e.g., “1/128” -> 128).
     * Unknown values sort as rarest (POSITIVE_INFINITY). “Always” -> 0.
     */
    public double getRarityValue()
    {
        String oneOver = getOneOverRarity();
        if (oneOver.isEmpty())
        {
            return Double.POSITIVE_INFINITY;
        }

        Matcher m = ONE_OVER.matcher(oneOver);
        if (m.find())
        {
            try { return Double.parseDouble(m.group(1)); }
            catch (NumberFormatException ignored) { return Double.POSITIVE_INFINITY; }
        }

        if (oneOver.equalsIgnoreCase("Always"))
        {
            return 0d;
        }

        return Double.POSITIVE_INFINITY;
    }

    private String normalizeSegment(String raw)
    {
        String cleaned = raw == null ? "" : raw;
        cleaned = BRACKETS.matcher(cleaned).replaceAll("");
        cleaned = cleaned
                .replace("×", "x")
                .replace(",", "")
                .replace("≈", "")
                .replace("~", "")
                .replaceAll(PAREN.pattern(), "")
                .replaceAll(IN_SYNT.pattern(), "/")
                .trim();

        // Handle ranges like "1/128 – 1/64"
        String[] range = cleaned.split("\\s*[–—-]\\s*");
        if (range.length > 1)
        {
            return Arrays.stream(range)
                    .map(this::simplifySingle)
                    .collect(Collectors.joining("–"));
        }

        return simplifySingle(cleaned);
    }

    private String simplifySingle(String s)
    {
        if (s == null || s.isEmpty())
        {
            return "";
        }

        Matcher m;

        // 12.5%
        m = PCT.matcher(s);
        if (m.matches())
        {
            double pct = safeDouble(m.group(1));
            if (pct == 0) return "0";
            return formatOneOver(100.0 / pct);
        }

        // 2 x 1 / 128
        m = MULT.matcher(s);
        if (m.matches())
        {
            double factor = safeDouble(m.group(1));
            double a = safeDouble(m.group(2));
            double b = safeDouble(m.group(3));
            if (factor != 0 && a != 0)
            {
                return formatOneOver(b / (a * factor));
            }
        }

        // 1/128
        m = FRAC.matcher(s);
        if (m.matches())
        {
            double a = safeDouble(m.group(1));
            double b = safeDouble(m.group(2));
            if (a != 0)
            {
                return formatOneOver(b / a);
            }
        }

        // fallback to cleaned input
        return s;
    }

    private double safeDouble(String s)
    {
        try { return Double.parseDouble(s); }
        catch (Exception e) { return Double.NaN; }
    }

    private String formatOneOver(double val)
    {
        if (Double.isNaN(val) || Double.isInfinite(val))
        {
            return "";
        }
        if (Math.abs(val - Math.round(val)) < 0.01)
        {
            return "1/" + Math.round(val);
        }
        return String.format(Locale.ROOT, "1/%.2f", val);
    }
}
