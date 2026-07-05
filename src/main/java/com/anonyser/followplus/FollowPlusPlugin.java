package com.anonyser.followplus;

import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.WorldView;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Follow Plus",
	description = "Puts Follow at the top of player menus and shows follow, prayer and Soul Wars status in a small overlay",
	tags = {"follow", "soul wars", "taxi", "prayer", "overlay"}
)
public class FollowPlusPlugin extends Plugin
{
	// The activity bar varbit runs 0..800 (verified against the Lucidare soul-wars hub plugin).
	static final int MAX_ACTIVITY = 800;
	// Top-level children of the Soul Wars game interface scanned for the match clock.
	private static final int SW_WIDGET_SCAN_CHILDREN = 60;
	// A hitsplat on us within this many ticks counts as "currently being attacked" (~10s).
	private static final int HIT_WINDOW_TICKS = 17;
	// Ticks without interacting with the follow target before deciding we stopped. The larger
	// unconfirmed grace covers the pathing delay between the Follow click and the first
	// interaction; once following was seen, loss of interaction means it ended.
	private static final int FOLLOW_GRACE_UNCONFIRMED = 10;
	private static final int FOLLOW_GRACE_CONFIRMED = 3;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private FollowPlusOverlay overlay;

	@Inject
	private FollowPlusConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private ConfigManager configManager;

	// created/disposed on the EDT, read on the game thread
	private volatile FollowPlusWindow window;

	private final UnderAttackTracker underAttack = new UnderAttackTracker(HIT_WINDOW_TICKS);
	private final ActivityDecayEstimator activityEstimator = new ActivityDecayEstimator();
	private final GameTimerTracker gameTimer = new GameTimerTracker();
	private final GameTimerTracker lobbyTimer = new GameTimerTracker();

	// Follow state
	private String followTargetName;
	private boolean followConfirmed;
	private int followMismatchTicks;

	// Snapshots the overlay reads (written and read on the client thread)
	private UnderAttackTracker.Status attackStatus = UnderAttackTracker.Status.NONE;
	private final List<String> activePrayerNames = new ArrayList<>();
	private int activeDrainEffect;
	private int prayerBonus;
	private boolean prayerBonusKnown;
	private int soulWarsTeam;
	private int activityValue = -1;
	private double activitySecondsLeft = -1;
	private int gameSecondsLeft = -1;
	private boolean inLobby;
	private int waitingPlayers = -1;
	private int lobbySecondsLeft = -1;

