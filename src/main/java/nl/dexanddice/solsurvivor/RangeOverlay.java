package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveRoster;

/**
 * The player's attack range on the floor and the NPCs in it, two settings, the first off by default and the second on: "My attack range"
 * outlines the tiles the player can hit from where they stand, their weapon's range with Myopia's cut (core.WeaponRange)
 * and in their line of sight, which RuneLite's WorldArea works out from the game's own collision data; "Highlight NPCs
 * in range" outlines the NPCs they can hit, any of whose tiles is in that range and in sight, as RuneLite's Attack Ranges
 * and Dynamic line of sight plugins do, in the look the player picks (SolSurvivorConfig.InRangeStyle). Both say where the player can attack from, never where to stand or what to do.
 * The tiles and the NPCs are worked out once a tick, on the client thread (update), and drawn each frame from those.
 */
class RangeOverlay extends Overlay
{
	/** Half a tile in local units, from a tile's centre to its edge. */
	private static final int HALF_TILE = Perspective.LOCAL_TILE_SIZE / 2;
	/** How strongly the tiles in range are filled, out of 255, under their outline. */
	private static final int FILL_ALPHA = 28;
	/** How strongly a bold tile under an NPC in range is filled, out of 255. */
	private static final int BOLD_FILL_ALPHA = 80;
	/** The model outline's width and feather in pixels, wider than NPC colours' own so that the two tell apart. */
	private static final int OUTLINE_WIDTH = 4;
	private static final int OUTLINE_FEATHER = 4;
	/** How far above an NPC's head the arrow's anchor is, in local units, as the hitpoints tag's (NpcHitpointsOverlay). */
	private static final int ABOVE_HEAD = 60;
	/** The gap in pixels between that anchor and the arrow's tip, which leaves room for the hitpoints tag under it. */
	private static final int ARROW_LIFT = 26;
	/** The arrow's half width in pixels, and its height. */
	private static final int ARROW_HALF = 9;
	private static final int ARROW_HEIGHT = 13;
	/** How far along each edge of the tile a corner bracket reaches, as a share of the edge. */
	private static final double BRACKET_SHARE = 0.28;
	private static final Color EDGE = Color.BLACK;
	private static final Stroke THIN = new BasicStroke(1.5f);
	private static final Stroke LINE = new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	private static final Stroke EDGE_LINE = new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private final ModelOutlineRenderer outlines;
	private Set<WorldPoint> tiles = Collections.emptySet();
	private List<NPC> npcs = Collections.emptyList();

