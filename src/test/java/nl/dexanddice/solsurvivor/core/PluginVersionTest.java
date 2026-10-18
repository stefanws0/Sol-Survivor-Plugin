package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Test;

/** The version the wave log names is the one the build gives the plugin. */
public class PluginVersionTest
{
	@Test
	public void theVersionMatchesTheBuild() throws IOException
	{
		// Gradle runs the tests in the project's directory, where build.gradle is.
		String build = new String(Files.readAllBytes(Paths.get("build.gradle")), StandardCharsets.UTF_8);
		assertTrue("build.gradle's version is not " + PluginVersion.VERSION,
			build.contains("version = '" + PluginVersion.VERSION + "'"));
	}
}
