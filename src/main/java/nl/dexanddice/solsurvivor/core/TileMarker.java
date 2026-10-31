package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One of the player's own tile markers in the arena, from RuneLite's Ground Markers, as the state links give it to the
 * site (`markers`, src/link/statePayload.ts's TileMarkerV1): its grid tile, its colour as "#rrggbb" and its label, or
 * null for none. The plugin only sends the tiles the player marked by hand; it draws nothing new in the game.
 */
public final class TileMarker
{
	/** The most markers a link holds, the site's MAX_TILE_MARKERS. */
	public static final int MAX_MARKERS = 100;

	/** The longest label a link holds, the site's MAX_TILE_MARKER_LABEL; a longer one is cut to it. */
	public static final int MAX_LABEL = 40;

	/** Ground Markers' own colour for a tile marked without one: its "Color of the tile" setting's default, yellow. */
	static final int DEFAULT_RGB = 0xFFFF00;

	/**
	 * The faintest colour a marker is sent in, by its alpha: one fainter cannot be seen in the game, such as the pillars'
	 * edges Tile Packs' Colosseum pack marks at an alpha of 1, and would show on the site's map as a tile the player never
	 * saw.
	 */
	static final int MIN_ALPHA = 0x10;

	private final GridTile tile;
	private final String colour;
	private final String label;

	private TileMarker(GridTile tile, String colour, String label)
	{
		this.tile = tile;
		this.colour = colour;
		this.label = label;
	}

	/**
	 * The marker of a point Ground Markers saved: in `regionId`, at `regionX` and `regionY` within it on plane `z`, in the
	 * colour `argb` (null for Ground Markers' default) with `label` (null or blank for none). Null for a point outside the
	 * arena's region, off its grid, on another plane than `plane`, the player's, or too faint to see (MIN_ALPHA).
	 */
	public static TileMarker of(int regionId, int regionX, int regionY, int z, int plane, Integer argb, String label)
	{
		if (regionId != ArenaGeometry.REGION_ID || z != plane || (argb != null && argb >>> 24 < MIN_ALPHA))
		{
			return null;
		}
		GridTile tile = ArenaGeometry.fromRegion(regionX, regionY);
		if (!ArenaGeometry.onGrid(tile))
		{
			return null;
		}
		String colour = String.format("#%06x", (argb == null ? DEFAULT_RGB : argb) & 0xFFFFFF);
		return new TileMarker(tile, colour, labelOf(label));
	}

	/** A label as a link holds it: trimmed, cut to MAX_LABEL without splitting a character in two, and null when blank. */
	private static String labelOf(String label)
	{
		if (label == null || label.trim().isEmpty())
		{
			return null;
		}
		String s = label.trim();
		if (s.length() <= MAX_LABEL)
		{
			return s;
		}
		int end = Character.isHighSurrogate(s.charAt(MAX_LABEL - 1)) ? MAX_LABEL - 1 : MAX_LABEL;
		return s.substring(0, end);
	}

	/**
	 * `markers` with one marker on a tile, the first given for it, in their order, and at most MAX_MARKERS: the player's
	 * own Ground Markers come before a tile pack's, so theirs win a tile both mark.
	 */
	public static List<TileMarker> distinct(List<TileMarker> markers)
	{
		Map<GridTile, TileMarker> byTile = new LinkedHashMap<>();
		for (TileMarker m : markers)
		{
			if (byTile.size() < MAX_MARKERS)
			{
				byTile.putIfAbsent(m.getTile(), m);
			}
		}
		return new ArrayList<>(byTile.values());
	}

	public GridTile getTile()
	{
		return tile;
	}

	/** The colour as "#rrggbb", lower case. */
	public String getColour()
	{
		return colour;
	}

	/** The label, or null for none. */
	public String getLabel()
	{
		return label;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof TileMarker))
		{
			return false;
		}
		TileMarker m = (TileMarker) o;
		return tile.equals(m.tile) && colour.equals(m.colour) && Objects.equals(label, m.label);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(tile, colour, label);
	}

	@Override
	public String toString()
	{
		return tile + " " + colour + (label == null ? "" : " " + label);
	}
}
