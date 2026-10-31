package nl.dexanddice.solsurvivor;

import static nl.dexanddice.solsurvivor.SolSurvivorPanel.bold;
import static nl.dexanddice.solsurvivor.SolSurvivorPanel.clear;
import static nl.dexanddice.solsurvivor.SolSurvivorPanel.label;
import static nl.dexanddice.solsurvivor.SolSurvivorPanel.small;
import static nl.dexanddice.solsurvivor.SolSurvivorPanel.wrap;
import static nl.dexanddice.solsurvivor.SolSurvivorPanel.wrapHtml;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import nl.dexanddice.solsurvivor.core.Modifier;
import nl.dexanddice.solsurvivor.core.WaveReview;
import nl.dexanddice.solsurvivor.core.WaveRoster;

/**
 * The panel's cards for the wave, as the plugin feature designs of 2026-10-26 draw them (the "Plugin Feature Designs"
 * canvas, which the user approved): the wave's list inside the wave's row, its NPCs by their faces, struck out as they
 * die, or what the next wave brings, and the modifiers held by their pictures; the wave report, the mistake tracker's,
 * once a wave ends, short in the panel and in full on a page of its own; and the death recap, once the player has died.
 * During a wave they show only what is in the arena: the report waits for the wave's end, and nothing here says how to
 * play it. Built on the Swing thread.
 *
 * After a death the recap is a page of its own, the death report, which opens by itself as the player comes out of the
 * arena and goes Home to the panel, where a line opens it again (the user's ask of 2026-10-28: a player who has just died
 * was met by the recap, the report and the rest at once). Out of the arena the report is a line too, as the player saw
 * it between waves; its page holds the whole of it.
 */
final class WaveCards
{
	/** The colours of the prayer bar's four parts, the designs': on time, a tick off, the wrong prayer, none up. */
	private static final Color ON_TIME = new Color(0x2f8f2f);
	private static final Color A_TICK_OFF = new Color(0xffb000);
	private static final Color WRONG = new Color(0xe8452e);
	private static final Color NONE_UP = Stone.Pill.HIT;
	/** The run chart's and the recap's hitpoints: a clean wave or plenty of hitpoints, a few mistakes or half, many or few. */
	private static final Color FINE = Stone.GOOD;
	private static final Color FAIR = A_TICK_OFF;
	private static final Color POOR = WRONG;
	/** A dead NPC's face and a reinforcement's that is still to come, faded. */
	private static final float DEAD_ALPHA = 0.45f;
	private static final float TO_COME_ALPHA = 0.6f;
	/** How many mistakes the report shows in the panel; the rest are on its page. */
	private static final int SHORT_LIST = 5;

	private static final ImageIcon PLAY_ICON = Stone.play(8, Stone.ORANGE);
	/** A mistake's play button and Watch's icons while Upload past runs is off, when every Watch is secondary. */
	private static final ImageIcon PLAY_OFF_ICON = Stone.play(8, Stone.MUTED);
	/** Yellow, as the text of the primary button it sits on, as Arena now's icon. */
	private static final ImageIcon WATCH_ICON = Stone.replay(12, Stone.YELLOW);
	private static final ImageIcon WATCH_OFF_ICON = Stone.replay(12, Stone.MUTED);
	/** What every Watch needs, under the buttons while it is off. */
	private static final String UPLOAD_OFF = "Watch needs Upload past runs, in the settings.";
	/** The log book in the report's slot, as the site marks a wave's log with it (design round two's item 7). */
	private static final Icon REPORT_ICON = Icons.of("log-book", 24);
	private static final Icon SKULL = Icons.of("skull", 20);

	/** The wave report as the panel shows it, with the settings applied: the mistakes the player counts, and their run's. */
	static final class Report
	{
		final int id;
		final int wave;
		final String time;
		final int onTime;
		final int tickOff;
		final int wrong;
		final int none;
		/** The hitpoints the wave cost, every hitsplat, and the ticks of the idle stretches long enough to count. */
		final int damage;
		final int idleTicks;
		final List<Row> mistakes;
		/** The kinds the settings leave out, in a sentence; null when every kind counts. */
		final String notCounted;
		/** The run's waves so far and each one's mistakes, oldest first, for the chart. */
		final int[] runWaves;
		final int[] runMistakes;
		/** What the run's mistakes are mostly, in a sentence; null when there are too few to say. */
		final String common;
		/** The wave's last tick, the length of the report's strip of the wave. */
		final int lastTick;

		Report(int id, int wave, String time, int onTime, int tickOff, int wrong, int none, int damage, int idleTicks, List<Row> mistakes,
			String notCounted, int[] runWaves, int[] runMistakes, String common)
		{
			this(id, wave, time, onTime, tickOff, wrong, none, damage, idleTicks, mistakes, notCounted, runWaves, runMistakes, common,
				mistakes.stream().mapToInt(m -> m.tick).max().orElse(0));
		}

		Report(int id, int wave, String time, int onTime, int tickOff, int wrong, int none, int damage, int idleTicks, List<Row> mistakes,
			String notCounted, int[] runWaves, int[] runMistakes, String common, int lastTick)
		{
			this.lastTick = lastTick;
			this.id = id;
			this.wave = wave;
			this.time = time;
			this.onTime = onTime;
			this.tickOff = tickOff;
			this.wrong = wrong;
			this.none = none;
			this.damage = damage;
			this.idleTicks = idleTicks;
			this.mistakes = mistakes;
			this.notCounted = notCounted;
			this.runWaves = runWaves;
			this.runMistakes = runMistakes;
			this.common = common;
		}

		/**
		 * The report of the logged wave `id`, wave `wave`, `time` long, from its review, as the settings ask: the mistakes
		 * of the kinds counted, idle stretches from `idleFrom` ticks, and the kinds left out; and its run's waves up to and
		 * with this one that have a review, `runWaves` and `runReviews` in the same order, for the chart.
		 */
		static Report of(int id, int wave, String time, WaveReview r, Set<WaveReview.Kind> kinds, int idleFrom, List<Integer> runWaves,
			List<WaveReview> runReviews)
		{
			List<Row> rows = new ArrayList<>();
			int idle = 0;
			for (WaveReview.Mistake m : r.getMistakes())
			{
				if (m.getKind() == WaveReview.Kind.IDLE && m.getTicks() >= idleFrom)
				{
					idle += m.getTicks();
				}
			}
			for (WaveReview.Mistake m : r.counted(kinds, idleFrom))
			{
				String badge = m.getKind() == WaveReview.Kind.IDLE ? m.getTicks() + "t" : m.getDamage() == null ? "\u2013" : String.valueOf(m.getDamage());
				rows.add(new Row(m.getTick(), m.getText(), badge, m.getIcon(), m.getDamage() != null && m.getDamage() > 0, m.getSummary(), m.getGroup(),
					m.getStyle()));
			}
			List<String> off = new ArrayList<>();
			for (WaveReview.Kind kind : WaveReview.Kind.values())
			{
				if (!kinds.contains(kind))
				{
					off.add(WaveReview.kindName(kind));
				}
			}
			String notCounted = off.isEmpty() ? null : "Not counted, as your settings ask: " + String.join(", ", off) + ".";
			int[] waves = new int[runWaves.size()];
			int[] counts = new int[runWaves.size()];
			for (int i = 0; i < waves.length; i++)
			{
				waves[i] = runWaves.get(i);
				counts[i] = runReviews.get(i).counted(kinds, idleFrom).size();
			}
			return new Report(id, wave, time, r.count(WaveReview.Prayed.ON_TIME), r.count(WaveReview.Prayed.LATE) + r.count(WaveReview.Prayed.EARLY),
				r.count(WaveReview.Prayed.WRONG), r.count(WaveReview.Prayed.NONE), r.getDamage(), idle, rows, notCounted, waves, counts,
				WaveReview.mostly(runReviews, kinds, idleFrom), r.getLast());
		}

