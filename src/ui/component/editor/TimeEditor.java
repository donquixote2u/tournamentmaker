package ui.component.editor;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import ui.util.GenericUtils;

public class TimeEditor extends GenericEditor {
	private static final long serialVersionUID = 5476076921288758381L;
	private JPanel panel;
	private Date date;
	
	public TimeEditor(JFrame owner) {
		super(owner, false);
	}
	
	@SuppressWarnings("unchecked")
	public Object getCellEditorValue() {
		int h = Integer.parseInt(((JComboBox<String>) panel.getComponent(0)).getSelectedItem().toString());
		int m = Integer.parseInt(((JComboBox<String>) panel.getComponent(2)).getSelectedItem().toString());
		if(h == 0 && m == 0) {
			return null;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.add(Calendar.HOUR_OF_DAY, h);
		cal.add(Calendar.MINUTE, m);
		return cal.getTime();
	}

	@SuppressWarnings("unchecked")
	protected void setEditorValue(JTable table, Object value, int row, int column) {
		((JComboBox<String>) panel.getComponent(0)).setSelectedIndex(0);
		((JComboBox<String>) panel.getComponent(2)).setSelectedIndex(0);
		if(value instanceof Date) {
			long ms = ((Date) value).getTime() - date.getTime();
			int numberOfHours = 0;
			long hour = 3600000;
			while(ms > hour) {
				ms -= hour;
				++numberOfHours;
			}
			int numberOfMins = (int) (ms / 60000);
			if(numberOfMins == 60) {
				++numberOfHours;
				numberOfMins = 0;
			}
			((JComboBox<String>) panel.getComponent(0)).setSelectedIndex(numberOfHours);
			((JComboBox<String>) panel.getComponent(2)).setSelectedIndex(numberOfMins);
		}
	}
	
	protected String generateButtonText(Object value) {
		return GenericUtils.getDuration(date, value);
	}
	
	protected Component getDisplay() {
		panel = new JPanel(new FlowLayout());
		JComboBox<String> hour = new JComboBox<String>();
		for(int i = 0; i < 24; ++i) {
			hour.addItem(String.valueOf(i));
		}
		hour.setFocusable(false);
		panel.add(hour);
		panel.add(new JLabel(":"));
		JComboBox<String> min = new JComboBox<String>();
		for(int i = 0; i < 60; ++i) {
			if(i < 10) {
				min.addItem("0" + String.valueOf(i));
			}
			else {
				min.addItem(String.valueOf(i));
			}
		}
		min.setFocusable(false);
		panel.add(min);
		return panel;
	}
	
	protected boolean updateDisplay() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		date = cal.getTime();
		return false;
	}
}
