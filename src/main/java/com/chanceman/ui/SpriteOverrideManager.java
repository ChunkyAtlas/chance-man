package com.chanceman.ui;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.SpriteID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.game.SpriteOverride;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Overrides the RuneLite music tab sprite with drops.png.
 * Call register() when showing drops and unregister() to restore the original.
 * Clears the widget sprite cache to force the client to pick up the new override.
 */
@Slf4j
@Singleton
public class SpriteOverrideManager implements SpriteOverride
{
    private static final int SPRITE_ID = SpriteID.TAB_MUSIC;
    private static final String RESOURCE_PATH = "/com/chanceman/drops.png";

    private final SpriteManager spriteManager;
    private final Client client;
    private final ClientThread clientThread;

    @Inject
    public SpriteOverrideManager(SpriteManager spriteManager, Client client, ClientThread clientThread)
    {
        this.spriteManager = spriteManager;
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Apply the custom drops icon override.
     */
    public void register()
    {
        spriteManager.addSpriteOverrides(new SpriteOverride[]{this});
        clientThread.invokeLater(() -> {
            client.getWidgetSpriteCache().reset();
        });
    }

    /**
     * Remove the override, restoring the original music tab icon.
     */
    public void unregister()
    {
        spriteManager.removeSpriteOverrides(new SpriteOverride[]{this});
        clientThread.invokeLater(() -> {
            client.getWidgetSpriteCache().reset();
        });
    }

    @Override
    public int getSpriteId()
    {
        return SPRITE_ID;
    }

    @Override
    public String getFileName()
    {
        // Resource path relative to the classpath
        return RESOURCE_PATH;
    }
}
