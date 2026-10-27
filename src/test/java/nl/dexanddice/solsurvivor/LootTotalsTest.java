package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import nl.dexanddice.solsurvivor.core.Modifier;
import org.junit.Test;

/** "Loot totals": the game's own values read and added up, written under them once however often the screen redraws. */
public class LootTotalsTest
{
	@Test
	public void theGamesValuesAreRead()
	{
		assertEquals(Long.valueOf(1234567), LootTotals.value("1,234,567 GP"));
		assertEquals(Long.valueOf(12345), LootTotals.value("<col=ff981f>12,345 GP</col>"));
		assertEquals(Long.valueOf(0), LootTotals.value("0 GP"));
		assertNull(LootTotals.value("Loot"));
		assertNull(LootTotals.value(" GP"));
		assertNull(LootTotals.value("about 5 GP"));
		assertNull(LootTotals.value(null));
	}

	@Test
	public void theLinesAreTheTotalNowAndWithTheNextWave()
	{
		assertEquals("Total so far:<br><col=ffff00>1,284,310 GP</col>", LootTotals.label(LootTotals.NOW, 1284310));

		Map<Integer, Widget> screen = new HashMap<>();
		Fake all = new Fake(null, 0);
		all.statics = new Widget[] {new Fake("Sunfire splinters", 0).widget(), new Fake("1,284,310 GP", 0).widget()};
		Fake left = new Fake(null, 0);
		Fake right = new Fake(null, 0);
		screen.put(InterfaceID.ColosseumIntermission2.TAB_CONTENT_2_CONTENT, all.widget());
		screen.put(InterfaceID.ColosseumIntermission2.LEFT, left.widget());
		screen.put(InterfaceID.ColosseumIntermission2.LEFT_LOOT_VALUE, new Fake("96,400 GP", 180).widget());
		screen.put(InterfaceID.ColosseumIntermission2.RIGHT, right.widget());
		screen.put(InterfaceID.ColosseumIntermission2.RIGHT_LOOT_VALUE, new Fake("402,775 GP", 180).widget());
		Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
			(p, m, a) -> "getWidget".equals(m.getName()) ? screen.get((Integer) a[0]) : null);

		LootTotals.write(client);
		LootTotals.write(client);

		assertEquals(1, left.dynamic.size());
		assertEquals("Total so far:<br><col=ffff00>1,284,310 GP</col>", left.dynamic.get(0).getText());
		assertEquals(206, left.dynamic.get(0).getOriginalY());
		assertEquals(1, right.dynamic.size());
		assertEquals("With the next wave:<br><col=ffff00>1,687,085 GP</col>", right.dynamic.get(0).getText());
	}

	@Test
	public void aTierIsWrittenInRomanNumerals()
	{
		assertEquals("II", ModifiersBarOverlay.tierOf(Modifier.FRAILTY, 2));
		assertEquals("", ModifiersBarOverlay.tierOf(Modifier.DYNAMIC_DUO, 1));
	}

	/** A widget that keeps its text, its y and the children created on it, for the calls the totals make. */
	private static final class Fake
	{
		String text;
		int y;
		Widget[] statics;
		final List<Widget> dynamic = new ArrayList<>();
		private Widget self;

		Fake(String text, int y)
		{
			this.text = text;
			this.y = y;
		}

		Widget widget()
		{
			if (self == null)
			{
				self = (Widget) Proxy.newProxyInstance(Widget.class.getClassLoader(), new Class<?>[]{Widget.class}, (p, m, a) ->
				{
					switch (m.getName())
					{
						case "getText":
							return text;
						case "setText":
							text = (String) a[0];
							return p;
						case "getOriginalY":
							return y;
						case "setPos":
							y = (Integer) a[1];
							return p;
						case "getStaticChildren":
							return statics;
						case "getDynamicChildren":
							return dynamic.toArray(new Widget[0]);
						case "createChild":
							Widget child = new Fake(null, 0).widget();
							dynamic.add(child);
							return child;
						case "revalidate":
							return null;
						default:
							return m.getReturnType() == Widget.class ? p : null;
					}
				});
			}
			return self;
		}
	}
}
