package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;

import net.runelite.api.HitsplatID;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import nl.dexanddice.solsurvivor.core.AttackAnimations;
import nl.dexanddice.solsurvivor.core.ColosseumTracker;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveReview;
import org.junit.Test;

/**
 * The game's ids that core/ keeps as plain numbers, held to RuneLite's own names for them. core/ imports nothing of
 * RuneLite, so that its tests run without the client, and so nothing there says when the game renumbers one: the build
 * takes RuneLite's latest release, and with this a renumbering fails a test, not a player's wave.
 */
public class GameIdsTest
{
	@Test
	public void theNpcsAreTheGamesOwn()
	{
		assertEquals(NpcID.COLOSSEUM_JAGUAR_WARRIOR, NpcKind.JAGUAR_ID);
		assertEquals(NpcID.COLOSSEUM_STANDARD_MAGER, NpcKind.SHAMAN_ID);
		assertEquals(NpcID.COLOSSEUM_MINOTAUR, NpcKind.MINOTAUR_ID);
		assertEquals(NpcID.COLOSSEUM_MINOTAUR_ROUTEFIND, NpcKind.MINOTAUR_RED_FLAG_ID);
		assertEquals(NpcID.COLOSSEUM_JAVELIN_COLOSSUS, NpcKind.JAVELIN_ID);
		assertEquals(NpcID.COLOSSEUM_MANTICORE, NpcKind.MANTICORE_ID);
		assertEquals(NpcID.COLOSSEUM_SHOCKWAVE_COLOSSUS, NpcKind.SHOCKWAVE_ID);
		assertEquals(NpcID.COLOSSEUM_SOLAR_FLARE, NpcKind.SOLARFLARE_ORB_ID);
		assertEquals(NpcID.COLOSSEUM_WARBANDER_RANGED_FEMALE, WaveReview.FREM_ARCHER_ID);
		assertEquals(NpcID.COLOSSEUM_WARBANDER_MAGE_MALE, WaveReview.FREM_SEER_ID);
		assertEquals(NpcID.COLOSSEUM_WARBANDER_MELEE_MALE, WaveReview.FREM_BERSERKER_ID);
	}

	@Test
	public void theAttackAnimationsAreTheGamesOwn()
	{
		assertEquals(AnimationID.NPC_SERPENT_MAGER_CASTING, AttackAnimations.SHAMAN_CAST);
		assertEquals(AnimationID.NPC_COLOSSI_JAVELIN_01_RANGE_ATTACK, AttackAnimations.JAVELIN_THROW);
		assertEquals(AnimationID.NPC_COLOSSI_JAVELIN_01_ARTILLERY_ATTACK, AttackAnimations.JAVELIN_SPECIAL);
		assertEquals(AnimationID.NPC_JAGUAR_RANGER_CLAWS_ATTACK, AttackAnimations.JAGUAR_CLAWS);
		assertEquals(AnimationID.NPC_MINOTAUR_BOSS_ATTACK_MELEE, AttackAnimations.MINOTAUR_MELEE);
		assertEquals(AnimationID.NPC_MINOTAUR_BOSS_ATTACK_MAGIC, AttackAnimations.MINOTAUR_MAGIC);
		assertEquals(AnimationID.NPC_COLOSSI_SHOCKWAVE_01_CLAPATTACK, AttackAnimations.SHOCKWAVE_CLAP);
	}

	@Test
	public void theJavelinsLandingAndTheHealAreTheGamesOwn()
	{
		assertEquals(SpotanimID.WILD_FALLOFF_METEOR_BLAST_RED, ColosseumTracker.JAVELIN_LANDING_GRAPHIC);
		assertEquals(HitsplatID.HEAL, ColosseumTracker.HEAL_HITSPLAT);
	}
}
