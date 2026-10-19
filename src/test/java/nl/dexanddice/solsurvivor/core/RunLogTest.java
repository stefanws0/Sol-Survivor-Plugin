package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * The run log, which "Copy whole run" copies: which of the waves logged make the newest run (WaveLog.getRun), and its
 * JSON (RunLogWriter), compared whole with src/test/resources/fixtures/run-log.json. The waves are started and recorded
 * as the RuneLite layer does it, with the plugin's version given as "test"; the NPC, animation and hitsplat type ids are
 * made up, as in WaveLogTest.
 */
public class RunLogTest
{
	/** The state link's payload on wave 1's first tick, as the RuneLite layer passes it on: here a short made-up one. */
	private static final String START_STATE = "{\"v\":1,\"setup\":{\"npcs\":[[13,20,\"2\"]],\"player\":[7,15],\"waveStart\":true,\"wave\":1}}";

	private WaveLog log;

	@Before
	public void setUp()
	{
		log = new WaveLog();
	}

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = RunLogTest.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
			{
				throw new IOException("no fixture " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	private static WaveLog.PlayerTick player(int x, int y, int hp)
	{
		return new WaveLog.PlayerTick(new GridTile(x, y), hp, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE);
	}

	private static WaveLog.NpcTick shaman(int index, int x, int y, int animation)
	{
		return new WaveLog.NpcTick(index, NpcKind.SHAMAN_ID, new GridTile(x, y), animation, -1, -1, WaveLog.TARGET_PLAYER);
	}

	/** Wave `wave` starts on game tick `gameTick` with no modifiers, and `ticks` ticks of it are recorded. */
	private void wave(int wave, int gameTick, int ticks)
	{
		log.start(wave, gameTick, Collections.emptyMap(), true, "test", null);
		for (int t = 0; t < ticks; t++)
		{
			log.record(gameTick + t, player(7, 15, 99), Collections.emptyList());
		}
	}

	/** The waves of the newest run, in the order the run gives them. */
	private List<Integer> runWaves()
	{
		List<Integer> out = new ArrayList<>();
		for (WaveLog.Past p : log.getRun())
		{
			out.add(p.getWave());
		}
		return out;
	}

	@Test
	public void aRunLog() throws Exception
	{
		// An earlier run, which the player left in wave 2.
		wave(1, 100, 2);
		log.finish(WaveLog.End.COMPLETED);
		wave(2, 200, 1);
		log.finish(WaveLog.End.LEFT);

		// The newest run: wave 1, completed; wave 2, whose end the plugin missed, with Reentry I taken; and wave 3 under way.
		Map<Modifier, Integer> reentry = new EnumMap<>(Modifier.class);
		reentry.put(Modifier.REENTRY, 1);
		log.start(1, 1000, Collections.emptyMap(), true, "test", START_STATE);
		log.record(1000, player(7, 15, 99), Collections.singletonList(shaman(11, 13, 20, -1)));
		log.onNpcHit(11, 14, 12);
		log.onNpcAnimation(11, 10859);
		log.record(1001, player(7, 14, 99), Collections.singletonList(shaman(11, 13, 19, 10859)));
		log.finish(WaveLog.End.COMPLETED);
		log.start(2, 1100, reentry, true, "test", null);
		log.record(1100, player(7, 15, 99), Arrays.asList(shaman(14, 13, 20, -1), shaman(15, 15, 20, -1)));
		log.start(3, 1200, reentry, true, "test", null);
		log.onPlayerHit(9, 16);
		log.record(1200, player(7, 15, 90), Collections.singletonList(shaman(16, 13, 20, -1)));

		assertEquals(Arrays.asList(1, 2, 3), runWaves());
		assertEquals(fixture("run-log.json"), RunLogWriter.toJson(log.getRun(), "test"));
	}

	@Test
	public void eachWaveIsAsCopyWaveLogCopiesIt()
	{
		wave(4, 100, 2);
		log.finish(WaveLog.End.COMPLETED);
		wave(5, 200, 3);
		log.finish(WaveLog.End.COMPLETED);
		wave(6, 300, 1);

		// The past waves as they were kept, and the wave under way as WaveLogWriter writes it now.
		String wave4 = log.getPast().get(1).getJson();
		String wave5 = log.getPast().get(0).getJson();
		String wave6 = WaveLogWriter.toJson(log);
		List<WaveLog.Past> run = log.getRun();
		assertEquals(Arrays.asList(wave4, wave5, wave6), Arrays.asList(run.get(0).getJson(), run.get(1).getJson(), run.get(2).getJson()));
		assertEquals(Arrays.asList(2, 3, 1), Arrays.asList(run.get(0).size(), run.get(1).size(), run.get(2).size()));
		assertEquals(log.getId(), run.get(2).getId());
		assertEquals("{\"v\":1,\"kind\":\"solSurvivorRun\",\"plugin\":\"test\",\"waves\":[\n" + wave4 + ",\n" + wave5 + ",\n" + wave6 + "\n]}",
			RunLogWriter.toJson(run, "test"));

		// The newest is written when the run is asked for, so that it holds the ticks recorded since.
		log.record(301, player(7, 14, 99), Collections.emptyList());
		assertEquals(2, log.getRun().get(2).size());
		assertEquals(WaveLogWriter.toJson(log), log.getRun().get(2).getJson());
	}

	@Test
	public void theRunStartsAtWave1AndNeverReachesIntoTheRunBefore()
	{
		wave(1, 100, 1);
		wave(2, 200, 1);
		wave(3, 300, 1);
		log.finish(WaveLog.End.LEFT);
		wave(1, 1000, 1);
		assertEquals(Collections.singletonList(1), runWaves());
		wave(2, 1100, 1);
		assertEquals(Arrays.asList(1, 2), runWaves());
	}

	@Test
	public void aRunThePluginJoinedLateStartsAtTheFirstWaveItLogged()
	{
		wave(4, 100, 1);
		wave(5, 200, 1);
		wave(6, 300, 1);
		log.finish(WaveLog.End.COMPLETED);
		assertEquals(Arrays.asList(4, 5, 6), runWaves());
	}

	@Test
	public void aWaveMissingOrLoggedTwicePartsTheRun()
	{
		// Wave 4 was not logged: the run is the newer part.
		wave(2, 100, 1);
		wave(3, 200, 1);
		wave(5, 300, 1);
		wave(6, 400, 1);
		assertEquals(Arrays.asList(5, 6), runWaves());

		log.clear();
		wave(3, 100, 1);
		wave(4, 200, 1);
		wave(4, 300, 1);
		assertEquals(Collections.singletonList(4), runWaves());
	}

	@Test
	public void howAWaveEndedDoesNotPartTheRun()
	{
		wave(1, 100, 1);
		log.finish(WaveLog.End.COMPLETED);
		wave(2, 200, 1);
		log.finish(WaveLog.End.LEFT);
		wave(3, 300, 1);
		assertEquals(Arrays.asList(1, 2, 3), runWaves());
	}

	@Test
	public void aWholeRunIsAlwaysKept()
	{
		// Three runs of all 12 waves, Sol Heredit's included: the logs kept are more than a run's waves, so that the newest
		// run is whole at each wave even once the oldest logs are dropped.
		int tick = 0;
		for (int run = 0; run < 3; run++)
		{
			for (int w = 1; w <= ColosseumTracker.LAST_WAVE + 1; w++)
			{
				tick += 100;
				wave(w, tick, 1);
				log.finish(WaveLog.End.COMPLETED);
				List<Integer> expected = new ArrayList<>();
				for (int i = 1; i <= w; i++)
				{
					expected.add(i);
				}
				assertEquals(expected, runWaves());
			}
		}
		assertEquals(WaveLog.MAX_PAST, log.getPast().size());
	}

	@Test
	public void noLogNoRun()
	{
		assertTrue(log.getRun().isEmpty());
		assertEquals("{\"v\":1,\"kind\":\"solSurvivorRun\",\"plugin\":\"test\",\"waves\":[]}", RunLogWriter.toJson(log.getRun(), "test"));

		wave(1, 100, 1);
		wave(2, 200, 1);
		log.clear();
		assertTrue(log.getRun().isEmpty());
	}
}
