package nl.dexanddice.solsurvivor;

import static org.junit.Assert.assertEquals;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JLabel;
import org.junit.Test;

/**
 * Past waves fold into their heading in the arena, to keep the panel clean while the player plays, and show as before
 * in the lobby and everywhere else out of it (the user's ask of 2026-10-27).
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

	private static SolSurvivorPanel panel()
	{
		return new SolSurvivorPanel((SolSurvivorPanel.Actions) Proxy.newProxyInstance(SolSurvivorPanelTest.class.getClassLoader(),
			new Class<?>[] {SolSurvivorPanel.Actions.class}, (p, m, a) -> null));
	}

	/** Wave 6 under way, or wave 5 done, in the arena. */
	private static void arena(SolSurvivorPanel panel, boolean underWay)
	{
		panel.showState(true, false, 6, underWay ? "Wave 6" : "Wave 6 next", underWay ? "Under way" : "Between waves", NO_OFFERS, "",
			runs(underWay));
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
