package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Was it lag? (Stefan's pick of the feature round of 2026-11-12): the game ticks of one wave that came late. A tick
 * comes 0.6 seconds after the one before; one that takes a second or more is counted late, with how long it took, as
 * the plugin's own clock saw the two arrive. It is what already happened on the player's own connection, kept in memory
 * for the wave's report and its death recap, and never written to the wave log or sent anywhere.
 */
public final class LateTicks
{
	/** A game tick's length, in milliseconds. */
	public static final int TICK_MS = 600;
	/** From this long after the tick before, a tick counts as late. */
	public static final int LATE_MS = 1000;

	/** A tick of the wave that came late, and the milliseconds since the tick before it. */
	public static final class Late
	{
		public final int tick;
		public final long took;

		Late(int tick, long took)
		{
			this.tick = tick;
			this.took = took;
		}
	}

	private final List<Late> late = new ArrayList<>();
	private long lastAt = -1;

	/** A tick of the wave, by its number in the wave, as it arrives at `now` on the computer's clock in milliseconds. */
	public void record(int tick, long now)
	{
		if (lastAt >= 0 && now - lastAt >= LATE_MS)
		{
			late.add(new Late(tick, now - lastAt));
		}
		lastAt = now;
	}

	/** The late ticks from `from` to `to` of the wave, both counted, oldest first. */
	public List<Late> between(int from, int to)
	{
		List<Late> in = new ArrayList<>();
		for (Late l : late)
		{
			if (l.tick >= from && l.tick <= to)
			{
				in.add(l);
			}
		}
		return in;
	}

	/**
	 * The late ticks in a line, such as "Tick 44 took 1.4 s to come" or "Ticks 44 and 45 came late, the slowest in
	 * 1.4 s", or null for none.
	 */
	public static String text(List<Late> ticks)
	{
		if (ticks.isEmpty())
		{
			return null;
		}
		long slowest = 0;
		for (Late l : ticks)
		{
			slowest = Math.max(slowest, l.took);
		}
		String seconds = String.format(Locale.ROOT, "%.1f s", slowest / 1000.0);
		if (ticks.size() == 1)
		{
			return "Tick " + ticks.get(0).tick + " took " + seconds + " to come, not 0.6 s.";
		}
		StringBuilder names = new StringBuilder();
		int shown = Math.min(ticks.size(), 4);
		for (int i = 0; i < shown; i++)
		{
			names.append(i == 0 ? "" : i == shown - 1 && shown == ticks.size() ? " and " : ", ").append(ticks.get(i).tick);
		}
		String more = ticks.size() > shown ? " and " + (ticks.size() - shown) + " more" : "";
		return "Ticks " + names + more + " came late, the slowest in " + seconds + ", not 0.6 s.";
	}
}
