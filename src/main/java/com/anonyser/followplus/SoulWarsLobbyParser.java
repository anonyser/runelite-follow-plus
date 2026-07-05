package com.anonyser.followplus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SoulWarsLobbyParser
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern TIME = Pattern.compile("(?:\\d{1,2}:)?\\d{1,2}:\\d{2}");
	private static final Pattern NUMBER = Pattern.compile("(\\d{1,5})");

	private SoulWarsLobbyParser()
	{
	}

	/**
	 * Pulls a waiting-player count out of lobby text. Only texts that talk about players or
	 * waiting are considered, and times are stripped first so "next game 1:30" can't be read
	 * as a count.
	 *
	 * @return the count, or -1 when the text has none
	 */
	static int parsePlayerCount(String text)
	{
		if (text == null || text.isEmpty())
		{
			return -1;
		}
		final String clean = TAG.matcher(text).replaceAll(" ");
		final String lower = clean.toLowerCase();
		if (!lower.contains("player") && !lower.contains("waiting"))
		{
			return -1;
		}
		final Matcher m = NUMBER.matcher(TIME.matcher(clean).replaceAll(" "));
		return m.find() ? Integer.parseInt(m.group(1)) : -1;
	}
}
