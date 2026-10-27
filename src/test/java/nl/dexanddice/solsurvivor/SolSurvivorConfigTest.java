package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.client.config.ConfigItem;
import org.junit.Test;

/**
 * The settings 1.0 promises to keep (the 1.0 plan of 2026-10-26): a key renamed resets the setting for every player who
 * changed it, so each key a release shipped stays as it is, and a new one is added beside them. Everything the plugin
 * draws on the game's screen or sends to the site is off until the player switches it on.
 */
public class SolSurvivorConfigTest
{
	/** The keys 0.9.0 shipped, then those 1.0 adds. */
	private static final List<String> SHIPPED = Arrays.asList(
		"copyHotkey", "includeGear", "chatConfirm", "openOnEntering", "onlyAtColosseum", "recordWaveLog", "keepRuns", "uploadPastRuns",
		"shareWaveLogs", "leaderboard", "leaderboardName", "leaderboardKey", "setTriangle", "triangleSafe", "triangleFast", "triangleEasy",
		"countFlicks", "prayerRecord",
		"trackMistakes", "reportInChat", "countPastPrayer", "countLateFlicks", "countSpecials", "countVolatility", "countSolarflareSand",
		"countIdle", "idleFrom", "deathRecap", "showWaveNpcs", "markDead", "listWarband", "showNextWave", "showModifiers", "doomLimit",
		"waveBox", "modifiersBar", "doomByOrb", "volatilityReminder", "offerNames", "myRange", "highlightInRange", "rangeColour",
		"hidePillars", "lootTotals", "leftClickBankAll");

	private final SolSurvivorConfig defaults = new SolSurvivorConfig()
	{
	};

	@Test
	public void everyKeyAReleaseShippedIsStillThereUnderItsGroup()
	{
		assertEquals("solsurvivor", SolSurvivorConfig.GROUP);
		Set<String> keys = new HashSet<>();
		for (Method m : SolSurvivorConfig.class.getMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null)
			{
				assertTrue("Two settings share the key " + item.keyName(), keys.add(item.keyName()));
			}
		}
		for (String key : SHIPPED)
		{
			assertTrue("The setting " + key + " is gone or renamed", keys.contains(key));
		}
	}

	@Test
	public void whatDrawsOnTheScreenOrSendsToTheSiteIsOffUntilSwitchedOn()
	{
		assertEquals(SolSurvivorConfig.WaveBox.OFF, defaults.waveBox());
		assertFalse(defaults.doomByOrb());
		assertFalse(defaults.offerNames());
		assertFalse(defaults.hidePillars());
		assertFalse(defaults.modifiersBar());
		assertFalse(defaults.volatilityReminder());
		assertFalse(defaults.lootTotals());
		assertFalse(defaults.leftClickBankAll());
		assertFalse(defaults.myRange());
		assertFalse(defaults.highlightInRange());
		assertFalse(defaults.uploadPastRuns());
		assertFalse(defaults.shareWaveLogs());
		assertFalse(defaults.leaderboard());
		// The panel's own cards show from the start: they draw nothing in the game and send nothing.
		assertTrue(defaults.trackMistakes());
		assertTrue(defaults.deathRecap());
		assertTrue(defaults.showWaveNpcs());
	}
}
