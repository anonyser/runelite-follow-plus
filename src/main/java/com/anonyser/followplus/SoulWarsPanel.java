package com.anonyser.followplus;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import net.runelite.api.Experience;
import net.runelite.api.Skill;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Side panel: Zeal balance and lifetime score, crates that buys, the daily Zeal-XP tally with a
 * reset countdown, a Zeal calculator (From and Target both editable and auto-filled from your own
 * levels, each independently entered as a level or as XP with flexible number entry) and a
 * quick-reference of XP per Zeal Token. Pure Swing; the plugin feeds it primitives on the EDT.
 */
class SoulWarsPanel extends PluginPanel
{
	static final Skill[] SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS,
		Skill.RANGED, Skill.MAGIC, Skill.PRAYER
	};
	private static final long MS_PER_DAY = 86_400_000L;

	private final JLabel tokensValue = value();
	private final JLabel lifetimeValue = value();
	private final JLabel cratesValue = value();
	private final JLabel dailyValue = value();
	private final JLabel resetsValue = value();

	private final JComboBox<String> fromModeBox = new JComboBox<>(new String[]{"Level", "XP"});
	private final JComboBox<String> targetModeBox = new JComboBox<>(new String[]{"Level", "XP"});
	private final JTextField[] from = new JTextField[SKILLS.length];
	private final JTextField[] target = new JTextField[SKILLS.length];
	private final JLabel[] zealNeeded = new JLabel[SKILLS.length];
	private final int[] currentXp = new int[SKILLS.length];
	private final int[] currentLvl = new int[SKILLS.length];
	private boolean seeded;
	private boolean fromXpMode;
	private boolean targetXpMode;

	SoulWarsPanel()
	{
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		final JPanel body = new JPanel();
		body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
		body.setBackground(ColorScheme.DARK_GRAY_COLOR);

		body.add(title("Zeal"));
		body.add(buildSummary());

		body.add(gap());
		body.add(title("Zeal calculator"));
		body.add(buildCalculator());

		body.add(gap());
		body.add(blurb());
		body.add(title("XP per Zeal Token"));
		body.add(buildReference());

		add(body, BorderLayout.NORTH);
	}

	private JPanel buildSummary()
	{
		final JPanel grid = new JPanel(new GridLayout(0, 2, 6, 3));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		grid.add(muted("Zeal tokens"));
		grid.add(tokensValue);
		grid.add(muted("Lifetime"));
		grid.add(lifetimeValue);
		grid.add(muted("Crates"));
		grid.add(cratesValue);
		grid.add(muted("XP today"));
		grid.add(dailyValue);
		grid.add(muted("Resets in"));
		grid.add(resetsValue);
		return grid;
	}

	private JPanel buildCalculator()
	{
		final JPanel grid = new JPanel(new GridLayout(SKILLS.length + 1, 4, 5, 4));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		// header row: the two column dropdowns pick how From / Target are entered (level vs XP)
		grid.add(header("Skill"));
		fromModeBox.addActionListener(e -> onFromModeChanged());
		grid.add(fromModeBox);
		targetModeBox.addActionListener(e -> onTargetModeChanged());
		grid.add(targetModeBox);
		grid.add(header("Zeal"));
		for (int i = 0; i < SKILLS.length; i++)
		{
			final int idx = i;
			grid.add(muted(skillLabel(SKILLS[i])));
			from[i] = field(idx);
			grid.add(from[i]);
			target[i] = field(idx);
			grid.add(target[i]);
			zealNeeded[i] = value();
			grid.add(zealNeeded[i]);
		}
		return grid;
	}

	private JTextField field(int idx)
	{
		final JTextField f = new JTextField(4);
		f.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				recompute(idx);
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				recompute(idx);
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				recompute(idx);
			}
		});
		return f;
	}

	private JPanel buildReference()
	{
		final List<Integer> levels = bandLevels();
		final JPanel grid = new JPanel(new GridLayout(levels.size() + 1, 4, 5, 2));
		grid.setBackground(ColorScheme.DARK_GRAY_COLOR);
		grid.add(centered("Lvl", ColorScheme.LIGHT_GRAY_COLOR));
		grid.add(centered("Mel/HP", ColorScheme.LIGHT_GRAY_COLOR));
		grid.add(centered("Rng/Mag", ColorScheme.LIGHT_GRAY_COLOR));
		grid.add(centered("Pray", ColorScheme.LIGHT_GRAY_COLOR));
		for (int lvl : levels)
		{
			grid.add(centered(Integer.toString(lvl), ColorScheme.LIGHT_GRAY_COLOR));
			grid.add(centered(Integer.toString(SoulWarsXpCalculator.xpPerToken(Skill.ATTACK, lvl)), Color.WHITE));
			grid.add(centered(Integer.toString(SoulWarsXpCalculator.xpPerToken(Skill.RANGED, lvl)), Color.WHITE));
			grid.add(centered(Integer.toString(SoulWarsXpCalculator.xpPerToken(Skill.PRAYER, lvl)), Color.WHITE));
		}
		return grid;
	}

	/** Levels where XP-per-token steps up (a new band), from 30 to 99. */
	private static List<Integer> bandLevels()
	{
		final List<Integer> levels = new ArrayList<>();
		int lastMult = -1;
		for (int lvl = SoulWarsXpCalculator.MIN_LEVEL; lvl <= 99; lvl++)
		{
			final int mult = lvl * lvl / 600;
			if (mult != lastMult)
			{
				levels.add(lvl);
				lastMult = mult;
			}
		}
		if (!levels.contains(99))
		{
			levels.add(99);
		}
		return levels;
	}

	/** EDT only. -1 for tokens/lifetime/dailyXp means "unknown". */
	void update(int tokens, int lifetime, int dailyXp, int[] levels, int[] xps)
	{
		tokensValue.setText(tokens >= 0 ? format(tokens) : "open Nomad");
		lifetimeValue.setText(lifetime >= 0 ? format(lifetime) : "—");
		cratesValue.setText(tokens >= 0 ? Integer.toString(SoulWarsXpCalculator.cratesAffordable(tokens)) : "—");
		dailyValue.setText(dailyXp >= 0 ? format(dailyXp) + " / 1M" : "—");
		resetsValue.setText(timeToReset());

		boolean known = false;
		for (int i = 0; i < SKILLS.length; i++)
		{
			currentLvl[i] = levels != null && i < levels.length ? levels[i] : -1;
			currentXp[i] = xps != null && i < xps.length ? xps[i] : 0;
			known |= currentLvl[i] >= 1;
		}
		// fill From/Target from the character once, then leave both entirely to the user
		if (!seeded && known)
		{
			seeded = true;
			for (int i = 0; i < SKILLS.length; i++)
			{
				seedFrom(i);
				seedTarget(i);
			}
		}
		for (int i = 0; i < SKILLS.length; i++)
		{
			recompute(i);
		}
	}

	private void onFromModeChanged()
	{
		fromXpMode = fromModeBox.getSelectedIndex() == 1;
		for (int i = 0; i < SKILLS.length; i++)
		{
			seedFrom(i);
			recompute(i);
		}
	}

	private void onTargetModeChanged()
	{
		targetXpMode = targetModeBox.getSelectedIndex() == 1;
		for (int i = 0; i < SKILLS.length; i++)
		{
			seedTarget(i);
			recompute(i);
		}
	}

	private void seedFrom(int i)
	{
		from[i].setText(seedText(i, fromXpMode));
	}

	private void seedTarget(int i)
	{
		target[i].setText(seedText(i, targetXpMode));
	}

	private String seedText(int i, boolean xpMode)
	{
		return xpMode
			? Integer.toString(Math.max(currentXp[i], 0))
			: Integer.toString(Math.max(currentLvl[i], 1));
	}

	private void recompute(int i)
	{
		final long fromXp = fieldToXp(from[i].getText(), fromXpMode);
		final long targetXp = fieldToXp(target[i].getText(), targetXpMode);
		if (fromXp < 0 || targetXp < 0)
		{
			zealNeeded[i].setText("—");
			return;
		}
		final long tokens = SoulWarsXpCalculator.tokensToXp(
			SKILLS[i], (int) Math.min(fromXp, SoulWarsXpCalculator.MAX_XP), targetXp);
		zealNeeded[i].setText(tokens <= 0 ? "—" : format((int) Math.min(tokens, Integer.MAX_VALUE)));
	}

	/** Interpret a From/Target field as total XP, as a level or as raw XP per that column's mode. */
	private static long fieldToXp(String text, boolean xpMode)
	{
		if (xpMode)
		{
			return SoulWarsXpCalculator.parseXp(text);
		}
		final int lvl = parseInt(text);
		if (lvl < 1 || lvl > Experience.MAX_REAL_LEVEL)
		{
			return -1;
		}
		return Experience.getXpForLevel(lvl);
	}

	private static String timeToReset()
	{
		final long ms = MS_PER_DAY - (System.currentTimeMillis() % MS_PER_DAY);
		return (ms / 3_600_000L) + "h " + ((ms % 3_600_000L) / 60_000L) + "m";
	}

	private static int parseInt(String s)
	{
		if (s == null)
		{
			return -1;
		}
		try
		{
			return Integer.parseInt(s.trim());
		}
		catch (NumberFormatException e)
		{
			return -1;
		}
	}

	private static String format(int n)
	{
		return String.format("%,d", n);
	}

	private static String skillLabel(Skill skill)
	{
		final String s = skill.getName();
		return s.length() > 4 ? s.substring(0, 4) : s;
	}

	private JLabel title(String text)
	{
		final JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeBoldFont());
		l.setForeground(Color.WHITE);
		l.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 0));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private JLabel blurb()
	{
		final JLabel l = new JLabel("<html>XP entry accepts 1000K, 1M, 1.12M, 200M or 200000000 "
			+ "(commas optional).</html>");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		l.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
		l.setAlignmentX(Component.LEFT_ALIGNMENT);
		return l;
	}

	private static JLabel header(String text)
	{
		final JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return l;
	}

	private static JLabel muted(String text)
	{
		final JLabel l = new JLabel(text);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		return l;
	}

	private static JLabel centered(String text, Color color)
	{
		final JLabel l = new JLabel(text, SwingConstants.CENTER);
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(color);
		return l;
	}

	private static JLabel value()
	{
		final JLabel l = new JLabel("—");
		l.setFont(FontManager.getRunescapeSmallFont());
		l.setForeground(Color.WHITE);
		return l;
	}

	private JPanel gap()
	{
		final JPanel p = new JPanel();
		p.setBackground(ColorScheme.DARK_GRAY_COLOR);
		p.setPreferredSize(new Dimension(1, 8));
		p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
		return p;
	}
}
