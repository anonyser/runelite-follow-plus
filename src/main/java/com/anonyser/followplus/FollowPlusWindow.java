package com.anonyser.followplus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.FontManager;

/**
 * The external always-on-top window (AFK Overlay style): stays visible while the RuneLite client
 * is buried behind other windows, which is the whole point for people running alts. Undecorated,
 * drag anywhere to move, position saved to config. It renders the {@link StatusLine} model the
 * plugin builds; a label per line, updated in place so the text never jitters, re-packing only
 * when the number of lines changes. This class never touches the client API.
 */
class FollowPlusWindow extends JFrame
{
	private static final Color BG = new Color(24, 24, 24);
	private static final Color BORDER = new Color(70, 70, 70);
	private static final Color FG = new Color(220, 220, 220);
	private static final Color MUTED = new Color(160, 160, 160);
	private static final int WIDTH_PX = 220;

	private static final String KEY_X = "windowX";
	private static final String KEY_Y = "windowY";
	// Movement under this many pixels between press and release counts as a click, not a drag.
	private static final int CLICK_SLOP_PX = 4;

	private final ConfigManager configManager;
	private final Runnable onClick;
	private final JPanel content;
	private final Font normalFont;
	private final Font boldFont;
	private final List<JLabel> labels = new ArrayList<>();
	private final Timer flashTimer = new Timer(300, e -> applyFlash());
	private List<StatusLine> model;

	private Point dragPoint;
	private Point pressScreen;

	FollowPlusWindow(ConfigManager configManager, Runnable onClick)
	{
		this.configManager = configManager;
		this.onClick = onClick;

		setTitle("Soul Wars Status");
		setUndecorated(true);
		setAlwaysOnTop(true);
		setType(Type.NORMAL);
		// never steal focus from the game (or whatever the main account is doing)
		setFocusableWindowState(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		content = new JPanel()
		{
			@Override
			public Dimension getPreferredSize()
			{
				// fixed width so text changes don't make the window wobble
				return new Dimension(WIDTH_PX, super.getPreferredSize().height);
			}
		};
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(BG);
		content.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createLineBorder(BORDER),
			BorderFactory.createEmptyBorder(6, 10, 8, 10)));

		normalFont = FontManager.getRunescapeFont();
		boldFont = normalFont.deriveFont(Font.BOLD);

		final JLabel title = makeLabel("Soul Wars Status", MUTED);
		content.add(title);
		setContentPane(content);

		// drag anywhere to move; a click (no real drag) raises the RuneLite client. The labels have
		// no listeners so events fall through to us.
		final MouseAdapter dragger = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				dragPoint = e.getPoint();
				pressScreen = e.getLocationOnScreen();
			}

			@Override
			public void mouseDragged(MouseEvent e)
			{
				if (dragPoint != null)
				{
					final Point p = getLocation();
					setLocation(p.x + e.getX() - dragPoint.x, p.y + e.getY() - dragPoint.y);
				}
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				final boolean click = pressScreen != null
					&& e.getLocationOnScreen().distance(pressScreen) <= CLICK_SLOP_PX;
				dragPoint = null;
				pressScreen = null;
				if (click)
				{
					if (onClick != null)
					{
						onClick.run();
					}
				}
				else
				{
					savePosition();
				}
			}
		};
		addMouseListener(dragger);
		addMouseMotionListener(dragger);

		pack();
		loadPosition();
		flashTimer.start();
	}

	@Override
	public void dispose()
	{
		flashTimer.stop();
		super.dispose();
	}

	/** EDT only. */
	void update(List<StatusLine> model)
	{
		this.model = model;
		boolean structureChanged = false;
		if (labels.size() != model.size())
		{
			rebuild(model.size());
			structureChanged = true;
		}
		for (int i = 0; i < model.size(); i++)
		{
			applyLine(labels.get(i), model.get(i));
		}
		if (structureChanged)
		{
			pack();
		}
	}

	/** Re-tints flashing lines each timer tick so they pulse between updates. */
	private void applyFlash()
	{
		final List<StatusLine> m = model;
		if (m == null)
		{
			return;
		}
		final Color c = StatusLine.flashColor(System.currentTimeMillis());
		for (int i = 0; i < m.size() && i < labels.size(); i++)
		{
			if (m.get(i).flash)
			{
				labels.get(i).setForeground(c);
			}
		}
	}

	private void rebuild(int count)
	{
		for (JLabel label : labels)
		{
			content.remove(label);
		}
		labels.clear();
		for (int i = 0; i < count; i++)
		{
			final JLabel label = makeLabel("", FG);
			content.add(label);
			labels.add(label);
		}
		content.revalidate();
	}

	private void applyLine(JLabel label, StatusLine line)
	{
		label.setText(line.right != null ? line.left + ":  " + line.right : line.left);
		label.setForeground(line.flash
			? StatusLine.flashColor(System.currentTimeMillis())
			: (line.color != null ? line.color : FG));
		label.setFont(line.header ? boldFont : normalFont);
	}

	private static JLabel makeLabel(String text, Color color)
	{
		final JLabel label = new JLabel(text);
		label.setForeground(color);
		label.setFont(FontManager.getRunescapeFont());
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		label.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
		return label;
	}

	private void loadPosition()
	{
		final Integer x = configManager.getConfiguration(FollowPlusConfig.GROUP, KEY_X, Integer.class);
		final Integer y = configManager.getConfiguration(FollowPlusConfig.GROUP, KEY_Y, Integer.class);
		final Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		if (x != null && y != null && screen.intersects(new Rectangle(x, y, WIDTH_PX, 60)))
		{
			setLocation(x, y);
		}
		else
		{
			setLocation(100, 100);
		}
	}

	private void savePosition()
	{
		configManager.setConfiguration(FollowPlusConfig.GROUP, KEY_X, getX());
		configManager.setConfiguration(FollowPlusConfig.GROUP, KEY_Y, getY());
	}
}
