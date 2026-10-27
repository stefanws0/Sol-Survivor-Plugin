package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** The wave's list that the panel and the wave box show (WaveRoster), and Doom's limit. */
public class WaveRosterTest
{
	private static Map<Modifier, Integer> held(Modifier... modifiers)
	{
		Map<Modifier, Integer> out = new EnumMap<>(Modifier.class);
		for (Modifier m : modifiers)
		{
			out.put(m, 1);
		}
		return out;
	}

	private static String lines(List<WaveRoster.Line> lines)
	{
		StringBuilder out = new StringBuilder();
		for (WaveRoster.Line l : lines)
		{
			out.append(l).append(l.isDead() ? " dead" : "").append('\n');
		}
		return out.toString();
	}

	@Test
	public void wave7MidWayHasItsDeadStruckOutAndTheMinotaurToCome()
	{
		// The Javelin is dead; the Manticore, the Shockwave Colossus and two of the warband are left.
		WaveRoster.Alive alive = new WaveRoster.Alive().add(NpcKind.MANTICORE).add(NpcKind.SHOCKWAVE_COLOSSUS)
			.addFrem(WaveReview.FREM_ARCHER_ID).addFrem(WaveReview.FREM_SEER_ID);
		assertEquals(""
				+ "Manticore 1\n"
				+ "Shockwave Colossus 1\n"
				+ "Javelin Colossus 0 dead\n"
				+ "The warband of three 2\n"
				+ "Minotaur (reinforcement) to come\n",
			lines(WaveRoster.thisWave(7, held(), alive, false, false, 0, false)));
	}

	@Test
	public void theReinforcementsCountOnceTheyHaveCome()
	{
		WaveRoster.Alive alive = new WaveRoster.Alive().add(NpcKind.SERPENT_SHAMAN).add(NpcKind.MANTICORE).add(NpcKind.REINFORCEMENT_SHAMAN);
		assertEquals(""
				+ "Manticore 1\n"
				+ "Serpent shaman 1\n"
				+ "The warband of three 0 dead\n"
				+ "Jaguar warrior (reinforcement) 0 dead\n"
				+ "Serpent shaman (reinforcement) 1\n",
			lines(WaveRoster.thisWave(4, held(), alive, true, false, 0, false)));
	}

	@Test
	public void dynamicDuosTwoAndQuartetsFourthHaveLinesOfTheirOwn()
	{
		// Wave 8 under both: one Shockwave Colossus dead, and two berserkers left, one of them Quartet's.
		WaveRoster.Alive alive = new WaveRoster.Alive().add(NpcKind.JAVELIN_COLOSSUS).add(NpcKind.JAVELIN_COLOSSUS).add(NpcKind.MANTICORE)
			.add(NpcKind.SHOCKWAVE_COLOSSUS).addFrem(WaveReview.FREM_BERSERKER_ID).addFrem(WaveReview.FREM_BERSERKER_ID);
		List<WaveRoster.Line> lines = WaveRoster.thisWave(8, held(Modifier.DYNAMIC_DUO, Modifier.QUARTET), alive, false, false,
			WaveReview.FREM_BERSERKER_ID, true);
		assertEquals(""
				+ "Manticore 1\n"
				+ "Shockwave Colossus (Dynamic Duo) 0 dead\n"
				+ "Shockwave Colossus (Dynamic Duo) 1\n"
				+ "Javelin Colossus 2\n"
				+ "Archer (warband) 0 dead\n"
				+ "Seer (warband) 0 dead\n"
				+ "Berserker (warband) 1\n"
				+ "Berserker, Quartet's fourth (Quartet) 1\n"
				+ "Minotaur (reinforcement) to come\n",
			lines(lines));
	}

	@Test
	public void onTheWavesFirstTickEveryoneIsThere()
	{
		List<WaveRoster.Line> lines = WaveRoster.thisWave(10, held(Modifier.QUARTET), new WaveRoster.Alive(), false, true, 0, false);
		assertEquals(""
				+ "Manticore 2\n"
				+ "Javelin Colossus 2\n"
				+ "The warband of three 3\n"
				+ "Quartet's fourth frem (Quartet) 1\n"
				+ "Minotaur (reinforcement) to come\n"
				+ "Serpent shaman (reinforcement) to come\n",
			lines(lines));
	}

	@Test
	public void theNextWaveSaysWhoComesAndWhoJoinsLater()
	{
		// The warband comes at every wave's start, with the wave's own NPCs: only the reinforcements join later (the user,
		// 2026-10-26, from a wave 2 box that had the warband joining with the Jaguar).
		assertEquals("Javelin Colossus 2\nManticore 1\nShockwave Colossus 2\nThe warband of three 3\n",
			lines(WaveRoster.nextWave(8, held(Modifier.DYNAMIC_DUO))));
		assertEquals("Javelin Colossus 1\nSerpent shaman 1\nThe warband of three 3\n", lines(WaveRoster.nextWave(2, held())));
		assertEquals("Then a Jaguar warrior, the reinforcement.", WaveRoster.nextWaveThen(2, held()));
		assertEquals("Then a Minotaur, the reinforcement.", WaveRoster.nextWaveThen(8, held()));
		assertEquals("Javelin Colossus 1\nManticore 1\nSerpent shaman 1\nThe warband of three 3\nQuartet's fourth frem (Quartet) 1\n",
			lines(WaveRoster.nextWave(5, held(Modifier.QUARTET))));
		assertEquals("Then a Jaguar warrior and a second shaman, the reinforcements.", WaveRoster.nextWaveThen(5, held(Modifier.QUARTET)));
		// The wave box lists the reinforcements under its rule, each still to come.
		assertEquals("Jaguar warrior (reinforcement) to come\nSerpent shaman (reinforcement) to come\n", lines(WaveRoster.nextWaveLater(5)));
		assertEquals("Minotaur (reinforcement) to come\n", lines(WaveRoster.nextWaveLater(8)));
	}

	@Test
	public void solHereditsWaveHasNoList()
	{
		assertTrue(WaveRoster.thisWave(12, held(), new WaveRoster.Alive(), false, false, 0, false).isEmpty());
		assertTrue(WaveRoster.nextWave(12, held()).isEmpty());
		assertNull(WaveRoster.nextWaveThen(12, held()));
		assertTrue(WaveRoster.nextWaveLater(12).isEmpty());
		assertTrue(WaveRoster.thisWave(null, held(), new WaveRoster.Alive(), false, false, 0, false).isEmpty());
	}

	@Test
	public void doomKillsAtFifteenTenAndFiveHits()
	{
		assertEquals(Arrays.asList(0, 15, 10, 5), Arrays.asList(WaveRoster.doomLimit(0), WaveRoster.doomLimit(1), WaveRoster.doomLimit(2),
			WaveRoster.doomLimit(3)));
	}

	@Test
	public void modifiersAreNamedAsTheGameNamesThem()
	{
		assertEquals("Mantimayhem II", Modifier.MANTIMAYHEM.getName(2));
		assertEquals("Bees! III", Modifier.BEES.getName(3));
		assertEquals("Dynamic Duo", Modifier.DYNAMIC_DUO.getName(1));
		assertEquals("Red Flag", Modifier.RED_FLAG.getName());
		assertEquals(Collections.singletonList("Doom I"), Collections.singletonList(Modifier.DOOM.getName(1)));
	}
}
