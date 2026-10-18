package nl.dexanddice.solsurvivor.core;

import static nl.dexanddice.solsurvivor.core.OrbStyle.MAGIC;
import static nl.dexanddice.solsurvivor.core.OrbStyle.MELEE;
import static nl.dexanddice.solsurvivor.core.OrbStyle.RANGED;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class OrbOrderTest
{
	private static OrbOrder seen(OrbStyle... styles)
	{
		OrbOrder order = new OrbOrder();
		for (OrbStyle s : styles)
		{
			order.see(s);
		}
		return order;
	}

	@Test
	public void noOrbIsUnchargedWithItsOrderUnknown()
	{
		assertEquals("u", seen().losCode(false));
		assertEquals("u", seen().losCode(true));
	}

	@Test
	public void theStandardOrdersAreNamedByTheirFirstOrb()
	{
		assertEquals("r", seen(RANGED, MAGIC, MELEE).losCode(false));
		assertEquals("m", seen(MAGIC, RANGED, MELEE).losCode(false));
	}

	@Test
	public void mantimayhemIIIsOrdersAreNamedInFull()
	{
		assertEquals("Mrm", seen(MELEE, RANGED, MAGIC).losCode(true));
		assertEquals("Mmr", seen(MELEE, MAGIC, RANGED).losCode(true));
		assertEquals("rMm", seen(RANGED, MELEE, MAGIC).losCode(true));
		assertEquals("mMr", seen(MAGIC, MELEE, RANGED).losCode(true));
		assertEquals("r", seen(RANGED, MAGIC, MELEE).losCode(true));
	}

	@Test
	public void withoutMantimayhemIIITheFirstOrbSettlesTheOrder()
	{
		assertEquals("ur", seen(RANGED).losCode(false));
		assertEquals("um", seen(MAGIC, RANGED).losCode(false));
	}

	@Test
	public void withMantimayhemIIITwoOrbsSettleTheOrder()
	{
		assertEquals("u", seen(MELEE).losCode(true));
		assertEquals("uMrm", seen(MELEE, RANGED).losCode(true));
		assertEquals("urMm", seen(RANGED, MELEE).losCode(true));
		assertEquals("ur", seen(RANGED, MAGIC).losCode(true));
	}

	@Test
	public void anOrbSeenAgainIsPassedOver()
	{
		// A graphic stays on for several ticks, and the order repeats once all three have fired.
		assertEquals("m", seen(MAGIC, MAGIC, RANGED, RANGED, MELEE, MAGIC, RANGED).losCode(false));
		assertEquals(3, seen(MAGIC, MAGIC, RANGED, MELEE, MELEE).getCount());
	}
}
