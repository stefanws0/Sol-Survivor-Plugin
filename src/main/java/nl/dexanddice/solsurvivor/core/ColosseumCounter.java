package nl.dexanddice.solsurvivor.core;

/**
 * The game's own counters for the wave and the run under way, which the wave log keeps on the wave's first tick and on
 * each tick one changes (WaveLogWriter's `counters`): each is a varp or a varbit, by the id RuneLite's gameval names it
 * by (VarPlayerID, VarbitID), and its key in the log is that name in camel case without COLOSSEUM_. What each holds is
 * the game's to say and the logs' to show: nothing in the plugin or on the site reads them yet. They are there so that
 * the logs can check the planner's Doom count and price a wave in the game's own glory.
 *
 * Only the counters of the wave and the run under way are logged: none of the account's own, such as its best glory or
 * waves completed, which the hiscores could tie to the player in a shared log. A shared log leaves out the wave's start
 * time and the run's total duration too (`shared`): nobody has seen what they hold, and a count of the world's ticks
 * would date the wave, which the Privacy page says a shared log does not. Copy wave log keeps them.
 */
public enum ColosseumCounter
{
	/** Doom's stacks on the player: VarbitID.COLOSSEUM_DOOM_STACKS_CLIENT. */
	DOOM_STACKS("doomStacks", true, 9801, true),
	/** VarPlayerID.COLOSSEUM_CURRENT_GLORY. */
	CURRENT_GLORY("currentGlory", false, 4132, true),
	/** VarPlayerID.COLOSSEUM_WAVE_START_TIME: not shared. */
	WAVE_START_TIME("waveStartTime", false, 4133, false),
	/** VarPlayerID.COLOSSEUM_WAVE_DAMAGE_TAKEN. */
	WAVE_DAMAGE_TAKEN("waveDamageTaken", false, 4134, true),
	/** VarPlayerID.COLOSSEUM_LAST_MODIFIER_GLORY. */
	LAST_MODIFIER_GLORY("lastModifierGlory", false, 4135, true),
	/** VarPlayerID.COLOSSEUM_LAST_WAVE_DURATION. */
	LAST_WAVE_DURATION("lastWaveDuration", false, 4136, true),
	/** VarPlayerID.COLOSSEUM_TOTAL_DURATION: not shared. */
	TOTAL_DURATION("totalDuration", false, 4137, false);

	private final String key;
	private final boolean varbit;
	private final int id;
	private final boolean shared;

	ColosseumCounter(String key, boolean varbit, int id, boolean shared)
	{
		this.key = key;
		this.varbit = varbit;
		this.id = id;
		this.shared = shared;
	}

	/** Its key in the log's `counters`. */
	public String getKey()
	{
		return key;
	}

	/** Whether it is a varbit; else it is a varp. */
	public boolean isVarbit()
	{
		return varbit;
	}

	/** The game's id for the varbit or the varp. */
	public int getId()
	{
		return id;
	}

	/** Whether a shared log (WaveLogWriter.toShare) holds it. */
	public boolean isShared()
	{
		return shared;
	}
}
