package nl.dexanddice.solsurvivor;

import com.google.inject.Provides;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemEquipmentStats;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.ItemStats;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.HotkeyListener;
import net.runelite.client.util.LinkBrowser;
import nl.dexanddice.solsurvivor.core.ArenaGeometry;
import nl.dexanddice.solsurvivor.core.ArenaState;
import nl.dexanddice.solsurvivor.core.ColosseumTracker;
import nl.dexanddice.solsurvivor.core.Equipment;
import nl.dexanddice.solsurvivor.core.ExportException;
import nl.dexanddice.solsurvivor.core.GridTile;
import nl.dexanddice.solsurvivor.core.Modifier;
import nl.dexanddice.solsurvivor.core.OrbStyle;
import nl.dexanddice.solsurvivor.core.PickPayload;
import nl.dexanddice.solsurvivor.core.PluginVersion;
import nl.dexanddice.solsurvivor.core.SeenNpc;
import nl.dexanddice.solsurvivor.core.StatePayload;
import nl.dexanddice.solsurvivor.core.WaveLog;
import nl.dexanddice.solsurvivor.core.WaveLogWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the Fortis Colosseum's state as a Sol Survivor link (design spec 2026-10-14-runelite-plugin-design.md). It only
 * reads the game and exports: it draws nothing in the game, neither tiles nor prayers nor plans, as Jagex's rules for
 * third-party clients require. The link goes to the clipboard from the side panel or a hotkey, and the player pastes it
 * into the site. While Minimus offers his three modifiers, the panel's "Open in recommender" opens the site's modifier
 * pick in the browser with them filled in (core.PickPayload); the offers ride in the link's hash, which the browser
 * keeps to itself.
 *
 * This class passes the game's events to ColosseumTracker, which holds the logic and is tested without RuneLite, and
 * reads the arena on the client thread when a link is asked for. With "Record wave log" on it also records each wave it
 * saw start, a tick at a time, in a WaveLog that keeps the waves since the plugin started and that the panel copies as
 * JSON for the site's wave reviewer, any of them picked from a list.
 *
 * It sends nothing anywhere: a bad plan is reported from the site's own page.
 */
@PluginDescriptor(
	name = "Sol Survivor",
	description = "Copies the Fortis Colosseum's state as a Sol Survivor link. Draws nothing in the game.",
	tags = {"fortis", "colosseum", "sol", "planner", "export"}
)
public class SolSurvivorPlugin extends Plugin
{
	private static final Logger log = LoggerFactory.getLogger(SolSurvivorPlugin.class);

	/** The script that fills the modifier selector, with the three on offer and the mask of those taken. */
	private static final int SCRIPT_MODIFIER_SELECT_INIT = 4931;
	/** The option picked in the selector, 1 to 3, read when Minimus leaves. */
	private static final int VARBIT_MODIFIER_SELECTED = 9788;

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

	private final ColosseumTracker tracker = new ColosseumTracker();
	private boolean inColosseum;
	/** The link of the current wave's first tick, taken as it happens, since the player cannot press a key that fast. */
	private String waveStartLink;
	/** The same link's payload, which the wave's log starts with. */
	private String waveStartState;
	/** The log of the wave under way, or of the last one until the next starts, and the past ones; on the client thread only. */
	private final WaveLog waveLog = new WaveLog();

