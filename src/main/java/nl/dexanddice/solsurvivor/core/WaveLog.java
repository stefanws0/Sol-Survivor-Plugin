package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A tick-by-tick record of one wave as the player played it, for the site's wave reviewer to replay; WaveLogWriter turns
 * it into JSON. Like the rest of the plugin it only reads and exports: nothing here is drawn in the game.
 *
 * The RuneLite layer starts it on the first tick of a wave the tracker saw start, feeds it every hitsplat as it comes and
 * one snapshot of the player and the arena's NPCs each game tick, and finishes it when the wave is completed or the player
 * leaves. The log is kept after that, so that the player can copy it once the wave is over. When the next wave starts,
 * the finished log is kept among the past ones as its JSON, so that any wave since the plugin started can still be
 * copied and reviewed, up to MAX_PAST of them. It holds at most MAX_TICKS ticks, so that a wave left running cannot
 * fill the memory; past that it says it was cut.
 *
 * The animation ids are logged as the game gives them, unfiltered: they are how each NPC's attack animation will be
 * confirmed (an open question in the design spec), so nothing here decides which of them matter.
 */
public final class WaveLog
{
	/** The most ticks a log holds: 15 minutes, far longer than a wave lasts. */
	public static final int MAX_TICKS = 1500;

	/** The most past waves kept besides the one under way: two whole runs. As JSON, each is at most a few hundred kB. */
	public static final int MAX_PAST = 24;

	/** An actor's target when it is interacting with nothing. */
	public static final int TARGET_NONE = -1;
	/** An actor's target when it is interacting with a player, which in the Colosseum is the player themself. */
	public static final int TARGET_PLAYER = -2;

	/** How a wave's log ended: with the game's "Wave N completed!", or with the player leaving the arena. */
	public enum End
	{
		COMPLETED("completed"),
		LEFT("left");

		private final String key;

		End(String key)
		{
			this.key = key;
		}

		/** Its name in the log's JSON. */
		public String getKey()
		{
			return key;
		}
	}

	/** A hitsplat: its amount and the game's hitsplat type id, unfiltered. */
	public static final class Hit
	{
		private final int amount;
		private final int type;

		public Hit(int amount, int type)
		{
			this.amount = amount;
			this.type = type;
		}

		public int getAmount()
		{
			return amount;
		}

		public int getType()
		{
			return type;
		}
	}

	/** The player on one tick. */
	public static final class PlayerTick
	{
		private final GridTile tile;
		private final int hitpoints;
		private final int prayerPoints;
		private final List<String> prayers;
		private final int animation;
		private final int target;
		private final int[] worn;

		/** The player without their gear, as when "Include gear and stats" is off. */
		public PlayerTick(GridTile tile, int hitpoints, int prayerPoints, List<String> prayers, int animation, int target)
		{
			this(tile, hitpoints, prayerPoints, prayers, animation, target, null);
		}

		/**
		 * `tile` is null when the player is off the grid; `prayers` names the prayers on, as prayerName gives them;
		 * `animation` is the game's id, -1 for none; `target` is an NPC's index, TARGET_PLAYER or TARGET_NONE; `worn` is the
		 * item id in each of the link's gear slots, in its order (WORN_SLOTS), -1 for an empty one, or null to leave the gear
		 * out.
		 */
		public PlayerTick(GridTile tile, int hitpoints, int prayerPoints, List<String> prayers, int animation, int target, int[] worn)
		{
			this.tile = tile;
			this.hitpoints = hitpoints;
			this.prayerPoints = prayerPoints;
			this.prayers = Collections.unmodifiableList(new ArrayList<>(prayers));
			this.animation = animation;
			this.target = target;
			this.worn = worn == null ? null : worn.clone();
		}

		public GridTile getTile()
		{
			return tile;
		}

		public int getHitpoints()
		{
			return hitpoints;
		}

