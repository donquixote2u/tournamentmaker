package ui.component.panel;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import ui.component.textfield.GameTextField;
import ui.util.GenericUtils;
import data.match.Game;
import data.match.Match;
import data.team.Team;

public class MatchPanel extends JPanel {
	private static final long serialVersionUID = 6422105314610350039L;
	public static final String INVALID_VALUES = "Invalid values detected for this match. No winner could be calculated.";
	private static final String TEAMS = "Teams";
	private static final String TEAMS_PRINT = "(Circle Winner)";
	private static final String FORFEIT = "Forfeit";
	private static final String WITHDRAW = "Withdraw";
	private Match match;
	private JCheckBox forfeitTeam1, forfeitTeam2;
	private JCheckBox withdrawTeam1, withdrawTeam2;
	private JTextField team1, team2;
	private JPanel gamesPanel;
	private JDialog parent;
	private JButton button;
	private JLabel teamTitle, courtTitle;
	private Color defaultColor;
	private boolean hasWinner;
	
	public MatchPanel(JDialog parent) {
		super(new GridBagLayout());
		this.parent = parent;
		if(parent == null) {
			throw new RuntimeException("MatchComponent must have a parent.");
		}
		defaultColor = getBackground();
		courtTitle = new JLabel();
		courtTitle.setHorizontalAlignment(SwingConstants.CENTER);
		courtTitle.setFont(courtTitle.getFont().deriveFont(18.0f).deriveFont(Font.BOLD));
		add(courtTitle, new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
		teamTitle = new JLabel(TEAMS);
		add(teamTitle, new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 5, 0, 5), 0, 0));
		team1 = new JTextField(30);
		team1.setEditable(false);
		add(team1, new GridBagConstraints(0, 2, 1, 1, 0.1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
		team2 = new JTextField(30);
		team2.setEditable(false);
		add(team2, new GridBagConstraints(0, 3, 1, 1, 0.1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
		gamesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
		add(gamesPanel, new GridBagConstraints(1, 1, 1, 3, 0.9, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(5, 5, 0, 5), 0, 0));
		forfeitTeam1 = new JCheckBox(FORFEIT);
		forfeitTeam1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(!forfeitTeam1.isSelected()) {
					withdrawTeam1.setSelected(false);
				}
				withdrawTeam1.setEnabled(forfeitTeam1.isSelected());
				match.setTeam1Forfeit(forfeitTeam1.isSelected(), withdrawTeam1.isSelected());
				checkForWinner();
			}
		});
		add(forfeitTeam1, new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		forfeitTeam2 = new JCheckBox(FORFEIT);
		forfeitTeam2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(!forfeitTeam2.isSelected()) {
					withdrawTeam2.setSelected(false);
				}
				withdrawTeam2.setEnabled(forfeitTeam2.isSelected());
				match.setTeam2Forfeit(forfeitTeam2.isSelected(), withdrawTeam2.isSelected());
				checkForWinner();
			}
		});
		add(forfeitTeam2, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
		withdrawTeam1 = new JCheckBox(WITHDRAW);
		withdrawTeam1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				match.setTeam1Forfeit(forfeitTeam1.isSelected(), withdrawTeam1.isSelected());
			}
		});
		withdrawTeam1.setEnabled(false);
		add(withdrawTeam1, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		withdrawTeam2 = new JCheckBox(WITHDRAW);
		withdrawTeam2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				match.setTeam2Forfeit(forfeitTeam2.isSelected(), withdrawTeam2.isSelected());
			}
		});
		withdrawTeam2.setEnabled(false);
		add(withdrawTeam2, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 5, 0, 5), 0, 0));
	}
	
	public void setFinishButton(JButton button) {
		this.button = button;
	}
	
	public Match getMatch() {
		return match;
	}
	
	public void setMatch(Match match) {
		setMatch(match, false);
	}
	
	public boolean hasWinner() {
		return hasWinner;
	}
	
	public void setMatch(Match match, boolean formatForPrint) {
		this.match = match;
		team1.setText(match.getTeam1().getName());
		team1.setSelectionStart(0);
		team1.setSelectionEnd(0);
		team2.setText(match.getTeam2().getName());
		team2.setSelectionStart(0);
		team2.setSelectionEnd(0);
		gamesPanel.removeAll();
		List<Game> games = match.getGames();
		GameTextField pre = null;
		for(int i = 0; i < games.size(); ++i) {
			GameTextField cur = new GameTextField(i + 1, games.get(i), this);
			if(pre != null) {
				pre.setNextGame(cur);
			}
			gamesPanel.add(cur);
			pre = cur;
		}
		forfeitTeam1.setSelected(match.getTeam1Forfeit());
		if(match.getTeam1Forfeit()) {
			withdrawTeam1.setEnabled(true);
			withdrawTeam1.setSelected(match.getTeam1().isWithdrawn());
		}
		else {
			withdrawTeam1.setEnabled(false);
			withdrawTeam1.setSelected(false);
		}
		forfeitTeam2.setSelected(match.getTeam2Forfeit());
		if(match.getTeam2Forfeit()) {
			withdrawTeam2.setEnabled(true);
			withdrawTeam2.setSelected(match.getTeam2().isWithdrawn());
		}
		else {
			withdrawTeam2.setEnabled(false);
			withdrawTeam2.setSelected(false);
		}
		if(formatForPrint) {
			setBackground(Color.WHITE);
			courtTitle.setText(match.getCourt() != null ? "Court " + match.getCourt().getId() : null);
			team1.setEditable(true);
			team2.setEditable(true);
			gamesPanel.setBackground(Color.WHITE);
			forfeitTeam1.setBackground(Color.WHITE);
			forfeitTeam2.setBackground(Color.WHITE);
			String description = match.getEvent().getName() + " ";
			if(match.getEvent().showDisplayLevel()) {
				description += "- " + match.getLevel() + " ";
			}
			if(match.getMatchDescription() != null) {
				description += match.getMatchDescription() + " ";
			}
			teamTitle.setText(GenericUtils.html(description.trim() + GenericUtils.bold(" " + TEAMS_PRINT)));
		}
		else {
			setBackground(defaultColor);
			courtTitle.setText(null);
			team1.setEditable(false);
			team2.setEditable(false);
			gamesPanel.setBackground(defaultColor);
			forfeitTeam1.setBackground(defaultColor);
			forfeitTeam2.setBackground(defaultColor);
			teamTitle.setText(TEAMS);
		}
		courtTitle.setVisible(formatForPrint);
		withdrawTeam1.setVisible(!formatForPrint);
		withdrawTeam2.setVisible(!formatForPrint);
		parent.pack();
		// set focus to each component in the games panel so we can immediately check for a winner
		for(int i = 0; i < gamesPanel.getComponentCount(); ++i) {
			((GameTextField) gamesPanel.getComponent(i)).setFocus();
		}
		checkForWinner();
		if(gamesPanel.getComponentCount() > 0) {
			((GameTextField) gamesPanel.getComponent(0)).setFocus();
		}
	}
	
	public void checkForWinner() {
		hasWinner = false;
		if(match.getTeam1Forfeit() && match.getTeam2Forfeit()) {
			team1.setFont(team1.getFont().deriveFont(Font.PLAIN));
			team2.setFont(team2.getFont().deriveFont(Font.PLAIN));
			hasWinner = true;
			return;
		}
		if(match.getTeam1Forfeit()) {
			team1.setFont(team1.getFont().deriveFont(Font.PLAIN));
			team2.setFont(team2.getFont().deriveFont(Font.BOLD));
			hasWinner = true;
			return;
		}
		if(match.getTeam2Forfeit()) {
			team1.setFont(team1.getFont().deriveFont(Font.BOLD));
			team2.setFont(team2.getFont().deriveFont(Font.PLAIN));
			hasWinner = true;
			return;
		}
		int t1Count = 0;
		int t2Count = 0;
		int indexFinished = -1;
		List<Game> games = match.getGames();
		int gamesToWin = (int) Math.ceil(games.size() / 2.0);
		for(int i = 0; i < games.size(); ++i) {
			Game game = games.get(i);
			Team winner = game.getWinner();
			if(winner == match.getTeam1()) {
				++t1Count;
			}
			else if(winner == match.getTeam2()) {
				++t2Count;
			}
			else if(winner == null) {
				if(game.getTeam1Score() >= 0 || game.getTeam2Score() >= 0) {
					indexFinished = -1;
				}
				break;
			}
			if(indexFinished == -1 && (t1Count == gamesToWin || t2Count == gamesToWin)) {
				indexFinished = i;
			}
		}
		if(indexFinished == -1) {
			team1.setFont(team1.getFont().deriveFont(Font.PLAIN));
			team2.setFont(team2.getFont().deriveFont(Font.PLAIN));
			return;
		}
		if(t1Count + t2Count != indexFinished + 1) {
			team1.setFont(team1.getFont().deriveFont(Font.PLAIN));
			team2.setFont(team2.getFont().deriveFont(Font.PLAIN));
			return;
		}
		team1.setFont(team1.getFont().deriveFont(gamesToWin == t1Count ? Font.BOLD : Font.PLAIN));
		team2.setFont(team2.getFont().deriveFont(gamesToWin == t2Count ? Font.BOLD : Font.PLAIN));
		if(gamesToWin == t1Count || gamesToWin == t2Count) {
			hasWinner = true;
		}
		if(button != null && hasWinner) {
			button.requestFocus();
		}
	}
}
