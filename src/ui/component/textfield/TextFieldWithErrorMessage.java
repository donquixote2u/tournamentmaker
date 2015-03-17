package ui.component.textfield;

import javax.swing.JTextField;
import javax.swing.text.Document;

public class TextFieldWithErrorMessage extends JTextField {
	private static final long serialVersionUID = -6609447346499765494L;
	private String errorMessage;
	
	public TextFieldWithErrorMessage() {
		super();
	}
	
	public TextFieldWithErrorMessage(int columns) {
		super(columns);
	}
	
	
	public TextFieldWithErrorMessage(String text) {
		super(text);
	}
	
	public TextFieldWithErrorMessage(String text, int columns) {
		super(text, columns);
	}
	
	public TextFieldWithErrorMessage(Document doc, String text, int columns) {
		super(doc, text, columns);
	}
	
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	
	public String getErrorMessage() {
		return errorMessage;
	}
}
