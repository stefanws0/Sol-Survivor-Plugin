package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntToLongFunction;

/**
 * What a death in the Colosseum costs to reclaim, estimated (Stefan's word of 2026-11-04: the death is not safe, and
 * the grave is in the Colosseum's lobby). By the OSRS Wiki's Fortis Colosseum and Grave pages: the player keeps their
 * most valuable items, 3 or 4 with Protect Item, and the rest goes to a grave, whose fee is a flat amount for each item
 * by its value, nothing under 100K, 1,000 coins under 1M, 10,000 under 10M and 100,000 from 10M up, at most 500,000 in
 * all; an ironman pays half. Until the player has cleared 100 Colosseum waves the fee is a quarter of that, so at most
 * 125,000. The fee comes out of Death's coffer, or the bank, which the plugin cannot see paid: this is the fee as the
 * grave would ask it, at the Grand Exchange's own guide prices, which the grave goes by, not the live trade prices
 * RuneLite shows by default (Stefan's gravestone of 2026-11-04: the estimate 2,250 over the fee, with Protect Item and
 * without, as one item over 1M at the live price is under it at the guide price, so a 2,500 fee where the grave asked
 * 250). A stack is one item, worth the whole stack, which is kept or lost
 * whole; an item that does not stack is one item for each of it, so that eight brews are eight items each under 100K
 * and free, not one worth more.
 */
public final class DeathFee
{
	/** The most a grave asks before a discount. */
	public static final long CAP = 500_000;
	/** The waves a player clears before the Colosseum's discount ends. */
	public static final int DISCOUNT_WAVES = 100;

	private DeathFee()
	{
	}

	/**
	 * The fee for `carried` as the player died: the `kept` most valuable items stay with them, a stack counting as one
	 * item worth the whole stack, and what is left goes to the grave. `stackable` says which items stack. `discounted` while the player has cleared fewer than DISCOUNT_WAVES waves.
	 */
	public static long estimate(Map<Integer, Integer> carried, IntToLongFunction price, IntPredicate stackable, int kept, boolean discounted,
		boolean ironman)
	{
		// The grave's items: a stack is one item, worth the whole stack, and an item that does not stack is one for each
		// of it. The kept ones are the most valuable of these, a stack kept whole. Unconfirmed: the wiki does not say how
		// the game ranks a stack for keeping.
		List<long[]> items = new ArrayList<>();
		for (Map.Entry<Integer, Integer> item : carried.entrySet())
		{
			long each = price.applyAsLong(item.getKey());
			if (stackable.test(item.getKey()))
			{
				items.add(new long[] {each * item.getValue()});
			}
			else
			{
				for (int i = 0; i < item.getValue(); i++)
				{
					items.add(new long[] {each});
				}
			}
		}
		items.sort((x, y) -> Long.compare(y[0], x[0]));
		long fee = 0;
		for (int i = Math.max(0, kept); i < items.size(); i++)
		{
			fee += tier(items.get(i)[0]);
		}
		fee = Math.min(fee, CAP);
		if (discounted)
		{
			fee /= 4;
		}
		if (ironman)
		{
			fee /= 2;
		}
		return fee;
	}

	/** A grave's fee for one item (a stack) worth `value`. */
	static long tier(long value)
	{
		if (value < 100_000)
		{
			return 0;
		}
		if (value < 1_000_000)
		{
			return 1_000;
		}
		if (value < 10_000_000)
		{
			return 10_000;
		}
		return 100_000;
	}
}
