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
		description = "Copies a link of the arena as it is now, without opening the browser, so that the game keeps its focus mid-wave. Paste it into the site.",
		position = 0
	)
	default Keybind copyHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "includeGear",
		name = "Include gear and stats",
		description = "Puts your worn items, weapon, attack style, prayer bonus, prayer points and combat levels in the link, and your gear in the wave log.",
		position = 1
	)
	default boolean includeGear()
	{
		return true;
	}

	@ConfigItem(
		keyName = "sendMarkers",
		name = "Send my tile markers",
		description = "Puts the arena's tiles you marked with RuneLite's Ground Markers, and those of the packs Tile Packs draws, with their colours and labels, in Arena now's link, and the site draws them on its map. A label goes into a short link you make of the page.",
		position = 2
	)
	default boolean sendMarkers()
	{
		return false;
	}

	@ConfigItem(
		keyName = "chatConfirm",
		name = "Messages in chat",
		description = "Puts the plugin's messages in your chat box: what each button did, or why it could not, the hint as you first enter and the leaderboard's answers. Off, they show only in the panel.",
		position = 3
	)
	default boolean chatConfirm()
	{
		return true;
	}

	@ConfigItem(
		keyName = "openOnEntering",
		name = "Open panel on entering",
		description = "Opens this panel as you enter the Fortis Colosseum, and RuneLite's sidebar with it if it is shut. Off, the panel opens only from its sun in the sidebar.",
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
		description = "Shows the plugin's sun in RuneLite's sidebar only in the Fortis Colosseum and its lobby. Past waves are then at hand only there.",
		position = 5
	)
	default boolean onlyAtColosseum()
	{
		return false;
	}

	@ConfigItem(
		keyName = "recordWaveLog",
		name = "Record wave log",
		description = "Keeps the last 25 waves for Past waves: tiles, hitpoints, prayers, attacks, projectiles, hits and optional gear. Nothing uploads unless Upload past runs or Share wave logs is on.",
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
		description = "Saves the waves Record wave log keeps on this computer, in RuneLite's folder for this plugin, each account's apart, so that Past waves still lists them after RuneLite closes. Nothing is sent anywhere. Off, they last until RuneLite closes, and those saved are deleted.",
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

	/** The mistake tracker: the wave report once each wave ends, and which mistakes it counts. */
	@ConfigSection(
		name = "Mistake tracker",
		description = "Counts your mistakes quietly from the wave log, and shows the report in the panel once the wave ends. Nothing shows while a wave is under way.",
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
		keyName = "countPastPrayer",
		name = "Hits past my prayer",
		description = "Counts each attack that met the wrong protection prayer or none, with what it hit for.",
		position = 3,
		section = mistakesSection
	)
	default boolean countPastPrayer()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countLateFlicks",
		name = "Flicks a tick late",
		description = "Counts each attack whose protection prayer came on a tick late or went off a tick early.",
		position = 4,
		section = mistakesSection
	)
	default boolean countLateFlicks()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countSpecials",
		name = "Javelin specials that hit",
		description = "Counts each Javelin Colossus special that landed on you.",
		position = 5,
		section = mistakesSection
	)
	default boolean countSpecials()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countVolatility",
		name = "Volatility blasts that hit",
		description = "Counts each Volatility explosion you stood in.",
		position = 6,
		section = mistakesSection
	)
	default boolean countVolatility()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countSolarflareSand",
		name = "Solarflare and molten sand",
		description = "Counts each hit of Solarflare's orb, and each hit no attack explains while you stood on molten sand.",
		position = 7,
		section = mistakesSection
	)
	default boolean countSolarflareSand()
	{
		return true;
	}

	@ConfigItem(
		keyName = "countIdle",
		name = "Idle ticks",
		description = "Counts each stretch in which you had a living NPC as your target and your weapon ready, but neither attacked, ate nor took a step. Waiting with no target, as on a pillar stack, is not counted.",
		position = 8,
		section = mistakesSection
	)
	default boolean countIdle()
	{
		return true;
	}

	@Range(min = 1, max = 20)
	@Units(Units.TICKS)
	@ConfigItem(
		keyName = "idleFrom",
		name = "Idle from",
		description = "How many idle ticks in a row count as a mistake.",
		position = 9,
		section = mistakesSection
	)
	default int idleFrom()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "deathRecap",
		name = "Death recap",
		description = "After a death, shows in the panel the last ticks before it: your hitpoints, your prayer, and each hit and what dealt it.",
		position = 10,
		section = mistakesSection
	)
	default boolean deathRecap()
	{
		return true;
	}

	/** What the panel lists of the wave under way and the next, and of the modifiers held. */
	@ConfigSection(
		name = "Waves and modifiers",
		description = "What the panel and the wave box list of each wave and of the modifiers you hold: only what is in the arena, no timers.",
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
		name = "On the game screen",
		description = "What the plugin draws on the game's screen, only in the Fortis Colosseum. Switch off any you have from another plugin.",
		position = 12
	)
	String screenSection = "screen";

	/**
	 * RuneLite cannot hide a setting while another is off (checked in 1.13.4's ConfigItem and ConfigPanel), so each
	 * feature with settings of its own has a shut section, its switch first and the settings it needs under it
	 * (Stefan's choice, 2026-10-30). Their switches are on by default as On the game screen's are, but for My attack
	 * range and Hide the pillars.
	 */
	@ConfigSection(
		name = "Modifiers above the chatbox",
		description = "The modifiers you hold in a row of their pictures, above the chatbox.",
		position = 13,
		closedByDefault = true
	)
	String modifiersBarSection = "modifiersBar";

	@ConfigSection(
		name = "Doom",
		description = "Doom's stacks on the game's screen.",
		position = 14,
		closedByDefault = true
	)
	String doomSection = "doom";

	@ConfigSection(
		name = "Attack range",
		description = "Your attack range and the NPCs in it, outlined on the game's screen.",
		position = 15,
		closedByDefault = true
	)
	String rangeSection = "range";

	@ConfigSection(
		name = "Pillars",
		description = "Hiding the arena's four pillars.",
		position = 16,
		closedByDefault = true
	)
	String pillarsSection = "pillars";

	@ConfigSection(
		name = "Tick counter and wave timer",
		description = "A plain tick count and the wave's time, in boxes you move with Alt and drag.",
		position = 17,
		closedByDefault = true
	)
	String clocksSection = "clocks";

	@ConfigItem(
		keyName = "waveBox",
		name = "Wave box",
		description = "A box on the game's screen with the wave's NPCs, the next wave's, or both: the same as the panel's list, no timers. Move it by holding Alt and dragging.",
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
		position = 1,
		section = modifiersBarSection
	)
	default boolean modifiersBar()
	{
		return true;
	}

	@Range(min = 100, max = 300)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "modifiersBarSize",
		name = "Size",
		description = "How big the modifiers' pictures are, from 100% to 300%.",
		position = 2,
		section = modifiersBarSection
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
		name = "Tiers",
		description = "Each modifier's tier in its slot's corner, or its name and tier written under its slot.",
		position = 3,
		section = modifiersBarSection
	)
	default ModifierTiers modifiersBarTiers()
	{
		return ModifierTiers.CORNER;
	}

	/**
	 * Doom's stacks on the game's screen. The key keeps 1.0's name, from when they showed only by the hitpoints orb, so
	 * that a player who switched them on keeps them on; Doom's place now says where they show.
	 */
	@ConfigItem(
		keyName = "doomByOrb",
		name = "Doom stacks",
		description = "Doom's stacks against its limit, as \"4/10\", red on the last stack.",
		position = 1,
		section = doomSection
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
		name = "Doom's place",
		description = "Beside the hitpoints orb, or in a box you move with Alt and drag.",
		position = 2,
		section = doomSection
	)
	default DoomPlace doomPlace()
	{
		return DoomPlace.BY_ORB;
	}

	@ConfigItem(
		keyName = "volatilityReminder",
		name = "Volatility reminder",
		description = "While you hold Volatility, Volatility's picture over each NPC that dies, for 5 ticks, as a reminder that it will explode. It does not mark the blast's area.",
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
		description = "Writes each of Minimus's three offers' names on his screen with the tier taking it brings, such as Frailty III, its tier white for I, blue for II and purple for III.",
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
		description = "Each NPC's hitpoints in a small box over its head, as \"172/250\" with a bar, red at a quarter or less and \"~\" when only the game's health bar gives them.",
		position = 5,
		section = screenSection
	)
	default boolean npcHitpoints()
	{
		return true;
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
		name = "Highlight NPCs in range",
		description = "Outlines the NPCs you can hit from where you stand.",
		position = 2,
		section = rangeSection
	)
	default boolean highlightInRange()
	{
		return true;
	}

	@Alpha
	@ConfigItem(
		keyName = "rangeColour",
		name = "Range colour",
		description = "The colour of the range's outline and of the NPCs in it.",
		position = 3,
		section = rangeSection
	)
	default Color rangeColour()
	{
		return new Color(0xFF, 0xD2, 0x4A, 0xFF);
	}

	String HIDE_PILLARS = "hidePillars";

	@ConfigItem(
		keyName = HIDE_PILLARS,
		name = "Hide the pillars",
		description = "Hides the four pillars and marks their squares on the floor; they still block paths and sight.",
		position = 1,
		section = pillarsSection
	)
	default boolean hidePillars()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenPillarsColour",
		name = "Hidden pillars' colour",
		description = "The colour of each hidden pillar's square; set its opacity to 0 for none.",
		position = 2,
		section = pillarsSection
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
		return true;
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
		name = "Tick counter counts",
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
		name = "Combine them",
		description = "Puts the tick counter and the wave timer in one box.",
		position = 7,
		section = clocksSection
	)
	default boolean combineWaveTimers()
	{
		return true;
	}

	enum CombinedTimerLayout
	{
		STACKED("Stacked"), ROWS("Rows");

		private final String label;

		CombinedTimerLayout(String label)
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
		keyName = "combinedTimerLayout",
		name = "Combined layout",
		description = "Stacked puts a large number over a small one; Rows puts Ticks and Time on two lines.",
		position = 8,
		section = clocksSection
	)
	default CombinedTimerLayout combinedTimerLayout()
	{
		return CombinedTimerLayout.STACKED;
	}

	enum LargeTimerNumber
	{
		TICK("Tick"), TIME("Time");

		private final String label;

		LargeTimerNumber(String label)
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
		keyName = "largeTimerNumber",
		name = "Large number",
		description = "Which number is large in the stacked box.",
		position = 9,
		section = clocksSection
	)
	default LargeTimerNumber largeTimerNumber()
	{
		return LargeTimerNumber.TICK;
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
		description = "With Loop 1 to N, colours each position from Tick loop colours, on the number or the box.",
		position = 4,
		section = clocksSection
	)
	default LoopColours loopColours()
	{
		return LoopColours.OFF;
	}

	/** The colours of the tick counter's loop, one for each position, used only with Loop colours on. */
	@ConfigSection(
		name = "Tick loop colours",
		description = "One colour for each position of the tick counter's loop, used with Loop colours on. Positions past the loop's length are not used.",
		position = 18,
		closedByDefault = true
	)
	String loopColoursSection = "loopColours";

	@ConfigItem(
		keyName = "tickLoopColour1",
		name = "Position 1",
		description = "The colour of the loop's position 1, with Loop colours on.",
		position = 1,
		section = loopColoursSection
	)
	default Color tickLoopColour1()
	{
		return new Color(0x5ce1e6);
	}

	@ConfigItem(
		keyName = "tickLoopColour2",
		name = "Position 2",
		description = "The colour of the loop's position 2, with Loop colours on.",
		position = 2,
		section = loopColoursSection
	)
	default Color tickLoopColour2()
	{
		return new Color(0x7bd96b);
	}

	@ConfigItem(
		keyName = "tickLoopColour3",
		name = "Position 3",
		description = "The colour of the loop's position 3, with Loop colours on.",
		position = 3,
		section = loopColoursSection
	)
	default Color tickLoopColour3()
	{
		return new Color(0xf2d45c);
	}

	@ConfigItem(
		keyName = "tickLoopColour4",
		name = "Position 4",
		description = "The colour of the loop's position 4, with Loop colours on.",
		position = 4,
		section = loopColoursSection
	)
	default Color tickLoopColour4()
	{
		return new Color(0xff9a3d);
	}

	@ConfigItem(
		keyName = "tickLoopColour5",
		name = "Position 5",
		description = "The colour of the loop's position 5, with Loop colours on.",
		position = 5,
		section = loopColoursSection
	)
	default Color tickLoopColour5()
	{
		return new Color(0xe57ad9);
	}

	@ConfigItem(
		keyName = "tickLoopColour6",
		name = "Position 6",
		description = "The colour of the loop's position 6, with Loop colours on.",
		position = 6,
		section = loopColoursSection
	)
	default Color tickLoopColour6()
	{
		return new Color(0x6a9cff);
	}

	@ConfigItem(
		keyName = "tickLoopColour7",
		name = "Position 7",
		description = "The colour of the loop's position 7, with Loop colours on.",
		position = 7,
		section = loopColoursSection
	)
	default Color tickLoopColour7()
	{
		return new Color(0xff6b6b);
	}

	@ConfigItem(
		keyName = "tickLoopColour8",
		name = "Position 8",
		description = "The colour of the loop's position 8, with Loop colours on.",
		position = 8,
		section = loopColoursSection
	)
	default Color tickLoopColour8()
	{
		return new Color(0xc8f26b);
	}

	@ConfigItem(
		keyName = "tickLoopColour9",
		name = "Position 9",
		description = "The colour of the loop's position 9, with Loop colours on.",
		position = 9,
		section = loopColoursSection
	)
	default Color tickLoopColour9()
	{
		return new Color(0xff9ecb);
	}

	@ConfigItem(
		keyName = "tickLoopColour10",
		name = "Position 10",
		description = "The colour of the loop's position 10, with Loop colours on.",
		position = 10,
		section = loopColoursSection
	)
	default Color tickLoopColour10()
	{
		return new Color(0xb39dff);
	}

	/** Minimus's loot: what it adds up to, and banking it in one click. */
	@ConfigSection(
		name = "Loot",
		description = "Minimus's loot between waves and the reward chest at the end.",
		position = 19
	)
	String lootSection = "loot";

	@ConfigItem(
		keyName = "lootTotals",
		name = "Loot totals",
		description = "On Minimus's screen between waves, the value of all the loot so far under the last wave's, and what it comes to with the next wave's.",
		position = 1,
		section = lootSection
	)
	default boolean lootTotals()
	{
		return true;
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
		return true;
	}

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
		name = "Sending to the site",
		description = "Off until you switch them on. With Upload past runs and Share wave logs off, the plugin sends nothing to the Sol Survivor site.",
		position = 9
	)
	String sendingSection = "sending";

	@ConfigItem(
		keyName = "uploadPastRuns",
		name = "Upload past runs",
		description = "Lets a past run's Watch button upload the run to the Sol Survivor site, which opens it in your browser to play back: tiles, hitpoints, prayers, hits and your gear with Include gear and stats on, with no account name. Anyone with its link can view it for seven days.",
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
		description = "Sends each wave's log to the Sol Survivor site as the wave ends, so that the planner can be checked against real games, and its Learned ranking trained on how waves start. No account name or combat levels; your gear only with Include gear and stats on. Shown on the site only on its leaderboard, with Leaderboard on, and deleted after 90 days but for the leaderboard's records.",
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
		description = "Puts each wave you finish on the Sol Survivor site's leaderboard, timed by the game's own wave clock, where anyone can see it and watch the run: under your RuneScape name, or the Leaderboard name below.",
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
		description = "The name your waves go up under with Leaderboard on. Blank, your RuneScape name. Up to 12 letters, digits, spaces, hyphens or underscores.",
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
		description = "Where the site's Safe / Fast / Easy triangle starts when you open a wave from the plugin.",
		position = 20,
		closedByDefault = true
	)
	String triangleSection = "triangle";

	@ConfigItem(
		keyName = "setTriangle",
		name = "Set the triangle",
		description = "Opens every wave from the plugin with the triangle's dot where Safe, Fast and Easy below put it, and the plan it suggests. Off, the site keeps the dot where you last left it.",
		position = 1,
		section = triangleSection
	)
	default boolean setTriangle()
	{
		return false;
	}

	@Range(max = TriangleDot.MAX_SHARE)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "triangleSafe",
		name = "Safe",
		description = "How much the plan suggested minds the hits you take. Copy the three from the site, as it reads them out under the triangle; they need not add up to 100, as each counts as its share of their total.",
		position = 2,
		section = triangleSection
	)
	default int triangleSafe()
	{
		return 33;
	}

	@Range(max = TriangleDot.MAX_SHARE)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "triangleFast",
		name = "Fast",
		description = "How much the plan suggested minds the ticks the wave takes.",
		position = 3,
		section = triangleSection
	)
	default int triangleFast()
	{
		return 33;
	}

	@Range(max = TriangleDot.MAX_SHARE)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "triangleEasy",
		name = "Easy",
		description = "How much the plan suggested minds the inputs: prayer switches, clicks and tiles walked.",
		position = 4,
		section = triangleSection
	)
	default int triangleEasy()
	{
		return 33;
	}

	@ConfigSection(
		name = "Prayer reliability",
		description = "Counts your protection prayer flicks, so that the site's Prayer reliability can be your own.",
		position = 21,
		closedByDefault = true
	)
	String prayerSection = "prayer";

	@ConfigItem(
		keyName = "countFlicks",
		name = "Count my flicks",
		description = "Counts each wave's protection prayers against the attacks, on time or late, and puts the newest 60 waves' totals in the links, so that the site's Prayer reliability opens on Mine: as reliable as your flicks show, kept on the safe side.",
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
}
