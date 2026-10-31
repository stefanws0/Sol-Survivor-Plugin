package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.awt.Canvas;
import java.awt.event.KeyEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.config.Keybind;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.input.KeyManager;
import org.junit.Test;

/** Real RuneLite key dispatch reaches the existing copy/open path and stops reaching it after plugin shutdown. */
public class ArenaHotkeysTest
{
	@Test
	public void openingIsOptInAndTheExistingCopyBindingStaysCopyOnly() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.plugin.registerHotkeys();
		assertFalse(fixture.press(KeyEvent.VK_F8));
		assertTrue(fixture.plugin.requests.isEmpty());
		assertTrue(fixture.press(KeyEvent.VK_F7));
		assertEquals(Arrays.asList(false), fixture.plugin.requests);
		fixture.plugin.unregisterHotkeys();
	}

	@Test
	public void configuredOpeningUsesTheSamePathAsArenaNowAndUnregisteringRemovesBothBindings() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.settings.open = new Keybind(KeyEvent.VK_F8, 0);
		assertFalse(fixture.press(KeyEvent.VK_F8));
		fixture.plugin.registerHotkeys();
		fixture.plugin.registerHotkeys();
		assertTrue(fixture.press(KeyEvent.VK_F7));
		assertTrue(fixture.press(KeyEvent.VK_F8));
		assertEquals(Arrays.asList(false, true), fixture.plugin.requests);
		fixture.plugin.unregisterHotkeys();
		assertFalse(fixture.press(KeyEvent.VK_F7));
		assertFalse(fixture.press(KeyEvent.VK_F8));
		assertEquals(Arrays.asList(false, true), fixture.plugin.requests);
	}

	@Test
	public void changingTheConfiguredKeyDoesNotNeedAPluginRestartAndNothingRunsOnTheLoginScreen() throws Exception
	{
		Fixture fixture = new Fixture();
		fixture.settings.open = new Keybind(KeyEvent.VK_F8, 0);
		fixture.plugin.registerHotkeys();
		fixture.settings.open = new Keybind(KeyEvent.VK_F9, 0);
		assertFalse(fixture.press(KeyEvent.VK_F8));
		assertTrue(fixture.press(KeyEvent.VK_F9));
		assertEquals(Arrays.asList(true), fixture.plugin.requests);
		fixture.state = GameState.LOGIN_SCREEN;
		assertFalse(fixture.press(KeyEvent.VK_F9));
		assertFalse(fixture.press(KeyEvent.VK_F7));
		assertEquals(Arrays.asList(true), fixture.plugin.requests);
		fixture.plugin.unregisterHotkeys();
	}

	private static final class Fixture
	{
		private final RecordingPlugin plugin = new RecordingPlugin();
		private final Settings settings = new Settings();
		private final KeyManager keys;
		private GameState state = GameState.LOGGED_IN;

		private Fixture() throws Exception
		{
			Client client = (Client) Proxy.newProxyInstance(Client.class.getClassLoader(), new Class<?>[]{Client.class},
				(proxy, method, args) -> method.getName().equals("getGameState") ? state : null);
			Constructor<KeyManager> constructor = KeyManager.class.getDeclaredConstructor(Client.class, EventBus.class);
			constructor.setAccessible(true);
			keys = constructor.newInstance(client, new EventBus());
			set("config", settings);
			set("keyManager", keys);
		}

		private void set(String name, Object value) throws Exception
		{
			Field field = SolSurvivorPlugin.class.getDeclaredField(name);
			field.setAccessible(true);
			field.set(plugin, value);
		}

		private boolean press(int code)
		{
			KeyEvent press = key(KeyEvent.KEY_PRESSED, code);
			keys.processKeyPressed(press);
			keys.processKeyReleased(key(KeyEvent.KEY_RELEASED, code));
			return press.isConsumed();
		}

		private static KeyEvent key(int type, int code)
		{
			// AWT fills the extended key code for native events; a headless synthetic event must supply it itself.
			return new KeyEvent(new Canvas(), type, 0, 0, code, KeyEvent.CHAR_UNDEFINED)
			{
				@Override
				public int getExtendedKeyCode()
				{
					return code;
				}
			};
		}
	}

	private static final class RecordingPlugin extends SolSurvivorPlugin
	{
		private final List<Boolean> requests = new ArrayList<>();

		@Override
		void copyCurrent(boolean open)
		{
			requests.add(open);
		}
	}

	private static final class Settings implements SolSurvivorConfig
	{
		private Keybind open = Keybind.NOT_SET;

		@Override
		public Keybind copyHotkey()
		{
			return new Keybind(KeyEvent.VK_F7, 0);
		}

		@Override
		public Keybind openArenaHotkey()
		{
			return open;
		}
	}
}
