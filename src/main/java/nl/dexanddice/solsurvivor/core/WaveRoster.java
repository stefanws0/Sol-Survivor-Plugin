package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Who each wave brings, and the list of the wave under way that the panel and the wave box show: each of its NPCs with
 * how many are left, struck out as the last of a kind dies, the Fremennik warband, Quartet's fourth frem by its kind once
 * it joins, each of Dynamic Duo's two Shockwave Colossi on a line of its own, and the reinforcements, still to come until
 * they do. Only what is in the arena or on its way, as Fortis Colosseum's wave box shows it: no timers and no counts of
 * attacks. The waves are the site's (src/data/waves.ts), from the Fortis Colosseum plugin's WaveSpawns and the OSRS Wiki.
 */
public final class WaveRoster
{
	/** Each wave's NPCs at its start, 1 to 11: serpent shamans, Javelin Colossi, Manticores and Shockwave Colossi. */
	private static final int[][] SPAWNS = {
		{1, 0, 0, 0},
		{1, 1, 0, 0},
		{1, 2, 0, 0},
		{1, 0, 1, 0},
		{1, 1, 1, 0},
		{1, 2, 1, 0},
		{0, 1, 1, 1},
		{0, 2, 1, 1},
		{0, 1, 2, 0},
		{0, 2, 2, 0},
		{0, 1, 2, 1},
	};

	/** Each wave's reinforcements, which join about 40 seconds in: Jaguar warriors, Minotaurs and serpent shamans. */
	private static final int[][] REINFORCEMENTS = {
		{1, 0, 0},
		{1, 0, 0},
		{1, 0, 0},
		{1, 0, 1},
		{1, 0, 1},
		{1, 0, 1},
		{0, 1, 0},
		{0, 1, 0},
		{0, 1, 0},
		{0, 1, 1},
		{0, 1, 1},
	};

	/** Doom, by tier: how many hits that deal damage in one wave kill the player (the site's DOOM_LETHAL_HITS). */
	private static final int[] DOOM_LIMITS = {0, 15, 10, 5};

	/** The warband's three, by id, in the order the list names them. */
	private static final int[] FREM_IDS = {WaveReview.FREM_ARCHER_ID, WaveReview.FREM_SEER_ID, WaveReview.FREM_BERSERKER_ID};

	/**
	 * A line of a wave's list: who, in full and short, the icon that shows them, a note such as "Dynamic Duo", and how many
	 * are left or come.
	 */
	public static final class Line
	{
		private final String name;
		private final String shortName;
		private final String icon;
		private final String note;
		private final int count;
		private final boolean toCome;

		Line(String name, String shortName, String icon, String note, int count, boolean toCome)
		{
			this.name = name;
			this.shortName = shortName;
			this.icon = icon;
			this.note = note;
			this.count = count;
			this.toCome = toCome;
		}

		/** "Manticore", "The warband of three", "Berserker, Quartet's fourth". */
		public String getName()
		{
			return name;
		}

		/** The name where room is short, as the wave box and the next wave's list give it: "Javelin", "Warband". */
		public String getShortName()
		{
			return shortName;
		}

		/**
		 * The icon of who the line names, by the plugin's icon names: "javelin", "manticore", "archer", or "warband" for
		 * the three together.
		 */
		public String getIcon()
		{
			return icon;
		}

		/** "Dynamic Duo", "Quartet", "reinforcement"; null for none. */
		public String getNote()
		{
			return note;
		}

		/** How many are left, of the wave under way; how many come, of the next. */
		public int getCount()
		{
			return count;
		}

		/** A reinforcement that has not come yet. */
		public boolean isToCome()
		{
			return toCome;
		}

