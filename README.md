# Chance Man

## Overview

**Chance Man** is a RuneLite plugin that locks tradeable items behind a random roll system, separating **unlocking** an item from **obtaining** it. It is designed for players who enjoy adding extra randomness, progression, or self-imposed restrictions to their gameplay.

Chance Man tracks two distinct states for every item — **Obtained** and **Rolled** — and uses both to control when items become usable. Progress is saved per character and synced via RuneLite across sessions and machines.

---

## Core Concepts

### Obtained Items
Items you have actually received in-game, such as:
- NPC drops
- Loot from chests or rewards
- Ground items you pick up

Obtaining an item does **not** automatically make it usable.

### Rolled Items
Items that have been unlocked by the roll system (legacy: *Unlocked*).  
A rolled item is *allowed* to be used **once you actually obtain it**.

### Usable Items
An item is fully usable **only when it has been both obtained and rolled**.

This distinction is reflected clearly in chat messages, for example:

> Rolled *Guthix robe legs* by obtaining *Swift albatross feather*

---

## Features

### Locking Mechanic

- All tradeable items (excluding coins) start locked.
- Locked items cannot be equipped, used, eaten, or otherwise interacted with (except examine/drop).
- Items become usable **only after they have been both rolled and obtained**.

---

### Rolling System

- When you obtain a locked item for the first time, Chance Man triggers a roll animation.
- The roll selects a random locked item to unlock.
- Roll results are announced in chat, showing:
    - the item that was rolled (unlocked), and
    - the item whose obtain triggered the roll.
- A **Roll** button in the sidebar allows you to manually unlock a random locked item if any remain.

---

### Automatic Detection

- Rolls trigger automatically when:
    - you encounter locked items on the ground, or
    - you receive locked items in your inventory (drops, rewards, etc.).
- Each item ID is processed only once per state to prevent repeated rolls from duplicate drops.

---

### Sidebar Panel

- The Chance Man side panel shows progress in a single searchable list.
- A dropdown selects which list is shown:
    - **Rolled** – items that have been rolled (unlocked)
    - **Obtained** – items you have obtained in-game
    - **Rolled, not Obtained** – items you are allowed to obtain but haven’t yet
    - **Usable** – items that are both rolled and obtained
- Lists are ordered newest-first.
- Tooltips explain what each filter represents.
- The panel layout fills the sidebar cleanly without unused space.
- A Discord icon links to the community server.

---

### Show Drops Menu

- Right-click an NPC and choose **Show Drops** to fetch its drop table from the wiki.
- You can also search for NPCs or drop tables using the Music tab search.
- The Show Drops view displays:
    - item icons from the NPC’s drop table,
    - a progress bar showing how many of those items you have **obtained**,
    - visual dimming for drops you have not yet obtained,
    - item names on hover.
- This view tracks **obtained items only**, independent of roll/unlock status.
- Drop tables can be sorted by rarity.

---

### Visual Feedback

- Locked item icons are dimmed across interfaces (inventory, GE, etc.).
- Dimming strength is configurable.
- Locked items can be hidden or visually suppressed in Grand Exchange searches until rolled.

---

### Persistence & Sync

- Progress is stored per character.
- Data is saved locally and mirrored to RuneLite’s cloud profile.
- Progress automatically syncs across machines.
- Legacy data is migrated where applicable.

---

## Configuration

Open RuneLite’s plugin settings and select **Chance Man** to configure options such as:

- **Free To Play Mode**
- **Include F2P trade-only items**
- **Roll Item Sets**
- **Roll Flatpacks**
- **Weapon Poison Unlock Requirements**
- **Enable Roll Sounds**
- **Grand Exchange roll requirements**
- **Rolled item chat color**
- **Unlocked item chat color**
- **Sort drops by rarity**
- **Show rare drop table**
- **Show gem drop table**
    - Toggling either clears cached drop data
- **Dim locked items**
- **Dim opacity**

---

## Usage

1. **Enable the Plugin**
    - Enable **Chance Man** in RuneLite.
    - Tradeable items are automatically detected and locked.

2. **Obtaining Items**
    - When you obtain a locked item, Chance Man triggers a roll if that item hasn’t already done so.
    - The item remains unusable until it has been rolled.

3. **Manual Rolls**
    - Use the **Roll** button in the Chance Man panel to manually unlock a random locked item.

4. **Tracking Progress**
    - Open the Chance Man side panel to view obtained, rolled, and usable items.
    - Use the dropdown and search bar to filter and find items.

5. **Restrictions**
    - Locked items cannot be used, equipped, eaten, or otherwise interacted with.
    - Once rolled and obtained, items behave normally.

---

## File Locations

Progress is stored per character under:
- **Obtained Items**  
  `chanceman_obtained.json`
- **Rolled Items**  
  `chanceman_rolled.json`

### Legacy Files

Older installs may contain:

- `chanceman_unlocked.json`

Legacy data is automatically migrated or merged where applicable.

---

## Contribution

Contributions, bug reports, and UX feedback are welcome.  
If something feels confusing or incorrect, please open an issue or submit a pull request.

---

## Contact

For questions, support, or feature requests, open a GitHub issue or contact:

**monstermonitor@proton.me**
