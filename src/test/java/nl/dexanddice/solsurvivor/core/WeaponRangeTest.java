package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** The player's attack range from the weapon worn and its combat option, and Myopia's cut (WeaponRange). */
public class WeaponRangeTest
{
	private static final List<String> BOW = Arrays.asList("Ranging", "Ranging", "Longrange");
	private static final List<String> STAFF = Arrays.asList("Casting", "Casting", "Defensive Casting");
	private static final List<String> SWORD = Arrays.asList("Accurate", "Aggressive", "Aggressive", "Defensive");

	@Test
	public void theKillTablesWeaponsReachWhatTheWikiSays()
	{
		assertEquals(10, WeaponRange.of("Twisted bow", BOW, 1));
		assertEquals(5, WeaponRange.of("Toxic blowpipe", BOW, 1));
		assertEquals(7, WeaponRange.of("Toxic blowpipe", BOW, 3));
		assertEquals(8, WeaponRange.of("Tumeken's shadow", STAFF, 0));
		assertEquals(7, WeaponRange.of("Trident of the swamp (e)", STAFF, 0));
		assertEquals(10, WeaponRange.of("Bow of faerdhinen (c)", BOW, 0));
		assertEquals(2, WeaponRange.of("Noxious halberd", SWORD, 1));
	}

	@Test
	public void anotherWeaponReachesAsItsKindDoes()
	{
		assertEquals(7, WeaponRange.of("Rune crossbow", BOW, 0));
		assertEquals(9, WeaponRange.of("Rune crossbow", BOW, 2));
		assertEquals(3, WeaponRange.of("Dragon dart", BOW, 1));
		assertEquals(6, WeaponRange.of("Craw's bow", BOW, 0));
		assertEquals(7, WeaponRange.of("Accursed sceptre", STAFF, 0));
		assertEquals(2, WeaponRange.of("Crystal halberd", SWORD, 0));
		assertEquals(1, WeaponRange.of("Abyssal whip", SWORD, 0));
		assertEquals(1, WeaponRange.of(null, Collections.<String>emptyList(), 0));
	}

	@Test
	public void anAutocastReachesTen()
	{
		assertEquals(10, WeaponRange.of("Kodai wand", STAFF, 4));
	}

	@Test
	public void myopiaCutsRangesAboveOne()
	{
		assertEquals(Arrays.asList(10, 8, 6, 4), Arrays.asList(WeaponRange.underMyopia(10, 0), WeaponRange.underMyopia(10, 1),
			WeaponRange.underMyopia(10, 2), WeaponRange.underMyopia(10, 3)));
		assertEquals(1, WeaponRange.underMyopia(5, 3));
		assertEquals(1, WeaponRange.underMyopia(1, 3));
		assertTrue(WeaponRange.cardinalOnly(1));
		assertFalse(WeaponRange.cardinalOnly(2));
	}
}