	@Inject
	RangeOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/**
	 * The tiles and the NPCs in range on this tick, for a range of `range` tiles, Myopia's cut taken off, that reaches only
	 * the four tiles beside the player when `cardinalOnly`. Nothing outside the arena, or with both settings off. On the
	 * client thread.
	 */
	void update(int range, boolean cardinalOnly)
	{
		tiles = Collections.emptySet();
		npcs = Collections.emptyList();
		Player player = client.getLocalPlayer();
		if (player == null || !plugin.inArena() || range < 1 || !(config.myRange() || config.highlightInRange()))
		{
			return;
		}
		WorldView view = client.getTopLevelWorldView();
		WorldPoint at = player.getWorldLocation();
		WorldArea me = new WorldArea(at, 1, 1);
		if (config.myRange())
		{
			Set<WorldPoint> out = new HashSet<>();
			for (int dx = -range; dx <= range; dx++)
			{
				for (int dy = -range; dy <= range; dy++)
				{
					boolean beside = Math.abs(dx) + Math.abs(dy) == 1;
					if ((dx == 0 && dy == 0) || (cardinalOnly && !beside))
					{
						continue;
					}
					WorldPoint tile = at.dx(dx).dy(dy);
					if (me.hasLineOfSightTo(view, tile))
					{
						out.add(tile);
					}
				}
			}
			tiles = out;
		}
		if (config.highlightInRange())
		{
			List<NPC> out = new ArrayList<>();
			for (NPC npc : view.npcs())
			{
				if (npc.isDead() || (NpcKind.forNpcId(npc.getId(), false) == null && !WaveRoster.isFrem(npc.getId())))
				{
					continue;
				}
				WorldArea area = npc.getWorldArea();
				boolean in = cardinalOnly ? me.isInMeleeDistance(area) : me.distanceTo(area) <= range && me.hasLineOfSightTo(view, area);
				if (in)
				{
					out.add(npc);
				}
			}
			npcs = out;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!plugin.inArena())
		{
			return null;
		}
		Color colour = config.rangeColour();
		if (config.myRange() && !tiles.isEmpty())
		{
			outline(graphics, colour);
		}
		if (config.highlightInRange() && !npcs.isEmpty())
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			SolSurvivorConfig.InRangeStyle style = config.inRangeStyle();
			for (NPC npc : npcs)
			{
				mark(graphics, npc, style == null ? SolSurvivorConfig.InRangeStyle.BOLD_TILE : style, colour);
			}
		}
		return null;
	}

	/**
	 * One NPC in range, in the look picked. Each line is drawn on a black edge, so that it shows on the sand whatever
	 * the colour, the thin gold tile drawn before having been hard to see there (Stefan, 2026-11-08).
	 */
	private void mark(Graphics2D graphics, NPC npc, SolSurvivorConfig.InRangeStyle style, Color colour)
	{
		if (style == SolSurvivorConfig.InRangeStyle.OUTLINE)
		{
			outlines.drawOutline(npc, OUTLINE_WIDTH, colour, OUTLINE_FEATHER);
			return;
		}
		Polygon poly = npc.getCanvasTilePoly();
		if (style == SolSurvivorConfig.InRangeStyle.ARROW)
		{
			if (poly != null)
			{
				graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(150, colour.getAlpha())));
				graphics.setStroke(THIN);
				graphics.draw(poly);
			}
			arrow(graphics, npc, colour);
			return;
		}
		if (poly == null)
		{
			return;
		}
		if (style == SolSurvivorConfig.InRangeStyle.BRACKETS && poly.npoints == 4)
		{
			brackets(graphics, poly, colour);
			return;
		}
		graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(BOLD_FILL_ALPHA, colour.getAlpha())));
		graphics.fill(poly);
		edged(graphics, poly, colour);
	}

	/** The shape drawn in the colour on a black line twice its width. */
	private static void edged(Graphics2D graphics, Shape shape, Color colour)
	{
		graphics.setStroke(EDGE_LINE);
		graphics.setColor(EDGE);
		graphics.draw(shape);
		graphics.setStroke(LINE);
		graphics.setColor(colour);
		graphics.draw(shape);
	}

	/** A downward arrow over the NPC's head, its tip just above where the hitpoints tag sits. */
	private static void arrow(Graphics2D graphics, NPC npc, Color colour)
	{
		Point at = npc.getCanvasTextLocation(graphics, "", npc.getLogicalHeight() + ABOVE_HEAD);
		if (at == null)
		{
			return;
		}
		int x = at.getX();
		int tip = at.getY() - ARROW_LIFT;
		Polygon arrow = new Polygon(new int[]{x - ARROW_HALF, x + ARROW_HALF, x}, new int[]{tip - ARROW_HEIGHT, tip - ARROW_HEIGHT, tip}, 3);
		graphics.setStroke(EDGE_LINE);
		graphics.setColor(EDGE);
		graphics.draw(arrow);
		graphics.setColor(colour);
		graphics.fill(arrow);
	}

	/** Brackets on the tile's four corners, each reaching a share of the way along the two edges that meet there. */
	private static void brackets(Graphics2D graphics, Polygon poly, Color colour)
	{
		Path2D.Double path = new Path2D.Double();
		for (int i = 0; i < 4; i++)
		{
			int ax = poly.xpoints[i];
			int ay = poly.ypoints[i];
			int bx = poly.xpoints[(i + 1) % 4];
			int by = poly.ypoints[(i + 1) % 4];
			int cx = poly.xpoints[(i + 3) % 4];
			int cy = poly.ypoints[(i + 3) % 4];
			path.moveTo(ax + (bx - ax) * BRACKET_SHARE, ay + (by - ay) * BRACKET_SHARE);
			path.lineTo(ax, ay);
			path.lineTo(ax + (cx - ax) * BRACKET_SHARE, ay + (cy - ay) * BRACKET_SHARE);
		}
		edged(graphics, path, colour);
	}

	/**
	 * The tiles in range, filled faintly and outlined as one shape: each tile's edge is drawn where the tile beyond it is
	 * out of range, so that the outline shows the range's border and the gaps a pillar's shadow leaves, not a grid.
	 */
	private void outline(Graphics2D graphics, Color colour)
	{
		WorldView view = client.getTopLevelWorldView();
		int plane = view.getPlane();
		graphics.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), Math.min(FILL_ALPHA, colour.getAlpha())));
		for (WorldPoint tile : tiles)
		{
			LocalPoint centre = LocalPoint.fromWorld(view, tile);
			Polygon poly = centre == null ? null : Perspective.getCanvasTilePoly(client, centre);
			if (poly != null)
			{
				graphics.fill(poly);
			}
		}
		graphics.setColor(colour);
		graphics.setStroke(new BasicStroke(2));
		for (WorldPoint tile : tiles)
		{
			LocalPoint centre = LocalPoint.fromWorld(view, tile);
			if (centre == null)
			{
				continue;
			}
			int x = centre.getX();
			int y = centre.getY();
			// North is +y in the scene's local coordinates, as it is in the world's.
			if (!tiles.contains(tile.dy(1)))
			{
				edge(graphics, view, plane, x - HALF_TILE, y + HALF_TILE, x + HALF_TILE, y + HALF_TILE);
			}
			if (!tiles.contains(tile.dy(-1)))
			{
				edge(graphics, view, plane, x - HALF_TILE, y - HALF_TILE, x + HALF_TILE, y - HALF_TILE);
			}
			if (!tiles.contains(tile.dx(1)))
			{
				edge(graphics, view, plane, x + HALF_TILE, y - HALF_TILE, x + HALF_TILE, y + HALF_TILE);
			}
			if (!tiles.contains(tile.dx(-1)))
			{
				edge(graphics, view, plane, x - HALF_TILE, y - HALF_TILE, x - HALF_TILE, y + HALF_TILE);
			}
		}
	}

	private void edge(Graphics2D graphics, WorldView view, int plane, int x0, int y0, int x1, int y1)
	{
		Point a = Perspective.localToCanvas(client, new LocalPoint(x0, y0, view), plane);
		Point b = Perspective.localToCanvas(client, new LocalPoint(x1, y1, view), plane);
		if (a != null && b != null)
		{
			graphics.drawLine(a.getX(), a.getY(), b.getX(), b.getY());
		}
	}
}
