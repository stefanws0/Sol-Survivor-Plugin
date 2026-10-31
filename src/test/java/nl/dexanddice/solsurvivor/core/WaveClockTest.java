package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/** The counter's origins are its first tick in the arena, the player's hotkey and, counting up, the pick; never an NPC. */
public class WaveClockTest
{
	private final WaveClock clock = new WaveClock();

	@Test
	public void theCounterRunsFromItsFirstTickWithNoPickOrPress()
	{
		assertFalse(clock.view().counterVisible());
		clock.onTick(50);
		assertTrue(clock.view().counterVisible());
		assertFalse(clock.view().timerVisible());
		assertEquals("0", clock.view().up());
		for (int count = 1; count <= 37; count++)
		{
			clock.onTick(50 + count);
			assertEquals(Integer.toString(count), clock.view().up());
		}
		clock.onTick(87);
		assertEquals("37", clock.view().up());
	}

	@Test
	public void countingUpThePickAndTheHotkeyEachRestartAtZero()
	{
		clock.onTick(50);
		clock.onModifierChosen(7, 100, 0, true);
		assertEquals("0", clock.view().up());
		clock.onTick(105);
		assertEquals("5", clock.view().up());
		clock.resetCounter(106);
		assertEquals("0", clock.view().up());
		clock.onTick(107);
		assertEquals("1", clock.view().up());
	}

	@Test
	public void theLoopStartsAtOnceAndOnlyTheHotkeyRestartsIt()
	{
		clock.onTick(100);
		for (int count = 1; count <= 13; count++)
		{
			clock.onTick(100 + count);
			assertEquals(Integer.toString((count - 1) % 5 + 1), clock.view().loop(5));
		}
		clock.onModifierChosen(7, 114, 0, false);
		clock.onWaveStarted(7, 0);
		clock.onTick(115);
		assertEquals("5", clock.view().loop(5));
		clock.resetCounter(120);
		clock.onTick(121);
		assertEquals("1", clock.view().loop(5));
		clock.onTick(122);
		assertEquals("2", clock.view().loop(5));
	}

	@Test
	public void theCounterGoesOnPastTheWavesEndWhileTheTimerFreezes()
	{
		clock.onTick(100);
		clock.onModifierChosen(7, 100, 1_000, true);
		clock.onTick(137);
		clock.onGameMessage("Wave 6 completed!", 23_000);
		assertTrue(clock.view().running);
		clock.onGameMessage("<col=ff0000>Wave 7 completed!</col>", 23_200);
		assertEquals("Wave 7 done", clock.view().title());
		assertEquals("0:22", clock.view().time(99_000));
		clock.onTick(140);
		assertEquals("40", clock.view().up());
		clock.onModifierChosen(8, 141, 50_000, true);
		assertEquals("Wave 8", clock.view().title());
		assertEquals("0:00", clock.view().time(50_999));
	}

	@Test
	public void theTimerIsRealSecondsWithMinutesAndHours()
	{
		clock.onModifierChosen(3, 0, 10_000, true);
		assertEquals("0:00", clock.view().time(10_000));
		assertEquals("0:59", clock.view().time(69_999));
		assertEquals("1:00", clock.view().time(70_000));
		assertEquals("59:59", clock.view().time(10_000 + 3_599_000));
		assertEquals("1:00:00", clock.view().time(10_000 + 3_600_000));
		assertEquals("2:03:04", clock.view().time(10_000 + (2 * 3600 + 3 * 60 + 4) * 1000L));
	}

	@Test
	public void aWaveWhosePickWasMissedStartsItsTimerAtItsStartMessage()
	{
		clock.onTick(10);
		clock.onWaveStarted(4, 5_000);
		assertTrue(clock.view().timerVisible());
		assertEquals("Wave 4", clock.view().title());
		assertEquals("0:03", clock.view().time(8_000));
		clock.onModifierChosen(5, 40, 30_000, true);
		clock.onWaveStarted(5, 33_000);
		assertEquals("0:05", clock.view().time(35_000));
	}

	@Test
	public void solsWaveAndLeavingHideBothClocks()
	{
		clock.onTick(10);
		clock.onModifierChosen(11, 100, 0, true);
		clock.onWaveStarted(12, 0);
		assertFalse(clock.view().counterVisible());
		assertFalse(clock.view().timerVisible());
		clock.onTick(120);
		assertFalse(clock.view().counterVisible());
		clock.clear();
		assertFalse(clock.view().counterVisible());
		clock.onModifierChosen(12, 200, 0, true);
		assertFalse(clock.view().counterVisible());
		assertFalse(clock.view().timerVisible());
	}

	@Test
	public void loopLengthsAreBoundedEvenWhenAnImportedSettingBypassesTheSettingsPanel()
	{
		clock.onTick(0);
		clock.onTick(3);
		assertEquals("1", clock.view().loop(0));
		clock.onTick(11);
		assertEquals("1", clock.view().loop(99));
	}
}
