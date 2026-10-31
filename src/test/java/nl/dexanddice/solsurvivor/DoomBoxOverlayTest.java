package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

/** Doom's stacks in their movable box: only with Doom stacks on and placed in a box, and only while a wave with Doom runs. */
public class DoomBoxOverlayTest
{
	private final AtomicBoolean arena = new AtomicBoolean(true);
	private final AtomicReference<WaveView> view = new AtomicReference<>(wave(true, 4, 10));
	private final AtomicBoolean on = new AtomicBoolean(true);
	private final AtomicReference<SolSurvivorConfig.DoomPlace> place = new AtomicReference<>(SolSurvivorConfig.DoomPlace.MOVABLE);
	private final SolSurvivorPlugin plugin = new SolSurvivorPlugin()
	{
		@Override
		boolean inArena()
		{
			return arena.get();
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
				case "doomByOrb": return on.get();
				case "doomPlace": return place.get();
				default: return null;
			}
		});
	private final DoomBoxOverlay box = new DoomBoxOverlay(plugin, config);

	@Test
	public void theBoxDrawsOnlyWhenPlacedInABoxAndIsMovable()
	{
		Dimension size = render();
		assertNotNull(size);
		assertEquals(DoomOverlay.HEIGHT, size.height);
		assertTrue(size.toString(), size.width > DoomOverlay.ICON && size.width < 80);
		assertTrue(box.isMovable());
		assertFalse(DoomOverlay.shows(config, plugin, view.get(), SolSurvivorConfig.DoomPlace.BY_ORB));
		place.set(SolSurvivorConfig.DoomPlace.BY_ORB);
		assertNull(render());
		assertTrue(DoomOverlay.shows(config, plugin, view.get(), SolSurvivorConfig.DoomPlace.BY_ORB));
		place.set(SolSurvivorConfig.DoomPlace.MOVABLE);
		on.set(false);
		assertNull(render());
	}

	@Test
	public void theBoxHidesOutsideTheArenaBetweenWavesAndWithoutDoom()
	{
		arena.set(false);
		assertNull(render());
		arena.set(true);
		view.set(wave(false, 4, 10));
		assertNull(render());
		view.set(wave(true, 0, 0));
		assertNull(render());
		view.set(wave(true, 9, 10));
		assertNotNull(render());
	}

	private Dimension render()
	{
		Graphics2D graphics = new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			return box.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static WaveView wave(boolean underWay, int stacks, int limit)
	{
		return new WaveView(5, underWay, Collections.emptyList(), null, Collections.emptyList(), Collections.emptyList(), null,
			Collections.emptyMap(), stacks, limit);
	}
}