		public int getPrayerPoints()
		{
			return prayerPoints;
		}

		public List<String> getPrayers()
		{
			return prayers;
		}

		public int getAnimation()
		{
			return animation;
		}

		public int getTarget()
		{
			return target;
		}

		/** The item ids worn, slot by slot in WORN_SLOTS' order, or null when the gear is left out. */
		public int[] getWorn()
		{
			return worn == null ? null : worn.clone();
		}
	}

	/** An NPC on one tick, any NPC in the arena by its raw id: the waves' NPCs, the frems, Solarflare's orbs and the rest. */
	public static final class NpcTick
	{
		private final int index;
		private final int npcId;
		private final GridTile tile;
		private final int animation;
		private final int healthRatio;
		private final int healthScale;
		private final int target;
		private final int[] spotAnims;

		/** An NPC that shows no graphics. */
		public NpcTick(int index, int npcId, GridTile tile, int animation, int healthRatio, int healthScale, int target)
		{
			this(index, npcId, tile, animation, healthRatio, healthScale, target, new int[0]);
		}

		/**
		 * `tile` is its south-west tile; `animation` is the game's id, -1 for none; the health ratio and scale are as the
		 * game gives them, -1 when unknown; `target` is an NPC's index, TARGET_PLAYER or TARGET_NONE; `spotAnims` are the
		 * ids of the graphics it shows, such as a Manticore's orbs, unfiltered.
		 */
		public NpcTick(int index, int npcId, GridTile tile, int animation, int healthRatio, int healthScale, int target, int[] spotAnims)
		{
			this.index = index;
			this.npcId = npcId;
			this.tile = tile;
			this.animation = animation;
			this.healthRatio = healthRatio;
			this.healthScale = healthScale;
			this.target = target;
			this.spotAnims = spotAnims.clone();
		}

		public int getIndex()
		{
			return index;
		}

		public int getNpcId()
		{
			return npcId;
		}

		public GridTile getTile()
		{
			return tile;
		}

		public int getAnimation()
		{
			return animation;
		}

		public int getHealthRatio()
		{
			return healthRatio;
		}

		public int getHealthScale()
		{
			return healthScale;
		}

		public int getTarget()
		{
			return target;
		}

		/** The ids of the graphics it shows on the tick. */
		public int[] getSpotAnims()
		{
			return spotAnims.clone();
		}
	}

	/**
	 * A past wave's log, kept as its JSON once the next wave started. getRun gives the wave under way or the last one the
	 * same way, as its JSON when it was asked for.
	 */
	public static final class Past
	{
		private final int id;
		private final int wave;
		private final int size;
		private final End end;
		private final String json;

		Past(int id, int wave, int size, End end, String json)
		{
			this.id = id;
			this.wave = wave;
			this.size = size;
			this.end = end;
			this.json = json;
		}

		/** The log's id, as getId gave it while it was the one under way. */
		public int getId()
		{
			return id;
		}

		public int getWave()
		{
			return wave;
		}

		/** How many ticks it holds. */
		public int size()
		{
			return size;
		}

		/** How the wave ended, or null when the next wave started before its end was seen. */
		public End getEnd()
		{
			return end;
		}

		/** The log as WaveLogWriter wrote it. */
		public String getJson()
		{
			return json;
		}
	}

	/** One tick of the wave: the player, the NPCs, the hitsplats that came with it and Solarflare's orbs. */
	static final class Tick
	{
		final int t;
		final PlayerTick player;
		final List<Hit> playerHits;
		final List<NpcTick> npcs;
		/** The hitsplats on NPCs, by NPC index, in the order they came. */
		final Map<Integer, List<Hit>> npcHits;
		final List<GridTile> orbs;
		/**
		 * The animations NPCs started since the last tick, [index, animation id] each, in the order they came. Unlike the
		 * animation an NPC shows on a tick, these mark each start, an attack that repeats the one before included, which
		 * is what times the NPCs' attacks.
		 */
		final List<int[]> animations;
		/** The animations the player started since the last tick, in the order they came: each attack, a repeat included. */
		final List<Integer> playerAnimations;
		/** The item ids worn, on the wave's first tick and on each tick the gear changed; null on the others. */
		final int[] worn;
		/** The graphics the game put on the grid's tiles since the last tick, [x, y, graphic id] each, in the order they came. */
		final List<int[]> tileGraphics;

