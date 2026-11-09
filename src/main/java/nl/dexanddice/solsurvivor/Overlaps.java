package nl.dexanddice.solsurvivor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import nl.dexanddice.solsurvivor.core.WaveReview;

/**
 * Which of In the game's features another enabled plugin also draws, for the panel's "also" marks (idea 375, design B,
 * Stefan's pick of 2026-11-08). Each rule names one of our features, the other plugin as the Plugin Hub lists it, the
 * setting of its that draws the same, and how to read whether that setting is on. The plugin only reads those settings,
 * never writes them: a Hub plugin leaves another plugin's settings alone, so the mark tells the player where to look.
 * <p>
 * The settings were checked in each plugin's source on 8 November 2026, at the commit the Plugin Hub builds where it
 * could be fetched: Fortis Colosseum (config group `fortiscolosseum`), Tzhaar/Colo HP Tracker (`TzhaarHPTracker`),
 * Attack Ranges (`attackRanges`), Visual Metronome (`visualmetronome`), Tzhaar/Colo Additions (`TzhaarColoAdditions`)
 * and Better NPC Highlight (`BetterNpcHighlight`). A setting the player never changed is not stored, so a null reads as
 * that plugin's default, which each rule spells out.
 */
final class Overlaps
{
	/** Reads another plugin's stored setting, null when it holds its default. */
	interface Settings
	{
		String get(String group, String key);
	}

	/** One overlap: our feature's key, the other plugin's name, where its setting is, and whether that setting draws. */
	static final class Rule
	{
		final String ours;
		final String plugin;
		final String setting;
		final Predicate<Settings> draws;

		Rule(String ours, String plugin, String setting, Predicate<Settings> draws)
		{
			this.ours = ours;
			this.plugin = plugin;
			this.setting = setting;
			this.draws = draws;
		}
	}

	static final String FORTIS = "Fortis Colosseum";
	static final String HP_TRACKER = "Tzhaar/Colo HP Tracker";
	static final String ATTACK_RANGES = "Attack Ranges";
	static final String METRONOME = "Visual Metronome";
	static final String ADDITIONS = "Tzhaar/Colo Additions";
	static final String BETTER_NPC_HIGHLIGHT = "Better NPC Highlight";

	/** The config groups the rules read, and RuneLite's own, which holds which plugins are on. */
	static final Set<String> GROUPS = new HashSet<>(Arrays.asList("fortiscolosseum", "TzhaarHPTracker", "attackRanges",
		"visualmetronome", "TzhaarColoAdditions", "BetterNpcHighlight", "runelite"));

	/** Better NPC Highlight's lists of NPC ids, each a highlight of its own. */
	private static final String[] HIGHLIGHT_ID_LISTS = {"tileIds", "trueTileIds", "swTileIds", "swTrueTileIds", "hullIds",
		"areaIds", "outlineIds", "clickboxIds"};

	/** The Fremennik warband's ids, which Frem and orb colours outline. */
	private static final Set<String> FREM_IDS = new HashSet<>(Arrays.asList(Integer.toString(WaveReview.FREM_ARCHER_ID),
		Integer.toString(WaveReview.FREM_SEER_ID), Integer.toString(WaveReview.FREM_BERSERKER_ID)));

