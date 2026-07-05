package com.anonyser.followplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FollowPlusConfig.GROUP)
public interface FollowPlusConfig extends Config
{
	String GROUP = "followplus";

	// Config tooltips render as HTML (ConfigPanel wraps name + description in <html> tags), so
	// <br> gives real line breaks — without them a long tooltip runs off the screen in one line.

	@ConfigItem(
		keyName = "followAtTop",
		name = "Follow at the Top",
		description = "Move the Follow option to the top when you<br>"
			+ "right-click another player, above Attack,<br>"
			+ "Trade, Walk here and the rest, so you don't<br>"
			+ "misclick when you only want to follow.",
		position = 0
	)
	default boolean followAtTop()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Draw the status overlay inside the game<br>"
			+ "window. Off by default in favour of the<br>"
			+ "external window, so you can minimise the<br>"
			+ "client and still see it.",
		position = 1
	)
	default boolean showOverlay()
	{
		return false;
	}

	@ConfigItem(
		keyName = "externalWindow",
		name = "External window",
		description = "Show the status in a separate always-on-top<br>"
			+ "window that stays visible when RuneLite is<br>"
			+ "behind other windows. Drag it to move it;<br>"
			+ "its position is remembered.",
		position = 2
	)
	default boolean externalWindow()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFollowingStatus",
		name = "Show following status",
		description = "Show who you are currently following, or<br>"
			+ "\"Not following anyone\" in red when you<br>"
			+ "are not.",
		position = 3
	)
	default boolean showFollowingStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerTimer",
		name = "Show prayer depletion timer",
		description = "Show your active prayers, prayer points<br>"
			+ "and how long your prayer will last at the<br>"
			+ "current drain.",
		position = 4
	)
	default boolean showPrayerTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showActivityTimer",
		name = "Show Soul Wars activity timer",
		description = "In Soul Wars, show the activity bar and<br>"
			+ "how long until it runs out if you stay<br>"
			+ "idle. Shows Unknown when it can't be read.",
		position = 5
	)
	default boolean showActivityTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGameTimer",
		name = "Show Soul Wars game timer",
		description = "In Soul Wars, show the time left in the<br>"
			+ "current game. Shows Unknown when it can't<br>"
			+ "be read.",
		position = 6
	)
	default boolean showGameTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLobbyInfo",
		name = "Show Soul Wars lobby info",
		description = "In the Soul Wars lobby, show how many<br>"
			+ "players are waiting and the time until the<br>"
			+ "next game starts.",
		position = 7
	)
	default boolean showLobbyInfo()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHpWarning",
		name = "Show hitpoints and combat warning",
		description = "Show your current and maximum hitpoints,<br>"
			+ "plus a warning when you are being attacked.",
		position = 8
	)
	default boolean showHpWarning()
	{
		return true;
	}
}
