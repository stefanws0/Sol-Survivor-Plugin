package nl.dexanddice.solsurvivor;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.swing.Icon;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import nl.dexanddice.solsurvivor.core.Modifier;

/**
 * "Volatility reminder", a setting that is on by default: while the player holds Volatility, the modifier's picture
 * over each NPC that dies, for 5 ticks from its death, as a reminder that it will explode (the user's ask of 2026-10-26,
 * so that Fortis Colosseum's own reminder can be switched off for this). It marks no tile and no blast's reach: the game
 * shows the explosion itself as it goes off. Every NPC that dies gets one, as the planner has every NPC killed explode.
 */
class VolatilityOverlay extends Overlay
{
	/** How long a reminder stays, in game ticks from the NPC's death. */
	static final int TICKS = 5;
	private static final int ICON = 28;

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private final Icon icon = Icons.of("modifier-volatility", ICON);
	private final List<Reminder> reminders = new ArrayList<>();

	/** Where an NPC died, how high its middle stands, and the last tick its reminder shows on. */
	private static final class Reminder
	{
		final LocalPoint at;
		final int height;
		final int until;

		Reminder(LocalPoint at, int height, int until)
		{
			this.at = at;
			this.height = height;
			this.until = until;
		}
	}

	@Inject
	VolatilityOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** An NPC died on this tick: a reminder over it, with the setting on and Volatility held. On the client thread. */
	void died(NPC npc)
	{
		if (!config.volatilityReminder() || !plugin.inArena() || !plugin.waveView().held.containsKey(Modifier.VOLATILITY))
		{
			return;
		}
		LocalPoint at = npc.getLocalLocation();
		if (at != null)
		{
			reminders.add(new Reminder(at, npc.getLogicalHeight() / 2, client.getTickCount() + TICKS - 1));
		}
	}

	/** Forgets every reminder: the player left the arena, or the plugin stopped. On the client thread. */
	void clear()
	{
		reminders.clear();
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		int tick = client.getTickCount();
		reminders.removeIf(r -> tick > r.until);
		if (reminders.isEmpty() || !config.volatilityReminder() || !plugin.inArena())
		{
			return null;
		}
		int plane = client.getTopLevelWorldView().getPlane();
		for (Reminder r : reminders)
		{
			Point p = Perspective.localToCanvas(client, r.at, plane, r.height);
			if (p != null)
			{
				icon.paintIcon(null, graphics, p.getX() - ICON / 2, p.getY() - ICON / 2);
			}
		}
		return null;
	}
}
