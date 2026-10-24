package nl.dexanddice.solsurvivor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nl.dexanddice.solsurvivor.core.ReplayLink;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/** A selected recording only, sent by the plugin's injected RuneLite HTTP client on its background dispatcher. */
final class ReplayUpload
{
	static final int MAX_BYTES = 8 * 1024 * 1024;

	private ReplayUpload()
	{
	}

	static Request request(String site, String json)
	{
		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_BYTES)
		{
			throw new IllegalArgumentException("This run is too large to open (8 MiB maximum). You can still copy its wave logs.");
		}
		return new Request.Builder().url(ReplayLink.endpoint(site))
			.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bytes)).build();
	}

	static String browserUrl(String site, Response response) throws IOException
	{
		if (!response.isSuccessful())
		{
			String error = response.code() == 429 ? "Too many runs opened. Try again in a minute."
				: response.code() == 413 ? "This run is too large to open. You can still copy its wave logs."
				: response.code() == 400 ? "The site could not read this recording. Update the plugin and try again."
				: "The site could not store this run. Try again.";
			throw new IOException(error);
		}
		ResponseBody body = response.body();
		if (body == null || body.source().request(4097) || body.source().buffer().size() > 4096)
		{
			throw new IOException("The site returned an invalid recording link. Try again.");
		}
		try
		{
			JsonObject data = new JsonParser().parse(body.source().readUtf8()).getAsJsonObject();
			return ReplayLink.browser(site, data.get("id").getAsString());
		}
		catch (RuntimeException ex)
		{
			throw new IOException("The site returned an invalid recording link. Try again.", ex);
		}
	}
}
