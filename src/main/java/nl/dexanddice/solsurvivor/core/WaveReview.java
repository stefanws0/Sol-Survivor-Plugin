package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;

/**
 * One wave's log read for the wave report and the death recap, which the panel shows once the wave is over: each attack
 * at the player and the protection prayer that met it, each hit the player took and what likeliest dealt it, and the
 * stretches the player stood idle. It reads the log as the site's run review does (src/viewer/runReview.ts, reviewRun),
 * so that the report and the site agree on a wave: the same attacks, the same ticks from an attack to its hitsplat, and
 * the same order of likelihood when two attacks could have dealt a hit. src/test/resources/fixtures/wave-review.json is
 * a log both read (test/link/pluginFixtures.test.ts).
 *
 * The prayer against each attack is judged as the site's Prayer reliability judges it (PrayerFlicks): on time, a tick
 * late or off a tick early, the wrong prayer, or none up. Unlike that count, an attack met with another prayer is kept,
 * as a wrong prayer: the report says what got past, whether or not tanking it was the plan.
 *
 * Nothing here is shown during a wave, and nothing here says how to play one: it says what the log saw.
 */
public final class WaveReview
{
	/** How the player's protection prayer met an attack. */
	public enum Prayed
	{
		/** The right prayer was on as the attack was checked. */
		ON_TIME,
		/** The right prayer came on a tick after the check, or the switch was caught half-way, the old prayer counting. */
		LATE,
		/** The right prayer was on the tick before and switched off on it. */
		EARLY,
		/** Another protection prayer was on. */
		WRONG,
		/** No protection prayer was on. */
		NONE
	}

	/** What the report counts as a mistake, each with a switch of its own in the settings. */
	public enum Kind
	{
		/** An attack that met the wrong prayer or none, with what it hit for. */
		PAST_PRAYER,
		/** An attack whose prayer came a tick late or went a tick early. */
		LATE_FLICK,
		/** A Javelin Colossus's special that landed on the player. */
		SPECIAL,
		/** A Volatility explosion the player stood in. */
		VOLATILITY,
		/** A hit of Solarflare's orb, or one no attack explains while the player stood on molten sand. */
		SOLARFLARE_SAND,
		/** A stretch of ticks with the weapon ready, no attack and no step. */
		IDLE
	}

	/**
	 * A mistake of the wave: its tick, its kind, what happened, the damage it cost or the idle stretch's ticks, and the
	 * picture of what did it.
	 */
	public static final class Mistake
	{
		private final int tick;
		private final Kind kind;
		private final String text;
		private final Integer damage;
		private final int ticks;
		private final String icon;

		Mistake(int tick, Kind kind, String text, Integer damage, int ticks, String icon)
		{
			this.tick = tick;
			this.kind = kind;
			this.text = text;
			this.damage = damage;
			this.ticks = ticks;
			this.icon = icon;
		}

		/**
		 * The picture of what did it, by the plugin's icon names: the NPC's face ("npc-javelin"), the modifier's
		 * ("modifier-volatility"), "molten-sand", or "hourglass" for an idle stretch; null when nothing in the log says.
		 */
		public String getIcon()
		{
			return icon;
		}

		/** The wave's tick it happened on: the attack's check, the hit's landing or the idle stretch's first. */
		public int getTick()
		{
			return tick;
		}

		public Kind getKind()
		{
			return kind;
		}

		public String getText()
		{
			return text;
		}

		/** The damage it cost, 0 for an attack past the prayer that hit for nothing; null for an idle stretch. */
		public Integer getDamage()
		{
			return damage;
		}

		/** An idle stretch's length in ticks; 0 for the other kinds. */
		public int getTicks()
		{
			return ticks;
		}

		@Override
		public String toString()
		{
			return "t" + tick + " " + kind + " " + text + (damage == null ? "" : " " + damage);
		}
	}

	/** A line of the death recap: the tick, the player's hitpoints and protection prayer on it, and what happened. */
	public static final class RecapLine
	{
		private final int tick;
		private final Integer hitpoints;
		private final String prayer;
		private final String text;
		private final String hit;

		RecapLine(int tick, Integer hitpoints, String prayer, String text, String hit)
		{
			this.tick = tick;
			this.hitpoints = hitpoints;
			this.prayer = prayer;
			this.text = text;
			this.hit = hit;
		}

		public int getTick()
		{
			return tick;
		}

		/** The hitpoints the log has on the tick; null where it has no player. */
		public Integer getHitpoints()
		{
			return hitpoints;
		}

		/** The protection prayer on, "magic", "ranged" or "melee"; "" for none, null where the log does not say. */
		public String getPrayer()
		{
			return prayer;
		}

		public String getText()
		{
			return text;
		}

		/** The damage that landed, "blocked", "0" for a hit of nothing, or "–" for none. */
		public String getHit()
		{
			return hit;
		}

		@Override
		public String toString()
		{
			return tick + " " + hitpoints + " " + prayer + " " + text + " " + hit;
		}
	}

	/** The site's PROTECT names, by the style each blocks. */
	private static final Map<String, String> PRAYER_NAMES = new HashMap<>();

	/** The NPC ids the site's review models (its LOG_NPC_TYPES), each with its footprint's size. */
	private static final Map<Integer, Integer> SIZES = new HashMap<>();

	/**
	 * The Fremennik warband by id: its name and the style of its attack, whose animation PrayerFlicks's FREM_ATTACKS
	 * holds. Each frem's hitsplat is on the player on the tick after it attacks (the site's FREM_HIT_DELAY).
	 */
	private static final Map<Integer, String[]> FREMS = new HashMap<>();
	private static final Map<Integer, Integer> FREM_ATTACKS = new HashMap<>();
	public static final int FREM_ARCHER_ID = 12814;
	public static final int FREM_SEER_ID = 12815;
	public static final int FREM_BERSERKER_ID = 12816;
	private static final int FREM_HIT_DELAY = 1;

	/** The NPC whose attacks the log cannot show but which can hurt the player: Solarflare's orb (the site's UNSEEN_ATTACKERS). */
	private static final int SOLARFLARE_ORB = NpcKind.SOLARFLARE_ORB_ID;

