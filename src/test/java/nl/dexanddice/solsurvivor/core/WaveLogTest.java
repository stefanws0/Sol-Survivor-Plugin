package nl.dexanddice.solsurvivor.core;

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
import org.junit.Before;
import org.junit.Test;

/**
 * The wave log, fed mocked game data as the RuneLite layer passes it on: a wave's start from the tracker, the hitsplats
 * as they come and a snapshot of the player and the NPCs each tick. The JSON is compared whole with
 * src/test/resources/fixtures/wave-log.json. The animation and hitsplat type ids here are made up: the log passes on
 * whatever the game gives.
 */
public class WaveLogTest
{
	private static final int FREM_RANGER_ID = 12814;
	/** An NPC the plugin has no kind for, logged by its raw id all the same: Minimus, here standing on the grid. */
	private static final int MINIMUS_ID = 12808;

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
		log.start(tracker.getWave(), gameTick, tracker.modifiers(m -> varbits.getOrDefault(m, 0)), tracker.isModifiersComplete(), "test");
	}

	@Test
	public void aWaveLog() throws Exception
	{
		// A hitsplat that came on the wave's first tick, before the tracker saw its message, is kept for that tick.
		log.onNpcHit(12, 0, 12);
		startWave4(100);
		log.record(100, player(7, 15, 99, 77, Arrays.asList("magic", "piety"), -1, WaveLog.TARGET_NONE), Arrays.asList(
			npc(11, NpcKind.SHAMAN_ID, 13, 20, -1, -1, -1, WaveLog.TARGET_PLAYER),
			npc(12, NpcKind.MANTICORE_ID, 16, 24, 7590, 30, 30, WaveLog.TARGET_PLAYER),
			npc(13, FREM_RANGER_ID, 7, 14, -1, -1, -1, WaveLog.TARGET_NONE),
			npc(20, NpcKind.SOLARFLARE_ORB_ID, 4, 4, -1, -1, -1, WaveLog.TARGET_NONE),
			// Off the grid: left out.
			npc(21, MINIMUS_ID, 40, 4, -1, -1, -1, WaveLog.TARGET_NONE)));

		log.onPlayerHit(13, 16);
		log.onPlayerHit(0, 17);
		log.onNpcHit(11, 21, 12);
		log.onNpcHit(11, 9, 12);
		log.record(101, player(7, 14, 86, 75, Arrays.asList("ranged", "rigour"), 7552, 11), Arrays.asList(
			npc(11, NpcKind.SHAMAN_ID, 13, 19, 7579, 20, 30, WaveLog.TARGET_PLAYER),
			npc(12, NpcKind.MANTICORE_ID, 16, 23, -1, 30, 30, WaveLog.TARGET_PLAYER),
			npc(13, FREM_RANGER_ID, 7, 13, 426, -1, -1, 12),
			npc(20, NpcKind.SOLARFLARE_ORB_ID, 5, 4, -1, -1, -1, WaveLog.TARGET_NONE),
			npc(21, MINIMUS_ID, 20, 4, -1, -1, -1, WaveLog.TARGET_NONE)));

		// A tick the plugin missed leaves a gap in `t`; a hit on an NPC that has gone is a stray hit.
		log.onNpcHit(11, 5, 12);
		log.record(103, new WaveLog.PlayerTick(null, 86, 74, Collections.emptyList(), -1, WaveLog.TARGET_NONE), Arrays.asList(
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

		log.start(5, 200, Collections.emptyMap(), true, "test");
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
		assertTrue(json.endsWith("{\"t\":" + (WaveLog.MAX_TICKS - 1) + ",\"player\":[7,15,99,77,[],-1,null],\"hits\":[],\"npcs\":[],\"orbs\":[]}\n]}"));
	}

	@Test
	public void anEmptyLog()
	{
		assertFalse(log.hasLog());
		assertEquals("{\"v\":1,\"kind\":\"solSurvivorWaveLog\",\"plugin\":\"\",\"wave\":null,\"modifiers\":{},\"modifiersComplete\":false,"
			+ "\"maxTicks\":1500,\"cut\":false,\"end\":null,\"ticks\":[]}", WaveLogWriter.toJson(log));

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
