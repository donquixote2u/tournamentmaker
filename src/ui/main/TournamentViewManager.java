package ui.main;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import ui.component.GenericWrapper;
import ui.component.button.CourtButton;
import ui.component.button.EventButton;
import ui.component.dialog.CourtDialog;
import ui.component.dialog.EventResultDialog;
import ui.component.dialog.EventTeamSelector;
import ui.component.editor.ButtonAction;
import ui.component.editor.EventEditor;
import ui.component.editor.InfoAction;
import ui.component.editor.MatchEditor;
import ui.component.editor.TeamEditor;
import ui.component.editor.TimeEditor;
import ui.component.label.FormattedListCellRenderer;
import ui.component.panel.EventBracketCanvas;
import ui.component.panel.MatchActionPanel;
import ui.component.panel.MatchPanel;
import ui.component.table.GenericTableModel;
import ui.component.table.GenericValue;
import ui.util.GenericUtils;
import data.event.Event;
import data.event.painter.EventPainter;
import data.match.Game;
import data.match.Match;
import data.player.Player;
import data.team.Team;
import data.tournament.Court;
import data.tournament.Tournament;

@SuppressWarnings({"serial", "unchecked"})
public class TournamentViewManager {
	public static final String TOURNAMENT_TAB = "Tournament Overview";
	public static final String EVENTS_TAB = "Event Overview";
	public static final String PLAYERS_TAB = "Player Overview";
	public static final String TEAMS_TAB = "Team Overview";
	public static final String MATCHES_TAB = "Completed Matches";
	public static final String BYE = "BYE";
	public static final String WALKOVER = "(w/o)";
	private static final String NEW_ACTION = TournamentViewManager.class.getCanonicalName() + ".tournament_view_set";
	private static final String UPDATE_ACTION = TournamentViewManager.class.getCanonicalName() + ".tournament_view_update";
	private static final String REFRESH_ACTION = TournamentViewManager.class.getCanonicalName() + ".tournament_view_refresh";
	private static final String PLAYING_STATUS = "In Game";
	private static final String NOT_CHECKED_IN_STATUS = "Absent";
	private static final String INVALID_STATUS = "Invalid";
	private static final String WITHDRAWN_STATUS = "Withdrew";
	private static final String UNAVAILABLE_STATUS = "Unavailable";
	private boolean modified;
	private JTabbedPane displayPane;
	private Color backgroundColor;
	private TournamentUI ui;
	private Tournament tournament;
	private EventTeamSelector teamSelector;
	private EventResultDialog resultDialog;
	private CourtDialog courtDialog;
	private JList<Match> matches;
	private EventBracketCanvas eventCanvas;
	private Timer update;
	// private JPanel courtsPanel;
	public JPanel courtsPanel;	
	public TournamentViewManager(TournamentUI ui, JTabbedPane displayPane, Color backgroundColor) {
		if(ui == null || displayPane == null || backgroundColor == null) {
			throw new RuntimeException("invalid parameters");
		}
		this.ui = ui;
		this.displayPane = displayPane;
		this.backgroundColor = backgroundColor;
		teamSelector = new EventTeamSelector(ui, this);
		resultDialog = new EventResultDialog(ui, this);
		courtDialog = new CourtDialog(ui, this);
		createTabs();
		// create a timer to refresh the current screen every 30 seconds
		update = new Timer();
		update.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if(tournament != null) {
					// recalculate the time estimations for the matches
					ArrayList<Match> curMatches = new ArrayList<Match>();
					for(int i = 0; i < matches.getModel().getSize(); ++i) {
						curMatches.add(matches.getModel().getElementAt(i));
					}
					tournament.updateEstimatedTimes(curMatches);
				}
				// any functions called in the refresh action should be synchronized
				refreshCurrentTab();
			}
		}, 1, 30000);
	}
	
	public void close() {
		update.cancel();
	}
	
	public void setTournament(Tournament tournament) {
		this.tournament = tournament;
		for(int i = 0; i < displayPane.getTabCount(); ++i) {
			callComponentAction(displayPane.getComponentAt(i), NEW_ACTION);
		}
		switchToTab(TOURNAMENT_TAB, false);
		resetModified();
		String title = TournamentUI.APP_NAME;
		if(tournament != null) {
			title += " - " + tournament.getName();
			if(tournament.getFilePath() == null) {
				modified();
			}
		}
		ui.setTitle(title);
		ui.setEnabledForTournamentButtons(tournament != null);
	}
	
	public void updateTournament() {
		ui.setTitle(TournamentUI.APP_NAME + " - " + tournament.getName());
		updateCurrentTab();
		modified();
	}
	
	public Tournament getTournament() {
		return tournament;
	}
	
	public void switchToTab(String tabName, boolean modified) {
		if(modified) {
			modified();
		}
		if(displayPane.getTitleAt(displayPane.getSelectedIndex()).equals(tabName)) {
			// we're already here so just update the tab
			if(tournament != null) {
				// tries to keep the same match selection
				Match match = matches.getSelectedValue();
				callComponentAction(displayPane.getSelectedComponent(), UPDATE_ACTION);
				matches.setSelectedValue(match, true);
			}
			return;
		}
		for(int i = 0; i < displayPane.getTabCount(); ++i) {
			if(displayPane.getTitleAt(i).equals(tabName)) {
				displayPane.setSelectedIndex(i);
				return;
			}
		}
	}
	
	public void refreshCurrentTab() {
		if(tournament == null) {
			return;
		}
		callComponentAction(displayPane.getSelectedComponent(), REFRESH_ACTION);
	}
	
	public void updateCurrentTab() {
		if(tournament == null) {
			return;
		}
		callComponentAction(displayPane.getSelectedComponent(), UPDATE_ACTION);
	}
	
	public boolean resetModified() {
		if(modified) {
			modified = false;
			return true;
		}
		return false;
	}
	
	public boolean isModified() {
		return modified;
	}
	
	public void modified() {
		modified = true;
	}
	
	public boolean canStartSelectedMatch(CourtButton endCourt) {
		if(endCourt == null || !endCourt.isUsableCourt()) {
			return false;
		}
		CourtButton startCourt = getStartCourtForSwap();
		if(startCourt != null) {
			return (startCourt.isUsableCourt() || endCourt.isAvailableCourt()) && !startCourt.equals(endCourt);
		}
		Match match = matches.getSelectedValue();
		return match != null && match.canStartMatch();
	}
	
	public boolean addSelectedMatchToCourtButton(CourtButton courtButton) {
		if(courtButton == null || !courtButton.isUsableCourt()) {
			return false;
		}
		CourtButton startCourt = getStartCourtForSwap();
		if(startCourt != null) {
			if((startCourt.isUsableCourt() || courtButton.isAvailableCourt()) && !startCourt.equals(courtButton)) {
				return startCourt.swapMatch(courtButton);
			}
			return false;
		}
		return addMatchToCourtButton(matches.getSelectedValue(), courtButton);
	}
	
	public void print() {
		if(tournament == null) {
			JOptionPane.showMessageDialog(ui, "No tournament loaded.", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		int index = displayPane.getSelectedIndex();
		if(index == -1) {
			return;
		}
		if(TOURNAMENT_TAB.equals(displayPane.getTitleAt(index))) {
			Match match = matches.getSelectedValue();
			if(match == null) {
				JOptionPane.showMessageDialog(ui, "No match selected.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			if(match.getTeam1() == null || match.getTeam2() == null) {
				JOptionPane.showMessageDialog(ui, "Invalid match selected.", "Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			printMatch(match);
		}
		else if(EVENTS_TAB.equals(displayPane.getTitleAt(index))) {
			if(eventCanvas.hasPainter()) {
				printComponent(eventCanvas);
			}
			else {
				JOptionPane.showMessageDialog(ui, "No event selected.", "Error", JOptionPane.ERROR_MESSAGE);
			}
		}
		else {
			printComponent(displayPane.getSelectedComponent());
		}
	}
	
	public void printComponent(Component component) {
		ui.print(component);
	}
	
	public void printMatch(Match match) {
		JDialog dialog = new JDialog();
		try {
			MatchPanel panel = new MatchPanel(dialog);
			dialog.getContentPane().setLayout(new BorderLayout());
			dialog.getContentPane().add(panel, BorderLayout.CENTER);
			panel.setMatch(match, true);
			printComponent(panel);
		}
		catch(Exception e) {
			JOptionPane.showMessageDialog(ui, "An error was encountered, unable to print select match. Please try again.", "Error", JOptionPane.ERROR_MESSAGE);
		}
		finally {
			dialog.dispose();
		}
	}
	
	public Color getBackgroundColor() {
		return backgroundColor;
	}
	
	private CourtButton getStartCourtForSwap() {
		if(courtsPanel == null) {
			return null;
		}
		for(int i = 0; i < courtsPanel.getComponentCount(); ++i) {
			CourtButton courtButton = (CourtButton) courtsPanel.getComponent(i);
			if(courtButton.startedDrag()) {
				return courtButton;
			}
		}
		return null;
	}
	
	public boolean addMatchToCourtButton(final Match match, CourtButton courtButton) {
		if(courtButton != null && courtButton.addMatch(match)) {
			if(match.getNextAvailableCourtOrder() > 0) {
				tournament.setMatchOrder(match, 0);
			}
			else {
				tournament.decrementMatchOrder();
			}
                        
			tournament.removeMatch(match);
                        int index = matches.getSelectedIndex();   
                        try    {((DefaultListModel<Match>) matches.getModel()).remove(index);}
                        catch (Exception ignore) { }
			courtButton.updateCourtStatus();
			switchToTab(TOURNAMENT_TAB, true);
			// allow the ui to update before starting the print dialog
			if(tournament.getAutoPrintMatches()) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						printMatch(match);
					}
				});
			}
			return true;
		}
		return false;
	}
	
	private synchronized void callComponentAction(Component comp, String key) {
		if(comp != null && comp instanceof JComponent) {
			ActionMap map = ((JComponent) comp).getActionMap();
			if(map != null) {
				Action action = map.get(key);
				if(action != null) {
					action.actionPerformed(null);
				}
			}
		}
	}
	
	private void createTabs() {
		displayPane.add(TOURNAMENT_TAB, createTournamentOverview());
		displayPane.setMnemonicAt(0, KeyEvent.VK_M);
		displayPane.add(EVENTS_TAB, createEventOverview());
		displayPane.setMnemonicAt(1, KeyEvent.VK_E);
		displayPane.add(PLAYERS_TAB, createPlayerOverview());
		displayPane.setMnemonicAt(2, KeyEvent.VK_P);
		displayPane.add(TEAMS_TAB, createTeamOverview());
		displayPane.setMnemonicAt(3, KeyEvent.VK_T);
		displayPane.add(MATCHES_TAB, createMatchOverview());
		displayPane.setMnemonicAt(4, KeyEvent.VK_C);
		// adding a change listener which will trigger the update action each time a tab is selected
		displayPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				if(tournament == null) {
					return;
				}
				callComponentAction(displayPane.getSelectedComponent(), UPDATE_ACTION);
			}
		});
	}
	
	private Component createTournamentOverview() {
		JPanel main = new JPanel(new GridBagLayout());
		main.setBackground(backgroundColor);
		courtsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
		courtsPanel.setBackground(backgroundColor);
		JScrollPane scrollPane = new JScrollPane(courtsPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(0, 150));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(20);
		JPanel courtsPanelWrapper = new JPanel(new BorderLayout());
		courtsPanelWrapper.add(scrollPane, BorderLayout.CENTER);
		main.add(courtsPanelWrapper,  new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0, 0, 0), 0, 0));
		// adding a panel for additional match actions
		final MatchActionPanel buttonsPanel = new MatchActionPanel(ui, this);
		scrollPane = new JScrollPane(buttonsPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(0, 25));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		JPanel buttonsPanelWrapper = new JPanel(new BorderLayout());
		buttonsPanelWrapper.add(scrollPane, BorderLayout.CENTER);
		main.add(buttonsPanelWrapper, new GridBagConstraints(0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 0, 5, 0), 0, 0));
		final DefaultListModel<Match> listModel = new DefaultListModel<Match>();
		matches = new JList<Match>(listModel);
		matches.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		matches.setLayoutOrientation(JList.VERTICAL);
		matches.setVisibleRowCount(-1);
		matches.setBackground(backgroundColor);
		final List<Match> relatedMatches = new ArrayList<Match>();
		// setting the selection color to be 50% transparent
		Color color = matches.getSelectionBackground();
		matches.setSelectionBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 127));
		matches.setSelectionForeground(Color.BLACK);
		final Color lineColor = new Color(192, 192, 192, 255);
		matches.setFont(matches.getFont().deriveFont(18.0f));
		matches.setCellRenderer(new FormattedListCellRenderer(false) {
			private final Color GREEN = new Color(0x2D9900);
			private final Color BLUE = new Color(0x3048FF);
			private final Color NOT_AVAILABLE = new Color(0x7FB7B7B7, true);
			private final Color AVAILABLE = new Color(0x647FFF8E, true);
			private final Color RELATED = new Color(0xBFFFDD38, true);
			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				setBorder(BorderFactory.createMatteBorder(index == 0 ? 1 : 0, 0, 1, 0, lineColor));
				Match match = (Match) value;
				// grey out the cell if a match can't start and it isn't selected
				if(!isSelected && !match.canStartMatch()) {
					setBackground(NOT_AVAILABLE);
				}
				if(match.getRequestedDate() != null) {
					addText(" ");
					addText(GenericUtils.dateToString(match.getRequestedDate(), "hh:mm a"), isSelected ? Color.BLACK : BLUE);
					addText(" -");
				}
				else if(match.getEstimatedDate() != null) {
					addText(" ");
					addText(GenericUtils.dateToString(match.getEstimatedDate(), "hh:mm a"), GREEN);
					addText(" -");
				}
				addItalic(" " + match.getEvent().getName());
				if(match.getEvent().showDisplayLevel()) {
					addItalic(" " + match.getLevel());
				}
				if(match.getMatchDescription() != null) {
					addBoldItalic(" " + match.getMatchDescription());
				}
				if(match.getIndex() > 0) {
					addItalic(" #" + match.getIndex());
				}
				addText(" - ");
				String tooltip = "";
				Team team = match.getTeam1();
				if(team == null) {
					String wait = match.getToTeam1MatchDescription();
					addText(wait, Color.RED);
					tooltip = match.getToTeam1MatchLongDiscription(wait);
				}
				else {
					String status = getTeamStatus(team);
					addText(team.getName(), status == null ? Color.BLACK : Color.RED);
					tooltip = status == null ? "" : status;
				}
				if(match.getT1ForfeitOnStart()) {
					addText(" " + WALKOVER, isSelected ? Color.BLACK : BLUE);
				}
				addText(" vs ");
				team = match.getTeam2();
				if(team == null) {
					String wait = match.getToTeam2MatchDescription();
					addText(wait, Color.RED);
					if(!tooltip.isEmpty()) {
						tooltip += " ";
					}
					tooltip += match.getToTeam2MatchLongDiscription(wait);
				}
				else {
					String status = getTeamStatus(team);
					addText(team.getName(), status == null ? Color.BLACK : Color.RED);
					if(!tooltip.isEmpty() && status != null) {
						tooltip += " ";
					}
					tooltip += status == null ? "" : status;
				}
				if(match.getT2ForfeitOnStart()) {
					addText(" " + WALKOVER, isSelected ? Color.BLACK : BLUE);
				}
				setToolTipText(tooltip.isEmpty() ? "Available to play." : tooltip);
				// show if this match can be played now
				if(!isSelected && match.canStartMatch()) {
					int freeCourts = 0;
					for(Court court : tournament.getCourts()) {
						if(court.isAvailable() && court.isUsable()) {
							++freeCourts;
						}
					}
					for(int i = 0; freeCourts > 0 && i < index; ++i) {
						if(listModel.get(i).canStartMatch()) {
							--freeCourts;
						}
					}
					if(freeCourts > 0) {
						setBackground(AVAILABLE);
					}
				}
				// giving a visual indicator of the court order
				if(match.getNextAvailableCourtOrder() > 0) {
					String courtText = " - ";
					if(match.getNextAvailableCourtOrder() == 1) {
						courtText += "Next Available";
					}
					else if(match.getNextAvailableCourtOrder() == 2) {
						courtText += "2nd Available";
					}
					else if(match.getNextAvailableCourtOrder() == 3) {
						courtText += "3rd Available";
					}
					else {
						courtText += match.getNextAvailableCourtOrder() + "th Available";
					}
					addText(courtText, isSelected ? Color.BLACK : BLUE);
				}
				// check to see if this match is related to the selected match
				if(relatedMatches.contains(match)) {
					setBackground(RELATED);
				}
				return this;
			}
			
			private String getTeamStatus(Team team) {
				if(!team.isValid()) {
					return team.getName() + " is invalid.";
				}
				Date currentDate = new Date();
				String status = "";
				for(Player player : team.getPlayers()) {
					if(!player.isCheckedIn()) {
						status += " " + player.getName() + " is absent.";
						continue;
					}
					if(player.isInGame()) {
						status += " " + player.getName() + " is playing.";
						continue;
					}
					Date delay = player.getRequestedDelay();
					if(delay != null && delay.before(currentDate)) {
						player.setRequestedDelay(null);
						modified();
					}
					else if(delay != null) {
						status += " " + player.getName() + " requested a delay until " + GenericUtils.dateToString(delay, "hh:mm a") + ".";
						continue;
					}
					Date lastPlay = player.getLastMatchTime();
					if(lastPlay != null && currentDate.getTime() - lastPlay.getTime() < tournament.getLongTimeBetweenMatches()) {
						status += " " + player.getName() + " last played at " + GenericUtils.dateToString(lastPlay, "hh:mm a") + ".";
						continue;
					}
				}
				return status.isEmpty() ? null : status.trim();
			}
		});
		matches.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				relatedMatches.clear();
				Match match = matches.getSelectedValue();
				if(match != null && (match.getTeam1() == null || match.getTeam2() == null)) {
					relatedMatches.addAll(match.getRelatedMatches());
				}
				relatedMatches.remove(match);
				matches.repaint();
				buttonsPanel.setMatch(match);
			}
		});
		matches.setDragEnabled(true);
		matches.setTransferHandler(new TransferHandler() {
			protected Transferable createTransferable(JComponent comp) {
				return new StringSelection(CourtButton.MATCH_TEXT);
			}
			
			public int getSourceActions(JComponent c) {
				return TransferHandler.COPY;
			}
			
			public boolean canImport(TransferSupport info) {
				return isMatchDrop(info);
			}
			
			public boolean importData(TransferSupport info) {
				if(!canImport(info)) {
					return false;
				}
				CourtButton courtButton = getStartCourtForSwap();
				Match match = courtButton.undoMatch();
				if(match == null) {
					return false;
				}
				tournament.addMatches(Arrays.asList(match));
				switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
				return true;
			}
			
			private boolean isMatchDrop(TransferSupport info) {
				CourtButton courtButton = getStartCourtForSwap();
				if(courtButton == null || courtButton.isAvailableCourt()) {
					return false;
				}
				try {
					return info.isDrop() && CourtButton.MATCH_TEXT.equals(info.getTransferable().getTransferData(DataFlavor.stringFlavor));
				}
				catch(Exception e) {}
				return false;
			}
		});
		matches.setDropMode(DropMode.ON);
		// pressing enter on a match will add it to a free court
		matches.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				int keyCode = event.getKeyCode();
				Match match = matches.getSelectedValue();
				if(match == null) {
					return;
				}
				if(keyCode == KeyEvent.VK_0 || keyCode == KeyEvent.VK_NUMPAD0 || keyCode == KeyEvent.VK_DELETE || keyCode == KeyEvent.VK_BACK_SPACE) {
					event.consume();
					tournament.setMatchOrder(match, 0);
					switchToTab(TOURNAMENT_TAB, true);
					return;
				}
				if(!match.canStartMatch()) {
					return;
				}
				if(keyCode == KeyEvent.VK_ENTER) {
					event.consume();
					for(Component comp : courtsPanel.getComponents()) {
						CourtButton courtButton = (CourtButton) comp;
						if(!courtButton.isAvailableCourt() || !courtButton.isUsableCourt()) {
							continue;
						}
						if(addMatchToCourtButton(match, courtButton)) {
							return;
						}
					}
					if(match.getNextAvailableCourtOrder() == 0) {
						int nextAvailable = tournament.getNextFreeCourt();
						if(nextAvailable > 0) {
							match.setNextAvailableCourtOrder(nextAvailable);
							switchToTab(TOURNAMENT_TAB, true);
						}
					}
				}
				else if(keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
					event.consume();
					tournament.setMatchOrder(match, -(KeyEvent.VK_0 - keyCode));
					switchToTab(TOURNAMENT_TAB, true);
				}
				else if(keyCode >= KeyEvent.VK_NUMPAD1 && keyCode <= KeyEvent.VK_NUMPAD9) {
					event.consume();
					tournament.setMatchOrder(match, -(KeyEvent.VK_NUMPAD0 - keyCode));
					switchToTab(TOURNAMENT_TAB, true);
				}
			}
		});
		JPanel matchesWrapper = new JPanel(new BorderLayout());
		scrollPane = new JScrollPane(matches);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(0, 100));
		matchesWrapper.add(scrollPane, BorderLayout.CENTER);
		main.add(matchesWrapper, new GridBagConstraints(0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 10, 0), 0, 0));
		final JPanel eventsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
		eventsPanel.setBackground(backgroundColor);
		scrollPane = new JScrollPane(eventsPanel);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.setPreferredSize(new Dimension(0, 65));
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(5);
		JPanel eventsPanelWrapper = new JPanel(new BorderLayout());
		eventsPanelWrapper.add(scrollPane, BorderLayout.CENTER);
		main.add(eventsPanelWrapper,  new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 0, 10, 0), 0, 0));
		main.getActionMap().put(REFRESH_ACTION,  new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				for(int i = 0; i < courtsPanel.getComponentCount(); ++i) {
					((CourtButton) courtsPanel.getComponent(i)).updateCourtStatus();
				}
				matches.repaint();
				for(int i = 0; i < eventsPanel.getComponentCount(); ++i) {
					((EventButton) eventsPanel.getComponent(i)).updateStatus();
				}
			}
		});
		main.getActionMap().put(UPDATE_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				ui.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				for(int i = 0; i < courtsPanel.getComponentCount(); ++i) {
					((CourtButton) courtsPanel.getComponent(i)).updateCourtStatus();
				}
				listModel.clear();
				for(Match match : tournament.getMatches()) {
					listModel.addElement(match);
				}
				buttonsPanel.update();
				boolean modifiedEvents = false;
				ArrayList<Event> events = new ArrayList<Event>(tournament.getEvents());
				int i;
				for(i = 0; i < eventsPanel.getComponentCount(); ++i) {
					EventButton button = (EventButton) eventsPanel.getComponent(i);
					if(events.size() <= i) {
						eventsPanel.remove(i--);
						modifiedEvents = true;
						continue;
					}
					Event tEvent = events.get(i);
					if(button.getEvent().equals(tEvent)) {
						button.updateStatus();
						continue;
					}
					// check to see if we are adding an event or removing an event
					if(events.contains(button.getEvent())) {
						eventsPanel.add(new EventButton(tEvent, teamSelector, resultDialog), i);
					}
					else {
						eventsPanel.remove(i--);
					}
					modifiedEvents = true;
				}
				for(; i < events.size(); ++i) {
					eventsPanel.add(new EventButton(events.get(i), teamSelector, resultDialog));
					modifiedEvents = true;
				}
				if(modifiedEvents) {
					eventsPanel.revalidate();
					eventsPanel.repaint();
				}
				ui.setCursor(null);
			}
		});
		main.getActionMap().put(NEW_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				courtsPanel.removeAll();
				eventsPanel.removeAll();
				listModel.clear();
				if(tournament != null) {
					for(Court court : tournament.getCourts()) {
						courtsPanel.add(new CourtButton(court, TournamentViewManager.this, courtDialog));
					}
					for(Match match : tournament.getMatches()) {
						listModel.addElement(match);
					}
					for(Event tEvent : tournament.getEvents()) {
						eventsPanel.add(new EventButton(tEvent, teamSelector, resultDialog));
					}
				}
				courtsPanel.revalidate();
				courtsPanel.repaint();
				eventsPanel.revalidate();
				eventsPanel.repaint();
			}
		});
		return main;
	}
	
	private Component createEventOverview() {
		JPanel events = new JPanel();
		events.setBackground(backgroundColor);
		events.setLayout(new BorderLayout());
		final JScrollPane scrollPane = new JScrollPane();
		eventCanvas = new EventBracketCanvas(36, 10, 10, 14.0f, 5, scrollPane);
		eventCanvas.setBackground(backgroundColor);
		scrollPane.setViewportView(eventCanvas);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		scrollPane.getVerticalScrollBar().setUnitIncrement(36);
		scrollPane.getHorizontalScrollBar().setUnitIncrement(36);
		events.add(scrollPane, BorderLayout.CENTER);
		final JComboBox<GenericWrapper<EventPainter>> filter = new JComboBox<GenericWrapper<EventPainter>>();
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				GenericWrapper<EventPainter> selected = (GenericWrapper<EventPainter>) filter.getSelectedItem();
				if(selected == null) {
					eventCanvas.setEventPainter(null);
				}
				else {
					eventCanvas.setEventPainter(selected.getValue());
				}
			}
		});
		filter.setEnabled(false);
		events.add(filter, BorderLayout.PAGE_START);
		events.getActionMap().put(REFRESH_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				eventCanvas.repaint();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						scrollPane.revalidate();
					}
				});
			}
		});
		events.getActionMap().put(UPDATE_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				boolean containsSelectedItem = false;
				GenericWrapper<EventPainter> selected = (GenericWrapper<EventPainter>) filter.getSelectedItem();
				filter.removeAllItems();
				for(Event tEvent : tournament.getEvents()) {
					if(tEvent.isStarted()) {
						for(String level : tEvent.getDisplayLevels()) {
							for(EventPainter eventPainter : tEvent.getEventPainters(level, tournament.getDisableBracketPooling())) {
								if(selected != null && eventPainter.equals(selected.getValue())) {
									containsSelectedItem = true;
								}
								filter.addItem(new GenericWrapper<EventPainter>(eventPainter));
							}
						}
					}
				}
				if(filter.getItemCount() > 0) {
					filter.setEnabled(true);
					if(containsSelectedItem) {
						filter.setSelectedItem(selected);
					}
					else {
						filter.setSelectedIndex(0);
					}
					filter.requestFocus();
				}
				else {
					filter.setEnabled(false);
					eventCanvas.setEventPainter(null);
				}
			}
		});
		events.getActionMap().put(NEW_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				eventCanvas.setEventPainter(null);
				filter.removeAllItems();
				if(tournament != null) {
					filter.setEnabled(true);
					for(Event tEvent : tournament.getEvents()) {
						for(String level : tEvent.getDisplayLevels()) {
							if(tEvent.isStarted()) {
								for(EventPainter eventPainter : tEvent.getEventPainters(level, tournament.getDisableBracketPooling())) {
									filter.addItem(new GenericWrapper<EventPainter>(eventPainter));
								}
							}
						}
					}
				}
				if(filter.getItemCount() == 0) {
					filter.setEnabled(false);
				}
			}
		});
		return events;
	}
	
	private Component createPlayerOverview() {
		JPanel players = new JPanel(new BorderLayout());
		players.setBackground(backgroundColor);
		JTable playersTable = new JTable();
		playersTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// setting the selection color to be 50% transparent
		Color color = playersTable.getSelectionBackground();
		playersTable.setSelectionBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 127));
		playersTable.setSelectionForeground(Color.BLACK);
		final GenericTableModel<Player> tableModel = new GenericTableModel<Player>(this);
		tableModel.addColumn("Upcoming Matches", true, Player.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player;
			}
		});
		tableModel.addColumn("Status", false, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				if(!player.isInGame() && player.isCheckedIn()) {
					String estimatedTime = player.getEstimatedDate() != null ? GenericUtils.dateToString(player.getEstimatedDate(), "hh:mm a") : "";
					return GenericUtils.html(GenericUtils.color(estimatedTime, "green"));
				}
				else if(player.isCheckedIn()) {
					String status = PLAYING_STATUS;
					for(Court court : tournament.getCourts()) {
						Match match = court.getCurrentMatch();
						if(match != null && match.getPlayers().contains(player)) {
							status += " (Court " + court.getId() + ")";
							break;
						}
					}
					return GenericUtils.html(GenericUtils.color(status, "red"));
				}
				else {
					return GenericUtils.html(GenericUtils.color(NOT_CHECKED_IN_STATUS, "red"));
				}
			}
		}, true);
		tableModel.addColumn("Name", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getName();
			}
			
			public void setValue(Object value, Player player) {
				if(value != null && !((String) value).isEmpty()) {
					player.setName(((String) value).trim());
				}
			}
		});
		tableModel.addColumn("Gender", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.isMale() ? "Male" : "Female";
			}
			
			public void setValue(Object value, Player player) {
				player.setIsMale("Male".equals(value));
			}
		});
		tableModel.addColumn("Checked In", true, Boolean.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getCheckInRawValue();
			}
			
			public void setValue(Object value, Player player) {
				player.setCheckedIn((Boolean) value);
			}
		}, Arrays.asList(1));
		tableModel.addColumn("Amount Paid", true, Double.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getAmountPaid();
			}
			
			public void setValue(Object value, Player player) {
				player.setAmountPaid((Double) value);
			}
		});
		tableModel.addColumn("Amount Due", true, Double.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getAmountDue();
			}
			
			public void setValue(Object value, Player player) {
				player.setAmountDue((Double) value);
			}
		});
		tableModel.addColumn("Phone Number", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getPhoneNumber();
			}
			
			public void setValue(Object value, Player player) {
				player.setPhoneNumber((String) value);
			}
		});
		tableModel.addColumn("Events", true, Set.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getEvents();
			}
			
			public void setValue(Object value, Player player) {
				player.setEvents((Set<String>) value);
			}
		});
		tableModel.addColumn("Level", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getLevel();
			}
			
			public void setValue(Object value, Player player) {
				player.setLevel((String) value);
			}
		});
		tableModel.addColumn("Club", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getClub();
			}
			
			public void setValue(Object value, Player player) {
				player.setClub((String) value);
			}
		});
		tableModel.addColumn("Membership Number", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getMembershipNumber();
			}
			
			public void setValue(Object value, Player player) {
				player.setMembershipNumber((String) value);
			}
		});
		tableModel.addColumn("Date Of Birth", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return GenericUtils.dateToString(player.getDateOfBirth(), "MM/dd/yyyy");
			}
			
			public void setValue(Object value, Player player) {
				player.setDateOfBirth(GenericUtils.stringToDate((String) value, "MM/dd/yy"));
			}
		});
		tableModel.addColumn("Address", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getAddress();
			}
			
			public void setValue(Object value, Player player) {
				player.setAddress((String) value);
			}
		});
		tableModel.addColumn("Email", true, String.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getEmail();
			}
			
			public void setValue(Object value, Player player) {
				player.setEmail((String) value);
			}
		});
		tableModel.addColumn("Last Match", false, Date.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player.getLastMatchTime();
			}
		}, true);
		tableModel.addColumn("Requested Delay", true, Date.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				Date date = player.getRequestedDelay();
				// clear the delay request if it has passed
				if(date != null && date.before(new Date())) {
					player.setRequestedDelay(null);
					modified();
					return null;
				}
				return date;
			}
			
			public void setValue(Object value, Player player) {
				player.setRequestedDelay((Date) value);
			}
		}, true);
		tableModel.addColumn("Delete", true, Player.class, new GenericValue<Player>() {
			public Object getValue(Player player) {
				return player;
			}
			
			public void setValue(Object value, final Player player) {
				String result = tournament.removePlayer(player);
				if(result == null) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							tableModel.removeRow(tableModel.indexOf(player));
						}
					});
				}
				else {
					JOptionPane.showMessageDialog(ui, result, "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		playersTable.setModel(tableModel);
		final TableRowSorter<GenericTableModel<Player>> rowSorter = new TableRowSorter<GenericTableModel<Player>>(tableModel);
		playersTable.setRowSorter(rowSorter);
		// setting the default renderer for dates
		playersTable.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setValue(GenericUtils.dateToString((Date) value, "MM/dd hh:mm:ss a"));
				return this;
			}
		});
		TableColumnModel columnModel = playersTable.getColumnModel();
		// setting the editor for gender
		columnModel.getColumn(3).setCellEditor(new DefaultCellEditor(new JComboBox<String>(new String[]{"Male", "Female"})));
		// setting the renderer for amount paid and amount due
		DefaultTableCellRenderer currencyRenderer = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setValue(NumberFormat.getCurrencyInstance().format((Double) value));
				int index = table.convertRowIndexToModel(row);
				if(index != -1 && !tableModel.getData(index).paidInFull()) {
					setForeground(Color.RED);
				}
				else {
					setForeground(Color.BLACK);
				}
				return this;
			}
		};
		columnModel.getColumn(5).setCellRenderer(currencyRenderer);
		columnModel.getColumn(6).setCellRenderer(currencyRenderer);
		// setting the editor for the player events
		columnModel.getColumn(8).setCellEditor(new EventEditor(ui, this, "No valid events detected."));
		// setting the editor for the player level
		final JComboBox<String> levelEditorCB = new JComboBox<String>();
		columnModel.getColumn(9).setCellEditor(new DefaultCellEditor(levelEditorCB));
		// setting the editor for requested delay
		columnModel.getColumn(16).setCellEditor(new TimeEditor(ui));
		// setting the editor and renderer for upcoming matches
		columnModel.getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
			private JButton button = new JButton("");
			
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return button;
			}
		});
		columnModel.getColumn(0).setCellEditor(new InfoAction(ui) {
			protected String generateActionMessage(Object value) {
				return tournament.getUpcomingMatchesForPlayer(((Player) value));
			}

			protected String generateActionTitle(Object value) {
				return "Upcoming matches for " + ((Player) value).getName();
			}
		});
		// setting the editor and renderer for delete
		columnModel.getColumn(17).setCellRenderer(new DefaultTableCellRenderer() {
			private JButton button = new JButton("");
			
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return button;
			}
		});
		columnModel.getColumn(17).setCellEditor(new ButtonAction(ui) {
			protected String generateActionMessage(Object value) {
				return "Are you sure you want to delete " + ((Player) value).getName() + "?";
			}

			protected String generateActionTitle(Object value) {
				return "Delete Player";
			}
		});
		JScrollPane scrollPane = new JScrollPane(playersTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		playersTable.setFillsViewportHeight(true);
		players.add(scrollPane, BorderLayout.CENTER);
		final JTextField filter = new JTextField();
		filter.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
		filter.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent event) {
				filter.selectAll();
			}
		});
		filter.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.VK_ENTER) {
					filter.selectAll();
				}
				try {
					// commas count as AND, slashes count as OR
					String text = filter.getText();
					if(text == null) {
						text = "";
					}
					ArrayList<RowFilter<Object, Object>> parameters = new ArrayList<RowFilter<Object, Object>>();
					for(String andParam : filter.getText().split(",")) {
						if(andParam == null || andParam.trim().isEmpty()) {
							continue;
						}
						ArrayList<RowFilter<Object, Object>> list = new ArrayList<RowFilter<Object, Object>>();
						for(String orParam : andParam.split("[\\\\/]")) {
							if(orParam == null || orParam.trim().isEmpty()) {
								continue;
							}
							list.add(RowFilter.regexFilter("(?i)" + orParam));
						}
						if(!list.isEmpty()) {
							parameters.add(RowFilter.orFilter(list));
						}
					}
					rowSorter.setRowFilter(parameters.isEmpty() ? null : RowFilter.andFilter(parameters));
				}
				catch(Exception e) {
					rowSorter.setRowFilter(null);
				}
			}
		});
		filter.setEnabled(false);
		players.add(filter, BorderLayout.PAGE_START);
		players.getActionMap().put(REFRESH_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				tableModel.update();
			}
		});
		players.getActionMap().put(UPDATE_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				tableModel.addData(tournament.getNewPlayers());
				refreshCurrentTab();
				// setting the focus to the text field
				filter.requestFocus();
			}
		});
		players.getActionMap().put(NEW_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				levelEditorCB.removeAllItems();
				levelEditorCB.addItem(null);
				filter.setText(null);
				rowSorter.setRowFilter(null);
				rowSorter.setSortKeys(null);
				if(tournament == null) {
					filter.setEnabled(false);
					tableModel.setData(null);
				}
				else {
					filter.setEnabled(true);
					for(String level : tournament.getLevels()) {
						levelEditorCB.addItem(level);
					}
					tableModel.setData(tournament.getPlayers());
				}
			}
		});
		return players;
	}
	
	private Component createTeamOverview() {
		JPanel teams = new JPanel(new BorderLayout());
		teams.setBackground(backgroundColor);
		JTable teamsTable = new JTable();
		teamsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// setting the selection color to be 50% transparent
		Color color = teamsTable.getSelectionBackground();
		teamsTable.setSelectionBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 127));
		teamsTable.setSelectionForeground(Color.BLACK);
		final GenericTableModel<Team> tableModel = new GenericTableModel<Team>(this);
		tableModel.addColumn("Status", false, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				if(team.isWithdrawn()) {
					return GenericUtils.html(GenericUtils.color(WITHDRAWN_STATUS, "red"));
				}
				if(!team.isValid()) {
					return GenericUtils.html(GenericUtils.color(INVALID_STATUS, "red"));
				}
				if(!team.isCheckedIn()) {
					return GenericUtils.html(GenericUtils.color(NOT_CHECKED_IN_STATUS, "red"));
				}
				if(team.isInMatch()) {
					return GenericUtils.html(GenericUtils.color(PLAYING_STATUS, "red"));
				}
				if(!team.canStartMatch()) {
					return GenericUtils.html(GenericUtils.color(UNAVAILABLE_STATUS, "red"));
				}
				String estimatedTime = team.getEstimatedDate() != null ? GenericUtils.dateToString(team.getEstimatedDate(), "hh:mm a") : "";
				return GenericUtils.html(GenericUtils.color(estimatedTime, "green"));
			}
		}, true);
		tableModel.addColumn("Type", false, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team.getTeamType();
			}
		});
		tableModel.addColumn("Event", false, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				if(tournament == null || team == null || !team.getInEvent()) {
					return null;
				}
				for(Event event : tournament.getEvents()) {
					if(!event.isStarted() || !event.getTeamFilter().isValidTeam(team)) {
						continue;
					}
					if(event.getTeams().contains(team)) {
						return event.getName();
					}
				}
				return null;
			}
		}, true);
		tableModel.addColumn("Name", true, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team.getName();
			}
			
			public void setValue(Object value, Team team) {
				team.setName((String) value);
			}
		}, true);
		tableModel.addColumn("Players", true, List.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team.getPlayers();
			}
			
			public void setValue(Object value, Team team) {
				List<Player> list = (List<Player>) value;
				if(list == null || list.size() != team.getNumberOfPlayers()) {
					return;
				}
				for(int i = 0; i < team.getNumberOfPlayers(); ++i) {
					team.setPlayer(i, list.get(i));
				}
			}
		}, true, Arrays.asList(0, 3, 5));
		tableModel.addColumn("Club", false, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team.getClub();
			}
		}, true);
		tableModel.addColumn("Seed", true, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team.getSeed();
			}
			
			public void setValue(Object value, Team team) {
				team.setSeed((String) value);
			}
		});
		tableModel.addColumn("Record", false, String.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team.getMatchesWon() + " - " + team.getMatchesLost();
			}
		}, true);
		tableModel.addColumn("Delete", true, Team.class, new GenericValue<Team>() {
			public Object getValue(Team team) {
				return team;
			}
			
			public void setValue(Object value, final Team team) {
				String result = tournament.removeTeam(team);
				if(result == null) {
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							tableModel.removeRow(tableModel.indexOf(team));
							if(!tournament.getTeamTypes().contains(team.getClass())) {
								switchToTab(TEAMS_TAB, true);
							}
						}
					});
				}
				else {
					JOptionPane.showMessageDialog(ui, result, "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		teamsTable.setModel(tableModel);
		final TableRowSorter<GenericTableModel<Team>> rowSorter = new TableRowSorter<GenericTableModel<Team>>(tableModel);
		teamsTable.setRowSorter(rowSorter);
		TableColumnModel columnModel = teamsTable.getColumnModel();
		// setting the editor for teams
		final TeamEditor teamEditor = new TeamEditor(ui, "This team does not accept players.");
		columnModel.getColumn(4).setCellEditor(teamEditor);
		// setting the renderer for record
		columnModel.getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				setHorizontalAlignment(SwingConstants.CENTER);
				return this;
			}
		});
		// setting the editor and renderer for delete
		columnModel.getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
			private JButton button = new JButton("");

			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				return button;
			}
		});
		columnModel.getColumn(8).setCellEditor(new ButtonAction(ui) {
			protected String generateActionMessage(Object value) {
				return "Are you sure you want to delete " + ((Team) value).getName() + "?";
			}

			protected String generateActionTitle(Object value) {
				return "Delete Team";
			}
		});
		// setting the preferred size by the percent * the screen width so swing will resize the columns properly
		int screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		columnModel.getColumn(0).setPreferredWidth(6 * screenWidth);
		columnModel.getColumn(1).setPreferredWidth(6 * screenWidth);
		columnModel.getColumn(2).setPreferredWidth(8 * screenWidth);
		columnModel.getColumn(3).setPreferredWidth(25 * screenWidth);
		columnModel.getColumn(4).setPreferredWidth(25 * screenWidth);
		columnModel.getColumn(5).setPreferredWidth(20 * screenWidth);
		columnModel.getColumn(6).setPreferredWidth(3 * screenWidth);
		columnModel.getColumn(7).setPreferredWidth(4 * screenWidth);
		columnModel.getColumn(8).setPreferredWidth(3 * screenWidth);
		JScrollPane scrollPane = new JScrollPane(teamsTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		teamsTable.setFillsViewportHeight(true);
		teams.add(scrollPane, BorderLayout.CENTER);
		final JComboBox<String> filter = new JComboBox<String>();
		filter.setRenderer(new DefaultListCellRenderer() {
			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				String actualValue = " ";
				if(!(value == null || ((String) value).isEmpty())) {
					actualValue = tournament.getTeamByType((String) value).getTeamTypeDescription();
				}
				return super.getListCellRendererComponent(list, actualValue, index, isSelected, cellHasFocus);
			}
		});
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String type = (String) filter.getSelectedItem();
				if(type != null) {
					try {
						if(type.isEmpty()) {
							rowSorter.setRowFilter(null);
						}
						else {
							rowSorter.setRowFilter(RowFilter.regexFilter("^\\Q" + type + "\\E$", 1));
						}
					}
					catch(Exception e) {
						rowSorter.setRowFilter(null);
					}
				}
			}
		});
		filter.setEnabled(false);
		teams.add(filter, BorderLayout.PAGE_START);
		teams.getActionMap().put(REFRESH_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				tableModel.update();
			}
		});
		teams.getActionMap().put(UPDATE_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				teamEditor.setPlayers(tournament.getPlayers());
				tableModel.addData(tournament.getNewTeams());
				refreshCurrentTab();
				String selected = (String) filter.getSelectedItem();
				filter.removeAllItems();
				filter.addItem("");
				for(String key : tournament.getTeamTypes()) {
					filter.addItem(key);
				}
				if(selected != null) {
					filter.setSelectedItem(selected);
				}
				else {
					filter.setSelectedIndex(0);
				}
				filter.requestFocus();
			}
		});
		teams.getActionMap().put(NEW_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				rowSorter.setSortKeys(null);
				filter.removeAllItems();
				if(tournament == null) {
					teamEditor.setPlayers(null);
					filter.setEnabled(false);
					tableModel.setData(null);
				}
				else {
					teamEditor.setPlayers(tournament.getPlayers());
					// this should be the first item added and so it will fire the action listener and clear any previous filters
					filter.addItem("");
					for(String key : tournament.getTeamTypes()) {
						filter.addItem(key);
					}
					filter.setEnabled(true);
					tableModel.setData(tournament.getTeams());
				}
			}
		});
		return teams;
	}
	
	private Component createMatchOverview() {
		JPanel matches = new JPanel(new BorderLayout());
		matches.setBackground(backgroundColor);
		JTable matchesTable = new JTable();
		matchesTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		// setting the selection color to be 50% transparent
		Color color = matchesTable.getSelectionBackground();
		matchesTable.setSelectionBackground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 127));
		matchesTable.setSelectionForeground(Color.BLACK);
		matchesTable.setFillsViewportHeight(true);
		final GenericTableModel<Match> tableModel = new GenericTableModel<Match>(this);
		tableModel.addColumn("Event", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return match.getEvent().getName() + (match.getEvent().showDisplayLevel() ? " - " + match.getLevel() : "");
			}
		});
		tableModel.addColumn("Round", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return match.getMatchDescription();
			}
		});
		tableModel.addColumn("Teams", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return GenericUtils.html(getTeamDescription(match.getTeam1(), match.getWinner(), match.getTeam1Forfeit()) + 
						" - " + getTeamDescription(match.getTeam2(), match.getWinner(), match.getTeam2Forfeit()));
			}
			
			private String getTeamDescription(Team team, Team winner, boolean forfeit) {
				if(team == null) {
					return BYE;
				}
				else if(team.getName() == null) {
					return "";
				}
				String value = team.getName();
				if(team.getSeed() != null) {
					value += " [" + team.getSeed() + "]";
				}
				if(team.equals(winner)) {
					return GenericUtils.bold(value);
				}
				else if(forfeit) {
					value += " " + WALKOVER;
				}
				return value;
			}
		}, true);
		tableModel.addColumn("Score", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				String value = "";
				for(Game game : match.getGames()) {
					value += " " + getGameDescription(game);
					value = value.trim();
				}
				if((match.getTeam1Forfeit() || match.getTeam2Forfeit()) && match.getTeam1() != null && match.getTeam2() != null) {
					value += " " + WALKOVER;
				}
				return value;
			}
			
			private String getGameDescription(Game game) {
				if(game.getTeam1Score() < 0 || game.getTeam2Score() < 0) {
					return "";
				}
				return game.getTeam1Score() + "-" + game.getTeam2Score();
			}
		});
		tableModel.addColumn("Start", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return GenericUtils.dateToString(match.getStart(), "EEE MM/dd/yyyy hh:mm:ss a z");
			}
		});
		tableModel.addColumn("End", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return GenericUtils.dateToString(match.getEnd(), "EEE MM/dd/yyyy hh:mm:ss a z");
			}
		});
		tableModel.addColumn("Duration", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return GenericUtils.getDuration(match.getStart(), match.getEnd());
			}
		});
		tableModel.addColumn("Court", false, String.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				Court court = match.getCourt();
				if(court == null) {
					return null;
				}
				return court.getId();
			}
		});
		tableModel.addColumn("Undo", true, Match.class, new GenericValue<Match>() {
			public Object getValue(Match match) {
				return match;
			}
			
			public void setValue(Object value, Match match) {
				// reset all the reset match flags
				for(Team team : tournament.getTeams()) {
					team.setResetMatch(false);
				}
				// refresh all the completed matches
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						tableModel.setData(tournament.getCompletedMatches());
					}
				});
			}
		}, Arrays.asList(2, 3));
		matchesTable.setModel(tableModel);
		// setting the preferred size by the percent * the screen width so swing will resize the columns properly
		int screenWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
		TableColumnModel columnModel = matchesTable.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(10 * screenWidth);
		columnModel.getColumn(1).setPreferredWidth(3 * screenWidth);
		columnModel.getColumn(2).setPreferredWidth(25 * screenWidth);
		columnModel.getColumn(3).setPreferredWidth(10 * screenWidth);
		columnModel.getColumn(4).setPreferredWidth(20 * screenWidth);
		columnModel.getColumn(5).setPreferredWidth(20 * screenWidth);
		columnModel.getColumn(6).setPreferredWidth(5 * screenWidth);
		columnModel.getColumn(7).setPreferredWidth(4 * screenWidth);
		columnModel.getColumn(8).setPreferredWidth(3 * screenWidth);
		// set the renderer and editor for the undo column
		columnModel.getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
			private JButton button = new JButton();
			private JLabel label = new JLabel("");
			
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
				if(value == null) {
					return label;
				}
				Match match = (Match) value;
				if(match == null || match.getTeam1() == null || match.getTeam2() == null) {
					return label;
				}
				return button;
			}
		});
		columnModel.getColumn(8).setCellEditor(new MatchEditor(ui, this));
		matchesTable.setRowSorter(new TableRowSorter<GenericTableModel<Match>>(tableModel));
		JScrollPane scrollPane = new JScrollPane(matchesTable);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		matches.add(scrollPane, BorderLayout.CENTER);
		matches.getActionMap().put(UPDATE_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				tableModel.setData(tournament.getCompletedMatches());
			}
		});
		matches.getActionMap().put(NEW_ACTION, new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				if(tournament == null) {
					tableModel.setData(null);
				}
				else {
					tableModel.setData(tournament.getCompletedMatches());
				}
			}
		});
		return matches;
	}
}
