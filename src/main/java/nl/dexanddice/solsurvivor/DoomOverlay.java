package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import javax.inject.Inject;
import javax.swing.Icon;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;

/**
 * Doom's stacks by the hitpoints orb, a setting that is on by default: Doom's icon and "4/10", the game's own count
 * against the tier's limit, red on the last stack, beside the orb where the player already reads their hitpoints (the
 * third of the five designs of 2026-10-26, the user's choice). It sits just left of the orb, on its row, as the prayer
 * orb sits under it. Facts only, as the buffs bar's own Doom count: nothing of how to play. With Doom's place set to
 * a movable box, DoomBoxOverlay draws the same box where the player puts it instead.
 */
class DoomOverlay extends Overlay
{
	/** The hitpoints orb, in each of the game's layouts of the orbs: by the minimap, without it, and their other two. */
	private static final int[] ORBS = {InterfaceID.Orbs.ORB_HEALTH, InterfaceID.OrbsNomap.ORB_HEALTH, InterfaceID.OrbsOsm.ORB_HEALTH,
		InterfaceID.OrbsOsmNomap.ORB_HEALTH};
	static final int ICON = 16;
	static final int HEIGHT = 20;
	private static final Color GROUND = new Color(0x1a, 0x16, 0x12, 225);
	private static final Color TEXT = Color.WHITE;
	private static final Color LAST = new Color(0xff3c3c);

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private final Icon icon = Icons.of("modifier-doom", ICON);

	@Inject
	DoomOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WaveView view = plugin.waveView();
		if (!shows(config, plugin, view, SolSurvivorConfig.DoomPlace.BY_ORB))
		{
			return null;
		}
		Widget orb = healthOrb();
		if (orb == null)
		{
			return null;
		}
		Rectangle bounds = orb.getBounds();
		graphics.setFont(FontManager.getRunescapeSmallFont());
		int width = width(graphics, view);
		paint(graphics, icon, view, bounds.x - 4 - width, bounds.y + bounds.height / 2 - HEIGHT / 2);
		return null;
	}

	/** Whether Doom's stacks show now in the given place: switched on there, in the arena, mid-wave and Doom held. */
	static boolean shows(SolSurvivorConfig config, SolSurvivorPlugin plugin, WaveView view, SolSurvivorConfig.DoomPlace place)
	{
		return config.doomByOrb() && config.doomPlace() == place && plugin.inArena() && view.underWay && view.doomLimit > 0;
	}

	/** The width of the stacks' box in the graphics' font: its icon, the count and their margins. */
	static int width(Graphics2D graphics, WaveView view)
	{
		return 3 + ICON + 3 + graphics.getFontMetrics().stringWidth(text(view)) + 4;
	}

	/**
	 * Draws the stacks' box with its top left corner at the given point, in the graphics' font: the same box by the orb
	 * and in the movable overlay (DoomBoxOverlay).
	 */
	static void paint(Graphics2D graphics, Icon icon, WaveView view, int left, int top)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		graphics.setColor(GROUND);
		graphics.fillRoundRect(left, top, width(graphics, view), HEIGHT, 6, 6);
		icon.paintIcon(null, graphics, left + 3, top + (HEIGHT - ICON) / 2);
		TextComponent words = new TextComponent();
		words.setText(text(view));
		words.setColor(view.doomLast() ? LAST : TEXT);
		words.setFont(graphics.getFont());
		words.setPosition(new Point(left + 3 + ICON + 3, top + HEIGHT / 2 + metrics.getAscent() / 2));
		words.render(graphics);
	}

	private static String text(WaveView view)
	{
		return view.doomStacks + "/" + view.doomLimit;
	}

	/** The hitpoints orb the game shows now, or null with none on the screen. */
	private Widget healthOrb()
	{
		for (int id : ORBS)
		{
			Widget orb = client.getWidget(id);
			if (orb != null && !orb.isHidden())
			{
				return orb;
			}
		}
		return null;
	}
}
