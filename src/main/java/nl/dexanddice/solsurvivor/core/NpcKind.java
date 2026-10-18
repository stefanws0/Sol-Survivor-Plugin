package nl.dexanddice.solsurvivor.core;

/**
 * The Colosseum's NPCs that a state link holds, with the game's NPC ids (combat research §1) and the LoS link's type codes
 * (src/data/types.ts). The Fremennik warband has no code: links never hold frems. A serpent shaman that spawns after the
 * wave's first tick is the reinforcement shaman, which the link codes apart.
 */
public enum NpcKind
{
	SERPENT_SHAMAN(1),
	JAVELIN_COLOSSUS(2),
	JAGUAR_WARRIOR(3),
	MANTICORE(4),
	MINOTAUR(5),
	SHOCKWAVE_COLOSSUS(6),
	REINFORCEMENT_SHAMAN(7);

	public static final int SHAMAN_ID = 12811;
	public static final int JAGUAR_ID = 12810;
	public static final int MINOTAUR_ID = 12812;
	/** The Minotaur under Red Flag, which routefinds round the pillars. */
	public static final int MINOTAUR_RED_FLAG_ID = 12813;
	public static final int JAVELIN_ID = 12817;
	public static final int MANTICORE_ID = 12818;
	public static final int SHOCKWAVE_ID = 12819;
	/** Solarflare's orb, nameless and with no options (combat research §11): not an NPC the link holds, but its tile is. */
	public static final int SOLARFLARE_ORB_ID = 12826;

	private final int code;

	NpcKind(int code)
	{
		this.code = code;
	}

	/** The LoS link's type code. */
	public int getCode()
	{
		return code;
	}

	/**
	 * The kind of an NPC by its id, or null for an NPC the link does not hold. A serpent shaman is the reinforcement shaman
	 * when it did not spawn on the wave's first tick.
	 */
	public static NpcKind forNpcId(int npcId, boolean reinforcement)
	{
		switch (npcId)
		{
			case SHAMAN_ID:
				return reinforcement ? REINFORCEMENT_SHAMAN : SERPENT_SHAMAN;
			case JAVELIN_ID:
				return JAVELIN_COLOSSUS;
			case JAGUAR_ID:
				return JAGUAR_WARRIOR;
			case MANTICORE_ID:
				return MANTICORE;
			case MINOTAUR_ID:
			case MINOTAUR_RED_FLAG_ID:
				return MINOTAUR;
			case SHOCKWAVE_ID:
				return SHOCKWAVE_COLOSSUS;
			default:
				return null;
		}
	}
}
