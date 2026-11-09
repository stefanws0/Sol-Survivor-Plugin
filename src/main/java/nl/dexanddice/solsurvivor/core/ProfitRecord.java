package nl.dexanddice.solsurvivor.core;

import java.util.Locale;

/**
 * The runs' profit added up (RunProfit), for the panel's Profit card: how many runs, how many ended in a death, what the
 * chest gave, what the runs used up and what the deaths' graves asked, and the last run on its own. The plugin keeps one for the session and one for
 * every run since the player last reset it, which RuneLite's settings keep for each account as its text (toText).
 */
public final class ProfitRecord
{
	/** The text's version, its first part; v1, before the deaths' fees, is read with none. */
	private static final String VERSION = "v2";
	private static final String VERSION_1 = "v1";

	private static final ProfitRecord EMPTY = new ProfitRecord(0, 0, 0, 0, 0, null);

	private final int runs;
	private final int deaths;
	private final long loot;
	private final long supplies;
	private final long deathFees;
	private final RunProfit last;

	private ProfitRecord(int runs, int deaths, long loot, long supplies, long deathFees, RunProfit last)
	{
		this.deathFees = deathFees;
		this.runs = runs;
		this.deaths = deaths;
		this.loot = loot;
		this.supplies = supplies;
		this.last = last;
	}

	/** No run counted. */
	public static ProfitRecord empty()
	{
		return EMPTY;
	}

	/** The record with `run` added as the last; itself for a run with nothing to count (RunProfit.counts). */
	public ProfitRecord with(RunProfit run)
	{
		if (!run.counts())
		{
			return this;
		}
		return new ProfitRecord(runs + 1, deaths + (run.isDied() ? 1 : 0), loot + run.getLoot(), supplies + run.getSupplies(),
			deathFees + run.getDeathFee(), run);
	}

	public int getRuns()
	{
		return runs;
	}

	public int getDeaths()
	{
		return deaths;
	}

	public long getLoot()
	{
		return loot;
	}

	public long getSupplies()
	{
		return supplies;
	}

	/** What the deaths' graves asked, estimated. */
	public long getDeathFees()
	{
		return deathFees;
	}

	/**
	 * The record with the last run's death fee set to `fee`, the grave's own as the player opened it, in place of the
	 * estimate; itself when the last run was no death.
	 */
	public ProfitRecord withLastDeathFee(long fee)
	{
		if (last == null || !last.isDied() || fee < 0)
		{
			return this;
		}
		RunProfit run = new RunProfit(last.getLoot(), last.getSupplies(), fee, true, last.getWave());
		return new ProfitRecord(runs, deaths, loot, supplies, Math.max(0, deathFees - last.getDeathFee() + fee), run);
	}

	/** The loot less the supplies and the deaths' fees. */
	public long getProfit()
	{
		return loot - supplies - deathFees;
	}

	/** The last run counted, or null for none. */
	public RunProfit getLast()
	{
		return last;
	}

	/** Whether it holds no run. */
	public boolean isEmpty()
	{
		return runs == 0;
	}

	/**
	 * The record as RuneLite's settings keep it: "v2", then the runs, deaths, loot, supplies and deaths' fees, then the last
	 * run's loot, supplies, fee, 1 for a death or 0, and its wave or nothing. A "v1" text has no fees.
	 */
	public String toText()
	{
		StringBuilder b = new StringBuilder(VERSION).append(';').append(runs).append(',').append(deaths).append(',').append(loot).append(',')
			.append(supplies).append(',').append(deathFees);
		if (last != null)
		{
			b.append(';').append(last.getLoot()).append(',').append(last.getSupplies()).append(',').append(last.getDeathFee()).append(',')
				.append(last.isDied() ? 1 : 0).append(',')
				.append(last.getWave() == null ? "" : last.getWave().toString());
		}
		return b.toString();
	}

	/** The record from its text (toText); empty when there is none, or it cannot be read. */
	public static ProfitRecord of(String text)
	{
		if (text == null)
		{
			return EMPTY;
		}
		String[] parts = text.split(";", -1);
		boolean v1 = parts[0].equals(VERSION_1);
		if (!(v1 || parts[0].equals(VERSION)) || parts.length < 2 || parts.length > 3)
		{
			return EMPTY;
		}
		// A v1 text is a v2 one with no fees, in the totals and in the last run.
		int fields = v1 ? 4 : 5;
		try
		{
			String[] t = parts[1].split(",", -1);
			if (t.length != fields)
			{
				return EMPTY;
			}
			int runs = Integer.parseInt(t[0]);
			int deaths = Integer.parseInt(t[1]);
			long loot = Long.parseLong(t[2]);
			long supplies = Long.parseLong(t[3]);
			long fees = v1 ? 0 : Long.parseLong(t[4]);
			if (runs < 0 || deaths < 0 || deaths > runs || loot < 0 || supplies < 0 || fees < 0)
			{
				return EMPTY;
			}
			RunProfit last = null;
			if (parts.length == 3)
			{
				String[] l = parts[2].split(",", -1);
				if (l.length != fields)
				{
					return EMPTY;
				}
				String died = l[fields - 2];
				if (!(died.equals("0") || died.equals("1")))
				{
					return EMPTY;
				}
				long lastLoot = Long.parseLong(l[0]);
				long lastSupplies = Long.parseLong(l[1]);
				long lastFee = v1 ? 0 : Long.parseLong(l[2]);
				if (lastLoot < 0 || lastSupplies < 0 || lastFee < 0)
				{
					return EMPTY;
				}
				String wave = l[fields - 1];
				last = new RunProfit(lastLoot, lastSupplies, lastFee, died.equals("1"), wave.isEmpty() ? null : Integer.valueOf(wave));
			}
			return new ProfitRecord(runs, deaths, loot, supplies, fees, last);
		}
		catch (NumberFormatException ex)
		{
			return EMPTY;
		}
	}

	/**
	 * Coins as the game's stacks show them, short: "950", "12.5K", "310K", "1.23M", "2.1B", with no sign, so that the panel
	 * stays narrow.
	 */
	public static String coins(long gp)
	{
		long a = Math.abs(gp);
		if (a < 10_000)
		{
			return String.format(Locale.UK, "%,d", a);
		}
		if (a < 100_000)
		{
			return trim(String.format(Locale.UK, "%.1f", a / 1_000.0)) + "K";
		}
		if (a < 1_000_000)
		{
			return (a / 1_000) + "K";
		}
		if (a < 1_000_000_000)
		{
			return trim(String.format(Locale.UK, "%.2f", a / 1_000_000.0)) + "M";
		}
		return trim(String.format(Locale.UK, "%.2f", a / 1_000_000_000.0)) + "B";
	}

	/** Coins with their sign: "+1.23M", "-310K", or "0" for none. */
	public static String signed(long gp)
	{
		return (gp > 0 ? "+" : gp < 0 ? "-" : "") + coins(gp);
	}

	/** "12.50" to "12.5", "3.00" to "3". */
	private static String trim(String number)
	{
		if (!number.contains("."))
		{
			return number;
		}
		String t = number.replaceAll("0+$", "");
		return t.endsWith(".") ? t.substring(0, t.length() - 1) : t;
	}
}
