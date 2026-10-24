package nl.dexanddice.solsurvivor.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

/**
 * Sol Survivor's state link, version 1, as src/link/statePayload.ts defines it: the site's address, "#s=", and the
 * JSON payload packed as the site packs its own (src/ui/linkPacking.ts): "1", then the JSON deflated against the site's
 * preset dictionary and written as unpadded base64url. Plugins before 0.5.0 wrote the JSON's base64url unpacked, which
 * the site still reads. The plugin writes the `setup`, the arena as the game shows it (mid-wave with the fight so far:
 * the NPCs' health bars, Doom's stacks; and the molten sand on the floor), and with the player's gear the `prayer`
 * values and the `equipment`; with the player's setting for it, the place of the dot on the site's Safe / Fast / Easy
 * triangle (`triangle`, withTriangle). The site keeps its own kill table and solver settings.
 *
 * The JSON is written by hand, ASCII only and in a fixed order, so that a state always gives the same link and the
 * tests can compare links whole.
 */
public final class StatePayload
{
	/** The payload's version, the site's STATE_VERSION. */
	public static final int VERSION = 1;

	/** The site's state hash, STATE_HASH. */
	public static final String STATE_HASH = "#s=";

	/** The leading character of text packed with the site's first dictionary, the site's PACKED_V1. */
	public static final String PACKED_V1 = "1";

	/**
	 * The site's first preset dictionary, STATE_DICTIONARY_V1, byte for byte: the site's tests compare the two
	 * (test/link/pluginFixtures.test.ts). It is frozen, so a link packed with it reads on every later site. Null when
	 * the resource cannot be read, and the links are then written unpacked, as before 0.5.0, which the site reads too.
	 */
	static final byte[] DICTIONARY_V1 = readDictionary();

	private static byte[] readDictionary()
	{
		try (InputStream in = StatePayload.class.getResourceAsStream("state-dictionary-v1.txt"))
		{
			return in == null ? null : in.readAllBytes();
		}
		catch (IOException e)
		{
			return null;
		}
	}

	private StatePayload()
	{
	}

	/** The prayer bonus's range the site reads (PRAYER_BONUS_LIMITS); a bonus outside it is clamped to it. */
	private static final int MAX_PRAYER_BONUS = 73;
	/** The most prayer points the site reads (PRAYER_POINTS_LIMITS), the most a level 99 has. */
	private static final int MAX_PRAYER_POINTS = 99;

