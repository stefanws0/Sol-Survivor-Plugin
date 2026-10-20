package nl.dexanddice.solsurvivor.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The arena on one tick, as the state link's `setup` holds it (src/link/statePayload.ts, SetupV1). A field the plugin
 * does not know is null, and the link leaves it out, so that the site falls back on its own: the wave when the plugin
 * started after the wave's message, the tick within the wave likewise.
 */
public final class ArenaState
{
	private final List<ExportNpc> npcs;
	private final GridTile player;
	private final boolean waveStart;
	private final Integer wave;
	private final Integer waveTick;
	private final boolean reinforcementsArrived;
	private final Map<Modifier, Integer> modifiers;
	private final boolean modifiersComplete;
	private final List<GridTile> solarflareOrbs;
	private final Integer doomStacks;
	private final List<GridTile> sand;

	public ArenaState(
		List<ExportNpc> npcs,
		GridTile player,
		boolean waveStart,
		Integer wave,
		Integer waveTick,
		boolean reinforcementsArrived,
		Map<Modifier, Integer> modifiers,
		boolean modifiersComplete,
		List<GridTile> solarflareOrbs,
		Integer doomStacks,
		List<GridTile> sand)
	{
		this.npcs = Collections.unmodifiableList(npcs);
		this.player = player;
		this.waveStart = waveStart;
		this.wave = wave;
		this.waveTick = waveTick;
		this.reinforcementsArrived = reinforcementsArrived;
		this.modifiers = Collections.unmodifiableMap(modifiers);
		this.modifiersComplete = modifiersComplete;
		this.solarflareOrbs = Collections.unmodifiableList(solarflareOrbs);
		this.doomStacks = doomStacks;
		this.sand = Collections.unmodifiableList(sand);
	}

	/**
	 * Mid-wave under Doom: the hits that dealt the player damage since the wave started, Doom's stacks; null without
	 * Doom, at the wave's start, or when the plugin did not see the wave start.
	 */
	public Integer getDoomStacks()
	{
		return doomStacks;
	}

	/** The tiles of molten sand on the floor, as far as the plugin knows them: see ColosseumTracker.capture. */
	public List<GridTile> getSand()
	{
		return sand;
	}

	/** The tiles of Solarflare's orbs on this tick, in the order the game lists them; empty without Solarflare. */
	public List<GridTile> getSolarflareOrbs()
	{
		return solarflareOrbs;
	}

	public List<ExportNpc> getNpcs()
	{
		return npcs;
	}

	public GridTile getPlayer()
	{
		return player;
	}

	/** Whether this is the wave's first tick, the LoS link's "_ws". */
	public boolean isWaveStart()
	{
		return waveStart;
	}

	/** The wave, 1 to 11, or null when the plugin has not seen it start. */
	public Integer getWave()
	{
		return wave;
	}

	/** Mid-wave: how many ticks ago the wave started, or null when the plugin has not seen it start. */
	public Integer getWaveTick()
	{
		return waveTick;
	}

	/** Mid-wave: whether the wave's reinforcements have come, after which none will. */
	public boolean isReinforcementsArrived()
	{
		return reinforcementsArrived;
	}

	/** The modifiers taken, each with its tier (1 for one without tiers). */
	public Map<Modifier, Integer> getModifiers()
	{
		return modifiers;
	}

	/**
	 * Whether the modifiers are all known. Those with tiers are read from their varbits at any time; Quartet, Red Flag and
	 * Totemic only from the modifier selector, so a plugin started mid-run knows them from the next pick on.
	 */
	public boolean isModifiersComplete()
	{
		return modifiersComplete;
	}

	/** Mantimayhem III, the LoS link's "_mm3", which lets a Manticore fire its melee orb first. */
	public boolean isMm3()
	{
		Integer level = modifiers.get(Modifier.MANTIMAYHEM);
		return level != null && level == 3;
	}
}
