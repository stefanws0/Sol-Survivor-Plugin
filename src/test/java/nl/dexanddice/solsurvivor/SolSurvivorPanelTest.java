package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import nl.dexanddice.solsurvivor.core.GridTile;
import nl.dexanddice.solsurvivor.core.WaveLog;
import nl.dexanddice.solsurvivor.core.WaveReview;
import nl.dexanddice.solsurvivor.core.WaveRoster;
import org.junit.Test;

/**
 * Past waves fold into their heading in the arena, to keep the panel clean while the player plays, and show as before
 * in the lobby and everywhere else out of it (the user's ask of 2026-10-27). And the fixes of the UX review of
 * 2026-10-27 that the 1.0.0 review took: Arena now lit only when it has a wave to open (F1), every Watch saying it needs
 * Upload past runs before the press (F2), Getting started moving on in the arena (F4), a short help (F5), and "died"
 * for a death (F6). And the calmer panel of 2026-10-28: the death report on a page of its own with Home back to the rest,
 * the report a line out of the arena, the greyed pick its head alone (F9), and nothing of the report during a wave (F13).
 */
public class SolSurvivorPanelTest
{
	private static final List<SolSurvivorPanel.Offer> NO_OFFERS = new ArrayList<>();

	@Test
	public void pastWavesShowOutOfTheArena()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(1, count(panel, "Latest run"));
		assertEquals(1, count(panel, "Wave 5"));
		assertEquals(0, count(panel, "6 waves"));
	}

	@Test
	public void inTheArenaOnlyTheHeadingShows()
	{
		SolSurvivorPanel panel = panel();
		arena(panel, true);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(0, count(panel, "Latest run"));
		assertEquals(1, count(panel, "6 waves"));
		arena(panel, false);
		assertEquals(0, count(panel, "Latest run"));
	}

	@Test
	public void aClickOnTheHeadingOpensThemUntilThePlayerLeaves()
	{
		SolSurvivorPanel panel = panel();
		arena(panel, true);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		clickPastWaves(panel);
		// Opened, the newest run shows its waves, so that the wave under way's start is one click away.
		assertEquals(1, count(panel, "Latest run"));
		assertEquals(1, count(panel, "Wave 5"));
		assertEquals(0, count(panel, "6 waves"));
		// It stays open as the wave ends.
		arena(panel, false);
		assertEquals(1, count(panel, "Latest run"));
		clickPastWaves(panel);
		assertEquals(0, count(panel, "Latest run"));
		clickPastWaves(panel);

		lobby(panel);
		assertEquals(1, count(panel, "Latest run"));
		// A click out of the arena folds nothing: there the heading is only a heading.
		clickPastWaves(panel);
		assertEquals(1, count(panel, "Latest run"));
		// Back in, it starts shut again.
		arena(panel, false);
		assertEquals(0, count(panel, "Latest run"));
	}

	@Test
	public void arenaNowIsLitOnlyWhileAWaveItPlansIsUnderWay()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		assertFalse(button(panel, "Arena now").isEnabled());
		// In the arena between waves there are no NPCs to open: greyed, and its tooltip says where a wave's start is.
		arena(panel, false);
		assertFalse(button(panel, "Arena now").isEnabled());
		assertTrue(button(panel, "Arena now").getToolTipText().contains("once the wave starts"));
		arena(panel, true);
		assertTrue(button(panel, "Arena now").isEnabled());
		// Sol Heredit's wave is under way, but the site plans waves 1 to 11.
		panel.showState(true, false, 12, "Wave 12", "Under way", NO_OFFERS, "", runs(true));
		assertFalse(button(panel, "Arena now").isEnabled());
		assertTrue(button(panel, "Arena now").getToolTipText().contains("Sol Heredit"));
	}

	@Test
	public void everyWatchSaysItNeedsUploadPastRunsWhileThatIsOff()
	{
		SolSurvivorPanel panel = panel();
		arena(panel, false);
		WaveCards cards = panel.cards;
		cards.showReview(report(5), null, false);
		// Off, as it is until the player switches it on: secondary, with the line under it.
		assertFalse(button(cards.reportCard, "Watch wave 5").isPrimary());
		assertEquals(1, shows(cards.reportCard, "Watch needs Upload past runs"));
		assertEquals(1, shows(cards.reportPage, "Watch needs Upload past runs"));
		lobby(panel);
		cards.showReview(report(6), recap(6), true);
		assertFalse(button(cards.deathPage, "Watch from tick 0").isPrimary());
		assertEquals(1, shows(cards.deathPage, "Watch needs Upload past runs"));

		panel.showSends(true, false, false);
		assertTrue(button(cards.deathPage, "Watch from tick 0").isPrimary());
		assertEquals(0, shows(cards.deathPage, "Watch needs Upload past runs"));
		assertEquals(0, shows(cards.reportPage, "Watch needs Upload past runs"));
		arena(panel, false);
		cards.showReview(report(5), null, false);
		assertTrue(button(cards.reportCard, "Watch wave 5").isPrimary());
		assertEquals(0, shows(cards.reportCard, "Watch needs Upload past runs"));
	}

	@Test
	public void aDeathOpensItsReportOnAPageOfItsOwnWithHomeBackToTheRest()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		WaveCards cards = panel.cards;
		cards.showReview(report(6), recap(6), true);
		// The death report takes the panel's place as the player comes out of the arena.
		assertEquals(1, count(panel, "Death report"));
		assertEquals(1, count(panel, "You died on wave 6"));
		assertEquals(0, count(panel, "Latest run"));

		button(panel, "‹ Home").doClick();
		assertEquals(SolSurvivorPanel.Tab.WAVE, panel.shownTab());
		// At home the death is one line that opens it again, and neither the recap nor the report shows there.
		assertEquals(1, count(panel, "You died on wave 6"));
		assertEquals(0, count(panel, "Death report"));
		assertEquals(0, shows(panel, "Watch from tick"));
		assertEquals(0, shows(panel, "Wave 6 report"));
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(1, count(panel, "Latest run"));
		panel.showTab(SolSurvivorPanel.Tab.WAVE);
		// The same death shown again, as a setting changes, stays at home.
		cards.showReview(report(6), recap(6), true);
		assertEquals(0, count(panel, "Death report"));

		button(cards.deathRow, "Open").doClick();
		assertEquals(1, count(panel, "Death report"));
		// The wave's report is a line on it, whose page goes Back to the death report.
		button(panel, "Open").doClick();
		assertEquals(1, shows(panel, "Copy report"));
		button(panel, "‹ Back").doClick();
		assertEquals(1, count(panel, "Death report"));

		// Back in the arena, the death and its pages go.
		arena(panel, false);
		cards.showReview(null, null, false);
		assertEquals(0, count(panel, "Death report"));
		assertEquals(0, count(panel, "You died on wave 6"));
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(1, count(panel, "6 waves"));
	}

	@Test
	public void outOfTheArenaTheReportIsALineWhoseOpenShowsItWhole()
	{
		SolSurvivorPanel panel = panel();
		arena(panel, false);
		WaveCards cards = panel.cards;
		cards.showReview(report(5), null, false);
		assertEquals(1, shows(panel, "Watch wave 5"));

		lobby(panel);
		cards.showReview(report(5), null, true);
		assertEquals(1, count(panel, "Wave 5 report"));
		assertEquals(0, shows(panel, "Watch wave 5"));
		assertEquals(1, shows(panel, "No mistakes counted"));
		button(cards.reportCard, "Open").doClick();
		// The page holds the figures the short card has, and the rest.
		assertEquals(1, shows(panel, "damage"));
		assertEquals(1, shows(panel, "Watch wave 5"));
		button(panel, "‹ Back").doClick();
		assertEquals(1, count(panel, "Wave 5 report"));
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(1, count(panel, "Latest run"));
	}

	@Test
	public void aResultShowsOnThePageThatShowsAndAPageOpensWithNone()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		WaveCards cards = panel.cards;
		cards.showReview(report(6), recap(6), true);
		panel.showResult("Uploading the run to watch it back…");
		assertEquals(1, shows(panel, "Uploading the run"));
		button(panel, "Open").doClick();
		assertEquals(0, shows(panel, "Uploading the run"));
	}

	@Test
	public void duringAWaveThePickIsItsHeadAloneAndTheReportWaits()
	{
		SolSurvivorPanel panel = panel();
		arena(panel, true);
		panel.cards.showReview(null, null, false);
		assertEquals(1, shows(panel, "Opens when Minimus makes his offers."));
		assertEquals(0, shows(panel, "Open the pick"));
		assertEquals(0, shows(panel, "Comes when the wave ends"));

		panel.showState(true, false, 7, "Wave 7 next", "Between waves", Arrays.asList(new SolSurvivorPanel.Offer("myopia", "Myopia"),
			new SolSurvivorPanel.Offer("frailty", "Frailty"), new SolSurvivorPanel.Offer("blasphemy", "Blasphemy")), "", runs(false));
		assertEquals(1, shows(panel, "Open the pick"));
	}

	@Test
	public void gettingStartedMovesOnInTheArenaWhereWhatYouShareStaysShut()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		panel.showGettingStarted(true, true, false);
		assertEquals(1, shows(panel, "Try a sample wave"));
		assertEquals(1, shows(panel, "Walk into the Fortis Colosseum"));
		// What you share, on the Past tab, is open while the card shows out of the arena.
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(1, shows(panel, "Each is off until you switch it on"));

		arena(panel, false);
		panel.showGettingStarted(true, true, true);
		assertEquals(0, shows(panel, "Each is off until you switch it on"));
		panel.showTab(SolSurvivorPanel.Tab.WAVE);
		assertEquals(0, shows(panel, "Try a sample wave"));
		assertEquals(1, shows(panel, "You are in the Colosseum."));
		assertEquals(1, shows(panel, "below"));
		assertEquals(1, shows(panel, "Solve wave"));

		lobby(panel);
		panel.showGettingStarted(true, true, false);
		assertEquals(1, shows(panel, "Try a sample wave"));
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		assertEquals(1, shows(panel, "Each is off until you switch it on"));
	}

	@Test
	public void inTheGameIsTheScreenTabsAndSwitchesEachFeature()
	{
		List<String> pressed = new ArrayList<>();
		SolSurvivorPanel panel = new SolSurvivorPanel((SolSurvivorPanel.Actions) Proxy.newProxyInstance(
			SolSurvivorPanelTest.class.getClassLoader(), new Class<?>[] {SolSurvivorPanel.Actions.class}, (p, m, a) ->
			{
				if (m.getName().equals("setInGame"))
				{
					pressed.add(a[0] + "=" + a[1]);
				}
				return null;
			}));
		boolean[] on = new boolean[SolSurvivorPanel.IN_GAME.length];
		Arrays.fill(on, true);
		on[7] = false;
		lobby(panel);
		panel.showInGame(on);
		// On its own tab, Screen, open, with how many are on; the Wave tab shows first.
		assertEquals(SolSurvivorPanel.Tab.WAVE, panel.shownTab());
		assertEquals(0, shows(panel, "12 of 13 on"));
		panel.showTab(SolSurvivorPanel.Tab.SCREEN);
		assertEquals(SolSurvivorPanel.Tab.SCREEN, panel.shownTab());
		assertEquals(1, shows(panel, "12 of 13 on"));
		assertEquals(1, shows(panel, "Wave box"));
		assertEquals(1, shows(panel, "My attack range"));

		button(panel, "Off").doClick();
		assertEquals(Collections.singletonList("myRange=true"), pressed);

		// It stays during a wave, on its tab; a wave starting brings the Wave tab back.
		arena(panel, true);
		assertEquals(SolSurvivorPanel.Tab.WAVE, panel.shownTab());
		panel.showTab(SolSurvivorPanel.Tab.SCREEN);
		assertEquals(1, shows(panel, "12 of 13 on"));
	}

	@Test
	public void screensPresetsSwitchTheDrawingFeaturesAndLeaveTheGamesOwnAlone()
	{
		List<String> pressed = new ArrayList<>();
		SolSurvivorPanel panel = new SolSurvivorPanel((SolSurvivorPanel.Actions) Proxy.newProxyInstance(
			SolSurvivorPanelTest.class.getClassLoader(), new Class<?>[] {SolSurvivorPanel.Actions.class}, (p, m, a) ->
			{
				if (m.getName().equals("setInGame"))
				{
					pressed.add(a[0] + "=" + a[1]);
				}
				return null;
			}));
		boolean[] on = new boolean[SolSurvivorPanel.IN_GAME.length];
		Arrays.fill(on, true);
		lobby(panel);
		panel.showInGame(on);
		panel.showTab(SolSurvivorPanel.Tab.SCREEN);
		// Every switch on, Hide the pillars and Left-click Bank-all too: the drawing features are All's.
		assertEquals("All", SolSurvivorPanel.presetOf(on));
		button(panel, "Minimal").doClick();
		// Each drawing feature but the wave box goes off; Hide the pillars and Left-click Bank-all stay as they are.
		assertEquals(10, pressed.size());
		assertTrue(pressed.contains("npcHitpoints=false"));
		assertFalse(pressed.contains("waveBox=false"));
		assertFalse(pressed.stream().anyMatch(x -> x.startsWith("hidePillars") || x.startsWith("leftClickBankAll")));
		boolean[] minimal = new boolean[SolSurvivorPanel.IN_GAME.length];
		minimal[0] = true;
		assertEquals("Minimal", SolSurvivorPanel.presetOf(minimal));
		minimal[5] = true;
		assertNull(SolSurvivorPanel.presetOf(minimal));
	}

	@Test
	public void theHelpIsFiveShortLines()
	{
		String[] lines = SolSurvivorPanel.HELP.split("<br><br>");
		assertEquals(5, lines.length);
		for (String line : lines)
		{
			assertTrue(line, line.replaceAll("<[^>]+>", "").split(" ").length <= 30);
		}
	}

	@Test
	public void pastWaveActionsHaveAVisibleSharedMenuAndKeepTheirAnswerBesideTheWave()
	{
		List<String> calls = new ArrayList<>();
		AtomicReference<SolSurvivorPanel> current = new AtomicReference<>();
		SolSurvivorPanel panel = new SolSurvivorPanel((SolSurvivorPanel.Actions) Proxy.newProxyInstance(getClass().getClassLoader(),
			new Class<?>[] {SolSurvivorPanel.Actions.class}, (p, m, a) ->
			{
				calls.add(m.getName() + ":" + a[0]);
				current.get().showResult("Copied wave " + a[0] + ".");
				return null;
			}));
		current.set(panel);
		lobby(panel);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		Stone.Button more = accessibleButton(panel, "More actions for wave 5");
		JPopupMenu menu = more.getComponentPopupMenu();
		assertEquals(2, more.getParent().getComponentCount());
		assertTrue(more.isFocusable());
		assertTrue(more.isFocusPainted());
		assertEquals(menu, ((JComponent) labels(panel, "Wave 5").get(0)).getComponentPopupMenu());
		assertEquals("Open at the reinforcements", ((JMenuItem) menu.getComponent(0)).getText());
		assertTrue(menu.getComponent(0).isEnabled());
		assertFalse(accessibleButton(panel, "More actions for wave 6").getComponentPopupMenu().getComponent(0).isEnabled());
		((JMenuItem) menu.getComponent(1)).doClick();
		assertEquals(Collections.singletonList("copyWaveLog:5"), calls);
		// Upload changes rebuild the runs; the answer must stay under the wave rather than on a detached label.
		panel.showReplayBusy(true);
		Container wave = accessibleButton(panel, "More actions for wave 5").getParent().getParent().getParent();
		assertEquals(1, shows(wave, "Copied wave 5."));
	}

	@Test
	public void delayedAnswersReturnToTheActionThatStartedThem()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		button(panel, "Watch").doClick();
		String upload = panel.resultTarget();
		JPopupMenu menu = accessibleButton(panel, "More actions for wave 5").getComponentPopupMenu();
		((JMenuItem) menu.getComponent(1)).doClick();
		String copied = panel.resultTarget();
		panel.showResult("Copied wave 5.", copied);
		panel.showResult("Opened the run.", upload);
		Container wave = accessibleButton(panel, "More actions for wave 5").getParent().getParent().getParent();
		assertEquals(0, shows(wave, "Opened the run."));
		Container run = button(panel, "Watch").getParent().getParent().getParent();
		assertEquals(1, shows(run, "Opened the run."));
		// A non-past result must not be attributed to either earlier action.
		panel.showResult("Opened the current arena.", null);
		run = button(panel, "Watch").getParent().getParent().getParent();
		assertEquals(0, shows(run, "Opened the current arena."));
	}

	@Test
	public void runAnswersStayBelowTheRunHeadAcrossUploadRebuilds()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		button(panel, "Watch").doClick();
		panel.showResult("Uploading this run…");
		panel.showReplayBusy(true);
		Container head = button(panel, "Watch").getParent().getParent().getParent();
		assertEquals(1, shows(head, "Uploading this run…"));
		assertFalse(button(panel, "Watch").isEnabled());
		panel.showResult("Opened this run.");
		panel.showReplayBusy(false);
		head = button(panel, "Watch").getParent().getParent().getParent();
		assertEquals(1, shows(head, "Opened this run."));
		arena(panel, true);
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		clickPastWaves(panel);
		panel.showTab(SolSurvivorPanel.Tab.WAVE);
		button(panel, "Arena now").doClick();
		panel.showTab(SolSurvivorPanel.Tab.PAST);
		head = button(panel, "Watch").getParent().getParent().getParent();
		assertEquals(0, shows(head, "Opened this run."));
	}

	@Test
	public void theShortReportGroupsRepeatedMistakesButItsPageKeepsEveryTick()
	{
		List<WaveCards.Row> rows = Arrays.asList(
			new WaveCards.Row(14, "Magic orb past Protect from Missiles", "12", "npc-manticore", true, "Manticore: attacks past your prayer"),
			new WaveCards.Row(18, "Ranged orb with no protection prayer", "0", "npc-manticore", false, "Manticore: attacks past your prayer"),
			new WaveCards.Row(25, "Magic orb past Protect from Missiles", "45", "npc-manticore", true, "Manticore: attacks past your prayer"),
			new WaveCards.Row(30, "Special landed on your tile", "17", "npc-javelin", true),
			new WaveCards.Row(36, "Idle for 5 ticks", "5t", "hourglass", false, "Idle with the weapon ready"),
			new WaveCards.Row(44, "Idle for 7 ticks", "7t", "hourglass", false, "Idle with the weapon ready"));
		List<WaveCards.Group> groups = WaveCards.grouped(rows);
		assertEquals(3, groups.size());
		assertEquals("×3 · 57 damage · t14–t25", groups.get(0).figures());
		assertEquals("×2 · 12 idle ticks · t36–t44", groups.get(2).figures());
		SolSurvivorPanel panel = panel();
		arena(panel, false);
		panel.cards.showReview(new WaveCards.Report(5, 5, "1:15", 10, 0, 0, 3, 74, 12, rows, null, new int[] {5}, new int[] {6}, null), null, false);
		assertEquals(1, shows(panel.cards.reportCard, "Manticore: attacks past your prayer"));
		assertEquals(1, shows(panel.cards.reportCard, "Special landed on your tile"));
		assertEquals(1, shows(panel.cards.reportCard, "×3 · 57 damage"));
		assertEquals(0, shows(panel.cards.reportCard, "more in the full report"));
		button(panel.cards.reportCard, "All mistakes").doClick();
		assertEquals(2, shows(panel.cards.reportPage, "Magic orb past Protect from Missiles"));
		assertEquals(1, shows(panel.cards.reportPage, "Ranged orb with no protection prayer"));
		assertEquals(1, shows(panel.cards.reportPage, "t18"));
	}

	@Test
	public void shortGroupsFollowTheRecordedAttackerRatherThanItsChangingDirectionalName()
	{
		List<WaveCards.Group> groups = WaveCards.grouped(Arrays.asList(
			new WaveCards.Row(10, "East Manticore's magic orb", "12", "npc-manticore", true, "East Manticore: attacks past your prayer", "PAST_PRAYER:14"),
			new WaveCards.Row(15, "West Manticore's ranged orb", "8", "npc-manticore", true, "West Manticore: attacks past your prayer", "PAST_PRAYER:14"),
			new WaveCards.Row(18, "East Manticore's magic orb", "5", "npc-manticore", true, "East Manticore: attacks past your prayer", "PAST_PRAYER:21")));
		assertEquals(2, groups.size());
		assertEquals("×2 · 20 damage · t10–t15", groups.get(0).figures());
		assertEquals("×1 · 5 damage · t18", groups.get(1).figures());
	}

	@Test
	public void theWaveListUsesTheSameShortNamesAndToComeAsTheWaveBox()
	{
		SolSurvivorPanel panel = panel();
		Map<nl.dexanddice.solsurvivor.core.Modifier, Integer> held = Collections.emptyMap();
		List<WaveRoster.Line> roster = WaveRoster.thisWave(5, held, new WaveRoster.Alive(), false, true, 0, false);
		panel.cards.showWave(new WaveView(5, true, roster, null, Collections.emptyList(), Collections.emptyList(), null, held, 0, 0),
			true, true, false, false, false);
		assertEquals(0, shows(panel.cards.waveBody, "Serpent shaman"));
		assertEquals(0, shows(panel.cards.waveBody, "Javelin Colossus"));
		assertTrue(shows(panel.cards.waveBody, "Shaman") > 0);
		assertTrue(shows(panel.cards.waveBody, "Javelin") > 0);
		assertTrue(shows(panel.cards.waveBody, "to come") > 0);
		assertEquals(0, shows(panel.cards.waveBody, "still to come"));
	}

	@Test
	public void aBlockedRecapHitFitsItsNarrowColumnAndExplainsItselfOnHover()
	{
		JLabel blocked = (JLabel) WaveCards.recapHit("blocked");
		assertEquals("–", blocked.getText());
		assertEquals("Blocked: the attack did no damage.", blocked.getToolTipText());
		assertTrue(blocked.getPreferredSize().width < 26);
		assertEquals("–", ((JLabel) WaveCards.recapHit("–")).getText());
	}

	@Test
	public void aDeathReadsDiedAndALeavingLeft()
	{
		assertEquals(", died", SolSurvivorPlugin.endName(wave(0, WaveLog.End.LEFT)));
		assertEquals(", left", SolSurvivorPlugin.endName(wave(40, WaveLog.End.LEFT)));
		assertEquals("", SolSurvivorPlugin.endName(wave(40, WaveLog.End.COMPLETED)));
	}

	/** Wave 6 logged for a tick on which the player had `hitpoints`, then ended as `end`. */
	private static WaveLog.Past wave(int hitpoints, WaveLog.End end)
	{
		WaveLog log = new WaveLog();
		log.start(6, 100, Collections.emptyMap(), true, "test", null);
		log.record(100, new WaveLog.PlayerTick(new GridTile(7, 15), hitpoints, 50, Collections.emptyList(), -1, WaveLog.TARGET_NONE),
			Collections.emptyList());
		log.finish(end);
		return log.current();
	}

	@Test
	public void theDeathInStepsHoldsEachHitAndTheAttackPastThePrayerWithTheHitpointsBeforeAndAfter()
	{
		List<WaveReview.RecapLine> lines = Arrays.asList(
			new WaveReview.RecapLine(28, 18, "ranged", "East Javelin's throw", "blocked"),
			new WaveReview.RecapLine(29, 32, "ranged", "Nothing hit you", ""),
			new WaveReview.RecapLine(32, 14, "ranged", "Shaman's spell", "18"),
			new WaveReview.RecapLine(33, 14, "ranged", "Berserker's melee attack, past your prayer", ""),
			new WaveReview.RecapLine(34, 3, "ranged", "Berserker's melee attack", "11"),
			new WaveReview.RecapLine(35, 0, "ranged", "Seer's magic attack", "8"));
		assertEquals("[32 Shaman's spell 32→14 18, 33 Berserker's melee attack, past your prayer 14→14 null past, "
			+ "34 Berserker's melee attack 14→3 11, 35 Seer's magic attack 3→0 8]", WaveCards.steps(lines).toString());
		// A recap where nothing hit and nothing got past the prayer has no steps, and its table stands alone.
		assertTrue(WaveCards.steps(lines.subList(0, 2)).isEmpty());
	}

	@Test
	public void theWaveStripColoursEachMistakeByItsAttacksStyleOrWhatItWas()
	{
		assertEquals(WaveCards.WaveStrip.MAGIC, WaveCards.WaveStrip.colourOf(row("magic", "npc-manticore")));
		assertEquals(WaveCards.WaveStrip.RANGED, WaveCards.WaveStrip.colourOf(row("ranged", "npc-archer")));
		assertEquals(WaveCards.WaveStrip.MELEE, WaveCards.WaveStrip.colourOf(row("melee", "npc-manticore")));
		assertEquals(Stone.ORANGE, WaveCards.WaveStrip.colourOf(row(null, "modifier-volatility")));
		assertEquals(Stone.DIM, WaveCards.WaveStrip.colourOf(row(null, "hourglass")));
	}

	/** Draws the panel at the sidebar's width to build/panel-*.png, to look at: Wave with a report, Screen, and a death. */
	@Test
	public void thePanelDrawsItsTabsTheReportsStripAndTheDeathsSteps() throws IOException
	{
		SolSurvivorPanel panel = panel();
		arena(panel, false);
		boolean[] on = new boolean[SolSurvivorPanel.IN_GAME.length];
		Arrays.fill(on, true);
		panel.showInGame(on);
		List<WaveCards.Row> rows = Arrays.asList(
			new WaveCards.Row(11, "Archer's ranged attack, with Protect from Magic up", "1", "npc-archer", true, "Archer: attacks past your prayer", "a", "ranged"),
			new WaveCards.Row(319, "Manticore's magic orb: Protect from Magic a tick late", "31", "npc-manticore", true, "Manticore: prayer a tick off", "b", "magic"),
			new WaveCards.Row(320, "Manticore's melee orb: Protect from Melee a tick late", "28", "npc-manticore", true, "Manticore: prayer a tick off", "b", "melee"),
			new WaveCards.Row(422, "Volatility blast from the Manticore you killed", "24", "modifier-volatility", true));
		panel.cards.showReview(new WaveCards.Report(5, 5, "5:01", 76, 2, 1, 0, 84, 0, rows, null, new int[] {5}, new int[] {4}, null, 500), null, false);
		draw(panel, "build/panel-wave.png");
		panel.showTab(SolSurvivorPanel.Tab.SCREEN);
		draw(panel, "build/panel-screen.png");
		List<WaveReview.RecapLine> lines = Arrays.asList(
			new WaveReview.RecapLine(28, 18, "ranged", "East Javelin's throw", "blocked"),
			new WaveReview.RecapLine(32, 14, "ranged", "Shaman's spell", "18"),
			new WaveReview.RecapLine(33, 14, "ranged", "Berserker's melee attack, past your prayer", ""),
			new WaveReview.RecapLine(34, 3, "ranged", "Berserker's melee attack", "11"),
			new WaveReview.RecapLine(35, 0, "ranged", "Seer's magic attack", "8"));
		lobby(panel);
		panel.cards.showReview(report(6), new WaveCards.Recap(6, 6, 35, "0:21", "The Seer's magic attack hit you for 8 at 3 hitpoints.", lines, 99), true);
		draw(panel, "build/panel-death.png");
	}

	private static WaveCards.Row row(String style, String icon)
	{
		return new WaveCards.Row(10, "x", "5", icon, true, "x", "x", style);
	}

	/** Lays the panel out at 242 px, the sidebar's width, and draws it to `path`. */
	private static void draw(SolSurvivorPanel panel, String path) throws IOException
	{
		panel.setSize(242, 100);
		layOut(panel);
		int height = Math.max(100, panel.getPreferredSize().height);
		panel.setSize(242, height);
		layOut(panel);
		java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(242, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
		java.awt.Graphics2D g = image.createGraphics();
		try
		{
			g.setColor(Stone.BACKGROUND);
			g.fillRect(0, 0, 242, height);
			panel.printAll(g);
		}
		finally
		{
			g.dispose();
		}
		new java.io.File("build").mkdirs();
		javax.imageio.ImageIO.write(image, "png", new java.io.File(path));
	}

	private static void layOut(Container c)
	{
		c.doLayout();
		for (Component child : c.getComponents())
		{
			if (child instanceof Container)
			{
				layOut((Container) child);
			}
		}
	}

	/** The report of wave `wave`, a clean one. */
	private static WaveCards.Report report(int wave)
	{
		return new WaveCards.Report(wave, wave, "1:15", 10, 0, 0, 0, 0, 0, Collections.emptyList(), null, new int[] {wave}, new int[] {0}, null);
	}

	private static WaveCards.Recap recap(int wave)
	{
		return new WaveCards.Recap(wave, wave, 42, "0:25", null, Collections.emptyList(), 99);
	}

	private static SolSurvivorPanel panel()
	{
		return new SolSurvivorPanel((SolSurvivorPanel.Actions) Proxy.newProxyInstance(SolSurvivorPanelTest.class.getClassLoader(),
			new Class<?>[] {SolSurvivorPanel.Actions.class}, (p, m, a) -> null));
	}

	/** Wave 6 under way, or wave 5 done, in the arena. */
	private static void arena(SolSurvivorPanel panel, boolean underWay)
	{
		panel.showState(true, underWay, 6, underWay ? "Wave 6" : "Wave 6 next", underWay ? "Under way" : "Between waves", NO_OFFERS,
			"", runs(underWay));
	}

	private static void lobby(SolSurvivorPanel panel)
	{
		panel.showState(false, false, null, "Not in the Colosseum", "Enter it to open a wave.", NO_OFFERS, "", runs(false));
	}

	/** One run of waves 1 to 5, and wave 6 under way. */
	private static List<SolSurvivorPanel.RunGroup> runs(boolean underWay)
	{
		List<SolSurvivorPanel.WaveRow> waves = new ArrayList<>();
		waves.add(new SolSurvivorPanel.WaveRow(6, 6, underWay ? "Under way" : "0:25", underWay, true, false, null));
		for (int w = 5; w >= 1; w--)
		{
			waves.add(new SolSurvivorPanel.WaveRow(w, w, "1:15", false, true, true, null));
		}
		return Arrays.asList(new SolSurvivorPanel.RunGroup(1, "Latest run", "Waves 1–6", null, waves));
	}

	/** Clicks Past waves' heading, as the player would. */
	private static void clickPastWaves(SolSurvivorPanel panel)
	{
		Component heading = labels(panel, "Past waves").get(0).getParent().getParent();
		for (MouseListener l : heading.getMouseListeners())
		{
			l.mouseClicked(new MouseEvent(heading, MouseEvent.MOUSE_CLICKED, 0, 0, 1, 1, 1, false));
		}
	}

	/** How many labels with the text show in the panel: in it, and they and every one of their parents visible. */
	private static int count(SolSurvivorPanel panel, String text)
	{
		int n = 0;
		for (JLabel l : labels(panel, text))
		{
			Component c = l;
			while (c != null && c != panel && c.isVisible())
			{
				c = c.getParent();
			}
			n += c == panel ? 1 : 0;
		}
		return n;
	}

	/** How many labels and buttons whose words hold `text` show under `root`: they and every parent up to it visible. */
	private static int shows(Container root, String text)
	{
		int n = 0;
		for (Component c : all(root))
		{
			String words = c instanceof JLabel ? ((JLabel) c).getText() : c instanceof AbstractButton ? ((AbstractButton) c).getText() : null;
			if (words == null || !words.contains(text))
			{
				continue;
			}
			Component up = c;
			while (up != null && up != root && up.isVisible())
			{
				up = up.getParent();
			}
			n += up == root ? 1 : 0;
		}
		return n;
	}

	/** The button with the words `text` under `root`. */
	private static Stone.Button button(Container root, String text)
	{
		for (Component c : all(root))
		{
			if (c instanceof Stone.Button && text.equals(((Stone.Button) c).getText()))
			{
				return (Stone.Button) c;
			}
		}
		throw new AssertionError("No button " + text);
	}

	private static Stone.Button accessibleButton(Container root, String name)
	{
		for (Component component : all(root))
		{
			if (component instanceof Stone.Button && name.equals(component.getAccessibleContext().getAccessibleName()))
			{
				return (Stone.Button) component;
			}
		}
		throw new AssertionError("No button named " + name);
	}

	private static List<Component> all(Container in)
	{
		List<Component> out = new ArrayList<>();
		for (Component c : in.getComponents())
		{
			out.add(c);
			if (c instanceof Container)
			{
				out.addAll(all((Container) c));
			}
		}
		return out;
	}

	private static List<JLabel> labels(Container in, String text)
	{
		List<JLabel> out = new ArrayList<>();
		for (Component c : in.getComponents())
		{
			if (c instanceof JLabel && text.equals(((JLabel) c).getText()))
			{
				out.add((JLabel) c);
			}
			if (c instanceof Container)
			{
				out.addAll(labels((Container) c, text));
			}
		}
		return out;
	}
}
