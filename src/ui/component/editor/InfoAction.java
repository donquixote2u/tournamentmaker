package ui.component.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;

public abstract class InfoAction extends EditorButton {
	private static final long serialVersionUID = 5420063041795674896L;
	private JFrame owner;
	private Object value;
	
	public InfoAction(final JFrame owner) {
		this.owner = owner;
	}
	
	public Object getCellEditorValue() {
		return value;
	}
	
	protected ActionListener getButtonAction() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JOptionPane.showMessageDialog(owner, generateActionMessage(value), generateActionTitle(value), JOptionPane.INFORMATION_MESSAGE);
				fireEditingCanceled();
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
