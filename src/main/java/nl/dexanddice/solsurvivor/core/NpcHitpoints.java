package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Each of the wave's NPCs' hitpoints, for the stone tags over their heads (NpcHitpointsOverlay). The game sends no NPC's
 * hitpoints, only its health bar's ratio of a scale while the bar shows, so they are counted: an NPC that spawns starts
 * at its kind's full hitpoints (NpcKind), each hitsplat of damage on it takes its amount off, and each heal adds its
 * amount back. Whenever the game shows the bar, its ratio gives the range the hitpoints must lie in, by RuneLite's own
 * reading of the bar (as its opponent information does): a count inside the range stands, and one outside it, as the
 * Minotaur's regeneration leaves it, which shows no hitsplat, is moved to the range's nearest end and marked an estimate
 * until a bar pins it again. An NPC first seen after it spawned, the plugin turned on mid-wave, starts as an estimate at
 * full. Whether the count meets the game's own on every wave is unconfirmed: item 40 of docs/manual-steps.md checks it.
 *
 * The player's own hits are forecast too, for the tag's "172 › 131" (the arrow forecast of the predicted hit designs of
 * 2026-11-08, the user's choice): the game gives the experience for an attack as it is thrown, a tick or more before its
 * hitsplat lands, and each point of damage gives 4/3 Hitpoints experience whatever the style or spell, times the NPC's
 * bonus (NpcKind.getXpBonus). The client shows whole experience while the game keeps tenths, so a forecast can be a
 * point out. Each forecast waits until the player's hitsplats on the NPC take it off, and goes once its hit has landed
 * (a tick's grace for a special's later splats) or after FORECAST_TICKS whatever lands.
 */
public final class NpcHitpoints
{
	/** How many ticks a heal's amount shows beside the hitpoints after it lands. */
	public static final int HEAL_SHOWN_TICKS = 3;
	/** How many ticks a forecast hit waits for its hitsplat at most: a spell from 10 tiles lands within 4. */
	public static final int FORECAST_TICKS = 5;

	/** One NPC's hitpoints as the tag shows them. */
	public static final class Reading
	{
		public final int hitpoints;
		public final int max;
		/** The heals that landed in the last HEAL_SHOWN_TICKS ticks, 0 with none, which the tag shows in pink. */
		public final int healed;
		/** Whether the hitpoints come from the health bar rather than the count of every hitsplat since it spawned. */
		public final boolean estimated;
		/** The player's hits thrown at it that have not landed yet, as their experience gives them, 0 with none. */
		public final int incoming;

		Reading(int hitpoints, int max, int healed, boolean estimated)
		{
			this(hitpoints, max, healed, estimated, 0);
		}

		Reading(int hitpoints, int max, int healed, boolean estimated, int incoming)
		{
			this.hitpoints = hitpoints;
			this.max = max;
			this.healed = healed;
			this.estimated = estimated;
			this.incoming = incoming;
		}

		/** The same reading with no hit on its way, for the tag with the forecast set off. */
		public Reading withoutIncoming()
		{
			return incoming == 0 ? this : new Reading(hitpoints, max, healed, estimated, 0);
		}

		/** Its hitpoints once the hits on their way land, 0 when they kill it. */
		public int after()
		{
			return Math.max(0, hitpoints - incoming);
		}

		/** A quarter of its hitpoints or fewer left, which the tag shows in red. */
		public boolean low()
		{
			return hitpoints * 4 <= max;
		}
	}

	/** One hit the player threw: what is left of it to land, the tick it was thrown, and the tick it first landed. */
	private static final class Forecast
	{
		int amount;
		final int thrown;
		int landed = -1;

		Forecast(int amount, int thrown)
		{
			this.amount = amount;
			this.thrown = thrown;
		}
	}

	private static final class Entry
	{
		final int max;
		final int xpBonus;
		int hitpoints;
		boolean estimated;
		int healed;
		int healTick;
		final List<Forecast> forecasts = new ArrayList<>();

		Entry(NpcKind kind, boolean estimated)
		{
			this.max = kind.getHitpoints();
			this.xpBonus = kind.getXpBonus();
			this.hitpoints = max;
			this.estimated = estimated;
		}
	}

	private final Map<Integer, Entry> npcs = new HashMap<>();

	/** An NPC spawned: one of a kind the planner knows starts at its full hitpoints, counted exactly from here. */
	public void onSpawned(int index, int npcId)
	{
		NpcKind kind = NpcKind.forNpcId(npcId, false);
		if (kind == null)
		{
			npcs.remove(index);
			return;
		}
		npcs.put(index, new Entry(kind, false));
	}

	public void onDespawned(int index)
	{
		npcs.remove(index);
	}

	/** The player left the arena: nothing counted there holds on their return. */
	public void clear()
	{
		npcs.clear();
	}

	/**
	 * A hitsplat on an NPC on `tick`: a heal adds its amount, up to full, and any other with an amount above 0 that is
	 * damage (ColosseumTracker.isDamage) takes it off, down to 0.
	 */
	public void onHit(int index, int amount, int type, int tick)
	{
		Entry entry = npcs.get(index);
		if (entry == null || amount <= 0)
		{
			return;
		}
		if (type == ColosseumTracker.HEAL_HITSPLAT)
		{
			entry.hitpoints = Math.min(entry.max, entry.hitpoints + amount);
			entry.healed = tick - entry.healTick < HEAL_SHOWN_TICKS ? entry.healed + amount : amount;
			entry.healTick = tick;
		}
		else if (ColosseumTracker.isDamage(amount, type))
		{
			entry.hitpoints = Math.max(0, entry.hitpoints - amount);
		}
	}

	/**
	 * An NPC on a tick, with its health bar's ratio of `scale`, or a ratio below 0 while the game hides the bar. One of a
	 * kind the planner knows that the plugin did not see spawn starts here, as an estimate at full. A bar shown sets the
	 * range the hitpoints lie in (range), and the count is kept inside it.
	 */
	public void onTick(int index, int npcId, int ratio, int scale)
	{
		Entry entry = npcs.get(index);
		if (entry == null)
		{
			NpcKind kind = NpcKind.forNpcId(npcId, false);
			if (kind == null)
			{
				return;
			}
			entry = new Entry(kind, true);
			npcs.put(index, entry);
		}
		if (ratio < 0 || scale <= 0 || ratio > scale)
		{
			return;
		}
		int[] range = range(entry.max, ratio, scale);
		if (entry.hitpoints < range[0] || entry.hitpoints > range[1])
		{
			entry.hitpoints = Math.max(range[0], Math.min(range[1], entry.hitpoints));
			entry.estimated = range[0] != range[1];
		}
		else if (range[0] == range[1])
		{
			entry.estimated = false;
		}
	}

	/** An NPC's hitpoints on `tick`, or null for one the plugin does not count (a frem, Sol Heredit, an orb). */
	public Reading reading(int index, int tick)
	{
		Entry entry = npcs.get(index);
		if (entry == null)
		{
			return null;
		}
		int healed = tick - entry.healTick < HEAL_SHOWN_TICKS ? entry.healed : 0;
		prune(entry, tick);
		int incoming = 0;
		for (Forecast forecast : entry.forecasts)
		{
			incoming += forecast.amount;
		}
		return new Reading(entry.hitpoints, entry.max, healed, entry.estimated, incoming);
	}

	/**
	 * The player gained `xp` Hitpoints experience on `tick` attacking the NPC at `index`: the hit it gives is forecast on
	 * that NPC until it lands. Nothing is forecast on an NPC the plugin does not count.
	 */
	public void onHitpointsXp(int index, int xp, int tick)
	{
		Entry entry = npcs.get(index);
		if (entry == null || xp <= 0)
		{
			return;
		}
		int damage = forecast(xp, entry.xpBonus);
		if (damage > 0)
		{
			prune(entry, tick);
			entry.forecasts.add(new Forecast(damage, tick));
		}
	}

	/**
	 * One of the player's own hitsplats of damage landed on the NPC at `index` on `tick`: it takes its amount off the
	 * oldest forecasts, which go once a tick has passed since they first landed, or at once when what is left is within
	 * the experience's rounding.
	 */
	public void onMyHit(int index, int amount, int tick)
	{
		Entry entry = npcs.get(index);
		if (entry == null || amount <= 0)
		{
			return;
		}
		prune(entry, tick);
		for (Forecast forecast : entry.forecasts)
		{
			if (amount <= 0)
			{
				break;
			}
			if (forecast.thrown >= tick || forecast.amount <= 0)
			{
				continue;
			}
			int taken = Math.min(amount, forecast.amount);
			forecast.amount -= taken;
			amount -= taken;
			if (forecast.landed < 0)
			{
				forecast.landed = tick;
			}
			if (forecast.amount <= 1)
			{
				forecast.amount = 0;
			}
		}
		prune(entry, tick);
	}

	/**
	 * Drops the forecasts that are spent, those that landed before the last tick, whose rest the experience overstated
	 * (a spell that hit several NPCs at once), and those older than FORECAST_TICKS.
	 */
	private static void prune(Entry entry, int tick)
	{
		entry.forecasts.removeIf(f -> f.amount <= 0 || tick - f.thrown >= FORECAST_TICKS || (f.landed >= 0 && tick - f.landed >= 2));
	}

	/** The damage `xp` Hitpoints experience gives on an NPC with `bonus` percent bonus experience, to the nearest point. */
	static int forecast(int xp, int bonus)
	{
		return (int) Math.round(xp * 300.0 / (4.0 * (100 + bonus)));
	}

	/**
	 * The hitpoints, [lowest, highest], that a health bar showing `ratio` of `scale` allows for an NPC of `max` hitpoints:
	 * the game fills a bar only at full health and empties it only at 0, so the scale's other steps share the rest, as
	 * RuneLite's opponent information reads them.
	 */
	static int[] range(int max, int ratio, int scale)
	{
		if (ratio == 0)
		{
			return new int[] {0, 0};
		}
		if (ratio >= scale)
		{
			return new int[] {max, max};
		}
		if (scale == 1)
		{
			return new int[] {1, max};
		}
		int lowest = ratio > 1 ? (max * (ratio - 1) + scale - 2) / (scale - 1) : 1;
		int highest = (max * ratio - 1) / (scale - 1) + 1;
		return new int[] {Math.max(1, lowest), Math.min(max - 1, highest)};
	}
}
