package com.anonyser.followplus;

/**
 * Tracks Soul Wars Zeal from the two account varps (verified live):
 * <ul>
 *   <li>lifetime Zeal — the cumulative score, varp 2871 (only ever rises),</li>
 *   <li>Zeal Token balance — the spendable currency, varp 2876.</li>
 * </ul>
 *
 * <p>Both are handed in on each update; everything else is derived from the lifetime value's
 * movement, so this class holds no game references and is fully unit-tested. "Zeal earned" for a
 * game / session / since install is just the rise in lifetime Zeal between two anchors. The
 * install anchor is persisted by the plugin (in config); the session and last-game anchors live
 * for the session only.
 *
 * <p>A varp reading of -1 means "not known yet" (e.g. before the game has synced it) and is
 * ignored, so a missing value never corrupts an anchor.
 */
class ZealTracker
{
	private int lifetime = -1;
	private int tokens = -1;

	private long sessionStartMs;
	private int sessionStartLifetime = -1;

	private boolean inGamePrev;
	private int gameStartLifetime = -1;
	private int lastGameGained = -1;

	void startSession(long nowMs)
	{
		sessionStartMs = nowMs;
		sessionStartLifetime = -1;
		inGamePrev = false;
		gameStartLifetime = -1;
		lastGameGained = -1;
	}

	/**
	 * Feed the latest readings. {@code lifetimeZeal} / {@code tokenBalance} are the raw varp values
	 * (-1 if not yet known); {@code inGame} is true while in a Soul Wars game.
	 */
	void update(int lifetimeZeal, int tokenBalance, boolean inGame, long nowMs)
	{
		if (lifetimeZeal >= 0)
		{
			lifetime = lifetimeZeal;
			if (sessionStartLifetime < 0)
			{
				sessionStartLifetime = lifetimeZeal;
			}
		}
		if (tokenBalance >= 0)
		{
			tokens = tokenBalance;
		}

		// Game boundaries drive "last game" zeal: anchor lifetime on entry, diff it on exit.
		if (inGame && !inGamePrev && lifetime >= 0)
		{
			gameStartLifetime = lifetime;
		}
		else if (!inGame && inGamePrev && gameStartLifetime >= 0 && lifetime >= gameStartLifetime)
		{
			lastGameGained = lifetime - gameStartLifetime;
		}
		inGamePrev = inGame;
	}

	boolean lifetimeKnown()
	{
		return lifetime >= 0;
	}

	int lifetime()
	{
		return lifetime;
	}

	int tokens()
	{
		return tokens;
	}

	/** Zeal gained since the plugin started this session, or -1 until lifetime is known. */
	int sessionGained()
	{
		return lifetime >= 0 && sessionStartLifetime >= 0 ? lifetime - sessionStartLifetime : -1;
	}

	/** Zeal earned in the most recent completed game this session, or -1 if none yet. */
	int lastGameGained()
	{
		return lastGameGained;
	}

	long sessionMillis(long nowMs)
	{
		return Math.max(0, nowMs - sessionStartMs);
	}

	/** Estimated Zeal per hour this session (session gain over session time), or -1 if not ready. */
	int zealPerHour(long nowMs)
	{
		final long ms = sessionMillis(nowMs);
		final int gained = sessionGained();
		if (gained <= 0 || ms < 1000)
		{
			return -1;
		}
		return (int) (gained * 3_600_000L / ms);
	}
}
