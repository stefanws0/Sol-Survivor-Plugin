package nl.dexanddice.solsurvivor.core;

/**
 * A Manticore's orbs in the order they showed, and its LoS link orb code. A Manticore charges its three orbs one after
 * another, each shown as a graphic on it; the order they show in is the order it fires them. Once all three have shown
 * it has charged, and its code is the order alone ("r" and "m" for the two standard orders, whose melee orb comes last,
 * and "Mrm", "Mmr", "rMm" or "mMr" for Mantimayhem III's). Before that its code is "u", uncharged, followed by the order
 * when the orbs seen so far settle it: the first orb does without Mantimayhem III, two orbs do with it.
 */
public final class OrbOrder
{
	private final OrbStyle[] seen = new OrbStyle[3];
	private int count;

	/**
	 * An orb seen on the Manticore this tick. An orb already seen is passed over, since a graphic stays on for several
	 * ticks and the orders repeat once all three have fired.
	 */
	public void see(OrbStyle style)
	{
		if (style == null || count == 3)
		{
			return;
		}
		for (int i = 0; i < count; i++)
		{
			if (seen[i] == style)
			{
				return;
			}
		}
		seen[count++] = style;
	}

	/** How many of the three orbs have shown. */
	public int getCount()
	{
		return count;
	}

	/** The LoS link orb code, with Mantimayhem III on or off. */
	public String losCode(boolean mm3)
	{
		if (count == 3)
		{
			return patternOf(seen[0], seen[1], seen[2]);
		}
		if (count >= 1 && !mm3 && seen[0] != OrbStyle.MELEE)
		{
			// The standard orders put the melee orb last, so the first orb settles the order.
			OrbStyle second = seen[0] == OrbStyle.RANGED ? OrbStyle.MAGIC : OrbStyle.RANGED;
			return "u" + patternOf(seen[0], second, OrbStyle.MELEE);
		}
		if (count == 2 && mm3)
		{
			return "u" + patternOf(seen[0], seen[1], remaining(seen[0], seen[1]));
		}
		return "u";
	}

	private static OrbStyle remaining(OrbStyle a, OrbStyle b)
	{
		for (OrbStyle s : OrbStyle.values())
		{
			if (s != a && s != b)
			{
				return s;
			}
		}
		throw new IllegalArgumentException("two orbs of one style");
	}

	/** The order's name in the LoS link: the first orb's letter for a standard order, all three otherwise. */
	private static String patternOf(OrbStyle first, OrbStyle second, OrbStyle third)
	{
		if (third == OrbStyle.MELEE)
		{
			return String.valueOf(first.getCode());
		}
		return "" + first.getCode() + second.getCode() + third.getCode();
	}
}