	private SolSurvivorPanel panel;
	private NavigationButton navButton;
	/** What the panel last showed, so that it is redrawn only when that changes rather than every tick. */
	private boolean shownInColosseum;
	private boolean shownWaveStart;
	private String shownWaveLog;
	private List<SolSurvivorPanel.LogChoice> shownLogs = new ArrayList<>();
	private boolean shownOffers;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.copyHotkey())
	{
		@Override
		public void hotkeyPressed()
		{
			copyCurrent(false);
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
		panel = new SolSurvivorPanel(this::openWaveStart, () -> copyCurrent(true), this::openRecommender, this::copyWaveLog);
		navButton = NavigationButton.builder()
			.tooltip("Sol Survivor")
			.icon(SolSurvivorPanel.icon())
			.priority(10)
			.panel(panel)
			.build();
		clientToolbar.addNavigation(navButton);
		keyManager.registerKeyListener(hotkeyListener);
	}

	@Override
	protected void shutDown()
	{
		keyManager.unregisterKeyListener(hotkeyListener);
		clientToolbar.removeNavigation(navButton);
		tracker.reset();
		waveLog.clear();
		inColosseum = false;
		waveStartLink = null;
		waveStartState = null;
		shownInColosseum = false;
		shownWaveStart = false;
		shownWaveLog = null;
		shownLogs = new ArrayList<>();
		shownOffers = false;
		panel = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged e)
	{
		// Logged out, hopping or logging in again: the run is over for the plugin, whose next tick may be far off.
		GameState state = e.getGameState();
		if (state == GameState.LOGIN_SCREEN || state == GameState.LOGGING_IN || state == GameState.HOPPING)
		{
			leave();
		}
	}

	/** "Record wave log" switched off drops the log at once, and the panel shows the setting as it is, in or out of the arena. */
	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!SolSurvivorConfig.GROUP.equals(e.getGroup()) || !"recordWaveLog".equals(e.getKey()))
		{
			return;
		}
		clientThread.invokeLater(() ->
		{
			if (!config.recordWaveLog())
			{
				waveLog.clear();
				waveLog.discardPending();
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
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned e)
	{
		NPC npc = e.getNpc();
		tracker.onNpcSpawned(npc.getIndex(), npc.getId(), client.getTickCount());
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
		if (inColosseum && npc.getId() == NpcID.COLOSSEUM_MASTER)
		{
			// Minimus leaves once a modifier is picked.
			tracker.onModifierChosen(client.getVarbitValue(VARBIT_MODIFIER_SELECTED));
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
	 * A hitsplat, which RuneLite passes on between ticks: the wave log keeps those on the player and on NPCs for the next
	 * tick it records. Those on the wave's first tick may come before the wave's message, so they are kept whether or not
	 * a wave is being recorded, and dropped on the next tick if none is.
	 */
	@Subscribe
	public void onHitsplatApplied(HitsplatApplied e)
	{
		if (!inColosseum || !config.recordWaveLog())
		{
			return;
		}
		Actor actor = e.getActor();
		int amount = e.getHitsplat().getAmount();
		int type = e.getHitsplat().getHitsplatType();
		if (actor != null && actor == client.getLocalPlayer())
		{
			waveLog.onPlayerHit(amount, type);
		}
		else if (actor instanceof NPC)
		{
			waveLog.onNpcHit(((NPC) actor).getIndex(), amount, type);
		}
	}

	/**
	 * The game put a graphic on a tile, such as where a Javelin Colossus's special lands: the wave log keeps those on the
	 * grid for the next tick it records, as it does the hitsplats.
	 */
	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated e)
	{
		if (!inColosseum || !config.recordWaveLog())
		{
			return;
		}
		GraphicsObject graphic = e.getGraphicsObject();
		waveLog.onTileGraphic(gridTileOf(graphic.getLocation()), graphic.getId());
	}

	@Subscribe
	public void onGameTick(GameTick e)
	{
		boolean now = isInColosseum();
		if (inColosseum && !now)
		{
			leave();
		}
		inColosseum = now;
		if (!inColosseum)
		{
			return;
		}

		// Every tick, rather than on graphic events, which do not fire for a Manticore out of view behind a pillar.
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
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
		if (tracker.isWaveStartTick(tick))
		{
			try
			{
				ArenaState start = capture();
				Equipment gear = gear();
				waveStartLink = StatePayload.link(config.siteUrl(), start, gear);
				waveStartState = StatePayload.toJson(start, gear);
			}
			catch (ExportException ex)
			{
				waveStartLink = null;
				waveStartState = null;
			}
		}
		else if (!tracker.isWaveStarted())
		{
			waveStartLink = null;
			waveStartState = null;
		}
		recordWaveLog(tick);
		updatePanel();
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
			waveLog.start(tracker.getWave(), tick, tracker.modifiers(m -> readLevel(m.getLevelVarbit())), tracker.isModifiersComplete(),
				PluginVersion.VERSION, waveStartState);
		}
		if (!waveLog.isRecording())
		{
			waveLog.discardPending();
			return;
		}
		waveLog.record(tick, playerTick(), npcTicks());
		if (!tracker.isWaveStarted())
		{
			waveLog.finish(WaveLog.End.COMPLETED);
		}
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
	 * The prayers on, by their names in the wave log. Eagle Eye and Deadeye, and Mystic Might and Mystic Vigour, share a
	 * slot in the prayer book, which Client.isPrayerActive does not tell apart; RuneLite's own prayer plugin settles which
	 * one is meant by whether the newer prayer is unlocked, and so does this.
	 */
	@SuppressWarnings("deprecation")
	private List<String> activePrayers()
	{
		boolean deadeye = readLevel(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
		boolean vigour = readLevel(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
		List<String> out = new ArrayList<>();
		for (Prayer prayer : Prayer.values())
		{
			if ((prayer == Prayer.EAGLE_EYE && deadeye) || (prayer == Prayer.DEADEYE && !deadeye)
				|| (prayer == Prayer.MYSTIC_MIGHT && vigour) || (prayer == Prayer.MYSTIC_VIGOUR && !vigour))
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
		// The log of a wave the player died in or ran from is kept, so that it can be copied after.
		waveLog.finish(WaveLog.End.LEFT);
		tracker.reset();
		inColosseum = false;
		waveStartLink = null;
		waveStartState = null;
		updatePanel();
	}

	private boolean isInColosseum()
	{
		Player player = client.getLocalPlayer();
		if (player == null || !client.isInInstancedRegion())
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
		Player player = client.getLocalPlayer();
		GridTile playerTile = player == null ? null : gridTileOf(player.getWorldLocation());
		return tracker.capture(client.getTickCount(), playerTile, seen, m -> readLevel(m.getLevelVarbit()));
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

	/** Opens the wave start's link on the site, and copies it too, for sharing. */
	private void openWaveStart()
	{
		clientThread.invokeLater(() ->
		{
			if (waveStartLink == null)
			{
				report("No wave start to open: the plugin takes one on the first tick of each wave it sees start.");
				return;
			}
			LinkBrowser.browse(waveStartLink);
			copy(waveStartLink, "Opened the wave start on the site, and copied its link.", "Opened the wave start on the site.");
		});
	}

	/**
	 * Copies the arena's link of now: the hotkey only copies, and the panel's button (`open`) opens it on the site too, as
	 * opening a browser from a key pressed in the game would take the player out of it.
	 */
	private void copyCurrent(boolean open)
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
				String link = StatePayload.link(config.siteUrl(), state, gear());
				if (open)
				{
					LinkBrowser.browse(link);
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
	 * is open: the link holds them, the modifiers held and the next wave, and nothing else.
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
			String link = PickPayload.link(config.siteUrl(), wave, tracker.modifiers(m -> readLevel(m.getLevelVarbit())), offers);
			LinkBrowser.browse(link);
			report(wave == null
				? "Opened the recommender with Minimus's offers. The plugin has not seen a wave yet: set the wave on the site."
				: "Opened the recommender with Minimus's offers for wave " + wave + ".");
		});
	}

	/** Copies the log the panel's list picked, by its id: the wave under way or the last one, or a past one. */
	private void copyWaveLog(int id)
	{
		clientThread.invokeLater(() ->
		{
			if (!waveLog.hasLog())
			{
				report(config.recordWaveLog()
					? "No wave log to copy: the plugin records each wave it sees start."
					: "No wave log to copy: switch on \"Record wave log\" in the plugin's settings.");
				return;
			}
			if (waveLog.getId() == id)
			{
				String note = "Copied the wave log of wave " + waveLog.getWave() + ", " + waveLog.size() + " ticks"
					+ (waveLog.isCut() ? ", cut at " + WaveLog.MAX_TICKS : "") + ".";
				copy(WaveLogWriter.toJson(waveLog), note, null);
				return;
			}
			for (WaveLog.Past past : waveLog.getPast())
			{
				if (past.getId() == id)
				{
					copy(past.getJson(), "Copied the wave log of wave " + past.getWave() + ", " + past.size() + " ticks.", null);
					return;
				}
			}
			// Kept no longer: more waves have come since than the log keeps.
			report("That wave log is no longer kept: pick another.");
		});
	}

	/**
	 * Puts `text` on the clipboard and says `message`. Another program may hold the clipboard, as on Windows, and then
	 * the player hears so, after `opened` when the link opened anyway.
	 */
	private void copy(String text, String message, String opened)
	{
		try
		{
			Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
		}
		catch (IllegalStateException ex)
		{
			log.debug("Could not copy to the clipboard", ex);
			String busy = "Another program is using the clipboard, so nothing was copied: try again.";
			report(opened == null ? busy : opened + " " + busy);
			return;
		}
		report(message);
	}

	/** Tells the player what happened, in the panel and, if they want it, in the chat box. */
	private void report(String message)
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
			SwingUtilities.invokeLater(() -> p.showResult(message));
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
		boolean waveStart = waveStartLink != null;
		String log = waveLogStatus();
		List<SolSurvivorPanel.LogChoice> logs = logChoices();
		boolean offers = !tracker.getOptions().isEmpty();
		if (colosseum == shownInColosseum && waveStart == shownWaveStart && log.equals(shownWaveLog) && logs.equals(shownLogs)
			&& offers == shownOffers)
		{
			return;
		}
		shownInColosseum = colosseum;
		shownWaveStart = waveStart;
		shownWaveLog = log;
		shownLogs = logs;
		shownOffers = offers;
		SwingUtilities.invokeLater(() -> p.showState(colosseum, waveStart, offers, log, logs));
	}

	/**
	 * The logs the panel's list offers, newest first: the wave under way or the last one, then the past ones. A wave under
	 * way leaves its tick count out, so that the list stays as it is, and keeps the player's pick, while it records.
	 */
	private List<SolSurvivorPanel.LogChoice> logChoices()
	{
		List<SolSurvivorPanel.LogChoice> out = new ArrayList<>();
		if (!waveLog.hasLog())
		{
			return out;
		}
		String now = waveLog.isRecording() ? "recording" : waveLog.size() + " ticks, " + endName(waveLog.getEnd());
		out.add(new SolSurvivorPanel.LogChoice(waveLog.getId(), "Wave " + waveLog.getWave() + ", " + now));
		for (WaveLog.Past past : waveLog.getPast())
		{
			out.add(new SolSurvivorPanel.LogChoice(past.getId(), "Wave " + past.getWave() + ", " + past.size() + " ticks, " + endName(past.getEnd())));
		}
		return out;
	}

	/** How a log's wave ended, in the panel's words. */
	private static String endName(WaveLog.End end)
	{
		if (end == null)
		{
			return "end not seen";
		}
		return end == WaveLog.End.COMPLETED ? "completed" : "left";
	}

	/** What the panel says of the wave log: which wave, how many ticks, and whether it is still recording. */
	private String waveLogStatus()
	{
		if (!config.recordWaveLog())
		{
			return "Wave log: off.";
		}
		if (!waveLog.hasLog())
		{
			return "Wave log: none yet. It records each wave the plugin sees start.";
		}
		String state;
		if (waveLog.isRecording())
		{
			state = waveLog.isCut() ? "full, the rest is left out" : "recording";
		}
		else
		{
			state = waveLog.getEnd() == WaveLog.End.COMPLETED ? "wave completed" : "left the wave";
		}
		int past = waveLog.getPast().size();
		return "Wave log: wave " + waveLog.getWave() + ", " + waveLog.size() + (waveLog.size() == 1 ? " tick" : " ticks") + " (" + state + ")."
			+ (past == 0 ? "" : " The list also holds " + (past == 1 ? "the wave" : "the " + past + " waves") + " before it.");
	}
}