		int attacks()
		{
			return onTime + tickOff + wrong + none;
		}

		private String key()
		{
			return id + "|" + wave + "|" + time + "|" + onTime + "," + tickOff + "," + wrong + "," + none + "|" + damage + "," + idleTicks + "|" + mistakes + "|" + notCounted + "|"
				+ Arrays.toString(runWaves) + Arrays.toString(runMistakes) + "|" + common + "|" + lastTick;
		}

		@Override
		public boolean equals(Object o)
		{
			return o instanceof Report && ((Report) o).key().equals(key());
		}

		@Override
		public int hashCode()
		{
			return key().hashCode();
		}
	}

	/**
	 * A mistake's line: its tick, what happened, its badge, the damage or an idle stretch's ticks, the picture of what did
	 * it (Icons' name, or null), and whether it cost hitpoints, which puts the badge on the hitsplat's red.
	 */
	static final class Row
	{
		final int tick;
		final String text;
		final String badge;
		final String icon;
		final boolean hit;
		final String summary;
		final String group;
		/** The attack's style, "magic", "ranged" or "melee", or null for a mistake that is no attack's (WaveReview.Mistake). */
		final String style;

		Row(int tick, String text, String badge, String icon, boolean hit)
		{
			this(tick, text, badge, icon, hit, text);
		}

		Row(int tick, String text, String badge, String icon, boolean hit, String summary)
		{
			this(tick, text, badge, icon, hit, summary, summary + "|" + icon);
		}

		Row(int tick, String text, String badge, String icon, boolean hit, String summary, String group)
		{
			this(tick, text, badge, icon, hit, summary, group, null);
		}

		Row(int tick, String text, String badge, String icon, boolean hit, String summary, String group, String style)
		{
			this.style = style;
			this.tick = tick;
			this.text = text;
			this.badge = badge;
			this.icon = icon;
			this.hit = hit;
			this.summary = summary;
			this.group = group;
		}

		@Override
		public String toString()
		{
			return tick + " " + text + " " + badge + " " + icon + " " + group + " " + style;
		}
	}

	/** A short report's group, keeping the full report's rows intact and summing only their recorded damage or idle ticks. */
	static final class Group
	{
		final String text;
		final String icon;
		int count;
		int first;
		int last;
		int damage;
		int idle;
		boolean hasDamage;

		Group(Row row)
		{
			text = row.summary;
			icon = row.icon;
			first = row.tick;
			last = row.tick;
		}

		String figures()
		{
			return "×" + count + " · " + (hasDamage ? damage + " damage" : idle + " idle ticks") + " · t" + first
				+ (last == first ? "" : "–t" + last);
		}
	}

	/** The short report groups the same kind and attacker in the order they first appeared; its page still shows every tick. */
	static List<Group> grouped(List<Row> rows)
	{
		Map<String, Group> groups = new LinkedHashMap<>();
		for (Row row : rows)
		{
			Group group = groups.computeIfAbsent(row.group + "|" + row.icon, key -> new Group(row));
			group.count++;
			group.first = Math.min(group.first, row.tick);
			group.last = Math.max(group.last, row.tick);
			if (row.badge.matches("[0-9]+"))
			{
				group.damage += Integer.parseInt(row.badge);
				group.hasDamage = true;
			}
			else if (row.badge.matches("[0-9]+t"))
			{
				group.idle += Integer.parseInt(row.badge.substring(0, row.badge.length() - 1));
			}
		}
		return new ArrayList<>(groups.values());
	}

	/**
	 * The death recap: the wave's log id and number, when the player died, the tick of the wave and how long into it in
	 * game time, such as "0:21", what killed them, the last ticks, and the most hitpoints the player had in the wave,
	 * which colours the hitpoints of each tick.
	 */
	static final class Recap
	{
		final int id;
		final int wave;
		/** When the player died, in full on the death report, such as "Tick 35 · 0:21 into the wave". */
		final String when;
		/** When, short for the line at home, such as "0:21 into the wave". */
		final String into;
		final String cause;
		final List<WaveReview.RecapLine> lines;
		final int maxHitpoints;

		Recap(int id, int wave, int tick, String time, String cause, List<WaveReview.RecapLine> lines, int maxHitpoints)
		{
			this.id = id;
			this.wave = wave;
			this.into = time + " into the wave";
			this.when = "Tick " + tick + " \u00b7 " + into;
			this.cause = cause;
			this.lines = lines;
			this.maxHitpoints = maxHitpoints;
		}

		@Override
		public boolean equals(Object o)
		{
			if (!(o instanceof Recap))
			{
				return false;
			}
			Recap r = (Recap) o;
			return r.id == id && r.when.equals(when) && Objects.equals(r.cause, cause) && r.lines.size() == lines.size()
				&& r.maxHitpoints == maxHitpoints;
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(id, when, cause, lines.size());
		}
	}

	/** What the cards' buttons do, which the plugin carries out. */
	interface Actions
	{
		/** Watch the run of the logged wave `id` on the site, at that wave and at `tick` of it, or at its start for null. */
		void watchAt(int id, Integer tick);

		/** Copy the logged wave `id`'s report, as text. */
		void copyReport(int id);
	}

	/**
	 * Where the pages show: the panel puts one in place of its rows, over the rows or another page, which its Back goes
	 * back to.
	 */
	interface Pages
	{
		void showPage(JComponent page);

		/** Takes `page` away wherever it is among the pages shown, showing what was under it. */
		void closePage(JComponent page);

		/** The panel's rows again, from every page. */
		void showMain();
	}

	private final Actions actions;
	private final Pages pages;
	/** The wave's list and the modifiers held, which the wave's row holds under its buttons. */
	final JPanel waveBody = new Stone.Group(null);
	/** The wave report under the wave's row: in short between waves, a line out of the arena; hidden after a death. */
	final Stone.Row reportCard = new Stone.Row(null, 5, 6, 6, 6);
	/** Out of the arena after a death, at the panel's top: a line that opens the death report again. */
	final Stone.Row deathRow = new Stone.Row(null, 5, 6, 6, 6);
	/** The report in full, on a page of its own in place of the panel's rows. */
	final JPanel reportPage = new Stone.Group(null);
	/** The death report: the recap, its buttons and a line to the wave's report, on a page of its own. */
	final JPanel deathPage = new Stone.Group(null);
	/**
	 * What the last button did, on each page under its Home or Back, as the panel's well is under the page: only what came
	 * while a page showed, so that a page opens with none.
	 */
	private final JLabel reportResult = label("", small(), Stone.TEXT);
	private final JPanel reportResultBox = resultBox(reportResult);
	private final JLabel deathResult = label("", small(), Stone.TEXT);
	private final JPanel deathResultBox = resultBox(deathResult);
	/**
	 * Whether Upload past runs is on, which every Watch needs: off, each goes secondary, with a line under it that says so
	 * before the press rather than after (the UX review of 2026-10-27, F2).
	 */
	private boolean upload;
	/** The report and the recap shown, to build again as the setting changes; null while none shows. */
	private Report report;
	private Recap recap;
	/** Whether the player is out of the arena, where the report is a line. */
	private boolean outside;
	/** The log id of the death whose report opened by itself, so that it opens once; null for none yet. */
	private Integer deathOpened;

	WaveCards(Actions actions, Pages pages)
	{
		this.actions = actions;
		this.pages = pages;
		for (JPanel card : new JPanel[] {waveBody, reportCard, deathRow, reportPage, deathPage})
		{
			card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
			card.setVisible(false);
		}
		// The pages show only once the panel puts them in its place.
		reportPage.setVisible(true);
		deathPage.setVisible(true);
	}

