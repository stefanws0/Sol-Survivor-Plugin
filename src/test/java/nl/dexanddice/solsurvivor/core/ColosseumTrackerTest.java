package nl.dexanddice.solsurvivor.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

/**
 * The tracker and the link it gives, fed mocked game data: the chat messages, NPC spawns, orb graphics and modifier picks
 * the RuneLite layer passes on. The links are compared with the fixtures under src/test/resources/fixtures, which the
 * site's own tests read too (test/link/pluginFixtures.test.ts), so that both sides agree on the format.
 */
public class ColosseumTrackerTest
{
	private static final int FREM_BERSERKER_ID = 12816;

	private ColosseumTracker tracker;
	private final Map<Modifier, Integer> varbits = new EnumMap<>(Modifier.class);
	private final List<SeenNpc> arena = new ArrayList<>();

	@Before
	public void setUp()
	{
		tracker = new ColosseumTracker();
		varbits.clear();
		arena.clear();
	}

	private int level(Modifier m)
	{
		return varbits.getOrDefault(m, 0);
	}

	/** An NPC that spawns on `tick` and is in the arena from then on, at its south-west tile. */
	private void spawn(int index, int npcId, int x, int y, int tick)
	{
		tracker.onNpcSpawned(index, npcId, tick);
		arena.add(new SeenNpc(index, npcId, new GridTile(x, y)));
	}

	private ArenaState capture(int tick, int x, int y) throws ExportException
	{
		return tracker.capture(tick, new GridTile(x, y), arena, this::level);
	}

	private static String fixture(String name) throws IOException
	{
		try (InputStream in = ColosseumTrackerTest.class.getResourceAsStream("/fixtures/" + name))
		{
			if (in == null)
			{
				throw new IOException("no fixture " + name);
			}
			return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
		}
	}

