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
		description = "Copies a link of the arena as it is now.",
		position = 1
	)
	default Keybind copyHotkey()
	{
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "includeGear",
		name = "Include gear and stats",
		description = "Puts your worn items, weapon, attack style, prayer bonus, prayer points and combat levels in the link, and your gear in the wave log.",
		position = 2
	)
	default boolean includeGear()
	{
		return true;
	}

	@ConfigItem(
		keyName = "chatConfirm",
		name = "Say so in chat",
		description = "Puts a line in your chat box when a link is copied, or why it could not be.",
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
		name = "Show only at the Colosseum",
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

	/** The mistake tracker: the wave report once each wave ends, and which mistakes it counts. */
	@ConfigSection(
		name = "Mistake tracker",
		description = "Counts your mistakes quietly from the wave log, and shows the report in the panel once the wave ends. Nothing shows while a wave is under way.",
		position = 8
	)
	String mistakesSection = "mistakes";

	String TRACK_MISTAKES = "trackMistakes";

	@ConfigItem(
		keyName = TRACK_MISTAKES,
		name = "Track mistakes",
		description = "Counts them quietly; the report shows in the panel when the wave ends. Needs Record wave log.",
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
		description = "After a death, shows in the panel the last ticks before it: your hitpoints, your prayer, and each hit and what dealt it. Needs Record wave log.",
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
		position = 9
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

	/** What the plugin draws on the game's screen: all of it off until the player switches it on. */
	@ConfigSection(
		name = "On the game screen",
		description = "Everything here draws on the game's screen, so it is all off until you switch it on.",
		position = 10
	)
	String screenSection = "screen";

	@ConfigItem(
		keyName = "waveBox",
		name = "Wave box",
		description = "A box on the game's screen with the wave's NPCs, the next wave's, or both: the same as the panel's list, no timers. Move it by holding Alt and dragging.",
		position = 1,
		section = screenSection
	)
	default WaveBox waveBox()
	{
		return WaveBox.OFF;
	}

	@ConfigItem(
		keyName = "modifiersBar",
		name = "Modifiers above the chatbox",
		description = "The modifiers you hold in a row of their pictures with their tiers, above the chatbox, as the panel shows them. Move it by holding Alt and dragging.",
		position = 2,
		section = screenSection
	)
	default boolean modifiersBar()
	{
		return false;
	}

	@ConfigItem(
		keyName = "doomByOrb",
		name = "Doom by the hitpoints orb",
		description = "Doom's stacks against its limit, as \"4/10\", beside the hitpoints orb, red on the last stack, from the game's own count.",
		position = 3,
		section = screenSection
	)
	default boolean doomByOrb()
	{
		return false;
	}

	@ConfigItem(
		keyName = "volatilityReminder",
		name = "Volatility reminder",
		description = "While you hold Volatility, Volatility's picture over each NPC that dies, for 5 ticks, as a reminder that it will explode. It does not mark the blast's area.",
		position = 4,
		section = screenSection
	)
	default boolean volatilityReminder()
	{
		return false;
	}

	@ConfigItem(
		keyName = "offerNames",
		name = "Names on Minimus's offers",
		description = "Writes each of Minimus's three offers' names on his screen with the tier taking it brings, such as Frailty III, its tier white for I, blue for II and purple for III.",
		position = 5,
		section = screenSection
	)
	default boolean offerNames()
	{
		return false;
	}

	@ConfigItem(
		keyName = "myRange",
		name = "My attack range",
		description = "Outlines on the floor the tiles you can hit from where you stand: your weapon's range, with Myopia's cut, and only those in your line of sight.",
		position = 6,
		section = screenSection
	)
	default boolean myRange()
	{
		return false;
	}

	@ConfigItem(
		keyName = "highlightInRange",
		name = "Highlight NPCs in range",
		description = "Outlines the NPCs you can hit from where you stand, as Attack Ranges and Dynamic line of sight do.",
		position = 7,
		section = screenSection
	)
	default boolean highlightInRange()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "rangeColour",
		name = "Range colour",
		description = "The colour of your range's outline and of the NPCs in it.",
		position = 8,
		section = screenSection
	)
	default Color rangeColour()
	{
		return new Color(0xFF, 0xD2, 0x4A, 0xFF);
	}

	String HIDE_PILLARS = "hidePillars";

	@ConfigItem(
		keyName = HIDE_PILLARS,
		name = "Hide the pillars",
		description = "Stops the game drawing the arena's four pillars, so that you can see past them, and marks where each stands flat on the floor. They still block your path and your line of sight.",
		position = 9,
		section = screenSection
	)
	default boolean hidePillars()
	{
		return false;
	}

	@Alpha
	@ConfigItem(
		keyName = "hiddenPillarsColour",
		name = "Hidden pillars' colour",
		description = "With the pillars hidden, the colour of the square left flat on the floor where each one stands, which you cannot walk onto. Set its opacity to 0 for none.",
		position = 10,
		section = screenSection
	)
	default Color hiddenPillarsColour()
	{
		return new Color(0x1A, 0x12, 0x0C, 0x96);
	}

	/** Minimus's loot: what it adds up to, and banking it in one click. */
	@ConfigSection(
		name = "Loot",
		description = "Minimus's loot between waves and the reward chest at the end. Off until you switch them on.",
		position = 11
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

	/**
	 * RuneLite's wording for a setting that sends anything to a server it does not run: each such setting is off by
	 * default and shows it as the player switches it on, as RuneLite's plugin guide asks (runelite/example-plugin's
	 * AGENTS.md), so that the Plugin Hub's listing needs no warning of its own.
	 */
	String SHARE_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	/** The settings that send something to the site, together, so that the player sees they are off by default. */
	@ConfigSection(
		name = "Sending to the site",
		description = "Off until you switch them on. With Upload past runs and Share wave logs off, the plugin sends nothing to the Sol Survivor site.",
		position = 12
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
		description = "Sends each wave's log to the Sol Survivor site as the wave ends, so that the planner can be checked against real games, and its Learned ranking trained on how waves start. No account name or combat levels; your gear only with Include gear and stats on. Shown on the site only on its leaderboard, with Leaderboard on, and deleted after 90 days but for the leaderboard's records. Needs Record wave log.",
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
		description = "Puts each wave you finish on the Sol Survivor site's leaderboard, timed by the game's own wave clock, where anyone can see it and watch the run: under your RuneScape name, or the Leaderboard name below. Needs Share wave logs.",
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
		position = 12,
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
		position = 13,
		closedByDefault = true
	)
	String prayerSection = "prayer";

	@ConfigItem(
		keyName = "countFlicks",
		name = "Count my flicks",
		description = "Counts each wave's protection prayers against the attacks, on time or late, and puts the newest 60 waves' totals in the links, so that the site's Prayer reliability opens on Mine: as reliable as your flicks show, kept on the safe side. Needs Record wave log. Off, it forgets the waves counted.",
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
