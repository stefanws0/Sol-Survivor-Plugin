package nl.dexanddice.solsurvivor;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Test;
import static org.junit.Assert.*;

public class ReplayUploadTest
{
	private static final String SITE = "https://sol.example/";
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
		Request request = ReplayUpload.request(SITE, "{\"kind\":\"solSurvivorRun\"}");
		assertEquals("POST", request.method());
		assertEquals("https://sol.example/api/replays", request.url().toString());
		assertEquals("application/json; charset=utf-8", request.body().contentType().toString());
		try (Response res = response(201, "{\"id\":\"" + ID + "\",\"url\":\"https://evil.example\"}"))
		{
			assertEquals(SITE + "#replay=" + ID, ReplayUpload.browserUrl(SITE, res));
		}
	}

	@Test
	public void failuresAndMalformedResponsesCannotOpenAnArbitraryUrl()
	{
		for (int status : new int[]{400, 413, 429, 503})
		{
			try (Response res = response(status, "{}"))
			{
				assertThrows(IOException.class, () -> ReplayUpload.browserUrl(SITE, res));
			}
		}
		for (String body : new String[]{"{}", "not json", "{\"id\":\"https://evil.example\"}", "x".repeat(4097)})
		{
			try (Response res = response(201, body))
			{
				assertThrows(IOException.class, () -> ReplayUpload.browserUrl(SITE, res));
			}
		}
		assertThrows(IllegalArgumentException.class, () -> ReplayUpload.request(SITE, "x".repeat(ReplayUpload.MAX_BYTES + 1)));
	}
}
