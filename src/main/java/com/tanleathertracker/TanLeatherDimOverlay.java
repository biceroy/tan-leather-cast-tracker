package com.tanleathertracker;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class TanLeatherDimOverlay extends Overlay
{
	private static final Color DIM_COLOR = new Color(0, 0, 0, 180);
	private static final Color CAST_DIM_COLOR = new Color(0, 0, 0, 120);
	private static final Color TEXT_COLOR = Color.WHITE;
	private static final Color INEFFICIENT_COLOR = new Color(255, 200, 0);
	private static final String OUT_OF_HIDES_MESSAGE = "Out of hides!";
	private static final String INEFFICIENT_SUBLINE = "Inefficient cast — same runes, fewer hides";

	private final Client client;
	private final TanLeatherTrackerPlugin plugin;

	@Inject
	public TanLeatherDimOverlay(Client client, TanLeatherTrackerPlugin plugin)
	{
		this.client = client;
		this.plugin = plugin;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.shouldShowCastTimer())
		{
			return renderCastTimer(graphics);
		}

		if (plugin.shouldShowDim())
		{
			return renderOutOfHides(graphics);
		}

		return null;
	}

	private Dimension renderCastTimer(Graphics2D graphics)
	{
		Dimension size = client.getRealDimensions();

		if (plugin.shouldShowCastDim())
		{
			graphics.setColor(CAST_DIM_COLOR);
			graphics.fillRect(0, 0, size.width, size.height);
		}

		double seconds = plugin.getCastRemainingMs() / 1000.0;
		String text = String.format("%.1f", seconds);

		graphics.setColor(TEXT_COLOR);
		graphics.setFont(new Font("Arial", Font.BOLD, 48));
		FontMetrics fm = graphics.getFontMetrics();
		int x = (size.width - fm.stringWidth(text)) / 2;
		int y = (size.height / 2) + fm.getAscent() / 2;
		graphics.drawString(text, x, y);

		if (plugin.isLastCastInefficient())
		{
			graphics.setColor(INEFFICIENT_COLOR);
			graphics.setFont(new Font("Arial", Font.BOLD, 16));
			FontMetrics subFm = graphics.getFontMetrics();
			int subX = (size.width - subFm.stringWidth(INEFFICIENT_SUBLINE)) / 2;
			int subY = y + subFm.getHeight() + 4;
			graphics.drawString(INEFFICIENT_SUBLINE, subX, subY);
		}

		return size;
	}

	private Dimension renderOutOfHides(Graphics2D graphics)
	{
		Dimension size = client.getRealDimensions();

		graphics.setColor(DIM_COLOR);
		graphics.fillRect(0, 0, size.width, size.height);

		graphics.setColor(TEXT_COLOR);
		graphics.setFont(new Font("Arial", Font.BOLD, 24));
		FontMetrics fm = graphics.getFontMetrics();
		int x = (size.width - fm.stringWidth(OUT_OF_HIDES_MESSAGE)) / 2;
		int y = (size.height / 2) + fm.getAscent() / 2;
		graphics.drawString(OUT_OF_HIDES_MESSAGE, x, y);

		return size;
	}
}
