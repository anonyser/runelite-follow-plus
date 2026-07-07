package com.anonyser.followplus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class SessionResultTrackerTest
{
	@Test
	public void decideReadsKillsRelativeToYourTeam()
	{
		// Blue has more avatar kills: a win for blue, a loss for red.
		assertEquals(SessionResultTracker.Outcome.WIN,
			SessionResultTracker.decide(5, 3, SoulWarsTeam.BLUE));
		assertEquals(SessionResultTracker.Outcome.LOSS,
			SessionResultTracker.decide(5, 3, SoulWarsTeam.RED));
		// Red has more: mirror image.
		assertEquals(SessionResultTracker.Outcome.LOSS,
			SessionResultTracker.decide(2, 4, SoulWarsTeam.BLUE));
		assertEquals(SessionResultTracker.Outcome.WIN,
			SessionResultTracker.decide(2, 4, SoulWarsTeam.RED));
	}

	@Test
	public void equalKillsIsADraw()
	{
		assertEquals(SessionResultTracker.Outcome.DRAW,
			SessionResultTracker.decide(3, 3, SoulWarsTeam.BLUE));
		assertEquals(SessionResultTracker.Outcome.DRAW,
			SessionResultTracker.decide(0, 0, SoulWarsTeam.RED));
	}

	@Test
	public void decideReturnsNullWhenItCannotDecide()
	{
		assertNull(SessionResultTracker.decide(-1, 3, SoulWarsTeam.BLUE));
		assertNull(SessionResultTracker.decide(3, -1, SoulWarsTeam.RED));
		assertNull(SessionResultTracker.decide(5, 3, SoulWarsTeam.NONE));
	}

	@Test
	public void recordTalliesPerTeamAndOutcome()
	{
		final SessionResultTracker t = new SessionResultTracker();
		t.record(SoulWarsTeam.RED, SessionResultTracker.Outcome.WIN);
		t.record(SoulWarsTeam.RED, SessionResultTracker.Outcome.WIN);
		t.record(SoulWarsTeam.RED, SessionResultTracker.Outcome.LOSS);
		t.record(SoulWarsTeam.BLUE, SessionResultTracker.Outcome.WIN);
		t.record(SoulWarsTeam.BLUE, SessionResultTracker.Outcome.DRAW);

		assertEquals(2, t.redWins());
		assertEquals(1, t.redLosses());
		assertEquals(1, t.blueWins());
		assertEquals(0, t.blueLosses());

		// Aggregate W/L ignores draws; the drawn game shows in neither column.
		assertEquals(3, t.wins());
		assertEquals(1, t.losses());
		assertEquals(1, t.draws());
	}

	@Test
	public void ignoresUnknownTeamAndNullOutcome()
	{
		final SessionResultTracker t = new SessionResultTracker();
		t.record(SoulWarsTeam.NONE, SessionResultTracker.Outcome.WIN);
		t.record(SoulWarsTeam.BLUE, null);
		assertEquals(0, t.wins());
		assertEquals(0, t.losses());
		assertEquals(0, t.draws());
	}

	@Test
	public void resetClearsEverything()
	{
		final SessionResultTracker t = new SessionResultTracker();
		t.record(SoulWarsTeam.RED, SessionResultTracker.Outcome.WIN);
		t.record(SoulWarsTeam.BLUE, SessionResultTracker.Outcome.LOSS);
		t.reset();
		assertEquals(0, t.wins());
		assertEquals(0, t.losses());
		assertEquals(0, t.draws());
		assertEquals(0, t.redWins());
		assertEquals(0, t.blueLosses());
	}
}