	/** A page's well for the last result, under its bar, with the gap above it; hidden until a result comes. */
	private static JPanel resultBox(JLabel words)
	{
		Stone.Well well = new Stone.Well(new BorderLayout(), 4, 5, 4, 5);
		well.add(words);
		JPanel box = new Stone.Group(new BorderLayout());
		box.add(well, BorderLayout.CENTER);
		box.setBorder(new EmptyBorder(6, 0, 0, 0));
		box.setVisible(false);
		return box;
	}

	/**
	 * The wave's list: during a wave its NPCs by their faces, each with how many are left, struck out and faded with a
	 * skull as the last dies, or left off with `markDead` off, and the reinforcements faded and still to come; between
	 * waves what the next brings and who joins it later. Under them the modifiers held, each by its picture and tier, and
	 * Doom against its limit. Hidden with nothing to show.
	 */
	void showWave(WaveView view, boolean list, boolean markDead, boolean next, boolean modifiers, boolean doom)
	{
		waveBody.removeAll();
		boolean any = false;
		if (view.underWay && list && !view.thisWave.isEmpty())
		{
			waveBody.add(section("This wave"));
			Stone.Well well = new Stone.Well(null, 2, 3, 2, 3);
			well.setLayout(new BoxLayout(well, BoxLayout.Y_AXIS));
			List<WaveRoster.Line> shown = new ArrayList<>();
			for (WaveRoster.Line line : view.thisWave)
			{
				if (!line.isDead() || markDead)
				{
					shown.add(line);
				}
			}
			for (int i = 0; i < shown.size(); i++)
			{
				well.add(npcLine(shown.get(i), i == shown.size() - 1));
			}
			waveBody.add(left(well));
			any = true;
		}
		else if (!view.underWay && next && view.next != null && !view.nextWave.isEmpty())
		{
			waveBody.add(section("Wave " + view.next + " brings"));
			Stone.Well well = new Stone.Well(null, 4, 5, 4, 5);
			well.setLayout(new BoxLayout(well, BoxLayout.Y_AXIS));
			JPanel chips = clear(new WrapLayout(10, 3, 190));
			for (WaveRoster.Line line : view.nextWave)
			{
				JLabel chip = label("<html><font color='#ffff00'>" + line.getCount() + "</font> " + escape(line.getShortName()) + "</html>", small(),
					Stone.TEXT);
				chip.setIcon(Icons.npc(line.getIcon(), 16, 1f));
				chip.setIconTextGap(4);
				chips.add(chip);
			}
			well.add(left(chips));
			if (view.nextThen != null)
			{
				well.add(SolSurvivorPanel.gap(3));
				well.add(left(label(wrap(view.nextThen, 186), small(), Stone.MUTED)));
			}
			waveBody.add(left(well));
			any = true;
		}
		if (modifiers && !view.held.isEmpty())
		{
			if (any)
			{
				waveBody.add(SolSurvivorPanel.gap(6));
			}
			waveBody.add(section("Modifiers held"));
			JPanel chips = clear(new WrapLayout(4, 4, 200));
			for (Map.Entry<Modifier, Integer> e : view.held.entrySet())
			{
				chips.add(new ModifierChip(e.getKey(), e.getValue()));
			}
			if (doom && view.underWay && view.doomLimit > 0 && view.held.containsKey(Modifier.DOOM))
			{
				// Doom's stacks this wave against the tier's limit, from the game's own count, red on the last.
				JLabel stacks = label("Doom " + view.doomStacks + " of " + view.doomLimit, small(), view.doomLast() ? Stone.BAD : Stone.GOLD);
				stacks.setToolTipText("Doom's stacks this wave, from the game's own count, against the hits its tier allows.");
				stacks.setBorder(new EmptyBorder(6, 2, 0, 0));
				chips.add(stacks);
			}
			waveBody.add(left(chips));
			any = true;
		}
		if (any)
		{
			waveBody.setBorder(new EmptyBorder(4, 0, 0, 0));
		}
		waveBody.setVisible(any);
		waveBody.revalidate();
		waveBody.repaint();
	}

	/** A line of the wave under way: the face, the NPC and a note, and how many are left, a skull, or "still to come". */
	private static JPanel npcLine(WaveRoster.Line line, boolean last)
	{
		boolean dead = line.isDead();
		boolean toCome = line.isToCome();
		JPanel row = clear(new BorderLayout(6, 0));
		JLabel face = new JLabel(Icons.npc(line.getIcon(), 16, dead ? DEAD_ALPHA : toCome ? TO_COME_ALPHA : 1f));
		face.setVerticalAlignment(JLabel.TOP);
		row.add(face, BorderLayout.WEST);
		String name = escape(line.getShortName());
		String html;
		if (dead)
		{
			html = "<s>" + name + "</s>";
		}
		else if (toCome)
		{
			html = name + ", the reinforcement";
		}
		else if (line.getNote() != null && !"warband".equals(line.getNote()) && !"Quartet".equals(line.getNote()))
		{
			html = name + " <font color='#8a7b62'>" + escape(line.getNote()) + "</font>";
		}
		else
		{
			html = name;
		}
		row.add(label(wrapHtml(html, toCome ? 100 : 128), small(), dead ? Stone.DIM : toCome ? Stone.MUTED : Stone.TEXT), BorderLayout.CENTER);
		JLabel right = dead ? new JLabel(Icons.of("skull", 12, 0.8f)) : label(toCome ? "to come" : line.getCount() + " left", small(),
			toCome ? Stone.DIM : Stone.YELLOW);
		right.setVerticalAlignment(JLabel.TOP);
		row.add(right, BorderLayout.EAST);
		row.setBorder(lined(last, new EmptyBorder(3, 3, 3, 3)));
		return left(row);
	}

	/** A modifier held: its picture in a slot, its tier in the corner, its name in the tooltip. */
	private static final class ModifierChip extends JComponent
	{
		private static final int SIZE = 26;
		private final javax.swing.Icon icon;
		private final String tier;

		ModifierChip(Modifier modifier, int tier)
		{
			this.icon = Icons.of(Icons.modifier(modifier.getKey()), 20);
			this.tier = modifier.hasTiers() && tier >= 1 && tier <= 3 ? new String[] {"I", "II", "III"}[tier - 1] : "";
			setFont(small());
			setToolTipText(modifier.getName(tier));
			Dimension d = new Dimension(SIZE, SIZE);
			setPreferredSize(d);
			setMinimumSize(d);
			setMaximumSize(d);
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			Stone.paintWell(g2, SIZE, SIZE);
			icon.paintIcon(this, g2, (SIZE - icon.getIconWidth()) / 2, (SIZE - icon.getIconHeight()) / 2);
			if (!tier.isEmpty())
			{
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
				g2.setFont(getFont());
				FontMetrics fm = g2.getFontMetrics();
				int x = SIZE - fm.stringWidth(tier) - 2;
				int y = SIZE - 2;
				g2.setColor(Color.BLACK);
				g2.drawString(tier, x + 1, y + 1);
				g2.setColor(Stone.YELLOW);
				g2.drawString(tier, x, y);
			}
			g2.dispose();
		}
	}

	/** Whether Upload past runs is on, as the settings hold it: the report and the recap shown are built again for it. */
	void showUpload(boolean on)
	{
		if (on == upload)
		{
			return;
		}
		upload = on;
		build();
	}

