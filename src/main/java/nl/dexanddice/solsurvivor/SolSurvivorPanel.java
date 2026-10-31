package nl.dexanddice.solsurvivor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;
import nl.dexanddice.solsurvivor.core.ColosseumTracker;

/**
 * The side panel, in the Sol Survivor site's stone look (design 1, "Stone rows", of the plugin panel designs of
 * 2026-10-25, which the user picked): the site's Setup tab in the sidebar. A raised row for the wave under way, its
 * number in a slot, with the button that opens the arena as it is now on the site, and under it a well with what the
 * last button did. The wave's start is not on that row: the wave's line under Past waves opens it, from the tick the
 * wave starts (the user's choice, 2026-10-25). A row for Minimus's pick, which lights up while he offers his three. Under Past
 * waves, each run a row that folds open and shut like the loot tracker's, onto its waves in a well: a line per wave
 * with small buttons that open it on the site at its start or on the tick its reinforcements came, and copy its log,
 * and on the run's head a button that copies the whole run and one that uploads it to watch it back. With Leaderboard
 * on, a row for the site's leaderboard: the name, its page outside a wave, and Remove my times. The help is in
 * the tooltip of the question mark at the top.
 *
 * 1.0 draws the panel as the plugin feature designs of 2026-10-26 do (WaveCards): the wave's list and the modifiers held
 * in the wave's row, by their pictures; the wave report once a wave ends, short under the wave's row and in full on a
 * page of its own, which takes the panel's place until Back; the death report after a death; and on each past wave its
 * mistakes. Each row shows only in the moment it is about (the user's ask of 2026-10-26, "Plugin 1.0: what the panel
 * shows when"): the pick's only in the arena, its head alone until Minimus offers; the leaderboard's only out of the
 * arena; and in the arena Past waves fold into their heading until the player opens them (the user's ask of
 * 2026-10-27). After a death the recap is a page of its own, the death report, which opens as the player comes out of
 * the arena, with Home to the rows, where a line opens it again; and out of the arena the last wave's report is a line
 * too (the user's ask of 2026-10-28, that a player who has just died is not met by everything at once). Like the rest of
 * the plugin the panel shows nothing about how to play the wave; the plans are the site's.
 */
class SolSurvivorPanel extends PluginPanel
{
	private boolean replayBusy;

	/** Runs can be rebuilt on a game tick; their Watch buttons keep the upload state. Called on the Swing thread. */
	void showReplayBusy(boolean busy)
	{
		replayBusy = busy;
		buildRuns();
	}

	/** What the panel's buttons do, which the plugin carries out on the client thread; the cards' too. */
	interface Actions extends WaveCards.Actions
	{
		void openNow();

		void openRecommender();

		/** Open the logged wave `id` on the site at its first tick. */
		void openPastStart(int id);

		/** Open the logged wave `id` on the site on the tick its reinforcements came. */
		void openPastReinforcements(int id);

		void copyWaveLog(int id);

		/** Copy the whole run that holds the logged wave `id`. */
		void copyRun(int id);

		/** Upload the selected run and open its recording directly in the browser. */
		void openPastRun(int id);

		/** Open the site's leaderboard in the browser. */
		void openLeaderboard();

		/** Take every time of the player's off the leaderboard, once they confirmed it. */
		void removeTimes();

		/** Open the sample wave on the site, with its plan, from the Getting started card. */
		void openSample();

		/** Hide the Getting started card for good. */
		void hideGettingStarted();

		/** Switch one of In the game's features on or off, by its key in the settings (IN_GAME). */
		void setInGame(String key, boolean on);
	}

	/** A logged wave, as its line shows it. */
	static final class WaveRow
	{
		private final int id;
		private final int wave;
		private final String detail;
		private final boolean underWay;
		private final boolean start;
		private final boolean reinforcements;
		private final Integer mistakes;

