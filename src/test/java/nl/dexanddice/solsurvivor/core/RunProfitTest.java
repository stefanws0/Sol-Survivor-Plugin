package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;
import java.util.function.IntToLongFunction;
import org.junit.Test;

/** A run's profit: the chest's loot claimed alive, less what the inventory and gear lost, at the prices given. */
public class RunProfitTest
{
	private static final int SHARK = 385;
	private static final int BREW_4 = 6685;
	private static final int BREW_3 = 6687;
	private static final int BOW = 20997;
	private static final int ARROWS = 11212;
	private static final int SPLINTERS = 28924;
	private static final int DEATH_RUNE = 560;

	private static final IntToLongFunction PRICE = id ->
	{
		switch (id)
		{
			case SHARK:
				return 800;
			case BREW_4:
				return 8_000;
			case BREW_3:
				return 6_000;
			case BOW:
				return 1_500_000_000L;
			case ARROWS:
				return 1_000;
			case SPLINTERS:
				return 100;
			case DEATH_RUNE:
				return 200;
			default:
				return 0;
		}
	};

	/** Which items stack: the arrows, the runes and the splinters. */
	private static final IntPredicate STACKS = id -> id == ARROWS || id == DEATH_RUNE || id == SPLINTERS;

	private static Map<Integer, Integer> items(int... idThenQuantity)
	{
		Map<Integer, Integer> m = new HashMap<>();
		for (int i = 0; i < idThenQuantity.length; i += 2)
		{
			m.put(idThenQuantity[i], idThenQuantity[i + 1]);
		}
		return m;
	}

	@Test
	public void aBankedChestLessTheSuppliesEatenDrunkAndShot()
	{
		RunProfit.Tracker run = new RunProfit.Tracker(items(SHARK, 10, BREW_4, 4, BOW, 1, ARROWS, 500), items());
		Map<Integer, Integer> start = items(SHARK, 10, BREW_4, 4, BOW, 1, ARROWS, 500);
		run.onTick(start, items(SPLINTERS, 300));
		// Four sharks eaten, a brew sipped once and 120 arrows shot.
		Map<Integer, Integer> end = items(SHARK, 6, BREW_4, 3, BREW_3, 1, BOW, 1, ARROWS, 380);
		run.onTick(end, items(SPLINTERS, 800, DEATH_RUNE, 1_000));
		// Bank-all: everything leaves the chest and none of it reaches the inventory.
		run.onTick(end, items());
		RunProfit p = run.finish(PRICE, PRICE, STACKS, 9);
		assertEquals(800 * 100 + 1_000 * 200, p.getLoot());
		assertEquals(4 * 800 + (8_000 - 6_000) + 120 * 1_000, p.getSupplies());
		assertEquals(p.getLoot() - p.getSupplies(), p.getProfit());
		assertFalse(p.isDied());
		assertEquals(Integer.valueOf(9), p.getWave());
		assertTrue(p.counts());
	}

	@Test
	public void lootTakenIntoTheInventoryIsLootNotSuppliesSaved()
	{
		RunProfit.Tracker run = new RunProfit.Tracker(items(SHARK, 10, DEATH_RUNE, 100), items());
		// Two sharks eaten and 40 death runes cast, the same as the chest holds.
		run.onTick(items(SHARK, 8, DEATH_RUNE, 60), items(SPLINTERS, 50, DEATH_RUNE, 500));
		// The chest taken into the inventory.
		run.onTick(items(SHARK, 8, DEATH_RUNE, 560, SPLINTERS, 50), items());
		RunProfit p = run.finish(PRICE, PRICE, STACKS, 5);
		assertEquals(50 * 100 + 500 * 200, p.getLoot());
		assertEquals(2 * 800 + 40 * 200, p.getSupplies());
	}

	@Test
	public void aDeathLosesTheChestAndCostsTheSupplies()
	{
		RunProfit.Tracker run = new RunProfit.Tracker(items(SHARK, 10, BOW, 1), items());
		run.onTick(items(SHARK, 2, BOW, 1), items(SPLINTERS, 800));
		run.onDeath(3, false, false);
		// The game empties the chest once the player has died, and the items go to the grave: none of that is used up.
		run.onTick(items(), items());
		RunProfit p = run.finish(PRICE, PRICE, STACKS, 7);
		assertTrue(p.isDied());
		assertEquals(0, p.getLoot());
		assertEquals(8 * 800, p.getSupplies());
		// The bow and both sharks are kept: nothing goes to the grave worth a fee.
		assertEquals(0, p.getDeathFee());
		assertEquals(-6_400, p.getProfit());
	}

	@Test
	public void aChestTheRunStartsWithIsClaimedWhenItEmpties()
	{
		// The plugin came back mid-run, after a login: the chest so far is in the game's container already.
		RunProfit.Tracker run = new RunProfit.Tracker(items(SHARK, 4), items(SPLINTERS, 400));
		run.onTick(items(SHARK, 4), items());
		RunProfit p = run.finish(PRICE, PRICE, STACKS, null);
		assertEquals(40_000, p.getLoot());
		assertEquals(0, p.getSupplies());
		assertNull(p.getWave());
	}

	@Test
	public void gearOnlyMovedBetweenTheInventoryAndTheBodyCostsNothing()
	{
		RunProfit.Tracker run = new RunProfit.Tracker(items(BOW, 1, SHARK, 3), items());
		run.onTick(items(SHARK, 3), items());
		run.onTick(items(BOW, 1, SHARK, 3), items());
		RunProfit p = run.finish(PRICE, PRICE, STACKS, 1);
		assertEquals(0, p.getSupplies());
		assertFalse(p.counts());
	}

