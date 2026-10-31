package nl.dexanddice.solsurvivor;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import nl.dexanddice.solsurvivor.core.WaveClock;

/** The small, independently movable tick number, with no distinguished tick or event; looping, optionally coloured. */
class TickCounterOverlay extends WaveClockOverlay
{
	@Inject
	TickCounterOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		super(plugin, config);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		WaveClock.View view = plugin.waveClock();
		if (!config.tickCounter() || combined() || !plugin.inArena() || !view.counterVisible())
		{
			return null;
		}
		background(view);
		number(ticks(view), 20f, numberColour(view));
		return draw(graphics, 52);
	}
}
