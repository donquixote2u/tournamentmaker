package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import ui.component.button.EventButton;
import ui.component.label.FormattedListCellRenderer;
import ui.main.TournamentUI;
import ui.main.TournamentViewManager;
import data.event.Event;
import data.event.RoundRobinEvent;
import data.player.Player;
import data.team.Team;
import data.tournament.TournamentUtils;

public class EventTeamSelector extends JDialog {
	private static final long serialVersionUID = -860682900368616436L;
	private static final String MAGIC_STRING = EventTeamSelector.class.getName() + ".MagicString." + (new Date()).getTime();
	private EventButton button;
	private Event currentEvent;
	private JTextField name;
	private JPanel upperTeams, lowerTeams;
	private JList<Team> teamsList;
	private DefaultListModel<Team> teams;
	private JCheckBox filterTeamsByLevel;
	private TournamentViewManager manager;
	private String teamSeed;
	
	@SuppressWarnings({"serial", "rawtypes"})
	public EventTeamSelector(final TournamentUI owner, TournamentViewManager manager) {
		super(owner, "Team Selector", true);
		this.manager = manager;
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration());
		setPreferredSize(new Dimension(dim.width - insets.left - insets.right, dim.height - insets.top - insets.bottom));
		JPanel root = new JPanel(new GridBagLayout());
		name = new JTextField();
		name.setFont(name.getFont().deriveFont(20.0f));
		root.add(name, new GridBagConstraints(0, 0, 3, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 5, 5), 0, 0));
		upperTeams = new JPanel(new GridBagLayout());
		JScrollPane scrollPane = new JScrollPane(upperTeams);
		root.add(scrollPane, new GridBagConstraints(0, 1, 1, 3, 0.33, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		lowerTeams = new JPanel(new GridBagLayout());
		scrollPane = new JScrollPane(lowerTeams);
		root.add(scrollPane, new GridBagConstraints(1, 1, 1, 3, 0.33, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		teams = new DefaultListModel<Team>();
		teamsList = new JList<Team>(teams);
		teamsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		teamsList.setLayoutOrientation(JList.VERTICAL);
		teamsList.setVisibleRowCount(-1);
		teamsList.setCellRenderer(new FormattedListCellRenderer(false) {
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if(value == null) {
					addText(TournamentViewManager.BYE, isSelected ? Color.WHITE : Color.BLACK);
				}
				else {
					addText(value.toString(), isSelected ? Color.WHITE : Color.BLACK);
				}
				if(isSelected) {
					addText(formatUserEntry(teamSeed), Color.WHITE);
				}
				return this;
			}
		});
		teamsList.setDragEnabled(true);
		teamsList.setTransferHandler(new TransferHandler() {
			protected Transferable createTransferable(JComponent comp) {
				return new StringSelection(MAGIC_STRING);
			}
			
			public int getSourceActions(JComponent c) {
				return TransferHandler.COPY;
			}
			
			public boolean canImport(TransferSupport info) {
				return isTeamDrop(info);
			}
			
			public boolean importData(TransferSupport info) {
				if(!canImport(info)) {
					return false;
				}
				getSelectedTeamLabel().setTeam(null, true);
				return true;
			}
			
			private boolean isTeamDrop(TransferSupport info) {
				TeamLabel teamLabel = getSelectedTeamLabel();
				if(teamLabel == null || teamLabel.team == null) {
					return false;
				}
				try {
					return info.isDrop() && MAGIC_STRING.equals(info.getTransferable().getTransferData(DataFlavor.stringFlavor));
				}
				catch(Exception e) {}
				return false;
			}
		});
		teamsList.setDropMode(DropMode.ON);
		teamsList.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				teamsList.clearSelection();
			}
		});
		teamsList.addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				// check for a selected TeamLabel
				TeamLabel teamLabel = getSelectedTeamLabel();
				if(teamLabel != null) {
					teamLabel.setSelected(false);
				}
				// clear the team seed
				teamSeed = "";
			}
		});
		teamsList.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent event) {
				int keyCode = event.getKeyCode();
				if(teamsList.getSelectedIndex() == -1) {
					return;
				}
				if(keyCode == KeyEvent.VK_BACK_SPACE) {
					event.consume();
					if(teamSeed.length() > 0) {
						teamSeed = teamSeed.substring(0, teamSeed.length() - 1);
						teamsList.repaint();
					}
				}
				else if(keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
					event.consume();
					teamSeed += -(KeyEvent.VK_0 - keyCode);
					teamsList.repaint();
				}
				else if(keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
					event.consume();
					teamSeed += -(KeyEvent.VK_NUMPAD0 - keyCode);
					teamsList.repaint();
				}
				else if(keyCode == KeyEvent.VK_ESCAPE) {
					event.consume();
					teamSeed = "";
					teamsList.repaint();
				}
				else if(keyCode == KeyEvent.VK_ENTER) {
					event.consume();
					TeamLabel teamLabel = getSeed(teamSeed);
					if(teamLabel == null) {
						teamSeed = "";
						teamsList.repaint();
					}
					else {
						teamLabel.setTeam(teamsList.getSelectedValue(), true);
					}
				}
			}
		});
		scrollPane = new JScrollPane(teamsList);
		root.add(scrollPane, new GridBagConstraints(2, 1, 1, 1, 0.33, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		filterTeamsByLevel = new JCheckBox("Filter Teams By Level");
		filterTeamsByLevel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				populateTeams();
			}
		});
		root.add(filterTeamsByLevel, new GridBagConstraints(2, 2, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
		JButton advanced = new JButton("Advanced Settings");
		advanced.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Event newEvent = ((TournamentUI) getOwner()).openEventDialog(currentEvent);
				if(newEvent == null) {
					return;
				}
				String nameValue = name.getText();
				newEvent.setFilterTeamByLevel(filterTeamsByLevel.isSelected());
				if(newEvent.getTeamFilter().getTeamType().equals(currentEvent.getTeamFilter().getTeamType())) {
					List<Team> currentTeams = getSetTeams();
					ArrayList<Team> newTeams = new ArrayList<Team>();
					for(int i = 0; i < newEvent.getNumberOfTeams(); ++i) {
						newTeams.add(i < currentTeams.size() ? currentTeams.get(i) : null);
					}
					newEvent.setTeams(newTeams);
				}
				currentEvent = newEvent;
				buildUI();
				name.setText(nameValue);
			}
		});
		root.add(advanced, new GridBagConstraints(2, 3, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 0, 5, 0), 0, 0));
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(root, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton preview = new JButton("Preview Event");
		preview.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String originalName = currentEvent.getName();
				if(!originalName.equals(name.getText()) && EventTeamSelector.this.manager.getTournament().getEvent(name.getText()) != null) {
					JOptionPane.showMessageDialog(owner, "Invalid/non-unique event name detected. Please fix before previewing the event.", "Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				boolean originalFilter = currentEvent.getFilterTeamByLevel();
				EventTeamSelector.this.button.setIsPreview(true);
				currentEvent.setName(name.getText());
				currentEvent.setFilterTeamByLevel(filterTeamsByLevel.isSelected());
				new EventPreviewDialog(owner, EventTeamSelector.this.manager, currentEvent, getSetTeams());
				currentEvent.setName(originalName);
				currentEvent.setFilterTeamByLevel(originalFilter);
				EventTeamSelector.this.button.setIsPreview(false);
			}
		});
		buttons.add(preview);
		JButton start = new JButton("Start Event");
		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				boolean renameEvent = !currentEvent.getName().equals(name.getText());
				if(renameEvent && EventTeamSelector.this.manager.getTournament().getEvent(name.getText()) != null) {
					JOptionPane.showMessageDialog(EventTeamSelector.this, "Invalid/non-unique event name detected. Please fix before starting the event.", "Error", JOptionPane.ERROR_MESSAGE);
					name.requestFocus();
					name.selectAll();
					return;
				}
				boolean originalFilter = currentEvent.getFilterTeamByLevel();
				currentEvent.setFilterTeamByLevel(filterTeamsByLevel.isSelected());
				List<Team> originalTeams = new ArrayList<Team>();
				originalTeams.addAll(currentEvent.getTeams());
				currentEvent.setTeams(getSetTeams());
				if(currentEvent.canStart()) {
					EventTeamSelector.this.manager.getTournament().replaceEvent(currentEvent);
					if(renameEvent) {
						EventTeamSelector.this.manager.getTournament().renameEvent(currentEvent, name.getText());
					}
					EventTeamSelector.this.dispose();
					EventTeamSelector.this.manager.getTournament().startEvent(currentEvent);
					EventTeamSelector.this.button.updateStatus();
					EventTeamSelector.this.manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
				}
				else {
					JOptionPane.showMessageDialog(EventTeamSelector.this, "Invalid teams detected. Please fix before starting the event.", "Error", JOptionPane.ERROR_MESSAGE);
					currentEvent.setFilterTeamByLevel(originalFilter);
					currentEvent.setTeams(originalTeams);
				}
			}
		});
		buttons.add(start);
		JButton delete = new JButton("Delete Event");
		delete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(JOptionPane.showConfirmDialog(EventTeamSelector.this, "Are you sure you want to delete this event?", "Delete Event", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					String result = EventTeamSelector.this.manager.getTournament().removeEvent(EventTeamSelector.this.button.getEvent());
					if(result == null) {
						EventTeamSelector.this.dispose();
						EventTeamSelector.this.manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
					}
					else {
						JOptionPane.showMessageDialog(EventTeamSelector.this, result, "Error", JOptionPane.ERROR_MESSAGE);
					}
				}
			}
		});
		buttons.add(delete);
		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				String newName = name.getText();
				boolean renameEvent = !currentEvent.getName().equals(name.getText());
				if(renameEvent && EventTeamSelector.this.manager.getTournament().getEvent(name.getText()) != null) {
					JOptionPane.showMessageDialog(EventTeamSelector.this, "Invalid/non-unique event name detected.", "Error", JOptionPane.ERROR_MESSAGE);
					name.requestFocus();
					name.selectAll();
					return;
				}
				EventTeamSelector.this.manager.getTournament().replaceEvent(currentEvent);
				if(renameEvent) {
					EventTeamSelector.this.manager.getTournament().renameEvent(currentEvent, newName);
				}
				currentEvent.setFilterTeamByLevel(filterTeamsByLevel.isSelected());
				currentEvent.setTeams(getSetTeams());
				EventTeamSelector.this.dispose();
				EventTeamSelector.this.manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
			}
		});
		buttons.add(save);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				EventTeamSelector.this.dispose();
			}
		});
		buttons.add(cancel);
		getContentPane().add(buttons, BorderLayout.PAGE_END);
	}
	
	public void show(EventButton button) {
		if(button == null || button.getEvent() == null) {
			return;
		}
		this.button = button;
		currentEvent = button.getEvent();
		buildUI();
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}
	
	private void buildUI() {
		name.setText(currentEvent.getName());
		filterTeamsByLevel.setSelected(currentEvent.getFilterTeamByLevel());
		filterTeamsByLevel.setVisible(!currentEvent.getLevels().isEmpty());
		List<Team> eventTeams = currentEvent.getTeams();
		upperTeams.removeAll();
		lowerTeams.removeAll();
		List<int[]> pairs = TournamentUtils.getPairings(currentEvent.getNumberOfTeams());
		for(int i = 0; i < currentEvent.getNumberOfTeams(); ++i) {
			JPanel panel = i % 2 == 0 ? upperTeams : lowerTeams;
			panel.add(new JLabel(pairs.get(i / 2)[i % 2] + ". "), new GridBagConstraints(0, i / 2, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 5, 5), 0, 0));
			GridBagConstraints constraint = new GridBagConstraints(1, i / 2, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 0, 5, 5), 0, 0);
			if(!eventTeams.isEmpty()) {
				panel.add(new TeamLabel(eventTeams.get(i)), constraint);
			}
			else {
				panel.add(new TeamLabel(null), constraint);
			}
		}
		populateTeams();
		pack();
		repaint();
		teamsList.requestFocusInWindow();
	}
	
	private void populateTeams() {
		ArrayList<Team> validTeams = new ArrayList<Team>();
		List<String> levels = currentEvent.getLevels();
		for(Team team : manager.getTournament().getTeams()) {
			if(team == null || !team.isValid() || team.getInEvent()) {
				continue;
			}
			if(filterTeamsByLevel.isSelected() && Collections.disjoint(levels, team.getLevels())) {
				continue;
			}
			if(!currentEvent.getTeamFilter().isValidTeam(team)) {
				continue;
			}
			boolean skipTeam = false;
			for(Player player : team.getPlayers()) {
				if(!player.getEvents().contains(currentEvent.getName())) {
					skipTeam = true;
					break;
				}
			}
			if(skipTeam) {
				continue;
			}
			validTeams.add(team);
		}
		// setting the team borders
		for(int i = 0; i < currentEvent.getNumberOfTeams(); ++i) {
			JLabel seed;
			TeamLabel teamLabel;
			if(i % 2 == 0) {
				seed = (JLabel) upperTeams.getComponent(i);
				teamLabel = (TeamLabel) upperTeams.getComponent(i + 1);
			}
			else {
				seed = (JLabel) lowerTeams.getComponent(i - 1);
				teamLabel = (TeamLabel) lowerTeams.getComponent(i);
			}
			// set the tooltip
			if(!(currentEvent instanceof RoundRobinEvent)) {
				String toolTip = "Playing against ";
				String otherSeed = ((JLabel) (i % 2 == 0 ? lowerTeams.getComponent(i) : upperTeams.getComponent(i - 1))).getText();
				toolTip += otherSeed.substring(0, otherSeed.length() - 2);
				toolTip += " (" + ((TeamLabel) (i % 2 == 0 ? lowerTeams.getComponent(i + 1) : upperTeams.getComponent(i))).getText() + ")";
				seed.setToolTipText(toolTip);
				teamLabel.setToolTipText(toolTip);
			}
			else {
				seed.setToolTipText(null);
				teamLabel.setToolTipText(null);
			}
			teamLabel.setBorder(teamLabel.defaultBorder);
			if(teamLabel.team == null) {
				continue;
			}
			// check for duplicate players
			boolean hasDuplicatePlayers = false;
			for(int j = 0; !hasDuplicatePlayers && j < currentEvent.getNumberOfTeams(); ++j) {
				Team team;
				if(j % 2 == 0) {
					team = ((TeamLabel) upperTeams.getComponent(j + 1)).team;
				}
				else {
					team = ((TeamLabel) lowerTeams.getComponent(j)).team;
				}
				if(team == null || teamLabel.team.equals(team)) {
					continue;
				}
				if(!Collections.disjoint(teamLabel.team.getPlayers(), team.getPlayers())) {
					hasDuplicatePlayers = true;
				}
			}
			// highlighting the invalid team
			if(hasDuplicatePlayers || !validTeams.contains(teamLabel.team)) {
				final Insets defaultInset = teamLabel.getInsets();
				@SuppressWarnings("serial")
				Border invalidBorder = new LineBorder(Color.RED, 1) {
					 public Insets getBorderInsets(Component c) {
						 return defaultInset;
					 }
					 
					 public Insets getBorderInsets(Component c, Insets insets) {
						 return defaultInset;
					 }
				};
				teamLabel.setBorder(invalidBorder);
			}
		}
		// populating the list of teams
		validTeams.removeAll(getSetTeams());
		validTeams.add(0, null);
		Collections.sort(validTeams, new Comparator<Team>() {
			public int compare(Team t1, Team t2) {
				if(t1 == null && t2 == null) {
					return 0;
				}
				if(t1 == null) {
					return -1;
				}
				if(t2 == null) {
					return 1;
				}
				String t1Seed = t1.getSeed();
				String t2Seed = t2.getSeed();
				if(t1Seed != null || t2Seed != null) {
					if(t1Seed == null) {
						return 1;
					}
					if(t2Seed == null) {
						return -1;
					}
					boolean s1 = false, s2 = false;
					try {
						Integer.parseInt(t1Seed);
						s1 = true;
					}
					catch(NumberFormatException e) {}
					try {
						Integer.parseInt(t2Seed);
						s2 = true;
					}
					catch(NumberFormatException e) {}
					if(s1 && s2) {
						int comp = Integer.parseInt(t1Seed) - Integer.parseInt(t2Seed);
						if(comp != 0) {
							return comp;
						}
					}
					if(s1) {
						return -1;
					}
					if(s2) {
						return 1;
					}
					int comp = t1Seed.compareToIgnoreCase(t2Seed);
					if(comp != 0) {
						return comp;
					}
				}
				String t1Name = t1.getName();
				String t2Name = t2.getName();
				if(t1Name == null && t2Name == null) {
					return 0;
				}
				if(t1Name == null) {
					return -1;
				}
				if(t2Name == null) {
					return 1;
				}
				return t1Name.compareToIgnoreCase(t2Name);
			}
		});
		teams.clear();
		for(Team team : validTeams) {
			teams.addElement(team);
		}
	}
	
	private List<Team> getSetTeams() {
		ArrayList<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < currentEvent.getNumberOfTeams(); ++i) {
			if(i % 2 == 0) {
				teams.add(((TeamLabel) upperTeams.getComponent(i + 1)).team);
			}
			else {
				teams.add(((TeamLabel) lowerTeams.getComponent(i)).team);
			}
		}
		return teams;
	}
	
	private TeamLabel getSelectedTeamLabel() {
		for(int i = 0; i < currentEvent.getNumberOfTeams(); ++i) {
			TeamLabel teamLabel;
			if(i % 2 == 0) {
				teamLabel = (TeamLabel) upperTeams.getComponent(i + 1);
			}
			else {
				teamLabel = (TeamLabel) lowerTeams.getComponent(i);
			}
			if(teamLabel.selected) {
				return teamLabel;
			}
		}
		return null;
	}
	
	private TeamLabel getSeed(String seed) {
		if(seed == null) {
			return null;
		}
		seed += ". ";
		for(int i = 0; i < currentEvent.getNumberOfTeams(); ++i) {
			JLabel label;
			TeamLabel teamLabel;
			if(i % 2 == 0) {
				label = (JLabel) upperTeams.getComponent(i);
				teamLabel = (TeamLabel) upperTeams.getComponent(i + 1);
			}
			else {
				label = (JLabel) lowerTeams.getComponent(i - 1);
				teamLabel = (TeamLabel) lowerTeams.getComponent(i);
			}
			if(seed.equals(label.getText())) {
				return teamLabel;
			}
		}
		return null;
	}
	
	private String formatUserEntry(String entry) {
		if(entry == null || entry.isEmpty()) {
			return "";
		}
		return " -> " + (entry.length() <= 3 ? entry : (entry.substring(0, 3) + "..."));
	}
	
	@SuppressWarnings("serial")
	private class TeamLabel extends JTextField {
		Team team;
		final Color selectedBackground = new Color(51, 153, 255, 255);
		Color defaultBackground;
		boolean selected;
		DragSource dragSource;
		String userEntry;
		Border defaultBorder;
		
		public TeamLabel(Team team) {
			super();
			setEditable(false);
			defaultBackground = getBackground();
			defaultBorder = getBorder();
			setTransferHandler(new TransferHandler() {
				protected Transferable createTransferable(JComponent comp) {
					return new StringSelection(MAGIC_STRING);
				}
				
				public int getSourceActions(JComponent c) {
					return TransferHandler.COPY;
				}
				
				public boolean canImport(TransferSupport info) {
					return isTeamDrop(info);
				}
				
				public boolean importData(TransferSupport info) {
					if(!canImport(info)) {
						return false;
					}
					TeamLabel teamLabel = getSelectedTeamLabel();
					if(teamLabel != null) {
						TeamLabel.this.swapTeams(teamLabel);
					}
					else {
						// we have to get the value directly from the jlist because the transferable object will create a new instance of team
						setTeam(teamsList.getSelectedValue(), true);
					}
					return true;
				}
				
				private boolean isTeamDrop(TransferSupport info) {
					TeamLabel teamLabel = getSelectedTeamLabel();
					if(teamLabel != null) {
						if(TeamLabel.this.equals(teamLabel)) {
							return false;
						}
					}
					else if(teamsList.getSelectedIndex() == -1) {
						return false;
					}
					try {
						return info.isDrop() && MAGIC_STRING.equals(info.getTransferable().getTransferData(DataFlavor.stringFlavor));
					}
					catch(Exception e) {}
					return false;
				}
			});
			dragSource = new DragSource();
			dragSource.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, new DragGestureListener() {
				public void dragGestureRecognized(DragGestureEvent event) {
					setSelectionStart(0);
					setSelectionEnd(0);
					dragSource.startDrag(event, DragSource.DefaultCopyNoDrop, new StringSelection(MAGIC_STRING), new DragSourceAdapter() {	
						public void dragEnter(DragSourceDragEvent event) {
							if(event.getTargetActions() == DnDConstants.ACTION_NONE) {
								event.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
							}
							else {
								event.getDragSourceContext().setCursor(DragSource.DefaultCopyDrop);
							}
						}
						
						public void dragExit(DragSourceEvent event) {
							event.getDragSourceContext().setCursor(DragSource.DefaultCopyNoDrop);
						}
					});
				}
			});
			addKeyListener(new KeyAdapter() {
				public void keyPressed(KeyEvent event) {
					int keyCode = event.getKeyCode();
					if(keyCode == KeyEvent.VK_DELETE) {
						event.consume();
						setTeam(null, true);
					}
					if(keyCode == KeyEvent.VK_BACK_SPACE) {
						event.consume();
						if(userEntry.length() > 0) {
							userEntry = userEntry.substring(0, userEntry.length() - 1);
							setText(getTeamText(TeamLabel.this.team) + formatUserEntry(userEntry));
						}
					}
					else if(keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
						event.consume();
						userEntry += -(KeyEvent.VK_0 - keyCode);
						setText(getTeamText(TeamLabel.this.team) + formatUserEntry(userEntry));
					}
					else if(keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
						event.consume();
						userEntry += -(KeyEvent.VK_NUMPAD0 - keyCode);
						setText(getTeamText(TeamLabel.this.team) + formatUserEntry(userEntry));
					}
					else if(keyCode == KeyEvent.VK_ESCAPE) {
						event.consume();
						userEntry = "";
						setText(getTeamText(TeamLabel.this.team));
					}
					else if(keyCode == KeyEvent.VK_ENTER) {
						event.consume();
						TeamLabel teamLabel = getSeed(userEntry);
						if(teamLabel == null) {
							userEntry = "";
							setText(getTeamText(TeamLabel.this.team));
						}
						else {
							TeamLabel.this.swapTeams(teamLabel);
						}
					}
				}
			});
			addFocusListener(new FocusAdapter() {
				public void focusGained(FocusEvent event) {
					setSelected(true);
				}
				
				public void focusLost(FocusEvent event) {
					setSelected(false);
				}
			});
			setTeam(team, false);
		}
		
		public void setTeam(Team team, boolean refreshTeams) {
			this.team = team;
			setText(getTeamText(team));
			setSelectionStart(0);
			setSelectionEnd(0);
			if(refreshTeams) {
				populateTeams();
				((JPanel) getParent()).scrollRectToVisible(getBounds());
				teamsList.requestFocusInWindow();
			}
		}
		
		public void swapTeams(TeamLabel teamLabel) {
			Team t1 = team;
			Team t2 = teamLabel.team;
			setTeam(t2, false);
			teamLabel.setTeam(t1, true);
		}
		
		public void setSelected(boolean selected) {
			this.selected = selected;
			if(selected) {
				setBackground(selectedBackground);
				setForeground(Color.WHITE);
			}
			else {
				setBackground(defaultBackground);
				setForeground(Color.BLACK);
			}
			userEntry = "";
			setText(getTeamText(team));
		}
		
		private String getTeamText(Team team) {
			if(team == null) {
				return TournamentViewManager.BYE;
			}
			return team.toString();
		}
	}
}
