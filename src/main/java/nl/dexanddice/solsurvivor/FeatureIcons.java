package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

/**
 * The small drawn icons beside the Screen tab's switches and on New in this update's card (Stefan's pick of 2026-11-08,
 * design B2 of thread "Help first-time plugin installers get set up"): one for each of In the game's features and one for
 * each look of NPCs in range, each a sketch of what the feature draws on the game's screen in its own colours, so that a
 * player can tell the switches apart before seeing them in a wave. They are drawn on a 16 pixel grid with no smoothing,
 * as the game's own icons are, rather than cut from screenshots, which blur at this size.
 */
final class FeatureIcons
{
	static final int SIZE = 16;

	private static final Color INK = new Color(0x0e0c09);
	private static final Color STONE = new Color(0x5b4e3c);
	private static final Color BOX = new Color(0x1e1912);
	private static final Color SAND = new Color(0xb08a55);
	private static final Color CYAN = new Color(0x3fe0ff);
	private static final Color FLESH = new Color(0x8a5a33);
	private static final Color PLAYER = new Color(0x3a6bd0);

	private FeatureIcons()
	{
	}

	/** Draws one 16 pixel icon, crisp: lines and fills fall on whole pixels. */
	private interface Painter
	{
		void paint(Graphics2D g);
	}

	private static ImageIcon draw(Painter p)
	{
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		p.paint(g);
		g.dispose();
		return new ImageIcon(image);
	}

	/** A rectangle's outline one pixel wide, `w` by `h` from its top left. */
	private static void frame(Graphics2D g, Color c, int x, int y, int w, int h)
	{
		g.setColor(c);
		g.drawRect(x, y, w - 1, h - 1);
	}

	/** A filled rectangle. */
	private static void fill(Graphics2D g, Color c, int x, int y, int w, int h)
	{
		g.setColor(c);
		g.fillRect(x, y, w, h);
	}

