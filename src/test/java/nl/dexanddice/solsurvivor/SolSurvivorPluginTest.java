package nl.dexanddice.solsurvivor;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/** Starts RuneLite with the plugin loaded, for trying it in game: ./gradlew run. */
public class SolSurvivorPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(SolSurvivorPlugin.class);
		RuneLite.main(args);
	}
}
