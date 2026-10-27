package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * The wave report's and the death recap's reading of a wave's log (WaveReview). The wave below is made up, a mistake of
 * each kind a few ticks apart and a death at its end, and its log is compared whole with
 * src/test/resources/fixtures/wave-review.json, which the site's run review reads too (test/link/pluginFixtures.test.ts),
 * so that the report puts each hit down to what the site's review does.
 */
public class WaveReviewTest
{
	private static final int SHAMAN = 11;
	private static final int JAVELIN = 12;
	private static final int MANTICORE = 14;
	private static final int ARCHER = 15;
	private static final int JAGUAR = 17;
	private static final int ORB = 30;
	private static final int BLAST = 40;
	private static final int PLAYER_ATTACK = 426;
	private static final int TWISTED_BOW = 20997;

	/** The ticks the player stood on molten sand, which the tracker would know. */
	private static final Set<Integer> SAND = new HashSet<>(Collections.singletonList(44));

	/**
	 * The made-up wave, under Volatility I: the player on one tile, attacking every 4 ticks but for a gap after tick 21;
	 * a serpent shaman, a Javelin Colossus, a Manticore, a frem archer and a Jaguar warrior around them; Solarflare's orb on
	 * tick 40, and the Jaguar's explosion on tick 50. `gear` puts the twisted bow in the log.
	 */
	static WaveLog wave(boolean gear)
	{
		Map<Integer, List<String>> prayers = new HashMap<>();
		Map<Integer, List<int[]>> attacks = new HashMap<>();
		Map<Integer, Integer> hits = new HashMap<>();
		// The shaman's spell meets Protect from Missiles, and hits for 12 three ticks on.
		for (int t = 1; t <= 3; t++)
		{
			prayers.put(t, list("ranged"));
		}
		attack(attacks, 2, SHAMAN, AttackAnimations.SHAMAN_CAST);
		hits.put(5, 12);
		// Its next meets no prayer, and hits for 0.
		attack(attacks, 8, SHAMAN, AttackAnimations.SHAMAN_CAST);
		hits.put(11, 0);
		// The Javelin's throw: Protect from Missiles a tick late, and a hit of 9.
		prayers.put(14, list("magic"));
		prayers.put(15, list("ranged"));
		attack(attacks, 14, JAVELIN, AttackAnimations.JAVELIN_THROW);
		hits.put(17, 9);
		// Its special lands on the player 6 ticks on.
		attack(attacks, 20, JAVELIN, AttackAnimations.JAVELIN_SPECIAL);
		hits.put(26, 17);
		// The Manticore's orbs on 30, 31 and 32: magic blocked, ranged past Protect from Magic for 22, melee blocked.
		prayers.put(30, list("magic"));
		prayers.put(31, list("magic"));
		prayers.put(32, list("melee"));
		hits.put(32, 22);
		// The frem archer's attack meets no prayer, and hits for 5 a tick on.
		attack(attacks, 36, ARCHER, 10850);
		hits.put(37, 5);
		// Solarflare's orb, which the log cannot see attack; then a hit on the sand.
		hits.put(40, 3);
		hits.put(44, 4);
		// The Jaguar dies on 48, and its explosion goes off on 50 with the player in its square.
		hits.put(50, 6);
		// The Javelin's throw that kills: Protect from Magic up, and a hit of the last 21.
		prayers.put(53, list("magic"));
		attack(attacks, 53, JAVELIN, AttackAnimations.JAVELIN_THROW);
		hits.put(56, 21);

		Map<Modifier, Integer> modifiers = new EnumMap<>(Modifier.class);
		modifiers.put(Modifier.VOLATILITY, 1);
		WaveLog log = new WaveLog();
		log.start(5, 2000, modifiers, true, "test", null);
		int hitpoints = 99;
		for (int t = 0; t <= 56; t++)
		{
			for (int[] a : attacks.getOrDefault(t, Collections.emptyList()))
			{
				log.onNpcAnimation(a[0], a[1]);
			}
			if ((t - 1) % 4 == 0 && (t <= 21 || t >= 29))
			{
				log.onPlayerAnimation(PLAYER_ATTACK);
			}
			Integer hit = hits.get(t);
			if (hit != null)
			{
				log.onPlayerHit(hit, 16);
				hitpoints -= hit;
			}
			int[] orbs = t == 29 ? new int[] {2681, 2683, 2685} : t == 30 ? new int[] {2683, 2685} : t == 31 ? new int[] {2685} : new int[0];
			List<WaveLog.NpcTick> npcs = new ArrayList<>(Arrays.asList(
				npc(SHAMAN, NpcKind.SHAMAN_ID, 13, 20, 30),
				npc(JAVELIN, NpcKind.JAVELIN_ID, 11, 8, 30),
				new WaveLog.NpcTick(MANTICORE, NpcKind.MANTICORE_ID, new GridTile(14, 9), -1, 30, 30, WaveLog.TARGET_PLAYER, orbs),
				npc(ARCHER, WaveReview.FREM_ARCHER_ID, 7, 12, -1)));
			if (t <= 50)
			{
				npcs.add(npc(JAGUAR, NpcKind.JAGUAR_ID, 9, 14, t >= 48 ? 0 : 30));
			}
			if (t == 40)
			{
				npcs.add(npc(ORB, NpcKind.SOLARFLARE_ORB_ID, 10, 10, -1));
			}
			if (t >= 50 && t <= 52)
			{
				npcs.add(npc(BLAST, 12828, 9, 14, -1));
			}
			int[] worn = gear ? new int[] {-1, -1, -1, TWISTED_BOW, -1, -1, -1, -1, -1, -1, -1} : null;
			// The player fights the shaman throughout, so that a gap in their attacks is idling.
			log.record(2000 + t, new WaveLog.PlayerTick(new GridTile(8, 15), hitpoints, 50, prayers.getOrDefault(t, Collections.emptyList()), -1,
				SHAMAN, worn), npcs);
		}
		log.finish(WaveLog.End.LEFT);
		return log;
	}

