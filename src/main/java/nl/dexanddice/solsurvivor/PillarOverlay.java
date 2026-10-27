package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * What "Hide the pillars" leaves on the floor (the user's ask of 2026-10-27): each hidden pillar's three by three tiles,
 * filled faintly and outlined flat on the ground in "Hidden pillars' colour", so that the player still sees where they
 * cannot walk once the pillar is gone. It marks the very objects the hider keeps from being drawn, where the game holds
 * them, and nothing else: no tile to stand on and no line of sight.
 */
class PillarOverlay extends Overlay
{
	/** How much fainter the fill is than the outline: the outline's opacity divided by this. */
	private static final int FILL_SHARE = 3;

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	/** The objects drawn with a pillar's model that the scene spawned; which are the arena's is asked as they are drawn. */
	private final List<GameObject> pillars = new ArrayList<>();

	@Inject
	PillarOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** An object the scene spawned: kept if it is drawn with a pillar's model, whatever the setting. On the client thread. */
	void spawned(GameObject object)
	{
		if (PillarHider.isPillarModel(object))
		{
			pillars.add(object);
		}
	}

	/** An object the scene took away. On the client thread. */
	void despawned(GameObject object)
	{
		pillars.remove(object);
	}

	/** Forgets the pillars: the scene is loading again and spawns its objects anew, or the plugin stopped. */
	void clear()
	{
		pillars.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Color colour = config.hiddenPillarsColour();
		if (pillars.isEmpty() || !config.hidePillars() || !plugin.inArena() || colour.getAlpha() == 0)
		{
			return null;
		}
		Color fill = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), colour.getAlpha() / FILL_SHARE);
		graphics.setStroke(new BasicStroke(2));
		for (GameObject pillar : footprints(client.getTopLevelWorldView().getScene(), pillars))
		{
			// A pillar's local location is the middle of the tiles it stands on.
			Polygon poly = Perspective.getCanvasTileAreaPoly(client, pillar.getLocalLocation(), pillar.sizeX(), pillar.sizeY(),
				pillar.getPlane(), 0);
			if (poly != null)
			{
				graphics.setColor(fill);
				graphics.fill(poly);
				graphics.setColor(colour);
				graphics.draw(poly);
			}
		}
		return null;
	}

	/**
	 * The arena's pillars among the objects, as the hider finds them in the scene, one for each footprint: a pillar the
	 * game drew with both models would otherwise be filled twice, darker than the rest.
	 */
	static List<GameObject> footprints(Scene scene, Collection<GameObject> objects)
	{
		List<GameObject> out = new ArrayList<>();
		Set<Point> seen = new HashSet<>();
		for (GameObject object : objects)
		{
			if (PillarHider.isArenaPillar(scene, object) && seen.add(object.getSceneMinLocation()))
			{
				out.add(object);
			}
		}
		return out;
	}
}
