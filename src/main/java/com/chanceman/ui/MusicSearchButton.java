package com.chanceman.ui;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MusicSearchButton {
    private static final int MUSIC_GROUP = InterfaceID.Music.UNIVERSE >>> 16;

    private static final int SPRITE_SEARCH = 1970;
    private static final int W = 14, H = 14;

    private static final int GAP = 4;
    private static final int NUDGE_LEFT = 26;
    private static final int NUDGE_DOWN = 0;

    private final Client client;
    private final ClientThread clientThread;
    private final MusicWidgetController musicWidgetController;

    private Widget icon;
    @Getter private boolean overrideActive = false;

    @Inject
    public MusicSearchButton(Client client, ClientThread clientThread, MusicWidgetController musicWidgetController) {
        this.client = client;
        this.clientThread = clientThread;
        this.musicWidgetController = musicWidgetController;
    }

    public void onStart() { clientThread.invokeLater(this::placeSearchIcon); }
    public void onStop() { clientThread.invokeLater(this::hide); }
    public void onOverrideActivated() { overrideActive = true; clientThread.invokeLater(this::hide); }
    public void onOverrideDeactivated() { overrideActive = false; clientThread.invokeLater(this::placeSearchIcon); }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded e) {
        if (e.getGroupId() == MUSIC_GROUP) clientThread.invokeLater(this::placeSearchIcon);
    }

    // Create/position the search icon next to "Toggle all"
    public void placeSearchIcon() {
        if (overrideActive) { hide(); return; }

        Widget contents = client.getWidget(InterfaceID.Music.CONTENTS);
        Widget frame = client.getWidget(InterfaceID.Music.FRAME);
        if (contents == null || frame == null) return;

        Widget root = client.getWidget(InterfaceID.Music.UNIVERSE);
        Widget toggleAll = findByAction(root, "Toggle all");

        int x, y;
        if (toggleAll != null) {
            x = toggleAll.getOriginalX() - W - GAP - NUDGE_LEFT;
            y = toggleAll.getOriginalY() + (toggleAll.getOriginalHeight() - H) / 2 + NUDGE_DOWN;
        } else {
            int frameRight = frame.getOriginalX() + frame.getOriginalWidth();
            x = frameRight - W - (GAP + 10) - NUDGE_LEFT;
            y = Math.max(6, frame.getOriginalY() - H - GAP) + NUDGE_DOWN;
        }

        if (icon != null && icon.getParentId() != contents.getId()) {
            icon = null;
        }

        if (icon == null) {
            icon = contents.createChild(-1, WidgetType.GRAPHIC);
            icon.setHasListener(true);
            icon.setAction(0, "Search Drops");
            icon.setOnOpListener((JavaScriptCallback) ev -> musicWidgetController.openDropsSearch());
        }

        icon.setHidden(false);
        icon.setSpriteId(SPRITE_SEARCH);
        move(icon, x, y, W, H);
        icon.revalidate();
    }

    public void hide() {
        if (icon != null) icon.setHidden(true);
    }

    private void move(Widget w, int x, int y, int width, int height) {
        w.setOriginalX(x);
        w.setOriginalY(y);
        w.setOriginalWidth(width);
        w.setOriginalHeight(height);
        w.setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT);
        w.setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP);
    }

    // Find any widget under parent that exposes the given right-click action text
    private Widget findByAction(Widget parent, String action) {
        if (parent == null) return null;
        Widget[] kids = merge(parent.getChildren(), parent.getDynamicChildren());
        if (kids == null) return null;
        for (Widget c : kids) {
            if (c == null) continue;
            String[] actions = c.getActions();
            if (actions != null) {
                for (String a : actions) if (a != null && a.equalsIgnoreCase(action)) return c;
            }
            Widget deeper = findByAction(c, action);
            if (deeper != null) return deeper;
        }
        return null;
    }

    private static Widget[] merge(Widget[] a, Widget[] b) {
        if (a == null) return b;
        if (b == null) return a;
        Widget[] out = new Widget[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}