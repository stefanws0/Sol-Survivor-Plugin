package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import net.runelite.client.ui.FontManager;
import nl.dexanddice.solsurvivor.core.Modifier;
import nl.dexanddice.solsurvivor.core.WaveRoster;
import org.junit.Test;

/** The wave box: each count first, coloured by how many, "+1" for those still to come, and Doom left to its own box. */
public class WaveBoxOverlayTest
{
	private final AtomicReference<SolSurvivorConfig.WaveBox> mode = new AtomicReference<>(SolSurvivorConfig.WaveBox.BOTH);
	private final AtomicReference<WaveView> view = new AtomicReference<>();
	private final SolSurvivorPlugin plugin = new SolSurvivorPlugin()
	{
		@Override
		boolean inArena()
		{
			return true;
		}

		@Override
		WaveView waveView()
		{
			return view.get();
		}
	};
	private final SolSurvivorConfig config = (SolSurvivorConfig) Proxy.newProxyInstance(getClass().getClassLoader(),
		new Class<?>[] {SolSurvivorConfig.class}, (p, m, a) ->
		{
			switch (m.getName())
			{
				case "waveBox": return mode.get();
				case "markDead": return true;
				case "doomLimit": return true;
				default: return null;
			}
		});
	private final WaveBoxOverlay box = new WaveBoxOverlay(plugin, config);
	private final Map<Modifier, Integer> held = Collections.emptyMap();

	@Test
	public void aCountIsColouredByHowMany()
	{
		assertEquals(WaveBoxOverlay.countColour(0), WaveBoxOverlay.countColour(-1));
		assertNotEquals(WaveBoxOverlay.countColour(1), WaveBoxOverlay.countColour(2));
		assertNotEquals(WaveBoxOverlay.countColour(2), WaveBoxOverlay.countColour(3));
		assertEquals(WaveBoxOverlay.countColour(3), WaveBoxOverlay.countColour(7));
	}

	@Test
	public void thoseStillToComeReadPlusAndTheDeadReadZero()
	{
		// On a wave's first tick its reinforcements are still to come; once they have come and nobody is left, all are dead.
		List<WaveRoster.Line> fresh = WaveRoster.thisWave(5, held, new WaveRoster.Alive(), false, true, 0, false);
		WaveRoster.Line toCome = fresh.stream().filter(WaveRoster.Line::isToCome).findFirst().orElse(null);
		assertNotNull(fresh.toString(), toCome);
		assertEquals("+" + toCome.getCount(), WaveBoxOverlay.count(toCome, true));
		WaveRoster.Line here = fresh.stream().filter(l -> !l.isToCome() && !l.isDead()).findFirst().orElse(null);
		assertNotNull(here);
		assertEquals(String.valueOf(here.getCount()), WaveBoxOverlay.count(here, false));
		List<WaveRoster.Line> cleared = WaveRoster.thisWave(5, held, new WaveRoster.Alive(), true, false, 0, false);
		WaveRoster.Line dead = cleared.stream().filter(WaveRoster.Line::isDead).findFirst().orElse(null);
		assertNotNull(cleared.toString(), dead);
		assertEquals("0", WaveBoxOverlay.count(dead, true));
	}

	@Test
	public void theBoxDrawsTheWaveAndTheNextAndHidesWhenOff()
	{
		List<WaveRoster.Line> roster = WaveRoster.thisWave(5, held, new WaveRoster.Alive(), false, true, 0, false);
		view.set(new WaveView(5, true, roster, 6, roster, Collections.emptyList(), null, held, 4, 10));
		Dimension both = render();
		assertNotNull(both);
		mode.set(SolSurvivorConfig.WaveBox.THIS_WAVE);
		Dimension one = render();
		assertNotNull(one);
		assertEquals(both.width, one.width);
		mode.set(SolSurvivorConfig.WaveBox.OFF);
		assertNull(render());
	}

	private Dimension render()
	{
		Graphics2D graphics = new BufferedImage(300, 400, BufferedImage.TYPE_INT_ARGB).createGraphics();
		graphics.setFont(FontManager.getRunescapeFont());
		try
		{
			// Two frames, as the game draws one after another: a panel sizes itself from the frame before.
			box.render(graphics);
			return box.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
	}
}
