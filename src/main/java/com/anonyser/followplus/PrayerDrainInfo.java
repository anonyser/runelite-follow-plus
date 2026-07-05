package com.anonyser.followplus;

import net.runelite.api.Prayer;

/**
 * Per-prayer drain effects, copied verbatim from RuneLite's core prayer plugin
 * (net.runelite.client.plugins.prayer.PrayerType), which sources them from
 * https://oldschool.runescape.wiki/w/Prayer#Prayer_drain_mechanics
 */
enum PrayerDrainInfo
{
	THICK_SKIN("Thick Skin", Prayer.THICK_SKIN, 1),
	BURST_OF_STRENGTH("Burst of Strength", Prayer.BURST_OF_STRENGTH, 1),
	CLARITY_OF_THOUGHT("Clarity of Thought", Prayer.CLARITY_OF_THOUGHT, 1),
	SHARP_EYE("Sharp Eye", Prayer.SHARP_EYE, 1),
	MYSTIC_WILL("Mystic Will", Prayer.MYSTIC_WILL, 1),
	ROCK_SKIN("Rock Skin", Prayer.ROCK_SKIN, 6),
	SUPERHUMAN_STRENGTH("Superhuman Strength", Prayer.SUPERHUMAN_STRENGTH, 6),
	IMPROVED_REFLEXES("Improved Reflexes", Prayer.IMPROVED_REFLEXES, 6),
	RAPID_RESTORE("Rapid Restore", Prayer.RAPID_RESTORE, 1),
	RAPID_HEAL("Rapid Heal", Prayer.RAPID_HEAL, 2),
	PROTECT_ITEM("Protect Item", Prayer.PROTECT_ITEM, 2),
	HAWK_EYE("Hawk Eye", Prayer.HAWK_EYE, 6),
	MYSTIC_LORE("Mystic Lore", Prayer.MYSTIC_LORE, 6),
	STEEL_SKIN("Steel Skin", Prayer.STEEL_SKIN, 12),
	ULTIMATE_STRENGTH("Ultimate Strength", Prayer.ULTIMATE_STRENGTH, 12),
	INCREDIBLE_REFLEXES("Incredible Reflexes", Prayer.INCREDIBLE_REFLEXES, 12),
	PROTECT_FROM_MAGIC("Protect from Magic", Prayer.PROTECT_FROM_MAGIC, 12),
	PROTECT_FROM_MISSILES("Protect from Missiles", Prayer.PROTECT_FROM_MISSILES, 12),
	PROTECT_FROM_MELEE("Protect from Melee", Prayer.PROTECT_FROM_MELEE, 12),
	EAGLE_EYE("Eagle Eye", Prayer.EAGLE_EYE, 12),
	MYSTIC_MIGHT("Mystic Might", Prayer.MYSTIC_MIGHT, 12),
	RETRIBUTION("Retribution", Prayer.RETRIBUTION, 3),
	REDEMPTION("Redemption", Prayer.REDEMPTION, 6),
	SMITE("Smite", Prayer.SMITE, 18),
	PRESERVE("Preserve", Prayer.PRESERVE, 2),
	CHIVALRY("Chivalry", Prayer.CHIVALRY, 24),
	DEADEYE("Deadeye", Prayer.DEADEYE, 12),
	MYSTIC_VIGOUR("Mystic Vigour", Prayer.MYSTIC_VIGOUR, 12),
	PIETY("Piety", Prayer.PIETY, 24),
	RIGOUR("Rigour", Prayer.RIGOUR, 24),
	AUGURY("Augury", Prayer.AUGURY, 24),

	RP_REJUVENATION("Rejuvenation", Prayer.RP_REJUVENATION, 4),
	RP_ANCIENT_STRENGTH("Ancient Strength", Prayer.RP_ANCIENT_STRENGTH, 18),
	RP_ANCIENT_SIGHT("Ancient Sight", Prayer.RP_ANCIENT_SIGHT, 18),
	RP_ANCIENT_WILL("Ancient Will", Prayer.RP_ANCIENT_WILL, 18),
	RP_PROTECT_ITEM("Protect Item", Prayer.RP_PROTECT_ITEM, 18),
	RP_RUINOUS_GRACE("Ruinous Grace", Prayer.RP_RUINOUS_GRACE, 1),
	RP_DAMPEN_MAGIC("Dampen Magic", Prayer.RP_DAMPEN_MAGIC, 14),
	RP_DAMPEN_RANGED("Dampen Ranged", Prayer.RP_DAMPEN_RANGED, 14),
	RP_DAMPEN_MELEE("Dampen Melee", Prayer.RP_DAMPEN_MELEE, 14),
	RP_TRINITAS("Trinitas", Prayer.RP_TRINITAS, 22),
	RP_BERSERKER("Berserker", Prayer.RP_BERSERKER, 2),
	RP_PURGE("Purge", Prayer.RP_PURGE, 18),
	RP_METABOLISE("Metabolise", Prayer.RP_METABOLISE, 12),
	RP_REBUKE("Rebuke", Prayer.RP_REBUKE, 12),
	RP_VINDICATION("Vindication", Prayer.RP_VINDICATION, 9),
	RP_DECIMATE("Decimate", Prayer.RP_DECIMATE, 28),
	RP_ANNIHILATE("Annihilate", Prayer.RP_ANNIHILATE, 28),
	RP_VAPORISE("Vaporise", Prayer.RP_VAPORISE, 28),
	RP_FUMUS_VOW("Fumus' Vow", Prayer.RP_FUMUS_VOW, 14),
	RP_UMBRAS_VOW("Umbra's Vow", Prayer.RP_UMBRA_VOW, 14),
	RP_CRUORS_VOW("Cruor's Vow", Prayer.RP_CRUORS_VOW, 14),
	RP_GLACIES_VOW("Glacies' Vow", Prayer.RP_GLACIES_VOW, 14),
	RP_WRATH("Wrath", Prayer.RP_WRATH, 3),
	RP_INTENSIFY("Intensify", Prayer.RP_INTENSIFY, 28);

	private final String displayName;
	private final Prayer prayer;
	private final int drainEffect;

	PrayerDrainInfo(String displayName, Prayer prayer, int drainEffect)
	{
		this.displayName = displayName;
		this.prayer = prayer;
		this.drainEffect = drainEffect;
	}

	String getDisplayName()
	{
		return displayName;
	}

	Prayer getPrayer()
	{
		return prayer;
	}

	int getDrainEffect()
	{
		return drainEffect;
	}
}
