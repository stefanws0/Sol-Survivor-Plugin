package nl.dexanddice.solsurvivor;

import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;

/**
 * A request's body made when OkHttp sends it, on OkHttp's own thread, and kept for a retry: the plugin hands a shared
 * wave log or a run to OkHttp from the client thread, and the JSON or the gzip that takes a few milliseconds is made
 * here rather than on the tick, as RuneLite asks of a plugin. What the body is made of must not change after it is
 * handed over: a WaveLog's snapshot, or the past waves' logs. A body that cannot be made fails the call with its
 * IOException, which the call's onFailure gets.
 */
final class LazyBody extends RequestBody
{
	/** How the body's bytes are made. */
	interface Maker
	{
		byte[] make() throws IOException;
	}

	private final MediaType type;
	private Maker maker;
	private byte[] bytes;

	LazyBody(String type, Maker maker)
	{
		this.type = MediaType.parse(type);
		this.maker = maker;
	}

	/** The bytes, made the first time they are asked for: OkHttp asks for the length before it writes them. */
	private synchronized byte[] bytes() throws IOException
	{
		if (bytes == null)
		{
			bytes = maker.make();
			maker = null;
		}
		return bytes;
	}

	@Override
	public MediaType contentType()
	{
		return type;
	}

	@Override
	public long contentLength() throws IOException
	{
		return bytes().length;
	}

	@Override
	public void writeTo(BufferedSink sink) throws IOException
	{
		sink.write(bytes());
	}
}
