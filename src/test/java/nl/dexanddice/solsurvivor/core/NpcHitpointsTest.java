package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * The NPC hitpoints' count, fed the spawns, hitsplats and health bars the RuneLite layer passes on: the count from the
 * hitsplats, the bar's range keeping it honest, and the tag's heal and estimate.
 */
public class NpcHitpointsTest
{
	private static final int DAMAGE = 16;
	private static final int BLOCK = 12;
	private static final int FREM_BERSERKER_ID = 12816;

	private NpcHitpoints hitpoints;

	@Before
	public void setUp()
	{
		hitpoints = new NpcHitpoints();
	}

	@Test
	public void anNpcThatSpawnsStartsAtItsKindsFullHitpoints()
	{
		hitpoints.onSpawned(1, NpcKind.MANTICORE_ID);
		NpcHitpoints.Reading reading = hitpoints.reading(1, 10);
		assertEquals(250, reading.hitpoints);
		assertEquals(250, reading.max);
		assertFalse(reading.estimated);
		assertEquals(0, reading.healed);
	}

	@Test
	public void damageComesOffAndAHealGoesBackUpToFull()
	{
		hitpoints.onSpawned(1, NpcKind.SHAMAN_ID);
		hitpoints.onHit(1, 40, DAMAGE, 5);
		hitpoints.onHit(1, 0, BLOCK, 6);
		assertEquals(85, hitpoints.reading(1, 6).hitpoints);
		hitpoints.onHit(1, 60, ColosseumTracker.HEAL_HITSPLAT, 7);
		NpcHitpoints.Reading reading = hitpoints.reading(1, 7);
		assertEquals(125, reading.hitpoints);
		assertEquals(60, reading.healed);
		assertEquals(0, hitpoints.reading(1, 7 + NpcHitpoints.HEAL_SHOWN_TICKS).healed);
	}

	@Test
	public void healsCloseTogetherShowAsOne()
	{
		hitpoints.onSpawned(1, NpcKind.MINOTAUR_ID);
		hitpoints.onHit(1, 100, DAMAGE, 1);
		hitpoints.onHit(1, 5, ColosseumTracker.HEAL_HITSPLAT, 2);
		hitpoints.onHit(1, 5, ColosseumTracker.HEAL_HITSPLAT, 3);
		assertEquals(10, hitpoints.reading(1, 3).healed);
		assertEquals(135, hitpoints.reading(1, 3).hitpoints);
	}

	@Test
	public void theCountNeverGoesBelowNothing()
	{
		hitpoints.onSpawned(1, NpcKind.JAGUAR_ID);
		hitpoints.onHit(1, 200, DAMAGE, 1);
		assertEquals(0, hitpoints.reading(1, 1).hitpoints);
	}

	@Test
	public void aBarThatAgreesLeavesTheCountExact()
	{
		hitpoints.onSpawned(1, NpcKind.MANTICORE_ID);
		hitpoints.onHit(1, 78, DAMAGE, 1);
		// 172 of 250 shows as 20 of 30 by the game's rounding.
		hitpoints.onTick(1, NpcKind.MANTICORE_ID, 20, 30);
		NpcHitpoints.Reading reading = hitpoints.reading(1, 1);
		assertEquals(172, reading.hitpoints);
		assertFalse(reading.estimated);
	}

	@Test
	public void aBarThatDisagreesMovesTheCountIntoItsRangeAsAnEstimate()
	{
		// The Minotaur regenerates without a hitsplat, so the count falls behind it.
		hitpoints.onSpawned(1, NpcKind.MINOTAUR_ID);
		hitpoints.onHit(1, 150, DAMAGE, 1);
		int[] range = NpcHitpoints.range(225, 12, 30);
		hitpoints.onTick(1, NpcKind.MINOTAUR_ID, 12, 30);
		NpcHitpoints.Reading reading = hitpoints.reading(1, 2);
		assertEquals(range[0], reading.hitpoints);
		assertTrue(reading.estimated);
		// A full bar pins it again.
		hitpoints.onTick(1, NpcKind.MINOTAUR_ID, 30, 30);
		assertFalse(hitpoints.reading(1, 3).estimated);
		assertEquals(225, hitpoints.reading(1, 3).hitpoints);
	}

	@Test
	public void anNpcFirstSeenAfterItSpawnedIsAnEstimate()
	{
		hitpoints.onTick(4, NpcKind.JAVELIN_ID, -1, -1);
		NpcHitpoints.Reading reading = hitpoints.reading(4, 1);
		assertEquals(220, reading.hitpoints);
		assertTrue(reading.estimated);
	}

	@Test
	public void npcsThePlannerDoesNotKnowAreNotCounted()
	{
		hitpoints.onSpawned(2, FREM_BERSERKER_ID);
		hitpoints.onTick(3, NpcKind.SOLARFLARE_ORB_ID, 10, 30);
		assertNull(hitpoints.reading(2, 1));
		assertNull(hitpoints.reading(3, 1));
	}

	@Test
	public void aDespawnOrLeavingForgetsTheCount()
	{
		hitpoints.onSpawned(1, NpcKind.SHAMAN_ID);
		hitpoints.onSpawned(2, NpcKind.SHAMAN_ID);
		hitpoints.onDespawned(1);
		assertNull(hitpoints.reading(1, 1));
		hitpoints.clear();
		assertNull(hitpoints.reading(2, 1));
	}

	@Test
	public void theBarsRangeFollowsTheGamesRounding()
	{
		assertArrayEquals(new int[] {0, 0}, NpcHitpoints.range(250, 0, 30));
		assertArrayEquals(new int[] {250, 250}, NpcHitpoints.range(250, 30, 30));
		assertArrayEquals(new int[] {1, 9}, NpcHitpoints.range(250, 1, 30));
		assertArrayEquals(new int[] {242, 249}, NpcHitpoints.range(250, 29, 30));
		for (int ratio = 1; ratio < 30; ratio++)
		{
			int[] range = NpcHitpoints.range(250, ratio, 30);
			assertTrue("ratio " + ratio, range[0] <= range[1]);
		}
	}

	@Test
	public void aQuarterOrFewerIsLow()
	{
		hitpoints.onSpawned(1, NpcKind.MINOTAUR_ID);
		hitpoints.onHit(1, 168, DAMAGE, 1);
		assertFalse(hitpoints.reading(1, 1).low());
		hitpoints.onHit(1, 1, DAMAGE, 2);
		assertTrue(hitpoints.reading(1, 2).low());
	}
}
