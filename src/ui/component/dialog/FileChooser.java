package ui.component.dialog;

import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileFilter;

import ui.util.GenericUtils;

public class FileChooser extends JFileChooser {
	private static final long serialVersionUID = 3073773973783634966L;
	private static final String OPEN = "Open";
	private static final String SAVE = "Save";
	private static final String SAVE_AS = "Save As";
	private JDialog dialog;
	private JFrame owner;
	private int returnValue;
	
	public FileChooser(JFrame owner) {
		super();
		this.owner = owner;
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		setMultiSelectionEnabled(false);
		setAcceptAllFileFilterUsed(true);
	}
	
	public FileChooser(JFrame owner, final String fileExtension, final String description) {
		super();
		this.owner = owner;
		setFileSelectionMode(JFileChooser.FILES_ONLY);
		setMultiSelectionEnabled(false);
		setAcceptAllFileFilterUsed(false);
		setFileFilter(new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().toLowerCase().endsWith(fileExtension);
			}

			public String getDescription() {
				return description;
			}
		});
	}
	
	public void addFileFilter(final String fileExtension, final String description, boolean selectFileFilter) {
		FileFilter selected = getFileFilter();
		boolean acceptAllFiles = isAcceptAllFileFilterUsed();
		if(acceptAllFiles) {
			setAcceptAllFileFilterUsed(false);
		}
		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				return file.isDirectory() || file.getName().toLowerCase().endsWith(fileExtension);
			}

			public String getDescription() {
				return description;
			}
		};
		addChoosableFileFilter(filter);
		if(acceptAllFiles) {
			setAcceptAllFileFilterUsed(true);
		}
		if(selectFileFilter) {
			setFileFilter(filter);
		}
		else if(selected != null) {
			setFileFilter(selected);
		}
	}
	
	public int showDialog(boolean open) {
		if(dialog != null) {
			return ERROR_OPTION;
		}
		if(open) {
			setDialogTitle(OPEN);
			setApproveButtonText(OPEN);
		}
		else {
			setDialogTitle(SAVE_AS);
			setApproveButtonText(SAVE);
		}
		JDialog dialog = createDialog(owner);
		rescanCurrentDirectory();
		returnValue = ERROR_OPTION;
		dialog.setVisible(true);
		return returnValue;
	}
	
	public void approveSelection() {
		closeDialog();
		returnValue = APPROVE_OPTION;
		fireActionPerformed(APPROVE_SELECTION);
	}
	
	public void cancelSelection() {
		closeDialog();
		returnValue = CANCEL_OPTION;
		fireActionPerformed(CANCEL_SELECTION);
	}
	
	protected JDialog createDialog(Component parent) {
		dialog = super.createDialog(parent);
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				closeDialog();
			}
		});
		dialog.setLocation(GenericUtils.ensurePointIsDisplayable(parent.getLocation(), dialog));
		return dialog;
	}
	
	private void closeDialog() {
		if(dialog == null) {
			return;
		}
		returnValue = CANCEL_OPTION;
		dialog.setVisible(false);
		dialog.getContentPane().removeAll();
		dialog.dispose();
		dialog = null;
	}
}
