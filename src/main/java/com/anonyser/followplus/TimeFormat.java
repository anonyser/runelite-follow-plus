package com.anonyser.followplus;

final class TimeFormat
{
	private TimeFormat()
	{
	}

	/** Formats seconds as m:ss, or h:mm:ss above an hour. Negative values clamp to 0:00. */
	static String mmss(double seconds)
	{
		final long total = (long) Math.max(0, seconds);
		final long h = total / 3600;
		final long m = (total % 3600) / 60;
		final long s = total % 60;
		return h > 0
			? String.format("%d:%02d:%02d", h, m, s)
			: String.format("%d:%02d", m, s);
	}
}
