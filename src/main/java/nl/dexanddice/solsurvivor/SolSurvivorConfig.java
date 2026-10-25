package nl.dexanddice.solsurvivor;

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
		keyName = "recordWaveLog",
		name = "Record wave log",
		description = "Keeps the last 25 waves in memory until RuneLite closes: tiles, hitpoints, prayers, attacks, projectiles, hits and optional gear. Nothing uploads unless Upload past runs or Share wave logs is on.",
		position = 4
	)
	default boolean recordWaveLog()
	{
		return true;
	}

	/**
	 * RuneLite's wording for a setting that sends anything to a server it does not run: each such setting is off by
	 * default and shows it as the player switches it on, as RuneLite's plugin guide asks (runelite/example-plugin's
	 * AGENTS.md), so that the Plugin Hub's listing needs no warning of its own.
	 */
	String SHARE_WARNING = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers";

	@ConfigItem(
		keyName = "uploadPastRuns",
		name = "Upload past runs",
		description = "Lets Open past run upload the selected run to the Sol Survivor site, which opens it in your browser to play back: tiles, hitpoints, prayers, hits and your gear with Include gear and stats on, with no account name. Anyone with its link can view it for seven days.",
		warning = SHARE_WARNING,
		position = 5
	)
	default boolean uploadPastRuns()
	{
		return false;
	}

	@ConfigItem(
		keyName = "shareWaveLogs",
		name = "Share wave logs",
		description = "Sends each wave's log to the Sol Survivor site as the wave ends, so that the planner can be checked against real games. No account name or combat levels; your gear only with Include gear and stats on. Never shown on the site, and deleted after 90 days. Needs Record wave log.",
		warning = SHARE_WARNING,
		position = 6
	)
	default boolean shareWaveLogs()
	{
		return false;
	}

	@ConfigSection(
		name = "Safe / Fast / Easy",
		description = "Where the site's Safe / Fast / Easy triangle starts when you open a wave from the plugin.",
		position = 7
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
		position = 8
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
}
