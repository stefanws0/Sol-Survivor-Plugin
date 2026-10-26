package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.junit.Test;

/**
 * The panel's drawn parts at a display's scaling. Windows scales RuneLite by 125%, 150% or 200% on many laptops, and a
 * line drawn there with drawLine stays a whole screen pixel, so a well's right and bottom edges fell a pixel short of
 * its fill and the sun in the wave's slot sat off its centre (Stefan's screenshot of 2026-10-26).
 */
public class StoneTest
{
	private static final int KEY = Color.MAGENTA.getRGB();

	/** The wave's slot with its sun, painted at `scale` onto magenta, which nothing in it uses. */
	private static BufferedImage slotAt(double scale)
	{
		Stone.Slot slot = new Stone.Slot(34, Stone.sun(24));
		slot.setSize(34, 34);
		int side = (int) Math.ceil(34 * scale) + 2;
		BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.MAGENTA);
		g.fillRect(0, 0, side, side);
		g.scale(scale, scale);
		slot.paintComponent(g);
		g.dispose();
		return image;
	}

	/** The gaps between the slot's edges and its sun's orange: left, right, top and bottom, in screen pixels. */
	private static int[] gaps(BufferedImage image)
	{
		int fx0 = Integer.MAX_VALUE, fx1 = -1, fy0 = Integer.MAX_VALUE, fy1 = -1;
		int sx0 = Integer.MAX_VALUE, sx1 = -1, sy0 = Integer.MAX_VALUE, sy1 = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				int p = image.getRGB(x, y);
				if (p == KEY)
				{
					continue;
				}
				fx0 = Math.min(fx0, x);
				fx1 = Math.max(fx1, x);
				fy0 = Math.min(fy0, y);
				fy1 = Math.max(fy1, y);
				int r = (p >> 16) & 255, b = p & 255;
				if (r > 150 && r - b > 100)
				{
					sx0 = Math.min(sx0, x);
					sx1 = Math.max(sx1, x);
					sy0 = Math.min(sy0, y);
					sy1 = Math.max(sy1, y);
				}
			}
		}
		assertTrue("the slot shows its sun", sx1 >= 0);
		return new int[]{sx0 - fx0, fx1 - sx1, sy0 - fy0, fy1 - sy1};
	}

	@Test
	public void theSunSitsInTheMiddleOfTheWavesSlotAtWholeScalings()
	{
		// 125% is left out: there the 34-pixel slot is 42.5 screen pixels, which no drawing can halve.
		for (double scale : new double[]{1, 1.5, 2})
		{
			int[] g = gaps(slotAt(scale));
			assertEquals("left and right at " + scale, g[0], g[1]);
			assertEquals("top and bottom at " + scale, g[2], g[3]);
		}
	}

	@Test
	public void theToolbarSunIsTheSameMirroredEitherWay()
	{
		BufferedImage icon = SolSurvivorPanel.icon();
		for (int y = 0; y < 16; y++)
		{
			for (int x = 0; x < 16; x++)
			{
				// The sun is set out pixel by pixel, so each pixel is its mirror's colour exactly.
				int p = icon.getRGB(x, y);
				assertEquals("left and right at " + x + ", " + y, p, icon.getRGB(15 - x, y));
				assertEquals("top and bottom at " + x + ", " + y, p, icon.getRGB(x, 15 - y));
			}
		}
		assertEquals("the middle is white", 0xfffffffd, icon.getRGB(7, 7));
		assertEquals("a corner is clear", 0, icon.getRGB(0, 0) >>> 24);
	}
}
