package com.anonyser.followplus;

import java.awt.Color;

/**
 * The two Soul Wars teams, keyed off the {@code SOUL_WARS_TEAM} varbit (verified live: 1 = blue,
 * 2 = red; 0 = not in a game). {@code chatIdentifier} is the lowercase phrase the capture chat
 * messages use ("the red team has captured ...").
 */
enum SoulWarsTeam
{
	NONE(0, "", "", null),
	BLUE(1, "Blue team", "blue team", new Color(80, 140, 255)),
	RED(2, "Red team", "red team", new Color(216, 60, 62));

	private final int varbit;
	private final String label;
	private final String chatIdentifier;
	private final Color color;

	SoulWarsTeam(int varbit, String label, String chatIdentifier, Color color)
	{
		this.varbit = varbit;
		this.label = label;
		this.chatIdentifier = chatIdentifier;
		this.color = color;
	}

	String label()
	{
		return label;
	}

	String chatIdentifier()
	{
		return chatIdentifier;
	}

	Color color()
	{
		return color;
	}

	static SoulWarsTeam fromVarbit(int value)
	{
		for (SoulWarsTeam team : values())
		{
			if (team.varbit == value)
			{
				return team;
			}
		}
		return NONE;
	}
}
