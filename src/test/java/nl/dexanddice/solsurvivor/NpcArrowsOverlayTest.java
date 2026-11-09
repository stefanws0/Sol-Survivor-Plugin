package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveReview;
import org.junit.Test;

/** Arrows to NPCs out of view (idea 359): where on the view's edge each arrow sits, its name and its colour. */
public class NpcArrowsOverlayTest
{
	private final SolSurvivorConfig defaults = new SolSurvivorConfig()
	{
	};
	private final Rectangle view = new Rectangle(4, 4, 512, 334);

	@Test
	public void theTipSitsOnTheEdgeTheDirectionLeavesBy()
	{
		Point2D.Double centre = new Point2D.Double(260, 171);
		Point2D.Double right = NpcArrowsOverlay.edge(view, centre, 1, 0, 6);
		assertEquals(4 + 512 - 6, right.x, 1e-9);
		assertEquals(171, right.y, 1e-9);
		Point2D.Double up = NpcArrowsOverlay.edge(view, centre, 0, -3, 6);
		assertEquals(4 + 6, up.y, 1e-9);
		// Down and to the left at 45 degrees from the centre meets the bottom first, the view being wider than high.
		Point2D.Double corner = NpcArrowsOverlay.edge(view, centre, -1, 1, 6);
		assertEquals(4 + 334 - 6, corner.y, 1e-9);
		assertEquals(260 - (338 - 6 - 171), corner.x, 1e-9);
		assertNull(NpcArrowsOverlay.edge(view, centre, 0, 0, 6));
	}

	@Test
	public void aStartOutsideTheViewStartsFromItsCentre()
	{
		Point2D.Double tip = NpcArrowsOverlay.edge(view, new Point2D.Double(900, 900), 1, 0, 6);
		assertEquals(view.getCenterY(), tip.y, 1e-9);
	}

	@Test
	public void eachArrowIsInItsStyleColourAndTheManticoreStaysNeutral()
	{
		assertEquals(defaults.waveBoxMagicColour(), NpcArrowsOverlay.colour(NpcArrowsOverlay.icon(NpcKind.SHAMAN_ID), defaults));
		assertEquals(defaults.waveBoxRangedColour(), NpcArrowsOverlay.colour(NpcArrowsOverlay.icon(WaveReview.FREM_ARCHER_ID), defaults));
		assertEquals(defaults.waveBoxMeleeColour(), NpcArrowsOverlay.colour(NpcArrowsOverlay.icon(NpcKind.MINOTAUR_RED_FLAG_ID), defaults));
		assertEquals(new Color(0xd0d0d0), NpcArrowsOverlay.colour(NpcArrowsOverlay.icon(NpcKind.MANTICORE_ID), defaults));
		assertNull(NpcArrowsOverlay.icon(NpcKind.SOLARFLARE_ORB_ID));
		assertEquals("Shockwave", NpcArrowsOverlay.name(NpcArrowsOverlay.icon(NpcKind.SHOCKWAVE_ID)));
		assertEquals("Berserker", NpcArrowsOverlay.name(NpcArrowsOverlay.icon(WaveReview.FREM_BERSERKER_ID)));
		assertEquals(SolSurvivorConfig.NpcArrows.OFF, defaults.npcArrows());
	}

	@Test
	public void theWaveBoxStepsAsideOnSolsWaveOnlyWithQuietOn()
	{
		assertEquals(true, defaults.quietOnSol());
		assertEquals(true, WaveBoxOverlay.quiet(12, true));
		assertEquals(false, WaveBoxOverlay.quiet(12, false));
		assertEquals(false, WaveBoxOverlay.quiet(11, true));
	}
}
