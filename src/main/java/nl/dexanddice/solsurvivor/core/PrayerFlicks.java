package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The player's prayer against the attacks at them in one wave's log, for the site's Prayer reliability, "Mine": how often
 * a flick of theirs comes a tick late. The same count as the site's run review makes of a log (src/viewer/prayerRecord.ts,
 * flickCounts, on the attacks src/viewer/runReview.ts finds), so that a wave counted here and one counted there agree;
 * src/test/resources/fixtures/prayer-flicks.json is a log both count.
 *
 * Each attack at the player, by the NPCs the site models (a Manticore's each orb) and the Fremennik warband, is one of:
 * <ul>
 * <li>a flick: the right protection prayer went on as the attack was checked, off or another the tick before;</li>
 * <li>held: the right prayer was on since the tick before, so no flick was at stake;</li>
 * <li>late: a flick that missed, the right prayer on a tick after the check, on the tick before and switched off on it,
 * or caught half-way, both prayers showing on the tick and the old one counting;</li>
 * <li>unprayed: no protection prayer on the tick before, the tick or the tick after, with prayer points left.</li>
 * </ul>
 * The rest are left out: a Javelin Colossus's special, which no prayer blocks; an attack met with another prayer and the
 * right one nowhere near, as tanking one is often the plan; and one checked on the same tick as another of another
 * style that the prayer blocked, as only one could be.
 */
public final class PrayerFlicks
{
	/** No attack prayed against. */
	public static final PrayerFlicks NONE = new PrayerFlicks(0, 0, 0, 0);

	/** A tick's protection prayer when none is on; null is a tick the log does not say. */
	static final String NO_PRAYER = "";

	/** The graphics a Manticore shows for its orbs as it charges them, by style: the site's runReview.ts, ORB_GRAPHICS. */
	private static final Map<Integer, String> ORB_GRAPHICS = new HashMap<>();

	/** The Fremennik warband's attack animations, by NPC id, and their styles: the site's waveLog.ts, FREMENNIK_WARBAND. */
	private static final Map<Integer, int[]> FREM_ATTACKS = new HashMap<>();
	private static final Map<Integer, String> FREM_STYLES = new HashMap<>();

	/** The deaths the logs showed: the shaman's, the Jaguar warrior's, the Javelin Colossus's and the Manticore's. */
	private static final int[] DEATHS = {10860, 10849, 10894, 10866};

	/** The ticks after a Minotaur's swing that its hit checks the prayer: the site's MINOTAUR_PRAYER_DELAY. */
	static final int MINOTAUR_PRAYER_DELAY = 1;

	static
	{
		ORB_GRAPHICS.put(2681, "magic");
		ORB_GRAPHICS.put(2683, "ranged");
		ORB_GRAPHICS.put(2685, "melee");
		FREM_ATTACKS.put(12814, new int[] {10850});
		FREM_ATTACKS.put(12815, new int[] {10853});
		FREM_ATTACKS.put(12816, new int[] {10856});
		FREM_STYLES.put(12814, "ranged");
		FREM_STYLES.put(12815, "magic");
		FREM_STYLES.put(12816, "melee");
	}

	private final int flicks;
	private final int held;
	private final int late;
	private final int unprayed;

	public PrayerFlicks(int flicks, int held, int late, int unprayed)
	{
		this.flicks = flicks;
		this.held = held;
		this.late = late;
		this.unprayed = unprayed;
	}

	public int getFlicks()
	{
		return flicks;
	}

	public int getHeld()
	{
		return held;
	}

	public int getLate()
	{
		return late;
	}

	public int getUnprayed()
	{
		return unprayed;
	}

	/** Whether any attack was prayed against, so that the wave says something of the player's prayer. */
	public boolean isEmpty()
	{
		return flicks + held + late + unprayed == 0;
	}

	/** These counts and `other`'s together. */
	public PrayerFlicks plus(PrayerFlicks other)
	{
		return new PrayerFlicks(flicks + other.flicks, held + other.held, late + other.late, unprayed + other.unprayed);
	}

	/** An attack at the player: its style, the tick its prayer was checked on and the protection prayer on then. */
	private static final class Attack
	{
		final String style;
		final int checked;
		final String prayer;

		Attack(String style, int checked, String prayer)
		{
			this.style = style;
			this.checked = checked;
			this.prayer = prayer;
		}
	}

	/** An NPC the count follows from tick to tick: its id, its style and the tick it died on, or null. */
	private static final class Seen
	{
		final int npcId;
		Integer death;

		Seen(int npcId)
		{
			this.npcId = npcId;
		}
	}

	/** The player's prayer in a wave's log. */
	public static PrayerFlicks count(WaveLog log)
	{
		return count(log.getTicks());
	}

	static PrayerFlicks count(List<WaveLog.Tick> ticks)
	{
		Map<Integer, WaveLog.Tick> byT = new HashMap<>();
		Integer died = null;
		for (WaveLog.Tick tick : ticks)
		{
			byT.put(tick.t, tick);
			if (died == null && tick.player != null && tick.player.getHitpoints() == 0)
			{
				died = tick.t;
			}
		}
		List<Attack> attacks = attacks(ticks, byT, died);

		int flicks = 0;
		int held = 0;
		int late = 0;
		int unprayed = 0;
		for (Attack a : attacks)
		{
			String before = protectionOn(byT.get(a.checked - 1), byT.get(a.checked - 2));
			if (a.style.equals(a.prayer))
			{
				if (before == null || before.equals(a.style))
				{
					held++;
				}
				else
				{
					flicks++;
				}
				continue;
			}
			if (blockedOther(attacks, a))
			{
				continue;
			}
			String after = protectionOn(byT.get(a.checked + 1), byT.get(a.checked));
			WaveLog.Tick at = byT.get(a.checked);
			boolean halfWay = at != null && at.player != null && at.player.getPrayers().contains(a.style);
			if (halfWay || a.style.equals(after) || a.style.equals(before))
			{
				late++;
			}
			else if (NO_PRAYER.equals(a.prayer) && NO_PRAYER.equals(before) && NO_PRAYER.equals(after) && at.player.getPrayerPoints() > 0)
			{
				unprayed++;
			}
		}
		return new PrayerFlicks(flicks, held, late, unprayed);
	}

	/** Whether the prayer on as `a` was checked blocked another attack of another style checked on the same tick. */
	private static boolean blockedOther(List<Attack> attacks, Attack a)
	{
		for (Attack b : attacks)
		{
			if (b != a && b.checked == a.checked && !b.style.equals(a.style) && b.style.equals(b.prayer))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * The attacks at the player that a prayer could block, a tick at a time, as the site's run review finds them: each NPC
	 * by its index, a new one where the id changes, the index was not on the tick before or a dead NPC's bar comes back;
	 * none by an NPC after the tick it died on, and none after the player died. A Manticore's attacks are its orbs: each
	 * graphic it showed on the tick before and no longer shows. The others' are their attack animations' starts, a
	 * Javelin's special left out.
	 */
	private static List<Attack> attacks(List<WaveLog.Tick> ticks, Map<Integer, WaveLog.Tick> byT, Integer died)
	{
		List<Attack> out = new ArrayList<>();
		Map<Integer, Seen> latest = new HashMap<>();
		WaveLog.Tick before = null;
		for (WaveLog.Tick tick : ticks)
		{
			for (WaveLog.NpcTick npc : tick.npcs)
			{
				WaveLog.NpcTick prior = before == null ? null : npcOf(before, npc.getIndex());
				Seen s = latest.get(npc.getIndex());
				boolean fresh = s == null || s.npcId != npc.getNpcId() || prior == null || (prior.getHealthRatio() == 0 && npc.getHealthRatio() > 0);
				if (fresh)
				{
					s = new Seen(npc.getNpcId());
					latest.put(npc.getIndex(), s);
				}
				NpcKind kind = NpcKind.forNpcId(npc.getNpcId(), false);
				boolean frem = FREM_STYLES.containsKey(npc.getNpcId());
				if (s.death == null && (npc.getHealthRatio() == 0 || (kind != null && dies(tick, npc))))
				{
					s.death = tick.t;
				}
				boolean playerDead = died != null && tick.t > died;
				if ((kind == null && !frem) || playerDead || (s.death != null && s.death < tick.t))
				{
					continue;
				}
				WaveLog.NpcTick previous = fresh ? null : prior;
				if (frem)
				{
					if (started(tick, npc.getIndex(), FREM_ATTACKS.get(npc.getNpcId())))
					{
						out.add(new Attack(FREM_STYLES.get(npc.getNpcId()), tick.t, protectionOn(tick, before)));
					}
					continue;
				}
				if (kind == NpcKind.MANTICORE)
				{
					if (s.death != null || previous == null || before.t != tick.t - 1)
					{
						continue;
					}
					for (String style : orbsFired(previous, npc))
					{
						out.add(new Attack(style, tick.t, protectionOn(tick, before)));
					}
					continue;
				}
				int animation = attackStarted(tick, npc);
				if (animation == -1 || AttackAnimations.isSpecial(npc.getNpcId(), animation))
				{
					continue;
				}
				int checked = tick.t + (kind == NpcKind.MINOTAUR ? MINOTAUR_PRAYER_DELAY : 0);
				String prayer = protectionOn(byT.get(checked), byT.get(checked - 1));
				if (prayer != null)
				{
					out.add(new Attack(styleOf(kind), checked, prayer));
				}
			}
			before = tick;
		}
		// An attack whose tick the log does not say is left out.
		out.removeIf(a -> a.prayer == null);
		return out;
	}

	/** The style of an NPC's attacks; a Manticore's come from its orbs. */
	private static String styleOf(NpcKind kind)
	{
		switch (kind)
		{
			case JAVELIN_COLOSSUS:
				return "ranged";
			case JAGUAR_WARRIOR:
			case MINOTAUR:
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

	/** Whether the NPC at `index` started one of `animations` on the tick. */
	private static boolean started(WaveLog.Tick tick, int index, int[] animations)
	{
		for (int[] a : tick.animations)
		{
			for (int animation : animations)
			{
				if (a[0] == index && a[1] == animation)
				{
					return true;
				}
			}
		}
		return false;
	}

	/** Whether the NPC shows a death on the tick, in its animation or among the tick's animation starts. */
	private static boolean dies(WaveLog.Tick tick, WaveLog.NpcTick npc)
	{
		return started(tick, npc.getIndex(), DEATHS) || contains(DEATHS, npc.getAnimation());
	}

	private static boolean contains(int[] values, int value)
	{
		for (int v : values)
		{
			if (v == value)
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
		int[] still = now.getSpotAnims();
		for (int graphic : before.getSpotAnims())
		{
			String style = ORB_GRAPHICS.get(graphic);
			if (style != null && !contains(still, graphic))
			{
				out.add(style);
			}
		}
		return out;
	}

	/**
	 * The player's protection prayer on a tick, by the style it blocks: NO_PRAYER for none, null where the log does not
	 * say. Two on at once is a switch the log caught half-way, and the one also on `before`, the tick before, still counts,
	 * as the site's runReview.ts has it from Stefan's wave 4 log of 2026-10-19.
	 */
	static String protectionOn(WaveLog.Tick tick, WaveLog.Tick before)
	{
		if (tick == null || tick.player == null)
		{
			return null;
		}
		List<String> on = new ArrayList<>();
		for (String prayer : tick.player.getPrayers())
		{
			if (prayer.equals("magic") || prayer.equals("ranged") || prayer.equals("melee"))
			{
				on.add(prayer);
			}
		}
		if (on.size() > 1)
		{
			String was = protectionOn(before, null);
			if (was != null && on.contains(was))
			{
				return was;
			}
		}
		return on.isEmpty() ? NO_PRAYER : on.get(0);
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof PrayerFlicks))
		{
			return false;
		}
		PrayerFlicks p = (PrayerFlicks) o;
		return flicks == p.flicks && held == p.held && late == p.late && unprayed == p.unprayed;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(flicks, held, late, unprayed);
	}

	@Override
	public String toString()
	{
		return flicks + " flicks, " + held + " held, " + late + " late, " + unprayed + " unprayed";
	}
}
