package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
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
}
