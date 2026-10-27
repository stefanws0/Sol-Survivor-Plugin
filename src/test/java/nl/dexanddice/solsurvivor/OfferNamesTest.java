package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;

import nl.dexanddice.solsurvivor.core.Modifier;
import org.junit.Test;

/** The names written on Minimus's offers: each with the tier taking it brings, in that tier's colour. */
public class OfferNamesTest
{
	@Test
	public void anOfferIsNamedWithTheTierTakingItBrings()
	{
		assertEquals("Frailty <col=8cd2ff>I</col>", OfferNames.label(Modifier.FRAILTY, 0));
		assertEquals("Frailty <col=c070ff>III</col>", OfferNames.label(Modifier.FRAILTY, 2));
		// Its third tier is its last: one held at III that is offered again stays at III.
		assertEquals("Frailty <col=c070ff>III</col>", OfferNames.label(Modifier.FRAILTY, 3));
		assertEquals("Dynamic Duo", OfferNames.label(Modifier.DYNAMIC_DUO, 0));
		assertEquals("Bees! <col=4a8cff>II</col>", OfferNames.label(Modifier.BEES, 1));
		assertEquals("Red Flag", OfferNames.label(Modifier.RED_FLAG, 0));
	}
}
