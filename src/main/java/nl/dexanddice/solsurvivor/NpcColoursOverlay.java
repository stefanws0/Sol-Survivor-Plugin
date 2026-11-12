package nl.dexanddice.solsurvivor;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;
import nl.dexanddice.solsurvivor.core.NpcKind;
import nl.dexanddice.solsurvivor.core.WaveReview;

/**
 * "Frem and orb colours", a setting that starts off since 2026-11-11: each Fremennik of the warband outlined in the colour of its
 * attack style, the archer's, the seer's and the berserker's, and the tile under each of Solarflare's orbs outlined in
 * its own colour. Players set this up by hand today, pasting NPC ids and colours into Better NPC Highlight (a community
 * Colosseum FAQ hands out the codes, checked 2026-11-02; Stefan's choice of the same day to build it in). It colours
 * NPCs by what they are, as Better NPC Highlight does on the Hub, and says nothing of when they attack or where to stand.
 * The style of each Fremennik is the one the prayer record already reads (core.PrayerFlicks).
 */
class NpcColoursOverlay extends Overlay
{
	/** The outline round a Fremennik's model, in pixels, and how far it feathers. */
	private static final int OUTLINE_WIDTH = 2;
	private static final int OUTLINE_FEATHER = 2;
	/** How strongly an orb's tile is filled, out of 255, under its outline. */
	private static final int ORB_FILL_ALPHA = 40;

	private final Client client;
	private final SolSurvivorPlugin plugin;
	private final SolSurvivorConfig config;
	private final ModelOutlineRenderer outlines;

	@Inject
	NpcColoursOverlay(Client client, SolSurvivorPlugin plugin, SolSurvivorConfig config, ModelOutlineRenderer outlines)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.outlines = outlines;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/** The colour an NPC is drawn in, by its id: a Fremennik's style's or the orb's, null for any other NPC. */
	static Color colourFor(int npcId, SolSurvivorConfig config)
	{
		switch (npcId)
		{
			case WaveReview.FREM_ARCHER_ID:
				return config.fremArcherColour();
			case WaveReview.FREM_SEER_ID:
				return config.fremSeerColour();
			case WaveReview.FREM_BERSERKER_ID:
				return config.fremBerserkerColour();
			case NpcKind.SOLARFLARE_ORB_ID:
				return config.orbColour();
			default:
				return null;
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!config.npcColours() || !plugin.inArena())
		{
			return null;
		}
		for (NPC npc : client.getTopLevelWorldView().npcs())
		{
			Color colour = colourFor(npc.getId(), config);
			if (colour == null || colour.getAlpha() == 0 || npc.isDead())
			{
				continue;
			}
			if (npc.getId() == NpcKind.SOLARFLARE_ORB_ID)
			{
				// The orb is small and floats: its tile is what the player reads, so the tile is outlined, not the model.
				Polygon poly = npc.getCanvasTilePoly();
				if (poly != null)
				{
					Color fill = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(),
						Math.min(ORB_FILL_ALPHA, colour.getAlpha()));
					OverlayUtil.renderPolygon(graphics, poly, colour, fill, graphics.getStroke());
				}
			}
			else
			{
				outlines.drawOutline(npc, OUTLINE_WIDTH, colour, OUTLINE_FEATHER);
			}
		}
		return null;
	}
}
