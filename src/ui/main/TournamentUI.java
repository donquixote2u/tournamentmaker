package ui.main;

import images.Images;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileFilter;

import ui.component.dialog.CSVImportDialog;
import ui.component.dialog.FileChooser;
import ui.component.dialog.GenericImportDialog;
import ui.component.dialog.ImportDialog;
import ui.component.dialog.TournamentImportDialog;
import ui.component.label.MessageLabel;
import ui.component.textfield.TextFieldWithErrorMessage;
import ui.util.GenericUtils;
import ui.util.PrintUtils;
import ui.util.Wrapper;
import data.event.Event;
import data.player.Player;
import data.team.BaseModifierTeam;
import data.team.Team;
import data.team.modifier.CreatableTeamModifier;
import data.team.modifier.TeamModifier;
import data.tournament.Tournament;
import data.tournament.TournamentUtils;
import ui.component.dialog.AutoPairDialog;

@SuppressWarnings("serial")
public class TournamentUI extends JFrame {
	private static final long serialVersionUID = -1627827505359316413L;
	public static final String APP_NAME = "Tournament Maker";
	public static final String NEW_DISPLAY_TEXT = "New...";
	private static final String FILE_EXTENSION = ".tmdat";
	private static final int MAX_NUM_COURTS = 64;
	private static final int MAX_NUM_TEAMS_IN_EVENT = 128;
	private Map<String, Class<? extends Event>> events;
	private Map<String, Class<? extends TeamModifier>> modifiers;
	private FileChooser fileChooser, importFileChooser;
	private JButton edit, close, save, saveAs, addEvent, addPlayer, addTeam, autoPair, importData, print;
	private TournamentViewManager tournamentViewManager;
	
