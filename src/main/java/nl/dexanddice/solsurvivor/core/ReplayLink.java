package nl.dexanddice.solsurvivor.core;

import java.net.URI;
import java.net.URISyntaxException;

/** The upload and browser always use the plugin's site (SolSurvivorPlugin.SITE), never a URL returned by the server. */
public final class ReplayLink
{
	private ReplayLink()
	{
	}

	public static String endpoint(String site)
	{
		return origin(site) + "/api/replays";
	}

	public static String browser(String site, String id)
	{
		return browser(site, id, null, null);
	}

	/**
	 * The page that opens the stored run at wave `wave` and at `tick` of it, as the wave report and the death recap ask:
	 * "#replay=<id>.<wave>.<tick>", or without the tick, or without either when `wave` is null.
	 */
	public static String browser(String site, String id, Integer wave, Integer tick)
	{
		if (!isId(id))
		{
			throw new IllegalArgumentException("The site returned an invalid recording link. Try again.");
		}
		String at = wave == null ? "" : "." + wave + (tick == null ? "" : "." + Math.max(0, tick));
		return origin(site) + "/#replay=" + id + at;
	}

	/** Whether `id` is a stored run's id as the site gives them, 32 hexadecimal digits: nothing else goes in a link. */
	public static boolean isId(String id)
	{
		return id != null && id.matches("[a-f0-9]{32}");
	}

	/** The site's origin, its scheme, host and port: HTTPS, or HTTP on localhost for development. WaveShare posts to it too. */
	static String origin(String site)
	{
		try
		{
			URI uri = new URI(site.trim());
			String host = uri.getHost();
			boolean local = "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "[::1]".equals(host);
			if (host == null || uri.getRawUserInfo() != null || !("https".equalsIgnoreCase(uri.getScheme()) || local && "http".equalsIgnoreCase(uri.getScheme())))
			{
				throw new URISyntaxException(site, "Use HTTPS, or localhost for development");
			}
			return new URI(uri.getScheme(), null, host, uri.getPort(), null, null, null).toString();
		}
		catch (URISyntaxException | NullPointerException ex)
		{
			throw new IllegalArgumentException("The site's address must be HTTPS, or localhost for development.");
		}
	}
}
