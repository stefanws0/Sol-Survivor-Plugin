package nl.dexanddice.solsurvivor.core;

/**
 * The Sol Survivor site the plugin's links open and its uploads go to. A player's plugin always has the live site: only
 * a development run sets the system property PROPERTY, which `gradlew run` does on a release's holding branch, such as
 * release/1.1, so that its links open that branch's preview, the site that reads what the branch's plugin sends
 * (docs/release.md, "Working on plugin 1.1"). A preview runs on the live site's data, so its uploads land where the live
 * site's do.
 */
public final class Site
{
	/** The live site. */
	public static final String LIVE = "https://solsurvivor.app/";

	/** The system property a development run names another site in. */
	public static final String PROPERTY = "solsurvivor.site";

	private Site()
	{
	}

	/**
	 * The hosts a development run may name: a branch's preview of the site's own Worker, as build.gradle names it. The
	 * property ships in the plugin the Hub builds, so it takes no other host: a player talked into starting RuneLite
	 * with it set cannot be made to send their runs, their name and their leaderboard key to a stranger's server.
	 */
	private static final String PREVIEW = "https://[a-z0-9-]+-sol-survivor\\.stefankessel\\.workers\\.dev/?";

	/**
	 * The site for `override`, the system property's value: the live site for none, or for anything but the https
	 * address of one of the site's previews (PREVIEW); otherwise that address, ending in "/" as LIVE does.
	 */
	public static String of(String override)
	{
		if (override == null || !override.trim().matches(PREVIEW))
		{
			return LIVE;
		}
		String site = override.trim();
		return site.endsWith("/") ? site : site + "/";
	}
}
