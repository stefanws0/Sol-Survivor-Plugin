package nl.dexanddice.solsurvivor.core;

import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntToLongFunction;

/**
 * "Am I making a profit" for one run (Stefan's ask of 2026-11-04): what the reward chest gave against what the run used
 * up, both at the Grand Exchange's prices, which the plugin's RuneLite side passes in. A death in the arena loses the
 * chest (the site's src/viewer/chest.ts) and is not safe (Stefan, 2026-11-04): the items not kept go to a grave in the
 * Colosseum's lobby, whose fee is estimated (DeathFee). So a run the player died in costs its supplies and the fee, and
 * gains nothing.
 *
 * It follows a run from the plugin's first tick in the arena to its last. What the player carries, the inventory and
 * what they wear together, is taken as the run starts and as each tick ends; what it used up is the value carried at the
 * start less the value carried at the end. The chest's loot is what leaves the chest while the player is alive: Bank-all
 * and taking items alike. Loot taken into the inventory raises what is carried at the end, so what the inventory gained of
 * an item on the tick it left the chest, up to what left it, is taken back off before the supplies are counted. What is
 * carried stops changing at a death, so that what went to the grave is not counted as used. The runes in a rune pouch
 * count as carried. Charges inside a weapon, a blowpipe's scales and darts or a staff's runes, are not items: they are
 * estimated from the attacks the player made with it (ChargeCosts).
 *
 * Item ids are the RuneLite side's canonical ones, noted and placeholder items folded onto the item itself.
 */
public final class RunProfit
{
	private final long loot;
	private final long supplies;
	private final long deathFee;
	private final boolean died;
	private final Integer wave;

	public RunProfit(long loot, long supplies, long deathFee, boolean died, Integer wave)
	{
		this.loot = loot;
		this.supplies = supplies;
		this.deathFee = deathFee;
		this.died = died;
		this.wave = wave;
	}

	/** What the chest gave, in coins. */
	public long getLoot()
	{
		return loot;
	}

	/** What the run used up, in coins, never below nothing. */
	public long getSupplies()
	{
		return supplies;
	}

	/** What the grave asks after a death, estimated (DeathFee); nothing without one. */
	public long getDeathFee()
	{
		return deathFee;
	}

	/** Whether the player died in the arena, which loses the chest. */
	public boolean isDied()
	{
		return died;
	}

	/** The last wave the run reached, or null when the plugin saw none start. */
	public Integer getWave()
	{
		return wave;
	}

	/** The loot less the supplies and any death's fee. */
	public long getProfit()
	{
		return loot - supplies - deathFee;
	}

	/** Whether the run did anything to count: a walk into the arena and out again does not. */
	public boolean counts()
	{
		return died || loot != 0 || supplies != 0 || deathFee != 0;
	}

	/**
	 * Follows one run as the RuneLite side sees it, on the client thread. Not thread-safe; a new one for each run.
	 */
	public static final class Tracker
	{
		private final Map<Integer, Integer> start;
		private Map<Integer, Integer> carried;
		private Map<Integer, Integer> chest;
		private final Map<Integer, Integer> claimed = new HashMap<>();
		/** The part of the claimed loot that went into the inventory. */
		private final Map<Integer, Integer> taken = new HashMap<>();
		/** The attacks made with each weapon, by its name, for the charges inside it (ChargeCosts). */
		private final Map<String, Integer> attacks = new HashMap<>();
		private boolean died;
		private int kept;
		private boolean discounted;
		private boolean ironman;

		/**
		 * A run that starts with `carried` and the chest as the game holds it now (`chest`): empty after a run that ended
		 * in a death, as the chest that died with the player is no one's to claim, whatever the game still shows of it.
		 */
		public Tracker(Map<Integer, Integer> carried, Map<Integer, Integer> chest)
		{
			this.start = new HashMap<>(carried);
			this.carried = new HashMap<>(carried);
			this.chest = new HashMap<>(chest);
		}

		/**
		 * What the player carries and the chest holds as a tick ends. What left the chest while the player lives is loot
		 * claimed, and as much of it as the inventory gained on the same tick went into the inventory: taken, not banked.
		 */
		public void onTick(Map<Integer, Integer> nowCarried, Map<Integer, Integer> nowChest)
		{
			if (!died)
			{
				for (Map.Entry<Integer, Integer> was : chest.entrySet())
				{
					int left = was.getValue() - nowChest.getOrDefault(was.getKey(), 0);
					if (left > 0)
					{
						claimed.merge(was.getKey(), left, Integer::sum);
						int gained = nowCarried.getOrDefault(was.getKey(), 0) - carried.getOrDefault(was.getKey(), 0);
						if (gained > 0)
						{
							taken.merge(was.getKey(), Math.min(left, gained), Integer::sum);
						}
					}
				}
			}
			if (!died)
			{
				carried = new HashMap<>(nowCarried);
			}
			chest = new HashMap<>(nowChest);
		}

		/** An attack with the weapon of that name, which may use up charges inside it. */
		public void onAttack(String weapon)
		{
			if (!died && weapon != null)
			{
				attacks.merge(weapon, 1, Integer::sum);
			}
		}

		/**
		 * The player died in the arena: the chest is lost, nothing that leaves it after is theirs, and what they carried as
		 * they died is what the grave's fee is for, `kept` items kept, with the Colosseum's discount while `discounted`.
		 */
		public void onDeath(int kept, boolean discounted, boolean ironman)
		{
			if (died)
			{
				return;
			}
			died = true;
			this.kept = kept;
			this.discounted = discounted;
			this.ironman = ironman;
		}

		/** Whether the player died in this run. */
		public boolean isDied()
		{
			return died;
		}

		/**
		 * The run as it ended, at the prices `price` gives each item id, for each one of it. A death's fee (DeathFee) is at
		 * `guidePrice`, the Grand Exchange's own guide prices, which the grave goes by, and `stackable` says which items
		 * stack.
		 */
		public RunProfit finish(IntToLongFunction price, IntToLongFunction guidePrice, IntPredicate stackable, Integer wave)
		{
			// The loot taken into the inventory raises what is carried at the end: it is loot, not supplies left unused.
			long used = Math.max(0, value(start, price) - value(carried, price) + value(taken, price));
			double charges = 0;
			for (Map.Entry<String, Integer> a : attacks.entrySet())
			{
				for (Map.Entry<Integer, Double> each : ChargeCosts.perAttack(a.getKey()).entrySet())
				{
					charges += price.applyAsLong(each.getKey()) * each.getValue() * a.getValue();
				}
			}
			long fee = died ? DeathFee.estimate(carried, guidePrice, stackable, kept, discounted, ironman) : 0;
			return new RunProfit(value(claimed, price), used + Math.round(charges), fee, died, wave);
		}

		private static long value(Map<Integer, Integer> items, IntToLongFunction price)
		{
			long total = 0;
			for (Map.Entry<Integer, Integer> i : items.entrySet())
			{
				total += price.applyAsLong(i.getKey()) * i.getValue();
			}
			return total;
		}
	}
}
