package ui.component.textfield;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicComboBoxEditor;

public class TextFieldWithDropDown extends JComboBox<String> {
	private static final long serialVersionUID = 4832497916645839969L;
	private String value;
	
	public TextFieldWithDropDown() {
		super();
		super.setEditable(true);
		value = "";
		addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent event) {
				if(event.getStateChange() != ItemEvent.SELECTED || getSelectedIndex() == -1) {
					return;
				}
				JTextField editor = (JTextField) getEditor().getEditorComponent();
				String selected = (String) event.getItem();
				if(selected == null) {
					return;
				}
				selected = "[" + selected + "]";
				int start = Math.max(editor.getSelectionStart(), 0);
				int end = Math.min(editor.getSelectionEnd(), value.length()); 
				value = value.substring(0, start) + selected + value.substring(end, value.length());
				editor.setText(value);
				editor.setCaretPosition(start + selected.length());
				setSelectedIndex(-1);
			}
		});
		setEditor(new BasicComboBoxEditor() {
			public void setItem(Object item) {
				value = editor.getText();
			}
		});
		putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);
	}
	
	public TextFieldWithDropDown(Iterable<? extends String> items) {
		this();
		if(items == null) {
			throw new IllegalArgumentException("items can not be null");
		}
		for(String item : items) {
			addItem(item);
		}
	}
	
	public void addItem(String item) {
		super.addItem(item);
		value = "";
		((JTextField) getEditor().getEditorComponent()).setText(value);
	}
	
	public String getText() {
		return value != null ? value : "";
	}
	
	public void setEditable(boolean editable) {}
}