	/**
	 * The icon for In the game's feature `key` (SolSurvivorPanel.IN_GAME), or null for a key with none: the panel then
	 * shows the name alone.
	 */
	static ImageIcon of(String key)
	{
		switch (key)
		{
			case "waveBox":
				// The wave box: a stone box with an orange title and two lines, each with its yellow count.
				return draw(g ->
				{
					fill(g, STONE, 1, 1, 14, 14);
					frame(g, INK, 1, 1, 14, 14);
					fill(g, Stone.ORANGE, 3, 3, 10, 2);
					fill(g, Stone.TEXT, 3, 7, 6, 1);
					fill(g, Stone.YELLOW, 11, 7, 2, 1);
					fill(g, Stone.TEXT, 3, 10, 6, 1);
					fill(g, Stone.YELLOW, 11, 10, 2, 1);
				});
			case "modifiersBar":
				// The modifiers above the chatbox: two slots side by side, each with its picture.
				return draw(g ->
				{
					fill(g, new Color(0x3b3227), 0, 4, 7, 8);
					frame(g, INK, 0, 4, 7, 8);
					fill(g, Stone.BAD, 2, 6, 3, 4);
					fill(g, new Color(0x3b3227), 9, 4, 7, 8);
					frame(g, INK, 9, 4, 7, 8);
					fill(g, new Color(0x9fd8ff), 11, 6, 3, 4);
					fill(g, Stone.YELLOW, 14, 10, 1, 1);
				});
			case "doomByOrb":
				// Doom's stacks: a dark purple orb with a bar of stacks under it, the last one red.
				return draw(g ->
				{
					g.setColor(new Color(0x5b2a7a));
					g.fillOval(4, 1, 8, 8);
					g.setColor(INK);
					g.drawOval(4, 1, 8, 8);
					fill(g, INK, 1, 11, 14, 4);
					fill(g, new Color(0xb070e0), 2, 12, 9, 2);
					fill(g, Stone.BAD, 11, 12, 3, 2);
				});
			case "volatilityReminder":
				// Volatility's blast: an orange burst with a yellow heart.
				return draw(g ->
				{
					g.setColor(new Color(0xff5a1f));
					g.fillPolygon(new int[]{8, 10, 15, 11, 13, 8, 3, 5, 1, 6}, new int[]{0, 5, 4, 8, 15, 11, 15, 8, 4, 5}, 10);
					fill(g, Stone.YELLOW, 6, 6, 4, 4);
				});
			case "offerNames":
				// Minimus's three offers, each with its tier's colour on top: white, blue and purple.
				return draw(g ->
				{
					Color[] tiers = {Color.WHITE, new Color(0x4aa8ff), new Color(0xc070ff)};
					for (int i = 0; i < 3; i++)
					{
						int x = i * 5 + 1;
						fill(g, STONE, x, 3, 4, 11);
						frame(g, INK, x, 3, 4, 11);
						fill(g, tiers[i], x + 1, 4, 2, 2);
					}
				});
			case "npcHitpoints":
				// A hitpoints tag: two numbers on a dark plate over a green bar, the hit on its way in orange.
				return draw(g ->
				{
					fill(g, INK, 0, 3, 16, 10);
					fill(g, Stone.YELLOW, 2, 5, 5, 3);
					fill(g, Stone.TEXT, 9, 5, 5, 3);
					fill(g, Stone.GOOD, 2, 10, 6, 2);
					fill(g, Stone.ORANGE, 8, 10, 3, 2);
					fill(g, new Color(0xa3120c), 11, 10, 3, 2);
				});
			case "highlightInRange":
				// An NPC standing in a cyan tile.
				return draw(g ->
				{
					fill(g, new Color(0x3fe0ff).darker().darker(), 1, 1, 14, 14);
					frame(g, CYAN, 1, 1, 14, 14);
					frame(g, CYAN, 2, 2, 12, 12);
					fill(g, FLESH, 6, 4, 4, 8);
				});
			case "myRange":
				// The player in the middle, the range's outline dashed round them.
				return draw(g ->
				{
					g.setColor(CYAN);
					g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{2, 2}, 0));
					g.drawRect(1, 1, 13, 13);
					fill(g, PLAYER, 6, 5, 4, 6);
					fill(g, new Color(0xf0c8a0), 7, 3, 2, 2);
				});
			case "npcColours":
				// The warband's three outlines, each in its style's colour: ranged green, magic blue, melee red.
				return draw(g ->
				{
					frame(g, Stone.GOOD, 0, 3, 5, 11);
					frame(g, new Color(0x33a0ff), 5, 3, 5, 11);
					frame(g, Stone.BAD, 11, 3, 5, 11);
				});
			case "tickCounter":
				// A box with a count in it.
				return draw(g ->
				{
					fill(g, BOX, 0, 3, 16, 10);
					frame(g, INK, 0, 3, 16, 10);
					// A "1" and a "7" in white pixels.
					fill(g, Color.WHITE, 4, 5, 1, 6);
					fill(g, Color.WHITE, 3, 6, 1, 1);
					fill(g, Color.WHITE, 8, 5, 4, 1);
					fill(g, Color.WHITE, 11, 6, 1, 2);
					fill(g, Color.WHITE, 10, 8, 1, 3);
				});
			case "waveTimer":
				// A clock's face with its two hands.
				return draw(g ->
				{
					g.setColor(BOX);
					g.fillOval(1, 1, 14, 14);
					g.setColor(Stone.TEXT);
					g.drawOval(1, 1, 13, 13);
					fill(g, Color.WHITE, 7, 4, 2, 5);
					fill(g, Color.WHITE, 9, 7, 3, 2);
				});
			case "hidePillars":
				// A pillar's square on the sand, the pillar itself gone.
				return draw(g ->
				{
					fill(g, SAND, 0, 0, 16, 16);
					frame(g, new Color(0x3a2a18), 3, 3, 10, 10);
					frame(g, new Color(0x3a2a18), 4, 4, 8, 8);
				});
			case "lootTotals":
				// A stack of gold coins.
				return draw(g ->
				{
					for (int i = 0; i < 4; i++)
					{
						fill(g, new Color(0x8a6a00), 3, 12 - i * 3, 10, 3);
						fill(g, Stone.GOLD, 3, 12 - i * 3, 10, 2);
					}
					fill(g, new Color(0xfff2a0), 5, 3, 3, 1);
				});
			case "leftClickBankAll":
				// The reward chest: a wooden box with a gold band and lock.
				return draw(g ->
				{
					fill(g, new Color(0x6b3e1c), 1, 4, 14, 10);
					frame(g, INK, 1, 4, 14, 10);
					fill(g, Stone.GOLD, 2, 8, 12, 1);
					fill(g, Stone.GOLD, 7, 7, 2, 3);
				});
			default:
				return null;
		}
	}

	/** The icon for a look of NPCs in range, a sketch of how it marks an NPC. */
	static ImageIcon look(SolSurvivorConfig.InRangeStyle look)
	{
		switch (look)
		{
			case OUTLINE:
				// The NPC's own outline.
				return draw(g ->
				{
					g.setColor(CYAN);
					g.drawPolyline(new int[]{5, 10, 10, 13, 13, 2, 2, 5, 5}, new int[]{2, 2, 6, 6, 14, 14, 6, 6, 2}, 9);
					fill(g, FLESH, 4, 7, 8, 6);
					fill(g, FLESH, 6, 3, 4, 4);
				});
			case ARROW:
				// An arrow over the NPC's head.
				return draw(g ->
				{
					g.setColor(CYAN);
					g.fillPolygon(new int[]{3, 13, 8}, new int[]{1, 1, 7}, 3);
					g.setColor(INK);
					g.drawPolygon(new int[]{3, 13, 8}, new int[]{1, 1, 7}, 3);
					fill(g, FLESH, 6, 9, 4, 6);
				});
			case BRACKETS:
				// Four corners round the NPC.
				return draw(g ->
				{
					g.setColor(Stone.TEXT);
					int[][] corners = {{1, 1, 1, 1}, {14, 1, -1, 1}, {1, 14, 1, -1}, {14, 14, -1, -1}};
					for (int[] c : corners)
					{
						g.drawLine(c[0], c[1], c[0] + 3 * c[2], c[1]);
						g.drawLine(c[0], c[1], c[0], c[1] + 3 * c[3]);
					}
					fill(g, FLESH, 6, 5, 4, 7);
				});
			case BOLD_TILE:
			default:
				// A bold tile under the NPC, seen at an angle as the game's camera shows it.
				return draw(g ->
				{
					g.setColor(new Color(0x3fe0ff).darker().darker());
					g.fillPolygon(new int[]{1, 8, 15, 8}, new int[]{11, 7, 11, 15}, 4);
					g.setColor(CYAN);
					g.drawPolygon(new int[]{1, 8, 15, 8}, new int[]{11, 7, 11, 15}, 4);
					fill(g, FLESH, 6, 2, 4, 9);
				});
		}
	}
}
