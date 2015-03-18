package ui.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.text.SimpleDateFormat;
import java.util.Date;

public class GenericUtils {
	public static String html(String value) {
		return "<html>" + value + "</html>";
	}
	
	public static String bold(String value) {
		return "<b>" + value + "</b>";
	}
	
	public static String italics(String value) {
		return "<i>" + value + "</i>";
	}
	
	public static String color(String value, String color) {
		return "<font color=" + color + ">" + value + "</font>";
	}
	
	public static String dateToString(Date date, String format) {
		if(date == null) {
			return null;
		}
		return (new SimpleDateFormat(format)).format(date);
	}
	
	public static Date stringToDate(String string, String format) {
		try {
			return (new SimpleDateFormat(format).parse(string));
		}
		catch(Exception e) {
			return null;
		}
	}
	
	public static String getDuration(Object obj1, Object obj2) {
		if(!(obj1 instanceof Date) || !(obj2 instanceof Date)) {
			return "0:00";
		}
		Date start = (Date) obj1;
		Date end = (Date) obj2;
		long length = end.getTime() - start.getTime();
		if(length < 0) {
			return "0:00";
		}
		int numberOfHours = 0;
		long hour = 3600000;
		while(length > hour) {
			length -= hour;
			++numberOfHours;
		}
		int min = (int) (length / 60000);
		if(min == 60) {
			++numberOfHours;
			min = 0;
		}
		return numberOfHours + ":" + (min < 10 ? "0" + min : min);
	}
	
	public static Point ensurePointIsDisplayable(Point point, Component comp) {
		Point location = new Point(point);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
		if(location.x < -insets.left) {
			location.x = insets.left;
		}
		if(location.x + comp.getWidth() > screenSize.width - insets.right) {
			location.x = screenSize.width - comp.getWidth();
		}
		if(location.y < -insets.top) {
			location.y = insets.top;
		}
		if(location.y + comp.getHeight() > screenSize.height - insets.bottom) {
			location.y = screenSize.height - insets.bottom - comp.getHeight();
		}
		return location;
	}
	
	public static String extractValueFromString(String value, String data) {
		if(value == null || value.isEmpty() || data == null || value.length() > data.length()) {
			return null;
		}
		String newData = data.replaceFirst("(^|\\P{L})(?i)\\Q" + value + "\\E(\\P{L}|$)", " ");
		if(data.equals(newData)) {
			return null;
		}
		return newData;
	}
	
	public static GridBagConstraints createGridBagConstraint(int gridx, int gridy, double weightx) {
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = gridx;
		c.gridy = gridy;
		c.insets = new Insets(5, 5, 5, 5);
		c.weightx = weightx;
		c.fill = GridBagConstraints.HORIZONTAL;
		return c;
	}
}
