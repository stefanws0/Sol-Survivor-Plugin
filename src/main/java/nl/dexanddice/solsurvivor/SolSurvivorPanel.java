package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Polygon;
import java.awt.RenderingHints;
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
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The side panel, in RuneLite's own style. A card at the top for the wave under way: the buttons that open the wave
 * start's link or the arena's now on the site, and the one that opens the site's modifier pick while Minimus offers his
 * three. Under it, the waves the log kept, grouped by run, newest first, each run a box that folds open and shut like
 * the loot tracker's: a row per wave with small buttons that open it on the site at its start or on the tick its
 * reinforcements came, and copy its log, and a button on the run's head that copies the whole run. At the foot, a line
 * that says what the last button did. The orange buttons open the site and the grey ones only copy. Like the rest of
 * the plugin it shows nothing about how to play the wave; the plans are the site's.
 */
class SolSurvivorPanel extends PluginPanel
{
	private boolean replayBusy;

	/** Runs can be rebuilt on a game tick; their Open buttons keep the upload state. Called on the Swing thread. */
	void showReplayBusy(boolean busy)
	{
		replayBusy = busy;
		buildRuns();
	}

	/** What the panel's buttons do, which the plugin carries out on the client thread. */
	interface Actions
	{
		void openWaveStart();

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
	}

	/** A logged wave, as its row shows it. */
	static final class WaveRow
	{
		private final int id;
		private final int wave;
		private final String detail;
		private final boolean start;
		private final boolean reinforcements;

