package nl.dexanddice.solsurvivor;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import nl.dexanddice.solsurvivor.core.ArenaGeometry;
import nl.dexanddice.solsurvivor.core.TileMarker;

/**
 * The player's own tile markers in the arena, read from what RuneLite's Ground Markers saves, for Arena now's link with
 * "Send my tile markers" on (StatePayload.withMarkers). Ground Markers keeps a region's points in its config group
 * `groundMarker` under `region_<id>`, as a JSON list of {regionId, regionX, regionY, z, color, label}, the colour written
 * "#AARRGGBB" by the client's Gson (checked in RuneLite 1.13.4). The arena's points are under its template's region,
 * where the instance's tiles are marked.
 */
final class GroundMarkers
{
	/** Ground Markers' config group. */
	static final String GROUP = "groundMarker";

	/** The key of the arena's points. */
	static final String KEY = "region_" + ArenaGeometry.REGION_ID;

	/** A point as Ground Markers saves it: the fields its GroundMarkerPoint has. */
	private static final class Point
	{
		int regionId;
		int regionX;
		int regionY;
		int z;
		Color color;
		String label;
	}

	private GroundMarkers()
	{
	}

	/**
	 * The markers of Ground Markers' saved `json` for the arena, on the player's `plane`, in the order it saved them: none
	 * for no text, or text that is not its list. `gson` is the client's, which reads its colours.
	 */
	static List<TileMarker> read(Gson gson, String json, int plane)
	{
		return read(gson, json, plane, null, true);
	}

	/**
	 * The same, for points drawn by another plugin in the same format, Tile Packs: each in `override` rather than its own
	 * colour when that is not null, and without its label unless `labels`.
	 */
	static List<TileMarker> read(Gson gson, String json, int plane, Color override, boolean labels)
	{
		if (json == null || json.isEmpty())
		{
			return Collections.emptyList();
		}
		List<Point> points;
		try
		{
			points = gson.fromJson(json, new TypeToken<List<Point>>()
			{
			}.getType());
		}
		catch (JsonParseException | IllegalStateException e)
		{
			return Collections.emptyList();
		}
		List<TileMarker> markers = new ArrayList<>();
		for (Point p : points == null ? Collections.<Point>emptyList() : points)
		{
			Color colour = override != null ? override : p == null ? null : p.color;
			TileMarker m = p == null ? null : TileMarker.of(p.regionId, p.regionX, p.regionY, p.z, plane, colour == null ? null : colour.getRGB(), labels ? p.label : null);
			if (m != null && markers.size() < TileMarker.MAX_MARKERS)
			{
				markers.add(m);
			}
		}
		return markers;
	}
}
