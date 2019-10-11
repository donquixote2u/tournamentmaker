package ui;

import images.Images;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import ui.main.Loader;
import ui.main.LoaderData;
import ui.main.TournamentUI;

public class Main {
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowLoader();
			}
		});
	}
	
	private static void createAndShowLoader() {
		final JWindow loadingWindow = new JWindow();
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			UIManager.put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
			Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
			JPanel root = new JPanel();
			root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
			JLabel logo = new JLabel(new ImageIcon(Images.LOGO));
			root.add(logo);
			final JProgressBar progress = new JProgressBar();
			progress.setStringPainted(true);
			progress.setString("Loading " + TournamentUI.APP_NAME + " - 0%");
			root.add(progress);
			root.setPreferredSize(new Dimension(logo.getPreferredSize().width, logo.getPreferredSize().height + progress.getPreferredSize().height));
			loadingWindow.getContentPane().add(root);
			loadingWindow.pack();
			loadingWindow.setLocation((dim.width - loadingWindow.getWidth()) / 2, (dim.height - loadingWindow.getHeight()) / 2);
			loadingWindow.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			loadingWindow.setVisible(true);
			final Loader loader = new Loader("data");
			loader.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent event) {
					if(!loader.isDone()) {
						int value = loader.getProgress();
						progress.setValue(value);
						progress.setString("Loading " + TournamentUI.APP_NAME + " - " + value + "%");
					}
					else if("state".equals(event.getPropertyName()) && SwingWorker.StateValue.DONE == event.getNewValue()) {
						try {
							LoaderData data = loader.get();
							TournamentUI tournament = new TournamentUI(data.getEvents(), data.getModifiers());
							tournament.pack();
							loadingWindow.dispose();
							tournament.setVisible(true);
						}
						catch (Exception e) {
							loadingWindow.dispose();
        						JOptionPane.showMessageDialog(null, TournamentUI.APP_NAME + " has encountered a bad exception:\n" +  e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
					}
				}
			});
			loader.execute();
		}
		catch(HeadlessException | ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException e) {
			loadingWindow.dispose();
			JOptionPane.showMessageDialog(null, TournamentUI.APP_NAME + " has encountered a fatal exception:\n" +  e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
	}
}
