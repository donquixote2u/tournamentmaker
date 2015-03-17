package ui.component.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.MouseInfo;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;

import ui.component.label.MessageLabel;
import ui.util.GenericUtils;

public abstract class GenericEditor extends EditorButton {
	private static final long serialVersionUID = 7965821087507389780L;
	private JDialog editor;
	private boolean firstRun;
	
	public GenericEditor(final JFrame owner, boolean modal) {
		editor = new JDialog(owner);
		editor.setUndecorated(true);
		editor.getContentPane().setLayout(new BorderLayout());
		editor.getContentPane().add(getDisplay(), BorderLayout.CENTER);
		if(modal) {
			editor.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
			final MessageLabel message = new MessageLabel();
			editor.getContentPane().add(message, BorderLayout.PAGE_START);
			JPanel buttons = new JPanel(new FlowLayout());
			JButton ok = new JButton("OK");
			ok.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					if(validateEditorValue()) {
						message.reset();
						fireEditingStopped();
					}
					else {
						message.error("Invalid values selected.");
					}
				}
			});
			buttons.add(ok);
			JButton cancel = new JButton("Cancel");
			cancel.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent event) {
					message.reset();
					fireEditingCanceled();
				}
			});
			buttons.add(cancel);
			editor.getContentPane().add(buttons, BorderLayout.PAGE_END);
			editor.addWindowListener(new WindowAdapter() {
				public void windowActivated(WindowEvent event) {
					owner.getGlassPane().setVisible(true);
				}
				
				public void windowClosed(WindowEvent event) {
					owner.getGlassPane().setVisible(false);
				}
			});
		}
		else {
			editor.setModalityType(Dialog.ModalityType.MODELESS);
			editor.setAutoRequestFocus(true);
			editor.addFocusListener(new FocusAdapter() {
				public void focusLost(FocusEvent event) {
					fireEditingStopped();
				}
			});
			editor.addKeyListener(new KeyAdapter() {
				public void keyTyped(KeyEvent event) {
					if(event.getKeyChar() == KeyEvent.VK_ENTER) {
						event.consume();
						fireEditingStopped();
					}
					else if(event.getKeyChar() == KeyEvent.VK_ESCAPE) {
						event.consume();
						fireEditingCanceled();
					}
				}
			});
		}
		firstRun = true;
	}
	
	protected final void fireEditingStopped() {
		editor.dispose();
		super.fireEditingStopped();
	}
	
	protected final void fireEditingCanceled() {
		editor.dispose();
		super.fireEditingCanceled();
	}
	
	protected ActionListener getButtonAction() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(updateDisplay() || firstRun) {
					firstRun = false;
					editor.pack();
				}
				editor.setLocation(GenericUtils.ensurePointIsDisplayable(MouseInfo.getPointerInfo().getLocation(), editor));
		        editor.setVisible(true);
			}
        };
	}
	
	protected String generateButtonText(Object value) {
		return String.valueOf(value);
	}
	
	protected boolean validateEditorValue() {
		return true;
	}
	
	public abstract Object getCellEditorValue();
	protected abstract void setEditorValue(JTable table, Object value, int row, int column);
	protected abstract Component getDisplay();
	protected abstract boolean updateDisplay();
}
