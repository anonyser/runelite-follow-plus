package com.anonyser.followplus;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SoulWarsLobbyReaderTest
{
	@Test
	public void belowMinimumShowsCountOfNeededAndDash()
	{
		// live capture: not enough players yet
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Arrays.asList(
			"Players Waiting", "5/20", "Players Waiting", "5/20",
			"Next Game Start", "-", "Next Game Start", "-"));
		assertEquals("5/20", r.playersWaiting);
		assertEquals("-", r.nextGame);
	}

	@Test
	public void aboveMinimumShowsBareCountAndCountdown()
	{
		// live capture: enough players, countdown running
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Arrays.asList(
			"Players Waiting", "28", "Players Waiting", "28",
			"Next Game Start", "3 minutes", "Next Game Start", "3 minutes"));
		assertEquals("28", r.playersWaiting);
		assertEquals("3 minutes", r.nextGame);
	}

	@Test
	public void secondsCountdownIsMirroredVerbatim()
	{
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Arrays.asList(
			"Players Waiting", "40", "Next Game Start", "30 seconds"));
		assertEquals("40", r.playersWaiting);
		assertEquals("30 seconds", r.nextGame);
	}

	@Test
	public void handlesColourTags()
	{
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Arrays.asList(
			"<col=ffff00>Players Waiting</col>", "<col=ffffff>5/20</col>",
			"<col=ffff00>Next Game Start</col>", "<col=ffffff>10 seconds</col>"));
		assertEquals("5/20", r.playersWaiting);
		assertEquals("10 seconds", r.nextGame);
	}

	@Test
	public void unknownWhenLabelsAreMissing()
	{
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Arrays.asList(
			"Soul Wars", "Blue: 5", "Red: 3"));
		assertNull(r.playersWaiting);
		assertNull(r.nextGame);
	}

	@Test
	public void doesNotTakeAnotherLabelAsAValue()
	{
		// if a value widget is missing, the next text is the other label - must not be used
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Arrays.asList(
			"Players Waiting", "Next Game Start", "3 minutes"));
		assertNull(r.playersWaiting);
		assertEquals("3 minutes", r.nextGame);
	}

	@Test
	public void emptyInput()
	{
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(Collections.emptyList());
		assertNull(r.playersWaiting);
		assertNull(r.nextGame);
	}
}
