package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import org.junit.Test;

/**
 * The settings 1.0 promises to keep (the 1.0 plan of 2026-10-26): a key renamed resets the setting for every player who
 * changed it, so each key a release shipped stays as it is, and a new one is added beside them. Everything the plugin
 * draws on the game's screen or sends to the site is off until the player switches it on. The sections in the order of
 * the UX review of 2026-10-27 (F7).
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

	@Test
	public void sendingToTheSiteComesFirstAfterTheGeneralSettingsAndTheLongSectionsStartShut()
	{
		// RuneLite lists the sections and the settings in none by position, and two at one position by name, which put
		// Sending to the site under a shut Safe / Fast / Easy (checked in 1.13.4's ConfigPanel): each has its own.
		Map<Integer, ConfigSection> sections = new TreeMap<>();
		for (Field f : SolSurvivorConfig.class.getFields())
		{
			ConfigSection section = f.getAnnotation(ConfigSection.class);
			if (section != null)
			{
				assertNull("Two sections at position " + section.position(), sections.put(section.position(), section));
			}
		}
		int general = 0;
		for (Method m : SolSurvivorConfig.class.getMethods())
		{
			ConfigItem item = m.getAnnotation(ConfigItem.class);
			if (item != null && item.section().isEmpty() && !item.hidden())
			{
				general = Math.max(general, item.position());
			}
		}
		List<String> names = new ArrayList<>();
		List<String> shut = new ArrayList<>();
		for (ConfigSection section : sections.values())
		{
			assertTrue(section.name() + " sits among the general settings", section.position() > general);
			names.add(section.name());
			if (section.closedByDefault())
			{
				shut.add(section.name());
			}
		}
		assertEquals(Arrays.asList("Sending to the site", "Mistake tracker", "Waves and modifiers", "On the game screen", "Loot",
			"Safe / Fast / Easy", "Prayer reliability"), names);
		assertEquals(Arrays.asList("Mistake tracker", "Waves and modifiers", "Safe / Fast / Easy", "Prayer reliability"), shut);
	}
}
