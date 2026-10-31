package nl.dexanddice.solsurvivor;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.swing.Icon;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Doom's stacks in a box of their own that the player places, as the tick counter is (Stefan's request of
 * 2026-10-30): the same icon and "4/10" as by the hitpoints orb (DoomOverlay), red on the last stack, starting at the
 * top left with RuneLite's other boxes and moved with Alt and drag. It shows only with Doom's place set to a movable box.
 */
class DoomBoxOverlay extends Overlay
{
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private final Icon icon = Icons.of("modifier-doom", DoomOverlay.ICON);

	@Inject
	DoomBoxOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		super(plugin);
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WaveView view = plugin.waveView();
		if (!DoomOverlay.shows(config, plugin, view, SolSurvivorConfig.DoomPlace.MOVABLE))
		{
			return null;
		}
		graphics.setFont(FontManager.getRunescapeSmallFont());
		DoomOverlay.paint(graphics, icon, view, 0, 0);
		return new Dimension(DoomOverlay.width(graphics, view), DoomOverlay.HEIGHT);
	}
}
