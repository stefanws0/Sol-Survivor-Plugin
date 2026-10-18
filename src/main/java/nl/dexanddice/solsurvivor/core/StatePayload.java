package nl.dexanddice.solsurvivor.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Sol Survivor's state link, version 1, as src/link/statePayload.ts defines it: the site's address, "#s=", and the
 * base64url of a JSON payload, unpadded. The plugin writes the `setup`, the arena as the game shows it, and with the
 * player's gear the `prayer` values and the `equipment`; the site keeps its own kill table and solver settings.
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
			b.append(",\"solarflareOrbs\":[");
			first = true;
			for (GridTile t : state.getSolarflareOrbs())
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
		if (!e.getBonuses().isEmpty())
		{
			b.append(",\"bonuses\":{");
			first = true;
			for (Map.Entry<Integer, int[]> bonus : e.getBonuses().entrySet())
			{
				if (!first)
				{
					b.append(',');
				}
				first = false;
				b.append('"').append(bonus.getKey()).append("\":[");
				for (int i = 0; i < bonus.getValue().length; i++)
				{
					if (i > 0)
					{
						b.append(',');
					}
					b.append(bonus.getValue()[i]);
				}
				b.append(']');
			}
			b.append('}');
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

	/** The payload's text in a link: its JSON as unpadded base64url. */
	public static String encode(ArenaState state)
	{
		return encode(state, null);
	}

	/** The payload's text in a link, with the player's gear when `equipment` is not null. */
	public static String encode(ArenaState state, Equipment equipment)
	{
		return Base64.getUrlEncoder().withoutPadding().encodeToString(toJson(state, equipment).getBytes(StandardCharsets.US_ASCII));
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
		return base + STATE_HASH + encode(state, equipment);
	}
}
