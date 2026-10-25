package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The player's prayer over the newest waves the plugin counted (PrayerFlicks), for the site's Prayer reliability, "Mine":
 * the state links give their totals to the site as their `prayerRecord` (src/link/statePayload.ts), which sets the
 * reliability from them, kept to the safe side of what they show (src/viewer/prayerRecord.ts). It keeps the newest
 * MAX_WAVES waves that had an attack prayed against, as the site's own record does, so that it follows how the player
 * plays now; RuneLite's settings keep it between sessions as its text (toText).
 */
public final class PrayerRecord
{
	/** The most waves kept, about five whole runs: the site's RECORD_WAVES. */
	public static final int MAX_WAVES = 60;

	/** The text's version, its first part. */
	private static final String VERSION = "v1";

	private final List<PrayerFlicks> waves;

	private PrayerRecord(List<PrayerFlicks> waves)
	{
		this.waves = Collections.unmodifiableList(waves);
	}

	/** No wave counted. */
	public static PrayerRecord empty()
	{
		return new PrayerRecord(new ArrayList<>());
	}

	/** The record with a wave's counts added, and the oldest dropped past MAX_WAVES; itself for a wave with nothing prayed. */
	public PrayerRecord with(PrayerFlicks wave)
	{
		if (wave.isEmpty())
		{
			return this;
		}
		List<PrayerFlicks> next = new ArrayList<>(waves);
		next.add(wave);
		while (next.size() > MAX_WAVES)
		{
			next.remove(0);
		}
		return new PrayerRecord(next);
	}

	/** How many waves it holds. */
	public int getWaves()
	{
		return waves.size();
	}

	/** Its waves' counts together. */
	public PrayerFlicks total()
	{
		PrayerFlicks total = PrayerFlicks.NONE;
		for (PrayerFlicks w : waves)
		{
			total = total.plus(w);
		}
		return total;
	}

	/**
	 * The record as the state links give it to the site: [waves, flicks, held, late, unprayed], the totals of its waves
	 * (PrayerFlicks).
	 */
	public String toJson()
	{
		PrayerFlicks t = total();
		return "[" + waves.size() + "," + t.getFlicks() + "," + t.getHeld() + "," + t.getLate() + "," + t.getUnprayed() + "]";
	}

	/** The record as RuneLite's settings keep it: "v1", then each wave's flicks, held, late and unprayed, oldest first. */
	public String toText()
	{
		StringBuilder b = new StringBuilder(VERSION);
		for (PrayerFlicks w : waves)
		{
			b.append(';').append(w.getFlicks()).append(',').append(w.getHeld()).append(',').append(w.getLate()).append(',').append(w.getUnprayed());
		}
		return b.toString();
	}

	/** The record from its text (toText); empty when there is none, or it cannot be read. */
	public static PrayerRecord of(String text)
	{
		if (text == null || !text.startsWith(VERSION))
		{
			return empty();
		}
		String[] parts = text.split(";");
		if (!parts[0].equals(VERSION))
		{
			return empty();
		}
		List<PrayerFlicks> waves = new ArrayList<>();
		for (int i = 1; i < parts.length; i++)
		{
			String[] n = parts[i].split(",");
			if (n.length != 4)
			{
				return empty();
			}
			try
			{
				int[] v = new int[4];
				for (int k = 0; k < 4; k++)
				{
					v[k] = Integer.parseInt(n[k]);
					if (v[k] < 0)
					{
						return empty();
					}
				}
				waves.add(new PrayerFlicks(v[0], v[1], v[2], v[3]));
			}
			catch (NumberFormatException ex)
			{
				return empty();
			}
		}
		PrayerRecord record = empty();
		for (PrayerFlicks w : waves)
		{
			record = record.with(w);
		}
		return record;
	}
}
