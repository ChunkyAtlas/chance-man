package com.chanceman.ui.clog;

import com.chanceman.RolledItemsManager;
import com.chanceman.UnlockedItemsManager;
import com.chanceman.account.AccountChanged;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.chatbox.ChatboxPanelManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Overrides the Collection Log UI when it is opened to display Chance Man progress.
 */
@Singleton
public class CLog {

	enum OpenState {
		CLOSED,
		INIT,
		OPEN;
	}

	// Script IDs
	private static final int CLOG_UI_SETUP_ID = 7797;
	private static final int WINDOW_CLOSE_ID = 903;

	// CLog widget IDs
	private static final int CLOG_HEADER_COMP_ID = 40697876;
	private static final int ITEM_CONTAINER_COMP_ID = 40697893;
	private static final int CLOG_GROUP_ID = 621;
	private static final int CA_BUTTON_CHILD_ID = 21;
	private static final int SCROLLBAR_CHILD_ID = 38;

	// Summary widget IDs
	private static final int SUMMARY_COMP_ID = 46661634;
	private static final int SUMMARY_CHILD_INDEX = 88;

	// Dimensions/positions
	private static final int VIEW_WIDTH = 288;
	private static final int SCROLLBAR_WIDTH = 17;
	private static final int ITEM_WIDTH = 36;
	private static final int ITEM_HEIGHT = 32;

	private static final int CONTAINER_X_PAD = 7;
	private static final int SCROLLBAR_X_PAD = 10;
	private static final int ITEM_X_PAD = 5;
	private static final int ROW_Y_PAD = 8;

	private static final int ITEM_BASE_X = CONTAINER_X_PAD;
	private static final int ITEM_ROW_WIDTH = VIEW_WIDTH - SCROLLBAR_WIDTH - SCROLLBAR_X_PAD - CONTAINER_X_PAD * 2;

	private static final int X_INCREMENT = ITEM_WIDTH + ITEM_X_PAD;
	private static final int Y_INCREMENT = ITEM_HEIGHT + ROW_Y_PAD;
	private static final int PER_ROW = ITEM_ROW_WIDTH / X_INCREMENT;
	private static int calcX(int index) { return ITEM_BASE_X + (index % PER_ROW) * X_INCREMENT; }
	private static int calcY(int index) { return (index / PER_ROW) * Y_INCREMENT; }

	private static final int BUTTON_WIDTH = 18;
	private static final int BUTTON_HEIGHT = 17;
	private static final int BUTTON_Y = 20;
	private static final int SWAP_X = 5;
	private static final int SEARCH_X = 25;

	// Other widget values
	private static final int SWAP_SPRITE_ID = 1118;
	private static final int OPACITY_AVAILABLE = 0;
	private static final int OPACITY_UNAVAILABLE = 130;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final ChatboxPanelManager chatboxPanelManager;
	private final ChatMessageManager chatMessageManager;
	private final UnlockedItemsManager unlockedItemsManager;
	private final RolledItemsManager rolledItemsManager;

	@Setter private HashSet<Integer> allTradeableItems;
	private final ArrayList<Integer> unlocked = new ArrayList<>();
	private final ArrayList<Integer> rolled = new ArrayList<>();
	private final List<CLogEntry> unlockedEntries = new ArrayList<>();
	private final List<CLogEntry> rolledEntries = new ArrayList<>();

	private Widget itemHeader;
	private Widget itemContainer;
	private Widget charSummary;
	private boolean headerPresent = false;
	private boolean containerPresent = false;
	private boolean summaryPresent = false;

	private Widget searchButton;
	private final List<Widget> itemWidgets = new ArrayList<>();

	private String lastProgressText = "";
	private OpenState clogState = OpenState.CLOSED;
	private int currentHeight = -1;

	@Getter @Setter private boolean showRolled = false;
	@Getter @Setter private boolean pendingScrollUpdate = false;

	@Inject
	public CLog(
			Client client,
			ClientThread clientThread,
			EventBus eventBus,
			ChatboxPanelManager chatboxPanelManager,
			ChatMessageManager chatMessageManager,
			UnlockedItemsManager unlockedItemsManager,
			RolledItemsManager rolledItemsManager
	) {
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.chatboxPanelManager = chatboxPanelManager;
		this.chatMessageManager = chatMessageManager;
		this.unlockedItemsManager = unlockedItemsManager;
		this.rolledItemsManager = rolledItemsManager;
	}

	// Helper methods

