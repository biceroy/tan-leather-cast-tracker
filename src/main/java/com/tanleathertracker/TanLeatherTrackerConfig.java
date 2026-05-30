package com.tanleathertracker;

import java.awt.Color;
import java.awt.TrayIcon;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.FlashNotification;
import net.runelite.client.config.Notification;
import net.runelite.client.config.NotificationSound;
import net.runelite.client.config.RequestFocusType;

@ConfigGroup("tanleathertracker")
public interface TanLeatherTrackerConfig extends Config
{
	@ConfigSection(
		name = "Overlay",
		description = "Top-left cast counter overlay options",
		position = 0
	)
	String overlaySection = "overlay";

	@ConfigSection(
		name = "Cast timer",
		description = "Post-cast countdown overlay",
		position = 1
	)
	String castTimerSection = "castTimer";

	@ConfigSection(
		name = "Spellbook highlight",
		description = "Outline the Tan Leather icon on the Lunar spellbook",
		position = 2
	)
	String highlightSection = "highlight";

	@ConfigSection(
		name = "Out of hides screen dim",
		description = "Dim the screen when hides drop below 5",
		position = 3
	)
	String dimSection = "dim";

	@ConfigSection(
		name = "Low hides notification",
		description = "System notification when hides drop below 5",
		position = 4
	)
	String notifierSection = "notifier";

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show Overlay",
		description = "Show the cast count overlay when hides are in inventory",
		section = overlaySection
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showInefficientWarning",
		name = "Warn on inefficient tan",
		description = "Show a warning line when hide count is not a multiple of 5 — partial casts use the same runes for fewer hides",
		section = overlaySection
	)
	default boolean showInefficientWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCastTimer",
		name = "Show cast timer",
		description = "Show a countdown after each Tan Leather cast",
		section = castTimerSection
	)
	default boolean showCastTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "castTimerDimsScreen",
		name = "Dim screen during cast",
		description = "Dim the screen for the duration of the cast countdown",
		section = castTimerSection
	)
	default boolean castTimerDimsScreen()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSpellHighlight",
		name = "Highlight Tan Leather spell",
		description = "Outline Tan Leather on the Lunar spellbook when at least 5 tannable hides are in inventory",
		section = highlightSection
	)
	default boolean showSpellHighlight()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "spellHighlightColor",
		name = "Highlight colour",
		description = "Outline colour for the Tan Leather spell highlight",
		section = highlightSection
	)
	default Color spellHighlightColor()
	{
		return new Color(0, 255, 255, 200);
	}

	@ConfigItem(
		keyName = "lowHidesScreenDim",
		name = "Dim screen when out of hides",
		description = "Show a dark screen overlay with 'Out of hides!' when you run low. Move the mouse to dismiss.",
		section = dimSection
	)
	default boolean lowHidesScreenDim()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lowHidesNotification",
		name = "Low hides notification",
		description = "Fire a notification when hides drop below 5 after casting. Toggle ON for a single sound during gameplay; click to customise tray, volume, or in-game message.",
		section = notifierSection
	)
	default Notification lowHidesNotification()
	{
		// Pre-configured defaults — when the user toggles this on, it just works:
		// single native sound, no tray (avoids double ding), no flash, fires during gameplay.
		return new Notification(
			false,                          // enabled (off by default)
			true,                           // initialized — use the values below, not RuneLite globals
			true,                           // override
			false,                          // tray (off so it doesn't double up with the sound)
			TrayIcon.MessageType.NONE,
			RequestFocusType.OFF,
			NotificationSound.NATIVE,       // single RuneLite ding
			"",                             // custom sound path (unused)
			100,                            // volume
			10000,                          // timeout ms
			false,                          // game chat message
			FlashNotification.DISABLED,     // no red flash
			new Color(0x46FF0000, true),    // flash colour (unused while flash disabled)
			true                            // sendWhenFocused — fire during gameplay
		);
	}
}
