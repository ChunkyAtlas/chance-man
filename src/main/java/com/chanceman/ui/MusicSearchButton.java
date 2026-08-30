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
public class MusicSearchButton
{
    private static final int MUSIC_GROUP =
            InterfaceID.Music.UNIVERSE >>> 16;

    private static final int SPRITE_SEARCH = 1970;

    private static final int W = 14;
    private static final int H = 14;
    private static final int GAP = 4;
    private static final int NUDGE_LEFT = 26;
    private static final int NUDGE_DOWN = 0;

    private static final int PLACE_ATTEMPTS = 4;

    private final Client client;
    private final ClientThread clientThread;
    private final MusicWidgetController musicWidgetController;

    private Widget icon;
    private boolean running;

    @Getter
    private boolean overrideActive;

    @Inject
    public MusicSearchButton(
            Client client,
            ClientThread clientThread,
            MusicWidgetController musicWidgetController)
    {
        this.client = client;
        this.clientThread = clientThread;
        this.musicWidgetController = musicWidgetController;
    }

    public void onStart()
    {
        running = true;
        requestPlacement();
    }

    public void onStop()
    {
        running = false;
        overrideActive = false;

        clientThread.invokeLater(() ->
        {
            hide();
            icon = null;
        });
    }

    public void onOverrideActivated()
    {
        overrideActive = true;

        clientThread.invokeLater(
                this::hide
        );
    }

    public void onOverrideDeactivated()
    {
        overrideActive = false;

        if (running)
        {
            requestPlacement();
        }
    }

    public void invalidate()
    {
        icon = null;
    }

    @Subscribe
    public void onWidgetLoaded(
            WidgetLoaded event)
    {
        if (event.getGroupId()
                != MUSIC_GROUP)
        {
            return;
        }

        invalidate();

        if (running)
        {
            requestPlacement();
        }
    }

    private void requestPlacement()
    {
        placeSearchIcon(
                PLACE_ATTEMPTS
        );
    }

    private void placeSearchIcon(
            int attemptsRemaining)
    {
        clientThread.invokeLater(() ->
        {
            if (!running
                    || overrideActive)
            {
                hide();
                return;
            }

            if (!placeSearchIconInternal()
                    && attemptsRemaining > 1)
            {
                placeSearchIcon(
                        attemptsRemaining - 1
                );
            }
        });
    }

    public void placeSearchIcon()
    {
        if (!client.isClientThread())
        {
            requestPlacement();
            return;
        }

        placeSearchIconInternal();
    }

    private boolean placeSearchIconInternal()
    {
        if (!running
                || overrideActive)
        {
            hide();
            return false;
        }

        Widget contents =
                client.getWidget(
                        InterfaceID.Music.CONTENTS
                );

        Widget frame =
                client.getWidget(
                        InterfaceID.Music.FRAME
                );

        if (contents == null
                || frame == null)
        {
            return false;
        }

        Widget root =
                client.getWidget(
                        InterfaceID.Music.UNIVERSE
                );

        Widget toggleAll =
                findByAction(
                        root,
                        "Toggle all"
                );

        int x;
        int y;

        if (toggleAll != null)
        {
            x = toggleAll.getOriginalX()
                    - W
                    - GAP
                    - NUDGE_LEFT;

            y = toggleAll.getOriginalY()
                    + (toggleAll.getOriginalHeight() - H) / 2
                    + NUDGE_DOWN;
        }
        else
        {
            int frameRight =
                    frame.getOriginalX()
                            + frame.getOriginalWidth();

            x = frameRight
                    - W
                    - (GAP + 10)
                    - NUDGE_LEFT;

            y = Math.max(
                    6,
                    frame.getOriginalY()
                            - H
                            - GAP
            ) + NUDGE_DOWN;
        }

        /*
         * A Music interface rebuild can create a new widget tree with the same
         * parent ID, so verify that the cached icon object is actually still
         * present in the current parent's children.
         */
        if (!isCurrentIcon(contents))
        {
            icon = findExistingIcon(
                    contents
            );
        }

        if (icon == null)
        {
            icon = contents.createChild(
                    -1,
                    WidgetType.GRAPHIC
            );

            icon.setHasListener(true);
            icon.setAction(
                    0,
                    "Search Drops"
            );

            icon.setOnOpListener(
                    (JavaScriptCallback) event ->
                            musicWidgetController.openDropsSearch()
            );
        }

        icon.setHidden(false);
        icon.setSpriteId(
                SPRITE_SEARCH
        );

        move(
                icon,
                x,
                y,
                W,
                H
        );

        icon.revalidate();

        return true;
    }

    public void hide()
    {
        if (icon != null)
        {
            icon.setHidden(true);
        }
    }

    private boolean isCurrentIcon(
            Widget contents)
    {
        if (icon == null
                || icon.getParentId()
                != contents.getId())
        {
            return false;
        }

        Widget[] children =
                contents.getDynamicChildren();

        if (children == null)
        {
            return false;
        }

        for (Widget child : children)
        {
            if (child == icon)
            {
                return true;
            }
        }

        return false;
    }

    private Widget findExistingIcon(
            Widget contents)
    {
        Widget[] children =
                contents.getDynamicChildren();

        if (children == null)
        {
            return null;
        }

        for (Widget child : children)
        {
            if (child == null)
            {
                continue;
            }

            if (child.getSpriteId()
                    == SPRITE_SEARCH
                    && hasAction(
                    child,
                    "Search Drops"))
            {
                return child;
            }
        }

        return null;
    }

    private void move(
            Widget widget,
            int x,
            int y,
            int width,
            int height)
    {
        widget.setOriginalX(x);
        widget.setOriginalY(y);
        widget.setOriginalWidth(width);
        widget.setOriginalHeight(height);

        widget.setXPositionMode(
                WidgetPositionMode.ABSOLUTE_LEFT
        );

        widget.setYPositionMode(
                WidgetPositionMode.ABSOLUTE_TOP
        );
    }

    private Widget findByAction(
            Widget parent,
            String action)
    {
        if (parent == null)
        {
            return null;
        }

        if (hasAction(
                parent,
                action))
        {
            return parent;
        }

        Widget found =
                findByAction(
                        parent.getStaticChildren(),
                        action
                );

        if (found != null)
        {
            return found;
        }

        found =
                findByAction(
                        parent.getDynamicChildren(),
                        action
                );

        if (found != null)
        {
            return found;
        }

        return findByAction(
                parent.getNestedChildren(),
                action
        );
    }

    private Widget findByAction(
            Widget[] children,
            String action)
    {
        if (children == null)
        {
            return null;
        }

        for (Widget child : children)
        {
            Widget found =
                    findByAction(
                            child,
                            action
                    );

            if (found != null)
            {
                return found;
            }
        }

        return null;
    }

    private static boolean hasAction(
            Widget widget,
            String action)
    {
        String[] actions =
                widget.getActions();

        if (actions == null)
        {
            return false;
        }

        for (String widgetAction : actions)
        {
            if (widgetAction != null
                    && widgetAction.equalsIgnoreCase(
                    action))
            {
                return true;
            }
        }

        return false;
    }
}