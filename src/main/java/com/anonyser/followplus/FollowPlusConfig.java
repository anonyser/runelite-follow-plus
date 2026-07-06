package com.anonyser.followplus;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(FollowPlusConfig.GROUP)
public interface FollowPlusConfig extends Config
{
	String GROUP = "followplus";

	// Config tooltips render as HTML (ConfigPanel wraps name + description in <html> tags), so
	// <br> gives real line breaks — without them a long tooltip runs off the screen in one line.

	@ConfigSection(
		name = "Display",
		description = "Where the status is shown.",
		position = 0
	)
	String DISPLAY = "display";

	@ConfigSection(
		name = "Status lines",
		description = "Follow status, combat, team and run energy.",
		position = 1
	)
	String STATUS = "status";

	@ConfigSection(
		name = "Zeal",
		description = "Zeal Tokens, lifetime Zeal and session tracking.",
		position = 2
	)
	String ZEAL = "zeal";

	@ConfigSection(
		name = "Soul Wars match",
		description = "Live match stats while in a game.",
		position = 3
	)
	String MATCH = "match";

	// ----- Display -----

	@ConfigItem(
		keyName = "showOverlay",
		name = "Show overlay",
		description = "Draw the status overlay inside the game<br>"
			+ "window. Off by default in favour of the<br>"
			+ "external window, so you can minimise the<br>"
			+ "client and still see it.",
		position = 0,
		section = DISPLAY
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
		position = 1,
		section = DISPLAY
	)
	default boolean externalWindow()
	{
		return true;
	}

	// ----- Status lines -----

	@ConfigItem(
		keyName = "showTeam",
		name = "Show team",
		description = "Show whether you are on the Blue or Red<br>"
			+ "team, in that colour, next to the in-game<br>"
			+ "banner.",
		position = 0,
		section = STATUS
	)
	default boolean showTeam()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFollowingStatus",
		name = "Show following status",
		description = "Show who you are currently following, or<br>"
			+ "\"Not following anyone\" in red when you<br>"
			+ "are not.",
		position = 1,
		section = STATUS
	)
	default boolean showFollowingStatus()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMovingIndicator",
		name = "Show moving / not moving",
		description = "While following, add (moving) in green or<br>"
			+ "(not moving) in red so you can tell at a<br>"
			+ "glance if your target has stopped.",
		position = 2,
		section = STATUS
	)
	default boolean showMovingIndicator()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showHpWarning",
		name = "Show hitpoints and combat warning",
		description = "Show your current and maximum hitpoints,<br>"
			+ "plus when you are fighting the Avatar.",
		position = 3,
		section = STATUS
	)
	default boolean showHpWarning()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPrayerTimer",
		name = "Show prayer depletion timer",
		description = "Show your active prayers, prayer points<br>"
			+ "and how long your prayer will last at the<br>"
			+ "current drain.",
		position = 4,
		section = STATUS
	)
	default boolean showPrayerTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showRunEnergy",
		name = "Show run energy",
		description = "Show your current run energy percentage.",
		position = 5,
		section = STATUS
	)
	default boolean showRunEnergy()
	{
		return false;
	}

	// ----- Zeal -----

	@ConfigItem(
		keyName = "showCurrentZeal",
		name = "Current Zeal Tokens",
		description = "Show your spendable Zeal Token balance.",
		position = 0,
		section = ZEAL
	)
	default boolean showCurrentZeal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showSessionZeal",
		name = "Session Zeal",
		description = "Show Zeal earned since this session started.",
		position = 2,
		section = ZEAL
	)
	default boolean showSessionZeal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLastGameZeal",
		name = "Last game Zeal",
		description = "Show the Zeal earned in your most recent<br>"
			+ "finished game.",
		position = 3,
		section = ZEAL
	)
	default boolean showLastGameZeal()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLifetimeZeal",
		name = "Lifetime Zeal",
		description = "Show your lifetime Soul Wars Zeal score.",
		position = 4,
		section = ZEAL
	)
	default boolean showLifetimeZeal()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showSessionTimer",
		name = "Session time",
		description = "Show how long the current session has run.<br>"
			+ "Handy for taking a break before the 6-hour<br>"
			+ "logout.",
		position = 5,
		section = ZEAL
	)
	default boolean showSessionTimer()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showZealPerHour",
		name = "Zeal per hour",
		description = "Show estimated Zeal earned per hour, based<br>"
			+ "on this session's zeal and time.",
		position = 6,
		section = ZEAL
	)
	default boolean showZealPerHour()
	{
		return false;
	}

	// ----- Soul Wars match -----

	@ConfigItem(
		keyName = "showAvatarKills",
		name = "Avatar kills",
		description = "Show each team's avatar kill count (x/5).",
		position = 0,
		section = MATCH
	)
	default boolean showAvatarKills()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showAvatarHealth",
		name = "Avatar health",
		description = "Show each avatar's health percentage.",
		position = 1,
		section = MATCH
	)
	default boolean showAvatarHealth()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showAvatarStrength",
		name = "Avatar strength",
		description = "Show each avatar's strength percentage.",
		position = 2,
		section = MATCH
	)
	default boolean showAvatarStrength()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showAvatarDamage",
		name = "Your avatar damage",
		description = "Show the damage you have dealt to the enemy<br>"
			+ "Avatar this game.",
		position = 3,
		section = MATCH
	)
	default boolean showAvatarDamage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCaps",
		name = "Your captures",
		description = "Show how many areas you have helped capture<br>"
			+ "this game.",
		position = 4,
		section = MATCH
	)
	default boolean showCaps()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showFragments",
		name = "Fragments sacrificed",
		description = "Show how many soul fragments you have<br>"
			+ "sacrificed this game.",
		position = 5,
		section = MATCH
	)
	default boolean showFragments()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showBones",
		name = "Bones buried",
		description = "Show how many bones you have buried this<br>"
			+ "game.",
		position = 6,
		section = MATCH
	)
	default boolean showBones()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showActivityTimer",
		name = "Activity timer",
		description = "Show the activity bar and how long until it<br>"
			+ "runs out if you stay idle.",
		position = 7,
		section = MATCH
	)
	default boolean showActivityTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showGameTimer",
		name = "Game timer",
		description = "Show the time left in the current game.",
		position = 8,
		section = MATCH
	)
	default boolean showGameTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLobbyInfo",
		name = "Lobby info",
		description = "In the Soul Wars lobby, show how many<br>"
			+ "players are waiting and the time until the<br>"
			+ "next game starts.",
		position = 9,
		section = MATCH
	)
	default boolean showLobbyInfo()
	{
		return true;
	}

}
