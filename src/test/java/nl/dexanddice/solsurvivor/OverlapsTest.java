package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * The panel's "also" marks (idea 375): another plugin's overlay is marked only while that plugin is on and its setting
 * draws, reading a setting the player never changed as that plugin's own default.
 */
public class OverlapsTest
{
	private final Map<String, String> stored = new HashMap<>();
	private final Overlaps.Settings settings = (group, key) -> stored.get(group + "." + key);

	@Test
	public void nothingIsMarkedWithoutTheOtherPlugins()
	{
		assertTrue(Overlaps.find(Collections.emptyList(), settings).isEmpty());
		assertTrue(Overlaps.find(Collections.singletonList("Sol Survivor"), settings).isEmpty());
	}

	@Test
	public void fortisAtItsDefaultsDoublesTheWaveBoxModifiersOfferNamesLootAndBankAll()
	{
		Map<String, String[]> marks = Overlaps.find(Collections.singletonList(Overlaps.FORTIS), settings);
		assertEquals(Arrays.asList("waveBox", "modifiersBar", "offerNames", "lootTotals", "leftClickBankAll"), Arrays.asList(marks.keySet().toArray()));
		assertEquals("Fortis", marks.get("waveBox")[0]);
		assertTrue(marks.get("waveBox")[1].contains("Fortis Colosseum also draws this: its setting Waves Overlay, Display."));
	}

	@Test
	public void fortisSettingsTheyChangedAreRead()
	{
		stored.put("fortiscolosseum.wavesOverlayMode", "OFF");
		stored.put("fortiscolosseum.volatilityReminder", "true");
		stored.put("fortiscolosseum.splitsOverlayMode", "WAVE_TIME");
		stored.put("fortiscolosseum.lootInterfaceShowTotal", "OFF");
		Map<String, String[]> marks = Overlaps.find(Collections.singletonList(Overlaps.FORTIS), settings);
		assertFalse(marks.containsKey("waveBox"));
		assertTrue(marks.containsKey("volatilityReminder"));
		assertTrue(marks.containsKey("waveTimer"));
		assertFalse(marks.containsKey("lootTotals"));
	}

	@Test
	public void theOtherPluginsAtTheirDefaults()
	{
		List<String> on = Arrays.asList(Overlaps.HP_TRACKER, Overlaps.ATTACK_RANGES, Overlaps.METRONOME, Overlaps.ADDITIONS,
			Overlaps.BETTER_NPC_HIGHLIGHT);
		Map<String, String[]> marks = Overlaps.find(on, settings);
		// The metronome shows no tick number and Better NPC Highlight lists no frems until the player sets them.
		assertEquals(Arrays.asList("npcHitpoints", "myRange", "hidePillars"), Arrays.asList(marks.keySet().toArray()));
		assertEquals("HP Tracker", marks.get("npcHitpoints")[0]);
		stored.put("visualmetronome.showTick", "true");
		stored.put("BetterNpcHighlight.outlineIds", "12814, 12815");
		stored.put("attackRanges.npcHighlightEnableState", "INSTANCES_ONLY");
		marks = Overlaps.find(on, settings);
		assertTrue(marks.containsKey("tickCounter"));
		assertTrue(marks.containsKey("npcColours"));
		assertTrue(marks.containsKey("highlightInRange"));
		stored.put("visualmetronome.enableMetronome", "false");
		assertNull(Overlaps.find(on, settings).get("tickCounter"));
	}

	@Test
	public void everyRuleIsOneOfInTheGamesFeaturesAndReadsAWatchedGroup()
	{
		for (Overlaps.Rule rule : Overlaps.RULES)
		{
			boolean known = false;
			for (String[] feature : SolSurvivorPanel.IN_GAME)
			{
				known |= feature[0].equals(rule.ours);
			}
			assertTrue(rule.ours, known);
		}
		Map<String, String> asked = new HashMap<>();
		Overlaps.Settings recording = (group, key) ->
		{
			asked.put(group, key);
			return null;
		};
		for (Overlaps.Rule rule : Overlaps.RULES)
		{
			rule.draws.test(recording);
		}
		for (String group : asked.keySet())
		{
			assertTrue(group, Overlaps.GROUPS.contains(group));
		}
	}

	@Test
	public void aBoxThePlayerPlacedIsNeverMoved()
	{
		assertFalse(FixedLayout.placedByPlayer("TickCounterOverlay", settings));
		stored.put("runelite.TickCounterOverlay_preferredLocation", "10:20");
		assertTrue(FixedLayout.placedByPlayer("TickCounterOverlay", settings));
		stored.put("runelite.DoomBoxOverlay_preferredPosition", "TOP_RIGHT");
		assertTrue(FixedLayout.placedByPlayer("DoomBoxOverlay", settings));
	}
}
