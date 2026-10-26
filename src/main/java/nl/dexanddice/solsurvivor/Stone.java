package nl.dexanddice.solsurvivor;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * The Sol Survivor site's stone look, painted in Swing for the plugin's own panel: raised rows, inset wells, the game's
 * buttons and the panel's icons, all drawn here so that the plugin carries no image files. The colours are the site's
 * (src/ui/styles/01-base.css), and the rows its Setup tab's (design 1, "Stone rows", of the plugin panel designs of
 * 2026-10-25). Only the panel's own components are painted: RuneLite's look and feel and its sidebar are left as they
 * are.
 */
final class Stone
{
	static final Color BACKGROUND = new Color(0x1b1712);
	static final Color WELL = new Color(0x2b251d);
	static final Color BEVEL_LIGHT = new Color(0x66573f);
	static final Color BEVEL_DARK = new Color(0x211c15);
	static final Color OUTLINE = new Color(0x0e0c09);
	static final Color TEXT = new Color(0xece0c3);
	static final Color MUTED = new Color(0xbcab8b);
	static final Color DIM = new Color(0x8a7b62);
	static final Color ORANGE = new Color(0xff981f);
	static final Color YELLOW = new Color(0xffff00);
	static final Color GOLD = new Color(0xffd24a);
	static final Color GOOD = new Color(0x3cff3c);

	/** A raised thing's face: its gradient from top to bottom, its bevel's light and dark edges, and its words. */
	static final class Face
	{
		final Color top;
		final Color bottom;
		final Color light;
		final Color dark;
		final Color text;

		Face(int top, int bottom, int light, int dark, Color text)
		{
			this.top = new Color(top);
			this.bottom = new Color(bottom);
			this.light = new Color(light);
			this.dark = new Color(dark);
			this.text = text;
		}
	}

	/** A row, as the site's Setup tab has them. */
	static final Face ROW = new Face(0x483d2f, 0x3b3227, 0x66573f, 0x211c15, TEXT);
	/** A plain button, its words in the interface's orange, and the same under the mouse, its words gone yellow. */
	static final Face BUTTON = new Face(0x4e4334, 0x3e3529, 0x66573f, 0x211c15, ORANGE);
	static final Face BUTTON_HOVER = new Face(0x5d503d, 0x463c2e, 0x66573f, 0x211c15, YELLOW);
	/** The one main button of a group, the game's red-brown "accept", its words yellow. */
	static final Face PRIMARY = new Face(0x8a3b17, 0x5e250c, 0xb8622d, 0x3a1606, YELLOW);
	static final Face PRIMARY_HOVER = new Face(0xa14720, 0x6e2c10, 0xb8622d, 0x3a1606, Color.WHITE);
	/** Buttons that cannot be used now go flat, as the game greys out an option it won't take. */
	static final Face OFF = new Face(0x372f24, 0x372f24, 0x463c2e, 0x261f17, DIM);
	static final Face PRIMARY_OFF = new Face(0x50301d, 0x50301d, 0x6a4128, 0x2e1a10, new Color(0x9a8160));

	private Stone()
	{
	}

	/**
	 * A raised face over `w` by `h`, with its outline and bevel, and with `shadow` a two-pixel shadow under it, which the
	 * height takes in. The corners are left out by a pixel, as the site's 4 px radius reads at this size.
	 */
	static void paintRaised(Graphics2D g, int w, int h, Face f, boolean shadow)
	{
		int bh = shadow ? h - 2 : h;
		if (shadow)
		{
			g.setColor(new Color(0, 0, 0, 80));
			g.fillRect(1, 2, w - 2, bh);
		}
		g.setPaint(new GradientPaint(0, 0, f.top, 0, bh, f.bottom));
		g.fillRect(1, 1, w - 2, bh - 2);
		g.setColor(OUTLINE);
		line(g, 1, 0, w - 2, 0);
		line(g, 1, bh - 1, w - 2, bh - 1);
		line(g, 0, 1, 0, bh - 2);
		line(g, w - 1, 1, w - 1, bh - 2);
		g.setColor(f.light);
		line(g, 1, 1, w - 2, 1);
		line(g, 1, 1, 1, bh - 2);
		g.setColor(f.dark);
		line(g, 2, bh - 2, w - 2, bh - 2);
		line(g, w - 2, 2, w - 2, bh - 2);
	}

