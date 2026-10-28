package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JLabel;
import nl.dexanddice.solsurvivor.core.GridTile;
import nl.dexanddice.solsurvivor.core.WaveLog;
import org.junit.Test;

/**
 * Past waves fold into their heading in the arena, to keep the panel clean while the player plays, and show as before
 * in the lobby and everywhere else out of it (the user's ask of 2026-10-27). And the fixes of the UX review of
 * 2026-10-27 that the 1.0.0 review took: Arena now lit only when it has a wave to open (F1), every Watch saying it needs
 * Upload past runs before the press (F2), Getting started moving on in the arena (F4), a short help (F5), and "died"
 * for a death (F6).
 */
public class SolSurvivorPanelTest
{
	private static final List<SolSurvivorPanel.Offer> NO_OFFERS = new ArrayList<>();

	@Test
	public void pastWavesShowOutOfTheArena()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		assertEquals(1, count(panel, "Latest run"));
		assertEquals(1, count(panel, "Wave 5"));
		assertEquals(0, count(panel, "6 waves"));
	}

	@Test
	public void inTheArenaOnlyTheHeadingShows()
	{
		SolSurvivorPanel panel = panel();
		arena(panel, true);
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
		panel.showState(true, false, false, 12, "Wave 12", "Under way", NO_OFFERS, "", runs(true));
		assertFalse(button(panel, "Arena now").isEnabled());
		assertTrue(button(panel, "Arena now").getToolTipText().contains("Sol Heredit"));
	}

	@Test
	public void everyWatchSaysItNeedsUploadPastRunsWhileThatIsOff()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		WaveCards cards = panel.cards;
		cards.showReport(new WaveCards.Report(5, 5, "1:15", 10, 0, 0, 0, 0, 0, Collections.emptyList(), null, new int[] {5}, new int[] {0}, null));
		cards.showRecap(new WaveCards.Recap(6, 6, "0:25 into the wave", null, Collections.emptyList(), 99));
		// Off, as it is until the player switches it on: secondary, with the line under it.
		assertFalse(button(cards.reportCard, "Watch wave 5").isPrimary());
		assertFalse(button(cards.recapCard, "Watch from tick 0").isPrimary());
		assertEquals(1, shows(cards.reportCard, "Watch needs Upload past runs"));
		assertEquals(1, shows(cards.recapCard, "Watch needs Upload past runs"));
		assertEquals(1, shows(cards.reportPage, "Watch needs Upload past runs"));

		panel.showSends(true, false, false);
		assertTrue(button(cards.reportCard, "Watch wave 5").isPrimary());
		assertTrue(button(cards.recapCard, "Watch from tick 0").isPrimary());
		assertEquals(0, shows(cards.reportCard, "Watch needs Upload past runs"));
		assertEquals(0, shows(cards.recapCard, "Watch needs Upload past runs"));
		assertEquals(0, shows(cards.reportPage, "Watch needs Upload past runs"));

		// While the wave under way's report waits, it has no Watch to speak for.
		cards.showReportPending();
		panel.showSends(false, false, false);
		assertEquals(0, shows(cards.reportCard, "Watch"));
	}

	@Test
	public void gettingStartedMovesOnInTheArenaWhereWhatYouShareStaysShut()
	{
		SolSurvivorPanel panel = panel();
		lobby(panel);
		panel.showGettingStarted(true, true, false);
		assertEquals(1, shows(panel, "Try a sample wave"));
		assertEquals(1, shows(panel, "Walk into the Fortis Colosseum"));
		// What you share opens by itself beside the card, out of the arena.
		assertEquals(1, shows(panel, "Each is off until you switch it on"));

		arena(panel, false);
		panel.showGettingStarted(true, true, true);
		assertEquals(0, shows(panel, "Try a sample wave"));
		assertEquals(1, shows(panel, "You are in the Colosseum."));
		assertEquals(1, shows(panel, "below"));
		assertEquals(1, shows(panel, "Solve wave"));
		assertEquals(0, shows(panel, "Each is off until you switch it on"));

		lobby(panel);
		panel.showGettingStarted(true, true, false);
		assertEquals(1, shows(panel, "Try a sample wave"));
		assertEquals(1, shows(panel, "Each is off until you switch it on"));
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

	private static SolSurvivorPanel panel()
	{
		return new SolSurvivorPanel((SolSurvivorPanel.Actions) Proxy.newProxyInstance(SolSurvivorPanelTest.class.getClassLoader(),
			new Class<?>[] {SolSurvivorPanel.Actions.class}, (p, m, a) -> null));
	}

	/** Wave 6 under way, or wave 5 done, in the arena. */
	private static void arena(SolSurvivorPanel panel, boolean underWay)
	{
		panel.showState(true, underWay, false, 6, underWay ? "Wave 6" : "Wave 6 next", underWay ? "Under way" : "Between waves", NO_OFFERS,
			"", runs(underWay));
	}

	private static void lobby(SolSurvivorPanel panel)
	{
		panel.showState(false, false, false, null, "Not in the Colosseum", "Enter it to open a wave.", NO_OFFERS, "", runs(false));
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
