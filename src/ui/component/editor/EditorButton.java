package ui.component.editor;

import java.awt.Component;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public abstract class EditorButton extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 6057836324466821953L;
	private JButton button;
	
	public EditorButton() {
		button = new JButton();
		button.setBorderPainted(false);
		button.addActionListener(getButtonAction());
	}

	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		setEditorValue(table, value, row, column);
		button.setText(generateButtonText(value));
		return button;
	}
	
	public abstract Object getCellEditorValue();
	protected abstract ActionListener getButtonAction();
	protected abstract String generateButtonText(Object value);
	protected abstract void setEditorValue(JTable table, Object value, int row, int column);
}
