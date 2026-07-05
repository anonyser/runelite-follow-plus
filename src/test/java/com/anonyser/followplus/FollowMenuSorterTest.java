package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class FollowMenuSorterTest
{
	@Test
	public void movesFollowAboveOtherPlayerOptions()
	{
		// array order is bottom-to-top: Cancel, Walk here, Follow, Trade, Attack
		final boolean[] isFollow = {false, false, true, false, false};
		// Follow (index 2) should end up last (top of menu), everything else keeps its order
		assertArrayEquals(new int[]{0, 1, 3, 4, 2}, FollowMenuSorter.followsToTop(isFollow));
	}

	@Test
	public void noChangeWhenFollowAlreadyOnTop()
	{
		// Follow already at the highest index = already the top entry
		assertNull(FollowMenuSorter.followsToTop(new boolean[]{false, false, false, true}));
	}

	@Test
	public void noChangeWithoutFollowEntries()
	{
		assertNull(FollowMenuSorter.followsToTop(new boolean[]{false, false, false}));
		assertNull(FollowMenuSorter.followsToTop(new boolean[]{}));
	}

	@Test
	public void multipleFollowsKeepTheirRelativeOrder()
	{
		// two players on one tile: Cancel, Follow A, Attack A, Follow B, Attack B
		final boolean[] isFollow = {false, true, false, true, false};
		// both Follows move to the top; B's Follow (higher index = previously higher in the
		// menu) stays above A's
		assertArrayEquals(new int[]{0, 2, 4, 1, 3}, FollowMenuSorter.followsToTop(isFollow));
	}

	@Test
	public void singleEntryMenus()
	{
		assertNull(FollowMenuSorter.followsToTop(new boolean[]{true}));
		assertNull(FollowMenuSorter.followsToTop(new boolean[]{false}));
	}
}
