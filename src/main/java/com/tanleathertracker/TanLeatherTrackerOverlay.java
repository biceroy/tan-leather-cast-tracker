package com.tanleathertracker;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

public class TanLeatherTrackerOverlay extends OverlayPanel
{
	private static final Color INEFFICIENT_COLOR = new Color(255, 200, 0);
	private static final Color OUT_OF_RUNES_COLOR = new Color(255, 80, 80);

	private final TanLeatherTrackerPlugin plugin;
	private final TanLeatherTrackerConfig config;

	@Inject
	public TanLeatherTrackerOverlay(TanLeatherTrackerPlugin plugin, TanLeatherTrackerConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		boolean outOfRunes = plugin.isOutOfRunes();
		if (!config.showOverlay() || (!plugin.shouldShowCounter() && !outOfRunes))
		{
			return null;
		}

		if (plugin.shouldShowCounter())
		{
			int castCount = plugin.getCastCount();
			int maxCasts = plugin.getMaxCasts();
			String displayText = castCount + " / " + maxCasts;

			Color textColor = (castCount >= maxCasts && maxCasts > 0) ? Color.RED : Color.WHITE;

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Tan Casts:")
				.right(displayText)
				.rightColor(textColor)
				.build());
		}

		if (config.showInefficientWarning() && plugin.isInefficient())
		{
			String rightText;
			if (plugin.shouldSuggestDeposit())
			{
				rightText = "deposit " + plugin.hidesToDeposit();
			}
			else
			{
				rightText = "need " + plugin.hidesShortOfFullCast() + " more";
			}
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Inefficient")
				.right(rightText)
				.leftColor(INEFFICIENT_COLOR)
				.rightColor(INEFFICIENT_COLOR)
				.build());
		}

		if (outOfRunes)
		{
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Out of runes!")
				.leftColor(OUT_OF_RUNES_COLOR)
				.build());
		}

		return super.render(graphics);
	}
}
