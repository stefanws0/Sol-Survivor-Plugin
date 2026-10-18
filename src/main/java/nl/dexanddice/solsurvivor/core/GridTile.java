package nl.dexanddice.solsurvivor.core;

import java.util.Objects;

/**
 * A tile of the arena in the LoS tool's terms, as Sol Survivor's state link holds it: x runs east and y south, each from
 * 0 to 33.
 */
public final class GridTile
{
	private final int x;
	private final int y;

	public GridTile(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof GridTile))
		{
			return false;
		}
		GridTile t = (GridTile) o;
		return x == t.x && y == t.y;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y);
	}

	@Override
	public String toString()
	{
		return "(" + x + "," + y + ")";
	}
}
