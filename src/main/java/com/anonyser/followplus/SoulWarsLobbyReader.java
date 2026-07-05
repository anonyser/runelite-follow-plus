package com.anonyser.followplus;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads the Soul Wars lobby board (interface 434). Verified from live captures: the board holds
 * its labels and values in separate, consecutive widgets - "Players Waiting" then the count,
 * "Next Game Start" then a duration. The count and the timer both change format with lobby state:
 * below the minimum the game shows "5/20" and "-", and once enough players are in it switches to a
 * bare count ("28") and a real countdown ("3 minutes", "30 seconds"). Rather than parse any of
 * that, the value widget after each label is shown verbatim, so every format the game uses is
 * mirrored exactly. A value is ignored only if it is itself one of the labels (a missing widget).
 */
final class SoulWarsLobbyReader
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");

	private SoulWarsLobbyReader()
	{
	}

	static final class Result
	{
		final String playersWaiting; // null = unknown, e.g. "5/20" or "28"
		final String nextGame;       // null = unknown, e.g. "3 minutes", "30 seconds", "-"

		Result(String playersWaiting, String nextGame)
		{
			this.playersWaiting = playersWaiting;
			this.nextGame = nextGame;
		}
	}

	static Result read(List<String> orderedTexts)
	{
		String players = null;
		String nextGame = null;
		for (int i = 0; i + 1 < orderedTexts.size(); i++)
		{
			final String label = clean(orderedTexts.get(i)).toLowerCase();
			final String value = clean(orderedTexts.get(i + 1));
			if (isLabel(value))
			{
				continue;
			}
			if (players == null && label.contains("players waiting"))
			{
				players = value.isEmpty() ? null : value;
			}
			else if (nextGame == null && label.contains("next game"))
			{
				nextGame = value.isEmpty() ? null : value;
			}
		}
		return new Result(players, nextGame);
	}

	private static boolean isLabel(String value)
	{
		final String low = value.toLowerCase();
		return low.contains("players waiting") || low.contains("next game");
	}

	private static String clean(String s)
	{
		return s == null ? "" : TAG.matcher(s).replaceAll("").trim();
	}
}
