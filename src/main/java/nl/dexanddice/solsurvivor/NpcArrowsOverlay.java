package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveReview;

/**
 * Arrows to NPCs out of view (idea 359, Stefan's yes of 2026-11-08), off by default: for each living Colosseum NPC the
 * game view does not show, an arrow on the view's edge in the direction it is from the player, with its short name and
 * how many tiles away it is (the count the game uses for range: the nearest tile of the NPC's square). The arrow goes as
 * the NPC walks into view.
 * <p>
 * Positions only, as the minimap already gives them: the arrow is in the colour of the NPC's attack style, the wave box's
 * "Names by attack style" colours, and the Manticore's stays a neutral grey, as its style changes with its orbs. No arrow
 * says anything of line of sight or range, or of what the NPC will do (the limits Stefan accepted).
 * <p>
 * The direction is taken on the screen, from the player's point towards a point one tile from the player in the NPC's
 * direction, so that it holds for an NPC behind the camera too, which has no point on the screen of its own.
 */
class NpcArrowsOverlay extends Overlay
{
	/** How far in from the view's edge the arrow's tip sits. */
	static final int INSET = 6;
	private static final int ARROW_LENGTH = 14;
	private static final int ARROW_WIDTH = 12;
	private static final Color NEUTRAL = new Color(0xd0d0d0);
	private static final Color SHADOW = new Color(0, 0, 0, 170);

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;

	@Inject
	NpcArrowsOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	/** The wave box's icon name for a Colosseum NPC's id, which gives its style's colour, or null for any other NPC. */
	static String icon(int npcId)
	{
		switch (npcId)
		{
			case NpcKind.SHAMAN_ID:
				return "shaman";
			case NpcKind.SHOCKWAVE_ID:
				return "shockwave";
			case NpcKind.JAVELIN_ID:
				return "javelin";
			case NpcKind.JAGUAR_ID:
				return "jaguar";
			case NpcKind.MINOTAUR_ID:
			case NpcKind.MINOTAUR_RED_FLAG_ID:
				return "minotaur";
			case NpcKind.MANTICORE_ID:
				return "manticore";
			case WaveReview.FREM_ARCHER_ID:
				return "archer";
			case WaveReview.FREM_SEER_ID:
				return "seer";
			case WaveReview.FREM_BERSERKER_ID:
				return "berserker";
			default:
				return null;
		}
	}

	/** The short name an arrow carries, as the wave box gives it. */
	static String name(String icon)
	{
		switch (icon)
		{
			case "shockwave":
				return "Shockwave";
			case "javelin":
				return "Javelin";
			default:
				return Character.toUpperCase(icon.charAt(0)) + icon.substring(1);
		}
	}

	/** An arrow's colour: its NPC's style's, or neutral grey for the Manticore, whose style changes with its orbs. */
	static Color colour(String icon, SolSurvivorConfig config)
	{
		Color style = WaveBoxOverlay.styleColour(icon, config);
		return style != null ? style : NEUTRAL;
	}

