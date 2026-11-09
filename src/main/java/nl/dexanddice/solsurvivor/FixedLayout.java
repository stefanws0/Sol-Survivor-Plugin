package nl.dexanddice.solsurvivor;

import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The boxes' first places in the fixed 765 x 503 client (idea 363, Stefan's pick of 2026-11-08). Its game view is 512 x
 * 334, and RuneLite stacks every box at one corner on top of each other: the wave box, both clocks and Doom's box at
 * the top left come to most of the view's height there, beside the infoboxes RuneLite's own plugins keep in that corner.
 * So in the fixed client the small boxes start at the top centre instead, and the wave box keeps the top left to itself;
 * the modifiers bar keeps the bottom left, which in the fixed client is the view's own corner, clear of the chatbox.
 * <p>
 * Only on a fresh install, once, on its first login, and only for a box the player has not placed: RuneLite keeps a
 * box's place under its name in its own `runelite` group, so a box with either key set is left where it is.
 */
final class FixedLayout
{
	/** RuneLite's config group for the boxes' places (checked in 1.13.6's OverlayManager). */
	static final String GROUP = "runelite";

	/** Where the small boxes start in the fixed client. */
	static final OverlayPosition SMALL_BOXES = OverlayPosition.TOP_CENTER;

	private FixedLayout()
	{
	}

	/** Whether the player placed the box named `name`: snapped it to a corner or dragged it somewhere. */
	static boolean placedByPlayer(String name, Overlaps.Settings settings)
	{
		return settings.get(GROUP, name + "_preferredPosition") != null || settings.get(GROUP, name + "_preferredLocation") != null;
	}
}
