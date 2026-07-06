package com.anonyser.followplus;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Hitsplat;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetClosed;
import net.runelite.api.events.WidgetLoaded;
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
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Soul Wars Status",
	description = "Soul Wars status overlay: follow status, HP, prayer, team, zeal, live match stats and timers",
	tags = {"soul wars", "taxi", "prayer", "overlay", "follow", "lobby", "zeal"}
)
public class FollowPlusPlugin extends Plugin
{
	// The activity bar varbit runs 0..800 (verified against the Lucidare soul-wars hub plugin).
	static final int MAX_ACTIVITY = 800;
	// Ticks without interacting with the follow target before deciding we stopped. The larger
	// unconfirmed grace covers the pathing delay between the Follow click and the first
	// interaction; once following was seen, loss of interaction means it ended.
	private static final int FOLLOW_GRACE_UNCONFIRMED = 10;
	private static final int FOLLOW_GRACE_CONFIRMED = 3;

	// Account varps (verified live): lifetime Zeal score and the spendable Zeal Token balance.
	private static final int LIFETIME_ZEAL_VARP = 2871;
	private static final int ZEAL_TOKENS_VARP = 2876;
	// Avatar NPCs (blue = Creation, red = Destruction) and the soul fragment item.
	private static final int AVATAR_BLUE_ID = 10531;
	private static final int AVATAR_RED_ID = 10532;
	private static final int SOUL_FRAGMENT_ID = 25201;
	// Nomad's reward shop interface; XP gained while it's open is a Zeal purchase, not combat.
	private static final int REWARD_SHOP_GROUP = 442;

	static final Color GREEN = new Color(0, 200, 83);
	static final Color RED = new Color(216, 60, 62);
	static final Color AMBER = new Color(255, 200, 60);
	private static final Color MUTED = new Color(160, 160, 160);

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

	@Inject
	private ClientToolbar clientToolbar;

	// created/disposed on the EDT, read on the game thread
	private volatile FollowPlusWindow window;
	private SoulWarsPanel panel;
	private NavigationButton navButton;
	private final DailyXpTracker dailyXp = new DailyXpTracker(EnumSet.of(
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.HITPOINTS,
		Skill.RANGED, Skill.MAGIC, Skill.PRAYER));
	private boolean dailyRestored;
	private boolean rewardShopOpen;

	private final ActivityDecayEstimator activityEstimator = new ActivityDecayEstimator();
	private final GameTimerTracker gameTimer = new GameTimerTracker();
	private final ZealTracker zeal = new ZealTracker();
	private final SoulWarsMatchTracker match = new SoulWarsMatchTracker();

	// Follow state
	private String followTargetName;
	private boolean followConfirmed;
	private int followMismatchTicks;
	private boolean followTargetMoving;
	private WorldPoint lastFollowLoc;

	// Snapshots the overlay/window read (written and read on the client thread)
	private final List<String> activePrayerNames = new ArrayList<>();
	private int activeDrainEffect;
	private int prayerBonus;
	private boolean prayerBonusKnown;
	private int soulWarsTeam;
	private SoulWarsTeam team = SoulWarsTeam.NONE;
	private int activityValue = -1;
	private double activitySecondsLeft = -1;
	private int gameSecondsLeft = -1;
	private boolean inLobby;
	private String playersWaitingText;
	private String nextGameText;
	private int runEnergyPct;
	private int blueKills = -1;
	private int redKills = -1;
	private int blueHealth = -1;
	private int redHealth = -1;
	private int blueStrength = -1;
	private int redStrength = -1;

	private volatile List<StatusLine> statusModel = Collections.emptyList();

	@Override
	protected void startUp()
	{
		zeal.startSession(System.currentTimeMillis());
		overlayManager.add(overlay);
		panel = new SoulWarsPanel();
		navButton = NavigationButton.builder()
			.tooltip("Soul Wars")
			.icon(navIcon())
			.priority(7)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		if (config.externalWindow())
		{
			openWindow();
		}
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				soulWarsTeam = client.getVarbitValue(VarbitID.SOUL_WARS_TEAM);
				team = SoulWarsTeam.fromVarbit(soulWarsTeam);
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
		if (navButton != null)
		{
			clientToolbar.removeNavigation(navButton);
		}
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

		team = SoulWarsTeam.fromVarbit(soulWarsTeam);
		updateFollow(local);
		updateFollowMovement(local);
		updatePrayerSnapshot();
		if (!prayerBonusKnown)
		{
			updatePrayerBonus();
		}
		updateSoulWars(tickCount);
		updateZealAndHud();
		final List<StatusLine> model = buildStatusModel(local);
		statusModel = model;
		pushWindow(model);
		pushPanel();
	}

