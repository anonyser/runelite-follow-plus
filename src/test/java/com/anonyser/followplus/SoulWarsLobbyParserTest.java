package com.anonyser.followplus;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SoulWarsLobbyParserTest
{
	@Test
	public void parsesCountsInVariousShapes()
	{
		assertEquals(12, SoulWarsLobbyParser.parsePlayerCount("Players waiting: 12"));
		assertEquals(12, SoulWarsLobbyParser.parsePlayerCount("12 players are waiting"));
		assertEquals(3, SoulWarsLobbyParser.parsePlayerCount("<col=ffffff>Waiting players: 3</col>"));
		assertEquals(0, SoulWarsLobbyParser.parsePlayerCount("Players waiting: 0"));
	}

	@Test
	public void timesAreNeverReadAsCounts()
	{
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount("Next game: 1:30"));
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount("Waiting for players... starts at 1:30"));
		assertEquals(12, SoulWarsLobbyParser.parsePlayerCount("Players waiting: 12  Next game: 1:30"));
		assertEquals(2, SoulWarsLobbyParser.parsePlayerCount("2 players waiting, game starts in 0:45"));
	}

	@Test
	public void rejectsTextsWithoutTheKeywords()
	{
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount("Blue: 5"));
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount("Soul Wars"));
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount("waiting"));
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount(null));
		assertEquals(-1, SoulWarsLobbyParser.parsePlayerCount(""));
	}
}
