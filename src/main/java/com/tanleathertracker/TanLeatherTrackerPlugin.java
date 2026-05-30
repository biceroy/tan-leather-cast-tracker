package com.tanleathertracker;

import com.google.inject.Provides;
import java.awt.event.MouseEvent;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.Notification;
import net.runelite.client.events.ConfigChanged;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@PluginDescriptor(
	name = "Tan Leather Cast Tracker",
	description = "Cast counter and timer for the Tan Leather spell",
	tags = {"magic", "crafting", "lunar", "tan", "leather", "tracker", "cow", "snake", "dragon"}
)
public class TanLeatherTrackerPlugin extends Plugin implements MouseListener
{
	private static final Logger log = LoggerFactory.getLogger(TanLeatherTrackerPlugin.class);

	// Hides consumed by the Lunar Tan Leather spell
	static final Set<Integer> HIDE_ITEM_IDS = Set.of(
		1739, // Cowhide
		7801, // Snake hide
		1753, // Green dragon hide
		1751, // Blue dragon hide
		1749, // Red dragon hide
		1747  // Black dragon hide
	);

	// Cast cooldown — the actual re-cast window is shorter than the full 3-tick animation.
	// Empirically ~1.3s feels right; the animation finishes its tail visually after the
	// spell can already be re-queued.
	private static final long CAST_DURATION_MS = 1300L;

	// Tolerance between the Tan Leather animation firing and inventory updating
	private static final long ANIM_TO_INV_TOLERANCE_MS = 3500L;

	// The generic Lunar cast animation Tan Leather plays — paired with a hide-count drop
	// to disambiguate from other spells using the same animation
	private static final int TAN_LEATHER_CAST_ANIM = AnimationID.HUMAN_CASTLOWLVLALCHEMY;

	// Varbits.SPELLBOOK value for the Lunar spellbook
	private static final int LUNAR_SPELLBOOK_VARBIT_VALUE = 2;

	// How long the counter stays visible after the last cast even when inventory is empty
	private static final long COUNTER_LINGER_MS = 10_000L;

	// How long the "Out of runes" warning stays visible after a failed cast attempt
	private static final long OUT_OF_RUNES_DISPLAY_MS = 30_000L;

	@Inject
	private Client client;

	@Inject
	private TanLeatherTrackerConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private Notifier notifier;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private TanLeatherTrackerOverlay overlay;

	@Inject
	private TanLeatherDimOverlay dimOverlay;

	@Inject
	private TanLeatherSpellHighlightOverlay spellHighlightOverlay;

	@Inject
	private OverlayManager overlayManager;

	private int castCount = 0;
	private int maxCasts = 0;
	private int prevHideCount = -1;
	private int currentHideCount = 0;
	private boolean hasRelevantItems = false;
	private boolean lowHides = false;
	private boolean sessionActive = false;
	private boolean notifiedLowHides = false;
	// True after a cast actually crosses the 5→<5 threshold. Reset on restock or mouse dismiss.
	// Starts false so flipping the config option doesn't immediately dim.
	private boolean dimArmed = false;
	private boolean mouseListenerRegistered = false;
	private long lastCastMillis = 0L;
	private long lastTanAnimMillis = 0L;
	private long lastOutOfRunesMillis = 0L;
	private boolean lastCastInefficient = false;

	public int getCastCount()
	{
		return castCount;
	}

	public int getMaxCasts()
	{
		return maxCasts;
	}

	public boolean hasRelevantItems()
	{
		return hasRelevantItems;
	}

	public boolean hasEnoughHidesForCast()
	{
		return currentHideCount >= 5;
	}

	public int getCurrentHideCount()
	{
		return currentHideCount;
	}

	public boolean isInefficient()
	{
		return hasRelevantItems && (currentHideCount % 5) != 0;
	}

	// Conventional max efficient load — 25 hides (5 full 5-hide casts).
	// Beyond this, the user has more than they can usefully cast in one inventory.
	private static final int MAX_EFFICIENT_HIDES = 25;

	public int hidesShortOfFullCast()
	{
		int rem = currentHideCount % 5;
		return rem == 0 ? 0 : 5 - rem;
	}

	public boolean shouldSuggestDeposit()
	{
		return currentHideCount > MAX_EFFICIENT_HIDES && (currentHideCount % 5) != 0;
	}

