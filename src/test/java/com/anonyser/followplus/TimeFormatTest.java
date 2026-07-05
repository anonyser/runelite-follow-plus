package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeFormatTest
{
	@Test
	public void formatsMinutesAndSeconds()
	{
		assertEquals("0:00", TimeFormat.mmss(0));
		assertEquals("0:09", TimeFormat.mmss(9.7));
		assertEquals("2:09", TimeFormat.mmss(129));
		assertEquals("15:00", TimeFormat.mmss(900));
	}

	@Test
	public void formatsHours()
	{
		assertEquals("1:01:01", TimeFormat.mmss(3661));
	}

	@Test
	public void clampsNegativeToZero()
	{
		assertEquals("0:00", TimeFormat.mmss(-5));
	}
}