	private void pushWindow(List<StatusLine> model)
	{
		final FollowPlusWindow w = window;
		if (w != null)
		{
			SwingUtilities.invokeLater(() -> w.update(model));
		}
	}

	private void pushPanel()
	{
		final SoulWarsPanel p = panel;
		if (p == null)
		{
			return;
		}
		final int[] levels = new int[SoulWarsPanel.SKILLS.length];
		final int[] xps = new int[SoulWarsPanel.SKILLS.length];
		for (int i = 0; i < SoulWarsPanel.SKILLS.length; i++)
		{
			levels[i] = client.getRealSkillLevel(SoulWarsPanel.SKILLS[i]);
			xps[i] = client.getSkillExperience(SoulWarsPanel.SKILLS[i]);
		}
		final int tokens = zeal.tokens();
		final int lifetime = zeal.lifetime();
		final int daily = dailyXp.usedToday(System.currentTimeMillis());
		SwingUtilities.invokeLater(() -> p.update(tokens, lifetime, daily, levels, xps));
	}

	private static BufferedImage navIcon()
	{
		final BufferedImage img = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D g = img.createGraphics();
		g.setColor(new Color(80, 140, 255));
		g.fillRect(0, 0, 12, 24);
		g.setColor(new Color(216, 60, 62));
		g.fillRect(12, 0, 12, 24);
		g.dispose();
		return img;
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		final Actor actor = event.getActor();
		if (soulWarsTeam != 0 && actor instanceof NPC && isAvatar(((NPC) actor).getId()))
		{
			final Hitsplat h = event.getHitsplat();
			if (h != null && h.isMine())
			{
				match.addAvatarDamage(h.getAmount());
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (soulWarsTeam == 0)
		{
			return;
		}
		// Soul Wars is instanced, so getWorldLocation() returns instance coords; convert back to the
		// template coords the capture areas are defined in.
		final Player local = client.getLocalPlayer();
		WorldPoint loc = null;
		if (local != null)
		{
			loc = client.isInInstancedRegion() && local.getLocalLocation() != null
				? WorldPoint.fromLocalInstance(client, local.getLocalLocation())
				: local.getWorldLocation();
		}
		match.onChatMessage(event.getMessage(), team, loc);
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		if (event.getVarbitId() == VarbitID.SOUL_WARS_TEAM)
		{
			final int value = event.getValue();
			if (value != soulWarsTeam)
			{
				final boolean entering = soulWarsTeam == 0 && value != 0;
				soulWarsTeam = value;
				team = SoulWarsTeam.fromVarbit(value);
				activityEstimator.reset();
				gameTimer.reset();
				activityValue = value != 0 ? client.getVarbitValue(VarbitID.SOUL_WARS_ACTIVITY_VALUE) : -1;
				if (entering)
				{
					match.reset();
				}
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
		else if (event.getContainerId() == InventoryID.INV)
		{
			match.onInventoryFragments(countFragments(event.getItemContainer()), soulWarsTeam != 0);
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
				dailyRestored = false;
				break;
			case LOGGED_IN:
				prayerBonusKnown = false;
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded event)
	{
		if (event.getGroupId() == REWARD_SHOP_GROUP)
		{
			rewardShopOpen = true;
		}
	}

	@Subscribe
	public void onWidgetClosed(WidgetClosed event)
	{
		if (event.getGroupId() == REWARD_SHOP_GROUP)
		{
			rewardShopOpen = false;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (!dailyRestored)
		{
			restoreDailyXp();
		}
		dailyXp.onXp(event.getSkill(), event.getXp(), rewardShopOpen, System.currentTimeMillis());
		// persist per-account so today's total survives a relaunch (rolls over at the daily reset)
		configManager.setRSProfileConfiguration(FollowPlusConfig.GROUP, "dailyXpDay", dailyXp.dayKey());
		configManager.setRSProfileConfiguration(FollowPlusConfig.GROUP, "dailyXpUsed", dailyXp.used());
	}

	private void restoreDailyXp()
	{
		dailyRestored = true;
		final Long day = configManager.getRSProfileConfiguration(FollowPlusConfig.GROUP, "dailyXpDay", Long.class);
		if (day != null)
		{
			final Integer used = configManager.getRSProfileConfiguration(FollowPlusConfig.GROUP, "dailyXpUsed", Integer.class);
			dailyXp.seed(day, used != null ? used : 0);
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

	/** Whether the person we follow moved tiles since last tick (drives the moving/not-moving tag). */
	private void updateFollowMovement(Player local)
	{
		if (followTargetName == null)
		{
			lastFollowLoc = null;
			followTargetMoving = false;
			return;
		}
		final Player target = findPlayer(followTargetName);
		final WorldPoint loc = target != null ? target.getWorldLocation() : null;
		if (loc != null && lastFollowLoc != null)
		{
			followTargetMoving = !loc.equals(lastFollowLoc);
		}
		lastFollowLoc = loc;
	}

	private void clearFollow()
	{
		followTargetName = null;
		followConfirmed = false;
		followMismatchTicks = 0;
		followTargetMoving = false;
		lastFollowLoc = null;
	}

	// ----- combat -----

	private StatusLine combatLine(Player local)
	{
		final Actor interacting = local.getInteracting();
		if (interacting instanceof NPC && isAvatar(((NPC) interacting).getId()))
		{
			final int hp = enemyAvatarHealth();
			return StatusLine.plain("Fighting the Avatar" + (hp >= 0 ? " (" + hp + "%)" : ""), GREEN);
		}
		return null;
	}

	/** The enemy avatar's health % (the one you attack), read from the HUD. */
	private int enemyAvatarHealth()
	{
		if (team == SoulWarsTeam.RED)
		{
			return blueHealth;
		}
		if (team == SoulWarsTeam.BLUE)
		{
			return redHealth;
		}
		return -1;
	}

	private static boolean isAvatar(int npcId)
	{
		return npcId == AVATAR_BLUE_ID || npcId == AVATAR_RED_ID;
	}

	private Player findPlayer(String name)
	{
		final WorldView wv = client.getTopLevelWorldView();
		if (wv == null)
		{
			return null;
		}
		for (Player p : wv.players())
		{
			if (name.equals(p.getName()))
			{
				return p;
			}
		}
		return null;
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

	// ----- soul wars: zeal + HUD -----

	private void updateZealAndHud()
	{
		// Varps read 0 before they sync; treat 0 as "unknown" so a fresh login doesn't anchor to 0.
		final int rawLifetime = client.getVarpValue(LIFETIME_ZEAL_VARP);
		final int rawTokens = client.getVarpValue(ZEAL_TOKENS_VARP);
		final boolean inGame = soulWarsTeam != 0;
		zeal.update(rawLifetime > 0 ? rawLifetime : -1, rawTokens > 0 ? rawTokens : -1,
			inGame, System.currentTimeMillis());
		runEnergyPct = client.getEnergy() / 100;

		if (inGame)
		{
			blueKills = SoulWarsHud.parseFraction(hudText(SoulWarsHud.BLUE_KILLS));
			redKills = SoulWarsHud.parseFraction(hudText(SoulWarsHud.RED_KILLS));
			blueHealth = SoulWarsHud.parsePercent(hudText(SoulWarsHud.BLUE_HEALTH));
			redHealth = SoulWarsHud.parsePercent(hudText(SoulWarsHud.RED_HEALTH));
			blueStrength = SoulWarsHud.parsePercent(hudText(SoulWarsHud.BLUE_STRENGTH));
			redStrength = SoulWarsHud.parsePercent(hudText(SoulWarsHud.RED_STRENGTH));
		}
		else
		{
			clearHud();
		}
	}

	private String hudText(int child)
	{
		final Widget w = client.getWidget(InterfaceID.SOUL_WARS_GAME, child);
		return w != null ? w.getText() : null;
	}

	private void clearHud()
	{
		blueKills = redKills = -1;
		blueHealth = redHealth = -1;
		blueStrength = redStrength = -1;
	}

	private int countFragments(ItemContainer inv)
	{
		if (inv == null)
		{
			return 0;
		}
		int count = 0;
		for (Item item : inv.getItems())
		{
			if (item.getId() == SOUL_FRAGMENT_ID)
			{
				count += item.getQuantity();
			}
		}
		return count;
	}

	private void updateSoulWars(int tickCount)
	{
		if (soulWarsTeam != 0)
		{
			inLobby = false;
			playersWaitingText = null;
			nextGameText = null;
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
			playersWaitingText = null;
			nextGameText = null;
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
	 * The lobby board (interface 434) holds its labels and values in separate, consecutive widgets
	 * ("Players Waiting" then "5/20" or "28", "Next Game Start" then "-" or "3 minutes" - verified
	 * live), so the texts are collected in reading order and paired by SoulWarsLobbyReader, which
	 * mirrors each value verbatim. Missing values stay "Unknown".
	 */
	private void scanLobbyWidgets()
	{
		final List<String> texts = new ArrayList<>();
		final int visible = forEachWidgetText(InterfaceID.SOUL_WARS_LOBBY, (key, text) -> texts.add(text));
		inLobby = visible > 0;
		final SoulWarsLobbyReader.Result r = SoulWarsLobbyReader.read(texts);
		playersWaitingText = r.playersWaiting;
		nextGameText = r.nextGame;
	}

	private interface TextVisitor
	{
		void visit(int key, String text);
	}

	private static final int SW_WIDGET_SCAN_CHILDREN = 60;

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

	// ----- status model (shared by overlay + window) -----

	private List<StatusLine> buildStatusModel(Player local)
	{
		final List<StatusLine> lines = new ArrayList<>();
		final boolean inGame = soulWarsTeam != 0;
		lines.add(StatusLine.header(inGame ? "In game" : "Out of game", inGame ? GREEN : RED));

		if (config.showTeam() && team != SoulWarsTeam.NONE)
		{
			lines.add(StatusLine.plain(team.label(), team.color()));
		}

		if (config.showFollowingStatus())
		{
			if (followTargetName != null)
			{
				String right = followTargetName;
				if (config.showMovingIndicator())
				{
					right += followTargetMoving ? "  (moving)" : "  (still)";
				}
				lines.add(StatusLine.of("Following", right, followTargetMoving || !config.showMovingIndicator() ? GREEN : AMBER));
			}
			else
			{
				lines.add(StatusLine.plain("Not following anyone", RED));
			}
		}

		if (config.showHpWarning())
		{
			lines.add(StatusLine.of("HP", client.getBoostedSkillLevel(Skill.HITPOINTS)
				+ " / " + client.getRealSkillLevel(Skill.HITPOINTS), null));
			final StatusLine combat = combatLine(local);
			if (combat != null)
			{
				lines.add(combat);
			}
		}

		if (config.showPrayerTimer())
		{
			final int points = client.getBoostedSkillLevel(Skill.PRAYER);
			final int maxPrayer = client.getRealSkillLevel(Skill.PRAYER);
			String prayer = points + " / " + maxPrayer;
			final double seconds = PrayerDrainCalculator.secondsRemaining(points, activeDrainEffect, prayerBonus);
			if (seconds >= 0)
			{
				prayer += " (~" + TimeFormat.mmss(seconds) + ")";
			}
			// colour by prayer remaining: >=50% green, 25-50% yellow, 10-25% red, <10% flashing
			final double pct = maxPrayer > 0 ? points * 100.0 / maxPrayer : 100;
			if (pct < 10)
			{
				lines.add(StatusLine.alert("Prayer", prayer, RED));
			}
			else
			{
				lines.add(StatusLine.of("Prayer", prayer, pct < 25 ? RED : pct < 50 ? AMBER : GREEN));
			}
			for (String name : activePrayerNames)
			{
				lines.add(StatusLine.plain("· " + name, MUTED));
			}
		}

		if (config.showRunEnergy())
		{
			lines.add(StatusLine.of("Run energy", runEnergyPct + "%", runEnergyPct <= 20 ? AMBER : null));
		}

		addZealLines(lines);

		if (inGame)
		{
			addMatchLines(lines);
		}
		else if (config.showLobbyInfo() && inLobby)
		{
			lines.add(StatusLine.of("Players waiting", playersWaitingText != null ? playersWaitingText : "Unknown", null));
			lines.add(StatusLine.of("Next game", nextGameText != null ? nextGameText : "Unknown", null));
		}

		return lines;
	}

	private void addZealLines(List<StatusLine> lines)
	{
		if (config.showCurrentZeal())
		{
			lines.add(zeal.tokens() >= 0
				? StatusLine.of("Zeal tokens", format(zeal.tokens()), null)
				: StatusLine.of("Zeal tokens", "open Nomad to sync", MUTED));
		}
		if (config.showSessionZeal() && zeal.sessionGained() >= 0)
		{
			lines.add(StatusLine.of("Session zeal", "+" + zeal.sessionGained(), null));
		}
		if (config.showLastGameZeal() && zeal.lastGameGained() >= 0)
		{
			lines.add(StatusLine.of("Last game", "+" + zeal.lastGameGained(), null));
		}
		if (config.showLifetimeZeal() && zeal.lifetime() >= 0)
		{
			lines.add(StatusLine.of("Lifetime zeal", format(zeal.lifetime()), null));
		}
		if (config.showSessionTimer())
		{
			lines.add(StatusLine.of("Session", TimeFormat.mmss(zeal.sessionMillis(System.currentTimeMillis()) / 1000.0), null));
		}
		if (config.showZealPerHour())
		{
			final int rate = zeal.zealPerHour(System.currentTimeMillis());
			if (rate >= 0)
			{
				lines.add(StatusLine.of("Zeal/hr", "~" + format(rate), null));
			}
		}
	}

	private void addMatchLines(List<StatusLine> lines)
	{
		if (config.showAvatarKills() && blueKills >= 0 && redKills >= 0)
		{
			lines.add(StatusLine.of("Avatar kills", "B " + blueKills + " / R " + redKills, null));
		}
		if (config.showAvatarHealth() && blueHealth >= 0 && redHealth >= 0)
		{
			lines.add(StatusLine.of("Avatar HP", "B " + blueHealth + "%  R " + redHealth + "%", null));
		}
		if (config.showAvatarStrength() && blueStrength >= 0 && redStrength >= 0)
		{
			lines.add(StatusLine.of("Avatar str", "B " + blueStrength + "%  R " + redStrength + "%", null));
		}
		if (config.showAvatarDamage())
		{
			lines.add(StatusLine.of("Your avatar dmg", Integer.toString(match.avatarDamage()), null));
		}
		if (config.showCaps())
		{
			lines.add(StatusLine.of("Your captures", Integer.toString(match.captures()), null));
		}
		if (config.showFragments())
		{
			lines.add(StatusLine.of("Fragments", Integer.toString(match.fragmentsSacrificed()), null));
		}
		if (config.showBones())
		{
			lines.add(StatusLine.of("Bones buried", Integer.toString(match.bonesBuried()), null));
		}
		if (config.showActivityTimer())
		{
			final int actPct = activityValue >= 0 ? Math.round(activityValue * 100f / MAX_ACTIVITY) : -1;
			lines.add(actPct >= 0 && actPct < 35
				? StatusLine.alert("Activity", actPct + "%", RED)
				: StatusLine.of("Activity", actPct >= 0 ? actPct + "%" : "Unknown", null));
			if (activitySecondsLeft >= 0)
			{
				lines.add(StatusLine.of("Inactive in", "~" + TimeFormat.mmss(activitySecondsLeft),
					activitySecondsLeft < 30 ? RED : null));
			}
		}
		if (config.showGameTimer())
		{
			lines.add(gameSecondsLeft >= 0 && gameSecondsLeft < 300
				? StatusLine.alert("Game time", TimeFormat.mmss(gameSecondsLeft), RED)
				: StatusLine.of("Game time", gameSecondsLeft >= 0 ? TimeFormat.mmss(gameSecondsLeft) : "Unknown", null));
		}
	}

	private static String format(int n)
	{
		return String.format("%,d", n);
	}

	// ----- shared helpers -----

	private void resetAll()
	{
		clearFollow();
		activityEstimator.reset();
		gameTimer.reset();
		match.reset();
		activePrayerNames.clear();
		activeDrainEffect = 0;
		prayerBonusKnown = false;
		soulWarsTeam = 0;
		team = SoulWarsTeam.NONE;
		activityValue = -1;
		activitySecondsLeft = -1;
		gameSecondsLeft = -1;
		inLobby = false;
		playersWaitingText = null;
		nextGameText = null;
		clearHud();
		rewardShopOpen = false;
		statusModel = Collections.emptyList();
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

	// ----- overlay accessor (client thread only) -----

	List<StatusLine> getStatusModel()
	{
		return statusModel;
	}
}