	/**
	 * Volatility's explosions, by id: the NPC the game puts on a dead NPC's tile for the ticks its explosion shows, and
	 * the size of the footprint it stands for, 0 where it stands for more than one and the dead NPC's own tells (the
	 * site's VOLATILITY_EXPLOSIONS).
	 */
	private static final Map<Integer, Integer> EXPLOSIONS = new HashMap<>();

	/** How many tiles beyond an NPC's footprint its explosion reaches, by Volatility's tier: the site's VOLATILITY_REACH. */
	private static final int[] VOLATILITY_REACH = {0, 1, 2, 2};

	/** Ticks from a Javelin special's throw to its landing: the site's JAVELIN_SPECIAL_LANDING. */
	private static final int JAVELIN_SPECIAL_LANDING = 6;

	/** From Mantimayhem I, each Manticore orb is two projectiles, each with its hitsplat (MANTIMAYHEM_PROJECTILES_PER_ORB). */
	private static final int MANTIMAYHEM_PROJECTILES_PER_ORB = 2;

	/** The player eating or drinking, which is no attack (the site's EAT_ANIM). */
	public static final int EAT_ANIMATION = 829;

	/** The most lines the death recap shows: the ticks up to the one the player died on. */
	public static final int RECAP_TICKS = 8;

	static
	{
		PRAYER_NAMES.put("magic", "Protect from Magic");
		PRAYER_NAMES.put("ranged", "Protect from Missiles");
		PRAYER_NAMES.put("melee", "Protect from Melee");
		SIZES.put(NpcKind.SHAMAN_ID, 1);
		SIZES.put(NpcKind.JAGUAR_ID, 2);
		SIZES.put(NpcKind.MINOTAUR_ID, 3);
		SIZES.put(NpcKind.MINOTAUR_RED_FLAG_ID, 3);
		SIZES.put(NpcKind.JAVELIN_ID, 3);
		SIZES.put(NpcKind.MANTICORE_ID, 3);
		SIZES.put(NpcKind.SHOCKWAVE_ID, 3);
		FREMS.put(FREM_ARCHER_ID, new String[] {"Archer", "ranged"});
		FREMS.put(FREM_SEER_ID, new String[] {"Seer", "magic"});
		FREMS.put(FREM_BERSERKER_ID, new String[] {"Berserker", "melee"});
		FREM_ATTACKS.put(FREM_ARCHER_ID, 10850);
		FREM_ATTACKS.put(FREM_SEER_ID, 10853);
		FREM_ATTACKS.put(FREM_BERSERKER_ID, 10856);
		EXPLOSIONS.put(12828, 0);
		EXPLOSIONS.put(12829, 3);
		EXPLOSIONS.put(12831, 3);
	}

	/** An attack at the player, with what the review made of it. */
	private static final class Attack
	{
		final int tick;
		final int checked;
		final int index;
		final int npcId;
		final String name;
		final String style;
		final boolean special;
		final boolean frem;
		/** The protection prayer on as it was checked: "" for none, null where the log does not say. */
		final String prayer;
		final Integer distance;
		Prayed prayed;
		int damage;
		int matches;

		Attack(int tick, int checked, int index, int npcId, String name, String style, boolean special, boolean frem, String prayer,
			Integer distance)
		{
			this.tick = tick;
			this.checked = checked;
			this.index = index;
			this.npcId = npcId;
			this.name = name;
			this.style = style;
			this.special = special;
			this.frem = frem;
			this.prayer = prayer;
			this.distance = distance;
		}

		boolean blocked()
		{
			return !special && style.equals(prayer);
		}

		/** "East Manticore's magic orb", "Javelin's throw", "Archer's ranged attack". */
		String text()
		{
			return name + "'s " + (special ? "special" : what(npcId, style));
		}
	}

	/** A hit on the player that did damage, and what the review put it down to. */
	private static final class Hit
	{
		final int tick;
		final int damage;
		final Attack attack;
		/** What dealt it when no attack did, as a line says it and as a sentence names it; null for nothing the review knows. */
		final String other;
		final String otherNoun;
		final Kind otherKind;

		Hit(int tick, int damage, Attack attack)
		{
			this(tick, damage, attack, null, null, null);
		}

		Hit(int tick, int damage, Attack attack, String other, String otherNoun, Kind otherKind)
		{
			this.tick = tick;
			this.damage = damage;
			this.attack = attack;
			this.other = other;
			this.otherNoun = otherNoun;
			this.otherKind = otherKind;
		}

		/** What dealt it, as a line of the report or the recap says it: "Javelin's special landed on your tile". */
		String text()
		{
			if (attack != null)
			{
				return attack.special ? attack.name + "'s special landed on your tile" : attack.text();
			}
			return other == null ? "A hit no attack in the log explains" : other;
		}

		/** What dealt it, as a sentence names it: "the Javelin's special", "molten sand". */
		String noun()
		{
			if (attack != null)
			{
				return "the " + (attack.special ? attack.name + "'s special" : attack.text());
			}
			return otherNoun == null ? "a hit no attack in the log explains" : otherNoun;
		}
	}

	/** What the review remembers of an NPC from the ticks before. */
	private static final class Seen
	{
		final int npcId;
		final int first;
		int last;
		Integer death;

		Seen(int npcId, int first)
		{
			this.npcId = npcId;
			this.first = first;
			this.last = first;
		}
	}

	private final Integer wave;
	private final Integer died;
	private final int last;
	private final Map<Prayed, Integer> prayed;
	private final List<Mistake> mistakes;
	private final int damage;
	private final List<RecapLine> recap;
	private final String cause;
	private final int maxHitpoints;

	private WaveReview(Integer wave, Integer died, int last, Map<Prayed, Integer> prayed, List<Mistake> mistakes, int damage, List<RecapLine> recap,
		String cause, int maxHitpoints)
	{
		this.wave = wave;
		this.died = died;
		this.last = last;
		this.prayed = prayed;
		this.mistakes = mistakes;
		this.damage = damage;
		this.recap = recap;
		this.cause = cause;
		this.maxHitpoints = maxHitpoints;
	}

	/** The most hitpoints the player had on a tick of the wave, which the recap measures its hitpoints by; 0 for none. */
	public int getMaxHitpoints()
	{
		return maxHitpoints;
	}

	/** The wave reviewed, or null when the log does not say. */
	public Integer getWave()
	{
		return wave;
	}

