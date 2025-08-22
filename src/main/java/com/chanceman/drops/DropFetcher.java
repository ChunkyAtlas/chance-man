package com.chanceman.drops;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Retrieves NPC drop information from the wiki and
 * resolves item and NPC IDs.
 */
@Slf4j
@Singleton
public class DropFetcher
{
    private final OkHttpClient httpClient;
    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private ExecutorService fetchExecutor;

    @Inject
    public DropFetcher(OkHttpClient httpClient, ItemManager itemManager, ClientThread clientThread)
    {
        this.httpClient = httpClient;
        this.itemManager  = itemManager;
        this.clientThread = clientThread;
        startUp();
    }

    /**
     * Asynchronously fetch an NPC's drop table from the wiki.
     * The lookup first attempts an ID-based redirect and falls back to the given
     * name if necessary.
     */
    public CompletableFuture<NpcDropData> fetch(int npcId, String name, int level)
    {
        return CompletableFuture.supplyAsync(() ->
                {
                    String url = buildWikiUrl(npcId, name);
                    String html = fetchHtml(url);
                    Document doc = Jsoup.parse(html);
                    String actualName = Optional
                            .ofNullable(doc.selectFirst("h1#firstHeading"))
                            .map(Element::text)
                            .orElse(name);
                    int resolvedLevel = level > 0 ? level : parseCombatLevel(doc);
                    int actualId = resolveNpcId(doc);
                    List<DropTableSection> sections = parseSections(doc);
                    if (sections.isEmpty())
                    {
                        return null; // skip NPCs without drop tables
                    }
                    return new NpcDropData(actualId, actualName, resolvedLevel, sections);
                }, fetchExecutor)

                .thenCompose(data ->
                {
                    if (data == null)
                    {
                        return CompletableFuture.completedFuture(null);
                    }
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
                                            return comp != null && comp.getName().equalsIgnoreCase(itemName);
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
     * Extract drop table sections from the provided document.
     */
    private List<DropTableSection> parseSections(Document doc)
    {
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
                    .map(td -> new DropItem(
                            0,
                            td.get(1).text().replace("(m)", "").trim(),
                            td.get(3).text().trim()))
                    .collect(Collectors.toList());

            if (!items.isEmpty())
            {
                sections.add(new DropTableSection(header, items));
            }
        }

        return sections;
    }

    /**
     * Attempt to parse the combat level from the NPC infobox.
     */
    private int parseCombatLevel(Document doc)
    {
        Element infobox = doc.selectFirst("table.infobox");
        if (infobox == null)
        {
            return 0;
        }
        Elements rows = infobox.select("tr");
        for (Element row : rows)
        {
            Element th = row.selectFirst("th");
            Element td = row.selectFirst("td");
            if (th != null && td != null && th.text().toLowerCase(Locale.ROOT).contains("combat level"))
            {
                String txt = td.text();
                for (String part : txt.split("[^0-9]+"))
                {
                    if (!part.isEmpty())
                    {
                        try
                        {
                            return Integer.parseInt(part);
                        }
                        catch (NumberFormatException ignore)
                        {
                        }
                    }
                }
            }
        }
        return 0;
    }

    /**
     * Resolve the canonical wiki page ID for the provided document.
     *
     * @param doc parsed wiki HTML
     * @return numeric page ID, or {@code 0} if it could not be determined
     */
    private int resolveNpcId(Document doc)
    {
        Element link = doc.selectFirst("link[rel=canonical]");
        if (link == null)
        {
            return 0;
        }

        String href = link.attr("href");
        String title = href.substring(href.lastIndexOf('/') + 1);
        // Canonical links may already be URL-encoded; decode first then re-encode
        title = URLDecoder.decode(title, StandardCharsets.UTF_8);
        title = title.replace(' ', '_');
        String apiUrl = "https://oldschool.runescape.wiki/api.php?action=query&format=json&prop=info&titles="
                + URLEncoder.encode(title, StandardCharsets.UTF_8);

        Request req = new Request.Builder()
                .url(apiUrl)
                .header("User-Agent", "RuneLite-ChanceMan/2.6.1")
                .build();

        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                log.warn("Failed to resolve NPC ID for {}: HTTP {}", title, res.code());
                return 0;
            }

            String body = res.body().string();
            JsonElement root = new JsonParser().parse(body);
            JsonElement pages = root.getAsJsonObject()
                    .getAsJsonObject("query")
                    .getAsJsonObject("pages");

            for (Map.Entry<String, JsonElement> entry : pages.getAsJsonObject().entrySet())
            {
                JsonElement page = entry.getValue();
                if (page.getAsJsonObject().has("pageid"))
                {
                    return page.getAsJsonObject().get("pageid").getAsInt();
                }
            }

            log.warn("No page ID found for title {}", title);
        }
        catch (IOException ex)
        {
            log.warn("Error resolving NPC ID for {}", title, ex);
        }

        return 0;
    }

    /**
     * Query the wiki's search API for NPC names matching the provided text.
     */
    public List<String> searchNpcNames(String query)
    {
        String url = "https://oldschool.runescape.wiki/api.php?action=opensearch&format=json&limit=20&namespace=0&search="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "RuneLite-ChanceMan/2.6.1")
                .build();
        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful())
            {
                throw new IOException("HTTP " + res.code());
            }
            String body = res.body().string();
            JsonArray arr = new JsonParser().parse(body).getAsJsonArray();
            JsonArray titles = arr.get(1).getAsJsonArray();
            List<String> names = new ArrayList<>();
            for (JsonElement el : titles)
            {
                names.add(el.getAsString());
            }
            return names;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    private String buildWikiUrl(int npcId, String name)
    {
        String fallback = URLEncoder.encode(name.replace(' ', '_'), StandardCharsets.UTF_8);
        StringBuilder url = new StringBuilder("https://oldschool.runescape.wiki/w/Special:Lookup?type=npc");

        if (npcId > 0)
        {
            url.append("&id=").append(npcId);
        }

        if (!fallback.isEmpty())
        {
            url.append("&name=").append(fallback);
        }

        url.append("#Drops");
        return url.toString();
    }

    private String fetchHtml(String url)
    {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "RuneLite-ChanceMan/2.6.1")
                .build();
        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful()) throw new IOException("HTTP " + res.code());
            return res.body().string();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Creates the fetch executor if it is missing or has been shut down.
     */
    public void startUp()
    {
        if (fetchExecutor == null || fetchExecutor.isShutdown() || fetchExecutor.isTerminated())
        {
            fetchExecutor = Executors.newFixedThreadPool(
                    4,
                    new ThreadFactoryBuilder().setNameFormat("dropfetch-%d").build()
            );
        }
    }

    /**
     * Shut down the executor service.
     */
    public void shutdown()
    {
        if (fetchExecutor != null)
        {
            fetchExecutor.shutdownNow();
            fetchExecutor = null;
        }
    }
}