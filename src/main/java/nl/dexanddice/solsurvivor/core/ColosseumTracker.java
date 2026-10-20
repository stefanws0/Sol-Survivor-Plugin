package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the plugin remembers of a Colosseum run between ticks, fed the game's events by the RuneLite layer and free of
 * RuneLite itself, so that its tests run on mocked game data. It follows the wave from the game's chat messages, which
 * NPCs spawned after the wave's first tick (the reinforcements), the orbs each Manticore has shown, each NPC's attacks
 * and health bar, the hits that dealt the player damage this wave (Doom's stacks), the molten sand on the floor, and the
 * modifiers taken; capture() turns that and the NPCs and player of a tick into an ArenaState.
 *
 * It only reads: nothing here, or in the plugin, acts on the game or draws in it.
 */
public final class ColosseumTracker
{
	/** The last wave Sol Survivor plans: wave 12 is Sol Heredit. */
	public static final int LAST_WAVE = 11;

	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern WAVE_START = Pattern.compile("Wave: (\\d+)");
	private static final Pattern WAVE_DONE = Pattern.compile("Wave (\\d+) completed!");
	private static final String SOL_START = "Sol Heredit jumps down from his seat";

	/**
	 * The graphic on a Javelin Colossus's special's landing tile as it lands (RuneLite's SpotanimID
	 * WILD_FALLOFF_METEOR_BLAST_RED), with Reentry or without: in the user's logs it comes on the tile of the shadow, three
	 * ticks after it.
	 */
	public static final int JAVELIN_LANDING_GRAPHIC = 2265;
	/** Molten Sand, the scenery that Reentry's and Volatility III's sand is (the OSRS Wiki's Molten Sand page). */
	public static final int MOLTEN_SAND_ID = 50746;
	/** A hitsplat that heals (RuneLite's HitsplatID.HEAL). */
	public static final int HEAL_HITSPLAT = 6;
	/**
	 * The hitsplats with an amount that is not damage taken, by RuneLite's HitsplatID: a heal, the cyan ones, a prayer
	 * drain, sanity's drain and restore, and Doom's own. Any other with an amount above 0 counts as a stack of Doom.
	 */
	private static final Set<Integer> NOT_DAMAGE = new HashSet<>(Arrays.asList(HEAL_HITSPLAT, 11, 15, 60, 71, 72, 73));
	/**
	 * The tiles a Javelin special's landing turns to sand at each Reentry tier, 0 to 3, as [dx, dy] from the landing tile:
	 * the site's REENTRY_SAND_OFFSETS (src/data/calibration.ts), from the wiki, unconfirmed in game.
	 */
	private static final int[][][] SAND_OFFSETS = {{}, {{0, 0}}, {{0, 0}, {-1, 1}}, {{0, 0}, {-1, 1}, {-1, 0}}};

	private Integer wave;
	private boolean waveStarted;
	private int waveStartTick = -1;

	/** The NPCs, by index, that spawned after their wave's first tick. */
	private final Set<Integer> reinforcements = new HashSet<>();
	/** Whether a reinforcement has come this wave, even one that has since died. */
	private boolean reinforcementsArrived;
	private final Map<Integer, OrbOrder> orbs = new HashMap<>();
	/** The tick each NPC's last attack animation started on, by index. */
	private final Map<Integer, Integer> lastAttack = new HashMap<>();
	/**
	 * The attacks each NPC has made, by index, for those whose count is known: it spawned while the plugin watched, or for
	 * a Javelin Colossus the plugin saw a special. Kept across the wave's start, since the game may spawn the wave's NPCs
	 * before or after its message on the same tick; an NPC's despawn clears its entries.
	 */
	private final Map<Integer, Integer> attackCounts = new HashMap<>();
	/** Each NPC's health bar as the game last showed it, [ratio, scale], by index; an NPC's despawn or a heal clears it. */
	private final Map<Integer, int[]> healthBars = new HashMap<>();
	/** The hitsplats on the player that dealt damage since the wave the plugin saw start: Doom's stacks. */
	private int damageTaken;
	/** The sand the Javelins' landings laid this wave below Reentry II, which goes when the wave does. */
	private final Set<GridTile> waveSand = new LinkedHashSet<>();
	/** The sand they laid at Reentry II or III, which lasts the run. */
	private final Set<GridTile> lastingSand = new LinkedHashSet<>();
	/** The Molten Sand objects in the scene, by tile, each with how many stand there. */
	private final Map<GridTile, Integer> sandObjects = new LinkedHashMap<>();

	private final Set<Modifier> taken = EnumSet.noneOf(Modifier.class);
	private boolean modifiersComplete;
	private final List<Modifier> options = new ArrayList<>(3);

	/** Forget the run: the player left the Colosseum, hopped or logged out. */
	public void reset()
	{
		wave = null;
		waveStarted = false;
		waveStartTick = -1;
		taken.clear();
		modifiersComplete = false;
		options.clear();
		lastAttack.clear();
		attackCounts.clear();
		healthBars.clear();
		lastingSand.clear();
		sandObjects.clear();
		resetWave();
	}

	/**
	 * A wave starts or ends: its reinforcements, orbs and Doom stacks go (the wiki: Doom's stacks clear at the end of the
	 * wave), and so does the sand laid below Reentry II (the wiki's Javelin Colossus page).
	 */
	private void resetWave()
	{
		reinforcements.clear();
		reinforcementsArrived = false;
		orbs.clear();
		damageTaken = 0;
		waveSand.clear();
	}

	/** A game message, on the tick it came. */
	public void onGameMessage(String message, int tick)
	{
		String text = TAG.matcher(message).replaceAll("");
		Matcher m;
		if (text.contains(SOL_START))
		{
			startWave(LAST_WAVE + 1, tick);
		}
		else if ((m = WAVE_START.matcher(text)).find())
		{
			startWave(Integer.parseInt(m.group(1)), tick);
		}
		else if ((m = WAVE_DONE.matcher(text)).find())
		{
			wave = Integer.parseInt(m.group(1)) + 1;
			waveStarted = false;
			resetWave();
		}
	}

	private void startWave(int number, int tick)
	{
		wave = number;
		waveStarted = true;
		waveStartTick = tick;
		resetWave();
	}

	/** Whether this tick is the first of a wave the plugin saw start. */
	public boolean isWaveStartTick(int tick)
	{
		return waveStarted && tick == waveStartTick;
	}

	/** Whether a wave is under way that the plugin saw start. */
	public boolean isWaveStarted()
	{
		return waveStarted;
	}

	/** Whether a reinforcement has spawned in the wave under way, even one that has since died. */
	public boolean isReinforcementsArrived()
	{
		return reinforcementsArrived;
	}

	/** The wave under way, or the next one between waves; null when the plugin has not seen a wave's message yet. */
	public Integer getWave()
	{
		return wave;
	}

	/** Whether the modifiers taken are all known: see ArenaState.isModifiersComplete. */
	public boolean isModifiersComplete()
	{
		return modifiersComplete;
	}

	/** An NPC spawned, on the tick it did. One that spawns after its wave's first tick is a reinforcement. */
	public void onNpcSpawned(int index, int npcId, int tick)
	{
		if (NpcKind.forNpcId(npcId, false) == null)
		{
			return;
		}
		if (waveStarted && tick != waveStartTick)
		{
			reinforcements.add(index);
			reinforcementsArrived = true;
		}
		if (npcId == NpcKind.MANTICORE_ID)
		{
			orbs.put(index, new OrbOrder());
		}
		else
		{
			lastAttack.remove(index);
			attackCounts.put(index, 0);
		}
	}

	public void onNpcDespawned(int index)
	{
		reinforcements.remove(index);
		orbs.remove(index);
		lastAttack.remove(index);
		attackCounts.remove(index);
		healthBars.remove(index);
	}

	/**
	 * An NPC's health bar on this tick: its ratio of `scale`, or a ratio below 0 while the game hides the bar, which it
	 * does some ticks after the last hit. The last bar shown is kept until the NPC despawns or is healed: in the user's
	 * logs no bar came back higher after it hid, for up to 71 ticks, so the NPCs did not heal unseen in that time.
	 */
	public void onHealthBar(int index, int ratio, int scale)
	{
		if (ratio >= 0 && scale > 0 && ratio <= scale)
		{
			healthBars.put(index, new int[] {ratio, scale});
		}
	}

	/**
	 * A hitsplat on an NPC. A heal forgets its bar, which no longer says how hurt it is, until the game shows the bar again:
	 * the link then gives the NPC at full health rather than more hurt than it is.
	 */
	public void onNpcHit(int index, int amount, int type)
	{
		if (type == HEAL_HITSPLAT)
		{
			healthBars.remove(index);
		}
	}

	/**
	 * A hitsplat on the player. Doom's stacks are the hits that dealt damage in the wave (the wiki: a stack for each
	 * instance of damage taken), so each one with an amount above 0 counts, while a wave the plugin saw start is under
	 * way; a block, a heal or a drain does not (NOT_DAMAGE). Which hitsplats the game counts is unconfirmed: those of
	 * Reentry's sand and Solarflare count here, as the site's Doom counts them.
	 */
	public void onPlayerHit(int amount, int type)
	{
		if (waveStarted && amount > 0 && !NOT_DAMAGE.contains(type))
		{
			damageTaken++;
		}
	}

	/**
	 * A graphic on a tile, with Reentry's tier at the time. A Javelin special landing under Reentry lays sand on its tile
	 * and, from tier II, the tiles SAND_OFFSETS adds, those on the grid: below tier II for the rest of the wave, from it
	 * for good. A tile the game moves to another, when one is a wall or a pillar, is not known here; the Molten Sand
	 * objects show it, if they show at all.
	 */
	public void onTileGraphic(GridTile tile, int graphicId, int reentryTier)
	{
		if (graphicId != JAVELIN_LANDING_GRAPHIC || tile == null || reentryTier < 1 || reentryTier > 3)
		{
			return;
		}
		for (int[] offset : SAND_OFFSETS[reentryTier])
		{
			GridTile sand = new GridTile(tile.getX() + offset[0], tile.getY() + offset[1]);
			if (ArenaGeometry.onGrid(sand))
			{
				(reentryTier >= 2 ? lastingSand : waveSand).add(sand);
			}
		}
	}

	/** A Molten Sand object came into the scene on `tile`, or left it; any other object is ignored by the caller. */
	public void onSandObject(GridTile tile, boolean spawned)
	{
		if (tile == null || !ArenaGeometry.onGrid(tile))
		{
			return;
		}
		int count = sandObjects.getOrDefault(tile, 0) + (spawned ? 1 : -1);
		if (count > 0)
		{
			sandObjects.put(tile, count);
		}
		else
		{
			sandObjects.remove(tile);
		}
	}

	/** The scene is loading again, which spawns its objects anew: the Molten Sand objects are counted from there. */
	public void onSceneLoading()
	{
		sandObjects.clear();
	}

	/**
	 * An NPC started an animation, on the tick it did. An attack (AttackAnimations, unconfirmed) restarts its attack timer
	 * and adds to its count. A Javelin Colossus's special is its 5th attack (JAVELIN_SPECIAL_EVERY on the site), so it
	 * sets the count to the next multiple of 5, which fixes the phase of its specials even when the plugin missed its
	 * spawn.
	 */
	public void onNpcAnimation(int index, int npcId, int animationId, int tick)
	{
		if (!AttackAnimations.isAttack(npcId, animationId))
		{
			return;
		}
		lastAttack.put(index, tick);
		Integer count = attackCounts.get(index);
		if (AttackAnimations.isSpecial(npcId, animationId))
		{
			int next = count == null ? 1 : count + 1;
			attackCounts.put(index, (next + 4) / 5 * 5);
		}
		else if (count != null)
		{
			attackCounts.put(index, count + 1);
		}
	}

	/** An orb graphic on a Manticore this tick. */
	public void onOrb(int index, OrbStyle style)
	{
		orbs.computeIfAbsent(index, i -> new OrbOrder()).see(style);
	}

	/**
	 * The modifier selector opened between waves: the game ids of the three on offer, and the mask of those already taken,
	 * which settles every modifier taken so far.
	 */
	public void onModifierOptions(int first, int second, int third, int takenMask)
	{
		options.clear();
		options.add(Modifier.forId(first));
		options.add(Modifier.forId(second));
		options.add(Modifier.forId(third));
		taken.addAll(Modifier.forMask(takenMask));
		modifiersComplete = true;
	}

	/**
	 * The three modifiers the selector offers while it is open, in its order, each null for an id this plugin does not
	 * know; empty while it is closed.
	 */
	public List<Modifier> getOptions()
	{
		return Collections.unmodifiableList(new ArrayList<>(options));
	}

	/** The selector closed with the option at `choice`, 1 to 3; 0 when none was picked. */
	public void onModifierChosen(int choice)
	{
		if (choice >= 1 && choice <= options.size() && options.get(choice - 1) != null)
		{
			taken.add(options.get(choice - 1));
		}
		options.clear();
	}

	/**
	 * The arena on this tick: the NPCs the game shows, the player's tile and the modifiers' tiers, with what the tracker
	 * remembers. An ExportException says why there is nothing to export, such as no NPCs or Sol Heredit's wave.
	 */
	public ArenaState capture(int tick, GridTile player, List<SeenNpc> seen, ModifierLevels levels) throws ExportException
	{
		if (wave != null && wave > LAST_WAVE)
		{
			throw new ExportException("Sol Survivor plans waves 1 to 11, not Sol Heredit's.");
		}
		if (player == null || !ArenaGeometry.onGrid(player))
		{
			throw new ExportException("You are not in the Colosseum's arena.");
		}

		Map<Modifier, Integer> modifiers = modifiers(levels);
		boolean mm3 = Integer.valueOf(3).equals(modifiers.get(Modifier.MANTIMAYHEM));
		boolean waveStart = isWaveStartTick(tick);

		List<ExportNpc> npcs = new ArrayList<>();
		List<GridTile> solarflareOrbs = new ArrayList<>();
		boolean arrived = reinforcementsArrived;
		for (SeenNpc n : seen)
		{
			if (n.getNpcId() == NpcKind.SOLARFLARE_ORB_ID && ArenaGeometry.onGrid(n.getTile()))
			{
				solarflareOrbs.add(n.getTile());
				continue;
			}
			NpcKind kind = NpcKind.forNpcId(n.getNpcId(), isReinforcement(n));
			if (kind == null || !ArenaGeometry.onGrid(n.getTile()))
			{
				continue;
			}
			// The Jaguar, the Minotaur and the second shaman only ever come as reinforcements.
			arrived |= kind == NpcKind.JAGUAR_WARRIOR || kind == NpcKind.MINOTAUR || kind == NpcKind.REINFORCEMENT_SHAMAN;
			String code = null;
			Integer sinceAttack = null;
			Integer attacks = null;
			if (kind == NpcKind.MANTICORE)
			{
				OrbOrder order = orbs.get(n.getIndex());
				code = order == null ? "u" : order.losCode(mm3);
			}
			else if (!waveStart)
			{
				Integer last = lastAttack.get(n.getIndex());
				sinceAttack = last == null ? null : Integer.valueOf(tick - last);
				attacks = attackCounts.get(n.getIndex());
			}
			// Mid-wave only: at its start every NPC is at full health.
			int[] health = waveStart ? null : healthBars.get(n.getIndex());
			npcs.add(new ExportNpc(kind, n.getTile(), code, sinceAttack, attacks, health));
		}
		if (npcs.isEmpty())
		{
			throw new ExportException("There are no NPCs in the arena to export.");
		}

		Integer waveTick = waveStarted && !waveStart ? Integer.valueOf(tick - waveStartTick) : null;
		// Doom's stacks only when the plugin saw every hit of the wave, from its start.
		Integer doomStacks = waveStarted && !waveStart && modifiers.containsKey(Modifier.DOOM) ? Integer.valueOf(damageTaken) : null;
		return new ArenaState(npcs, player, waveStart, waveStarted ? wave : null, waveTick, !waveStart && arrived, modifiers, modifiersComplete,
			solarflareOrbs, doomStacks, sand());
	}

	/**
	 * The molten sand on the floor: that the Javelins' landings laid (onTileGraphic), lasting sand first, and the tiles of
	 * the Molten Sand objects, each tile once. How long a tile has left is not sent: below Reentry II it lasts the wave,
	 * and from it the run, which is what the site models.
	 */
	private List<GridTile> sand()
	{
		Set<GridTile> tiles = new LinkedHashSet<>(lastingSand);
		tiles.addAll(waveSand);
		tiles.addAll(sandObjects.keySet());
		return new ArrayList<>(tiles);
	}

	/**
	 * Whether an NPC came as a reinforcement: it spawned after its wave's first tick. A shaman already there when the
	 * plugin started is the reinforcement one from wave 7 on, where no wave starts with a shaman; before, it is taken for
	 * the wave's own.
	 */
	private boolean isReinforcement(SeenNpc n)
	{
		if (reinforcements.contains(n.getIndex()))
		{
			return true;
		}
		return n.getNpcId() == NpcKind.SHAMAN_ID && wave != null && wave >= 7;
	}

	/** The modifiers taken, with their tiers: those with tiers by their varbits. The wave log names them too. */
	public Map<Modifier, Integer> modifiers(ModifierLevels levels)
	{
		Map<Modifier, Integer> out = new EnumMap<>(Modifier.class);
		for (Modifier m : Modifier.values())
		{
			if (m.hasTiers())
			{
				int level = Math.max(0, Math.min(3, levels.level(m)));
				// A modifier the selector says is taken is at tier I at least, whatever its varbit reads.
				if (level == 0 && taken.contains(m))
				{
					level = 1;
				}
				if (level > 0)
				{
					out.put(m, level);
				}
			}
			else if (taken.contains(m))
			{
				out.put(m, 1);
			}
		}
		return out;
	}
}
