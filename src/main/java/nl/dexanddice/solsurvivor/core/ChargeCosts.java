package nl.dexanddice.solsurvivor.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * What one attack uses up inside a charged weapon, estimated (Stefan's ask of 2026-11-04: estimates are fine), from
 * the OSRS Wiki's page for each weapon, said here in our own words: the items a charge is made of and how many of each,
 * by item id, which the RuneLite side prices. Weapons are matched by name, so that an ornament or holy variant counts as
 * its weapon. The RuneLite side counts the attacks (RunProfit.Tracker.onAttack) by how each weapon spends its charges
 * (Counting): a ranged or magic weapon's by its projectiles leaving the player, so that a cast whose target dies before
 * it lands, which shows no hitsplat, still counts (Stefan's friend's Shadow, 2026-11-04); the scythe's by a tick with a
 * hit of the player's that did damage, as it uses a charge only then; the blade of Saeldor's and the toxic staff of the
 * dead's, melee too, by a tick with any hit of the player's, a miss's 0 too.
 *
 * Estimates: the blowpipe's darts assume Ava's assembler or Dizana's quiver keeps four in five, and a dragon dart; its
 * scales are two in three shots. A crystal shard has no Grand Exchange price, so the bow of Faerdhinen's, the crystal
 * bow's and the blade of Saeldor's charge is valued in coins at a shard's worth as the wiki values it, about 131 coins
 * for 100 charges. The toxic staff of the dead uses its scales over time in combat, here about 0.4 an attack.
 */
public final class ChargeCosts
{
	static final int COINS = 995;
	static final int FIRE_RUNE = 554;
	static final int EARTH_RUNE = 557;
	static final int DEATH_RUNE = 560;
	static final int CHAOS_RUNE = 562;
	static final int BLOOD_RUNE = 565;
	static final int SOUL_RUNE = 566;
	static final int DRAGON_DART = 11230;
	static final int ZULRAHS_SCALES = 12934;
	static final int REVENANT_ETHER = 21820;
	static final int VIAL_OF_BLOOD = 22446;
	static final int ANCIENT_ESSENCE = 27616;

	/** How the attacks that use a weapon's charges are counted. */
	public enum Counting
	{
		/** None: the weapon has no charges. */
		NONE,
		/** A projectile from the player, once a tick. */
		PROJECTILE,
		/** A tick with a hit of the player's that did damage. */
		DAMAGE,
		/** A tick with any hit of the player's. */
		HIT
	}

	/** Each weapon's name in lower case, as part of its variants' names, with what one attack uses. */
	private static final Map<String, Map<Integer, Double>> COSTS = new LinkedHashMap<>();

	static
	{
		put("toxic blowpipe", ZULRAHS_SCALES, 2 / 3.0, DRAGON_DART, 0.2);
		put("blazing blowpipe", ZULRAHS_SCALES, 2 / 3.0, DRAGON_DART, 0.2);
		put("trident of the seas", FIRE_RUNE, 5, CHAOS_RUNE, 1, DEATH_RUNE, 1, COINS, 10);
		put("trident of the swamp", FIRE_RUNE, 5, CHAOS_RUNE, 1, DEATH_RUNE, 1, ZULRAHS_SCALES, 1);
		put("sanguinesti staff", BLOOD_RUNE, 3);
		put("tumeken's shadow", SOUL_RUNE, 2, CHAOS_RUNE, 5);
		put("scythe of vitur", BLOOD_RUNE, 2, VIAL_OF_BLOOD, 0.01);
		put("bow of faerdhinen", COINS, 1.31);
		put("crystal bow", COINS, 1.31);
		put("blade of saeldor", COINS, 1.31);
		put("venator bow", ANCIENT_ESSENCE, 1);
		put("accursed sceptre", REVENANT_ETHER, 1);
		put("webweaver bow", REVENANT_ETHER, 1);
		put("warped sceptre", CHAOS_RUNE, 2, EARTH_RUNE, 5);
		put("eye of ayak", DEATH_RUNE, 2, CHAOS_RUNE, 1);
		put("toxic staff of the dead", ZULRAHS_SCALES, 0.4);
	}

	/** How a weapon's attacks are counted, by its name. */
	public static Counting counting(String weapon)
	{
		if (perAttack(weapon).isEmpty())
		{
			return Counting.NONE;
		}
		String name = weapon.toLowerCase(Locale.ROOT);
		if (name.contains("scythe of vitur"))
		{
			return Counting.DAMAGE;
		}
		// The two melee weapons with a charge a swing, hit or miss.
		return name.contains("blade of saeldor") || name.contains("toxic staff of the dead") ? Counting.HIT : Counting.PROJECTILE;
	}

	private ChargeCosts()
	{
	}

	private static void put(String name, double... idThenAmount)
	{
		Map<Integer, Double> each = new LinkedHashMap<>();
		for (int i = 0; i < idThenAmount.length; i += 2)
		{
			each.put((int) idThenAmount[i], idThenAmount[i + 1]);
		}
		COSTS.put(name, Collections.unmodifiableMap(each));
	}

	/** What one attack with the weapon of that name uses up, by item id; nothing for a weapon with no charges. */
	public static Map<Integer, Double> perAttack(String weapon)
	{
		if (weapon == null)
		{
			return Collections.emptyMap();
		}
		String name = weapon.toLowerCase(Locale.ROOT);
		for (Map.Entry<String, Map<Integer, Double>> c : COSTS.entrySet())
		{
			if (name.contains(c.getKey()))
			{
				return c.getValue();
			}
		}
		return Collections.emptyMap();
	}
}
