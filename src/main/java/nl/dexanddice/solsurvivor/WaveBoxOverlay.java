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
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;
import nl.dexanddice.solsurvivor.core.ColosseumTracker;
import nl.dexanddice.solsurvivor.core.WaveRoster;

/**
 * The wave box, a setting that is on by default: the wave's NPCs on the game's screen, the same list as the panel's,
 * the next wave's, or both. Each wave sits under a dark band with its number, and each line reads its count first, then
 * the NPC's face and short name (the user's choice of the 2026-10-31 drafts). The count is coloured by how many, so that
 * a crowd stands out: yellow for one, orange for two, red for three or more, grey for none. Reinforcements still to come
 * read "+1" in the list, with their names grey, rather than under a rule. Doom's stacks are not here: Doom has its own
 * box. Only what is in the arena or on its way, as Fortis Colosseum's own box shows it, which offers the same four
 * choices: no timers, and nothing of how to play the wave. It sits at the top left, and moves like any RuneLite box, by
 * holding Alt and dragging. Each name is tinted by how its NPC attacks, with Names by attack style on (Stefan's request,
 * 2026-11-08): magic, ranged or melee in the colours the player picks, the Manticore and the warband together white, as
 * they use more than one.
 */
class WaveBoxOverlay extends OverlayPanel
{
	private static final Color TITLE = new Color(0xff981f);
	private static final Color TEXT = Color.WHITE;
	private static final Color DEAD = new Color(0xa8a8a8);
	private static final Color TO_COME = new Color(0xd0d0d0);
	private static final Color ONE = new Color(0xffff00);
	private static final Color TWO = new Color(0xff9a2e);
	private static final Color MORE = new Color(0xff5040);
	/** The bands behind the waves' titles, darker than the box. */
	private static final Color BAND = new Color(0, 0, 0, 80);
	private static final String REINFORCEMENT = "reinforcement";
	/** The faces' size, a line of the box's font high. */
	private static final int FACE = 16;
	/** The count's column, wide enough for "+3", right-aligned before the face. */
	private static final int COUNT_WIDTH = 16;
	private static final int GAP = 5;

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
		panelComponent.setGap(new Point(0, 1));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SolSurvivorConfig.WaveBox mode = config.waveBox();
		WaveView view = plugin.waveView();
		if (mode == SolSurvivorConfig.WaveBox.OFF || !plugin.inArena() || view.wave == null || quiet(view.wave, config.quietOnSol()))
		{
			return null;
		}
		boolean any = false;
		if (mode.showsThisWave() && view.underWay && !view.thisWave.isEmpty())
		{
			panelComponent.getChildren().add(new Band("Wave " + view.wave, "now"));
			for (WaveRoster.Line line : view.thisWave)
			{
				if (line.isDead() && !config.markDead())
				{
					continue;
				}
				panelComponent.getChildren().add(new Row(line, line.isToCome(), face(line.getIcon(), alpha(line, line.isToCome())), tint(line)));
			}
			any = true;
		}
		if (mode.showsNextWave() && view.next != null && !view.nextWave.isEmpty())
		{
			if (any)
			{
				panelComponent.getChildren().add(new Space(3));
			}
			panelComponent.getChildren().add(new Band("Wave " + view.next, "next"));
			for (WaveRoster.Line line : view.nextWave)
			{
				panelComponent.getChildren().add(new Row(line, false, face(line.getIcon(), 1f), tint(line)));
			}
			// Who joins later, in the list as "+1" and faded, as the wave under way shows its reinforcements still to come.
			for (WaveRoster.Line line : view.nextLater)
			{
				panelComponent.getChildren().add(new Row(line, true, face(line.getIcon(), alpha(line, true)), tint(line)));
			}
			any = true;
		}
		return any ? super.render(graphics) : null;
	}

	/**
	 * Whether the box steps aside for Sol Heredit's wave, under way or next, with Quiet on Sol Heredit's wave on (idea
	 * 362): it has nothing to list for that fight.
	 */
	static boolean quiet(int wave, boolean quietOnSol)
	{
		return quietOnSol && wave > ColosseumTracker.LAST_WAVE;
	}

	/** The tint of a line's name by how its NPC attacks, or null for none: Names by attack style off, or more than one style. */
	private Color tint(WaveRoster.Line line)
	{
		return config.waveBoxStyleColours() ? styleColour(line.getIcon(), config) : null;
	}

	/**
	 * The colour of an NPC's attack style, by the roster's icon name: the Serpent shaman, the Shockwave Colossus and the
	 * seer attack with magic, the Javelin Colossus and the archer with ranged, the Jaguar warrior, the Minotaur and the
	 * berserker with melee. Null for the Manticore and the warband together, which use all three.
	 */
	static Color styleColour(String icon, SolSurvivorConfig config)
	{
		if (icon == null)
		{
			return null;
		}
		switch (icon)
		{
			case "shaman":
			case "shockwave":
			case "seer":
				return config.waveBoxMagicColour();
			case "javelin":
			case "archer":
				return config.waveBoxRangedColour();
			case "jaguar":
			case "minotaur":
			case "berserker":
				return config.waveBoxMeleeColour();
			default:
				return null;
		}
	}

	/** A tinted name still to come: the tint a little darker, as an untinted one is grey rather than white. */
	static Color toCome(Color tint)
	{
		return new Color(tint.getRed() * 4 / 5, tint.getGreen() * 4 / 5, tint.getBlue() * 4 / 5);
	}

	/** How many there are as a colour: yellow for one, orange for two, red for three or more, grey for none. */
	static Color countColour(int count)
	{
		return count <= 0 ? DEAD : count == 1 ? ONE : count == 2 ? TWO : MORE;
	}

	/** The count as a line reads it: "+1" for those still to come, the dead's "0" without the sign. */
	static String count(WaveRoster.Line line, boolean toCome)
	{
		return (toCome && !line.isDead() ? "+" : "") + line.getCount();
	}

	private static float alpha(WaveRoster.Line line, boolean toCome)
	{
		return line.isDead() ? 0.45f : toCome ? 0.8f : 1f;
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

	/** Words as RuneLite's own components draw them: a black shadow a pixel down and right. */
	private static void text(Graphics2D graphics, String words, int x, int baseline, Color colour)
	{
		graphics.setColor(Color.BLACK);
		graphics.drawString(words, x + 1, baseline + 1);
		graphics.setColor(colour);
		graphics.drawString(words, x, baseline);
	}

	/** A line's height in the box's font, at least a face high, with its text's baseline centred in it. */
	private static int height(FontMetrics metrics)
	{
		return Math.max(FACE, metrics.getHeight()) + 1;
	}

	private static int baseline(FontMetrics metrics, int y)
	{
		return y + (height(metrics) - metrics.getHeight()) / 2 + metrics.getAscent();
	}

	/** A piece of the box laid out across its width: the panel tells it where it goes and how wide it is. */
	private abstract static class Piece implements LayoutableRenderableEntity
	{
		Point at = new Point();
		Dimension size = new Dimension(ComponentConstants.STANDARD_WIDTH, 0);
		final Rectangle bounds = new Rectangle();

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

		Dimension drawn(int height)
		{
			bounds.setBounds(at.x, at.y, size.width, height);
			return new Dimension(size.width, height);
		}
	}

	/** A wave's head: a dark band across the box, its title on the left and "now" or "next" quietly on the right. */
	private static final class Band extends Piece
	{
		private final String title;
		private final String which;

		Band(String title, String which)
		{
			this.title = title;
			this.which = which;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			int height = height(metrics);
			graphics.setColor(BAND);
			// The band runs into the box's border, two pixels each side, so that it reads as a bar across the box.
			graphics.fillRect(at.x - 2, at.y, size.width + 4, height);
			text(graphics, title, at.x + 2, baseline(metrics, at.y), TITLE);
			Font was = graphics.getFont();
			graphics.setFont(FontManager.getRunescapeSmallFont());
			FontMetrics small = graphics.getFontMetrics();
			int y = at.y + (height - small.getHeight()) / 2 + small.getAscent();
			text(graphics, which, at.x + size.width - small.stringWidth(which) - 2, y, TO_COME);
			graphics.setFont(was);
			return drawn(height);
		}
	}

	/**
	 * A line of the box: the count, coloured by how many, then the NPC's face and short name, tinted by its attack style,
	 * and struck when all are dead.
	 */
	private static final class Row extends Piece
	{
		private final WaveRoster.Line line;
		private final boolean toCome;
		private final BufferedImage face;
		private final Color tint;

		Row(WaveRoster.Line line, boolean toCome, BufferedImage face, Color tint)
		{
			this.line = line;
			this.toCome = toCome;
			this.face = face;
			this.tint = tint;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			FontMetrics metrics = graphics.getFontMetrics();
			int height = height(metrics);
			int baseline = baseline(metrics, at.y);
			String count = count(line, toCome);
			text(graphics, count, at.x + COUNT_WIDTH - metrics.stringWidth(count), baseline, countColour(line.getCount()));
			int x = at.x + COUNT_WIDTH + GAP;
			if (face != null)
			{
				graphics.drawImage(face, x, at.y + (height - FACE) / 2, null);
			}
			x += FACE + GAP;
			boolean dead = line.isDead();
			String name = line.getShortName();
			Color colour = tint == null ? (toCome ? TO_COME : TEXT) : toCome ? toCome(tint) : tint;
			text(graphics, name, x, baseline, dead ? DEAD : colour);
			if (dead)
			{
				graphics.setColor(DEAD);
				graphics.fillRect(x - 1, at.y + height / 2, metrics.stringWidth(name) + 2, 1);
			}
			return drawn(height);
		}
	}

	/** A strip of nothing, between the wave under way and the next. */
	private static final class Space extends Piece
	{
		private final int height;

		Space(int height)
		{
			this.height = height;
		}

		@Override
		public Dimension render(Graphics2D graphics)
		{
			return drawn(height);
		}
	}
}
