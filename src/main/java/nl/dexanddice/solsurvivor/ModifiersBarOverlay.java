package nl.dexanddice.solsurvivor;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import nl.dexanddice.solsurvivor.core.Modifier;

/**
 * "Modifiers above the chatbox", a setting that is on by default: the modifiers held in a row at the game screen's
 * bottom left, each its picture in a slot with its tier in the corner, as the panel's Modifiers held draws them (the
 * user's ask of 2026-10-26, so that Fortis Colosseum's own row can be switched off for this). RuneLite stacks a box at
 * the bottom left above the chatbox, and the player can move it with Alt. Its Size setting draws the slots, pictures
 * and gaps from 100% to 300% (the user's ask of 2026-10-30), the tiers in the game's larger font from 150%. Its Tiers
 * setting (the design review's 7, 2026-10-31) writes each tier on a dark plate in the slot's corner, where on the bare
 * picture a red tier was lost on a red picture such as Frailty's, or the modifier's name and tier under its slot, each
 * slot then as wide as its name. Facts only: what the game's own list holds.
 */
class ModifiersBarOverlay extends Overlay
{
	private static final int SLOT = 32;
	private static final int ICON = 26;
	private static final int GAP = 2;
	/** The gap between slots with their names under them, so that two names never run into one. */
	private static final int NAME_GAP = 8;
	private static final String[] TIERS = {"I", "II", "III"};
	/** The size from which the tiers are written in the game's larger font, which the small one looks lost beside. */
	private static final int LARGE_FONT_FROM = 150;

	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private final Client client;
	private final TooltipManager tooltipManager;
	/** Each slot's width as last drawn, gaps aside: the slot's, or with names under the slots its name's where wider. */
	private int[] widths = new int[0];
	/** The gap between the slots as last drawn. */
	private int drawnGap;

	@Inject
	ModifiersBarOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config, Client client, TooltipManager tooltipManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		this.tooltipManager = tooltipManager;
		setPosition(OverlayPosition.BOTTOM_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		Map<Modifier, Integer> held = plugin.waveView().held;
		if (!config.modifiersBar() || !plugin.inArena() || held.isEmpty())
		{
			return null;
		}
		int slot = scaled(SLOT);
		int icon = scaled(ICON);
		graphics.setFont(size() >= LARGE_FONT_FROM ? FontManager.getRunescapeFont() : FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		boolean under = config.modifiersBarTiers() == SolSurvivorConfig.ModifierTiers.NAME_UNDER;
		int gap = scaled(under ? NAME_GAP : GAP);
		int inset = scaled(2);
		int[] drawn = new int[held.size()];
		int at = 0;
		int x = 0;
		for (Map.Entry<Modifier, Integer> e : held.entrySet())
		{
			Modifier modifier = e.getKey();
			String name = modifier.getName(e.getValue());
			int width = under ? Math.max(slot, metrics.stringWidth(name)) : slot;
			drawn[at++] = width;
			graphics.translate(x + (width - slot) / 2, 0);
			Stone.paintWell(graphics, slot, slot);
			Icons.of(Icons.modifier(modifier.getKey()), icon).paintIcon(null, graphics, (slot - icon) / 2, (slot - icon) / 2);
			String tier = tierOf(modifier, e.getValue());
			if (!under && !tier.isEmpty())
			{
				// The tier on a dark plate in the corner, the slot's own outline round it, so that it reads on any picture.
				int pad = scaled(1);
				int plateWidth = metrics.stringWidth(tier) + 2 * pad + 2;
				int plateHeight = metrics.getAscent() + pad + 1;
				graphics.setColor(Stone.OUTLINE);
				graphics.fillRect(slot - plateWidth, slot - plateHeight, plateWidth, plateHeight);
				graphics.setColor(Stone.BEVEL_LIGHT);
				graphics.drawLine(slot - plateWidth, slot - plateHeight, slot - 1, slot - plateHeight);
				graphics.drawLine(slot - plateWidth, slot - plateHeight, slot - plateWidth, slot - 1);
				TextComponent words = new TextComponent();
				words.setText(tier);
				words.setColor(Stone.YELLOW);
				words.setFont(graphics.getFont());
				words.setPosition(new Point(slot - pad - 1 - metrics.stringWidth(tier), slot - inset));
				words.render(graphics);
			}
			graphics.translate(-(x + (width - slot) / 2), 0);
			if (under)
			{
				TextComponent words = new TextComponent();
				words.setText(name);
				words.setColor(Stone.YELLOW);
				words.setFont(graphics.getFont());
				words.setPosition(new Point(x + (width - metrics.stringWidth(name)) / 2, slot + inset + metrics.getAscent()));
				words.render(graphics);
			}
			x += width + gap;
		}
		widths = drawn;
		drawnGap = gap;
		int height = under ? slot + inset + metrics.getAscent() + metrics.getDescent() : slot;
		return new Dimension(x - gap, height);
	}

	/** The Size setting, kept to its range: a value typed past either end draws at that end. */
	private int size()
	{
		return Math.max(100, Math.min(300, config.modifiersBarSize()));
	}

	/** A length of the bar at 100%, at the size chosen. */
	private int scaled(int length)
	{
		return Math.round(length * size() / 100f);
	}

	/** RuneLite calls this after placing the overlay, so the hovered slot follows it when the player drags it. */
	@Override
	public void onMouseOver()
	{
		if (!config.modifiersBar() || !plugin.inArena())
		{
			return;
		}
		net.runelite.api.Point mouse = client.getMouseCanvasPosition();
		int x = mouse.getX() - getBounds().x;
		int y = mouse.getY() - getBounds().y;
		int index = slotAt(x, y);
		int at = 0;
		for (Map.Entry<Modifier, Integer> entry : plugin.waveView().held.entrySet())
		{
			if (at++ == index)
			{
				tooltipManager.add(new Tooltip(entry.getKey().getName(entry.getValue())));
				return;
			}
		}
	}

	/** Which slot, as last drawn, is under a point `x`, `y` from the bar's top left: -1 for a gap or none. */
	private int slotAt(int x, int y)
	{
		if (x < 0 || y < 0 || y >= getBounds().height)
		{
			return -1;
		}
		int left = 0;
		for (int i = 0; i < widths.length; i++)
		{
			if (x < left + widths[i])
			{
				return i;
			}
			left += widths[i] + drawnGap;
			if (x < left)
			{
				return -1;
			}
		}
		return -1;
	}

	/** The tier held in roman numerals, "II"; nothing for a modifier without tiers. */
	static String tierOf(Modifier modifier, int tier)
	{
		return modifier.hasTiers() && tier >= 1 && tier <= 3 ? TIERS[tier - 1] : "";
	}
}
