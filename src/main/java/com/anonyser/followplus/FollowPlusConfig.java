package com.anonyser.followplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(FollowPlusConfig.GROUP)
public interface FollowPlusConfig extends Config
{
	String GROUP = "followplus";

	@ConfigItem(
		keyName = "followAtTop",
		name = "Follow at the Top",
		description = "Move the Follow option above Attack, Trade, Walk here and the rest when right-clicking another player",
		position = 0
	)
	default boolean followAtTop()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Show the Follow Plus status overlay on screen",
		position = 1
	)
	default boolean showOverlay()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFollowingStatus",
		name = "Show following status",
		description = "Show who you are currently following in the overlay",
		position = 2
	)
	default boolean showFollowingStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerTimer",
		name = "Show prayer depletion timer",
		description = "Show active prayers, prayer points and an estimated time until prayer reaches zero",
		position = 3
	)
	default boolean showPrayerTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showActivityTimer",
		name = "Show Soul Wars activity timer",
		description = "Inside Soul Wars, show the activity bar and an estimated time until it reaches zero",
		position = 4
	)
	default boolean showActivityTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGameTimer",
		name = "Show Soul Wars game timer",
		description = "Inside Soul Wars, show the estimated match time remaining",
		position = 5
	)
	default boolean showGameTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHpWarning",
		name = "Show hitpoints and combat warning",
		description = "Show current hitpoints and a warning while you are being attacked",
		position = 6
	)
	default boolean showHpWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "debugLogging",
		name = "Debug logging",
		description = "Log menu reordering, follow changes and Soul Wars widget contents to the client log",
		position = 7
	)
	default boolean debugLogging()
	{
		return false;
	}
}
