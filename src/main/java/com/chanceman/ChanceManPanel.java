package com.chanceman;

import com.chanceman.managers.ObtainedItemsManager;
import com.chanceman.managers.RollAnimationManager;
import com.chanceman.managers.RolledItemsManager;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Panel for Chance Man.
 * Shows a searchable list of items from rolled/obtained state. A dropdown selects which list:
 *  - Obtained
 *  - Rolled
 *  - Rolled, not Obtained
 *  - Usable (in both sets)
 * Items are shown newest-first based on underlying manager insertion order; for Usable,
 * ordering follows rolled recency.
 */
public class ChanceManPanel extends PluginPanel
{
    private final ObtainedItemsManager obtainedItemsManager;
    private final RolledItemsManager rolledItemsManager;
    private final ItemManager itemManager;
    private final HashSet<Integer> allTradeableItems;
    private final ClientThread clientThread;
    private final RollAnimationManager rollAnimationManager;

    private final Map<Integer, ImageIcon> itemIconCache = new ConcurrentHashMap<>();
    private final Map<Integer, String> itemNameCache = new ConcurrentHashMap<>();
    private final Set<Integer> nameFetchInFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<Integer> iconFetchInFlight = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final JLabel modeLabel = new JLabel("Items Rolled");
    private final JTextField searchField = new JTextField();
    private final DefaultListModel<Integer> listModel = new DefaultListModel<>();
    private final JList<Integer> itemList = new JList<>(listModel);
    private final JLabel countLabel = new JLabel("0/0");
    private final JButton rollButton = new JButton("Roll");

    private enum ListMode
    {
        ROLLED("Rolled"),
        OBTAINED("Obtained"),
        ROLLED_NOT_OBTAINED("Rolled, not Obtained"),
        USABLE("Usable");

        private final String label;

        ListMode(String label)
        {
            this.label = label;
        }

        public String label()
        {
            return label;
        }

        @Override
        public String toString()
        {
            return label;
        }
    }

    private static final Map<ListMode, String> MODE_TOOLTIPS = Map.of(
            ListMode.ROLLED, "Items that have been rolled/unlocked (legacy: Unlocked).",
            ListMode.OBTAINED, "Items you have obtained. (legacy: Rolled).",
            ListMode.ROLLED_NOT_OBTAINED, "Items you have rolled, but have not obtained yet.",
            ListMode.USABLE, "Items that are both obtained and rolled."
    );

    private volatile ListMode listMode = ListMode.ROLLED;
    private volatile String searchText = "";

    private final JComboBox<ListMode> modeDropdown = new JComboBox<>(ListMode.values());

    public ChanceManPanel(
            ObtainedItemsManager obtainedItemsManager,
            RolledItemsManager rolledItemsManager,
            ItemManager itemManager,
            HashSet<Integer> allTradeableItems,
            ClientThread clientThread,
            RollAnimationManager rollAnimationManager
    )
    {
        this.obtainedItemsManager = obtainedItemsManager;
        this.rolledItemsManager = rolledItemsManager;
        this.itemManager = itemManager;
        this.allTradeableItems = allTradeableItems;
        this.clientThread = clientThread;
        this.rollAnimationManager = rollAnimationManager;
        init();
    }

    private void init()
    {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        setBackground(new Color(37, 37, 37));

        add(buildTop(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);

        itemList.setFixedCellHeight(36);
        itemList.setVisibleRowCount(-1);

        setMode(ListMode.ROLLED);
        updatePanel();
    }

    private JPanel buildTop()
    {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        top.add(buildHeaderRow());
        top.add(Box.createVerticalStrut(10));
        top.add(buildDropdownRow());
        top.add(Box.createVerticalStrut(8));
        top.add(buildModeLabelRow());
        top.add(Box.createVerticalStrut(10));
        top.add(buildSearchBar());

        return top;
    }

    private JPanel buildHeaderRow()
    {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        ImageIcon headerIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/net/runelite/client/plugins/chanceman/icon.png"))
        );
        JLabel iconLabel = new JLabel(headerIcon);

        JLabel titleLabel = new JLabel("Chance Man", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLabel.setForeground(new Color(220, 220, 220));

        JButton discord = new JButton();
        discord.setToolTipText("Join The Chance Man Discord");
        ImageIcon discordIcon = new ImageIcon(
                Objects.requireNonNull(getClass().getResource("/net/runelite/client/plugins/chanceman/discord.png"))
        );
        Image scaledImage = discordIcon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
        discord.setIcon(new ImageIcon(scaledImage));
        discord.setOpaque(false);
        discord.setContentAreaFilled(false);
        discord.setBorderPainted(false);
        discord.addActionListener(e -> LinkBrowser.browse("https://discord.gg/TMkAYXxncU"));

        headerPanel.add(iconLabel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        headerPanel.add(discord, BorderLayout.EAST);

        return headerPanel;
    }

    private JPanel buildDropdownRow()
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        modeDropdown.setFocusable(false);
        modeDropdown.setBackground(new Color(60, 63, 65));
        modeDropdown.setForeground(Color.WHITE);
        modeDropdown.setFont(new Font("SansSerif", Font.BOLD, 12));
        modeDropdown.setBorder(
                new CompoundBorder(new LineBorder(new Color(80, 80, 80)),
                        new EmptyBorder(2, 6, 2, 6))
        );

        modeDropdown.setRenderer(new DefaultListCellRenderer()
        {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus)
            {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                lbl.setBorder(new EmptyBorder(4, 6, 4, 6));

                if (value instanceof ListMode)
                {
                    ListMode mode = (ListMode) value;

                    lbl.setText(index == -1 ? "Filter: " + mode.label() : mode.label());

                    String tip = MODE_TOOLTIPS.get(mode);
                    lbl.setToolTipText(tip);
                    modeDropdown.setToolTipText(tip);
                }
                else
                {
                    lbl.setToolTipText(null);
                }

                lbl.setBackground(isSelected
                        ? new Color(75, 78, 80)
                        : new Color(60, 63, 65));
                lbl.setForeground(Color.WHITE);

                return lbl;
            }
        });

        modeDropdown.addActionListener(e ->
        {
            Object sel = modeDropdown.getSelectedItem();
            if (sel instanceof ListMode)
            {
                setMode((ListMode) sel);
                updatePanel();
            }
        });

        row.add(modeDropdown, BorderLayout.CENTER);
        return row;
    }