	/** The tick the player died on, the first with no hitpoints, or null when they did not. */
	public Integer getDied()
	{
		return died;
	}

	/** The log's last tick. */
	public int getLast()
	{
		return last;
	}

	/** How many attacks the prayer met in each way; an attack the log cannot judge is left out. */
	public int count(Prayed p)
	{
		return prayed.getOrDefault(p, 0);
	}

	/** The attacks the prayer was judged against. */
	public int attacksJudged()
	{
		int n = 0;
		for (int c : prayed.values())
		{
			n += c;
		}
		return n;
	}

	/** Every mistake of the wave, by tick: each kind, whatever the settings count. */
	public List<Mistake> getMistakes()
	{
		return mistakes;
	}

	/**
	 * The mistakes the settings count: those of the kinds in `kinds`, and of the idle stretches only those `idleFrom` ticks
	 * or longer.
	 */
	public List<Mistake> counted(Set<Kind> kinds, int idleFrom)
	{
		List<Mistake> out = new ArrayList<>();
		for (Mistake m : mistakes)
		{
			if (kinds.contains(m.getKind()) && (m.getKind() != Kind.IDLE || m.getTicks() >= idleFrom))
			{
				out.add(m);
			}
		}
		return out;
	}

	/** A kind's name in the report's sentences, as many: "hits past your prayer", "idle stretches". */
	public static String kindName(Kind kind)
	{
		switch (kind)
		{
			case PAST_PRAYER:
				return "hits past your prayer";
			case LATE_FLICK:
				return "flicks a tick off";
			case SPECIAL:
				return "Javelin specials";
			case VOLATILITY:
				return "Volatility blasts";
			case SOLARFLARE_SAND:
				return "Solarflare and the sand";
			default:
				return "idle stretches";
		}
	}

	/**
	 * What a run's counted mistakes are mostly, in a sentence: the kind with the most and its share, as "Most of this
	 * run's mistakes are hits past your prayer: 7 of 12." Null for fewer than 3 mistakes, too few to say, or when no kind
	 * has more than any other.
	 */
	public static String mostly(List<WaveReview> run, Set<Kind> kinds, int idleFrom)
	{
		Map<Kind, Integer> by = new EnumMap<>(Kind.class);
		int total = 0;
		for (WaveReview r : run)
		{
			for (Mistake m : r.counted(kinds, idleFrom))
			{
				by.merge(m.getKind(), 1, Integer::sum);
				total++;
			}
		}
		Kind top = null;
		boolean tied = false;
		for (Map.Entry<Kind, Integer> e : by.entrySet())
		{
			int most = top == null ? 0 : by.get(top);
			if (e.getValue() > most)
			{
				top = e.getKey();
				tied = false;
			}
			else if (e.getValue() == most)
			{
				tied = true;
			}
		}
		if (total < 3 || top == null || tied)
		{
			return null;
		}
		int n = by.get(top);
		String most = n * 2 > total ? "Most of this run's mistakes are " : "This run's most common mistakes are ";
		return most + kindName(top) + ": " + n + " of " + total + ".";
	}

	/** The damage the player took in the wave, every hitsplat above 0. */
	public int getDamage()
	{
		return damage;
	}

	/** The death recap's lines, the last RECAP_TICKS ticks up to the death; empty when the player did not die. */
	public List<RecapLine> getRecap()
	{
		return recap;
	}

	/** What killed the player, in a sentence; null when they did not die. */
	public String getCause()
	{
		return cause;
	}

	/** A wave with nothing in it, as for a log without ticks. */
	public static WaveReview empty(Integer wave)
	{
		return new WaveReview(wave, null, 0, new EnumMap<>(Prayed.class), Collections.<Mistake>emptyList(), 0,
			Collections.<RecapLine>emptyList(), null, 0);
	}

	/**
	 * Review a wave's log. `sandTicks` are the ticks the player stood on molten sand, which the tracker knows and the log
	 * does not; `speedOf` gives a weapon's attack speed in ticks by its item id, 0 when it is not known, for the idle
	 * stretches. The log must not change while it is read: a snapshot, or the log on the client thread.
	 */
	public static WaveReview of(WaveLog log, Set<Integer> sandTicks, IntUnaryOperator speedOf)
	{
		Map<Modifier, Integer> modifiers = log.getModifiers();
		return of(log.getWave(), log.getTicks(), modifiers.getOrDefault(Modifier.MANTIMAYHEM, 0), modifiers.getOrDefault(Modifier.VOLATILITY, 0),
			sandTicks, speedOf);
	}

	static WaveReview of(Integer wave, List<WaveLog.Tick> ticks, int mantimayhem, int volatility, Set<Integer> sandTicks, IntUnaryOperator speedOf)
	{
		if (ticks.isEmpty())
		{
			return empty(wave);
		}
		Map<Integer, WaveLog.Tick> byT = new HashMap<>();
		Integer died = null;
		int most = 0;
		for (WaveLog.Tick tick : ticks)
		{
			byT.put(tick.t, tick);
			if (died == null && tick.player != null && tick.player.getHitpoints() == 0)
			{
				died = tick.t;
			}
			if (tick.player != null)
			{
				most = Math.max(most, tick.player.getHitpoints());
			}
		}
		Map<Integer, Seen> latest = new HashMap<>();
		List<Seen> seen = new ArrayList<>();
		List<Attack> attacks = attacks(ticks, byT, died, latest, seen);
		int last = ticks.get(ticks.size() - 1).t;
		// An NPC gone from the grid before the log's end with no death seen died as it went.
		for (Seen s : seen)
		{
			if (s.death == null && s.last < last && (SIZES.containsKey(s.npcId) || FREMS.containsKey(s.npcId)))
			{
				s.death = s.last + 1;
			}
		}
		for (Attack a : attacks)
		{
			a.prayed = a.special ? null : judge(a, attacks, byT);
		}
		List<Hit> hits = hits(ticks, attacks, mantimayhem, volatility, sandTicks);

		Map<Prayed, Integer> prayed = new EnumMap<>(Prayed.class);
		for (Attack a : attacks)
		{
			if (a.prayed != null)
			{
				prayed.merge(a.prayed, 1, Integer::sum);
			}
		}
		List<Mistake> mistakes = mistakes(attacks, hits);
		mistakes.addAll(idle(ticks, latest, died, speedOf));
		mistakes.sort(Comparator.comparingInt(Mistake::getTick).thenComparing(Mistake::getKind));
		int damage = 0;
		for (Hit h : hits)
		{
			damage += h.damage;
		}
		List<RecapLine> recap = died == null ? Collections.<RecapLine>emptyList() : recap(ticks, byT, died, attacks, hits);
		String cause = died == null ? null : cause(byT, died, attacks, hits);
		return new WaveReview(wave, died, last, prayed, Collections.unmodifiableList(mistakes), damage, Collections.unmodifiableList(recap), cause,
			most);
	}

