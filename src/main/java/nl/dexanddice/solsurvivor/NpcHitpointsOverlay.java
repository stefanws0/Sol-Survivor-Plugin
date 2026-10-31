package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.TextComponent;
import nl.dexanddice.solsurvivor.core.NpcHitpoints;

/**
 * "NPC hitpoints", a setting that is on by default: a stone tag over each of the wave's NPCs' heads, its hitpoints in
 * the game's yellow against its full hitpoints in grey, as "172/250", over a thin bar of the share left (design B,
 * "Stone tag", of the NPC hitpoints designs of 2026-10-30, the user's choice). The number goes red at a quarter or
 * fewer and green at full health (the user's choice, 2026-10-30), a heal shows in pink for a few ticks, and "~" leads the number while only the game's health bar gives it
 * (core.NpcHitpoints). The tag wears the Doom box's ground, so the plugin's boxes on the game's screen look alike, and
 * sits above the game's own health bar and overhead icons rather than over them. Facts only, as the Doom box: nothing
 * of which NPC to attack.
 */
class NpcHitpointsOverlay extends Overlay
{
	/** How far above an NPC's head the tag's foot sits, in local units, clear of the game's health bar. */
	private static final int ABOVE_HEAD = 60;
	private static final int PAD_X = 7;
	/** The narrowest tag, so that a short number such as a shaman's still has a bar long enough to read. */
	private static final int MIN_WIDTH = 64;
	private static final int BAR = 2;
	private static final Color GROUND = new Color(0x1a, 0x16, 0x12, 225);
	private static final Color LOW = new Color(0xff3c3c);
	private static final Color HEAL = new Color(0xff6fcf);
	private static final Color BAR_LEFT = new Color(0x22c32e);
	private static final Color BAR_GONE = new Color(0x5a1610);
	private static final Color HATCH = new Color(255, 255, 255, 160);

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;

	@Inject
	NpcHitpointsOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.npcHitpoints() || !plugin.inArena())
		{
			return null;
		}
		graphics.setFont(FontManager.getRunescapeSmallFont());
		NpcHitpoints hitpoints = plugin.npcHitpoints();
		int tick = client.getTickCount();
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			NpcHitpoints.Reading reading = hitpoints.reading(npc.getIndex(), tick);
			if (reading == null || reading.hitpoints <= 0 || npc.isDead())
			{
				continue;
			}
			Point at = npc.getCanvasTextLocation(graphics, "", npc.getLogicalHeight() + ABOVE_HEAD);
			if (at != null)
			{
				paint(graphics, reading, at.getX(), at.getY());
			}
		}
		return null;
	}

	/** Draws one tag, centred across at `x`, its foot at `y`. */
	static void paint(Graphics2D graphics, NpcHitpoints.Reading reading, int x, int y)
	{
		FontMetrics metrics = graphics.getFontMetrics();
		String number = (reading.estimated ? "~" : "") + reading.hitpoints;
		String max = "/" + reading.max;
		String healed = reading.healed > 0 ? " +" + reading.healed : "";
		int words = metrics.stringWidth(number) + metrics.stringWidth(max) + metrics.stringWidth(healed);
		int width = Math.max(MIN_WIDTH, PAD_X + words + PAD_X);
		int textHeight = metrics.getAscent();
		int height = 2 + textHeight + 3 + BAR + 2;
		int left = x - width / 2;
		int top = y - height;

		graphics.setColor(GROUND);
		graphics.fillRoundRect(left, top, width, height, 6, 6);

		int baseline = top + 2 + textHeight;
		int cursor = left + (width - words) / 2;
		cursor = text(graphics, number, colour(reading), cursor, baseline);
		cursor = text(graphics, max, Stone.MUTED, cursor, baseline);
		if (!healed.isEmpty())
		{
			text(graphics, healed, HEAL, cursor, baseline);
		}

		int barLeft = left + PAD_X;
		int barWidth = width - 2 * PAD_X;
		int barTop = top + height - 2 - BAR;
		int now = share(barWidth, reading.hitpoints, reading.max);
		int before = share(barWidth, reading.hitpoints - reading.healed, reading.max);
		graphics.setColor(BAR_GONE);
		graphics.fillRect(barLeft, barTop, barWidth, BAR);
		graphics.setColor(BAR_LEFT);
		graphics.fillRect(barLeft, barTop, before, BAR);
		if (now > before)
		{
			graphics.setColor(HEAL);
			graphics.fillRect(barLeft + before, barTop, now - before, BAR);
		}
		if (reading.estimated)
		{
			// A few dotted pixels past the bar's end, as the range the game's bar leaves open.
			graphics.setColor(HATCH);
			int band = Math.max(2, barWidth / 12);
			for (int i = 0; i < band && now + i < barWidth; i += 2)
			{
				graphics.fillRect(barLeft + now + i, barTop, 1, BAR);
			}
		}
	}

	/**
	 * The number's colour: red at a quarter or fewer, green at full health counted exactly, and the game's yellow between,
	 * and for full health the bar alone gives, which may not be quite full.
	 */
	static Color colour(NpcHitpoints.Reading reading)
	{
		if (reading.low())
		{
			return LOW;
		}
		return reading.hitpoints == reading.max && !reading.estimated ? Stone.GOOD : Stone.YELLOW;
	}

	/** Draws `words` with the game's one-pixel shadow and returns where the next words start. */
	private static int text(Graphics2D graphics, String words, Color colour, int x, int baseline)
	{
		TextComponent component = new TextComponent();
		component.setText(words);
		component.setColor(colour);
		component.setFont(graphics.getFont());
		component.setPosition(new java.awt.Point(x, baseline));
		component.render(graphics);
		return x + graphics.getFontMetrics().stringWidth(words);
	}

	/** The pixels of a bar `width` wide that `part` of `whole` fills, at least one for anything above 0. */
	private static int share(int width, int part, int whole)
	{
		if (part <= 0)
		{
			return 0;
		}
		return Math.max(1, width * Math.min(part, whole) / whole);
	}
}