    private JPanel buildModeLabelRow()
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);

        modeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        modeLabel.setForeground(new Color(210, 210, 210));

        row.add(modeLabel, BorderLayout.WEST);
        return row;
    }

    private JPanel buildSearchBar()
    {
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setBackground(new Color(30, 30, 30));
        searchBox.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JLabel icon = new JLabel("\uD83D\uDD0D");
        icon.setForeground(new Color(200, 200, 200));
        icon.setBorder(new EmptyBorder(0, 0, 0, 6));
        searchBox.add(icon, BorderLayout.WEST);

        searchField.setBackground(new Color(45, 45, 45));
        searchField.setForeground(Color.WHITE);
        searchField.setBorder(null);
        searchField.setCaretColor(Color.WHITE);

        searchField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                searchText = searchField.getText().toLowerCase();
                updatePanel();
            }
        });

        searchBox.add(searchField, BorderLayout.CENTER);

        container.add(searchBox, BorderLayout.CENTER);
        return container;
    }

    private JPanel buildCenter()
    {
        itemList.setCellRenderer(new ItemCellRenderer());
        itemList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane scroll = new JScrollPane(
                itemList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scroll.setBorder(null);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new CompoundBorder(
                new LineBorder(new Color(80, 80, 80)),
                new EmptyBorder(6, 6, 6, 6)));

        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel buildBottom()
    {
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

        bottom.add(Box.createVerticalStrut(10));

        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        countPanel.setOpaque(false);
        countLabel.setFont(new Font("Arial", Font.BOLD, 11));
        countLabel.setForeground(new Color(220, 220, 220));
        countPanel.add(countLabel);
        bottom.add(countPanel);

        bottom.add(Box.createVerticalStrut(10));

        JPanel rollPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        rollPanel.setOpaque(false);
        rollButton.setToolTipText("Manual roll (select a random locked item)");
        rollButton.addActionListener(this::performManualRoll);
        rollButton.setPreferredSize(new Dimension(120, 30));
        rollPanel.add(rollButton);
        bottom.add(rollPanel);

        bottom.add(Box.createVerticalStrut(4));
        return bottom;
    }

    private void setMode(ListMode mode)
    {
        listMode = mode;

        String tip = MODE_TOOLTIPS.get(mode);
        modeDropdown.setToolTipText(tip);

        switch (mode)
        {
            case ROLLED:
                modeLabel.setText("Items Rolled");
                break;
            case OBTAINED:
                modeLabel.setText("Items Obtained");
                break;
            case ROLLED_NOT_OBTAINED:
                modeLabel.setText("Items Rolled, not Obtained");
                break;
            case USABLE:
                modeLabel.setText("Items Usable");
                break;
        }
    }

    public void updatePanel()
    {
        final ListMode modeSnap = listMode;
        final String searchSnap = searchText;

        clientThread.invokeLater(() ->
        {
            Set<Integer> obtained = obtainedItemsManager.getObtainedItems();
            Set<Integer> rolled = rolledItemsManager.getRolledItems();
            List<Integer> base = new ArrayList<>();

            switch (modeSnap)
            {
                case ROLLED:
                    base.addAll(rolled);
                    break;

                case OBTAINED:
                    base.addAll(obtained);
                    break;

                case ROLLED_NOT_OBTAINED:
                    // Show what you can buy/unlock but haven't obtained yet
                    base.addAll(rolled);
                    base.removeIf(obtained::contains);
                    break;

                case USABLE:
                    base.addAll(rolled);
                    base.removeIf(id -> !obtained.contains(id));
                    break;
            }

            Collections.reverse(base);

            if (searchSnap != null && !searchSnap.isEmpty())
            {
                base.removeIf(id ->
                {
                    ItemComposition comp = itemManager.getItemComposition(id);
                    String name = comp.getName();
                    return name == null || !name.toLowerCase().contains(searchSnap);
                });
            }

            final int total;
            synchronized (allTradeableItems)
            {
                total = allTradeableItems.size();
            }

            SwingUtilities.invokeLater(() ->
            {
                listModel.clear();
                for (Integer id : base)
                {
                    listModel.addElement(id);
                }
                countLabel.setText(base.size() + "/" + total);

                // extra stability during mode swaps
                itemList.revalidate();
                itemList.repaint();
            });
        });
    }

    private class ItemCellRenderer extends JPanel implements ListCellRenderer<Integer>
    {
        private final JLabel icon = new JLabel();
        private final JLabel name = new JLabel();

        ItemCellRenderer()
        {
            setLayout(new BorderLayout(8, 0));
            setOpaque(true);
            icon.setPreferredSize(new Dimension(32, 32));

            add(icon, BorderLayout.WEST);
            add(name, BorderLayout.CENTER);
            name.setFont(new Font("SansSerif", Font.PLAIN, 11));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends Integer> list, Integer value, int index,
                boolean isSelected, boolean cellHasFocus)
        {
            ImageIcon ico = itemIconCache.get(value);
            if (ico != null)
            {
                icon.setIcon(ico);
            }
            else
            {
                icon.setIcon(null);
                requestItemIcon(value, list, index);
            }

            String cached = itemNameCache.get(value);
            if (cached != null)
            {
                name.setText(cached);
            }
            else
            {
                name.setText("Loading...");
                requestItemName(value, list, index);
            }

            setBackground(isSelected ? list.getSelectionBackground() : new Color(60, 63, 65));
            name.setForeground(new Color(220, 220, 220));
            return this;
        }
    }

    private void requestItemName(int itemId, JList<?> list, int index)
    {
        if (!nameFetchInFlight.add(itemId))
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            String resolved = "Unknown";
            try
            {
                ItemComposition comp = itemManager.getItemComposition(itemId);
                if (comp.getName() != null)
                {
                    resolved = comp.getName();
                }
            }
            catch (Exception ignored)
            {
            }

            final String finalName = resolved;
            SwingUtilities.invokeLater(() ->
            {
                itemNameCache.put(itemId, finalName);
                nameFetchInFlight.remove(itemId);

                Rectangle r = list.getCellBounds(index, index);
                if (r != null) list.repaint(r);
                else list.repaint();
            });
        });
    }

    private void requestItemIcon(int itemId, JList<?> list, int index)
    {
        if (!iconFetchInFlight.add(itemId))
        {
            return;
        }

        clientThread.invokeLater(() ->
        {
            ImageIcon resolved = null;
            try
            {
                BufferedImage img = itemManager.getImage(itemId, 1, false);
                if (img != null)
                {
                    resolved = new ImageIcon(img);
                }
            }
            catch (Exception ignored)
            {
            }

            final ImageIcon finalIcon = resolved;
            SwingUtilities.invokeLater(() ->
            {
                if (finalIcon != null)
                {
                    itemIconCache.put(itemId, finalIcon);
                }
                iconFetchInFlight.remove(itemId);

                Rectangle r = list.getCellBounds(index, index);
                if (r != null) list.repaint(r);
                else list.repaint();
            });
        });
    }

    private void performManualRoll(java.awt.event.ActionEvent e)
    {
        if (rollAnimationManager.isRolling()) return;
        if (!rollAnimationManager.hasTradeablesReady()) return;

        final List<Integer> tradeableSnapshot;
        synchronized (allTradeableItems)
        {
            tradeableSnapshot = new ArrayList<>(allTradeableItems);
        }

        List<Integer> locked = new ArrayList<>();
        for (int id : tradeableSnapshot)
        {
            if (!rolledItemsManager.isRolled(id))
            {
                locked.add(id);
            }
        }

        if (locked.isEmpty()) return;

        rollAnimationManager.setManualRoll(true);
        rollAnimationManager.enqueueRoll(
                locked.get(new Random().nextInt(locked.size()))
        );
    }

    @Override
    public Dimension getPreferredSize()
    {
        Dimension d = super.getPreferredSize();
        int viewportH = getViewportHeight();
        if (viewportH > 0 && d.height < viewportH)
        {
            return new Dimension(d.width, viewportH);
        }
        return d;
    }

    private int getViewportHeight()
    {
        Container p = getParent();
        while (p != null && !(p instanceof JViewport))
        {
            p = p.getParent();
        }
        if (p != null)
        {
            return Math.max(((JViewport) p).getHeight(), 0);
        }
        return 0;
    }
}