		/** `start` and `reinforcements`: whether the log kept the wave's state on its first tick and when they came. */
		WaveRow(int id, int wave, String detail, boolean start, boolean reinforcements)
		{
			this.id = id;
			this.wave = wave;
			this.detail = detail;
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
			return r.id == id && r.wave == wave && r.detail.equals(detail) && r.start == start && r.reinforcements == reinforcements;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id, wave, detail, start, reinforcements);
		}
	}

	/** A run's box: its key, the id of its first wave's log, which stays as the run grows, its title and its waves, newest first. */
	static final class RunGroup
	{
		private final int key;
		private final String title;
		private final List<WaveRow> waves;

		RunGroup(int key, String title, List<WaveRow> waves)
		{
			this.key = key;
			this.title = title;
			this.waves = waves;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof RunGroup && ((RunGroup) o).key == key && ((RunGroup) o).title.equals(title) && ((RunGroup) o).waves.equals(waves);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(key, title, waves);
		}
	}

	private static final int ICON = 12;
	private static final ImageIcon START_ICON = icon(Shape.START, ColorScheme.DARK_GRAY_COLOR);
	private static final ImageIcon REINFORCEMENTS_ICON = icon(Shape.REINFORCEMENTS, ColorScheme.DARK_GRAY_COLOR);
	private static final ImageIcon COPY_ICON = icon(Shape.COPY, ColorScheme.LIGHT_GRAY_COLOR);
	private static final ImageIcon OPEN_ICON = icon(Shape.OPEN, ColorScheme.LIGHT_GRAY_COLOR);
	private static final ImageIcon SHUT_ICON = icon(Shape.SHUT, ColorScheme.LIGHT_GRAY_COLOR);

	private final Actions actions;
	private final JLabel waveTitle = new JLabel();
	private final JLabel waveState = new JLabel();
	private final JButton openWaveStart = new JButton("Open wave start");
	private final JButton openNow = new JButton("Open arena now");
	private final JButton openRecommender = new JButton("Open in recommender");
	private final JPanel runsBox = new JPanel();
	private final JLabel result = new JLabel();
	/** Which runs the player folded open or shut, by key; a run not here is open when it is the newest. */
	private final Map<Integer, Boolean> folded = new HashMap<>();
	private List<RunGroup> shownRuns;
	private String shownNote;

	SolSurvivorPanel(Actions actions)
	{
		this.actions = actions;

		JLabel title = new JLabel("Sol Survivor");
		title.setForeground(Color.WHITE);
		title.setFont(FontManager.getRunescapeBoldFont());
		add(title);

		add(currentCard());

		JLabel past = new JLabel("Past waves");
		past.setForeground(Color.WHITE);
		past.setFont(FontManager.getRunescapeBoldFont());
		past.setBorder(new EmptyBorder(6, 0, 0, 0));
		add(past);

		runsBox.setLayout(new BoxLayout(runsBox, BoxLayout.Y_AXIS));
		runsBox.setOpaque(false);
		add(runsBox);

		result.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		result.setFont(FontManager.getRunescapeSmallFont());
		add(result);

		JLabel help = new JLabel(wrap("The orange buttons open the Sol Survivor site; the grey ones copy a wave log to paste "
			+ "into it. Open past run uploads the selected run's tiles, hitpoints, prayers, hits and recorded gear to the configured site. "
			+ "Anyone with its link can view it for seven days. Nothing uploads automatically. This plugin draws nothing in the game."));
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		help.setFont(FontManager.getRunescapeSmallFont());
		add(help);

		showState(false, "Not in the Fortis Colosseum.", "", false, false, "", new ArrayList<>());
	}

	/** The card for the wave under way: its title, a line on its state, and the three buttons that open the site. */
	private JPanel currentCard()
	{
		JPanel card = new JPanel(new BorderLayout(0, 4));
		card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		card.setBorder(new EmptyBorder(8, 8, 8, 8));

		JPanel head = new JPanel(new GridLayout(2, 1));
		head.setOpaque(false);
		waveTitle.setForeground(Color.WHITE);
		waveTitle.setFont(FontManager.getRunescapeBoldFont());
		waveState.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		waveState.setFont(FontManager.getRunescapeSmallFont());
		head.add(waveTitle);
		head.add(waveState);
		card.add(head, BorderLayout.NORTH);

		JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
		buttons.setOpaque(false);
		openWaveStart.setToolTipText("Opens the arena on the wave's first tick on the site, and copies its link.");
		openNow.setToolTipText("Opens the arena as it is now on the site, and copies its link.");
		openRecommender.setToolTipText("While Minimus offers his three modifiers: opens the site's modifier pick with them filled in.");
		openWaveStart.addActionListener(e -> actions.openWaveStart());
		openNow.addActionListener(e -> actions.openNow());
		openRecommender.addActionListener(e -> actions.openRecommender());
		for (JButton b : new JButton[]{openWaveStart, openNow, openRecommender})
		{
			opensSite(b);
			buttons.add(b);
		}
		card.add(buttons, BorderLayout.CENTER);
		return card;
	}

	/**
	 * What the panel shows, on the Swing thread: the wave card's title and line, whether the wave start was taken and
	 * Minimus is offering modifiers, a note in place of the runs when there are none, and the runs, newest first. The
	 * runs' boxes are built again only when they changed, so that a row under the mouse stays as it is.
	 */
	void showState(boolean inColosseum, String title, String state, boolean waveStart, boolean offers, String note, List<RunGroup> runs)
	{
		waveTitle.setText(title);
		waveState.setText(state);
		waveState.setVisible(!state.isEmpty());
		openWaveStart.setEnabled(waveStart);
		openNow.setEnabled(inColosseum);
		openRecommender.setEnabled(offers);
		if (runs.equals(shownRuns) && note.equals(shownNote))
		{
			return;
		}
		shownRuns = new ArrayList<>(runs);
		shownNote = note;
		buildRuns();
	}

	/** What the last button did. On the Swing thread. */
	void showResult(String message)
	{
		result.setText(wrap(message));
	}

	private void buildRuns()
	{
		runsBox.removeAll();
		if (shownRuns.isEmpty())
		{
			JLabel empty = new JLabel(wrap(shownNote));
			empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
			empty.setFont(FontManager.getRunescapeSmallFont());
			empty.setAlignmentX(Component.LEFT_ALIGNMENT);
			runsBox.add(empty);
		}
		for (int i = 0; i < shownRuns.size(); i++)
		{
			RunGroup run = shownRuns.get(i);
			boolean open = folded.getOrDefault(run.key, i == 0);
			runsBox.add(runHead(run, open));
			if (open)
			{
				for (WaveRow row : run.waves)
				{
					runsBox.add(waveRow(row));
				}
			}
			runsBox.add(gap(6));
		}
		runsBox.revalidate();
		runsBox.repaint();
	}

	/** A run's head: a click on it folds the run open or shut, and its button copies the whole run. */
	private JPanel runHead(RunGroup run, boolean open)
	{
		JPanel head = new JPanel(new BorderLayout());
		head.setBackground(ColorScheme.DARKER_GRAY_COLOR);
		head.setBorder(new EmptyBorder(5, 6, 5, 4));
		head.setAlignmentX(Component.LEFT_ALIGNMENT);
		head.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		JLabel name = new JLabel(run.title, open ? OPEN_ICON : SHUT_ICON, JLabel.LEFT);
		name.setIconTextGap(6);
		name.setForeground(Color.WHITE);
		name.setFont(FontManager.getRunescapeSmallFont());
		head.add(name, BorderLayout.CENTER);

		WaveRow newest = run.waves.get(0);
		JButton copyRun = smallButton(COPY_ICON, false, "Copy whole run: every wave of this run in one paste, for the site.");
		copyRun.addActionListener(e -> actions.copyRun(newest.id));
		head.add(copyRun, BorderLayout.EAST);

		JButton replay = new JButton(replayBusy ? "Opening run…" : "Open past run");
		opensSite(replay);
		replay.setEnabled(!replayBusy);
		replay.setToolTipText("Uploads this run's recording to the configured site and opens it in your browser. Anyone with its link can view it for seven days.");
		replay.addActionListener(e -> actions.openPastRun(newest.id));
		head.add(replay, BorderLayout.SOUTH);

		head.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent e)
			{
				folded.put(run.key, !open);
				buildRuns();
			}
		});
		hover(head, ColorScheme.DARKER_GRAY_COLOR);
		return head;
	}

	/** A wave's row: its name and how it went, and the buttons that open it at its start or its reinforcements and copy its log. */
	private JPanel waveRow(WaveRow row)
	{
		JPanel panel = new JPanel(new BorderLayout(4, 0));
		Color background = ColorScheme.DARK_GRAY_COLOR;
		panel.setBackground(background);
		panel.setBorder(BorderFactory.createCompoundBorder(
			BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.DARKER_GRAY_COLOR),
			new EmptyBorder(4, 8, 4, 4)));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JPanel text = new JPanel(new GridLayout(2, 1));
		text.setOpaque(false);
		JLabel name = new JLabel("Wave " + row.wave);
		name.setForeground(Color.WHITE);
		name.setFont(FontManager.getRunescapeSmallFont());
		JLabel detail = new JLabel(row.detail);
		detail.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		detail.setFont(FontManager.getRunescapeSmallFont());
		text.add(name);
		text.add(detail);
		panel.add(text, BorderLayout.CENTER);

		JPanel buttons = new JPanel(new GridLayout(1, 3, 3, 0));
		buttons.setOpaque(false);
		JButton start = smallButton(START_ICON, true, row.start
			? "Open wave start: this wave on the site at its first tick, and copy its link."
			: "Open wave start: the plugin did not see this wave start.");
		start.setEnabled(row.start);
		start.addActionListener(e -> actions.openPastStart(row.id));
		JButton reinforcements = smallButton(REINFORCEMENTS_ICON, true, row.reinforcements
			? "Open at reinforcements: this wave on the site on the tick its reinforcements came, and copy its link."
			: "Open at reinforcements: the plugin did not see this wave's reinforcements come.");
		reinforcements.setEnabled(row.reinforcements);
		reinforcements.addActionListener(e -> actions.openPastReinforcements(row.id));
		JButton copy = smallButton(COPY_ICON, false, "Copy wave log: this wave's log, for the site's wave reviewer.");
		copy.addActionListener(e -> actions.copyWaveLog(row.id));
		buttons.add(start);
		buttons.add(reinforcements);
		buttons.add(copy);
		panel.add(buttons, BorderLayout.EAST);

		hover(panel, background);
		return panel;
	}

	/** Lights a row up while the mouse is over it, its buttons included, as RuneLite's own lists do. */
	private static void hover(JPanel panel, Color background)
	{
		MouseAdapter light = new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				panel.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				// Moving onto one of the row's buttons leaves the row's own area but not the row.
				if (!panel.contains(SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), panel)))
				{
					panel.setBackground(background);
				}
			}
		};
		panel.addMouseListener(light);
		for (Component c : descendants(panel))
		{
			c.addMouseListener(light);
		}
	}

	private static List<Component> descendants(JPanel panel)
	{
		List<Component> out = new ArrayList<>();
		for (Component c : panel.getComponents())
		{
			out.add(c);
			if (c instanceof JPanel)
			{
				out.addAll(descendants((JPanel) c));
			}
		}
		return out;
	}

	/** A small square button with an icon: orange when it opens the site, the panel's grey when it only copies. */
	private static JButton smallButton(ImageIcon icon, boolean site, String tip)
	{
		JButton b = new JButton(icon);
		b.setToolTipText(tip);
		b.setPreferredSize(new Dimension(22, 22));
		b.setFocusPainted(false);
		b.setBorder(new EmptyBorder(0, 0, 0, 0));
		b.setBackground(site ? ColorScheme.BRAND_ORANGE : ColorScheme.MEDIUM_GRAY_COLOR);
		b.setOpaque(true);
		return b;
	}

	private static JPanel gap(int height)
	{
		JPanel gap = new JPanel();
		gap.setOpaque(false);
		gap.setAlignmentX(Component.LEFT_ALIGNMENT);
		gap.setPreferredSize(new Dimension(0, height));
		gap.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		return gap;
	}

	/** A button that opens the site: the brand's orange with dark text, apart from the panel's grey ones that only copy. */
	private static void opensSite(JButton button)
	{
		button.setBackground(ColorScheme.BRAND_ORANGE);
		button.setForeground(ColorScheme.DARK_GRAY_COLOR);
		button.setOpaque(true);
	}

	/** HTML that wraps at the panel's width, which a plain label does not. */
	private static String wrap(String text)
	{
		String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<html><body style='width: " + (PANEL_WIDTH - 40) + "px'>" + escaped + "</body></html>";
	}

	/** The shapes of the panel's small icons, drawn here so that the plugin carries no image files. */
	private enum Shape
	{
		/** A play mark: open the wave at its start. */
		START,
		/** Two figures, one behind the other: open the wave where its reinforcements came. */
		REINFORCEMENTS,
		/** Two sheets, one over the other: copy. */
		COPY,
		/** A run folded open, and shut. */
		OPEN,
		SHUT
	}

	private static ImageIcon icon(Shape shape, Color color)
	{
		BufferedImage image = new BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(color);
		switch (shape)
		{
			case START:
				g.fillPolygon(new Polygon(new int[]{3, 10, 3}, new int[]{1, 6, 11}, 3));
				break;
			case REINFORCEMENTS:
				g.fillOval(1, 1, 4, 4);
				g.fillRoundRect(0, 6, 6, 6, 3, 3);
				g.fillOval(7, 0, 4, 4);
				g.fillRoundRect(6, 5, 6, 7, 3, 3);
				break;
			case COPY:
				g.setStroke(new BasicStroke(1.2f));
				g.drawRect(1, 3, 7, 8);
				g.drawRect(4, 0, 7, 8);
				break;
			case OPEN:
				g.fillPolygon(new Polygon(new int[]{1, 11, 6}, new int[]{3, 3, 9}, 3));
				break;
			default:
				g.fillPolygon(new Polygon(new int[]{3, 9, 3}, new int[]{1, 6, 11}, 3));
				break;
		}
		g.dispose();
		return new ImageIcon(image);
	}

	/** The toolbar icon, a small sun, drawn here so that the plugin carries no image file. */
	static BufferedImage icon()
	{
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(ColorScheme.BRAND_ORANGE);
		g.fillOval(4, 4, 8, 8);
		g.setStroke(new BasicStroke(1.5f));
		for (int i = 0; i < 8; i++)
		{
			double a = Math.PI * i / 4;
			g.drawLine((int) Math.round(8 + 5.5 * Math.cos(a)), (int) Math.round(8 + 5.5 * Math.sin(a)),
				(int) Math.round(8 + 7.5 * Math.cos(a)), (int) Math.round(8 + 7.5 * Math.sin(a)));
		}
		g.dispose();
		return image;
	}
}
