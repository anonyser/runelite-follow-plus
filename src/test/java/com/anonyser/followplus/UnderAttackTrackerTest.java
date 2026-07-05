package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UnderAttackTrackerTest
{
	@Test
	public void recentHitMeansUnderAttack()
	{
		final UnderAttackTracker t = new UnderAttackTracker(17);
		t.recordIncomingHit(100);
		assertEquals(UnderAttackTracker.Status.UNDER_ATTACK, t.status(100, 0));
		assertEquals(UnderAttackTracker.Status.UNDER_ATTACK, t.status(117, 0));
	}

	@Test
	public void hitExpiresAfterTheWindow()
	{
		final UnderAttackTracker t = new UnderAttackTracker(17);
		t.recordIncomingHit(100);
		assertEquals(UnderAttackTracker.Status.NONE, t.status(118, 0));
	}

	@Test
	public void targetingOnlyMeansPossibly()
	{
		final UnderAttackTracker t = new UnderAttackTracker(17);
		assertEquals(UnderAttackTracker.Status.POSSIBLE, t.status(50, 1));
	}

	@Test
	public void recentHitOutranksTargeting()
	{
		final UnderAttackTracker t = new UnderAttackTracker(17);
		t.recordIncomingHit(100);
		assertEquals(UnderAttackTracker.Status.UNDER_ATTACK, t.status(110, 2));
	}

	@Test
	public void cleanStateIsNone()
	{
		final UnderAttackTracker t = new UnderAttackTracker(17);
		assertEquals(UnderAttackTracker.Status.NONE, t.status(0, 0));
		t.recordIncomingHit(100);
		t.reset();
		assertEquals(UnderAttackTracker.Status.NONE, t.status(101, 0));
	}
}
