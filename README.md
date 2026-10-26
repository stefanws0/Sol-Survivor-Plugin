**Plan your Fortis Colosseum waves tick by tick.** Press a button in RuneLite and the wave you are in opens on
[Sol Survivor](https://solsurvivor.app/), a planner in your browser that works out where to stand, what to attack and
which prayer to have up on every tick, down to the last kill.

The plugin draws nothing in the game and never plays for you. It reads the arena, turns it into a link, and opens that
link in your browser.

![Sol Survivor's planner with a wave 7 opened from the plugin: the arena, the plan's steps tick by tick, and the prayer to have up on each tick](images/planner.webp)

## How it works

1. In the Colosseum, press **Arena now** in the Sol Survivor panel at any point in a wave, or the play button on the
   wave's line under **Past waves** to open it as it was on its first tick.
2. The planner opens in your browser with the wave as it is in game: the NPCs on their tiles, you on yours, and the
   modifiers you took. Each link the plugin opens closes the tab it opened before, so one tab keeps up with you. With **Include gear and stats** on, it also knows how quickly your gear kills each NPC.
3. Press **Solve wave**. Your own computer does the searching, so it gets busy for a while
   ([more on that below](#your-computer-does-the-solving)); the best plans found so far show as it goes.
4. Pick a plan on the Safe · Fast · Easy triangle and play it through tick by tick: where to run, what to click, and
   when to switch prayers.

The site is made for a desktop or laptop browser, beside the game, not for a phone.

## Your computer does the solving

No server works out your plans: the planner runs in your browser, on your own computer, and solving a wave is heavy
work. Expect your computer to notice.

- **Nothing runs until you press Solve.** Opening a link only shows the wave.
- **A solve uses all but one of your processor's threads**, 15 at most, for anything from a few seconds to a few
  minutes: the busier the wave, the longer. On a computer with four threads, a wave 7 kept the processor 85%
  busy for two minutes.
- **Your fans may spin up.** A laptop can get warm and drain its battery faster, and on a computer with many threads
  the tab can take up to about 2 GB of memory. It all stops when the solve ends.
- **If the game stutters**, solve between waves, or press **Stop**: the plans found so far stay.
- **The modifier pick solves too.** It plays the next wave under each of Minimus's offers, which keeps your computer
  busy for a little while.
- **In RuneLite, the plugin is light.** It notes where everything stands each tick and makes a link when you press a
  button.

## The panel

<img src="images/panel.png" width="242" align="right" alt="The Sol Survivor panel in RuneLite during wave 5: the wave's row with Arena now, the Modifier pick, and past waves grouped by run">

It opens by itself as you enter the Colosseum, and from the sun in RuneLite's sidebar at any time. It wears the Sol
Survivor site's stone look. The row at the top is for the wave you are in, with its number. Its **Arena now** opens the
wave as it is right now, with the fight so far, and a line under it says what the last button did. To open the wave as
it was on its first tick, press the play button on its line under Past waves.

The **Modifier pick** row lights up while Minimus offers his three modifiers: its **Open** button opens the site's
modifier pick with his offers, the modifiers you hold and the next wave filled in.

Each of these also copies its link, so you can share the wave. A hotkey, unset by default, copies the arena's link
without leaving the game.

**Past waves** lists your last 25 waves, grouped by run, newest first, and keeps them when RuneLite closes. Each
wave's line gives its time and the glory it earned, by the game's own counter, and each run adds up its glory under
its head. Click a run's head to fold it open or shut. Each wave's line has three buttons:

- The play button opens the wave as it was on its first tick.
- The two-figure button opens it on the tick its reinforcements came.
- The copy button copies the wave's log. Paste it on the site to see where the planner's simulation differed from
  your run.

**Watch**, on a run's head, opens the whole run on the site to watch back. The copy button beside it copies the whole
run for the site's log checker. The question mark at the top of the panel sums all this up.

With **Leaderboard** on, a **Leaderboard** row shows the name your waves go up under, with **Open**, which opens
the site's leaderboard outside a wave, and **Remove my times**.

<br clear="right">

## Between waves: which modifier to take

When Minimus makes his offers, press **Open** on the panel's Modifier pick. The site fills in his three offers and the
modifiers you hold, solves the next wave on a few spawn layouts under each offer, and recommends the one that costs you least. Each
offer also shows the OSRS Wiki's verdict on it, which decides between offers that cost about the same.

![Sol Survivor's modifier pick with Minimus's three offers for wave 8, filled in from the plugin and priced by what each adds to the next wave](images/modifier-pick.webp)

## After a run: watch it back

**Watch**, on a run in the panel, uploads that run's recording and opens it on the site: every wave on the arena as it happened, to
play, step through or scrub to any tick. The review picks out the moments worth a look, such as damage you took and the
prayer you had up when it landed.

![A recorded wave 6 on Sol Survivor's run review, at the moment four hits got through and the prayer that was up when they landed](images/run-review.webp)

## The leaderboard

Switch **Leaderboard** on in the settings, with **Share wave logs**, and each wave you finish goes on the
[Sol Survivor leaderboard](https://solsurvivor.app/leaderboard) under your RuneScape name, or the **Leaderboard name**
you type instead: the full run, each wave, and the sum of
your best waves, timed by the game's own wave clock, with the modifiers you held. As a wave ends, the panel and the
chat say its time and place. Anyone can watch a run on the boards, wave by wave, in the site's run review. A time the
game's clock does not back is not counted, and one that looks too good to be true is held for a look before it shows.
**Remove my times** takes your name and every time of yours off, and switches Leaderboard off.

## Privacy

- **A wave link keeps the arena after its `#`**, the part of a link that browsers do not send to the site's server.
- **Watch uploads only once you turn on Upload past runs, and only when you press it.** It sends the selected
  run's recording to the Sol Survivor site's `/api/replays` over HTTPS: tiles, hitpoints, prayers, animations, hits, projectiles and NPC state, plus your gear and stats when
  **Include gear and stats** is on. It holds no account name. The site keeps the recording for seven days, and anyone
  with its link can view it.
- **Share wave logs is off until you turn it on.** Then each wave's log goes to the site's `/api/wave-logs` as the
  wave ends, so that the planner can be checked against real games: the same tiles, hitpoints, prayer points,
  animations, hits, projectiles and NPC state, the game's own counters for the wave and the run, such as your Doom
  stacks and glory, and the wave and its modifiers. From 0.8.3 the site also trains the planner's Learned ranking on
  where each wave starts, the NPCs' tiles, yours and the modifiers, and on nothing else of the log. It holds no
  account name, world or time of day, and no combat levels or prayer unlocks: your gear only with **Include gear and
  stats** on, and then without them. A run's waves share a random id that the plugin forgets when you leave the
  arena, so that nothing joins one of your runs to another. The site keeps the day a log came, not your address, shows
  the logs to nobody but on the leaderboard, and deletes each after 90 days.
- **The leaderboard is off until you switch it on**, and RuneLite warns as you do that your name goes up in public.
  Then, as the site takes each wave's log, the plugin asks for the wave to go on the leaderboard (`/api/board/waves`)
  under your RuneScape name, or the Leaderboard name you typed, with the run's random id and a random key of yours that
  the plugin makes once and keeps in RuneLite's settings, so that the name and the times stay yours. Anyone can see the names, times and modifiers on the boards and
  watch their runs from the shared logs; the logs of a run in the first 20 places of a board are kept past their 90
  days while it stays there. **Remove my times** (`/api/board/remove`) deletes your name and every time of yours.
- **Both are off until you turn them on**, and RuneLite warns as you do that each sends your IP address to a server it
  does not run. With both off, the plugin sends nothing, Leaderboard on or not.
- **Wave logs stay on your computer**, unless you copy one, watch a run or share them. With **Keep runs between
  sessions** on, they are saved in RuneLite's folder for this plugin, each account's apart, and only the last 25 are
  kept. With
  **Count my flicks** on, RuneLite's settings keep how many attacks you prayed against on time or late in your
  newest 60 waves, and the wave links carry those totals after their `#` too.

**What a wave link holds:** the NPCs and their tiles, your tile, the wave and the tick within it, the modifiers you
took, each Manticore's orb order once it has shown it, and when each NPC last attacked. Mid-wave it also holds the fight
so far: each NPC's health bar as you last saw it, your Doom stacks this wave, and the molten sand on the floor. With
**Include gear and stats** on, it also holds your worn gear, the weapons in your inventory, your combat levels, your
prayer points and which of the stronger prayers you have unlocked, from which the site works out how quickly you kill
each NPC.

## Settings

- **Copy link hotkey**: copies the arena's link as it is now.
- **Include gear and stats**: on by default.
- **Say so in chat**: a line in your chat box when a link is copied, or why it could not be.
- **Open panel on entering**: on by default. Opens the panel as you enter the Colosseum, and RuneLite's sidebar with
  it if it is shut. Open another panel during the run and it stays open until you next come in.
- **Show only at the Colosseum**: off by default. Shows the plugin's sun in RuneLite's sidebar only in the Colosseum
  and its lobby, so Past waves is at hand only there.
- **Record wave log**: on by default. It keeps the last 25 waves for Past waves, up to 1,500 ticks each.
- **Keep runs between sessions**: on by default. Saves those waves on your computer, each account's apart, so that
  Past waves still lists them after RuneLite closes. Switch it off to delete the saved waves; Past waves then lasts
  until RuneLite closes.
- **Sending to the site**: the settings that send something, together.
  - **Upload past runs**: off by default. Lets a run's Watch button upload that run to the site (see Privacy); until
    it is on, pressing Watch says so.
  - **Share wave logs**: off by default. Sends each wave's log to the site as the wave ends (see Privacy). It
    needs **Record wave log**, and the panel says so under Past waves while it is on.
  - **Leaderboard**: off by default. Puts each wave you finish on the site's leaderboard (see The leaderboard), under
    your RuneScape name or the name below. It needs **Share wave logs**.
  - **Leaderboard name**: blank by default, for your RuneScape name. Another name for the leaderboard: 1 to 12 letters,
    digits, spaces, hyphens or underscores.
- **Safe / Fast / Easy**: with **Set the triangle** on, every wave you open from the plugin starts with the site's
  Safe / Fast / Easy triangle where you set it, and so with the plan it suggests, instead of where you last left it.
  Copy the three numbers from the site's readout under the triangle, such as Safe 10% · Fast 0% · Easy 90%; they need
  not add up to 100. Off by default.
- **Prayer reliability**: with **Count my flicks** on, the plugin counts your protection prayers in each wave, on time
  or late, and every wave you open from it starts with the site's Prayer reliability on Mine: as reliable as your
  newest 60 waves show you to be, kept on the safe side, so that a few lucky waves never make the plans harder. It
  needs **Record wave log**. Switch it off to forget the waves counted. Off by default.

## Credits

Sol Survivor is a fan-made tool, not affiliated with Jagex. The game's mechanics come from the
[OSRS Wiki](https://oldschool.runescape.wiki/), and the planner's engine is checked tick for tick against Supalosa's
[LoS tool](https://los.colosim.com/). The modifiers' ids and varbits and the Manticore's orb graphics are the ones
LlemonDuck's Fortis Colosseum plugin reads.
