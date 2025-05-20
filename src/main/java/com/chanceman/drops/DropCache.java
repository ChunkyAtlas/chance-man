package com.chanceman.drops;

import static net.runelite.client.RuneLite.RUNELITE_DIR;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.runelite.api.Client;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class DropCache
{
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Client client;

    @Inject
    public DropCache(Client client)
    {
        this.client = client;
    }

    private Path getDropsDir() throws IOException
    {
        String player = client.getLocalPlayer().getName();
        if (player == null) throw new IOException("Player name is null");
        Path dir = RUNELITE_DIR.toPath()
                .resolve("chanceman")
                .resolve(player)
                .resolve("drops");
        Files.createDirectories(dir);
        return dir;
    }

    public CompletableFuture<NpcDropData> get(int npcId, String name, int level)
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                Path dropsDir = getDropsDir();
                String fname = name.replaceAll("[^A-Za-z0-9]", "_") + "_" + level + ".json";
                Path file = dropsDir.resolve(fname);
                if (Files.exists(file))
                {
                    try (Reader r = Files.newBufferedReader(file))
                    {
                        return GSON.fromJson(r, NpcDropData.class);
                    }
                }
                NpcDropData data = DropFetcher.fetch(npcId, name, level).join();
                try (Writer w = Files.newBufferedWriter(file))
                {
                    GSON.toJson(data, w);
                }
                return data;
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to load or cache drop data", e);
            }
        });
    }
}