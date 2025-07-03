package com.chanceman.ui;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import net.runelite.api.Client;
import net.runelite.api.SpritePixels;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.ImageUtil;

/**
 * Caches custom item sprites scaled for widget display.
 */
@Singleton
public class ItemSpriteCache
{
    private static final int ICON_SIZE = 32;

    private final ItemManager itemManager;
    private final Client client;
    private final Map<Integer, Integer> spriteIds = new HashMap<>();
    private int nextGeneratedSpriteId = 0x10000;

    @Inject
    public ItemSpriteCache(ItemManager itemManager, Client client)
    {
        this.itemManager = itemManager;
        this.client = client;
    }

    private int generateNextId()
    {
        return nextGeneratedSpriteId++;
    }

    /**
     * Returns a sprite ID for the given item
     */
    public int getSpriteId(int itemId)
    {
        return spriteIds.computeIfAbsent(itemId, id ->
        {
            BufferedImage img = itemManager.getImage(id, 1, false);
            if (img == null)
            {
                return -1;
            }

            // resize to ICON_SIZE x ICON_SIZE
            BufferedImage resized = ImageUtil.resizeImage(img, ICON_SIZE, ICON_SIZE);

            // convert to SpritePixels for the client's override map
            SpritePixels pixels = ImageUtil.getImageSpritePixels(resized, client);

            // generate a unique sprite ID and register the override
            int spriteId = generateNextId();
            client.getSpriteOverrides().put(spriteId, pixels);

            return spriteId;
        });
    }

    /**
     * Clears all cached sprites and unregisters them from the client's override map.
     */
    public void clear()
    {
        spriteIds.values().forEach(client.getSpriteOverrides()::remove);
        spriteIds.clear();
        nextGeneratedSpriteId = 0x10000;
    }
}
