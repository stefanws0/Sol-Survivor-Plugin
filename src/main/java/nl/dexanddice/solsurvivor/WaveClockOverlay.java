package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.TitleComponent;
import nl.dexanddice.solsurvivor.core.WaveClock;

/** Shared drawing for the movable clocks: the wave box's background, orange title and white shadowed figures. */
abstract class WaveClockOverlay extends OverlayPanel
{
	protected final SolSurvivorPlugin plugin;
	protected final SolSurvivorConfig config;

	WaveClockOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	protected boolean combined()
	{
		return config.tickCounter() && config.waveTimer() && config.combineWaveTimers();
	}

	protected String ticks(WaveClock.View view)
	{
		return config.tickCounterCounts() == SolSurvivorConfig.TickCounterCounts.LOOP
			? view.loop(config.tickLoopLength()) : view.up();
	}

	/** The wave timer's reading at this frame: real seconds, so it moves between ticks as a clock does. */
	protected static String time(WaveClock.View view)
	{
		return view.time(System.currentTimeMillis());
	}

	/**
	 * The colour of the loop's current position, or null for the plain white look: counting up, with Loop colours off,
	 * or with the counter hidden. Every position has a colour of its own, so no tick stands out from the rest.
	 */
	protected Color loopColour(WaveClock.View view)
	{
		if (config.tickCounterCounts() != SolSurvivorConfig.TickCounterCounts.LOOP
			|| config.loopColours() == SolSurvivorConfig.LoopColours.OFF || !view.counterVisible())
		{
			return null;
		}
		switch (view.loopPosition(config.tickLoopLength()))
		{
			case 1: return config.tickLoopColour1();
			case 2: return config.tickLoopColour2();
			case 3: return config.tickLoopColour3();
			case 4: return config.tickLoopColour4();
			case 5: return config.tickLoopColour5();
			case 6: return config.tickLoopColour6();
			case 7: return config.tickLoopColour7();
			case 8: return config.tickLoopColour8();
			case 9: return config.tickLoopColour9();
			default: return config.tickLoopColour10();
		}
	}

	/** The number's colour: the loop's colour with Number chosen, else white. */
	protected Color numberColour(WaveClock.View view)
	{
		Color colour = loopColour(view);
		return colour != null && config.loopColours() == SolSurvivorConfig.LoopColours.NUMBER ? colour : Color.WHITE;
	}

	/**
	 * With Box chosen, the box takes a darker shade of the loop's colour, so that the white figures stay readable on it;
	 * otherwise RuneLite's standard background.
	 */
	protected void background(WaveClock.View view)
	{
		Color colour = loopColour(view);
		panelComponent.setBackgroundColor(colour != null && config.loopColours() == SolSurvivorConfig.LoopColours.BOX
			? new Color(colour.getRed() / 2, colour.getGreen() / 2, colour.getBlue() / 2, 200)
			: ComponentConstants.STANDARD_BACKGROUND_COLOR);
	}

	protected void title(WaveClock.View view)
	{
		panelComponent.getChildren().add(TitleComponent.builder().text(view.title()).color(new Color(0xff981f)).build());
	}

	protected void number(String value, float size)
	{
		number(value, size, Color.WHITE);
	}

	protected void number(String value, float size, Color colour)
	{
		panelComponent.getChildren().add(new Figures(value, FontManager.getRunescapeBoldFont().deriveFont(size), colour));
	}

	protected Dimension draw(Graphics2D graphics, int width)
	{
		panelComponent.setPreferredSize(new Dimension(width, 0));
		Font was = graphics.getFont();
		graphics.setFont(FontManager.getRunescapeSmallFont());
		try
		{
			return super.render(graphics);
		}
		finally
		{
			graphics.setFont(was);
		}
	}

	/** Centred figures use a common digit advance, so a tick cannot make the number jump sideways. */
	private static final class Figures implements LayoutableRenderableEntity
	{
		private final String text;
		private final Font font;
		private final Color colour;
		private Point at = new Point();
		private Dimension preferred = new Dimension();
		private final Rectangle bounds = new Rectangle();

		private Figures(String text, Font font, Color colour)
		{
			this.text = text;
			this.font = font;
			this.colour = colour;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			Font was = graphics.getFont();
			graphics.setFont(font);
			FontMetrics metrics = graphics.getFontMetrics();
			int digit = 0;
			for (char c = '0'; c <= '9'; c++)
			{
				digit = Math.max(digit, metrics.charWidth(c));
			}
			int textWidth = 0;
			for (char c : text.toCharArray())
			{
				textWidth += Character.isDigit(c) ? digit : metrics.charWidth(c);
			}
			int width = Math.max(preferred.width, textWidth + 2);
			int x = at.x + (width - textWidth) / 2;
			int y = at.y + metrics.getAscent();
			for (char c : text.toCharArray())
			{
				int advance = Character.isDigit(c) ? digit : metrics.charWidth(c);
				int offset = (advance - metrics.charWidth(c)) / 2;
				graphics.setColor(Color.BLACK);
				graphics.drawString(String.valueOf(c), x + offset + 1, y + 1);
				graphics.setColor(colour);
				graphics.drawString(String.valueOf(c), x + offset, y);
				x += advance;
			}
			graphics.setFont(was);
			bounds.setBounds(at.x, at.y, width, metrics.getHeight() + 1);
			return bounds.getSize();
		}

		@Override
		public Rectangle getBounds()
		{
			return bounds;
		}

		@Override
		public void setPreferredLocation(Point location)
		{
			at = location;
		}

		@Override
		public void setPreferredSize(Dimension size)
		{
			preferred = size;
		}
	}
}
