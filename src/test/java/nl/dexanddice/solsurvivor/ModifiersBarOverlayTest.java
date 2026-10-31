package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import nl.dexanddice.solsurvivor.core.Modifier;
import org.junit.Test;

/**
 * Hover names follow each modifier slot, including after the player moves the bar with Alt and drag, at the size the
 * player chose, and with the names written under the slots.
 */
public class ModifiersBarOverlayTest
{
	@Test
	public void theHoveredModifierNamesItsTierAndGapsHaveNoTooltip() throws IOException
	{
		Map<Modifier, Integer> held = new LinkedHashMap<>();
		held.put(Modifier.FRAILTY, 2);
		held.put(Modifier.DYNAMIC_DUO, 1);
		AtomicBoolean arena = new AtomicBoolean(true);
		SolSurvivorPlugin plugin = new SolSurvivorPlugin()
		{
			@Override
			boolean inArena()
			{
				return arena.get();
			}

			@Override
			WaveView waveView()
			{
				return new WaveView(5, true, Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), null, held, 0, 0);
			}
		};
		SolSurvivorConfig config = config(100);
		AtomicReference<Point> mouse = new AtomicReference<>(new Point(105, 205));
		Client client = (Client) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Client.class},
			(p, m, a) -> "getMouseCanvasPosition".equals(m.getName()) ? mouse.get() : null);
		TooltipManager tooltips = new TooltipManager();
		ModifiersBarOverlay overlay = new ModifiersBarOverlay(plugin, config, client, tooltips);
		draw(overlay, "build/modifiers-bar-100.png");
		overlay.setBounds(new Rectangle(100, 200, 66, 32));
		overlay.onMouseOver();
		assertEquals("Frailty II", tooltips.getTooltips().get(0).getText());
		tooltips.clear();
		mouse.set(new Point(135, 205));
		overlay.onMouseOver();
		assertEquals("Dynamic Duo", tooltips.getTooltips().get(0).getText());
		tooltips.clear();
		mouse.set(new Point(132, 205));
		overlay.onMouseOver();
		assertTrue(tooltips.getTooltips().isEmpty());
		// The same slot after dragging; its old screen position must no longer offer the name.
		overlay.setBounds(new Rectangle(300, 400, 66, 32));
		overlay.onMouseOver();
		assertTrue(tooltips.getTooltips().isEmpty());
		mouse.set(new Point(305, 405));
		overlay.onMouseOver();
		assertEquals("Frailty II", tooltips.getTooltips().get(0).getText());
		tooltips.clear();
		arena.set(false);
		overlay.onMouseOver();
		assertTrue(tooltips.getTooltips().isEmpty());
	}

	@Test
	public void atTwiceTheSizeTheSlotsAndTheBarAreTwiceAsBig() throws IOException
	{
		Map<Modifier, Integer> held = new LinkedHashMap<>();
		held.put(Modifier.FRAILTY, 2);
		held.put(Modifier.DYNAMIC_DUO, 1);
		SolSurvivorPlugin plugin = new SolSurvivorPlugin()
		{
			@Override
			boolean inArena()
			{
				return true;
			}

			@Override
			WaveView waveView()
			{
				return new WaveView(5, true, Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), null, held, 0, 0);
			}
		};
		AtomicReference<Point> mouse = new AtomicReference<>(new Point(160, 250));
		Client client = (Client) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Client.class},
			(p, m, a) -> "getMouseCanvasPosition".equals(m.getName()) ? mouse.get() : null);
		TooltipManager tooltips = new TooltipManager();
		ModifiersBarOverlay overlay = new ModifiersBarOverlay(plugin, config(200), client, tooltips);
		BufferedImage image = new BufferedImage(140, 70, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		try
		{
			assertEquals(new Dimension(132, 64), overlay.render(graphics));
		}
		finally
		{
			graphics.dispose();
		}
		// The picture goes to build/modifiers-bar-200.png, to look at.
		new File("build").mkdirs();
		ImageIO.write(image, "png", new File("build/modifiers-bar-200.png"));
		// At 100% this point, 60 px in, would be past both slots; at 200% it is inside the first.
		overlay.setBounds(new Rectangle(100, 200, 132, 64));
		overlay.onMouseOver();
		assertEquals("Frailty II", tooltips.getTooltips().get(0).getText());
		tooltips.clear();
		mouse.set(new Point(170, 250));
		overlay.onMouseOver();
		assertEquals("Dynamic Duo", tooltips.getTooltips().get(0).getText());
	}

	@Test
	public void withNamesUnderTheSlotsEachSlotIsAsWideAsItsNameAndItsWholeColumnNamesIt() throws IOException
	{
		Map<Modifier, Integer> held = new LinkedHashMap<>();
		held.put(Modifier.FRAILTY, 2);
		held.put(Modifier.DYNAMIC_DUO, 1);
		SolSurvivorPlugin plugin = new SolSurvivorPlugin()
		{
			@Override
			boolean inArena()
			{
				return true;
			}

			@Override
			WaveView waveView()
			{
				return new WaveView(5, true, Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), null, held, 0, 0);
			}
		};
		AtomicReference<Point> mouse = new AtomicReference<>(new Point(0, 0));
		Client client = (Client) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {Client.class},
			(p, m, a) -> "getMouseCanvasPosition".equals(m.getName()) ? mouse.get() : null);
		TooltipManager tooltips = new TooltipManager();
		ModifiersBarOverlay overlay = new ModifiersBarOverlay(plugin, config(100, SolSurvivorConfig.ModifierTiers.NAME_UNDER), client, tooltips);
		Dimension size = draw(overlay, "build/modifiers-bar-names.png");
		// Taller than the slots by a line of words, and wider than two slots, as "Dynamic Duo" is wider than its slot.
		assertTrue(size.height > 32);
		assertTrue(size.width > 66);
		overlay.setBounds(new Rectangle(100, 200, size.width, size.height));
		// The name under a slot names it too, and so does the slot's column beside its picture.
		mouse.set(new Point(100 + size.width - 2, 200 + size.height - 2));
		overlay.onMouseOver();
		assertEquals("Dynamic Duo", tooltips.getTooltips().get(0).getText());
		tooltips.clear();
		mouse.set(new Point(105, 205));
		overlay.onMouseOver();
		assertEquals("Frailty II", tooltips.getTooltips().get(0).getText());
	}

	/** Draws the bar, its picture to `path` under build/ to look at, and gives its size. */
	private static Dimension draw(ModifiersBarOverlay overlay, String path) throws IOException
	{
		BufferedImage image = new BufferedImage(200, 70, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		Dimension size;
		try
		{
			size = overlay.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
		new File("build").mkdirs();
		ImageIO.write(image, "png", new File(path));
		return size;
	}

	/** The settings with the bar switched on at `size` percent, its tiers in the corner. */
	private SolSurvivorConfig config(int size)
	{
		return config(size, SolSurvivorConfig.ModifierTiers.CORNER);
	}

	/** The settings with the bar switched on at `size` percent, its tiers shown as `tiers` says. */
	private SolSurvivorConfig config(int size, SolSurvivorConfig.ModifierTiers tiers)
	{
		return (SolSurvivorConfig) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {SolSurvivorConfig.class},
			(p, m, a) ->
			{
				switch (m.getName())
				{
					case "modifiersBar": return true;
					case "modifiersBarSize": return size;
					case "modifiersBarTiers": return tiers;
					default: return false;
				}
			});
	}
}
