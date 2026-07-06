package com.anonyser.followplus;

/**
 * The Soul Wars in-game HUD (interface {@code SOUL_WARS_GAME} = 375). Its top-level children hold
 * the live match stats as plain text, verified live, in Blue-left / Red-right column order:
 *
 * <pre>
 *   child 11 = Blue avatar kills "x/5"     child 12 = Red avatar kills
 *   child 15 = Blue avatar health "100%"   child 16 = Red avatar health
 *   child 19 = Blue avatar strength "100%" child 20 = Red avatar strength
 *   child 23 = Time left "mm:ss"
 * </pre>
 *
 * <p>The plugin reads the widget text on the client thread; the parsing lives here so it can be
 * unit-tested. A parse miss returns -1 ("unknown") so a blank/absent widget never shows a wrong
 * number.
 */
final class SoulWarsHud
{
	static final int BLUE_KILLS = 11;
	static final int RED_KILLS = 12;
	static final int BLUE_HEALTH = 15;
	static final int RED_HEALTH = 16;
	static final int BLUE_STRENGTH = 19;
	static final int RED_STRENGTH = 20;
	static final int TIME_LEFT = 23;

	private SoulWarsHud()
	{
	}

	/** Numerator of an "x/y" fraction (e.g. avatar kills "3/5" -> 3), or -1. */
	static int parseFraction(String text)
	{
		if (text == null)
		{
			return -1;
		}
		final int slash = text.indexOf('/');
		if (slash <= 0)
		{
			return -1;
		}
		return parseIntOrNeg(text.substring(0, slash).trim());
	}

	/** A percentage (e.g. avatar strength "45%" -> 45), or -1. */
	static int parsePercent(String text)
	{
		if (text == null)
		{
			return -1;
		}
		final int pct = text.indexOf('%');
		if (pct <= 0)
		{
			return -1;
		}
		return parseIntOrNeg(text.substring(0, pct).trim());
	}

	private static int parseIntOrNeg(String s)
	{
		try
		{
			final int v = Integer.parseInt(s);
			return v < 0 ? -1 : v;
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}
}
