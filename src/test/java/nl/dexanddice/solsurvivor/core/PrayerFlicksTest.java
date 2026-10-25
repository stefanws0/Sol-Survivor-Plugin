package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The player's prayer in a wave's log (PrayerFlicks) and over the waves counted (PrayerRecord), for the site's Prayer
 * reliability. The wave below is made up, a case of each kind a few ticks apart, and its log is compared whole with
 * src/test/resources/fixtures/prayer-flicks.json, which the site's tests count too (test/link/pluginFixtures.test.ts), so
 * that the plugin's count and the site's agree. The link with the record is wave4-prayer.json, which the site opens.
 */
public class PrayerFlicksTest
{
	private static final int SHAMAN = 11;
	private static final int JAVELIN = 12;
	private static final int MINOTAUR = 13;
	private static final int MANTICORE = 14;
	private static final int ARCHER = 15;
	private static final int SEER = 16;
	private static final int JAGUAR = 17;
	private static final int FREM_ARCHER_ID = 12814;
	private static final int FREM_SEER_ID = 12815;


	/**
	 * The made-up wave: the player on one tile, a serpent shaman, a Javelin Colossus, a Minotaur, a Manticore, a frem
	 * archer and seer and a Jaguar warrior around them, each attacking on the ticks below, and the protection prayers on
	 * each tick.
	 */
	private static WaveLog wave()
	{
		Map<Integer, List<String>> prayers = new HashMap<>();
		Map<Integer, List<int[]>> attacks = new HashMap<>();
		// A flick: Magic on as the shaman's spell is checked.
		prayers.put(2, list("magic"));
		attack(attacks, 2, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// Held: Magic on since the tick before.
		prayers.put(5, list("magic"));
		prayers.put(6, list("magic"));
		attack(attacks, 6, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// Late: Magic a tick after.
		prayers.put(9, list("ranged"));
		prayers.put(10, list("ranged"));
		prayers.put(11, list("magic"));
		attack(attacks, 10, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// Early: Magic switched off on the tick.
		prayers.put(13, list("magic"));
		prayers.put(14, list("ranged"));
		attack(attacks, 14, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// Half-way: both on, the old one counting.
		prayers.put(17, list("ranged"));
		prayers.put(18, list("ranged", "magic"));
		prayers.put(19, list("magic"));
		attack(attacks, 18, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// Unprayed, with points left; then with none, which is left out.
		attack(attacks, 22, SHAMAN, AttackAnimations.SHAMAN_CAST);
		attack(attacks, 26, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// Tanked: Missiles on throughout, left out.
		prayers.put(29, list("ranged"));
		prayers.put(30, list("ranged"));
		prayers.put(31, list("ranged"));
		attack(attacks, 30, SHAMAN, AttackAnimations.SHAMAN_CAST);
		// A flick against the Javelin's throw; its special is left out.
		prayers.put(33, list("magic"));
		prayers.put(34, list("ranged"));
		attack(attacks, 34, JAVELIN, AttackAnimations.JAVELIN_THROW);
		attack(attacks, 38, JAVELIN, AttackAnimations.JAVELIN_SPECIAL);
		// The shaman and the Javelin on one tick: Magic held for the one, the other left out.
		prayers.put(41, list("magic"));
		prayers.put(42, list("magic"));
		attack(attacks, 42, SHAMAN, AttackAnimations.SHAMAN_CAST);
		attack(attacks, 42, JAVELIN, AttackAnimations.JAVELIN_THROW);
		// The Minotaur's swing, checked a tick later: a flick.
		prayers.put(46, list("ranged"));
		prayers.put(47, list("melee"));
		attack(attacks, 46, MINOTAUR, AttackAnimations.MINOTAUR_MELEE);
		// The Manticore's orbs, fired on 51, 52 and 53: held, a flick, and late.
		prayers.put(50, list("magic"));
		prayers.put(51, list("magic"));
		prayers.put(52, list("ranged"));
		prayers.put(53, list("ranged"));
		prayers.put(54, list("melee"));
		// The frem archer's attack, held; the seer's, tanked.
		prayers.put(55, list("ranged"));
		prayers.put(56, list("ranged"));
		attack(attacks, 56, ARCHER, 10850);
		prayers.put(59, list("ranged"));
		prayers.put(60, list("ranged"));
		prayers.put(61, list("ranged"));
		attack(attacks, 60, SEER, 10853);
		// The Jaguar's claws after its death, and the shaman's after the player's: left out.
		attack(attacks, 66, JAGUAR, AttackAnimations.JAGUAR_CLAWS);
		attack(attacks, 72, SHAMAN, AttackAnimations.SHAMAN_CAST);

		WaveLog log = new WaveLog();
		log.start(4, 1000, new EnumMap<>(Modifier.class), true, "test", null);
		for (int t = 0; t <= 72; t++)
		{
			for (int[] a : attacks.getOrDefault(t, Collections.emptyList()))
			{
				log.onNpcAnimation(a[0], a[1]);
			}
			int[] orbs = t < 50 ? new int[0] : t == 50 ? new int[] {2681, 2683, 2685} : t == 51 ? new int[] {2683, 2685} : t == 52 ? new int[] {2685} : new int[0];
			List<WaveLog.NpcTick> npcs = Arrays.asList(
				npc(SHAMAN, NpcKind.SHAMAN_ID, 13, 20, 30),
				npc(JAVELIN, NpcKind.JAVELIN_ID, 11, 8, 30),
				npc(MINOTAUR, NpcKind.MINOTAUR_ID, 6, 18, 30),
				new WaveLog.NpcTick(MANTICORE, NpcKind.MANTICORE_ID, new GridTile(14, 9), -1, 30, 30, WaveLog.TARGET_PLAYER, orbs),
				npc(ARCHER, FREM_ARCHER_ID, 7, 12, -1),
				npc(SEER, FREM_SEER_ID, 8, 12, -1),
				npc(JAGUAR, NpcKind.JAGUAR_ID, 9, 14, t >= 64 ? 0 : 30));
			int hitpoints = t >= 70 ? 0 : 99;
			int points = t == 26 ? 0 : 50;
			log.record(1000 + t, new WaveLog.PlayerTick(new GridTile(7, 15), hitpoints, points, prayers.getOrDefault(t, Collections.emptyList()), -1,
				WaveLog.TARGET_NONE), npcs);
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

	@Test
	public void eachKindOfAttackCountsAsTheSitesRunReviewCountsIt() throws Exception
	{
		WaveLog log = wave();
		assertEquals(StatePayloadTest.fixture("prayer-flicks.json"), WaveLogWriter.toJson(log));
		// Flicks on 2, 34, 47 and 52; held on 6, 42, 51 and 56; late on 10, 14, 18 and 53; unprayed on 22.
		assertEquals(new PrayerFlicks(4, 4, 4, 1), PrayerFlicks.count(log));
	}

	@Test
	public void aSwitchCaughtHalfWayKeepsTheOldPrayer()
	{
		WaveLog log = new WaveLog();
		log.start(1, 0, new EnumMap<>(Modifier.class), true, "test", null);
		log.record(0, new WaveLog.PlayerTick(new GridTile(7, 15), 99, 50, list("ranged"), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.record(1, new WaveLog.PlayerTick(new GridTile(7, 15), 99, 50, list("magic", "ranged"), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		List<WaveLog.Tick> ticks = log.getTicks();
		assertEquals("ranged", PrayerFlicks.protectionOn(ticks.get(1), ticks.get(0)));
		assertEquals("magic", PrayerFlicks.protectionOn(ticks.get(1), null));
		assertEquals(PrayerFlicks.NO_PRAYER, PrayerFlicks.protectionOn(new WaveLog.Tick(2, new WaveLog.PlayerTick(null, 99, 50, Collections.emptyList(), -1,
			WaveLog.TARGET_NONE), null, Collections.emptyList(), null, null, null, null, null, null), null));
	}

	@Test
	public void theRecordKeepsTheNewestWavesWithAnAttackPrayedAgainst()
	{
		PrayerRecord record = PrayerRecord.empty();
		assertSame(record, record.with(PrayerFlicks.NONE));
		for (int i = 0; i < PrayerRecord.MAX_WAVES + 5; i++)
		{
			record = record.with(new PrayerFlicks(i, 1, 0, 0));
		}
		assertEquals(PrayerRecord.MAX_WAVES, record.getWaves());
		// The oldest five, with 0 to 4 flicks, are gone.
		int flicks = 0;
		for (int i = 5; i < PrayerRecord.MAX_WAVES + 5; i++)
		{
			flicks += i;
		}
		assertEquals(new PrayerFlicks(flicks, PrayerRecord.MAX_WAVES, 0, 0), record.total());
	}

	@Test
	public void theRecordIsKeptAsTextAndJunkReadsAsNone()
	{
		PrayerRecord record = PrayerRecord.empty().with(new PrayerFlicks(4, 4, 4, 1)).with(new PrayerFlicks(20, 40, 1, 0));
		assertEquals("v1;4,4,4,1;20,40,1,0", record.toText());
		assertEquals(record.toText(), PrayerRecord.of(record.toText()).toText());
		assertEquals("[2,24,44,5,1]", record.toJson());
		for (String junk : new String[] {null, "", "v2;1,2,3,4", "v1;1,2,3", "v1;1,2,3,-4", "v1;a,b,c,d", "v10"})
		{
			assertEquals(junk, 0, PrayerRecord.of(junk).getWaves());
		}
		assertEquals(0, PrayerRecord.of("v1").getWaves());
	}

	@Test
	public void theRecordComesAfterTheRestOfTheLink() throws Exception
	{
		// wave4-prayer.json is wave4-start.json with the record above, which the site's tests open.
		String start = StatePayloadTest.fixture("wave4-start.json");
		PrayerRecord record = PrayerRecord.empty().with(new PrayerFlicks(4, 4, 4, 1)).with(new PrayerFlicks(20, 40, 1, 0));
		assertEquals(StatePayloadTest.fixture("wave4-prayer.json"), StatePayload.withPrayerRecord(start, record));
		String link = StatePayload.linkOfJson("https://sol.dexanddice.nl/", start, null, record);
		assertEquals(StatePayloadTest.fixture("wave4-prayer.json"), StatePayloadTest.unpack(link.substring(link.indexOf(StatePayload.STATE_HASH) + StatePayload.STATE_HASH.length())));
		// With the triangle too, the record comes last; with no wave counted, the link is as it was.
		assertTrue(StatePayload.withPrayerRecord(StatePayload.withTriangle(start, TriangleDot.of(10, 0, 90)), record).endsWith(",\"triangle\":[10,0,90],\"prayerRecord\":[2,24,44,5,1]}"));
		assertEquals(start, StatePayload.withPrayerRecord(start, PrayerRecord.empty()));
		assertEquals(start, StatePayload.withPrayerRecord(start, null));
	}
}