	/**
	 * The last wave's report and the recap of the death in it, or none for null, as the panel shows them: in the arena
	 * between waves the report in short under the wave's row; out of it (`outside`) the report as a line, or after a death
	 * a line at the top that opens the death report. That page opens by itself the first time a death's recap shows, as
	 * the player comes out of the arena; null closes it, and the report's page with a null report, as a wave starts.
	 */
	void showReview(Report report, Recap recap, boolean outside)
	{
		boolean died = recap != null && !Objects.equals(deathOpened, recap.id);
		this.report = report;
		this.recap = recap;
		this.outside = outside;
		build();
		if (report == null)
		{
			pages.closePage(reportPage);
		}
		if (recap == null)
		{
			pages.closePage(deathPage);
		}
		else if (died)
		{
			deathOpened = recap.id;
			open(deathPage);
		}
	}

	/**
	 * What the last button did, shown on the page that shows, scrolled into sight: the panel's own well is under the page.
	 * Each page opens with none.
	 */
	void showResult(String message)
	{
		reportResult.setText(wrap(message, 196));
		deathResult.setText(wrap(message, 196));
		for (JPanel box : new JPanel[] {reportResultBox, deathResultBox})
		{
			box.setVisible(!message.isEmpty());
			box.revalidate();
			if (!message.isEmpty())
			{
				// After the layout the revalidate asked for, so that the well's place is known; on a page not shown it does nothing.
				javax.swing.SwingUtilities.invokeLater(() -> box.scrollRectToVisible(new java.awt.Rectangle(0, 0, box.getWidth(), box.getHeight())));
			}
		}
	}

	/** Shows a page from its top, with no result yet. */
	private void open(JComponent page)
	{
		showResult("");
		pages.showPage(page);
	}

	/** The panel's cards and the pages, as the report, the recap and the setting are now. */
	private void build()
	{
		reportCard.removeAll();
		deathRow.removeAll();
		reportPage.removeAll();
		deathPage.removeAll();
		Report r = report;
		if (r != null && recap == null)
		{
			if (outside)
			{
				reportCard.setGlow(null);
				reportCard.add(left(reportLine(r)));
			}
			else
			{
				buildCard(r);
			}
		}
		reportCard.setVisible(r != null && recap == null);
		if (recap != null)
		{
			deathRow.add(left(line(new Stone.Slot(30, SKULL), "You died on wave " + recap.wave, recap.into,
				"Opens the death report: what did it, the last ticks before, and the wave's report.", () -> open(deathPage))));
			buildDeathPage(recap);
		}
		deathRow.setVisible(recap != null);
		if (r != null)
		{
			buildPage(r);
		}
		for (JPanel card : new JPanel[] {reportCard, deathRow, reportPage, deathPage})
		{
			card.revalidate();
			card.repaint();
		}
	}

