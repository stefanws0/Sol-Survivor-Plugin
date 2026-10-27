package nl.dexanddice.solsurvivor;

import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import nl.dexanddice.solsurvivor.core.Modifier;

/**
 * "Modifiers above the chatbox", a setting that is off by default: the modifiers held in a row at the game screen's
 * bottom left, each its picture in a slot with its tier in the corner, as the panel's Modifiers held draws them (the
 * user's ask of 2026-10-26, so that Fortis Colosseum's own row can be switched off for this). RuneLite stacks a box at
 * the bottom left above the chatbox, and the player can move it with Alt. Facts only: what the game's own list holds.
 */
class ModifiersBarOverlay extends Overlay
{
	private static final int SLOT = 32;
	private static final int ICON = 26;
	private static final int GAP = 2;
	private static final String[] TIERS = {"I", "II", "III"};

	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;

	@Inject
	ModifiersBarOverlay(SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.plugin = plugin;
		this.config = config;
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
		graphics.setFont(FontManager.getRunescapeSmallFont());
		FontMetrics metrics = graphics.getFontMetrics();
		int x = 0;
		for (Map.Entry<Modifier, Integer> e : held.entrySet())
		{
			Modifier modifier = e.getKey();
			graphics.translate(x, 0);
			Stone.paintWell(graphics, SLOT, SLOT);
			Icons.of(Icons.modifier(modifier.getKey()), ICON).paintIcon(null, graphics, (SLOT - ICON) / 2, (SLOT - ICON) / 2);
			String tier = tierOf(modifier, e.getValue());
			if (!tier.isEmpty())
			{
				TextComponent words = new TextComponent();
				words.setText(tier);
				words.setColor(Stone.YELLOW);
				words.setFont(graphics.getFont());
				words.setPosition(new Point(SLOT - 3 - metrics.stringWidth(tier), SLOT - 3));
				words.render(graphics);
			}
			graphics.translate(-x, 0);
			x += SLOT + GAP;
		}
		return new Dimension(x - GAP, SLOT);
	}

	/** The tier held in roman numerals, "II"; nothing for a modifier without tiers. */
	static String tierOf(Modifier modifier, int tier)
	{
		return modifier.hasTiers() && tier >= 1 && tier <= 3 ? TIERS[tier - 1] : "";
	}
}
