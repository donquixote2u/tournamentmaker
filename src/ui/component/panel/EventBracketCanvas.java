package ui.component.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import data.event.painter.EventPainter;

public class EventBracketCanvas extends JPanel {
	private static final long serialVersionUID = -5116844349124765854L;
	private int matchHeight, xPadding, yPadding, textPadding;
	private float fontSize;
	private JScrollPane parent;
	private EventPainter eventPainter;
	
	public EventBracketCanvas(int matchHeight, int xPadding, int yPadding, float fontSize, int textPadding, JScrollPane parent) {
		this.matchHeight = matchHeight;
		this.xPadding = xPadding;
		this.yPadding = yPadding;
		this.fontSize = fontSize;
		this.textPadding = textPadding;
		this.parent = parent;
	}
	
	public void setEventPainter(EventPainter eventPainter) {
		this.eventPainter = eventPainter;
		repaint();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if(parent != null) {
					parent.revalidate();
					parent.getVerticalScrollBar().setValue(0);
					parent.getHorizontalScrollBar().setValue(0);
				}
			}
		});
	}
	
	public boolean hasPainter() {
		return eventPainter != null;
	}
	
	public Dimension getPreferredSize() {
		if(eventPainter == null || eventPainter.getCanvasSize() == null) {
			return super.getPreferredSize();
		}
		return eventPainter.getCanvasSize();
	}
	
	protected void paintComponent(Graphics g) {
		// should clear the screen
		super.paintComponent(g);
		if(eventPainter == null) {
			return;
		}
		// setting the font properties
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		g.setFont(getFont());
		g.setColor(Color.BLACK);
		eventPainter.paint(matchHeight, xPadding, yPadding, fontSize, textPadding, g);
	}
}
