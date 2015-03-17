package ui.component.label;

import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class MessageLabel extends JLabel {
	private static final long serialVersionUID = -835755086116023011L;
	public static final String EMPTY_TEXT = " ";
	public static final Color SUCCESS_BG = new Color(224, 242, 192);
	public static final Color SUCCESS_FG = new Color(85, 138, 29);
	public static final Color WARNING_BG = new Color(253, 239, 180);
	public static final Color WARNING_FG = new Color(155, 95, 1);
	public static final Color ERROR_BG = new Color(251, 185, 185);
	public static final Color ERROR_FG = new Color(207, 0, 0);
	private Color normalBg, normalFg;
	
	public MessageLabel() {
		super(EMPTY_TEXT);
		normalBg = getBackground();
		normalFg = getForeground();
		setOpaque(true);
		setHorizontalAlignment(SwingConstants.CENTER);
	}
	
	public void reset() {
		setText(EMPTY_TEXT);
		setBackground(normalBg);
		setForeground(normalFg);
	}
	
	public void success(String message) {
		setText(message);
		setBackground(SUCCESS_BG);
		setForeground(SUCCESS_FG);
	}
	
	public void warn(String message) {
		setText(message);
		setBackground(WARNING_BG);
		setForeground(WARNING_FG);
	}
	
	public void error(String message) {
		setText(message);
		setBackground(ERROR_BG);
		setForeground(ERROR_FG);
	}
}