	/**
	 * @return True if the CLog UI is ready to be overridden once opened
	 */
	private boolean managersReady() {
		return unlockedItemsManager.ready() && allTradeableItems != null;
	}
	private List<Integer> targetItemList(boolean isRolled) { return isRolled ? rolled : unlocked; }
	private List<CLogEntry> targetEntries(boolean isRolled) { return isRolled ? rolledEntries : unlockedEntries; }
	private boolean isAvailable(int itemId)
	{
		return showRolled ? unlockedItemsManager.isUnlocked(itemId) : rolledItemsManager.isRolled(itemId);
	}

	/**
	 * Updates the fields for core widgets (widgets that are targeted for modification)
	 */
	private void updateCoreWidgets()
	{
		itemHeader = client.getWidget(CLOG_HEADER_COMP_ID);
		itemContainer = client.getWidget(ITEM_CONTAINER_COMP_ID);
		charSummary = client.getWidget(SUMMARY_COMP_ID);
		headerPresent = itemHeader != null;
		containerPresent = itemContainer != null;
		summaryPresent = charSummary != null;
	}

	/**
	 * <p>Creates a new CLogEntry for the given index within the current target list (unlocked or rolled).</p>
	 * <p>Appends the new CLogEntry to the relevant list.</p>
	 * @param index The index within the target list
	 * @param isRolled Whether the target list is rolled or unlocked
	 */
	private void addEntry(int index, boolean isRolled) {
		int itemId = targetItemList(isRolled).get(index);
		ItemComposition comp = client.getItemDefinition(itemId);
		CLogEntry entry = new CLogEntry(
				index,
				itemId,
				comp.getName()
		);
		if (isRolled) {
			rolledEntries.add(entry);
		} else {
			unlockedEntries.add(entry);
		}
	}

	/**
	 * Clears prior roll/unlock data and, if ready, updates the entry lists with any new entries.
	 */
	private void update()
	{
		rolled.clear();
		unlocked.clear();
		rolled.addAll(rolledItemsManager.getRolledItems());
		unlocked.addAll(unlockedItemsManager.getUnlockedItems());
		for (int i = unlockedEntries.size(); i < unlocked.size(); i++)
		{
			addEntry(i, false);
		}
		for (int i = rolledEntries.size(); i < rolled.size(); i++)
		{
			addEntry(i, true);
		}
	}

	/**
	 * <p>Resets state for initialisation when the CLog is next opened.</p>
	 * <p>Preserves the CLogEntry values for re-use.</p>
	 */
	private void reset()
	{
		if (itemHeader != null) { itemHeader.deleteAllChildren(); }
		headerPresent = false;
		containerPresent = false;
		unlocked.clear();
		rolled.clear();
		itemWidgets.clear();
		closeSearch(true);
	}

	/**
	 * <p>Resets state for initialisation with a new account.</p>
	 * <p>Clears the CLogEntry values.</p>
	 * <p>Called upon account changes.</p>
	 */
	private void clearAllData() {
		reset();
		unlockedEntries.clear();
		rolledEntries.clear();
		itemWidgets.clear();
	}

	/**
	 * Main method to override the CLog content
	 */
	private void override()
	{
		update();
		updateCoreWidgets();
		if (headerPresent)
		{
			Widget button = client.getWidget(CLOG_GROUP_ID, CA_BUTTON_CHILD_ID);
			if (button != null) button.setHidden(true);
			replaceHeaderContent();
			createSwapButton();
			createSearchButton();
		}
		if (containerPresent)
		{
			createWidgets();
			updateWidgets("");
		}
	}

	/**
	 * Creates a child widget to display items within the itemContainer, positioned according to its index.
	 */
	private void createWidget()
	{
		Widget widget = itemContainer.createChild(WidgetType.GRAPHIC);
		int index = widget.getIndex();
		int x = calcX(index);
		int y = calcY(index);
		widget.setContentType(0);
		widget.setItemQuantity(1);
		widget.setItemQuantityMode(0);
		widget.setModelId(-1);
		widget.setModelType(1);
		widget.setSpriteId(-1);
		widget.setBorderType(1);
		widget.setOriginalX(x);
		widget.setOriginalY(y);
		widget.setOriginalWidth(ITEM_WIDTH);
		widget.setOriginalHeight(ITEM_HEIGHT);
		widget.setHasListener(true);
		widget.setNoClickThrough(true);
		widget.setOnOpListener((JavaScriptCallback) e -> {
			StringBuilder message = new StringBuilder();
			String name = widget.getName();
			boolean available = isAvailable(widget.getItemId());
			if (showRolled)
			{
				message.append(name).append(" was rolled");
				message.append(available ? " and unlocked." : ", but is not unlocked.");
			} else {
				message.append(name).append(" is unlocked");
				message.append(available ? " and rolled." : ", but has not been rolled.");
			}
			chatMessageManager.queue(
					QueuedMessage.builder()
								 .type(ChatMessageType.ITEM_EXAMINE)
								 .runeLiteFormattedMessage(message.toString())
								 .build()
			);
		});
		widget.setAction(1, "Examine");
		widget.setHidden(false);
		itemWidgets.add(widget);
	}

