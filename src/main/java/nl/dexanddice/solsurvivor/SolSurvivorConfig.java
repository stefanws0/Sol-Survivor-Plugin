package nl.dexanddice.solsurvivor;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(SolSurvivorConfig.GROUP)
public interface SolSurvivorConfig extends Config
{
	String GROUP = "solsurvivor";

	@ConfigItem(
		keyName = "siteUrl",
		name = "Site address",
		description = "The Sol Survivor site the links open.",
		position = 0
	)
	default String siteUrl()
	{
		return "https://sol.dexanddice.nl/";
	}

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
		description = "Puts your worn items, weapon, attack style, prayer bonus, prayer points and combat levels in the link.",
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
		description = "Records each wave a tick at a time, your tile, hitpoints, prayers and hitsplats and the NPCs', for the panel to copy for the site's wave reviewer. It is sent nowhere.",
		position = 4
	)
	default boolean recordWaveLog()
	{
		return true;
	}
}
