package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The pick link, from the tracker as the selector's script and varbits feed it. The JSON is compared whole with
 * src/test/resources/fixtures/pick.json, which test/link/pluginFixtures.test.ts reads as the site does.
 */
public class PickPayloadTest
{
	private static String fixture(String name) throws IOException
	{
		try (InputStream in = PickPayloadTest.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
			{
				throw new IOException("no fixture " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	@Test
	public void theOffersAfterWave3()
	{
		ColosseumTracker tracker = new ColosseumTracker();
		Map<Modifier, Integer> varbits = new EnumMap<>(Modifier.class);
		varbits.put(Modifier.RELENTLESS, 2);
		varbits.put(Modifier.BEES, 1);
		tracker.onGameMessage("Wave 3 completed!", 500);
		// Relentless at its next tier, Totemic, which has none, and an id this plugin does not know.
		tracker.onModifierOptions(Modifier.RELENTLESS.getId(), Modifier.TOTEMIC.getId(), 99,
			(1 << Modifier.RELENTLESS.getId()) | (1 << Modifier.BEES.getId()) | (1 << Modifier.QUARTET.getId())
				| (1 << Modifier.DYNAMIC_DUO.getId()));
		assertEquals(3, tracker.getOptions().size());

		String json = PickPayload.toJson(tracker.getWave(), tracker.modifiers(m -> varbits.getOrDefault(m, 0)), tracker.getOptions());
		try
		{
			assertEquals(fixture("pick.json"), json);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}

		String link = PickPayload.link("https://example.org/sol#s=old", tracker.getWave(), tracker.modifiers(m -> varbits.getOrDefault(m, 0)),
			tracker.getOptions());
		assertTrue(link, link.startsWith("https://example.org/sol/#pick="));
		String payload = link.substring(link.indexOf("#pick=") + "#pick=".length());
		assertEquals(json, new String(Base64.getUrlDecoder().decode(payload), StandardCharsets.US_ASCII));

		// Once one is picked, the selector closes and there is nothing to open.
		tracker.onModifierChosen(1);
		assertTrue(tracker.getOptions().isEmpty());
	}

	@Test
	public void beforeAnyWaveTheWaveIsNull()
	{
		ColosseumTracker tracker = new ColosseumTracker();
		tracker.onModifierOptions(Modifier.DOOM.getId(), Modifier.FRAILTY.getId(), Modifier.MYOPIA.getId(), 0);
		assertEquals("{\"v\":1,\"kind\":\"solSurvivorPick\",\"wave\":null,\"modifiers\":{},\"offers\":[[\"doom\",1],[\"frailty\",1],[\"myopia\",1]]}",
			PickPayload.toJson(tracker.getWave(), tracker.modifiers(m -> 0), tracker.getOptions()));
	}

	/**
	 * A run's waves finished go into the pick link: wave 1 clean in 101 ticks, then Blasphemy I taken and wave 2 hit once
	 * in 121. The game's glory counter is taken as it last read before the next wave started: 600 and 1,660, what the
	 * site's rules make of the two waves, as a made-up check. Its wave damage counter, 7 after wave 2's hit, is kept at
	 * the most it read, though it reads 0 again before wave 3.
	 */
	@Test
	public void theRunSoFarAfterWave2()
	{
		ColosseumTracker tracker = new ColosseumTracker();
		Map<Modifier, Integer> varbits = new EnumMap<>(Modifier.class);
		ModifierLevels levels = m -> varbits.getOrDefault(m, 0);
		tracker.onGameMessage("Wave: 1", 100);
		tracker.onRunTick(levels, 0, 0);
		tracker.onGameMessage("Wave 1 completed!", 200);
		// The counter may move a tick after the message: the run keeps its last value before the next wave.
		tracker.onRunTick(levels, 0, 0);
		tracker.onRunTick(levels, 600, 0);
		tracker.onModifierOptions(Modifier.BLASPHEMY.getId(), Modifier.DOOM.getId(), Modifier.FRAILTY.getId(), 0);
		tracker.onModifierChosen(1);
		varbits.put(Modifier.BLASPHEMY, 1);
		tracker.onRunTick(levels, 600, 0);

		tracker.onGameMessage("Wave: 2", 300);
		tracker.onRunTick(levels, 600, 0);
		tracker.onPlayerHit(7, 16);
		tracker.onRunTick(levels, 600, 7);
		tracker.onGameMessage("Wave 2 completed!", 420);
		tracker.onRunTick(levels, 1660, 7);
		tracker.onRunTick(levels, 1660, 0);
		tracker.onModifierOptions(Modifier.DOOM.getId(), Modifier.BLASPHEMY.getId(), Modifier.FRAILTY.getId(), 1 << Modifier.BLASPHEMY.getId());

		assertEquals(2, tracker.getRun().size());
		String json = PickPayload.toJson(tracker.getWave(), tracker.modifiers(levels), tracker.getOptions(), tracker.getRun());
		try
		{
			assertEquals(fixture("pick-run.json"), json);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}
	}

	/**
	 * The reward chest after wave 4 rides in the pick link as the game holds it: wave 5's reward, and the chest so far,
	 * waves 1 and 2's splinters stacked as the game stacks them. The JSON is compared whole with
	 * src/test/resources/fixtures/pick-chest.json, which the site's tests read too.
	 */
	@Test
	public void theChestAfterWave4()
	{
		ColosseumTracker tracker = new ColosseumTracker();
		tracker.onGameMessage("Wave 4 completed!", 500);
		tracker.onModifierOptions(Modifier.DOOM.getId(), Modifier.FRAILTY.getId(), Modifier.MYOPIA.getId(), 0);
		List<ChestItem> next = Arrays.asList(new ChestItem("Dragon platelegs", 1));
		List<ChestItem> held = Arrays.asList(new ChestItem("Sunfire splinters", 230), new ChestItem("Death rune", 150), new ChestItem("Rune platebody", 1));
		String json = PickPayload.toJson(tracker.getWave(), tracker.modifiers(m -> 0), tracker.getOptions(), Collections.emptyList(), next, held);
		try
		{
			assertEquals(fixture("pick-chest.json"), json);
		}
		catch (IOException e)
		{
			throw new AssertionError(e);
		}
		// Without the chest, as before the game sends it, the link has none.
		assertTrue(PickPayload.toJson(tracker.getWave(), tracker.modifiers(m -> 0), tracker.getOptions(), Collections.emptyList(),
			Collections.emptyList(), Collections.emptyList()).endsWith("[\"myopia\",1]]}"));
	}

	/** A wave the plugin did not see start has no ticks and no hits; a new run's wave 1 forgets the last run. */
	@Test
	public void aWaveSeenPartWayAndANewRun()
	{
		ColosseumTracker tracker = new ColosseumTracker();
		ModifierLevels none = m -> 0;
		tracker.onGameMessage("Wave 4 completed!", 50);
		tracker.onRunTick(none, 3000, 25);
		assertEquals(1, tracker.getRun().size());
		RunWave four = tracker.getRun().get(0);
		assertEquals(4, four.getWave());
		assertNull(four.getTicks());
		assertNull(four.getHit());
		assertNull(four.getDamage());
		assertEquals(3000, four.getGlory());
		assertTrue(PickPayload.toJson(5, tracker.modifiers(none), tracker.getOptions(), tracker.getRun())
			.endsWith(",\"run\":[{\"wave\":4,\"ticks\":null,\"hit\":null,\"damage\":null,\"modifiers\":{},\"glory\":3000}]}"));

		tracker.onGameMessage("Wave: 1", 900);
		assertTrue(tracker.getRun().isEmpty());
		tracker.onGameMessage("Wave 1 completed!", 999);
		tracker.onRunTick(none, 0, 0);
		tracker.reset();
		assertTrue(tracker.getRun().isEmpty());
	}

	/** On a wave's first tick the damage counter may still hold the last wave's, which the wave does not take. */
	@Test
	public void theWaveDamageCounterSkipsTheWavesFirstTick()
	{
		ColosseumTracker tracker = new ColosseumTracker();
		ModifierLevels none = m -> 0;
		tracker.onGameMessage("Wave: 3", 100);
		tracker.onRunTick(none, 1500, 12);
		tracker.onRunTick(none, 1500, 0);
		tracker.onGameMessage("Wave 3 completed!", 160);
		tracker.onRunTick(none, 2400, 0);
		assertEquals(Integer.valueOf(0), tracker.getRun().get(0).getDamage());
	}
}