	/**
	 * Mid-wave: `attacks`, one entry per NPC in the order of `npcs`, each [ticks since its last attack, attacks made] with
	 * null for what the plugin does not know, or null for an NPC it knows nothing of (a Manticore always). Left out when
	 * it knows nothing of any.
	 */
	private static void attacks(StringBuilder b, ArenaState state)
	{
		boolean any = false;
		for (ExportNpc n : state.getNpcs())
		{
			any |= n.getSinceAttack() != null || n.getAttacks() != null;
		}
		if (!any)
		{
			return;
		}
		b.append(",\"attacks\":[");
		boolean first = true;
		for (ExportNpc n : state.getNpcs())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			if (n.getSinceAttack() == null && n.getAttacks() == null)
			{
				b.append("null");
			}
			else
			{
				b.append('[').append(n.getSinceAttack()).append(',').append(n.getAttacks()).append(']');
			}
		}
		b.append(']');
	}

	/**
	 * Mid-wave: `health`, one entry per NPC in the order of `npcs`, each its health bar as the game last showed it,
	 * [ratio, scale], or null for an NPC whose bar the plugin has not seen. Left out when it has seen none.
	 */
	private static void health(StringBuilder b, ArenaState state)
	{
		boolean any = false;
		for (ExportNpc n : state.getNpcs())
		{
			any |= n.getHealth() != null;
		}
		if (!any)
		{
			return;
		}
		b.append(",\"health\":[");
		boolean first = true;
		for (ExportNpc n : state.getNpcs())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			int[] health = n.getHealth();
			if (health == null)
			{
				b.append("null");
			}
			else
			{
				b.append('[').append(health[0]).append(',').append(health[1]).append(']');
			}
		}
		b.append(']');
	}

	/** A list of tiles, such as `[[16,10],[15,11]]`. */
	private static void tiles(StringBuilder b, List<GridTile> tiles)
	{
		b.append('[');
		boolean first = true;
		for (GridTile t : tiles)
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			b.append('[').append(t.getX()).append(',').append(t.getY()).append(']');
		}
		b.append(']');
	}

	/** The payload's JSON, without the gear. */
	public static String toJson(ArenaState state)
	{
		return toJson(state, null);
	}

	/** The payload's JSON, with the player's gear and stats when `equipment` is not null. */
	public static String toJson(ArenaState state, Equipment equipment)
	{
		StringBuilder b = new StringBuilder();
		b.append("{\"v\":").append(VERSION).append(",\"setup\":{\"npcs\":[");
		boolean first = true;
		for (ExportNpc n : state.getNpcs())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			b.append('[').append(n.getTile().getX()).append(',').append(n.getTile().getY()).append(',').append(n.getKind().getCode());
			if (n.getOrbs() != null)
			{
				b.append(',');
				string(b, n.getOrbs());
			}
			b.append(']');
		}
		b.append("],\"player\":[").append(state.getPlayer().getX()).append(',').append(state.getPlayer().getY()).append(']');
		b.append(",\"waveStart\":").append(state.isWaveStart());
		if (state.isMm3())
		{
			b.append(",\"mm3\":true");
		}
		if (state.getWave() != null)
		{
			b.append(",\"wave\":").append(state.getWave());
		}
		if (!state.isWaveStart())
		{
			// Once the reinforcements have come, none will; before, the plugin gives the tick within the wave and leaves
			// the timer to the site.
			if (state.isReinforcementsArrived())
			{
				b.append(",\"timer\":\"none\"");
			}
			if (state.getWaveTick() != null)
			{
				b.append(",\"waveTick\":").append(state.getWaveTick());
			}
			attacks(b, state);
			health(b, state);
			if (state.getDoomStacks() != null)
			{
				b.append(",\"doomStacks\":").append(state.getDoomStacks());
			}
		}
		b.append(",\"modifiers\":{");
		first = true;
		for (Map.Entry<Modifier, Integer> e : state.getModifiers().entrySet())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			string(b, e.getKey().getKey());
			b.append(':').append(valueOf(e.getKey(), e.getValue()));
		}
		b.append('}');
		if (!state.getSolarflareOrbs().isEmpty())
		{
			b.append(",\"solarflareOrbs\":");
			tiles(b, state.getSolarflareOrbs());
		}
		// The sand on the floor, at a wave's start too, where Reentry II's and III's from earlier waves lie.
		if (!state.getSand().isEmpty())
		{
			b.append(",\"sand\":");
			tiles(b, state.getSand());
		}
		b.append('}');
		if (equipment != null)
		{
			gear(b, equipment);
		}
		b.append('}');
		return b.toString();
	}

	/** The `prayer` values the site plans with, and the `equipment` it does not read yet. */
	private static void gear(StringBuilder b, Equipment e)
	{
		b.append(",\"prayer\":{");
		// The site reads 1 to 99 points: with none left, it keeps its own.
		if (e.getPrayerPoints() >= 1)
		{
			b.append("\"start\":").append(Math.min(e.getPrayerPoints(), MAX_PRAYER_POINTS)).append(',');
		}
		b.append("\"bonus\":").append(Math.max(0, Math.min(e.getPrayerBonus(), MAX_PRAYER_BONUS))).append('}');

		b.append(",\"equipment\":{\"items\":{");
		boolean first = true;
		for (Map.Entry<String, Equipment.Item> i : e.getItems().entrySet())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			string(b, i.getKey());
			b.append(":[").append(i.getValue().getId()).append(',');
			string(b, i.getValue().getName());
			b.append(',').append(i.getValue().getQuantity()).append(']');
		}
		b.append('}');
		if (!e.getSwitches().isEmpty())
		{
			// The switches: each [id, name, quantity, slot], in the inventory's order, and a weapon's attack speed after
			// them when it is known.
			b.append(",\"switches\":[");
			for (int i = 0; i < e.getSwitches().size(); i++)
			{
				Equipment.Switch sw = e.getSwitches().get(i);
				if (i > 0)
				{
					b.append(',');
				}
				b.append('[').append(sw.getItem().getId()).append(',');
				string(b, sw.getItem().getName());
				b.append(',').append(sw.getItem().getQuantity()).append(',');
				string(b, sw.getSlot());
				if (sw.getSpeed() > 0)
				{
					b.append(',').append(sw.getSpeed());
				}
				b.append(']');
			}
			b.append(']');
		}
		b.append(",\"weaponSpeed\":").append(e.getWeaponSpeed());
		b.append(",\"weaponCategory\":").append(e.getWeaponCategory());
		b.append(",\"attackStyle\":").append(e.getAttackStyle());
		if (!e.getStyleNames().isEmpty())
		{
			b.append(",\"styleNames\":[");
			for (int i = 0; i < e.getStyleNames().size(); i++)
			{
				if (i > 0)
				{
					b.append(',');
				}
				string(b, e.getStyleNames().get(i));
			}
			b.append(']');
		}
		b.append(",\"stats\":{");
		first = true;
		for (Map.Entry<String, int[]> st : e.getStats().entrySet())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			string(b, st.getKey());
			b.append(":[").append(st.getValue()[0]).append(',').append(st.getValue()[1]).append(']');
		}
		b.append('}');
		byItem(b, "bonuses", e.getBonuses());
		byItem(b, "defences", e.getDefences());
		int[] unlocks = e.getUnlocks();
		if (unlocks != null)
		{
			b.append(",\"unlocks\":{");
			for (int i = 0; i < Equipment.UNLOCK_FLAGS.length; i++)
			{
				if (i > 0)
				{
					b.append(',');
				}
				b.append('"').append(Equipment.UNLOCK_FLAGS[i]).append("\":").append(unlocks[i]);
			}
			b.append('}');
		}
		b.append('}');
	}

	/** A map of numbers by item id, as `,"name":{"id":[...],...}`, or nothing when it is empty. */
	private static void byItem(StringBuilder b, String name, Map<Integer, int[]> values)
	{
		if (values.isEmpty())
		{
			return;
		}
		b.append(",\"").append(name).append("\":{");
		boolean first = true;
		for (Map.Entry<Integer, int[]> item : values.entrySet())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			b.append('"').append(item.getKey()).append("\":[");
			for (int i = 0; i < item.getValue().length; i++)
			{
				if (i > 0)
				{
					b.append(',');
				}
				b.append(item.getValue()[i]);
			}
			b.append(']');
		}
		b.append('}');
	}

	/**
	 * A modifier's value in the link: its tier, true for Dynamic Duo, Red Flag and Totemic, and "random" for Quartet. The
	 * wave log writes its modifiers the same way, so that the site reads both with one reader.
	 */
	static String valueOf(Modifier m, int level)
	{
		switch (m)
		{
			case QUARTET:
				// The engine draws the fourth frem's kind at the wave start, as the game does.
				return "\"random\"";
			case DYNAMIC_DUO:
			case RED_FLAG:
			case TOTEMIC:
				return "true";
			default:
				return String.valueOf(level);
		}
	}

	/**
	 * A JSON string, ASCII only: an item's name may hold any character, which is escaped past ASCII and below a space. The
	 * wave log writes its strings with it too.
	 */
	static void string(StringBuilder b, String s)
	{
		b.append('"');
		for (char c : (s == null ? "" : s).toCharArray())
		{
			if (c == '"' || c == '\\')
			{
				b.append('\\').append(c);
			}
			else if (c < 0x20 || c > 0x7e)
			{
				b.append(String.format("\\u%04x", (int) c));
			}
			else
			{
				b.append(c);
			}
		}
		b.append('"');
	}

	/** The payload's text in a link: its JSON packed (pack). */
	public static String encode(ArenaState state)
	{
		return encode(state, null);
	}

	/** The payload's text in a link, with the player's gear when `equipment` is not null. */
	public static String encode(ArenaState state, Equipment equipment)
	{
		return pack(toJson(state, equipment));
	}

	/**
	 * A payload's JSON as link text, packed as the site packs its own: PACKED_V1, then the JSON deflated at the highest
	 * level against DICTIONARY_V1, raw (no zlib header), as unpadded base64url. The twelve wave starts of Stefan's logs of
	 * 2026-10-18 and 19, each with three switches, went from 1,796 to 1,921 characters to 849 to 884, well under the
	 * 2,047 or so at which Windows is reported to cut a link that another program opens. Without the dictionary the
	 * JSON's base64url, unpacked.
	 */
	public static String pack(String json)
	{
		byte[] bytes = json.getBytes(StandardCharsets.US_ASCII);
		if (DICTIONARY_V1 == null)
		{
			return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
		}
		Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
		try
		{
			deflater.setDictionary(DICTIONARY_V1);
			deflater.setInput(bytes);
			deflater.finish();
			ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length / 2 + 64);
			byte[] buffer = new byte[4096];
			while (!deflater.finished())
			{
				out.write(buffer, 0, deflater.deflate(buffer));
			}
			return PACKED_V1 + Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
		}
		finally
		{
			deflater.end();
		}
	}

	/**
	 * The whole link: the site's address, "#s=" and the payload's text. The address loses any hash it had and ends in a
	 * slash.
	 */
	public static String link(String siteUrl, ArenaState state)
	{
		return link(siteUrl, state, null);
	}

	/** The whole link, with the player's gear when `equipment` is not null. */
	public static String link(String siteUrl, ArenaState state, Equipment equipment)
	{
		return link(siteUrl, state, equipment, null);
	}

	/**
	 * The whole link, with the player's gear when `equipment` is not null, and the place they set for the site's triangle
	 * when `triangle` is not null (withTriangle).
	 */
	public static String link(String siteUrl, ArenaState state, Equipment equipment, TriangleDot triangle)
	{
		return linkOfJson(siteUrl, toJson(state, equipment), triangle);
	}

	/**
	 * A payload's JSON with the place the player set for the dot of the site's Safe / Fast / Easy triangle, `triangle`,
	 * after the rest: its three shares, as TriangleDot holds them. The site opens the link with the dot there, in place of
	 * where the viewer last left it. The JSON as it was when `triangle` is null. Only the links take it: the wave log keeps
	 * its payloads without, so that a past wave opens with the setting as it is when the player opens it.
	 */
	public static String withTriangle(String json, TriangleDot triangle)
	{
		if (triangle == null)
		{
			return json;
		}
		// toJson's JSON is one object, so its last character closes it.
		StringBuilder b = new StringBuilder(json.substring(0, json.lastIndexOf('}')));
		b.append(",\"triangle\":[").append(triangle.getSafe()).append(',').append(triangle.getFast()).append(',').append(triangle.getEasy()).append("]}");
		return b.toString();
	}

	/** The whole link of a payload toJson wrote earlier, such as a past wave's start the wave log kept. */
	public static String linkOfJson(String siteUrl, String json)
	{
		return linkOfJson(siteUrl, json, null);
	}

	/**
	 * The whole link of a payload toJson wrote earlier, with the place the player set for the site's triangle when
	 * `triangle` is not null (withTriangle).
	 */
	public static String linkOfJson(String siteUrl, String json, TriangleDot triangle)
	{
		String base = siteUrl.trim();
		int hash = base.indexOf('#');
		if (hash >= 0)
		{
			base = base.substring(0, hash);
		}
		if (!base.endsWith("/"))
		{
			base += "/";
		}
		return base + STATE_HASH + pack(withTriangle(json, triangle));
	}
}
