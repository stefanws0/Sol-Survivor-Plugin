package nl.dexanddice.solsurvivor;

import com.google.gson.Gson;
import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import net.runelite.api.Actor;
import net.runelite.api.ActorSpotAnim;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumID;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.NPC;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.SpriteID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.RenderCallbackManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Filepath;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import nl.dexanddice.solsurvivor.core.ArenaGeometry;
import nl.dexanddice.solsurvivor.core.ArenaState;
import nl.dexanddice.solsurvivor.core.BoardEntry;
import nl.dexanddice.solsurvivor.core.ChestItem;
import nl.dexanddice.solsurvivor.core.ColosseumCounter;
import nl.dexanddice.solsurvivor.core.ColosseumTracker;
import nl.dexanddice.solsurvivor.core.Equipment;
import nl.dexanddice.solsurvivor.core.ExportException;
import nl.dexanddice.solsurvivor.core.GridTile;
import nl.dexanddice.solsurvivor.core.Modifier;
import nl.dexanddice.solsurvivor.core.ModifierLevels;
import nl.dexanddice.solsurvivor.core.NpcHitpoints;
import nl.dexanddice.solsurvivor.core.OrbStyle;
import nl.dexanddice.solsurvivor.core.PickPayload;
import nl.dexanddice.solsurvivor.core.PluginVersion;
import nl.dexanddice.solsurvivor.core.PrayerFlicks;
import nl.dexanddice.solsurvivor.core.PrayerRecord;
import nl.dexanddice.solsurvivor.core.ReplayLink;
import nl.dexanddice.solsurvivor.core.Site;
import nl.dexanddice.solsurvivor.core.RunLogWriter;
import nl.dexanddice.solsurvivor.core.RunUpload;
import nl.dexanddice.solsurvivor.core.SeenNpc;
import nl.dexanddice.solsurvivor.core.StatePayload;
import nl.dexanddice.solsurvivor.core.TileMarker;
import nl.dexanddice.solsurvivor.core.TriangleDot;
import nl.dexanddice.solsurvivor.core.WaveLog;
import nl.dexanddice.solsurvivor.core.WaveLogWriter;
import nl.dexanddice.solsurvivor.core.WaveClock;
import nl.dexanddice.solsurvivor.core.WaveReview;
import nl.dexanddice.solsurvivor.core.WaveRoster;
import nl.dexanddice.solsurvivor.core.WaveShare;
import nl.dexanddice.solsurvivor.core.WaveStore;
import nl.dexanddice.solsurvivor.core.WeaponRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Copies the Fortis Colosseum's state as a Sol Survivor link (design spec 2026-10-14-runelite-plugin-design.md). It reads
 * the game and exports; the link goes to the clipboard from the side panel or a hotkey, and the player pastes it into the
 * site. Nothing it shows says how to play a wave: the plans stay on the site, opened by a press.
 *
 * From 1.0 the panel lists the wave's NPCs and the modifiers held (WaveView, core.WaveRoster), and once a wave ends its
 * report and, after a death, the recap, read from the wave log (core.WaveReview). Overlays, most of them on until the
 * player switches them off, draw facts on the game's screen as other plugins on the Hub do: the wave box (WaveBoxOverlay),
 * as Fortis Colosseum's, and the player's attack range and the NPCs in it (RangeOverlay), as Attack Ranges and Dynamic
 * line of sight. None of them reads a plan, picks a target or marks a tile to stand on. While Minimus offers his three modifiers, the Open button on the panel's Modifier pick opens the
 * site's modifier pick in the browser with them filled in (core.PickPayload); the offers ride in the link's hash, which the browser
 * keeps to itself.
 *
 * This class passes the game's events to ColosseumTracker, which holds the logic and is tested without RuneLite, and
 * reads the arena on the client thread when a link is asked for. With "Record wave log" on it also records each wave it
 * saw start, a tick at a time, in a WaveLog that keeps the waves since the plugin started and that the panel copies as
 * JSON for the site's wave reviewer, any of them picked from a list, or every wave of the newest run in one paste
 * (RunLogWriter). With "Keep runs between sessions" on, as it is by default, each wave's log is saved as it ends in the
 * plugin's own folder of RuneLite's, each account's apart (core.WaveStore), and the account gets them back in Past waves
 * as it logs in, so that a run can still be copied or watched after RuneLite closes.
 *
 * A run's Watch, with "Upload past runs" on (off by default), uploads only the selected run to the site, then opens its
 * recording in the browser. With "Share wave logs" on, off by default too, each wave's log goes to the site as the wave
 * ends, without the levels (core.WaveShare), and with "Leaderboard" on too, the wave then goes on the site's
 * leaderboard under the player's RuneScape name or the Leaderboard name they typed (core.BoardEntry). Nothing else goes to the site. With "Count my flicks" on, off by default
 * too, each wave's protection prayers are counted as it ends (core.PrayerFlicks), and the links carry the totals for the
 * site's Prayer reliability (core.PrayerRecord), in the hash with the rest.
 */
