package nl.dexanddice.solsurvivor.core;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The wave log's JSON, for the site's log checker (src/viewer/waveLogCheck.ts). Like StatePayload it is written by
 * hand, ASCII only and in a fixed order, so that a log always gives the same text and the tests can compare it whole;
 * each tick is on a line of its own, so that a log reads and diffs a tick at a time.
 *
 * <pre>
 * {"v":1,"kind":"solSurvivorWaveLog","plugin":"0.2.0","wave":4,
 *  "modifiers":{"reentry":2,"quartet":"random"},"modifiersComplete":true,
 *  "maxTicks":1500,"cut":false,"end":"completed","start":{"v":1,"setup":{...}},"ticks":[
 * {"t":0,"player":[7,15,99,77,["magic","piety"],-1,null],"hits":[],
 *  "npcs":[[11,12811,13,20,-1,-1,-1,"player",[]],[12,12818,16,24,7590,30,30,"player",[[0,12]]]],"orbs":[],
 *  "worn":[26382,21295,-1,20997,26384,-1,26386,-1,-1,-1,-1]},
 * {"t":1,...,"anims":[[11,10859]],"playerAnims":[426],"spots":[[12,2681]],"projectiles":[[2678,12,"player",0,51]],
 *  "counters":{"waveDamageTaken":13}},
 * ...
 * ]}
 * </pre>
 *
 * <ul>
 * <li>`v`: the log's version, 1; `kind`: always "solSurvivorWaveLog", which tells it apart from a state link's payload;
 * `plugin`: the plugin's version.</li>
 * <li>`wave`: the wave, 1 to 11, or 12 for Sol Heredit's.</li>
 * <li>`modifiers`: the modifiers taken at the wave's start, written as the state link writes them (its tier, true or
 * "random"); `modifiersComplete`: whether Quartet, Red Flag and Totemic are known (see ArenaState).</li>
 * <li>`maxTicks`: the most ticks a log holds; `cut`: whether the wave ran past it and the rest was left out.</li>
 * <li>`end`: "completed" when the game said the wave was completed, "left" when the player left the arena (died, ran or
 * logged out), null while the wave is under way, or when the next wave started before its end was seen.</li>
 * <li>`start`: the state link's payload on the wave's first tick (StatePayload, the site's src/link/statePayload.ts),
 * with the gear and stats when "Include gear and stats" is on, so that the site can plan the wave from where it began
 * and put the plan beside what the player did; null when it could not be taken.</li>
 * <li>`ticks`: one object a game tick, from the wave's first tick on:
 * <ul>
 * <li>`t`: the tick within the wave, 0 on the tick of the game's "Wave: N" message;</li>
 * <li>`player`: [x, y, hitpoints, prayer points, [prayers on], animation id, target], with x and y the grid tile (null
 * off the grid), the prayers as WaveLog.prayerName names them, -1 for no animation, and the target an NPC's index,
 * "player" or null;</li>
 * <li>`hits`: the hitsplats on the player that came with this tick, [amount, hitsplat type id] each;</li>
 * <li>`npcs`: every NPC on the grid, by its raw id: [index, NPC id, x, y, animation id, health ratio, health scale,
 * target, [hitsplats]], with x and y its south-west tile, -1 for no animation and for an unknown health ratio or scale,
 * the target as the player's, and its hitsplats this tick as the player's;</li>
 * <li>`orbs`: the tiles of Solarflare's orbs (NPC 12826), [x, y] each, which are in `npcs` too;</li>
 * <li>`strayHits`, only when there are any: hitsplats on an NPC that is not in `npcs` this tick, [index, amount, type]
 * each;</li>
 * <li>`anims`, only when there are any: the animations NPCs started since the tick before, [index, animation id] each,
 * in the order they came. Each start is there, an attack that repeats the one before included, where the animation in
 * `npcs` only shows what an NPC is doing on the tick. An NPC whose animation stops is there too, with -1.</li>
 * <li>`playerAnims`, only when there are any: the animations the player started since the tick before, as `anims` has
 * the NPCs', so that each of the player's attacks is there even when the weapon's animation runs on unbroken.</li>
 * <li>`spots`, only when there are any: the graphics the NPCs in `npcs` show on the tick, [index, graphic id] each, such
 * as a Manticore's orbs as it charges them, unfiltered.</li>
 * <li>`worn`, on the wave's first tick and on each tick the player's gear changed, when "Include gear and stats" is on:
 * the item id in each slot of WaveLog.WORN_SLOTS, in its order, -1 for an empty one.</li>
 * <li>`tileGraphics`, only when there are any: the graphics the game put on the grid's tiles since the tick before,
 * [x, y, graphic id] each, in the order they came, unfiltered, such as where a Javelin Colossus's special lands.</li>
 * <li>`projectiles`, only when there are any (from 0.8.0): the projectiles first seen since the tick before, [graphic id,
 * from, to, start, end] each, in the order they were seen, unfiltered. From and to are each an NPC's index, "player",
 * or, at an end with no actor, such as a Javelin Colossus's special aimed at a tile, that tile as [x, y], null off the
 * grid. Start and end are the client cycles (20 ms, 30 to a tick) from this tick to when it sets off and when it lands,
 * below 0 for one that set off before this tick, as one first seen a tick late may have; end less start is its flight.
 * The Venator bow's bounces are each one from the NPC it bounces off to the next.</li>
 * <li>`counters`, on the wave's first tick and on each tick one changed (from 0.8.0): the game's own counters for the
 * wave and the run (ColosseumCounter), by key, such as Doom's stacks ("doomStacks") and the game's current glory
 * ("currentGlory"); those that did not change since the tick before are left out.</li>
 * </ul>
 * </li>
 * </ul>
 * The hitsplats of a tick are those the game showed since the tick before, which RuneLite passes on between ticks.
 */
public final class WaveLogWriter
{
	/** The log's version. */
	public static final int VERSION = 1;

	/** The log's `kind`. */
	public static final String KIND = "solSurvivorWaveLog";

	private WaveLogWriter()
	{
	}

	/** The log's JSON; an empty log, with no wave, has a null wave and no ticks. */
	public static String toJson(WaveLog log)
	{
		return toJson(log, log.getStartState());
	}

	/**
	 * The log as the player shares it with "Share wave logs" (WaveShare): the same JSON, but with its `start` written
	 * without the gear's levels and prayer unlocks (Equipment.withoutLevels), which the site refuses in a shared log.
	 */
	public static String toShare(WaveLog log)
	{
		return toJson(log, log.getShareState());
	}

	private static String toJson(WaveLog log, String start)
	{
		StringBuilder b = new StringBuilder();
		b.append("{\"v\":").append(VERSION).append(",\"kind\":");
		StatePayload.string(b, KIND);
		b.append(",\"plugin\":");
		StatePayload.string(b, log.getVersion());
		b.append(",\"wave\":").append(log.getWave());
		b.append(",\"modifiers\":{");
		boolean first = true;
		for (Map.Entry<Modifier, Integer> e : log.getModifiers().entrySet())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			StatePayload.string(b, e.getKey().getKey());
			b.append(':').append(StatePayload.valueOf(e.getKey(), e.getValue()));
		}
		b.append("},\"modifiersComplete\":").append(log.isModifiersComplete());
		b.append(",\"maxTicks\":").append(WaveLog.MAX_TICKS);
		b.append(",\"cut\":").append(log.isCut());
		b.append(",\"end\":");
		if (log.getEnd() == null)
		{
			b.append("null");
		}
		else
		{
			StatePayload.string(b, log.getEnd().getKey());
		}
		b.append(",\"start\":").append(start == null ? "null" : start);
		b.append(",\"ticks\":[");
		first = true;
		for (WaveLog.Tick tick : log.getTicks())
		{
			b.append(first ? "\n" : ",\n");
			first = false;
			tick(b, tick);
		}
		b.append(first ? "]}" : "\n]}");
		return b.toString();
	}

	private static void tick(StringBuilder b, WaveLog.Tick tick)
	{
		b.append("{\"t\":").append(tick.t).append(",\"player\":");
		WaveLog.PlayerTick p = tick.player;
		if (p == null)
		{
			b.append("null");
		}
		else
		{
			b.append('[');
			tile(b, p.getTile());
			b.append(',').append(p.getHitpoints()).append(',').append(p.getPrayerPoints()).append(",[");
			boolean first = true;
			for (String prayer : p.getPrayers())
			{
				if (!first)
				{
					b.append(',');
				}
				first = false;
				StatePayload.string(b, prayer);
			}
			b.append("],").append(p.getAnimation()).append(',');
			target(b, p.getTarget());
			b.append(']');
		}
		b.append(",\"hits\":");
		hits(b, tick.playerHits);

		b.append(",\"npcs\":[");
		boolean first = true;
		for (WaveLog.NpcTick n : tick.npcs)
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			b.append('[').append(n.getIndex()).append(',').append(n.getNpcId()).append(',');
			tile(b, n.getTile());
			b.append(',').append(n.getAnimation()).append(',').append(n.getHealthRatio()).append(',').append(n.getHealthScale()).append(',');
			target(b, n.getTarget());
			b.append(',');
			hits(b, tick.npcHits.getOrDefault(n.getIndex(), Collections.emptyList()));
			b.append(']');
		}
		b.append("],\"orbs\":[");
		first = true;
		for (GridTile t : tick.orbs)
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			b.append('[').append(t.getX()).append(',').append(t.getY()).append(']');
		}
		b.append(']');

		// Hitsplats on an NPC the tick does not list, such as one that despawned before the tick's snapshot, are kept.
		StringBuilder stray = new StringBuilder();
		for (Map.Entry<Integer, List<WaveLog.Hit>> e : tick.npcHits.entrySet())
		{
			if (listed(tick.npcs, e.getKey()))
			{
				continue;
			}
			for (WaveLog.Hit h : e.getValue())
			{
				stray.append(stray.length() == 0 ? "" : ",").append('[').append(e.getKey()).append(',').append(h.getAmount()).append(',')
					.append(h.getType()).append(']');
			}
		}
		if (stray.length() > 0)
		{
			b.append(",\"strayHits\":[").append(stray).append(']');
		}
		if (!tick.animations.isEmpty())
		{
			b.append(",\"anims\":[");
			for (int i = 0; i < tick.animations.size(); i++)
			{
				int[] a = tick.animations.get(i);
				b.append(i == 0 ? "" : ",").append('[').append(a[0]).append(',').append(a[1]).append(']');
			}
			b.append(']');
		}
		if (!tick.playerAnimations.isEmpty())
		{
			b.append(",\"playerAnims\":[");
			for (int i = 0; i < tick.playerAnimations.size(); i++)
			{
				b.append(i == 0 ? "" : ",").append(tick.playerAnimations.get(i));
			}
			b.append(']');
		}
		StringBuilder spots = new StringBuilder();
		for (WaveLog.NpcTick n : tick.npcs)
		{
			for (int id : n.getSpotAnims())
			{
				spots.append(spots.length() == 0 ? "" : ",").append('[').append(n.getIndex()).append(',').append(id).append(']');
			}
		}
		if (spots.length() > 0)
		{
			b.append(",\"spots\":[").append(spots).append(']');
		}
		if (tick.worn != null)
		{
			b.append(",\"worn\":[");
			for (int i = 0; i < tick.worn.length; i++)
			{
				b.append(i == 0 ? "" : ",").append(tick.worn[i]);
			}
			b.append(']');
		}
		if (!tick.tileGraphics.isEmpty())
		{
			b.append(",\"tileGraphics\":[");
			for (int i = 0; i < tick.tileGraphics.size(); i++)
			{
				int[] g = tick.tileGraphics.get(i);
				b.append(i == 0 ? "" : ",").append('[').append(g[0]).append(',').append(g[1]).append(',').append(g[2]).append(']');
			}
			b.append(']');
		}
		if (!tick.projectiles.isEmpty())
		{
			b.append(",\"projectiles\":[");
			for (int i = 0; i < tick.projectiles.size(); i++)
			{
				WaveLog.Projectile shot = tick.projectiles.get(i);
				b.append(i == 0 ? "" : ",").append('[').append(shot.getId()).append(',');
				end(b, shot.getFrom(), shot.getFromTile());
				b.append(',');
				end(b, shot.getTo(), shot.getToTile());
				b.append(',').append(shot.getStart()).append(',').append(shot.getEnd()).append(']');
			}
			b.append(']');
		}
		if (!tick.counters.isEmpty())
		{
			b.append(",\"counters\":{");
			boolean firstCounter = true;
			for (Map.Entry<ColosseumCounter, Integer> e : tick.counters.entrySet())
			{
				b.append(firstCounter ? "" : ",");
				firstCounter = false;
				StatePayload.string(b, e.getKey().getKey());
				b.append(':').append(e.getValue());
			}
			b.append('}');
		}
		b.append('}');
	}

	private static boolean listed(List<WaveLog.NpcTick> npcs, int index)
	{
		for (WaveLog.NpcTick n : npcs)
		{
			if (n.getIndex() == index)
			{
				return true;
			}
		}
		return false;
	}

	/** A tile as two numbers, x and y, or two nulls for none. */
	private static void tile(StringBuilder b, GridTile t)
	{
		if (t == null)
		{
			b.append("null,null");
		}
		else
		{
			b.append(t.getX()).append(',').append(t.getY());
		}
	}

	/** A projectile's end: its actor as a target is written, or with none the tile as [x, y], null off the grid. */
	private static void end(StringBuilder b, int actor, GridTile tile)
	{
		if (actor != WaveLog.TARGET_NONE)
		{
			target(b, actor);
		}
		else if (tile == null || !ArenaGeometry.onGrid(tile))
		{
			b.append("null");
		}
		else
		{
			b.append('[').append(tile.getX()).append(',').append(tile.getY()).append(']');
		}
	}

	private static void target(StringBuilder b, int target)
	{
		if (target == WaveLog.TARGET_PLAYER)
		{
			b.append("\"player\"");
		}
		else if (target < 0)
		{
			b.append("null");
		}
		else
		{
			b.append(target);
		}
	}

	private static void hits(StringBuilder b, List<WaveLog.Hit> hits)
	{
		b.append('[');
		boolean first = true;
		for (WaveLog.Hit h : hits)
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			b.append('[').append(h.getAmount()).append(',').append(h.getType()).append(']');
		}
		b.append(']');
	}
}
