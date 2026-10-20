package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.junit.Test;

/**
 * The packed link text (StatePayload.pack): it unpacks, against the site's dictionary, to the JSON it was made from, and
 * it is about half as long. The site's tests unpack the plugin's packed fixture and compare the plugin's copy of the
 * dictionary with the site's, byte for byte (test/link/pluginFixtures.test.ts).
 */
public class StatePayloadTest
{
	/** The state fixtures, each a payload as toJson writes it. */
	private static final String[] FIXTURES = {"wave4-start.json", "wave4-mid.json", "late-start.json", "wave8-gear.json"};

	static String fixture(String name) throws IOException
	{
		try (InputStream in = StatePayloadTest.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
			{
				throw new IOException("no fixture " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	/** Packed link text unpacked to its JSON, as the site's unpackStateText does it; an AssertionError if it does not unpack. */
	static String unpack(String text)
	{
		assertTrue(text.startsWith(StatePayload.PACKED_V1));
		Inflater inflater = new Inflater(true);
		try
		{
			inflater.setDictionary(StatePayload.DICTIONARY_V1);
			inflater.setInput(Base64.getUrlDecoder().decode(text.substring(StatePayload.PACKED_V1.length())));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[4096];
			while (!inflater.finished())
			{
				int n = inflater.inflate(buffer);
				if (n == 0 && inflater.needsInput())
				{
					throw new AssertionError("the packed text is cut short");
				}
				out.write(buffer, 0, n);
			}
			return out.toString(StandardCharsets.US_ASCII);
		}
		catch (DataFormatException e)
		{
			throw new AssertionError("the packed text does not unpack", e);
		}
		finally
		{
			inflater.end();
		}
	}

	@Test
	public void theSitesDictionaryIsThere()
	{
		assertNotNull(StatePayload.DICTIONARY_V1);
		assertEquals(1496, StatePayload.DICTIONARY_V1.length);
	}

	@Test
	public void aPackedPayloadUnpacksToItsJsonAtLessThanThreeFifthsOfTheLength() throws Exception
	{
		for (String name : FIXTURES)
		{
			String json = fixture(name);
			String packed = StatePayload.pack(json);
			String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.US_ASCII));
			assertEquals(name, json, unpack(packed));
			assertTrue(name + ": " + packed.length() + " against " + plain.length(), packed.length() * 5 < plain.length() * 3);
		}
	}

	@Test
	public void thePackedFixtureIsAWaveStartsLink() throws Exception
	{
		// wave4-start.packed.txt is this plugin's packing of wave4-start.json, which the site's tests unpack too. It is not
		// compared with pack's text today: another zlib may deflate the same bytes otherwise, and both read the same.
		assertEquals(fixture("wave4-start.json"), unpack(fixture("wave4-start.packed.txt")));
	}
}