	/**
	 * Where a ray from `from` in the direction (dx, dy) leaves `view`, drawn in by `inset`: the arrow's tip. Null for no
	 * direction. A `from` outside the view starts from the view's centre instead.
	 */
	static Point2D.Double edge(Rectangle view, Point2D from, double dx, double dy, int inset)
	{
		double length = Math.hypot(dx, dy);
		if (length == 0 || view.width <= 2 * inset || view.height <= 2 * inset)
		{
			return null;
		}
		double left = view.x + inset;
		double top = view.y + inset;
		double right = view.x + view.width - inset;
		double bottom = view.y + view.height - inset;
		double x0 = from.getX();
		double y0 = from.getY();
		if (x0 < left || x0 > right || y0 < top || y0 > bottom)
		{
			x0 = view.getCenterX();
			y0 = view.getCenterY();
		}
		double ux = dx / length;
		double uy = dy / length;
		double t = Double.MAX_VALUE;
		if (ux > 0)
		{
			t = Math.min(t, (right - x0) / ux);
		}
		else if (ux < 0)
		{
			t = Math.min(t, (left - x0) / ux);
		}
		if (uy > 0)
		{
			t = Math.min(t, (bottom - y0) / uy);
		}
		else if (uy < 0)
		{
			t = Math.min(t, (top - y0) / uy);
		}
		return new Point2D.Double(x0 + ux * t, y0 + uy * t);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		SolSurvivorConfig.NpcArrows mode = config.npcArrows();
		Player me = client.getLocalPlayer();
		if (mode == SolSurvivorConfig.NpcArrows.OFF || !plugin.inArena() || me == null)
		{
			return null;
		}
		WorldView world = client.getTopLevelWorldView();
		LocalPoint mine = me.getLocalLocation();
		if (world == null || mine == null)
		{
			return null;
		}
		int plane = world.getPlane();
		Point myPoint = Perspective.localToCanvas(client, mine, plane);
		if (myPoint == null)
		{
			return null;
		}
		Rectangle view = new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(), client.getViewportWidth(),
			client.getViewportHeight());
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setFont(FontManager.getRunescapeSmallFont());
		for (NPC npc : world.npcs())
		{
			String icon = icon(npc.getId());
			LocalPoint at = npc.getLocalLocation();
			if (icon == null || npc.isDead() || at == null)
			{
				continue;
			}
			Point seen = Perspective.localToCanvas(client, at, plane, npc.getLogicalHeight() / 2);
			if (seen != null && view.contains(seen.getX(), seen.getY()))
			{
				continue;
			}
			// One tile from the player towards the NPC, on the screen: the arrow's direction there.
			double dx = at.getX() - mine.getX();
			double dy = at.getY() - mine.getY();
			double length = Math.hypot(dx, dy);
			if (length == 0)
			{
				continue;
			}
			LocalPoint step = new LocalPoint(mine.getX() + (int) Math.round(dx / length * Perspective.LOCAL_TILE_SIZE),
				mine.getY() + (int) Math.round(dy / length * Perspective.LOCAL_TILE_SIZE), world);
			Point toward = Perspective.localToCanvas(client, step, plane);
			if (toward == null)
			{
				continue;
			}
			double sx = toward.getX() - myPoint.getX();
			double sy = toward.getY() - myPoint.getY();
			Point2D.Double tip = edge(view, new Point2D.Double(myPoint.getX(), myPoint.getY()), sx, sy, INSET);
			if (tip == null)
			{
				continue;
			}
			Color colour = colour(icon, config);
			double angle = Math.atan2(sy, sx);
			drawArrow(graphics, tip, angle, colour);
			if (mode == SolSurvivorConfig.NpcArrows.NAME_AND_DISTANCE)
			{
				WorldArea area = npc.getWorldArea();
				int tiles = area == null ? -1 : area.distanceTo(me.getWorldLocation());
				String label = name(icon) + (tiles >= 0 && tiles < 100 ? " " + tiles : "");
				drawLabel(graphics, view, tip, angle, label, colour);
			}
		}
		return null;
	}

	/** A filled arrowhead with its tip at `tip`, pointing along `angle`, outlined in black so that it reads on the floor. */
	private static void drawArrow(Graphics2D graphics, Point2D.Double tip, double angle, Color colour)
	{
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double baseX = tip.x - cos * ARROW_LENGTH;
		double baseY = tip.y - sin * ARROW_LENGTH;
		double half = ARROW_WIDTH / 2.0;
		Polygon arrow = new Polygon();
		arrow.addPoint((int) Math.round(tip.x), (int) Math.round(tip.y));
		arrow.addPoint((int) Math.round(baseX - sin * half), (int) Math.round(baseY + cos * half));
		arrow.addPoint((int) Math.round(baseX + sin * half), (int) Math.round(baseY - cos * half));
		graphics.setColor(colour);
		graphics.fillPolygon(arrow);
		graphics.setStroke(new BasicStroke(1.5f));
		graphics.setColor(SHADOW);
		graphics.drawPolygon(arrow);
	}

	/** The arrow's name and distance just inside it, kept within the view. */
	private static void drawLabel(Graphics2D graphics, Rectangle view, Point2D.Double tip, double angle, String label, Color colour)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		int width = metrics.stringWidth(label);
		double back = ARROW_LENGTH + 4 + Math.abs(Math.cos(angle)) * width / 2.0 + Math.abs(Math.sin(angle)) * metrics.getAscent() / 2.0;
		double cx = tip.x - Math.cos(angle) * back;
		double cy = tip.y - Math.sin(angle) * back;
		int x = (int) Math.round(Math.max(view.x + 2, Math.min(view.x + view.width - width - 2, cx - width / 2.0)));
		int y = (int) Math.round(Math.max(view.y + metrics.getAscent() + 2, Math.min(view.y + view.height - 2, cy + metrics.getAscent() / 2.0)));
		graphics.setColor(Color.BLACK);
		graphics.drawString(label, x + 1, y + 1);
		graphics.setColor(colour);
		graphics.drawString(label, x, y);
	}
}
