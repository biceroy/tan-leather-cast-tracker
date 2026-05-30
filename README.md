# Tan Leather Cast Tracker

A [RuneLite](https://runelite.net/) external plugin for Old School RuneScape that tracks your **Tan Leather** lunar spell casts, shows a per-cast countdown timer, highlights the spell in your spellbook, and warns you when an inventory will produce an inefficient cast.

---
![Tan Leather Cast Tracker demo](docs/demo.gif)

## What It Does

When you cast **Tan Leather** (Lunar Spellbook), each cast tans up to 5 hides at once. This plugin:

- **Counts your casts** for the current inventory cycle and shows a `current / max` display
- **Highlights Tan Leather** on the Lunar spellbook when you have at least 5 tannable hides
- **Shows a brief cast-countdown** so you can pace back-to-back casts without misclicking
- **Warns you on inefficient batches** when your hide count isn't a multiple of 5
- **Warns when you run out** with a sound notifier or screen dim

Supported hides (everything the Tan Leather spell can tan):
- Cowhide
- Snake hide
- Green dragon hide
- Blue dragon hide
- Red dragon hide
- Black dragon hide

---

## The Overlay

When tannable hides are detected in your inventory, a small overlay appears showing:

```
Tan Casts:   2 / 5
```

- **White** — efficient casts remaining in the current cycle
- **Red** — cycle complete (counter resets on the next cast)
- The max count is `floor(hides / 5)` — only full 5-hide casts count toward the maximum

The overlay lingers for ~10 seconds after the last cast even with an empty inventory so you can see the final tally.

### Inefficient warning

If your hide count is not a multiple of 5, a yellow line appears under the cast counter:

```
Inefficient   need 3 more
```

…and during the cast countdown, an inefficient cast displays a subline:

> *Inefficient cast — same runes, fewer hides*

---

## Cast Countdown Timer

Each cast triggers a brief centered countdown over a lightly dimmed screen. The timer starts when the cast animation begins and clears as soon as the spell is ready to be re-cast. It's suppressed on the final cast of a batch (when no further cast is possible) so you aren't dimmed mid-bank.

---

## Spellbook Highlight

When you're on the Lunar spellbook AND you have at least 5 tannable hides in your inventory, the Tan Leather icon is outlined in your chosen color so it's easier to find in the menu. The outline disappears as soon as you switch books or drop below 5 hides. Right-click still works normally for the soft/hard-leather toggle.

---

## Low Hides Warning

Once a session starts (at least one cast detected) and your hide count drops below 5, the plugin fires a warning. There are two notification modes selectable in the plugin settings:

### Sound / Tray Notification
A system tray notification pops up with the message:

> *Not enough hides for Tan Leather!*

The notification re-arms itself when you restock to 5 or more hides, so it will warn you again next time you run low.

> **Note:** Tray and sound notifications require RuneLite's built-in notification setting to be enabled.
> Go to **RuneLite Settings → Notifications** and make sure **"Notification sound"** and/or **"Send notifications"** are turned on.
> Also check that Windows Focus Assist / Do Not Disturb is not blocking notifications.

### Screen Dim
A dark overlay covers the game screen with a centred message:

> *Out of hides!*

Move your mouse to dismiss the dim. It will reappear if you restock and run low again.

---

## Settings

| Section | Setting | Description | Default |
|---|---|---|---|
| **Overlay** | Show Overlay | Show the cast count overlay when hides are in your inventory | Enabled |
| | Warn on inefficient tan | Yellow warning line when hide count is not a multiple of 5 | Enabled |
| **Cast timer** | Show cast timer | Show a brief countdown after each cast | Enabled |
| | Dim screen during cast | Lightly dim the screen behind the countdown | Enabled |
| **Spellbook highlight** | Highlight Tan Leather spell | Outline Tan Leather on the Lunar spellbook | Enabled |
| | Highlight colour | Outline colour | Cyan |
| **Low hides notifier** | Enable low hides notifier | Fire a warning when hides drop below 5 after casting | Enabled |
| | Notifier type | **Sound** (tray notification) or **Screen Dim** (dark overlay) | Sound |

---

## How the Cast Cycle Works

1. You start with some hides in your inventory (e.g. 25)
2. On your **first cast**, the plugin locks in `maxCasts = floor(25 / 5) = 5`
3. Each detected cast increments the counter: `1 / 5`, `2 / 5`, …
4. When the counter reaches `5 / 5`, it resets to `0` automatically
5. If you bank and withdraw a fresh batch mid-cycle, the counter resets and recomputes from the new total

The plugin pairs an **ItemContainerChanged** event (server-confirmed inventory drop) with the **Tan Leather cast animation** on the Lunar spellbook, so banking or dropping a hide never inflates the count.

---

## Installation

Search for **Tan Leather Cast Tracker** in the [RuneLite Plugin Hub](https://runelite.net/plugin-hub) and click Install.

### Running Locally (Development)

1. Clone this repository
2. Open the project in IntelliJ IDEA with a Java 11+ JDK configured
3. Run the main method in `src/test/java/com/tanleathertracker/TanLeatherTrackerPluginTest.java`
4. Log in via the RuneLite launcher that opens

---

## License

[BSD 2-Clause License](LICENSE)
