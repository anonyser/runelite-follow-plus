package com.anonyser.followplus;

/**
 * Estimates how long until the Soul Wars activity bar reaches zero, from observed decay only.
 * The decay cadence isn't documented anywhere trustworthy, so instead of hardcoding a guess we
 * measure it: every time the activity varbit drops we update a smoothed per-tick rate, and the
 * estimate stays "unknown" (-1) until at least one real drop has been seen.
 */
class ActivityDecayEstimator
{
	private static final double SMOOTHING = 0.5;

	private double ratePerTick;
	private boolean hasRate;
	private int lastValue = -1;
	private int lastValueTick;

	void reset()
	{
		ratePerTick = 0;
		hasRate = false;
		lastValue = -1;
		lastValueTick = 0;
	}

	/** Feed every activity varbit change, with the game tick it happened on. */
	void onValue(int value, int tick)
	{
		if (lastValue >= 0 && value < lastValue && tick > lastValueTick)
		{
			final double observed = (double) (lastValue - value) / (tick - lastValueTick);
			ratePerTick = hasRate ? SMOOTHING * ratePerTick + (1 - SMOOTHING) * observed : observed;
			hasRate = true;
		}
		lastValue = value;
		lastValueTick = tick;
	}

	/** Estimated ticks until the bar reaches zero if nothing changes, or -1 when unknown. */
	double ticksToZero(int currentTick)
	{
		if (!hasRate || lastValue < 0)
		{
			return -1;
		}
		final double predicted = lastValue - ratePerTick * (currentTick - lastValueTick);
		return Math.max(0, predicted / ratePerTick);
	}

	int lastValue()
	{
		return lastValue;
	}
}
