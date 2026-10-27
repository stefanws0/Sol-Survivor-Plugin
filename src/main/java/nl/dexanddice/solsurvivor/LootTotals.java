package nl.dexanddice.solsurvivor;

import java.util.Locale;
import net.runelite.api.Client;
import net.runelite.api.FontID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetPositionMode;
import net.runelite.api.widgets.WidgetSizeMode;
import net.runelite.api.widgets.WidgetTextAlignment;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.util.Text;

/**
 * "Loot totals", a setting that is off by default: two lines on Minimus's screen between waves (the user's ask of
 * 2026-10-26, so that Fortis Colosseum's own totals can be switched off for these). The game values the last wave's loot
 * on the left and the next wave's on the right, and totals all the loot so far only at the foot of its list in the
 * screen's second tab. The plugin writes that total under the left's value, what the player would leave with now, and
 * the total with the next wave's added under the right's, what they would leave with after it, both read from the
 * game's own values, never priced by the plugin. The game drops the lines with the screen.
 */
final class LootTotals
{
	static final String NOW = "Total so far";
	static final String AFTER_NEXT = "With the next wave";
	private static final String GP = " GP";

	private LootTotals()
	{
	}

	/** Writes the two lines once the screen is drawn, or sets them again where they are already. On the client thread. */
	static void write(Client client)
	{
		Widget all = client.getWidget(InterfaceID.ColosseumIntermission2.TAB_CONTENT_2_CONTENT);
		Widget left = client.getWidget(InterfaceID.ColosseumIntermission2.LEFT);
		Widget leftValue = client.getWidget(InterfaceID.ColosseumIntermission2.LEFT_LOOT_VALUE);
		Widget right = client.getWidget(InterfaceID.ColosseumIntermission2.RIGHT);
		Widget rightValue = client.getWidget(InterfaceID.ColosseumIntermission2.RIGHT_LOOT_VALUE);
		if (all == null || left == null || leftValue == null)
		{
			return;
		}
		Long total = lastValue(all);
		if (total == null)
		{
			return;
		}
		put(left, leftValue, label(NOW, total));
		Long next = rightValue == null ? null : value(rightValue.getText());
		if (right != null && next != null)
		{
			put(right, rightValue, label(AFTER_NEXT, total + next));
		}
	}

	/** "Total so far:" over the sum in the game's yellow, as two lines. */
	static String label(String what, long gp)
	{
		return what + ":<br><col=ffff00>" + String.format(Locale.UK, "%,d", gp) + GP + "</col>";
	}

	/** A value as the game writes one, "1,234,567 GP"; null for any other text. */
	static Long value(String text)
	{
		if (text == null)
		{
			return null;
		}
		String plain = Text.removeTags(text).trim();
		if (!plain.endsWith(GP))
		{
			return null;
		}
		String digits = plain.substring(0, plain.length() - GP.length()).replace(",", "").trim();
		if (digits.isEmpty() || !digits.chars().allMatch(Character::isDigit))
		{
			return null;
		}
		try
		{
			return Long.parseLong(digits);
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}

	/** The last value in GP among a widget's children: the game's total, at the foot of its list of the loot. */
	private static Long lastValue(Widget list)
	{
		for (Widget[] children : new Widget[][] {list.getDynamicChildren(), list.getStaticChildren()})
		{
			if (children == null)
			{
				continue;
			}
			for (int i = children.length - 1; i >= 0; i--)
			{
				Long gp = children[i] == null ? null : value(children[i].getText());
				if (gp != null)
				{
					return gp;
				}
			}
		}
		return null;
	}

	/** The line under the value: the one already written there is set again, so that a redraw adds none. */
	private static void put(Widget side, Widget under, String text)
	{
		String prefix = text.substring(0, text.indexOf(':') + 1);
		Widget[] children = side.getDynamicChildren();
		if (children != null)
		{
			for (Widget child : children)
			{
				if (child != null && child.getText() != null && child.getText().startsWith(prefix))
				{
					child.setText(text);
					return;
				}
			}
		}
		side.createChild(WidgetType.TEXT)
			.setText(text)
			.setTextColor(0xffffff)
			.setTextShadowed(true)
			.setFontId(FontID.PLAIN_11)
			.setLineHeight(14)
			.setSize(0, 26, WidgetSizeMode.MINUS, WidgetSizeMode.ABSOLUTE)
			.setPos(0, under.getOriginalY() + 26, WidgetPositionMode.ABSOLUTE_LEFT, WidgetPositionMode.ABSOLUTE_TOP)
			.setXTextAlignment(WidgetTextAlignment.CENTER)
			.setYTextAlignment(WidgetTextAlignment.CENTER)
			.revalidate();
	}
}
