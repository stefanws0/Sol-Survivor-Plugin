package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The runs' profit added up, kept as text between sessions, and coins written as short as the game's stacks. */
public class ProfitRecordTest
{
	@Test
	public void runsAddUpAndTheLastIsKept()
	{
		ProfitRecord r = ProfitRecord.empty()
			.with(new RunProfit(2_000_000, 300_000, 0, false, 12))
			.with(new RunProfit(0, 150_000, 5_000, true, 7));
		assertEquals(2, r.getRuns());
		assertEquals(1, r.getDeaths());
		assertEquals(2_000_000, r.getLoot());
		assertEquals(450_000, r.getSupplies());
		assertEquals(5_000, r.getDeathFees());
		assertEquals(1_545_000, r.getProfit());
		assertTrue(r.getLast().isDied());
		assertEquals(Integer.valueOf(7), r.getLast().getWave());
	}

	@Test
	public void theGravesOwnFeeTakesTheEstimatesPlace()
	{
		ProfitRecord r = ProfitRecord.empty()
			.with(new RunProfit(0, 100_000, 11_250, true, 6))
			.with(new RunProfit(0, 150_000, 36_250, true, 7));
		ProfitRecord fixed = r.withLastDeathFee(34_000);
		assertEquals(11_250 + 34_000, fixed.getDeathFees());
		assertEquals(34_000, fixed.getLast().getDeathFee());
		assertEquals(Integer.valueOf(7), fixed.getLast().getWave());
		ProfitRecord alive = ProfitRecord.empty().with(new RunProfit(1_000_000, 100_000, 0, false, 12));
		assertSame(alive, alive.withLastDeathFee(5_000));
	}

	@Test
	public void aRunWithNothingToCountLeavesTheRecordAsItIs()
	{
		ProfitRecord r = ProfitRecord.empty();
		assertSame(r, r.with(new RunProfit(0, 0, 0, false, null)));
	}

	@Test
	public void theTextRoundTrips()
	{
		ProfitRecord r = ProfitRecord.empty()
			.with(new RunProfit(2_000_000, 300_000, 0, false, 12))
			.with(new RunProfit(0, 150_000, 5_000, true, null));
		assertEquals("v2;2,1,2000000,450000,5000;0,150000,5000,1,", r.toText());
		ProfitRecord back = ProfitRecord.of(r.toText());
		assertEquals(r.toText(), back.toText());
		assertNull(back.getLast().getWave());
		assertEquals("v2;0,0,0,0,0", ProfitRecord.empty().toText());
		assertTrue(ProfitRecord.of("v1;0,0,0,0").isEmpty());
		// A v1 text, from before the deaths' fees, reads with none.
		ProfitRecord v1 = ProfitRecord.of("v1;2,1,2000000,450000;0,150000,1,7");
		assertEquals(2, v1.getRuns());
		assertEquals(0, v1.getDeathFees());
		assertEquals(Integer.valueOf(7), v1.getLast().getWave());
	}

	@Test
	public void unreadableTextIsAnEmptyRecord()
	{
		for (String text : new String[] {null, "", "v2;1,0,5,5", "v1;1,0,5", "v1;1,2,5,5", "v1;x,0,5,5", "v1;1,0,5,5;1,1,2,", "v1;1,0,-5,5", "v2;1,0,5,5", "v2;1,0,5,5,-1"})
		{
			assertTrue(text, ProfitRecord.of(text).isEmpty());
		}
	}

	@Test
	public void coinsAreWrittenShort()
	{
		assertEquals("0", ProfitRecord.coins(0));
		assertEquals("9,999", ProfitRecord.coins(9_999));
		assertEquals("12.5K", ProfitRecord.coins(12_500));
		assertEquals("40K", ProfitRecord.coins(40_000));
		assertEquals("310K", ProfitRecord.coins(310_499));
		assertEquals("1.23M", ProfitRecord.coins(1_234_567));
		assertEquals("2M", ProfitRecord.coins(2_000_000));
		assertEquals("2.1B", ProfitRecord.coins(2_100_000_000L));
		assertEquals("+1.5M", ProfitRecord.signed(1_500_000));
		assertEquals("-310K", ProfitRecord.signed(-310_000));
		assertEquals("0", ProfitRecord.signed(0));
	}
}