	private static List<String> list(String... prayers)
	{
		return Arrays.asList(prayers);
	}

	private static void attack(Map<Integer, List<int[]>> attacks, int t, int index, int animation)
	{
		attacks.computeIfAbsent(t, k -> new ArrayList<>()).add(new int[] {index, animation});
	}

	private static WaveLog.NpcTick npc(int index, int id, int x, int y, int ratio)
	{
		return new WaveLog.NpcTick(index, id, new GridTile(x, y), -1, ratio, ratio < 0 ? -1 : 30, WaveLog.TARGET_PLAYER);
	}

	private static String lines(List<?> items)
	{
		StringBuilder out = new StringBuilder();
		for (Object item : items)
		{
			out.append(item).append('\n');
		}
		return out.toString();
	}

	@Test
	public void eachHitIsPutDownToWhatTheSitesRunReviewPutsItDownTo() throws Exception
	{
		WaveLog log = wave(false);
		assertEquals(StatePayloadTest.fixture("wave-review.json"), WaveLogWriter.toJson(log));
		WaveReview review = WaveReview.of(log, SAND, id -> 0);
		assertEquals(Integer.valueOf(5), review.getWave());
		assertEquals(""
				+ "t2 PAST_PRAYER Shaman's spell, with Protect from Missiles up 12\n"
				+ "t8 PAST_PRAYER Shaman's spell, with no protection prayer up 0\n"
				+ "t14 LATE_FLICK Javelin's throw: Protect from Missiles a tick late 9\n"
				+ "t25 IDLE Idle for 4 ticks\n"
				+ "t26 SPECIAL Javelin's special landed on your tile 17\n"
				+ "t31 PAST_PRAYER Manticore's ranged orb, with Protect from Magic up 22\n"
				+ "t36 PAST_PRAYER Archer's ranged attack, with no protection prayer up 5\n"
				+ "t40 SOLARFLARE_SAND Solarflare's orb hit you 3\n"
				+ "t44 SOLARFLARE_SAND Molten sand burned you 4\n"
				+ "t50 VOLATILITY Volatility blast from the Jaguar you killed 6\n"
				+ "t53 PAST_PRAYER Javelin's throw, with Protect from Magic up 21\n",
			lines(review.getMistakes()));
		// The Manticore's magic and melee orbs on time; the Javelin's throw late; three past a wrong prayer, two past none.
		assertEquals(2, review.count(WaveReview.Prayed.ON_TIME));
		assertEquals(1, review.count(WaveReview.Prayed.LATE));
		assertEquals(0, review.count(WaveReview.Prayed.EARLY));
		assertEquals(3, review.count(WaveReview.Prayed.WRONG));
		assertEquals(2, review.count(WaveReview.Prayed.NONE));
		assertEquals(8, review.attacksJudged());
		assertEquals(99, review.getDamage());
	}

	@Test
	public void theRecapListsTheLastEightTicksAndWhatKilled()
	{
		WaveReview review = WaveReview.of(wave(false), SAND, id -> 0);
		assertEquals(Integer.valueOf(56), review.getDied());
		assertEquals(""
				+ "49 27  Nothing hit you –\n"
				+ "50 21  Volatility blast from the Jaguar you killed 6\n"
				+ "51 21  Nothing hit you –\n"
				+ "52 21  Nothing hit you –\n"
				+ "53 21 magic Javelin's throw, past your prayer –\n"
				+ "54 21  Nothing hit you –\n"
				+ "55 21  Nothing hit you –\n"
				+ "56 0  Javelin's throw 21\n",
			lines(review.getRecap()));
		assertEquals("The Javelin's throw hit you for 21 at 21 hitpoints.", review.getCause());
	}

