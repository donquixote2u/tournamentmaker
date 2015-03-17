package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;

import ui.component.button.EventButton;
import ui.main.TournamentViewManager;
import ui.util.Pair;
import data.event.Event;
import data.event.result.EventResult;
import data.match.Match;
import data.team.Team;
import data.tournament.EventInfo;

public class EventResultDialog extends JDialog {
	private static final long serialVersionUID = -5971630348975035715L;
	private static final String PAUSE = "Pause Event";
	private static final String RESUME = "Resume Event";
	private JTextArea textArea;
	private EventButton button;
	private JButton pause;
	private JPanel levels, pausePanel;
	private JComboBox<Pair<String, Integer>> matchLevel;
	
	@SuppressWarnings("serial")
	public EventResultDialog(JFrame owner, final TournamentViewManager manager) {
		super(owner, "Event Results", true);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		textArea = new JTextArea();
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setPreferredSize(new Dimension(700, 500));
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
		pausePanel = new JPanel();
		pausePanel.setLayout(new BoxLayout(pausePanel, BoxLayout.Y_AXIS));
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		levels = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panel.add(levels);;
		matchLevel = new JComboBox<Pair<String, Integer>>();
		matchLevel.setRenderer(new DefaultListCellRenderer() {
			@SuppressWarnings({ "rawtypes", "unchecked" })
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) { 
				return super.getListCellRendererComponent(list, ((Pair<String, Integer>) value).getKey(), index, isSelected, cellHasFocus);
			}
		});
		panel.add(matchLevel);
		pausePanel.add(panel);
		pausePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		pause = new JButton(PAUSE);
		pause.addActionListener(new ActionListener() {
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent event) {
				if(EventResultDialog.this.button != null && EventResultDialog.this.button.getEvent() != null) {
					if(PAUSE.equals(pause.getText())) {
						HashSet<String> selectedLevels = new HashSet<String>();
						for(Component comp : levels.getComponents()) {
							JCheckBox checkBox = (JCheckBox) comp;
							if(checkBox.isSelected()) {
								selectedLevels.add(checkBox.getText());
							}
						}
						EventResultDialog.this.button.getEvent().setPaused(selectedLevels, ((Pair<String, Integer>) matchLevel.getSelectedItem()).getValue());
					}
					else {
						EventResultDialog.this.button.getEvent().setPaused(null, -1);
					}
					EventResultDialog.this.button.updateStatus();
					// adding an empty list of matches to force the list of matches to refresh
					manager.getTournament().addMatches(new ArrayList<Match>());
					manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
					EventResultDialog.this.dispose();
				}
			}
		});
		panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		panel.add(pause);
		pausePanel.add(panel);
		pausePanel.add(Box.createRigidArea(new Dimension(0, 5)));
		buttonPanel.add(pausePanel);
		panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		JButton print = new JButton("Print Event");
		print.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				manager.printComponent(textArea);
				EventResultDialog.this.dispose();
			}
		});
		panel.add(print);
		JButton undo = new JButton("Undo Event");
		undo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(JOptionPane.showConfirmDialog(EventResultDialog.this, "Are you sure you want to undo this event?", "Undo Event", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
					manager.getTournament().undoEvent(button.getEvent());
					manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
					EventResultDialog.this.dispose();
				}
			}
		});
		panel.add(undo);
		buttonPanel.add(panel);
		getContentPane().add(buttonPanel, BorderLayout.PAGE_END);
	}
	
	public void show(EventButton button) {
		this.button = button;
		if(button == null || button.getEvent() == null) {
			return;
		}
		Event event = button.getEvent();
		if(event.isStarted() && !event.isComplete()) {
			pausePanel.setVisible(true);
			pause.setText(event.getPausedLevels() == null ? PAUSE : RESUME);
			HashSet<String> selectedLevels = new HashSet<String>(event.getPausedLevels() == null ? event.getDisplayLevels() : event.getPausedLevels());
			// building the level panel
			levels.removeAll();
			for(String level : event.getDisplayLevels()) {
				JCheckBox checkbox = new JCheckBox(level);
				checkbox.setSelected(selectedLevels.contains(level));
				checkbox.setEnabled(event.getPausedLevels() == null);
				levels.add(checkbox);
			}
			// building the match level drop down
			matchLevel.setEnabled(event.getPausedLevels() == null);
			matchLevel.removeAllItems();
			TreeSet<Pair<String, Integer>> set = new TreeSet<Pair<String, Integer>>(new Comparator<Pair<String, Integer>> () {
				public int compare(Pair<String, Integer> p1, Pair<String, Integer> p2) {
					return p1.getValue().compareTo(p2.getValue());
				}
			});
			for(String level : event.getDisplayLevels()) {
				Match match = event.getMatches(level).get(0);
				while(match != null) {
					String description = "Round of ";
					if(match.getMatchLevel() > 0) {
						description += Integer.toString((int) Math.pow(2, match.getMatchLevel()));
					}
					else {
						description += Integer.toString(event.getMatches(level).size());
					}
					if(match.getMatchDescription() != null) {
						description += " (" + match.getMatchDescription() + ")";
					}
					set.add(new Pair<String, Integer>(description, match.getMatchLevel()));
					match = match.getWinnerMatch();
				}
			}
			for(Pair<String, Integer> pair : set) {
				matchLevel.addItem(pair);
				if(pair.getValue().intValue() == event.getPausedMatchLevel()) {
					matchLevel.setSelectedItem(pair);
				}
			}
		}
		else {
			pausePanel.setVisible(false);
		}
		Map<Team, TeamData> winners = new HashMap<Team, TeamData>();
		List<String> levels = event.getDisplayLevels();
		for(int i = 0; i < levels.size(); ++i) {
			String level = levels.get(i);
			if(!event.isComplete(level)) {
				continue;
			}
			EventResult result = event.getWinners(level);
			for(int j = 0; j < result.getNumberOfWinners(); ++j) {
				List<Team> teams = result.getWinners(j);
				for(Team team : teams) {
					TeamData data = winners.get(team);
					if(data == null) {
						winners.put(team, new TeamData(i, j));
					}
					else if(data.rank > j) {
						data.level = i;
						data.rank = j;
					}
				}
				j += teams.size() - 1;
			}
		}
		HashSet<Team> placedTeams = new HashSet<Team>();
		String text = event.getName() + " - " + event.getDisplayLevels() + "\n\n";
		for(int i = 0; i < levels.size(); ++i) {
			String level = levels.get(i);
			if(event.isComplete(level)) {
				EventResult result = event.getWinners(level);
				text += result.getDescription() + "\n";
				int rank = 1;
				for(int j = 0; j < result.getNumberOfWinners(); ++j) {
					ArrayList<Team> teams = new ArrayList<Team>(result.getWinners(j));
					j += teams.size() - 1;
					Iterator<Team> iter = teams.iterator();
					while(iter.hasNext()) {
						Team team = iter.next();
						if(winners.get(team).level != i || !placedTeams.add(team)) {
							iter.remove();
						}
					}
					if(teams.isEmpty()) {
						continue;
					}
					if(teams.size() == 1) {
						text += rank + ". ";
					}
					else {
						text += rank + "-" + (rank + teams.size() - 1) + ". ";
					}
					rank += teams.size();
					for(Team team : teams) {
						text += team.getName() + ", ";
					}
					text = text.substring(0, text.length() - 2) + "\n";
				}
			}
			else {
				text += level + "\n";
				EventInfo info = new EventInfo(event.getMatches(level));
				text += info.getNumberOfCompleteMatches() + " / " + info.getNumberOfMatches() + " matches finished (" + info.getPercentComplete() + "%)\n";
			}
			text += "\n";
		}
		textArea.setText(text);
		textArea.setSelectionStart(0);
		textArea.setSelectionEnd(0);
		textArea.setLineWrap(true);
		pack();
		setLocationRelativeTo(getOwner());
		setVisible(true);
	}
	
	private class TeamData {
		int level, rank;
		
		TeamData(int level, int rank) {
			this.level = level;
			this.rank = rank;
		}
	}
}
