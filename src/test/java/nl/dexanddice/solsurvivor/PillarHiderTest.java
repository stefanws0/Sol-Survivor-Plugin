package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import net.runelite.api.Actor;
import net.runelite.api.GameObject;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.TileObject;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.gameval.ObjectID;
import org.junit.Test;

/** "Hide the pillars": only the arena's pillars go, found by the scene's own instance template, and only while it is on. */
public class PillarHiderTest
{
	/** Region 7216's chunk at its tile (20, 20), which a scene's chunks are all built from in these tests. */
	static final int ARENA_CHUNK = template(28 * 64 + 16, 48 * 64 + 16);

	/** Lumbridge's chunk, outside the arena. */
	static final int ELSEWHERE_CHUNK = template(3222, 3218);

	@Test
	public void theArenaPillarsAreHiddenWhileTheSettingIsOn()
	{
		Scene arena = instance(ARENA_CHUNK);
		TileObject pillar = gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM01, null);
		TileObject other = gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM02, null);
		assertTrue(PillarHider.isArenaPillar(arena, pillar));
		assertTrue(PillarHider.isArenaPillar(arena, other));

		assertFalse(new PillarHider(() -> true).drawObject(arena, pillar));
		assertTrue(new PillarHider(() -> false).drawObject(arena, pillar));
	}

	@Test
	public void nothingElseIsHidden()
	{
		Scene arena = instance(ARENA_CHUNK);
		// Another object of the arena, the same model outside it, and an NPC the scene holds as an object.
		assertFalse(PillarHider.isArenaPillar(arena, gameObject(ObjectID.CIVITAS_COLOSSEUM_BRAZIER01, null)));
		assertFalse(PillarHider.isArenaPillar(instance(ELSEWHERE_CHUNK), gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM01, null)));
		assertFalse(PillarHider.isArenaPillar(arena, gameObject(ObjectID.PILLAR_CIVITAS01_COLOSSEUM01, proxy(Actor.class))));
		assertFalse(PillarHider.isArenaPillar(arena, proxy(WallObject.class)));
		assertTrue(new PillarHider(() -> true).drawObject(arena, gameObject(ObjectID.CIVITAS_COLOSSEUM_BRAZIER01, null)));
	}

	/** A chunk of the template at a world tile, unrotated on the ground floor, as the game packs them. */
	private static int template(int worldX, int worldY)
	{
		return (worldX / 8) << 14 | (worldY / 8) << 3;
	}

	/** An instanced scene built all of one template chunk. */
	static Scene instance(int chunk)
	{
		int[][][] chunks = new int[4][13][13];
		for (int[] row : chunks[0])
		{
			Arrays.fill(row, chunk);
		}
		return (Scene) Proxy.newProxyInstance(Scene.class.getClassLoader(), new Class<?>[]{Scene.class}, (p, m, a) ->
		{
			switch (m.getName())
			{
				case "isInstance":
					return true;
				case "getInstanceTemplateChunks":
					return chunks;
				default:
					throw new UnsupportedOperationException(m.getName());
			}
		});
	}

	/** An object at scene tile (52, 52), the middle of its chunk, drawn with the renderable given. */
	private static TileObject gameObject(int id, Actor renderable)
	{
		return gameObject(id, renderable, 52, 52);
	}

	/** A one-tile object on the scene tile given, drawn with the renderable given. */
	static GameObject gameObject(int id, Actor renderable, int sceneX, int sceneY)
	{
		LocalPoint at = new LocalPoint(sceneX * 128 + 64, sceneY * 128 + 64, -1);
		return (GameObject) Proxy.newProxyInstance(GameObject.class.getClassLoader(), new Class<?>[]{GameObject.class}, (p, m, a) ->
		{
			switch (m.getName())
			{
				case "getId":
					return id;
				case "getLocalLocation":
					return at;
				case "getPlane":
					return 0;
				case "getRenderable":
					return renderable;
				case "getSceneMinLocation":
					return new Point(sceneX, sceneY);
				case "equals":
					return p == a[0];
				case "hashCode":
					return System.identityHashCode(p);
				default:
					throw new UnsupportedOperationException(m.getName());
			}
		});
	}

	@SuppressWarnings("unchecked")
	private static <T> T proxy(Class<T> type)
	{
		return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (p, m, a) ->
		{
			throw new UnsupportedOperationException(m.getName());
		});
	}
}
