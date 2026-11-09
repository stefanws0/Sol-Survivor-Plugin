package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

/** Potions by their doses, and the doses drunk between two looks at the inventory. */
public class PotionsTest
{
	@Test
	public void aPotionCountsItsDosesAndAnythingElseNothing()
	{
		Map<String, Integer> doses = Potions.doses(Arrays.asList(
			new ChestItem("Saradomin brew(4)", 3), new ChestItem("Shark", 4), new ChestItem("Saradomin brew(1)", 1),
			new ChestItem("Super restore(3)", 2), new ChestItem("Twisted bow", 1)));
		Map<String, Integer> expected = new LinkedHashMap<>();
		expected.put("Saradomin brew", 13);
		expected.put("Super restore", 6);
		assertEquals(expected, doses);
	}

	@Test
	public void onlyWhatWentDownWasDrunk()
	{
		Map<String, Integer> before = new LinkedHashMap<>();
		before.put("Saradomin brew", 13);
		before.put("Super restore", 6);
		before.put("Stamina potion", 4);
		Map<String, Integer> after = new LinkedHashMap<>();
		after.put("Saradomin brew", 9);
		after.put("Super restore", 6);
		Map<String, Integer> expected = new LinkedHashMap<>();
		expected.put("Saradomin brew", 4);
		expected.put("Stamina potion", 4);
		assertEquals(expected, Potions.drunk(before, after));
		assertEquals(Collections.emptyMap(), Potions.drunk(after, before));
	}
}
