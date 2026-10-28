package nl.dexanddice.solsurvivor.core;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A finished wave's log as "Keep runs between sessions" saves it, so that Past waves still holds it after RuneLite
 * closes: one gzipped file a wave, in the plugin's own folder of RuneLite's (Plugin.getPluginDirectory), each account's
 * apart. A file holds what WaveLog.Past does: the wave, its ticks, how it ended, the glory it earned and whether the
 * player died in it, the state links' payloads on its first tick and on the tick its reinforcements came, and its log as
 * WaveLogWriter wrote it. The
 * RuneLite layer reads and writes the files, never on the client thread, as RuneLite's plugin guide asks; this class
 * only turns a wave into bytes and back, and names and sorts the files. It holds no RuneLite import, as the rest of the
 * core.
 *
 * The bytes are the plugin's own and nothing else reads them: a magic number and a version, then the fields in order,
 * each text its UTF-8 length and bytes, or -1 for none. Version 2 added the death after the end; a file of version 1,
 * as 0.9.0 and the 1.0 candidates saved it, is read as a wave without one. A file of another version, or one cut short,
 * is not read.
 */
public final class WaveStore
{
	/** How a saved wave's file name ends. */
	public static final String SUFFIX = ".wave.gz";

	/** The most waves kept on disk for an account: as many as WaveLog keeps, the wave under way with the past ones. */
	public static final int MAX_SAVED = WaveLog.MAX_PAST + 1;

	/** "SSWV": a file this class wrote. */
	private static final int MAGIC = 0x53535756;
	private static final int VERSION = 2;
	/** The most a text in a file may be: far past a wave's log, so that a damaged length cannot fill the memory. */
	private static final int MAX_TEXT_BYTES = 64 * 1024 * 1024;

	private WaveStore()
	{
	}

	/**
	 * The file name of a wave saved at `savedAt`, milliseconds since 1970: the time first, padded, so that the names sort
	 * as the waves were saved, then the wave, for a person looking in the folder.
	 */
	public static String fileName(long savedAt, int wave)
	{
		return String.format(Locale.ROOT, "%013d-wave%02d%s", savedAt, wave, SUFFIX);
	}

	/** The saved waves among the file names `names`, newest first; any other file in the folder is passed over. */
	public static List<String> newestFirst(Collection<String> names)
	{
		List<String> out = new ArrayList<>();
		for (String name : names)
		{
			if (name.endsWith(SUFFIX))
			{
				out.add(name);
			}
		}
		out.sort(Collections.reverseOrder());
		return out;
	}

	/** The saved waves among `names` past the newest `keep`, which the RuneLite layer deletes. */
	public static List<String> beyond(Collection<String> names, int keep)
	{
		List<String> sorted = newestFirst(names);
		return sorted.size() <= keep ? Collections.<String>emptyList() : new ArrayList<>(sorted.subList(keep, sorted.size()));
	}

	/** Writes `wave` to `out`, gzipped; its JSON is written here if it was not yet, so not on the client thread. */
	public static void write(WaveLog.Past wave, OutputStream out) throws IOException
	{
		GZIPOutputStream zip = new GZIPOutputStream(out);
		DataOutputStream data = new DataOutputStream(zip);
		data.writeInt(MAGIC);
		data.writeInt(VERSION);
		data.writeInt(wave.getWave());
		data.writeInt(wave.size());
		data.writeByte(wave.getEnd() == null ? 0 : wave.getEnd() == WaveLog.End.COMPLETED ? 1 : 2);
		data.writeBoolean(wave.isDied());
		data.writeBoolean(wave.getGlory() != null);
		data.writeInt(wave.getGlory() == null ? 0 : wave.getGlory());
		text(data, wave.getStartState());
		text(data, wave.getReinforcementState());
		text(data, wave.getJson());
		data.flush();
		zip.finish();
	}

	/**
	 * The wave `in` holds, as write wrote it, finished and under id 0 until WaveLog.restore gives it one; an IOException
	 * when it is not a saved wave of this version, or is cut short.
	 */
	public static WaveLog.Past read(InputStream in) throws IOException
	{
		DataInputStream data = new DataInputStream(new GZIPInputStream(in));
		if (data.readInt() != MAGIC)
		{
			throw new IOException("Not a saved wave");
		}
		int version = data.readInt();
		if (version != 1 && version != VERSION)
		{
			throw new IOException("A saved wave of version " + version);
		}
		int wave = data.readInt();
		int size = data.readInt();
		int endCode = data.readByte();
		if (endCode < 0 || endCode > 2)
		{
			throw new IOException("A saved wave with an unknown end: " + endCode);
		}
		WaveLog.End end = endCode == 0 ? null : endCode == 1 ? WaveLog.End.COMPLETED : WaveLog.End.LEFT;
		boolean died = version >= 2 && data.readBoolean();
		boolean hasGlory = data.readBoolean();
		int glory = data.readInt();
		String startState = text(data);
		String reinforcementState = text(data);
		String json = text(data);
		if (json == null)
		{
			throw new IOException("A saved wave without its log");
		}
		Supplier<String> log = () -> json;
		return new WaveLog.Past(0, wave, size, end, false, log, startState, reinforcementState, hasGlory ? glory : null, died);
	}

	private static void text(DataOutputStream data, String text) throws IOException
	{
		if (text == null)
		{
			data.writeInt(-1);
			return;
		}
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		data.writeInt(bytes.length);
		data.write(bytes);
	}

	private static String text(DataInputStream data) throws IOException
	{
		int length = data.readInt();
		if (length == -1)
		{
			return null;
		}
		if (length < 0 || length > MAX_TEXT_BYTES)
		{
			throw new IOException("A saved wave with a text of " + length + " bytes");
		}
		byte[] bytes = new byte[length];
		data.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