	private static void assertMatchesFixture(String name, ArenaState state) throws IOException
	{
		String json = fixture(name);
		assertEquals(json, StatePayload.toJson(state));
		assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.US_ASCII)), StatePayload.encode(state));
	}

	/** Wave 4 from its first tick: a shaman, a Manticore and a frem, after picking Quartet with Reentry II and Totemic taken. */
	private void startWave4()
	{
		tracker.onModifierOptions(Modifier.MANTIMAYHEM.getId(), Modifier.QUARTET.getId(), Modifier.DOOM.getId(),
			(1 << Modifier.REENTRY.getId()) | (1 << Modifier.TOTEMIC.getId()));
		tracker.onModifierChosen(2);
		varbits.put(Modifier.REENTRY, 2);

		tracker.onGameMessage("<col=ef1020>Wave: 4</col>", 100);
		spawn(11, NpcKind.SHAMAN_ID, 13, 20, 100);
		spawn(12, NpcKind.MANTICORE_ID, 16, 24, 100);
		spawn(13, FREM_BERSERKER_ID, 7, 14, 100);
	}

	@Test
	public void aWaveStartLink() throws Exception
	{
		startWave4();
		assertTrue(tracker.isWaveStartTick(100));

		ArenaState state = capture(100, 7, 15);
		assertTrue(state.isWaveStart());
		assertEquals(Integer.valueOf(4), state.getWave());
		assertNull(state.getWaveTick());
		assertTrue(state.isModifiersComplete());
		assertEquals(2, state.getNpcs().size());
		assertMatchesFixture("wave4-start.json", state);
	}

	@Test
	public void aMidWaveLinkAfterTheReinforcements() throws Exception
	{
		startWave4();
		tracker.onOrb(12, OrbStyle.MAGIC);
		tracker.onOrb(12, OrbStyle.MAGIC);
		tracker.onOrb(12, OrbStyle.RANGED);
		tracker.onOrb(12, OrbStyle.MELEE);
		tracker.onNpcAnimation(11, NpcKind.SHAMAN_ID, AttackAnimations.SHAMAN_CAST, 160);
		tracker.onNpcAnimation(11, NpcKind.SHAMAN_ID, 4321, 163);
		tracker.onNpcAnimation(11, NpcKind.SHAMAN_ID, AttackAnimations.SHAMAN_CAST, 165);
		spawn(14, NpcKind.JAGUAR_ID, 15, 1, 167);
		spawn(15, NpcKind.SHAMAN_ID, 16, 1, 167);
		tracker.onNpcAnimation(14, NpcKind.JAGUAR_ID, AttackAnimations.JAGUAR_CLAWS, 168);

		ArenaState state = capture(170, 7, 8);
		assertFalse(state.isWaveStart());
		// The shaman last cast 5 ticks ago, its 2nd; the Jaguar clawed 2 ticks ago; the new shaman has not cast.
		assertEquals(Integer.valueOf(5), state.getNpcs().get(0).getSinceAttack());
		assertEquals(Integer.valueOf(2), state.getNpcs().get(0).getAttacks());
		assertNull(state.getNpcs().get(1).getAttacks());
		assertEquals(Integer.valueOf(2), state.getNpcs().get(2).getSinceAttack());
		assertNull(state.getNpcs().get(3).getSinceAttack());
		assertEquals(Integer.valueOf(0), state.getNpcs().get(3).getAttacks());
		assertEquals(Integer.valueOf(70), state.getWaveTick());
		assertTrue(state.isReinforcementsArrived());
		assertEquals(NpcKind.SERPENT_SHAMAN, state.getNpcs().get(0).getKind());
		assertEquals(NpcKind.REINFORCEMENT_SHAMAN, state.getNpcs().get(3).getKind());
		assertEquals("m", state.getNpcs().get(1).getOrbs());
		assertMatchesFixture("wave4-mid.json", state);
	}

	@Test
	public void aJavelinsSpecialFixesThePhaseOfItsThrowsWhenItsSpawnWasMissed() throws Exception
	{
		tracker.onGameMessage("<col=ef1020>Wave: 9</col>", 100);
		// Already in the arena when the plugin started: no spawn seen, so its count is unknown.
		arena.add(new SeenNpc(21, NpcKind.JAVELIN_ID, new GridTile(10, 25)));
		tracker.onNpcAnimation(21, NpcKind.JAVELIN_ID, AttackAnimations.JAVELIN_THROW, 120);
		assertNull(capture(122, 7, 15).getNpcs().get(0).getAttacks());

		tracker.onNpcAnimation(21, NpcKind.JAVELIN_ID, AttackAnimations.JAVELIN_SPECIAL, 125);
		tracker.onNpcAnimation(21, NpcKind.JAVELIN_ID, AttackAnimations.JAVELIN_THROW, 130);
		ExportNpc javelin = capture(133, 7, 15).getNpcs().get(0);
		assertEquals(Integer.valueOf(6), javelin.getAttacks());
		assertEquals(Integer.valueOf(3), javelin.getSinceAttack());
		assertTrue(StatePayload.toJson(capture(133, 7, 15)).contains("\"attacks\":[[3,6]]"));
	}

	@Test
	public void aJavelinsSpecialOutOfStepWithItsCountMovesItToTheNextMultipleOfFive() throws Exception
	{
		tracker.onGameMessage("<col=ef1020>Wave: 9</col>", 100);
		spawn(21, NpcKind.JAVELIN_ID, 10, 25, 100);
		tracker.onNpcAnimation(21, NpcKind.JAVELIN_ID, AttackAnimations.JAVELIN_THROW, 110);
		tracker.onNpcAnimation(21, NpcKind.JAVELIN_ID, AttackAnimations.JAVELIN_SPECIAL, 115);
		assertEquals(Integer.valueOf(5), capture(116, 7, 15).getNpcs().get(0).getAttacks());
	}

	@Test
	public void aWaveStartLinkAndALinkThatKnowsNoAttacksHaveNoAttacks() throws Exception
	{
		startWave4();
		tracker.onNpcAnimation(11, NpcKind.SHAMAN_ID, AttackAnimations.SHAMAN_CAST, 100);
		assertFalse(StatePayload.toJson(capture(100, 7, 15)).contains("attacks"));

		tracker.reset();
		arena.clear();
		arena.add(new SeenNpc(11, NpcKind.SHAMAN_ID, new GridTile(13, 20)));
		tracker.onNpcAnimation(11, NpcKind.SHAMAN_ID, 4321, 140);
		assertFalse(StatePayload.toJson(capture(150, 7, 15)).contains("attacks"));
	}

	@Test
	public void aMidWaveLinkBeforeTheReinforcementsGivesTheTickAndNoTimer() throws Exception
	{
		startWave4();
		tracker.onOrb(12, OrbStyle.RANGED);

		ArenaState state = capture(130, 7, 9);
		assertFalse(state.isReinforcementsArrived());
		assertEquals("ur", state.getNpcs().get(1).getOrbs());
		String json = StatePayload.toJson(state);
		assertTrue(json, json.contains("\"waveTick\":30"));
		assertFalse(json, json.contains("timer"));
	}

	@Test
	public void aPluginStartedMidRunExportsWhatItCanRead() throws Exception
	{
		// No wave message and no modifier selector seen: the wave and the tick are left to the site, and only the
		// modifiers with tiers are known, from their varbits.
		varbits.put(Modifier.MANTIMAYHEM, 3);
		varbits.put(Modifier.SOLARFLARE, 1);
		arena.add(new SeenNpc(21, NpcKind.JAVELIN_ID, new GridTile(13, 20)));
		arena.add(new SeenNpc(22, NpcKind.MANTICORE_ID, new GridTile(18, 22)));
		arena.add(new SeenNpc(23, NpcKind.MINOTAUR_RED_FLAG_ID, new GridTile(16, 3)));
		tracker.onOrb(22, OrbStyle.MELEE);

		ArenaState state = capture(5000, 20, 20);
		assertNull(state.getWave());
		assertNull(state.getWaveTick());
		assertFalse(state.isModifiersComplete());
		assertTrue(state.isMm3());
		// A Minotaur only comes as a reinforcement, so the reinforcements have come.
		assertTrue(state.isReinforcementsArrived());
		assertMatchesFixture("late-start.json", state);
	}

	@Test
	public void aShamanAlreadyThereFromWave7OnIsTheReinforcementOne() throws Exception
	{
		tracker.onGameMessage("Wave 9 completed!", 10);
		tracker.onGameMessage("Wave: 10", 20);
		arena.add(new SeenNpc(31, NpcKind.SHAMAN_ID, new GridTile(16, 1)));
		assertEquals(NpcKind.REINFORCEMENT_SHAMAN, capture(90, 7, 15).getNpcs().get(0).getKind());
	}

	@Test
	public void aCompletedWaveEndsTheWave() throws Exception
	{
		startWave4();
		tracker.onGameMessage("Wave 4 completed! Wave duration: <col=ef1020>1:02.40</col>", 204);
		assertFalse(tracker.isWaveStarted());
		ArenaState state = capture(210, 7, 15);
		assertNull(state.getWave());
		assertFalse(state.isWaveStart());
	}

	/** Ranged gear as the RuneLite layer reads it from the worn items, the varps and the skills. */
	private static Equipment rangedGear()
	{
		return new Equipment()
			.item("head", new Equipment.Item(27235, "Masori mask (f)", 1))
			.item("cape", new Equipment.Item(28947, "Dizana's quiver", 1))
			.item("weapon", new Equipment.Item(20997, "Twisted bow", 1))
			.item("ammo", new Equipment.Item(11212, "Dragon arrow", 812))
			.switchItem("weapon", new Equipment.Item(22325, "Scythe of vitur", 1), 5)
			.switchItem("body", new Equipment.Item(26384, "Torva platebody", 1))
			.bonuses(20997, new int[]{0, 0, 0, 0, 70, 0, 20, 0, 1})
			.bonuses(22325, new int[]{70, 125, 30, -6, 0, 75, 0, 0, 1})
			.weapon(5, 3, 1)
			.prayer(9, 77)
			.stat("ranged", 112, 99)
			.stat("hitpoints", 88, 99)
			.stat("prayer", 77, 77);
	}

	@Test
	public void aLinkWithSolarflaresOrbsAndTheGear() throws Exception
	{
		tracker.onModifierOptions(Modifier.SOLARFLARE.getId(), Modifier.BEES.getId(), Modifier.MYOPIA.getId(),
			(1 << Modifier.SOLARFLARE.getId()) | (1 << Modifier.DYNAMIC_DUO.getId()));
		tracker.onModifierChosen(2);
		varbits.put(Modifier.SOLARFLARE, 2);
		varbits.put(Modifier.BEES, 1);
		tracker.onGameMessage("Wave: 8", 300);
		spawn(41, NpcKind.JAVELIN_ID, 13, 20, 300);
		spawn(42, NpcKind.SHOCKWAVE_ID, 20, 12, 300);
		// The orbs are NPCs the link does not hold: their tiles go apart, in the order the game lists them.
		spawn(43, NpcKind.SOLARFLARE_ORB_ID, 11, 7, 300);
		spawn(44, NpcKind.SOLARFLARE_ORB_ID, 26, 11, 300);
		spawn(45, NpcKind.SOLARFLARE_ORB_ID, 22, 26, 300);
		spawn(46, NpcKind.SOLARFLARE_ORB_ID, 7, 22, 300);

		ArenaState state = capture(312, 7, 15);
		assertEquals(4, state.getSolarflareOrbs().size());
		assertEquals(2, state.getNpcs().size());
		String json = fixture("wave8-gear.json");
		assertEquals(json, StatePayload.toJson(state, rangedGear()));
		assertEquals(Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.US_ASCII)), StatePayload.encode(state, rangedGear()));
	}

	@Test
	public void thePrayerValuesStayInTheSitesRange() throws Exception
	{
		arena.add(new SeenNpc(1, NpcKind.SHAMAN_ID, new GridTile(13, 20)));
		ArenaState state = capture(1, 7, 15);
		String none = StatePayload.toJson(state, new Equipment().prayer(-5, 0));
		assertTrue(none, none.contains("\"prayer\":{\"bonus\":0}"));
		String high = StatePayload.toJson(state, new Equipment().prayer(90, 120));
		assertTrue(high, high.contains("\"prayer\":{\"start\":99,\"bonus\":73}"));
	}

	@Test
	public void theWeaponsStyleNamesGoWithTheAttackStyle() throws Exception
	{
		arena.add(new SeenNpc(1, NpcKind.SHAMAN_ID, new GridTile(13, 20)));
		Equipment gear = new Equipment().weapon(5, 3, 3).styleNames(Arrays.asList("Ranging", "Ranging", "Other", "Longrange"));
		String json = StatePayload.toJson(capture(1, 7, 15), gear);
		assertTrue(json, json.contains("\"attackStyle\":3,\"styleNames\":[\"Ranging\",\"Ranging\",\"Other\",\"Longrange\"],\"stats\""));
	}

	@Test
	public void theSwitchesStopAtTheInventorysSize() throws Exception
	{
		arena.add(new SeenNpc(1, NpcKind.SHAMAN_ID, new GridTile(13, 20)));
		Equipment gear = new Equipment();
		for (int i = 0; i < Equipment.MAX_SWITCHES + 3; i++)
		{
			gear.switchItem("ring", new Equipment.Item(i + 1, "Ring", 1));
		}
		assertEquals(Equipment.MAX_SWITCHES, gear.getSwitches().size());
		String none = StatePayload.toJson(capture(1, 7, 15), new Equipment());
		assertTrue(none, !none.contains("switches"));
	}

	@Test
	public void anItemNameIsEscapedToAscii() throws Exception
	{
		arena.add(new SeenNpc(1, NpcKind.SHAMAN_ID, new GridTile(13, 20)));
		String json = StatePayload.toJson(capture(1, 7, 15), new Equipment().item("weapon", new Equipment.Item(1, "Caf\u00e9 \"bow\"", 1)));
		assertTrue(json, json.contains("[1,\"Caf\\u00e9 \\\"bow\\\"\",1]"));
	}

	@Test
	public void solHereditsWaveIsNotExported()
	{
		tracker.onGameMessage("Sol Heredit jumps down from his seat...", 50);
		arena.add(new SeenNpc(1, NpcKind.MANTICORE_ID, new GridTile(16, 16)));
		assertExportFails("Sol Heredit", () -> capture(60, 7, 15));
	}

	@Test
	public void anEmptyArenaIsNotExported()
	{
		tracker.onGameMessage("Wave: 1", 1);
		arena.add(new SeenNpc(1, FREM_BERSERKER_ID, new GridTile(7, 14)));
		assertExportFails("no NPCs", () -> capture(1, 7, 15));
	}

	@Test
	public void aPlayerOutsideTheArenaIsNotExported()
	{
		arena.add(new SeenNpc(1, NpcKind.SHAMAN_ID, new GridTile(13, 20)));
		assertExportFails("not in the Colosseum", () -> tracker.capture(1, ArenaGeometry.fromRegion(2, 2), arena, this::level));
	}

	@Test
	public void npcsOffTheGridAreLeftOut() throws Exception
	{
		arena.addAll(Arrays.asList(new SeenNpc(1, NpcKind.SHAMAN_ID, new GridTile(13, 20)), new SeenNpc(2, NpcKind.JAVELIN_ID, new GridTile(40, 20))));
		assertEquals(1, capture(1, 7, 15).getNpcs().size());
	}

	@Test
	public void resetForgetsTheRun() throws Exception
	{
		startWave4();
		tracker.reset();
		varbits.clear();
		ArenaState state = capture(100, 7, 15);
		assertFalse(state.isWaveStart());
		assertTrue(state.getModifiers().isEmpty());
		assertFalse(state.isModifiersComplete());
	}

	@Test
	public void theLinkPointsAtTheSite() throws Exception
	{
		startWave4();
		ArenaState state = capture(100, 7, 15);
		String expected = "https://sol.dexanddice.nl/#s=" + StatePayload.encode(state);
		assertEquals(expected, StatePayload.link("https://sol.dexanddice.nl", state));
		assertEquals(expected, StatePayload.link(" https://sol.dexanddice.nl/#s=old ", state));
	}

	private interface Capture
	{
		void run() throws ExportException;
	}

	private static void assertExportFails(String reason, Capture capture)
	{
		try
		{
			capture.run();
			fail("expected an ExportException saying " + reason);
		}
		catch (ExportException e)
		{
			assertTrue(e.getMessage(), e.getMessage().contains(reason));
		}
	}
}