	/** An inset well over `w` by `h`, as the game's item slots are: dark at its top and left edges, lit at the others. */
	static void paintWell(Graphics2D g, int w, int h)
	{
		g.setColor(WELL);
		g.fillRect(1, 1, w - 2, h - 2);
		g.setColor(OUTLINE);
		line(g, 1, 0, w - 2, 0);
		line(g, 1, h - 1, w - 2, h - 1);
		line(g, 0, 1, 0, h - 2);
		line(g, w - 1, 1, w - 1, h - 2);
		g.setColor(Color.BLACK);
		line(g, 1, 1, w - 2, 1);
		line(g, 1, 1, 1, h - 2);
		g.setColor(BEVEL_LIGHT);
		line(g, 2, h - 2, w - 2, h - 2);
		line(g, w - 2, 2, w - 2, h - 2);
	}

	/**
	 * A line a pixel wide from (x0, y0) to (x1, y1), across or down, both ends in: the pixels drawLine sets at 100%, but
	 * filled as a rectangle, so that under a display's scaling (150% on many Windows laptops) it covers the same share of
	 * the screen as the fills beside it. drawLine keeps its line a whole screen pixel there, which left a well's right and
	 * bottom edges short and the sun in the wave's slot visibly off its centre.
	 */
	private static void line(Graphics2D g, int x0, int y0, int x1, int y1)
	{
		g.fillRect(Math.min(x0, x1), Math.min(y0, y1), Math.abs(x1 - x0) + 1, Math.abs(y1 - y0) + 1);
	}

	/** The outline drawRect draws round (x, y) to (x + w, y + h), drawn as line does, each pixel once. */
	private static void ring(Graphics2D g, int x, int y, int w, int h)
	{
		line(g, x, y, x + w, y);
		line(g, x, y + h, x + w, y + h);
		line(g, x, y + 1, x, y + h - 1);
		line(g, x + w, y + 1, x + w, y + h - 1);
	}

	/** A panel on a raised row, with a shadow under it; `glow` rings it, as a row that wants the player's eye. */
	static class Row extends JPanel
	{
		private Color glow;

		Row(LayoutManager layout, int top, int left, int bottom, int right)
		{
			super(layout);
			setOpaque(false);
			setBorder(new EmptyBorder(top, left, bottom + 2, right));
		}

		void setGlow(Color glow)
		{
			if (!java.util.Objects.equals(this.glow, glow))
			{
				this.glow = glow;
				repaint();
			}
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			paintRaised(g2, getWidth(), getHeight(), ROW, true);
			if (glow != null)
			{
				int bh = getHeight() - 2;
				g2.setColor(glow);
				ring(g2, 0, 0, getWidth() - 1, bh - 1);
				g2.setColor(new Color(glow.getRed(), glow.getGreen(), glow.getBlue(), 90));
				ring(g2, 1, 1, getWidth() - 3, bh - 3);
			}
			g2.dispose();
		}
	}

	/** A panel sunk into an inset well. */
	static class Well extends JPanel
	{
		Well(LayoutManager layout, int top, int left, int bottom, int right)
		{
			super(layout);
			setOpaque(false);
			setBorder(new EmptyBorder(top, left, bottom, right));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			paintWell(g2, getWidth(), getHeight());
			g2.dispose();
		}
	}

	/** Words on one line with the game's one-pixel black shadow under them. */
	static class Shadowed extends JLabel
	{
		Shadowed(String text, Font font, Color color)
		{
			super(text);
			setFont(font);
			setForeground(color);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			String text = getText();
			if (text == null || text.isEmpty())
			{
				return;
			}
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			Insets in = getInsets();
			int y = in.top + (getHeight() - in.top - in.bottom - fm.getHeight()) / 2 + fm.getAscent();
			g2.setColor(Color.BLACK);
			g2.drawString(text, in.left + 1, y + 1);
			g2.setColor(getForeground());
			g2.drawString(text, in.left, y);
			g2.dispose();
		}
	}

