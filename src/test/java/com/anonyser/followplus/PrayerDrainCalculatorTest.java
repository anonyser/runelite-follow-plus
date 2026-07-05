package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PrayerDrainCalculatorTest
{
	private static final double EPS = 0.0001;

	@Test
	public void protectFromMeleeAtZeroBonus()
	{
		// wiki: Protect from Melee (drain effect 12) at +0 bonus costs 1 point per 3 seconds
		assertEquals(3.0, PrayerDrainCalculator.secondsRemaining(1, 12, 0), EPS);
		assertEquals(129.0, PrayerDrainCalculator.secondsRemaining(43, 12, 0), EPS);
	}

	@Test
	public void thirtyPrayerBonusDoublesDuration()
	{
		// wiki: every +30 prayer bonus makes prayers last twice as long as at +0
		final double base = PrayerDrainCalculator.secondsRemaining(50, 24, 0);
		assertEquals(base * 2, PrayerDrainCalculator.secondsRemaining(50, 24, 30), EPS);
	}

	@Test
	public void stackedPrayersSumTheirDrainEffects()
	{
		// Piety (24) + Protect from Melee (12) = 36; at +0 bonus that is exactly 1 point/second
		assertEquals(43.0, PrayerDrainCalculator.secondsRemaining(43, 36, 0), EPS);
	}

	@Test
	public void positiveBonusSlowsDrain()
	{
		// drain resistance 2*15+60 = 90 -> 4.5s per point on Protect from Melee
		assertEquals(90.0, PrayerDrainCalculator.secondsRemaining(20, 12, 15), EPS);
	}

	@Test
	public void noActivePrayersMeansNoDrain()
	{
		assertEquals(-1, PrayerDrainCalculator.secondsRemaining(50, 0, 15), EPS);
	}

	@Test
	public void zeroPointsMeansZeroSeconds()
	{
		assertEquals(0, PrayerDrainCalculator.secondsRemaining(0, 12, 15), EPS);
	}
}