	public TournamentUI(Map<String, Class<? extends Event>> events, Map<String, Class<? extends TeamModifier>> modifiers) {
		// setting the window title
		super(APP_NAME);
		// checking the list of events and modifiers
		if(events == null || events.isEmpty()) {
			throw new RuntimeException("no events found");
		}
		if(modifiers == null || modifiers.isEmpty()) {
			throw new RuntimeException("no modifiers found");
		}
		this.events = events;
		this.modifiers = modifiers;
		// setting the screen size and location
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
		setPreferredSize(new Dimension(Math.min(1024, dim.width - insets.left - insets.right), Math.min(768, dim.height - insets.top - insets.bottom)));
		setLocation((dim.width - getPreferredSize().width - insets.left - insets.right) / 2, (dim.height - getPreferredSize().height - insets.top - insets.bottom) / 2);
		setMinimumSize(new Dimension(800, 600));
		// setting the icon images
		ArrayList<Image> iconImages = new ArrayList<Image>();
		iconImages.add(Toolkit.getDefaultToolkit().getImage(Images.LOGO_16));
		iconImages.add(Toolkit.getDefaultToolkit().getImage(Images.LOGO_24));
		iconImages.add(Toolkit.getDefaultToolkit().getImage(Images.LOGO_32));
		iconImages.add(Toolkit.getDefaultToolkit().getImage(Images.LOGO_48));
		setIconImages(iconImages);
		// setting up the file chooser that is used for loading and saving
		fileChooser = new FileChooser(this, FILE_EXTENSION, "Tournament Maker Data File (*" + FILE_EXTENSION + ")");
		// setting up the file chooser for the data import
		importFileChooser = new FileChooser(this);
		importFileChooser.setAcceptAllFileFilterUsed(false);
		importFileChooser.addChoosableFileFilter(new FileFilter() {
			public boolean accept(File file) {
				String fileName = file.getName().toLowerCase();
				return file.isDirectory() || fileName.endsWith(CSVImportDialog.FILE_EXTENSION) || fileName.endsWith(FILE_EXTENSION);
			}

			public String getDescription() {
				return "CSV File (*" + CSVImportDialog.FILE_EXTENSION + ") and Tournament Maker Data File (*" + FILE_EXTENSION + ")";
			}
		});
		importFileChooser.setAcceptAllFileFilterUsed(true);
		// creating the toolbar and the tournament ui component
		createToolbar();
		createTournamentView();
		setEnabledForTournamentButtons(false);
		// set up a window listener to check for unsaved changes before exiting
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				if(checkForSaveAction()) {
					checkForMultiDayTournament();
					tournamentViewManager.close();
					fileChooser = null;
					importFileChooser = null;
					dispose();
				}
			}
		});
		// set up the glass pane for modal popups
		JPanel glassPane = new JPanel(null) {
			Color fill = new Color(0xB4F0F0F0, true);
			
			protected void paintComponent(Graphics g) {
				g.setColor(fill);
				g.fillRect(0, 0, getWidth(), getHeight());
			}
		};
		glassPane.setFocusable(false);
		glassPane.setEnabled(false);
		glassPane.setOpaque(false);
		glassPane.setVisible(false);
		setGlassPane(glassPane);
	}
	
	public void setEnabledForTournamentButtons(boolean enabled) {
		edit.setEnabled(enabled);
		close.setEnabled(enabled);
		save.setEnabled(enabled);
		saveAs.setEnabled(enabled);
		addEvent.setEnabled(enabled);
		addPlayer.setEnabled(enabled);
		addTeam.setEnabled(enabled);
                autoPair.setEnabled(enabled); // added 4/19 bvw
		importData.setEnabled(enabled);
		print.setEnabled(enabled);
	}
	
	public void print(Component component) {
		try {
			PrintUtils.printComponent(this, component, tournamentViewManager.getTournament().getUseDefaultPrinter());
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private void checkForMultiDayTournament() {
		if(tournamentViewManager.getTournament() == null) {
			return;
		}
		if(tournamentViewManager.isModified()) {
			return;
		}
		if(!tournamentViewManager.getTournament().isCloseForMultiDayTournament()) {
			return;
		}
		int answer = JOptionPane.showConfirmDialog(TournamentUI.this, "Would you like to reset the check in status for all the players before exiting?", 
				"Check Out Players", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if(answer == JOptionPane.YES_OPTION) {
			tournamentViewManager.getTournament().checkOutAllPlayers();
			save(true);
		}
	}
	
	private boolean checkForSaveAction() {
		if(tournamentViewManager.resetModified()) {
			int answer = JOptionPane.showConfirmDialog(this, "Do you want to save the changes you made?", APP_NAME, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
			if(answer == JOptionPane.OK_OPTION) {
				if(save(true)) {
					return true;
				}
			}
			else if(answer == JOptionPane.NO_OPTION) {
				tournamentViewManager.modified();
				return true;
			}
			tournamentViewManager.modified();
			return false;
		}
		return true;
	}
	
	private boolean save(boolean useStoredFilePath) {
		Tournament tournament = tournamentViewManager.getTournament();
		if(tournament == null) {
			return false;
		}
		if(!useStoredFilePath || tournament.getFilePath() == null || !(new File(tournament.getFilePath()).exists())) {
			String filePath = openFileSelectionDialog(false);
			if(filePath == null) {
				return false;
			}
			tournament.setFilePath(filePath);
		}
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			File file = new File(tournament.getFilePath());
			(new File(file.getParent())).mkdirs();
			ObjectOutput output = null;
			try {
				tournament.updateVersion();
				output = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file, false)));
				output.writeObject(tournament);
				output.close();
				output = null;
			}
			catch(Exception e) {
				setCursor(null);
				JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			}
			finally {
				if(output != null) {
					output.close();
				}
			}
		}
		catch(Exception e) {
			setCursor(null);
			JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		tournamentViewManager.resetModified();
		setCursor(null);
		return true;
	}
	
	private String openFileSelectionDialog(boolean open) {
		Tournament tournament = tournamentViewManager.getTournament();
		if(tournament != null && tournament.getFilePath() != null) {
			fileChooser.setSelectedFile(new File(tournament.getFilePath()));
		}
		if(fileChooser.showDialog(open) == FileChooser.APPROVE_OPTION) {
			try {
				String filePath = fileChooser.getSelectedFile().getCanonicalPath();
				if(!filePath.toLowerCase().endsWith(FILE_EXTENSION)) {
					filePath += FILE_EXTENSION;
				}
				return filePath;
			}
			catch(IOException e) {
				// shouldn't ever reach here
				JOptionPane.showMessageDialog(this, "Error occurred while selecting a file. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		return null;
	}
	
	private void openTournamentDialog(final boolean edit) {
		final JDialog dialog = new JDialog(this, edit ? "Edit Tournament" : "New Tournament");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new BorderLayout());
		final MessageLabel message = new MessageLabel();
		dialog.add(message, BorderLayout.PAGE_START);
		JPanel basic = new JPanel(new GridBagLayout());
		basic.add(new JLabel("Name"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		final TextFieldWithErrorMessage name = new TextFieldWithErrorMessage(15);
		name.setErrorMessage("Tournament name can not be empty.");
		final Border defaultBorder = name.getBorder();
		name.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					if(!((JTextField) input).getText().trim().isEmpty()) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		if(edit) {
			name.setText(tournamentViewManager.getTournament().getName());
		}
		basic.add(name, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		basic.add(new JLabel("Levels (Comma Separated List)"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		final TextFieldWithErrorMessage levels = new TextFieldWithErrorMessage(15);
		if(edit) {
			String levelString = "";
			for(String level : tournamentViewManager.getTournament().getLevels()) {
				levelString += level + ", ";
			}
			if(!levelString.isEmpty()) {
				levelString = levelString.substring(0, levelString.length() - 2);
			}
			levels.setText(levelString);
			levels.setEnabled(false);
		}
		basic.add(levels, GenericUtils.createGridBagConstraint(1, 1, 0.7));
		basic.add(new JLabel("Number Of Courts"), GenericUtils.createGridBagConstraint(0, 2, 0.3));
		final TextFieldWithErrorMessage courts = new TextFieldWithErrorMessage(15);
		courts.setErrorMessage("Number of courts must be between 1 and " + MAX_NUM_COURTS + " (inclusive).");
		courts.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					int number = Integer.parseInt(((JTextField) input).getText().trim());
					if(number > 0 && number <= MAX_NUM_COURTS) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		if(edit) {
			courts.setText(Integer.toString(tournamentViewManager.getTournament().getCourts().size()));
			courts.setEnabled(false);
		}
		basic.add(courts, GenericUtils.createGridBagConstraint(1, 2, 0.7));
		basic.add(new JLabel("Rest Between Matches (Minutes)"), GenericUtils.createGridBagConstraint(0, 3, 0.3));
		final TextFieldWithErrorMessage rest = new TextFieldWithErrorMessage(15);
		rest.setErrorMessage("Rest between matches must be a number.");
		rest.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					String text = ((JTextField) input).getText().trim();
					if(text.isEmpty() || Integer.parseInt(text) >= 0) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		if(edit) {
			rest.setText(Integer.toString(tournamentViewManager.getTournament().getTimeBetweenMatches()));
		}
		basic.add(rest, GenericUtils.createGridBagConstraint(1, 3, 0.7));
		JPanel advanced = new JPanel(new GridBagLayout());
		final JCheckBox playerStatus = new JCheckBox("Allow match start without player check in");
		if(edit) {
			playerStatus.setSelected(tournamentViewManager.getTournament().getIgnorePlayerStatus());
		}
		advanced.add(playerStatus, GenericUtils.createGridBagConstraint(0, 0, 0.5));
		final JCheckBox defaultPrinter = new JCheckBox("Print directly to default printer");
		if(edit) {
			defaultPrinter.setSelected(tournamentViewManager.getTournament().getUseDefaultPrinter());
		}
		advanced.add(defaultPrinter, GenericUtils.createGridBagConstraint(0, 1, 0.5));
		final JCheckBox autoPrint = new JCheckBox("Automatically print match after match start");
		if(edit) {
			autoPrint.setSelected(tournamentViewManager.getTournament().getAutoPrintMatches());
		}
		advanced.add(autoPrint, GenericUtils.createGridBagConstraint(0, 2, 0.5));
		final JCheckBox showMatches = new JCheckBox("Show all matches");
		if(edit) {
			showMatches.setSelected(tournamentViewManager.getTournament().getShowAllMatches());
		}
		advanced.add(showMatches, GenericUtils.createGridBagConstraint(1, 0, 0.5));
		final JCheckBox disablePooling = new JCheckBox("Draw all matches from a bracket together");
		if(edit) {
			disablePooling.setSelected(tournamentViewManager.getTournament().getDisableBracketPooling());
		}
		advanced.add(disablePooling, GenericUtils.createGridBagConstraint(1, 1, 0.5));
		final JCheckBox disableScheduler = new JCheckBox("Disable the scheduler");
		if(edit) {
			disableScheduler.setSelected(tournamentViewManager.getTournament().getDisableScheduler());
		}
		advanced.add(disableScheduler, GenericUtils.createGridBagConstraint(1, 2, 0.5));
		final JTabbedPane root = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		root.add("Basic", basic);
		root.add("Advanced", advanced);
		dialog.getContentPane().add(root, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				message.reset();
				root.setSelectedIndex(0);
				if(!name.getInputVerifier().verify(name)) {
					name.requestFocus();
					return;
				}
				if(!courts.getInputVerifier().verify(courts)) {
					courts.requestFocus();
					return;
				}
				if(!rest.getInputVerifier().verify(rest)) {
					rest.requestFocus();
					return;
				}
				String tName = name.getText().trim();
				List<String> tLevels = new ArrayList<String>();
				for(String string : levels.getText().split(",")) {
					string = string.trim();
					if(!string.isEmpty() && !tLevels.contains(string)) {
						tLevels.add(string);
					}
				}
				int tCourts = Integer.parseInt(courts.getText().trim());
				int tRest = 0;
				String restText = rest.getText().trim();
				if(!restText.isEmpty()) {
					tRest = Integer.parseInt(restText);
				}
				dialog.dispose();
				if(edit) {
					tournamentViewManager.getTournament().setName(tName);
					tournamentViewManager.getTournament().setTimeBetweenMatches(tRest);
					tournamentViewManager.getTournament().setIgnorePlayerStatus(playerStatus.isSelected());
					tournamentViewManager.getTournament().setShowAllMatches(showMatches.isSelected());
					tournamentViewManager.getTournament().setUseDefaultPrinter(defaultPrinter.isSelected());
					tournamentViewManager.getTournament().setAutoPrintMatches(autoPrint.isSelected());
					tournamentViewManager.getTournament().setDisableBracketPooling(disablePooling.isSelected());
					tournamentViewManager.getTournament().setDisableScheduler(disableScheduler.isSelected());
					tournamentViewManager.updateTournament();
				}
				else {
					Tournament tournament = new Tournament(tName, tLevels, tCourts, tRest);
					tournament.setIgnorePlayerStatus(playerStatus.isSelected());
					tournament.setShowAllMatches(showMatches.isSelected());
					tournament.setUseDefaultPrinter(defaultPrinter.isSelected());
					tournament.setAutoPrintMatches(autoPrint.isSelected());
					tournament.setDisableBracketPooling(disablePooling.isSelected());
					tournament.setDisableScheduler(disableScheduler.isSelected());
					tournamentViewManager.setTournament(tournament);
					TournamentUtils.resetMatchIndex();
				}
			}
		});
		buttons.add(ok);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
			}
		});
		buttons.add(cancel);
		dialog.getContentPane().add(buttons, BorderLayout.PAGE_END);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}
	
	public Event openEventDialog(final Event oldEvent) {
		final Wrapper<Event> newEvent = new Wrapper<Event>();
		if(!checkForExistingTeamTypes()) {
			return newEvent.getValue();
		}
		final JDialog dialog = new JDialog(this, oldEvent == null ? "New Event" : "Edit Event");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new BorderLayout());
		final MessageLabel message = new MessageLabel();
		dialog.add(message, BorderLayout.PAGE_START);
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Type Of Event"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		List<String> types = new ArrayList<String>(events.keySet());
		Collections.sort(types);
		final JComboBox<String> type = new JComboBox<String>(types.toArray(new String[0]));
		panel.add(type, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		panel.add(new JLabel("Type Of Team"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		types = tournamentViewManager.getTournament().getTeamTypes();
		Collections.sort(types);
		types.add(NEW_DISPLAY_TEXT);
		final JComboBox<String> teamType = new JComboBox<String>(types.toArray(new String[0]));
		teamType.addActionListener(new ActionListener() {
			private String previouslySelectedType = teamType.getItemAt(0);
			public void actionPerformed(ActionEvent event) {
				String type = (String) teamType.getSelectedItem();
				if(NEW_DISPLAY_TEXT.equals(type)) {
					String team = openNewTeamTypeDialog();
					if(team != null) {
						previouslySelectedType = team;
						for(int i = 0; i < teamType.getItemCount(); ++i) {
							if(team.compareTo(teamType.getItemAt(i)) < 0) {
								teamType.insertItemAt(team, i);
								break;
							}
							if(i == teamType.getItemCount() - 1) {
								teamType.insertItemAt(team, i);
								break;
							}
						}
					}
					teamType.setSelectedItem(previouslySelectedType);
				}
				else {
					previouslySelectedType = type;
				}
			}
		});
		panel.add(teamType, GenericUtils.createGridBagConstraint(1, 1, 0.7));
		JLabel nameLabel = new JLabel("Name");
		panel.add(nameLabel, GenericUtils.createGridBagConstraint(0, 2, 0.3));
		final TextFieldWithErrorMessage name = new TextFieldWithErrorMessage(15);
		name.setErrorMessage("Event name can not be empty.");
		final Border defaultBorder = name.getBorder();
		name.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					if(!((JTextField) input).getText().trim().isEmpty()) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		panel.add(name, GenericUtils.createGridBagConstraint(1, 2, 0.7));
		JLabel label = new JLabel("Levels");
		label.setVisible(!tournamentViewManager.getTournament().getLevels().isEmpty());
		panel.add(label, GenericUtils.createGridBagConstraint(0, 3, 0.3));
		final JPanel levels = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
                tournamentViewManager.getTournament().getLevels().forEach((level) -> {
                    JCheckBox J = new JCheckBox(level, true); // bvw 9/10/19 selected=Yes is default option
                    levels.add(J);
                    });
		levels.setVisible(!tournamentViewManager.getTournament().getLevels().isEmpty());
		panel.add(levels, GenericUtils.createGridBagConstraint(1, 3, 0.7));
		panel.add(new JLabel("Number Of Teams"), GenericUtils.createGridBagConstraint(0, 4, 0.3));
		final TextFieldWithErrorMessage numTeams = new TextFieldWithErrorMessage(15);
		numTeams.setErrorMessage("Number of teams must be between 1 and " + MAX_NUM_TEAMS_IN_EVENT + " (inclusive).");
		numTeams.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					int number = Integer.parseInt(((JTextField) input).getText().trim());
					if(number > 0 && number <= MAX_NUM_TEAMS_IN_EVENT) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		panel.add(numTeams, GenericUtils.createGridBagConstraint(1, 4, 0.7));
		panel.add(new JLabel("Minimum Winning Score"), GenericUtils.createGridBagConstraint(0, 5, 0.3));
		InputVerifier scoreVerifier = new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					String text = ((JTextField) input).getText().trim();
					if(text.isEmpty() || Integer.parseInt(text) >= 0) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		};
		final TextFieldWithErrorMessage minScore = new TextFieldWithErrorMessage(15);
		minScore.setErrorMessage("Minimum winning score must be a number.");
		minScore.setInputVerifier(scoreVerifier);
		panel.add(minScore, GenericUtils.createGridBagConstraint(1, 5, 0.7));
		panel.add(new JLabel("Maximum Winning Score"), GenericUtils.createGridBagConstraint(0, 6, 0.3));
		final TextFieldWithErrorMessage maxScore = new TextFieldWithErrorMessage(15);
		maxScore.setErrorMessage("Maximum winning score must be a number.");
		maxScore.setInputVerifier(scoreVerifier);
		panel.add(maxScore, GenericUtils.createGridBagConstraint(1, 6, 0.7));
		panel.add(new JLabel("Win By"), GenericUtils.createGridBagConstraint(0, 7, 0.3));
		final TextFieldWithErrorMessage winBy = new TextFieldWithErrorMessage(15);
		winBy.setErrorMessage("Games must be won by at least one point.");
		winBy.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					if(Integer.parseInt(((JTextField) input).getText().trim()) > 0) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		panel.add(winBy, GenericUtils.createGridBagConstraint(1, 7, 0.7));
		panel.add(new JLabel("Best Of"),  GenericUtils.createGridBagConstraint(0, 8, 0.3));
		final TextFieldWithErrorMessage bestOf = new TextFieldWithErrorMessage(15);
		bestOf.setErrorMessage("Matches must have an odd number of games.");
		bestOf.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					if(Integer.parseInt(((JTextField) input).getText().trim()) % 2 == 1) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		panel.add(bestOf, GenericUtils.createGridBagConstraint(1, 8, 0.7));
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("OK");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				message.reset();
				if(!name.getInputVerifier().verify(name)) {
					name.requestFocus();
					return;
				}
				if(!numTeams.getInputVerifier().verify(numTeams)) {
					numTeams.requestFocus();
					return;
				}
				if(!minScore.getInputVerifier().verify(minScore)) {
					minScore.requestFocus();
					return;
				}
				if(!maxScore.getInputVerifier().verify(maxScore)) {
					maxScore.requestFocus();
					return;
				}
				if(!winBy.getInputVerifier().verify(winBy)) {
					winBy.requestFocus();
					return;
				}
				if(!bestOf.getInputVerifier().verify(bestOf)) {
					bestOf.requestFocus();
					return;
				}
				Class<? extends Event> eventType = events.get(type.getSelectedItem());
				Team teamForEvent = tournamentViewManager.getTournament().getTeamByType((String) teamType.getSelectedItem());
				String eventName = name.getText().trim();
				List<String> eventLevels = new ArrayList<String>();
				for(int i = 0; i < levels.getComponentCount(); ++i) {
					JCheckBox box = (JCheckBox) levels.getComponent(i);
					if(box.isSelected()) {
						eventLevels.add(box.getText());
					}
				}
				int eventTeams = Integer.parseInt(numTeams.getText().trim());
				int eventMinScore = 0, eventMaxScore = Integer.MAX_VALUE;
				String text = minScore.getText().trim();
				if(!text.isEmpty()) {
					eventMinScore = Integer.parseInt(text);
				}
				text = maxScore.getText().trim();
				if(!text.isEmpty()) {
					eventMaxScore = Integer.parseInt(text);
				}
				if(eventMinScore > eventMaxScore) {
					JOptionPane.showMessageDialog(TournamentUI.this, "The minimum winning score can not be greater than the maximum winning score.", "Error", JOptionPane.ERROR_MESSAGE);
					maxScore.requestFocus();
					return;
				}
				int eventWinBy = Integer.parseInt(winBy.getText().trim());
				if(eventMaxScore - eventMinScore < eventWinBy) {
					JOptionPane.showMessageDialog(TournamentUI.this, "Invalid combination of minimum winning score, maximum winning score, and points to win by in a game.", "Error", JOptionPane.ERROR_MESSAGE);
					winBy.requestFocus();
					return;
				}
				int eventBestOf = Integer.parseInt(bestOf.getText().trim());
				try {
					if(oldEvent == null && tournamentViewManager.getTournament().getEvent(eventName) != null) {
						JOptionPane.showMessageDialog(TournamentUI.this, "An event must have an unique name.", "Error", JOptionPane.ERROR_MESSAGE);
						name.requestFocus();
					}
					Constructor<? extends Event> constructor = eventType.getConstructor(String.class, List.class, Team.class, int.class, int.class, int.class, int.class, int.class);
					newEvent.setValue(constructor.newInstance(eventName, eventLevels, teamForEvent, eventTeams, eventMinScore, eventMaxScore, eventWinBy, eventBestOf));
					dialog.dispose();
				}
				catch(InvocationTargetException e) {
					JOptionPane.showMessageDialog(TournamentUI.this, e.getTargetException().getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
				catch(Exception e) {
					JOptionPane.showMessageDialog(TournamentUI.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		buttons.add(ok);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
			}
		});
		buttons.add(cancel);
		dialog.getContentPane().add(buttons, BorderLayout.PAGE_END);
		// set defaults from the old event
		if(oldEvent != null) {
			for(String key : events.keySet()) {
				if(oldEvent.getClass().equals(events.get(key))) {
					type.setSelectedItem(key);
					break;
				}
			}
			teamType.setSelectedItem(oldEvent.getTeamFilter().getTeamType());
			nameLabel.setVisible(false);
			name.setText(oldEvent.getName());
			name.setVisible(false);
			for(Component comp : levels.getComponents()) {
				JCheckBox box = (JCheckBox) comp;
				for(String level : oldEvent.getLevels()) {
					if(box.getText().equals(level)) {
						box.setSelected(true);
						break;
					}
				}
			}
			numTeams.setText(String.valueOf(oldEvent.getNumberOfTeams()));
			if(oldEvent.getMinScore() > 0) {
				minScore.setText(String.valueOf(oldEvent.getMinScore()));
			}
			if(oldEvent.getMaxScore() < Integer.MAX_VALUE) {
				maxScore.setText(String.valueOf(oldEvent.getMaxScore()));
			}
			winBy.setText(String.valueOf(oldEvent.getWinBy()));
			bestOf.setText(String.valueOf(oldEvent.getBestOf()));
		}
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		return newEvent.getValue();
	}
	
	private void openNewPlayerDialog() {
		final JDialog dialog = new JDialog(this, "New Player");
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new BorderLayout());
		final MessageLabel message = new MessageLabel();
		dialog.add(message, BorderLayout.PAGE_START);
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Name"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		final TextFieldWithErrorMessage name = new TextFieldWithErrorMessage(15);
		name.setErrorMessage("Player name can not be empty.");
		final Border defaultBorder = name.getBorder();
		name.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					if(!((JTextField) input).getText().trim().isEmpty()) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		panel.add(name, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		panel.add(new JLabel("Gender"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		final JComboBox<String> gender = new JComboBox<String>(new String[]{"Male", "Female"});
		panel.add(gender, GenericUtils.createGridBagConstraint(1, 1, 0.7));
		panel.add(new JLabel("Address"), GenericUtils.createGridBagConstraint(0, 2, 0.3));
		final JTextField address = new JTextField(15);
		panel.add(address, GenericUtils.createGridBagConstraint(1, 2, 0.7));
		panel.add(new JLabel("Email"), GenericUtils.createGridBagConstraint(0, 3, 0.3));
		final JTextField email = new JTextField(15);
		panel.add(email, GenericUtils.createGridBagConstraint(1, 3, 0.7));
		panel.add(new JLabel("Phone Number"), GenericUtils.createGridBagConstraint(0, 4, 0.3));
		final JTextField phone = new JTextField(15);
		panel.add(phone, GenericUtils.createGridBagConstraint(1, 4, 0.7));
		JLabel label = new JLabel("Level");
		label.setVisible(!tournamentViewManager.getTournament().getLevels().isEmpty());
		panel.add(label, GenericUtils.createGridBagConstraint(0, 5, 0.3));
		ArrayList<String> levels = new ArrayList<String>(tournamentViewManager.getTournament().getLevels());
		levels.add(0, "");
		final JComboBox<String> level = new JComboBox<String>(levels.toArray(new String[0]));
		level.setVisible(!tournamentViewManager.getTournament().getLevels().isEmpty());
		panel.add(level, GenericUtils.createGridBagConstraint(1, 5, 0.7));
		panel.add(new JLabel("Date of Birth (MM/DD/YYYY)"), GenericUtils.createGridBagConstraint(0, 6, 0.3));
		final TextFieldWithErrorMessage dateOfBirth = new TextFieldWithErrorMessage(15);
		dateOfBirth.setErrorMessage("Date of birth must be a date.");
		panel.add(dateOfBirth, GenericUtils.createGridBagConstraint(1, 6, 0.7));
		panel.add(new JLabel("Membership Number"), GenericUtils.createGridBagConstraint(0, 7, 0.3));
		final JTextField membership = new JTextField(15);
		panel.add(membership, GenericUtils.createGridBagConstraint(1, 7, 0.7));
		panel.add(new JLabel("Club"), GenericUtils.createGridBagConstraint(0, 8, 0.3));
		final JTextField club = new JTextField(15);
		panel.add(club, GenericUtils.createGridBagConstraint(1, 8, 0.7));
		InputVerifier amountVerifier = new InputVerifier() {
			public boolean verify(JComponent input) {
				try {
					String text = ((JTextField) input).getText().trim();
					if(text.isEmpty() || Double.parseDouble(text) >= 0.0) {
						message.reset();
						input.setBorder(defaultBorder);
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		};
		panel.add(new JLabel("Amount Paid"), GenericUtils.createGridBagConstraint(0, 9, 0.3));
		final TextFieldWithErrorMessage paid = new TextFieldWithErrorMessage(15);
		paid.setErrorMessage("Amount paid must be a number.");
		paid.setInputVerifier(amountVerifier);
		panel.add(paid, GenericUtils.createGridBagConstraint(1, 9, 0.7));
		panel.add(new JLabel("Amount Due"), GenericUtils.createGridBagConstraint(0, 10, 0.3));
		final TextFieldWithErrorMessage due = new TextFieldWithErrorMessage(15);
		due.setErrorMessage("Amount due must be a number.");
		due.setInputVerifier(amountVerifier);
		panel.add(due, GenericUtils.createGridBagConstraint(1, 10, 0.7));
		panel.add(new JLabel("Events"), GenericUtils.createGridBagConstraint(0, 11, 0.3));
		final JPanel events = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
		gender.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				events.removeAll();
				Player player = new Player(null, gender.getSelectedIndex() == 0);
				player.setDateOfBirth(GenericUtils.stringToDate(dateOfBirth.getText(), "MM/dd/yyyy"));
				for(Event tEvent : tournamentViewManager.getTournament().getEvents()) {
					if(tEvent.getTeamFilter().isValidPlayer(player)) {
						events.add(new JCheckBox(tEvent.getName()));
					}
				}
				dialog.pack();
				dialog.setLocationRelativeTo(TournamentUI.this);
			}
		});
		gender.setSelectedIndex(0);
		dateOfBirth.setInputVerifier(new InputVerifier() {
			private Date lastDate;
			public boolean verify(JComponent input) {
				String text = ((JTextField) input).getText().trim();
				Date date = GenericUtils.stringToDate(text, "MM/dd/yyyy");
				if((lastDate == null && date != null) || (lastDate != null && !lastDate.equals(date))) {
					lastDate = date;
					events.removeAll();
					Player player = new Player(null, gender.getSelectedIndex() == 0);
					player.setDateOfBirth(date);
					for(Event tEvent : tournamentViewManager.getTournament().getEvents()) {
						if(tEvent.getTeamFilter().isValidPlayer(player)) {
							events.add(new JCheckBox(tEvent.getName()));
						}
					}
					dialog.pack();
					dialog.setLocationRelativeTo(TournamentUI.this);
				}
				if(text.isEmpty() || date != null) {
					message.reset();
					input.setBorder(defaultBorder);
					return true;
				}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				return false;
			}
		});
		panel.add(events, GenericUtils.createGridBagConstraint(1, 11, 0.7));
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("Add Player");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				message.reset();
				if(!name.getInputVerifier().verify(name)) {
					name.requestFocus();
					return;
				}
				if(!dateOfBirth.getInputVerifier().verify(dateOfBirth)) {
					dateOfBirth.requestFocus();
					return;
				}
				if(!paid.getInputVerifier().verify(paid)) {
					paid.requestFocus();
					return;
				}
				if(!due.getInputVerifier().verify(due)) {
					due.requestFocus();
					return;
				}
				Player player = new Player(name.getText().trim(), gender.getSelectedIndex() == 0);
				name.setText(null);
				player.setAddress(address.getText().trim());
				address.setText(null);
				player.setEmail(email.getText().trim());
				email.setText(null);
				player.setPhoneNumber(phone.getText().trim());
				phone.setText(null);
				String pLevel = (String) level.getSelectedItem();
				if(!pLevel.isEmpty()) {
					player.setLevel(pLevel);
				}
				level.setSelectedIndex(0);
				player.setDateOfBirth(GenericUtils.stringToDate(dateOfBirth.getText(), "MM/dd/yyyy"));
				dateOfBirth.setText(null);
				player.setMembershipNumber(membership.getText().trim());
				membership.setText(null);
				player.setClub(club.getText().trim());
				club.setText(null);
				String text = paid.getText().trim();
				paid.setText(null);
				if(!text.isEmpty()) {
					player.setAmountPaid(Double.parseDouble(text));
				}
				text = due.getText().trim();
				due.setText(null);
				if(!text.isEmpty()) {
					player.setAmountDue(Double.parseDouble(text));  
				}
				HashSet<String> pEvents = new HashSet<String>();
				for(int i = 0; i < events.getComponentCount(); ++i) {
					JCheckBox box = (JCheckBox) events.getComponent(i);
					if(box.isSelected()) {
						pEvents.add(box.getText());
					}
					box.setSelected(false);
				}
				player.setEvents(pEvents);
				gender.setSelectedIndex(0);
				dateOfBirth.getInputVerifier().verify(dateOfBirth);
				tournamentViewManager.getTournament().addPlayer(player);
				message.success("Player successfully added.");
				name.requestFocus();
			}
		});
		buttons.add(ok);
		final JButton cancel = new JButton("Done");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
				if(tournamentViewManager.getTournament().hasNewPlayers()) {
					tournamentViewManager.switchToTab(TournamentViewManager.PLAYERS_TAB, true);
				}
			}
		});
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				cancel.doClick();
			}
		});
		buttons.add(cancel);
		dialog.getContentPane().add(buttons, BorderLayout.PAGE_END);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}
	
	public String openNewTeamTypeDialog() {
		final StringBuffer newTeamType = new StringBuffer();
		final JDialog dialog = new JDialog(this, "New Team Type");
		dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new BorderLayout());
		final MessageLabel message = new MessageLabel();
		dialog.add(message, BorderLayout.PAGE_START);
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Team Type"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		final JTextField type = new JTextField(15);
		final Border defaultBorder = type.getBorder();
		type.setInputVerifier(new InputVerifier() {
			public boolean verify(JComponent input) {
				String value = ((JTextField) input).getText();
				if(value == null || value.trim().isEmpty()) {
					message.error("Team type can not be empty.");
					input.setBorder(new LineBorder(Color.RED, 1));
					return false;
				}
				value = value.trim();
				if(NEW_DISPLAY_TEXT.equals(value)) {
					message.error("\"" + NEW_DISPLAY_TEXT + "\" is a reserved name.");
					input.setBorder(new LineBorder(Color.RED, 1));
					return false;
				}
				if(tournamentViewManager.getTournament().getTeamByType(value) != null) {
					message.error("Team type must be unique.");
					input.setBorder(new LineBorder(Color.RED, 1));
					return false;
				}
				message.reset();
				input.setBorder(defaultBorder);
				return true;
			}
		});
		type.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.VK_ENTER) {
					event.consume();
					type.getInputVerifier().verify(type);
				}
			}
		});
		panel.add(type, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		panel.add(new JLabel("Size"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		final TextFieldWithErrorMessage size = new TextFieldWithErrorMessage(15);
		size.setErrorMessage("Team size must be greater than zero.");
		size.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.VK_ENTER) {
					event.consume();
					size.getInputVerifier().verify(size);
				}
			}
		});
		panel.add(size, GenericUtils.createGridBagConstraint(1, 1, 0.7));
		panel.add(new JLabel("Team Modifiers"), GenericUtils.createGridBagConstraint(0, 2, 1));
		final JPanel modifierPanel = new JPanel();
		modifierPanel.setLayout(new BoxLayout(modifierPanel, BoxLayout.PAGE_AXIS));
		ActionListener modifierAction = new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
				JComboBox<String> modifier = (JComboBox<String>) event.getSource();
				String selected = (String) modifier.getSelectedItem();
				if(!modifier.isEnabled() || selected == null) {
					return;
				}
				JPanel parent = (JPanel) modifier.getParent();
				while(parent.getComponentCount() > 2) {
					parent.remove(2);
				}
				boolean last = modifierPanel.getComponent(modifierPanel.getComponentCount() - 1).equals(parent);
				if(selected.isEmpty() && !last) {
					modifierPanel.remove(parent);
				}
				else if(!selected.isEmpty()) {
					CreatableTeamModifier annotation = modifiers.get(selected).getAnnotation(CreatableTeamModifier.class);
					for(String parameter : annotation.parameters()) {
						JPanel panel = new JPanel(new BorderLayout());
						panel.add(new JLabel("   " + parameter), BorderLayout.LINE_START);
						panel.add(new JTextField(15), BorderLayout.LINE_END);
						parent.add(panel);
					}
					if(last) {
						int teamSize = 0;
						if(size.getInputVerifier().verify(size)) {
							teamSize = Integer.parseInt(size.getText());
						}
						modifierPanel.add(createNewModifierSelector(teamSize, this, true));
					}
				}
				dialog.pack();
			}
		};
		modifierPanel.add(createNewModifierSelector(0, modifierAction, false));
		GridBagConstraints constraints = GenericUtils.createGridBagConstraint(0, 3, 1);
		constraints.gridwidth = 2;
		constraints.gridheight = GridBagConstraints.REMAINDER;
		panel.add(modifierPanel, constraints);
		size.setInputVerifier(new InputVerifier() {
			@SuppressWarnings("unchecked")
			public boolean verify(JComponent input) {
				try {
					int value = Integer.parseInt(((JTextField) input).getText().trim());
					if(value > 0) {
						message.reset();
						input.setBorder(defaultBorder);
						// check to see if the selected modifiers are still applicable
						for(int i = 0; i < modifierPanel.getComponentCount(); ++i) {
							JPanel panel = (JPanel) modifierPanel.getComponent(i);
							JComboBox<String> comboBox = (JComboBox<String>) panel.getComponent(1);
							String modifier = (String) comboBox.getSelectedItem();
							comboBox.setEnabled(false);
							comboBox.removeAllItems();
							comboBox.addItem("");
							List<String> keys = new ArrayList<String>(modifiers.keySet());
							Collections.sort(keys);
							for(String key : keys) {
								if(TeamModifier.isApplicableForTeamSize(value, modifiers.get(key))) {
									comboBox.addItem(key);
								}
							}
							comboBox.setSelectedItem(modifier);
							comboBox.setEnabled(true);
							if(modifier.isEmpty()) {
								continue;
							}
							if(!TeamModifier.isApplicableForTeamSize(value, modifiers.get(modifier))) {
								comboBox.setSelectedItem("");
								--i;
								continue;
							}
						}
						dialog.pack();
						return true;
					}
				}
				catch(Exception e) {}
				message.error(((TextFieldWithErrorMessage) input).getErrorMessage());
				input.setBorder(new LineBorder(Color.RED, 1));
				// remove all the non-selected items
				for(int i = 0; i < modifierPanel.getComponentCount(); ++i) {
					JComboBox<String> comboBox = (JComboBox<String>) ((JPanel) modifierPanel.getComponent(i)).getComponent(1);
					comboBox.setEnabled(false);
					String selected = (String) comboBox.getSelectedItem();
					comboBox.removeAllItems();
					comboBox.addItem(selected);
					comboBox.setEnabled(true);
				}
				return false;
			}
		});
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("Save");
		ok.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
				message.reset();
				if(!type.getInputVerifier().verify(type)) {
					type.requestFocus();
					return;
				}
				if(!size.getInputVerifier().verify(size)) {
					size.requestFocus();
					return;
				}
				try {
					BaseModifierTeam team = new BaseModifierTeam(Integer.parseInt(size.getText().trim()), type.getText().trim());
					for(int i = 0; i < modifierPanel.getComponentCount(); ++i) {
						JPanel panel = (JPanel) modifierPanel.getComponent(i);
						String modifier = (String) ((JComboBox<String>) panel.getComponent(1)).getSelectedItem();
						if(modifier == null || modifier.isEmpty()) {
							continue;
						}
						Class<? extends TeamModifier> modifierClass = modifiers.get(modifier);
						CreatableTeamModifier annotation = modifierClass.getAnnotation(CreatableTeamModifier.class);
						Object parameters = new String[annotation.parameters().length];
						if(((String[]) parameters).length == 0) {
							team.addModifier(modifierClass.getConstructor().newInstance());
							continue;
						}
						for(int j = 2; j < panel.getComponentCount(); ++j) {
							((String[]) parameters)[j - 2] = ((JTextField) ((JPanel) panel.getComponent(j)).getComponent(1)).getText().trim();
						}
						team.addModifier(modifierClass.getDeclaredConstructor(String[].class).newInstance(parameters));
					}
					if(tournamentViewManager.getTournament().addTeamType(team)) {
						newTeamType.append(team.getTeamType());
						tournamentViewManager.updateCurrentTab();
						tournamentViewManager.modified();
						dialog.dispose();
					}
					else {
						// we shouldn't ever get here
						message.error("Unable to add the new team type. Please try again.");
					}
				}
				catch(Exception e) {
					if(e.getCause() != null) {
						message.error(e.getCause().getMessage());
					}
					else {
						message.error("Unable to create a modifier. Please check all the parameters and try again.");
					}
				}
			}
		});
		buttons.add(ok);
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
			}
		});
		buttons.add(cancel);
		dialog.getContentPane().add(buttons, BorderLayout.PAGE_END);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
		return newTeamType.toString().isEmpty() ? null : newTeamType.toString();
	}
	
	private JPanel createNewModifierSelector(int numberOfPlayers, ActionListener action, boolean addSeparator) {
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
		panel.add(Box.createRigidArea(new Dimension(0, addSeparator ? 10 : 0)));
		List<String> list = new ArrayList<String>();
		for(String key : modifiers.keySet()) {
			if(TeamModifier.isApplicableForTeamSize(numberOfPlayers, modifiers.get(key))) {
				list.add(key);
			}
		}
		Collections.sort(list);
		list.add(0, "");
		JComboBox<String> modifier = new JComboBox<String>(list.toArray(new String[0]));
		modifier.addActionListener(action);
		panel.add(modifier);
		return panel;
	}
	
	public boolean checkForExistingTeamTypes() {
		// we need to create a new team type before we can continue
		if(tournamentViewManager.getTournament().getTeamTypes().isEmpty() && openNewTeamTypeDialog() == null) {
			JOptionPane.showMessageDialog(TournamentUI.this, "You must create at least one type of team.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	private void openNewTeamDialog() {   
		if(!checkForExistingTeamTypes()) {
			return;
		}
		final JDialog dialog = new JDialog(this, "New Team");
		dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		dialog.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setResizable(false);
		dialog.getContentPane().setLayout(new BorderLayout());
		final MessageLabel message = new MessageLabel();
		dialog.add(message, BorderLayout.PAGE_START);
		JPanel panel = new JPanel(new GridBagLayout());
		panel.add(new JLabel("Type of Team"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		final JComboBox<String> type = new JComboBox<String>();
		panel.add(type, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		panel.add(new JLabel("Name"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		final JTextField name = new JTextField(15);
		panel.add(name, GenericUtils.createGridBagConstraint(1, 1, 0.7));
		panel.add(new JLabel("Seed"), GenericUtils.createGridBagConstraint(0, 2, 0.3));
		final JTextField seed = new JTextField(15);
		panel.add(seed, GenericUtils.createGridBagConstraint(1, 2, 0.7));
		GridBagConstraints c = GenericUtils.createGridBagConstraint(0, 3, 1);
		c.gridwidth = 2;
		panel.add(new JLabel("Players"), c);
		final HashMap<String, Player> players = new HashMap<String, Player>();
		for(Player player : tournamentViewManager.getTournament().getPlayers()) {
			if(player != null) {
				players.put(player.getName(), player);
			}
		}
		final JPanel playersPanel = new JPanel();
		playersPanel.setLayout(new BoxLayout(playersPanel, BoxLayout.PAGE_AXIS));
		c = GenericUtils.createGridBagConstraint(0, 4, 1);
		c.gridwidth = 2;
		c.gridheight = GridBagConstraints.REMAINDER;
		panel.add(playersPanel, c);
		type.addActionListener(new ActionListener() {
			private String previouslySelectedType;
			public void actionPerformed(ActionEvent event) {
				String teamType = (String) type.getSelectedItem();
				if(NEW_DISPLAY_TEXT.equals(teamType)) {
					String team = openNewTeamTypeDialog();
					if(team != null) {
						previouslySelectedType = team;
						for(int i = 0; i < type.getItemCount(); ++i) {
							if(team.compareTo(type.getItemAt(i)) < 0) {
								type.insertItemAt(team, i);
								break;
							}
							if(i == type.getItemCount() - 1) {
								type.insertItemAt(team, i);
								break;
							}
						}
					}
					type.setSelectedItem(previouslySelectedType);
				}
				else {
					previouslySelectedType = teamType;
				}
				playersPanel.removeAll();
				Team team = tournamentViewManager.getTournament().getTeamByType(previouslySelectedType);
				String[] playerNames = TournamentUtils.getValidPlayers(team, players.values()).toArray(new String[0]);
				for(int i = 0; i < team.getNumberOfPlayers(); ++i) {
					playersPanel.add(new JComboBox<String>(playerNames));
					playersPanel.add(Box.createVerticalStrut(5));
				}
				dialog.pack();
			}
		});
		List<String> list = tournamentViewManager.getTournament().getTeamTypes();
		Collections.sort(list);
		list.add(NEW_DISPLAY_TEXT);
		for(String teamType : list) {
			// the first item added should be auto selected and will fire the action listener to build and populate the players panel
			type.addItem(teamType);
		}
		dialog.getContentPane().add(panel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton ok = new JButton("Add Team");
		ok.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
				message.reset();
				Team team = tournamentViewManager.getTournament().getTeamByType((String) type.getSelectedItem()).newInstance();
				team.setName(name.getText());
				team.setSeed(seed.getText());
				for(int i = 0; i < playersPanel.getComponentCount(); i += 2) {
					String playerName = (String) ((JComboBox<String>) playersPanel.getComponent(i)).getSelectedItem();
					if(!team.setPlayer(i / 2, players.get(playerName))) {
						message.error(playerName + " is not a valid player for " + type.getSelectedItem() + ".");
						return;
					}
				}
                                // new test added for players being already in a team of same type  22/4/19 bvw
                                List<Team> teams= tournamentViewManager.getTournament().getTeams();
                                for(Team allTeams : teams) {
                                   if(allTeams.getTeamType().equals(team.getTeamType())) {
                                       for(Player anyplayer :  allTeams.getPlayers()) {
                                           for(Player newplayer : team.getPlayers()) {
                                               if(newplayer==anyplayer) {
                                                message.error("Player already in team.");
                                                return;   
                                               }
                                           }
                                        }
                                   }
                                }
				if(!team.isValid()) {
					message.error("This is not a valid team.");
					return;
				}
                            	tournamentViewManager.getTournament().addTeam(team);
				name.setText(null);
				seed.setText(null);
				for(int i = 0; i < playersPanel.getComponentCount(); i += 2) {
					((JComboBox<String>) playersPanel.getComponent(i)).setSelectedIndex(0);
				}
				message.success("Team successfully added.");
			}
		});
		buttons.add(ok);
		final JButton cancel = new JButton("Done");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
				if(tournamentViewManager.getTournament().hasNewTeams()) {
					tournamentViewManager.switchToTab(TournamentViewManager.TEAMS_TAB, true);
				}
			}
		});
		dialog.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				cancel.doClick();
			}
		});
		buttons.add(cancel);
		dialog.getContentPane().add(buttons, BorderLayout.PAGE_END);
		dialog.pack();
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}
       
	private void createTournamentView() {
		JTabbedPane displayPane = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
		tournamentViewManager = new TournamentViewManager(this, displayPane, Color.WHITE);
		getContentPane().add(displayPane, BorderLayout.CENTER);
	}
	
	private void createToolbar() {
		JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
		toolbar.setFloatable(false);
		toolbar.setFocusable(false);
		final JButton newTournament = new JButton(new ImageIcon(Images.NEW));
		newTournament.setFocusable(false);
		newTournament.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(checkForSaveAction()) {
					openTournamentDialog(false);
				}
			}
		});
		newTournament.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), "click");
		newTournament.getActionMap().put("click", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				newTournament.doClick();
			}
		});
		newTournament.setToolTipText("New Tournament (Ctrl + N)");
		toolbar.add(newTournament);
		final JButton openTournament = new JButton(new ImageIcon(Images.OPEN));
		openTournament.setFocusable(false);
		openTournament.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(checkForSaveAction()) {
					String filePath = openFileSelectionDialog(true);
					if(filePath == null) {
						return;
					}
					try {
						ObjectInput input = null;
						try {
							setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							TournamentUtils.resetMatchIndex();
							input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
							Tournament tournament = (Tournament) input.readObject();
							tournamentViewManager.setTournament(tournament);
							input.close();
							input = null;
							// this usually happens if the tournament data file was copied/moved from another location
							if(!filePath.equals(tournament.getFilePath())) {
								tournament.setFilePath(filePath);
								tournamentViewManager.modified();
							}
							setCursor(null);
							if(Tournament.COMPATIBLE > tournament.getVersion()) {
								JOptionPane.showMessageDialog(TournamentUI.this, "Old data file detected. Modifying this file is not recommended.", "Warning", JOptionPane.WARNING_MESSAGE);
							}
							else if(Tournament.VERSION < tournament.getVersion()) {
								JOptionPane.showMessageDialog(TournamentUI.this, "This data file was created by a newer version of " + APP_NAME + ".\nPlease use the new version to access this file.", "Warning", JOptionPane.WARNING_MESSAGE);
							}
						}
						catch(Exception e) {
							setCursor(null);
							JOptionPane.showMessageDialog(TournamentUI.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
						}
						finally {
							if(input != null) {
								input.close();
							}
						}
					}
					catch(Exception e) {
						setCursor(null);
						JOptionPane.showMessageDialog(TournamentUI.this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		openTournament.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), "click");
		openTournament.getActionMap().put("click", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				openTournament.doClick();
			}
		});
		openTournament.setToolTipText("Open Tournament (Ctrl + O)");
		toolbar.add(openTournament);
		edit = new JButton(new ImageIcon(Images.EDIT));
		edit.setFocusable(false);
		edit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openTournamentDialog(true);
			}
		});
		edit.setToolTipText("Edit Tournament");
		toolbar.add(edit);
		close = new JButton(new ImageIcon(Images.CLOSE));
		close.setFocusable(false);
		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(checkForSaveAction()) {
					checkForMultiDayTournament();
					tournamentViewManager.setTournament(null);
					TournamentUtils.resetMatchIndex();
				}
			}
		});
		close.setToolTipText("Close Tournament");
		toolbar.add(close);
		toolbar.addSeparator();
		save = new JButton(new ImageIcon(Images.SAVE));
		save.setFocusable(false);
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				save(true);
			}
		});
		save.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "click");
		save.getActionMap().put("click", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				save.doClick();
			}
		});
		save.setToolTipText("Save Tournament (Ctrl + S)");
		toolbar.add(save);
		saveAs = new JButton(new ImageIcon(Images.SAVE_AS));
		saveAs.setFocusable(false);
		saveAs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				save(false);
			}
		});
		saveAs.setToolTipText("Save Tournament As...");
		toolbar.add(saveAs);
		toolbar.addSeparator();
		addEvent = new JButton(new ImageIcon(Images.ADD_EVENT));
		addEvent.setFocusable(false);
		addEvent.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Event newEvent = openEventDialog(null);
				if(newEvent != null) {
					tournamentViewManager.getTournament().addEvent(newEvent);
					tournamentViewManager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
				}
			}
		});
		addEvent.setToolTipText("Add Event");
		toolbar.add(addEvent);
		addPlayer = new JButton(new ImageIcon(Images.ADD_PLAYER));
		addPlayer.setFocusable(false);
		addPlayer.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openNewPlayerDialog();
			}
		});
		addPlayer.setToolTipText("Add Player");
		toolbar.add(addPlayer);
		addTeam = new JButton(new ImageIcon(Images.ADD_TEAM));
		addTeam.setFocusable(false);
		addTeam.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				openNewTeamDialog();
			}
		});
                addTeam.setToolTipText("Add Team");
		toolbar.add(addTeam);
                // new button to create pairs 21/4/19 bvw
                autoPair = new JButton(new ImageIcon(Images.AUTO_PAIR));
		autoPair.setFocusable(false);
		autoPair.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				AutoPairDialog dialog = new AutoPairDialog(TournamentUI.this, tournamentViewManager);
			}
		});
		autoPair.setToolTipText("Auto Pair");
		toolbar.add(autoPair);
                
		importData = new JButton(new ImageIcon(Images.IMPORT_DATA));
		importData.setFocusable(false);
		importData.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(importFileChooser.showDialog(true) != FileChooser.APPROVE_OPTION) {
					return;
				}
				File file = importFileChooser.getSelectedFile();
				if(file == null) {
					return;
				}
				ImportDialog dialog = new TournamentImportDialog(TournamentUI.this, tournamentViewManager, file);
				if(dialog.getResult()) {
					return;
				}
				String message = dialog.checkImportCompatibility();
				if(message != null) {
					JOptionPane.showMessageDialog(TournamentUI.this, message, "Import Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				dialog = new CSVImportDialog(TournamentUI.this, tournamentViewManager, file);
				if(dialog.getResult()) {
					return;
				}
				dialog = new GenericImportDialog(TournamentUI.this, tournamentViewManager, file);
				if(dialog.getResult()) {
					return;
				}
				else {
					JOptionPane.showMessageDialog(TournamentUI.this, "Unable to read selected data file.", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		importData.setToolTipText("Import Data");
		toolbar.add(importData);
		toolbar.addSeparator();
		print = new JButton(new ImageIcon(Images.PRINT));
		print.setFocusable(false);
		print.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				tournamentViewManager.print();
			}
		});
		print.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK), "click");
		print.getActionMap().put("click", new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				print.doClick();
			}
		});
		print.setToolTipText("Print (Ctrl + P)");
		toolbar.add(print);
		JButton help = new JButton(new ImageIcon(Images.HELP));
		help.setFocusable(false);
		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JOptionPane.showMessageDialog(TournamentUI.this, APP_NAME + " was created by Jason Young.\n" +
						"Special thanks to: Aileen Chyn, Allen Liou, Andrew Kim, Elaine Hui, and Kevin Tai.\n" +
						"Version: " + Tournament.VERSION + "\n", 
						"About " + APP_NAME, JOptionPane.INFORMATION_MESSAGE);
			}
		});
		help.setToolTipText("Help");
		toolbar.add(help);
		getContentPane().add(toolbar, BorderLayout.PAGE_START);
	}
}
