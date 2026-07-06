package com.anonyser.followplus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * The in-game overlay. It only renders the status model the plugin builds on the game tick, so it
 * touches no client state itself.
 */
class FollowPlusOverlay extends OverlayPanel
{
	private final FollowPlusPlugin plugin;
	private final FollowPlusConfig config;

	@Inject
	FollowPlusOverlay(FollowPlusPlugin plugin, FollowPlusConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}
		final List<StatusLine> model = plugin.getStatusModel();
		if (model.isEmpty())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(190, 0));
		panelComponent.getChildren().add(TitleComponent.builder().text("Soul Wars Status").build());

		final long now = System.currentTimeMillis();
		for (StatusLine line : model)
		{
			final Color color = line.flash ? StatusLine.flashColor(now) : line.color;
			if (line.header)
			{
				final TitleComponent.TitleComponentBuilder b = TitleComponent.builder().text(line.left);
				if (color != null)
				{
					b.color(color);
				}
				panelComponent.getChildren().add(b.build());
			}
			else if (line.right != null)
			{
				final LineComponent.LineComponentBuilder b = LineComponent.builder().left(line.left).right(line.right);
				if (color != null)
				{
					b.rightColor(color);
				}
				panelComponent.getChildren().add(b.build());
			}
			else
			{
				final LineComponent.LineComponentBuilder b = LineComponent.builder().left(line.left);
				if (color != null)
				{
					b.leftColor(color);
				}
				panelComponent.getChildren().add(b.build());
			}
		}

		return super.render(graphics);
	}
}
