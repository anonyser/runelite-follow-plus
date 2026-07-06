package com.anonyser.followplus;

import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * Per-game personal contribution counters that the game only shows on its end screen, so they're
 * tallied client-side (the same display-only approach the existing "Soul Wars" hub plugin uses):
 * <ul>
 *   <li><b>captures</b> — a capture chat line naming your team and an area, while you stand in it,</li>
 *   <li><b>fragments sacrificed</b> — the drop in your inventory fragment count during a game
 *       (Soul Wars is a safe minigame, so fragments only leave your pack by charging the obelisk;
 *       this avoids depending on the order the game fires the inventory update vs the chat line),</li>
 *   <li><b>bones buried</b> — the "bury the bones" line,</li>
 *   <li><b>avatar damage</b> — your hitsplats on the enemy Avatar, fed in from the plugin.</li>
 * </ul>
 * Counters reset each game; the inventory fragment count persists (it's account state, not a score).
 * The three capture areas are the verified Soul Wars world areas.
 */
class SoulWarsMatchTracker
{
	static final WorldArea WEST_GRAVEYARD = new WorldArea(2157, 2893, 11, 11, 0);
	static final WorldArea OBELISK = new WorldArea(2199, 2904, 16, 16, 0);
	static final WorldArea EAST_GRAVEYARD = new WorldArea(2248, 2920, 11, 11, 0);
	// The capture line fires when you may be a few tiles off the exact area, so count a capture when
	// you are within this many tiles of it rather than dead-on the box.
	static final int CAPTURE_TOLERANCE = 12;

	private int captures;
	private int fragmentsSacrificed;
	private int bonesBuried;
	private int avatarDamage;
	private int inventoryFragments;

	/** Clears the per-game counters (called when a game starts). Inventory count is left alone. */
	void reset()
	{
		captures = 0;
		fragmentsSacrificed = 0;
		bonesBuried = 0;
		avatarDamage = 0;
	}

	void addAvatarDamage(int amount)
	{
		if (amount > 0)
		{
			avatarDamage += amount;
		}
	}

	/**
	 * Feeds the current soul-fragment count in the inventory. A drop in the count during a game is
	 * counted as fragments sacrificed; the latest count is always remembered so the baseline is
	 * right when you next sacrifice.
	 */
	void onInventoryFragments(int count, boolean inGame)
	{
		final int c = Math.max(0, count);
		if (inGame && c < inventoryFragments)
		{
			fragmentsSacrificed += inventoryFragments - c;
		}
		inventoryFragments = c;
	}

	/**
	 * Applies a game chat line. The bones line is matched first. A capture only counts when the line
	 * names your team and an area you are currently standing in.
	 */
	void onChatMessage(String message, SoulWarsTeam team, WorldPoint location)
	{
		if (message == null || team == null || team == SoulWarsTeam.NONE)
		{
			return;
		}
		final String m = message.toLowerCase();
		if (m.contains("you bury the bones"))
		{
			bonesBuried++;
			return;
		}
		if (!m.contains(team.chatIdentifier()))
		{
			return;
		}
		final WorldArea area = capturedArea(m);
		if (area != null && location != null && area.distanceTo(location) <= CAPTURE_TOLERANCE)
		{
			captures++;
		}
	}

	/** The area a capture line refers to, or null if the line isn't a capture. */
	static WorldArea capturedArea(String lowerMessage)
	{
		if (lowerMessage.contains("western graveyard"))
		{
			return WEST_GRAVEYARD;
		}
		if (lowerMessage.contains("eastern graveyard"))
		{
			return EAST_GRAVEYARD;
		}
		if (lowerMessage.contains("soul obelisk"))
		{
			return OBELISK;
		}
		return null;
	}

	int captures()
	{
		return captures;
	}

	int fragmentsSacrificed()
	{
		return fragmentsSacrificed;
	}

	int bonesBuried()
	{
		return bonesBuried;
	}

	int avatarDamage()
	{
		return avatarDamage;
	}
}
