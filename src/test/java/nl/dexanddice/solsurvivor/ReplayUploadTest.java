package nl.dexanddice.solsurvivor;

import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import nl.dexanddice.solsurvivor.core.RunUpload;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayUploadTest
{
	private static final String SITE = "https://sol.example/";
	/** As RuneLite injects it. */
	private static final Gson GSON = new Gson();
	private static final String ID = "0123456789abcdef0123456789abcdef";

	private Response response(int status, String text)
	{
		return new Response.Builder().request(new Request.Builder().url(SITE).build()).protocol(Protocol.HTTP_1_1)
			.code(status).message("test").body(ResponseBody.create(MediaType.parse("application/json"), text)).build();
	}

	@Test
	public void selectedRunGoesInThePostBodyAndOnlyAValidatedIdOpens()
		throws Exception
	{
		// The run's JSON is written, and gzipped, only when OkHttp asks for the body, and once.
		String json = "{\"kind\":\"solSurvivorRun\"}";
		AtomicInteger written = new AtomicInteger();
		Request request = ReplayUpload.request(SITE, () ->
		{
			written.incrementAndGet();
			return json;
		});
		assertEquals(0, written.get());
		assertEquals("POST", request.method());
		assertEquals("https://sol.example/api/replays", request.url().toString());
		assertEquals("application/gzip", request.body().contentType().toString());
		Buffer sent = new Buffer();
		long length = request.body().contentLength();
		request.body().writeTo(sent);
		assertEquals(length, sent.size());
		assertEquals(1, written.get());
		try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(sent.readByteArray())))
		{
			assertEquals(json, new String(in.readAllBytes(), StandardCharsets.UTF_8));
		}
		try (Response res = response(201, "{\"id\":\"" + ID + "\",\"url\":\"https://evil.example\"}"))
		{
			assertEquals(SITE + "#replay=" + ID, ReplayUpload.browserUrl(GSON, SITE, res));
		}
	}

	@Test
	public void failuresAndMalformedResponsesCannotOpenAnArbitraryUrl()
	{
		for (int status : new int[]{400, 413, 429, 503})
		{
			try (Response res = response(status, "{}"))
			{
				assertThrows(IOException.class, () -> ReplayUpload.browserUrl(GSON, SITE, res));
			}
		}
		for (String body : new String[]{"{}", "not json", "{\"id\":\"https://evil.example\"}", "x".repeat(4097)})
		{
			try (Response res = response(201, body))
			{
				assertThrows(IOException.class, () -> ReplayUpload.browserUrl(GSON, SITE, res));
			}
		}
		// A run too large fails the call as its body is made, with the message the player is shown.
		Request large = ReplayUpload.request(SITE, () -> "x".repeat(RunUpload.MAX_JSON_BYTES + 1));
		IOException refused = assertThrows(IOException.class, () -> large.body().contentLength());
		assertTrue(refused instanceof RunUpload.TooLarge);
	}
}