	/**
	 * The attacks at the player, a tick at a time, as the site's review finds them: each NPC by its index, a new one where
	 * the id changes, the index was not on the tick before, or a dead NPC's bar comes back; none by an NPC after the tick it
	 * died on, and none after the player died. A Manticore's attacks are its orbs, each graphic it showed on the tick
	 * before and no longer shows; the frems' and the others' are their attack animations' starts. A Jaguar's attack on the
	 * tick it is killed brings no hit, so it is none (the site's HITLESS_ON_KILLING_TICK).
	 */
	private static List<Attack> attacks(List<WaveLog.Tick> ticks, Map<Integer, WaveLog.Tick> byT, Integer died, Map<Integer, Seen> latest,
		List<Seen> seen)
	{
		List<Attack> out = new ArrayList<>();
		WaveLog.Tick before = null;
		for (WaveLog.Tick tick : ticks)
		{
			for (WaveLog.NpcTick npc : tick.npcs)
			{
				WaveLog.NpcTick prior = before == null ? null : npcOf(before, npc.getIndex());
				Seen s = latest.get(npc.getIndex());
				if (s == null || s.npcId != npc.getNpcId() || prior == null || (prior.getHealthRatio() == 0 && npc.getHealthRatio() > 0))
				{
					s = new Seen(npc.getNpcId(), tick.t);
					latest.put(npc.getIndex(), s);
					seen.add(s);
				}
				s.last = tick.t;
				Integer size = SIZES.get(npc.getNpcId());
				if (s.death == null && (npc.getHealthRatio() == 0 || (size != null && dies(tick, npc))))
				{
					s.death = tick.t;
				}
				boolean playerDead = died != null && tick.t > died;
				if ((s.death != null && s.death < tick.t) || playerDead)
				{
					continue;
				}
				WaveLog.NpcTick previous = s.first == tick.t ? null : prior;
				String[] frem = FREMS.get(npc.getNpcId());
				if (frem != null)
				{
					if (started(tick, npc.getIndex(), FREM_ATTACKS.get(npc.getNpcId())))
					{
						out.add(new Attack(tick.t, tick.t, npc.getIndex(), npc.getNpcId(), nameOf(tick, npc), frem[1], false, true,
							PrayerFlicks.protectionOn(tick, before, byT.get(tick.t + 1)), null));
					}
					continue;
				}
				if (size == null)
				{
					continue;
				}
				Integer distance = tick.player == null || tick.player.getTile() == null ? null : distanceTo(npc.getTile(), size, tick.player.getTile());
				if (npc.getNpcId() == NpcKind.MANTICORE_ID)
				{
					if (s.death != null || before == null || previous == null || before.t != tick.t - 1)
					{
						continue;
					}
					for (String style : orbsFired(previous, npc))
					{
						out.add(new Attack(tick.t, tick.t, npc.getIndex(), npc.getNpcId(), nameOf(tick, npc), style, false, false,
							PrayerFlicks.protectionOn(tick, before, byT.get(tick.t + 1)), distance));
					}
					continue;
				}
				if (npc.getNpcId() == NpcKind.JAGUAR_ID && s.death != null && s.death == tick.t)
				{
					continue;
				}
				int animation = attackStarted(tick, npc);
				if (animation == -1)
				{
					continue;
				}
				boolean minotaur = npc.getNpcId() == NpcKind.MINOTAUR_ID || npc.getNpcId() == NpcKind.MINOTAUR_RED_FLAG_ID;
				int checked = tick.t + (minotaur ? PrayerFlicks.MINOTAUR_PRAYER_DELAY : 0);
				out.add(new Attack(tick.t, checked, npc.getIndex(), npc.getNpcId(), nameOf(tick, npc), styleOf(npc.getNpcId()),
					AttackAnimations.isSpecial(npc.getNpcId(), animation), false, PrayerFlicks.protectionOn(byT.get(checked), byT.get(checked - 1), byT.get(checked + 1)), distance));
			}
			before = tick;
		}
		return out;
	}

	/**
	 * How the prayer met an attack, as PrayerFlicks judges it: on time, a tick late, off a tick early, or caught half-way;
	 * otherwise another prayer or none. An attack checked on the tick of another of another style that the prayer blocked
	 * is left out, as only one could be, and so is one whose tick the log does not say.
	 */
	private static Prayed judge(Attack a, List<Attack> attacks, Map<Integer, WaveLog.Tick> byT)
	{
		if (a.prayer == null)
		{
			return null;
		}
		if (a.style.equals(a.prayer))
		{
			return Prayed.ON_TIME;
		}
		for (Attack b : attacks)
		{
			if (b != a && !b.special && b.checked == a.checked && !b.style.equals(a.style) && b.style.equals(b.prayer))
			{
				return null;
			}
		}
		WaveLog.Tick at = byT.get(a.checked);
		String before = PrayerFlicks.protectionOn(byT.get(a.checked - 1), byT.get(a.checked - 2), at);
		String after = PrayerFlicks.protectionOn(byT.get(a.checked + 1), at, byT.get(a.checked + 2));
		boolean halfWay = at != null && at.player != null && at.player.getPrayers().contains(a.style);
		if (halfWay || a.style.equals(after))
		{
			return Prayed.LATE;
		}
		if (a.style.equals(before))
		{
			return Prayed.EARLY;
		}
		return PrayerFlicks.NO_PRAYER.equals(a.prayer) ? Prayed.NONE : Prayed.WRONG;
	}

