package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.junit.Test;

/**
 * The packed link text (StatePayload.pack): it unpacks, against the site's dictionary, to the JSON it was made from, and
 * it is about half as long. The site's tests unpack the plugin's packed fixture and compare the plugin's copy of the
 * dictionary with the site's, byte for byte (test/link/pluginFixtures.test.ts).
 */
public class StatePayloadTest
{
	/** The state fixtures, each a payload as toJson writes it. */
	private static final String[] FIXTURES = {"wave4-start.json", "wave4-mid.json", "late-start.json", "wave8-gear.json"};

	static String fixture(String name) throws IOException
	{
		try (InputStream in = StatePayloadTest.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
			{
				throw new IOException("no fixture " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	/** Packed link text unpacked to its JSON, as the site's unpackStateText does it; an AssertionError if it does not unpack. */
	static String unpack(String text)
	{
		assertTrue(text.startsWith(StatePayload.PACKED_V1));
		Inflater inflater = new Inflater(true);
		try
		{
			inflater.setDictionary(StatePayload.DICTIONARY_V1);
			inflater.setInput(Base64.getUrlDecoder().decode(text.substring(StatePayload.PACKED_V1.length())));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			while (!inflater.finished())
			{
				int n = inflater.inflate(buffer);
				if (n == 0 && inflater.needsInput())
				{
					throw new AssertionError("the packed text is cut short");
				}
				out.write(buffer, 0, n);
			}
			return out.toString(StandardCharsets.US_ASCII);
		}
		catch (DataFormatException e)
		{
			throw new AssertionError("the packed text does not unpack", e);
		}
		finally
		{
			inflater.end();
		}
	}

	@Test
	public void theSitesDictionaryIsThere()
	{
		assertNotNull(StatePayload.DICTIONARY_V1);
		assertEquals(1496, StatePayload.DICTIONARY_V1.length);
	}

	@Test
	public void aPackedPayloadUnpacksToItsJsonAtLessThanThreeFifthsOfTheLength() throws Exception
	{
		for (String name : FIXTURES)
		{
			String json = fixture(name);
			String packed = StatePayload.pack(json);
			String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.US_ASCII));
			assertEquals(name, json, unpack(packed));
			assertTrue(name + ": " + packed.length() + " against " + plain.length(), packed.length() * 5 < plain.length() * 3);
		}
	}

	@Test
	public void theTrianglesPlaceComesAfterTheRestOfTheLink() throws Exception
	{
		// wave4-triangle.json is wave4-start.json with the dot of the site's readout "Safe 10% · Fast 0% · Easy 90%", which
		// the site's tests open (test/link/pluginFixtures.test.ts).
		String start = fixture("wave4-start.json");
		TriangleDot dot = TriangleDot.of(10, 0, 90);
		assertEquals(fixture("wave4-triangle.json"), StatePayload.withTriangle(start, dot));
		String link = StatePayload.linkOfJson("https://solsurvivor.app/", start, dot);
		assertEquals(fixture("wave4-triangle.json"), unpack(link.substring(link.indexOf(StatePayload.STATE_HASH) + StatePayload.STATE_HASH.length())));
		// Without one, the link is as it was.
		assertEquals(start, StatePayload.withTriangle(start, null));
		assertEquals(StatePayload.linkOfJson("https://solsurvivor.app/", start), StatePayload.linkOfJson("https://solsurvivor.app/", start, null));
	}

	@Test
	public void thePlayersTileMarkersComeAfterTheRestOfTheLink() throws Exception
	{
		// wave4-markers.json is wave4-start.json with the markers of points as Ground Markers saves them, which the site's
		// tests open (test/link/pluginFixtures.test.ts): a red "A" west of the north-west pillar, the start tile marked in
		// Ground Markers' own yellow, and a green label longer than a link holds, cut to its 40 characters.
		String start = fixture("wave4-start.json");
		List<TileMarker> markers = new ArrayList<>();
		markers.add(TileMarker.of(7216, 23, 43, 0, 0, 0xFFFF0000, "A"));
		markers.add(TileMarker.of(7216, 23, 36, 0, 0, null, " "));
		markers.add(TileMarker.of(7216, 21, 26, 0, 0, 0xFF00FF00, "stall at the SW pillar for the reinforcement shaman"));
		assertEquals(fixture("wave4-markers.json"), StatePayload.withMarkers(start, markers));
		String link = StatePayload.linkOfJson("https://solsurvivor.app/", StatePayload.withMarkers(start, markers));
		assertEquals(fixture("wave4-markers.json"), unpack(link.substring(link.indexOf(StatePayload.STATE_HASH) + StatePayload.STATE_HASH.length())));
		// Without any, the link is as it was.
		assertEquals(start, StatePayload.withMarkers(start, null));
		assertEquals(start, StatePayload.withMarkers(start, new ArrayList<>()));
	}

	@Test
	public void aMarkerOffTheArenaOrOnAnotherPlaneIsLeftOut()
	{
		// Another region, the lobby's; another plane than the player's; and a tile of the arena's region off its grid.
		assertNull(TileMarker.of(ArenaGeometry.LOBBY_REGION_ID, 23, 43, 0, 0, null, null));
		assertNull(TileMarker.of(7216, 23, 43, 1, 0, null, null));
		assertNull(TileMarker.of(7216, 60, 43, 0, 0, null, null));
		assertNotNull(TileMarker.of(7216, 23, 43, 1, 1, null, null));
	}

	@Test
	public void aMarkerTooFaintToSeeInTheGameIsLeftOut()
	{
		assertNull(TileMarker.of(7216, 23, 43, 0, 0, 0x01000000, null));
		assertNull(TileMarker.of(7216, 23, 43, 0, 0, 0x0FFFFFFF, null));
		assertNotNull(TileMarker.of(7216, 23, 43, 0, 0, 0x10FFFFFF, null));
	}

	@Test
	public void aTileMarkedTwiceKeepsItsFirstMarker()
	{
		List<TileMarker> markers = new ArrayList<>();
		markers.add(TileMarker.of(7216, 23, 43, 0, 0, 0xFFFF0000, "mine"));
		markers.add(TileMarker.of(7216, 23, 36, 0, 0, null, null));
		markers.add(TileMarker.of(7216, 23, 43, 0, 0, 0xFF00FFFF, "A"));
		assertEquals("[(7,8) #ff0000 mine, (7,15) #ffff00]", TileMarker.distinct(markers).toString());
	}

	@Test
	public void aLinkHoldsAtMostAHundredMarkers()
	{
		String start = "{\"v\":1}";
		List<TileMarker> markers = new ArrayList<>();
		for (int i = 0; i < 150; i++)
		{
			markers.add(TileMarker.of(7216, 16 + i % 34, 51 - i / 34, 0, 0, null, null));
		}
		String json = StatePayload.withMarkers(start, markers);
		assertEquals(TileMarker.MAX_MARKERS, json.split("#ffff00").length - 1);
	}

	@Test
	public void theFirstLinksMarkComesAfterTheRestOfTheLink() throws Exception
	{
		// wave4-first.json is wave4-start.json with the mark of the plugin's first link, which the site's tests open
		// (test/link/pluginFixtures.test.ts) and count as a plugin player's first page.
		String start = fixture("wave4-start.json");
		assertEquals(fixture("wave4-first.json"), StatePayload.withFirst(start));
		String link = StatePayload.linkOfJson("https://solsurvivor.app/", StatePayload.withFirst(start));
		assertEquals(fixture("wave4-first.json"), unpack(link.substring(link.indexOf(StatePayload.STATE_HASH) + StatePayload.STATE_HASH.length())));
	}

	@Test
	public void theTrianglesSharesAreKeptToTheSettingsRangeAndNoneAboveNothingPlacesNoDot()
	{
		assertEquals(TriangleDot.of(0, 100, 0), TriangleDot.of(-5, 200, 0));
		assertEquals(TriangleDot.MAX_SHARE, TriangleDot.of(33, 101, 33).getFast());
		assertNull(TriangleDot.of(0, 0, 0));
		assertNull(TriangleDot.of(-1, 0, -40));
	}

	@Test
	public void thePackedFixtureIsAWaveStartsLink() throws Exception
	{
		// wave4-start.packed.txt is this plugin's packing of wave4-start.json, which the site's tests unpack too. It is not
		// compared with pack's text today: another zlib may deflate the same bytes otherwise, and both read the same.
		assertEquals(fixture("wave4-start.json"), unpack(fixture("wave4-start.packed.txt")));
	}
}
