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
 * changed it, so each key a release shipped stays as it is, and a new one is added beside them. What the plugin sends
 * to the site is off until the player switches it on; most of what it draws on the game's screen is on, so that a player
 * sees what it offers (Stefan's choice, 2026-10-30). The sections in the order of the UX review of 2026-10-27 (F7).
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
	public void whatSendsToTheSiteIsOffAndMostOfTheGameScreenIsOn()
	{
		assertFalse(defaults.uploadPastRuns());
		assertFalse(defaults.shareWaveLogs());
		assertFalse(defaults.leaderboard());
		// On, so that a player with another plugin for the same thing sees this one offers it too.
		assertEquals(SolSurvivorConfig.WaveBox.BOTH, defaults.waveBox());
		assertTrue(defaults.modifiersBar());
		assertTrue(defaults.doomByOrb());
		assertTrue(defaults.offerNames());
		assertTrue(defaults.npcHitpoints());
		assertTrue(defaults.highlightInRange());
		assertTrue(defaults.tickCounter());
		assertTrue(defaults.waveTimer());
		assertTrue(defaults.combineWaveTimers());
		assertTrue(defaults.lootTotals());
		assertTrue(defaults.leftClickBankAll());
		assertTrue(defaults.volatilityReminder());
		// Off: an outline that covers the floor, and a change to how the arena looks.
		assertFalse(defaults.myRange());
		assertFalse(defaults.hidePillars());
		// The panel's own cards show from the start: they draw nothing in the game and send nothing.
		assertTrue(defaults.trackMistakes());
		assertTrue(defaults.deathRecap());
		assertTrue(defaults.showWaveNpcs());
	}

	@Test
	public void thePanelsInTheGameSwitchesAreSettingsOfTheirOwn() throws Exception
	{
		Set<String> seen = new HashSet<>();
		for (String[] feature : SolSurvivorPanel.IN_GAME)
		{
			assertTrue("Twice in the panel: " + feature[0], seen.add(feature[0]));
			Method m = SolSurvivorConfig.class.getMethod(feature[0]);
			assertEquals(feature[0], m.getAnnotation(ConfigItem.class).keyName());
			// Named as in the settings, so that a player finds it there too; the one too long for the panel's line, shortened.
			if (!feature[0].equals("highlightInRange"))
			{
				assertEquals(m.getAnnotation(ConfigItem.class).name(), feature[1]);
			}
			assertTrue(feature[0], m.getReturnType() == boolean.class || m.getReturnType() == SolSurvivorConfig.WaveBox.class);
		}
	}

	@Test
	public void clocksCountUpFromZeroInOneStackedBoxUntilThePlayerChoosesOtherwise() throws Exception
	{
		assertEquals(SolSurvivorConfig.TickCounterCounts.UP_FROM_ZERO, defaults.tickCounterCounts());
		assertEquals(5, defaults.tickLoopLength());
		assertEquals(net.runelite.client.config.Keybind.NOT_SET, defaults.tickResetHotkey());
		assertEquals(SolSurvivorConfig.CombinedTimerLayout.STACKED, defaults.combinedTimerLayout());
		assertEquals(SolSurvivorConfig.LargeTimerNumber.TICK, defaults.largeTimerNumber());
		assertEquals(SolSurvivorConfig.LoopColours.OFF, defaults.loopColours());
		net.runelite.client.config.Range range = SolSurvivorConfig.class.getMethod("tickLoopLength")
			.getAnnotation(net.runelite.client.config.Range.class);
		assertEquals(2, range.min());
		assertEquals(10, range.max());
		Set<Integer> positions = new HashSet<>();
		for (Method method : SolSurvivorConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null && SolSurvivorConfig.screenSection.equals(item.section()))
			{
				assertTrue("Two screen settings at " + item.position(), positions.add(item.position()));
			}
		}
	}

	/**
	 * RuneLite cannot hide a setting while another is off, so each feature's settings share a shut section, its switch
	 * first and what only matters with it on under it (Stefan's choice, 2026-10-30), with no two at one position.
	 */
	@Test
	public void eachFeatureKeepsItsSettingsInItsOwnSectionWithItsSwitchFirst()
	{
		Map<String, List<String>> expected = new TreeMap<>();
		expected.put(SolSurvivorConfig.screenSection, Arrays.asList("waveBox", "volatilityReminder", "offerNames", "npcHitpoints"));
		expected.put(SolSurvivorConfig.modifiersBarSection, Arrays.asList("modifiersBar", "modifiersBarSize", "modifiersBarTiers"));
		expected.put(SolSurvivorConfig.doomSection, Arrays.asList("doomByOrb", "doomPlace"));
		expected.put(SolSurvivorConfig.rangeSection, Arrays.asList("myRange", "highlightInRange", "rangeColour"));
		expected.put(SolSurvivorConfig.pillarsSection, Arrays.asList("hidePillars", "hiddenPillarsColour"));
		expected.put(SolSurvivorConfig.clocksSection, Arrays.asList("tickCounter", "tickCounterCounts", "tickLoopLength",
			"loopColours", "tickResetHotkey", "waveTimer", "combineWaveTimers", "combinedTimerLayout", "largeTimerNumber"));
		Map<String, Map<Integer, String>> found = new TreeMap<>();
		for (Method method : SolSurvivorConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null && expected.containsKey(item.section()))
			{
				String was = found.computeIfAbsent(item.section(), k -> new TreeMap<>()).put(item.position(), item.keyName());
				assertNull("Two settings at " + item.position() + " in " + item.section(), was);
			}
		}
		for (Map.Entry<String, List<String>> e : expected.entrySet())
		{
			assertEquals(e.getValue(), new ArrayList<>(found.get(e.getKey()).values()));
		}
		assertEquals(SolSurvivorConfig.DoomPlace.BY_ORB, defaults.doomPlace());
	}

	@Test
	public void openingTheBrowserHasNoDefaultHotkeyAndTheGeneralSettingsHaveSeparatePositions()
	{
		assertEquals(net.runelite.client.config.Keybind.NOT_SET, defaults.openArenaHotkey());
		Set<Integer> positions = new HashSet<>();
		for (Method method : SolSurvivorConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null && item.section().isEmpty() && !item.hidden())
			{
				assertTrue("Two general settings at " + item.position(), positions.add(item.position()));
			}
		}
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
		assertEquals(Arrays.asList("Sending to the site", "Mistake tracker", "Waves and modifiers", "On the game screen",
			"Modifiers above the chatbox", "Doom",
			"Attack range", "Pillars", "Tick counter and wave timer", "Tick loop colours", "Loot", "Safe / Fast / Easy",
			"Prayer reliability"), names);
		assertEquals(Arrays.asList("Mistake tracker", "Waves and modifiers", "Modifiers above the chatbox", "Doom", "Attack range", "Pillars",
			"Tick counter and wave timer", "Tick loop colours", "Safe / Fast / Easy", "Prayer reliability"), shut);
	}
}