	static final List<Rule> RULES = Collections.unmodifiableList(Arrays.asList(
		new Rule("waveBox", FORTIS, "Waves Overlay, Display", s -> !"OFF".equals(s.get("fortiscolosseum", "wavesOverlayMode"))),
		new Rule("modifiersBar", FORTIS, "Modifiers, Overlay Enabled", s -> on(s.get("fortiscolosseum", "modifiersOverlayEnabled"), true)),
		new Rule("volatilityReminder", FORTIS, "Modifiers, Volatility Reminder", s -> on(s.get("fortiscolosseum", "volatilityReminder"), false)),
		new Rule("offerNames", FORTIS, "Modifiers, Show Names on Selector", s -> on(s.get("fortiscolosseum", "modifiersNamesOnSelectWidget"), true)),
		new Rule("waveTimer", FORTIS, "Splits, Overlay Panel", s -> isSet(s.get("fortiscolosseum", "splitsOverlayMode"), "OFF")),
		new Rule("lootTotals", FORTIS, "Loot Interface, Show Loot Total", s -> !"OFF".equals(s.get("fortiscolosseum", "lootInterfaceShowTotal"))),
		new Rule("leftClickBankAll", FORTIS, "Loot Interface, Left-Click Bank-All", s -> on(s.get("fortiscolosseum", "leftClickBankAll"), true)),
		new Rule("npcHitpoints", HP_TRACKER, "Highlight Settings, Show HP", s -> !"OFF".equals(s.get("TzhaarHPTracker", "showHp"))),
		new Rule("myRange", ATTACK_RANGES, "Options, Display Overlay", s -> !"OFF".equals(s.get("attackRanges", "playerEnableState"))),
		new Rule("highlightInRange", ATTACK_RANGES, "Options, Highlight NPCs", s -> isSet(s.get("attackRanges", "npcHighlightEnableState"), "OFF")),
		// The metronome's own box carries no number; it counts ticks only with one of its two tick numbers on.
		new Rule("tickCounter", METRONOME, "Tick Number Settings, Show Metronome Tick Number",
			s -> on(s.get("visualmetronome", "enableMetronome"), true)
				&& (on(s.get("visualmetronome", "showTick"), false) || on(s.get("visualmetronome", "showPlayerTick"), false))),
		new Rule("hidePillars", ADDITIONS, "Colosseum, Hide Colosseum pillars", s -> on(s.get("TzhaarColoAdditions", "hideColosseumPillars"), true)),
		new Rule("npcColours", BETTER_NPC_HIGHLIGHT, "the Fremennik ids in its highlight ID lists", Overlaps::listsFrems)
	));

	private Overlaps()
	{
	}

	/**
	 * Each of our features that an enabled plugin also draws, in RULES' order, with its mark: the first such plugin's
	 * short name, and the tooltip's line, which names each plugin and the setting of its that draws the same. `enabled`
	 * holds the names of the plugins on.
	 */
	static Map<String, String[]> find(Collection<String> enabled, Settings settings)
	{
		Map<String, List<Rule>> found = new LinkedHashMap<>();
		for (Rule rule : RULES)
		{
			if (enabled.contains(rule.plugin) && rule.draws.test(settings))
			{
				found.computeIfAbsent(rule.ours, k -> new ArrayList<>()).add(rule);
			}
		}
		Map<String, String[]> marks = new LinkedHashMap<>();
		for (Map.Entry<String, List<Rule>> e : found.entrySet())
		{
			StringBuilder tip = new StringBuilder();
			for (Rule rule : e.getValue())
			{
				tip.append(tip.length() == 0 ? "" : " ").append(rule.plugin).append(" also draws this: its setting ")
					.append(rule.setting).append('.');
			}
			tip.append(" Switch off ours here, or theirs in its settings, to see it once.");
			marks.put(e.getKey(), new String[]{shortName(e.getValue().get(0).plugin), tip.toString()});
		}
		return marks;
	}

	/** The short name a mark shows: Fortis Colosseum as "Fortis", the HP Tracker as "HP Tracker" and so on. */
	static String shortName(String plugin)
	{
		switch (plugin)
		{
			case FORTIS:
				return "Fortis";
			case HP_TRACKER:
				return "HP Tracker";
			case ADDITIONS:
				return "Colo Additions";
			case BETTER_NPC_HIGHLIGHT:
				return "NPC Highlight";
			case METRONOME:
				return "Metronome";
			default:
				return plugin;
		}
	}

	/** A stored boolean, or the plugin's default when it holds none. */
	private static boolean on(String stored, boolean byDefault)
	{
		return stored == null ? byDefault : Boolean.parseBoolean(stored);
	}

	/** Whether a choice whose default is `off` is set to something else. */
	private static boolean isSet(String stored, String off)
	{
		return stored != null && !off.equals(stored);
	}

	/** Whether any of Better NPC Highlight's ID lists names a Fremennik, whose outline ours draws too. */
	private static boolean listsFrems(Settings settings)
	{
		for (String key : HIGHLIGHT_ID_LISTS)
		{
			String list = settings.get("BetterNpcHighlight", key);
			if (list == null)
			{
				continue;
			}
			for (String id : list.split("[,\\s]+"))
			{
				if (FREM_IDS.contains(id.trim()))
				{
					return true;
				}
			}
		}
		return false;
	}
}
