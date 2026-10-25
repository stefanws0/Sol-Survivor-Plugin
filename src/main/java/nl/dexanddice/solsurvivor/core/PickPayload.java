package nl.dexanddice.solsurvivor.core;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The pick link, as src/link/pickLink.ts defines it: the site's address, "#pick=", and the base64url of a JSON payload
 * that opens the site's modifier pick with Minimus's three offers filled in, and the modifiers held and the next wave as
 * the plugin knows them. The panel's "Open in recommender" opens it while the selector is open.
 *
 * <pre>
 * {"v":1,"kind":"solSurvivorPick","wave":4,"modifiers":{"relentless":1,"dynamicDuo":true},
 *  "offers":[["relentless",2],["bees",1],null],
 *  "run":[{"wave":3,"ticks":118,"hit":false,"damage":0,"modifiers":{"relentless":1},"glory":1500}]}
 * </pre>
 *
 * `wave` is the wave the offers come before, null when the plugin has not seen a wave's message; `modifiers` are written
 * as the state link writes them; each offer is the modifier's key and the tier taking it brings, the one after the tier
 * held for a modifier with tiers and 1 for one without, or null for an id this plugin does not know. `run`, from 0.8.0
 * and only when the plugin saw a wave of the run finish, is the run's waves finished so far (RunWave), from which the
 * site works out the run's glory for the title it aims at.
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

	/** The payload's JSON without the run: the next wave, the modifiers held with their tiers and the offers. */
	public static String toJson(Integer wave, Map<Modifier, Integer> modifiers, List<Modifier> offers)
	{
		return toJson(wave, modifiers, offers, Collections.emptyList());
	}

	/**
	 * The payload's JSON: the next wave, the modifiers held with their tiers (ColosseumTracker.modifiers), the offers, and
	 * the run's waves finished so far (ColosseumTracker.getRun), left out when there are none.
	 */
	public static String toJson(Integer wave, Map<Modifier, Integer> modifiers, List<Modifier> offers, List<RunWave> run)
	{
		StringBuilder b = new StringBuilder();
		b.append("{\"v\":").append(VERSION).append(",\"kind\":");
		StatePayload.string(b, KIND);
		b.append(",\"wave\":").append(wave);
		b.append(",\"modifiers\":");
		modifiersJson(b, modifiers);
		b.append(",\"offers\":[");
		boolean first = true;
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
		b.append(']');
		if (!run.isEmpty())
		{
			b.append(",\"run\":[");
			first = true;
			for (RunWave w : run)
			{
				if (!first)
				{
					b.append(',');
				}
				first = false;
				b.append("{\"wave\":").append(w.getWave());
				b.append(",\"ticks\":").append(w.getTicks());
				b.append(",\"hit\":").append(w.getHit());
				b.append(",\"damage\":").append(w.getDamage());
				b.append(",\"modifiers\":");
				modifiersJson(b, w.getModifiers());
				b.append(",\"glory\":").append(w.getGlory()).append('}');
			}
			b.append(']');
		}
		b.append('}');
		return b.toString();
	}

	/** Modifiers with their tiers as the state link writes them: {"relentless":2,"quartet":"random"}. */
	private static void modifiersJson(StringBuilder b, Map<Modifier, Integer> modifiers)
	{
		b.append('{');
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
		b.append('}');
	}

	/** The whole link without the run: see the other. */
	public static String link(String siteUrl, Integer wave, Map<Modifier, Integer> modifiers, List<Modifier> offers)
	{
		return link(siteUrl, wave, modifiers, offers, Collections.emptyList());
	}

	/** The whole link: the site's address, losing any hash it had and ending in a slash, "#pick=" and the payload. */
	public static String link(String siteUrl, Integer wave, Map<Modifier, Integer> modifiers, List<Modifier> offers, List<RunWave> run)
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
		String json = toJson(wave, modifiers, offers, run);
		return base + PICK_HASH + Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.US_ASCII));
	}
}
