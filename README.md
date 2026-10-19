# Sol Survivor

Plan your Fortis Colosseum waves on [Sol Survivor](https://sol.dexanddice.nl/). The plugin reads the arena and opens it
on the site in your browser, so you never have to set up a wave by hand.

**It draws nothing in the game.** No tiles, prayers, timers or plans appear in the client, and it never acts for you.
Everything happens on the site, in your browser.

## The panel

- **Open wave start** opens the arena as it was on the wave's first tick.
- **Open arena now** opens the arena as it is right now.
- **Open in recommender** is available while Minimus offers his three modifiers. It opens the site's modifier pick
  with those offers, the modifiers you hold and the next wave already filled in.
- **Copy wave log** copies a record of a wave: where you and every NPC stood on each tick, what each did, and the gear
  you wore when **Include gear and stats** is on. Pick any wave since RuneLite started from the list above the button.
  Paste it on the site to see where the planner's simulation differed from your run.
- **Copy whole run** copies every wave of your latest run in one paste, from wave 1, or from the first wave the plugin
  saw if you turned it on mid-run, to the latest, so the site can go through them one wave after another.

Each Open button also copies its link, so you can share it. A hotkey (unset by default) copies the current arena's link
without leaving the game.

## What a link holds

A link holds the NPCs and their tiles, your tile, the wave and the tick within it, the modifiers you took, each
Manticore's orb order once it has shown it, and when each NPC last attacked. With **Include gear and stats** on, it
also holds your worn gear, the weapons in your inventory, your combat levels and your prayer points, which the site
uses to work out how quickly you kill each NPC.

## Privacy

The plugin sends nothing over the network. A link opens in your browser or goes to your clipboard. The arena rides in
the part of the link after `#`, which browsers do not send to the site's server. The wave logs stay in memory and only
go to your clipboard; nothing is written to disk.

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
