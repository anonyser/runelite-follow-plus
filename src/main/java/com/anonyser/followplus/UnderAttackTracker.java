package com.anonyser.followplus;

/**
 * Conservative "am I being attacked" state: a recent hitsplat on us means yes; someone merely
 * targeting us (which could also be a follower or a trade attempt) only means possibly.
 */
class UnderAttackTracker
{
	enum Status
	{
		NONE,
		POSSIBLE,
		UNDER_ATTACK
	}

	private final int hitWindowTicks;
	private int lastHitTick = Integer.MIN_VALUE / 2;

	UnderAttackTracker(int hitWindowTicks)
	{
		this.hitWindowTicks = hitWindowTicks;
	}

	void recordIncomingHit(int tick)
	{
		lastHitTick = tick;
	}

	void reset()
	{
		lastHitTick = Integer.MIN_VALUE / 2;
	}

	Status status(int tick, int actorsTargetingUs)
	{
		if (tick - lastHitTick <= hitWindowTicks)
		{
			return Status.UNDER_ATTACK;
		}
		return actorsTargetingUs > 0 ? Status.POSSIBLE : Status.NONE;
	}
}
