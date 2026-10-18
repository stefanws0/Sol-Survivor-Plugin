package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the plugin remembers of a Colosseum run between ticks, fed the game's events by the RuneLite layer and free of
 * RuneLite itself, so that its tests run on mocked game data. It follows the wave from the game's chat messages, which
 * NPCs spawned after the wave's first tick (the reinforcements), the orbs each Manticore has shown, each NPC's attacks,
 * and the modifiers taken; capture() turns that and the NPCs and player of a tick into an ArenaState.
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
		resetWave();
	}

	private void resetWave()
	{
		reinforcements.clear();
		reinforcementsArrived = false;
		orbs.clear();
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
			npcs.add(new ExportNpc(kind, n.getTile(), code, sinceAttack, attacks));
		}
		if (npcs.isEmpty())
		{
			throw new ExportException("There are no NPCs in the arena to export.");
		}

		Integer waveTick = waveStarted && !waveStart ? Integer.valueOf(tick - waveStartTick) : null;
		return new ArenaState(npcs, player, waveStart, waveStarted ? wave : null, waveTick, !waveStart && arrived, modifiers, modifiersComplete,
			solarflareOrbs);
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
