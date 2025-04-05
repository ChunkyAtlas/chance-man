package com.chanceman;

import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JColorChooser;

/**
 * Panel for displaying rolled and unlocked items.
 * It provides UI for manual roll actions, search/filter functionality,
 * and displays each item with its icon and full item name.
 * Each item panel shows a tooltip on both the icon and the panel with the item name.
 */
public class ChanceManPanel extends PluginPanel
{
    private final UnlockedItemsManager unlockedItemsManager;
    private final RolledItemsManager rolledItemsManager;
    private final ItemManager itemManager;
    private final List<Integer> allTradeableItems;
    private final ClientThread clientThread;
    private final RollAnimationManager rollAnimationManager;

    // Caches for item icons and names
    private final Map<Integer, ImageIcon> itemIconCache = new HashMap<>();
    private final Map<Integer, String> itemNameCache = new HashMap<>();

    // CardLayout panel to show either Rolled or Unlocked view
    private final JPanel centerCardPanel = new JPanel(new CardLayout());
    private final JPanel rolledContentPanel = new JPanel();
    private final JPanel unlockedContentPanel = new JPanel();

    // View selection row: 3 buttons (swap, filter unlocked-not-rolled, filter unlocked-and-rolled)
    private final JButton swapViewButton = new JButton("🔄");
    private final JToggleButton filterUnlockedNotRolledButton = new JToggleButton("🔓");
    private final JToggleButton filterUnlockedAndRolledButton = new JToggleButton("🔀");

    // Flag for current view: true = showing Unlocked, false = showing Rolled
    private boolean showingUnlocked = true;

    // Search text
    private String searchText = "";

    // Single count label at the bottom
    private final JLabel countLabel = new JLabel("Unlocked: 0/0");

    // Roll button for manual roll actions
    private final JButton rollButton = new JButton("Roll");

    // Active filter: "NONE", "UNLOCKED_NOT_ROLLED", or "UNLOCKED_AND_ROLLED"
    private String activeFilter = "NONE";

    // Join Discord Button links to discord invite
    private final JButton discordButton = new JButton();

    // Default color for item text
    private final Color defaultItemTextColor = new Color(220, 220, 220);

    // Map to hold custom text colors for specific items (key: itemId, value: chosen Color)
    private final Map<Integer, Color> itemTextColorMap = new HashMap<>();

    /**
     * Constructs a ChanceManPanel.
     *
     * @param unlockedItemsManager Manager for unlocked items.
     * @param rolledItemsManager   Manager for rolled items.
     * @param itemManager          The item manager.
     * @param allTradeableItems    List of all tradeable item IDs.
     * @param clientThread         The client thread for scheduling UI updates.
     * @param rollAnimationManager The roll animation manager to trigger animations.
     */
    public ChanceManPanel(
            UnlockedItemsManager unlockedItemsManager,
            RolledItemsManager rolledItemsManager,
            ItemManager itemManager,
            List<Integer> allTradeableItems,
            ClientThread clientThread,
            RollAnimationManager rollAnimationManager
    )
    {
        this.unlockedItemsManager = unlockedItemsManager;
        this.rolledItemsManager = rolledItemsManager;
        this.itemManager = itemManager;
        this.allTradeableItems = allTradeableItems;
        this.clientThread = clientThread;
        this.rollAnimationManager = rollAnimationManager;
        init();
    }