		Tick(int t, PlayerTick player, List<Hit> playerHits, List<NpcTick> npcs, Map<Integer, List<Hit>> npcHits, List<GridTile> orbs,
			List<int[]> animations, List<Integer> playerAnimations, int[] worn, List<int[]> tileGraphics)
		{
			this.t = t;
			this.player = player;
			this.playerHits = playerHits;
			this.npcs = npcs;
			this.npcHits = npcHits;
			this.orbs = orbs;
			this.animations = animations;
			this.playerAnimations = playerAnimations;
			this.worn = worn;
			this.tileGraphics = tileGraphics;
		}
	}

	/** The gear slots `worn` lists, in its order: those the state link names (Equipment), not the player's look. */
	public static final List<String> WORN_SLOTS = Collections.unmodifiableList(Arrays.asList(
		"head", "cape", "amulet", "weapon", "body", "shield", "legs", "gloves", "boots", "ring", "ammo"));

	private Integer wave;
	private int startTick;
	private Map<Modifier, Integer> modifiers = new EnumMap<>(Modifier.class);
	private boolean modifiersComplete;
	private String version = "";
	/** The state link's payload on the wave's first tick, as StatePayload wrote it, or null when it could not be taken. */
	private String startState;
	private final List<Tick> ticks = new ArrayList<>();
	private boolean cut;
	private End end;
	private boolean recording;
	/** The log's id, which tells it apart from the past ones, and the last id given. */
	private int id;
	private int lastId;
	/** The past waves' logs, newest first. */
	private final List<Past> past = new ArrayList<>();
	/** The gear worn on the last tick recorded, so that a tick lists it only when it changed. */
	private int[] lastWorn;

	/** The hitsplats since the last tick was recorded, which go in the next one. */
	private final List<Hit> pendingPlayerHits = new ArrayList<>();
	private final Map<Integer, List<Hit>> pendingNpcHits = new LinkedHashMap<>();
	/** The animations NPCs started since the last tick was recorded, which go in the next one. */
	private final List<int[]> pendingAnimations = new ArrayList<>();
	/** The animations the player started since the last tick was recorded. */
	private final List<Integer> pendingPlayerAnimations = new ArrayList<>();
	/** The graphics put on tiles since the last tick was recorded. */
	private final List<int[]> pendingTileGraphics = new ArrayList<>();

	/**
	 * A wave starts on game tick `gameTick`: the last wave's log goes among the past ones, and the log records this one.
	 * `startState` is the state link's payload on this tick (StatePayload.toJson), or null. The hitsplats already passed on
	 * this tick are kept for it.
	 */
	public void start(int wave, int gameTick, Map<Modifier, Integer> modifiers, boolean modifiersComplete, String version, String startState)
	{
		if (this.wave != null)
		{
			past.add(0, new Past(id, this.wave, ticks.size(), end, WaveLogWriter.toJson(this)));
			while (past.size() > MAX_PAST)
			{
				past.remove(past.size() - 1);
			}
		}
		this.wave = wave;
		this.startTick = gameTick;
		this.modifiers = new EnumMap<>(Modifier.class);
		this.modifiers.putAll(modifiers);
		this.modifiersComplete = modifiersComplete;
		this.version = version;
		this.startState = startState;
		ticks.clear();
		cut = false;
		end = null;
		recording = true;
		id = ++lastId;
		lastWorn = null;
	}

