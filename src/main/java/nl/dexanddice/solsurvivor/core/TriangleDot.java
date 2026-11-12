package nl.dexanddice.solsurvivor.core;

/**
 * Where the player wants the dot of the site's Safe / Fast / Easy triangle, which picks the plan the site suggests: one
 * of its corners, its middle, or the three shares an earlier release's settings held. The state links give it to the
 * site as their `triangle` (src/link/statePayload.ts), three shares that the site reads each as its part of the three's
 * total, so a corner is 100 for its own share and 0 for the others, and the middle is the same for all three.
 */
public final class TriangleDot
{
	public static final TriangleDot BALANCED = new TriangleDot(33, 33, 33);
	public static final TriangleDot SAFE = new TriangleDot(100, 0, 0);
	public static final TriangleDot FAST = new TriangleDot(0, 100, 0);
	public static final TriangleDot EASY = new TriangleDot(0, 0, 100);

	private final int safe;
	private final int fast;
	private final int easy;

	public TriangleDot(int safe, int fast, int easy)
	{
		this.safe = safe;
		this.fast = fast;
		this.easy = easy;
	}

	public int getSafe()
	{
		return safe;
	}

	public int getFast()
	{
		return fast;
	}

	public int getEasy()
	{
		return easy;
	}
}
