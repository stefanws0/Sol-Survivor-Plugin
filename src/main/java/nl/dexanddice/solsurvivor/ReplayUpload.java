package nl.dexanddice.solsurvivor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.function.Supplier;
import nl.dexanddice.solsurvivor.core.ReplayLink;
import nl.dexanddice.solsurvivor.core.RunUpload;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * A selected recording only, sent by the plugin's injected RuneLite HTTP client on its background dispatcher. The run's
 * JSON is written and gzipped there too (LazyBody, core.RunUpload), not on the client thread; a run too large for the
 * site fails the call with RunUpload.TooLarge, whose message the player is shown.
 */
final class ReplayUpload
{
	private ReplayUpload()
	{
	}

	/** The upload of the run whose JSON `json` writes, which must be the past waves' logs as they were handed over. */
	static Request request(String site, Supplier<String> json)
	{
		return new Request.Builder().url(ReplayLink.endpoint(site)).post(new LazyBody(RunUpload.TYPE, () -> RunUpload.pack(json.get()))).build();
	}

	/**
	 * The page that opens the stored run, from the site's answer read with RuneLite's Gson: only an ID the site gave, on
	 * the site's own address, so that an answer cannot send the browser elsewhere.
	 */
	static String browserUrl(Gson gson, String site, Response response) throws IOException
	{
		return ReplayLink.browser(site, idOf(gson, response));
	}

	/** The stored run's id from the site's answer, checked as browserUrl checks it, for a link to a wave and a tick of it. */
	static String idOf(Gson gson, Response response) throws IOException
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
		if (body == null || body.source().request(4097) || body.source().getBuffer().size() > 4096)
		{
			throw new IOException("The site returned an invalid recording link. Try again.");
		}
		try
		{
			JsonObject data = gson.fromJson(body.source().readUtf8(), JsonObject.class);
			String id = data.get("id").getAsString();
			if (!ReplayLink.isId(id))
			{
				throw new IOException("The site returned an invalid recording link. Try again.");
			}
			return id;
		}
		catch (RuntimeException ex)
		{
			throw new IOException("The site returned an invalid recording link. Try again.", ex);
		}
	}
}
