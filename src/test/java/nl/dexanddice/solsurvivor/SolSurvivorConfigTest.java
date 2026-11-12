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
	/**
	 * The keys 0.9.0 shipped, then those 1.0 adds, less the ones Stefan chose to take out on 2026-11-12: the mistake
	 * tracker's switch for each kind with Idle from, and Set Safe / Fast / Easy with its three shares, which Suggested plan
	 * replaces.
	 */
	private static final List<String> SHIPPED = Arrays.asList(
		"copyHotkey", "includeGear", "chatConfirm", "openOnEntering", "onlyAtColosseum", "recordWaveLog", "keepRuns", "uploadPastRuns",
		"shareWaveLogs", "leaderboard", "leaderboardName", "leaderboardKey",
		"countFlicks", "prayerRecord",
		"trackMistakes", "reportInChat", "deathRecap", "showWaveNpcs", "markDead", "listWarband", "showNextWave", "showModifiers", "doomLimit",
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
	public void whatSendsToTheSiteIsOffAndTheGameScreenStartsAtThePlannerPreset()
	{
		assertFalse(defaults.uploadPastRuns());
		assertFalse(defaults.shareWaveLogs());
		assertFalse(defaults.leaderboard());
		// On: the Planner preset, what a player who came for the planner wants on the game's screen.
		assertEquals(SolSurvivorConfig.WaveBox.BOTH, defaults.waveBox());
		assertTrue(defaults.modifiersBar());
		assertTrue(defaults.doomByOrb());
		assertTrue(defaults.offerNames());
		assertTrue(defaults.waveTimer());
		assertTrue(defaults.volatilityReminder());
		// Off since 2026-11-11, so that a fresh install does not fill the screen: the Screen tab's More to switch on
		// shows each with its picture. These are the six a player who had the plugin before keeps on.
		assertFalse(defaults.npcHitpoints());
		assertFalse(defaults.highlightInRange());
		assertFalse(defaults.npcColours());
		assertFalse(defaults.tickCounter());
		assertFalse(defaults.lootTotals());
		assertFalse(defaults.leftClickBankAll());
		assertEquals(java.util.Arrays.asList("npcHitpoints", "highlightInRange", "npcColours", "tickCounter", "lootTotals", "leftClickBankAll"),
			java.util.Arrays.asList(SolSurvivorConfig.QUIET_FROM_INSTALL));
		assertFalse(defaults.moreToSwitchOnHidden());
		// As they are once their feature is switched on.
		assertTrue(defaults.hitForecast());
		assertEquals(SolSurvivorConfig.InRangeStyle.BOLD_TILE, defaults.inRangeStyle());
		assertTrue(defaults.combineWaveTimers());
		assertTrue(defaults.waveBoxStyleColours());
		// Off: an outline that covers the floor, and a change to how the arena looks.
		assertFalse(defaults.myRange());
		assertFalse(defaults.hidePillars());
		// The panel's own cards show from the start: they draw nothing in the game and send nothing.
		assertTrue(defaults.trackMistakes());
		assertTrue(defaults.deathRecap());
		assertTrue(defaults.showWaveNpcs());
	}

	@Test
	public void thePanelKnowsWhichSwitchesAFreshInstallHasOff() throws Exception
	{
		// More to switch on and the tests draw a fresh install by this list, so it must stay the defaults' own.
		for (String[] feature : SolSurvivorPanel.IN_GAME)
		{
			Object value = SolSurvivorConfig.class.getMethod(feature[0]).invoke(defaults);
			boolean on = value instanceof Boolean ? (Boolean) value : value != SolSurvivorConfig.WaveBox.OFF;
			assertEquals(feature[0], !on, SolSurvivorPanel.OFF_AS_INSTALLED.contains(feature[0]));
		}
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
	public void eachInTheGameSwitchReadsItsOwnSetting()
	{
		// A key with no case in the plugin's reader always read off, so its switch could not be turned on (Frem and orb
		// colours, found in RuneLite on 2026-11-04). Each feature alone switched on reads on, and every other one off.
		for (String[] only : SolSurvivorPanel.IN_GAME)
		{
			SolSurvivorConfig config = (SolSurvivorConfig) java.lang.reflect.Proxy.newProxyInstance(
				SolSurvivorConfig.class.getClassLoader(), new Class<?>[]{SolSurvivorConfig.class},
				(proxy, method, args) ->
				{
					boolean on = method.getName().equals(only[0]);
					if (method.getReturnType() == SolSurvivorConfig.WaveBox.class)
					{
						return on ? SolSurvivorConfig.WaveBox.BOTH : SolSurvivorConfig.WaveBox.OFF;
					}
					return method.getReturnType() == boolean.class ? (Object) on : null;
				});
			for (String[] feature : SolSurvivorPanel.IN_GAME)
			{
				assertEquals(only[0] + " on, " + feature[0], feature == only, SolSurvivorPlugin.inGame(config, feature[0]));
			}
		}
	}

	@Test
	public void clocksCountUpFromZeroInOneStackedBoxUntilThePlayerChoosesOtherwise() throws Exception
	{
		assertEquals(SolSurvivorConfig.TickCounterCounts.UP_FROM_ZERO, defaults.tickCounterCounts());
		assertEquals(5, defaults.tickLoopLength());
		assertEquals(net.runelite.client.config.Keybind.NOT_SET, defaults.tickResetHotkey());
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
		expected.put(SolSurvivorConfig.screenSection, Arrays.asList("waveBox", "volatilityReminder", "offerNames", "npcHitpoints", "hitForecast",
			"quietOnSol", "npcArrows", "npcColours", "hidePillars", "modifiersBar", "modifiersBarSize", "modifiersBarTiers", "doomByOrb",
			"doomPlace"));
		expected.put(SolSurvivorConfig.rangeSection, Arrays.asList("myRange", "highlightInRange", "inRangeStyle"));
		// Every colour in one shut section at the foot (the plugin's UI review of 2026-11-11), named for what it colours.
		expected.put(SolSurvivorConfig.coloursSection, Arrays.asList("rangeColour", "fremArcherColour", "fremSeerColour", "fremBerserkerColour",
			"orbColour", "waveBoxMagicColour", "waveBoxRangedColour", "waveBoxMeleeColour", "hiddenPillarsColour"));
		expected.put(SolSurvivorConfig.clocksSection, Arrays.asList("tickCounter", "tickCounterCounts", "tickLoopLength",
			"loopColours", "tickResetHotkey", "waveTimer", "combineWaveTimers"));
		expected.put(SolSurvivorConfig.mistakesSection, Arrays.asList("trackMistakes", "reportInChat", "deathRecap"));
		expected.put(SolSurvivorConfig.triangleSection, Arrays.asList("suggestedPlan"));
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
		for (Method method : SolSurvivorConfig.class.getMethods())
		{
			ConfigItem item = method.getAnnotation(ConfigItem.class);
			if (item != null && method.getReturnType() == java.awt.Color.class)
			{
				assertEquals(item.keyName(), SolSurvivorConfig.coloursSection, item.section());
			}
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
		// What you share under a shut Safe / Fast / Easy (checked in 1.13.4's ConfigPanel): each has its own.
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
		assertEquals(Arrays.asList("What you share", "Mistake tracker", "Waves and modifiers", "On screen",
			"Attack range", "Tick counter and wave timer", "Loot",
			"Safe / Fast / Easy", "Prayer reliability", "Colours"), names);
		assertEquals(Arrays.asList("Mistake tracker", "Waves and modifiers", "Attack range",
			"Tick counter and wave timer", "Safe / Fast / Easy", "Prayer reliability", "Colours"), shut);
	}

	@Test
	public void anUpdateKeepsWhatAPlayerHadOnTheScreenAndAFreshInstallStartsQuiet()
	{
		// A player who had the plugin, and had switched the tick counter off themself: the other five are kept on.
		java.util.Map<String, String> held = new java.util.HashMap<>();
		held.put("tickCounter", "false");
		java.util.Map<String, Boolean> written = new java.util.LinkedHashMap<>();
		SolSurvivorPlugin.settleDefaults(true, held::get, written::put);
		for (String key : SolSurvivorConfig.QUIET_FROM_INSTALL)
		{
			assertEquals(key, key.equals("tickCounter") ? null : Boolean.TRUE, written.get(key));
		}
		assertEquals(Boolean.TRUE, written.get(SolSurvivorConfig.MORE_HIDDEN));
		assertEquals(Boolean.TRUE, written.get(SolSurvivorConfig.DEFAULTS_SETTLED));
		// A fresh install is only marked: its settings stay at their defaults, and More to switch on shows.
		written.clear();
		SolSurvivorPlugin.settleDefaults(false, key -> null, written::put);
		assertEquals(java.util.Collections.singletonMap(SolSurvivorConfig.DEFAULTS_SETTLED, Boolean.TRUE), written);
	}

	@Test
	public void theSuggestedPlanIsTheSitesOwnUntilChosenAndAnEarlierInstallKeepsItsSharesAsItSetThem()
	{
		SolSurvivorConfig.SuggestedPlan own = SolSurvivorConfig.SuggestedPlan.SITES_OWN;
		assertEquals(own, defaults.suggestedPlan());
		assertNull(defaults.suggestedPlan().dot());
		assertEquals(Arrays.asList("Site's own", "Balanced", "Safe", "Fast", "Easy"),
			Arrays.stream(SolSurvivorConfig.SuggestedPlan.values()).map(Object::toString).collect(java.util.stream.Collectors.toList()));
		// The switch on with its shares as they were stored: the links carry them unchanged, and a share never set was 33.
		assertEquals(Arrays.asList(100, 0, 0), shares(own.dotWith("true", "100", "0", "0")));
		assertEquals(Arrays.asList(0, 100, 0), shares(own.dotWith("true", "0", "100", "0")));
		assertEquals(Arrays.asList(0, 0, 100), shares(own.dotWith("true", "0", "0", "100")));
		assertEquals(Arrays.asList(60, 30, 10), shares(own.dotWith("true", "60", "30", "10")));
		assertEquals(Arrays.asList(50, 33, 33), shares(own.dotWith("true", "50", null, null)));
		assertEquals(Arrays.asList(33, 33, 33), shares(own.dotWith("true", null, null, null)));
		// Off, never set, or every share 0, sent nothing before and sends nothing now.
		assertNull(own.dotWith("false", "100", "0", "0"));
		assertNull(own.dotWith(null, null, null, null));
		assertNull(own.dotWith("true", "0", "0", "0"));
		// A plan chosen here goes before the earlier shares.
		assertEquals(Arrays.asList(0, 100, 0), shares(SolSurvivorConfig.SuggestedPlan.FAST.dotWith("true", "100", "0", "0")));
	}

	private static java.util.List<Integer> shares(nl.dexanddice.solsurvivor.core.TriangleDot dot)
	{
		return Arrays.asList(dot.getSafe(), dot.getFast(), dot.getEasy());
	}
}
