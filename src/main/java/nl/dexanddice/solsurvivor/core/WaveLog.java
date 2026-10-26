package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A tick-by-tick record of one wave as the player played it, for the site's wave reviewer to replay; WaveLogWriter turns
 * it into JSON. Like the rest of the plugin it only reads and exports: nothing here is drawn in the game.
 *
 * The RuneLite layer starts it on the first tick of a wave the tracker saw start, feeds it every hitsplat as it comes, each
 * projectile as it first sees it, and one snapshot of the player, the arena's NPCs and the game's Colosseum counters
 * (ColosseumCounter) each game tick, and finishes it when the wave is completed or the player
 * leaves. The log is kept after that, so that the player can copy it once the wave is over. When the next wave starts,
 * the finished log is kept among the past ones, its JSON written the first time it is asked for (snapshot), so that any
 * wave since the plugin started can still be copied and reviewed, up to MAX_PAST of them. Nothing is written on the
 * wave's first tick, the one tick a player least wants a hitch on, and a log that is written may be written on another
 * thread than the client's, as a snapshot never changes. It holds at most MAX_TICKS ticks, so that a wave left running cannot
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
	 * A projectile the game showed, as the RuneLite layer first saw it: its graphic's id, where it flies from and to, and
	 * when it sets off and lands. Each end is an actor, an NPC by its index or TARGET_PLAYER, or, where the projectile has
	 * no actor at that end, such as a Javelin Colossus's special aimed at a tile, TARGET_NONE with the tile it flies from
	 * or to (null off the grid). Like the animations, the ids are logged as the game gives them, unfiltered.
	 */
	public static final class Projectile
	{
		private final int id;
		private final int from;
		private final GridTile fromTile;
		private final int to;
		private final GridTile toTile;
		private final int start;
		private final int end;

		/**
		 * `start` and `end` are the client cycles (20 ms, 30 to a game tick) from the tick it is logged on to when it sets
		 * off and when it lands: below 0 for one that set off before that tick, as one first seen a tick late may have.
		 * A tile is only kept for an end with no actor.
		 */
		public Projectile(int id, int from, GridTile fromTile, int to, GridTile toTile, int start, int end)
		{
			this.id = id;
			this.from = from;
			this.fromTile = from == TARGET_NONE ? fromTile : null;
			this.to = to;
			this.toTile = to == TARGET_NONE ? toTile : null;
			this.start = start;
			this.end = end;
		}

		public int getId()
		{
			return id;
		}

		/** The actor it flies from: an NPC's index, TARGET_PLAYER, or TARGET_NONE when it has none. */
		public int getFrom()
		{
			return from;
		}

		/** The tile it flies from when it has no actor there, or null. */
		public GridTile getFromTile()
		{
			return fromTile;
		}

		/** The actor it flies to: an NPC's index, TARGET_PLAYER, or TARGET_NONE when it has none. */
		public int getTo()
		{
			return to;
		}

		/** The tile it flies to when it has no actor there, or null. */
		public GridTile getToTile()
		{
			return toTile;
		}

		public int getStart()
		{
			return start;
		}

		public int getEnd()
		{
			return end;
		}

		/** Whether either end says where it is: an actor, or a tile on the grid. */
		boolean isPlaced()
		{
			return from != TARGET_NONE || to != TARGET_NONE || (fromTile != null && ArenaGeometry.onGrid(fromTile))
				|| (toTile != null && ArenaGeometry.onGrid(toTile));
		}
	}

	/**
	 * A past wave's log, kept as its JSON once the next wave started, with the state links' payloads on its first tick and
	 * on the tick its reinforcements came, and the glory it earned. getRun and getRuns give the wave under way or the last
	 * one the same way, its JSON written when it is asked for, so that the panel can list the waves every tick without
	 * writing any. A wave saved in an earlier session (WaveStore) comes back as one too, its JSON read with it.
	 */
	public static final class Past
	{
		private final int id;
		private final int wave;
		private final int size;
		private final End end;
		private final boolean recording;
		private final Supplier<String> json;
		private final String startState;
		private final String reinforcementState;
		private final Integer glory;

		Past(int id, int wave, int size, End end, boolean recording, Supplier<String> json, String startState, String reinforcementState,
			Integer glory)
		{
			this.id = id;
			this.wave = wave;
			this.size = size;
			this.end = end;
			this.recording = recording;
			this.json = json;
			this.startState = startState;
			this.reinforcementState = reinforcementState;
			this.glory = glory;
		}

		/** The same wave under the id `newId`, as WaveLog.restore gives a saved one. */
		Past withId(int newId)
		{
			return new Past(newId, wave, size, end, recording, json, startState, reinforcementState, glory);
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

		/** Whether this is the wave under way, still recording. */
		public boolean isRecording()
		{
			return recording;
		}

		/** The log as WaveLogWriter wrote it. */
		public String getJson()
		{
			return json.get();
		}

		/** The state link's payload on the wave's first tick (StatePayload.toJson), or null when it could not be taken. */
		public String getStartState()
		{
			return startState;
		}

		/** The state link's payload on the tick the wave's reinforcements came, or null when they were not seen to come. */
		public String getReinforcementState()
		{
			return reinforcementState;
		}

		/** The glory the wave earned, by the game's own counter (WaveLog.getGlory), or null when that is not known. */
		public Integer getGlory()
		{
			return glory;
		}
	}

	/**
	 * One tick of the wave: the player, the NPCs, the hitsplats that came with it, Solarflare's orbs, and the rest that
	 * came since the tick before.
	 */
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
		/** The projectiles first seen since the last tick, in the order they were seen. */
		final List<Projectile> projectiles;
		/** The game's counters (ColosseumCounter) that changed since the last tick, every one on the wave's first tick. */
		final Map<ColosseumCounter, Integer> counters;

		/** A tick with no projectiles and no counters, as a test builds one. */
		Tick(int t, PlayerTick player, List<Hit> playerHits, List<NpcTick> npcs, Map<Integer, List<Hit>> npcHits, List<GridTile> orbs,
			List<int[]> animations, List<Integer> playerAnimations, int[] worn, List<int[]> tileGraphics)
		{
			this(t, player, playerHits, npcs, npcHits, orbs, animations, playerAnimations, worn, tileGraphics, Collections.<Projectile>emptyList(),
				Collections.<ColosseumCounter, Integer>emptyMap());
		}

		Tick(int t, PlayerTick player, List<Hit> playerHits, List<NpcTick> npcs, Map<Integer, List<Hit>> npcHits, List<GridTile> orbs,
			List<int[]> animations, List<Integer> playerAnimations, int[] worn, List<int[]> tileGraphics, List<Projectile> projectiles,
			Map<ColosseumCounter, Integer> counters)
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
			this.projectiles = projectiles;
			this.counters = counters;
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
	/**
	 * The same payload with the gear's levels left out (Equipment.withoutLevels), for the log the player shares
	 * (WaveLogWriter.toShare), or null when it could not be taken.
	 */
	private String shareState;
	/** The state link's payload on the tick the reinforcements came, or null before then. */
	private String reinforcementState;
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
	/** The game's counters on the last tick recorded, so that a tick lists only those that changed; null before the first. */
	private int[] lastCounters;
	/** The game's glory counter on the first tick recorded that read it, null before then, and on the last. */
	private Integer firstGlory;
	private int lastGlory;

	/** The hitsplats since the last tick was recorded, which go in the next one. */
	private final List<Hit> pendingPlayerHits = new ArrayList<>();
	private final Map<Integer, List<Hit>> pendingNpcHits = new LinkedHashMap<>();
	/** The animations NPCs started since the last tick was recorded, which go in the next one. */
	private final List<int[]> pendingAnimations = new ArrayList<>();
	/** The animations the player started since the last tick was recorded. */
	private final List<Integer> pendingPlayerAnimations = new ArrayList<>();
	/** The graphics put on tiles since the last tick was recorded. */
	private final List<int[]> pendingTileGraphics = new ArrayList<>();
	/** The projectiles first seen since the last tick was recorded. */
	private final List<Projectile> pendingProjectiles = new ArrayList<>();

	/**
	 * A wave starts on game tick `gameTick`: the last wave's log goes among the past ones, and the log records this one.
	 * `startState` is the state link's payload on this tick (StatePayload.toJson), or null. The hitsplats already passed on
	 * this tick are kept for it.
	 */
	public void start(int wave, int gameTick, Map<Modifier, Integer> modifiers, boolean modifiersComplete, String version, String startState)
	{
		start(wave, gameTick, modifiers, modifiersComplete, version, startState, startState);
	}

	/**
	 * A wave starts, as start above, with `shareState` the state link's payload on this tick without the gear's levels, for
	 * the log the player shares, or null.
	 */
	public void start(int wave, int gameTick, Map<Modifier, Integer> modifiers, boolean modifiersComplete, String version, String startState,
		String shareState)
	{
		if (this.wave != null)
		{
			WaveLog done = snapshot();
			past.add(0, new Past(id, this.wave, ticks.size(), end, false, once(() -> WaveLogWriter.toJson(done)), this.startState, reinforcementState,
				getGlory()));
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
		this.shareState = shareState;
		reinforcementState = null;
		ticks.clear();
		cut = false;
		end = null;
		recording = true;
		id = ++lastId;
		lastWorn = null;
		lastCounters = null;
		firstGlory = null;
		lastGlory = 0;
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
	 * A projectile the RuneLite layer saw for the first time, as it comes; it goes in the next tick recorded. One with
	 * neither an actor nor a tile on the grid at either end is left out.
	 */
	public void onProjectile(Projectile projectile)
	{
		if (projectile.isPlaced())
		{
			pendingProjectiles.add(projectile);
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
		record(gameTick, player, npcs, null);
	}

	/**
	 * Game tick `gameTick` of the wave, as record above, with `counters` the game's counters on it, one for each
	 * ColosseumCounter in its order, or null when they could not be read. The tick keeps those that changed since the tick
	 * before, and every one on the wave's first tick.
	 */
	public void record(int gameTick, PlayerTick player, List<NpcTick> npcs, int[] counters)
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
		Map<ColosseumCounter, Integer> changedCounters = new EnumMap<>(ColosseumCounter.class);
		if (counters != null && counters.length == ColosseumCounter.values().length)
		{
			for (ColosseumCounter c : ColosseumCounter.values())
			{
				int value = counters[c.ordinal()];
				if (lastCounters == null || lastCounters[c.ordinal()] != value)
				{
					changedCounters.put(c, value);
				}
			}
			lastCounters = counters.clone();
			int glory = counters[ColosseumCounter.CURRENT_GLORY.ordinal()];
			if (firstGlory == null)
			{
				firstGlory = glory;
			}
			lastGlory = glory;
		}
		ticks.add(new Tick(t, player, Collections.unmodifiableList(new ArrayList<>(pendingPlayerHits)), Collections.unmodifiableList(onGrid),
			Collections.unmodifiableMap(npcHits), Collections.unmodifiableList(orbs), Collections.unmodifiableList(new ArrayList<>(pendingAnimations)),
			Collections.unmodifiableList(new ArrayList<>(pendingPlayerAnimations)), changed, Collections.unmodifiableList(new ArrayList<>(pendingTileGraphics)),
			Collections.unmodifiableList(new ArrayList<>(pendingProjectiles)), Collections.unmodifiableMap(changedCounters)));
		discardPending();
	}

	/**
	 * Forget the hitsplats and the rest since the last tick: the RuneLite layer calls this on each tick it does not
	 * record.
	 */
	public void discardPending()
	{
		pendingPlayerHits.clear();
		pendingNpcHits.clear();
		pendingAnimations.clear();
		pendingPlayerAnimations.clear();
		pendingTileGraphics.clear();
		pendingProjectiles.clear();
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
		shareState = null;
		reinforcementState = null;
		id = 0;
		lastWorn = null;
		lastCounters = null;
		firstGlory = null;
		lastGlory = 0;
		past.clear();
		discardPending();
	}

	/**
	 * The wave's reinforcements came on this tick: `state` is the state link's payload on it (StatePayload.toJson), which
	 * the panel opens on the site. Only the first is kept, while the wave records.
	 */
	public void reinforcementsCame(String state)
	{
		if (recording && reinforcementState == null)
		{
			reinforcementState = state;
		}
	}

	/** Whether the reinforcements' state was taken this wave. */
	public boolean hasReinforcementState()
	{
		return reinforcementState != null;
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
		List<List<Past>> runs = getRuns();
		return runs.isEmpty() ? Collections.<Past>emptyList() : runs.get(0);
	}

	/**
	 * Every run logged, newest first, each its logs oldest first: the newest is getRun's, and each run before it is
	 * parted from the next the same way, where a log's wave is not the one before the wave of the log after it. The
	 * wave under way or the last one comes as it is now (snapshot), its JSON written when it is asked for, and the waves
	 * restored from an earlier session after this session's. Empty when there is no log, past or present.
	 */
	public List<List<Past>> getRuns()
	{
		List<Past> newestFirst = new ArrayList<>();
		if (wave != null)
		{
			newestFirst.add(current());
		}
		newestFirst.addAll(past);
		List<List<Past>> runs = new ArrayList<>();
		List<Past> run = new ArrayList<>();
		for (Past p : newestFirst)
		{
			if (!run.isEmpty() && p.getWave() != run.get(0).getWave() - 1)
			{
				runs.add(Collections.unmodifiableList(run));
				run = new ArrayList<>();
			}
			run.add(0, p);
		}
		if (!run.isEmpty())
		{
			runs.add(Collections.unmodifiableList(run));
		}
		return Collections.unmodifiableList(runs);
	}

	/**
	 * The wave under way or the last one as a Past, as it is now (snapshot), its JSON written when it is asked for, on
	 * any thread: what getRuns lists first, and what the plugin saves as the wave ends (WaveStore). Null when there is no
	 * log.
	 */
	public Past current()
	{
		if (wave == null)
		{
			return null;
		}
		WaveLog now = snapshot();
		return new Past(id, wave, ticks.size(), end, recording, once(() -> WaveLogWriter.toJson(now)), startState, reinforcementState,
			getGlory());
	}

	/**
	 * Waves saved in an earlier session (WaveStore), newest first, go behind the past ones, each under a new id, so that
	 * Past waves lists them as it lists this session's: getRuns parts them into runs the same way. Those past MAX_PAST
	 * are let go.
	 */
	public void restore(List<Past> saved)
	{
		for (Past p : saved)
		{
			if (past.size() >= MAX_PAST)
			{
				break;
			}
			past.add(p.withId(++lastId));
		}
	}

	/**
	 * The glory the wave earned by the game's own counter, COLOSSEUM_CURRENT_GLORY: what it read on the last tick
	 * recorded less what it read on the first, which matched the site's sums by the game's rules on every wave finished
	 * in the user's shared runs of 25 and 26 October (the site's test/viewer/runGlory.test.ts). Only for a wave completed:
	 * a wave the player died in or left earns none, and one under way has not earned its yet. Null too when the counter
	 * could not be read.
	 */
	public Integer getGlory()
	{
		return end == End.COMPLETED && firstGlory != null ? lastGlory - firstGlory : null;
	}

	/**
	 * The log as it is now, which records nothing more: what WaveLogWriter writes of it, copied, so that its JSON can be
	 * written later, on any thread, while this log goes on. The ticks themselves never change once recorded, so only
	 * their list is copied.
	 */
	public WaveLog snapshot()
	{
		WaveLog copy = new WaveLog();
		copy.wave = wave;
		copy.startTick = startTick;
		copy.modifiers = new EnumMap<>(Modifier.class);
		copy.modifiers.putAll(modifiers);
		copy.modifiersComplete = modifiersComplete;
		copy.version = version;
		copy.startState = startState;
		copy.shareState = shareState;
		copy.reinforcementState = reinforcementState;
		copy.ticks.addAll(ticks);
		copy.cut = cut;
		copy.end = end;
		copy.id = id;
		copy.lastId = lastId;
		copy.firstGlory = firstGlory;
		copy.lastGlory = lastGlory;
		return copy;
	}

	/**
	 * `make`'s text, made the first time it is asked for and kept: a log's JSON, which may be asked for on the client
	 * thread or on OkHttp's, hence the lock. Once made, `make` is let go, and with it the snapshot it writes.
	 */
	static Supplier<String> once(Supplier<String> make)
	{
		return new Supplier<String>()
		{
			private Supplier<String> maker = make;
			private String text;

			@Override
			public synchronized String get()
			{
				if (text == null)
				{
					text = maker.get();
					maker = null;
				}
				return text;
			}
		};
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

	String getShareState()
	{
		return shareState;
	}

	/** The state link's payload on the tick the reinforcements came, or null before then. */
	public String getReinforcementState()
	{
		return reinforcementState;
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