	@Test
	public void itemsPickedUpNeverMakeTheSuppliesNegative()
	{
		RunProfit.Tracker run = new RunProfit.Tracker(items(SHARK, 1), items());
		run.onTick(items(SHARK, 1, ARROWS, 10), items());
		assertEquals(0, run.finish(PRICE, PRICE, STACKS, 1).getSupplies());
	}

	@Test
	public void chargesInsideAWeaponCountForEachAttack()
	{
		RunProfit.Tracker run = new RunProfit.Tracker(items(), items());
		for (int i = 0; i < 30; i++)
		{
			run.onAttack("Holy scythe of vitur");
		}
		run.onAttack("Dragon claws");
		run.onAttack(null);
		IntToLongFunction price = id -> id == ChargeCosts.BLOOD_RUNE ? 300 : id == ChargeCosts.VIAL_OF_BLOOD ? 1_000 : 0;
		// 2 blood runes and a hundredth of a vial a swing.
		assertEquals(30 * (2 * 300 + 10), run.finish(price, price, STACKS, 3).getSupplies());
	}

	@Test
	public void theGravesFeeIsForTheItemsNotKept()
	{
		Map<Integer, Integer> carried = items(BOW, 1, SHARK, 10, ARROWS, 500, DEATH_RUNE, 2_000);
		// The bow is kept; 500 arrows (500K) and 2,000 runes (400K) are a stack each and cost 1,000 each, the sharks nothing.
		assertEquals(2_000, DeathFee.estimate(carried, PRICE, STACKS, 1, false, false));
		assertEquals(500, DeathFee.estimate(carried, PRICE, STACKS, 1, true, false));
		assertEquals(250, DeathFee.estimate(carried, PRICE, STACKS, 1, true, true));
		// Four kept: the bow and both stacks, whole, and a shark.
		assertEquals(0, DeathFee.estimate(carried, PRICE, STACKS, 4, false, false));
		// Kept nothing, the bow's 1.5B costs 100,000 more.
		assertEquals(102_000, DeathFee.estimate(carried, PRICE, STACKS, 0, false, false));
		Map<Integer, Integer> rich = new HashMap<>();
		for (int id = 1; id <= 10; id++)
		{
			rich.put(id, 1);
		}
		assertEquals(DeathFee.CAP, DeathFee.estimate(rich, i -> 50_000_000L, STACKS, 0, false, false));
		assertEquals(125_000, DeathFee.estimate(rich, i -> 50_000_000L, STACKS, 0, true, false));
	}

	@Test
	public void eachWeaponsAttacksAreCountedAsItSpendsItsCharges()
	{
		// A cast whose target dies before it lands shows no hitsplat: the projectile counts it.
		assertEquals(ChargeCosts.Counting.PROJECTILE, ChargeCosts.counting("Tumeken's shadow"));
		assertEquals(ChargeCosts.Counting.PROJECTILE, ChargeCosts.counting("Toxic blowpipe"));
		assertEquals(ChargeCosts.Counting.DAMAGE, ChargeCosts.counting("Sanguine scythe of vitur"));
		assertEquals(ChargeCosts.Counting.HIT, ChargeCosts.counting("Blade of saeldor (c)"));
		assertEquals(ChargeCosts.Counting.NONE, ChargeCosts.counting("Twisted bow"));
		assertEquals(ChargeCosts.Counting.NONE, ChargeCosts.counting(null));
	}

	@Test
	public void itemsThatDoNotStackAreEachAnItemForTheGrave()
	{
		// Stefan's check in game, 2026-11-04: eight brews at 8,000 are eight free items, not one worth 64,000; and 20 at
		// 8,000 are not one item worth 160,000 either.
		assertEquals(0, DeathFee.estimate(items(BREW_4, 20), PRICE, STACKS, 0, false, false));
		// 200 arrows at 1,000 are one stack, worth 200,000.
		assertEquals(1_000, DeathFee.estimate(items(BREW_4, 20, ARROWS, 200), PRICE, STACKS, 0, false, false));
	}

	@Test
	public void aStackIsKeptWholeByTheValueOfTheWholeStack()
	{
		// Stefan's gravestone, 2026-11-04: a stack of thousands kept whole while dearer single items went to the grave.
		// 5,000 death runes at 200 are worth 1M, more than the 1,000-coin arrows the grave then charges.
		Map<Integer, Integer> carried = items(DEATH_RUNE, 5_000, ARROWS, 150, SHARK, 3);
		assertEquals(1_000, DeathFee.estimate(carried, PRICE, STACKS, 1, false, false));
	}

	@Test
	public void theGravesFeeGoesByTheGuidePriceNotTheLivePrice()
	{
		// Stefan's gravestone, 2026-11-04: an item over 1M at the live price but under it at the guide price costs 250
		// with the discount, not 2,500.
		RunProfit.Tracker run = new RunProfit.Tracker(items(BOW, 1, ARROWS, 1), items());
		run.onDeath(1, true, false);
		IntToLongFunction live = id -> id == BOW ? 2_000_000_000L : id == ARROWS ? 1_050_000 : 0;
		IntToLongFunction guide = id -> id == BOW ? 2_000_000_000L : id == ARROWS ? 980_000 : 0;
		assertEquals(250, run.finish(live, guide, STACKS, 6).getDeathFee());
		assertEquals(2_500, run.finish(live, live, STACKS, 6).getDeathFee());
	}
}
