package nl.dexanddice.solsurvivor;

import java.util.List;
import java.util.function.ToIntFunction;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import nl.dexanddice.solsurvivor.core.Modifier;

/**
 * The names of Minimus's three offers, written on his screen under each offer's picture: the modifier's name in white
 * and the tier taking it brings in that tier's colour, "Frailty III", as the panel's pick names them (the user's ask of
 * 2026-10-26, so that Fortis Colosseum's names on the selector can be switched off for these). The game's screen draws
 * the pictures alone; the plugin adds a line of text to each offer's button, which the game drops with the screen.
 */
final class OfferNames
{
	/** The three offers' buttons on Minimus's screen, in the order the selector's script names the offers. */
	private static final int[] BUTTONS = {InterfaceID.ColosseumIntermission2.MOD_BUTTON_1, InterfaceID.ColosseumIntermission2.MOD_BUTTON_2,
		InterfaceID.ColosseumIntermission2.MOD_BUTTON_3};
	/**
	 * The colour of an offer's tier by the tier taking it brings: light blue for I, a deeper blue for II, purple for III.
	 * Neither good nor bad, so that the colour tells the tier and nothing of which offer to take (the user's choice of
	 * 2026-10-26, over green, yellow and red).
	 */
	private static final String[] TIER_COLOURS = {"8cd2ff", "4a8cff", "c070ff"};

	private OfferNames()
	{
	}

	/**
	 * Writes each offer's name on its button, once: the screen's scripts run again as the player hovers and picks, and a
	 * button that has its line already keeps it. `held` gives the tier held of a modifier, 0 for none. On the client
	 * thread.
	 */
	static void write(Client client, List<Modifier> options, ToIntFunction<Modifier> held)
	{
		for (int i = 0; i < BUTTONS.length && i < options.size(); i++)
		{
			Modifier offer = options.get(i);
			Widget button = client.getWidget(BUTTONS[i]);
			if (offer == null || button == null)
			{
				continue;
			}
			String text = label(offer, held.applyAsInt(offer));
			if (has(button, text))
			{
				continue;
			}
			button.createChild(WidgetType.TEXT)
				.setText(text)
				.setTextColor(0xffffff)
				.setTextShadowed(true)
				.setFontId(FontID.PLAIN_11)
				.setSize(0, 0, WidgetSizeMode.MINUS, WidgetSizeMode.MINUS)
				.setPos(0, 2, WidgetPositionMode.ABSOLUTE_CENTER, WidgetPositionMode.ABSOLUTE_BOTTOM)
				.setXTextAlignment(WidgetTextAlignment.CENTER)
				.setYTextAlignment(WidgetTextAlignment.BOTTOM)
				.revalidate();
		}
	}

	/**
	 * "Frailty <col=c070ff>III</col>": the name in the screen's white and the tier taking it brings in that tier's colour;
	 * a modifier without tiers by its name alone.
	 */
	static String label(Modifier offer, int held)
	{
		if (!offer.hasTiers())
		{
			return offer.getName();
		}
		int tier = Math.min(3, held + 1);
		String name = offer.getName();
		String numeral = offer.getName(tier).substring(name.length()).trim();
		return name + " <col=" + TIER_COLOURS[tier - 1] + ">" + numeral + "</col>";
	}

	private static boolean has(Widget button, String text)
	{
		Widget[] children = button.getDynamicChildren();
		if (children == null)
		{
			return false;
		}
		for (Widget child : children)
		{
			if (child != null && child.getType() == WidgetType.TEXT && text.equals(child.getText()))
			{
				return true;
			}
		}
		return false;
	}
}
