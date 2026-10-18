package nl.dexanddice.solsurvivor.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The player's gear and stats as the game shows them (design spec, "Equipment"): each worn item by its slot, with its id,
 * name and quantity; the weapon's attack speed in ticks, its category and the attack style picked, as the game numbers
 * them; the items in the inventory that can be worn, the switches a player brings; each item's attack and strength bonuses; the gear's prayer bonus; the prayer points now; and each combat skill's boosted and real level. The site maps
 * them to its kill table; the plugin passes on what the game says and works nothing out.
 */
public final class Equipment
{
	/** A worn item. */
	public static final class Item
	{
		private final int id;
		private final String name;
		private final int quantity;

		public Item(int id, String name, int quantity)
		{
			this.id = id;
			this.name = name;
			this.quantity = quantity;
		}

		public int getId()
		{
			return id;
		}

		public String getName()
		{
			return name;
		}

		public int getQuantity()
		{
			return quantity;
		}
	}

	/** An item in the inventory that can be worn, the slot it goes in, and a weapon's attack speed in ticks (0 unknown). */
	public static final class Switch
	{
		private final String slot;
		private final Item item;
		private final int speed;

		public Switch(String slot, Item item, int speed)
		{
			this.slot = slot;
			this.item = item;
			this.speed = speed;
		}

		public int getSpeed()
		{
			return speed;
		}

		public String getSlot()
		{
			return slot;
		}

		public Item getItem()
		{
			return item;
		}
	}

	/** The numbers an item's bonuses hold (bonuses). */
	public static final int BONUS_COUNT = 9;

	/** The most switches kept: the inventory's 28 places. */
	public static final int MAX_SWITCHES = 28;

	private final Map<String, Item> items = new LinkedHashMap<>();
	private final List<Switch> switches = new ArrayList<>();
	private final Map<String, int[]> stats = new LinkedHashMap<>();
	private final Map<Integer, int[]> bonuses = new LinkedHashMap<>();
	private int weaponSpeed;
	private int weaponCategory;
	private int attackStyle;
	private final List<String> styleNames = new ArrayList<>();
	private int prayerBonus;
	private int prayerPoints;

	/** An item worn in a slot, by the slot's name in lower case, such as "weapon" or "ammo". */
	public Equipment item(String slot, Item item)
	{
		items.put(slot, item);
		return this;
	}

	/**
	 * An item in the inventory that can be worn, by the slot it goes in, in the inventory's order; past MAX_SWITCHES it is
	 * passed over.
	 */
	public Equipment switchItem(String slot, Item item)
	{
		return switchItem(slot, item, 0);
	}

	/** A weapon in the inventory, with its attack speed in ticks (0 unknown). */
	public Equipment switchItem(String slot, Item item, int speed)
	{
		if (switches.size() < MAX_SWITCHES)
		{
			switches.add(new Switch(slot, item, speed));
		}
		return this;
	}

	/**
	 * An item's bonuses, by its id: stab, slash, crush, magic and ranged attack, melee and ranged strength, magic damage
	 * in tenths of a percent, and 1 when it takes both hands, else 0. The site works out kill times from them.
	 */
	public Equipment bonuses(int id, int[] values)
	{
		if (values.length != BONUS_COUNT)
		{
			throw new IllegalArgumentException("expected " + BONUS_COUNT + " bonuses, not " + values.length);
		}
		bonuses.put(id, values.clone());
		return this;
	}

	/** A skill's boosted and real level, by the skill's name in lower case, such as "ranged". */
	public Equipment stat(String skill, int boosted, int real)
	{
		stats.put(skill, new int[]{boosted, real});
		return this;
	}

	/** The weapon's attack speed in ticks (0 when unknown), its category varbit and the attack style varp. */
	public Equipment weapon(int speed, int category, int style)
	{
		this.weaponSpeed = speed;
		this.weaponCategory = category;
		this.attackStyle = style;
		return this;
	}

	/**
	 * The names the game's data gives the weapon's combat options, in attackStyle's order ("Ranging", "Longrange", and
	 * "Other" for an empty place), which tell the site which style is picked.
	 */
	public Equipment styleNames(List<String> names)
	{
		styleNames.clear();
		styleNames.addAll(names);
		return this;
	}

	/** The gear's prayer bonus, summed over the worn items, and the prayer points now. */
	public Equipment prayer(int bonus, int points)
	{
		this.prayerBonus = bonus;
		this.prayerPoints = points;
		return this;
	}

	public Map<String, Item> getItems()
	{
		return Collections.unmodifiableMap(items);
	}

	public List<Switch> getSwitches()
	{
		return Collections.unmodifiableList(switches);
	}

	public Map<Integer, int[]> getBonuses()
	{
		return Collections.unmodifiableMap(bonuses);
	}

	public Map<String, int[]> getStats()
	{
		return Collections.unmodifiableMap(stats);
	}

	public int getWeaponSpeed()
	{
		return weaponSpeed;
	}

	public int getWeaponCategory()
	{
		return weaponCategory;
	}

	public int getAttackStyle()
	{
		return attackStyle;
	}

	public List<String> getStyleNames()
	{
		return Collections.unmodifiableList(styleNames);
	}

	public int getPrayerBonus()
	{
		return prayerBonus;
	}

	public int getPrayerPoints()
	{
		return prayerPoints;
	}
}