	public int hidesToDeposit()
	{
		return currentHideCount % 5;
	}

	public boolean isLastCastInefficient()
	{
		return lastCastInefficient;
	}

	public boolean shouldShowCounter()
	{
		if (hasRelevantItems)
		{
			return true;
		}
		return lastCastMillis > 0
			&& (System.currentTimeMillis() - lastCastMillis) < COUNTER_LINGER_MS;
	}

	public boolean isOutOfRunes()
	{
		return lastOutOfRunesMillis > 0
			&& (System.currentTimeMillis() - lastOutOfRunesMillis) < OUT_OF_RUNES_DISPLAY_MS;
	}

	public long getCastRemainingMs()
	{
		long elapsed = System.currentTimeMillis() - lastCastMillis;
		long remaining = CAST_DURATION_MS - elapsed;
		return remaining > 0 ? remaining : 0L;
	}

	public boolean isCastTimerActive()
	{
		return lastCastMillis > 0 && getCastRemainingMs() > 0;
	}

	public boolean shouldShowCastTimer()
	{
		return config.showCastTimer() && isCastTimerActive();
	}

	public boolean shouldShowCastDim()
	{
		return shouldShowCastTimer() && config.castTimerDimsScreen();
	}

	public boolean shouldShowDim()
	{
		return sessionActive
			&& lowHides
			&& dimArmed
			&& !isCastTimerActive()
			&& config.lowHidesScreenDim();
	}

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		overlayManager.add(dimOverlay);
		overlayManager.add(spellHighlightOverlay);
		resetState();
		log.debug("Tan Leather Cast Tracker started");
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		overlayManager.remove(dimOverlay);
		overlayManager.remove(spellHighlightOverlay);
		resetState();
		log.debug("Tan Leather Cast Tracker stopped");
	}

	private void resetState()
	{
		castCount = 0;
		maxCasts = 0;
		prevHideCount = -1;
		currentHideCount = 0;
		sessionActive = false;
		notifiedLowHides = false;
		lowHides = false;
		dimArmed = false;
		lastCastMillis = 0L;
		lastTanAnimMillis = 0L;
		lastOutOfRunesMillis = 0L;
		lastCastInefficient = false;
		if (mouseListenerRegistered)
		{
			mouseManager.unregisterMouseListener(this);
			mouseListenerRegistered = false;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!"tanleathertracker".equals(event.getGroup())
			|| !"lowHidesNotification".equals(event.getKey()))
		{
			return;
		}
		// Force Customize/override on whenever the notification is enabled, so the user's
		// per-trigger settings always apply. Avoids confusion where toggling the notification
		// 'on' silently falls back to RuneLite global settings.
		Notification n = config.lowHidesNotification();
		if (n.isEnabled() && !n.isOverride())
		{
			Notification forced = new Notification(
				n.isEnabled(),
				true,                       // initialized
				true,                       // override
				n.isTray(),
				n.getTrayIconType(),
				n.getRequestFocus(),
				n.getSound(),
				n.getSoundName(),
				n.getVolume(),
				n.getTimeout(),
				n.isGameMessage(),
				n.getFlash(),
				n.getFlashColor(),
				n.isSendWhenFocused()
			);
			configManager.setConfiguration("tanleathertracker", "lowHidesNotification", forced);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		ChatMessageType type = event.getType();
		if (type != ChatMessageType.GAMEMESSAGE && type != ChatMessageType.SPAM)
		{
			return;
		}
		String msg = event.getMessage();
		if (msg == null)
		{
			return;
		}
		// Matches both 'You do not have enough runes to cast this spell.'
		// and 'You do not have enough Nature Runes to cast this spell.' etc.
		String lower = msg.toLowerCase();
		if (lower.contains("do not have enough") && lower.contains("to cast this spell"))
		{
			lastOutOfRunesMillis = System.currentTimeMillis();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Player local = client.getLocalPlayer();
		if (local == null || event.getActor() != local)
		{
			return;
		}
		int anim = local.getAnimation();
		if (anim != TAN_LEATHER_CAST_ANIM)
		{
			return;
		}
		// Animation 712 is shared with Low Alchemy on the Standard spellbook.
		// Require the player to be on the Lunar book before treating this as a Tan Leather cast.
		if (client.getVarbitValue(VarbitID.SPELLBOOK) != LUNAR_SPELLBOOK_VARBIT_VALUE)
		{
			return;
		}
		long now = System.currentTimeMillis();
		lastTanAnimMillis = now;
		// Only start the countdown if another cast can follow this one.
		// On the final cast (<=5 hides about to be consumed) the timer is just noise.
		if (currentHideCount > 5)
		{
			lastCastMillis = now;
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != InventoryID.INV)
		{
			return;
		}

		currentHideCount = countHides(event.getItemContainer());
		hasRelevantItems = currentHideCount > 0;

		long now = System.currentTimeMillis();
		boolean recentTanAnim = (now - lastTanAnimMillis) < ANIM_TO_INV_TOLERANCE_MS;
		boolean castDetected = prevHideCount != -1
			&& currentHideCount < prevHideCount
			&& recentTanAnim;
		boolean restocked = prevHideCount != -1 && currentHideCount > prevHideCount;

		// Mid-cycle restock — banking and withdrawing fresh hides resets the cycle
		if (restocked)
		{
			castCount = 0;
		}

		// Cast count logic — maxCasts counts only full 5-hide (efficient) casts
		if (castDetected)
		{
			if (castCount == 0)
			{
				// Lock in maxCasts from the hide count before this cast
				maxCasts = prevHideCount / 5;
			}
			castCount++;
			if (maxCasts > 0 && castCount >= maxCasts)
			{
				castCount = 0;
			}
			else if (maxCasts == 0)
			{
				// Inefficient cast on <5 hides — keep counter at 0
				castCount = 0;
			}
			sessionActive = true;
			lastCastInefficient = (prevHideCount - currentHideCount) < 5;
			// Successful cast clears any stale 'out of runes' warning
			lastOutOfRunesMillis = 0L;
			// Arm the dim only if this specific cast pushed us below 5
			if (prevHideCount >= 5 && currentHideCount < 5)
			{
				dimArmed = true;
			}
		}

		// Between cycles — sync maxCasts to current hide count
		if (castCount == 0)
		{
			maxCasts = currentHideCount / 5;
		}

		// Update low hides flag
		lowHides = currentHideCount < 5;

		// Fire the configurable notification once when hides drop below 5 after casting
		if (sessionActive && lowHides && !notifiedLowHides)
		{
			log.debug("Firing low-hides notification (currentHides={})", currentHideCount);
			notifier.notify(config.lowHidesNotification(), "Not enough hides for Tan Leather!");
			notifiedLowHides = true;
		}

		// Register mouse listener only when dim first becomes active
		boolean dimShouldShow = sessionActive
			&& lowHides
			&& dimArmed
			&& config.lowHidesScreenDim();

		if (dimShouldShow && !mouseListenerRegistered)
		{
			mouseManager.registerMouseListener(this);
			mouseListenerRegistered = true;
		}

		// Reset notifier + dim state when restocked to 5 or more
		if (currentHideCount >= 5)
		{
			notifiedLowHides = false;
			dimArmed = false;
		}

		prevHideCount = currentHideCount;
	}

	private int countHides(ItemContainer container)
	{
		int count = 0;
		Item[] items = container.getItems();
		if (items == null)
		{
			return 0;
		}
		for (Item item : items)
		{
			if (item != null && HIDE_ITEM_IDS.contains(item.getId()))
			{
				count += item.getQuantity();
			}
		}
		return count;
	}

	// --- MouseListener ---

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		if (mouseListenerRegistered)
		{
			dimArmed = false;
			mouseManager.unregisterMouseListener(this);
			mouseListenerRegistered = false;
		}
		return event;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event) { return event; }

	@Override
	public MouseEvent mousePressed(MouseEvent event) { return event; }

	@Override
	public MouseEvent mouseReleased(MouseEvent event) { return event; }

	@Override
	public MouseEvent mouseEntered(MouseEvent event) { return event; }

	@Override
	public MouseEvent mouseExited(MouseEvent event) { return event; }

	@Override
	public MouseEvent mouseDragged(MouseEvent event) { return event; }

	@Provides
	TanLeatherTrackerConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(TanLeatherTrackerConfig.class);
	}
}
