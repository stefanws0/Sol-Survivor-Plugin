package nl.dexanddice.solsurvivor.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * The pick link, as src/link/pickLink.ts defines it: the site's address, "#pick=", and the base64url of a JSON payload
 * that opens the site's modifier pick with Minimus's three offers filled in, and the modifiers held and the next wave as
 * the plugin knows them. The panel's "Open in recommender" opens it while the selector is open.
 *
 * <pre>
 * {"v":1,"kind":"solSurvivorPick","wave":4,"modifiers":{"relentless":1,"dynamicDuo":true},
 *  "offers":[["relentless",2],["bees",1],null]}
 * </pre>
 *
 * `wave` is the wave the offers come before, null when the plugin has not seen a wave's message; `modifiers` are written
 * as the state link writes them; each offer is the modifier's key and the tier taking it brings, the one after the tier
 * held for a modifier with tiers and 1 for one without, or null for an id this plugin does not know.
 */
public final class PickPayload
{
	/** The payload's version. */
	public static final int VERSION = 1;

	/** The payload's `kind`. */
	public static final String KIND = "solSurvivorPick";

	/** How the site's URL hash holds a pick: this, then the payload's base64url. */
	public static final String PICK_HASH = "#pick=";

	private PickPayload()
	{
	}

	/** The payload's JSON: the next wave, the modifiers held with their tiers (ColosseumTracker.modifiers) and the offers. */
	public static String toJson(Integer wave, Map<Modifier, Integer> modifiers, List<Modifier> offers)
	{
		StringBuilder b = new StringBuilder();
		b.append("{\"v\":").append(VERSION).append(",\"kind\":");
		StatePayload.string(b, KIND);
		b.append(",\"wave\":").append(wave);
		b.append(",\"modifiers\":{");
		boolean first = true;
		for (Map.Entry<Modifier, Integer> e : modifiers.entrySet())
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			StatePayload.string(b, e.getKey().getKey());
			b.append(':').append(StatePayload.valueOf(e.getKey(), e.getValue()));
		}
		b.append("},\"offers\":[");
		first = true;
		for (Modifier m : offers)
		{
			if (!first)
			{
				b.append(',');
			}
			first = false;
			if (m == null)
			{
				b.append("null");
				continue;
			}
			int tier = m.hasTiers() ? Math.min(3, modifiers.getOrDefault(m, 0) + 1) : 1;
			b.append('[');
			StatePayload.string(b, m.getKey());
			b.append(',').append(tier).append(']');
		}
		b.append("]}");
		return b.toString();
	}

	/** The whole link: the site's address, losing any hash it had and ending in a slash, "#pick=" and the payload. */
	public static String link(String siteUrl, Integer wave, Map<Modifier, Integer> modifiers, List<Modifier> offers)
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
		String json = toJson(wave, modifiers, offers);
		return base + PICK_HASH + Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.US_ASCII));
	}
}