    /**
     * Initializes the panel UI components.
     */
    private void init()
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(15, 15, 15, 15));
        setBackground(new Color(37, 37, 37));

        // ========== TOP PANEL (Header, Search, Buttons Row) ==========
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        // Header
        topPanel.add(buildHeaderPanel());
        topPanel.add(Box.createVerticalStrut(10));

        // Search Bar
        topPanel.add(buildSearchBar());
        topPanel.add(Box.createVerticalStrut(10));

        // Button row: 3 columns, each for one button
        JPanel buttonRowPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        buttonRowPanel.setOpaque(false);

        // Style the 3 buttons identically
        styleButton(swapViewButton);
        styleToggleButton(filterUnlockedNotRolledButton);
        styleToggleButton(filterUnlockedAndRolledButton);

        // Tooltips & actions
        swapViewButton.setToolTipText("Swap between Unlocked and Rolled views");
        swapViewButton.addActionListener(e -> toggleView());

        filterUnlockedNotRolledButton.setToolTipText("Filter: Show items that are unlocked but not rolled");
        filterUnlockedNotRolledButton.addActionListener(e ->
        {
            if (filterUnlockedNotRolledButton.isSelected())
            {
                activeFilter = "UNLOCKED_NOT_ROLLED";
                filterUnlockedAndRolledButton.setSelected(false);
            }
            else
            {
                activeFilter = "NONE";
            }
            updatePanel();
        });

        filterUnlockedAndRolledButton.setToolTipText("Filter: Show items that are both unlocked and rolled");
        filterUnlockedAndRolledButton.addActionListener(e ->
        {
            if (filterUnlockedAndRolledButton.isSelected())
            {
                activeFilter = "UNLOCKED_AND_ROLLED";
                filterUnlockedNotRolledButton.setSelected(false);
            }
            else
            {
                activeFilter = "NONE";
            }
            updatePanel();
        });

        // Add them in left->right order
        buttonRowPanel.add(swapViewButton);
        buttonRowPanel.add(filterUnlockedNotRolledButton);
        buttonRowPanel.add(filterUnlockedAndRolledButton);

        // Add the row to the top panel
        topPanel.add(buttonRowPanel);

        // EXTRA SPACE between the buttons row and the icon panel
        topPanel.add(Box.createVerticalStrut(10));

        add(topPanel, BorderLayout.NORTH);

        // ========== CENTER PANEL (CardLayout) ==========
        rolledContentPanel.setLayout(new BoxLayout(rolledContentPanel, BoxLayout.Y_AXIS));
        rolledContentPanel.setBackground(new Color(60, 63, 65));

        unlockedContentPanel.setLayout(new BoxLayout(unlockedContentPanel, BoxLayout.Y_AXIS));
        unlockedContentPanel.setBackground(new Color(60, 63, 65));

        JPanel rolledContainer = createTitledPanel("Rolled Items", rolledContentPanel);
        JPanel unlockedContainer = createTitledPanel("Unlocked Items", unlockedContentPanel);

        centerCardPanel.add(rolledContainer, "ROLLED");
        centerCardPanel.add(unlockedContainer, "UNLOCKED");
        add(centerCardPanel, BorderLayout.CENTER);

        // ========== BOTTOM PANEL (Count + Roll) ==========
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);

        // Single count label
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        countPanel.setOpaque(false);
        countLabel.setFont(new Font("Arial", Font.BOLD, 11));
        countLabel.setForeground(new Color(220, 220, 220));
        countPanel.add(countLabel);
        bottomPanel.add(countPanel);
        bottomPanel.add(Box.createVerticalStrut(10));

        // Roll button
        JPanel rollButtonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        rollButtonPanel.setOpaque(false);
        rollButton.setPreferredSize(new Dimension(100, 30));
        rollButton.setFocusPainted(false);
        rollButton.setBackground(new Color(60, 63, 65));
        rollButton.setForeground(Color.WHITE);
        rollButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        rollButton.addActionListener(this::performManualRoll);
        rollButtonPanel.add(rollButton);
        bottomPanel.add(rollButtonPanel);

        add(bottomPanel, BorderLayout.SOUTH);

        // Default to Unlocked view
        showingUnlocked = true;
        ((CardLayout) centerCardPanel.getLayout()).show(centerCardPanel, "UNLOCKED");
        updatePanel();
    }

    /**
     * Toggles between Unlocked view and Rolled view.
     */
    private void toggleView()
    {
        showingUnlocked = !showingUnlocked;
        CardLayout cl = (CardLayout) centerCardPanel.getLayout();
        if (showingUnlocked)
        {
            cl.show(centerCardPanel, "UNLOCKED");
        }
        else
        {
            cl.show(centerCardPanel, "ROLLED");
        }
        updatePanel();
    }

    /**
     * Creates a titled container panel that wraps the given content panel.
     *
     * @param title        The title to display on the border.
     * @param contentPanel The panel to wrap.
     * @return The container panel.
     */
    private JPanel createTitledPanel(String title, JPanel contentPanel)
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        Border line = new LineBorder(new Color(80, 80, 80));
        Border empty = new EmptyBorder(5, 5, 5, 5);
        TitledBorder titled = BorderFactory.createTitledBorder(line, title);
        titled.setTitleColor(new Color(200, 200, 200));
        container.setBorder(new CompoundBorder(titled, empty));

        JScrollPane scrollPane = new JScrollPane(
                contentPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(250, 300));

        container.add(scrollPane, BorderLayout.CENTER);
        return container;
    }

    /**
     * Styles a general JButton to match the design.
     *
     * @param button The button to style.
     */
    private void styleButton(JButton button)
    {
        button.setFocusPainted(false);
        button.setBackground(new Color(60, 63, 65));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(50, 30));
    }

    /**
     * Styles a JToggleButton to match the design.
     *
     * @param button The toggle button to style.
     */
    private void styleToggleButton(JToggleButton button)
    {
        button.setFocusPainted(false);
        button.setBackground(new Color(60, 63, 65));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("SansSerif", Font.BOLD, 12));
        button.setPreferredSize(new Dimension(50, 30));
    }

    /**
     * Builds the header panel with an icon and title.
     *
     * @return The header panel.
     */
    private JPanel buildHeaderPanel()
    {
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headerPanel.setOpaque(false);

        // Header icon
        ImageIcon headerIcon = new ImageIcon(getClass().getResource("/net/runelite/client/plugins/chanceman/icon.png"));
        JLabel iconLabel = new JLabel(headerIcon);

        // Title label
        JLabel titleLabel = new JLabel("Chance Man");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(220, 220, 220));

        // Create the Discord button
        JButton discordButton = new JButton();
        discordButton.setToolTipText("Join The Chance Man Discord");
        // Scale the Discord icon to 16x16
        ImageIcon discordIcon = new ImageIcon(getClass().getResource("/net/runelite/client/plugins/chanceman/discord.png"));
        Image scaledImage = discordIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        discordButton.setIcon(new ImageIcon(scaledImage));
        // Make the button look flat (no border or background)
        discordButton.setOpaque(false);
        discordButton.setContentAreaFilled(false);
        discordButton.setBorderPainted(false);
        // Add an action to open the Discord link
        discordButton.addActionListener(e -> LinkBrowser.browse("https://discord.gg/TMkAYXxncU"));

        // Assemble the header panel
        headerPanel.add(iconLabel);
        headerPanel.add(Box.createHorizontalStrut(10));
        headerPanel.add(titleLabel);
        headerPanel.add(discordButton);

        return headerPanel;
    }

    /**
     * Builds the search bar panel.
     *
     * @return The search bar panel.
     */
    private JPanel buildSearchBar()
    {
        JPanel searchBarPanel = new JPanel(new BorderLayout());
        searchBarPanel.setOpaque(false);
        searchBarPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JPanel searchContainer = new JPanel(new BorderLayout());
        searchContainer.setBackground(new Color(30, 30, 30));
        searchContainer.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

        // Search icon
        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setForeground(new Color(200, 200, 200));
        searchIcon.setBorder(new EmptyBorder(0, 5, 0, 5));
        searchContainer.add(searchIcon, BorderLayout.WEST);

        // Search field
        JTextField searchField = new JTextField();
        searchField.setBackground(new Color(45, 45, 45));
        searchField.setForeground(Color.WHITE);
        searchField.setBorder(null);
        searchField.setCaretColor(Color.WHITE);
        searchField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                SwingUtilities.invokeLater(() ->
                {
                    searchText = searchField.getText().toLowerCase();
                    updatePanel();
                });
            }
        });
        searchContainer.add(searchField, BorderLayout.CENTER);

        // Clear label to reset search
        JLabel clearLabel = new JLabel("❌");
        clearLabel.setFont(new Font("SansSerif", Font.PLAIN, 9));
        clearLabel.setForeground(Color.RED);
        clearLabel.setBorder(new EmptyBorder(0, 6, 0, 6));
        clearLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                searchField.setText("");
                searchText = "";
                updatePanel();
            }
        });
        searchContainer.add(clearLabel, BorderLayout.EAST);

        searchBarPanel.add(searchContainer, BorderLayout.CENTER);
        return searchBarPanel;
    }

    /**
     * Triggers a manual roll animation when the Roll button is clicked.
     *
     * @param e The action event.
     */
    private void performManualRoll(java.awt.event.ActionEvent e)
    {
        if (rollAnimationManager.isRolling())
        {
            return;
        }
        List<Integer> locked = new ArrayList<>();
        for (int id : allTradeableItems)
        {
            if (!unlockedItemsManager.isUnlocked(id))
            {
                locked.add(id);
            }
        }
        if (locked.isEmpty())
        {
            JOptionPane.showMessageDialog(
                    this,
                    "All items are unlocked!",
                    "ChanceMan",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        int randomItemId = locked.get(new Random().nextInt(locked.size()));
        rollAnimationManager.enqueueRoll(randomItemId);
    }

    /**
     * Main update routine: filters the active set (Unlocked or Rolled), applies search text and filter toggles,
     * updates the single count label, and then builds the item list without trailing gaps.
     */
    public void updatePanel()
    {
        clientThread.invokeLater(() ->
        {
            // Build filtered lists
            List<Integer> filteredRolled = new ArrayList<>();
            for (Integer id : rolledItemsManager.getRolledItems())
            {
                ItemComposition comp = itemManager.getItemComposition(id);
                if (comp != null)
                {
                    String name = comp.getName().toLowerCase();
                    if (searchText.isEmpty() || name.contains(searchText))
                    {
                        filteredRolled.add(id);
                    }
                }
            }

            List<Integer> filteredUnlocked = new ArrayList<>();
            for (Integer id : unlockedItemsManager.getUnlockedItems())
            {
                ItemComposition comp = itemManager.getItemComposition(id);
                if (comp != null)
                {
                    String name = comp.getName().toLowerCase();
                    if (searchText.isEmpty() || name.contains(searchText))
                    {
                        filteredUnlocked.add(id);
                    }
                }
            }

            // Apply active filter toggles
            if (activeFilter.equals("UNLOCKED_NOT_ROLLED"))
            {
                filteredUnlocked.removeIf(id -> rolledItemsManager.getRolledItems().contains(id));
                filteredRolled.clear();
            }
            else if (activeFilter.equals("UNLOCKED_AND_ROLLED"))
            {
                filteredUnlocked.removeIf(id -> !rolledItemsManager.getRolledItems().contains(id));
                filteredRolled.removeIf(id -> !unlockedItemsManager.getUnlockedItems().contains(id));
            }

            int totalTrackable = allTradeableItems.size();
            int rolledCount = rolledItemsManager.getRolledItems().size();
            int unlockedCount = unlockedItemsManager.getUnlockedItems().size();

            // Switch to the Swing thread to update the UI
            SwingUtilities.invokeLater(() ->
            {
                // If we're showing unlocked, show "Unlocked: X/Y"; otherwise "Rolled: X/Y"
                if (showingUnlocked)
                {
                    countLabel.setText("Unlocked: " + unlockedCount + "/" + totalTrackable);
                }
                else
                {
                    countLabel.setText("Rolled: " + rolledCount + "/" + totalTrackable);
                }

                // Determine which panel is active (rolled or unlocked)
                JPanel activeContentPanel = showingUnlocked ? unlockedContentPanel : rolledContentPanel;
                activeContentPanel.removeAll();

                // Decide which list of item IDs to show
                List<Integer> filteredIds = showingUnlocked ? filteredUnlocked : filteredRolled;
                for (int i = 0; i < filteredIds.size(); i++)
                {
                    int itemId = filteredIds.get(i);
                    activeContentPanel.add(createHorizontalItemPanel(itemId));

                    // Only add a vertical strut if this isn't the last item
                    if (i < filteredIds.size() - 1)
                    {
                        activeContentPanel.add(Box.createVerticalStrut(3));
                    }
                }

                activeContentPanel.revalidate();
                activeContentPanel.repaint();
            });
        });
    }

    /**
     * Creates a horizontal panel with an icon on the left and item name on the right.
     * Also sets a hover tooltip on both the panel and the icon to display the full item name.
     *
     * @param itemId The item ID.
     * @return The horizontal item panel.
     */
    private JPanel createHorizontalItemPanel(int itemId)
    {
        // Use BoxLayout on the X axis for a tight horizontal layout
        JPanel itemPanel = new JPanel();
        itemPanel.setLayout(new BoxLayout(itemPanel, BoxLayout.X_AXIS));
        itemPanel.setOpaque(false);
        itemPanel.setAlignmentX(LEFT_ALIGNMENT);

        // Create and add the icon label with fixed size
        ImageIcon icon = getItemIcon(itemId);
        JLabel iconLabel = new JLabel(icon != null ? icon : new ImageIcon());
        iconLabel.setPreferredSize(new Dimension(32, 32));
        iconLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        itemPanel.add(iconLabel);

        // Add a small horizontal gap between the icon and the name label
        itemPanel.add(Box.createHorizontalStrut(5));

        // Determine the text color for this item: either a custom color or the default
        Color textColor = itemTextColorMap.getOrDefault(itemId, defaultItemTextColor);

        // Create and add the name label with the chosen text color
        String cachedName = itemNameCache.get(itemId);
        JLabel nameLabel = new JLabel(cachedName != null ? cachedName : "Loading...");
        nameLabel.setForeground(textColor);
        nameLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        nameLabel.setAlignmentY(Component.CENTER_ALIGNMENT);
        itemPanel.add(nameLabel);

        // If the item name is not cached, fetch it asynchronously
        if (cachedName == null)
        {
            itemPanel.setToolTipText("Loading...");
            iconLabel.setToolTipText("Loading...");
            getItemNameAsync(itemId, name -> {
                itemNameCache.put(itemId, name);
                nameLabel.setText(name);
                // Update tooltips for both the panel and the icon
                itemPanel.setToolTipText(name);
                iconLabel.setToolTipText(name);
                nameLabel.repaint();
            });
        }
        else
        {
            itemPanel.setToolTipText(cachedName);
            iconLabel.setToolTipText(cachedName);
        }

        // Constrain the panel's height to match the icon's height
        itemPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        // Add a mouse listener to show the context menu on right-click for this specific item
        itemPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showItemTextColorPopup(e, itemId, nameLabel);
                }
            }
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showItemTextColorPopup(e, itemId, nameLabel);
                }
            }
        });

        return itemPanel;
    }

    private void showItemTextColorPopup(java.awt.event.MouseEvent e, int itemId, JLabel label) {
        JPopupMenu popup = new JPopupMenu();

        JMenuItem changeColorItem = new JMenuItem("Change Text Color");
        JMenuItem resetColorItem = new JMenuItem("Reset Color");

        changeColorItem.addActionListener(ev -> {
            // Get current color or default
            Color currentColor = itemTextColorMap.getOrDefault(itemId, defaultItemTextColor);
            Color chosen = JColorChooser.showDialog(ChanceManPanel.this, "Choose Text Color", currentColor);
            if (chosen != null) {
                // Save the chosen color and update the label
                itemTextColorMap.put(itemId, chosen);
                label.setForeground(chosen);
            }
        });

        resetColorItem.addActionListener(ev -> {
            // Remove any custom color for this item and revert to default
            itemTextColorMap.remove(itemId);
            label.setForeground(defaultItemTextColor);
        });

        popup.add(changeColorItem);
        popup.add(resetColorItem);
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * Retrieves (and caches) the item icon for a given item ID.
     *
     * @param itemId The item ID.
     * @return The ImageIcon for the item, or null if not available.
     */
    private ImageIcon getItemIcon(int itemId)
    {
        if (itemIconCache.containsKey(itemId))
        {
            return itemIconCache.get(itemId);
        }
        BufferedImage image = itemManager.getImage(itemId, 1, false);
        if (image == null)
        {
            return null;
        }
        ImageIcon icon = new ImageIcon(image);
        itemIconCache.put(itemId, icon);
        return icon;
    }

    /**
     * Asynchronously retrieves the item name for a given item ID and passes it to the callback.
     *
     * @param itemId   The item ID.
     * @param callback Consumer to receive the item name.
     */
    private void getItemNameAsync(int itemId, Consumer<String> callback)
    {
        clientThread.invokeLater(() -> {
            ItemComposition comp = itemManager.getItemComposition(itemId);
            String name = (comp != null) ? comp.getName() : "Unknown";
            SwingUtilities.invokeLater(() -> callback.accept(name));
        });
    }
}
