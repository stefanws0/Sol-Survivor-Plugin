package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.junit.Test;

/**
 * The wave logs the player shares with "Share wave logs": a random id for each run and none that outlives it, each log
 * once, gzipped, and its gear without the levels or the prayer unlocks, which the site refuses in a shared log.
 */
public class WaveShareTest
{
	private static final String START = "{\"v\":1,\"setup\":{\"wave\":4},\"equipment\":{\"stats\":{\"ranged\":[112,99]}}}";
	private static final String SHARED_START = "{\"v\":1,\"setup\":{\"wave\":4},\"equipment\":{\"stats\":{}}}";

	@Test
	public void aRunKeepsItsIdWaveAfterWaveAndANewRunGetsANewOne()
	{
		WaveShare share = new WaveShare(new Random(7));
		assertNull(share.getRun());
		String run = share.onWaveStart(1);
		assertTrue(run, run.matches("[a-f0-9]{32}"));
		assertEquals(run, share.onWaveStart(2));
		assertEquals(run, share.getRun());
		// A wave that does not follow the last one is another run, as is wave 1 again.
		String next = share.onWaveStart(5);
		assertNotEquals(run, next);
		assertNotEquals(next, share.onWaveStart(1));
	}

	@Test
	public void leavingTheArenaForgetsTheRun()
	{
		WaveShare share = new WaveShare(new Random(7));
		String run = share.onWaveStart(3);
		share.onLeave();
		assertNull(share.getRun());
		// A plugin started mid-run, or back after leaving, starts a run of its own at the next wave.
		String next = share.onWaveStart(4);
		assertTrue(next.matches("[a-f0-9]{32}"));
		assertNotEquals(run, next);
	}

	@Test
	public void eachLogIsClaimedOnceAndNoLogNever()
	{
		WaveShare share = new WaveShare(new Random(7));
		assertFalse(share.claim(0));
		assertTrue(share.claim(1));
		assertFalse(share.claim(1));
		assertTrue(share.claim(2));
	}

	@Test
	public void theBodyIsTheLogGzipped() throws IOException
	{
		String json = "{\"v\":1,\"kind\":\"solSurvivorWaveLog\",\"ticks\":[]}";
		byte[] body = WaveShare.gzip(json);
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(body)))
		{
			assertEquals(json, new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
	}

	@Test
	public void itPostsToTheSitesOwnEndpoint()
	{
		assertEquals("https://solsurvivor.app/api/wave-logs", WaveShare.endpoint("https://solsurvivor.app/"));
	}

	@Test
	public void theSharedLogStartsWithTheGearWithoutTheLevels()
	{
		WaveLog log = new WaveLog();
		log.start(4, 100, new EnumMap<>(Modifier.class), true, "0.7.0", START, SHARED_START);
		log.record(100, new WaveLog.PlayerTick(new GridTile(7, 15), 99, 77, Collections.emptyList(), -1, WaveLog.TARGET_NONE),
			Collections.emptyList());
		String copied = WaveLogWriter.toJson(log);
		String shared = WaveLogWriter.toShare(log);
		assertTrue(copied, copied.contains("\"start\":" + START));
		assertTrue(shared, shared.contains("\"start\":" + SHARED_START));
		// Everything else is the log as Copy wave log copies it.
		assertEquals(copied.replace(START, SHARED_START), shared);
		// A start given once is shared as it is.
		log.start(5, 200, new EnumMap<>(Modifier.class), true, "0.7.0", SHARED_START);
		assertTrue(WaveLogWriter.toShare(log).contains("\"start\":" + SHARED_START));
	}

	@Test
	public void gearWithoutLevelsKeepsEverythingButTheLevelsAndTheUnlocks()
	{
		Equipment gear = new Equipment()
			.item("weapon", new Equipment.Item(20997, "Twisted bow", 1))
			.switchItem("weapon", new Equipment.Item(22325, "Scythe of vitur", 1), 5)
			.bonuses(20997, new int[]{0, 0, 0, 0, 70, 0, 20, 0, 1})
			.defences(20997, new int[]{0, 0, 0, 0, 0})
			.weapon(5, 3, 1)
			.prayer(9, 77)
			.stat("ranged", 112, 99)
			.unlocks(1, 0, 1, 0, 8, 0);
		Equipment shared = gear.withoutLevels();
		assertTrue(shared.getStats().isEmpty());
		assertNull(shared.getUnlocks());
		assertEquals(gear.getItems().keySet(), shared.getItems().keySet());
		assertEquals(1, shared.getSwitches().size());
		assertArrayEquals(gear.getBonuses().get(20997), shared.getBonuses().get(20997));
		assertArrayEquals(gear.getDefences().get(20997), shared.getDefences().get(20997));
		assertEquals(5, shared.getWeaponSpeed());
		assertEquals(9, shared.getPrayerBonus());
		assertEquals(77, shared.getPrayerPoints());
		// The gear it was made from keeps its levels.
		assertEquals(1, gear.getStats().size());
	}
}