@PluginDescriptor(
	name = "Sol Survivor",
	// The name the Plugin Hub knows it by (its plugins/sol-survivor), which names its folder for the saved runs.
	internalName = "sol-survivor",
	description = "Opens the Fortis Colosseum's arena in Sol Survivor, a planner and solver in your browser for the waves between you and Dizana's quiver. Lists each wave's NPCs, reports your mistakes once a wave ends, and shows what hit you after a death. In the arena it draws a wave box, NPC hitpoints, a tick counter and more, each one you can switch off. Off by default: Upload past runs lets a past run's Watch button upload it to the Sol Survivor site and open it in your browser; Share wave logs sends each wave's log to the site and, with Leaderboard on, puts your waves on its leaderboard.",
	tags = {"fortis", "colosseum", "colo", "varlamore", "sol", "planner", "solver", "quiver", "stack", "wave", "los", "export", "mistakes"}
)
public class SolSurvivorPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SolSurvivorPlugin.class);

	/**
	 * The Sol Survivor site, which every link opens and Watch uploads to. It is not a setting, as the user asked
	 * (2026-10-20): a player's plugin has the one live site, which moved here from sol.dexanddice.nl on 2026-10-25; that
	 * host still answers the releases before this one, and sends their links on here. Only a development run of a
	 * release's holding branch names its preview instead (Site.PROPERTY, set by build.gradle's run task).
	 */
	static final String SITE = Site.of(System.getProperty(Site.PROPERTY));

	/**
	 * The Getting started card's sample wave (the onboarding ideas of 2026-10-27, O1): wave 7 with the plan the site found
	 * for it, the site's own sample link (src/ui/samples.ts, SAMPLE_LINKS), which test/link/pluginSample.test.ts holds it
	 * to. It opens with no Colosseum needed, for a player to look round before they play.
	 */
	static final String SAMPLE_WAVE = SITE + "#s=11Va7bsMwDPwVI0MnoZBlS7b_o5vHIi0MtN075N97R1m2pNBt2qFAlsQ66kWejuThhm1n4AKEMbfBuN4EfOEPg17OYvy_PwrRf4F63xam_9Mr9cOEv3xAN8_ndxSMJ8l5IdxMEw4y3nDzgqq-whNdBZxT5rvcUtBWrtmp66fdACJuEIuii_-QijykTQKbShbWZvtoKYpf5KUyE_Eh4sZ1LoqGVjUMZjrABxUfNdyZGeFX8YFKucJ7GlqkVbhVTA8K2qmTJ16HqHJoNHQ56u169wLtbFwuvYCAUFyq_HEvb5Dc6jO8xyE4BT7jFVQOk-JywZ7ZVYZA4lWZzAojl6zXFA7IZT4GheVwyIdwOh9SJqmmymSfW9sRE5zYHGzIc7hKqssQpQUIhxPisHtgRPfSLYuKYs6ArA7HxuWwmDs71vUcTc2naVIT0zw0bIGkvlfl3JyQa87MkSm3_aFbUfqjLV4puKsveogQQSLZjH0c-xkZI3ev0Rt-ih6eX7EdH11OgRZf3lzp6u7SFVC0tdszVc0fKmZDRXCXyxc";

	/**
	 * The script that fills the modifier selector, with the three on offer and the mask of those taken. RuneLite's
	 * gamevals name no scripts, so its id is kept here.
	 */
	private static final int SCRIPT_MODIFIER_SELECT_INIT = 4931;

	/** The script that draws the selector's offers again, with the outline of the one under the mouse or picked. */
	private static final int SCRIPT_MODIFIER_SELECT_OUTLINE = 4938;
	/** The reward chest's option that banks all its loot. */
	private static final String BANK_ALL = "Bank-all";

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private ItemManager itemManager;

	@Inject
	private SolSurvivorConfig config;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OkHttpClient httpClient;

	/** For the installed Tile Packs plugin, whose built-in packs are read from its own jar (TilePacks). */
	@Inject
	private PluginManager pluginManager;

	/** RuneLite's own, for the site's answers, as its plugin guide asks. */
	@Inject
	private Gson gson;

	/** RuneLite's executor, for work too long for the client thread: Copy whole run's JSON. */
	@Inject
	private ScheduledExecutorService executor;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private WaveBoxOverlay waveBoxOverlay;

	@Inject
	private TickCounterOverlay tickCounterOverlay;

	@Inject
	private WaveTimerOverlay waveTimerOverlay;

	@Inject
	private CombinedWaveTimerOverlay combinedWaveTimerOverlay;

	@Inject
	private RangeOverlay rangeOverlay;

	@Inject
	private DoomOverlay doomOverlay;

	@Inject
	private NpcHitpointsOverlay npcHitpointsOverlay;

	@Inject
	private DoomBoxOverlay doomBoxOverlay;

	@Inject
	private ModifiersBarOverlay modifiersBarOverlay;

	@Inject
	private VolatilityOverlay volatilityOverlay;

	@Inject
	private PillarOverlay pillarOverlay;

	@Inject
	private SpriteManager spriteManager;

	@Inject
	private RenderCallbackManager renderCallbackManager;

	/** Hides the arena's pillars while "Hide the pillars" is on; asked from the scene's build, never the tick. */
	private final PillarHider pillarHider = new PillarHider(() -> config.hidePillars());

	/** Only one upload at a time. Cleared and cancelled on shutdown; late callbacks cannot open the browser. */
	private volatile Call replayCall;

	/** The last shared wave log on its way to the site, cancelled on shutdown. */
	private volatile Call shareCall;
	/** The last request to the leaderboard, a wave going on it or Remove my times, cancelled on shutdown. */
	private volatile Call boardCall;
	/** Which run the waves belong to and which log was shared last, for "Share wave logs"; on the client thread only. */
	private final WaveShare waveShare = new WaveShare();

	private final ColosseumTracker tracker = new ColosseumTracker();
	/** Each NPC's hitpoints, counted from its hitsplats, for NpcHitpointsOverlay. */
	private final NpcHitpoints npcHitpoints = new NpcHitpoints();
	private final WaveClock waveClock = new WaveClock();
	/** The pick's tick is logged beside the wave message's tick, rather than assuming their origins coincide. */
	private int modifierPickTick = -1;
	private boolean inColosseum;
	/**
	 * The payload of the current wave's first tick, taken as it happens, since the player cannot press a key that fast:
	 * the wave's log starts with it, and the wave's line under Past waves links it.
	 */
	private String waveStartState;
	/** The same payload with the gear's levels left out, for the log the player shares (Equipment.withoutLevels). */
	private String waveShareState;
	/** The log of the wave under way, or of the last one until the next starts, and the past ones; on the client thread only. */
	private final WaveLog waveLog = new WaveLog();
	/**
	 * The projectiles in flight that the wave log has been given, by identity, so that each goes in once: the game keeps
	 * the same object for a projectile from the tick it comes to the tick it lands.
	 */
	private final Set<Projectile> seenProjectiles = Collections.newSetFromMap(new IdentityHashMap<>());
	/**
	 * The player's prayer over the waves "Count my flicks" counted, which RuneLite's settings keep between sessions; on the
	 * client thread only.
	 */
	private PrayerRecord prayerRecord = PrayerRecord.empty();
	/** The id of the last wave log whose turn to be counted came, so that each is counted once (countFlicks). */
	private int flicksCounted;

	/**
	 * What the panel's wave card and the wave box show on this tick. Written on the client thread each tick, which the
	 * overlays are drawn on too.
	 */
	private WaveView waveView = WaveView.NONE;
	/** The game tick the wave log under way started on, so that a game tick is a tick of the log. */
	private int logStartTick;
	/** The ticks of the wave log under way on which the player stood on molten sand, for its review (core.WaveReview). */
	private final Set<Integer> sandTicks = new HashSet<>();
	/** The id of the last wave log whose turn to be reviewed came, so that each is reviewed once (reviewLog). */
	private int logReviewed;
	/** The waves' reviews by their logs' ids, for the waves Past waves still lists; on the client thread only. */
	private final Map<Integer, WaveReview> reviews = new HashMap<>();
	/** The log id of the newest review, whose report and recap the panel shows between waves; 0 for none. */
	private int lastReview;
	/**
	 * The runs uploaded this session, by their first and last waves' log ids: the report's and the recap's buttons open one
	 * again at another tick without sending it twice, while the site keeps it (UPLOAD_KEPT_MS). On the client thread.
	 */
	private final Map<String, Uploaded> uploads = new HashMap<>();
	/** How long a run's upload is opened again without sending it anew: a day short of the seven the site keeps it. */
	private static final long UPLOAD_KEPT_MS = 6L * 24 * 60 * 60 * 1000;

	/**
	 * "Keep runs between sessions": the account whose waves the log holds, by its hash, or -1 before one logged in with
	 * the setting on; and its folder for them, under the plugin's own, or null until RuneLite's executor has found it. On
	 * the client thread only.
	 */
	private long runsAccount = -1;
	private Filepath runsFolder;
	/** The id of the last wave log whose turn to be saved came, so that each is saved once (saveLog). */
	private int logSaved;

	private SolSurvivorPanel panel;
	private NavigationButton navButton;
	/**
	 * Whether the sidebar holds the plugin's sun, which "Sidebar icon only at the Colosseum" takes away outside the arena and its
	 * lobby (updateButton). On the client thread, after startUp.
	 */
	private boolean buttonShown;
	/** What the panel last showed, so that it is redrawn only when that changes rather than every tick. */
	private boolean shownInColosseum;
	private String shownTitle;
	private String shownState;
	private String shownNote;
	private List<SolSurvivorPanel.RunGroup> shownRuns = new ArrayList<>();
	private List<SolSurvivorPanel.Offer> shownOffers;
	private boolean shownSharing;
	private String shownBoard;
	private boolean shownBoardOpen;
	private List<Object> shownWave;
	private WaveCards.Report shownReport;
	private WaveCards.Recap shownRecap;
	/** Whether the cards were last shown out of the arena, where the report is a line. */
	private boolean shownOutside;
	/** Whether a wave was under way at the last update, so that the last result is cleared as one starts. */
	private boolean shownDuring;
	/** The first-run cards as last shown: the three sending settings, Getting started's, and whether a wave is under way. */
	private List<Object> shownFirstRun;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.copyHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			copyCurrent(false);
		}
	};

	private final HotkeyListener openArenaHotkeyListener = new HotkeyListener(() -> config.openArenaHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			copyCurrent(true);
		}
	};

	private final HotkeyListener tickResetHotkeyListener = new HotkeyListener(() -> config.tickResetHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() ->
			{
				if (inColosseum && config.tickCounter())
				{
					waveClock.resetCounter(client.getTickCount());
				}
			});
		}
	};

	@Provides
	SolSurvivorConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SolSurvivorConfig.class);
	}

	@Override
	protected void startUp()
	{
		// The protection prayers' icons in the wave report and the death recap are the game's own sprites, read once the
		// game's cache is loaded, rather than a copy of them in the plugin.
		spriteManager.getSpriteAsync(SpriteID.Prayeron._12, 0, image -> Icons.put("prayer-magic", image));
		spriteManager.getSpriteAsync(SpriteID.Prayeron._13, 0, image -> Icons.put("prayer-ranged", image));
		spriteManager.getSpriteAsync(SpriteID.Prayeron._14, 0, image -> Icons.put("prayer-melee", image));
		panel = new SolSurvivorPanel(new SolSurvivorPanel.Actions()
		{
			@Override
			public void openNow()
			{
				copyCurrent(true);
			}

			@Override
			public void openRecommender()
			{
				SolSurvivorPlugin.this.openRecommender();
			}

			@Override
			public void openPastStart(int id)
			{
				openPast(id, false);
			}

			@Override
			public void openPastReinforcements(int id)
			{
				openPast(id, true);
			}

			@Override
			public void copyWaveLog(int id)
			{
				SolSurvivorPlugin.this.copyWaveLog(id);
			}

			@Override
			public void copyRun(int id)
			{
				SolSurvivorPlugin.this.copyRun(id);
			}

			@Override
			public void openPastRun(int id)
			{
				SolSurvivorPlugin.this.openPastRun(id);
			}

			@Override
			public void openLeaderboard()
			{
				LinkBrowser.browse(BoardEntry.page(SITE));
			}

			@Override
			public void removeTimes()
			{
				SolSurvivorPlugin.this.removeTimes();
			}

			@Override
			public void watchAt(int id, Integer tick)
			{
				watch(id, true, tick);
			}

			@Override
			public void copyReport(int id)
			{
				SolSurvivorPlugin.this.copyReport(id);
			}

			@Override
			public void openSample()
			{
				LinkBrowser.browse(SAMPLE_WAVE);
			}

			@Override
			public void hideGettingStarted()
			{
				configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.GETTING_STARTED_DONE, true);
			}

			@Override
			public void setInGame(String key, boolean on)
			{
				// The wave box's setting is a choice of four, which the switch sets to Both or Off.
				Object value = "waveBox".equals(key) ? (on ? SolSurvivorConfig.WaveBox.BOTH : SolSurvivorConfig.WaveBox.OFF) : on;
				configManager.setConfiguration(SolSurvivorConfig.GROUP, key, value);
			}
		});
		// The first-run cards as the settings are now, before the panel first shows: updatePanel keeps them so after.
		panel.showGettingStarted(!config.gettingStartedDone(), config.openOnEntering(), false);
		panel.showSends(config.uploadPastRuns(), config.shareWaveLogs(), config.leaderboard());
		panel.showInGame(inGame());
		navButton = NavigationButton.builder()
			.tooltip("Sol Survivor")
			.icon(SolSurvivorPanel.icon())
			.priority(10)
			.panel(panel)
			.build();
		// With "Sidebar icon only at the Colosseum" on, the first tick in the arena or its lobby puts the sun in the sidebar.
		buttonShown = !config.onlyAtColosseum();
		if (buttonShown)
		{
			clientToolbar.addNavigation(navButton);
		}
		registerHotkeys();
		overlayManager.add(waveBoxOverlay);
		overlayManager.add(tickCounterOverlay);
		overlayManager.add(waveTimerOverlay);
		overlayManager.add(combinedWaveTimerOverlay);
		overlayManager.add(rangeOverlay);
		overlayManager.add(doomOverlay);
		overlayManager.add(doomBoxOverlay);
		overlayManager.add(npcHitpointsOverlay);
		overlayManager.add(modifiersBarOverlay);
		overlayManager.add(volatilityOverlay);
		overlayManager.add(pillarOverlay);
		renderCallbackManager.register(pillarHider);
		if (config.hidePillars())
		{
			clientThread.invokeLater(this::rebuildArena);
		}
		prayerRecord = PrayerRecord.of(config.prayerRecord());
		// Switched on while logged in: the account's saved waves come back now, not at its next login.
		clientThread.invokeLater(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				openRuns();
			}
		});
	}

	@Override
	protected void shutDown()
	{
		Call pending = replayCall;
		replayCall = null;
		if (pending != null)
		{
			pending.cancel();
		}
		Call sharing = shareCall;
		shareCall = null;
		if (sharing != null)
		{
			sharing.cancel();
		}
		Call board = boardCall;
		boardCall = null;
		if (board != null)
		{
			board.cancel();
		}
		unregisterHotkeys();
		overlayManager.remove(waveBoxOverlay);
		overlayManager.remove(tickCounterOverlay);
		overlayManager.remove(waveTimerOverlay);
		overlayManager.remove(combinedWaveTimerOverlay);
		overlayManager.remove(rangeOverlay);
		overlayManager.remove(doomOverlay);
		overlayManager.remove(doomBoxOverlay);
		overlayManager.remove(npcHitpointsOverlay);
		overlayManager.remove(modifiersBarOverlay);
		overlayManager.remove(volatilityOverlay);
		overlayManager.remove(pillarOverlay);
		renderCallbackManager.unregister(pillarHider);
		if (config.hidePillars())
		{
			clientThread.invokeLater(this::rebuildArena);
		}
		clientToolbar.removeNavigation(navButton);
		prayerRecord = PrayerRecord.empty();
		panel = null;
		// shutDown runs on Swing's thread, and the client thread writes the tracker, the wave log and the rest on each
		// tick, so they are cleared there, never under a tick in flight.
		clientThread.invoke(() ->
		{
			tracker.reset();
			waveClock.clear();
			modifierPickTick = -1;
			waveLog.clear();
			seenProjectiles.clear();
			pillarOverlay.clear();
			waveShare.onLeave();
			inColosseum = false;
			waveStartState = null;
			waveShareState = null;
			shownInColosseum = false;
			shownTitle = null;
			shownState = null;
			shownNote = null;
			shownRuns = new ArrayList<>();
			shownOffers = null;
			shownSharing = false;
			shownBoard = null;
			shownBoardOpen = false;
			shownWave = null;
			shownReport = null;
			shownRecap = null;
			shownOutside = false;
			shownDuring = false;
			shownFirstRun = null;
			waveView = WaveView.NONE;
			sandTicks.clear();
			reviews.clear();
			lastReview = 0;
			uploads.clear();
			runsAccount = -1;
			runsFolder = null;
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		// Logged out, hopping or logging in again: the run is over for the plugin, whose next tick may be far off.
		GameState state = e.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.LOGGING_IN || state == GameState.HOPPING)
		{
			leave();
			// No ticks come at the login screen, so the sidebar's sun goes now, with "Sidebar icon only at the Colosseum" on.
			updateButton();
		}
		else if (state == GameState.LOADING)
		{
			// The scene loads again and spawns its objects anew, the molten sand and the pillars among them.
			tracker.onSceneLoading();
			pillarOverlay.clear();
		}
		else if (state == GameState.LOGGED_IN)
		{
			openRuns();
		}
	}

	/** Which of the panel's In the game features are on, in SolSurvivorPanel.IN_GAME's order, as the settings hold them. */
	private boolean[] inGame()
	{
		String[][] features = SolSurvivorPanel.IN_GAME;
		boolean[] on = new boolean[features.length];
		for (int i = 0; i < features.length; i++)
		{
			on[i] = inGame(features[i][0]);
		}
		return on;
	}

	/** Whether In the game's feature `key` is on; the wave box is, whichever wave it shows. */
	private boolean inGame(String key)
	{
		switch (key)
		{
			case "waveBox":
				return config.waveBox() != SolSurvivorConfig.WaveBox.OFF;
			case "modifiersBar":
				return config.modifiersBar();
			case "doomByOrb":
				return config.doomByOrb();
			case "volatilityReminder":
				return config.volatilityReminder();
			case "offerNames":
				return config.offerNames();
			case "npcHitpoints":
				return config.npcHitpoints();
			case "highlightInRange":
				return config.highlightInRange();
			case "myRange":
				return config.myRange();
			case "tickCounter":
				return config.tickCounter();
			case "waveTimer":
				return config.waveTimer();
			case "hidePillars":
				return config.hidePillars();
			case "lootTotals":
				return config.lootTotals();
			case "leftClickBankAll":
				return config.leftClickBankAll();
			default:
				return false;
		}
	}

	/** Whether `key` is one of In the game's features, whose change the panel's switches show. */
	private static boolean isInGame(String key)
	{
		for (String[] feature : SolSurvivorPanel.IN_GAME)
		{
			if (feature[0].equals(key))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * "Record wave log" switched off drops the log at once, and the panel shows the settings as they are, in or out of the
	 * arena: the log's, and whether the waves are shared. "Count my flicks" switched off forgets the waves counted.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!SolSurvivorConfig.GROUP.equals(e.getGroup()))
		{
			return;
		}
		if ("countFlicks".equals(e.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				if (!config.countFlicks())
				{
					prayerRecord = PrayerRecord.empty();
					configManager.unsetConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.PRAYER_RECORD);
				}
			});
			return;
		}
		if (SolSurvivorConfig.LEADERBOARD.equals(e.getKey()) || SolSurvivorConfig.LEADERBOARD_NAME.equals(e.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				boardNameNote();
				updatePanel();
			});
			return;
		}
		if (SolSurvivorConfig.ONLY_AT_COLOSSEUM.equals(e.getKey()))
		{
			clientThread.invokeLater(this::updateButton);
			return;
		}
		if (SolSurvivorConfig.HIDE_PILLARS.equals(e.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				rebuildArena();
				updatePanel();
			});
			return;
		}
		if (SolSurvivorConfig.KEEP_RUNS.equals(e.getKey()))
		{
			clientThread.invokeLater(() ->
			{
				if (config.keepRuns())
				{
					// From now on each wave is saved as it ends: those already in Past waves stay in memory alone.
					openRuns();
					return;
				}
				runsAccount = -1;
				runsFolder = null;
				executor.execute(this::deleteRuns);
			});
			return;
		}
		if ("uploadPastRuns".equals(e.getKey()) || "openOnEntering".equals(e.getKey())
			|| SolSurvivorConfig.GETTING_STARTED_DONE.equals(e.getKey()) || isInGame(e.getKey()))
		{
			clientThread.invokeLater(this::updatePanel);
			return;
		}
		if (!("recordWaveLog".equals(e.getKey()) || "shareWaveLogs".equals(e.getKey())))
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (!config.recordWaveLog())
			{
				waveLog.clear();
				waveLog.discardPending();
				// Switched on again, the account's saved waves come back, as the log was emptied.
				runsAccount = -1;
				runsFolder = null;
			}
			else if (client.getGameState() == GameState.LOGGED_IN)
			{
				openRuns();
			}
			updatePanel();
		});
	}

	@Subscribe
	public void onChatMessage(ChatMessage e)
	{
		if (inColosseum && e.getType() == ChatMessageType.GAMEMESSAGE)
		{
			tracker.onGameMessage(e.getMessage(), client.getTickCount());
			waveClock.onGameMessage(e.getMessage(), System.currentTimeMillis());
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		NPC npc = e.getNpc();
		tracker.onNpcSpawned(npc.getIndex(), npc.getId(), client.getTickCount());
		npcHitpoints.onSpawned(npc.getIndex(), npc.getId());
	}

	/**
	 * An actor started an animation. For an NPC it may be an attack, which times its next one: the event, unlike the
	 * animation read each tick, also marks an attack that repeats the one before. The player's go in the wave log too, so
	 * that each of their attacks is there.
	 */
	@Subscribe
	public void onAnimationChanged(AnimationChanged e)
	{
		Actor actor = e.getActor();
		if (inColosseum && actor instanceof NPC)
		{
			NPC npc = (NPC) actor;
			tracker.onNpcAnimation(npc.getIndex(), npc.getId(), npc.getAnimation(), client.getTickCount());
			// Only for a log being kept: with recording off nothing would use them up (recordWaveLog).
			if (config.recordWaveLog())
			{
				waveLog.onNpcAnimation(npc.getIndex(), npc.getAnimation());
			}
		}
		else if (inColosseum && actor != null && actor == client.getLocalPlayer() && config.recordWaveLog())
		{
			waveLog.onPlayerAnimation(actor.getAnimation());
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned e)
	{
		NPC npc = e.getNpc();
		tracker.onNpcDespawned(npc.getIndex());
		npcHitpoints.onDespawned(npc.getIndex());
		if (inColosseum && npc.getId() == NpcID.COLOSSEUM_MASTER)
		{
			// Minimus leaves once a modifier is picked, which the varbit holds as the option picked, 1 to 3.
			int choice = client.getVarbitValue(VarbitID.COLOSSEUM_SELECTED_MODIFIER);
			tracker.onModifierChosen(choice);
			if (choice >= 1 && choice <= 3)
			{
				modifierPickTick = client.getTickCount();
				Integer wave = tracker.getWave();
				waveClock.onModifierChosen(wave == null ? 1 : wave, modifierPickTick, System.currentTimeMillis(),
					config.tickCounterCounts() == SolSurvivorConfig.TickCounterCounts.UP_FROM_ZERO);
			}
		}
	}

	/**
	 * Once Minimus's screen is drawn, or drawn again, the offers' names on it, with "Names on Minimus's offers" on, and
	 * the loot's totals, with "Loot totals" on.
	 */
	@Subscribe
	public void onScriptPostFired(ScriptPostFired e)
	{
		int script = e.getScriptId();
		if (!inColosseum || (script != SCRIPT_MODIFIER_SELECT_INIT && script != SCRIPT_MODIFIER_SELECT_OUTLINE))
		{
			return;
		}
		if (config.offerNames())
		{
			OfferNames.write(client, tracker.getOptions(), m -> m.hasTiers() ? readLevel(m.getLevelVarbit()) : 0);
		}
		if (config.lootTotals() && script == SCRIPT_MODIFIER_SELECT_INIT)
		{
			LootTotals.write(client);
		}
	}

	/** An NPC that dies in the arena gets Volatility's reminder over it, with "Volatility reminder" on. */
	@Subscribe
	public void onActorDeath(ActorDeath e)
	{
		if (e.getActor() instanceof NPC)
		{
			volatilityOverlay.died((NPC) e.getActor());
		}
	}

	/**
	 * With "Left-click Bank-all" on, the reward chest's Bank-all is its left click in the arena, as RuneLite's Menu Entry
	 * Swapper makes an option the left click: the arena has no other Bank-all.
	 */
	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded e)
	{
		if (config.leftClickBankAll() && inColosseum && BANK_ALL.equals(Text.removeTags(e.getOption())))
		{
			e.getMenuEntry().setForceLeftClick(true);
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired e)
	{
		if (e.getScriptId() != SCRIPT_MODIFIER_SELECT_INIT || e.getScriptEvent() == null)
		{
			return;
		}
		try
		{
			Object[] args = e.getScriptEvent().getArguments();
			tracker.onModifierOptions((Integer) args[2], (Integer) args[3], (Integer) args[4], (Integer) args[8]);
		}
		catch (RuntimeException ex)
		{
			// Never throw into the game's script: a changed script costs the modifiers, not the client.
			log.warn("Could not read the modifier selector's arguments", ex);
		}
	}

	/**
	 * A hitsplat, which RuneLite passes on between ticks. The tracker counts those on the player that dealt damage, Doom's
	 * stacks, and forgets an NPC's health bar when it is healed. With "Record wave log" on, the wave log keeps those on
	 * the player and on NPCs for the next tick it records. Those on the wave's first tick may come before the wave's
	 * message, so they are kept whether or not a wave is being recorded, and dropped on the next tick if none is.
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		if (!inColosseum)
		{
			return;
		}
		Actor actor = e.getActor();
		int amount = e.getHitsplat().getAmount();
		int type = e.getHitsplat().getHitsplatType();
		if (actor != null && actor == client.getLocalPlayer())
		{
			tracker.onPlayerHit(amount, type);
			if (config.recordWaveLog())
			{
				waveLog.onPlayerHit(amount, type);
			}
		}
		else if (actor instanceof NPC)
		{
			int index = ((NPC) actor).getIndex();
			tracker.onNpcHit(index, amount, type);
			npcHitpoints.onHit(index, amount, type, client.getTickCount());
			if (config.recordWaveLog())
			{
				waveLog.onNpcHit(index, amount, type);
			}
		}
	}

	/**
	 * The game put a graphic on a tile, such as where a Javelin Colossus's special lands, which under Reentry lays molten
	 * sand that the tracker keeps for the link. With "Record wave log" on, the wave log keeps those on the grid for the
	 * next tick it records, as it does the hitsplats.
	 */
	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated e)
	{
		if (!inColosseum)
		{
			return;
		}
		GraphicsObject graphic = e.getGraphicsObject();
		GridTile tile = gridTileOf(graphic.getLocation());
		tracker.onTileGraphic(tile, graphic.getId(), readLevel(Modifier.REENTRY.getLevelVarbit()));
		if (config.recordWaveLog())
		{
			waveLog.onTileGraphic(tile, graphic.getId());
		}
	}

	/*
	 * The Molten Sand objects, which the game may place as scenery or on the floor, so both kinds of event are read. They
	 * spawn as the scene loads, before the player's first tick in the arena, so they are taken whether or not the plugin
	 * knows the player is in the Colosseum yet: a tile outside the arena has no grid tile and is passed over. The pillars'
	 * objects go to PillarOverlay the same way, for the marks it leaves on the floor while they are hidden.
	 */

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned e)
	{
		sandObject(e.getGameObject(), true);
		pillarOverlay.spawned(e.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned e)
	{
		sandObject(e.getGameObject(), false);
		pillarOverlay.despawned(e.getGameObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned e)
	{
		sandObject(e.getGroundObject(), true);
	}

	@Subscribe
	public void onGroundObjectDespawned(GroundObjectDespawned e)
	{
		sandObject(e.getGroundObject(), false);
	}

	/** An object came or went: a Molten Sand one goes to the tracker by its grid tile. */
	private void sandObject(TileObject object, boolean spawned)
	{
		if (object != null && object.getId() == ColosseumTracker.MOLTEN_SAND_ID)
		{
			tracker.onSandObject(gridTileOf(object.getLocalLocation()), spawned);
		}
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		boolean now = isInColosseum();
		boolean entered = !inColosseum && now;
		if (inColosseum && !now)
		{
			leave();
		}
		inColosseum = now;
		// The sun first, so that the panel opening on entering has its button in the sidebar.
		updateButton();
		if (entered)
		{
			openOnEntering();
			sayOnFirstEntering();
		}
		if (!inColosseum)
		{
			return;
		}

		// Every tick, rather than on graphic events, which do not fire for a Manticore out of view behind a pillar. Each
		// NPC's health bar too, while the game shows it, which the tracker keeps for the link once it hides and which keeps
		// the NPC hitpoints' count in the bar's range.
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			tracker.onHealthBar(npc.getIndex(), npc.getHealthRatio(), npc.getHealthScale());
			npcHitpoints.onTick(npc.getIndex(), npc.getId(), npc.getHealthRatio(), npc.getHealthScale());
			if (npc.getId() != NpcID.COLOSSEUM_MANTICORE)
			{
				continue;
			}
			for (ActorSpotAnim anim : npc.getSpotAnims())
			{
				OrbStyle style = orbStyleOf(anim.getId());
				if (style != null)
				{
					tracker.onOrb(npc.getIndex(), style);
				}
			}
		}

		int tick = client.getTickCount();
		waveClock.onTick(tick);
		if (tracker.isWaveStartTick(tick))
		{
			Integer wave = tracker.getWave();
			if (wave != null)
			{
				waveClock.onWaveStarted(wave, System.currentTimeMillis());
				if (modifierPickTick >= 0 && (config.tickCounter() || config.waveTimer()))
				{
					log.debug("Wave {}: modifier confirmation tick {}, wave message tick {}, difference {} ticks.",
						wave, modifierPickTick, tick, tick - modifierPickTick);
				}
				modifierPickTick = -1;
			}
			try
			{
				ArenaState start = capture();
				Equipment gear = gear();
				waveStartState = StatePayload.toJson(start, gear);
				waveShareState = gear == null ? waveStartState : StatePayload.toJson(start, gear.withoutLevels());
			}
			catch (ExportException ex)
			{
				waveStartState = null;
				waveShareState = null;
			}
		}
		else if (!tracker.isWaveStarted())
		{
			waveStartState = null;
			waveShareState = null;
		}
		recordWaveLog(tick);
		sandTick(tick);
		// The run's waves finished, for the pick link, with the game's glory and wave damage counters beside them.
		tracker.onRunTick(m -> readLevel(m.getLevelVarbit()), client.getVarpValue(ColosseumCounter.CURRENT_GLORY.getId()),
			client.getVarpValue(ColosseumCounter.WAVE_DAMAGE_TAKEN.getId()));
		waveView = waveView(tick);
		updateRange();
		updatePanel();
	}

	/** Whether the player is in the arena, for the overlays, which draw only there. On the client thread. */
	boolean inArena()
	{
		return inColosseum;
	}

	/** Each NPC's hitpoints, for NpcHitpointsOverlay. On the client thread. */
	NpcHitpoints npcHitpoints()
	{
		return npcHitpoints;
	}

	/** The clocks' immutable reading, with their own origin at the modifier confirmation. */
	WaveClock.View waveClock()
	{
		return waveClock.view();
	}

	/** What the wave box shows on this tick (WaveView). On the client thread. */
	WaveView waveView()
	{
		return waveView;
	}

	/**
	 * The wave under way and its NPCs left, or the next wave and what it brings, with the modifiers held and Doom's stacks
	 * by the game's own count against its limit (WaveRoster): only what the game has spawned or will, no timers.
	 */
	private WaveView waveView(int tick)
	{
		Integer wave = tracker.getWave();
		if (wave == null)
		{
			return WaveView.NONE;
		}
		ModifierLevels levels = m -> readLevel(m.getLevelVarbit());
		Map<Modifier, Integer> held = tracker.modifiers(levels);
		boolean underWay = tracker.isWaveStarted();
		List<WaveRoster.Line> thisWave = underWay ? tracker.roster(tick, seenNpcs(), levels, config.listWarband())
			: Collections.<WaveRoster.Line>emptyList();
		// Between waves the tracker's wave is the next one.
		Integer next = underWay ? Integer.valueOf(wave + 1) : wave;
		boolean known = WaveRoster.knows(next);
		int doom = held.getOrDefault(Modifier.DOOM, 0);
		return new WaveView(wave, underWay, thisWave, known ? next : null,
			known ? WaveRoster.nextWave(next, held) : Collections.<WaveRoster.Line>emptyList(), WaveRoster.nextWaveLater(known ? next : null),
			known ? WaveRoster.nextWaveThen(next, held) : null,
			held, readLevel(ColosseumCounter.DOOM_STACKS.getId()), WaveRoster.doomLimit(doom));
	}

	/**
	 * The player's attack range on this tick for the range overlay, with either of its settings on: the worn weapon's by
	 * its name and the combat option set (core.WeaponRange), cut by the Myopia held.
	 */
	private void updateRange()
	{
		if (!config.myRange() && !config.highlightInRange())
		{
			rangeOverlay.update(0, false);
			return;
		}
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item weapon = worn == null ? null : worn.getItem(EquipmentInventorySlot.WEAPON.getSlotIdx());
		String name = weapon == null || weapon.getId() <= 0 ? null : itemManager.getItemComposition(weapon.getId()).getName();
		int category = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
		int range = WeaponRange.of(name, styleNamesOf(category), client.getVarpValue(VarPlayerID.COM_MODE));
		int myopia = waveView.held.getOrDefault(Modifier.MYOPIA, 0);
		rangeOverlay.update(WeaponRange.underMyopia(range, myopia), WeaponRange.cardinalOnly(range));
	}

	/** The wave log under way: whether the player stands on molten sand on this tick, as the tracker knows it. */
	private void sandTick(int tick)
	{
		if (!waveLog.isRecording())
		{
			return;
		}
		Player player = client.getLocalPlayer();
		if (player != null && tracker.isSand(gridTileOf(player.getWorldLocation())))
		{
			sandTicks.add(tick - logStartTick);
		}
	}

	/**
	 * This tick in the wave log: it starts on the first tick of a wave the tracker saw start, records each tick after,
	 * the tick of the game's "Wave N completed!" included, and then stops and keeps the log until the next wave.
	 */
	private void recordWaveLog(int tick)
	{
		if (!config.recordWaveLog())
		{
			// Switched off: no log is kept, not even a half-recorded one.
			if (waveLog.hasLog())
			{
				waveLog.clear();
			}
			waveLog.discardPending();
			return;
		}
		if (tracker.isWaveStartTick(tick) && tracker.getWave() != null)
		{
			// The last wave's log, when its end was never seen, has its turn before the next wave's starts.
			shareLog();
			saveLog();
			countFlicks();
			reviewLog();
			logStartTick = tick;
			sandTicks.clear();
			waveShare.onWaveStart(tracker.getWave());
			waveLog.start(tracker.getWave(), tick, tracker.modifiers(m -> readLevel(m.getLevelVarbit())), tracker.isModifiersComplete(),
				PluginVersion.VERSION, waveStartState, waveShareState);
		}
		if (!waveLog.isRecording())
		{
			waveLog.discardPending();
			seenProjectiles.clear();
			return;
		}
		projectiles();
		waveLog.record(tick, playerTick(), npcTicks(), counters());
		if (tracker.isReinforcementsArrived() && !waveLog.hasReinforcementState())
		{
			// The tick the reinforcements came, which the panel opens on the site for a wave gone by.
			try
			{
				waveLog.reinforcementsCame(StatePayload.toJson(capture(), gear()));
			}
			catch (ExportException ex)
			{
				log.debug("Could not take the arena as the reinforcements came", ex);
			}
		}
		if (!tracker.isWaveStarted())
		{
			waveLog.finish(WaveLog.End.COMPLETED);
			shareLog();
			saveLog();
			countFlicks();
			reviewLog();
		}
	}

	/**
	 * The wave log under way or the last one has its turn to be reviewed, as its wave ends: with "Track mistakes" or
	 * "Death recap" on, the wave report and the recap read it (core.WaveReview), and the panel shows them from then until
	 * the next wave starts. Each log has one turn, as shareLog's. On the client thread, which the log is read on: a wave
	 * is read once, in one pass over its ticks.
	 */
	private void reviewLog()
	{
		if (!waveLog.hasLog() || waveLog.size() == 0 || waveLog.getId() == logReviewed)
		{
			return;
		}
		logReviewed = waveLog.getId();
		if (!config.trackMistakes() && !config.deathRecap())
		{
			return;
		}
		WaveReview review;
		try
		{
			review = WaveReview.of(waveLog, sandTicks, this::attackSpeed);
		}
		catch (RuntimeException ex)
		{
			// A log the review cannot read costs its report, never the wave's log.
			log.warn("Could not review the log of wave {}", waveLog.getWave(), ex);
			return;
		}
		reviews.put(logReviewed, review);
		lastReview = logReviewed;
		// Only the waves Past waves still lists keep their reviews.
		Set<Integer> kept = new HashSet<>();
		for (List<WaveLog.Past> run : waveLog.getRuns())
		{
			for (WaveLog.Past wave : run)
			{
				kept.add(wave.getId());
			}
		}
		reviews.keySet().retainAll(kept);
		if (config.trackMistakes() && config.reportInChat())
		{
			WaveCards.Report report = reportOf(logReviewed);
			if (report != null)
			{
				chatMessageManager.queue(QueuedMessage.builder()
					.type(ChatMessageType.GAMEMESSAGE)
					.runeLiteFormattedMessage("[Sol Survivor] " + chatLine(report, review))
					.build());
			}
		}
	}

	/** The report in one line for the chat box: its mistakes, the damage taken and the prayer against the attacks. */
	private static String chatLine(WaveCards.Report r, WaveReview review)
	{
		String mistakes = r.mistakes.isEmpty() ? "no mistakes counted" : WaveCards.plural(r.mistakes.size(), "mistake");
		String prayer = r.attacks() == 0 ? "" : ", your prayer on time against " + r.onTime + " of " + WaveCards.plural(r.attacks(), "attack");
		return "Wave " + r.wave + ": " + mistakes + ", " + review.getDamage() + " damage taken" + prayer + ".";
	}

	/** A weapon's attack speed in ticks by its item id, from RuneLite's item data; 0 when it is not known. */
	private int attackSpeed(int itemId)
	{
		ItemStats stats = itemManager.getItemStats(itemId);
		return stats == null || stats.getEquipment() == null ? 0 : stats.getEquipment().getAspeed();
	}

	/** The mistake kinds the settings count, each its own switch under Mistake tracker. */
	private Set<WaveReview.Kind> countedKinds()
	{
		Set<WaveReview.Kind> kinds = EnumSet.noneOf(WaveReview.Kind.class);
		if (config.countPastPrayer())
		{
			kinds.add(WaveReview.Kind.PAST_PRAYER);
		}
		if (config.countLateFlicks())
		{
			kinds.add(WaveReview.Kind.LATE_FLICK);
		}
		if (config.countSpecials())
		{
			kinds.add(WaveReview.Kind.SPECIAL);
		}
		if (config.countVolatility())
		{
			kinds.add(WaveReview.Kind.VOLATILITY);
		}
		if (config.countSolarflareSand())
		{
			kinds.add(WaveReview.Kind.SOLARFLARE_SAND);
		}
		if (config.countIdle())
		{
			kinds.add(WaveReview.Kind.IDLE);
		}
		return kinds;
	}

	/**
	 * The report of the logged wave `id` as the panel shows it, the settings applied: the mistakes counted, the kinds left
	 * out, and the run's waves up to this one with their mistakes. Null when the wave has no review or is no longer kept.
	 */
	private WaveCards.Report reportOf(int id)
	{
		WaveReview r = reviews.get(id);
		WaveLog.Past wave = loggedWave(id);
		List<WaveLog.Past> run = loggedRun(id);
		if (r == null || wave == null || run == null)
		{
			return null;
		}
		List<Integer> waves = new ArrayList<>();
		List<WaveReview> runReviews = new ArrayList<>();
		for (WaveLog.Past w : run)
		{
			WaveReview each = reviews.get(w.getId());
			if (each != null)
			{
				waves.add(w.getWave());
				runReviews.add(each);
			}
			if (w.getId() == id)
			{
				break;
			}
		}
		return WaveCards.Report.of(id, wave.getWave(), duration(wave.size()), r, countedKinds(), config.idleFrom(), waves, runReviews);
	}

	/** The death recap of the logged wave `id`, or null when the player did not die in it or it is no longer kept. */
	private WaveCards.Recap recapOf(int id)
	{
		WaveReview r = reviews.get(id);
		WaveLog.Past wave = loggedWave(id);
		if (r == null || wave == null || r.getDied() == null)
		{
			return null;
		}
		return new WaveCards.Recap(id, wave.getWave(), r.getDied(), duration(r.getDied()), r.getCause(), r.getRecap(), r.getMaxHitpoints());
	}

	/** Copies the report of the logged wave `id` as text, for a clan's chat or the site's Discord. */
	private void copyReport(int id)
	{
		clientThread.invokeLater(() ->
		{
			WaveCards.Report report = reportOf(id);
			if (report == null)
			{
				report("That wave's report is no longer kept: more waves have come since than the log keeps.");
				return;
			}
			copy(WaveCards.text(report), "Copied the report of wave " + report.wave + ".", null);
		});
	}

	/**
	 * The wave log under way or the last one has its turn to be counted, as its wave ends: with "Count my flicks" on, its
	 * protection prayers against the attacks go into the player's record (core.PrayerFlicks), which RuneLite's settings
	 * keep for the next session. Each log has one turn, so that a wave that ended with the setting off is never counted.
	 */
	private void countFlicks()
	{
		if (!waveLog.hasLog() || waveLog.size() == 0 || waveLog.getId() == flicksCounted)
		{
			return;
		}
		flicksCounted = waveLog.getId();
		if (!config.countFlicks())
		{
			return;
		}
		PrayerRecord next = prayerRecord.with(PrayerFlicks.count(waveLog));
		if (next != prayerRecord)
		{
			prayerRecord = next;
			configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.PRAYER_RECORD, next.toText());
		}
	}

	/**
	 * The wave log under way or the last one has its turn to be shared, as its wave ends: with "Share wave logs" on it goes
	 * to the site as WaveLogWriter.toShare writes it, gzipped, under the run's id (WaveShare). Each log has one turn, so
	 * that a wave that ended with the setting off is never sent. On the client thread, which the log is read on, as far as
	 * a snapshot of it: its JSON, the gzip and the upload go on OkHttp's threads, and a log the site refuses or cannot
	 * reach is dropped without a word to the player.
	 */
	private void shareLog()
	{
		if (!waveLog.hasLog() || waveLog.size() == 0 || !waveShare.claim(waveLog.getId()))
		{
			return;
		}
		String run = waveShare.getRun();
		if (!config.shareWaveLogs() || run == null)
		{
			return;
		}
		WaveLog done = waveLog.snapshot();
		// With Leaderboard on, the wave goes on the boards once the site has its log, which the boards read it from.
		String name = boardName();
		String entry = name == null || done.getWave() == null ? null : BoardEntry.body(run, done.getWave(), name, boardKey());
		Request request = new Request.Builder().url(WaveShare.endpoint(SITE)).header(WaveShare.RUN_HEADER, run)
			.post(new LazyBody(WaveShare.TYPE, () -> WaveShare.gzip(WaveLogWriter.toShare(done)))).build();
		Call call = httpClient.newCall(request);
		shareCall = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call failed, IOException ex)
			{
				log.debug("Could not share the wave log", ex);
			}

			@Override
			public void onResponse(Call completed, Response response)
			{
				boolean taken = response.isSuccessful();
				if (!taken)
				{
					log.debug("The site did not take the shared wave log: {}", response.code());
				}
				response.close();
				if (taken && entry != null)
				{
					enterBoard(entry, name);
				}
			}
		});
	}

	/**
	 * The player's key on the leaderboard (core.BoardEntry), made and kept in the settings the first time a wave goes up,
	 * so that the name and the times stay the player's. On the client thread.
	 */
	private String boardKey()
	{
		String key = config.leaderboardKey();
		if (!BoardEntry.isKey(key))
		{
			key = BoardEntry.newKey(new SecureRandom());
			configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.LEADERBOARD_KEY, key);
		}
		return key;
	}

	/**
	 * Asks for a wave whose log the site has taken to go on the leaderboard, with its body (BoardEntry.body), `name` being
	 * the name in it, and tells the player what became of it: its time and place, or why it is not on. A request the site
	 * refuses or cannot reach is dropped without a word, as a shared log is.
	 */
	private void enterBoard(String entry, String name)
	{
		Call call = httpClient.newCall(BoardUpload.enter(SITE, entry));
		boardCall = call;
		call.enqueue(new Callback()
		{
			@Override
			public void onFailure(Call failed, IOException ex)
			{
				log.debug("Could not put the wave on the leaderboard", ex);
			}

			@Override
			public void onResponse(Call completed, Response response)
			{
				String said;
				try (Response closed = response)
				{
					said = BoardUpload.entered(gson, closed, name);
				}
				if (said != null)
				{
					clientThread.invokeLater(() -> report(said));
				}
			}
		});
	}

	/**
	 * The wave log under way or the last one has its turn to be saved, as its wave ends: with "Keep runs between sessions"
	 * on it goes to the account's folder (core.WaveStore), and the account's waves past the newest WaveStore.MAX_SAVED are
	 * deleted. Each log has one turn, as shareLog's. On the client thread as far as a snapshot of the log: its JSON, the
	 * gzip and the files go on RuneLite's executor, as RuneLite's plugin guide keeps disk work off the client thread. A
	 * wave that cannot be saved stays in Past waves until RuneLite closes, as before the setting.
	 */
	private void saveLog()
	{
		if (!waveLog.hasLog() || waveLog.size() == 0 || waveLog.getId() == logSaved)
		{
			return;
		}
		logSaved = waveLog.getId();
		Filepath folder = runsFolder;
		if (folder == null || !config.keepRuns())
		{
			return;
		}
		WaveLog.Past wave = waveLog.current();
		long savedAt = System.currentTimeMillis();
		executor.execute(() ->
		{
			try
			{
				folder.createDirectories();
				try (OutputStream out = folder.joinSegment(WaveStore.fileName(savedAt, wave.getWave())).openOutputStream())
				{
					WaveStore.write(wave, out);
				}
				for (String old : WaveStore.beyond(fileNames(folder), WaveStore.MAX_SAVED))
				{
					folder.joinSegment(old).deleteIfExists();
				}
			}
			catch (IOException | RuntimeException ex)
			{
				log.warn("Could not save the log of wave {}", wave.getWave(), ex);
			}
		});
	}

	/**
	 * The name the player's waves go up under: the Leaderboard name they typed, or else their RuneScape name (the user's
	 * choice, 2026-10-26); null with Leaderboard off, before the player is logged in, or for a name the site would refuse.
	 * On the client thread.
	 */
	private String boardName()
	{
		if (!config.leaderboard())
		{
			return null;
		}
		String typed = config.leaderboardName();
		if (!BoardEntry.isBlank(typed))
		{
			return BoardEntry.name(typed);
		}
		Player player = client.getLocalPlayer();
		return player == null ? null : BoardEntry.fromPlayer(player.getName());
	}

	/** Leaderboard switched on, or a name typed, that the site would refuse or that needs Share wave logs is said so at once. */
	private void boardNameNote()
	{
		if (!config.leaderboard())
		{
			return;
		}
		String typed = config.leaderboardName();
		if (!BoardEntry.isBlank(typed) && BoardEntry.name(typed) == null)
		{
			report("The Leaderboard name must be 1 to 12 letters, digits, spaces, hyphens or underscores, beginning and ending with a letter or digit.");
		}
		else if (!config.shareWaveLogs() || !config.recordWaveLog())
		{
			report("The leaderboard needs Share wave logs and Record wave log on too: your waves go on it from their logs.");
		}
	}

	/**
	 * Remove my times, once the player confirmed it on the panel: takes every time of theirs off the leaderboard, with
	 * their name, and switches Leaderboard off so that no more go up.
	 */
	private void removeTimes()
	{
		clientThread.invokeLater(() ->
		{
			String key = config.leaderboardKey();
			if (!BoardEntry.isKey(key))
			{
				configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.LEADERBOARD, false);
				report("You have no times on the leaderboard.");
				return;
			}
			Call call = httpClient.newCall(BoardUpload.remove(SITE, key));
			boardCall = call;
			report("Removing your times from the leaderboard…");
			call.enqueue(new Callback()
			{
				@Override
				public void onFailure(Call failed, IOException ex)
				{
					clientThread.invokeLater(() -> report("The site could not be reached, so your times are still on the leaderboard. Try again."));
				}

				@Override
				public void onResponse(Call completed, Response response)
				{
					String said;
					try (Response closed = response)
					{
						int waves = BoardUpload.removed(gson, closed);
						said = (waves == 1 ? "Your one time is" : waves == 0 ? "You had no times, and nothing is" : "Your " + waves + " times are")
							+ " off the leaderboard, with your name, and Leaderboard is switched off in the settings.";
						clientThread.invokeLater(() -> configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.LEADERBOARD, false));
					}
					catch (IOException ex)
					{
						said = ex.getMessage();
					}
					String message = said;
					clientThread.invokeLater(() -> report(message));
				}
			});
		});
	}

	/**
	 * With "Keep runs between sessions" and "Record wave log" on, the account logged in gets its saved waves back in
	 * Past waves, behind any of this session's (WaveLog.restore). Another account's waves in the log are let go first, so
	 * that each account sees its own. The folder is found and the files read on RuneLite's executor; a file that cannot
	 * be read is passed over. On the client thread.
	 */
	private void openRuns()
	{
		long account = client.getAccountHash();
		if (account == -1 || account == runsAccount || !config.keepRuns() || !config.recordWaveLog())
		{
			return;
		}
		if (runsAccount != -1)
		{
			waveLog.clear();
		}
		runsAccount = account;
		runsFolder = null;
		executor.execute(() ->
		{
			Filepath folder = null;
			List<WaveLog.Past> saved = new ArrayList<>();
			try
			{
				folder = getPluginDirectory().joinSegment(Long.toString(account));
				for (String name : WaveStore.newestFirst(fileNames(folder)))
				{
					if (saved.size() >= WaveLog.MAX_PAST)
					{
						break;
					}
					try (InputStream in = folder.joinSegment(name).openInputStream())
					{
						saved.add(WaveStore.read(in));
					}
					catch (IOException ex)
					{
						log.debug("Could not read the saved wave {}", name, ex);
					}
				}
			}
			catch (IOException | RuntimeException ex)
			{
				log.warn("Could not read the saved runs", ex);
			}
			Filepath found = folder;
			clientThread.invokeLater(() ->
			{
				// Another account may have logged in, or the setting gone off, while the files were read.
				if (runsAccount != account)
				{
					return;
				}
				runsFolder = found;
				waveLog.restore(saved);
				if (!saved.isEmpty())
				{
					markFirstRunDone();
				}
				updatePanel();
			});
		});
	}

	/** "Keep runs between sessions" switched off: every account's saved waves are deleted. On RuneLite's executor. */
	private void deleteRuns()
	{
		try
		{
			Filepath root = getPluginDirectory();
			List<Filepath> waves = new ArrayList<>();
			try (Stream<Filepath> files = root.walk(2))
			{
				files.filter(f -> f.isFile() && f.getFileName().endsWith(WaveStore.SUFFIX)).forEach(waves::add);
			}
			for (Filepath wave : waves)
			{
				wave.deleteIfExists();
			}
		}
		catch (IOException | RuntimeException ex)
		{
			log.warn("Could not delete the saved runs", ex);
		}
	}

	/** The names of the files in `folder`, none when it is not there yet. */
	private static List<String> fileNames(Filepath folder) throws IOException
	{
		List<String> names = new ArrayList<>();
		if (!folder.isDirectory())
		{
			return names;
		}
		try (Stream<Filepath> files = folder.walk(1))
		{
			files.filter(Filepath::isFile).forEach(f -> names.add(f.getFileName()));
		}
		return names;
	}

	/**
	 * The projectiles the game shows that the wave log has not been given yet go to it, each once, with its ends and the
	 * client cycles from now to when it sets off and lands: a projectile that came after the last tick's look is taken on
	 * this one, a little after it set off. A projectile the game cannot describe costs the log its projectiles for the
	 * tick, not the tick. On the client thread only.
	 */
	private void projectiles()
	{
		try
		{
			int cycle = client.getGameCycle();
			Set<Projectile> live = Collections.newSetFromMap(new IdentityHashMap<>());
			for (Projectile p : client.getProjectiles())
			{
				live.add(p);
				if (seenProjectiles.contains(p))
				{
					continue;
				}
				Actor from = p.getSourceActor();
				Actor to = p.getTargetActor();
				waveLog.onProjectile(new WaveLog.Projectile(p.getId(), targetOf(from), endTile(from, p.getSourcePoint()), targetOf(to),
					endTile(to, p.getTargetPoint()), p.getStartCycle() - cycle, p.getEndCycle() - cycle));
			}
			seenProjectiles.clear();
			seenProjectiles.addAll(live);
		}
		catch (RuntimeException ex)
		{
			log.debug("Could not read the projectiles", ex);
		}
	}

	/** A projectile's end on the grid when it has no actor there, or null: one with an actor is logged by the actor. */
	private GridTile endTile(Actor actor, WorldPoint point)
	{
		return actor != null || point == null ? null : gridTileOf(point);
	}

	/** The game's Colosseum counters on this tick, one for each ColosseumCounter in its order, for the wave log. */
	private int[] counters()
	{
		ColosseumCounter[] all = ColosseumCounter.values();
		int[] values = new int[all.length];
		for (int i = 0; i < all.length; i++)
		{
			values[i] = all[i].isVarbit() ? readLevel(all[i].getId()) : client.getVarpValue(all[i].getId());
		}
		return values;
	}

	/** The player on this tick, for the wave log, with their gear when "Include gear and stats" is on; on the client thread only. */
	private WaveLog.PlayerTick playerTick()
	{
		Player player = client.getLocalPlayer();
		GridTile tile = player == null ? null : gridTileOf(player.getWorldLocation());
		if (tile != null && !ArenaGeometry.onGrid(tile))
		{
			// Off the grid the log has no tile for the player (WaveLogWriter).
			tile = null;
		}
		int animation = player == null ? -1 : player.getAnimation();
		int target = player == null ? WaveLog.TARGET_NONE : targetOf(player.getInteracting());
		return new WaveLog.PlayerTick(tile, client.getBoostedSkillLevel(Skill.HITPOINTS), client.getBoostedSkillLevel(Skill.PRAYER),
			activePrayers(), animation, target, config.includeGear() ? wornIds() : null);
	}

	/** The item id in each slot the link names, in WaveLog.WORN_SLOTS' order (SLOTS), -1 for an empty one. */
	private int[] wornIds()
	{
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item[] items = worn == null ? new Item[0] : worn.getItems();
		int[] ids = new int[SLOTS.length];
		for (int i = 0; i < SLOTS.length; i++)
		{
			int slot = SLOTS[i].getSlotIdx();
			Item item = slot < items.length ? items[slot] : null;
			ids[i] = item == null || item.getId() <= 0 ? -1 : item.getId();
		}
		return ids;
	}

	/**
	 * Every NPC the game shows on this tick, dead ones included, with the graphics it shows, for the wave log, which keeps
	 * those on the grid.
	 */
	private List<WaveLog.NpcTick> npcTicks()
	{
		List<WaveLog.NpcTick> out = new ArrayList<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			GridTile tile = gridTileOf(npc.getWorldLocation());
			if (tile == null)
			{
				continue;
			}
			List<Integer> spots = new ArrayList<>();
			for (ActorSpotAnim anim : npc.getSpotAnims())
			{
				spots.add(anim.getId());
			}
			int[] spotAnims = new int[spots.size()];
			for (int i = 0; i < spotAnims.length; i++)
			{
				spotAnims[i] = spots.get(i);
			}
			out.add(new WaveLog.NpcTick(npc.getIndex(), npc.getId(), tile, npc.getAnimation(), npc.getHealthRatio(), npc.getHealthScale(),
				targetOf(npc.getInteracting()), spotAnims));
		}
		return out;
	}

	/** An actor's target in the wave log: an NPC by its index, any player as the player, since the Colosseum is solo. */
	private static int targetOf(Actor target)
	{
		if (target instanceof NPC)
		{
			return ((NPC) target).getIndex();
		}
		if (target instanceof Player)
		{
			return WaveLog.TARGET_PLAYER;
		}
		return WaveLog.TARGET_NONE;
	}

	/**
	 * The prayers on, by their names in the wave log. Eagle Eye and Deadeye, Mystic Might and Mystic Vigour, and Piety and
	 * Zeal share a slot in the prayer book, which Client.isPrayerActive does not tell apart; RuneLite's own prayer plugin
	 * settles which one is meant by whether the newer prayer is unlocked, and so does this.
	 */
	@SuppressWarnings("deprecation")
	private List<String> activePrayers()
	{
		boolean deadeye = readLevel(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
		boolean vigour = readLevel(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
		boolean zeal = readLevel(VarbitID.PRAYER_ZEAL_UNLOCKED) != 0;
		List<String> out = new ArrayList<>();
		for (Prayer prayer : Prayer.values())
		{
			if ((prayer == Prayer.EAGLE_EYE && deadeye) || (prayer == Prayer.DEADEYE && !deadeye)
				|| (prayer == Prayer.MYSTIC_MIGHT && vigour) || (prayer == Prayer.MYSTIC_VIGOUR && !vigour)
				|| (prayer == Prayer.PIETY && zeal) || (prayer == Prayer.ZEAL && !zeal))
			{
				continue;
			}
			if (client.isPrayerActive(prayer))
			{
				out.add(WaveLog.prayerName(prayer.name()));
			}
		}
		return out;
	}

	private void leave()
	{
		// The log of a wave the player died in or ran from is kept, so that it can be copied after, and shared as it ends.
		waveLog.finish(WaveLog.End.LEFT);
		shareLog();
		saveLog();
		countFlicks();
		reviewLog();
		waveView = WaveView.NONE;
		seenProjectiles.clear();
		volatilityOverlay.clear();
		waveShare.onLeave();
		tracker.reset();
		npcHitpoints.clear();
		waveClock.clear();
		modifierPickTick = -1;
		inColosseum = false;
		waveStartState = null;
		waveShareState = null;
		updatePanel();
	}

	/**
	 * With "Open panel on entering" on, opens the side panel as the player comes into the arena, and RuneLite's sidebar
	 * with it, as Hunllef Helper opens its own on entering the Gauntlet. It opens once each time the player comes in, at
	 * a login or a hop into the arena too, so that a player who switches to another panel mid-run keeps it. Leaving puts
	 * nothing back: the run just played is in the panel's Past waves, to copy or watch. ClientToolbar.openPanel must run
	 * on Swing's thread, and does nothing once shutDown has taken the button away.
	 */
	private void openOnEntering()
	{
		NavigationButton button = navButton;
		if (button != null && config.openOnEntering())
		{
			SwingUtilities.invokeLater(() -> clientToolbar.openPanel(button));
		}
	}

	/**
	 * The first time the player enters the arena with the plugin (the onboarding ideas of 2026-10-27, O3), one line in the
	 * chat box from the plugin names Arena now and where it is, and never again: walking in is when it matters. Not for a
	 * player who had saved waves before the line came (markFirstRunDone), nor with "Messages in chat" off. The plugin says
	 * it in the game's chat box alone, as its other lines; nothing is typed for the player.
	 */
	private void sayOnFirstEntering()
	{
		if (config.enteringSaid())
		{
			return;
		}
		configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.ENTERING_SAID, true);
		if (config.chatConfirm())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage("[Sol Survivor] Press Arena now in the Sol Survivor side panel to open this wave on the site, "
					+ "at its start or on a pillar stack.")
				.build());
		}
	}

	/**
	 * A player who had saved waves before the first-run help came is no newcomer: Getting started, the line on first
	 * entering and the first link's mark are done with for them. On the client thread.
	 */
	private void markFirstRunDone()
	{
		for (String key : new String[]{SolSurvivorConfig.GETTING_STARTED_DONE, SolSurvivorConfig.ENTERING_SAID, SolSurvivorConfig.FIRST_LINK_OPENED})
		{
			if (!Boolean.parseBoolean(configManager.getConfiguration(SolSurvivorConfig.GROUP, key)))
			{
				configManager.setConfiguration(SolSurvivorConfig.GROUP, key, true);
			}
		}
	}

	/**
	 * With "Sidebar icon only at the Colosseum" on, the plugin's sun is in RuneLite's sidebar only while the player is in the arena
	 * or its lobby, as players asked of other minigames' plugins (the ToA and ToB Mistake Trackers' issues 9 and 11); off,
	 * it always is. ClientToolbar adds and takes it on Swing's thread, in turn with openOnEntering's opening. On the
	 * client thread.
	 */
	private void updateButton()
	{
		boolean show = !config.onlyAtColosseum() || inColosseum || isInLobby();
		NavigationButton button = navButton;
		if (show == buttonShown || button == null)
		{
			return;
		}
		buttonShown = show;
		if (show)
		{
			clientToolbar.addNavigation(button);
		}
		else
		{
			clientToolbar.removeNavigation(button);
		}
	}

	/** Whether the player is in the Colosseum's lobby, the region the run is entered from. */
	private boolean isInLobby()
	{
		Player player = client.getLocalPlayer();
		LocalPoint lp = player == null ? null : player.getLocalLocation();
		return lp != null && WorldPoint.fromLocalInstance(client, lp).getRegionID() == ArenaGeometry.LOBBY_REGION_ID;
	}

	/**
	 * Has the game build the arena's scene again, as RuneLite's Roof Removal does for its own setting, so that the
	 * pillars come and go at once rather than at the next scene: the game draws a scene's fixed objects from what it
	 * built. Only in the arena, where the pillars are, so that the setting never reloads the scene anywhere else.
	 */
	private void rebuildArena()
	{
		if (client.getGameState() == GameState.LOGGED_IN && isInColosseum())
		{
			client.setGameState(GameState.LOADING);
		}
	}

	private boolean isInColosseum()
	{
		Player player = client.getLocalPlayer();
		if (player == null || !client.getTopLevelWorldView().isInstance())
		{
			return false;
		}
		LocalPoint lp = player.getLocalLocation();
		return lp != null && WorldPoint.fromLocalInstance(client, lp).getRegionID() == ArenaGeometry.REGION_ID;
	}

	private static OrbStyle orbStyleOf(int spotAnimId)
	{
		switch (spotAnimId)
		{
			case SpotanimID.VFX_MANTICORE_01_PROJECTILE_MAGIC_01:
				return OrbStyle.MAGIC;
			case SpotanimID.VFX_MANTICORE_01_PROJECTILE_RANGED_01:
				return OrbStyle.RANGED;
			case SpotanimID.VFX_MANTICORE_01_PROJECTILE_MELEE_01:
				return OrbStyle.MELEE;
			default:
				return null;
		}
	}

	/** A tile's grid tile, through the instance's template so that it is in region 7216's coordinates. */
	private GridTile gridTileOf(WorldPoint world)
	{
		return gridTileOf(LocalPoint.fromWorld(client.getTopLevelWorldView(), world));
	}

	/** A scene point's grid tile, or null for none, as gridTileOf(WorldPoint). */
	private GridTile gridTileOf(LocalPoint lp)
	{
		if (lp == null)
		{
			return null;
		}
		WorldPoint template = WorldPoint.fromLocalInstance(client, lp);
		if (template.getRegionID() != ArenaGeometry.REGION_ID)
		{
			return null;
		}
		return ArenaGeometry.fromRegion(template.getRegionX(), template.getRegionY());
	}

	/** The arena as it is on this tick; on the client thread only. */
	private ArenaState capture() throws ExportException
	{
		if (!inColosseum)
		{
			throw new ExportException("You are not in the Fortis Colosseum.");
		}
		Player player = client.getLocalPlayer();
		GridTile playerTile = player == null ? null : gridTileOf(player.getWorldLocation());
		return tracker.capture(client.getTickCount(), playerTile, seenNpcs(), m -> readLevel(m.getLevelVarbit()));
	}

	/** The NPCs alive on the grid on this tick, each on its south-west tile. On the client thread only. */
	private List<SeenNpc> seenNpcs()
	{
		List<SeenNpc> seen = new ArrayList<>();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			if (npc.isDead())
			{
				continue;
			}
			// An NPC's world location is its south-west tile, the tile the link gives it.
			GridTile tile = gridTileOf(npc.getWorldLocation());
			if (tile != null)
			{
				seen.add(new SeenNpc(npc.getIndex(), npc.getId(), tile));
			}
		}
		return seen;
	}

	/** The skills whose levels go in the link with the gear. */
	private static final Skill[] SKILLS = {
		Skill.ATTACK, Skill.STRENGTH, Skill.DEFENCE, Skill.RANGED, Skill.MAGIC, Skill.HITPOINTS, Skill.PRAYER
	};

	/** The slots the link names: those that hold gear, not the hair and jaw of the player's look. */
	private static final EquipmentInventorySlot[] SLOTS = {
		EquipmentInventorySlot.HEAD, EquipmentInventorySlot.CAPE, EquipmentInventorySlot.AMULET,
		EquipmentInventorySlot.WEAPON, EquipmentInventorySlot.BODY, EquipmentInventorySlot.SHIELD,
		EquipmentInventorySlot.LEGS, EquipmentInventorySlot.GLOVES, EquipmentInventorySlot.BOOTS,
		EquipmentInventorySlot.RING, EquipmentInventorySlot.AMMO
	};

	/**
	 * Where the player set the site's Safe / Fast / Easy triangle to start, which the state links give the site; null when
	 * "Set the triangle" is off, or Safe, Fast and Easy are all 0, and the site keeps its own.
	 */
	private TriangleDot triangle()
	{
		return config.setTriangle() ? TriangleDot.of(config.triangleSafe(), config.triangleFast(), config.triangleEasy()) : null;
	}

	/**
	 * The player's own tile markers in the arena, on their plane, which Arena now's link gives the site to draw: Ground
	 * Markers' first, then those of the packs Tile Packs draws, one to a tile; null when "Send my tile markers" is off. On
	 * the client thread only.
	 */
	private List<TileMarker> markers()
	{
		Player me = client.getLocalPlayer();
		if (!config.sendMarkers() || me == null || me.getLocalLocation() == null)
		{
			return null;
		}
		int plane = WorldPoint.fromLocalInstance(client, me.getLocalLocation()).getPlane();
		List<TileMarker> markers = new ArrayList<>(GroundMarkers.read(gson, configManager.getConfiguration(GroundMarkers.GROUP, GroundMarkers.KEY), plane));
		markers.addAll(tilePackMarkers(plane));
		return TileMarker.distinct(markers);
	}

	/** The plugin whose built-in tile packs `tilePacksBuiltIn` holds, so that they are read once while it stays loaded. */
	private Plugin tilePacksPlugin;

	private Map<Integer, TilePacks.Pack> tilePacksBuiltIn = Collections.emptyMap();

	/**
	 * The arena's tiles on `plane` that Tile Packs draws, when it is installed and on: none otherwise, as the game then
	 * shows none of them.
	 */
	private List<TileMarker> tilePackMarkers(int plane)
	{
		Plugin packs = null;
		for (Plugin p : pluginManager.getPlugins())
		{
			if (p.getClass().getName().equals(TilePacks.PLUGIN_CLASS) && pluginManager.isPluginEnabled(p))
			{
				packs = p;
			}
		}
		if (packs == null)
		{
			return Collections.emptyList();
		}
		if (packs != tilePacksPlugin)
		{
			tilePacksPlugin = packs;
			tilePacksBuiltIn = TilePacks.builtIn(gson, resourceText(packs.getClass(), TilePacks.BUILT_IN));
		}
		List<String> saved = new ArrayList<>();
		String prefix = TilePacks.GROUP + "." + TilePacks.PACK_PREFIX;
		for (String key : configManager.getConfigurationKeys(prefix))
		{
			saved.add(configManager.getConfiguration(TilePacks.GROUP, key.substring(TilePacks.GROUP.length() + 1)));
		}
		Boolean override = configManager.getConfiguration(TilePacks.SETTINGS, "overrideColorActive", Boolean.class);
		Color colour = configManager.getConfiguration(TilePacks.SETTINGS, "overrideColor", Color.class);
		Boolean labels = configManager.getConfiguration(TilePacks.SETTINGS, "showLabels", Boolean.class);
		return TilePacks.read(gson, tilePacksBuiltIn, saved, configManager.getConfiguration(TilePacks.GROUP, TilePacks.DRAWN),
			Boolean.TRUE.equals(override) ? (colour == null ? TilePacks.DEFAULT_OVERRIDE : colour) : null, !Boolean.FALSE.equals(labels), plane);
	}

	/** The text of the resource `name` beside `type`, or null when it is missing or cannot be read. */
	private static String resourceText(Class<?> type, String name)
	{
		try (InputStream in = type.getResourceAsStream(name))
		{
			return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		catch (IOException e)
		{
			return null;
		}
	}

	/**
	 * The player's prayer over the waves counted, which the state links give the site's Prayer reliability; null when
	 * "Count my flicks" is off, and the site keeps its own.
	 */
	private PrayerRecord prayer()
	{
		return config.countFlicks() ? prayerRecord : null;
	}

	/**
	 * The player's gear and stats now, or null when the player has switched them off. The items' names and stats come
	 * from RuneLite's item data; the weapon's category and style are the game's own numbers. On the client thread only.
	 */
	private Equipment gear()
	{
		if (!config.includeGear())
		{
			return null;
		}
		Equipment gear = new Equipment();
		int prayerBonus = 0;
		int weaponSpeed = 0;
		ItemContainer worn = client.getItemContainer(InventoryID.WORN);
		Item[] items = worn == null ? new Item[0] : worn.getItems();
		for (EquipmentInventorySlot slot : SLOTS)
		{
			Item item = slot.getSlotIdx() < items.length ? items[slot.getSlotIdx()] : null;
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			String name = itemManager.getItemComposition(item.getId()).getName();
			gear.item(slot.name().toLowerCase(Locale.ROOT), new Equipment.Item(item.getId(), name, item.getQuantity()));
			ItemStats stats = itemManager.getItemStats(item.getId());
			if (stats != null && stats.getEquipment() != null)
			{
				gear.bonuses(item.getId(), bonusesOf(stats.getEquipment()));
				gear.defences(item.getId(), defencesOf(stats.getEquipment()));
				prayerBonus += stats.getEquipment().getPrayer();
				if (slot == EquipmentInventorySlot.WEAPON)
				{
					weaponSpeed = stats.getEquipment().getAspeed();
				}
			}
		}
		switchesOf(gear);
		int category = client.getVarbitValue(VarbitID.COMBAT_WEAPON_CATEGORY);
		gear.weapon(weaponSpeed, category, client.getVarpValue(VarPlayerID.COM_MODE));
		gear.styleNames(styleNamesOf(category));
		gear.prayer(prayerBonus, client.getBoostedSkillLevel(Skill.PRAYER));
		for (Skill skill : SKILLS)
		{
			gear.stat(skill.name().toLowerCase(Locale.ROOT), client.getBoostedSkillLevel(skill), client.getRealSkillLevel(skill));
		}
		// Which offensive prayers need more than levels and are unlocked, for the site to pick the strongest the player has.
		gear.unlocks(readLevel(VarbitID.PRAYER_RIGOUR_UNLOCKED), readLevel(VarbitID.PRAYER_AUGURY_UNLOCKED),
			readLevel(VarbitID.PRAYER_DEADEYE_UNLOCKED), readLevel(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED),
			readLevel(VarbitID.KR_KNIGHTWAVES_STATE), readLevel(VarbitID.PRAYER_ZEAL_UNLOCKED));
		return gear;
	}

	/**
	 * The switches: each item in the inventory that RuneLite's item data says can be worn, with the slot it goes in and a
	 * weapon's attack speed, in the inventory's order. Food, potions and runes have no slot and are left out. On the client thread only.
	 */
	private void switchesOf(Equipment gear)
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INV);
		if (inventory == null)
		{
			return;
		}
		for (Item item : inventory.getItems())
		{
			if (item == null || item.getId() <= 0)
			{
				continue;
			}
			ItemStats stats = itemManager.getItemStats(item.getId());
			if (stats == null || !stats.isEquipable() || stats.getEquipment() == null)
			{
				continue;
			}
			String slot = slotName(stats.getEquipment().getSlot());
			if (slot == null)
			{
				continue;
			}
			gear.bonuses(item.getId(), bonusesOf(stats.getEquipment()));
			String name = itemManager.getItemComposition(item.getId()).getName();
			int speed = "weapon".equals(slot) ? stats.getEquipment().getAspeed() : 0;
			gear.switchItem(slot, new Equipment.Item(item.getId(), name, item.getQuantity()), speed);
		}
	}

	/** An item's bonuses as the link writes them (Equipment.bonuses), its magic damage in tenths of a percent. */
	private static int[] bonusesOf(ItemEquipmentStats e)
	{
		return new int[]{
			e.getAstab(), e.getAslash(), e.getAcrush(), e.getAmagic(), e.getArange(), e.getStr(), e.getRstr(),
			Math.round(e.getMdmg() * 10), e.isTwoHanded() ? 1 : 0
		};
	}

	/**
	 * A worn item's defence bonuses as the link writes them (Equipment.defences): stab, slash, crush, magic and ranged,
	 * which the site rolls the NPCs' accuracy against.
	 */
	private static int[] defencesOf(ItemEquipmentStats e)
	{
		return new int[]{e.getDstab(), e.getDslash(), e.getDcrush(), e.getDmagic(), e.getDrange()};
	}

	/** The link's name of a slot, by its index among the worn items, or null for one it does not name. */
	private static String slotName(int index)
	{
		for (EquipmentInventorySlot slot : SLOTS)
		{
			if (slot.getSlotIdx() == index)
			{
				return slot.name().toLowerCase(Locale.ROOT);
			}
		}
		return null;
	}

	/**
	 * The names of a weapon category's combat options, from the game's data as RuneLite's attack styles plugin reads
	 * them (script 4525's enum and structs); empty when the category has no enum, such as the partisan's.
	 */
	private List<String> styleNamesOf(int category)
	{
		List<String> names = new ArrayList<>();
		try
		{
			int styles = client.getEnum(EnumID.WEAPON_STYLES).getIntValue(category);
			if (styles == -1)
			{
				return names;
			}
			for (int struct : client.getEnum(styles).getIntVals())
			{
				names.add(client.getStructComposition(struct).getStringValue(ParamID.ATTACK_STYLE_NAME));
			}
		}
		catch (RuntimeException ex)
		{
			// The site falls back on the style's place alone.
			log.debug("Could not read the weapon's style names", ex);
			names.clear();
		}
		return names;
	}

	private int readLevel(int varbit)
	{
		try
		{
			return client.getVarbitValue(varbit);
		}
		catch (RuntimeException ex)
		{
			// A varbit the game has moved reads as not taken rather than breaking the export.
			log.debug("Could not read varbit {}", varbit, ex);
			return 0;
		}
	}

	/** The listeners have one lifecycle, including the separate copy-only and browser-opening bindings. */
	void registerHotkeys()
	{
		keyManager.registerKeyListener(hotkeyListener);
		keyManager.registerKeyListener(openArenaHotkeyListener);
		keyManager.registerKeyListener(tickResetHotkeyListener);
	}

	void unregisterHotkeys()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		keyManager.unregisterKeyListener(openArenaHotkeyListener);
		keyManager.unregisterKeyListener(tickResetHotkeyListener);
	}

	/**
	 * Copies the arena's link of now. Arena now and its opening hotkey also open the same link in the browser, on the
	 * player's explicit press; the separate Copy link hotkey keeps copying without moving focus to the browser.
	 */
	void copyCurrent(boolean open)
	{
		clientThread.invokeLater(() ->
		{
			try
			{
				ArenaState state = capture();
				String done = open ? "Opened the arena on the site, and copied its link." : "Copied the arena's link.";
				String note = state.isModifiersComplete()
					? done
					: done + " Quartet, Red Flag and Totemic show from your next modifier pick on: set them on the site.";
				// The first arena the plugin opens on the site carries the mark of a first link, for the site's short tour of
				// a plugin player's page (O4); and once the player has an arena's link, Getting started has done its job (O1).
				boolean first = open && !config.firstLinkOpened();
				String link = StatePayload.link(SITE, state, gear(), triangle(), prayer(), first, markers());
				if (open)
				{
					LinkBrowser.browse(link);
				}
				if (first)
				{
					configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.FIRST_LINK_OPENED, true);
				}
				if (!config.gettingStartedDone())
				{
					configManager.setConfiguration(SolSurvivorConfig.GROUP, SolSurvivorConfig.GETTING_STARTED_DONE, true);
				}
				copy(link, note, open ? "Opened the arena on the site." : null);
			}
			catch (ExportException ex)
			{
				report(ex.getMessage());
			}
		});
	}

	/**
	 * Opens the site's modifier pick in the browser with the three modifiers Minimus offers filled in, while his selector
	 * is open, and copies its link, as Arena now does (the UX review of 2026-10-27, F3): the link holds them, the
	 * modifiers held, the next wave, the run's waves finished so far and the reward chest as the game holds it
	 * (chestItems), and nothing else.
	 */
	private void openRecommender()
	{
		clientThread.invokeLater(() ->
		{
			List<Modifier> offers = tracker.getOptions();
			if (offers.isEmpty())
			{
				report("No offers to open: press this while Minimus offers his three modifiers.");
				return;
			}
			Integer wave = tracker.getWave();
			String link = PickPayload.link(SITE, wave, tracker.modifiers(m -> readLevel(m.getLevelVarbit())), offers, tracker.getRun(),
				chestItems(InventoryID.COLOSSEUM_REWARDS_FUTURE), chestItems(InventoryID.COLOSSEUM_REWARDS));
			LinkBrowser.browse(link);
			copy(link, wave == null
				? "Opened the modifier pick with Minimus's offers, and copied its link. The plugin has not seen a wave yet: set the "
					+ "wave on the site."
				: "Opened the modifier pick with Minimus's offers for wave " + wave + ", and copied its link.",
				"Opened the modifier pick on the site.");
		});
	}

	/**
	 * The items of one of the Colosseum's reward containers, each by its name, a note read as the item it stands for, and
	 * how many: COLOSSEUM_REWARDS_FUTURE for what clearing the next wave adds, COLOSSEUM_REWARDS for the chest so far
	 * (PickPayload's `chest`). None when the game has not sent the container. Called on the client thread.
	 */
	private List<ChestItem> chestItems(int containerId)
	{
		ItemContainer container = client.getItemContainer(containerId);
		if (container == null)
		{
			return Collections.emptyList();
		}
		List<ChestItem> items = new ArrayList<>();
		for (Item item : container.getItems())
		{
			if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
			{
				continue;
			}
			String name = itemManager.getItemComposition(itemManager.canonicalize(item.getId())).getName();
			items.add(new ChestItem(name, item.getQuantity()));
		}
		return items;
	}

	/**
	 * Opens a logged wave on the site, and copies its link: at its first tick, or on the tick its reinforcements came
	 * (`reinforcements`), as the wave log kept them.
	 */
	private void openPast(int id, boolean reinforcements)
	{
		Consumer<String> reply = reporter();
		clientThread.invokeLater(() ->
		{
			WaveLog.Past wave = loggedWave(id);
			if (wave == null)
			{
				reply.accept("That wave is no longer kept: more waves have come since than the log keeps.");
				return;
			}
			String state = reinforcements ? wave.getReinforcementState() : wave.getStartState();
			if (state == null)
			{
				reply.accept(reinforcements
					? "The plugin did not see wave " + wave.getWave() + "'s reinforcements come."
					: "The plugin did not see wave " + wave.getWave() + " start.");
				return;
			}
			String link = StatePayload.linkOfJson(SITE, state, triangle(), prayer());
			String what = reinforcements ? "wave " + wave.getWave() + " at its reinforcements" : "wave " + wave.getWave() + "'s start";
			LinkBrowser.browse(link);
			copy(link, "Opened " + what + " on the site, and copied its link.", "Opened " + what + " on the site.", reply);
		});
	}

	/** The logged wave `id`, the one under way or the last one or a past one, or null when it is no longer kept. */
	private WaveLog.Past loggedWave(int id)
	{
		for (List<WaveLog.Past> run : waveLog.getRuns())
		{
			for (WaveLog.Past wave : run)
			{
				if (wave.getId() == id)
				{
					return wave;
				}
			}
		}
		return null;
	}

	/** Copies the log of the panel's row `id`: the wave under way or the last one, or a past one. */
	private void copyWaveLog(int id)
	{
		Consumer<String> reply = reporter();
		clientThread.invokeLater(() ->
		{
			WaveLog.Past wave = loggedWave(id);
			if (wave == null)
			{
				reply.accept("That wave log is no longer kept: more waves have come since than the log keeps.");
				return;
			}
			String cut = waveLog.getId() == id && waveLog.isCut() ? ", cut at " + WaveLog.MAX_TICKS : "";
			copy(wave.getJson(), "Copied the wave log of wave " + wave.getWave() + ", " + wave.size() + " ticks" + cut + ".", null, reply);
		});
	}

	private List<WaveLog.Past> loggedRun(int id)
	{
		for (List<WaveLog.Past> run : waveLog.getRuns())
		{
			for (WaveLog.Past wave : run)
			{
				if (wave.getId() == id)
				{
					return run;
				}
			}
		}
		return null;
	}

	/** A run's Watch: uploads the run and opens it on the site at its first wave. */
	private void openPastRun(int id)
	{
		watch(id, false, null);
	}

	/**
	 * Opens the run that holds the logged wave `id` on the site: at its first wave, or with `atWave` at that wave, and at
	 * `tick` of it when given, as the wave report and the death recap ask. Snapshot on the client thread, upload in the
	 * background, then the recording's short link opens in the browser. A run uploaded this session opens again without
	 * being sent twice (uploads). Only with "Upload past runs" on, off by default, which shows RuneLite's warning as the
	 * player switches it on.
	 */
	private void watch(int id, boolean atWave, Integer tick)
	{
		Consumer<String> reply = reporter();
		clientThread.invokeLater(() ->
		{
			if (panel == null)
			{
				return;
			}
			if (!config.uploadPastRuns())
			{
				reply.accept("Watching a run needs Upload past runs, in the plugin's settings: it sends the run to the Sol Survivor site to play it back there.");
				return;
			}
			if (replayCall != null)
			{
				reply.accept("A run is already opening. Please wait.");
				return;
			}
			List<WaveLog.Past> run = loggedRun(id);
			WaveLog.Past wave = loggedWave(id);
			if (run == null || wave == null)
			{
				reply.accept("That run is no longer kept: more waves have come since than the log keeps.");
				return;
			}
			run = new ArrayList<>(run);
			run.removeIf(w -> w.size() == 0);
			if (run.isEmpty())
			{
				reply.accept("No ticks have been recorded yet. Wait for a game tick and press Watch again.");
				return;
			}
			Integer at = atWave ? Integer.valueOf(wave.getWave()) : null;
			Integer from = atWave ? tick : null;
			String key = run.get(0).getId() + "-" + run.get(run.size() - 1).getId();
			Uploaded done = uploads.get(key);
			if (done != null && System.currentTimeMillis() - done.at < UPLOAD_KEPT_MS)
			{
				LinkBrowser.browse(ReplayLink.browser(SITE, done.id, at, from));
				reply.accept(opened(at, from));
				return;
			}
			// The past waves' logs never change once kept, so the run's JSON is written, and gzipped, on OkHttp's thread.
			List<WaveLog.Past> chosen = run;
			Call call = httpClient.newCall(ReplayUpload.request(SITE, () -> RunLogWriter.toJson(chosen, PluginVersion.VERSION)));
			replayCall = call;
			SwingUtilities.invokeLater(() -> { if (panel != null) panel.showReplayBusy(true); });
			reply.accept("Uploading the run to watch it back…");
			call.enqueue(new Callback()
			{
				@Override
				public void onFailure(Call failed, IOException ex)
				{
					finishReplay(failed, key, null, at, from, ex instanceof RunUpload.TooLarge ? ex.getMessage()
						: "The run could not reach the site. Check your connection and try again.", reply);
				}

				@Override
				public void onResponse(Call completed, Response response)
				{
					try (Response closed = response)
					{
						finishReplay(completed, key, ReplayUpload.idOf(gson, closed), at, from, null, reply);
					}
					catch (IOException ex)
					{
						finishReplay(completed, key, null, at, from, ex.getMessage(), reply);
					}
				}
			});
		});
	}

	/** What the player hears once their run opens: at its start, or at a wave and a tick of it. */
	private static String opened(Integer wave, Integer tick)
	{
		return wave == null ? "Opened your recorded run in the browser."
			: tick == null ? "Opened your recorded run in the browser at wave " + wave + "."
			: "Opened your recorded run in the browser at tick " + tick + " of wave " + wave + ".";
	}

	private void finishReplay(Call call, String key, String replayId, Integer wave, Integer tick, String error, Consumer<String> reply)
	{
		clientThread.invokeLater(() ->
		{
			if (panel == null || replayCall != call)
			{
				return;
			}
			replayCall = null;
			SwingUtilities.invokeLater(() -> { if (panel != null) panel.showReplayBusy(false); });
			if (replayId != null)
			{
				uploads.put(key, new Uploaded(replayId, System.currentTimeMillis()));
				LinkBrowser.browse(ReplayLink.browser(SITE, replayId, wave, tick));
				reply.accept(opened(wave, tick));
			}
			else
			{
				reply.accept(error);
			}
		});
	}

	/** A run the site stored this session: its id there, and when it was sent. */
	private static final class Uploaded
	{
		private final String id;
		private final long at;

		Uploaded(String id, long at)
		{
			this.id = id;
			this.at = at;
		}
	}

	/**
	 * Copies the whole run that holds the panel's row `id` in one paste, as RunLogWriter writes it. The run is taken on the
	 * client thread, and its JSON, up to twelve waves of it, written on RuneLite's executor, as Watch's is on
	 * OkHttp's: the past waves' logs never change once kept.
	 */
	private void copyRun(int id)
	{
		Consumer<String> reply = reporter();
		clientThread.invokeLater(() ->
		{
			List<WaveLog.Past> run = loggedRun(id);
			if (run == null)
			{
				reply.accept("That run is no longer kept: more waves have come since than the log keeps.");
				return;
			}
			int from = run.get(0).getWave();
			int to = run.get(run.size() - 1).getWave();
			int ticks = 0;
			for (WaveLog.Past wave : run)
			{
				ticks += wave.size();
			}
			String waves = from == to ? "wave " + from : "waves " + from + " to " + to;
			String note = "Copied the whole run, " + waves + ", " + ticks + (ticks == 1 ? " tick" : " ticks") + "."
				+ (from == 1 ? "" : " The plugin logged it from wave " + from + " on.");
			executor.execute(() -> copy(RunLogWriter.toJson(run, PluginVersion.VERSION), note, null, reply));
		});
	}

	/**
	 * Puts `text` on the clipboard and says `message`. Another program may hold the clipboard, as on Windows, and then
	 * the player hears so, after `opened` when the link opened anyway.
	 */
	private void copy(String text, String message, String opened)
	{
		copy(text, message, opened, this::report);
	}

	private void copy(String text, String message, String opened, Consumer<String> reply)
	{
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
		}
		catch (IllegalStateException ex)
		{
			log.debug("Could not copy to the clipboard", ex);
			String busy = "Another program is using the clipboard, so nothing was copied: try again.";
			reply.accept(opened == null ? busy : opened + " " + busy);
			return;
		}
		reply.accept(message);
	}

	/** Tells the player what happened, in the panel and, if they want it, in the chat box. */
	private void report(String message)
	{
		report(message, null);
	}

	/** Captures a pressed past-wave action before its client-thread, clipboard or upload work can finish out of order. */
	private Consumer<String> reporter()
	{
		SolSurvivorPanel p = panel;
		String target = p == null ? null : p.resultTarget();
		return message -> report(message, target);
	}

	private void report(String message, String target)
	{
		if (config.chatConfirm())
		{
			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.GAMEMESSAGE)
				.runeLiteFormattedMessage("[Sol Survivor] " + message)
				.build());
		}
		SolSurvivorPanel p = panel;
		if (p != null)
		{
			SwingUtilities.invokeLater(() -> p.showResult(message, target));
		}
	}

	private void updatePanel()
	{
		SolSurvivorPanel p = panel;
		if (p == null)
		{
			return;
		}
		boolean colosseum = inColosseum;
		Integer wave = tracker.getWave();
		String title;
		String state;
		if (!colosseum)
		{
			title = "Not in the Colosseum";
			state = "Enter it to open a wave.";
		}
		else if (wave == null)
		{
			title = "In the Colosseum";
			state = "Waiting for a wave to start.";
		}
		else if (tracker.isWaveStarted())
		{
			title = "Wave " + wave;
			state = "Under way";
		}
		else
		{
			title = "Wave " + wave + " next";
			state = "Between waves";
		}
		String note = config.recordWaveLog()
			? "Each wave you play shows here, newest first, once the plugin sees it start."
			: "Switch on \"Record wave log\" in the plugin's settings to keep past waves here.";
		List<SolSurvivorPanel.RunGroup> runs = runGroups();
		List<SolSurvivorPanel.Offer> offers = offers();
		boolean sharing = config.shareWaveLogs() && config.recordWaveLog();
		// The leaderboard's row, while Leaderboard is on with a name, or Remove my times has times to take off; its page
		// opens only outside a wave, as the plugin shows nothing of the boards while one is under way.
		String board = boardName();
		if (board == null && config.leaderboard() && BoardEntry.isBlank(config.leaderboardName()))
		{
			// Logged out, the RuneScape name is not known yet.
			board = "your RuneScape name";
		}
		// The board's row only out of the arena: neither a wave nor the time between two is for managing it.
		String boardRow = colosseum ? null : board != null ? board : BoardEntry.isKey(config.leaderboardKey()) ? "" : null;
		boolean boardOpen = !(colosseum && tracker.isWaveStarted());
		// The cards, each in its moment ("Plugin 1.0: what the panel shows when", 2026-10-26): the wave's list in the
		// arena; nothing of the report during a wave, which is the wave's (the UX review of 2026-10-27, F13); the report of
		// the last wave reviewed between waves, and out of the arena after a run as a line, until the player is back in the
		// arena before a new run's first wave; and the recap only out of the arena, after the death that ended the run, as
		// the death report's page with a line to it (the user's ask of 2026-10-28).
		WaveView view = colosseum ? waveView : WaveView.NONE;
		List<Object> waveCard = Arrays.asList(view, config.showWaveNpcs(), config.markDead(), config.showNextWave(), config.showModifiers(),
			config.doomLimit());
		boolean during = colosseum && tracker.isWaveStarted();
		// Arena now has a wave to open only while one it plans is under way: before a wave and between two the arena holds
		// no NPCs, and Sol Heredit's is not planned (the UX review of 2026-10-27, F1). The title and state say as much, so
		// the check below that skips an unchanged panel covers it.
		boolean live = during && wave != null && wave <= ColosseumTracker.LAST_WAVE;
		boolean tracking = config.trackMistakes() && config.recordWaveLog();
		boolean newRun = colosseum && wave == null;
		WaveCards.Report report = tracking && !during && !newRun ? reportOf(lastReview) : null;
		WaveCards.Recap recap = config.deathRecap() && !colosseum ? recapOf(lastReview) : null;
		boolean cardsSame = waveCard.equals(shownWave) && Objects.equals(report, shownReport) && Objects.equals(recap, shownRecap)
			&& !colosseum == shownOutside;
		// The last result is about the moment before: it goes as a wave starts and as the player leaves the arena (the UX
		// review of 2026-10-27, F15).
		if (during && !shownDuring || !colosseum && shownInColosseum)
		{
			SwingUtilities.invokeLater(() -> p.showResult(""));
		}
		shownDuring = during;
		// The first-run cards (the onboarding ideas of 2026-10-27): Getting started until it is done with, and What you share
		// as the settings are; neither during a wave, which the panel keeps to. While Minimus offers, Getting started waits,
		// so that his pick is the one lit card (the UX review of 2026-10-27, F4).
		boolean upload = config.uploadPastRuns();
		boolean share = config.shareWaveLogs();
		boolean leaderboard = config.leaderboard();
		boolean gettingStarted = !config.gettingStartedDone() && !during && offers.isEmpty();
		boolean opensItself = config.openOnEntering();
		boolean[] inGame = inGame();
		List<Object> firstRun = Arrays.asList(upload, share, leaderboard, gettingStarted, opensItself, during, colosseum,
			Arrays.toString(inGame));
		if (!firstRun.equals(shownFirstRun))
		{
			shownFirstRun = firstRun;
			SwingUtilities.invokeLater(() ->
			{
				p.showGettingStarted(gettingStarted, opensItself, colosseum);
				p.showSends(upload, share, leaderboard);
				p.showSendsRow(!during);
				p.showInGame(inGame);
			});
		}
		if (colosseum == shownInColosseum && title.equals(shownTitle) && state.equals(shownState)
			&& note.equals(shownNote) && runs.equals(shownRuns) && offers.equals(shownOffers) && sharing == shownSharing
			&& Objects.equals(boardRow, shownBoard) && boardOpen == shownBoardOpen && cardsSame)
		{
			return;
		}
		if (!cardsSame)
		{
			shownWave = waveCard;
			shownReport = report;
			shownRecap = recap;
			shownOutside = !colosseum;
			boolean list = config.showWaveNpcs();
			boolean markDead = config.markDead();
			boolean next = config.showNextWave();
			boolean modifiers = config.showModifiers();
			boolean doom = config.doomLimit();
			SwingUtilities.invokeLater(() ->
			{
				p.cards.showWave(view, list, markDead, next, modifiers, doom);
				p.cards.showReview(report, recap, !colosseum);
			});
		}
		shownInColosseum = colosseum;
		shownTitle = title;
		shownState = state;
		shownNote = note;
		shownRuns = runs;
		shownOffers = offers;
		shownSharing = sharing;
		shownBoard = boardRow;
		shownBoardOpen = boardOpen;
		SwingUtilities.invokeLater(() ->
		{
			p.showState(colosseum, live, colosseum ? wave : null, title, state, offers, note, runs);
			p.showSharing(sharing);
			p.showBoard(boardRow, sharing, boardOpen);
		});
	}

	/**
	 * Minimus's offers as the pick's row shows them: each by its key, for its picture, and its name with the tier taking it
	 * brings, the one after the tier held, as PickPayload sends them; an offer the plugin does not know is left out.
	 */
	private List<SolSurvivorPanel.Offer> offers()
	{
		List<SolSurvivorPanel.Offer> out = new ArrayList<>();
		Map<Modifier, Integer> held = waveView.held;
		for (Modifier m : tracker.getOptions())
		{
			if (m != null)
			{
				int tier = m.hasTiers() ? Math.min(3, held.getOrDefault(m, 0) + 1) : 1;
				out.add(new SolSurvivorPanel.Offer(m.getKey(), m.getName(tier)));
			}
		}
		return out;
	}

	/**
	 * The runs the panel shows, newest first, each its waves newest first. A wave under way says so without its time, so
	 * that its line stays as it is while it records, and the run's time is that of its waves done, for the same reason.
	 * With the mistake tracker on, a wave reviewed carries the mistakes its report counts.
	 */
	private List<SolSurvivorPanel.RunGroup> runGroups()
	{
		List<SolSurvivorPanel.RunGroup> out = new ArrayList<>();
		boolean tracking = config.trackMistakes() && config.recordWaveLog();
		Set<WaveReview.Kind> kinds = countedKinds();
		int idleFrom = config.idleFrom();
		List<List<WaveLog.Past>> runs = waveLog.getRuns();
		for (int i = 0; i < runs.size(); i++)
		{
			List<WaveLog.Past> run = runs.get(i);
			List<SolSurvivorPanel.WaveRow> rows = new ArrayList<>();
			int ticksDone = 0;
			Integer gloryDone = null;
			for (WaveLog.Past wave : run)
			{
				String detail = wave.isRecording() ? "Under way" : duration(wave.size()) + endName(wave) + glory(wave.getGlory());
				WaveReview review = tracking && !wave.isRecording() ? reviews.get(wave.getId()) : null;
				rows.add(0, new SolSurvivorPanel.WaveRow(wave.getId(), wave.getWave(), detail, wave.isRecording(),
					wave.getStartState() != null, wave.getReinforcementState() != null, review == null ? null : review.counted(kinds, idleFrom).size()));
				if (!wave.isRecording())
				{
					ticksDone += wave.size();
				}
				if (wave.getGlory() != null)
				{
					gloryDone = (gloryDone == null ? 0 : gloryDone) + wave.getGlory();
				}
			}
			int from = run.get(0).getWave();
			int to = run.get(run.size() - 1).getWave();
			String waves = from == to ? "Wave " + from : "Waves " + from + "\u2013" + to;
			String detail = ticksDone > 0 ? waves + " \u00b7 " + duration(ticksDone) : waves;
			out.add(new SolSurvivorPanel.RunGroup(run.get(0).getId(), i == 0 ? "Latest run" : "Earlier run", detail,
				gloryDone == null ? null : gloryText(gloryDone), rows));
		}
		return out;
	}

	/**
	 * A wave's glory after its time, by the game's own counter (WaveLog.getGlory): only a wave completed earned any, and a
	 * run's is that of its waves completed, as the game counts nothing from a death on.
	 */
	private static String glory(Integer glory)
	{
		return glory == null ? "" : " \u00b7 " + gloryText(glory);
	}

	private static String gloryText(int glory)
	{
		return String.format(Locale.UK, "%,d glory", glory);
	}

	/** A wave's length in game time, minutes and seconds, from its ticks of 0.6 seconds. */
	private static String duration(int ticks)
	{
		int seconds = ticks * 3 / 5;
		return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
	}

	/**
	 * How a log's wave ended, after its time: only a wave the player died in or left says so, as completing it is the
	 * rule. A death ends the log as LEFT too, so the wave's own mark tells them apart (the UX review of 2026-10-27, F6).
	 */
	static String endName(WaveLog.Past wave)
	{
		return wave.isDied() ? ", died" : wave.getEnd() == WaveLog.End.LEFT ? ", left" : "";
	}
}
