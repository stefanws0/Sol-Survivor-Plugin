package nl.dexanddice.solsurvivor.core;

/**
 * An item of the Colosseum's reward chest as the game holds it between waves: the name the game gives it, noted or not,
 * and how many. The pick link carries the next wave's reward and the chest so far as lists of them (PickPayload's
 * `chest`), so that the site's Claim the chest or go on needs nothing typed. The plugin prices nothing: the site matches
 * each name to its own table of the chest's items, and leaves one it does not know, such as wave 3's Varlamore token,
 * unpriced.
 */
public final class ChestItem
{
	private final String name;
	private final int quantity;

	public ChestItem(String name, int quantity)
	{
		this.name = name;
		this.quantity = quantity;
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
