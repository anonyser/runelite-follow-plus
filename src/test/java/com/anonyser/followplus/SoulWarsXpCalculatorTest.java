package com.anonyser.followplus;

import net.runelite.api.Experience;
import net.runelite.api.Skill;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SoulWarsXpCalculatorTest
{
	@Test
	public void multipliersMatchWiki()
	{
		assertEquals(30, SoulWarsXpCalculator.multiplier(Skill.ATTACK));
		assertEquals(30, SoulWarsXpCalculator.multiplier(Skill.STRENGTH));
		assertEquals(30, SoulWarsXpCalculator.multiplier(Skill.DEFENCE));
		assertEquals(30, SoulWarsXpCalculator.multiplier(Skill.HITPOINTS));
		assertEquals(27, SoulWarsXpCalculator.multiplier(Skill.MAGIC));
		assertEquals(27, SoulWarsXpCalculator.multiplier(Skill.RANGED));
		assertEquals(14, SoulWarsXpCalculator.multiplier(Skill.PRAYER));
	}

	@Test
	public void nonZealSkillsAreNotTrainable()
	{
		assertFalse(SoulWarsXpCalculator.isTrainable(Skill.WOODCUTTING));
		assertFalse(SoulWarsXpCalculator.isTrainable(Skill.SLAYER));
		assertEquals(0, SoulWarsXpCalculator.multiplier(Skill.MINING));
		assertTrue(SoulWarsXpCalculator.isTrainable(Skill.RANGED));
	}

	@Test
	public void xpPerTokenMatchesWikiWorkedExamples()
	{
		// floor(level^2 / 600) * N
		assertEquals(30, SoulWarsXpCalculator.xpPerToken(Skill.ATTACK, 30));
		assertEquals(480, SoulWarsXpCalculator.xpPerToken(Skill.ATTACK, 99));
		assertEquals(432, SoulWarsXpCalculator.xpPerToken(Skill.RANGED, 99));
		assertEquals(224, SoulWarsXpCalculator.xpPerToken(Skill.PRAYER, 99));
		// band step: floor(35^2/600) = 2
		assertEquals(60, SoulWarsXpCalculator.xpPerToken(Skill.STRENGTH, 35));
	}

	@Test
	public void tokensToLevelSumsOneLevel()
	{
		// Level 30 -> 31 for Attack: (14833 - 13363) xp at 30 xp/token = 49 tokens.
		final long tokens = SoulWarsXpCalculator.tokensToLevel(
			Skill.ATTACK, Experience.getXpForLevel(30), 31);
		assertEquals(49, tokens);
	}

	@Test
	public void tokensToLevelZeroWhenAlreadyThere()
	{
		assertEquals(0, SoulWarsXpCalculator.tokensToLevel(
			Skill.MAGIC, Experience.getXpForLevel(80), 80));
		assertEquals(0, SoulWarsXpCalculator.tokensToLevel(
			Skill.MAGIC, Experience.getXpForLevel(80), 70));
	}

	@Test
	public void tokensToLevelIsNegativeForUntrainableSkill()
	{
		assertEquals(-1, SoulWarsXpCalculator.tokensToLevel(
			Skill.FISHING, Experience.getXpForLevel(40), 50));
	}

	@Test
	public void belowThirtyCountsFromThirty()
	{
		// Starting under 30 gives the same answer as starting at exactly 30 (can't buy below 30).
		final long fromLow = SoulWarsXpCalculator.tokensToLevel(Skill.DEFENCE, 0, 31);
		final long fromThirty = SoulWarsXpCalculator.tokensToLevel(
			Skill.DEFENCE, Experience.getXpForLevel(30), 31);
		assertEquals(fromThirty, fromLow);
	}

	@Test
	public void tokensToXpMatchesTokensToLevelAtLevelBoundaries()
	{
		final int xp30 = Experience.getXpForLevel(30);
		assertEquals(
			SoulWarsXpCalculator.tokensToLevel(Skill.ATTACK, xp30, 31),
			SoulWarsXpCalculator.tokensToXp(Skill.ATTACK, xp30, Experience.getXpForLevel(31)));
	}

	@Test
	public void tokensToXpZeroAndNegativeCases()
	{
		assertEquals(0, SoulWarsXpCalculator.tokensToXp(
			Skill.MAGIC, Experience.getXpForLevel(80), Experience.getXpForLevel(70)));
		assertEquals(-1, SoulWarsXpCalculator.tokensToXp(
			Skill.FISHING, 0, 1_000_000));
	}

	@Test
	public void tokensToXpHandlesBigTargetsAbove99()
	{
		// 200M strength from level 92 should be a large but finite token count
		final long tokens = SoulWarsXpCalculator.tokensToXp(
			Skill.STRENGTH, Experience.getXpForLevel(92), 200_000_000L);
		assertTrue(tokens > 0);
	}

	@Test
	public void parseXpFormats()
	{
		assertEquals(200_000_000L, SoulWarsXpCalculator.parseXp("200M"));
		assertEquals(200_000_000L, SoulWarsXpCalculator.parseXp("200000000"));
		assertEquals(200_000_000L, SoulWarsXpCalculator.parseXp("200,000,000"));
		assertEquals(1_000_000L, SoulWarsXpCalculator.parseXp("1000K"));
		assertEquals(1_120_000L, SoulWarsXpCalculator.parseXp("1.12M"));
		assertEquals(20_000_000L, SoulWarsXpCalculator.parseXp("20m"));
	}

	@Test
	public void parseXpRejectsJunk()
	{
		assertEquals(-1, SoulWarsXpCalculator.parseXp(null));
		assertEquals(-1, SoulWarsXpCalculator.parseXp(""));
		assertEquals(-1, SoulWarsXpCalculator.parseXp("abc"));
	}

	@Test
	public void cratesAffordable()
	{
		assertEquals(0, SoulWarsXpCalculator.cratesAffordable(29));
		assertEquals(1, SoulWarsXpCalculator.cratesAffordable(30));
		assertEquals(3, SoulWarsXpCalculator.cratesAffordable(95));
		assertEquals(0, SoulWarsXpCalculator.cratesAffordable(-5));
	}
}
