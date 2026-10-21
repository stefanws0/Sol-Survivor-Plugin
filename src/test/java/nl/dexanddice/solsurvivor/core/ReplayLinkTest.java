package nl.dexanddice.solsurvivor.core;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayLinkTest
{
	private static final String ID = "0123456789abcdef0123456789abcdef";

	@Test
	public void uploadAndBrowserUseTheConfiguredOrigin()
	{
		assertEquals("https://sol.example/api/replays", ReplayLink.endpoint("https://sol.example/old/#s=state"));
		assertEquals("https://sol.example/#replay=" + ID, ReplayLink.browser("https://sol.example", ID));
		assertEquals("http://localhost:4174/api/replays", ReplayLink.endpoint("http://localhost:4174/"));
	}

	@Test
	public void unsafeSitesAndServerSuppliedUrlsAreRejected()
	{
		for (String site : new String[]{"javascript:alert(1)", "file:///tmp/run", "https://user:pass@sol.example", "http://remote.example", "not a url"})
		{
			assertThrows(IllegalArgumentException.class, () -> ReplayLink.endpoint(site));
		}
		for (String id : new String[]{"https://evil.example", "../private", "bad", ID + "?extra"})
		{
			assertThrows(IllegalArgumentException.class, () -> ReplayLink.browser("https://sol.example", id));
		}
	}
}
