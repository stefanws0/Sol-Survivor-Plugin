package nl.dexanddice.solsurvivor;

import static nl.dexanddice.solsurvivor.PillarHiderTest.ARENA_CHUNK;
import static nl.dexanddice.solsurvivor.PillarHiderTest.ELSEWHERE_CHUNK;
import static nl.dexanddice.solsurvivor.PillarHiderTest.gameObject;
import static nl.dexanddice.solsurvivor.PillarHiderTest.instance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** The marks "Hide the pillars" leaves on the floor: one for each of the arena's pillars, and nothing else. */
public class PillarOverlayTest
{
	@Test
	public void eachPillarIsMarkedOnceThoughBothModelsStandOnIt()
	{
		Scene arena = instance(ARENA_CHUNK);
		GameObject north = gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM01, null, 52, 52);
		GameObject sameTiles = gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM02, null, 52, 52);
		GameObject east = gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM02, null, 60, 52);

		assertEquals(Arrays.asList(north, east), PillarOverlay.footprints(arena, Arrays.asList(north, sameTiles, east)));
	}

	@Test
	public void onlyTheArenasPillarsAreMarked()
	{
		GameObject brazier = gameObject(ObjectID.CIVITAS_COLOSSEUM_BRAZIER01, null, 52, 52);
		GameObject pillar = gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM01, null, 60, 52);
		List<GameObject> both = Arrays.asList(brazier, pillar);

		assertEquals(Arrays.asList(pillar), PillarOverlay.footprints(instance(ARENA_CHUNK), both));
		assertTrue(PillarOverlay.footprints(instance(ELSEWHERE_CHUNK), both).isEmpty());
	}
}
