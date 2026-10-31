package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import net.runelite.client.ui.FontManager;
import nl.dexanddice.solsurvivor.core.ColosseumTracker;
import nl.dexanddice.solsurvivor.core.NpcHitpoints;
import nl.dexanddice.solsurvivor.core.NpcKind;
import org.junit.Test;

/**
 * The NPC hitpoints' stone tag drawn off screen in each of its states: the number in yellow when hurt, red at a quarter or fewer,
 * green at full health and a heal in pink. The picture goes to build/npc-hitpoints.png, to look at.
 */
public class NpcHitpointsOverlayTest
{
	private static final int DAMAGE = 16;

	@Test
	public void theTagShowsEachState() throws IOException
	{
		NpcHitpoints hitpoints = new NpcHitpoints();
		hitpoints.onSpawned(1, NpcKind.SHAMAN_ID);
		hitpoints.onSpawned(2, NpcKind.MANTICORE_ID);
		hitpoints.onHit(2, 78, DAMAGE, 1);
		hitpoints.onSpawned(3, NpcKind.MINOTAUR_ID);
		hitpoints.onHit(3, 184, DAMAGE, 1);
		hitpoints.onSpawned(4, NpcKind.MINOTAUR_ID);
		hitpoints.onHit(4, 184, DAMAGE, 1);
		hitpoints.onHit(4, 5, ColosseumTracker.HEAL_HITSPLAT, 2);
		hitpoints.onTick(5, NpcKind.JAVELIN_ID, 21, 30);

		BufferedImage image = new BufferedImage(400, 40, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(0x5e4d35));
		graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
		graphics.setFont(FontManager.getRunescapeSmallFont());
		for (int index = 1; index <= 5; index++)
		{
			NpcHitpointsOverlay.paint(graphics, hitpoints.reading(index, 2), 40 + (index - 1) * 80, 30);
		}
		graphics.dispose();
		new File("build").mkdirs();
		ImageIO.write(image, "png", new File("build/npc-hitpoints.png"));

		assertTrue("green number at full health", has(image, 0, 80, Stone.GOOD));
		assertFalse("no yellow at full health", has(image, 0, 80, Stone.YELLOW));
		assertTrue("yellow number when hurt", has(image, 80, 160, Stone.YELLOW));
		assertFalse("no red on a full shaman", has(image, 0, 80, new Color(0xff3c3c)));
		assertTrue("red when low", has(image, 160, 240, new Color(0xff3c3c)));
		assertTrue("pink heal", has(image, 240, 320, new Color(0xff6fcf)));
	}

	private static boolean has(BufferedImage image, int from, int to, Color colour)
	{
		for (int x = from; x < to; x++)
		{
			for (int y = 0; y < image.getHeight(); y++)
			{
				if ((image.getRGB(x, y) & 0xffffff) == (colour.getRGB() & 0xffffff))
				{
					return true;
				}
			}
		}
		return false;
	}
}
