package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GameTimerTrackerTest
{
	@Test
	public void unknownUntilACandidateProvesItself()
	{
		final GameTimerTracker t = new GameTimerTracker();
		assertEquals(-1, t.getSeconds(0));
		t.observe(1, 300, 0);
		assertEquals(-1, t.getSeconds(0));
	}

	@Test
	public void locksOntoACountdown()
	{
		final GameTimerTracker t = new GameTimerTracker();
		long ms = 0;
		int seconds = 300;
		for (int i = 0; i < 8; i++) // ~4.2s of consistent countdown at tick cadence
		{
			t.observe(7, seconds, ms);
			ms += 600;
			if (ms % 1000 < 600)
			{
				seconds--;
			}
		}
		assertTrue(t.isLocked());
		assertTrue(t.getSeconds(ms) >= 0);
	}

	@Test
	public void neverLocksOntoStaticText()
	{
		final GameTimerTracker t = new GameTimerTracker();
		for (long ms = 0; ms < 20000; ms += 600)
		{
			t.observe(3, 120, ms); // a "2:00" that never moves (score, requirement, ...)
		}
		assertFalse(t.isLocked());
		assertEquals(-1, t.getSeconds(20000));
	}

	@Test
	public void prefersTheRealClockOverStaticText()
	{
		final GameTimerTracker t = new GameTimerTracker();
		int seconds = 900;
		for (long ms = 0; ms <= 6000; ms += 1000)
		{
			t.observe(3, 120, ms);          // static
			t.observe(9, seconds--, ms);    // counts down 1/s
		}
		assertTrue(t.isLocked());
		assertEquals(9, t.lockedKey());
		assertEquals(seconds + 1, t.getSeconds(6000));
	}

	@Test
	public void goesUnknownWhenTheWidgetDisappears()
	{
		final GameTimerTracker t = new GameTimerTracker();
		int seconds = 900;
		long ms = 0;
		for (; ms <= 6000; ms += 1000)
		{
			t.observe(9, seconds--, ms);
		}
		assertTrue(t.isLocked());
		// no observations for a while (match over, interface closed)
		assertEquals(-1, t.getSeconds(ms + 10000));
	}

	@Test
	public void resetClearsTheLock()
	{
		final GameTimerTracker t = new GameTimerTracker();
		int seconds = 900;
		for (long ms = 0; ms <= 6000; ms += 1000)
		{
			t.observe(9, seconds--, ms);
		}
		assertTrue(t.isLocked());
		t.reset();
		assertFalse(t.isLocked());
		assertEquals(-1, t.getSeconds(6000));
	}
}
