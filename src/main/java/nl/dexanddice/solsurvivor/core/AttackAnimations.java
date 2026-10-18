package nl.dexanddice.solsurvivor.core;

/**
 * The animations that mark a Colosseum NPC's attack, so that the plugin can time each NPC's next attack and count a
 * Javelin Colossus's throws (the design spec's "Attack timing"). Partly confirmed: the ids are taken from the names RuneLite
 * gives the game's animations (net.runelite.api.gameval.AnimationID), and the user's wave logs have confirmed four of them so far.
 *
 * - The first wave log (wave 2, 2026-10-18) showed the shaman casting with SHAMAN_CAST while its spells hit the player,
 *   and the Javelin Colossus with JAVELIN_THROW as its javelin hit. The second (wave 2 again) had both attack 5 ticks
 *   apart, as the site's engine has them.
 * - The third (wave 1, 2026-10-18), the first with the animation starts, showed the Jaguar warrior starting JAGUAR_CLAWS
 *   every 5 ticks from the tick after it reached a tile beside the player, each attack bringing three hitsplats on the
 *   player. The shaman started SHAMAN_CAST every 5 ticks too, and 10860 when it died.
 * - The fourth (wave 2, Reentry I and Frailty I) showed a Javelin Colossus throwing JAVELIN_THROW four times and then
 *   JAVELIN_SPECIAL as its 5th, 10th and 15th attacks, 5 ticks apart throughout. It first attacked 833 ticks after it
 *   spawned, so the special is counted by its attacks, as ColosseumTracker counts them, not by ticks since its spawn.
 *   The Jaguar there started 10848 (its block) each time the player hit it, which is not an attack.
 * - Still unconfirmed: the Minotaur's two and SHOCKWAVE_CLAP.
 *
 * The logs also showed each NPC's death: the shaman 10860, the Jaguar 10849 and the Javelin Colossus 10894.
 *
 * Each NPC shows 10861 on the wave's first ticks, its spawn, and a Jaguar that spawns mid-wave shows 10862.
 *
 * The Manticore is left out: its attacks are its orbs, which OrbOrder follows, and its timer turns on its charge. So is
 * the Minotaur's heal, which shares its attack timer but has no animation named for it.
 */
public final class AttackAnimations
{
	/** NPC_SERPENT_MAGER_CASTING: the serpent shaman's spell. */
	public static final int SHAMAN_CAST = 10859;
	/** NPC_COLOSSI_JAVELIN_01_RANGE_ATTACK: the Javelin Colossus's normal throw. */
	public static final int JAVELIN_THROW = 10892;
	/** NPC_COLOSSI_JAVELIN_01_ARTILLERY_ATTACK: the Javelin Colossus's special, the javelins that land on tiles. */
	public static final int JAVELIN_SPECIAL = 10893;
	/** NPC_JAGUAR_RANGER_CLAWS_ATTACK: the Jaguar warrior's claws. */
	public static final int JAGUAR_CLAWS = 10847;
	/** NPC_MINOTAUR_BOSS_ATTACK_MELEE: the Minotaur's melee attack. */
	public static final int MINOTAUR_MELEE = 10843;
	/** NPC_MINOTAUR_BOSS_ATTACK_MAGIC: the Minotaur's other attack animation, counted as an attack too. */
	public static final int MINOTAUR_MAGIC = 10844;
	/** NPC_COLOSSI_SHOCKWAVE_01_CLAPATTACK: the Shockwave Colossus's clap. */
	public static final int SHOCKWAVE_CLAP = 10903;

	private AttackAnimations()
	{
	}

	/** Whether an animation of an NPC of this id is one of its attacks. */
	public static boolean isAttack(int npcId, int animationId)
	{
		switch (npcId)
		{
			case NpcKind.SHAMAN_ID:
				return animationId == SHAMAN_CAST;
			case NpcKind.JAVELIN_ID:
				return animationId == JAVELIN_THROW || animationId == JAVELIN_SPECIAL;
			case NpcKind.JAGUAR_ID:
				return animationId == JAGUAR_CLAWS;
			case NpcKind.MINOTAUR_ID:
			case NpcKind.MINOTAUR_RED_FLAG_ID:
				return animationId == MINOTAUR_MELEE || animationId == MINOTAUR_MAGIC;
			case NpcKind.SHOCKWAVE_ID:
				return animationId == SHOCKWAVE_CLAP;
			default:
				return false;
		}
	}

	/** Whether an animation of an NPC of this id is a Javelin Colossus's special. */
	public static boolean isSpecial(int npcId, int animationId)
	{
		return npcId == NpcKind.JAVELIN_ID && animationId == JAVELIN_SPECIAL;
	}
}
