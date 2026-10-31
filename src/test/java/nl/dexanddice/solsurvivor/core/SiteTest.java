package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SiteTest
{
	@Test
	public void aPlayersPluginHasTheLiveSite()
	{
		assertEquals("https://solsurvivor.app/", Site.of(null));
		assertEquals(Site.LIVE, Site.of(""));
	}

	@Test
	public void aDevelopmentRunOpensTheSiteItNames()
	{
		assertEquals("https://release-1-1-sol-survivor.stefankessel.workers.dev/",
			Site.of("https://release-1-1-sol-survivor.stefankessel.workers.dev"));
		assertEquals("https://release-1-1-sol-survivor.stefankessel.workers.dev/",
			Site.of(" https://release-1-1-sol-survivor.stefankessel.workers.dev/ "));
	}

	@Test
	public void anythingButAnHttpsHostIsTheLiveSite()
	{
		for (String odd : new String[]{"http://localhost:5173/", "solsurvivor.app", "https://a.dev/path", "https://a.dev/#s=1",
			"javascript:alert(1)"})
		{
			assertEquals(odd, Site.LIVE, Site.of(odd));
		}
	}
}
