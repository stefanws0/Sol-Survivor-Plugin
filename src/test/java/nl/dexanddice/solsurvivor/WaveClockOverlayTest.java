package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import nl.dexanddice.solsurvivor.core.WaveClock;
import org.junit.Test;

/** The real RuneLite overlays rendered headlessly, including settings that must never leave duplicate boxes. */
public class WaveClockOverlayTest
{
	private final ClockPlugin plugin = new ClockPlugin();
	private final Settings config = new Settings();
	private final TickCounterOverlay ticks = new TickCounterOverlay(plugin, config);
	private final WaveTimerOverlay time = new WaveTimerOverlay(plugin, config);
	private final CombinedWaveTimerOverlay both = new CombinedWaveTimerOverlay(plugin, config);

	@Test
	public void onlyEnabledOverlaysDrawAndCombiningRequiresBothToBeEnabled()
	{
		plugin.clock.onTick(90);
		plugin.clock.onModifierChosen(7, 100, 0, true);
		assertNull(render(ticks));
		assertNull(render(time));
		assertNull(render(both));
		config.tick = true;
		config.combine = true;
		assertNotNull(render(ticks));
		assertNull(render(time));
		assertNull(render(both));
		config.timer = true;
		assertNull(render(ticks));
		assertNull(render(time));
		assertNotNull(render(both));
		config.tick = false;
		assertNull(render(ticks));
		assertNotNull(render(time));
		assertNull(render(both));
	}

	@Test
	public void theCounterAloneShowsBeforeAnyPickAndTheCombinedBoxLeavesOutTheTime()
	{
		plugin.clock.onTick(90);
		config.tick = true;
		config.timer = true;
		assertNotNull(render(ticks));
		assertNull(render(time));
		config.combine = true;
		assertFits(both, 170, 120);
		config.rows = true;
		assertFits(both, 170, 90);
	}

	@Test
	public void separateAndEveryCombinedLookFitAtTheirNativeSizeAndMoveAsRuneLitePanels()
	{
		plugin.clock.onTick(90);
		plugin.clock.onModifierChosen(7, 100, System.currentTimeMillis() - 22_000, true);
		plugin.clock.onTick(137);
		config.tick = true;
		config.timer = true;
		assertFits(ticks, 80, 50);
		assertFits(time, 160, 100);
		config.combine = true;
		assertFits(both, 170, 120);
		config.large = SolSurvivorConfig.LargeTimerNumber.TIME;
		assertFits(both, 170, 120);
		config.rows = true;
		assertFits(both, 170, 90);
		config.loop = true;
		assertFits(both, 170, 90);
		plugin.clock.resetCounter(137);
		plugin.clock.onTick(138);
		assertFits(both, 170, 90);
	}

	@Test
	public void eachLoopPositionTakesItsOwnColourOnlyWhenLoopingWithColoursOn()
	{
		plugin.clock.onTick(100);
		config.tick = true;
		assertEquals(Color.WHITE, ticks.numberColour(plugin.clock.view()));
		config.loop = true;
		assertEquals(Color.WHITE, ticks.numberColour(plugin.clock.view()));
		assertNull(ticks.loopColour(plugin.clock.view()));
		config.colours = SolSurvivorConfig.LoopColours.NUMBER;
		Set<Color> seen = new HashSet<>();
		Color[] expected = {config.tickLoopColour1(), config.tickLoopColour2(), config.tickLoopColour3(), config.tickLoopColour4(),
			config.tickLoopColour5()};
		for (int count = 1; count <= 10; count++)
		{
			plugin.clock.onTick(100 + count);
			Color colour = ticks.numberColour(plugin.clock.view());
			assertEquals(expected[(count - 1) % 5], colour);
			seen.add(colour);
			assertNotNull(render(ticks));
		}
		assertEquals(5, seen.size());
		config.colours = SolSurvivorConfig.LoopColours.BOX;
		assertEquals(Color.WHITE, ticks.numberColour(plugin.clock.view()));
		assertNotNull(ticks.loopColour(plugin.clock.view()));
		assertFits(ticks, 80, 50);
		config.timer = true;
		config.combine = true;
		assertFits(both, 170, 120);
		config.loop = false;
		assertNull(ticks.loopColour(plugin.clock.view()));
	}

	@Test
	public void allLooksHideOutsideTheArenaAndOnSolsWaveAndRetainTheCompletedWave()
	{
		config.tick = true;
		config.timer = true;
		plugin.clock.onTick(90);
		plugin.clock.onModifierChosen(11, 100, 0, true);
		plugin.clock.onGameMessage("Wave 11 completed!", 22_200);
		assertNotNull(render(ticks));
		assertNotNull(render(time));
		plugin.arena = false;
		assertNull(render(ticks));
		assertNull(render(time));
		config.combine = true;
		assertNull(render(both));
		plugin.arena = true;
		plugin.clock.onModifierChosen(12, 200, 0, true);
		assertNull(render(both));
		config.combine = false;
		assertNull(render(ticks));
		assertNull(render(time));
	}

