package nl.dexanddice.solsurvivor.core;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

/**
 * The Colosseum's modifiers: the game's id for each (its bit in the taken-modifiers mask the selector script gets), the
 * varbit that holds its tier where it has tiers, and its key in the state link's `modifiers` (SimConfig's names). The ids
 * and varbits are the ones the Fortis Colosseum plugin reads (LlemonDuck, BSD 2-Clause). Bees! and Dynamic Duo are
 * exported as "bees" and "dynamicDuo" so that the link names every modifier taken; the site does not simulate them, and
 * its reader passes over names it does not know.
 */
public enum Modifier
{
	MANTIMAYHEM(0, 4588, "mantimayhem"),
	REENTRY(1, 9792, "reentry"),
	BEES(2, 9791, "bees"),
	VOLATILITY(3, 9799, "volatility"),
	BLASPHEMY(4, 9790, "blasphemy"),
	RELENTLESS(5, 9798, "relentless"),
	QUARTET(6, -1, "quartet"),
	TOTEMIC(7, -1, "totemic"),
	DOOM(8, 10681, "doom"),
	DYNAMIC_DUO(9, -1, "dynamicDuo"),
	SOLARFLARE(10, 9797, "solarflare"),
	MYOPIA(11, 9795, "myopia"),
	FRAILTY(12, 9796, "frailty"),
	RED_FLAG(13, -1, "redFlag");

	/** The level varbit of a modifier without tiers (written -1 above, where a constant cannot yet be named). */
	private static final int NO_TIERS = -1;

	private final int id;
	private final int levelVarbit;
	private final String key;

	Modifier(int id, int levelVarbit, String key)
	{
		this.id = id;
		this.levelVarbit = levelVarbit;
		this.key = key;
	}

	/** The game's id, which is also its bit in the taken-modifiers mask. */
	public int getId()
	{
		return id;
	}

	/** Whether it has tiers I to III, whose level a varbit holds. */
	public boolean hasTiers()
	{
		return levelVarbit != NO_TIERS;
	}

	/** The varbit that holds its tier, 0 while it is not taken; only for a modifier with tiers. */
	public int getLevelVarbit()
	{
		return levelVarbit;
	}

	/** Its key in the state link's `modifiers`. */
	public String getKey()
	{
		return key;
	}

	/** Its name as the game gives it: "Mantimayhem", "Bees!", "Dynamic Duo". */
	public String getName()
	{
		switch (this)
		{
			case BEES:
				return "Bees!";
			case DYNAMIC_DUO:
				return "Dynamic Duo";
			case RED_FLAG:
				return "Red Flag";
			default:
				return name().charAt(0) + name().substring(1).toLowerCase(Locale.ROOT);
		}
	}

	/** Its name at a tier, as the game gives it: "Mantimayhem II", or "Quartet" for a modifier without tiers. */
	public String getName(int tier)
	{
		if (!hasTiers() || tier < 1 || tier > 3)
		{
			return getName();
		}
		return getName() + " " + (tier == 1 ? "I" : tier == 2 ? "II" : "III");
	}

	/** The modifier with a game id, or null for an id this plugin does not know. */
	public static Modifier forId(int id)
	{
		for (Modifier m : values())
		{
			if (m.id == id)
			{
				return m;
			}
		}
		return null;
	}

	/** The modifiers in a taken-modifiers mask. */
	public static Set<Modifier> forMask(int mask)
	{
		Set<Modifier> out = EnumSet.noneOf(Modifier.class);
		for (Modifier m : values())
		{
			if ((mask & (1 << m.id)) != 0)
			{
				out.add(m);
			}
		}
		return out;
	}
}
