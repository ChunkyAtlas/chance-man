package com.chanceman.menus;

import com.chanceman.ChanceManPlugin;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginInstantiationException;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Polygon;

@Singleton
public class TutorialIslandGuard {
    private static final Polygon TUTORIAL_ISLAND_POLY = new Polygon(
            new int[]{3111, 3112, 3116, 3122, 3125, 3131, 3135, 3145, 3153, 3150, 3148, 3150, 3146, 3141, 3142, 3134, 3121, 3108, 3099, 3091, 3070, 3071, 3065, 3065, 3065, 3057, 3056, 3067, 3064, 3064, 3065, 3069, 3072, 3077, 3081, 3087, 3096, 3101, 3108, 3110, 3111},
            new int[]{3054, 3060, 3062, 3066, 3069, 3070, 3073, 3074, 3077, 3092, 3100, 3103, 3114, 3123, 3128, 3133, 3133, 3130, 3131, 3133, 3133, 3125, 3119, 3111, 3104, 3100, 3094, 3082, 3075, 3067, 3063, 3063, 3066, 3065, 3061, 3058, 3064, 3068, 3065, 3062, 3062},
            41
    );

    private static final String CFG_GROUP = "chanceman";
    private static final String CFG_TUT_DONE = "tutIslandDone";

    @Inject
    private ConfigManager configManager;

    @Inject
    private Client client;
    @Inject
    private EventBus eventBus;

    @Inject
    private PluginManager pluginManager;
    @Inject
    private ChanceManPlugin plugin;

    private boolean shown = false;

    private static String keyForAccountHash(long accountHash) {
        return CFG_TUT_DONE + ".hash." + accountHash;
    }

    public void startUp() {
        shown = false;
        eventBus.register(this);
    }

    public void shutDown() {
        eventBus.unregister(this);
        shown = false;
    }

    private String currentPlayerKey() {
        long accountHash = client.getAccountHash();
        if (accountHash == -1) {
            return null;
        }

        return keyForAccountHash(accountHash);
    }

    private boolean isTutDone() {
        String key = currentPlayerKey();
        if (key == null) {
            return false;
        }
        Boolean v = configManager.getConfiguration(CFG_GROUP, key, Boolean.class);
        return Boolean.TRUE.equals(v);
    }

    private void setTutDone() {
        String key = currentPlayerKey();
        if (key == null) {
            return;
        }
        configManager.setConfiguration(CFG_GROUP, key, true);
    }

    @Subscribe
    public void onGameTick(GameTick e) {
        if (isTutDone()) {
            return;
        }

        Player p = client.getLocalPlayer();
        if (p == null) {
            return;
        }

        WorldPoint wp = p.getWorldLocation();

        if (wp.getPlane() != 0) {
            return;
        }

        // Automatically mark complete as soon as the player is outside Tutorial Island.
        if (!TUTORIAL_ISLAND_POLY.contains(wp.getX(), wp.getY())) {
            setTutDone();
            return;
        }

        if (shown) {
            return;
        }
        shown = true;

        SwingUtilities.invokeLater(() ->
        {
            JOptionPane.showMessageDialog(
                    null,
                    "Please leave Chance Man off until AFTER you leave tut island and SELL ALL your tut island stuff!"
                            + " SIMPLY DROPPING ITEMS THEN TURNING THE PLUGIN ON WILL RESULT IN THE DROPPED ITEMS BEING ROLLED."
                            + " SELL YOUR STUFF FIRST",
                    "Chance Man - Tutorial Island",
                    JOptionPane.WARNING_MESSAGE
            );

            pluginManager.setPluginEnabled(plugin, false);
            try {
                pluginManager.stopPlugin(plugin);
            } catch (PluginInstantiationException ignored) {}
        });
    }
}