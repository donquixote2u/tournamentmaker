package ui.component.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;

public abstract class ButtonAction extends EditorButton {
	private static final long serialVersionUID = -6372407026924275394L;
	private JFrame owner;
	private Object value;
	
	public ButtonAction(final JFrame owner) {
		this.owner = owner;
	}
	
	public Object getCellEditorValue() {
		return value;
	}
	
	protected ActionListener getButtonAction() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				int answer = JOptionPane.YES_OPTION;
				String message = generateActionMessage(value);
				if(message != null) {
					answer = JOptionPane.showConfirmDialog(owner, message, generateActionTitle(value), JOptionPane.YES_NO_OPTION);
				}
				if(answer == JOptionPane.YES_OPTION) {
					fireEditingStopped();
				}
				else {
					fireEditingCanceled();
				}
			}
		};
	}
	
	protected String generateButtonText(Object value) {
		return "";
	}
	
	protected void setEditorValue(JTable table, Object value, int row, int column) {
		this.value = value;
	}
	
	protected abstract String generateActionMessage(Object value);
	protected abstract String generateActionTitle(Object value);
}
