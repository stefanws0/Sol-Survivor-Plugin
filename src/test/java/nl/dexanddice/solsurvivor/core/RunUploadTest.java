package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import org.junit.Test;

/**
 * Open past run's body: the run's JSON gzipped, which the site stores as it comes, and a run past the site's limits
 * refused before anything is sent, with what the player can do instead.
 */
public class RunUploadTest
{
	@Test
	public void aRunIsSentGzippedAndUnpacksToItsJson() throws IOException
	{
		String json = "{\"v\":1,\"kind\":\"solSurvivorRun\",\"waves\":[" + "{\"t\":1},".repeat(10_000) + "{}]}";
		byte[] packed = RunUpload.pack(json);
		assertTrue(packed.length < json.length() / 10);
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(packed)))
		{
			assertEquals(json, new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
		assertEquals("application/gzip", RunUpload.TYPE);
	}

	@Test
	public void aRunPastTheSitesLimitsIsRefusedWithWhatThePlayerCanDo()
	{
		String tooLong = "x".repeat(RunUpload.MAX_JSON_BYTES + 1);
		assertRefused(tooLong, "This run is too large to open (8 MiB maximum). You can still copy its wave logs.");
		// Noise does not pack: past the stored size, it is refused too.
		byte[] noise = new byte[RunUpload.MAX_PACKED_BYTES];
		new Random(7).nextBytes(noise);
		StringBuilder hex = new StringBuilder();
		for (byte b : noise)
		{
			hex.append(String.format("%02x", b));
		}
		assertRefused(hex.toString(), "This run is too large to store on the site. You can still copy its wave logs.");
	}

	private static void assertRefused(String json, String message)
	{
		try
		{
			RunUpload.pack(json);
			fail("packed a run past the site's limits");
		}
		catch (RunUpload.TooLarge ex)
		{
			assertEquals(message, ex.getMessage());
		}
	}
}
