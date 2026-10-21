# Sol Survivor

Plan your Fortis Colosseum waves on [Sol Survivor](https://sol.dexanddice.nl/). The plugin reads the arena and opens it
on the site in your browser, so you never have to set up a wave by hand.

**It draws nothing in the game.** No tiles, prayers, timers or plans appear in the client, and it never acts for you.
Everything happens on the site, in your browser.

## The panel

The card at the top is for the wave you are in:

- **Open wave start** opens the arena as it was on the wave's first tick.
- **Open arena now** opens the arena as it is right now.
- **Open in recommender** is available while Minimus offers his three modifiers. It opens the site's modifier pick
  with those offers, the modifiers you hold and the next wave already filled in.

Under it, **Past waves** lists every wave since RuneLite started, grouped by run, newest first. Click a run's head to
fold it open or shut. Each wave's row has three small buttons:

- The orange play button opens the wave on the site as it was on its first tick.
- The orange two-figure button opens it on the tick its reinforcements came.
- The grey copy button copies the wave's log: where you and every NPC stood on each tick, what each did, and the gear
  you wore when **Include gear and stats** is on. Paste it on the site to see where the planner's simulation differed
  from your run.

**Open past run** on a run's head uploads that run's recording and opens it directly in your browser, in the usual
arena. Choose a wave, play it back, or scrub to a particular tick. The grey copy button still copies the whole run for
the site's log checker.

The arena state Open buttons also copy their links, so you can share them. Open past run opens the browser directly,
without using the clipboard. A hotkey (unset by default) copies the current arena's link without leaving the game.

## What a link holds

A link holds the NPCs and their tiles, your tile, the wave and the tick within it, the modifiers you took, each
Manticore's orb order once it has shown it, and when each NPC last attacked. Mid-wave it also holds the fight so far:
each NPC's health bar as you last saw it, your Doom stacks this wave, and the molten sand on the floor. With **Include gear and stats** on, it
also holds your worn gear, the weapons in your inventory, your combat levels and your prayer points, which the site
uses to work out how quickly you kill each NPC.

## Privacy

Arena state links put their state after `#`, which browsers do not send to the site's server. Wave logs stay in memory
until RuneLite closes, unless you copy them or click **Open past run**.

**Open past run uploads only the selected run to the configured site's `/api/replays`.** It includes recorded positions,
hitpoints, prayers, animations, hits, NPC state and optional gear/stats, with no account name. Nothing uploads
automatically. The site stores the recording for seven days; anyone with its replay link can view it. The
upload happens in the background, then a short replay link opens directly in your browser. Remote uploads use HTTPS.

## Settings

- **Site address**: the site the links open.
- **Copy link hotkey**: copies the current arena's link.
- **Say so in chat**: a line in your chat box when a link is copied, or why it could not be.
- **Include gear and stats**: on by default.
- **Record wave log**: on by default; it keeps the last 25 waves in memory, up to 1,500 ticks each, until RuneLite
  closes.

## Credits

The modifiers' ids and varbits and the Manticore's orb graphics are the ones LlemonDuck's Fortis Colosseum plugin
reads.