	/**
	 * Deletes all children and creates the needed number of child widgets within itemContainer.
	 */
	private void createWidgets()
	{
		if (!containerPresent) return;
		itemContainer.deleteAllChildren();
		itemWidgets.clear();
		for (int i = 0; i < Math.max(rolledEntries.size(), unlockedEntries.size()); i++)
		{
			createWidget();
		}
	}

	/**
	 * Updates the displayed widgets to show only those matching the given filter
	 * @param filter The filter to apply
	 */
	private void updateWidgets(String filter)
	{
		List<CLogEntry> matchingEntries = filter.isEmpty() ? targetEntries(showRolled) : targetEntries(showRolled)
				.stream()
				.filter(e -> e.getItemName().toLowerCase().contains(filter))
				.collect(Collectors.toList());

		for (int i = 0; i < itemWidgets.size(); i++)
		{
			Widget w = itemWidgets.get(i);
			if (i < matchingEntries.size())
			{
				updateWidget(w, matchingEntries.get(i));
				w.setHidden(false);
			} else {
				w.setHidden(true);
			}
			w.revalidate();
		}

		currentHeight = Math.max(Y_INCREMENT, (matchingEntries.size() + (PER_ROW - 1)) / PER_ROW * Y_INCREMENT);
		setPendingScrollUpdate(true);
	}

	/**
	 * Overrides the progress displayed in the character summary
	 */
	private void replaceProgress() {
		if (!summaryPresent) return;

		Widget progress = charSummary.getChild(SUMMARY_CHILD_INDEX);
		if (progress == null || progress.getText().equals(lastProgressText)) return;

		String newText = String.format("<col=0dc10d>%s/%s</col>",
									   unlockedItemsManager.getUnlockCount(), allTradeableItems.size());
		progress.setText(newText);
		lastProgressText = newText;
	}

	/**
	 * Replaces the text of widgets within the CLog header
	 */
	private void replaceHeaderContent() {
		Widget[] headerComponents = itemHeader.getDynamicChildren();
		if (headerComponents.length < 2) return;

		if (headerComponents[0] != null) {
			headerComponents[0].setText("Chance Man");
		}
		if (headerComponents[1] != null) {
			String progressText = String.format("<col=0dc10d>%s/%s</col>",
												unlockedItemsManager.getUnlockCount(), allTradeableItems.size());
			headerComponents[1].setText(progressText);
		}
		if (headerComponents.length > 2 && headerComponents[2] != null) {
			headerComponents[2].setText("");
		}
	}

	/**
	 * Updates an individual widget to display the given CLogEntry item
	 * @param widget The widget to update
	 * @param display The CLogEntry to display
	 */
	private void updateWidget(Widget widget, CLogEntry display) {
		widget.setHidden(false);
		widget.setItemId(display.getItemId());
		widget.setName(display.getItemName());
		if (showRolled)
		{
			widget.setOpacity(unlockedItemsManager.isUnlocked(display.getItemId()) ? OPACITY_AVAILABLE :
									  OPACITY_UNAVAILABLE);
		} else {
			widget.setOpacity(rolledItemsManager.isRolled(display.getItemId()) ? OPACITY_AVAILABLE :
									  OPACITY_UNAVAILABLE);
		}
		widget.revalidate();
	}

	// Button creation
	/**
	 * Sets up button widget in the CLog header with an on op action
	 * @param button The button widget
	 * @param spriteId The sprite id
	 * @param x The x position
	 * @param action The action name
	 * @param onOpAction The on op action to run
	 */
	private void setupButton(Widget button, int spriteId, int x, String action, Runnable onOpAction) {
		button.setSpriteId(spriteId);
		button.setOriginalWidth(BUTTON_WIDTH);
		button.setOriginalHeight(BUTTON_HEIGHT);
		button.setXPositionMode(WidgetPositionMode.ABSOLUTE_RIGHT);
		button.setOriginalX(x);
		button.setOriginalY(BUTTON_Y);
		button.setHasListener(true);
		button.setAction(1, action);
		button.setOnOpListener((JavaScriptCallback) e -> onOpAction.run());
		button.setName("");
		button.revalidate();
	}

