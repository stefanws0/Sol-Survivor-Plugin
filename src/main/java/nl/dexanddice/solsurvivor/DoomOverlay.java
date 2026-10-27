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
 * Doom's stacks by the hitpoints orb, a setting that is off by default: Doom's icon and "4/10", the game's own count
 * against the tier's limit, red on the last stack, beside the orb where the player already reads their hitpoints (the
 * third of the five designs of 2026-10-26, the user's choice). It sits just left of the orb, on its row, as the prayer
 * orb sits under it. Facts only, as the buffs bar's own Doom count: nothing of how to play.
 */
class DoomOverlay extends Overlay
{
	/** The hitpoints orb, in each of the game's layouts of the orbs: by the minimap, without it, and their other two. */
	private static final int[] ORBS = {InterfaceID.Orbs.ORB_HEALTH, InterfaceID.OrbsNomap.ORB_HEALTH, InterfaceID.OrbsOsm.ORB_HEALTH,
		InterfaceID.OrbsOsmNomap.ORB_HEALTH};
	private static final int ICON = 16;
	private static final int HEIGHT = 20;
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
		if (!config.doomByOrb() || !plugin.inArena() || !view.underWay || view.doomLimit == 0)
		{
			return null;
		}
		Widget orb = healthOrb();
		if (orb == null)
		{
			return null;
		}
		Rectangle bounds = orb.getBounds();
		String text = view.doomStacks + "/" + view.doomLimit;
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int width = 3 + ICON + 3 + metrics.stringWidth(text) + 4;
		int left = bounds.x - 4 - width;
		int top = bounds.y + bounds.height / 2 - HEIGHT / 2;
		graphics.setColor(GROUND);
		graphics.fillRoundRect(left, top, width, HEIGHT, 6, 6);
		icon.paintIcon(null, graphics, left + 3, top + (HEIGHT - ICON) / 2);
		TextComponent words = new TextComponent();
		words.setText(text);
		words.setColor(view.doomLast() ? LAST : TEXT);
		words.setFont(graphics.getFont());
		words.setPosition(new Point(left + 3 + ICON + 3, top + HEIGHT / 2 + metrics.getAscent() / 2));
		words.render(graphics);
		return null;
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
