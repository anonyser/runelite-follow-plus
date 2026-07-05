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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.FontManager;

/**
 * The external always-on-top window (AFK Overlay style): stays visible while the RuneLite
 * client is buried behind other windows, which is the whole point for people running alts.
 * Undecorated, drag anywhere to move, position saved to config. All updates arrive as
 * prebuilt Snapshots on the EDT - this class never touches the client API.
 */
class FollowPlusWindow extends JFrame
{
	static final Color RED = new Color(216, 60, 62);
	static final Color GREEN = new Color(0, 200, 83);
	static final Color AMBER = new Color(255, 200, 60);
	private static final Color BG = new Color(24, 24, 24);
	private static final Color BORDER = new Color(70, 70, 70);
	private static final Color FG = new Color(220, 220, 220);
	private static final Color MUTED = new Color(160, 160, 160);
	private static final int WIDTH_PX = 220;

	private static final String KEY_X = "windowX";
	private static final String KEY_Y = "windowY";

	/** Immutable view of everything the window shows; null/empty text hides a line. */
	static final class Snapshot
	{
		final boolean inGame;
		final String following;
		final Color followingColor;
		final String hp;
		final String warning;
		final Color warningColor;
		final String prayer;
		final List<String> prayerNames;
		final String swLine1;
		final String swLine2;

		Snapshot(boolean inGame, String following, Color followingColor, String hp,
			String warning, Color warningColor, String prayer, List<String> prayerNames,
			String swLine1, String swLine2)
		{
			this.inGame = inGame;
			this.following = following;
			this.followingColor = followingColor;
			this.hp = hp;
			this.warning = warning;
			this.warningColor = warningColor;
			this.prayer = prayer;
			this.prayerNames = prayerNames;
			this.swLine1 = swLine1;
			this.swLine2 = swLine2;
		}
	}

	private final ConfigManager configManager;

	private final JLabel statusLabel = makeLabel("—", FG);
	private final JLabel followingLabel = makeLabel("", FG);
	private final JLabel hpLabel = makeLabel("", FG);
	private final JLabel warningLabel = makeLabel("", RED);
	private final JLabel prayerLabel = makeLabel("", FG);
	private final JLabel prayersLabel = makeLabel("", MUTED);
	private final JLabel swLabel1 = makeLabel("", FG);
	private final JLabel swLabel2 = makeLabel("", FG);

	private Point dragPoint;
	private boolean structureChanged;
	private List<String> lastPrayers;

	FollowPlusWindow(ConfigManager configManager)
	{
		this.configManager = configManager;

		setTitle("Soul Wars Status");
		setUndecorated(true);
		setAlwaysOnTop(true);
		setType(Type.NORMAL);
		// never steal focus from the game (or whatever the main account is doing)
		setFocusableWindowState(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		final JPanel content = new JPanel()
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

		final JLabel title = makeLabel("Soul Wars Status", MUTED);
		content.add(title);
		statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
		content.add(statusLabel);
		content.add(followingLabel);
		content.add(hpLabel);
		content.add(warningLabel);
		content.add(prayerLabel);
		content.add(prayersLabel);
		content.add(swLabel1);
		content.add(swLabel2);
		setContentPane(content);

		// only the title and status show until the first snapshot arrives
		followingLabel.setVisible(false);
		hpLabel.setVisible(false);
		warningLabel.setVisible(false);
		prayerLabel.setVisible(false);
		prayersLabel.setVisible(false);
		swLabel1.setVisible(false);
		swLabel2.setVisible(false);

		// drag anywhere to move; the labels have no listeners so events fall through to us
		final MouseAdapter dragger = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent e)
			{
				dragPoint = e.getPoint();
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
				dragPoint = null;
				savePosition();
			}
		};
		addMouseListener(dragger);
		addMouseMotionListener(dragger);

		pack();
		loadPosition();
	}

	/** EDT only. */
	void update(Snapshot s)
	{
		statusLabel.setText(s.inGame ? "IN GAME" : "OUT OF GAME");
		statusLabel.setForeground(s.inGame ? GREEN : RED);
		setLine(followingLabel, s.following, s.followingColor);
		setLine(hpLabel, s.hp, FG);
		setLine(warningLabel, s.warning, s.warningColor);
		setLine(prayerLabel, s.prayer, FG);
		setPrayers(s.prayerNames);
		setLine(swLabel1, s.swLine1, FG);
		setLine(swLabel2, s.swLine2, FG);
		if (structureChanged)
		{
			structureChanged = false;
			pack();
		}
	}

	private void setLine(JLabel label, String text, Color color)
	{
		final boolean visible = text != null && !text.isEmpty();
		if (label.isVisible() != visible)
		{
			label.setVisible(visible);
			structureChanged = true;
		}
		if (visible)
		{
			label.setText(text);
			if (color != null)
			{
				label.setForeground(color);
			}
		}
	}

	/**
	 * Active prayers, one bullet per line like the in-client overlay. A single fixed-width label
	 * would truncate a long list, so each prayer gets its own line via an HTML label and the
	 * window re-packs to grow when the number of lines changes.
	 */
	private void setPrayers(List<String> names)
	{
		final boolean visible = names != null && !names.isEmpty();
		if (prayersLabel.isVisible() != visible)
		{
			prayersLabel.setVisible(visible);
			structureChanged = true;
		}
		if (visible && !names.equals(lastPrayers))
		{
			final StringBuilder sb = new StringBuilder("<html>");
			for (int i = 0; i < names.size(); i++)
			{
				if (i > 0)
				{
					sb.append("<br>");
				}
				sb.append("· ").append(names.get(i));
			}
			sb.append("</html>");
			prayersLabel.setText(sb.toString());
			// the line count may have changed, so the window must re-pack to fit
			structureChanged = true;
		}
		lastPrayers = names;
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
