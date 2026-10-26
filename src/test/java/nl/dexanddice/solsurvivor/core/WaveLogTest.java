package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;

/**
 * The wave log, fed mocked game data as the RuneLite layer passes it on: a wave's start from the tracker, the hitsplats
 * as they come and a snapshot of the player and the NPCs each tick. The JSON is compared whole with
 * src/test/resources/fixtures/wave-log.json. The animation, graphic, item and hitsplat type ids here are made up: the
 * log passes on whatever the game gives.
 */
public class WaveLogTest
{
	private static final int FREM_RANGER_ID = 12814;
	/** An NPC the plugin has no kind for, logged by its raw id all the same: Minimus, here standing on the grid. */
	private static final int MINIMUS_ID = 12808;
	/** The state link's payload on the wave's first tick, as the RuneLite layer passes it on: here a short made-up one. */
	private static final String START_STATE = "{\"v\":1,\"setup\":{\"npcs\":[[13,20,\"2\"]],\"player\":[7,15],\"waveStart\":true,\"wave\":4}}";
	/** Gear worn, slot by slot in WaveLog.WORN_SLOTS' order: a bow, then a fang in its place. */
	private static final int[] BOW = {1001, 1002, -1, 1004, 1005, -1, 1007, -1, -1, -1, 1011};
	private static final int[] FANG = {1001, 1002, -1, 2004, 1005, -1, 1007, -1, -1, -1, 1011};
	/** The game's counters, one for each ColosseumCounter in its order: Doom's stacks, the glory, the times and the damage. */
	private static final int[] COUNTERS = {0, 1200, 51000, 0, 150, 120, 900};
	private static final int[] COUNTERS_HIT = {0, 1200, 51000, 13, 150, 120, 900};

	private WaveLog log;

