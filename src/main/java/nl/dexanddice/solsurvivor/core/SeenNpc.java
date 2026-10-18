package nl.dexanddice.solsurvivor.core;

/** An NPC in the arena as the game shows it on a tick: its index in the scene, its id and its south-west tile. */
public final class SeenNpc
{
	private final int index;
	private final int npcId;
	private final GridTile tile;

	public SeenNpc(int index, int npcId, GridTile tile)
	{
		this.index = index;
		this.npcId = npcId;
		this.tile = tile;
	}

	public int getIndex()
	{
		return index;
	}

	public int getNpcId()
	{
		return npcId;
	}

	/** Its south-west tile on the grid, the tile the state link gives an NPC. */
	public GridTile getTile()
	{
		return tile;
	}
}
