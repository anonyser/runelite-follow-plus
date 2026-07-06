package com.anonyser.followplus;

import net.runelite.api.coords.WorldPoint;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class SoulWarsMatchTrackerTest
{
	private static final WorldPoint IN_EAST_GRAVEYARD = new WorldPoint(2253, 2925, 0);
	private static final WorldPoint OUTSIDE = new WorldPoint(2100, 2900, 0);

	@Test
	public void fragmentsCountedOnInventoryDrop()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.onInventoryFragments(10, true); // holding 10 in a game
		t.onInventoryFragments(0, true);  // sacrificed all 10
		assertEquals(10, t.fragmentsSacrificed());
		t.onInventoryFragments(6, true);  // gathered 6 more
		t.onInventoryFragments(2, true);  // sacrificed 4
		assertEquals(14, t.fragmentsSacrificed());
	}

	@Test
	public void fragmentGainsAndOutOfGameDropsDoNotCount()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.onInventoryFragments(5, true);  // gaining fragments never counts
		t.onInventoryFragments(20, true);
		assertEquals(0, t.fragmentsSacrificed());
		t.onInventoryFragments(0, false); // a drop outside a game doesn't count
		assertEquals(0, t.fragmentsSacrificed());
	}

	@Test
	public void bonesBuriedCounts()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.onChatMessage("You bury the bones.", SoulWarsTeam.BLUE, OUTSIDE);
		t.onChatMessage("You bury the bones.", SoulWarsTeam.BLUE, OUTSIDE);
		assertEquals(2, t.bonesBuried());
	}

	@Test
	public void captureCountsWhenYourTeamAndInArea()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.onChatMessage("The red team has captured the eastern graveyard!", SoulWarsTeam.RED, IN_EAST_GRAVEYARD);
		assertEquals(1, t.captures());
	}

	@Test
	public void captureIgnoredWhenNotInArea()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.onChatMessage("The red team has captured the eastern graveyard!", SoulWarsTeam.RED, OUTSIDE);
		assertEquals(0, t.captures());
	}

	@Test
	public void captureIgnoredForEnemyTeam()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		// I'm red, message is about blue capturing - not my capture
		t.onChatMessage("The blue team has captured the eastern graveyard!", SoulWarsTeam.RED, IN_EAST_GRAVEYARD);
		assertEquals(0, t.captures());
	}

	@Test
	public void avatarDamageAccumulatesAndIgnoresNonPositive()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.addAvatarDamage(12);
		t.addAvatarDamage(0);
		t.addAvatarDamage(-5);
		t.addAvatarDamage(8);
		assertEquals(20, t.avatarDamage());
	}

	@Test
	public void resetClearsCountersButKeepsInventoryFragments()
	{
		final SoulWarsMatchTracker t = new SoulWarsMatchTracker();
		t.onInventoryFragments(5, true);
		t.addAvatarDamage(30);
		t.reset();
		assertEquals(0, t.avatarDamage());
		// inventory count survived the reset, so sacrificing now still counts from 5
		t.onInventoryFragments(0, true);
		assertEquals(5, t.fragmentsSacrificed());
	}

	@Test
	public void capturedAreaMapping()
	{
		assertSame(SoulWarsMatchTracker.WEST_GRAVEYARD,
			SoulWarsMatchTracker.capturedArea("captured the western graveyard"));
		assertSame(SoulWarsMatchTracker.EAST_GRAVEYARD,
			SoulWarsMatchTracker.capturedArea("captured the eastern graveyard"));
		assertSame(SoulWarsMatchTracker.OBELISK,
			SoulWarsMatchTracker.capturedArea("captured the soul obelisk"));
		assertNull(SoulWarsMatchTracker.capturedArea("nothing here"));
	}
}
