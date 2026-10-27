package nl.dexanddice.solsurvivor;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * A FlowLayout that wraps and asks for the height its wrapped lines take, which FlowLayout does not: its preferred size
 * is one long line, so a row of chips in the panel would be cut off rather than wrap. The width it wraps at is the
 * container's own once it has one, or `width` before the first layout.
 */
final class WrapLayout extends FlowLayout
{
	private final int width;

	WrapLayout(int hgap, int vgap, int width)
	{
		super(FlowLayout.LEFT, hgap, vgap);
		this.width = width;
	}

	@Override
	public Dimension preferredLayoutSize(Container target)
	{
		return size(target, true);
	}

	@Override
	public Dimension minimumLayoutSize(Container target)
	{
		return size(target, false);
	}

	private Dimension size(Container target, boolean preferred)
	{
		synchronized (target.getTreeLock())
		{
			Insets in = target.getInsets();
			int room = (target.getWidth() > 0 ? target.getWidth() : width) - in.left - in.right;
			int lineWidth = 0;
			int lineHeight = 0;
			int height = 0;
			int widest = 0;
			for (Component c : target.getComponents())
			{
				if (!c.isVisible())
				{
					continue;
				}
				Dimension d = preferred ? c.getPreferredSize() : c.getMinimumSize();
				if (lineWidth > 0 && lineWidth + getHgap() + d.width > room)
				{
					height += lineHeight + getVgap();
					widest = Math.max(widest, lineWidth);
					lineWidth = 0;
					lineHeight = 0;
				}
				lineWidth += (lineWidth > 0 ? getHgap() : 0) + d.width;
				lineHeight = Math.max(lineHeight, d.height);
			}
			height += lineHeight;
			widest = Math.max(widest, lineWidth);
			return new Dimension(widest + in.left + in.right, height + in.top + in.bottom);
		}
	}

	@Override
	public void layoutContainer(Container target)
	{
		synchronized (target.getTreeLock())
		{
			Insets in = target.getInsets();
			int room = target.getWidth() - in.left - in.right;
			int x = 0;
			int y = 0;
			int lineHeight = 0;
			for (Component c : target.getComponents())
			{
				if (!c.isVisible())
				{
					continue;
				}
				Dimension d = c.getPreferredSize();
				if (x > 0 && x + getHgap() + d.width > room)
				{
					y += lineHeight + getVgap();
					x = 0;
					lineHeight = 0;
				}
				x += x > 0 ? getHgap() : 0;
				c.setBounds(in.left + x, in.top + y, d.width, d.height);
				x += d.width;
				lineHeight = Math.max(lineHeight, d.height);
			}
		}
	}
}
