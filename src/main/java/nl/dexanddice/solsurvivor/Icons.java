package nl.dexanddice.solsurvivor;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The panel's pictures, as the plugin feature designs of 2026-10-26 show them: the NPCs' faces, the modifiers and the
 * marks for a death and an idle stretch, the Sol Survivor site's own, which tools/plugin-icons.mjs takes from its
 * src/ui/icons as 48-pixel PNGs; and the protection prayers, the game's own sprites, which the plugin reads from the game
 * as it starts (put) rather than carry a copy of Jagex's art. Each is drawn at the size a row needs and smoothed down,
 * so that a display scaled to 150% or 200% draws it from more pixels than it shows.
 */
final class Icons
{
	private static final Logger log = LoggerFactory.getLogger(Icons.class);
	private static final Map<String, BufferedImage> IMAGES = new HashMap<>();

	private Icons()
	{
	}

	/** The picture by its name, "npc-javelin", "modifier-doom", "prayer-magic"; null when the plugin has none by it. */
	static synchronized BufferedImage image(String name)
	{
		if (IMAGES.containsKey(name))
		{
			return IMAGES.get(name);
		}
		BufferedImage image = null;
		try (InputStream in = Icons.class.getResourceAsStream("icons/" + name + ".png"))
		{
			if (in != null)
			{
				image = halved(ImageIO.read(in));
			}
		}
		catch (IOException ex)
		{
			log.warn("Could not read the icon {}", name, ex);
		}
		IMAGES.put(name, image);
		return image;
	}

	/** A picture the game gives the plugin, such as a prayer's sprite, under its name. */
	static synchronized void put(String name, BufferedImage image)
	{
		IMAGES.put(name, halved(image));
	}

	/**
	 * The picture as an icon `size` pixels a side, or an empty square of that size while the plugin has none by its name:
	 * it is looked up as it is drawn, so that a sprite the game gives later shows on the panel's next paint.
	 */
	static Icon of(String name, int size)
	{
		return new Scaled(name, size, 1f);
	}

	/** The same, at `alpha`: the dead and the reinforcements still to come are drawn faded. */
	static Icon of(String name, int size, float alpha)
	{
		return new Scaled(name, size, alpha);
	}

	/** An NPC's face by the roster's icon name: "javelin", "archer"; "warband" is the three frems' faces together. */
	static Icon npc(String icon, int size, float alpha)
	{
		if ("warband".equals(icon))
		{
			return new Warband(size, alpha);
		}
		return of("npc-" + icon, size, alpha);
	}

	/**
	 * The Fremennik warband's three faces in one square as big as another NPC's face, so that its name lines up with the
	 * rest: the seer's at the top, the archer's and the berserker's under it, each a little over half the square.
	 */
	private static final class Warband implements Icon
	{
		private final int size;
		private final Icon[] faces;

		Warband(int size, float alpha)
		{
			this.size = size;
			int face = (size * 5 + 4) / 8;
			faces = new Icon[] {of("npc-seer", face, alpha), of("npc-archer", face, alpha), of("npc-berserker", face, alpha)};
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			int face = faces[0].getIconWidth();
			faces[0].paintIcon(c, g, x + (size - face) / 2, y);
			faces[1].paintIcon(c, g, x, y + size - face);
			faces[2].paintIcon(c, g, x + size - face, y + size - face);
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}

	/** A modifier's picture, by its key in the state link ("dynamicDuo"), as the site names its files ("dynamic-duo"). */
	static String modifier(String key)
	{
		return "modifier-" + key.replaceAll("([A-Z])", "-$1").toLowerCase(java.util.Locale.ROOT);
	}

	/**
	 * The picture halved while it is 64 pixels or more: the 48-pixel icons are kept as they are, so that the one smoothing
	 * step down to a row's size reads about two source pixels for each it draws, which bilinear filtering does well and one
	 * long step from many more pixels does not.
	 */
	private static BufferedImage halved(BufferedImage image)
	{
		if (image == null)
		{
			return null;
		}
		BufferedImage out = image;
		while (out.getWidth() >= 64)
		{
			int w = out.getWidth() / 2;
			int h = out.getHeight() / 2;
			BufferedImage half = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = half.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(out, 0, 0, w, h, null);
			g.dispose();
			out = half;
		}
		return out;
	}

	/** A picture by its name, drawn `size` pixels a side, smoothed, at an alpha. */
	static final class Scaled implements Icon
	{
		private final String name;
		private final int size;
		private final float alpha;

		Scaled(String name, int size, float alpha)
		{
			this.name = name;
			this.size = size;
			this.alpha = alpha;
		}

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			BufferedImage image = image(name);
			if (image == null)
			{
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			if (alpha < 1f)
			{
				g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			}
			g2.drawImage(image, x, y, size, size, null);
			g2.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return size;
		}

		@Override
		public int getIconHeight()
		{
			return size;
		}
	}
}
