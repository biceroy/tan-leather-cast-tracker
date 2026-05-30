package com.tanleathertracker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.BasicStroke;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class TanLeatherSpellHighlightOverlay extends Overlay
{
	// Varbits.SPELLBOOK value for the Lunar spellbook
	private static final int LUNAR_SPELLBOOK_VARBIT_VALUE = 2;

	private static final Stroke OUTLINE_STROKE = new BasicStroke(2f);

	// How many top-level children of the spellbook interface to scan
	private static final int MAX_TOP_LEVEL_CHILDREN = 64;

	private final Client client;
	private final TanLeatherTrackerPlugin plugin;
	private final TanLeatherTrackerConfig config;

	@Inject
	public TanLeatherSpellHighlightOverlay(Client client, TanLeatherTrackerPlugin plugin, TanLeatherTrackerConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showSpellHighlight()
			|| !plugin.hasEnoughHidesForCast()
			|| client.getVarbitValue(VarbitID.SPELLBOOK) != LUNAR_SPELLBOOK_VARBIT_VALUE)
		{
			return null;
		}

		Widget spell = findTanLeatherWidget();
		if (spell == null || spell.isHidden())
		{
			return null;
		}

		Rectangle bounds = spell.getBounds();
		if (bounds == null || bounds.width <= 0 || bounds.height <= 0)
		{
			return null;
		}

		graphics.setColor(config.spellHighlightColor());
		graphics.setStroke(OUTLINE_STROKE);
		graphics.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);

		return null;
	}

	private Widget findTanLeatherWidget()
	{
		for (int childId = 0; childId < MAX_TOP_LEVEL_CHILDREN; childId++)
		{
			Widget container = client.getWidget(InterfaceID.MAGIC_SPELLBOOK, childId);
			if (container == null)
			{
				continue;
			}
			Widget hit = searchForTanLeather(container);
			if (hit != null)
			{
				return hit;
			}
		}
		return null;
	}

	private Widget searchForTanLeather(Widget root)
	{
		if (root == null)
		{
			return null;
		}

		String name = root.getName();
		if (name != null && name.toLowerCase().contains("tan leather"))
		{
			return root;
		}

		Widget hit = searchArray(root.getDynamicChildren());
		if (hit != null) return hit;

		hit = searchArray(root.getStaticChildren());
		if (hit != null) return hit;

		hit = searchArray(root.getNestedChildren());
		return hit;
	}

	private Widget searchArray(Widget[] children)
	{
		if (children == null)
		{
			return null;
		}
		for (Widget child : children)
		{
			Widget hit = searchForTanLeather(child);
			if (hit != null)
			{
				return hit;
			}
		}
		return null;
	}
}