	/**
	 * A line that stands for a card: its picture in a slot, its title and what it holds, and Open, which shows its page.
	 * Out of the arena, where the player has played the wave, the report and the death are each one of these.
	 */
	private static JPanel line(Stone.Slot slot, String title, String holds, String tip, Runnable open)
	{
		JPanel text = clear(new BorderLayout(0, 1));
		text.add(new Stone.Shadowed(title, bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(label(wrap(holds, 120), small(), Stone.MUTED), BorderLayout.CENTER);
		Stone.Button button = new Stone.Button("Open", null, false, small(), 7, 6);
		button.setToolTipText(tip);
		button.addActionListener(e -> open.run());
		JPanel row = clear(new BorderLayout(8, 0));
		row.add(slot, BorderLayout.WEST);
		row.add(text, BorderLayout.CENTER);
		row.add(top(button), BorderLayout.EAST);
		return row;
	}

	/** The report as a line: the wave, its mistakes and its time, and Open to its page. */
	private JPanel reportLine(Report r)
	{
		String mistakes = r.mistakes.isEmpty() ? "No mistakes counted" : plural(r.mistakes.size(), "mistake");
		return line(new Stone.Slot(30, REPORT_ICON), "Wave " + r.wave + " report", mistakes + " \u00b7 " + r.time,
			"Opens the whole report in the panel: what the wave cost you, your prayer, every mistake with a button to watch it, and the run's waves.",
			() -> open(reportPage));
	}

	/**
	 * The wave report in short, between waves in the arena: the wave's time, its three figures, its mistakes by their
	 * pictures with what each cost, the first few of them, and the buttons to the site and to the report's own page. On
	 * that page, the whole report: the figures, the prayer against the wave's attacks, every mistake by tick with a button
	 * to watch it, the kinds the settings leave out, and the run's mistakes wave by wave.
	 */
	private void buildCard(Report r)
	{
		reportCard.setGlow(Stone.GOLD);
		reportCard.add(left(reportHead(r)));
		reportCard.add(SolSurvivorPanel.gap(5));
		reportCard.add(left(stats(r)));
		if (!r.mistakes.isEmpty() && r.lastTick > 0)
		{
			reportCard.add(SolSurvivorPanel.gap(6));
			reportCard.add(left(new WaveStrip(r.mistakes, r.lastTick, r.time)));
		}
		reportCard.add(SolSurvivorPanel.gap(6));
		reportCard.add(left(new Stone.Shadowed(r.mistakes.isEmpty() ? "No mistakes counted" : plural(r.mistakes.size(), "mistake"), small(),
			r.mistakes.isEmpty() ? Stone.GOOD : Stone.ORANGE)));
		if (!r.mistakes.isEmpty())
		{
			Stone.Well well = new Stone.Well(null, 2, 3, 2, 3);
			well.setLayout(new BoxLayout(well, BoxLayout.Y_AXIS));
			List<Group> groups = grouped(r.mistakes);
			int shown = Math.min(SHORT_LIST, groups.size());
			for (int i = 0; i < shown; i++)
			{
				well.add(shortLine(groups.get(i), i == shown - 1 && shown == groups.size()));
			}
			if (shown < groups.size())
			{
				JLabel more = label("and " + (groups.size() - shown) + " more in the full report", small(), Stone.DIM);
				more.setBorder(new EmptyBorder(3, 4, 3, 4));
				well.add(left(more));
			}
			reportCard.add(left(well));
		}
		Stone.Button full = new Stone.Button("All mistakes", null, false, small(), 7, 6);
		full.setToolTipText("Opens the whole report in the panel: your prayer, every mistake with a button to watch it, and the run's waves.");
		full.addActionListener(e -> open(reportPage));
		reportCard.add(left(buttons(reviewButton(r), full)));
		addUploadOff(reportCard);
	}

	/** The report's three figures side by side: the damage the wave cost, the attacks prayed on time and the idle ticks. */
	private static JPanel stats(Report r)
	{
		JPanel stats = clear(new java.awt.GridLayout(1, 3, 4, 0));
		stats.add(stat(String.valueOf(r.damage), "damage<br>taken"));
		stats.add(stat(r.attacks() == 0 ? "\u2013" : r.onTime + "/" + r.attacks(), "prayed<br>on time"));
		stats.add(stat(String.valueOf(r.idleTicks), "idle<br>ticks"));
		return stats;
	}

	/** One of the report's three figures in a well: its value in the values' yellow over its words, on two lines. */
	private static JComponent stat(String value, String words)
	{
		Stone.Well well = new Stone.Well(new BorderLayout(0, 1), 3, 2, 4, 2);
		JLabel v = new Stone.Shadowed(value, bold(), Stone.YELLOW);
		v.setHorizontalAlignment(JLabel.CENTER);
		well.add(centred(v), BorderLayout.NORTH);
		JLabel w = label("<html><div style='text-align: center'>" + words + "</div></html>", small(), Stone.MUTED);
		w.setHorizontalAlignment(JLabel.CENTER);
		well.add(w, BorderLayout.CENTER);
		return well;
	}

	/** A label in the middle of its cell: Shadowed draws from its left edge, so it is centred by its panel. */
	private static JPanel centred(JComponent c)
	{
		JPanel box = clear(new FlowLayout(FlowLayout.CENTER, 0, 0));
		box.add(c);
		return box;
	}

	/** The report's title and the wave's time, beside each other. */
	private static JPanel reportHead(Report r)
	{
		JPanel head = clear(new BorderLayout(4, 0));
		head.add(new Stone.Shadowed("Wave " + r.wave + " report", bold(), Stone.ORANGE), BorderLayout.WEST);
		head.add(label(r.time, small(), Stone.YELLOW), BorderLayout.EAST);
		return head;
	}

	/** The line under a card's Watch while Upload past runs is off. */
	private void addUploadOff(JPanel card)
	{
		if (!upload)
		{
			card.add(SolSurvivorPanel.gap(4));
			card.add(left(label(wrap(UPLOAD_OFF, 200), small(), Stone.DIM)));
		}
	}

	/** The report's Watch, which plays the wave back on the site: the card's main button while it can. */
	private Stone.Button reviewButton(Report r)
	{
		Stone.Button review = new Stone.Button("Watch wave " + r.wave, upload ? WATCH_ICON : WATCH_OFF_ICON, upload, small(), 7, 6);
		review.setToolTipText("Uploads this run and opens it on the Sol Survivor site at this wave, to play it back. Needs Upload past runs, in the settings.");
		review.addActionListener(e -> actions.watchAt(r.id, null));
		return review;
	}

	/** A kind and attacker in the short list: its picture and description, with its count, cost and tick range beneath. */
	private static JPanel shortLine(Group m, boolean last)
	{
		JPanel line = clear(new BorderLayout(5, 0));
		JLabel face = new JLabel(m.icon == null ? null : Icons.of(m.icon, 16));
		face.setPreferredSize(new Dimension(16, 16));
		face.setVerticalAlignment(JLabel.TOP);
		line.add(face, BorderLayout.WEST);
		JPanel words = clear(new BorderLayout(0, 2));
		words.add(label(wrap(m.text, 170), small(), Stone.TEXT), BorderLayout.NORTH);
		words.add(label(m.figures(), small(), m.damage > 0 ? Stone.ORANGE : Stone.MUTED), BorderLayout.CENTER);
		line.add(words, BorderLayout.CENTER);
		line.setBorder(lined(last, new EmptyBorder(4, 3, 4, 2)));
		return left(line);
	}

	/**
	 * The report's own page: a way back, to the panel or the death report it opened from, then the three figures, the
	 * prayer, every mistake, the run, and the buttons.
	 */
	private void buildPage(Report r)
	{
		Stone.Button back = new Stone.Button("‹ Back", null, false, small(), 7, 5);
		back.setToolTipText("Back to where you opened it.");
		back.addActionListener(e -> pages.closePage(reportPage));
		JPanel bar = clear(new BorderLayout(8, 0));
		bar.add(back, BorderLayout.WEST);
		bar.add(reportHead(r), BorderLayout.CENTER);
		reportPage.add(left(bar));
		reportPage.add(left(reportResultBox));
		reportPage.add(SolSurvivorPanel.gap(8));
		// The figures, as the short card has them, so that the page holds the whole report where the panel shows a line.
		reportPage.add(left(stats(r)));
		reportPage.add(SolSurvivorPanel.gap(6));

		if (r.attacks() > 0)
		{
			Stone.Row prayer = card();
			prayer.add(left(new Stone.Shadowed("Your prayer against " + plural(r.attacks(), "attack"), bold(), Stone.ORANGE)));
			prayer.add(SolSurvivorPanel.gap(4));
			Stone.Well bar2 = new Stone.Well(new BorderLayout(), 2, 2, 2, 2);
			bar2.add(new PrayerBar(r));
			prayer.add(left(bar2));
			prayer.add(SolSurvivorPanel.gap(4));
			JPanel legend = clear(new java.awt.GridLayout(2, 2, 6, 2));
			legend.add(key(ON_TIME, r.onTime + " on time"));
			legend.add(key(A_TICK_OFF, r.tickOff + " a tick off"));
			legend.add(key(WRONG, r.wrong + " wrong prayer"));
			legend.add(key(NONE_UP, r.none + " with none up"));
			prayer.add(left(legend));
			prayer.add(SolSurvivorPanel.gap(5));
			JPanel judged = clear(new FlowLayout(FlowLayout.LEFT, 3, 0));
			for (String p : new String[] {"magic", "ranged", "melee"})
			{
				judged.add(new JLabel(Icons.of("prayer-" + p, 14)));
			}
			judged.add(label(wrap("Judged as the site's run review judges them.", 140), small(), Stone.MUTED));
			prayer.add(left(judged));
			reportPage.add(left(prayer));
			reportPage.add(SolSurvivorPanel.gap(6));
		}

		Stone.Row list = card();
		list.add(left(new Stone.Shadowed(r.mistakes.isEmpty() ? "No mistakes counted" : plural(r.mistakes.size(), "mistake") + ", by tick", bold(),
			r.mistakes.isEmpty() ? Stone.GOOD : Stone.ORANGE)));
		if (!r.mistakes.isEmpty())
		{
			list.add(SolSurvivorPanel.gap(4));
			Stone.Well well = new Stone.Well(null, 2, 3, 2, 3);
			well.setLayout(new BoxLayout(well, BoxLayout.Y_AXIS));
			for (int i = 0; i < r.mistakes.size(); i++)
			{
				well.add(fullLine(r.id, r.mistakes.get(i), i == r.mistakes.size() - 1));
			}
			list.add(left(well));
		}
		if (r.notCounted != null)
		{
			list.add(SolSurvivorPanel.gap(4));
			Stone.Well off = new Stone.Well(new BorderLayout(), 4, 6, 4, 6);
			off.add(label(wrap(r.notCounted, 180), small(), Stone.MUTED));
			list.add(left(off));
		}
		reportPage.add(left(list));

		if (r.runWaves.length > 1)
		{
			reportPage.add(SolSurvivorPanel.gap(6));
			Stone.Row run = card();
			run.add(left(new Stone.Shadowed("This run, wave by wave", bold(), Stone.ORANGE)));
			run.add(SolSurvivorPanel.gap(4));
			run.add(left(new RunChart(r.runWaves, r.runMistakes, r.wave)));
			if (r.common != null)
			{
				run.add(SolSurvivorPanel.gap(3));
				run.add(left(label(wrap(r.common, 190), small(), Stone.MUTED)));
			}
			reportPage.add(left(run));
		}

		Stone.Button copy = new Stone.Button("Copy report", null, false, small(), 7, 6);
		copy.setToolTipText("Copies this report as text.");
		copy.addActionListener(e -> actions.copyReport(r.id));
		JPanel foot = buttons(reviewButton(r), copy);
		foot.setBorder(new EmptyBorder(8, 0, 0, 0));
		reportPage.add(left(foot));
		addUploadOff(reportPage);
		reportPage.add(SolSurvivorPanel.gap(6));
		reportPage.add(left(label(wrap("Counted from the wave log once the wave ended. Nothing here is shown while a wave is under way.", 210),
			small(), Stone.DIM)));
	}

	/** A mistake on the report's page: its tick, its picture, what happened, its badge, and a button to watch it. */
	private JPanel fullLine(int id, Row m, boolean last)
	{
		JPanel line = new JPanel(new GridBagLayout());
		line.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(0, 0, 0, 4);
		cell(line, c, 0, fixed(label("t" + m.tick, small(), Stone.DIM), 26), 0);
		JLabel face = new JLabel(m.icon == null ? null : Icons.of(m.icon, 16));
		face.setPreferredSize(new Dimension(16, 16));
		cell(line, c, 1, face, 0);
		cell(line, c, 2, label(wrap(m.text, 88), small(), Stone.TEXT), 1);
		c.anchor = GridBagConstraints.NORTHEAST;
		cell(line, c, 3, new Stone.Pill(m.badge, m.hit, small()), 0);
		c.insets = new Insets(0, 0, 0, 0);
		Stone.Button watch = new Stone.Button(upload ? PLAY_ICON : PLAY_OFF_ICON, 16);
		watch.setToolTipText("Watch the run on the site from tick " + m.tick + ". Needs Upload past runs, in the settings.");
		watch.addActionListener(e -> actions.watchAt(id, m.tick));
		cell(line, c, 4, watch, 0);
		line.setBorder(lined(last, new EmptyBorder(4, 3, 4, 2)));
		return left(line);
	}

	/**
	 * The death report, a page of its own: Home to the panel; a card that says when the player died and what did it; how it
	 * happened, in steps; a card of the last ticks before, folded under the steps, each with the hitpoints, the protection
	 * prayer up, what happened and what it hit for; and the buttons; and under them the wave's report as a line, whose page Back brings the player here from.
	 */
	private void buildDeathPage(Recap r)
	{
		Stone.Button home = new Stone.Button("‹ Home", null, false, small(), 7, 5);
		home.setToolTipText("Back to the panel, where this report stays a click away until you enter the arena again.");
		home.addActionListener(e -> pages.showMain());
		JPanel bar = clear(new BorderLayout(8, 0));
		bar.add(home, BorderLayout.WEST);
		bar.add(new Stone.Shadowed("Death report", bold(), Stone.ORANGE), BorderLayout.CENTER);
		deathPage.add(left(bar));
		deathPage.add(left(deathResultBox));
		deathPage.add(SolSurvivorPanel.gap(8));

		Stone.Row head = card();
		head.setGlow(Stone.BAD);
		JPanel title = clear(new BorderLayout(8, 0));
		title.add(new Stone.Slot(30, SKULL), BorderLayout.WEST);
		JPanel text = clear(new BorderLayout(0, 1));
		text.add(new Stone.Shadowed("You died on wave " + r.wave, bold(), Stone.ORANGE), BorderLayout.NORTH);
		text.add(label(r.when, small(), Stone.MUTED), BorderLayout.CENTER);
		title.add(text, BorderLayout.CENTER);
		head.add(left(title));
		if (r.cause != null)
		{
			head.add(SolSurvivorPanel.gap(6));
			Stone.Well cause = new Stone.Well(null, 4, 6, 5, 6);
			cause.setLayout(new BoxLayout(cause, BoxLayout.Y_AXIS));
			cause.add(left(label("What did it", small(), Stone.MUTED)));
			cause.add(left(label(wrap(r.cause, 184), small(), Stone.TEXT)));
			head.add(left(cause));
		}
		deathPage.add(left(head));

		List<Step> steps = steps(r.lines);
		if (!steps.isEmpty())
		{
			// How it happened, in steps (the design review's 9, Stefan's choice of 2026-10-31): each hit that cost
			// hitpoints and each attack that got past the prayer, in order, the one past the prayer marked in yellow as
			// the moment to have done otherwise; every tick stays one click under it.
			deathPage.add(SolSurvivorPanel.gap(5));
			Stone.Row how = card();
			how.add(left(new Stone.Shadowed("How it happened", bold(), Stone.ORANGE)));
			how.add(SolSurvivorPanel.gap(4));
			for (int i = 0; i < steps.size(); i++)
			{
				how.add(left(stepLine(i + 1, steps.get(i))));
				how.add(SolSurvivorPanel.gap(3));
			}
			deathPage.add(left(how));
		}

		if (!r.lines.isEmpty())
		{
			deathPage.add(SolSurvivorPanel.gap(5));
			Stone.Row ticks = card();
			String span = r.lines.get(0).getTick() + " to " + r.lines.get(r.lines.size() - 1).getTick();
			if (steps.isEmpty())
			{
				ticks.add(left(new Stone.Shadowed("The last " + r.lines.size() + " ticks", bold(), Stone.ORANGE)));
				ticks.add(SolSurvivorPanel.gap(4));
			}
			Stone.Well well = new Stone.Well(null, 2, 2, 2, 2);
			well.setLayout(new BoxLayout(well, BoxLayout.Y_AXIS));
			well.add(recapRow(label("Tick", small(), Stone.DIM), label("HP", small(), Stone.DIM), new JLabel(), label("What happened", small(), Stone.DIM),
				label("Hit", small(), Stone.DIM), null, false));
			for (int i = 0; i < r.lines.size(); i++)
			{
				WaveReview.RecapLine l = r.lines.get(i);
				Integer hp = l.getHitpoints();
				JLabel tick = label(String.valueOf(l.getTick()), small(), Stone.DIM);
				JLabel hitpoints = label(hp == null ? "?" : String.valueOf(hp), small(), hpColour(hp, r.maxHitpoints));
				JLabel prayer = l.getPrayer() == null || l.getPrayer().isEmpty() ? label("–", small(), Stone.DIM)
					: new JLabel(Icons.of("prayer-" + l.getPrayer(), 14));
				prayer.setToolTipText(prayerName(l.getPrayer()));
				JLabel what = label(wrap(l.getText(), 80), small(), Stone.TEXT);
				boolean hit = l.getHit().matches("[1-9][0-9]*");
				JComponent landed = recapHit(l.getHit());
				Color tint = !hit ? null : i == r.lines.size() - 1 ? new Color(232, 69, 46, 52) : new Color(232, 69, 46, 31);
				well.add(recapRow(tick, hitpoints, prayer, what, landed, tint, i == r.lines.size() - 1));
			}
			if (!steps.isEmpty())
			{
				// Under the steps the table is folded, its button saying which ticks it holds.
				Stone.Button every = new Stone.Button("Every tick, " + span + " ▸", null, false, small(), 7, 5);
				every.setToolTipText("Shows each of the last ticks: your hitpoints, your prayer, what happened and what it hit for.");
				well.setVisible(false);
				every.addActionListener(e ->
				{
					well.setVisible(!well.isVisible());
					every.setText("Every tick, " + span + (well.isVisible() ? " ▾" : " ▸"));
					deathPage.revalidate();
					deathPage.repaint();
				});
				ticks.add(left(every));
				ticks.add(SolSurvivorPanel.gap(4));
			}
			ticks.add(left(well));
			deathPage.add(left(ticks));
		}

		int from = r.lines.isEmpty() ? 0 : r.lines.get(0).getTick();
		Stone.Button open = new Stone.Button("Watch from tick " + from, null, upload, small(), 7, 6);
		open.setToolTipText("Uploads this run and opens it on the Sol Survivor site at tick " + from + " of this wave. Needs Upload past runs, in the settings.");
		open.addActionListener(e -> actions.watchAt(r.id, from));
		Stone.Button watch = new Stone.Button("Watch the run", null, false, small(), 7, 6);
		watch.setToolTipText("Uploads this run and opens it on the Sol Survivor site at this wave's start. Needs Upload past runs, in the settings.");
		watch.addActionListener(e -> actions.watchAt(r.id, null));
		JPanel foot = buttons(open, watch);
		foot.setBorder(new EmptyBorder(6, 0, 2, 0));
		deathPage.add(left(foot));
		addUploadOff(deathPage);
		if (report != null)
		{
			// The wave's report as a line: the death is the page's, and the report's page is a click away, Back to here.
			deathPage.add(SolSurvivorPanel.gap(8));
			Stone.Row line = card();
			line.add(left(reportLine(report)));
			deathPage.add(left(line));
		}
	}

	/** A step of the death: its tick, what happened, the hitpoints before and after, what it hit for, and whether it is the turn. */
	static final class Step
	{
		final int tick;
		final String text;
		final Integer before;
		final Integer after;
		final String hit;
		final String prayer;
		/** An attack that got past the prayer: the moment to have done otherwise, marked in yellow. */
		final boolean past;

		Step(int tick, String text, Integer before, Integer after, String hit, String prayer, boolean past)
		{
			this.tick = tick;
			this.text = text;
			this.before = before;
			this.after = after;
			this.hit = hit;
			this.prayer = prayer;
			this.past = past;
		}

		@Override
		public String toString()
		{
			return tick + " " + text + " " + before + "→" + after + " " + hit + (past ? " past" : "");
		}
	}

	/**
	 * The death in steps, from the recap's ticks: each tick whose attack hit for something, and each whose attack got past
	 * the prayer, in order, with the hitpoints before it (the tick before's) and after it. Empty when no tick did either,
	 * and the recap's table stands alone.
	 */
	static List<Step> steps(List<WaveReview.RecapLine> lines)
	{
		List<Step> steps = new ArrayList<>();
		Integer before = null;
		for (WaveReview.RecapLine l : lines)
		{
			boolean hit = l.getHit().matches("[1-9][0-9]*");
			boolean past = l.getText().contains("past your prayer");
			if (hit || past)
			{
				steps.add(new Step(l.getTick(), l.getText(), before, l.getHitpoints(), hit ? l.getHit() : null, l.getPrayer(), past));
			}
			before = l.getHitpoints();
		}
		return steps;
	}

	/** A step's line: its number in a dot, what happened over its tick and hitpoints, and what it hit for. */
	private static JPanel stepLine(int n, Step step)
	{
		Stone.Well line = new Stone.Well(null, 4, 5, 4, 5);
		line.setLayout(new BorderLayout(6, 0));
		JLabel number = label(String.valueOf(n), small(), step.past ? Stone.YELLOW : Stone.MUTED);
		number.setVerticalAlignment(JLabel.TOP);
		number.setPreferredSize(new Dimension(12, number.getPreferredSize().height));
		line.add(number, BorderLayout.WEST);
		JPanel words = clear(new BorderLayout(0, 1));
		words.add(label(wrap(step.text, 150), small(), Stone.TEXT), BorderLayout.NORTH);
		String hitpoints = step.before == null || step.after == null ? "" : " · " + step.before + " → " + step.after;
		String detail = step.past
			? "t" + step.tick + " · " + (step.prayer == null || step.prayer.isEmpty() ? "no protection prayer up" : prayerName(step.prayer) + " up")
			: "t" + step.tick + hitpoints;
		words.add(label(detail, small(), step.past ? Stone.YELLOW : Stone.MUTED), BorderLayout.CENTER);
		line.add(words, BorderLayout.CENTER);
		if (step.hit != null)
		{
			JPanel pill = clear(new BorderLayout());
			pill.add(new Stone.Pill(step.hit, true, small()), BorderLayout.NORTH);
			line.add(pill, BorderLayout.EAST);
		}
		if (step.past)
		{
			line.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Stone.YELLOW), new EmptyBorder(3, 4, 3, 4)));
		}
		return left(line);
	}

	/** A blocked hit stays compact in the narrow Hit column, with the full meaning on hover. */
	static JComponent recapHit(String hit)
	{
		if (hit.matches("[1-9][0-9]*"))
		{
			return new Stone.Pill(hit, true, small());
		}
		JLabel value = label("blocked".equals(hit) ? "–" : hit, small(), Stone.MUTED);
		if ("blocked".equals(hit))
		{
			value.setToolTipText("Blocked: the attack did no damage.");
		}
		return value;
	}

	/** A row of the recap's table, its columns at fixed widths so that they line up from row to row, on a tint or none. */
	private static JPanel recapRow(JComponent tick, JComponent hp, JComponent prayer, JComponent what, JComponent hit, Color tint, boolean last)
	{
		JPanel row = new JPanel(new GridBagLayout())
		{
			@Override
			protected void paintComponent(Graphics g)
			{
				if (tint != null)
				{
					g.setColor(tint);
					g.fillRect(0, 0, getWidth(), getHeight());
				}
			}
		};
		row.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(0, 0, 0, 4);
		cell(row, c, 0, fixed(tick, 22), 0);
		cell(row, c, 1, fixed(hp, 18), 0);
		cell(row, c, 2, fixed(prayer, 15), 0);
		cell(row, c, 3, what, 1);
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHEAST;
		cell(row, c, 4, fixed(hit, 26), 0);
		row.setBorder(lined(last, new EmptyBorder(3, 3, 3, 4)));
		return left(row);
	}

	private static JComponent fixed(JComponent c, int width)
	{
		JPanel box = clear(new BorderLayout());
		box.add(c, BorderLayout.WEST);
		Dimension d = new Dimension(width, c.getPreferredSize().height);
		box.setPreferredSize(d);
		box.setMinimumSize(d);
		return box;
	}

	private static void cell(JPanel table, GridBagConstraints c, int x, JComponent cell, double weight)
	{
		c.gridx = x;
		c.gridy = 0;
		c.weightx = weight;
		c.fill = weight > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		table.add(cell, c);
	}

	/** Hitpoints coloured by their share of the wave's most: half or more green, a quarter or more amber, less red. */
	private static Color hpColour(Integer hp, int most)
	{
		if (hp == null || most <= 0)
		{
			return Stone.TEXT;
		}
		return hp * 2 >= most ? FINE : hp * 4 >= most ? FAIR : POOR;
	}

	private static String prayerName(String prayer)
	{
		if ("magic".equals(prayer))
		{
			return "Protect from Magic";
		}
		if ("ranged".equals(prayer))
		{
			return "Protect from Missiles";
		}
		if ("melee".equals(prayer))
		{
			return "Protect from Melee";
		}
		return prayer == null ? "Not in the log" : "No protection prayer";
	}

	/** A card of the report's page or the recap: a raised row whose parts go one under another. */
	private static Stone.Row card()
	{
		Stone.Row card = new Stone.Row(null, 6, 6, 6, 6);
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		return card;
	}

	/** Two buttons side by side, each half the width. */
	private static JPanel buttons(JComponent first, JComponent second)
	{
		JPanel row = clear(new java.awt.GridLayout(1, 2, 5, 0));
		row.add(first);
		row.add(second);
		row.setBorder(new EmptyBorder(6, 0, 0, 0));
		return row;
	}

	/** A small heading over a well, in the designs' muted words. */
	private static JComponent section(String text)
	{
		JLabel l = label(text, small(), Stone.MUTED);
		l.setBorder(new EmptyBorder(0, 1, 3, 0));
		return left(l);
	}

	/** A line of a well's list: a rule under it in the panel's ground, but under the last. */
	private static javax.swing.border.Border lined(boolean last, EmptyBorder padding)
	{
		return last ? padding : BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, Stone.BACKGROUND), padding);
	}

	/** A component held to the top of its cell, as BorderLayout's sides would stretch it to the row's height. */
	private static JPanel top(JComponent c)
	{
		JPanel box = clear(new BorderLayout());
		box.add(c, BorderLayout.NORTH);
		return box;
	}

	private static <T extends JComponent> T left(T c)
	{
		c.setAlignmentX(Component.LEFT_ALIGNMENT);
		return c;
	}

	/** A legend's key: a square of the bar's colour, and its words. */
	private static JLabel key(Color colour, String text)
	{
		JLabel l = label(text, small(), Stone.TEXT);
		l.setIcon(Stone.square(9, colour));
		l.setIconTextGap(4);
		return l;
	}

	private static String escape(String text)
	{
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	static String plural(int n, String one)
	{
		return n + " " + (n == 1 ? one : one + "s");
	}

	/** The prayer against the wave's attacks as one bar: on time, a tick off, the wrong prayer and none up, by their share. */
	private static final class PrayerBar extends JComponent
	{
		private final int[] parts;

		PrayerBar(Report r)
		{
			parts = new int[] {r.onTime, r.tickOff, r.wrong, r.none};
			setPreferredSize(new Dimension(180, 12));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 12));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			int w = getWidth();
			int h = getHeight();
			int total = parts[0] + parts[1] + parts[2] + parts[3];
			Color[] colours = {ON_TIME, A_TICK_OFF, WRONG, NONE_UP};
			int x = 0;
			int sum = 0;
			for (int i = 0; i < parts.length && total > 0; i++)
			{
				sum += parts[i];
				int to = (int) Math.round((double) w * sum / total);
				g2.setColor(colours[i]);
				g2.fillRect(x, 0, to - x, h);
				x = to;
			}
			g2.dispose();
		}
	}

	/**
	 * The run's mistakes wave by wave: a bar for each wave with its count over it and its number under it, green for a
	 * clean wave, amber for one or two mistakes and red for more, as the designs colour them.
	 */
	/**
	 * The wave in a strip (the design review's 8, Stefan's choice of 2026-10-31): its ticks from the start to its last
	 * across the width, and on it each mistake where it fell, in its attack's colour, the game's blue for magic, green for
	 * ranged and red for melee; orange for a special, a blast or the sand, and an idle stretch as long as it lasted in
	 * grey. Under it the start and the wave's time. So the eye sees at once whether the mistakes came together, and when.
	 */
	static final class WaveStrip extends JComponent
	{
		static final Color MAGIC = new Color(0x4d8bff);
		static final Color RANGED = new Color(0x38c246);
		static final Color MELEE = new Color(0xe23b2b);
		private static final int BAR = 16;

		private final List<Row> rows;
		private final int last;
		private final String time;

		WaveStrip(List<Row> rows, int last, String time)
		{
			this.rows = rows;
			this.last = last;
			this.time = time;
			setFont(small());
			setPreferredSize(new Dimension(190, BAR + getFontMetrics(small()).getHeight() + 2));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, getPreferredSize().height));
			setToolTipText("<html>Each mistake where it fell in the wave, in its attack's colour:<br>blue magic, green ranged, red melee;"
				+ "<br>orange a special, a blast or the sand; grey an idle stretch.</html>");
		}

		/** A mistake's colour on the strip, by its attack's style or, for no attack's, by what it was. */
		static Color colourOf(Row row)
		{
			if ("magic".equals(row.style))
			{
				return MAGIC;
			}
			if ("ranged".equals(row.style))
			{
				return RANGED;
			}
			if ("melee".equals(row.style))
			{
				return MELEE;
			}
			return "hourglass".equals(row.icon) ? Stone.DIM : Stone.ORANGE;
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			int width = getWidth();
			Stone.paintWell(g2, width, BAR);
			int inner = Math.max(1, width - 4);
			// The idle stretches first, under the marks, as long as they lasted.
			for (Row row : rows)
			{
				if ("hourglass".equals(row.icon) && row.badge.matches("[0-9]+t"))
				{
					int ticks = Integer.parseInt(row.badge.substring(0, row.badge.length() - 1));
					int from = 2 + inner * row.tick / Math.max(1, last);
					int to = 2 + inner * Math.min(last, row.tick + ticks) / Math.max(1, last);
					g2.setColor(Stone.DIM);
					g2.fillRect(from, 5, Math.max(2, to - from), BAR - 10);
				}
			}
			// Marks that land within a few pixels of each other, such as a Manticore's three orbs, share the mark's height,
			// one above another in their order, so that each colour shows.
			Map<Integer, List<Row>> spots = new LinkedHashMap<>();
			for (Row row : rows)
			{
				if (!"hourglass".equals(row.icon))
				{
					int x = Math.min(2 + inner * row.tick / Math.max(1, last), width - 4);
					spots.computeIfAbsent(x / 4, k -> new ArrayList<>()).add(row);
				}
			}
			for (List<Row> spot : spots.values())
			{
				int x = Math.min(2 + inner * spot.get(0).tick / Math.max(1, last), width - 4);
				int tall = BAR - 6;
				g2.setColor(Stone.OUTLINE);
				g2.fillRect(x - 2, 2, 5, BAR - 4);
				for (int i = 0; i < spot.size(); i++)
				{
					int top = 3 + tall * i / spot.size();
					int bottom = 3 + tall * (i + 1) / spot.size();
					g2.setColor(colourOf(spot.get(i)));
					g2.fillRect(x - 1, top, 3, Math.max(1, bottom - top));
				}
			}
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			int base = BAR + 1 + fm.getAscent();
			g2.setColor(Stone.MUTED);
			g2.drawString("start", 0, base);
			g2.drawString(time, width - fm.stringWidth(time), base);
			g2.dispose();
		}
	}

	private static final class RunChart extends JComponent
	{
		private final int[] waves;
		private final int[] counts;
		private final int selected;

		RunChart(int[] waves, int[] counts, int selected)
		{
			this.waves = waves;
			this.counts = counts;
			this.selected = selected;
			setPreferredSize(new Dimension(190, 64));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, 64));
			setFont(small());
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			g2.setFont(getFont());
			FontMetrics fm = g2.getFontMetrics();
			int most = 1;
			for (int c : counts)
			{
				most = Math.max(most, c);
			}
			int n = waves.length;
			// A slot for each wave across the width, as the designs share it, but no wider than a quarter of it.
			int slot = Math.max(1, getWidth() / Math.max(n, 4));
			int top = fm.getHeight() + 2;
			int bottom = getHeight() - fm.getHeight() - 1;
			int tall = Math.max(1, bottom - top);
			for (int i = 0; i < n; i++)
			{
				int x = i * slot;
				int barW = Math.max(3, slot - 3);
				int barH = counts[i] == 0 ? 2 : Math.max(3, tall * counts[i] / most);
				Color colour = counts[i] == 0 ? ON_TIME : counts[i] <= 2 ? FAIR : POOR;
				g2.setColor(colour);
				g2.fillRect(x + (slot - barW) / 2, bottom - barH, barW, barH);
				String count = String.valueOf(counts[i]);
				g2.setColor(counts[i] == 0 ? FINE : colour);
				g2.drawString(count, x + (slot - fm.stringWidth(count)) / 2, bottom - barH - 2);
				String wave = String.valueOf(waves[i]);
				g2.setColor(waves[i] == selected ? Stone.YELLOW : Stone.MUTED);
				g2.drawString(wave, x + (slot - fm.stringWidth(wave)) / 2, getHeight() - fm.getDescent());
			}
			g2.dispose();
		}
	}

	/** The report as text, for the clipboard. */
	static String text(Report r)
	{
		List<String> lines = new ArrayList<>();
		lines.add("Sol Survivor: wave " + r.wave + " report, " + r.time);
		if (r.attacks() > 0)
		{
			lines.add("Prayer against " + plural(r.attacks(), "attack") + ": " + r.onTime + " on time, " + r.tickOff + " a tick off, " + r.wrong
				+ " wrong prayer, " + r.none + " with none up.");
		}
		lines.add(r.mistakes.isEmpty() ? "No mistakes counted." : plural(r.mistakes.size(), "mistake") + ":");
		for (Row m : r.mistakes)
		{
			lines.add("Tick " + m.tick + ": " + m.text + " (" + m.badge + ")");
		}
		if (r.notCounted != null)
		{
			lines.add(r.notCounted);
		}
		if (r.common != null)
		{
			lines.add(r.common);
		}
		return String.join("\n", lines);
	}
}