	/**
	 * Each hit that did damage, matched to the attack likeliest to have dealt it: one not prayed against before one that
	 * was, and the likeliest delay first. An attack matched as often as it has hits is not matched again; a frem's hit
	 * comes FREM_HIT_DELAY ticks on, which puts it after an NPC's at that NPC's likeliest delay and before one at any
	 * other. A Volatility explosion the player stood in comes before an attack the prayer should have stopped, and so does
	 * Solarflare's orb, which the log cannot see attack; a hit nothing explains, on a tick the player stood on molten sand,
	 * is put down to the sand.
	 */
	private static List<Hit> hits(List<WaveLog.Tick> ticks, List<Attack> attacks, int mantimayhem, int volatility,
		Set<Integer> sandTicks)
	{
		List<Hit> out = new ArrayList<>();
		WaveLog.Tick before = null;
		for (WaveLog.Tick tick : ticks)
		{
			List<String> blasts = blastsOn(tick, before, volatility);
			before = tick;
			for (WaveLog.Hit hit : tick.playerHits)
			{
				int damage = hit.getAmount();
				if (damage <= 0)
				{
					continue;
				}
				Attack best = null;
				double bestRank = Double.POSITIVE_INFINITY;
				for (Attack a : attacks)
				{
					if (a.frem)
					{
						if (a.matches > 0 || a.tick != tick.t - FREM_HIT_DELAY)
						{
							continue;
						}
						double rank = (a.blocked() ? 10 : 0) + 0.5;
						if (rank < bestRank)
						{
							best = a;
							bestRank = rank;
						}
						continue;
					}
					int hitsOf = a.npcId == NpcKind.MANTICORE_ID && mantimayhem >= 1 ? MANTIMAYHEM_PROJECTILES_PER_ORB : 1;
					if (a.matches >= hitsOf || a.tick >= tick.t)
					{
						continue;
					}
					int delay = indexOf(hitDelays(a.npcId, a.special, a.distance), tick.t - a.tick);
					if (delay < 0)
					{
						continue;
					}
					double rank = (a.blocked() ? 10 : 0) + delay;
					if (rank < bestRank)
					{
						best = a;
						bestRank = rank;
					}
				}
				boolean solarflare = false;
				for (WaveLog.NpcTick n : tick.npcs)
				{
					solarflare |= n.getNpcId() == SOLARFLARE_ORB;
				}
				String blast = bestRank >= 10 && !blasts.isEmpty() ? blasts.remove(0) : null;
				if (blast != null)
				{
					out.add(new Hit(tick.t, damage, null, blast, "a " + blast.substring(0, 1).toLowerCase(Locale.ROOT) + blast.substring(1), Kind.VOLATILITY));
				}
				else if (best != null && !(bestRank >= 10 && solarflare))
				{
					best.damage += damage;
					best.matches++;
					out.add(new Hit(tick.t, damage, best));
				}
				else if (solarflare)
				{
					out.add(new Hit(tick.t, damage, null, "Solarflare's orb hit you", "Solarflare's orb", Kind.SOLARFLARE_SAND));
				}
				else if (sandTicks.contains(tick.t) || sandTicks.contains(tick.t - 1))
				{
					out.add(new Hit(tick.t, damage, null, "Molten sand burned you", "molten sand", Kind.SOLARFLARE_SAND));
				}
				else
				{
					out.add(new Hit(tick.t, damage, null));
				}
			}
		}
		return out;
	}

	/** The mistakes of the attacks and the hits, every kind. */
	private static List<Mistake> mistakes(List<Attack> attacks, List<Hit> hits)
	{
		List<Mistake> out = new ArrayList<>();
		for (Attack a : attacks)
		{
			if (a.prayed == null)
			{
				continue;
			}
			switch (a.prayed)
			{
				case WRONG:
					out.add(new Mistake(a.checked, Kind.PAST_PRAYER, a.text() + ", with " + PRAYER_NAMES.get(a.prayer) + " up", a.damage, 0, iconOf(a.npcId)));
					break;
				case NONE:
					out.add(new Mistake(a.checked, Kind.PAST_PRAYER, a.text() + ", with no protection prayer up", a.damage, 0, iconOf(a.npcId)));
					break;
				case LATE:
					out.add(new Mistake(a.checked, Kind.LATE_FLICK, a.text() + ": " + PRAYER_NAMES.get(a.style) + " a tick late", a.damage, 0, iconOf(a.npcId)));
					break;
				case EARLY:
					out.add(new Mistake(a.checked, Kind.LATE_FLICK, a.text() + ": " + PRAYER_NAMES.get(a.style) + " off a tick early", a.damage, 0,
						iconOf(a.npcId)));
					break;
				default:
					break;
			}
		}
		for (Hit h : hits)
		{
			if (h.attack != null && h.attack.special)
			{
				out.add(new Mistake(h.tick, Kind.SPECIAL, h.text(), h.damage, 0, iconOf(h.attack.npcId)));
			}
			else if (h.attack == null && h.otherKind != null)
			{
				String icon = h.otherKind == Kind.VOLATILITY ? "modifier-volatility" : h.other.startsWith("Solarflare") ? "modifier-solarflare" : "molten-sand";
				out.add(new Mistake(h.tick, h.otherKind, h.other, h.damage, 0, icon));
			}
		}
		return out;
	}

