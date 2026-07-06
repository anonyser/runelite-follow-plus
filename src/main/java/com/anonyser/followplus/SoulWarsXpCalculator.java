package com.anonyser.followplus;

import net.runelite.api.Experience;
import net.runelite.api.Skill;

/**
 * Soul Wars reward-shop maths (no game state needed, so fully unit-tested).
 *
 * <p>Experience per Zeal Token scales with the skill's level in bands:
 * {@code xp = floor(level^2 / 600) * N}, where N is 30 (Attack/Strength/Defence/Hitpoints),
 * 27 (Magic/Ranged) or 14 (Prayer). You must be at least level 30 in a skill to spend Zeal on it,
 * and no more than 1,000,000 experience can be bought with Zeal per day. A Spoils of War crate
 * costs 30 Zeal Tokens. (All figures from the OSRS Wiki, verified against its worked examples:
 * level 99 melee = 480 xp/token, ranged = 432, prayer = 224.)
 */
final class SoulWarsXpCalculator
{
	static final int MIN_LEVEL = 30;
	static final int CRATE_COST = 30;
	static final int DAILY_XP_CAP = 1_000_000;
	static final long MAX_XP = 200_000_000L;

	private SoulWarsXpCalculator()
	{
	}

	/** The N factor for a skill, or 0 if Zeal cannot be spent on it. */
	static int multiplier(Skill skill)
	{
		switch (skill)
		{
			case PRAYER:
				return 14;
			case MAGIC:
			case RANGED:
				return 27;
			case ATTACK:
			case STRENGTH:
			case DEFENCE:
			case HITPOINTS:
				return 30;
			default:
				return 0;
		}
	}

	static boolean isTrainable(Skill skill)
	{
		return multiplier(skill) > 0;
	}

	/** Experience awarded per Zeal Token at the given level for the given skill. */
	static int xpPerToken(Skill skill, int level)
	{
		return (level * level / 600) * multiplier(skill);
	}

	/**
	 * Zeal Tokens needed to reach {@code targetLevel} from the current experience, summed level by
	 * level at each level's rate. Returns 0 if already at/above the target, and -1 if the skill
	 * cannot be trained with Zeal. Levels below {@link #MIN_LEVEL} can't be bought with Zeal, so
	 * the count starts from level 30.
	 */
	static long tokensToLevel(Skill skill, int currentXp, int targetLevel)
	{
		final int n = multiplier(skill);
		if (n == 0)
		{
			return -1;
		}
		targetLevel = Math.min(Math.max(targetLevel, 1), Experience.MAX_REAL_LEVEL);
		final int startLevel = Math.max(Experience.getLevelForXp(currentXp), MIN_LEVEL);
		if (targetLevel <= startLevel)
		{
			return 0;
		}
		long pos = Math.max(currentXp, Experience.getXpForLevel(MIN_LEVEL));
		long tokens = 0;
		for (int level = startLevel; level < targetLevel; level++)
		{
			final long boundary = Experience.getXpForLevel(level + 1);
			final long need = boundary - pos;
			if (need > 0)
			{
				final int rate = xpPerToken(skill, level); // level >= 30, so rate >= 30
				tokens += (need + rate - 1) / rate; // ceil
			}
			pos = boundary;
		}
		return tokens;
	}

	/**
	 * Zeal Tokens needed to reach a target total experience from the current experience, summed at
	 * each level's rate (levels past 99 keep the level-99 rate). Returns 0 if already there, -1 if
	 * the skill can't be trained with Zeal.
	 */
	static long tokensToXp(Skill skill, int currentXp, long targetXp)
	{
		final int n = multiplier(skill);
		if (n == 0)
		{
			return -1;
		}
		targetXp = Math.min(Math.max(targetXp, 0), MAX_XP);
		long pos = Math.max(currentXp, Experience.getXpForLevel(MIN_LEVEL));
		if (targetXp <= pos)
		{
			return 0;
		}
		long tokens = 0;
		int level = Math.max(Experience.getLevelForXp((int) pos), MIN_LEVEL);
		while (pos < targetXp)
		{
			final long boundary = level < Experience.MAX_REAL_LEVEL
				? Math.min(targetXp, Experience.getXpForLevel(level + 1)) : targetXp;
			final long need = boundary - pos;
			if (need > 0)
			{
				final int rate = xpPerToken(skill, Math.min(level, Experience.MAX_REAL_LEVEL));
				tokens += (need + rate - 1) / rate; // ceil
			}
			pos = boundary;
			level++;
		}
		return tokens;
	}

	/**
	 * Parses a player-friendly experience value: a plain number (commas/spaces optional) or a
	 * shorthand with a K/M/B suffix, e.g. "200000000", "200,000,000", "200M", "1.12M", "1000K".
	 * Returns -1 if it can't be parsed.
	 */
	static long parseXp(String text)
	{
		if (text == null)
		{
			return -1;
		}
		String s = text.trim().toUpperCase().replace(",", "").replace(" ", "");
		if (s.isEmpty())
		{
			return -1;
		}
		double mult = 1;
		final char last = s.charAt(s.length() - 1);
		if (last == 'K')
		{
			mult = 1_000;
		}
		else if (last == 'M')
		{
			mult = 1_000_000;
		}
		else if (last == 'B')
		{
			mult = 1_000_000_000;
		}
		if (mult != 1)
		{
			s = s.substring(0, s.length() - 1);
		}
		try
		{
			final double v = Double.parseDouble(s) * mult;
			return v < 0 ? -1 : (long) v;
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	/** How many Spoils of War crates the given Zeal Token balance can buy. */
	static int cratesAffordable(int zealTokens)
	{
		return Math.max(0, zealTokens) / CRATE_COST;
	}
}
