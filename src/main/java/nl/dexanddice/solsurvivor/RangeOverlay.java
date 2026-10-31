package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
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
import net.runelite.client.ui.overlay.OverlayUtil;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveRoster;

/**
 * The player's attack range on the floor and the NPCs in it, two settings, the first off by default and the second on: "My attack range"
 * outlines the tiles the player can hit from where they stand, their weapon's range with Myopia's cut (core.WeaponRange)
 * and in their line of sight, which RuneLite's WorldArea works out from the game's own collision data; "Highlight NPCs
 * in range" outlines the NPCs they can hit, any of whose tiles is in that range and in sight, as RuneLite's Attack Ranges
 * and Dynamic line of sight plugins do. Both say where the player can attack from, never where to stand or what to do.
 * The tiles and the NPCs are worked out once a tick, on the client thread (update), and drawn each frame from those.
 */
class RangeOverlay extends Overlay
{
	/** Half a tile in local units, from a tile's centre to its edge. */
	private static final int HALF_TILE = Perspective.LOCAL_TILE_SIZE / 2;
	/** How strongly the tiles in range are filled, out of 255, under their outline. */
	private static final int FILL_ALPHA = 28;

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private Set<WorldPoint> tiles = Collections.emptySet();
	private List<NPC> npcs = Collections.emptyList();

	@Inject
	RangeOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
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
		if (config.highlightInRange())
		{
			for (NPC npc : npcs)
			{
				Polygon poly = npc.getCanvasTilePoly();
				if (poly != null)
				{
					OverlayUtil.renderPolygon(graphics, poly, colour);
				}
			}
		}
		return null;
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
