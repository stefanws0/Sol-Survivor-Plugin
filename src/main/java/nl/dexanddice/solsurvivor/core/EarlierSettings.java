package nl.dexanddice.solsurvivor.core;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * The settings that left the list on 2026-11-12, read as a player of an earlier release stored them: the mistake
 * tracker's seven sub-switches and the combined clock's layout and size (the Tick loop's ten colours are read beside
 * them, by the plugin). No setting shows them any more and nothing writes them, but a player who changed one keeps it
 * as they set it (Stefan's rule, 2026-11-12: an existing player's settings are left alone). Each is looked up by the key
 * it always had, as text, null when never stored; a value never stored, or one that does not read, is the default,
 * which is what the plugin does for everyone else.
 */
public final class EarlierSettings
{
	private EarlierSettings()
	{
	}

	/** The Tick loop colours' keys end in the loop's position, 1 to 10. */
	public static final String LOOP_COLOUR = "tickLoopColour";

	/** The mistake kinds counted: every kind but those whose switch the player turned off. */
	public static Set<WaveReview.Kind> counted(UnaryOperator<String> stored)
	{
		Set<WaveReview.Kind> kinds = EnumSet.allOf(WaveReview.Kind.class);
		for (WaveReview.Kind kind : WaveReview.Kind.values())
		{
			if ("false".equalsIgnoreCase(trimmed(stored.apply(switchOf(kind)))))
			{
				kinds.remove(kind);
			}
		}
		return kinds;
	}

	/** How many idle ticks in a row count as a mistake: 1 to 20 as the setting allowed, WaveReview.IDLE_FROM unless set. */
	public static int idleFrom(UnaryOperator<String> stored)
	{
		try
		{
			return Math.max(1, Math.min(20, Integer.parseInt(trimmed(stored.apply("idleFrom")))));
		}
		catch (NumberFormatException ex)
		{
			return WaveReview.IDLE_FROM;
		}
	}

	/** Whether the combined clock draws labelled rows, Ticks and Time, in place of the stacked numbers. */
	public static boolean clockInRows(UnaryOperator<String> stored)
	{
		return "ROWS".equals(trimmed(stored.apply("combinedTimerLayout")));
	}

	/** Whether the stacked combined clock draws the time large and the tick small, in place of the tick large. */
	public static boolean timeLarge(UnaryOperator<String> stored)
	{
		return "TIME".equals(trimmed(stored.apply("largeTimerNumber")));
	}

	private static String switchOf(WaveReview.Kind kind)
	{
		switch (kind)
		{
			case PAST_PRAYER:
				return "countPastPrayer";
			case LATE_FLICK:
				return "countLateFlicks";
			case SPECIAL:
				return "countSpecials";
			case VOLATILITY:
				return "countVolatility";
			case SOLARFLARE_SAND:
				return "countSolarflareSand";
			default:
				return "countIdle";
		}
	}

	private static String trimmed(String stored)
	{
		return stored == null ? "" : stored.trim();
	}
}
