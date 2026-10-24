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
		if (id == null || !id.matches("[a-f0-9]{32}"))
		{
			throw new IllegalArgumentException("The site returned an invalid recording link. Try again.");
		}
		return origin(site) + "/#replay=" + id;
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
