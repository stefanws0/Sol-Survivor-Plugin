package nl.dexanddice.solsurvivor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import nl.dexanddice.solsurvivor.core.BoardEntry;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * The leaderboard's requests (core.BoardEntry), sent by the plugin's injected RuneLite HTTP client, and the site's
 * answers read with RuneLite's Gson: a wave put on the boards once the site has its log, and all of the player's times
 * taken off by Remove my times. An answer is read up to MAX_ANSWER bytes, as the site's are a line of JSON.
 */
final class BoardUpload
{
	/** The most of an answer read: the site's are under 300 bytes. */
	private static final int MAX_ANSWER = 4096;

	private BoardUpload()
	{
	}

	/** The request that puts a wave on the boards, with its body (BoardEntry.body). */
	static Request enter(String site, String body)
	{
		return new Request.Builder().url(BoardEntry.endpoint(site))
			.post(new LazyBody(BoardEntry.TYPE, () -> body.getBytes(StandardCharsets.UTF_8))).build();
	}

	/** The request that takes all of the times of the player whose key is `key` off the boards. */
	static Request remove(String site, String key)
	{
		return new Request.Builder().url(BoardEntry.removeEndpoint(site))
			.post(new LazyBody(BoardEntry.TYPE, () -> BoardEntry.removeBody(key).getBytes(StandardCharsets.UTF_8))).build();
	}

	/** An answer's JSON, or an IOException for one that is too long or none. */
	private static String text(Response response) throws IOException
	{
		ResponseBody body = response.body();
		if (body == null || body.source().request(MAX_ANSWER + 1) || body.source().getBuffer().size() > MAX_ANSWER)
		{
			throw new IOException("The site's answer was not one it gives.");
		}
		return body.source().readUtf8();
	}

	/**
	 * What the site's answer to a wave means for the player, in words for the panel (BoardEntry.said), `asked` being the
	 * name sent; null for an answer that says nothing to tell, such as a refusal, which is dropped without a word as a
	 * shared log's is.
	 */
	static String entered(Gson gson, Response response, String asked)
	{
		if (!response.isSuccessful())
		{
			return null;
		}
		try
		{
			return BoardEntry.said(gson.fromJson(text(response), BoardEntry.Answer.class), asked);
		}
		catch (IOException | RuntimeException ex)
		{
			return null;
		}
	}

	/**
	 * How many waves Remove my times took off, from the site's answer; an IOException in words for the player when it
	 * did not.
	 */
	static int removed(Gson gson, Response response) throws IOException
	{
		if (!response.isSuccessful())
		{
			throw new IOException(response.code() == 429 ? "Too many requests. Try again in a minute."
				: "The site could not remove your times. Try again later.");
		}
		try
		{
			return gson.fromJson(text(response), JsonObject.class).get("removed").getAsInt();
		}
		catch (RuntimeException ex)
		{
			throw new IOException("The site's answer was not one it gives. Try again later.", ex);
		}
	}
}