	/**
	 * The stretches the player stood idle: ticks on which the player had an NPC they fight as their target, alive, their
	 * weapon was ready, as long as its attack speed since their last attack, and they neither attacked nor took a step.
	 * Eating is no idling, and a wait with no target, such as behind a pillar for the NPCs to line up or while planning
	 * the wave on the site, is none either. An attack shows as the player's attack animation starting or as their own
	 * hitsplat landing on an NPC: a weapon that attacks again with the animation it already shows, as a toxic blowpipe
	 * firing on, starts no animation (Stefan's wave 7 of 26 October, from tick 151), but its darts land. A weapon whose
	 * speed is not known takes the commonest gap of 2 ticks or more between two of the player's attacks in the wave, or
	 * 4 ticks without one: an animation the player shows as they are hit, which the log cannot tell from an attack, makes
	 * a shorter gap now and then. Each stretch is one mistake, whatever its length; the settings say from how many ticks
	 * it counts.
	 */
	private static List<Mistake> idle(List<WaveLog.Tick> ticks, Map<Integer, Seen> latest, Integer died, IntUnaryOperator speedOf)
	{
		Map<Integer, Integer> gaps = new HashMap<>();
		Integer previousAttack = null;
		for (WaveLog.Tick tick : ticks)
		{
			if (attacked(tick))
			{
				if (previousAttack != null && tick.t - previousAttack >= 2)
				{
					gaps.merge(tick.t - previousAttack, 1, Integer::sum);
				}
				previousAttack = tick.t;
			}
		}
		int fallback = 4;
		int most = 0;
		for (Map.Entry<Integer, Integer> e : gaps.entrySet())
		{
			if (e.getValue() > most || (e.getValue() == most && e.getKey() < fallback))
			{
				fallback = e.getKey();
				most = e.getValue();
			}
		}

		List<Mistake> out = new ArrayList<>();
		Integer lastAttack = null;
		int weapon = -1;
		WaveLog.Tick before = null;
		int from = -1;
		int length = 0;
		for (WaveLog.Tick tick : ticks)
		{
			if (tick.player != null && tick.player.getWorn() != null)
			{
				weapon = tick.player.getWorn()[WaveLog.WORN_SLOTS.indexOf("weapon")];
			}
			int known = weapon > 0 ? speedOf.applyAsInt(weapon) : 0;
			int speed = known > 0 ? known : fallback;
			boolean attacked = attacked(tick);
			boolean idle = !attacked && !eating(tick) && (died == null || tick.t < died) && before != null && before.t == tick.t - 1
				&& tick.player != null && before.player != null && tick.player.getTile() != null && tick.player.getTile().equals(before.player.getTile())
				&& (lastAttack == null || tick.t - lastAttack >= speed) && targetAlive(tick, latest);
			if (idle)
			{
				if (length == 0)
				{
					from = tick.t;
				}
				length++;
			}
			else if (length > 0)
			{
				out.add(new Mistake(from, Kind.IDLE, "Idle for " + plural(length, "tick"), null, length, "hourglass"));
				length = 0;
			}
			if (attacked)
			{
				lastAttack = tick.t;
			}
			before = tick;
		}
		if (length > 0)
		{
			out.add(new Mistake(from, Kind.IDLE, "Idle for " + plural(length, "tick"), null, length, "hourglass"));
		}
		return out;
	}

	/** Whether the player's target on the tick is an NPC they fight, one the site models or a frem, and not yet dead. */
	private static boolean targetAlive(WaveLog.Tick tick, Map<Integer, Seen> latest)
	{
		int target = tick.player == null ? WaveLog.TARGET_NONE : tick.player.getTarget();
		if (target < 0)
		{
			return false;
		}
		for (WaveLog.NpcTick n : tick.npcs)
		{
			if (n.getIndex() != target || (!SIZES.containsKey(n.getNpcId()) && !FREMS.containsKey(n.getNpcId())))
			{
				continue;
			}
			Seen s = latest.get(n.getIndex());
			return n.getHealthRatio() != 0 && (s == null || s.death == null || s.death > tick.t);
		}
		return false;
	}

	/** The hitsplat types of the player's own hits on an NPC, as the site's hit rates read them: blocked, damage and a max hit. */
	private static final Set<Integer> PLAYER_HITSPLATS = new HashSet<>(Arrays.asList(12, 16, 43));

