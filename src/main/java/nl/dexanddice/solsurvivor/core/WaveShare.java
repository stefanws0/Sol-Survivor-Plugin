package nl.dexanddice.solsurvivor.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

/**
 * The wave logs the player shares with "Share wave logs", off by default: each wave's log, as WaveLogWriter.toShare
 * writes it, gzipped and sent to the site's /api/wave-logs (its src/edge/waveLogs.ts) once, as the wave ends, so that the
 * planner can be checked against real games (its docs/wave-log-sharing.md). The site keeps it 90 days and shows it to
 * nobody.
 *
 * A log is sent under a random id for its run, made when wave 1 starts, or a wave that does not follow the last one, and
 * forgotten when the player leaves the arena, so that the site can join a run's waves and drop one sent twice without
 * any id that ties one run of the player's to another. The RuneLite layer sends the request; this class only decides
 * the run, which log goes, and the body. It holds no RuneLite import, as the rest of the core.
 */
public final class WaveShare
{
	/** The header that carries the run's id, as the site reads it (RUN_HEADER). */
	public static final String RUN_HEADER = "x-sol-survivor-run";

	/** The body's type: the log's JSON, gzipped (SHARED_LOG_TYPE). */
	public static final String TYPE = "application/gzip";

	private final Random random;
	/** The run's id, or null when no run is under way. */
	private String run;
	/** The last wave seen to start in this run. */
	private int lastWave;
	/** The id of the last log handed out to share (WaveLog.getId), so that each goes once. */
	private int sharedId;

	public WaveShare()
	{
		this(new SecureRandom());
	}

	/** With the random numbers given, for the tests. */
	WaveShare(Random random)
	{
		this.random = random;
	}

	/** The site's endpoint for shared logs. */
	public static String endpoint(String site)
	{
		return ReplayLink.origin(site) + "/api/wave-logs";
	}

	/** A wave starts: a new run's id when it is wave 1 or does not follow the last wave seen, else the run's own. */
	public String onWaveStart(int wave)
	{
		if (run == null || wave == 1 || wave != lastWave + 1)
		{
			byte[] bytes = new byte[16];
			random.nextBytes(bytes);
			StringBuilder id = new StringBuilder(32);
			for (byte b : bytes)
			{
				id.append(Character.forDigit((b >> 4) & 0xf, 16)).append(Character.forDigit(b & 0xf, 16));
			}
			run = id.toString();
		}
		lastWave = wave;
		return run;
	}

	/** The player left the arena, or the plugin stopped: the run's id is forgotten. */
	public void onLeave()
	{
		run = null;
		lastWave = 0;
	}

	/** The run's id, or null when no run is under way. */
	public String getRun()
	{
		return run;
	}

	/**
	 * Whether the log `logId` is still to be shared: true once for each log, and never for 0, no log. A log's turn comes
	 * as its wave ends, whether or not the setting is on, so that a wave played with sharing off is never sent later.
	 */
	public boolean claim(int logId)
	{
		if (logId == 0 || logId == sharedId)
		{
			return false;
		}
		sharedId = logId;
		return true;
	}

	/** The text gzipped, as the body the site takes. */
	public static byte[] gzip(String json)
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (GZIPOutputStream zip = new GZIPOutputStream(out))
		{
			zip.write(json.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException ex)
		{
			// A stream in memory does not fail.
			throw new UncheckedIOException(ex);
		}
		return out.toByteArray();
	}
}
