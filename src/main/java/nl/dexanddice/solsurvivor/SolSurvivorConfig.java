package nl.dexanddice.solsurvivor;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;
import nl.dexanddice.solsurvivor.core.TriangleDot;

@ConfigGroup(SolSurvivorConfig.GROUP)
public interface SolSurvivorConfig extends Config
{
	String GROUP = "solsurvivor";

	@ConfigItem(
		keyName = "copyHotkey",
		name = "Copy link hotkey",
		description = "Copies the arena's link without opening your browser, so the game keeps focus.",
		position = 0
	)
	default Keybind copyHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "includeGear",
		name = "Include gear and stats",
		description = "Puts your gear, weapon, prayer points and combat levels in the link, and your gear in the wave log.",
		position = 1
	)
	default boolean includeGear()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendMarkers",
		name = "Send my tile markers",
		description = "Puts the arena tiles you marked with Ground Markers or Tile Packs, with their colours and labels, on the site's map.",
		position = 2
	)
	default boolean sendMarkers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chatConfirm",
		name = "Messages in chat",
		description = "Puts the plugin's messages in your chat box as well as the panel.",
		position = 3
	)
	default boolean chatConfirm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "openOnEntering",
		name = "Open panel on entering",
		description = "Opens this panel as you enter the Fortis Colosseum.",
		position = 4
	)
	default boolean openOnEntering()
	{
		return true;
	}

	String ONLY_AT_COLOSSEUM = "onlyAtColosseum";

	@ConfigItem(
		keyName = ONLY_AT_COLOSSEUM,
		name = "Sidebar icon only at the Colosseum",
		description = "Shows the plugin's sun in RuneLite's sidebar only in the Fortis Colosseum and its lobby.",
		position = 5
	)
	default boolean onlyAtColosseum()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recordWaveLog",
		name = "Record wave log",
		description = "Keeps your last 25 waves for Past waves. Nothing uploads unless Upload past runs or Share wave logs is on.",
		position = 6
	)
	default boolean recordWaveLog()
	{
		return true;
	}

	String KEEP_RUNS = "keepRuns";

	@ConfigItem(
		keyName = KEEP_RUNS,
		name = "Keep runs between sessions",
		description = "Saves your last 25 waves in RuneLite's folder on this computer, so they are still there after RuneLite closes. Off deletes the saved ones.",
		position = 7
	)
	default boolean keepRuns()
	{
		return true;
	}

	@ConfigItem(
		keyName = "openArenaHotkey",
		name = "Open Arena now hotkey",
		description = "Opens the arena as it is now in your browser and copies its link, just like the panel's Arena now button.",
		position = 8
	)
	default Keybind openArenaHotkey()
	{
		return Keybind.NOT_SET;
	}

	/** The mistake tracker: the wave report once each wave ends, and the death report. */
	@ConfigSection(
		name = "Mistake tracker",
		description = "Counts your mistakes from the wave log and shows the report once the wave ends, never during it.",
		position = 10,
		closedByDefault = true
	)
	String mistakesSection = "mistakes";

	String TRACK_MISTAKES = "trackMistakes";

	@ConfigItem(
		keyName = TRACK_MISTAKES,
		name = "Track mistakes",
		description = "Counts them quietly; the report shows in the panel when the wave ends.",
		position = 1,
		section = mistakesSection
	)
	default boolean trackMistakes()
	{
		return true;
	}

	@ConfigItem(
		keyName = "reportInChat",
		name = "Say the report in chat",
		description = "One line in your chat box at the wave's end: its mistakes, the damage you took and your prayer.",
		position = 2,
		section = mistakesSection
	)
	default boolean reportInChat()
	{
		return false;
	}

	@ConfigItem(
		keyName = "deathRecap",
		name = "Death report",
		description = "After a death, shows your last ticks in the panel: hitpoints, prayer, and each hit with what dealt it.",
		position = 3,
		section = mistakesSection
	)
	default boolean deathRecap()
	{
		return true;
	}

	/** What the panel lists of the wave under way and the next, and of the modifiers held. */
	@ConfigSection(
		name = "Waves and modifiers",
		description = "What the panel and the wave box list of each wave and of the modifiers you hold.",
		position = 11,
		closedByDefault = true
	)
	String wavesSection = "waves";

	@ConfigItem(
		keyName = "showWaveNpcs",
		name = "Show the wave's NPCs",
		description = "Lists the wave's NPCs in the panel while it is under way, with how many of each are left.",
		position = 1,
		section = wavesSection
	)
	default boolean showWaveNpcs()
	{
		return true;
	}

	@ConfigItem(
		keyName = "markDead",
		name = "Mark the dead",
		description = "Struck through as each one dies. Off, the dead drop off the list.",
		position = 2,
		section = wavesSection
	)
	default boolean markDead()
	{
		return true;
	}

	@ConfigItem(
		keyName = "mostFirst",
		name = "Sort by count",
		description = "Lists each wave's NPCs by how many there are, most first. Those still to come and the dead go last.",
		position = 7,
		section = wavesSection
	)
	default boolean mostFirst()
	{
		return true;
	}

	@ConfigItem(
		keyName = "listWarband",
		name = "List the warband",
		description = "Lists the Fremennik warband's archer, seer and berserker one by one. Off, the three show as one line.",
		position = 3,
		section = wavesSection
	)
	default boolean listWarband()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showNextWave",
		name = "Show the next wave",
		description = "Between waves, lists what the next wave brings.",
		position = 4,
		section = wavesSection
	)
	default boolean showNextWave()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showModifiers",
		name = "Show modifiers held",
		description = "Lists the modifiers you hold, with their tiers.",
		position = 5,
		section = wavesSection
	)
	default boolean showModifiers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "doomLimit",
		name = "Doom against its limit",
		description = "Shows Doom's stacks as \"4 of 10\", red on the last stack, from the game's own count.",
		position = 6,
		section = wavesSection
	)
	default boolean doomLimit()
	{
		return true;
	}

	/** Where the wave box shows: off, the wave under way, the next, or both. */
	enum WaveBox
	{
		OFF("Off"),
		THIS_WAVE("This wave"),
		NEXT_WAVE("Next wave"),
		BOTH("Both");

		private final String name;

		WaveBox(String name)
		{
			this.name = name;
		}

		boolean showsThisWave()
		{
			return this == THIS_WAVE || this == BOTH;
		}

		boolean showsNextWave()
		{
			return this == NEXT_WAVE || this == BOTH;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/**
	 * What the plugin draws on the game's screen, only in the arena. Most of it is on from the start, so that a player
	 * who has another plugin for the same thing sees that this one offers it too, and switches off whichever they do not
	 * want (Stefan's choice, 2026-10-30); the range's outline and hiding the pillars stay off.
	 */
	@ConfigSection(
		name = "On screen",
		description = "What the plugin draws on the game's screen, only in the Fortis Colosseum. Switch off any you have from another plugin.",
		position = 12
	)
	String screenSection = "screen";

	/**
	 * RuneLite cannot hide a setting while another is off (checked in 1.13.4's ConfigItem and ConfigPanel), so a
	 * feature with several settings of its own has a shut section, its switch first and the settings it needs under it
	 * (Stefan's choice, 2026-10-30). The modifiers above the chatbox and Doom, with two settings each beside their
	 * switch, sit in On screen itself since the design updates' item 14 (2026-11-12), which left twelve sections ten. A fresh install has on those the Planner preset draws, and the rest off
	 * (Stefan's choice, 2026-11-11).
	 */

	@ConfigSection(
		name = "Attack range",
		description = "Your attack range and the NPCs in it, outlined on the game's screen.",
		position = 16,
		closedByDefault = true
	)
	String rangeSection = "range";



	@ConfigSection(
		name = "Tick counter and wave timer",
		description = "A plain tick count and the wave's time, in boxes you move with Alt and drag.",
		position = 19,
		closedByDefault = true
	)
	String clocksSection = "clocks";

	@ConfigItem(
		keyName = "waveBox",
		name = "Wave box",
		description = "A box with the wave's NPCs, the next wave's, or both. Hold Alt and drag to move it.",
		position = 1,
		section = screenSection
	)
	default WaveBox waveBox()
	{
		return WaveBox.BOTH;
	}


	@ConfigItem(
		keyName = "modifiersBar",
		name = "Modifiers above the chatbox",
		description = "The modifiers you hold in a row of their pictures with their tiers, above the chatbox, as the panel shows them. Move it by holding Alt and dragging.",
		position = 11,
		section = screenSection
	)
	default boolean modifiersBar()
	{
		return true;
	}

	@Range(min = 100, max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "modifiersBarSize",
		name = "Modifiers: size",
		description = "How big the modifiers' pictures are, from 100% to 300%.",
		position = 12,
		section = screenSection
	)
	default int modifiersBarSize()
	{
		return 100;
	}

	/**
	 * How the bar shows each modifier's tier (the design review's 7, Stefan's choice of 2026-10-31, to let the player pick
	 * between its A and C): in the slot's corner on a dark plate, where 1.0's tier in the corner was lost on a red picture
	 * such as Frailty's, or the modifier's name and tier written under its slot.
	 */
	enum ModifierTiers
	{
		CORNER("In the corner"), NAME_UNDER("Name and tier under");

		private final String label;

		ModifierTiers(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "modifiersBarTiers",
		name = "Modifiers: tiers",
		description = "Each modifier's tier in its slot's corner, or its name and tier written under its slot.",
		position = 13,
		section = screenSection
	)
	default ModifierTiers modifiersBarTiers()
	{
		return ModifierTiers.CORNER;
	}

	/**
	 * Doom's stacks on the game's screen. The key keeps 1.0's name, from when they showed only by the hitpoints orb, so
	 * that a player who switched them on keeps them on; Doom position now says where they show.
	 */
	@ConfigItem(
		keyName = "doomByOrb",
		name = "Doom stacks",
		description = "Doom's stacks against its limit, as \"4/10\", red on the last stack.",
		position = 14,
		section = screenSection
	)
	default boolean doomByOrb()
	{
		return true;
	}

	/** Where Doom's stacks show: beside the hitpoints orb, as 1.0 drew them, or in a box the player places. */
	enum DoomPlace
	{
		BY_ORB("By the hitpoints orb"), MOVABLE("Movable box");

		private final String label;

		DoomPlace(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "doomPlace",
		name = "Doom position",
		description = "Beside the hitpoints orb, or in a box you move with Alt and drag.",
		position = 15,
		section = screenSection
	)
	default DoomPlace doomPlace()
	{
		return DoomPlace.BY_ORB;
	}

	@ConfigItem(
		keyName = "volatilityReminder",
		name = "Volatility reminder",
		description = "While you hold Volatility, its picture over each NPC that dies, for 5 ticks. It does not mark the blast's area.",
		position = 3,
		section = screenSection
	)
	default boolean volatilityReminder()
	{
		return true;
	}

	@ConfigItem(
		keyName = "offerNames",
		name = "Names on Minimus's offers",
		description = "Writes each offer's name on Minimus's screen with the tier taking it brings, such as Frailty III.",
		position = 4,
		section = screenSection
	)
	default boolean offerNames()
	{
		return true;
	}

	@ConfigItem(
		keyName = "npcHitpoints",
		name = "NPC hitpoints",
		description = "Each NPC's hitpoints over its head, as \"172/250\" with a bar, red at a quarter or less.",
		position = 5,
		section = screenSection
	)
	default boolean npcHitpoints()
	{
		return false;
	}

	@ConfigItem(
		keyName = "hitForecast",
		name = "Your hit on its way",
		description = "With NPC hitpoints on, shows what your attack will leave before it lands, as \"172 \u203a 131/250\". An estimate, which can be a point out.",
		position = 6,
		section = screenSection
	)
	default boolean hitForecast()
	{
		return true;
	}

	/**
	 * Wave 12 quiet mode (idea 362, Stefan's pick of 2026-11-08): on Sol Heredit's wave the wave box, the tick counter and
	 * the wave timer have nothing for the fight, so they step aside unless this is off. Doom's stacks, the modifiers and
	 * what is drawn on the NPCs stay.
	 */
	@ConfigItem(
		keyName = "quietOnSol",
		name = "Quiet on Sol Heredit's wave",
		description = "Hides the wave box, the tick counter and the wave timer on Sol Heredit's wave.",
		position = 7,
		section = screenSection
	)
	default boolean quietOnSol()
	{
		return true;
	}

	/** What an arrow to an NPC out of view carries (idea 359). */
	enum NpcArrows
	{
		OFF("Off"), NAME_AND_DISTANCE("Name and distance"), ARROW_ONLY("Arrow only");

		private final String label;

		NpcArrows(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	/**
	 * Arrows to NPCs out of view (idea 359, Stefan's yes of 2026-11-08): an arrow on the game view's edge towards each
	 * living NPC the camera does not show, as the minimap already places them. Off by default. The Manticore's arrow
	 * stays neutral, and no arrow says anything of line of sight or range (the limits Stefan accepted).
	 */
	@ConfigItem(
		keyName = "npcArrows",
		name = "Arrows to NPCs out of view",
		description = "An arrow on the game view's edge towards each living NPC outside it, as the minimap places them.",
		position = 8,
		section = screenSection
	)
	default NpcArrows npcArrows()
	{
		return NpcArrows.OFF;
	}

	@ConfigItem(
		keyName = "myRange",
		name = "My attack range",
		description = "Outlines the tiles you can hit from where you stand, in your line of sight.",
		position = 1,
		section = rangeSection
	)
	default boolean myRange()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightInRange",
		name = "NPCs in range",
		description = "Marks the NPCs you can hit from where you stand.",
		position = 2,
		section = rangeSection
	)
	default boolean highlightInRange()
	{
		return false;
	}

	@ConfigItem(
		keyName = "inRangeStyle",
		name = "In range look",
		description = "How the NPCs you can hit are marked: their tile, an outline round them, an arrow over them or corner brackets.",
		position = 3,
		section = rangeSection
	)
	default InRangeStyle inRangeStyle()
	{
		return InRangeStyle.BOLD_TILE;
	}

	/**
	 * How the NPCs in range are marked, four of the five looks drawn for Stefan on 2026-11-08 (the project's
	 * in-range-tag designs), the bold tile replacing the thin one the plugin drew before, which was hard to see on the sand.
	 */
	enum InRangeStyle
	{
		BOLD_TILE("Bold tile"),
		OUTLINE("Outline"),
		ARROW("Arrow"),
		BRACKETS("Corner brackets");

		private final String name;

		InRangeStyle(String name)
		{
			this.name = name;
		}

		@Override
		public String toString()
		{
			return name;
		}
	}

	/**
	 * Cyan by default since 2026-11-08: the gold it was before is close to the Colosseum's sand, which hid the outlines.
	 */
	@Alpha
	@ConfigItem(
		keyName = "rangeColour",
		name = "In range",
		description = "The colour of the range's outline and of the NPCs in it.",
		position = 1,
		section = coloursSection
	)
	default Color rangeColour()
	{
		return new Color(0x3F, 0xE0, 0xFF, 0xFF);
	}

	@ConfigItem(
		keyName = "npcColours",
		name = "Frem and orb colours",
		description = "Outlines each Fremennik in its attack style's colour, and the tile under each of Solarflare's orbs.",
		position = 9,
		section = screenSection
	)
	default boolean npcColours()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "fremArcherColour",
		name = "Frem archer (ranged)",
		description = "The Fremennik archer's outline; set its opacity to 0 for none.",
		position = 2,
		section = coloursSection
	)
	default Color fremArcherColour()
	{
		return new Color(0x3C, 0xDC, 0x3C, 0xFF);
	}

	@Alpha
	@ConfigItem(
		keyName = "fremSeerColour",
		name = "Frem seer (magic)",
		description = "The Fremennik seer's outline; set its opacity to 0 for none.",
		position = 3,
		section = coloursSection
	)
	default Color fremSeerColour()
	{
		return new Color(0x3C, 0x8C, 0xFF, 0xFF);
	}

	@Alpha
	@ConfigItem(
		keyName = "fremBerserkerColour",
		name = "Frem berserker (melee)",
		description = "The Fremennik berserker's outline; set its opacity to 0 for none.",
		position = 4,
		section = coloursSection
	)
	default Color fremBerserkerColour()
	{
		return new Color(0xFF, 0x3C, 0x3C, 0xFF);
	}

	@Alpha
	@ConfigItem(
		keyName = "orbColour",
		name = "Solarflare's orbs",
		description = "The outline of the tile under each of Solarflare's orbs; set its opacity to 0 for none.",
		position = 5,
		section = coloursSection
	)
	default Color orbColour()
	{
		return new Color(0xFF, 0xA0, 0x1E, 0xFF);
	}

	@ConfigItem(
		keyName = "waveBoxStyleColours",
		name = "Names by attack style",
		description = "Tints each NPC's name in the wave box by how it attacks: magic, ranged or melee. The Manticore, which uses all three, stays white.",
		position = 8,
		section = wavesSection
	)
	default boolean waveBoxStyleColours()
	{
		return true;
	}

	@ConfigItem(
		keyName = "waveBoxMagicColour",
		name = "Wave box: magic",
		description = "The name of an NPC that attacks with magic: the Serpent shaman, the Shockwave Colossus and the seer.",
		position = 6,
		section = coloursSection
	)
	default Color waveBoxMagicColour()
	{
		return new Color(0x8C, 0xC8, 0xFF);
	}

	@ConfigItem(
		keyName = "waveBoxRangedColour",
		name = "Wave box: ranged",
		description = "The name of an NPC that attacks with ranged: the Javelin Colossus and the archer.",
		position = 7,
		section = coloursSection
	)
	default Color waveBoxRangedColour()
	{
		return new Color(0x9C, 0xE6, 0x9C);
	}

	@ConfigItem(
		keyName = "waveBoxMeleeColour",
		name = "Wave box: melee",
		description = "The name of an NPC that attacks with melee: the Jaguar warrior, the Minotaur and the berserker.",
		position = 8,
		section = coloursSection
	)
	default Color waveBoxMeleeColour()
	{
		return new Color(0xFF, 0xA0, 0xA0);
	}

	String HIDE_PILLARS = "hidePillars";

	@ConfigItem(
		keyName = HIDE_PILLARS,
		name = "Hide the pillars",
		description = "Hides the four pillars and marks their squares on the floor; they still block paths and sight.",
		position = 10,
		section = screenSection
	)
	default boolean hidePillars()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenPillarsColour",
		name = "Hidden pillars",
		description = "The colour of each hidden pillar's square; set its opacity to 0 for none.",
		position = 9,
		section = coloursSection
	)
	default Color hiddenPillarsColour()
	{
		return new Color(0x1A, 0x12, 0x0C, 0x96);
	}

	@ConfigItem(
		keyName = "tickCounter",
		name = "Tick counter",
		description = "A plain tick count from entering the arena. No tick is marked; hidden on Sol Heredit's wave.",
		position = 1,
		section = clocksSection
	)
	default boolean tickCounter()
	{
		return false;
	}

	enum TickCounterCounts
	{
		UP_FROM_ZERO("Up from 0"), LOOP("Loop 1 to N");

		private final String label;

		TickCounterCounts(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "tickCounterCounts",
		name = "Count",
		description = "Up from 0 restarts on your modifier pick and your hotkey; Loop 1 to N only on your hotkey.",
		position = 2,
		section = clocksSection
	)
	default TickCounterCounts tickCounterCounts()
	{
		return TickCounterCounts.UP_FROM_ZERO;
	}

	@Range(min = 2, max = 10)
	@ConfigItem(
		keyName = "tickLoopLength",
		name = "Loop length",
		description = "How many ticks the loop counts before starting again at 1.",
		position = 3,
		section = clocksSection
	)
	default int tickLoopLength()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "tickResetHotkey",
		name = "Tick counter reset hotkey",
		description = "Restarts the tick counter.",
		position = 5,
		section = clocksSection
	)
	default Keybind tickResetHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "waveTimer",
		name = "Wave timer",
		description = "The time since your modifier pick, stopping at the wave's end; hidden on Sol Heredit's wave.",
		position = 6,
		section = clocksSection
	)
	default boolean waveTimer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "combineWaveTimers",
		name = "One box for both",
		description = "Puts the tick counter and the wave timer in one box: the tick large, the time small beneath it.",
		position = 7,
		section = clocksSection
	)
	default boolean combineWaveTimers()
	{
		return true;
	}

	enum LoopColours
	{
		OFF("Off"), NUMBER("Number"), BOX("Box");

		private final String label;

		LoopColours(String label)
		{
			this.label = label;
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	@ConfigItem(
		keyName = "loopColours",
		name = "Loop colours",
		description = "With Loop 1 to N, colours each position, on the number or the box. Five colours, repeating on a longer loop.",
		position = 4,
		section = clocksSection
	)
	default LoopColours loopColours()
	{
		return LoopColours.OFF;
	}

	/** Minimus's loot: what it adds up to, and banking it in one click. */
	@ConfigSection(
		name = "Loot",
		description = "Minimus's loot between waves and the reward chest at the end.",
		position = 21
	)
	String lootSection = "loot";

	@ConfigItem(
		keyName = "lootTotals",
		name = "Loot totals",
		description = "On Minimus's screen between waves, the value of all your loot so far, and what it comes to with the next wave's.",
		position = 1,
		section = lootSection
	)
	default boolean lootTotals()
	{
		return false;
	}

	@ConfigItem(
		keyName = "leftClickBankAll",
		name = "Left-click Bank-all",
		description = "Makes Bank-all the reward chest's left click in the arena, so that one click banks the loot.",
		position = 2,
		section = lootSection
	)
	default boolean leftClickBankAll()
	{
		return false;
	}

	@ConfigItem(
		keyName = "trackProfit",
		name = "Track profit",
		description = "Adds up each run's profit on the Past tab: the chest you claim, less the supplies used and any death's fee, at Grand Exchange prices.",
		position = 3,
		section = lootSection
	)
	default boolean trackProfit()
	{
		return true;
	}

	/**
	 * The runs' profit since the player last reset it (core.ProfitRecord.toText), kept in each account's own settings
	 * (RuneLite's RuneScape profile), not here: the key only names it.
	 */
	String PROFIT_RECORD = "profitRecord";

	/**
	 * RuneLite's wording for a setting that sends anything to a server it does not run: each such setting is off by
	 * default and shows it as the player switches it on, as RuneLite's plugin guide asks (runelite/example-plugin's
	 * AGENTS.md), so that the Plugin Hub's listing needs no warning of its own.
	 */
	String SHARE_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	/**
	 * The settings that send something to the site, together, so that the player sees they are off by default: first
	 * after the general settings, where the panel's What you share sends the player (the UX review of 2026-10-27, F7).
	 */
	@ConfigSection(
		name = "What you share",
		description = "All off until you switch them on. With them off, the plugin sends nothing.",
		position = 9
	)
	String sendingSection = "sending";

	@ConfigItem(
		keyName = "uploadPastRuns",
		name = "Upload past runs",
		description = "Lets Watch upload a run to the Sol Survivor site to play back: tiles, hitpoints, prayers and hits, with no account name. Anyone with its link can view it for seven days.",
		warning = SHARE_WARNING,
		position = 1,
		section = sendingSection
	)
	default boolean uploadPastRuns()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareWaveLogs",
		name = "Share wave logs",
		description = "Sends each wave's log to the Sol Survivor site as the wave ends, to check and train the planner. No account name or combat levels. Deleted after 90 days, but for leaderboard records.",
		warning = SHARE_WARNING,
		position = 2,
		section = sendingSection
	)
	default boolean shareWaveLogs()
	{
		return false;
	}

	/** Whether the player's waves go on the site's leaderboard (core.BoardEntry); off by default. */
	String LEADERBOARD = "leaderboard";

	/**
	 * The warning RuneLite shows as the player switches the leaderboard on: like SHARE_WARNING, and saying that the name
	 * goes up in public, their RuneScape name unless they typed another (the user's choice, 2026-10-26).
	 */
	String LEADERBOARD_WARNING = "This puts your wave times on the Sol Survivor site's public leaderboard under your RuneScape name, or the Leaderboard name you type, on a 3rd-party server not controlled or verified by RuneLite developers";

	@ConfigItem(
		keyName = LEADERBOARD,
		name = "Leaderboard",
		description = "Puts each wave you finish on the site's public leaderboard under your RuneScape name, or the Leaderboard name below. Anyone can watch those runs.",
		warning = LEADERBOARD_WARNING,
		position = 3,
		section = sendingSection
	)
	default boolean leaderboard()
	{
		return false;
	}

	/** The name the player's times go up under on the leaderboard; blank, their RuneScape name. */
	String LEADERBOARD_NAME = "leaderboardName";

	@ConfigItem(
		keyName = LEADERBOARD_NAME,
		name = "Leaderboard name",
		description = "The name your waves go up under. Blank, your RuneScape name. Up to 12 letters, digits, spaces, hyphens or underscores.",
		position = 4,
		section = sendingSection
	)
	default String leaderboardName()
	{
		return "";
	}

	/**
	 * The player's key on the leaderboard (core.BoardEntry): a random secret made the first time a wave goes up, which keeps
	 * the name and the times the player's and lets Remove my times find them; not shown.
	 */
	String LEADERBOARD_KEY = "leaderboardKey";

	@ConfigItem(
		keyName = LEADERBOARD_KEY,
		name = "",
		description = "",
		hidden = true
	)
	default String leaderboardKey()
	{
		return "";
	}

	@ConfigSection(
		name = "Safe / Fast / Easy",
		description = "Which plan the site suggests when you open a wave from the plugin: the safest, the fastest or the easiest.",
		position = 22,
		closedByDefault = true
	)
	String triangleSection = "triangle";

	/**
	 * Which plan the site suggests for a wave opened from the plugin: the site's own choice, which sends nothing, or one of
	 * the dots of its Safe / Fast / Easy triangle.
	 */
	enum SuggestedPlan
	{
		SITES_OWN("Site's own", null), BALANCED("Balanced", TriangleDot.BALANCED), SAFE("Safe", TriangleDot.SAFE),
		FAST("Fast", TriangleDot.FAST), EASY("Easy", TriangleDot.EASY);

		private final String label;
		private final TriangleDot dot;

		SuggestedPlan(String label, TriangleDot dot)
		{
			this.label = label;
			this.dot = dot;
		}

		/** The dot the links carry; null for the site's own, which carries none. */
		TriangleDot dot()
		{
			return dot;
		}

		/**
		 * The dot the links carry for this choice beside what earlier releases held as a switch and three shares, each as
		 * the settings store it and null when the player never set it. A plan chosen here goes first. With the site's own,
		 * which every install holds until the player chooses, an earlier install that had the switch on keeps its three
		 * shares exactly as it set them (Stefan's rule, 2026-11-12: an existing player's settings are left alone); the switch
		 * off, or every share 0, sent nothing then and sends nothing now.
		 */
		TriangleDot dotWith(String on, String safe, String fast, String easy)
		{
			if (dot != null || !Boolean.parseBoolean(on))
			{
				return dot;
			}
			int s = share(safe);
			int f = share(fast);
			int e = share(easy);
			return s + f + e == 0 ? null : new TriangleDot(s, f, e);
		}

		/** A share as stored, 33 when never set, as its default was; 0 for one that does not read as a number. */
		private static int share(String stored)
		{
			if (stored == null)
			{
				return 33;
			}
			try
			{
				return Math.max(0, Integer.parseInt(stored.trim()));
			}
			catch (NumberFormatException ex)
			{
				return 0;
			}
		}

		@Override
		public String toString()
		{
			return label;
		}
	}

	String SUGGESTED_PLAN = "suggestedPlan";
	/** The switch and the three shares of earlier releases, which no setting shows any more: read, never written. */
	String EARLIER_TRIANGLE = "setTriangle";
	String EARLIER_SAFE = "triangleSafe";
	String EARLIER_FAST = "triangleFast";
	String EARLIER_EASY = "triangleEasy";

	@ConfigItem(
		keyName = SUGGESTED_PLAN,
		name = "Suggested plan",
		description = "Opens every wave from the plugin with the safest, the fastest or the easiest plan suggested, or one that weighs all three alike. Site's own leaves the choice to the site. Shares you set in an earlier version stay in use until you choose here.",
		position = 1,
		section = triangleSection
	)
	default SuggestedPlan suggestedPlan()
	{
		return SuggestedPlan.SITES_OWN;
	}

	@ConfigSection(
		name = "Prayer reliability",
		description = "Counts your protection prayer flicks, so that the site's Prayer reliability can be your own.",
		position = 23,
		closedByDefault = true
	)
	String prayerSection = "prayer";

	/**
	 * Every colour setting in one shut section at the foot (the plugin's UI review of 2026-11-11): they stood in four
	 * sections, two of them nothing but colours, and are the settings fewest players change. Each is named for what it
	 * colours. Their keys are as they were, so a colour a player chose stays.
	 */
	@ConfigSection(
		name = "Colours",
		description = "Every colour the plugin draws in, each named for what it colours.",
		position = 24,
		closedByDefault = true
	)
	String coloursSection = "colours";

	@ConfigItem(
		keyName = "countFlicks",
		name = "Count my flicks",
		description = "Counts your protection prayers on time or late over your newest 60 waves, and puts the totals in the links for the site's Prayer reliability.",
		position = 1,
		section = prayerSection
	)
	default boolean countFlicks()
	{
		return false;
	}

	/** The waves "Count my flicks" counted (core.PrayerRecord.toText), kept here between sessions; not shown. */
	String PRAYER_RECORD = "prayerRecord";

	@ConfigItem(
		keyName = PRAYER_RECORD,
		name = "",
		description = "",
		hidden = true
	)
	default String prayerRecord()
	{
		return "";
	}

	/**
	 * Whether the plugin has had its one chance to set its boxes' first places (idea 363): set on a fresh install's first
	 * login, after placing them for the fixed client if that is what the player plays in, and at once for a player who
	 * had the plugin before. A box the player placed is never moved. Not shown.
	 */
	String LAYOUT_PLACED = "layoutPlaced";

	@ConfigItem(
		keyName = LAYOUT_PLACED,
		name = "",
		description = "",
		hidden = true
	)
	default boolean layoutPlaced()
	{
		return false;
	}

	/**
	 * Whether the panel's Getting started card is done with (the onboarding ideas of 2026-10-27, O1): on Hide, once the
	 * player opened or copied an arena's link, or for a player who had saved waves before the card came; not shown.
	 */
	String GETTING_STARTED_DONE = "gettingStartedDone";

	@ConfigItem(
		keyName = GETTING_STARTED_DONE,
		name = "",
		description = "",
		hidden = true
	)
	default boolean gettingStartedDone()
	{
		return false;
	}

	/**
	 * Whether the plugin has said in chat, the first time the player entered the arena with it, where Arena now is (O3),
	 * or the player had saved waves before the line came; not shown.
	 */
	String ENTERING_SAID = "enteringSaid";

	@ConfigItem(
		keyName = ENTERING_SAID,
		name = "",
		description = "",
		hidden = true
	)
	default boolean enteringSaid()
	{
		return false;
	}

	/**
	 * Whether the plugin has opened its first arena on the site, whose link carries the site's mark of a first link, for
	 * the site's short tour of a plugin player's page (O4), or the player had saved waves before the mark came; not shown.
	 */
	String FIRST_LINK_OPENED = "firstLinkOpened";

	@ConfigItem(
		keyName = FIRST_LINK_OPENED,
		name = "",
		description = "",
		hidden = true
	)
	default boolean firstLinkOpened()
	{
		return false;
	}

	/**
	 * The newest round of settings that New in this update's card has shown (SolSurvivorPanel.showWhatsNew): the card
	 * shows while this is below WHATS_NEW, and Hide sets it to WHATS_NEW. A fresh install has it set at once, so that only
	 * a player who had the plugin before sees the card. Round 1, 2026-11-08: In range look and Your hit on its way. A later
	 * update that adds a setting worth a card raises WHATS_NEW and puts its settings on the card. Not shown.
	 */
	String WHATS_NEW_SEEN = "whatsNewSeen";

	/** The round of settings New in this update holds now: see WHATS_NEW_SEEN. */
	int WHATS_NEW = 1;

	@ConfigItem(
		keyName = WHATS_NEW_SEEN,
		name = "",
		description = "",
		hidden = true
	)
	default int whatsNewSeen()
	{
		return 0;
	}

	/**
	 * Whether the Screen tab's More to switch on card is done with: on its Hide, and at once for a player who had the
	 * plugin before a fresh install came to draw the Planner preset alone (2026-11-11), who keeps what it drew. Not shown.
	 */
	String MORE_HIDDEN = "moreToSwitchOnHidden";

	@ConfigItem(
		keyName = MORE_HIDDEN,
		name = "",
		description = "",
		hidden = true
	)
	default boolean moreToSwitchOnHidden()
	{
		return false;
	}

	/**
	 * Whether this install has been through the change of 2026-11-11, when NPC hitpoints, NPCs in range, Frem and orb
	 * colours, the tick counter, Loot totals and Left-click Bank-all came to start off (QUIET_FROM_INSTALL): a player who
	 * had the plugin before it has each of them still unset written as on, once, so that an update takes nothing off their
	 * screen. Not shown.
	 */
	String DEFAULTS_SETTLED = "quietDefaultsSettled";

	/** The settings that started on until 2026-11-11 and start off since: see DEFAULTS_SETTLED. */
	String[] QUIET_FROM_INSTALL = {"npcHitpoints", "highlightInRange", "npcColours", "tickCounter", "lootTotals", "leftClickBankAll"};

	@ConfigItem(
		keyName = DEFAULTS_SETTLED,
		name = "",
		description = "",
		hidden = true
	)
	default boolean quietDefaultsSettled()
	{
		return false;
	}
}
