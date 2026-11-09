package nl.dexanddice.solsurvivor.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A wave of the run under way that the player finished, as the tracker kept it for the pick link (PickPayload's `run`):
 * what the site needs to work the wave's glory out by the game's rules, as its run review does from a wave log. The
 * plugin works nothing out itself.
 *
 * <ul>
 * <li>`ticks`: the ticks from the tick of the game's "Wave: N" message to that of "Wave N completed!", both counted, as
 * the wave log holds them; null when the plugin did not see the wave start.</li>
 * <li>`hit`: whether a hitsplat dealt the player damage in the wave (ColosseumTracker.onPlayerHit, the hits Doom counts):
 * Solarflare's and the sand's among them, which do not cost the game's no-damage bonus, so it can say true where the
 * game paid the bonus. Null when the plugin did not see the wave start.</li>
 * <li>`damage`: the most the game's own VarPlayerID.COLOSSEUM_WAVE_DAMAGE_TAKEN read in the wave, from the tick after
 * its first to the last before the next wave starts. Whether it counts only the damage that costs the no-damage bonus
 * is unconfirmed: it is sent beside `hit` so that the site and the shared logs can settle that, not to price the wave.
 * Null when the plugin did not see the wave start.</li>
 * <li>`modifiers`: the modifiers held through the wave, with their tiers.</li>
 * <li>`glory`: the game's own VarPlayerID.COLOSSEUM_CURRENT_GLORY as the wave ended, its last value before the next wave
 * starts. What it holds is unconfirmed: it is sent so that the site can check its sums against it, not to show it.</li>
 * <li>`drunk`: the doses of each potion gone from the inventory from the end of the wave before, or the run's first tick
 * the plugin saw, to the wave's end (Potions), for the site's verdict on the chest. Null when the plugin did not see the
 * run from before the wave.</li>
 * </ul>
 */
public final class RunWave
{
	private final int wave;
	private final Integer ticks;
	private final Boolean hit;
	private final Integer damage;
	private final Map<Modifier, Integer> modifiers;
	private final int glory;
	private final Map<String, Integer> drunk;

	public RunWave(int wave, Integer ticks, Boolean hit, Integer damage, Map<Modifier, Integer> modifiers, int glory)
	{
		this(wave, ticks, hit, damage, modifiers, glory, null);
	}

	public RunWave(int wave, Integer ticks, Boolean hit, Integer damage, Map<Modifier, Integer> modifiers, int glory, Map<String, Integer> drunk)
	{
		this.drunk = drunk == null ? null : Collections.unmodifiableMap(new LinkedHashMap<>(drunk));
		this.wave = wave;
		this.ticks = ticks;
		this.hit = hit;
		this.damage = damage;
		this.modifiers = modifiers.isEmpty() ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(modifiers));
		this.glory = glory;
	}

	/**
	 * The same wave with the game's counters as they read now: its glory as it is, and its damage the higher of the one
	 * kept and this, since the game may clear the counter before the next wave.
	 */
	public RunWave withCounters(int gloryNow, int damageNow)
	{
		return new RunWave(wave, ticks, hit, damage == null ? null : Math.max(damage, damageNow), modifiers, gloryNow, drunk);
	}

	public int getWave()
	{
		return wave;
	}

	public Integer getTicks()
	{
		return ticks;
	}

	public Boolean getHit()
	{
		return hit;
	}

	public Integer getDamage()
	{
		return damage;
	}

	public Map<Modifier, Integer> getModifiers()
	{
		return modifiers;
	}

	public int getGlory()
	{
		return glory;
	}

	/** The doses of each potion drunk in the wave, or null when the plugin did not count them. */
	public Map<String, Integer> getDrunk()
	{
		return drunk;
	}
}