	/**
	 * A button drawn as the game's: raised, lighter with its words yellow under the mouse, sunk while it is pressed, and
	 * flat while it cannot be used. Its icon, if any, goes before its words.
	 */
	static class Button extends JButton
	{
		private final Face face;
		private final Face hover;
		private final Face off;
		private final Dimension size;
		private final int padX;
		private final int padY;

		/** A button with words, its size theirs and its icon's with `padX` and `padY` around them. */
		Button(String text, Icon icon, boolean primary, Font font, int padX, int padY)
		{
			this(text, icon, primary, font, padX, padY, null);
		}

		/** A square button with an icon alone, `size` pixels a side. */
		Button(Icon icon, int size)
		{
			this(null, icon, false, null, 0, 0, new Dimension(size, size));
		}

		private Button(String text, Icon icon, boolean primary, Font font, int padX, int padY, Dimension size)
		{
			super(text, icon);
			this.face = primary ? PRIMARY : BUTTON;
			this.hover = primary ? PRIMARY_HOVER : BUTTON_HOVER;
			this.off = primary ? PRIMARY_OFF : OFF;
			this.size = size;
			this.padX = padX;
			this.padY = padY;
			if (font != null)
			{
				setFont(font);
			}
			if (icon instanceof ImageIcon)
			{
				setDisabledIcon(faded((ImageIcon) icon));
			}
			setContentAreaFilled(false);
			setBorderPainted(false);
			setFocusPainted(false);
			setOpaque(false);
			setRolloverEnabled(true);
			setIconTextGap(5);
		}

		@Override
		public Dimension getPreferredSize()
		{
			if (size != null)
			{
				return size;
			}
			FontMetrics fm = getFontMetrics(getFont());
			String t = getText();
			boolean words = t != null && !t.isEmpty();
			Icon i = getIcon();
			int w = (words ? fm.stringWidth(t) : 0) + (i == null ? 0 : i.getIconWidth()) + (words && i != null ? getIconTextGap() : 0);
			int h = Math.max(words ? fm.getAscent() : 0, i == null ? 0 : i.getIconHeight());
			return new Dimension(w + 2 * padX, h + 2 * padY);
		}

		@Override
		public Dimension getMinimumSize()
		{
			return getPreferredSize();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			ButtonModel m = getModel();
			boolean pressed = isEnabled() && m.isArmed() && m.isPressed();
			Face f = !isEnabled() ? off : m.isRollover() ? hover : face;
			if (pressed)
			{
				paintWell(g2, getWidth(), getHeight());
			}
			else
			{
				paintRaised(g2, getWidth(), getHeight(), f, false);
			}
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			String t = getText();
			boolean words = t != null && !t.isEmpty();
			Icon i = isEnabled() ? getIcon() : getDisabledIcon();
			int tw = words ? fm.stringWidth(t) : 0;
			int iw = i == null ? 0 : i.getIconWidth();
			int gap = words && i != null ? getIconTextGap() : 0;
			int x = (getWidth() - tw - iw - gap) / 2 + (pressed ? 1 : 0);
			int dy = pressed ? 1 : 0;
			if (i != null)
			{
				i.paintIcon(this, g2, x, (getHeight() - i.getIconHeight()) / 2 + dy);
			}
			if (words)
			{
				int y = (getHeight() - fm.getAscent()) / 2 + fm.getAscent() - 1 + dy;
				g2.setColor(Color.BLACK);
				g2.drawString(t, x + iw + gap + 1, y + 1);
				g2.setColor(pressed ? YELLOW : f.text);
				g2.drawString(t, x + iw + gap, y);
			}
			g2.dispose();
		}
	}