	@Test
	public void anIdleStretchCountsFromTheWeaponsOwnSpeed()
	{
		// With the bow's speed known as 5, the gap after tick 21 is idle from 26 to 28.
		WaveReview review = WaveReview.of(wave(true), SAND, id -> id == TWISTED_BOW ? 5 : 0);
		List<WaveReview.Mistake> idle = new ArrayList<>();
		for (WaveReview.Mistake m : review.getMistakes())
		{
			if (m.getKind() == WaveReview.Kind.IDLE)
			{
				idle.add(m);
			}
		}
		assertEquals(1, idle.size());
		assertEquals(26, idle.get(0).getTick());
		assertEquals(3, idle.get(0).getTicks());
		assertNull(idle.get(0).getDamage());
	}

	@Test
	public void theReportCountsOnlyTheKindsTheSettingsCountAndSaysWhatARunsAreMostly()
	{
		WaveReview review = WaveReview.of(wave(false), SAND, id -> 0);
		Set<WaveReview.Kind> all = EnumSet.allOf(WaveReview.Kind.class);
		assertEquals(11, review.counted(all, 2).size());
		// The idle stretch of 4 ticks counts from 4 in a row, not from 5.
		assertEquals(11, review.counted(all, 4).size());
		assertEquals(10, review.counted(all, 5).size());
		assertEquals(5, review.counted(EnumSet.of(WaveReview.Kind.PAST_PRAYER), 2).size());
		List<WaveReview> run = Collections.singletonList(review);
		assertEquals("This run's most common mistakes are hits past your prayer: 5 of 11.", WaveReview.mostly(run, all, 2));
		assertEquals("Most of this run's mistakes are hits past your prayer: 5 of 6.",
			WaveReview.mostly(run, EnumSet.of(WaveReview.Kind.PAST_PRAYER, WaveReview.Kind.LATE_FLICK), 2));
		// Too few to say, and no kind ahead of the others.
		assertNull(WaveReview.mostly(run, EnumSet.of(WaveReview.Kind.SPECIAL, WaveReview.Kind.VOLATILITY), 2));
		assertNull(WaveReview.mostly(run, EnumSet.of(WaveReview.Kind.SPECIAL, WaveReview.Kind.VOLATILITY, WaveReview.Kind.LATE_FLICK), 2));
	}

	@Test
	public void aWaveWithoutTicksHasNothingToSay()
	{
		WaveLog log = new WaveLog();
		log.start(3, 0, new EnumMap<>(Modifier.class), true, "test", null);
		WaveReview review = WaveReview.of(log, Collections.emptySet(), id -> 0);
		assertTrue(review.getMistakes().isEmpty());
		assertEquals(0, review.attacksJudged());
		assertNull(review.getDied());
		assertNull(review.getCause());
	}

	@Test
	public void twoOfAKindAreToldApartByWhereTheyStand()
	{
		// Two Manticores, west and east of each other, both firing a magic orb that meets Protect from Missiles.
		WaveLog log = new WaveLog();
		log.start(9, 0, new EnumMap<>(Modifier.class), true, "test", null);
		for (int t = 0; t <= 3; t++)
		{
			int[] orbs = t == 1 ? new int[] {2681} : new int[0];
			List<WaveLog.NpcTick> npcs = Arrays.asList(
				new WaveLog.NpcTick(1, NpcKind.MANTICORE_ID, new GridTile(5, 9), -1, 30, 30, WaveLog.TARGET_PLAYER, orbs),
				new WaveLog.NpcTick(2, NpcKind.MANTICORE_ID, new GridTile(20, 9), -1, 30, 30, WaveLog.TARGET_PLAYER, orbs));
			log.record(t, new WaveLog.PlayerTick(new GridTile(12, 15), 99, 50, list("ranged"), -1, WaveLog.TARGET_NONE), npcs);
		}
		List<String> texts = new ArrayList<>();
		for (WaveReview.Mistake m : WaveReview.of(log, Collections.emptySet(), id -> 0).getMistakes())
		{
			if (m.getKind() == WaveReview.Kind.PAST_PRAYER)
			{
				texts.add(m.getText());
			}
		}
		assertEquals(Arrays.asList("West Manticore's magic orb, with Protect from Missiles up", "East Manticore's magic orb, with Protect from Missiles up"), texts);
	}
}