	/** Whether the player attacked on the tick: an attack animation of theirs started, or a hit of theirs landed on an NPC. */
	private static boolean attacked(WaveLog.Tick tick)
	{
		if (startedAttack(tick))
		{
			return true;
		}
		for (List<WaveLog.Hit> hits : tick.npcHits.values())
		{
			for (WaveLog.Hit h : hits)
			{
				if (PLAYER_HITSPLATS.contains(h.getType()))
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Whether the player started an attack on the tick: an animation of theirs began, eating and drinking left out. */
	private static boolean startedAttack(WaveLog.Tick tick)
	{
		for (int a : tick.playerAnimations)
		{
			if (a != -1 && a != EAT_ANIMATION)
			{
				return true;
			}
		}
		return false;
	}

	private static boolean eating(WaveLog.Tick tick)
	{
		return tick.playerAnimations.contains(EAT_ANIMATION);
	}

	/**
	 * The death recap: the last RECAP_TICKS ticks up to the one the player died on, each with its hitpoints, its prayer and
	 * what happened. A tick with hits says what dealt them and how much; one without, an attack the prayer blocked, an
	 * attack that got past it, whose hit lands on a later line, or a special thrown at the player's tile.
	 */
	private static List<RecapLine> recap(List<WaveLog.Tick> ticks, Map<Integer, WaveLog.Tick> byT, int died, List<Attack> attacks, List<Hit> hits)
	{
		List<RecapLine> out = new ArrayList<>();
		for (WaveLog.Tick tick : ticks)
		{
			if (tick.t > died || tick.t <= died - RECAP_TICKS)
			{
				continue;
			}
			Integer hp = tick.player == null ? null : tick.player.getHitpoints();
			String prayer = PrayerFlicks.protectionOn(tick, byT.get(tick.t - 1), byT.get(tick.t + 1));
			Map<String, Integer> landed = new LinkedHashMap<>();
			for (Hit h : hits)
			{
				if (h.tick == tick.t)
				{
					landed.merge(h.text(), h.damage, Integer::sum);
				}
			}
			if (!landed.isEmpty())
			{
				int total = 0;
				for (int d : landed.values())
				{
					total += d;
				}
				out.add(new RecapLine(tick.t, hp, prayer, joined(new ArrayList<>(landed.keySet())), String.valueOf(total)));
				continue;
			}
			List<String> blocked = new ArrayList<>();
			List<String> thrown = new ArrayList<>();
			List<String> past = new ArrayList<>();
			for (Attack a : attacks)
			{
				if (a.special && a.tick == tick.t)
				{
					thrown.add(a.name + " threw its special at your tile");
				}
				else if (a.blocked() && a.checked == tick.t)
				{
					blocked.add(a.text());
				}
				else if (!a.special && a.checked == tick.t)
				{
					past.add(a.text());
				}
			}
			if (!blocked.isEmpty())
			{
				out.add(new RecapLine(tick.t, hp, prayer, joined(blocked), "blocked"));
			}
			else if (!past.isEmpty())
			{
				out.add(new RecapLine(tick.t, hp, prayer, joined(past) + ", past your prayer", "\u2013"));
			}
			else if (!thrown.isEmpty())
			{
				out.add(new RecapLine(tick.t, hp, prayer, joined(thrown), "–"));
			}
			else
			{
				out.add(new RecapLine(tick.t, hp, prayer, "Nothing hit you", "–"));
			}
		}
		return out;
	}

	/**
	 * What killed the player, in a sentence: the hit of the tick they died on and their hitpoints before it, after the
	 * other attacks that got past their prayer and hit them in the recap's ticks before it.
	 */
	private static String cause(Map<Integer, WaveLog.Tick> byT, int died, List<Attack> attacks, List<Hit> hits)
	{
		List<String> killing = new ArrayList<>();
		Set<Attack> killers = new HashSet<>();
		int damage = 0;
		for (Hit h : hits)
		{
			if (h.tick == died)
			{
				if (!killing.contains(h.noun()))
				{
					killing.add(h.noun());
				}
				if (h.attack != null)
				{
					killers.add(h.attack);
				}
				damage += h.damage;
			}
		}
		if (killing.isEmpty())
		{
			return "You died on tick " + died + ", with no hit in the log on it.";
		}
		WaveLog.Tick before = byT.get(died - 1);
		Integer hp = before == null || before.player == null ? null : before.player.getHitpoints();
		List<String> past = new ArrayList<>();
		for (Attack a : attacks)
		{
			boolean got = a.prayed == Prayed.WRONG || a.prayed == Prayed.NONE || a.prayed == Prayed.LATE || a.prayed == Prayed.EARLY;
			if (got && a.damage > 0 && !killers.contains(a) && a.checked < died && a.checked > died - RECAP_TICKS)
			{
				past.add("the " + a.text() + " on tick " + a.checked);
			}
		}
		String at = hp == null ? "" : " at " + plural(hp, "hitpoint");
		String after = past.isEmpty() ? "" : ", after " + joined(past) + " got past your prayer";
		String who = joined(killing);
		return who.substring(0, 1).toUpperCase(Locale.ROOT) + who.substring(1) + " hit you for " + damage + at + after + ".";
	}

	/** "a", "a and b", "a, b and c". */
	static String joined(List<String> parts)
	{
		if (parts.size() <= 1)
		{
			return parts.isEmpty() ? "" : parts.get(0);
		}
		return String.join(", ", parts.subList(0, parts.size() - 1)) + " and " + parts.get(parts.size() - 1);
	}

	static String plural(int n, String one)
	{
		return n + " " + (n == 1 ? one : one + "s");
	}

	/**
	 * The ticks from an attack's start to its hitsplat on the player that may match it, the likeliest first, as the site's
	 * hitDelays has them from Stefan's logs: the shaman's 3 within 7 tiles and 4 from 9, either at 8; the Javelin's 3; a
	 * Manticore orb's 1 within 7 tiles, else 1 or 2; the Shockwave Colossus's later the further off; and windows round a
	 * guess for the melee NPCs and a Javelin special's landing.
	 */
	private static int[] hitDelays(int npcId, boolean special, Integer distance)
	{
		if (special)
		{
			return new int[] {JAVELIN_SPECIAL_LANDING, JAVELIN_SPECIAL_LANDING + 1};
		}
		switch (npcId)
		{
			case NpcKind.SHAMAN_ID:
				return distance == null || distance == 8 ? new int[] {3, 4} : distance < 8 ? new int[] {3} : new int[] {4};
			case NpcKind.JAVELIN_ID:
				return new int[] {3};
			case NpcKind.MANTICORE_ID:
				return distance != null && distance <= 7 ? new int[] {1} : new int[] {1, 2};
			case NpcKind.SHOCKWAVE_ID:
				if (distance == null)
				{
					return new int[] {3, 4, 5, 6, 2};
				}
				int delay = 3 + Math.max(0, distance - 1) / 4;
				return new int[] {delay, delay - 1, delay + 1};
			case NpcKind.JAGUAR_ID:
				return new int[] {1, 0, 2};
			case NpcKind.MINOTAUR_ID:
			case NpcKind.MINOTAUR_RED_FLAG_ID:
				int check = PrayerFlicks.MINOTAUR_PRAYER_DELAY;
				return new int[] {check + 1, check, check + 2};
			default:
				return new int[0];
		}
	}

	private static int indexOf(int[] values, int value)
	{
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] == value)
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * The Volatility explosions at `tier` that go off on the tick, each first shown on it, with the player in their square
	 * as the tick began or as it ended, as the site's blastsOn finds them: a square reaches VOLATILITY_REACH tiles beyond
	 * the footprint of the NPC it came of, the one on its tile with no health left. Each is said as its mistake says it.
	 */
	private static List<String> blastsOn(WaveLog.Tick tick, WaveLog.Tick before, int tier)
	{
		List<String> out = new ArrayList<>();
		if (tier <= 0 || before == null)
		{
			return out;
		}
		int reach = VOLATILITY_REACH[Math.min(3, tier)];
		for (WaveLog.NpcTick blast : tick.npcs)
		{
			Integer size = EXPLOSIONS.get(blast.getNpcId());
			if (size == null || npcOf(before, blast.getIndex()) != null)
			{
				continue;
			}
			WaveLog.NpcTick dead = null;
			for (List<WaveLog.NpcTick> npcs : Arrays.asList(tick.npcs, before.npcs))
			{
				for (WaveLog.NpcTick n : npcs)
				{
					if (dead == null && n.getTile().equals(blast.getTile()) && n.getHealthRatio() == 0 && !EXPLOSIONS.containsKey(n.getNpcId()))
					{
						dead = n;
					}
				}
			}
			int across = size > 0 ? size : dead != null && SIZES.containsKey(dead.getNpcId()) ? SIZES.get(dead.getNpcId()) : 1;
			int x = blast.getTile().getX();
			int y = blast.getTile().getY();
			if (inside(tick, x, y, across, reach) || inside(before, x, y, across, reach))
			{
				out.add(dead == null ? "Volatility blast" : "Volatility blast from the " + shortName(dead.getNpcId()) + " you killed");
			}
		}
		return out;
	}

	private static boolean inside(WaveLog.Tick tick, int x, int y, int across, int reach)
	{
		GridTile p = tick.player == null ? null : tick.player.getTile();
		return p != null && p.getX() >= x - reach && p.getX() <= x + across - 1 + reach && p.getY() >= y - across + 1 - reach && p.getY() <= y + reach;
	}

	/**
	 * The tiles between an NPC's footprint, its south-west tile and `size` tiles east and north (north is a lower y on the
	 * grid), and a tile, the larger of the two ways.
	 */
	static int distanceTo(GridTile npc, int size, GridTile tile)
	{
		int dx = Math.max(Math.max(npc.getX() - tile.getX(), 0), tile.getX() - (npc.getX() + size - 1));
		int dy = Math.max(Math.max(npc.getY() - size + 1 - tile.getY(), 0), tile.getY() - npc.getY());
		return Math.max(dx, dy);
	}

	/** The NPC's name as the report says it, "East" or "West" before it where another of its kind is on the tick. */
	private static String nameOf(WaveLog.Tick tick, WaveLog.NpcTick npc)
	{
		String name = shortName(npc.getNpcId());
		WaveLog.NpcTick twin = null;
		int others = 0;
		for (WaveLog.NpcTick n : tick.npcs)
		{
			if (n.getIndex() != npc.getIndex() && n.getNpcId() == npc.getNpcId() && n.getHealthRatio() != 0)
			{
				twin = n;
				others++;
			}
		}
		if (others != 1)
		{
			return name;
		}
		int dx = npc.getTile().getX() - twin.getTile().getX();
		int dy = npc.getTile().getY() - twin.getTile().getY();
		if (dx != 0)
		{
			return (dx < 0 ? "West " : "East ") + name;
		}
		return dy == 0 ? name : (dy < 0 ? "North " : "South ") + name;
	}

	/** The NPC's short name, as the report's lines say it. */
	static String shortName(int npcId)
	{
		switch (npcId)
		{
			case NpcKind.SHAMAN_ID:
				return "Shaman";
			case NpcKind.JAVELIN_ID:
				return "Javelin";
			case NpcKind.JAGUAR_ID:
				return "Jaguar";
			case NpcKind.MANTICORE_ID:
				return "Manticore";
			case NpcKind.MINOTAUR_ID:
			case NpcKind.MINOTAUR_RED_FLAG_ID:
				return "Minotaur";
			case NpcKind.SHOCKWAVE_ID:
				return "Shockwave";
			default:
				String[] frem = FREMS.get(npcId);
				return frem == null ? "NPC" : frem[0];
		}
	}

	/** The picture of an NPC's face by its id, by the plugin's icon names: "npc-javelin", "npc-archer"; null for none. */
	static String iconOf(int npcId)
	{
		String name = shortName(npcId);
		return "NPC".equals(name) ? null : "npc-" + name.toLowerCase(Locale.ROOT);
	}

	/** What an NPC's attack is called: "spell", "throw", "magic orb", "clap", "claws", "swing", "ranged attack". */
	private static String what(int npcId, String style)
	{
		switch (npcId)
		{
			case NpcKind.SHAMAN_ID:
				return "spell";
			case NpcKind.JAVELIN_ID:
				return "throw";
			case NpcKind.MANTICORE_ID:
				return style + " orb";
			case NpcKind.SHOCKWAVE_ID:
				return "clap";
			case NpcKind.JAGUAR_ID:
				return "claws";
			case NpcKind.MINOTAUR_ID:
			case NpcKind.MINOTAUR_RED_FLAG_ID:
				return "swing";
			default:
				return style + " attack";
		}
	}

	/** The style of an NPC's attacks, as PrayerFlicks has them; a Manticore's come from its orbs. */
	private static String styleOf(int npcId)
	{
		switch (npcId)
		{
			case NpcKind.JAVELIN_ID:
				return "ranged";
			case NpcKind.JAGUAR_ID:
			case NpcKind.MINOTAUR_ID:
			case NpcKind.MINOTAUR_RED_FLAG_ID:
				return "melee";
			default:
				return "magic";
		}
	}

	private static WaveLog.NpcTick npcOf(WaveLog.Tick tick, int index)
	{
		for (WaveLog.NpcTick n : tick.npcs)
		{
			if (n.getIndex() == index)
			{
				return n;
			}
		}
		return null;
	}

	/** The attack animation the NPC started on the tick, the last of them, or -1 for none. */
	private static int attackStarted(WaveLog.Tick tick, WaveLog.NpcTick npc)
	{
		int started = -1;
		for (int[] a : tick.animations)
		{
			if (a[0] == npc.getIndex() && AttackAnimations.isAttack(npc.getNpcId(), a[1]))
			{
				started = a[1];
			}
		}
		return started;
	}

	private static boolean started(WaveLog.Tick tick, int index, int animation)
	{
		for (int[] a : tick.animations)
		{
			if (a[0] == index && a[1] == animation)
			{
				return true;
			}
		}
		return false;
	}

	/** The deaths the logs showed: the shaman's, the Jaguar warrior's, the Javelin Colossus's and the Manticore's. */
	private static final Set<Integer> DEATHS = new HashSet<>(Arrays.asList(10860, 10849, 10894, 10866));

	private static boolean dies(WaveLog.Tick tick, WaveLog.NpcTick npc)
	{
		if (DEATHS.contains(npc.getAnimation()))
		{
			return true;
		}
		for (int[] a : tick.animations)
		{
			if (a[0] == npc.getIndex() && DEATHS.contains(a[1]))
			{
				return true;
			}
		}
		return false;
	}

	/** The orbs a Manticore fired: the orb graphics it showed on the tick before and no longer shows, in the order shown. */
	private static List<String> orbsFired(WaveLog.NpcTick before, WaveLog.NpcTick now)
	{
		List<String> out = new ArrayList<>();
		Set<Integer> still = new HashSet<>();
		for (int g : now.getSpotAnims())
		{
			still.add(g);
		}
		for (int graphic : before.getSpotAnims())
		{
			String style = graphic == 2681 ? "magic" : graphic == 2683 ? "ranged" : graphic == 2685 ? "melee" : null;
			if (style != null && !still.contains(graphic))
			{
				out.add(style);
			}
		}
		return out;
	}
}
