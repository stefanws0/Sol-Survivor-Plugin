package nl.dexanddice.solsurvivor;

import java.util.Set;
import java.util.function.BooleanSupplier;
import net.runelite.api.Actor;
import net.runelite.api.GameObject;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.RenderCallback;
import nl.dexanddice.solsurvivor.core.ArenaGeometry;

/**
 * Keeps the game from drawing the arena's four pillars while "Hide the pillars" is on, as the Tzhaar/Colo Additions
 * plugin on the Plugin Hub does with a setting of its own. Only the drawing goes: the game still keeps the pillars'
 * tiles out of the player's path and line of sight, and PillarOverlay marks those tiles flat on the floor. RuneLite asks
 * about each object as it builds the scene, so a change to the setting shows once the scene is built again, which the
 * plugin asks for while the player is in the arena.
 */
final class PillarHider implements RenderCallback
{
	/** The arena's pillars, the two models its four pillars are drawn with. */
	private static final Set<Integer> PILLARS = Set.of(ObjectID.PILLAR_CIVITAS01_COLOSSEUM01,
		ObjectID.PILLAR_CIVITAS01_COLOSSEUM02);

	private final BooleanSupplier on;

	PillarHider(BooleanSupplier on)
	{
		this.on = on;
	}

	@Override
	public boolean drawObject(Scene scene, TileObject object)
	{
		return !on.getAsBoolean() || !isArenaPillar(scene, object);
	}

	/**
	 * Whether the object is one of the arena's pillars: a pillar's model, in the arena's region by the scene's own
	 * instance template, so that the answer needs nothing of where the player stands while the scene is being built.
	 * The scene holds the NPCs and the players as objects too, which are never hidden.
	 */
	static boolean isArenaPillar(Scene scene, TileObject object)
	{
		if (!isPillarModel(object))
		{
			return false;
		}
		LocalPoint lp = object.getLocalLocation();
		return lp != null && WorldPoint.fromLocalInstance(scene, lp, object.getPlane()).getRegionID() == ArenaGeometry.REGION_ID;
	}

	/** Whether the object is drawn with a pillar's model, wherever it stands: an NPC or a player never is. */
	static boolean isPillarModel(TileObject object)
	{
		return object instanceof GameObject && PILLARS.contains(object.getId())
			&& !(((GameObject) object).getRenderable() instanceof Actor);
	}
}
