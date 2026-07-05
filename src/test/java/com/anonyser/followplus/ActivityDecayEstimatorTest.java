package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ActivityDecayEstimatorTest
{
	private static final double EPS = 0.0001;

	@Test
	public void unknownUntilADecayIsObserved()
	{
		final ActivityDecayEstimator e = new ActivityDecayEstimator();
		assertEquals(-1, e.ticksToZero(0), EPS);
		e.onValue(800, 0);
		assertEquals(-1, e.ticksToZero(5), EPS);
	}

	@Test
	public void estimatesFromObservedDecay()
	{
		final ActivityDecayEstimator e = new ActivityDecayEstimator();
		e.onValue(800, 0);
		e.onValue(760, 5); // -40 over 5 ticks = 8/tick
		assertEquals(95.0, e.ticksToZero(5), EPS); // 760 / 8
		// between changes the estimate keeps counting down
		assertEquals(90.0, e.ticksToZero(10), EPS);
	}

	@Test
	public void activityGainJustMovesTheBaseline()
	{
		final ActivityDecayEstimator e = new ActivityDecayEstimator();
		e.onValue(400, 0);
		e.onValue(360, 5); // 8/tick
		e.onValue(800, 6); // player did something
		assertEquals(100.0, e.ticksToZero(6), EPS); // 800 / 8
	}

	@Test
	public void estimateNeverGoesNegative()
	{
		final ActivityDecayEstimator e = new ActivityDecayEstimator();
		e.onValue(80, 0);
		e.onValue(40, 5);
		assertEquals(0.0, e.ticksToZero(1000), EPS);
	}

	@Test
	public void resetForgetsEverything()
	{
		final ActivityDecayEstimator e = new ActivityDecayEstimator();
		e.onValue(800, 0);
		e.onValue(760, 5);
		e.reset();
		assertEquals(-1, e.ticksToZero(10), EPS);
	}

	@Test
	public void smoothsAcrossSamples()
	{
		final ActivityDecayEstimator e = new ActivityDecayEstimator();
		e.onValue(800, 0);
		e.onValue(760, 5);  // 8/tick
		e.onValue(700, 10); // 12/tick -> smoothed 10/tick
		final double ticks = e.ticksToZero(10);
		assertTrue("expected 700/10 = 70, got " + ticks, Math.abs(ticks - 70.0) < EPS);
	}
}
