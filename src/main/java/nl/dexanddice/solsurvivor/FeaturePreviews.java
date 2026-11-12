package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import net.runelite.client.ui.FontManager;

/**
 * The pictures on the Screen tab's More to switch on card (Stefan's ask of 2026-11-11): one for each feature a fresh
 * install starts with off, a sketch of how it looks in the game, so that a player can choose it before a wave shows it.
 * Each is a patch of the arena's floor with what the feature draws on it, in the feature's own colours and the game's
 * lettering, drawn without smoothing as FeatureIcons' are. They show the default colours, whatever the settings hold.
 */
final class FeaturePreviews
{
	static final int WIDTH = 84;
	static final int HEIGHT = 44;

	private static final Color INK = new Color(0x0e0c09);
	private static final Color FLOOR = new Color(0x4a3d2c);
	private static final Color SEAM = new Color(0x41352a);
	private static final Color TAG = new Color(0x1a1612);
	private static final Color SCROLL = new Color(0x3e3529);
	private static final int TILE = 14;

	private FeaturePreviews()
	{
	}

	private interface Painter
	{
		void paint(Graphics2D g);
	}

	/** The picture for In the game's feature `key`, or null for one that has none. */
	static ImageIcon of(String key)
	{
		switch (key)
		{
			case "npcHitpoints":
				// A Manticore with its tag over its head: the hitpoints left of its whole, and the bar under them.
				return draw(true, g ->
				{
					face(g, "npc-manticore", 31, 20, 22);
					int w = text(g, "172/250", -1, 0, null) + 8;
					int x = (WIDTH - w) / 2;
					fill(g, TAG, x, 3, w, 16);
					frame(g, INK, x, 3, w, 16);
					text(g, "172/250", x + 4, 13, Color.WHITE);
					fill(g, new Color(0x5a1610), x + 3, 15, w - 6, 2);
					fill(g, new Color(0x22c32e), x + 3, 15, (w - 6) * 172 / 250, 2);
				});
			case "highlightInRange":
				// Two NPCs, the nearer on a tile ringed in the range's colour and the further left plain.
				return draw(true, g ->
				{
					fill(g, new Color(0x3f, 0xe0, 0xff, 60), 14, 14, 2 * TILE + 1, 2 * TILE + 1);
					frame(g, new Color(0x3fe0ff), 14, 14, 2 * TILE + 1, 2 * TILE + 1);
					frame(g, new Color(0x3fe0ff), 15, 15, 2 * TILE - 1, 2 * TILE - 1);
					face(g, "npc-shaman", 18, 17, 20);
					g.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.55f));
					face(g, "npc-javelin", 58, 3, 20);
				});
			case "npcColours":
				// The warband's three, each ringed in its attack style's colour: ranged, magic and melee.
				return draw(true, g ->
				{
					String[] faces = {"npc-archer", "npc-seer", "npc-berserker"};
					Color[] colours = {new Color(0x3cdc3c), new Color(0x3c8cff), new Color(0xff3c3c)};
					for (int i = 0; i < 3; i++)
					{
						int x = 5 + i * 27;
						face(g, faces[i], x + 2, 13, 18);
						frame(g, colours[i], x, 11, 22, 22);
						frame(g, colours[i], x - 1, 10, 24, 24);
					}
				});
			case "tickCounter":
				// The counter's box with a tick in it, in the game's bold figures.
				return draw(true, g ->
				{
					fill(g, TAG, 27, 9, 30, 26);
					frame(g, INK, 27, 9, 30, 26);
					Font bold = FontManager.getRunescapeBoldFont();
					g.setFont(bold);
					FontMetrics fm = g.getFontMetrics();
					int w = fm.stringWidth("17");
					g.setColor(INK);
					g.drawString("17", 27 + (30 - w) / 2 + 1, 28);
					g.setColor(Color.WHITE);
					g.drawString("17", 27 + (30 - w) / 2, 27);
				});
			case "lootTotals":
				// The two lines on Minimus's screen: what the total is of, and the total in the game's yellow.
				return draw(false, g ->
				{
					fill(g, SCROLL, 0, 0, WIDTH, HEIGHT);
					text(g, LootTotals.NOW + ":", 6, 18, Color.WHITE);
					text(g, "1,240,500 GP", 6, 32, new Color(0xffff00));
				});
			case "leftClickBankAll":
				// The game's line for what a left click does, over the chest: Bank-all first.
				return draw(true, g ->
				{
					int w = text(g, "Bank-all", 4, 13, Color.WHITE);
					text(g, "Chest", 4 + w + 3, 13, new Color(0x00ffff));
					fill(g, new Color(0x6b4a22), 30, 22, 24, 15);
					frame(g, INK, 30, 22, 24, 15);
					fill(g, new Color(0xd8b040), 30, 27, 24, 2);
					fill(g, new Color(0xd8b040), 40, 26, 4, 5);
				});
			default:
				return null;
		}
	}

	/** One picture: the floor with its seams when `floor`, what `p` draws, and a dark edge round it all. */
	private static ImageIcon draw(boolean floor, Painter p)
	{
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g.setFont(FontManager.getRunescapeSmallFont());
		if (floor)
		{
			fill(g, FLOOR, 0, 0, WIDTH, HEIGHT);
			g.setColor(SEAM);
			for (int x = 0; x < WIDTH; x += TILE)
			{
				g.drawLine(x, 0, x, HEIGHT);
			}
			for (int y = 0; y < HEIGHT; y += TILE)
			{
				g.drawLine(0, y, WIDTH, y);
			}
		}
		Graphics2D inner = (Graphics2D) g.create();
		p.paint(inner);
		inner.dispose();
		frame(g, INK, 0, 0, WIDTH, HEIGHT);
		g.dispose();
		return new ImageIcon(image);
	}

	/** An NPC's face `size` pixels a side, its top left at `x`, `y`. */
	private static void face(Graphics2D g, String name, int x, int y, int size)
	{
		Icon icon = Icons.of(name, size);
		icon.paintIcon(null, g, x, y);
	}

	/**
	 * `words` in the graphics' font with the game's shadow under them, their baseline at `y`; with no colour nothing is
	 * drawn. Returns their width.
	 */
	private static int text(Graphics2D g, String words, int x, int y, Color colour)
	{
		if (colour != null)
		{
			g.setColor(INK);
			g.drawString(words, x + 1, y + 1);
			g.setColor(colour);
			g.drawString(words, x, y);
		}
		return g.getFontMetrics().stringWidth(words);
	}

	private static void frame(Graphics2D g, Color c, int x, int y, int w, int h)
	{
		g.setColor(c);
		g.drawRect(x, y, w - 1, h - 1);
	}

	private static void fill(Graphics2D g, Color c, int x, int y, int w, int h)
	{
		g.setColor(c);
		g.fillRect(x, y, w, h);
	}
}
