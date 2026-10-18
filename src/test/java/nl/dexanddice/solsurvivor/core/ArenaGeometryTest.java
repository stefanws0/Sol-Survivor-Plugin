package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ArenaGeometryTest
{
	@Test
	public void theWikisStartTileIsTheLosToolsDefault()
	{
		// Combat research §9: the wiki's "start NW" marker, region (23, 36), is the LoS tool's default tile (7, 15).
		assertEquals(new GridTile(7, 15), ArenaGeometry.fromRegion(23, 36));
	}

	@Test
	public void theGridRunsEastAndSouth()
	{
		assertEquals(new GridTile(0, 0), ArenaGeometry.fromRegion(16, 51));
		assertEquals(new GridTile(33, 33), ArenaGeometry.fromRegion(49, 18));
	}

	@Test
	public void tilesOffTheGridAreNotInTheArena()
	{
		assertTrue(ArenaGeometry.onGrid(new GridTile(0, 33)));
		assertFalse(ArenaGeometry.onGrid(ArenaGeometry.fromRegion(15, 40)));
		assertFalse(ArenaGeometry.onGrid(ArenaGeometry.fromRegion(30, 52)));
		assertFalse(ArenaGeometry.onGrid(new GridTile(34, 0)));
	}
}
