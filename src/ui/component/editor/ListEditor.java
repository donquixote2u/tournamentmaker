package ui.component.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

public class ListEditor extends GenericEditor {
	private static final long serialVersionUID = -539967767039369396L;
	private String emptyValueMessage;
	private List<String> values;
	private JPanel panel;
	private Set<String> selectedStrings;
	private boolean displayChanged;
	
	public ListEditor(JFrame owner, String emptyValueMessage) {
		super(owner, false);
		this.emptyValueMessage = emptyValueMessage;
		values = new ArrayList<String>();
		selectedStrings = new HashSet<String>();
        buildPanel();
	}
	
	public void setValues(Collection<String> values) {
		if(values == null) {
			this.values.clear();
			buildPanel();
			return;
		}
		if(this.values.size() != values.size()) {
			this.values.clear();
			this.values.addAll(values);
			buildPanel();
		}
		boolean same = true;
		for(String value : values) {
			if(!this.values.contains(value)) {
				same = false;
				break;
			}
		}
		if(!same) {
			this.values.clear();
			this.values.addAll(values);
			buildPanel();
		}
	}
	
	public Object getCellEditorValue() {
		selectedStrings.clear();
		if(!values.isEmpty()) {
			for(Component comp : panel.getComponents()) {
				JCheckBox checkBox = (JCheckBox) comp;
				if(checkBox.isSelected()) {
					selectedStrings.add(checkBox.getText());
				}
			}
		}
		return selectedStrings;
	}
	
	@SuppressWarnings("unchecked")
	protected void setEditorValue(JTable table, Object value, int row, int column) {
		selectedStrings.clear();
		if(value instanceof Set) {
			selectedStrings.addAll((Set<String>) value);
		}
		if(values.isEmpty()) {
			return;
		}
		for(Component comp : panel.getComponents()) {
			JCheckBox checkBox = (JCheckBox) comp;
			checkBox.setSelected(selectedStrings.contains(checkBox.getText()));
		}
	}
	
	protected Component getDisplay() {
		panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
	}
	
	protected boolean updateDisplay() {
		if(displayChanged) {
			displayChanged = false;
			return true;
		}
		return false;
	}
	
	private void buildPanel() {
		panel.removeAll();
		if(values.isEmpty()) {
			panel.add(new JLabel(emptyValueMessage));
		}
		else {
			for(String value : values) {
				JCheckBox checkBox = new JCheckBox(value);
				checkBox.setFocusable(false);
				panel.add(checkBox);
			}
		}
		displayChanged = true;
	}
}
