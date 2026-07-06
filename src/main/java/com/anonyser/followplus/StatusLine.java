package com.anonyser.followplus;

import java.awt.Color;

/**
 * One line of the status display, shared by the in-game overlay and the external window so the
 * line-building logic lives in exactly one place (the plugin). A {@code header} renders bold /
 * banner-style; otherwise {@code left} is the label and {@code right} the value. {@code color}
 * tints the value (overlay) or the whole line (window); null means the default colour. When
 * {@code flash} is set the line pulses through {@link #flashColor} to demand attention (low
 * activity, low prayer, game nearly over).
 */
final class StatusLine
{
	private static final Color[] FLASH = {
		new Color(255, 80, 80), new Color(255, 215, 60), new Color(255, 255, 255)
	};

	final String left;
	final String right;
	final Color color;
	final boolean header;
	final boolean flash;

	private StatusLine(String left, String right, Color color, boolean header, boolean flash)
	{
		this.left = left;
		this.right = right;
		this.color = color;
		this.header = header;
		this.flash = flash;
	}

	static StatusLine header(String text, Color color)
	{
		return new StatusLine(text, null, color, true, false);
	}

	/** Label + value line. */
	static StatusLine of(String left, String right, Color color)
	{
		return new StatusLine(left, right, color, false, false);
	}

	/** Single-text line (no value column), e.g. a status such as "Not following anyone". */
	static StatusLine plain(String text, Color color)
	{
		return new StatusLine(text, null, color, false, false);
	}

	/** A label + value line that flashes for attention. */
	static StatusLine alert(String left, String right, Color color)
	{
		return new StatusLine(left, right, color, false, true);
	}

	/** The current flash colour for the given time, cycling through several attention colours. */
	static Color flashColor(long millis)
	{
		return FLASH[(int) ((millis / 300) % FLASH.length)];
	}
}
