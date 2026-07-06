package com.anonyser.followplus;

import java.util.EnumSet;
import java.util.Set;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DailyXpTrackerTest
{
	private static final Set<Skill> TRACKED = EnumSet.of(Skill.ATTACK, Skill.STRENGTH, Skill.PRAYER);
	private static final long DAY1 = 20_000L * 86_400_000L;
	private static final long DAY2 = 20_001L * 86_400_000L;

	@Test
	public void firstReadingIsBaselineOnly()
	{
		final DailyXpTracker t = new DailyXpTracker(TRACKED);
		t.onXp(Skill.ATTACK, 5000, true, DAY1);
		assertEquals(0, t.usedToday(DAY1));
	}

	@Test
	public void countsGainsInTrackedSkills()
	{
		final DailyXpTracker t = new DailyXpTracker(TRACKED);
		t.onXp(Skill.ATTACK, 5000, true, DAY1);
		t.onXp(Skill.ATTACK, 5030, true, DAY1);
		t.onXp(Skill.STRENGTH, 100, true, DAY1);
		t.onXp(Skill.STRENGTH, 160, true, DAY1);
		assertEquals(90, t.usedToday(DAY1));
	}

	@Test
	public void onlyCountsWhenFlagged()
	{
		final DailyXpTracker t = new DailyXpTracker(TRACKED);
		t.onXp(Skill.ATTACK, 5000, false, DAY1); // baseline
		t.onXp(Skill.ATTACK, 5100, false, DAY1); // combat: baseline moves, not counted
		assertEquals(0, t.usedToday(DAY1));
		t.onXp(Skill.ATTACK, 5150, true, DAY1); // purchase from 5100 -> +50
		assertEquals(50, t.usedToday(DAY1));
	}

	@Test
	public void ignoresUntrackedSkills()
	{
		final DailyXpTracker t = new DailyXpTracker(TRACKED);
		t.onXp(Skill.WOODCUTTING, 100, true, DAY1);
		t.onXp(Skill.WOODCUTTING, 999, true, DAY1);
		assertEquals(0, t.usedToday(DAY1));
	}

	@Test
	public void rollsOverAtDayBoundary()
	{
		final DailyXpTracker t = new DailyXpTracker(TRACKED);
		t.onXp(Skill.ATTACK, 5000, true, DAY1);
		t.onXp(Skill.ATTACK, 5100, true, DAY1);
		assertEquals(100, t.usedToday(DAY1));
		assertEquals(0, t.usedToday(DAY2));
		t.onXp(Skill.ATTACK, 5100, true, DAY2);
		t.onXp(Skill.ATTACK, 5150, true, DAY2);
		assertEquals(50, t.usedToday(DAY2));
	}

	@Test
	public void seedRestoresTotal()
	{
		final DailyXpTracker t = new DailyXpTracker(TRACKED);
		t.seed(20_000L, 12345);
		assertEquals(12345, t.usedToday(DAY1));
		assertEquals(20_000L, t.dayKey());
		// a purchase adds onto the restored total
		t.onXp(Skill.ATTACK, 1000, false, DAY1);
		t.onXp(Skill.ATTACK, 1050, true, DAY1);
		assertEquals(12395, t.usedToday(DAY1));
	}
}
