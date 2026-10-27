package nl.dexanddice.solsurvivor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import nl.dexanddice.solsurvivor.core.Modifier;
import nl.dexanddice.solsurvivor.core.WaveRoster;

/**
 * What the panel's wave card and the wave box show on a tick, built on the client thread from the tracker: the wave
 * under way and its list, or the next wave and what it brings, the modifiers held, and Doom's stacks against its limit.
 * Only what is in the arena or on its way: no timers, and nothing of how to play the wave.
 */
final class WaveView
{
	static final WaveView NONE = new WaveView(null, false, Collections.<WaveRoster.Line>emptyList(), null, Collections.<WaveRoster.Line>emptyList(),
		Collections.<WaveRoster.Line>emptyList(), null, Collections.<Modifier, Integer>emptyMap(), 0, 0);

	/** The wave under way, or the next between waves; null before the plugin has seen one. */
	final Integer wave;
	final boolean underWay;
	/** The list of the wave under way; empty between waves. */
	final List<WaveRoster.Line> thisWave;
	/** The wave after `wave` while it is under way, or `wave` itself between waves; null when there is none to list. */
	final Integer next;
	/** The next wave at its start, its reinforcements, and those in a sentence, the panel's "Then…". */
	final List<WaveRoster.Line> nextWave;
	final List<WaveRoster.Line> nextLater;
	final String nextThen;
	final Map<Modifier, Integer> held;
	/** Doom's stacks on the player, by the game's own count, and its limit at the tier held; the limit is 0 without Doom. */
	final int doomStacks;
	final int doomLimit;

	WaveView(Integer wave, boolean underWay, List<WaveRoster.Line> thisWave, Integer next, List<WaveRoster.Line> nextWave,
		List<WaveRoster.Line> nextLater, String nextThen, Map<Modifier, Integer> held, int doomStacks, int doomLimit)
	{
		this.wave = wave;
		this.underWay = underWay;
		this.thisWave = thisWave;
		this.next = next;
		this.nextWave = nextWave;
		this.nextLater = nextLater;
		this.nextThen = nextThen;
		this.held = held;
		this.doomStacks = doomStacks;
		this.doomLimit = doomLimit;
	}

	/** Whether Doom is on its last stack: one more hit that deals damage kills. */
	boolean doomLast()
	{
		return doomLimit > 0 && doomStacks >= doomLimit - 1;
	}

	@Override
	public boolean equals(Object o)
	{
		if (!(o instanceof WaveView))
		{
			return false;
		}
		WaveView v = (WaveView) o;
		return Objects.equals(v.wave, wave) && v.underWay == underWay && v.thisWave.equals(thisWave) && Objects.equals(v.next, next)
			&& v.nextWave.equals(nextWave) && v.nextLater.equals(nextLater) && Objects.equals(v.nextThen, nextThen) && v.held.equals(held)
			&& v.doomStacks == doomStacks && v.doomLimit == doomLimit;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(wave, underWay, thisWave, next, nextWave, nextLater, nextThen, held, doomStacks, doomLimit);
	}
}
