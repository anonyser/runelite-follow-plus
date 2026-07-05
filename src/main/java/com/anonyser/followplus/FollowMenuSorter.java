package com.anonyser.followplus;

final class FollowMenuSorter
{
	private FollowMenuSorter()
	{
	}

	/**
	 * RuneLite draws the right-click menu top-down from the END of the entry array, so moving
	 * the Follow entries to the end puts them at the top of the menu. The move is stable: the
	 * relative order of everything else (and of multiple Follow entries, when several players
	 * share a tile) is untouched, and nothing is removed.
	 *
	 * @param isFollow flags marking which entries are Follow options on other players
	 * @return the new order as indexes into the original array, or null when nothing changes
	 */
	static int[] followsToTop(boolean[] isFollow)
	{
		final int n = isFollow.length;
		final int[] order = new int[n];
		int w = 0;
		for (int i = 0; i < n; i++)
		{
			if (!isFollow[i])
			{
				order[w++] = i;
			}
		}
		for (int i = 0; i < n; i++)
		{
			if (isFollow[i])
			{
				order[w++] = i;
			}
		}
		for (int i = 0; i < n; i++)
		{
			if (order[i] != i)
			{
				return order;
			}
		}
		return null;
	}
}