	private static Dimension render(WaveClockOverlay overlay)
	{
		Graphics2D graphics = new BufferedImage(300, 200, BufferedImage.TYPE_INT_ARGB).createGraphics();
		try
		{
			// RuneLite's panel uses the preceding frame's child measurements for its background and returned size.
			// Warm that layout before checking or capturing it, including a change between stacked and row layouts.
			overlay.render(graphics);
			overlay.render(graphics);
			return overlay.render(graphics);
		}
		finally
		{
			graphics.dispose();
		}
	}

	private static void assertFits(WaveClockOverlay overlay, int width, int height)
	{
		Dimension size = render(overlay);
		assertNotNull(size);
		assertTrue(size.toString(), size.width > 0 && size.width <= width && size.height > 0 && size.height <= height);
		assertTrue(overlay.isMovable());
	}

	/** Writes the actual rendered overlays, at native size, for the release's visual review. */
	public static void main(String[] args) throws Exception
	{
		File folder = new File(args[0]);
		folder.mkdirs();
		WaveClockOverlayTest scene = new WaveClockOverlayTest();
		scene.plugin.clock.onTick(90);
		scene.plugin.clock.onModifierChosen(7, 100, System.currentTimeMillis() - 22_000, true);
		scene.plugin.clock.onTick(137);
		scene.config.tick = true;
		scene.config.timer = true;
		save(scene.ticks, new File(folder, "tick-counter.png"));
		save(scene.time, new File(folder, "wave-timer.png"));
		scene.config.combine = true;
		save(scene.both, new File(folder, "combined-tick.png"));
		scene.config.large = SolSurvivorConfig.LargeTimerNumber.TIME;
		save(scene.both, new File(folder, "combined-time.png"));
		scene.config.rows = true;
		save(scene.both, new File(folder, "combined-rows.png"));
		scene.config.loop = true;
		save(scene.both, new File(folder, "loop.png"));
		scene.config.colours = SolSurvivorConfig.LoopColours.NUMBER;
		scene.config.combine = false;
		for (int position = 1; position <= 5; position++)
		{
			scene.plugin.clock.resetCounter(200);
			scene.plugin.clock.onTick(200 + position);
			save(scene.ticks, new File(folder, "loop-number-" + position + ".png"));
		}
		scene.config.colours = SolSurvivorConfig.LoopColours.BOX;
		for (int position = 1; position <= 5; position++)
		{
			scene.plugin.clock.resetCounter(300);
			scene.plugin.clock.onTick(300 + position);
			save(scene.ticks, new File(folder, "loop-box-" + position + ".png"));
		}
		scene.config.colours = SolSurvivorConfig.LoopColours.OFF;
		scene.config.combine = true;
		scene.plugin.clock.onGameMessage("Wave 7 completed!", System.currentTimeMillis());
		save(scene.both, new File(folder, "wave-done.png"));
	}

	private static void save(WaveClockOverlay overlay, File file) throws Exception
	{
		Dimension size = render(overlay);
		BufferedImage image = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setColor(new Color(0x282828));
		graphics.fillRect(0, 0, size.width, size.height);
		overlay.render(graphics);
		graphics.dispose();
		ImageIO.write(image, "png", file);
	}

	private static final class ClockPlugin extends SolSurvivorPlugin
	{
		private final WaveClock clock = new WaveClock();
		private boolean arena = true;

		@Override
		boolean inArena()
		{
			return arena;
		}

		@Override
		WaveClock.View waveClock()
		{
			return clock.view();
		}
	}

	private static final class Settings implements SolSurvivorConfig
	{
		private boolean tick;
		private boolean timer;
		private boolean combine;
		private boolean rows;
		private boolean loop;
		private LargeTimerNumber large = LargeTimerNumber.TICK;
		private LoopColours colours = LoopColours.OFF;

		@Override
		public boolean tickCounter() { return tick; }
		@Override
		public boolean waveTimer() { return timer; }
		@Override
		public boolean combineWaveTimers() { return combine; }
		@Override
		public CombinedTimerLayout combinedTimerLayout() { return rows ? CombinedTimerLayout.ROWS : CombinedTimerLayout.STACKED; }
		@Override
		public TickCounterCounts tickCounterCounts() { return loop ? TickCounterCounts.LOOP : TickCounterCounts.UP_FROM_ZERO; }
		@Override
		public LargeTimerNumber largeTimerNumber() { return large; }
		@Override
		public LoopColours loopColours() { return colours; }
	}
}
