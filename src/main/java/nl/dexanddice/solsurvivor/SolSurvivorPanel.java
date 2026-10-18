package nl.dexanddice.solsurvivor;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JLabel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * The side panel: two buttons that open a link on the site and copy it, the wave start's or the arena's now, a button
 * that opens the site's modifier pick with Minimus's three offers while he offers them, those three in the brand's
 * orange as the ones that open the site, a button that copies the wave log
 * with a line on what it holds, and a line that says what the last button did. A bad plan is reported from the site's
 * own page. Like the rest of the plugin it shows nothing about how to
 * play the wave; the plans are the site's.
 */
class SolSurvivorPanel extends PluginPanel
{
	private final JLabel status = new JLabel();
	private final JButton copyWaveStart = new JButton("Open wave start");
	private final JButton copyNow = new JButton("Open arena now");
	private final JButton openRecommender = new JButton("Open in recommender");
	private final JLabel waveLog = new JLabel();
	private final JButton copyWaveLog = new JButton("Copy wave log");
	private final JLabel result = new JLabel();

	SolSurvivorPanel(Runnable onOpenWaveStart, Runnable onOpenNow, Runnable onOpenRecommender, Runnable onCopyWaveLog)
	{
		JLabel title = new JLabel("Sol Survivor");
		title.setForeground(Color.WHITE);
		add(title);

		status.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(status);

		copyWaveStart.setToolTipText("Opens the arena on the wave's first tick on the site, and copies its link.");
		copyNow.setToolTipText("Opens the arena as it is now on the site, and copies its link.");
		copyWaveStart.addActionListener(e -> onOpenWaveStart.run());
		copyNow.addActionListener(e -> onOpenNow.run());
		opensSite(copyWaveStart);
		opensSite(copyNow);
		add(copyWaveStart);
		add(copyNow);

		openRecommender.setToolTipText("While Minimus offers his three modifiers: opens the site's modifier pick with them filled in.");
		openRecommender.addActionListener(e -> onOpenRecommender.run());
		opensSite(openRecommender);
		add(openRecommender);

		waveLog.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		copyWaveLog.addActionListener(e -> onCopyWaveLog.run());
		add(waveLog);
		add(copyWaveLog);

		result.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(result);

		JLabel help = new JLabel(wrap("The orange buttons open the Sol Survivor site; paste the wave log into it. This plugin only "
			+ "opens the site and copies: it draws nothing in the game."));
		help.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
		add(help);

		showState(false, false, false, "", false);
	}

	/**
	 * Whether the player is in the Colosseum, whether this wave's start was taken, whether Minimus is offering modifiers,
	 * what the wave log holds, and whether there is one to copy. On the Swing thread.
	 */
	void showState(boolean inColosseum, boolean waveStart, boolean offers, String waveLogStatus, boolean hasWaveLog)
	{
		status.setText(wrap(inColosseum ? "In the Fortis Colosseum." : "Not in the Fortis Colosseum."));
		copyWaveStart.setEnabled(waveStart);
		copyNow.setEnabled(inColosseum);
		openRecommender.setEnabled(offers);
		waveLog.setText(wrap(waveLogStatus));
		copyWaveLog.setEnabled(hasWaveLog);
	}

	/** What the last button did. On the Swing thread. */
	void showResult(String message)
	{
		result.setText(wrap(message));
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
