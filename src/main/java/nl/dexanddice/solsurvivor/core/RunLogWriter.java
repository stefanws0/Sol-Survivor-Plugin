package nl.dexanddice.solsurvivor.core;

import java.util.List;

/**
 * The run log's JSON: every wave of the newest run in one paste, which the panel's "Copy whole run" copies, so that the
 * site can review the run one wave after another. Like WaveLogWriter it is written by hand, ASCII only and in a fixed
 * order, so that a run always gives the same text and the tests can compare it whole. Each wave's log starts on a line
 * of its own and keeps its own lines, a tick on each, so that a run reads and diffs a wave and a tick at a time.
 *
 * <pre>
 * {"v":1,"kind":"solSurvivorRun","plugin":"0.2.0","waves":[
 * {"v":1,"kind":"solSurvivorWaveLog","plugin":"0.2.0","wave":1,...,"ticks":[
 * {"t":0,...},
 * ...
 * ]},
 * {"v":1,"kind":"solSurvivorWaveLog","plugin":"0.2.0","wave":2,...},
 * ...
 * ]}
 * </pre>
 *
 * <ul>
 * <li>`v`: the run log's version, 1; `kind`: always "solSurvivorRun", which tells it apart from a wave log and a state
 * link's payload; `plugin`: the version of the plugin that copied it.</li>
 * <li>`waves`: the run's wave logs, oldest first, each exactly as Copy wave log copies it (WaveLogWriter documents its
 * shape), with its own `plugin` and `wave`. The run is the one WaveLog.getRun picks: the newest wave logged and the
 * waves before it, one after another, back to wave 1, or to the oldest of them the plugin logged when it did not see the
 * run from wave 1. The last wave may still be under way, its `end` null. Empty when there is no log.</li>
 * </ul>
 */
public final class RunLogWriter
{
	/** The run log's version. */
	public static final int VERSION = 1;

	/** The run log's `kind`. */
	public static final String KIND = "solSurvivorRun";

	private RunLogWriter()
	{
	}

	/** The run's JSON: `run` is its logs, oldest first, as WaveLog.getRun gives them; `plugin` is the plugin's version. */
	public static String toJson(List<WaveLog.Past> run, String plugin)
	{
		StringBuilder b = new StringBuilder();
		b.append("{\"v\":").append(VERSION).append(",\"kind\":");
		StatePayload.string(b, KIND);
		b.append(",\"plugin\":");
		StatePayload.string(b, plugin);
		b.append(",\"waves\":[");
		boolean first = true;
		for (WaveLog.Past wave : run)
		{
			b.append(first ? "\n" : ",\n");
			first = false;
			b.append(wave.getJson());
		}
		b.append(first ? "]}" : "\n]}");
		return b.toString();
	}
}
