package ui.component.label;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.border.Border;

import sun.swing.DefaultLookup;
import ui.util.Pair;

/**
 * This class allows for different formatting within text. All text should be added through the addText, addItalic, addBold,
 * and addBoldItalic functions. This class can be configured to not draw the drop location for drag and drop enabled JLists.
 * @author Jason Young
 *
 */
public class FormattedListCellRenderer extends DefaultListCellRenderer {
	private static final long serialVersionUID = -6555022157426999491L;
	private enum Format {
		ITALIC, BOLD, BOLD_ITALIC, DEFAULT
	}
	private List<Pair<String, Pair<Format, Color>>> text;
	private boolean drawDropLocation;
	
	public FormattedListCellRenderer(boolean drawDropLocation) {
		super();
		this.drawDropLocation = drawDropLocation;
		text = new ArrayList<Pair<String, Pair<Format, Color>>>();
	}
	
	public void addText(String value) {
		addText(value, Color.BLACK);
	}
	
	public void addText(String value, Color color) {
		addText(value, Format.DEFAULT, color);
	}
	
	private void addText(String value, Format format, Color color) {
		if(value == null || format == null || color == null) {
			return;
		}
		text.add(new Pair<String, Pair<Format, Color>>(value, new Pair<Format, Color>(format, color)));
	}
	
	public void addItalic(String value) {
		addItalic(value, Color.BLACK);
	}
	
	public void addItalic(String value, Color color) {
		addText(value, Format.ITALIC, color);
	}
	
	public void addBold(String value) {
		addBold(value, Color.BLACK);
	}
	
	public void addBold(String value, Color color) {
		addText(value, Format.BOLD, color);
	}
	
	public void addBoldItalic(String value) {
		addBoldItalic(value, Color.BLACK);
	}
	
	public void addBoldItalic(String value, Color color) {
		addText(value, Format.BOLD_ITALIC, color);
	}
	
	@SuppressWarnings("rawtypes")
	public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
		text.clear();
		// copying the default implementation so we can control if the drop location should be drawn
		setComponentOrientation(list.getComponentOrientation());
        Color bg = null;
        Color fg = null;
        if(drawDropLocation) {
        	JList.DropLocation dropLocation = list.getDropLocation();
        	if(dropLocation != null && !dropLocation.isInsert() && dropLocation.getIndex() == index) {
        		bg = DefaultLookup.getColor(this, ui, "List.dropCellBackground");
        		fg = DefaultLookup.getColor(this, ui, "List.dropCellForeground");
        		isSelected = true;
        	}
        }
        if(isSelected) {
            setBackground(bg == null ? list.getSelectionBackground() : bg);
            setForeground(fg == null ? list.getSelectionForeground() : fg);
        }
        else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        setText(" ");
        setEnabled(list.isEnabled());
        setFont(list.getFont());
        Border border = null;
        if(cellHasFocus) {
            if(isSelected) {
                border = DefaultLookup.getBorder(this, ui, "List.focusSelectedCellHighlightBorder");
            }
            if(border == null) {
                border = DefaultLookup.getBorder(this, ui, "List.focusCellHighlightBorder");
            }
        }
        else {
            border = noFocusBorder;
        }
        setBorder(border);
        return this;
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
		Insets insets = getInsets();
		Font defaultFont = g.getFont();
		int x = insets.left;
		int y = insets.top + g.getFontMetrics(defaultFont).getAscent();
		for(Pair<String, Pair<Format, Color>> entry : text) {
			g.setColor(entry.getValue().getValue());
			switch(entry.getValue().getKey()) {
				case ITALIC : g.setFont(defaultFont.deriveFont(Font.ITALIC));
					break;
				case BOLD : g.setFont(defaultFont.deriveFont(Font.BOLD));
					break;
				case BOLD_ITALIC : g.setFont(defaultFont.deriveFont(Font.BOLD | Font.ITALIC));
					break;
				case DEFAULT : g.setFont(defaultFont);
					break;
			}
			g.drawString(entry.getKey(), x, y);
			x += g.getFontMetrics(g.getFont()).stringWidth(entry.getKey());
		}
	}
}
