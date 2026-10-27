package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * A wave saved by "Keep runs between sessions" and read back, as the RuneLite layer writes and reads its files, and the
 * names it gives them, which sort as the waves were saved.
 */
public class WaveStoreTest
{
	private static final String START_STATE = "{\"v\":1,\"setup\":{\"npcs\":[[13,20,\"2\"]],\"player\":[7,15],\"waveStart\":true,\"wave\":4}}";

	private static byte[] saved(WaveLog.Past wave) throws IOException
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		WaveStore.write(wave, out);
		return out.toByteArray();
	}

	@Test
	public void aWaveComesBackAsItWasSaved() throws IOException
	{
		// Wave 4 as the plugin saves it as it ends: its log, written as it is saved, and its state on the first tick and
		// when the reinforcements came, the glory it earned, and a name in the log that is not ASCII.
		WaveLog log = new WaveLog();
		log.start(4, 100, Collections.emptyMap(), true, "test", START_STATE);
		log.record(100, new WaveLog.PlayerTick(new GridTile(7, 15), 99, 77, Arrays.asList("magic"), -1, WaveLog.TARGET_NONE),
			Collections.emptyList(), new int[] {0, 1200, 51000, 0, 150, 120, 900});
		log.reinforcementsCame("{\"v\":1,\"setup\":{\"wave\":4,\"note\":\"Fortis été\"}}");
		log.record(101, new WaveLog.PlayerTick(new GridTile(7, 14), 99, 77, Arrays.asList("magic"), -1, WaveLog.TARGET_NONE),
			Collections.emptyList(), new int[] {0, 2180, 51000, 0, 150, 120, 900});
		log.finish(WaveLog.End.COMPLETED);
		WaveLog.Past wave = log.current();

		WaveLog.Past back = WaveStore.read(new ByteArrayInputStream(saved(wave)));
		assertEquals(0, back.getId());
		assertEquals(4, back.getWave());
		assertEquals(2, back.size());
		assertEquals(WaveLog.End.COMPLETED, back.getEnd());
		assertFalse(back.isRecording());
		assertEquals(Integer.valueOf(980), back.getGlory());
		assertEquals(START_STATE, back.getStartState());
		assertEquals(wave.getReinforcementState(), back.getReinforcementState());
		assertEquals(WaveLogWriter.toJson(log), back.getJson());
	}

	@Test
	public void aWaveThatZeroNineSavedStillLoads() throws IOException
	{
		// Wave 4 as 0.9.0 saved it, kept as it was written: 1.0 promises its players' saved runs come back (the 1.0 plan
		// of 2026-10-26). A new format gets a new VERSION, and reads this one too.
		WaveLog.Past back;
		try (InputStream in = WaveStoreTest.class.getResourceAsStream("/fixtures/saved-wave-0.9.0.wave.gz"))
		{
			back = WaveStore.read(in);
		}
		assertEquals(4, back.getWave());
		assertEquals(2, back.size());
		assertEquals(WaveLog.End.COMPLETED, back.getEnd());
		assertEquals(Integer.valueOf(980), back.getGlory());
		assertEquals(START_STATE, back.getStartState());
		assertEquals("{\"v\":1,\"setup\":{\"wave\":4}}", back.getReinforcementState());
		assertTrue(back.getJson().startsWith("{\"v\":1,\"kind\":\"solSurvivorWaveLog\",\"plugin\":\"0.9.0\",\"wave\":4,"));
	}

	@Test
	public void aWaveWithoutItsStatesEndOrGloryComesBackWithoutThem() throws IOException
	{
		WaveLog.Past wave = new WaveLog.Past(7, 2, 140, null, false, () -> "{}", null, null, null);
		WaveLog.Past back = WaveStore.read(new ByteArrayInputStream(saved(wave)));
		assertNull(back.getEnd());
		assertNull(back.getGlory());
		assertNull(back.getStartState());
		assertNull(back.getReinforcementState());
		assertEquals("{}", back.getJson());

		WaveLog.Past left = new WaveLog.Past(7, 3, 90, WaveLog.End.LEFT, false, () -> "{}", null, null, null);
		assertEquals(WaveLog.End.LEFT, WaveStore.read(new ByteArrayInputStream(saved(left))).getEnd());
	}

	@Test
	public void aFileThatIsNotASavedWaveOrIsCutShortIsNotRead() throws IOException
	{
		byte[] bytes = saved(new WaveLog.Past(7, 2, 140, WaveLog.End.COMPLETED, false, () -> "{\"wave\":2}", START_STATE, null, 1114));
		for (byte[] bad : Arrays.asList(Arrays.copyOf(bytes, bytes.length / 2), "not a wave".getBytes(), WaveShare.gzip("{\"v\":1}")))
		{
			try
			{
				WaveStore.read(new ByteArrayInputStream(bad));
				fail("read a file that is not a whole saved wave");
			}
			catch (IOException expected)
			{
				// As the RuneLite layer passes such a file over.
			}
		}
	}

	@Test
	public void theNamesSortAsTheWavesWereSavedAndThoseBeyondTheNewestAreToGo()
	{
		String first = WaveStore.fileName(1_761_480_000_000L, 1);
		String second = WaveStore.fileName(1_761_480_090_000L, 2);
		String third = WaveStore.fileName(1_761_480_250_000L, 10);
		assertEquals("1761480000000-wave01.wave.gz", first);
		assertTrue(third.endsWith("-wave10.wave.gz"));
		List<String> names = new ArrayList<>(Arrays.asList(second, "notes.txt", third, first));
		assertEquals(Arrays.asList(third, second, first), WaveStore.newestFirst(names));
		assertEquals(Collections.singletonList(first), WaveStore.beyond(names, 2));
		assertTrue(WaveStore.beyond(names, 3).isEmpty());
		assertEquals(WaveLog.MAX_PAST + 1, WaveStore.MAX_SAVED);
	}
}