		/** Every one of the line's NPCs is dead: the line is struck out. */
		public boolean isDead()
		{
			return !toCome && count == 0;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Line))
			{
				return false;
			}
			Line l = (Line) o;
			return l.name.equals(name) && l.shortName.equals(shortName) && l.icon.equals(icon) && Objects.equals(l.note, note) && l.count == count
				&& l.toCome == toCome;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(name, shortName, icon, note, count, toCome);
		}

		@Override
		public String toString()
		{
			return name + (note == null ? "" : " (" + note + ")") + (toCome ? " to come" : " " + count);
		}
	}

	/** The NPCs alive in the arena now, by kind, and the frems by id. */
	public static final class Alive
	{
		private final int[] kinds = new int[NpcKind.values().length];
		private final int[] frems = new int[FREM_IDS.length];

		/** One more of a kind, a shaman come as a reinforcement as REINFORCEMENT_SHAMAN. */
		public Alive add(NpcKind kind)
		{
			kinds[kind.ordinal()]++;
			return this;
		}

		/** One more NPC by its id: a frem of the warband; any other id is passed over. */
		public Alive addFrem(int npcId)
		{
			for (int i = 0; i < FREM_IDS.length; i++)
			{
				if (FREM_IDS[i] == npcId)
				{
					frems[i]++;
				}
			}
			return this;
		}

		int of(NpcKind kind)
		{
			return kinds[kind.ordinal()];
		}
	}

	private WaveRoster()
	{
	}

	/** Whether Sol Survivor knows the wave's NPCs: waves 1 to 11. */
	public static boolean knows(Integer wave)
	{
		return wave != null && wave >= 1 && wave <= SPAWNS.length;
	}

	/** Whether a frem of the warband has this id. */
	public static boolean isFrem(int npcId)
	{
		for (int id : FREM_IDS)
		{
			if (id == npcId)
			{
				return true;
			}
		}
		return false;
	}

	/** Doom's limit at a tier, 1 to 3: the hits that deal damage in one wave that kill the player; 0 without Doom. */
	public static int doomLimit(int tier)
	{
		return tier >= 1 && tier <= 3 ? DOOM_LIMITS[tier] : 0;
	}

	/**
	 * The list of the wave under way: each kind of its NPCs with how many are left, the warband, and the reinforcements,
	 * each still to come until `arrived`. On the wave's first tick (`fresh`) every NPC is taken to be there, as the game
	 * may spawn them after the plugin reads the arena. `quartetFrem` is the id of the kind two of which joined, Quartet's
	 * fourth, or 0 while it is not known; `eachFrem` lists the warband's three one by one. Empty for a wave Sol Survivor
	 * does not know, such as Sol Heredit's.
	 */
	public static List<Line> thisWave(Integer wave, Map<Modifier, Integer> held, Alive alive, boolean arrived, boolean fresh, int quartetFrem,
		boolean eachFrem)
	{
		if (!knows(wave))
		{
			return Collections.emptyList();
		}
		int[] spawns = SPAWNS[wave - 1];
		boolean duo = held.containsKey(Modifier.DYNAMIC_DUO);
		List<Line> out = new ArrayList<>();
		kind(out, MANTICORE, spawns[2], alive.of(NpcKind.MANTICORE), fresh);
		int shockwaves = spawns[3];
		if (duo && shockwaves > 0)
		{
			// Dynamic Duo's two, each on a line of its own: the first struck out as the first of them dies.
			int left = fresh ? 2 : alive.of(NpcKind.SHOCKWAVE_COLOSSUS);
			out.add(SHOCKWAVE.line("Dynamic Duo", left >= 2 ? 1 : 0, false));
			out.add(SHOCKWAVE.line("Dynamic Duo", left >= 1 ? 1 : 0, false));
		}
		else
		{
			kind(out, SHOCKWAVE, shockwaves, alive.of(NpcKind.SHOCKWAVE_COLOSSUS), fresh);
		}
		kind(out, JAVELIN, spawns[1], alive.of(NpcKind.JAVELIN_COLOSSUS), fresh);
		kind(out, SHAMAN, spawns[0], alive.of(NpcKind.SERPENT_SHAMAN), fresh);
		warband(out, alive, fresh, held.containsKey(Modifier.QUARTET), quartetFrem, eachFrem);
		int[] reinforcements = REINFORCEMENTS[wave - 1];
		reinforcement(out, JAGUAR, reinforcements[0], alive.of(NpcKind.JAGUAR_WARRIOR), arrived);
		reinforcement(out, MINOTAUR, reinforcements[1], alive.of(NpcKind.MINOTAUR), arrived);
		reinforcement(out, SHAMAN, reinforcements[2], alive.of(NpcKind.REINFORCEMENT_SHAMAN), arrived);
		return out;
	}

	/** Who a line names: in full, short, and by its icon's name. */
	private static final class Who
	{
		final String name;
		final String shortName;
		final String icon;

		Who(String name, String shortName, String icon)
		{
			this.name = name;
			this.shortName = shortName;
			this.icon = icon;
		}

		Line line(String note, int count, boolean toCome)
		{
			return new Line(name, shortName, icon, note, count, toCome);
		}
	}

	private static final Who MANTICORE = new Who("Manticore", "Manticore", "manticore");
	private static final Who SHOCKWAVE = new Who("Shockwave Colossus", "Shockwave", "shockwave");
	private static final Who JAVELIN = new Who("Javelin Colossus", "Javelin", "javelin");
	private static final Who SHAMAN = new Who("Serpent shaman", "Shaman", "shaman");
	private static final Who JAGUAR = new Who("Jaguar warrior", "Jaguar", "jaguar");
	private static final Who MINOTAUR = new Who("Minotaur", "Minotaur", "minotaur");
	private static final Who WARBAND = new Who("The warband of three", "Warband", "warband");

	private static void kind(List<Line> out, Who who, int spawned, int alive, boolean fresh)
	{
		if (spawned > 0)
		{
			out.add(who.line(null, fresh ? spawned : Math.min(spawned, alive), false));
		}
	}

	private static void reinforcement(List<Line> out, Who who, int coming, int alive, boolean arrived)
	{
		if (coming > 0)
		{
			out.add(who.line("reinforcement", arrived ? Math.min(coming, alive) : coming, !arrived));
		}
	}

	/**
	 * The warband: its three on one line, or each on its own, and Quartet's fourth. Two frems of a kind are alike, so the
	 * fourth is whichever of a pair is left: the three's line counts one of each kind alive, and the fourth's the rest.
	 */
	private static void warband(List<Line> out, Alive alive, boolean fresh, boolean quartet, int quartetFrem, boolean eachFrem)
	{
		int three = 0;
		int extra = 0;
		for (int i = 0; i < FREM_IDS.length; i++)
		{
			int n = fresh ? 1 + (FREM_IDS[i] == quartetFrem ? 1 : 0) : alive.frems[i];
			three += Math.min(n, 1);
			extra += Math.max(n - 1, 0);
			if (eachFrem)
			{
				out.add(frem(FREM_IDS[i]).line("warband", Math.min(n, 1), false));
			}
		}
		if (!eachFrem)
		{
			out.add(WARBAND.line(null, three, false));
		}
		if (quartet)
		{
			out.add(fourth(quartetFrem).line("Quartet", fresh ? 1 : Math.min(extra, 1), false));
		}
	}

	/** A frem of the warband, by its id: "Archer", its icon "archer". */
	private static Who frem(int npcId)
	{
		String name = WaveReview.shortName(npcId);
		return new Who(name, name, name.toLowerCase(java.util.Locale.ROOT));
	}

	/** Quartet's fourth frem, by the id of its kind, or 0 while that is not known. */
	private static Who fourth(int quartetFrem)
	{
		if (quartetFrem == 0)
		{
			return new Who("Quartet's fourth frem", "Fourth frem", "warband");
		}
		Who kind = frem(quartetFrem);
		return new Who(kind.name + ", Quartet's fourth", kind.name + " (Quartet)", kind.icon);
	}

	/**
	 * What the next wave brings at its start, each kind with how many: Dynamic Duo's Shockwave Colossi counted twice, and
	 * the Fremennik warband, which comes at every wave's start (the user's logs, 2026-10-26), with Quartet's fourth. Empty
	 * for a wave Sol Survivor does not know.
	 */
	public static List<Line> nextWave(Integer wave, Map<Modifier, Integer> held)
	{
		if (!knows(wave))
		{
			return Collections.emptyList();
		}
		int[] spawns = SPAWNS[wave - 1];
		List<Line> out = new ArrayList<>();
		brings(out, JAVELIN, spawns[1]);
		brings(out, MANTICORE, spawns[2]);
		brings(out, SHOCKWAVE, spawns[3] * (held.containsKey(Modifier.DYNAMIC_DUO) ? 2 : 1));
		brings(out, SHAMAN, spawns[0]);
		brings(out, WARBAND, 3);
		if (held.containsKey(Modifier.QUARTET))
		{
			out.add(fourth(0).line("Quartet", 1, false));
		}
		return out;
	}

	private static void brings(List<Line> out, Who who, int count)
	{
		if (count > 0)
		{
			out.add(who.line(null, count, false));
		}
	}

	/**
	 * Who joins the next wave after its start, its reinforcements, each kind with how many, still to come: the wave box
	 * lists them under its rule. Empty for a wave Sol Survivor does not know.
	 */
	public static List<Line> nextWaveLater(Integer wave)
	{
		if (!knows(wave))
		{
			return Collections.emptyList();
		}
		int[] r = REINFORCEMENTS[wave - 1];
		List<Line> out = new ArrayList<>();
		reinforcement(out, JAGUAR, r[0], 0, false);
		reinforcement(out, MINOTAUR, r[1], 0, false);
		reinforcement(out, SHAMAN, r[2], 0, false);
		return out;
	}

	/**
	 * Who joins the next wave after its start, its reinforcements, in a sentence: "Then a Minotaur, the reinforcement";
	 * null for a wave Sol Survivor does not know.
	 */
	public static String nextWaveThen(Integer wave, Map<Modifier, Integer> held)
	{
		if (!knows(wave))
		{
			return null;
		}
		int[] r = REINFORCEMENTS[wave - 1];
		List<String> later = new ArrayList<>();
		if (r[0] > 0)
		{
			later.add("a Jaguar warrior");
		}
		if (r[1] > 0)
		{
			later.add("a Minotaur");
		}
		if (r[2] > 0)
		{
			later.add("a second shaman");
		}
		return "Then " + WaveReview.joined(later) + (later.size() == 1 ? ", the reinforcement." : ", the reinforcements.");
	}
}