	/** A square slot sunk into the row, holding an icon, or the wave's number in the values' yellow. */
	static class Slot extends JComponent
	{
		private final int size;
		private Icon icon;
		private String number;

		Slot(int size, Icon icon)
		{
			this.size = size;
			this.icon = icon;
			Dimension d = new Dimension(size, size);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
		}

		/** Shows `number` in the slot, or the icon while it is null. */
		void show(Icon icon, String number)
		{
			this.icon = icon;
			this.number = number;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			paintWell(g2, size, size);
			if (number != null)
			{
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
				// The RuneScape font is drawn for 16 px; at 1.5 times that it stays sharp enough for two digits.
				g2.setFont(getFont().deriveFont(size >= 30 ? 24f : 16f));
				FontMetrics fm = g2.getFontMetrics();
				int x = (size - fm.stringWidth(number)) / 2;
				int y = (size - fm.getAscent()) / 2 + fm.getAscent() - 1;
				g2.setColor(Color.BLACK);
				g2.drawString(number, x + 1, y + 1);
				g2.setColor(YELLOW);
				g2.drawString(number, x, y);
			}
			else if (icon != null)
			{
				icon.paintIcon(this, g2, (size - icon.getIconWidth()) / 2, (size - icon.getIconHeight()) / 2);
			}
			g2.dispose();
		}
	}

	static ImageIcon faded(ImageIcon icon)
	{
		BufferedImage out = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = out.createGraphics();
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
		g.drawImage(icon.getImage(), 0, 0, null);
		g.dispose();
		return new ImageIcon(out);
	}

	/** Draws an icon `size` pixels a side. */
	private interface Painter
	{
		void paint(Graphics2D g, int n);
	}

	private static ImageIcon draw(int size, Painter p)
	{
		BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		p.paint(g, size);
		g.dispose();
		return new ImageIcon(image);
	}

	/** Sol's sun: a disc lit from the middle, with sixteen rays, long and short by turns. */
	static ImageIcon sun(int size)
	{
		return draw(size, (g, n) ->
		{
			double c = n / 2.0;
			double r1 = n * 0.30;
			for (int i = 0; i < 16; i++)
			{
				double a = Math.PI * i / 8;
				double r2 = n * (i % 2 == 0 ? 0.50 : 0.42);
				double half = n * 0.07 / r1 * 1.6;
				Polygon ray = new Polygon();
				ray.addPoint((int) Math.round(c + r1 * Math.cos(a - half)), (int) Math.round(c + r1 * Math.sin(a - half)));
				ray.addPoint((int) Math.round(c + r2 * Math.cos(a)), (int) Math.round(c + r2 * Math.sin(a)));
				ray.addPoint((int) Math.round(c + r1 * Math.cos(a + half)), (int) Math.round(c + r1 * Math.sin(a + half)));
				g.setColor(i % 2 == 0 ? new Color(0xff7a00) : new Color(0xd94a00));
				g.fillPolygon(ray);
			}
			float r = (float) (n * 0.31);
			g.setPaint(new RadialGradientPaint(new Point2D.Double(c - n * 0.06, c - n * 0.06), r, new float[]{0f, 0.45f, 1f},
				new Color[]{new Color(0xfffbe0), GOLD, new Color(0xff7a00)}));
			Ellipse2D disc = new Ellipse2D.Double(c - r, c - r, 2 * r, 2 * r);
			g.fill(disc);
			g.setColor(new Color(0x7a2a00));
			g.setStroke(new BasicStroke(Math.max(1f, n / 24f)));
			g.draw(disc);
		});
	}

