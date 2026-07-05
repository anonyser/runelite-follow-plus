package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SoulWarsTimeParserTest
{
	@Test
	public void parsesPlainTimes()
	{
		assertEquals(754, SoulWarsTimeParser.parseTimeSeconds("12:34"));
		assertEquals(303, SoulWarsTimeParser.parseTimeSeconds("5:03"));
		assertEquals(0, SoulWarsTimeParser.parseTimeSeconds("0:00"));
	}

	@Test
	public void parsesLabelsAndColorTags()
	{
		assertEquals(303, SoulWarsTimeParser.parseTimeSeconds("<col=ffffff>Time Remaining: 5:03</col>"));
		assertEquals(754, SoulWarsTimeParser.parseTimeSeconds("Time left: 12:34"));
	}

	@Test
	public void parsesHours()
	{
		assertEquals(3723, SoulWarsTimeParser.parseTimeSeconds("1:02:03"));
	}

	@Test
	public void rejectsNonTimes()
	{
		assertEquals(-1, SoulWarsTimeParser.parseTimeSeconds(null));
		assertEquals(-1, SoulWarsTimeParser.parseTimeSeconds(""));
		assertEquals(-1, SoulWarsTimeParser.parseTimeSeconds("Blue: 5 Red: 3"));
		assertEquals(-1, SoulWarsTimeParser.parseTimeSeconds("Soul Wars"));
		assertEquals(-1, SoulWarsTimeParser.parseTimeSeconds("12:99"));
	}
}
