package nl.dexanddice.solsurvivor.core;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The two plain clocks. The tick counter runs on every game tick in the arena from the moment it is first seen there,
 * and only the player's hotkey resets it; counting up, the modifier confirmation also resets it to 0. The wave timer is
 * real time, in seconds, from the player's modifier confirmation to the wave's completion. Nothing here knows an
 * NPC's timing, a spawn tick or a plan; each number has exactly the same meaning and appearance.
 */
public final class WaveClock
{
	private static final Pattern TAG = Pattern.compile("<[^>]*>");
	private static final Pattern COMPLETE = Pattern.compile("Wave (\\d+) completed!");
	private int wave;
	private long startMillis;
	private long endMillis = -1;
	private boolean running;
	private boolean sol;
	private int resetTick = -1;
	private int now = -1;
	private volatile View view = View.HIDDEN;

	/**
	 * A modifier was confirmed on this tick, at this time: the wave timer starts again. Counting up, the tick counter
	 * restarts at 0 too; a loop keeps its own origin, which only the player's hotkey moves.
	 */
	public void onModifierChosen(int number, int tick, long millis, boolean resetCounter)
	{
		sol = number > ColosseumTracker.LAST_WAVE;
		running = false;
		wave = 0;
		endMillis = -1;
		if (number >= 1 && !sol)
		{
			wave = number;
			startMillis = millis;
			running = true;
		}
		if (resetCounter)
		{
			resetTick = tick;
			now = Math.max(now, tick);
		}
		publish();
	}

	/** Each game tick in the arena. The first one starts the tick counter, with no press or pick needed. */
	public void onTick(int tick)
	{
		if (resetTick < 0)
		{
			resetTick = tick;
		}
		now = Math.max(now, tick);
		publish();
	}

	/**
	 * The player's hotkey: counting up, the counter shows 0 now and 1 on the next tick; looping, the next tick is 1.
	 * Picks, spawns and wave starts never call this.
	 */
	public void resetCounter(int tick)
	{
		resetTick = tick;
		now = Math.max(now, tick);
		publish();
	}

	/** The game's completion message freezes the wave timer. The tick counter goes on. */
	public void onGameMessage(String message, long millis)
	{
		Matcher completed = COMPLETE.matcher(TAG.matcher(message).replaceAll(""));
		if (running && completed.find() && Integer.parseInt(completed.group(1)) == wave)
		{
			running = false;
			endMillis = millis;
			publish();
		}
	}

	/**
	 * A wave's start message. Sol Heredit's wave hides both clocks. A wave whose pick the plugin missed, as when it was
	 * switched on mid-run, starts its timer here instead, so the timer is never left blank for a whole wave.
	 */
	public void onWaveStarted(int number, long millis)
	{
		sol = number > ColosseumTracker.LAST_WAVE;
		if (sol)
		{
			running = false;
			wave = 0;
		}
		else if (number != wave)
		{
			wave = number;
			startMillis = millis;
			endMillis = -1;
			running = true;
		}
		publish();
	}

	/** Leaving the arena, hopping, logging out or disabling the plugin forgets both clocks. */
	public void clear()
	{
		wave = 0;
		startMillis = 0;
		endMillis = -1;
		running = false;
		sol = false;
		resetTick = -1;
		now = -1;
		view = View.HIDDEN;
	}

	/** An immutable snapshot for the overlay renderer, which must not read a count half way through an event. */
	public View view()
	{
		return view;
	}

	private void publish()
	{
		view = new View(wave, running, startMillis, endMillis, sol || resetTick < 0 ? -1 : now - resetTick, sol);
	}

	/** The facts an overlay may draw, with no timing marks or changes of colour. */
	public static final class View
	{
		public static final View HIDDEN = new View(0, false, 0, -1, -1, false);
		public final int wave;
		public final boolean running;
		private final long startMillis;
		private final long endMillis;
		/** Ticks since the counter's last reset, or -1 while it is hidden. */
		public final int ticks;
		private final boolean sol;

		private View(int wave, boolean running, long startMillis, long endMillis, int ticks, boolean sol)
		{
			this.wave = wave;
			this.running = running;
			this.startMillis = startMillis;
			this.endMillis = endMillis;
			this.ticks = ticks;
			this.sol = sol;
		}

		/** The tick counter shows in the arena from its first tick, but never on Sol Heredit's wave. */
		public boolean counterVisible()
		{
			return ticks >= 0 && !sol;
		}

		/** The wave timer shows from a pick or a wave's start, waves 1 to 11. */
		public boolean timerVisible()
		{
			return wave >= 1 && wave <= ColosseumTracker.LAST_WAVE;
		}

		public String title()
		{
			return "Wave " + wave + (running ? "" : " done");
		}

		public String up()
		{
			return Integer.toString(Math.max(0, ticks));
		}

		/**
		 * 1 to the length and round again, the tick after a reset being 1. A length outside the setting's limits is
		 * bounded first, including imported configurations.
		 */
		public String loop(int length)
		{
			return Integer.toString(loopPosition(length));
		}

		/** The loop's position, 1 to the bounded length, which picks the position's colour when colours are on. */
		public int loopPosition(int length)
		{
			int bounded = Math.max(2, Math.min(10, length));
			return Math.floorMod(ticks - 1, bounded) + 1;
		}

		/** Whole seconds of real time, as m:ss, or h:mm:ss from an hour; frozen at the wave's completion. */
		public String time(long nowMillis)
		{
			long seconds = Math.max(0, ((endMillis >= 0 ? endMillis : nowMillis) - startMillis) / 1000);
			long hours = seconds / 3600;
			long minutes = seconds / 60 % 60;
			String ss = (seconds % 60 < 10 ? "0" : "") + seconds % 60;
			return hours > 0 ? hours + ":" + (minutes < 10 ? "0" : "") + minutes + ":" + ss : minutes + ":" + ss;
		}
	}
}