	/** A play mark: open the wave at its start. */
	static ImageIcon play(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color);
			g.fillPolygon(new int[]{n / 4, n - n / 6, n / 4}, new int[]{n / 8, n / 2, n - n / 8}, 3);
		});
	}

	/** Two figures, one behind the other: open the wave where its reinforcements came. */
	static ImageIcon figures(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color.darker());
			g.fillOval(n * 7 / 12, n / 12, n / 3, n / 3);
			g.fillRoundRect(n / 2, n * 5 / 12, n / 2, n / 2 + 1, n / 4, n / 4);
			g.setColor(color);
			g.fillOval(n / 12, n / 6, n / 3, n / 3);
			g.fillRoundRect(0, n / 2, n / 2, n / 2, n / 4, n / 4);
		});
	}

	/** Two sheets, the one behind showing only its top and right edges: copy. */
	static ImageIcon copy(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			int w = n * 7 / 12;
			int h = n * 2 / 3;
			int o = Math.max(3, n / 4);
			g.setColor(color);
			g.fillRect(o, 0, w, 1);
			g.fillRect(o + w, 0, 1, h + 1);
			g.fillRect(o, 0, 1, o - 1);
			g.fillRect(w + 2, h, o - 1, 1);
			g.fillRect(0, o, w + 1, 1);
			g.fillRect(0, o + h, w + 1, 1);
			g.fillRect(0, o, 1, h);
			g.fillRect(w, o, 1, h);
		});
	}

	/** The arena from above, its round floor and four pillars, as the site draws it: the arena as it is now. */
	static ImageIcon arena(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color);
			g.setStroke(new BasicStroke(Math.max(1.2f, n / 11f)));
			g.drawOval(1, 1, n - 3, n - 3);
			int p = Math.max(2, n / 6);
			int a = n * 3 / 10;
			int b = n - a - p;
			g.fillRect(a, a, p, p);
			g.fillRect(b, a, p, p);
			g.fillRect(a, b, p, p);
			g.fillRect(b, b, p, p);
		});
	}

	/** Three cards fanned out: Minimus's three offers. */
	static ImageIcon cards(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			AffineTransform t = g.getTransform();
			double[] turns = {-0.38, 0, 0.38};
			Color[] faces = {color.darker().darker(), color.darker(), color};
			int w = n * 5 / 12;
			int h = n * 7 / 12;
			for (int i = 0; i < 3; i++)
			{
				g.setTransform(t);
				g.rotate(turns[i], n / 2.0, n * 0.95);
				g.setColor(faces[i]);
				g.fillRoundRect((n - w) / 2, n / 8, w, h, 3, 3);
				g.setColor(BACKGROUND);
				g.drawRoundRect((n - w) / 2, n / 8, w, h, 3, 3);
			}
			g.setTransform(t);
		});
	}

	/** A film's frame with a play mark: watch a run back. */
	static ImageIcon replay(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color);
			g.setStroke(new BasicStroke(Math.max(1.2f, n / 11f)));
			g.drawRoundRect(1, n / 6, n - 3, n * 2 / 3, 3, 3);
			g.fillPolygon(new int[]{n * 5 / 12, n * 3 / 4, n * 5 / 12}, new int[]{n / 3, n / 2, n * 2 / 3}, 3);
		});
	}

	/** A run folded open, pointing down, or shut, pointing right. */
	static ImageIcon caret(int size, Color color, boolean open)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color);
			if (open)
			{
				g.fillPolygon(new int[]{1, n - 1, n / 2}, new int[]{n / 4, n / 4, n - n / 4}, 3);
			}
			else
			{
				g.fillPolygon(new int[]{n / 4, n - n / 4, n / 4}, new int[]{1, n / 2, n - 1}, 3);
			}
		});
	}

	static ImageIcon dot(int size, Color color)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color);
			g.fillOval(n / 4, n / 4, n / 2, n / 2);
		});
	}

	/** A question mark in a ring, which holds the panel's help in its tooltip. */
	static ImageIcon question(int size, Color color, Font font)
	{
		return draw(size, (g, n) ->
		{
			g.setColor(color);
			g.setStroke(new BasicStroke(1.2f));
			g.drawOval(0, 0, n - 1, n - 1);
			g.setFont(font.deriveFont((float) n));
			FontMetrics fm = g.getFontMetrics();
			g.drawString("?", (n - fm.stringWidth("?")) / 2 + 1, n - 2);
		});
	}
}