	/** A hitsplat on the player, as it comes; it goes in the next tick recorded. */
	public void onPlayerHit(int amount, int type)
	{
		pendingPlayerHits.add(new Hit(amount, type));
	}

	/** A hitsplat on the NPC at `index`, as it comes; it goes in the next tick recorded. */
	public void onNpcHit(int index, int amount, int type)
	{
		pendingNpcHits.computeIfAbsent(index, i -> new ArrayList<>()).add(new Hit(amount, type));
	}

	/** An NPC started an animation, as it comes; it goes in the next tick recorded. */
	public void onNpcAnimation(int index, int animation)
	{
		pendingAnimations.add(new int[] {index, animation});
	}

	/** The player started an animation, as it comes; it goes in the next tick recorded. */
	public void onPlayerAnimation(int animation)
	{
		pendingPlayerAnimations.add(animation);
	}

	/**
	 * The game put a graphic on a tile, such as where a Javelin Colossus's special lands, as it comes; it goes in the next
	 * tick recorded. One off the grid, or with no tile, is left out.
	 */
	public void onTileGraphic(GridTile tile, int graphic)
	{
		if (tile != null && ArenaGeometry.onGrid(tile))
		{
			pendingTileGraphics.add(new int[] {tile.getX(), tile.getY(), graphic});
		}
	}

	/**
	 * Game tick `gameTick` of the wave: the player and every NPC the game shows. NPCs off the grid are left out, and the
	 * tiles of Solarflare's orbs are taken from the orb NPCs. The player's gear is kept for the tick only when it changed
	 * since the tick before. A tick already recorded, or one past MAX_TICKS, is passed over; either way the hitsplats since
	 * the last tick are used up.
	 */
	public void record(int gameTick, PlayerTick player, List<NpcTick> npcs)
	{
		if (!recording)
		{
			discardPending();
			return;
		}
		int t = gameTick - startTick;
		if (!ticks.isEmpty() && ticks.get(ticks.size() - 1).t >= t)
		{
			discardPending();
			return;
		}
		if (ticks.size() >= MAX_TICKS)
		{
			cut = true;
			discardPending();
			return;
		}
		List<NpcTick> onGrid = new ArrayList<>();
		List<GridTile> orbs = new ArrayList<>();
		for (NpcTick n : npcs)
		{
			if (n.getTile() == null || !ArenaGeometry.onGrid(n.getTile()))
			{
				continue;
			}
			onGrid.add(n);
			if (n.getNpcId() == NpcKind.SOLARFLARE_ORB_ID)
			{
				orbs.add(n.getTile());
			}
		}
		Map<Integer, List<Hit>> npcHits = new LinkedHashMap<>();
		for (Map.Entry<Integer, List<Hit>> e : pendingNpcHits.entrySet())
		{
			npcHits.put(e.getKey(), Collections.unmodifiableList(new ArrayList<>(e.getValue())));
		}
		int[] worn = player == null ? null : player.getWorn();
		int[] changed = worn != null && !Arrays.equals(worn, lastWorn) ? worn : null;
		if (worn != null)
		{
			lastWorn = worn;
		}
		ticks.add(new Tick(t, player, Collections.unmodifiableList(new ArrayList<>(pendingPlayerHits)), Collections.unmodifiableList(onGrid),
			Collections.unmodifiableMap(npcHits), Collections.unmodifiableList(orbs), Collections.unmodifiableList(new ArrayList<>(pendingAnimations)),
			Collections.unmodifiableList(new ArrayList<>(pendingPlayerAnimations)), changed, Collections.unmodifiableList(new ArrayList<>(pendingTileGraphics))));
		discardPending();
	}

	/** Forget the hitsplats since the last tick: the RuneLite layer calls this on each tick it does not record. */
	public void discardPending()
	{
		pendingPlayerHits.clear();
		pendingNpcHits.clear();
		pendingAnimations.clear();
		pendingPlayerAnimations.clear();
		pendingTileGraphics.clear();
	}

