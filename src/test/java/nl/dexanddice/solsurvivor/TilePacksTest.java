package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.runelite.http.api.RuneLiteAPI;
import nl.dexanddice.solsurvivor.core.TileMarker;
import org.junit.Test;

/**
 * The tiles Tile Packs draws in the arena, read as its build of 31 October 2026 keeps them, with the client's Gson: its
 * built-in packs in a commented JSON file in its jar, the player's own under tilePacks.pack_<id>, and the ids drawn
 * under tilePacks.packs. The packs here are made up in that format.
 */
public class TilePacksTest
{
	private static final String BUILT_IN = "//this file is sorted by bosses\n"
		+ "//next id: 92\n"
		+ "{\n"
		+ "//  PVM\n"
		+ "  \"1\": {\"id\": 1, \"packName\": \"Somewhere else\", \"link\": \"https://example.org\",\n"
		+ "    \"packTiles\": \"[{\\\"regionId\\\":11850,\\\"regionX\\\":27,\\\"regionY\\\":44,\\\"z\\\":0,\\\"color\\\":\\\"#FFFFFF00\\\"}]\"},\n"
		+ "  \"55\": {\"id\": 55, \"packName\": \"Colosseum\",\n"
		+ "    \"packTiles\": \"[{\\\"regionId\\\":7216,\\\"regionX\\\":23,\\\"regionY\\\":43,\\\"z\\\":0,\\\"color\\\":\\\"#FF00FFFF\\\",\\\"label\\\":\\\"A\\\"},"
		+ "{\\\"regionId\\\":7216,\\\"regionX\\\":27,\\\"regionY\\\":40,\\\"z\\\":0,\\\"color\\\":\\\"#01000000\\\"},"
		+ "{\\\"regionId\\\":7216,\\\"regionX\\\":26,\\\"regionY\\\":50,\\\"z\\\":0,\\\"color\\\":\\\"#FFFFA500\\\"}]\"}\n"
		+ "}";

	private static final String CUSTOM = "{\"id\":10001,\"packName\":\"Mine\",\"packTiles\":\"[{\\\"regionId\\\":7216,\\\"regionX\\\":21,"
		+ "\\\"regionY\\\":42,\\\"z\\\":0,\\\"color\\\":\\\"#7AFF0000\\\",\\\"label\\\":\\\"skip\\\"}]\",\"visible\":true}";

	/** A built-in pack's id saved by the player, which carries whether it is listed and never replaces its tiles. */
	private static final String BUILT_IN_SAVED = "{\"id\":55,\"visible\":false}";

	private static List<TileMarker> read(String drawn, Color override, boolean labels)
	{
		Map<Integer, TilePacks.Pack> builtIn = TilePacks.builtIn(RuneLiteAPI.GSON, BUILT_IN);
		return TilePacks.read(RuneLiteAPI.GSON, builtIn, Arrays.asList(CUSTOM, BUILT_IN_SAVED, "{broken"), drawn, override, labels, 0);
	}

	@Test
	public void theArenasTilesOfThePacksDrawnAreMarkersInTheirOrder()
	{
		assertEquals("[(5,9) #ff0000 skip, (7,8) #00ffff A, (10,1) #ffa500]", read("[10001,1,55]", null, true).toString());
	}

	@Test
	public void aPackNotDrawnOrUnknownAddsNone()
	{
		assertEquals("[(5,9) #ff0000 skip]", read("[10001,77]", null, true).toString());
		assertTrue(read("[]", null, true).isEmpty());
		assertTrue(read(null, null, true).isEmpty());
		assertTrue(read("{not a list", null, true).isEmpty());
	}

	@Test
	public void theOverrideColourAndHiddenLabelsAreAsTilePacksDrawsThem()
	{
		// The override draws even a tile the pack made too faint to see, so the game shows it, and so does the link.
		assertEquals("[(7,8) #ffff00, (11,11) #ffff00, (10,1) #ffff00]", read("[55]", Color.YELLOW, false).toString());
	}

	@Test
	public void aMissingOrBrokenBuiltInFileIsNoBuiltInPacks()
	{
		assertTrue(TilePacks.builtIn(RuneLiteAPI.GSON, null).isEmpty());
		assertTrue(TilePacks.builtIn(RuneLiteAPI.GSON, "{\"55\": [").isEmpty());
		assertEquals("[(5,9) #ff0000 skip]", TilePacks.read(RuneLiteAPI.GSON, Collections.emptyMap(), Collections.singletonList(CUSTOM),
			"[55,10001]", null, true, 0).toString());
	}

	@Test
	public void theKeysAreTilePacksOwn()
	{
		assertEquals("com.tilepacks.TilePacksPlugin", TilePacks.PLUGIN_CLASS);
		assertEquals("tilePacks.jsonc", TilePacks.BUILT_IN);
		assertEquals("tilePacks", TilePacks.GROUP);
		assertEquals("packs", TilePacks.DRAWN);
		assertEquals("pack_", TilePacks.PACK_PREFIX);
		assertEquals("tilepacks", TilePacks.SETTINGS);
	}
}