	@Before
	public void setUp()
	{
		log = new WaveLog();
	}

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = WaveLogTest.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
			{
				throw new IOException("no fixture " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	private static WaveLog.PlayerTick player(int x, int y, int hp, int prayer, List<String> prayers, int animation, int target)
	{
		return new WaveLog.PlayerTick(new GridTile(x, y), hp, prayer, prayers, animation, target);
	}

	private static WaveLog.PlayerTick wearing(int[] worn, int x, int y)
	{
		return new WaveLog.PlayerTick(new GridTile(x, y), 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE, worn);
	}

	private static WaveLog.NpcTick npc(int index, int id, int x, int y, int animation, int ratio, int scale, int target)
	{
		return new WaveLog.NpcTick(index, id, new GridTile(x, y), animation, ratio, scale, target);
	}

	/** Wave 4 started by the tracker, with Reentry II, Quartet and Totemic taken and Solarflare I's varbit set. */
	private void startWave4(int gameTick)
	{
		ColosseumTracker tracker = new ColosseumTracker();
		tracker.onModifierOptions(Modifier.SOLARFLARE.getId(), Modifier.QUARTET.getId(), Modifier.DOOM.getId(),
			(1 << Modifier.REENTRY.getId()) | (1 << Modifier.TOTEMIC.getId()));
		tracker.onModifierChosen(2);
		Map<Modifier, Integer> varbits = new EnumMap<>(Modifier.class);
		varbits.put(Modifier.REENTRY, 2);
		varbits.put(Modifier.SOLARFLARE, 1);
		tracker.onGameMessage("<col=ef1020>Wave: 4</col>", gameTick);
		assertTrue(tracker.isWaveStartTick(gameTick));
		log.start(tracker.getWave(), gameTick, tracker.modifiers(m -> varbits.getOrDefault(m, 0)), tracker.isModifiersComplete(), "test",
			START_STATE);
	}

	@Test
	public void aWaveLog() throws Exception
	{
		// A hitsplat that came on the wave's first tick, before the tracker saw its message, is kept for that tick.
		log.onNpcHit(12, 0, 12);
		startWave4(100);
		log.record(100, new WaveLog.PlayerTick(new GridTile(7, 15), 99, 77, Arrays.asList("magic", "piety"), -1, WaveLog.TARGET_NONE, BOW),
			Arrays.asList(
			npc(11, NpcKind.SHAMAN_ID, 13, 20, -1, -1, -1, WaveLog.TARGET_PLAYER),
			npc(12, NpcKind.MANTICORE_ID, 16, 24, 7590, 30, 30, WaveLog.TARGET_PLAYER),
			npc(13, FREM_RANGER_ID, 7, 14, -1, -1, -1, WaveLog.TARGET_NONE),
			npc(20, NpcKind.SOLARFLARE_ORB_ID, 4, 4, -1, -1, -1, WaveLog.TARGET_NONE),
			// Off the grid: left out.
			npc(21, MINIMUS_ID, 40, 4, -1, -1, -1, WaveLog.TARGET_NONE)), COUNTERS);

		log.onPlayerHit(13, 16);
		log.onPlayerHit(0, 17);
		log.onNpcHit(11, 21, 12);
		log.onNpcHit(11, 9, 12);
		// The player switched to the fang and struck twice with the same animation; the Manticore showed an orb.
		log.onPlayerAnimation(7552);
		log.onPlayerAnimation(7552);
		// A graphic on a tile of the grid is kept, one off it is not.
		log.onTileGraphic(new GridTile(9, 12), 4001);
		log.onTileGraphic(new GridTile(40, 4), 4002);
		// The Manticore's orb at the player; a special aimed at a tile, seen a tick late; and one with nothing on the grid at
		// either end, which is left out. The damage taken counts up, and only it is listed.
		log.onProjectile(new WaveLog.Projectile(5001, 12, null, WaveLog.TARGET_PLAYER, null, 0, 51));
		log.onProjectile(new WaveLog.Projectile(5002, 11, new GridTile(1, 1), WaveLog.TARGET_NONE, new GridTile(9, 12), -12, 48));
		log.onProjectile(new WaveLog.Projectile(5003, WaveLog.TARGET_NONE, new GridTile(40, 4), WaveLog.TARGET_NONE, null, 0, 30));
		log.record(101, new WaveLog.PlayerTick(new GridTile(7, 14), 86, 75, Arrays.asList("ranged", "rigour"), 7552, 11, FANG), Arrays.asList(
			npc(11, NpcKind.SHAMAN_ID, 13, 19, 7579, 20, 30, WaveLog.TARGET_PLAYER),
			new WaveLog.NpcTick(12, NpcKind.MANTICORE_ID, new GridTile(16, 23), -1, 30, 30, WaveLog.TARGET_PLAYER, new int[] {3001}),
			npc(13, FREM_RANGER_ID, 7, 13, 426, -1, -1, 12),
			npc(20, NpcKind.SOLARFLARE_ORB_ID, 5, 4, -1, -1, -1, WaveLog.TARGET_NONE),
			npc(21, MINIMUS_ID, 20, 4, -1, -1, -1, WaveLog.TARGET_NONE)), COUNTERS_HIT);

		// A tick the plugin missed leaves a gap in `t`; a hit on an NPC that has gone is a stray hit.
		log.onNpcHit(11, 5, 12);
		log.record(103, new WaveLog.PlayerTick(null, 86, 74, Collections.emptyList(), -1, WaveLog.TARGET_NONE, FANG), Arrays.asList(
			npc(12, NpcKind.MANTICORE_ID, 16, 22, -1, 30, 30, WaveLog.TARGET_PLAYER)));
		log.finish(WaveLog.End.COMPLETED);

		assertEquals(3, log.size());
		assertFalse(log.isRecording());
		assertEquals(WaveLog.End.COMPLETED, log.getEnd());
		assertEquals(fixture("wave-log.json"), WaveLogWriter.toJson(log));
	}

	@Test
	public void theLogIsKeptAfterTheWaveAndResetByTheNext()
	{
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.record(101, player(7, 14, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.finish(WaveLog.End.LEFT);

		// Ticks after the wave are not recorded, and their hitsplats do not wait for the next wave.
		log.onPlayerHit(5, 16);
		log.record(105, player(7, 14, 94, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		assertEquals(2, log.size());
		assertEquals(WaveLog.End.LEFT, log.getEnd());
		assertTrue(log.hasLog());

		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		log.record(200, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		assertEquals(Integer.valueOf(5), log.getWave());
		assertEquals(1, log.size());
		assertNull(log.getEnd());
		assertTrue(log.isRecording());
		assertTrue(WaveLogWriter.toJson(log).contains("\"hits\":[]"));
	}

	@Test
	public void theAnimationsStartedSinceTheTickBeforeAreLoggedEachTimeEvenARepeat()
	{
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.onNpcAnimation(11, 10859);
		log.onNpcAnimation(12, 10892);
		log.record(101, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.onNpcAnimation(11, 10859);
		log.record(102, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		String json = WaveLogWriter.toJson(log);
		assertTrue(json, json.contains("{\"t\":0,\"player\":[7,15,99,77,[],-1,null],\"hits\":[],\"npcs\":[],\"orbs\":[]}"));
		assertTrue(json, json.contains("\"orbs\":[],\"anims\":[[11,10859],[12,10892]]}"));
		assertTrue(json, json.contains("{\"t\":2,\"player\":[7,15,99,77,[],-1,null],\"hits\":[],\"npcs\":[],\"orbs\":[],\"anims\":[[11,10859]]}"));
	}

	@Test
	public void theCountersAreListedOnTheFirstTickAndWhenTheyChange()
	{
		// The prayer points drain a point a tick, so that no tick repeats the one before (aWaitIsWrittenOnce) and each has a line.
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), COUNTERS);
		log.record(101, player(7, 15, 99, 76, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), COUNTERS);
		log.record(102, player(7, 15, 99, 75, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), COUNTERS_HIT);
		// Counters that could not be read list nothing and leave the last ones standing.
		log.record(103, player(7, 15, 99, 74, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), null);
		log.record(104, player(7, 15, 99, 73, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), COUNTERS_HIT);
		String[] ticks = WaveLogWriter.toJson(log).split("\n");
		assertTrue(ticks[1], ticks[1].endsWith(",\"counters\":{\"doomStacks\":0,\"currentGlory\":1200,\"waveStartTime\":51000,"
			+ "\"waveDamageTaken\":0,\"lastModifierGlory\":150,\"lastWaveDuration\":120,\"totalDuration\":900}},"));
		assertFalse(ticks[2], ticks[2].contains("counters"));
		assertTrue(ticks[3], ticks[3].endsWith(",\"counters\":{\"waveDamageTaken\":13}},"));
		assertFalse(ticks[4], ticks[4].contains("counters"));
		assertFalse(ticks[5], ticks[5].contains("counters"));

		// A shared log leaves out the wave's start time and the run's total duration, and a tick that changed only those
		// lists no counters.
		String[] shared = WaveLogWriter.toShare(log).split("\n");
		assertTrue(shared[1], shared[1].endsWith(",\"counters\":{\"doomStacks\":0,\"currentGlory\":1200,"
			+ "\"waveDamageTaken\":0,\"lastModifierGlory\":150,\"lastWaveDuration\":120}},"));
		assertTrue(shared[3], shared[3].endsWith(",\"counters\":{\"waveDamageTaken\":13}},"));
		log.record(105, player(7, 15, 99, 72, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(),
			new int[]{0, 1200, 51000, 13, 150, 120, 901});
		String[] later = WaveLogWriter.toShare(log).split("\n");
		assertFalse(later[6], later[6].contains("counters"));
		assertTrue(WaveLogWriter.toJson(log).split("\n")[6].endsWith(",\"counters\":{\"totalDuration\":901}}"));

		// The next wave lists them all on its first tick again.
		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		log.record(200, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), COUNTERS_HIT);
		assertTrue(WaveLogWriter.toJson(log).contains("\"counters\":{\"doomStacks\":0,"));
	}

	@Test
	public void theProjectilesGoInTheNextTickRecordedOnly()
	{
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.onProjectile(new WaveLog.Projectile(5001, WaveLog.TARGET_PLAYER, null, 12, null, 0, 56));
		// The tile of an end with an actor is not kept.
		log.onProjectile(new WaveLog.Projectile(5004, 12, new GridTile(16, 23), 11, new GridTile(13, 19), 10, 40));
		log.record(101, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.record(102, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		String[] ticks = WaveLogWriter.toJson(log).split("\n");
		assertFalse(ticks[1], ticks[1].contains("projectiles"));
		assertTrue(ticks[2], ticks[2].endsWith(",\"projectiles\":[[5001,\"player\",12,0,56],[5004,12,11,10,40]]},"));
		assertFalse(ticks[3], ticks[3].contains("projectiles"));

		// After the wave, those seen on a tick not recorded are dropped with its hitsplats, and do not wait for the next wave.
		log.finish(WaveLog.End.COMPLETED);
		log.onProjectile(new WaveLog.Projectile(5001, WaveLog.TARGET_PLAYER, null, 12, null, 0, 56));
		log.record(105, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		log.record(200, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		assertFalse(WaveLogWriter.toJson(log).contains("projectiles"));
	}

	@Test
	public void thePastWavesAreKeptNewestFirstUpToTheCap()
	{
		startWave4(100);
		int first = log.getId();
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.finish(WaveLog.End.COMPLETED);
		String wave4 = WaveLogWriter.toJson(log);
		assertTrue(log.getPast().isEmpty());

		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		assertEquals(1, log.getPast().size());
		WaveLog.Past past = log.getPast().get(0);
		assertEquals(first, past.getId());
		assertEquals(4, past.getWave());
		assertEquals(1, past.size());
		assertEquals(WaveLog.End.COMPLETED, past.getEnd());
		assertEquals(wave4, past.getJson());
		assertTrue(log.getId() != first);

		// A wave whose end the plugin missed is kept all the same, with no end.
		log.record(200, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.start(6, 300, Collections.emptyMap(), true, "test", null);
		assertEquals(Arrays.asList(5, 4), Arrays.asList(log.getPast().get(0).getWave(), log.getPast().get(1).getWave()));
		assertNull(log.getPast().get(0).getEnd());

		for (int i = 0; i < WaveLog.MAX_PAST + 5; i++)
		{
			log.start(7, 400 + i, Collections.emptyMap(), true, "test", null);
		}
		assertEquals(WaveLog.MAX_PAST, log.getPast().size());

		log.clear();
		assertTrue(log.getPast().isEmpty());
		assertEquals(0, log.getId());
	}

	/** The counters with the glory counter at `glory`, the rest as COUNTERS. */
	private static int[] glory(int glory)
	{
		int[] counters = COUNTERS.clone();
		counters[ColosseumCounter.CURRENT_GLORY.ordinal()] = glory;
		return counters;
	}

	@Test
	public void aCompletedWaveEarnsTheGloryItsCounterRoseByAndOthersNone()
	{
		// Wave 4, completed: the counter read 1,200 on the first tick and 2,180 on the last, so it earned 980.
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), glory(1200));
		assertNull(log.getGlory());
		log.record(101, player(7, 14, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), glory(1200));
		log.record(102, player(7, 13, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), glory(2180));
		// Under way it has earned nothing yet, even with the counter risen.
		assertNull(log.getGlory());
		assertNull(log.current().getGlory());
		log.finish(WaveLog.End.COMPLETED);
		assertEquals(Integer.valueOf(980), log.getGlory());
		assertEquals(Integer.valueOf(980), log.snapshot().getGlory());

		// The next wave starts: wave 4 keeps its glory among the past ones. Wave 5 the player left, which earns none.
		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		assertEquals(Integer.valueOf(980), log.getPast().get(0).getGlory());
		log.record(200, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), glory(2180));
		log.record(201, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList(), glory(2500));
		log.finish(WaveLog.End.LEFT);
		assertNull(log.getGlory());

		// A wave whose counters could not be read earns none it is known to have.
		log.start(6, 300, Collections.emptyMap(), true, "test", null);
		log.record(300, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.finish(WaveLog.End.COMPLETED);
		assertNull(log.getGlory());
		assertEquals(Arrays.asList(Integer.valueOf(980), null, null), Arrays.asList(log.getRun().get(0).getGlory(), log.getRun().get(1).getGlory(),
			log.getRun().get(2).getGlory()));
	}

	@Test
	public void wavesSavedInAnEarlierSessionComeBackBehindThisSessionsUnderNewIds()
	{
		// The last session's run, waves 1 and 2, as WaveStore reads them back: newest first, each under id 0.
		WaveLog.Past one = new WaveLog.Past(0, 1, 60, WaveLog.End.COMPLETED, false, () -> "{\"wave\":1}", START_STATE, null, 739);
		WaveLog.Past two = new WaveLog.Past(0, 2, 140, WaveLog.End.LEFT, false, () -> "{\"wave\":2}", null, null, null);
		// With no wave this session, Past waves lists them all the same, as one run.
		log.restore(Arrays.asList(two, one));
		assertFalse(log.hasLog());
		List<List<WaveLog.Past>> runs = log.getRuns();
		assertEquals(1, runs.size());
		assertEquals(Arrays.asList(1, 2), Arrays.asList(runs.get(0).get(0).getWave(), runs.get(0).get(1).getWave()));
		assertEquals("{\"wave\":1}", runs.get(0).get(0).getJson());
		assertEquals(Integer.valueOf(739), runs.get(0).get(0).getGlory());
		assertEquals(START_STATE, runs.get(0).get(0).getStartState());
		int firstId = runs.get(0).get(0).getId();
		int secondId = runs.get(0).get(1).getId();
		assertTrue(firstId != 0 && secondId != 0 && firstId != secondId);

		// This session's wave 1 is a run of its own, before them, and its id is none of theirs.
		startWave4(100);
		log.start(1, 100, Collections.emptyMap(), true, "test", null);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		runs = log.getRuns();
		assertEquals(Arrays.asList(1, 4, 2), Arrays.asList(runs.get(0).get(0).getWave(), runs.get(1).get(0).getWave(), runs.get(2).get(1).getWave()));
		assertTrue(log.getId() != firstId && log.getId() != secondId);

		// They count among the past waves kept, so that no more than MAX_PAST are.
		List<WaveLog.Past> many = Collections.nCopies(WaveLog.MAX_PAST + 3, one);
		log.restore(many);
		assertEquals(WaveLog.MAX_PAST, log.getPast().size());
	}

	@Test
	public void aSnapshotWritesTheLogAsItWasWhileTheLogGoesOn()
	{
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		String then = WaveLogWriter.toJson(log);
		WaveLog snapshot = log.snapshot();
		log.record(101, player(7, 14, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.finish(WaveLog.End.COMPLETED);
		assertEquals(then, WaveLogWriter.toJson(snapshot));
		assertEquals(1, snapshot.size());
		assertFalse(snapshot.isRecording());
		// The wave under way in getRuns is as it was when asked for, not as it is when its JSON is written.
		WaveLog.Past now = log.getRun().get(0);
		String written = WaveLogWriter.toJson(log);
		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		assertEquals(written, now.getJson());
	}

	@Test
	public void aPastWavesJsonIsWrittenOnceWhenFirstAskedFor()
	{
		int[] made = {0};
		Supplier<String> once = WaveLog.once(() -> "log" + (++made[0]));
		assertEquals(0, made[0]);
		assertEquals("log1", once.get());
		assertEquals("log1", once.get());
		assertEquals(1, made[0]);
	}

	@Test
	public void theGearIsListedOnTheFirstTickAndWhenItChanges()
	{
		startWave4(100);
		log.record(100, wearing(BOW, 7, 15), Collections.emptyList());
		log.record(101, wearing(BOW, 7, 14), Collections.emptyList());
		log.record(102, wearing(FANG, 7, 13), Collections.emptyList());
		// A tick without the gear, as with "Include gear and stats" off, lists none and leaves the last one standing.
		log.record(103, player(7, 13, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.record(104, wearing(FANG, 7, 13), Collections.emptyList());
		String[] ticks = WaveLogWriter.toJson(log).split("\n");
		assertTrue(ticks[1], ticks[1].endsWith(",\"worn\":[1001,1002,-1,1004,1005,-1,1007,-1,-1,-1,1011]},"));
		assertFalse(ticks[2], ticks[2].contains("worn"));
		assertTrue(ticks[3], ticks[3].endsWith(",\"worn\":[1001,1002,-1,2004,1005,-1,1007,-1,-1,-1,1011]},"));
		assertFalse(ticks[4], ticks[4].contains("worn"));
		assertFalse(ticks[5], ticks[5].contains("worn"));

		// The next wave lists the gear on its first tick again.
		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		log.record(200, wearing(FANG, 7, 15), Collections.emptyList());
		assertTrue(WaveLogWriter.toJson(log).contains("\"worn\":[1001,1002,-1,2004"));
	}

	@Test
	public void theWaveStartIsInTheLogOrNull()
	{
		startWave4(100);
		assertTrue(WaveLogWriter.toJson(log).contains(",\"end\":null,\"start\":" + START_STATE + ",\"ticks\":["));
		log.start(5, 200, Collections.emptyMap(), true, "test", null);
		assertTrue(WaveLogWriter.toJson(log).contains(",\"end\":null,\"start\":null,\"ticks\":["));
	}

	@Test
	public void aTickIsRecordedOnce()
	{
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		log.record(100, player(8, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		assertEquals(1, log.size());
	}

	@Test
	public void theLogIsCutAtItsCap()
	{
		startWave4(0);
		for (int tick = 0; tick < WaveLog.MAX_TICKS + 10; tick++)
		{
			log.record(tick, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Collections.emptyList());
		}
		assertEquals(WaveLog.MAX_TICKS, log.size());
		assertTrue(log.isCut());
		assertTrue(log.isRecording());
		log.finish(WaveLog.End.COMPLETED);
		String json = WaveLogWriter.toJson(log);
		assertTrue(json.contains("\"cut\":true,\"end\":\"completed\""));
		// Every tick is the same, so the log is its first tick and the count of those that repeat it.
		assertTrue(json.endsWith("\"ticks\":[\n{\"t\":0,\"player\":[7,15,99,77,[],-1,null],\"hits\":[],\"npcs\":[],\"orbs\":[],\"repeats\":"
			+ (WaveLog.MAX_TICKS - 1) + "}\n]}"));
	}

	@Test
	public void aWaitIsWrittenOnce()
	{
		// The player waits on 7, 14 out of the shaman's sight from the wave's tick 1 to 5 and is hit on 3; then steps to
		// 7, 13 and waits again; a tick the plugin missed (9) ends the second wait, though 10 shows the same.
		startWave4(100);
		List<WaveLog.NpcTick> shaman = Collections.singletonList(npc(11, NpcKind.SHAMAN_ID, 13, 20, -1, -1, -1, WaveLog.TARGET_PLAYER));
		log.record(100, player(7, 15, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), shaman);
		for (int tick = 101; tick <= 108; tick++)
		{
			if (tick == 103)
			{
				log.onPlayerHit(0, 12);
			}
			log.record(tick, player(7, tick < 106 ? 14 : 13, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), shaman);
		}
		log.record(110, player(7, 13, 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE), shaman);
		String[] ticks = WaveLogWriter.toJson(log).split("\n");
		String rest = ",\"hits\":[],\"npcs\":[[11,12811,13,20,-1,-1,-1,\"player\",[]]],\"orbs\":[]";
		assertArrayEquals(new String[] {
			"{\"t\":0,\"player\":[7,15,99,77,[],-1,null]" + rest + "},",
			"{\"t\":1,\"player\":[7,14,99,77,[],-1,null]" + rest + ",\"repeats\":1},",
			"{\"t\":3,\"player\":[7,14,99,77,[],-1,null],\"hits\":[[0,12]]" + rest.substring(",\"hits\":[]".length()) + "},",
			"{\"t\":4,\"player\":[7,14,99,77,[],-1,null]" + rest + ",\"repeats\":1},",
			"{\"t\":6,\"player\":[7,13,99,77,[],-1,null]" + rest + ",\"repeats\":2},",
			"{\"t\":10,\"player\":[7,13,99,77,[],-1,null]" + rest + "}",
		}, Arrays.copyOfRange(ticks, 1, ticks.length - 1));
		// The log itself keeps every tick.
		assertEquals(10, log.size());
	}

	@Test
	public void anEmptyLog()
	{
		assertFalse(log.hasLog());
		assertEquals("{\"v\":1,\"kind\":\"solSurvivorWaveLog\",\"plugin\":\"\",\"wave\":null,\"modifiers\":{},\"modifiersComplete\":false,"
			+ "\"maxTicks\":1500,\"cut\":false,\"end\":null,\"start\":null,\"ticks\":[]}", WaveLogWriter.toJson(log));

		startWave4(100);
		log.clear();
		assertFalse(log.hasLog());
		assertFalse(log.isRecording());
		assertEquals(0, log.size());
	}

	@Test
	public void prayerNames()
	{
		assertEquals("magic", WaveLog.prayerName("PROTECT_FROM_MAGIC"));
		assertEquals("ranged", WaveLog.prayerName("PROTECT_FROM_MISSILES"));
		assertEquals("melee", WaveLog.prayerName("PROTECT_FROM_MELEE"));
		assertEquals("piety", WaveLog.prayerName("PIETY"));
		assertEquals("rp_ancient_strength", WaveLog.prayerName("RP_ANCIENT_STRENGTH"));
	}
}