	/** The wave ended: the log stops recording and is kept until the next wave starts. */
	public void finish(End how)
	{
		if (recording)
		{
			recording = false;
			end = how;
		}
		discardPending();
	}

	/** Forget the log and the past ones altogether: the plugin or its log was switched off. */
	public void clear()
	{
		wave = null;
		ticks.clear();
		cut = false;
		end = null;
		recording = false;
		modifiers = new EnumMap<>(Modifier.class);
		modifiersComplete = false;
		version = "";
		startState = null;
		id = 0;
		lastWorn = null;
		past.clear();
		discardPending();
	}

	/** Whether a wave is being recorded. */
	public boolean isRecording()
	{
		return recording;
	}

	/** Whether there is a log to copy: a wave was started, while recording or after. */
	public boolean hasLog()
	{
		return wave != null;
	}

	/** The log's id, given when its wave started, or 0 when there is none. */
	public int getId()
	{
		return id;
	}

	/** The past waves' logs, newest first. */
	public List<Past> getPast()
	{
		return Collections.unmodifiableList(past);
	}

	/**
	 * The newest run's logs, oldest first, for RunLogWriter: the newest log, the wave under way or the last one, and going
	 * back from it each log whose wave is the one before the wave of the log after it. The run starts at wave 1, which has
	 * no wave before it, so that it never reaches back into the run before; or, when the plugin did not log the run from
	 * wave 1, such as when it started mid-run, at the oldest wave of the series it logged. A wave missing from the series
	 * or logged twice parts it there, and the run is the newer part. How a wave ended does not part a run: the next run
	 * starts at wave 1 again, which parts the two already. The past waves kept, MAX_PAST, are more than a run's 12 waves
	 * (wave 12 is Sol Heredit's), so that a run is always whole. Each log is as Copy wave log copies it: a past one as
	 * its JSON, and the newest as WaveLogWriter writes it now. Empty when there is no log.
	 */
	public List<Past> getRun()
	{
		List<Past> run = new ArrayList<>();
		if (wave == null)
		{
			return run;
		}
		run.add(new Past(id, wave, ticks.size(), end, WaveLogWriter.toJson(this)));
		int next = wave;
		for (Past p : past)
		{
			if (p.getWave() != next - 1)
			{
				break;
			}
			run.add(0, p);
			next = p.getWave();
		}
		return Collections.unmodifiableList(run);
	}

	/** The wave logged, or null when there is none. */
	public Integer getWave()
	{
		return wave;
	}

	/** How many ticks the log holds. */
	public int size()
	{
		return ticks.size();
	}

	/** Whether ticks were left out because the log was full. */
	public boolean isCut()
	{
		return cut;
	}

	/** How the wave ended, or null while it is under way or when the log was never finished. */
	public End getEnd()
	{
		return end;
	}

	Map<Modifier, Integer> getModifiers()
	{
		return Collections.unmodifiableMap(modifiers);
	}

	boolean isModifiersComplete()
	{
		return modifiersComplete;
	}

	String getVersion()
	{
		return version;
	}

	String getStartState()
	{
		return startState;
	}

	List<Tick> getTicks()
	{
		return Collections.unmodifiableList(ticks);
	}

	/**
	 * A prayer's name in the log, from the name of RuneLite's Prayer constant: the three protection prayers are "magic",
	 * "ranged" and "melee", as the site names them, and every other prayer is its constant's name in lower case, such as
	 * "piety" or "rp_ancient_strength".
	 */
	public static String prayerName(String prayerConstant)
	{
		switch (prayerConstant)
		{
			case "PROTECT_FROM_MAGIC":
				return "magic";
			case "PROTECT_FROM_MISSILES":
				return "ranged";
			case "PROTECT_FROM_MELEE":
				return "melee";
			default:
				return prayerConstant.toLowerCase(Locale.ROOT);
		}
	}
}
