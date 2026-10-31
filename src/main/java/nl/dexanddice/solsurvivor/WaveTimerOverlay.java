package nl.dexanddice.solsurvivor;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import nl.dexanddice.solsurvivor.core.WaveClock;

/** The wave's elapsed real time from modifier confirmation, independently movable from the tick number. */
class WaveTimerOverlay extends WaveClockOverlay
{
	@Inject
	WaveTimerOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		super(plugin, config);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		WaveClock.View view = plugin.waveClock();
		if (!config.waveTimer() || combined() || !plugin.inArena() || !view.timerVisible())
		{
			return null;
		}
		title(view);
		number(time(view), 24f);
		return draw(graphics, 116);
	}
}
