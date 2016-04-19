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
	private Set<String> selectedStrings, disabledStrings;
	private boolean displayChanged;
	
	public ListEditor(JFrame owner, String emptyValueMessage) {
		super(owner, false);
		this.emptyValueMessage = emptyValueMessage;
		values = new ArrayList<String>();
		selectedStrings = new HashSet<String>();
		disabledStrings = new HashSet<String>();
        buildPanel();
	}
	
	public void setValues(Collection<String> values) {
		if(values == null) {
			if(this.values.isEmpty()) {
				return;
			}
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
	
	public void setDisabledStrings(Collection<String> disabledStrings) {
		boolean same = true;
		if(disabledStrings == null) {
			if(this.disabledStrings.isEmpty()) {
				return;
			}
			same = false;
		}
		else if(this.disabledStrings.size() != disabledStrings.size()) {
			same = false;
		}
		else {
			for(String disabled : disabledStrings) {
				if(!this.disabledStrings.contains(disabled)) {
					same = false;
					break;
				}
			}
		}
		if(!same) {
			this.disabledStrings.clear();
			if(disabledStrings != null) {
				this.disabledStrings.addAll(disabledStrings);
			}
			for(Component comp : panel.getComponents()) {
				if(!(comp instanceof JCheckBox)) {
					continue;
				}
				JCheckBox checkBox = (JCheckBox) comp;
				checkBox.setEnabled(!this.disabledStrings.contains(checkBox.getText()));
			}
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
				if(disabledStrings.contains(value)) {
					checkBox.setEnabled(false);
				}
				panel.add(checkBox);
			}
		}
		displayChanged = true;
	}
}
