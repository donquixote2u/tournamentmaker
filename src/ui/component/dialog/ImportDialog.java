package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import ui.main.TournamentViewManager;

public abstract class ImportDialog extends JDialog {
	private static final long serialVersionUID = 2564561655115801740L;
	private TournamentViewManager manager;
	private boolean result;
	
	public ImportDialog(JFrame owner, TournamentViewManager manager, String title, final File file) {
		super(owner, title, true);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		this.manager = manager;
		try {
			if(file == null || !file.exists() || !file.canRead()) {
				result = false;
			}
			else {
				result = isValid(file);
			}
		}
		catch(Exception e) {
			result = false;
		}
		if(!result) {
			dispose();
			return;
		}
		getContentPane().setLayout(new BorderLayout());
		try {
			getContentPane().add(getDisplay(), BorderLayout.CENTER);
		}
		catch(Exception e) {
			result = false;
			dispose();
			return;
		}
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("Import");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				String result;
				try {
					result = validateImportData();
				}
				catch(Exception e) {
					result = e.getMessage();
					if(result == null || result.trim().isEmpty()) {
						result = "Encountered an unknown error.";
					}
				}
				setCursor(null);
				if(result != null && !result.trim().isEmpty()) {
					showResultDialog(result, "Error", false);
					return;
				}
				setEnabledForComponent(getContentPane(), false);
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				try {
					if(file.exists() && file.canRead()) {
						result = importData();
					}
					else {
						result = "Cannot open data file. Unable to import data.";
					}
				}
				catch(Exception e) {
					result = e.getMessage();
					if(result == null || result.trim().isEmpty()) {
						result = "Encountered an unknown error.";
					}
				}
				setCursor(null);
				dispose();
				showResultDialog(result, "Import Result", true);
				ImportDialog.this.manager.updateCurrentTab();
				ImportDialog.this.manager.modified();
			}
		});
		buttons.add(ok);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dispose();
			}
		});
		buttons.add(cancel);
		getContentPane().add(buttons, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(owner);
		setVisible(true);
	}
	
	public boolean getResult() {
		return result;
	}
	
	protected TournamentViewManager getTournamentViewManager() {
		return manager;
	}
	
	private void showResultDialog(String result, String title, boolean showPrint) {
		final JDialog dialog = new JDialog(this, title, true);
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new FlowLayout());
		JPanel panel = new JPanel(new BorderLayout());
		panel.setPreferredSize(new Dimension(600, 400));
		if(result == null || result.trim().isEmpty()) {
			result = "Successfully imported all data.";
		}
		final JTextArea text = new JTextArea(result);
		text.setEditable(false);
		panel.add(new JScrollPane(text), BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});
		buttons.add(ok);
		if(showPrint) {
			JButton print = new JButton("Print");
			print.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					manager.printComponent(text);
				}
			});
			buttons.add(print);
		}
		panel.add(buttons, BorderLayout.PAGE_END);
		dialog.getContentPane().add(panel);
		dialog.pack();
		dialog.setLocationRelativeTo(getOwner());
		dialog.setVisible(true);
	}
	
	private void setEnabledForComponent(Component component, boolean enabled) {
		if(component == null) {
			return;
		}
		component.setEnabled(enabled);
		if(component instanceof Container) {
			for(Component comp : ((Container) component).getComponents()) {
				setEnabledForComponent(comp, enabled);
			}
		}
	}
	
	protected abstract Component getDisplay();
	protected abstract boolean isValid(File file);
	protected abstract String importData();
	protected abstract String validateImportData();
}
