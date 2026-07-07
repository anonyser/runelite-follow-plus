package com.anonyser.followplus;

/**
 * Session tally of finished Soul Wars games, split by the team you played on. Each game is recorded
 * as a win, loss or draw for Blue or Red; the counts are per-session (cleared when the session
 * starts) and drive the "Session win/loss" and "Team composition" display lines. Holds no game
 * references so it is unit-tested in isolation: the plugin decides the outcome from the final
 * avatar-kill counts and hands it in via {@link #record}. Draws are kept so a drawn game is never
 * miscounted as a loss, but the win/loss line only shows wins and losses.
 */
class SessionResultTracker
{
	enum Outcome
	{
		WIN,
		LOSS,
		DRAW
	}

	private int blueWins;
	private int blueLosses;
	private int blueDraws;
	private int redWins;
	private int redLosses;
	private int redDraws;

	void reset()
	{
		blueWins = 0;
		blueLosses = 0;
		blueDraws = 0;
		redWins = 0;
		redLosses = 0;
		redDraws = 0;
	}

	/**
	 * Records one finished game for the given team. A null outcome or an unknown team
	 * ({@link SoulWarsTeam#NONE}) is ignored so a bad read never corrupts the tally.
	 */
	void record(SoulWarsTeam team, Outcome outcome)
	{
		if (outcome == null)
		{
			return;
		}
		if (team == SoulWarsTeam.BLUE)
		{
			if (outcome == Outcome.WIN)
			{
				blueWins++;
			}
			else if (outcome == Outcome.LOSS)
			{
				blueLosses++;
			}
			else
			{
				blueDraws++;
			}
		}
		else if (team == SoulWarsTeam.RED)
		{
			if (outcome == Outcome.WIN)
			{
				redWins++;
			}
			else if (outcome == Outcome.LOSS)
			{
				redLosses++;
			}
			else
			{
				redDraws++;
			}
		}
	}

	/**
	 * Decides the outcome for {@code team} from each team's final avatar-kill count (the HUD "x/5"
	 * values): more kills than the enemy is a win, fewer is a loss, equal is a draw. Returns null
	 * when the inputs cannot decide a result (kills not read yet, or unknown team).
	 */
	static Outcome decide(int blueKills, int redKills, SoulWarsTeam team)
	{
		if (blueKills < 0 || redKills < 0
			|| (team != SoulWarsTeam.BLUE && team != SoulWarsTeam.RED))
		{
			return null;
		}
		final int mine = team == SoulWarsTeam.BLUE ? blueKills : redKills;
		final int theirs = team == SoulWarsTeam.BLUE ? redKills : blueKills;
		if (mine > theirs)
		{
			return Outcome.WIN;
		}
		if (mine < theirs)
		{
			return Outcome.LOSS;
		}
		return Outcome.DRAW;
	}

	int wins()
	{
		return blueWins + redWins;
	}

	int losses()
	{
		return blueLosses + redLosses;
	}

	int draws()
	{
		return blueDraws + redDraws;
	}

	int blueWins()
	{
		return blueWins;
	}

	int blueLosses()
	{
		return blueLosses;
	}

	int redWins()
	{
		return redWins;
	}

	int redLosses()
	{
		return redLosses;
	}
}
