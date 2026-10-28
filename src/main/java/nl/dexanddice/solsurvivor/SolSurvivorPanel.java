package nl.dexanddice.solsurvivor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
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
 * page of its own, which takes the panel's place until Back; the death recap after a death; and on each past wave its
 * mistakes. Each row shows only in the moment it is about (the user's ask of 2026-10-26, "Plugin 1.0: what the panel
 * shows when"): the pick's only while Minimus offers, at the top; Arena now only in the arena; the recap only out of it,
 * after a death, over the rest; the leaderboard's only out of the arena; and in the arena Past waves fold into their
 * heading until the player opens them (the user's ask of 2026-10-27). Like the rest of the plugin the panel shows nothing
 * about how to play the wave; the plans are the site's.
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
		+ "<font color='#ff981f'>Past waves</font>: a wave's play button opens it at its start. In the arena, click the heading to "
		+ "show them.<br><br>"
		+ "The <font color='#ff981f'>wave report</font> comes as each wave ends, and the <font color='#ff981f'>death recap</font> "
		+ "after a death.<br><br>"
		+ "<font color='#ff981f'>What you share</font>, at the foot, says what goes to the site. All of it is off until you switch it "
		+ "on, as is everything under On the game screen.";

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
	private static final ImageIcon REINFORCEMENTS_ICON = Stone.figures(12, Stone.ORANGE);
	private static final ImageIcon COPY_ICON = Stone.copy(11, Stone.MUTED);
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
	/** The wave's list, the wave report and the death recap, under the wave's row. */
	final WaveCards cards;
	private final Stone.Slot waveSlot = new Stone.Slot(34, SUN);
	private final JLabel waveTitle = new Stone.Shadowed("", bold(), Stone.ORANGE);
	private final JLabel waveState = label("", small(), Stone.TEXT);
	private final Stone.Button openNow = new Stone.Button("Arena now", ARENA, true, small(), 7, 6);
	/** The wave row's layout, whose gaps close up out of the arena, where the row keeps only its head and result. */
	private final BorderLayout waveLayout = new BorderLayout(0, 6);
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
	 * switched in the settings, where RuneLite shows its warning first.
	 */
	private final Stone.Row sendsRow = new Stone.Row(new BorderLayout(0, 5), 4, 6, 5, 5);
	/** Which of the three are on: Upload past runs, Share wave logs and Leaderboard, as the settings hold them. */
	private boolean[] shownSends;
	/**
	 * Whether the player folded What you share open or shut; null, it is open while the Getting started card shows out of
	 * the arena.
	 */
	private Boolean sendsOpen;
	/** Which runs the player folded open or shut, by key; a run not here is open when it is the newest. */
	private final Map<Integer, Boolean> folded = new HashMap<>();
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
	/** The page that takes the rows' place, the wave report in full, or null while the rows show. */
	private JComponent page;

	SolSurvivorPanel(Actions actions)
	{
		this.actions = actions;
		this.cards = new WaveCards(actions, new WaveCards.Pages()
		{
			@Override
			public void showPage(JComponent page)
			{
				SolSurvivorPanel.this.showPage(page);
			}

			@Override
			public void showMain()
			{
				SolSurvivorPanel.this.showMain();
			}
		});
		setBackground(Stone.BACKGROUND);
		getParent().setBackground(Stone.BACKGROUND);
		getScrollPane().getViewport().setBackground(Stone.BACKGROUND);
		getWrappedPanel().setBackground(Stone.BACKGROUND);

		rows.add(header());
		rows.add(startRow());
		rows.add(cards.recapCard);
		rows.add(pickRow());
		rows.add(waveRow());
		rows.add(cards.reportCard);
		rows.add(boardRow());
		rows.add(pastHeading());
		runsBox.setLayout(new BoxLayout(runsBox, BoxLayout.Y_AXIS));
		runsBox.setOpaque(false);
		rows.add(runsBox);
		rows.add(gap(6));
		rows.add(sendsRow);
		addRows();

		showSharing(false);
		showSends(false, false, false);
		showBoard(null, false, true);
		showState(false, false, false, null, "Not in the Colosseum", "Enter it to open a wave.", new ArrayList<>(), "", new ArrayList<>());
	}

	/** The rows in their order. */
	private void addRows()
	{
		removeAll();
		for (JComponent row : rows)
		{
			add(row);
		}
		revalidate();
		repaint();
	}

	/** The wave report's own page, in place of the rows, from its top. */
	private void showPage(JComponent shown)
	{
		page = shown;
		removeAll();
		add(shown);
		revalidate();
		repaint();
		javax.swing.SwingUtilities.invokeLater(() -> getScrollPane().getVerticalScrollBar().setValue(0));
	}

	/** The rows again, after the page's Back. */
	private void showMain()
	{
		if (page != null)
		{
			page = null;
			addRows();
		}
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

		openNow.addActionListener(e -> actions.openNow());

		resultWell.add(result);
		resultWell.setVisible(false);

		// The wave's list and the modifiers held go under the button and its well, in the wave's row, as the designs have them.
		JPanel under = clear(new BorderLayout(0, 0));
		under.add(resultWell, BorderLayout.NORTH);
		under.add(cards.waveBody, BorderLayout.CENTER);

		Stone.Row row = new Stone.Row(waveLayout, 5, 5, 6, 6);
		row.add(top, BorderLayout.NORTH);
		row.add(openNow, BorderLayout.CENTER);
		row.add(under, BorderLayout.SOUTH);
		return row;
	}

	/**
	 * Minimus's pick: a row at the top, in the arena, lit gold while he offers his three, which it shows by their
	 * pictures over a button as wide as the row, Arena now's twin, that opens them on the site; greyed until he does.
	 */
	private JPanel pickRow()
	{
		JPanel text = clear(new BorderLayout(0, 1));
		text.add(new Stone.Shadowed("Modifier pick", bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(pickState, BorderLayout.CENTER);
		pickState.setForeground(Stone.TEXT);
		openPick.addActionListener(e -> actions.openRecommender());
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
		openBoard.addActionListener(e -> actions.openLeaderboard());
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
				actions.removeTimes();
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
			sendsRow.add(body, BorderLayout.CENTER);
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
	 * What the panel shows, on the Swing thread: whether the player is in the Colosseum, whether Arena now has a wave to
	 * open (`live`: one of waves 1 to 11 under way), whether the death recap shows, the wave for the slot (null for none),
	 * the wave row's title and state, Minimus's offers while he makes them, a note in place of the runs when there are
	 * none, and the runs, newest first. The runs' rows are built again only when they changed, so that a line under the
	 * mouse stays as it is.
	 */
	void showState(boolean inColosseum, boolean live, boolean recap, Integer wave, String title, String state, List<Offer> offerList,
		String note, List<RunGroup> runs)
	{
		boolean offers = !offerList.isEmpty();
		if (!Objects.equals(wave, shownWave) || shownRuns == null)
		{
			waveSlot.show(SUN, wave == null ? null : String.valueOf(wave));
			shownWave = wave;
		}
		waveTitle.setText(title);
		waveState.setText(state);
		// The buttons stay where they are, greyed while they have nothing to open, so that the panel does not jump and shows
		// what it is for; both go while the death recap shows, which is the panel's then (the user's choice, 2026-10-26).
		// Arena now is lit only while a wave it plans is under way, as the arena has nothing to open before a wave or between
		// two (the UX review of 2026-10-27, F1). The pick is greyed in the arena until Minimus offers, and goes out of it,
		// where there is no run to pick for.
		openNow.setEnabled(live);
		openNow.setToolTipText(live ? "Opens the arena as it is now on the site, and copies its link."
			: !inColosseum ? "Opens the arena on the site once you are in the Colosseum."
			: wave != null && wave > ColosseumTracker.LAST_WAVE ? "Sol Survivor plans waves 1 to 11, not Sol Heredit's."
			: "Opens the arena on the site once the wave starts. A wave you played opens at its start from the play button on its "
				+ "line under Past waves.");
		if (openNow.isVisible() == recap)
		{
			openNow.setVisible(!recap);
			fitWaveRow();
		}
		boolean pick = inColosseum && !recap;
		if (!offerList.equals(shownOffers) || pick != pickRow.isVisible() || shownRuns == null)
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
			pickRow.setVisible(pick);
			openPick.setEnabled(offers);
			openPick.setToolTipText(offers ? "Opens the site's modifier pick with Minimus's three, the modifiers you hold and the next wave filled "
				+ "in, and copies its link." : "Opens the site's modifier pick once Minimus offers his three, after a wave.");
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

	/** What the last button did, in the well under the wave's buttons. On the Swing thread. */
	void showResult(String message)
	{
		result.setText(wrap(message, 196));
		resultWell.setVisible(!message.isEmpty());
		fitWaveRow();
	}

	/** The wave row's gaps, closed up while it holds nothing under its head. */
	private void fitWaveRow()
	{
		waveLayout.setVgap(openNow.isVisible() || resultWell.isVisible() ? 6 : 0);
		revalidate();
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
		copyRun.addActionListener(e -> actions.copyRun(newest.id));
		// While an upload is out every Watch button waits, under its own name: the well above says what is going on,
		// and a longer word would crowd the run's line once the scrollbar shows.
		Stone.Button watch = new Stone.Button("Watch", uploadOn() ? WATCH_ICON : WATCH_OFF_ICON, false, small(), 7, 6);
		watch.setEnabled(!replayBusy);
		watch.setToolTipText("Uploads this run's recording to the Sol Survivor site and opens it in your browser, to watch it back. "
			+ "Anyone with its link can view it for seven days. Needs Upload past runs, in the settings.");
		watch.addActionListener(e -> actions.openPastRun(newest.id));
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
		row.add(head, BorderLayout.NORTH);
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

	/** A wave's line: its name and how it went, and the buttons that open it at its start or its reinforcements and copy its log. */
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
		start.addActionListener(e -> actions.openPastStart(row.id));
		Stone.Button reinforcements = new Stone.Button(REINFORCEMENTS_ICON, 22);
		reinforcements.setToolTipText(row.reinforcements
			? "Reinforcements: this wave on the site on the tick its reinforcements came, and copy its link."
			: "Reinforcements: the plugin did not see this wave's reinforcements come.");
		reinforcements.setEnabled(row.reinforcements);
		reinforcements.addActionListener(e -> actions.openPastReinforcements(row.id));
		Stone.Button copy = new Stone.Button(COPY_ICON, 22);
		copy.setToolTipText("Copy wave log: this wave's log, for the site's wave reviewer.");
		copy.addActionListener(e -> actions.copyWaveLog(row.id));
		JPanel buttons = clear(new FlowLayout(FlowLayout.RIGHT, 3, 0));
		buttons.add(start);
		buttons.add(reinforcements);
		buttons.add(copy);

		JPanel line = clear(new BorderLayout(4, 0));
		line.add(text, BorderLayout.WEST);
		line.add(buttons, BorderLayout.EAST);
		line.setBorder(BorderFactory.createCompoundBorder(
			last ? new EmptyBorder(0, 0, 0, 0) : BorderFactory.createMatteBorder(0, 0, 1, 0, Stone.BEVEL_DARK),
			new EmptyBorder(3, 4, 3, 1)));
		line.setAlignmentX(Component.LEFT_ALIGNMENT);
		return line;
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
