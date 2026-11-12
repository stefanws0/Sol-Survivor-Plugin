package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

/** The ticks of a wave that came late, and the line that names them. */
public class LateTicksTest
{
	@Test
	public void aTickASecondOrMoreAfterTheLastIsLateAndAnOrdinaryOneIsNot()
	{
		LateTicks ticks = new LateTicks();
		ticks.record(0, 10_000);
		ticks.record(1, 10_600);
		ticks.record(2, 11_290);
		ticks.record(3, 12_690);
		ticks.record(4, 13_290);
		assertEquals(1, ticks.between(0, 4).size());
		assertEquals(3, ticks.between(0, 4).get(0).tick);
		assertEquals("Tick 3 took 1.4 s to come, not 0.6 s.", LateTicks.text(ticks.between(0, 4)));
		assertNull(LateTicks.text(ticks.between(4, 9)));
	}

	@Test
	public void severalAreNamedTogetherWithTheSlowest()
	{
		LateTicks ticks = new LateTicks();
		long at = 0;
		ticks.record(0, at);
		for (int t = 1; t <= 6; t++)
		{
			at += t == 2 ? 2_100 : 1_000;
			ticks.record(t, at);
		}
		assertEquals("Ticks 1 and 2 came late, the slowest in 2.1 s, not 0.6 s.", LateTicks.text(ticks.between(1, 2)));
		assertEquals("Ticks 1, 2, 3, 4 and 2 more came late, the slowest in 2.1 s, not 0.6 s.", LateTicks.text(ticks.between(0, 9)));
	}

	@Test
	public void theFirstTickHasNoneBeforeItToBeLateAfter()
	{
		LateTicks ticks = new LateTicks();
		ticks.record(0, 99_999);
		assertNull(LateTicks.text(ticks.between(0, 0)));
	}
}
