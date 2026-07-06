package com.anonyser.followplus;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class SoulWarsHudTest
{
	@Test
	public void parseFraction()
	{
		assertEquals(3, SoulWarsHud.parseFraction("3/5"));
		assertEquals(0, SoulWarsHud.parseFraction("0/5"));
		assertEquals(5, SoulWarsHud.parseFraction("5 / 5"));
	}

	@Test
	public void parseFractionRejectsJunk()
	{
		assertEquals(-1, SoulWarsHud.parseFraction(null));
		assertEquals(-1, SoulWarsHud.parseFraction(""));
		assertEquals(-1, SoulWarsHud.parseFraction("100%"));
		assertEquals(-1, SoulWarsHud.parseFraction("/5"));
	}

	@Test
	public void parsePercent()
	{
		assertEquals(100, SoulWarsHud.parsePercent("100%"));
		assertEquals(45, SoulWarsHud.parsePercent("45%"));
		assertEquals(0, SoulWarsHud.parsePercent("0%"));
	}

	@Test
	public void parsePercentRejectsJunk()
	{
		assertEquals(-1, SoulWarsHud.parsePercent(null));
		assertEquals(-1, SoulWarsHud.parsePercent(""));
		assertEquals(-1, SoulWarsHud.parsePercent("3/5"));
		assertEquals(-1, SoulWarsHud.parsePercent("%"));
	}
}
