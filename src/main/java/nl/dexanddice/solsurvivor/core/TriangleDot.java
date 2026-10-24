package nl.dexanddice.solsurvivor.core;

import java.util.Objects;

/**
 * Where the player wants the dot of the site's Safe / Fast / Easy triangle, which picks the plan the site suggests: the
 * three shares as the plugin's settings hold them, each from 0 to MAX_SHARE. The state links give it to the site as
 * their `triangle` (src/link/statePayload.ts), which reads each share as its part of the three's total, so they need not
 * add up to 100: 10, 0 and 90 is the dot the site reads out as "Safe 10% · Fast 0% · Easy 90%".
 */
public final class TriangleDot
{
	/** The most a share can be, the settings' range. */
	public static final int MAX_SHARE = 100;

	private final int safe;
	private final int fast;
	private final int easy;

	private TriangleDot(int safe, int fast, int easy)
	{
		this.safe = safe;
		this.fast = fast;
		this.easy = easy;
	}

	/**
	 * The dot of three shares, each kept within 0 and MAX_SHARE; null when none is above 0, which places no dot, so that
	 * the site keeps its own.
	 */
	public static TriangleDot of(int safe, int fast, int easy)
	{
		int s = share(safe);
		int f = share(fast);
		int e = share(easy);
		return s + f + e == 0 ? null : new TriangleDot(s, f, e);
	}

	private static int share(int value)
	{
		return Math.max(0, Math.min(MAX_SHARE, value));
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

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof TriangleDot))
		{
			return false;
		}
		TriangleDot d = (TriangleDot) o;
		return safe == d.safe && fast == d.fast && easy == d.easy;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(safe, fast, easy);
	}

	@Override
	public String toString()
	{
		return "Safe " + safe + " · Fast " + fast + " · Easy " + easy;
	}
}
