package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.util.List;
import net.runelite.http.api.RuneLiteAPI;
import nl.dexanddice.solsurvivor.core.TileMarker;
import org.junit.Test;

/**
 * Ground Markers' saved points read as the arena's markers, with the client's Gson as the plugin is given it: the text is
 * as RuneLite 1.13.4 saves it under groundMarker.region_7216, its colours "#AARRGGBB", and an older save's colour as
 * an object, which the client's Gson reads too.
 */
public class GroundMarkersTest
{
	@Test
	public void theArenasPointsOnThePlayersPlaneAreItsMarkers()
	{
		String saved = "[{\"regionId\":7216,\"regionX\":23,\"regionY\":43,\"z\":0,\"color\":\"#FFFF0000\",\"label\":\"A\"},"
			+ "{\"regionId\":7216,\"regionX\":23,\"regionY\":36,\"z\":0},"
			+ "{\"regionId\":7216,\"regionX\":21,\"regionY\":26,\"z\":0,\"color\":{\"value\":-16711936,\"falpha\":0.0}},"
			+ "{\"regionId\":7216,\"regionX\":20,\"regionY\":20,\"z\":1,\"color\":\"#FF0000FF\"},"
			+ "{\"regionId\":7316,\"regionX\":20,\"regionY\":20,\"z\":0,\"color\":\"#FF0000FF\"}]";
		List<TileMarker> markers = GroundMarkers.read(RuneLiteAPI.GSON, saved, 0);
		assertEquals("[(7,8) #ff0000 A, (7,15) #ffff00, (5,25) #00ff00]", markers.toString());
	}

	@Test
	public void noSaveOrABrokenOneIsNoMarkers()
	{
		assertTrue(GroundMarkers.read(RuneLiteAPI.GSON, null, 0).isEmpty());
		assertTrue(GroundMarkers.read(RuneLiteAPI.GSON, "", 0).isEmpty());
		assertTrue(GroundMarkers.read(RuneLiteAPI.GSON, "{not a list", 0).isEmpty());
		assertTrue(GroundMarkers.read(RuneLiteAPI.GSON, "{\"regionId\":7216}", 0).isEmpty());
	}

	@Test
	public void theKeyIsTheArenasRegion()
	{
		assertEquals("groundMarker", GroundMarkers.GROUP);
		assertEquals("region_7216", GroundMarkers.KEY);
	}
}
