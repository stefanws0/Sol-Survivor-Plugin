package nl.dexanddice.solsurvivor.core;

/**
 * An NPC as the state link holds it: its kind, its south-west tile and, for a Manticore, its orb code; and what the plugin
 * saw of its attacks, for the link's `attacks`.
 */
public final class ExportNpc
{
	private final NpcKind kind;
	private final GridTile tile;
	private final String orbs;
	private final Integer sinceAttack;
	private final Integer attacks;

	public ExportNpc(NpcKind kind, GridTile tile, String orbs)
	{
		this(kind, tile, orbs, null, null);
	}

	public ExportNpc(NpcKind kind, GridTile tile, String orbs, Integer sinceAttack, Integer attacks)
	{
		this.kind = kind;
		this.tile = tile;
		this.orbs = orbs;
		this.sinceAttack = sinceAttack;
		this.attacks = attacks;
	}

	/** How many ticks ago its last attack animation started, or null when the plugin has seen none. */
	public Integer getSinceAttack()
	{
		return sinceAttack;
	}

	/**
	 * The attacks it has made, or null when the plugin does not know: it saw the NPC spawn and counted each one since, or
	 * for a Javelin Colossus it saw a special, which sets the count to a multiple of 5.
	 */
	public Integer getAttacks()
	{
		return attacks;
	}

	public NpcKind getKind()
	{
		return kind;
	}

	public GridTile getTile()
	{
		return tile;
	}

	/** The Manticore's LoS link orb code, or null for any other NPC. */
	public String getOrbs()
	{
		return orbs;
	}
}
