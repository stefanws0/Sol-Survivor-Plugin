package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.components.LineComponent;
import nl.dexanddice.solsurvivor.core.EarlierSettings;
import nl.dexanddice.solsurvivor.core.WaveClock;

/**
 * Both clock readings in one movable box: the tick counter large, the wave timer small beneath it, unless the player
 * chose another look in an earlier release.
 */
class CombinedWaveTimerOverlay extends WaveClockOverlay
{
	@Inject
	CombinedWaveTimerOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		super(plugin, config);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		WaveClock.View view = plugin.waveClock();
		boolean counter = view.counterVisible(config.quietOnSol());
		boolean timer = view.timerVisible(config.quietOnSol());
		if (!combined() || !plugin.inArena() || !(counter || timer))
		{
			return null;
		}
		background(view);
		Color tick = numberColour(view);
		// The counter runs from entering the arena, before any pick has started the timer: until then the box shows the
		// counter alone, rather than a blank time.
		if (timer)
		{
			title(view);
		}
		// The labelled rows, and the time large, are an earlier release's choices, kept for a player who made them.
		if (EarlierSettings.clockInRows(plugin::earlier))
		{
			if (counter)
			{
				panelComponent.getChildren().add(LineComponent.builder().left("Ticks").leftColor(Color.WHITE)
					.right(ticks(view)).rightColor(tick).build());
			}
			if (timer)
			{
				panelComponent.getChildren().add(LineComponent.builder().left("Time").leftColor(Color.WHITE)
					.right(time(view)).rightColor(Color.WHITE).build());
			}
		}
		else if (!timer)
		{
			number(ticks(view), 30f, tick);
		}
		else if (!counter)
		{
			number(time(view), 30f);
		}
		else if (EarlierSettings.timeLarge(plugin::earlier))
		{
			number(time(view), 30f);
			number("Tick " + ticks(view), 13f, tick);
		}
		else
		{
			number(ticks(view), 30f, tick);
			number(time(view), 13f);
		}
		return draw(graphics, 132);
	}
}
