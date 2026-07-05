package com.anonyser.followplus;

final class PrayerDrainCalculator
{
	private PrayerDrainCalculator()
	{
	}

	/**
	 * Seconds until prayer points reach zero, using the exact math of RuneLite's core prayer
	 * plugin (PrayerPlugin#getEstimatedTimeRemaining), which follows
	 * https://oldschool.runescape.wiki/w/Prayer#Prayer_drain_mechanics:
	 * drain resistance = 2 * prayer bonus + 60; each point lasts
	 * 0.6s * drainResistance / drainEffect.
	 *
	 * @param prayerPoints current prayer points (boosted level)
	 * @param drainEffect  sum of the drain effects of all active prayers
	 * @param prayerBonus  total prayer bonus from worn equipment
	 * @return seconds remaining, or -1 when no prayer is draining
	 */
	static double secondsRemaining(int prayerPoints, int drainEffect, int prayerBonus)
	{
		if (drainEffect <= 0)
		{
			return -1;
		}
		if (prayerPoints <= 0)
		{
			return 0;
		}
		final int drainResistance = 2 * prayerBonus + 60;
		final double secondsPerPoint = 0.6 * ((double) drainResistance / drainEffect);
		return prayerPoints * secondsPerPoint;
	}
}