		/**
		 * `detail`: how the wave went, or that it is under way, which `underWay` marks in gold. `start` and
		 * `reinforcements`: whether the log kept the wave's state on its first tick and when they came. `mistakes`: the
		 * mistakes its report counts, or null when it has none.
		 */
		WaveRow(int id, int wave, String detail, boolean underWay, boolean start, boolean reinforcements, Integer mistakes)
		{
			this.id = id;
			this.wave = wave;
			this.detail = detail;
			this.underWay = underWay;
			this.start = start;
			this.reinforcements = reinforcements;
			this.mistakes = mistakes;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof WaveRow))
			{
				return false;
			}
			WaveRow r = (WaveRow) o;
			return r.id == id && r.wave == wave && r.detail.equals(detail) && r.underWay == underWay && r.start == start
				&& r.reinforcements == reinforcements && Objects.equals(r.mistakes, mistakes);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id, wave, detail, underWay, start, reinforcements, mistakes);
		}
	}

	/**
	 * A run's row: its key, the id of its first wave's log, which stays as the run grows; its name, such as "Latest run";
	 * the line under it, such as "Waves 1–5 · 5:11"; the glory its waves completed earned, such as "5,526 glory", on a
	 * line of its own so that the run's line keeps clear of its buttons, or null when none is known; and its waves,
	 * newest first.
	 */
	static final class RunGroup
	{
		private final int key;
		private final String name;
		private final String detail;
		private final String glory;
		private final List<WaveRow> waves;

		RunGroup(int key, String name, String detail, String glory, List<WaveRow> waves)
		{
			this.key = key;
			this.name = name;
			this.detail = detail;
			this.glory = glory;
			this.waves = waves;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof RunGroup))
			{
				return false;
			}
			RunGroup r = (RunGroup) o;
			return r.key == key && r.name.equals(name) && r.detail.equals(detail) && Objects.equals(r.glory, glory)
				&& r.waves.equals(waves);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(key, name, detail, glory, waves);
		}
	}

	/** A modifier Minimus offers: its key in the state link, for its picture, and its name with the tier it would be. */
	static final class Offer
	{
		private final String key;
		private final String name;

		Offer(String key, String name)
		{
			this.key = key;
			this.name = name;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof Offer && ((Offer) o).key.equals(key) && ((Offer) o).name.equals(name);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(key, name);
		}
	}

	/**
	 * The help, in the tooltip of the question mark beside the panel's title: a short line for each part of the panel, its
	 * name lit, and what goes to the site left to What you share (the UX review of 2026-10-27, F5).
	 */
	static final String HELP = "<font color='#ff981f'>Arena now</font> opens the wave you are in on the Sol Survivor site, as it is "
		+ "now, and copies its link.<br><br>"
		+ "<font color='#ff981f'>Open the pick</font> opens Minimus's three offers there while he makes them.<br><br>"
		+ "<font color='#ff981f'>Past waves</font>: Play opens the start; More actions or right-click opens reinforcements and Copy wave log. "
		+ "Click the heading in the arena.<br><br>"
		+ "The <font color='#ff981f'>wave report</font> comes as each wave ends, and the <font color='#ff981f'>death report</font> "
		+ "as you leave after a death.<br><br>"
		+ "<font color='#ff981f'>In the game</font> switches what the plugin draws. <font color='#ff981f'>What you share</font>, "
		+ "at the foot, says what goes to the site: all of it is off until you switch it on.";

	private static final ImageIcon SUN = Stone.sun(24);
	/** Yellow, as the text of the primary button it sits on. */
	private static final ImageIcon ARENA = Stone.arena(12, Stone.YELLOW);
	/**
	 * Minimus's face in the pick's slot, as the site shows him (design round two's item 7): in colour while he offers his
	 * three, faded until he does.
	 */
	private static final Icon MINIMUS_ON = Icons.of("minimus", 28);
	private static final Icon MINIMUS_OFF = Icons.of("minimus", 28, 0.35f);
	/** The gold medal in the leaderboard's slot, as the site's leaderboard gives first place (render 11). */
	private static final Icon MEDAL = Icons.of("medal-gold", 28);
	/** Yellow, as the text of the primary button it sits on, as Arena now's icon. */
	private static final ImageIcon PICK_ICON = Stone.cards(12, Stone.YELLOW);
	private static final ImageIcon START_ICON = Stone.play(10, Stone.ORANGE);
	private static final ImageIcon MORE_ICON = Stone.more(12, Stone.ORANGE);
	private static final ImageIcon COPY_RUN_ICON = Stone.copy(12, Stone.MUTED);
	private static final ImageIcon WATCH_ICON = Stone.replay(12, Stone.ORANGE);
	/** Watch's icon while Upload past runs is off, as the report's and the recap's Watch go secondary then. */
	private static final ImageIcon WATCH_OFF_ICON = Stone.replay(12, Stone.MUTED);
	private static final ImageIcon OPEN_ICON = Stone.caret(9, Stone.MUTED, true);
	/** Yellow, as the text of the primary button it sits on, as Arena now's icon. */
	private static final ImageIcon SAMPLE_ICON = Stone.play(10, Stone.YELLOW);
	private static final ImageIcon SHUT_ICON = Stone.caret(9, Stone.MUTED, false);
	/** The outlines of a past wave's tag: the designs' dark green for a clean wave, their dark red for one with mistakes. */
	private static final Color CLEAN_OUTLINE = new Color(0x1f5a1f);
	private static final Color MISTAKES_OUTLINE = new Color(0x5e250c);

	private final Actions actions;
	/** The wave's list under the wave's row, the wave report, the death report, and their pages. */
	final WaveCards cards;
	private final Stone.Slot waveSlot = new Stone.Slot(34, SUN);
	private final JLabel waveTitle = new Stone.Shadowed("", bold(), Stone.ORANGE);
	private final JLabel waveState = label("", small(), Stone.TEXT);
	private final Stone.Button openNow = new Stone.Button("Arena now", ARENA, true, small(), 7, 6);
	private final Stone.Well resultWell = new Stone.Well(new BorderLayout(), 4, 5, 4, 5);
	private final JLabel result = label("", small(), Stone.TEXT);
	private final Stone.Row pickRow = new Stone.Row(new BorderLayout(0, 6), 5, 5, 6, 6);
	private final Stone.Slot pickSlot = new Stone.Slot(34, MINIMUS_ON);
	private final JLabel pickState = label("", small(), Stone.MUTED);
	/** Minimus's three by their pictures, and the button that opens them on the site, Arena now's twin, under the row's head. */
	private final Stone.Well offersWell = new Stone.Well(new java.awt.GridLayout(1, 3, 4, 0), 4, 3, 4, 3);
	private final Stone.Button openPick = new Stone.Button("Open the pick", PICK_ICON, true, small(), 7, 6);
	/** The leaderboard's row, while Leaderboard is on or the player has times to remove (showBoard). */
	private final Stone.Row boardRow = new Stone.Row(new BorderLayout(0, 5), 5, 6, 6, 6);
	private final JLabel boardState = label("", small(), Stone.TEXT);
	private final Stone.Button openBoard = new Stone.Button("Open", null, false, small(), 7, 6);
	private final Stone.Button removeTimes = new Stone.Button("Remove my times", null, false, small(), 7, 6);
	/** Beside Past waves, while "Share wave logs" is on: each wave's log goes to the site as it ends. */
	private final JLabel sharing = label("Sharing logs", small(), Stone.GOOD);
	/**
	 * Past waves' heading, which in the arena is the whole section until the player opens it (the user's ask of
	 * 2026-10-27): its caret, and how many waves it holds while it is shut. Out of the arena it is a plain heading.
	 */
	private final JPanel pastHeading = clear(new BorderLayout(5, 0));
	private final JLabel pastCaret = new JLabel(SHUT_ICON);
	private final JPanel pastName = clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final JLabel pastCount = label("", small(), Stone.MUTED);
	private final JPanel runsBox = new JPanel();
	/**
	 * The Getting started card (the onboarding ideas of 2026-10-27, O1), at the top for a player new to the plugin: the
	 * three steps from the game to a plan, and a sample wave to try with no Colosseum needed. It goes for good on Hide or
	 * once the player has opened or copied an arena's link (the plugin's gettingStartedDone).
	 */
	private final Stone.Row startRow = new Stone.Row(new BorderLayout(0, 6), 5, 6, 6, 6);
	/** The first step, which says whether the panel opens by itself as the player enters (Open panel on entering). */
	private final JLabel enterStep = label("", small(), Stone.TEXT);
	/** The first step's number, ticked once the player is in the arena. */
	private final JLabel enterNumber = number(Stone.number(14, 1, small()));
	/** The second step, which points at Arena now below once the player is in the arena. */
	private final JLabel arenaStep = label("", small(), Stone.TEXT);
	/** The sample wave's button and its line, for a player not yet in the arena. */
	private final JPanel startFoot = clear(new BorderLayout(0, 3));
	/** Whether Getting started shows in the arena, where its first step is done and the sample goes (F4). */
	private boolean startInArena;
	/**
	 * What the plugin sends to the site (O2), in place of the foot's line: a row that folds open onto the three settings
	 * that send anything, each On or Off as set, what it sends and what it gets the player. It only shows them: each is
	 * switched in the settings, where RuneLite shows its warning first. Its head is bare, as Past waves' is, with what it
	 * holds in a row under it, so that the two folding sections' carets, names and counts line up.
	 */
	private final Stone.Group sendsRow = new Stone.Group(new BorderLayout(0, 4));
	/** Which of the three are on: Upload past runs, Share wave logs and Leaderboard, as the settings hold them. */
	private boolean[] shownSends;
	/**
	 * Whether the player folded What you share open or shut; null, it is open while the Getting started card shows out of
	 * the arena.
	 */
	private Boolean sendsOpen;
	/**
	 * What the plugin draws in the game (Stefan's ask of 2026-10-30), above What you share and folding as it does: a line
	 * for each feature with a switch that sets it at once, so that a player who never opens the settings still finds what
	 * the plugin offers, and one who has another plugin for a feature switches ours off in a click. The settings keep the
	 * rest of each feature's own settings.
	 */
	private final Stone.Group gameRow = new Stone.Group(new BorderLayout(0, 4));
	/** Which of IN_GAME's features are on, in its order, as the settings hold them. */
	private boolean[] shownGame;
	/** Whether the player folded In the game open or shut; null, it is open while the Getting started card shows. */
	private Boolean gameOpen;
	/** Which runs the player folded open or shut, by key; a run not here is open when it is the newest. */
	private final Map<Integer, Boolean> folded = new HashMap<>();
	/** The last past-wave action's location and answer, kept across rebuilding the rows on a tick or upload change. */
	private String pastResultKey;
	private String pastResultMessage = "";
	private List<RunGroup> shownRuns;
	private String shownNote;
	private Integer shownWave;
	private List<Offer> shownOffers;
	/** Whether the player is in the arena, where Past waves fold into their heading. */
	private boolean shownInColosseum;
	/** Whether the player opened Past waves in the arena; it shuts again as they leave it, so that each entry starts shut. */
	private boolean pastOpen;
	/** The panel's rows, top to bottom; each shows only in its moment, and has no size while it hides. */
	private final List<JComponent> rows = new ArrayList<>();

	/**
	 * The panel's three tabs under its name (the design review's 10, Stefan's choice of 2026-10-31), so that the wave
	 * under way is never below the past and the switches: the wave, its pick, its report and the leaderboard; the past
	 * waves and what the plugin sends; and what it draws on the game's screen, with its presets.
	 */
	enum Tab
	{
		WAVE("Wave"), PAST("Past"), SCREEN("Screen");

		final String label;

		Tab(String label)
		{
			this.label = label;
		}
	}

	/** The tab shown; Wave again as a wave starts or Minimus offers, which the player is there for. */
	private Tab tab = Tab.WAVE;
	private final JPanel tabStrip = clear(new java.awt.GridLayout(1, Tab.values().length, 2, 0));
	private final Map<Tab, JPanel> tabBodies = new HashMap<>();
	/** Whether a wave was under way, and Minimus offering, at the last state shown: Wave opens as either begins. */
	private boolean shownLive;
	private boolean shownOffering;
	/** The presets' card at the top of the Screen tab, built again as the switches change. */
	private final Stone.Group presetsRow = new Stone.Group(new BorderLayout(0, 4));

	/**
	 * Screen's presets, each its name, what it holds, and the drawing features it switches on, every other drawing
	 * feature off: the wave box alone; what a planner's player wants beside the plan; and all of them. Hide the pillars
	 * and Left-click Bank-all change the game rather than draw on it, so no preset touches them.
	 */
	static final String[][] PRESETS = {
		{"Minimal", "The wave box alone.", "waveBox"},
		{"Planner", "The wave box, the modifiers, Doom, the Volatility reminder, the offers' names and the wave timer.",
			"waveBox", "modifiersBar", "doomByOrb", "volatilityReminder", "offerNames", "waveTimer"},
		{"All", "Everything the plugin draws on the game's screen.",
			"waveBox", "modifiersBar", "doomByOrb", "volatilityReminder", "offerNames", "npcHitpoints", "highlightInRange", "myRange",
			"tickCounter", "waveTimer", "lootTotals"},
	};

	/** The In the game keys no preset touches: they change the game rather than draw on it. */
	private static final java.util.Set<String> NOT_IN_PRESETS = new java.util.HashSet<>(java.util.Arrays.asList("hidePillars", "leftClickBankAll"));
	/**
	 * The pages over the rows, the last the one that shows: the death report, and the wave report in full, which can open
	 * from it and goes Back to it. Empty while the rows show.
	 */
	private final List<JComponent> pages = new ArrayList<>();

	SolSurvivorPanel(Actions actions)
	{
		this.actions = actions;
		this.cards = new WaveCards(new WaveCards.Actions()
		{
			@Override
			public void watchAt(int id, Integer tick)
			{
				currentAction(() -> actions.watchAt(id, tick));
			}

			@Override
			public void copyReport(int id)
			{
				currentAction(() -> actions.copyReport(id));
			}
		}, new WaveCards.Pages()
		{
			@Override
			public void showPage(JComponent page)
			{
				pastResultKey = null;
				pages.remove(page);
				pages.add(page);
				showTop();
			}

			@Override
			public void closePage(JComponent page)
			{
				if (pages.remove(page))
				{
					showTop();
				}
			}

			@Override
			public void showMain()
			{
				if (!pages.isEmpty())
				{
					pages.clear();
					showTop();
				}
			}
		});
		setBackground(Stone.BACKGROUND);
		getParent().setBackground(Stone.BACKGROUND);
		getScrollPane().getViewport().setBackground(Stone.BACKGROUND);
		getWrappedPanel().setBackground(Stone.BACKGROUND);

		rows.add(header());
		rows.add(tabStrip);
		JPanel wave = tabBody(Tab.WAVE);
		wave.add(startRow());
		wave.add(cards.deathRow);
		wave.add(pickRow());
		wave.add(waveRow());
		wave.add(cards.reportCard);
		wave.add(boardRow());
		JPanel past = tabBody(Tab.PAST);
		past.add(pastHeading());
		runsBox.setLayout(new BoxLayout(runsBox, BoxLayout.Y_AXIS));
		runsBox.setOpaque(false);
		past.add(runsBox);
		past.add(gap(6));
		past.add(sendsRow);
		JPanel screen = tabBody(Tab.SCREEN);
		screen.add(presetsRow);
		screen.add(gameRow);
		buildTabs();
		addRows();

		showSharing(false);
		showInGame(new boolean[IN_GAME.length]);
		showSends(false, false, false);
		showBoard(null, false, true);
		showState(false, false, null, "Not in the Colosseum", "Enter it to open a wave.", new ArrayList<>(), "", new ArrayList<>());
	}

	/**
	 * A tab's body, its rows one under another with the panel's own gap. Only the shown tab's is in the panel (addRows),
	 * as RuneLite's grid gives a hidden row its height still.
	 */
	private JPanel tabBody(Tab t)
	{
		JPanel body = clear(new net.runelite.client.ui.DynamicGridLayout(0, 1, 0, 3));
		tabBodies.put(t, body);
		return body;
	}

	/** The tabs' strip: each tab's name, the one shown in yellow on an orange line, the others muted on a dark one. */
	private void buildTabs()
	{
		tabStrip.removeAll();
		for (Tab t : Tab.values())
		{
			javax.swing.JButton button = new javax.swing.JButton(t.label);
			button.setFont(bold());
			button.setForeground(t == tab ? Stone.YELLOW : Stone.MUTED);
			button.setContentAreaFilled(false);
			button.setOpaque(false);
			button.setFocusPainted(false);
			button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			button.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(0, 0, 2, 0, t == tab ? Stone.ORANGE : Stone.BEVEL_DARK), new EmptyBorder(3, 0, 3, 0)));
			button.getAccessibleContext().setAccessibleName(t.label + (t == tab ? ", shown" : ""));
			button.addActionListener(e -> showTab(t));
			tabStrip.add(button);
		}
		tabStrip.revalidate();
		tabStrip.repaint();
	}

	/** Shows tab `t` in place of the one shown. On the Swing thread. */
	void showTab(Tab t)
	{
		if (t == tab)
		{
			return;
		}
		tab = t;
		buildTabs();
		// Under a page the tab shows once the page closes (showTop).
		if (pages.isEmpty())
		{
			addRows();
		}
	}

	/** The tab shown. */
	Tab shownTab()
	{
		return tab;
	}

	/** The rows in their order: the name, the tabs, and the shown tab's body. */
	private void addRows()
	{
		removeAll();
		for (JComponent row : rows)
		{
			add(row);
		}
		add(tabBodies.get(tab));
		revalidate();
		repaint();
	}

	/** The last page, in place of the rows, from its top; or the rows again once no page is left. */
	private void showTop()
	{
		if (pages.isEmpty())
		{
			addRows();
			return;
		}
		removeAll();
		add(pages.get(pages.size() - 1));
		revalidate();
		repaint();
		javax.swing.SwingUtilities.invokeLater(() -> getScrollPane().getVerticalScrollBar().setValue(0));
	}

	static Font bold()
	{
		return FontManager.getRunescapeBoldFont();
	}

	static Font small()
	{
		return FontManager.getRunescapeSmallFont();
	}

	static JLabel label(String text, Font font, Color color)
	{
		JLabel l = new JLabel(text);
		l.setFont(font);
		l.setForeground(color);
		return l;
	}

	static JPanel clear(java.awt.LayoutManager layout)
	{
		JPanel p = new JPanel(layout);
		p.setOpaque(false);
		return p;
	}

	/** The sun, the panel's name, and the question mark that holds the help. */
	private static JPanel header()
	{
		JPanel h = clear(new BorderLayout(6, 0));
		h.add(new JLabel(Stone.sun(20)), BorderLayout.WEST);
		h.add(new Stone.Shadowed("Sol Survivor", bold(), Stone.ORANGE), BorderLayout.CENTER);
		JLabel help = new JLabel(Stone.question(13, Stone.MUTED, bold()));
		help.setToolTipText(wrapHtml(HELP, 260));
		h.add(help, BorderLayout.EAST);
		h.setBorder(new EmptyBorder(0, 0, 2, 2));
		return h;
	}

	/** The row for the wave under way: its number in a slot, its title and state, Arena now and the last result. */
	private JPanel waveRow()
	{
		waveSlot.setFont(bold());
		JPanel text = clear(new BorderLayout());
		text.add(waveTitle, BorderLayout.NORTH);
		text.add(waveState, BorderLayout.CENTER);
		JPanel top = clear(new BorderLayout(8, 0));
		top.add(waveSlot, BorderLayout.WEST);
		top.add(text, BorderLayout.CENTER);

		openNow.addActionListener(e -> currentAction(actions::openNow));

		resultWell.add(result);
		resultWell.setVisible(false);

		// The wave's list and the modifiers held go under the button and its well, in the wave's row, as the designs have them.
		JPanel under = clear(new BorderLayout(0, 0));
		under.add(resultWell, BorderLayout.NORTH);
		under.add(cards.waveBody, BorderLayout.CENTER);

		Stone.Row row = new Stone.Row(new BorderLayout(0, 6), 5, 5, 6, 6);
		row.add(top, BorderLayout.NORTH);
		row.add(openNow, BorderLayout.CENTER);
		row.add(under, BorderLayout.SOUTH);
		return row;
	}

	/**
	 * Minimus's pick: a row at the top, in the arena, lit gold while he offers his three, which it shows by their
	 * pictures over a button as wide as the row, Arena now's twin, that opens them on the site; until he does, its head
	 * alone, greyed.
	 */
	private JPanel pickRow()
	{
		JPanel text = clear(new BorderLayout(0, 1));
		text.add(new Stone.Shadowed("Modifier pick", bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(pickState, BorderLayout.CENTER);
		pickState.setForeground(Stone.TEXT);
		openPick.addActionListener(e -> currentAction(actions::openRecommender));
		openPick.setToolTipText("Opens the site's modifier pick with Minimus's three, the modifiers you hold and the next wave filled in, and copies "
			+ "its link.");
		JPanel head = clear(new BorderLayout(8, 0));
		head.add(pickSlot, BorderLayout.WEST);
		head.add(text, BorderLayout.CENTER);
		pickRow.setLayout(new BorderLayout(0, 6));
		pickRow.setVisible(false);
		pickRow.add(head, BorderLayout.NORTH);
		pickRow.add(offersWell, BorderLayout.CENTER);
		pickRow.add(openPick, BorderLayout.SOUTH);
		return pickRow;
	}

	/** An offer in the pick's well: its picture over its name and tier, the name in the tooltip too. */
	private static JComponent offer(Offer o)
	{
		JLabel l = label(wrapHtml("<div style='text-align: center'>" + o.name.replace("&", "&amp;").replace("<", "&lt;") + "</div>", 56), small(),
			Stone.TEXT);
		l.setIcon(Icons.of(Icons.modifier(o.key), 20));
		l.setVerticalTextPosition(JLabel.BOTTOM);
		l.setHorizontalTextPosition(JLabel.CENTER);
		l.setHorizontalAlignment(JLabel.CENTER);
		l.setVerticalAlignment(JLabel.TOP);
		l.setIconTextGap(2);
		l.setToolTipText(o.name);
		return l;
	}

	/** The leaderboard's row: its name and what goes up, the page's Open, and Remove my times under them. */
	private JPanel boardRow()
	{
		JPanel text = clear(new BorderLayout(0, 1));
		text.add(new Stone.Shadowed("Leaderboard", bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(boardState, BorderLayout.CENTER);
		openBoard.setToolTipText("Opens the Sol Survivor site's leaderboard in your browser, outside a wave.");
		openBoard.addActionListener(e -> currentAction(actions::openLeaderboard));
		JPanel east = clear(new BorderLayout());
		east.add(openBoard, BorderLayout.NORTH);
		removeTimes.setToolTipText("Takes your name and every time of yours off the leaderboard, and switches Leaderboard off.");
		removeTimes.addActionListener(e ->
		{
			int answer = JOptionPane.showConfirmDialog(this,
				"Take your name and every time of yours off the Sol Survivor leaderboard? Leaderboard is switched off too, so that no more go up.",
				"Remove my times", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if (answer == JOptionPane.OK_OPTION)
			{
				currentAction(actions::removeTimes);
			}
		});
		JPanel top = clear(new BorderLayout(8, 0));
		top.add(new Stone.Slot(34, MEDAL), BorderLayout.WEST);
		top.add(text, BorderLayout.CENTER);
		top.add(east, BorderLayout.EAST);
		JPanel foot = clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
		foot.add(removeTimes);
		boardRow.add(top, BorderLayout.NORTH);
		boardRow.add(foot, BorderLayout.CENTER);
		return boardRow;
	}

	/**
	 * The leaderboard's row, on the Swing thread: hidden for `name` null; for "", Leaderboard is off but the player has
	 * times to remove; else the name their waves go up under, while "Share wave logs" is on (`sharing`). The page opens only
	 * while `open`, outside a wave.
	 */
	void showBoard(String name, boolean sharing, boolean open)
	{
		boardRow.setVisible(name != null);
		if (name == null)
		{
			return;
		}
		boardState.setText(wrap(name.isEmpty() ? "Leaderboard is off: your times stay up until you remove them."
			: sharing ? "Your waves go up as " + name + "." : "Turn on Share wave logs for your waves to go up as " + name + ".", 140));
		openBoard.setEnabled(open);
		revalidate();
	}

	/**
	 * Past waves' heading: its name, and beside it while Share wave logs is on, that the logs go to the site. In the arena
	 * a click on it folds the section open or shut, as on What you share's head.
	 */
	private JPanel pastHeading()
	{
		sharing.setIcon(Stone.dot(9, Stone.GOOD));
		sharing.setIconTextGap(2);
		sharing.setToolTipText("Share wave logs is on: each wave's log goes to the site as the wave ends.");
		pastName.add(new Stone.Shadowed("Past waves", bold(), Stone.ORANGE));
		pastName.add(Box.createHorizontalStrut(5));
		pastName.add(pastCount);
		pastCaret.setVisible(false);
		pastHeading.add(pastCaret, BorderLayout.WEST);
		pastHeading.add(pastName, BorderLayout.CENTER);
		pastHeading.add(sharing, BorderLayout.EAST);
		pastHeading.setBorder(new EmptyBorder(6, 1, 0, 1));
		MouseAdapter fold = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				if (shownInColosseum)
				{
					pastOpen = !pastOpen;
					buildRuns();
				}
			}
		};
		pastHeading.addMouseListener(fold);
		for (Component c : pastHeading.getComponents())
		{
			c.addMouseListener(fold);
		}
		return pastHeading;
	}

	/** The Getting started card: its heading and Hide, the three steps, and the sample wave's button and what it opens. */
	private JPanel startRow()
	{
		Stone.Button hide = new Stone.Button("Hide", null, false, small(), 6, 3);
		hide.setToolTipText("Hides this card for good. The help is in the question mark at the top.");
		hide.addActionListener(e -> actions.hideGettingStarted());
		JPanel head = clear(new BorderLayout(6, 0));
		head.add(new Stone.Shadowed("Getting started", bold(), Stone.ORANGE), BorderLayout.CENTER);
		head.add(hide, BorderLayout.EAST);

		JPanel steps = clear(null);
		steps.setLayout(new BoxLayout(steps, BoxLayout.Y_AXIS));
		steps.add(step(enterNumber, enterStep));
		steps.add(gap(4));
		steps.add(step(number(Stone.number(14, 2, small())), arenaStep));
		steps.add(gap(4));
		steps.add(step(number(Stone.number(14, 3, small())), label(stepText("On the site, press <font color='#ffff00'>Solve wave</font> and "
			+ "play the plan it finds."), small(), Stone.TEXT)));

		Stone.Button sample = new Stone.Button("Try a sample wave", SAMPLE_ICON, true, small(), 7, 6);
		sample.setToolTipText("Opens wave 7 on the Sol Survivor site with the plan it found, to look round before you play.");
		sample.addActionListener(e -> actions.openSample());
		startFoot.add(sample, BorderLayout.NORTH);
		startFoot.add(label(wrap("Opens wave 7 on the site with its plan, no Colosseum needed.", 200), small(), Stone.DIM), BorderLayout.CENTER);

		startRow.setGlow(Stone.YELLOW);
		startRow.add(head, BorderLayout.NORTH);
		startRow.add(steps, BorderLayout.CENTER);
		startRow.add(startFoot, BorderLayout.SOUTH);
		startRow.setVisible(false);
		return startRow;
	}

	/** A step's words, wrapped to the card beside its number. */
	private static String stepText(String html)
	{
		return wrapHtml(html, 170);
	}

	/** A step's number in a yellow disc, or its tick once done, held to the top of its step. */
	private static JLabel number(ImageIcon disc)
	{
		JLabel number = new JLabel(disc);
		number.setVerticalAlignment(JLabel.TOP);
		number.setBorder(new EmptyBorder(1, 0, 0, 0));
		return number;
	}

	/** A step of Getting started: its number, then its words. */
	private static JPanel step(JLabel number, JLabel words)
	{
		JPanel line = clear(new BorderLayout(6, 0));
		line.add(number, BorderLayout.WEST);
		line.add(words, BorderLayout.CENTER);
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		return line;
	}

	/**
	 * The three settings that send anything to the site, as What you share lists them: each one's name in the settings,
	 * what it sends and what it gets the player.
	 */
	private static final String[][] SENDS = {
		{"Upload past runs", "a run's tiles, hitpoints, prayers and hits, when you press Watch.", "your run played back on the site, "
			+ "for anyone with its link, for seven days."},
		{"Share wave logs", "each wave's log as it ends, with no name or levels.", "a planner checked against real waves."},
		{"Leaderboard", "your name with each wave you finish. Needs Share wave logs.", "a place on the site's leaderboard."},
	};

	/**
	 * What you share, folded open or shut as the player left it, else open while the Getting started card shows out of the
	 * arena: in it, the panel is for the wave (F4).
	 */
	private void buildSends()
	{
		boolean open = sendsOpen != null ? sendsOpen : startRow.isVisible() && !startInArena;
		int on = 0;
		for (boolean send : shownSends)
		{
			on += send ? 1 : 0;
		}
		sendsRow.removeAll();

		JPanel head = clear(new BorderLayout(5, 0));
		head.add(new JLabel(open ? OPEN_ICON : SHUT_ICON), BorderLayout.WEST);
		head.add(new Stone.Shadowed("What you share", bold(), Stone.ORANGE), BorderLayout.CENTER);
		head.add(label(on == 0 ? "All off" : on + " on", small(), on == 0 ? Stone.MUTED : Stone.GOOD), BorderLayout.EAST);
		head.setBorder(new EmptyBorder(0, 1, 0, 1));
		head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		head.setToolTipText(open ? null : "What the plugin sends to the Sol Survivor site, and what each setting gets you.");
		MouseAdapter fold = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				sendsOpen = !open;
				buildSends();
			}
		};
		head.addMouseListener(fold);
		for (Component c : head.getComponents())
		{
			c.addMouseListener(fold);
		}
		sendsRow.add(head, BorderLayout.NORTH);

		if (open)
		{
			JPanel body = clear(null);
			body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
			body.add(text("Each is off until you switch it on. Here is what it sends, and what it gets you.", Stone.TEXT));
			for (int i = 0; i < SENDS.length; i++)
			{
				body.add(gap(5));
				body.add(send(SENDS[i], shownSends[i]));
			}
			body.add(gap(5));
			body.add(text("Switch them under Sending to the site in this plugin's settings, where RuneLite warns you first. "
				+ "With all three off, the plugin sends nothing.", Stone.DIM));
			Stone.Row card = new Stone.Row(new BorderLayout(), 4, 6, 5, 5);
			card.add(body, BorderLayout.CENTER);
			sendsRow.add(card, BorderLayout.CENTER);
		}
		sendsRow.revalidate();
		sendsRow.repaint();
	}

	/** A line of What you share's words, as wide as the row. */
	private static JComponent text(String words, Color color)
	{
		JLabel l = label(wrap(words, 200), small(), color);
		JPanel p = clear(new BorderLayout());
		p.add(l, BorderLayout.CENTER);
		p.setAlignmentX(Component.LEFT_ALIGNMENT);
		return p;
	}

	/** A setting in What you share's well: its name and whether it is on, what it sends and what it gets the player. */
	private static JComponent send(String[] setting, boolean on)
	{
		JPanel top = clear(new BorderLayout(4, 0));
		top.add(label(setting[0], small(), Stone.TEXT), BorderLayout.CENTER);
		Stone.Tag tag = on ? new Stone.Tag("On", Stone.GOOD, CLEAN_OUTLINE, small()) : new Stone.Tag("Off", Stone.MUTED, Stone.BEVEL_LIGHT, small());
		JPanel east = clear(new BorderLayout());
		east.add(tag, BorderLayout.NORTH);
		top.add(east, BorderLayout.EAST);
		JPanel words = clear(new BorderLayout(0, 1));
		words.add(label(wrapHtml("Sends: " + setting[1], 180), small(), Stone.MUTED), BorderLayout.NORTH);
		words.add(label(wrapHtml("<font color='#3cff3c'>Gets you:</font> " + setting[2], 180), small(), Stone.MUTED), BorderLayout.CENTER);
		Stone.Well well = new Stone.Well(new BorderLayout(0, 3), 4, 6, 5, 6);
		well.add(top, BorderLayout.NORTH);
		well.add(words, BorderLayout.CENTER);
		well.setAlignmentX(Component.LEFT_ALIGNMENT);
		return well;
	}

	/**
	 * In the game's features: each one's key in the settings, its name there, and what it draws, for the line's tooltip.
	 * The wave box's key holds Off, This wave, Next wave or Both: its switch sets Both, and reads any but Off as on.
	 */
	static final String[][] IN_GAME = {
		{"waveBox", "Wave box", "The wave's NPCs and the next wave's, in a box you move with Alt and drag."},
		{"modifiersBar", "Modifiers above the chatbox", "The modifiers you hold, by their pictures and tiers."},
		{"doomByOrb", "Doom stacks", "Doom's stacks against its limit, red on the last."},
		{"volatilityReminder", "Volatility reminder", "Volatility's picture over each NPC that dies, while you hold it."},
		{"offerNames", "Names on Minimus's offers", "Each offer's name and the tier taking it brings, on his screen."},
		{"npcHitpoints", "NPC hitpoints", "Each NPC's hitpoints over its head."},
		{"highlightInRange", "NPCs in range", "Outlines the NPCs you can hit from where you stand."},
		{"myRange", "My attack range", "Outlines the tiles you can hit from where you stand."},
		{"tickCounter", "Tick counter", "A plain tick count from entering the arena."},
		{"waveTimer", "Wave timer", "The time since your modifier pick."},
		{"hidePillars", "Hide the pillars", "Hides the four pillars and marks their squares on the floor."},
		{"lootTotals", "Loot totals", "The loot's value so far, on Minimus's screen."},
		{"leftClickBankAll", "Left-click Bank-all", "Makes Bank-all the reward chest's left click."},
	};

	/**
	 * In the game, folded open or shut as the player left it, else open: it is what the Screen tab is for. The presets'
	 * card above it is built with it.
	 */
	private void buildGame()
	{
		buildPresets();
		boolean open = gameOpen == null || gameOpen;
		int on = 0;
		for (boolean feature : shownGame)
		{
			on += feature ? 1 : 0;
		}
		gameRow.removeAll();

		JPanel head = clear(new BorderLayout(5, 0));
		head.add(new JLabel(open ? OPEN_ICON : SHUT_ICON), BorderLayout.WEST);
		head.add(new Stone.Shadowed("In the game", bold(), Stone.ORANGE), BorderLayout.CENTER);
		head.add(label(on + " of " + IN_GAME.length + " on", small(), on == 0 ? Stone.MUTED : Stone.GOOD), BorderLayout.EAST);
		head.setBorder(new EmptyBorder(0, 1, 0, 1));
		head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		head.setToolTipText(open ? null : "What the plugin draws in the arena, each with its switch.");
		MouseAdapter fold = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				gameOpen = !open;
				buildGame();
			}
		};
		head.addMouseListener(fold);
		for (Component c : head.getComponents())
		{
			c.addMouseListener(fold);
		}
		gameRow.add(head, BorderLayout.NORTH);

		if (open)
		{
			JPanel body = clear(null);
			body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
			body.add(text("What the plugin draws in the arena. Switch off any you already have from another plugin.", Stone.TEXT));
			for (int i = 0; i < IN_GAME.length; i++)
			{
				body.add(gap(3));
				body.add(feature(IN_GAME[i], shownGame[i]));
			}
			body.add(gap(5));
			body.add(text("Each one's colours, places and sizes are in this plugin's settings.", Stone.DIM));
			Stone.Row card = new Stone.Row(new BorderLayout(), 4, 6, 5, 5);
			card.add(body, BorderLayout.CENTER);
			gameRow.add(card, BorderLayout.CENTER);
		}
		gameRow.revalidate();
		gameRow.repaint();
	}

	/** A line of In the game: the feature's name, and its switch, which says On or Off and sets the other on a press. */
	private JComponent feature(String[] feature, boolean on)
	{
		JLabel name = label(feature[1], small(), on ? Stone.TEXT : Stone.MUTED);
		name.setToolTipText(feature[2]);
		Stone.Button toggle = new Stone.Button(on ? "On" : "Off", Stone.dot(9, on ? Stone.GOOD : Stone.MUTED), false, small(), 6, 3);
		toggle.setToolTipText((on ? "Switch off: " : "Switch on: ") + feature[2]);
		toggle.addActionListener(e -> actions.setInGame(feature[0], !on));
		JPanel east = clear(new BorderLayout());
		east.add(toggle, BorderLayout.NORTH);
		JPanel line = clear(new BorderLayout(4, 0));
		line.add(name, BorderLayout.CENTER);
		line.add(east, BorderLayout.EAST);
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		return line;
	}

	/** In the game: which of IN_GAME's features are on, in its order, as the settings hold them. On the Swing thread. */
	void showInGame(boolean[] on)
	{
		if (!java.util.Arrays.equals(on, shownGame))
		{
			shownGame = on.clone();
			buildGame();
		}
	}

	/** The preset whose drawing features are on and every other off, as the switches stand; null for none of them. */
	static String presetOf(boolean[] on)
	{
		for (String[] preset : PRESETS)
		{
			java.util.Set<String> keys = new java.util.HashSet<>(java.util.Arrays.asList(preset).subList(2, preset.length));
			boolean matches = true;
			for (int i = 0; i < IN_GAME.length && matches; i++)
			{
				String key = IN_GAME[i][0];
				matches = NOT_IN_PRESETS.contains(key) || on[i] == keys.contains(key);
			}
			if (matches)
			{
				return preset[0];
			}
		}
		return null;
	}

	/** The presets' card: a button for each, the one the switches stand at on the accept face, and a line on the others. */
	private void buildPresets()
	{
		presetsRow.removeAll();
		String current = presetOf(shownGame);
		JPanel head = clear(new BorderLayout(5, 0));
		head.add(new Stone.Shadowed("Preset", bold(), Stone.ORANGE), BorderLayout.CENTER);
		head.add(label(current == null ? "Your own" : current, small(), current == null ? Stone.MUTED : Stone.GOOD), BorderLayout.EAST);
		presetsRow.add(head, BorderLayout.NORTH);
		JPanel buttons = clear(new java.awt.GridLayout(1, PRESETS.length, 4, 0));
		for (String[] preset : PRESETS)
		{
			Stone.Button button = new Stone.Button(preset[0], null, preset[0].equals(current), small(), 4, 4);
			button.setToolTipText(wrapHtml(preset[1] + " Hide the pillars and Left-click Bank-all stay as they are.", 220));
			button.addActionListener(e -> applyPreset(preset));
			buttons.add(button);
		}
		presetsRow.add(buttons, BorderLayout.CENTER);
		presetsRow.revalidate();
		presetsRow.repaint();
	}

	/** Switches each drawing feature to the preset's: on where it lists it, off where not, leaving those already so. */
	private void applyPreset(String[] preset)
	{
		java.util.Set<String> keys = new java.util.HashSet<>(java.util.Arrays.asList(preset).subList(2, preset.length));
		for (int i = 0; i < IN_GAME.length; i++)
		{
			String key = IN_GAME[i][0];
			if (!NOT_IN_PRESETS.contains(key) && shownGame[i] != keys.contains(key))
			{
				actions.setInGame(key, keys.contains(key));
			}
		}
	}

	/**
	 * What the panel shows, on the Swing thread: whether the player is in the Colosseum, whether Arena now has a wave to
	 * open (`live`: one of waves 1 to 11 under way), the wave for the slot (null for none), the wave row's title and state,
	 * Minimus's offers while he makes them, a note in place of the runs when there are none, and the runs, newest first.
	 * The runs' rows are built again only when they changed, so that a line under the mouse stays as it is.
	 */
	void showState(boolean inColosseum, boolean live, Integer wave, String title, String state, List<Offer> offerList, String note,
		List<RunGroup> runs)
	{
		boolean offers = !offerList.isEmpty();
		// As a wave starts or Minimus offers, the Wave tab shows, which holds Arena now and the pick.
		if (live && !shownLive || offers && !shownOffering)
		{
			showTab(Tab.WAVE);
		}
		shownLive = live;
		shownOffering = offers;
		if (!Objects.equals(wave, shownWave) || shownRuns == null)
		{
			waveSlot.show(SUN, wave == null ? null : String.valueOf(wave));
			shownWave = wave;
		}
		waveTitle.setText(title);
		waveState.setText(state);
		// Arena now stays where it is, greyed while it has nothing to open, so that the panel does not jump and shows what it
		// is for (the user's choice, 2026-10-26); it is lit only while a wave it plans is under way, as the arena has nothing
		// to open before a wave or between two (the UX review of 2026-10-27, F1). The pick is its head alone in the arena
		// until Minimus offers, as its button can do nothing through a wave (F9, the user's ask of 2026-10-28 to fold what a
		// moment does not need), and goes out of the arena, where there is no run to pick for.
		openNow.setEnabled(live);
		openNow.setToolTipText(live ? "Opens the arena as it is now on the site, and copies its link."
			: !inColosseum ? "Opens the arena on the site once you are in the Colosseum."
			: wave != null && wave > ColosseumTracker.LAST_WAVE ? "Sol Survivor plans waves 1 to 11, not Sol Heredit's."
			: "Opens the arena on the site once the wave starts. A wave you played opens at its start from the play button on its "
				+ "line under Past waves.");
		if (!offerList.equals(shownOffers) || inColosseum != pickRow.isVisible() || shownRuns == null)
		{
			offersWell.removeAll();
			for (Offer o : offerList)
			{
				offersWell.add(offer(o));
			}
			offersWell.setVisible(offers);
			pickState.setText(!offers
				? wrap("Opens when Minimus makes his offers.", 150)
				: wave != null
				? wrapHtml("Minimus offers three for wave <font color='#ffff00'>" + wave + "</font>", 150)
				: wrap("Minimus offers three.", 150));
			pickState.setForeground(offers ? Stone.TEXT : Stone.MUTED);
			pickSlot.show(offers ? MINIMUS_ON : MINIMUS_OFF, null);
			pickRow.setGlow(offers ? Stone.GOLD : null);
			pickRow.setVisible(inColosseum);
			openPick.setVisible(offers);
			shownOffers = new ArrayList<>(offerList);
			pickRow.revalidate();
			pickRow.repaint();
			revalidate();
			repaint();
		}
		if (runs.equals(shownRuns) && note.equals(shownNote) && inColosseum == shownInColosseum)
		{
			return;
		}
		if (!inColosseum)
		{
			pastOpen = false;
		}
		shownRuns = new ArrayList<>(runs);
		shownNote = note;
		shownInColosseum = inColosseum;
		buildRuns();
	}

	/** Whether the waves' logs are shared, as "Share wave logs" and "Record wave log" are set. On the Swing thread. */
	void showSharing(boolean on)
	{
		sharing.setVisible(on);
	}

	/**
	 * The Getting started card (O1): `shown` until the player hid it or opened an arena's link, and never during a wave;
	 * its first step as "Open panel on entering" is set (`opensItself`). In the arena (`inArena`) the card moves on (the UX
	 * review of 2026-10-27, F4): the first step is ticked, the second points at Arena now below, and the sample wave goes,
	 * as the player has a wave of their own. On the Swing thread.
	 */
	void showGettingStarted(boolean shown, boolean opensItself, boolean inArena)
	{
		enterNumber.setIcon(inArena ? Stone.done(14) : Stone.number(14, 1, small()));
		enterStep.setText(stepText(inArena
			? "You are in the Colosseum."
			: opensItself
			? "Walk into the Fortis Colosseum. This panel opens by itself."
			: "Walk into the Fortis Colosseum, and open this panel from its sun in the sidebar."));
		enterStep.setForeground(inArena ? Stone.DIM : Stone.TEXT);
		arenaStep.setText(stepText(inArena
			? "Press <font color='#ffff00'>Arena now</font>, below, at a wave's start or on a pillar stack."
			: "Press <font color='#ffff00'>Arena now</font> at a wave's start, or on a pillar stack."));
		startFoot.setVisible(!inArena);
		if (startRow.isVisible() != shown || startInArena != inArena)
		{
			startRow.setVisible(shown);
			startInArena = inArena;
			if (shownSends != null)
			{
				buildSends();
			}
			if (shownGame != null)
			{
				buildGame();
			}
			revalidate();
			repaint();
		}
	}

	/**
	 * What you share (O2): which of Upload past runs, Share wave logs and Leaderboard are on, as the settings hold them;
	 * `shown` false hides the row, as during a wave. On the Swing thread.
	 */
	void showSends(boolean upload, boolean share, boolean board)
	{
		boolean[] sends = {upload, share, board};
		if (!java.util.Arrays.equals(sends, shownSends))
		{
			boolean uploadWas = uploadOn();
			shownSends = sends;
			buildSends();
			// Watch, in the cards and on each run, goes secondary while Upload past runs is off (F2).
			cards.showUpload(upload);
			if (upload != uploadWas && shownRuns != null)
			{
				buildRuns();
			}
		}
	}

	/** Whether Upload past runs is on, which every Watch needs; off until the settings say. */
	private boolean uploadOn()
	{
		return shownSends != null && shownSends[0];
	}

	/** What you share, hidden during a wave. On the Swing thread. */
	void showSendsRow(boolean shown)
	{
		if (sendsRow.isVisible() != shown)
		{
			sendsRow.setVisible(shown);
			revalidate();
			repaint();
		}
	}

	/** The last answer in the main well, beside its past-run or wave action, and on a page while one shows. */
	void showResult(String message)
	{
		showResult(message, pastResultKey);
	}

	/** The source captured at the press, before a slow upload can finish after a different action. */
	String resultTarget()
	{
		return pastResultKey;
	}

	void showResult(String message, String target)
	{
		boolean clearPast = pastResultKey != null && target == null;
		pastResultKey = target;
		result.setText(wrap(message, 196));
		resultWell.setVisible(!message.isEmpty());
		revalidate();
		cards.showResult(message);
		if (pastResultKey != null || clearPast)
		{
			pastResultMessage = message;
			buildRuns();
		}
	}

	/** A current-wave action no longer directs its answer to an older run or wave. */
	private void currentAction(Runnable action)
	{
		boolean clearPast = pastResultKey != null;
		pastResultKey = null;
		pastResultMessage = "";
		if (clearPast)
		{
			buildRuns();
		}
		action.run();
	}

	/** Remembers the pressed run or wave before its asynchronous action can answer. */
	private void pastAction(String key, Runnable action)
	{
		pastResultKey = key;
		pastResultMessage = "";
		buildRuns();
		action.run();
	}

	/** The last answer immediately below its run's head or wave's line, even with Messages in chat switched off. */
	private JPanel pastResult(String key, int width)
	{
		Stone.Well well = new Stone.Well(new BorderLayout(), 4, 5, 4, 5);
		boolean shown = key.equals(pastResultKey) && !pastResultMessage.isEmpty();
		if (shown)
		{
			well.add(label(wrap(pastResultMessage, width), small(), Stone.TEXT));
		}
		well.setVisible(shown);
		return well;
	}

	/**
	 * Past waves: out of the arena the runs, as the player left each one; in the arena only the heading, with how many
	 * waves it holds, until the player opens it, to keep the panel clean while they play (the user's ask of 2026-10-27).
	 */
	private void buildRuns()
	{
		runsBox.removeAll();
		boolean open = !shownInColosseum || pastOpen;
		int waves = 0;
		for (RunGroup run : shownRuns)
		{
			waves += run.waves.size();
		}
		pastCaret.setIcon(open ? OPEN_ICON : SHUT_ICON);
		pastCaret.setVisible(shownInColosseum);
		pastCount.setText(waves == 0 ? "" : WaveCards.plural(waves, "wave"));
		pastCount.setVisible(!open);
		pastHeading.setCursor(shownInColosseum ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : null);
		String tip = open ? null : "Your past waves, folded away in the arena. Click to show them; out of the arena they always show.";
		pastCaret.setToolTipText(tip);
		pastName.setToolTipText(tip);
		runsBox.setVisible(open);
		if (!open)
		{
			runsBox.revalidate();
			runsBox.repaint();
			return;
		}
		if (shownRuns.isEmpty())
		{
			Stone.Well empty = new Stone.Well(new BorderLayout(), 6, 7, 6, 7);
			empty.add(label(wrap(shownNote, 200), small(), Stone.MUTED));
			empty.setAlignmentX(Component.LEFT_ALIGNMENT);
			runsBox.add(empty);
		}
		for (int i = 0; i < shownRuns.size(); i++)
		{
			RunGroup run = shownRuns.get(i);
			if (i > 0)
			{
				runsBox.add(gap(5));
			}
			// The newest run is open, in the arena too once the player opened Past waves to see them; a run the player
			// opened or shut stays as they left it.
			runsBox.add(runRow(run, folded.getOrDefault(run.key, i == 0)));
		}
		runsBox.revalidate();
		runsBox.repaint();
	}

	/** A run's row: a click on its head folds it open or shut; its buttons copy the whole run and watch it back. */
	private JPanel runRow(RunGroup run, boolean open)
	{
		JPanel text = clear(new BorderLayout());
		text.add(new Stone.Shadowed(run.name, bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(label(run.detail, small(), Stone.TEXT), BorderLayout.CENTER);
		if (run.glory != null)
		{
			text.add(label(run.glory, small(), Stone.GOLD), BorderLayout.SOUTH);
		}

		WaveRow newest = run.waves.get(0);
		Stone.Button copyRun = new Stone.Button(COPY_RUN_ICON, 24);
		copyRun.setToolTipText("Copy whole run: every wave of this run in one paste, for the site.");
		copyRun.addActionListener(e -> pastAction("run:" + run.key, () -> actions.copyRun(newest.id)));
		// While an upload is out every Watch button waits; its answer stays immediately under this run's head.
		Stone.Button watch = new Stone.Button("Watch", uploadOn() ? WATCH_ICON : WATCH_OFF_ICON, false, small(), 7, 6);
		watch.setEnabled(!replayBusy);
		watch.setToolTipText("Uploads this run's recording to the Sol Survivor site and opens it in your browser, to watch it back. "
			+ "Anyone with its link can view it for seven days. Needs Upload past runs, in the settings.");
		watch.addActionListener(e -> pastAction("run:" + run.key, () -> actions.openPastRun(newest.id)));
		JPanel buttons = clear(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		buttons.add(copyRun);
		buttons.add(watch);
		buttons.setBorder(new EmptyBorder(3, 0, 0, 0));

		JPanel head = clear(new BorderLayout(5, 0));
		head.add(new JLabel(open ? OPEN_ICON : SHUT_ICON), BorderLayout.WEST);
		head.add(text, BorderLayout.CENTER);
		head.add(buttons, BorderLayout.EAST);
		head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		MouseAdapter fold = new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				folded.put(run.key, !open);
				buildRuns();
			}
		};
		head.addMouseListener(fold);
		for (Component c : head.getComponents())
		{
			if (c != buttons)
			{
				c.addMouseListener(fold);
			}
		}

		Stone.Row row = new Stone.Row(new BorderLayout(0, 5), 4, 6, 5, 5);
		row.setAlignmentX(Component.LEFT_ALIGNMENT);
		JPanel heading = clear(new BorderLayout(0, 4));
		heading.add(head, BorderLayout.NORTH);
		heading.add(pastResult("run:" + run.key, 190), BorderLayout.CENTER);
		row.add(heading, BorderLayout.NORTH);
		if (open)
		{
			Stone.Well waves = new Stone.Well(null, 2, 3, 2, 3);
			waves.setLayout(new BoxLayout(waves, BoxLayout.Y_AXIS));
			for (int i = 0; i < run.waves.size(); i++)
			{
				waves.add(waveLine(run.waves.get(i), i == run.waves.size() - 1));
			}
			row.add(waves, BorderLayout.CENTER);
		}
		return row;
	}

	/** A wave's line: Play opens its start; the visible menu and right-click offer the same remaining actions. */
	private JPanel waveLine(WaveRow row, boolean last)
	{
		JPanel name = clear(new FlowLayout(FlowLayout.LEFT, 0, 0));
		name.add(label("Wave " + row.wave, small(), Stone.TEXT));
		if (row.mistakes != null)
		{
			// The wave's mistakes, as its report counts them, tagged as the designs tag them: green for a clean wave.
			Stone.Tag tag = row.mistakes == 0 ? new Stone.Tag("clean", Stone.GOOD, CLEAN_OUTLINE, small())
				: new Stone.Tag(WaveCards.plural(row.mistakes, "mistake"), Stone.ORANGE, MISTAKES_OUTLINE, small());
			tag.setBorder(null);
			name.add(Box.createHorizontalStrut(5));
			name.add(tag);
		}
		JPanel text = clear(new BorderLayout());
		text.add(name, BorderLayout.NORTH);
		text.add(label(row.detail, small(), row.underWay ? Stone.GOLD : Stone.MUTED), BorderLayout.SOUTH);

		Stone.Button start = new Stone.Button(START_ICON, 22);
		start.setToolTipText(row.start
			? "Wave start: this wave on the site at its first tick, and copy its link."
			: "Wave start: the plugin did not see this wave start.");
		start.setEnabled(row.start);
		start.setFocusPainted(true);
		start.getAccessibleContext().setAccessibleName("Play wave " + row.wave + " from its start");
		start.addActionListener(e -> pastAction("wave:" + row.id, () -> actions.openPastStart(row.id)));
		JPopupMenu menu = new JPopupMenu();
		JMenuItem reinforcements = new JMenuItem("Open at the reinforcements");
		reinforcements.setToolTipText(row.reinforcements ? "Opens this wave when its reinforcements came, and copies the link."
			: "The plugin did not see this wave's reinforcements come.");
		reinforcements.setEnabled(row.reinforcements);
		reinforcements.addActionListener(e -> pastAction("wave:" + row.id, () -> actions.openPastReinforcements(row.id)));
		JMenuItem copy = new JMenuItem("Copy wave log");
		copy.addActionListener(e -> pastAction("wave:" + row.id, () -> actions.copyWaveLog(row.id)));
		menu.add(reinforcements);
		menu.add(copy);
		Stone.Button more = new Stone.Button(MORE_ICON, 22);
		more.setFocusPainted(true);
		more.setToolTipText("More actions for wave " + row.wave + ": open at the reinforcements or copy its log. Right-click the row for the same menu.");
		more.getAccessibleContext().setAccessibleName("More actions for wave " + row.wave);
		more.addActionListener(e -> menu.show(more, 0, more.getHeight()));
		more.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "waveMenu");
		more.getActionMap().put("waveMenu", new AbstractAction()
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				more.doClick();
			}
		});
		JPanel buttons = clear(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		buttons.add(start);
		buttons.add(more);

		JPanel head = clear(new BorderLayout(4, 0));
		head.add(text, BorderLayout.CENTER);
		head.add(buttons, BorderLayout.EAST);
		JPanel line = clear(new BorderLayout(0, 3));
		line.add(head, BorderLayout.NORTH);
		line.add(pastResult("wave:" + row.id, 178), BorderLayout.CENTER);
		popupOn(line, menu);
		line.setBorder(BorderFactory.createCompoundBorder(
			last ? new EmptyBorder(0, 0, 0, 0) : BorderFactory.createMatteBorder(0, 0, 1, 0, Stone.BEVEL_DARK),
			new EmptyBorder(3, 4, 3, 1)));
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		return line;
	}

	/** Right-clicking any part of a wave row opens the same menu as its visible more-actions button. */
	private static void popupOn(JComponent component, JPopupMenu menu)
	{
		component.setComponentPopupMenu(menu);
		for (Component child : component.getComponents())
		{
			if (child instanceof JComponent)
			{
				popupOn((JComponent) child, menu);
			}
		}
	}

	static JComponent gap(int height)
	{
		JPanel gap = clear(null);
		gap.setAlignmentX(Component.LEFT_ALIGNMENT);
		gap.setPreferredSize(new Dimension(0, height));
		gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return gap;
	}

	/**
	 * HTML that wraps at `width` pixels of the screen, which a plain label does not. Swing's HTML counts a CSS pixel as
	 * 1.3 of the screen's, so the CSS width is the width over 1.3. Each width the panel asks for fits inside its row while
	 * the sidebar's scrollbar shows, 7 pixels narrower than without it: a width wider than the row cuts the end of each
	 * line off.
	 */
	static String wrap(String text, int width)
	{
		return wrapHtml(text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"), width);
	}

	static String wrapHtml(String html, int width)
	{
		return "<html><body style='width: " + Math.round(width / 1.3f) + "px'>" + html + "</body></html>";
	}

	/**
	 * The toolbar's sun, set out pixel by pixel so that the plugin carries no image file and every ray falls on whole
	 * pixels. A letter is a pixel in the colour SUN_COLOURS gives it and a dot is clear. The layout was made for 16
	 * pixels; the disc's pale yellow glowing to amber at its rim is from the sun Stefan made in Higgsfield on
	 * 2026-10-26, whose own thin rays fell between pixels at this size and broke up.
	 */
	private static final String[] TOOLBAR_SUN = {
		".......KK.......",
		"..K...KRRK...K..",
		".KRK..KOOK..KRK.",
		"..KOKKKOOKKKOK..",
		"...KOOAAAAOOK...",
		"...KOAYYYYAOK...",
		".KKKAYYYYYYAKKK.",
		"KROOAYYWWYYAOORK",
		"KROOAYYWWYYAOORK",
		".KKKAYYYYYYAKKK.",
		"...KOAYYYYAOK...",
		"...KOOAAAAOOK...",
		"..KOKKKOOKKKOK..",
		".KRK..KOOK..KRK.",
		"..K...KRRK...K..",
		".......KK.......",
	};

	/** The sun's letters, black, red, orange, amber, yellow and white, and their colours. */
	private static final String SUN_LETTERS = "KROAYW";
	private static final int[] SUN_COLOURS = {0xff000000, 0xfff93402, 0xfffe680c, 0xfffeb002, 0xffffe535, 0xfffffffd};

	/** The toolbar icon, the sun above at 16 by 16. */
	static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < 16; y++)
		{
			for (int x = 0; x < 16; x++)
			{
				int letter = SUN_LETTERS.indexOf(TOOLBAR_SUN[y].charAt(x));
				if (letter >= 0)
				{
					image.setRGB(x, y, SUN_COLOURS[letter]);
				}
			}
		}
		return image;
	}
}
