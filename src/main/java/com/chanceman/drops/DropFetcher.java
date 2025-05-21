package com.chanceman.drops;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.http.api.item.ItemPrice;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class DropFetcher
{
    private static final OkHttpClient HTTP = new OkHttpClient();

    private final ItemManager   itemManager;
    private final ClientThread  clientThread;
    private final Executor      fetchExecutor;

    @Inject
    public DropFetcher(ItemManager itemManager, ClientThread clientThread)
    {
        this.itemManager  = itemManager;
        this.clientThread = clientThread;
        // optional: custom thread factory so you can debug these threads
        this.fetchExecutor = Executors.newFixedThreadPool(
                2,
                new ThreadFactoryBuilder().setNameFormat("dropfetch-%d").build()
        );
    }

    public CompletableFuture<NpcDropData> fetch(int npcId, String name, int level)
    {
        return CompletableFuture
                .supplyAsync(() ->
                {
                    String url  = buildWikiUrl(name);
                    String html = fetchHtml(url);
                    return parseWithJsoup(npcId, name, level, html);
                }, fetchExecutor)

                .thenCompose(data ->
                {
                    CompletableFuture<NpcDropData> resolved = new CompletableFuture<>();

                    clientThread.invoke(() ->
                    {
                        for (DropTableSection sec : data.getDropTableSections())
                        {
                            for (DropItem d : sec.getItems())
                            {
                                String itemName = d.getName();
                                int resolvedId = itemManager.search(itemName).stream()
                                        .map(ItemPrice::getId)
                                        .filter(id ->
                                        {
                                            ItemComposition comp = itemManager.getItemComposition(id);
                                            return comp != null
                                                    && comp.getName().equalsIgnoreCase(itemName);
                                        })
                                        .findFirst()
                                        .orElse(0);
                                d.setItemId(resolvedId);
                            }
                        }
                        resolved.complete(data);
                    });

                    return resolved;
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
                .header("User-Agent", "RuneLite-Client/" + HTTP.hashCode())
                .build();

        try (Response res = HTTP.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                throw new IOException("HTTP " + res.code());
            }
            return res.body().string();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /** Parses the HTML into names, quantities, rarity, etc. */
    private NpcDropData parseWithJsoup(int npcId, String name, int level, String html)
    {
        Document doc = Jsoup.parse(html);
        Elements tables = doc.select("table.item-drops");
        List<DropTableSection> sections = new ArrayList<>();

        for (Element table : tables)
        {
            // find the section header
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

            List<DropItem> items = table.select("tbody tr").stream()
                    .map(row -> row.select("td"))
                    .filter(td -> td.size() >= 6)
                    .map(td -> {
                        String imgUrl   = td.select("img").attr("src");
                        String itemName = td.get(1).text().replace("(m)", "").trim();
                        String qty      = td.get(2).text();
                        double rarity   = parseRarity(td.get(3).text());
                        int ge          = parsePrice(td.get(4).text());
                        int ha          = parsePrice(td.get(5).text());

                        // itemId = 0 for now; we’ll fill it on the client thread
                        return new DropItem(0, itemName, qty, rarity, ge, ha, imgUrl);
                    })
                    .collect(Collectors.toList());

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
            catch (NumberFormatException ignore) { }
        }
        try
        {
            return Double.parseDouble(text);
        }
        catch (NumberFormatException ignore)
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
        catch (Exception ignore)
        {
            return 0;
        }
    }
}
