package nl.dexanddice.solsurvivor.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Potions by their doses, for the pick link's `potions` and each run wave's `drunk` (PickPayload): the site's Claim the
 * chest or go on weighs whether the brews and prayer restores carried last the next wave (src/viewer/potionSupply.ts). A
 * potion is an item whose name ends in its doses, "Saradomin brew(4)", and counts as that many doses of
 * "Saradomin brew". The plugin sends every such item and the site picks the ones it weighs, so a charged item named the
 * same way, an "Amulet of glory(4)", rides along and is ignored there.
 */
public final class Potions
{
	/** A potion's name: its name, then its doses in brackets, one to four. */
	private static final Pattern DOSES = Pattern.compile("^(.+?)\\s*\\(([1-4])\\)$");

	private Potions()
	{
	}

	/** The doses of each potion among `items` (the inventory, by name), in the order first met. */
	public static Map<String, Integer> doses(List<ChestItem> items)
	{
		Map<String, Integer> doses = new LinkedHashMap<>();
		for (ChestItem item : items)
		{
			Matcher m = DOSES.matcher(item.getName());
			if (m.matches() && item.getQuantity() > 0)
			{
				doses.merge(m.group(1), Integer.parseInt(m.group(2)) * item.getQuantity(), Integer::sum);
			}
		}
		return doses;
	}

	/** The doses of each potion gone from `before` to `after`, in `before`'s order: none for one that did not go down. */
	public static Map<String, Integer> drunk(Map<String, Integer> before, Map<String, Integer> after)
	{
		Map<String, Integer> drunk = new LinkedHashMap<>();
		for (Map.Entry<String, Integer> e : before.entrySet())
		{
			int gone = e.getValue() - after.getOrDefault(e.getKey(), 0);
			if (gone > 0)
			{
				drunk.put(e.getKey(), gone);
			}
		}
		return drunk;
	}
}
