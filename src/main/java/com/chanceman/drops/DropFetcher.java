package com.chanceman.drops;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class DropFetcher
{
    private static final OkHttpClient CLIENT = new OkHttpClient();

    public static CompletableFuture<NpcDropData> fetch(int npcId, String name, int level)
    {
        return CompletableFuture.supplyAsync(() -> {
            String url = buildWikiUrl(name);
            log.info("[DropFetcher] Fetching from {}", url);
            String html = fetchHtml(url);
            return parseWithJsoup(npcId, name, level, html);
        });
    }

    private static String buildWikiUrl(String name)
    {
        String title = URLEncoder.encode(name.replace(' ', '_'), StandardCharsets.UTF_8);
        return "https://oldschool.runescape.wiki/w/" + title + "#Drops";
    }

    private static String fetchHtml(String url)
    {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "RuneLite-Client/" + CLIENT.hashCode())
                .build();

        try (Response res = CLIENT.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                throw new IOException("HTTP " + res.code());
            }
            return res.body().string();
        }
        catch (IOException ex)
        {
            log.error("[DropFetcher] HTTP fetch failed", ex);
            throw new RuntimeException(ex);
        }
    }

    private static NpcDropData parseWithJsoup(int npcId, String name, int level, String html)
    {
        Document doc = Jsoup.parse(html);
        Elements tables = doc.select("table.item-drops");
        List<DropTableSection> sections = new ArrayList<>();

        log.info("[DropFetcher] Found {} drop-tables", tables.size());

        for (Element table : tables)
        {
            String header = "Drops";
            Element prev = table.previousElementSibling();
            while (prev != null)
            {
                if (prev.tagName().matches("h[2-4]"))
                {
                    header = prev.text();
                    break;
                }
                prev = prev.previousElementSibling();
            }

            List<DropItem> items = new ArrayList<>();
            for (Element row : table.select("tbody tr"))
            {
                Elements td = row.select("td");
                if (td.size() < 6)
                    continue;

                String imgUrl = td.select("img").attr("src");
                String itemName = td.get(1).text().replace("(m)", "").trim();
                String quantity = td.get(2).text();
                double rarity    = parseRarity(td.get(3).text());
                int ge           = parsePrice(td.get(4).text());
                int ha           = parsePrice(td.get(5).text());

                items.add(new DropItem(0, itemName, quantity, rarity, ge, ha, imgUrl));
            }

            log.info("[DropFetcher] Section '{}' has {} items", header, items.size());
            if (!items.isEmpty())
            {
                sections.add(new DropTableSection(header, items));
            }
        }

        return new NpcDropData(npcId, name, level, sections);
    }

    private static double parseRarity(String text)
    {
        text = text.replace("~", "").trim();
        if (text.equalsIgnoreCase("always"))
            return 1.0;
        if (text.contains("/"))
        {
            String[] f = text.split("/");
            try
            {
                return Double.parseDouble(f[0]) / Double.parseDouble(f[1]);
            }
            catch (NumberFormatException ex) { }
        }
        try
        {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException ex)
        {
            return 0.0;
        }
    }

    private static int parsePrice(String text)
    {
        try
        {
            String num = text.replaceAll("[,\\.]", "");
            return Integer.parseInt(num);
        }
        catch (Exception ex)
        {
            return 0;
        }
    }
}
