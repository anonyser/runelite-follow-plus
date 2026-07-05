package com.anonyser.followplus;

import java.util.HashMap;
import java.util.Map;

/**
 * Locks onto the one Soul Wars widget whose mm:ss text actually counts down like a clock.
 * The exact child holding the match timer isn't verified anywhere, so every time-looking text
 * in the game interface is fed in as a candidate and nothing is displayed until one of them
 * has behaved like a countdown for a few seconds - a wrong timer is worse than "Unknown".
 */
class GameTimerTracker
{
	/** A candidate must tick down consistently for this long before it is trusted. */
	private static final long LOCK_AFTER_MS = 3000;
	/** Allowed disagreement between elapsed wall time and the displayed drop, in seconds. */
	private static final int TOLERANCE_SECONDS = 2;
	/** Forget observations this old (widget vanished, match ended). */
	private static final long STALE_MS = 3000;

	private static final class Candidate
	{
		int anchorSeconds;
		long anchorMs;
		int lastSeconds;
		long lastMs;
	}

	private final Map<Integer, Candidate> candidates = new HashMap<>();
	private int lockedKey = -1;

	void reset()
	{
		candidates.clear();
		lockedKey = -1;
	}

	/** Feed every parsed time text: a stable key for the widget, its value, and the wall clock. */
	void observe(int key, int seconds, long nowMs)
	{
		Candidate c = candidates.get(key);
		if (c == null)
		{
			c = new Candidate();
			c.anchorSeconds = seconds;
			c.anchorMs = nowMs;
			candidates.put(key, c);
		}
		else if (seconds > c.lastSeconds || nowMs - c.lastMs > STALE_MS)
		{
			// went up or vanished for a while - restart the countdown check from here
			c.anchorSeconds = seconds;
			c.anchorMs = nowMs;
			if (lockedKey == key)
			{
				lockedKey = -1;
			}
		}
		else
		{
			final long elapsed = nowMs - c.anchorMs;
			final long drop = c.anchorSeconds - seconds;
			if (Math.abs(drop - elapsed / 1000.0) > TOLERANCE_SECONDS)
			{
				// not moving like a clock (a static score, a kill count, ...)
				c.anchorSeconds = seconds;
				c.anchorMs = nowMs;
				if (lockedKey == key)
				{
					lockedKey = -1;
				}
			}
			else if (elapsed >= LOCK_AFTER_MS && lockedKey == -1)
			{
				lockedKey = key;
			}
		}
		c.lastSeconds = seconds;
		c.lastMs = nowMs;
	}

	/** Seconds left on the locked countdown, or -1 while no widget has proven itself. */
	int getSeconds(long nowMs)
	{
		final Candidate c = lockedKey >= 0 ? candidates.get(lockedKey) : null;
		if (c == null || nowMs - c.lastMs > STALE_MS)
		{
			return -1;
		}
		return c.lastSeconds;
	}

	boolean isLocked()
	{
		return lockedKey >= 0;
	}

	int lockedKey()
	{
		return lockedKey;
	}
}