	@Override
	protected void startUp()
	{
		overlayManager.add(overlay);
		if (config.externalWindow())
		{
			openWindow();
		}
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				soulWarsTeam = client.getVarbitValue(VarbitID.SOUL_WARS_TEAM);
				if (soulWarsTeam != 0)
				{
					activityValue = client.getVarbitValue(VarbitID.SOUL_WARS_ACTIVITY_VALUE);
				}
				updatePrayerBonus();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(overlay);
		closeWindow();
		resetAll();
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!FollowPlusConfig.GROUP.equals(event.getGroup()))
		{
			return;
		}
		if ("externalWindow".equals(event.getKey()))
		{
			if (config.externalWindow())
			{
				openWindow();
			}
			else
			{
				closeWindow();
			}
		}
	}

	private void openWindow()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (window == null)
			{
				window = new FollowPlusWindow(configManager);
				window.setVisible(true);
			}
		});
	}

	private void closeWindow()
	{
		SwingUtilities.invokeLater(() ->
		{
			if (window != null)
			{
				window.dispose();
				window = null;
			}
		});
	}

	@Provides
	FollowPlusConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FollowPlusConfig.class);
	}

	@Subscribe
	public void onMenuOpened(MenuOpened event)
	{
		if (!config.followAtTop())
		{
			return;
		}
		final MenuEntry[] entries = event.getMenuEntries();
		final boolean[] isFollow = new boolean[entries.length];
		boolean any = false;
		for (int i = 0; i < entries.length; i++)
		{
			isFollow[i] = isFollowEntry(entries[i]);
			any |= isFollow[i];
		}
		if (!any)
		{
			return;
		}
		final int[] order = FollowMenuSorter.followsToTop(isFollow);
		if (order == null)
		{
			return;
		}
		final MenuEntry[] sorted = new MenuEntry[entries.length];
		for (int i = 0; i < entries.length; i++)
		{
			sorted[i] = entries[order[i]];
		}
		client.getMenu().setMenuEntries(sorted);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		final MenuAction action = event.getMenuAction();
		if (isPlayerOption(action) && "Follow".equalsIgnoreCase(event.getMenuOption()))
		{
			final Player p = event.getMenuEntry().getPlayer();
			if (p != null && p != client.getLocalPlayer() && p.getName() != null)
			{
				followTargetName = p.getName();
				followConfirmed = false;
				followMismatchTicks = 0;
			}
			return;
		}
		if (followTargetName != null && cancelsFollow(action))
		{
			clearFollow();
		}
	}

	@Subscribe
	public void onGameTick(GameTick tick)
	{
		final Player local = client.getLocalPlayer();
		if (local == null)
		{
			return;
		}
		final int tickCount = client.getTickCount();

		updateFollow(local);
		updateAttackStatus(local, tickCount);
		updatePrayerSnapshot();
		if (!prayerBonusKnown)
		{
			updatePrayerBonus();
		}
		updateSoulWars(tickCount);
		pushWindowSnapshot();
	}

	/**
	 * Builds the external window's content on the game thread (client reads must happen
	 * here), then hands the immutable snapshot to the EDT. The window never sees the client.
	 */
	private void pushWindowSnapshot()
	{
		final FollowPlusWindow w = window;
		if (w == null)
		{
			return;
		}
		final boolean inGame = soulWarsTeam != 0;

		String following = null;
		Color followingColor = null;
		if (config.showFollowingStatus())
		{
			if (followTargetName != null)
			{
				following = "Following: " + followTargetName;
				followingColor = FollowPlusWindow.GREEN;
			}
			else
			{
				following = "Not following anyone";
				followingColor = FollowPlusWindow.RED;
			}
		}

		String hp = null;
		String warning = null;
		Color warningColor = null;
		if (config.showHpWarning())
		{
			hp = "HP: " + client.getBoostedSkillLevel(Skill.HITPOINTS)
				+ " / " + client.getRealSkillLevel(Skill.HITPOINTS);
			if (attackStatus == UnderAttackTracker.Status.UNDER_ATTACK)
			{
				warning = "Currently being attacked";
				warningColor = FollowPlusWindow.RED;
			}
			else if (attackStatus == UnderAttackTracker.Status.POSSIBLE)
			{
				warning = "Possibly being attacked";
				warningColor = FollowPlusWindow.AMBER;
			}
		}

		String prayer = null;
		String prayers = null;
		if (config.showPrayerTimer())
		{
			final int points = client.getBoostedSkillLevel(Skill.PRAYER);
			prayer = "Prayer: " + points + " / " + client.getRealSkillLevel(Skill.PRAYER);
			final double seconds = PrayerDrainCalculator.secondsRemaining(points, activeDrainEffect, prayerBonus);
			if (seconds >= 0)
			{
				prayer += " (~" + TimeFormat.mmss(seconds) + ")";
			}
			if (!activePrayerNames.isEmpty())
			{
				prayers = String.join(", ", activePrayerNames);
			}
		}

		String swLine1 = null;
		String swLine2 = null;
		if (inGame)
		{
			if (config.showActivityTimer())
			{
				swLine1 = "Activity: " + (activityValue >= 0
					? Math.round(activityValue * 100f / MAX_ACTIVITY) + "%" : "Unknown")
					+ (activitySecondsLeft >= 0 ? " (~" + TimeFormat.mmss(activitySecondsLeft) + ")" : "");
			}
			if (config.showGameTimer())
			{
				swLine2 = "Game: " + (gameSecondsLeft >= 0 ? TimeFormat.mmss(gameSecondsLeft) : "Unknown");
			}
		}
		else if (inLobby && config.showLobbyInfo())
		{
			swLine1 = "Players waiting: " + (waitingPlayers >= 0 ? String.valueOf(waitingPlayers) : "Unknown");
			swLine2 = "Next game: " + (lobbySecondsLeft >= 0 ? TimeFormat.mmss(lobbySecondsLeft) : "Unknown");
		}

		final FollowPlusWindow.Snapshot snap = new FollowPlusWindow.Snapshot(
			inGame, following, followingColor, hp, warning, warningColor, prayer, prayers, swLine1, swLine2);
		SwingUtilities.invokeLater(() -> w.update(snap));
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() == client.getLocalPlayer())
		{
			underAttack.recordIncomingHit(client.getTickCount());
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.SOUL_WARS_TEAM)
		{
			final int team = event.getValue();
			if (team != soulWarsTeam)
			{
				soulWarsTeam = team;
				activityEstimator.reset();
				gameTimer.reset();
				lobbyTimer.reset();
				activityValue = team != 0 ? client.getVarbitValue(VarbitID.SOUL_WARS_ACTIVITY_VALUE) : -1;
			}
		}
		else if (event.getVarbitId() == VarbitID.SOUL_WARS_ACTIVITY_VALUE)
		{
			activityValue = event.getValue();
			activityEstimator.onValue(activityValue, client.getTickCount());
		}
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() == InventoryID.WORN)
		{
			updatePrayerBonus();
		}
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		if (followTargetName != null && followTargetName.equals(event.getPlayer().getName()))
		{
			clearFollow();
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		switch (event.getGameState())
		{
			case LOGIN_SCREEN:
			case HOPPING:
				resetAll();
				break;
			case LOGGED_IN:
				prayerBonusKnown = false;
				break;
			default:
				break;
		}
	}

	// ----- follow -----

	private void updateFollow(Player local)
	{
		if (followTargetName == null)
		{
			return;
		}
		final Actor interacting = local.getInteracting();
		final boolean match = interacting instanceof Player
			&& followTargetName.equals(interacting.getName());
		if (match)
		{
			followConfirmed = true;
			followMismatchTicks = 0;
			return;
		}
		followMismatchTicks++;
		final int grace = followConfirmed ? FOLLOW_GRACE_CONFIRMED : FOLLOW_GRACE_UNCONFIRMED;
		if (followMismatchTicks > grace)
		{
			clearFollow();
		}
	}

	private void clearFollow()
	{
		followTargetName = null;
		followConfirmed = false;
		followMismatchTicks = 0;
	}

	// ----- combat -----

	private void updateAttackStatus(Player local, int tickCount)
	{
		int count = 0;
		final WorldView wv = client.getTopLevelWorldView();
		if (wv != null)
		{
			for (Player p : wv.players())
			{
				if (p != local && p.getInteracting() == local)
				{
					count++;
				}
			}
			for (NPC n : wv.npcs())
			{
				// combat level 0 filters out pets and other harmless followers
				if (n.getInteracting() == local && n.getCombatLevel() > 0)
				{
					count++;
				}
			}
		}
		attackStatus = underAttack.status(tickCount, count);
	}

	// ----- prayer -----

	private void updatePrayerSnapshot()
	{
		activePrayerNames.clear();
		int drain = 0;
		final boolean lms = client.getVarbitValue(VarbitID.BR_INGAME) != 0;
		final boolean deadeye = !lms && client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
		final boolean vigour = !lms && client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
		for (PrayerDrainInfo info : PrayerDrainInfo.values())
		{
			// Deadeye/Mystic Vigour replace Eagle Eye/Mystic Might in the same prayer slot;
			// count whichever the unlock varbits say the slot currently is (mirrors core PrayerType).
			if ((info == PrayerDrainInfo.EAGLE_EYE && deadeye)
				|| (info == PrayerDrainInfo.DEADEYE && !deadeye)
				|| (info == PrayerDrainInfo.MYSTIC_MIGHT && vigour)
				|| (info == PrayerDrainInfo.MYSTIC_VIGOUR && !vigour))
			{
				continue;
			}
			// isPrayerActive is deprecated ONLY because it can't tell Deadeye from Eagle Eye
			// (and Vigour from Might) - the varbit guards above resolve exactly that, the same
			// way the core prayer plugin does before calling it.
			if (client.isPrayerActive(info.getPrayer()))
			{
				drain += info.getDrainEffect();
				activePrayerNames.add(info.getDisplayName());
			}
		}
		activeDrainEffect = drain;
	}

	private void updatePrayerBonus()
	{
		final ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		if (worn == null)
		{
			return; // keep the last known value until the equipment container exists
		}
		int total = 0;
		for (Item item : worn.getItems())
		{
			final ItemStats stats = itemManager.getItemStats(item.getId());
			if (stats != null && stats.getEquipment() != null)
			{
				total += stats.getEquipment().getPrayer();
			}
		}
		prayerBonus = total;
		prayerBonusKnown = true;
	}

	// ----- soul wars -----

	private void updateSoulWars(int tickCount)
	{
		if (soulWarsTeam != 0)
		{
			inLobby = false;
			waitingPlayers = -1;
			lobbySecondsLeft = -1;
			final double ticksToZero = activityEstimator.ticksToZero(tickCount);
			activitySecondsLeft = ticksToZero < 0 ? -1 : ticksToZero * 0.6;
			if (config.showGameTimer())
			{
				scanGameTimerWidgets();
				gameSecondsLeft = gameTimer.getSeconds(System.currentTimeMillis());
			}
			else
			{
				gameSecondsLeft = -1;
			}
			return;
		}
		activitySecondsLeft = -1;
		gameSecondsLeft = -1;
		if (config.showLobbyInfo())
		{
			scanLobbyWidgets();
		}
		else
		{
			inLobby = false;
			waitingPlayers = -1;
			lobbySecondsLeft = -1;
		}
	}

	/**
	 * The exact child holding the Soul Wars match clock isn't verified anywhere, so every
	 * time-looking text in the game interface is fed to GameTimerTracker, which only trusts a
	 * widget once it has counted down like a real clock. Anything unproven stays "Unknown".
	 */
	private void scanGameTimerWidgets()
	{
		final long now = System.currentTimeMillis();
		forEachWidgetText(InterfaceID.SOUL_WARS_GAME, (key, text) ->
		{
			final int seconds = SoulWarsTimeParser.parseTimeSeconds(text);
			if (seconds >= 0)
			{
				gameTimer.observe(key, seconds, now);
			}
		});
	}

	/**
	 * Same discovery approach for the lobby board (interface 434): its layout isn't verified,
	 * so any counting-down time becomes "next game" via its own lock-on tracker, and the
	 * waiting count is taken from texts that mention players/waiting - one line per team is
	 * the likely layout, so multiple matches are summed. Anything unread stays "Unknown".
	 */
	private void scanLobbyWidgets()
	{
		final long now = System.currentTimeMillis();
		final int[] waitingSum = {-1};
		final int visible = forEachWidgetText(InterfaceID.SOUL_WARS_LOBBY, (key, text) ->
		{
			final int seconds = SoulWarsTimeParser.parseTimeSeconds(text);
			final int count = SoulWarsLobbyParser.parsePlayerCount(text);
			if (seconds >= 0)
			{
				lobbyTimer.observe(key, seconds, now);
			}
			if (count >= 0)
			{
				waitingSum[0] = waitingSum[0] < 0 ? count : waitingSum[0] + count;
			}
		});
		inLobby = visible > 0;
		waitingPlayers = waitingSum[0];
		lobbySecondsLeft = lobbyTimer.getSeconds(now);
	}

	private interface TextVisitor
	{
		void visit(int key, String text);
	}

	/**
	 * Walks every visible text in an interface, keyed stably enough to survive across ticks.
	 * Returns the number of visible top-level children, i.e. 0 when the interface isn't open.
	 */
	private int forEachWidgetText(int groupId, TextVisitor visitor)
	{
		int visible = 0;
		for (int child = 0; child < SW_WIDGET_SCAN_CHILDREN; child++)
		{
			final Widget w = client.getWidget(groupId, child);
			if (w == null || w.isHidden())
			{
				continue;
			}
			visible++;
			visitText(w, child * 1000, visitor);
			visitChildren(w.getStaticChildren(), child * 1000 + 100, visitor);
			visitChildren(w.getDynamicChildren(), child * 1000 + 400, visitor);
			visitChildren(w.getNestedChildren(), child * 1000 + 700, visitor);
		}
		return visible;
	}

	private void visitChildren(Widget[] children, int keyBase, TextVisitor visitor)
	{
		if (children == null)
		{
			return;
		}
		for (int i = 0; i < children.length && i < 100; i++)
		{
			if (children[i] != null && !children[i].isHidden())
			{
				visitText(children[i], keyBase + i, visitor);
			}
		}
	}

	private void visitText(Widget w, int key, TextVisitor visitor)
	{
		final String text = w.getText();
		if (text != null && !text.isEmpty())
		{
			visitor.visit(key, text);
		}
	}

	// ----- shared helpers -----

	private void resetAll()
	{
		clearFollow();
		underAttack.reset();
		activityEstimator.reset();
		gameTimer.reset();
		lobbyTimer.reset();
		attackStatus = UnderAttackTracker.Status.NONE;
		activePrayerNames.clear();
		activeDrainEffect = 0;
		prayerBonusKnown = false;
		soulWarsTeam = 0;
		activityValue = -1;
		activitySecondsLeft = -1;
		gameSecondsLeft = -1;
		inLobby = false;
		waitingPlayers = -1;
		lobbySecondsLeft = -1;
	}

	private boolean isFollowEntry(MenuEntry entry)
	{
		if (!isPlayerOption(entry.getType()) || !"Follow".equalsIgnoreCase(entry.getOption()))
		{
			return false;
		}
		final Player p = entry.getPlayer();
		return p != null && p != client.getLocalPlayer();
	}

	private static boolean isPlayerOption(MenuAction action)
	{
		switch (action)
		{
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case PLAYER_EIGHTH_OPTION:
				return true;
			default:
				return false;
		}
	}

	/**
	 * Clicks that issue a new action or movement, which the game answers by cancelling any
	 * current follow. Interface clicks (prayers, spells on yourself, config) don't cancel a
	 * follow, so they are deliberately not listed; the interaction check in updateFollow is
	 * the backstop for anything missed here.
	 */
	private static boolean cancelsFollow(MenuAction action)
	{
		switch (action)
		{
			case WALK:
			case PLAYER_FIRST_OPTION:
			case PLAYER_SECOND_OPTION:
			case PLAYER_THIRD_OPTION:
			case PLAYER_FOURTH_OPTION:
			case PLAYER_FIFTH_OPTION:
			case PLAYER_SIXTH_OPTION:
			case PLAYER_SEVENTH_OPTION:
			case PLAYER_EIGHTH_OPTION:
			case NPC_FIRST_OPTION:
			case NPC_SECOND_OPTION:
			case NPC_THIRD_OPTION:
			case NPC_FOURTH_OPTION:
			case NPC_FIFTH_OPTION:
			case GAME_OBJECT_FIRST_OPTION:
			case GAME_OBJECT_SECOND_OPTION:
			case GAME_OBJECT_THIRD_OPTION:
			case GAME_OBJECT_FOURTH_OPTION:
			case GAME_OBJECT_FIFTH_OPTION:
			case GROUND_ITEM_FIRST_OPTION:
			case GROUND_ITEM_SECOND_OPTION:
			case GROUND_ITEM_THIRD_OPTION:
			case GROUND_ITEM_FOURTH_OPTION:
			case GROUND_ITEM_FIFTH_OPTION:
			case ITEM_USE_ON_PLAYER:
			case ITEM_USE_ON_NPC:
			case ITEM_USE_ON_GAME_OBJECT:
			case ITEM_USE_ON_GROUND_ITEM:
			case WIDGET_TARGET_ON_PLAYER:
			case WIDGET_TARGET_ON_NPC:
			case WIDGET_TARGET_ON_GAME_OBJECT:
			case WIDGET_TARGET_ON_GROUND_ITEM:
				return true;
			default:
				return false;
		}
	}

	// ----- overlay accessors (client thread only) -----

	String getFollowTargetName()
	{
		return followTargetName;
	}

	UnderAttackTracker.Status getAttackStatus()
	{
		return attackStatus;
	}

	List<String> getActivePrayerNames()
	{
		return Collections.unmodifiableList(activePrayerNames);
	}

	int getActiveDrainEffect()
	{
		return activeDrainEffect;
	}

	int getPrayerBonus()
	{
		return prayerBonus;
	}

	boolean isInSoulWars()
	{
		return soulWarsTeam != 0;
	}

	int getActivityValue()
	{
		return activityValue;
	}

	double getActivitySecondsLeft()
	{
		return activitySecondsLeft;
	}

	int getGameSecondsLeft()
	{
		return gameSecondsLeft;
	}

	boolean isInSoulWarsLobby()
	{
		return inLobby;
	}

	int getWaitingPlayers()
	{
		return waitingPlayers;
	}

	int getLobbySecondsLeft()
	{
		return lobbySecondsLeft;
	}
}
