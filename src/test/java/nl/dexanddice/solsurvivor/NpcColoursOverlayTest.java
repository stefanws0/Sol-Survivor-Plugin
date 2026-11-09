package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveReview;
import org.junit.Test;

/** Frem and orb colours: each Fremennik in its style's colour, the orb in its own, and no other NPC coloured. */
public class NpcColoursOverlayTest
{
	private final SolSurvivorConfig defaults = new SolSurvivorConfig()
	{
	};

	@Test
	public void eachFremennikTakesItsStylesColourAndTheOrbItsOwn()
	{
		assertEquals(defaults.fremArcherColour(), NpcColoursOverlay.colourFor(WaveReview.FREM_ARCHER_ID, defaults));
		assertEquals(defaults.fremSeerColour(), NpcColoursOverlay.colourFor(WaveReview.FREM_SEER_ID, defaults));
		assertEquals(defaults.fremBerserkerColour(), NpcColoursOverlay.colourFor(WaveReview.FREM_BERSERKER_ID, defaults));
		assertEquals(defaults.orbColour(), NpcColoursOverlay.colourFor(NpcKind.SOLARFLARE_ORB_ID, defaults));
	}

	@Test
	public void theWavesOwnNpcsAreLeftAsTheyAre()
	{
		for (int id : new int[] {NpcKind.SHAMAN_ID, NpcKind.JAGUAR_ID, NpcKind.MINOTAUR_ID, NpcKind.MINOTAUR_RED_FLAG_ID,
			NpcKind.JAVELIN_ID, NpcKind.MANTICORE_ID, NpcKind.SHOCKWAVE_ID})
		{
			assertNull(NpcColoursOverlay.colourFor(id, defaults));
		}
	}

	@Test
	public void theFourDefaultColoursAreOpaqueAndTellApart()
	{
		Set<Integer> seen = new HashSet<>();
		for (Color c : new Color[] {defaults.fremArcherColour(), defaults.fremSeerColour(), defaults.fremBerserkerColour(),
			defaults.orbColour()})
		{
			assertEquals(255, c.getAlpha());
			assertTrue("Two default colours alike", seen.add(c.getRGB()));
		}
	}
}
