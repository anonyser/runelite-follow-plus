package com.anonyser.followplus;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ZealTrackerTest
{
	@Test
	public void tokensAndLifetimePassThrough()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(0);
		t.update(19456, 1452, false, 0);
		assertEquals(19456, t.lifetime());
		assertEquals(1452, t.tokens());
		assertTrue(t.lifetimeKnown());
	}

	@Test
	public void unknownReadingsAreIgnored()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(0);
		t.update(-1, -1, false, 0);
		assertFalse(t.lifetimeKnown());
		assertEquals(-1, t.tokens());
		// a later real reading still anchors the session correctly
		t.update(100, 50, false, 10);
		assertEquals(0, t.sessionGained());
	}

	@Test
	public void sessionGainedIsRiseSinceFirstReading()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(0);
		t.update(19401, 1397, false, 0);
		assertEquals(0, t.sessionGained());
		t.update(19456, 1452, false, 100);
		assertEquals(55, t.sessionGained());
	}

	@Test
	public void lastGameGainedIsDiffAcrossAGame()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(0);
		t.update(19401, 1397, false, 0); // in lobby
		assertEquals(-1, t.lastGameGained());
		t.update(19401, 1397, true, 10); // game starts, anchor 19401
		t.update(19401, 1397, true, 20); // mid game, no payout yet
		t.update(19456, 1452, false, 30); // game ends, payout applied
		assertEquals(55, t.lastGameGained());
	}

	@Test
	public void lastGameSurvivesUntilNextGameCompletes()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(0);
		t.update(100, 100, true, 0);
		t.update(140, 140, false, 10);
		assertEquals(40, t.lastGameGained());
		// still in lobby later - last game value stays
		t.update(140, 140, false, 20);
		assertEquals(40, t.lastGameGained());
	}

	@Test
	public void sessionMillisNeverNegative()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(1000);
		assertEquals(500, t.sessionMillis(1500));
		assertEquals(0, t.sessionMillis(900));
	}

	@Test
	public void zealPerHour()
	{
		final ZealTracker t = new ZealTracker();
		t.startSession(0);
		t.update(1000, 1000, false, 0);
		assertEquals(-1, t.zealPerHour(0)); // no gain yet
		t.update(1055, 1055, false, 3_600_000L); // +55 over one hour
		assertEquals(55, t.zealPerHour(3_600_000L));

		final ZealTracker half = new ZealTracker();
		half.startSession(0);
		half.update(0, 0, false, 0);
		half.update(30, 30, false, 1_800_000L); // 30 over half an hour
		assertEquals(60, half.zealPerHour(1_800_000L));
	}
}
