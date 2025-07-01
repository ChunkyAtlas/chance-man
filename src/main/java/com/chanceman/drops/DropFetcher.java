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
    private final OkHttpClient httpClient;
    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private final ExecutorService fetchExecutor;

    @Inject
    public DropFetcher(OkHttpClient httpClient, ItemManager itemManager, ClientThread clientThread)
    {
        this.httpClient = httpClient;
        this.itemManager  = itemManager;
        this.clientThread = clientThread;
        this.fetchExecutor = Executors.newFixedThreadPool(
                2,
                new ThreadFactoryBuilder().setNameFormat("dropfetch-%d").build()
        );
    }

    /**
     * Asynchronously fetches an NPC’s drop tables via the OldSchool RuneScape Wiki’s
     * Special:Lookup endpoint.
     *
     * <p>The lookup URL is constructed as
     * `/w/Special:Lookup?type=npc&amp;id={npcId}&amp;name={fallback}#Drops`,
     * which forces an ID‐first redirect to the exact NPC page (infobox match), and
     * only falls back to the provided name (or a search) if no ID match is found.
     *
     * @param npcId  the numeric NPC ID to look up in the wiki infobox
     * @param name   the NPC name (used as a fallback if the ID lookup fails)
     * @param level  the NPC’s combat level (carried through into the returned data)
     * @return a CompletableFuture that, when complete, yields an NpcDropData
     *         containing the raw HTML‐parsed drop sections and resolved item IDs
     */
    public CompletableFuture<NpcDropData> fetch(int npcId, String name, int level)
    {
        return CompletableFuture.supplyAsync(() -> {
                    String url = buildWikiUrl(npcId, name);
                    String html = fetchHtml(url);
                    List<DropTableSection> sections = parseSections(html);
                    return new NpcDropData(npcId, name, level, sections);
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
     * Extract drop table sections from HTML.
     */
    private List<DropTableSection> parseSections(String html)
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
                    .map(td -> new DropItem(0, td.get(1).text().replace("(m)", "").trim()))
                    .collect(Collectors.toList());

            if (!items.isEmpty())
            {
                sections.add(new DropTableSection(header, items));
            }
        }
        return sections;
    }

    private String buildWikiUrl(int npcId, String name)
    {
        String fallback = URLEncoder.encode(name.replace(' ', '_'), StandardCharsets.UTF_8);
        return "https://oldschool.runescape.wiki/w/Special:Lookup"
                + "?type=npc"
                + "&id="   + npcId
                + "&name=" + fallback
                + "#Drops";
    }

    private String fetchHtml(String url)
    {
        Request req = new Request.Builder()
                .url(url)
                .header("User-Agent", "RuneLite-ChanceMan/2.2.1")
                .build();
        try (Response res = httpClient.newCall(req).execute())
        {
            if (!res.isSuccessful())
                throw new IOException("HTTP " + res.code());
            return res.body().string();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     *  shut down the executor
     */
    public void shutdown()
    {
        fetchExecutor.shutdownNow();
    }
}
