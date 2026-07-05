package com.anonyser.followplus;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class SoulWarsTimeParser
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern TIME = Pattern.compile("(?:(\\d{1,2}):)?(\\d{1,2}):(\\d{2})");

	private SoulWarsTimeParser()
	{
	}

	/**
	 * Extracts an h:mm:ss or mm:ss time from widget text (color tags and labels are fine,
	 * e.g. "&lt;col=ffffff&gt;Time Remaining: 5:03&lt;/col&gt;").
	 *
	 * @return the time in seconds, or -1 when the text contains no valid time
	 */
	static int parseTimeSeconds(String text)
	{
		if (text == null || text.isEmpty())
		{
			return -1;
		}
		final Matcher m = TIME.matcher(TAG.matcher(text).replaceAll(" "));
		if (!m.find())
		{
			return -1;
		}
		final int hours = m.group(1) != null ? Integer.parseInt(m.group(1)) : 0;
		final int minutes = Integer.parseInt(m.group(2));
		final int seconds = Integer.parseInt(m.group(3));
		if (seconds > 59 || (m.group(1) != null && minutes > 59))
		{
			return -1;
		}
		return hours * 3600 + minutes * 60 + seconds;
	}
}
