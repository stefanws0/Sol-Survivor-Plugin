package nl.dexanddice.solsurvivor.core;

/**
 * The Colosseum's arena and the transform from the game's region coordinates to the LoS tool's grid (combat research §9):
 * x = regionX - 16 and y = 51 - regionY, in region 7216. The game gives an instance's tiles in their template's region,
 * which the RuneLite layer reads with WorldPoint.fromLocalInstance.
 */
public final class ArenaGeometry
{
	/** The arena's region, the template the instance is copied from. */
	public static final int REGION_ID = 7216;

	/** The lobby's region, where the run is entered from. */
	public static final int LOBBY_REGION_ID = 7316;

	/** The grid's width and height in tiles: 0 to 33 each way, the state link's MAP_SIZE. */
	public static final int SIZE = 34;

	private ArenaGeometry()
	{
	}

	/** The grid tile of a tile in the region, by its coordinates within it (0 to 63 each). */
	public static GridTile fromRegion(int regionX, int regionY)
	{
		return new GridTile(regionX - 16, 51 - regionY);
	}

	/** Whether a grid tile is on the grid the state link takes. */
	public static boolean onGrid(GridTile tile)
	{
		return tile.getX() >= 0 && tile.getX() < SIZE && tile.getY() >= 0 && tile.getY() < SIZE;
	}
}