	/**
	 * Creates the swap button widget in the CLog header
	 */
	private void createSwapButton()
	{
		Widget swapButton = itemHeader.createChild(WidgetType.GRAPHIC);
		setupButton(swapButton, SWAP_SPRITE_ID, SWAP_X, "Swap", this::toggleDisplay);
	}

	/**
	 * Creates the search button widget in the CLog header
	 */
	private void createSearchButton()
	{
		searchButton = itemHeader.createChild(WidgetType.GRAPHIC);
		setupButton(searchButton, SpriteID.GE_SEARCH, SEARCH_X, "Open", this::openSearch);
	}

	// Display toggle related
	/**
	 * Swaps between displaying rolled and unlocked items
	 */
	private void toggleDisplay() {
		clientThread.invokeLater(() -> {
			client.playSoundEffect(SoundEffectID.UI_BOOP);
			swapItems();
		});
	}

	/**
	 * Updates the displayed widgets when the display is toggled
	 */
	private void swapItems() {
		showRolled = !showRolled;
		for (int i = 0; i < targetEntries(showRolled).size(); i++) {
			Widget widget = itemWidgets.get(i);
			if (targetEntries(showRolled).size() > i)
			{
				CLogEntry display = targetEntries(showRolled).get(i);
				updateWidget(widget, display);
			} else {
				widget.setHidden(true);
			}
			widget.revalidate();
		}
		updateWidgets("");
	}

	// Search related
	/**
	 * Opens the search chatbox input and updates the search button to close it on op
	 */
	private void openSearch() {
		updateWidgets("");
		client.playSoundEffect(SoundEffectID.UI_BOOP);
		searchButton.setAction(1, "Close");
		searchButton.setOnOpListener((JavaScriptCallback) e -> closeSearch(false));
		chatboxPanelManager.openTextInput("Search unlock list")
						   .onChanged(s -> clientThread.invokeLater(() -> updateWidgets(s.trim())))
						   .onClose(this::resetSearchButton)
						   .build();
	}

	/**
	 * Closes the search chatbox input
	 */
	private void closeSearch(boolean silent) {
		if (clogState == OpenState.OPEN) updateWidgets("");
		chatboxPanelManager.close();
		if (!silent) client.playSoundEffect(SoundEffectID.UI_BOOP);
	}

	/**
	 * Clears the filter and updates the search button to open the search input on op
	 */
	private void resetSearchButton() {
		clientThread.invokeLater(() -> updateWidgets(""));
		searchButton.setOnOpListener((JavaScriptCallback) e -> openSearch());
		searchButton.setAction(1, "Open");
	}

	// Scrollbar
	/**
	 * Updates the scrollbar to match the current scroll height
	 */
	private void updateScrollbar() {
		if (!isPendingScrollUpdate()) return;

		Widget itemContainer = client.getWidget(ITEM_CONTAINER_COMP_ID);
		if (itemContainer == null) return;

		Widget scrollbar = client.getWidget(CLOG_GROUP_ID, SCROLLBAR_CHILD_ID);
		if (scrollbar == null) return;

		itemContainer.setScrollHeight(currentHeight);
		itemContainer.revalidateScroll();

		client.runScript(
				ScriptID.UPDATE_SCROLLBAR,
				scrollbar.getId(),
				itemContainer.getId(),
				0
		);

		itemContainer.revalidateScroll();
		scrollbar.revalidateScroll();

		setPendingScrollUpdate(false);
	}

	// Event subscriptions
	public void startUp() { eventBus.register(this); }
	public void shutDown() { eventBus.unregister(this); }

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event) {
		if (!managersReady()) return;
		if (event.getScriptId() == CLOG_UI_SETUP_ID) {
			switch (clogState)
			{
				case CLOSED:
					clogState = OpenState.INIT;
					break;
				case INIT:
					clogState = OpenState.OPEN;
					override();
					break;
			}
		} else if (event.getScriptId() == WINDOW_CLOSE_ID) {
			clogState = OpenState.CLOSED;
			reset();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event) {
		if (!managersReady()) return;
		if (clogState == OpenState.OPEN && isPendingScrollUpdate()) updateScrollbar();
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event) {
		if (event.getGroupId() == CLOG_GROUP_ID) {
			reset();
		}
	}

	@Subscribe
	public void onAccountChanged(AccountChanged event) {
		clearAllData();
		if (managersReady()) update();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!managersReady()) return;
		if (unlockedItemsManager.getUnlockCount() > unlockedEntries.size() ||
			rolledItemsManager.getRollCount() > rolledEntries.size())
		{
			update();
		}
	}

	@Subscribe
	public void onBeforeRender(BeforeRender event)
	{
		// Using client tick for this results in a one frame delay
		if (managersReady() && summaryPresent) replaceProgress();
	}
}
