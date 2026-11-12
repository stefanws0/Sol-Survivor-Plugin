package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** The settings that left the list on 2026-11-12, as an earlier release's player stored them: each is kept as set. */
public class EarlierSettingsTest
{
	private final Map<String, String> stored = new HashMap<>();

	@Test
	public void anInstallThatNeverSetThemCountsEveryKindFromTwoIdleTicksAndStacksTheClockWithTheTickLarge()
	{
		assertEquals(EnumSet.allOf(WaveReview.Kind.class), EarlierSettings.counted(stored::get));
		assertEquals(WaveReview.IDLE_FROM, EarlierSettings.idleFrom(stored::get));
		assertFalse(EarlierSettings.clockInRows(stored::get));
		assertFalse(EarlierSettings.timeLarge(stored::get));
		// The defaults as RuneLite stored them for an earlier install read the same.
		for (String key : new String[]{"countPastPrayer", "countLateFlicks", "countSpecials", "countVolatility", "countSolarflareSand", "countIdle"})
		{
			stored.put(key, "true");
		}
		stored.put("idleFrom", "2");
		stored.put("combinedTimerLayout", "STACKED");
		stored.put("largeTimerNumber", "TICK");
		assertEquals(EnumSet.allOf(WaveReview.Kind.class), EarlierSettings.counted(stored::get));
		assertEquals(2, EarlierSettings.idleFrom(stored::get));
		assertFalse(EarlierSettings.clockInRows(stored::get));
		assertFalse(EarlierSettings.timeLarge(stored::get));
	}

	@Test
	public void eachMistakeSwitchAPlayerTurnedOffStaysOff()
	{
		String[] keys = {"countPastPrayer", "countLateFlicks", "countSpecials", "countVolatility", "countSolarflareSand", "countIdle"};
		WaveReview.Kind[] kinds = {WaveReview.Kind.PAST_PRAYER, WaveReview.Kind.LATE_FLICK, WaveReview.Kind.SPECIAL,
			WaveReview.Kind.VOLATILITY, WaveReview.Kind.SOLARFLARE_SAND, WaveReview.Kind.IDLE};
		assertEquals(WaveReview.Kind.values().length, kinds.length);
		for (int i = 0; i < keys.length; i++)
		{
			stored.clear();
			stored.put(keys[i], "false");
			assertEquals(keys[i], EnumSet.complementOf(EnumSet.of(kinds[i])), EarlierSettings.counted(stored::get));
		}
	}

	@Test
	public void idleFromStaysAsSetWithinTheRangeTheSettingAllowed()
	{
		stored.put("idleFrom", "5");
		assertEquals(5, EarlierSettings.idleFrom(stored::get));
		stored.put("idleFrom", "1");
		assertEquals(1, EarlierSettings.idleFrom(stored::get));
		stored.put("idleFrom", "99");
		assertEquals(20, EarlierSettings.idleFrom(stored::get));
		stored.put("idleFrom", "soon");
		assertEquals(WaveReview.IDLE_FROM, EarlierSettings.idleFrom(stored::get));
	}

	@Test
	public void theCombinedClocksRowsAndItsLargeTimeStayAsSet()
	{
		stored.put("combinedTimerLayout", "ROWS");
		assertTrue(EarlierSettings.clockInRows(stored::get));
		stored.put("largeTimerNumber", "TIME");
		assertTrue(EarlierSettings.timeLarge(stored::get));
	}
}
