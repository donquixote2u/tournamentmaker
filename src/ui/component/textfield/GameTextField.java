package ui.component.textfield;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import ui.component.panel.MatchPanel;
import data.match.Game;

public class GameTextField extends JPanel {
	private static final long serialVersionUID = 8211348610366727997L;
	private static final InputVerifier VALIDATOR = new InputVerifier() {
		public boolean verify(JComponent comp) {
			try {
				String text = ((JTextField) comp).getText();
				if(text.isEmpty() || Integer.parseInt(text) >= 0) {
					comp.setBorder(DEFAULT);
					return true;
				}
			}
			catch(Exception e) {}
			comp.setBorder(ERROR);
			return false;
		}
	};
	private static final Border DEFAULT = (new JTextField()).getBorder();
	private static final Border ERROR = new LineBorder(Color.RED, 1);
	private Game game;
	private JTextField team1Score, team2Score;
	private MatchPanel matchPanel;
	private GameTextField nextGame;
	
	public GameTextField(int gameNumber, Game game, MatchPanel matchPanel) {
		super();
		if(game == null) {
			throw new IllegalArgumentException("game can not be null");
		}
		this.game = game;
		this.matchPanel = matchPanel;
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
		JLabel id = new JLabel(String.valueOf(gameNumber));
		id.setHorizontalAlignment(SwingConstants.CENTER);
		add(id);
		add(Box.createVerticalStrut(7));
		team1Score = new JTextField(5);
		team1Score.setInputVerifier(VALIDATOR);
		team1Score.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				try {
					String text = team1Score.getText();
					if(text.isEmpty()) {
						GameTextField.this.game.setTeam1Score(-1);
					}
					else {
						GameTextField.this.game.setTeam1Score(Integer.parseInt(text));
					}
					checkScore();
				}
				catch(Exception e) {}
			}
		});
		team1Score.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.VK_ENTER) {
					event.consume();
					if(nextGame != null && !team1Score.getText().isEmpty()) {
						nextGame.setFocus();
					}
					else {
						team2Score.requestFocus();
					}
				}
			}
		});
		team2Score = new JTextField(5);
		team2Score.setInputVerifier(VALIDATOR);
		team2Score.addFocusListener(new FocusAdapter() {
			public void focusLost(FocusEvent event) {
				try {
					String text = team2Score.getText();
					if(text.isEmpty()) {
						GameTextField.this.game.setTeam2Score(-1);
					}
					else {
						GameTextField.this.game.setTeam2Score(Integer.parseInt(text));
					}
					checkScore();
				}
				catch(Exception e) {}
			}
		});
		team2Score.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent event) {
				if(event.getKeyCode() == KeyEvent.VK_ENTER) {
					event.consume();
					if(nextGame != null && !team2Score.getText().isEmpty()) {
						nextGame.setFocus();
					}
					else {
						team1Score.requestFocus();
					}
				}
			}
		});
		add(team1Score);
		add(Box.createVerticalStrut(10));
		add(team2Score);
		checkScore();
	}
	
	public void setFocus() {
		team1Score.requestFocus();
	}
	
	public void setNextGame(GameTextField nextGame) {
		this.nextGame = nextGame;
	}
	
	private void checkScore() {
		if(game.getTeam1Score() >= 0) {
			team1Score.setText(String.valueOf(game.getTeam1Score()));
		}
		else {
			team1Score.setText(null);
		}
		if(game.getTeam2Score() >= 0) {
			team2Score.setText(String.valueOf(game.getTeam2Score()));
		}
		else {
			team2Score.setText(null);
		}
		if(game.getTeam1() == game.getWinner()) {
			team1Score.setFont(team1Score.getFont().deriveFont(Font.BOLD));
		}
		else {
			team1Score.setFont(team1Score.getFont().deriveFont(Font.PLAIN));
		}
		if(game.getTeam2() == game.getWinner()) {
			team2Score.setFont(team2Score.getFont().deriveFont(Font.BOLD));
		}
		else {
			team2Score.setFont(team2Score.getFont().deriveFont(Font.PLAIN));
		}
		matchPanel.checkForWinner();
	}
}
