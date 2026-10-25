package nl.dexanddice.solsurvivor.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * The body a run's Watch uploads to the site's /api/replays (its src/edge/replays.ts): the run's JSON, as RunLogWriter
 * writes it, gzipped here, so that the site, with its few milliseconds of CPU a request, only unpacks it to check it and
 * stores the bytes it got. The site takes at most MAX_PACKED_BYTES gzipped, which unpack to at most MAX_JSON_BYTES; a
 * run past either is refused here, with what the player can do instead, before anything is sent. It holds no RuneLite
 * import, as the rest of the core.
 */
public final class RunUpload
{
	/** The body's type: the run's JSON, gzipped (the site's REPLAY_GZIP_TYPE). */
	public static final String TYPE = "application/gzip";

	/** The most JSON a run may be (the site's MAX_REPLAY_BYTES). */
	public static final int MAX_JSON_BYTES = 8 * 1024 * 1024;

	/** The most the gzipped run may be (the site's MAX_STORED_REPLAY_BYTES). */
	public static final int MAX_PACKED_BYTES = 256 * 1024;

	/** A run too large to upload, with the message the player is shown. */
	public static final class TooLarge extends IOException
	{
		TooLarge(String message)
		{
			super(message);
		}
	}

	private RunUpload()
	{
	}

	/** The run's JSON gzipped, as the site takes it; TooLarge past either of its limits. */
	public static byte[] pack(String json) throws TooLarge
	{
		if (json.getBytes(StandardCharsets.UTF_8).length > MAX_JSON_BYTES)
		{
			throw new TooLarge("This run is too large to open (8 MiB maximum). You can still copy its wave logs.");
		}
		byte[] packed = WaveShare.gzip(json);
		if (packed.length > MAX_PACKED_BYTES)
		{
			throw new TooLarge("This run is too large to store on the site. You can still copy its wave logs.");
		}
		return packed;
	}
}
