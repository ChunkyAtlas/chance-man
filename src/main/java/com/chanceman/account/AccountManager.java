package com.chanceman.account;

import com.chanceman.managers.RolledItemsManager;
import com.chanceman.managers.ObtainedItemsManager;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.AccountHashChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Monitors for account changes and updates the stored display name.
 */
@Singleton
public class AccountManager
{
	@Inject
	private Client client;

	@Inject
	private ObtainedItemsManager obtainedItemsManager;
	@Inject
	private RolledItemsManager rolledItemsManager;

	@Inject
	private EventBus eventBus;

	private long hash = -1;
	@Getter @Setter private volatile String playerName;
	private boolean nameSet = false;

	public boolean ready() { return hash != -1 && nameSet; }

	public void init()
	{
		if (client.getGameState() == GameState.LOGGED_IN && client.getAccountHash() != -1)
		{
			hash = client.getAccountHash();
			nameSet = false;
		}
	}

	@Subscribe
	private void onAccountHashChanged(AccountHashChanged event)
	{
		long newHash = client.getAccountHash();
		if (hash != newHash)
		{
			hash = newHash;
			nameSet = false; // Player is null at this point, so name is set in onClientTick
		}
	}

	@Subscribe
	private void onClientTick(ClientTick event)
	{
		if (client.getGameState().getState() < GameState.LOADING.getState()) return;
		if (hash == -1) return;
		if (nameSet) return;

		Player player = client.getLocalPlayer();
		if (player == null) return;

		String name = player.getName();
		if (name == null) return;

		setPlayerName(name);
		nameSet = true;
		emit();
	}

	@Subscribe
	private void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN && hash != -1)
		{
			reset();
		}
	}

	public void reset() {
		hash = -1;
		setPlayerName(null);
		emit();
	}

	private void emit()
	{
		eventBus.post(new AccountChanged(hash, playerName));
	}
}
