package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.swing.Icon;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.SplitComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import nl.dexanddice.solsurvivor.core.WaveRoster;

/**
 * The wave box, a setting that is off by default: the wave's NPCs on the game's screen, the same list as the panel's,
 * each by its face and short name with how many are left, the next wave's, or both, with Doom's stacks against its
 * limit, as the plugin feature designs of 2026-10-26 draw it. A rule splits those at the wave's start from its
 * reinforcements, still to come or come (the user's choice of four designs, 2026-10-26). Only what is in the arena or
 * on its way, as Fortis Colosseum's own box shows it, which offers the same four choices: no timers, and nothing of how
 * to play the wave. It sits at the top left, and moves like any RuneLite box, by holding Alt and dragging.
 */
class WaveBoxOverlay extends OverlayPanel
{
	private static final Color TITLE = new Color(0xff981f);
	private static final Color TEXT = Color.WHITE;
	private static final Color DEAD = new Color(0x8a7b62);
	private static final Color COUNT = new Color(0xffff00);
	private static final Color TO_COME = new Color(0xc8c8c8);
	private static final Color DOOM_LAST = new Color(0xff3c3c);
	/** The rule between a wave's start and its reinforcements, in the gold of the panel's "Then…". */
	private static final Color RULE = new Color(0xffd24a);
	private static final String REINFORCEMENT = "reinforcement";
	/** The faces' size, a line of the box's font high. */
	private static final int FACE = 14;

	/** The faces drawn once, by their icon and alpha: the box is drawn every frame. */
	private final Map<String, BufferedImage> faces = new HashMap<>();

	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;

	@Inject
	WaveBoxOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SolSurvivorConfig.WaveBox mode = config.waveBox();
		WaveView view = plugin.waveView();
		if (mode == SolSurvivorConfig.WaveBox.OFF || !plugin.inArena() || view.wave == null)
		{
			return null;
		}
		boolean any = false;
		if (mode.showsThisWave() && view.underWay && !view.thisWave.isEmpty())
		{
			title("Wave " + view.wave);
			boolean ruled = false;
			for (WaveRoster.Line line : view.thisWave)
			{
				if (line.isDead() && !config.markDead())
				{
					continue;
				}
				if (!ruled && REINFORCEMENT.equals(line.getNote()))
				{
					panelComponent.getChildren().add(new Rule("reinforcements", RULE));
					ruled = true;
				}
				String right = line.isToCome() ? "to come" : line.isDead() ? "0" : String.valueOf(line.getCount());
				Color colour = line.isToCome() ? TO_COME : line.isDead() ? DEAD : TEXT;
				float alpha = line.isDead() ? 0.45f : line.isToCome() ? 0.6f : 1f;
				line(line.getIcon(), alpha, line.getShortName(), colour, right, line.isDead() || line.isToCome() ? colour : COUNT);
			}
			if (view.doomLimit > 0 && config.doomLimit())
			{
				Color colour = view.doomLast() ? DOOM_LAST : TEXT;
				panelComponent.getChildren().add(LineComponent.builder().left("Doom").leftColor(colour)
					.right(view.doomStacks + " of " + view.doomLimit).rightColor(colour).build());
			}
			any = true;
		}
		if (mode.showsNextWave() && view.next != null && !view.nextWave.isEmpty())
		{
			title("Wave " + view.next + " brings");
			for (WaveRoster.Line line : view.nextWave)
			{
				line(line.getIcon(), 1f, line.getShortName(), TEXT, String.valueOf(line.getCount()), COUNT);
			}
			if (!view.nextLater.isEmpty())
			{
				// Who joins later, under the rule and faded, as the wave under way shows its reinforcements still to come.
				panelComponent.getChildren().add(new Rule("reinforcements", RULE));
				for (WaveRoster.Line line : view.nextLater)
				{
					line(line.getIcon(), 0.6f, line.getShortName(), TO_COME, String.valueOf(line.getCount()), TO_COME);
				}
			}
			any = true;
		}
		return any ? super.render(graphics) : null;
	}

	/** A line of the box: the NPC's face, its short name, and on the right its count or "to come". */
	private void line(String icon, float alpha, String name, Color colour, String right, Color rightColour)
	{
		LineComponent words = LineComponent.builder().left(name).leftColor(colour).right(right).rightColor(rightColour).build();
		BufferedImage face = face(icon, alpha);
		if (face == null)
		{
			panelComponent.getChildren().add(words);
			return;
		}
		panelComponent.getChildren().add(SplitComponent.builder().first(new ImageComponent(face)).second(words)
			.orientation(ComponentOrientation.HORIZONTAL).gap(new Point(4, 0)).build());
	}

	private BufferedImage face(String icon, float alpha)
	{
		if (icon == null)
		{
			return null;
		}
		return faces.computeIfAbsent(icon + "@" + alpha, key ->
		{
			Icon drawn = Icons.npc(icon, FACE, alpha);
			BufferedImage image = new BufferedImage(drawn.getIconWidth(), drawn.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			drawn.paintIcon(null, g, 0, 0);
			g.dispose();
			return image;
		});
	}

	private void title(String text)
	{
		panelComponent.getChildren().add(TitleComponent.builder().text(text).color(TITLE).build());
	}

	/**
	 * A rule across the box with a word in its middle, in the box's small font, as RuneLite's own components draw their
	 * words: a black shadow a pixel down and right.
	 */
	private static final class Rule implements LayoutableRenderableEntity
	{
		private final String words;
		private final Color colour;
		private Point at = new Point();
		private Dimension size = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);
		private final Rectangle bounds = new Rectangle();

		Rule(String words, Color colour)
		{
			this.words = words;
			this.colour = colour;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			Font was = graphics.getFont();
			graphics.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics metrics = graphics.getFontMetrics();
			int height = metrics.getHeight();
			int width = size.width;
			int wordsWidth = metrics.stringWidth(words);
			int x = at.x + (width - wordsWidth) / 2;
			int y = at.y + height / 2;
			graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 160));
			graphics.fillRect(at.x, y, x - at.x - 4, 1);
			graphics.fillRect(x + wordsWidth + 4, y, at.x + width - x - wordsWidth - 4, 1);
			graphics.setColor(Color.BLACK);
			graphics.drawString(words, x + 1, at.y + metrics.getAscent() + 1);
			graphics.setColor(colour);
			graphics.drawString(words, x, at.y + metrics.getAscent());
			graphics.setFont(was);
			bounds.setBounds(at.x, at.y, width, height);
			return new Dimension(width, height);
		}

		@Override
		public Rectangle getBounds()
		{
			return bounds;
		}

		@Override
		public void setPreferredLocation(Point position)
		{
			at = position;
		}

		@Override
		public void setPreferredSize(Dimension dimension)
		{
			size = dimension;
		}
	}
}
