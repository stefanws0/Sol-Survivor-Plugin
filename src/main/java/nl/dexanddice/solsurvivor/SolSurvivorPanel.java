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
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

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
 * the tooltip of the question mark at the top. Like the rest of the plugin it shows nothing about how to play the
 * wave; the plans are the site's.
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

	/** What the panel's buttons do, which the plugin carries out on the client thread. */
	interface Actions
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

		/**
		 * `detail`: how the wave went, or that it is under way, which `underWay` marks in gold. `start` and
		 * `reinforcements`: whether the log kept the wave's state on its first tick and when they came.
		 */
		WaveRow(int id, int wave, String detail, boolean underWay, boolean start, boolean reinforcements)
		{
			this.id = id;
			this.wave = wave;
			this.detail = detail;
			this.underWay = underWay;
			this.start = start;
			this.reinforcements = reinforcements;
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
				&& r.reinforcements == reinforcements;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id, wave, detail, underWay, start, reinforcements);
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

	/** The help, in the tooltip of the question mark beside the panel's title. */
	private static final String HELP = "Arena now opens the wave on the Sol Survivor site as it is now, and copies its link. "
		+ "Modifier pick opens Minimus's three offers there while he makes them. Under Past waves, a wave's buttons open it at "
		+ "its start, the wave under way too, or on the tick its reinforcements came, and copy its log to paste into the site. "
		+ "With Upload past runs on in the settings, Watch uploads that run's tiles, hitpoints, prayers, hits and recorded gear "
		+ "to the site, where anyone with its link can view it for seven days. With Share wave logs on, each wave's log goes to "
		+ "the site, without your levels, as the wave ends, and with Leaderboard on too, each wave you finish goes on "
		+ "the site's leaderboard under your RuneScape name or the name you set. All are off until you turn them on. This plugin draws nothing in the game.";

	private static final ImageIcon SUN = Stone.sun(24);
	/** Yellow, as the text of the primary button it sits on. */
	private static final ImageIcon ARENA = Stone.arena(12, Stone.YELLOW);
	private static final ImageIcon CARDS_ON = Stone.cards(24, Stone.GOLD);
	private static final ImageIcon CARDS_OFF = Stone.cards(24, Stone.DIM);
	private static final ImageIcon START_ICON = Stone.play(10, Stone.ORANGE);
	private static final ImageIcon REINFORCEMENTS_ICON = Stone.figures(12, Stone.ORANGE);
	private static final ImageIcon COPY_ICON = Stone.copy(11, Stone.MUTED);
	private static final ImageIcon COPY_RUN_ICON = Stone.copy(12, Stone.MUTED);
	private static final ImageIcon WATCH_ICON = Stone.replay(12, Stone.ORANGE);
	private static final ImageIcon OPEN_ICON = Stone.caret(9, Stone.MUTED, true);
	private static final ImageIcon SHUT_ICON = Stone.caret(9, Stone.MUTED, false);

	private final Actions actions;
	private final Stone.Slot waveSlot = new Stone.Slot(34, SUN);
	private final JLabel waveTitle = new Stone.Shadowed("", bold(), Stone.ORANGE);
	private final JLabel waveState = label("", small(), Stone.TEXT);
	private final Stone.Button openNow = new Stone.Button("Arena now", ARENA, true, small(), 7, 6);
	private final Stone.Well resultWell = new Stone.Well(new BorderLayout(), 4, 5, 4, 5);
	private final JLabel result = label("", small(), Stone.TEXT);
	private final Stone.Row pickRow = new Stone.Row(new BorderLayout(8, 0), 5, 5, 5, 6);
	private final Stone.Slot pickSlot = new Stone.Slot(34, CARDS_OFF);
	private final JLabel pickState = label("", small(), Stone.MUTED);
	private final Stone.Button openRecommender = new Stone.Button("Open", null, true, small(), 7, 6);
	/** The leaderboard's row, while Leaderboard is on or the player has times to remove (showBoard). */
	private final Stone.Row boardRow = new Stone.Row(new BorderLayout(0, 5), 5, 6, 6, 6);
	private final JLabel boardState = label("", small(), Stone.TEXT);
	private final Stone.Button openBoard = new Stone.Button("Open", null, false, small(), 7, 6);
	private final Stone.Button removeTimes = new Stone.Button("Remove my times", null, false, small(), 7, 6);
	/** Beside Past waves, while "Share wave logs" is on: each wave's log goes to the site as it ends. */
	private final JLabel sharing = label("Sharing logs", small(), Stone.GOOD);
	private final JPanel runsBox = new JPanel();
	private final JLabel foot = label("", small(), Stone.DIM);
	/** Which runs the player folded open or shut, by key; a run not here is open when it is the newest. */
	private final Map<Integer, Boolean> folded = new HashMap<>();
	private List<RunGroup> shownRuns;
	private String shownNote;
	private Integer shownWave;
	private boolean shownOffers;

	SolSurvivorPanel(Actions actions)
	{
		this.actions = actions;
		setBackground(Stone.BACKGROUND);
		getParent().setBackground(Stone.BACKGROUND);
		getScrollPane().getViewport().setBackground(Stone.BACKGROUND);
		getWrappedPanel().setBackground(Stone.BACKGROUND);

		add(header());
		add(waveRow());
		add(pickRow());
		add(boardRow());
		add(pastHeading());
		runsBox.setLayout(new BoxLayout(runsBox, BoxLayout.Y_AXIS));
		runsBox.setOpaque(false);
		add(runsBox);
		foot.setBorder(new EmptyBorder(2, 1, 0, 0));
		add(foot);

		showSharing(false);
		showBoard(null, false, true);
		showState(false, null, "Not in the Colosseum", "Enter it to open a wave.", false, "", new ArrayList<>());
	}

	private static Font bold()
	{
		return FontManager.getRunescapeBoldFont();
	}

	private static Font small()
	{
		return FontManager.getRunescapeSmallFont();
	}

	private static JLabel label(String text, Font font, Color color)
	{
		JLabel l = new JLabel(text);
		l.setFont(font);
		l.setForeground(color);
		return l;
	}

	private static JPanel clear(java.awt.LayoutManager layout)
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
		help.setToolTipText(wrap(HELP, 260));
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

		openNow.setToolTipText("Opens the arena as it is now on the site, and copies its link.");
		openNow.addActionListener(e -> actions.openNow());

		resultWell.add(result);
		resultWell.setVisible(false);

		Stone.Row row = new Stone.Row(new BorderLayout(0, 6), 5, 5, 6, 6);
		row.add(top, BorderLayout.NORTH);
		row.add(openNow, BorderLayout.CENTER);
		row.add(resultWell, BorderLayout.SOUTH);
		return row;
	}

	/** Minimus's pick: its row stays, so that the player knows of it, and lights up gold while he offers his three. */
	private JPanel pickRow()
	{
		JPanel text = clear(new BorderLayout(0, 1));
		text.add(new Stone.Shadowed("Modifier pick", bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(pickState, BorderLayout.CENTER);
		openRecommender.setToolTipText("While Minimus offers his three modifiers: opens the site's modifier pick with them filled in.");
		openRecommender.addActionListener(e -> actions.openRecommender());
		JPanel east = clear(new BorderLayout());
		east.add(openRecommender, BorderLayout.NORTH);
		east.setBorder(new EmptyBorder(6, 0, 0, 0));
		pickRow.add(pickSlot, BorderLayout.WEST);
		pickRow.add(text, BorderLayout.CENTER);
		pickRow.add(east, BorderLayout.EAST);
		return pickRow;
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

	private JPanel pastHeading()
	{
		sharing.setIcon(Stone.dot(9, Stone.GOOD));
		sharing.setIconTextGap(2);
		sharing.setToolTipText("Share wave logs is on: each wave's log goes to the site as the wave ends.");
		JPanel h = clear(new BorderLayout(4, 0));
		h.add(new Stone.Shadowed("Past waves", bold(), Stone.ORANGE), BorderLayout.CENTER);
		h.add(sharing, BorderLayout.EAST);
		h.setBorder(new EmptyBorder(6, 1, 0, 1));
		return h;
	}

	/**
	 * What the panel shows, on the Swing thread: whether the player is in the Colosseum, the wave for the slot (null for
	 * none), the wave row's title and state, whether Minimus is offering modifiers, a note
	 * in place of the runs when there are none, and the runs, newest first. The runs' rows are built again only when
	 * they changed, so that a line under the mouse stays as it is.
	 */
	void showState(boolean inColosseum, Integer wave, String title, String state, boolean offers, String note,
		List<RunGroup> runs)
	{
		if (!Objects.equals(wave, shownWave) || shownRuns == null)
		{
			waveSlot.show(SUN, wave == null ? null : String.valueOf(wave));
			shownWave = wave;
		}
		waveTitle.setText(title);
		waveState.setText(state);
		openNow.setEnabled(inColosseum);
		if (offers != shownOffers || shownRuns == null)
		{
			pickSlot.show(offers ? CARDS_ON : CARDS_OFF, null);
			pickState.setForeground(offers ? Stone.TEXT : Stone.MUTED);
			pickState.setText(offers && wave != null
				? wrapHtml("Minimus offers three for wave <font color='#ffff00'>" + wave + "</font>", 112)
				: offers ? wrap("Minimus offers three.", 112) : wrap("Lights up when Minimus offers.", 112));
			pickRow.setGlow(offers ? Stone.GOLD : null);
			openRecommender.setEnabled(offers);
			shownOffers = offers;
		}
		if (runs.equals(shownRuns) && note.equals(shownNote))
		{
			return;
		}
		shownRuns = new ArrayList<>(runs);
		shownNote = note;
		buildRuns();
	}

	/** Whether the waves' logs are shared, as "Share wave logs" and "Record wave log" are set. On the Swing thread. */
	void showSharing(boolean on)
	{
		sharing.setVisible(on);
		foot.setText(wrap(on
			? "Draws nothing in the game. Each wave's log goes to the site as it ends (Share wave logs)."
			: "Draws nothing in the game, and sends nothing until you switch on Upload past runs or Share wave logs.", 215));
	}

	/** What the last button did, in the well under the wave's buttons. On the Swing thread. */
	void showResult(String message)
	{
		result.setText(wrap(message, 196));
		resultWell.setVisible(!message.isEmpty());
		revalidate();
	}

	private void buildRuns()
	{
		runsBox.removeAll();
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
		Stone.Button watch = new Stone.Button("Watch", WATCH_ICON, false, small(), 7, 6);
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
		JPanel text = clear(new BorderLayout());
		text.add(label("Wave " + row.wave, small(), Stone.TEXT), BorderLayout.NORTH);
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

	private static JComponent gap(int height)
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
	private static String wrap(String text, int width)
	{
		return wrapHtml(text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"), width);
	}

	private static String wrapHtml(String html, int width)
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
