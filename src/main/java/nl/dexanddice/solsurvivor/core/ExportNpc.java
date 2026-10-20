package nl.dexanddice.solsurvivor.core;

/**
 * An NPC as the state link holds it: its kind, its south-west tile and, for a Manticore, its orb code; what the plugin
 * saw of its attacks, for the link's `attacks`; and its health bar as the game last showed it, for the link's `health`.
 */
public final class ExportNpc
{
	private final NpcKind kind;
	private final GridTile tile;
	private final String orbs;
	private final Integer sinceAttack;
	private final Integer attacks;
	private final int[] health;

	public ExportNpc(NpcKind kind, GridTile tile, String orbs)
	{
		this(kind, tile, orbs, null, null, null);
	}

	public ExportNpc(NpcKind kind, GridTile tile, String orbs, Integer sinceAttack, Integer attacks)
	{
		this(kind, tile, orbs, sinceAttack, attacks, null);
	}

	public ExportNpc(NpcKind kind, GridTile tile, String orbs, Integer sinceAttack, Integer attacks, int[] health)
	{
		this.kind = kind;
		this.tile = tile;
		this.orbs = orbs;
		this.sinceAttack = sinceAttack;
		this.attacks = attacks;
		this.health = health == null ? null : health.clone();
	}

	/**
	 * Its health bar as the game last showed it, [ratio, scale], or null when the plugin has seen none since it spawned,
	 * or since a heal, which the bar has not shown yet.
	 */
	public int[] getHealth()
	{
		return health == null ? null : health.clone();
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
