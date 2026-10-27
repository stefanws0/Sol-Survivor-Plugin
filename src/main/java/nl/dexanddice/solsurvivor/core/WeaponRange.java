package nl.dexanddice.solsurvivor.core;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The player's attack range, for the range the plugin outlines on the floor and the NPCs it marks in it: the weapon
 * worn's range at the combat option picked, Myopia's cut taken off. RuneLite's item data has no range, so it comes from
 * the weapon's name, as the site's does (src/data/weapons.ts and weaponKinds.ts, from the OSRS Wiki): the kill table's
 * weapons by name, and any other by its kind, which its name tells, the shortest of its kind's where they differ, so
 * that the outline never shows a tile the weapon cannot reach. A spell autocast reaches 10.
 *
 * Myopia takes 2, 4 or 6 tiles off every range above 1, to at least 1 (the site's effectiveRange); a spell cast by hand
 * keeps its full range, but the outline is the range of what the player would attack with, the weapon or its autocast.
 */
public final class WeaponRange
{
	/** The most any attack reaches. */
	public static final int MAX = 10;

	/** Myopia, by tier: how many tiles shorter the ranges above 1 are (the site's MYOPIA_RANGE_PENALTY). */
	private static final int[] MYOPIA_CUT = {0, 2, 4, 6};

	/** The kill table's weapons, by name in lower case: their range at Accurate, and at Longrange. */
	private static final Map<String, int[]> NAMED = new HashMap<>();

	/** The kinds of ranged weapon by the words of their names, the first that matches winning, and their range. */
	private static final String[][] RANGED_KINDS = {
		{"chinchompa", "9"},
		{"ballista", "9"},
		{"crossbow", "7"},
		{"crossbows", "7"},
		{"dart", "3"},
		{"knife", "4"},
		{"thrownaxe", "4"},
		{"javelin", "5"},
		{"blowpipe", "5"},
		{"shortbow", "7"},
		{"longbow", "10"},
		{"comp bow", "10"},
	};

	/** Any other ranged weapon: the shortest of the bows of other names but the weakest. */
	private static final int OTHER_RANGED = 6;
	/** A powered staff the list lacks: the trident's, as most of them reach. */
	private static final int POWERED_STAFF = 7;
	private static final int SPELL = 10;
	private static final int HALBERD = 2;
	private static final int MELEE = 1;
	/** Longrange's extra tiles, to at most MAX. */
	private static final int LONGRANGE = 2;

	/** A name's suffix in brackets, such as a charged or imbued item's "(c)" or "(i)". */
	private static final Pattern SUFFIX = Pattern.compile("\\s*\\([^)]*\\)$");

	static
	{
		ranged("twisted bow", 10);
		ranged("bow of faerdhinen", 10);
		NAMED.put("toxic blowpipe", new int[] {5, 7});
		ranged("venator bow", 6);
		ranged("zaryte crossbow", 8);
		ranged("armadyl crossbow", 8);
		ranged("dragon hunter crossbow", 7);
		ranged("magic shortbow (i)", 7);
		ranged("magic shortbow", 7);
		ranged("webweaver bow", 9);
		ranged("eclipse atlatl", 6);
		ranged("scorching bow", 10);
		ranged("hunters' sunlight crossbow", 8);
		ranged("dragon crossbow", 7);
		ranged("dark bow", 10);
		NAMED.put("tumeken's shadow", new int[] {8, 10});
		NAMED.put("sanguinesti staff", new int[] {7, 9});
		NAMED.put("trident of the swamp", new int[] {7, 9});
		NAMED.put("trident of the seas", new int[] {7, 9});
		NAMED.put("eye of ayak", new int[] {6, 8});
		NAMED.put("warped sceptre", new int[] {7, 9});
		NAMED.put("harmonised nightmare staff", new int[] {SPELL, SPELL});
		NAMED.put("blood ancient sceptre", new int[] {SPELL, SPELL});
		NAMED.put("noxious halberd", new int[] {HALBERD, HALBERD});
	}

	private static void ranged(String name, int range)
	{
		NAMED.put(name, new int[] {range, Math.min(MAX, range + LONGRANGE)});
	}

	private WeaponRange()
	{
	}

	/**
	 * The range in tiles, before Myopia, of the weapon worn: `name` is its name, null or "" for none, which punches;
	 * `styleNames` the game's names of its combat options (the plugin's styleNamesOf), and `attackStyle` the option picked
	 * (varp 43), 4 or 5 while a spell is autocast.
	 */
	public static int of(String name, List<String> styleNames, int attackStyle)
	{
		if (attackStyle >= 4)
		{
			return SPELL;
		}
		String style = attackStyle >= 0 && attackStyle < styleNames.size() ? styleNames.get(attackStyle) : null;
		// A bow's Longrange is its fourth option, which a list of three leaves out.
		boolean longrange = "Longrange".equals(style) || (style == null && attackStyle == 3);
		String plain = name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
		int[] named = NAMED.get(plain);
		if (named == null)
		{
			named = NAMED.get(SUFFIX.matcher(plain).replaceAll(""));
		}
		if (named != null)
		{
			return named[longrange ? 1 : 0];
		}
		boolean casting = style != null && style.contains("Casting") || !styleNames.isEmpty() && "Casting".equals(styleNames.get(0));
		if (casting)
		{
			return longrange ? POWERED_STAFF + LONGRANGE : POWERED_STAFF;
		}
		if ("Ranging".equals(style) || longrange)
		{
			int range = rangedKind(plain);
			return longrange ? Math.min(MAX, range + LONGRANGE) : range;
		}
		return plain.matches(".*(^|[^a-z])halberd([^a-z]|$).*") ? HALBERD : MELEE;
	}

	/** A ranged weapon's range by the words of its name, else OTHER_RANGED. */
	private static int rangedKind(String plain)
	{
		for (String[] kind : RANGED_KINDS)
		{
			if (plain.matches(".*(^|[^a-z])" + Pattern.quote(kind[0]) + "([^a-z]|$).*"))
			{
				return Integer.parseInt(kind[1]);
			}
		}
		return OTHER_RANGED;
	}

	/** A range under Myopia at `tier`: ranges above 1 drop by 2, 4 or 6 tiles, to at least 1. */
	public static int underMyopia(int range, int tier)
	{
		if (range <= 1 || tier < 1)
		{
			return range;
		}
		return Math.max(1, range - MYOPIA_CUT[Math.min(3, tier)]);
	}

	/**
	 * Whether a range reaches only the four tiles beside the player, not those across a corner: a melee weapon's 1 does,
	 * where a halberd's 2, or a ranged weapon's that Myopia cut to 1, reaches diagonally too (the site's reachOf).
	 */
	public static boolean cardinalOnly(int rangeBeforeMyopia)
	{
		return rangeBeforeMyopia <= 1;
	}
}
