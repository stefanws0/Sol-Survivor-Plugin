package nl.dexanddice.solsurvivor;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nl.dexanddice.solsurvivor.core.TileMarker;

/**
 * The arena's tiles the Tile Packs plugin draws, for Arena now's link with "Send my tile markers" on, beside Ground
 * Markers' (GroundMarkers). Tile Packs (checked in its build of 31 October 2026) keeps:
 * <ul>
 * <li>its built-in packs in its own jar, as `tilePacks.jsonc`: a map from each pack's id to {id, packName, packTiles},
 * read here through the installed plugin's class, as Tile Packs itself reads it;</li>
 * <li>the player's own packs in its config group `tilePacks`, each under `pack_<id>` as {id, packName, packTiles,
 * visible}, where a built-in pack's id carries only whether it is listed, never its tiles;</li>
 * <li>the packs it draws under `packs`, a JSON list of their ids;</li>
 * <li>its settings in the group `tilepacks`: `overrideColorActive` and `overrideColor` draw every tile in one colour
 * (off, and yellow, by default), and `showLabels` its labels (on by default).</li>
 * </ul>
 * A pack's `packTiles` is Ground Markers' own format, as text.
 */
final class TilePacks
{
	/** The class of the installed Tile Packs plugin, which holds BUILT_IN beside it. */
	static final String PLUGIN_CLASS = "com.tilepacks.TilePacksPlugin";

	/** The built-in packs, a resource beside PLUGIN_CLASS. */
	static final String BUILT_IN = "tilePacks.jsonc";

	/** The config group of the packs and the list of those drawn. */
	static final String GROUP = "tilePacks";

	/** The key of the ids of the packs drawn. */
	static final String DRAWN = "packs";

	/** The prefix of each saved pack's key. */
	static final String PACK_PREFIX = "pack_";

	/** The config group of Tile Packs' settings. */
	static final String SETTINGS = "tilepacks";

	/** Tile Packs' colour for every tile while its override is on, unless the player picked another. */
	static final Color DEFAULT_OVERRIDE = Color.YELLOW;

	/** A pack as Tile Packs keeps one: its id and its tiles, as Ground Markers' text. */
	static final class Pack
	{
		Integer id;
		String packTiles;
	}

	private TilePacks()
	{
	}

	/**
	 * The built-in packs of Tile Packs' `json`, by id; none for no text or text that is not its map. `gson` is the
	 * client's, which reads the file's comments as Tile Packs' own reading does.
	 */
	static Map<Integer, Pack> builtIn(Gson gson, String json)
	{
		Map<Integer, Pack> packs = new HashMap<>();
		if (json == null || json.isEmpty())
		{
			return packs;
		}
		try
		{
			Map<String, Pack> read = gson.fromJson(json, new TypeToken<Map<String, Pack>>()
			{
			}.getType());
			for (Pack p : read == null ? Collections.<Pack>emptyList() : read.values())
			{
				if (p != null && p.id != null)
				{
					packs.put(p.id, p);
				}
			}
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return new HashMap<>();
		}
		return packs;
	}

	/**
	 * The markers of the packs Tile Packs draws on the player's `plane`, in the order of `drawn`, its list of their ids:
	 * each from `builtIn`, or from `saved`, the texts of the player's own packs, for an id not built in. A pack missing
	 * from both, or text that does not read, adds none. Every tile is in `override` when that is not null, and without
	 * its label unless `labels`, as Tile Packs draws it.
	 */
	static List<TileMarker> read(Gson gson, Map<Integer, Pack> builtIn, Collection<String> saved, String drawn, Color override,
		boolean labels, int plane)
	{
		Map<Integer, Pack> packs = new HashMap<>(builtIn);
		for (String text : saved)
		{
			Pack p = pack(gson, text);
			if (p != null && p.id != null)
			{
				packs.putIfAbsent(p.id, p);
			}
		}
		List<TileMarker> markers = new ArrayList<>();
		for (Integer id : ids(gson, drawn))
		{
			Pack p = id == null ? null : packs.get(id);
			if (p != null)
			{
				markers.addAll(GroundMarkers.read(gson, p.packTiles, plane, override, labels));
			}
		}
		return markers;
	}

	private static Pack pack(Gson gson, String text)
	{
		try
		{
			return text == null ? null : gson.fromJson(text, Pack.class);
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return null;
		}
	}

	private static List<Integer> ids(Gson gson, String drawn)
	{
		try
		{
			List<Integer> ids = drawn == null ? null : gson.fromJson(drawn, new TypeToken<List<Integer>>()
			{
			}.getType());
			return ids == null ? Collections.emptyList() : ids;
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return Collections.emptyList();
		}
	}
}
