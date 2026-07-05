package com.anonyser.followplus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * The always-visible utility window: follow status, HP + combat warning, prayers + depletion
 * estimate, and the Soul Wars timers while inside a game. Movable like any RuneLite overlay.
 */
class FollowPlusOverlay extends OverlayPanel
{
	private static final Color RED = new Color(216, 60, 62);
	private static final Color GREEN = new Color(0, 200, 83);
	private static final Color AMBER = new Color(255, 200, 60);
	private static final int MAX_PRAYER_LINES = 4;

	private final FollowPlusPlugin plugin;
	private final FollowPlusConfig config;
	private final Client client;

	@Inject
	FollowPlusOverlay(FollowPlusPlugin plugin, FollowPlusConfig config, Client client)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.showOverlay())
		{
			return null;
		}

		panelComponent.getChildren().clear();
		panelComponent.setPreferredSize(new Dimension(190, 0));
		panelComponent.getChildren().add(TitleComponent.builder().text("Follow Plus").build());

		if (config.showFollowingStatus())
		{
			final String target = plugin.getFollowTargetName();
			if (target != null)
			{
				addLine("Following", target, GREEN);
			}
			else
			{
				addLeftLine("Not following anyone", RED);
			}
		}

		if (config.showHpWarning())
		{
			addLine("HP", client.getBoostedSkillLevel(Skill.HITPOINTS)
				+ " / " + client.getRealSkillLevel(Skill.HITPOINTS), null);
			switch (plugin.getAttackStatus())
			{
				case UNDER_ATTACK:
					addLeftLine("Currently being attacked", RED);
					break;
				case POSSIBLE:
					addLeftLine("Possibly being attacked", AMBER);
					break;
				default:
					break;
			}
		}

		if (config.showPrayerTimer())
		{
			final int points = client.getBoostedSkillLevel(Skill.PRAYER);
			addLine("Prayer", points + " / " + client.getRealSkillLevel(Skill.PRAYER), null);
			final List<String> prayers = plugin.getActivePrayerNames();
			if (prayers.isEmpty())
			{
				addLine("Active", "none", null);
			}
			else
			{
				for (int i = 0; i < prayers.size() && i < MAX_PRAYER_LINES; i++)
				{
					addLeftLine("· " + prayers.get(i), null);
				}
				if (prayers.size() > MAX_PRAYER_LINES)
				{
					addLeftLine("· +" + (prayers.size() - MAX_PRAYER_LINES) + " more", null);
				}
				final double seconds = PrayerDrainCalculator.secondsRemaining(
					points, plugin.getActiveDrainEffect(), plugin.getPrayerBonus());
				addLine("Depletes in", seconds < 0 ? "—" : "~" + TimeFormat.mmss(seconds),
					seconds >= 0 && seconds < 30 ? RED : null);
			}
		}

		if (plugin.isInSoulWars())
		{
			if (config.showActivityTimer())
			{
				final int value = plugin.getActivityValue();
				addLine("Activity", value >= 0
					? Math.round(value * 100f / FollowPlusPlugin.MAX_ACTIVITY) + "%"
					: "Unknown", null);
				final double left = plugin.getActivitySecondsLeft();
				addLine("Inactive in", left >= 0 ? "~" + TimeFormat.mmss(left) : "Unknown",
					left >= 0 && left < 30 ? RED : null);
			}
			if (config.showGameTimer())
			{
				final int seconds = plugin.getGameSecondsLeft();
				addLine("Game time left", seconds >= 0 ? TimeFormat.mmss(seconds) : "Unknown", null);
			}
		}

		return super.render(graphics);
	}

	private void addLine(String left, String right, Color rightColor)
	{
		final LineComponent.LineComponentBuilder builder = LineComponent.builder().left(left).right(right);
		if (rightColor != null)
		{
			builder.rightColor(rightColor);
		}
		panelComponent.getChildren().add(builder.build());
	}

	private void addLeftLine(String left, Color leftColor)
	{
		final LineComponent.LineComponentBuilder builder = LineComponent.builder().left(left);
		if (leftColor != null)
		{
			builder.leftColor(leftColor);
		}
		panelComponent.getChildren().add(builder.build());
	}
}
