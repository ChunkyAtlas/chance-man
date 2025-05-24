package com.chanceman.drops;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
        this.fetchExecutor = Executors.newFixedThreadPool(
                2,
                new ThreadFactoryBuilder().setNameFormat("dropfetch-%d").build()
        );
    }

    public CompletableFuture<NpcDropData> fetch(int npcId, String name, int level)
    {
        return CompletableFuture
                .supplyAsync(() -> {
                    // resolve page title via NPC ID search
                    Optional<String> maybeTitle = findTitleByNpcId(npcId);
                    String titleToUse = maybeTitle.orElse(name);

                    // build the URL to fetch
                    String urlToFetch = buildWikiUrl(titleToUse);
                    String html;
                    try
                    {
                        html = fetchHtml(urlToFetch);
                    }
                    catch (UncheckedIOException e)
                    {
                        if (maybeTitle.isPresent())
                        {
                            String retryUrl = buildWikiUrl(name);
                            html = fetchHtml(retryUrl);
                            titleToUse = name;
                        }
                        else
                        {
                            throw e;
                        }
                    }
                    return parseWithJsoup(npcId, titleToUse, level, html);
                }, fetchExecutor)

                .thenCompose(data -> {
                    CompletableFuture<NpcDropData> resolved = new CompletableFuture<>();
                    clientThread.invoke(() -> {
                        for (DropTableSection sec : data.getDropTableSections())
                        {
                            for (DropItem d : sec.getItems())
                            {
                                String itemName = d.getName();
                                int resolvedId = itemManager.search(itemName).stream()
                                        .map(ItemPrice::getId)
                                        .filter(id -> {
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

    /**
     * Query the OSRS Wiki search API with the NPC's numeric ID + "Drops",
     * return the first page title if found.
     */
    private static Optional<String> findTitleByNpcId(int npcId)
    {
        String api = "https://oldschool.runescape.wiki/api.php"
                + "?action=query"
                + "&list=search"
                + "&srsearch=" + URLEncoder.encode(npcId + " Drops", StandardCharsets.UTF_8)
                + "&format=json";

        Request req = new Request.Builder()
                .url(api)
                .header("User-Agent", "RuneLite-Client/" + HTTP.hashCode())
                .build();

        try (Response res = HTTP.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                return Optional.empty();
            }
            String json = res.body().string();
            JsonParser parser = new JsonParser();
            JsonObject root = parser.parse(json).getAsJsonObject();

            JsonArray results = root
                    .getAsJsonObject("query")
                    .getAsJsonArray("search");
            if (results.size() > 0)
            {
                String title = results
                        .get(0)
                        .getAsJsonObject()
                        .get("title")
                        .getAsString();
                return Optional.of(title);
            }
        }
        catch (IOException e)
        {
            log.warn("ChanceMan[{}]: ID-search failed: {}", npcId, e.toString());
        }
        return Optional.empty();
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

    private NpcDropData parseWithJsoup(int npcId, String name, int level, String html)
    {
        Document doc = Jsoup.parse(html);
        Elements tables = doc.select("table.item-drops");

        List<DropTableSection> sections = new ArrayList<>();

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

            List<DropItem> items = table.select("tbody tr").stream()
                    .map(row -> row.select("td"))
                    .filter(td -> td.size() >= 6)
                    .map(td -> {
                        String itemName = td.get(1).text().replace("(m)", "").trim();
                        return new DropItem(0, itemName);
                    })
                    .collect(Collectors.toList());

            if (!items.isEmpty())
            {
                sections.add(new DropTableSection(header, items));
            }
        }

        return new NpcDropData(npcId, name, level, sections);
    }
}
