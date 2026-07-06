package com.anonyser.followplus;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import net.runelite.api.Skill;

/**
 * Best-effort tally of experience gained in the Zeal-spendable skills, against the daily
 * 1,000,000 XP cap. It sums XP gains (from stat changes) in the tracked skills and rolls over at
 * the UTC day boundary. It can't tell Zeal-bought XP apart from XP earned any other way, so for
 * anyone who also trains those skills normally it over-counts — hence "best effort" and it's off
 * by default. Pure logic, unit-tested; the plugin feeds it stat changes.
 */
class DailyXpTracker
{
	private static final long MS_PER_DAY = 86_400_000L;

	private final Set<Skill> tracked;
	private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
	private long dayKey = Long.MIN_VALUE;
	private int usedToday;

	DailyXpTracker(Set<Skill> tracked)
	{
		this.tracked = tracked;
	}

	/**
	 * Feed a skill's new total experience. The baseline is always updated so combat XP doesn't leak
	 * into a later purchase's delta, but the gain is only added to today's total when {@code count}
	 * is true (i.e. it came from spending Zeal at the reward shop). The first reading per skill only
	 * sets a baseline.
	 */
	void onXp(Skill skill, int totalXp, boolean count, long nowMs)
	{
		rollDay(nowMs);
		if (!tracked.contains(skill))
		{
			return;
		}
		final Integer last = lastXp.put(skill, totalXp);
		if (count && last != null && totalXp > last)
		{
			usedToday += totalXp - last;
		}
	}

	int usedToday(long nowMs)
	{
		rollDay(nowMs);
		return usedToday;
	}

	/** Restore a persisted total for a given day (rolls to 0 automatically if the day has passed). */
	void seed(long dayKey, int used)
	{
		this.dayKey = dayKey;
		this.usedToday = Math.max(0, used);
	}

	long dayKey()
	{
		return dayKey;
	}

	int used()
	{
		return usedToday;
	}

	private void rollDay(long nowMs)
	{
		final long key = nowMs / MS_PER_DAY;
		if (key != dayKey)
		{
			dayKey = key;
			usedToday = 0;
		}
	}
}
