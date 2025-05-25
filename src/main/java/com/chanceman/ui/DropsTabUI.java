package com.chanceman.ui;


import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.eventbus.EventBus;

@Slf4j
@Singleton
public class DropsTabUI
{
    private final EventBus eventBus;
    private final DropsMenuListener menuListener;
    private final TabListener tabListener;

    @Inject
    public DropsTabUI(
            EventBus eventBus,
            DropsMenuListener menuListener,
            TabListener tabListener
    )
    {
        this.eventBus = eventBus;
        this.menuListener = menuListener;
        this.tabListener = tabListener;
    }

    public void startUp()
    {
        eventBus.register(menuListener);
        eventBus.register(tabListener);
    }

    public void shutDown()
    {
        eventBus.unregister(menuListener);
        eventBus.unregister(tabListener);
    }
}
