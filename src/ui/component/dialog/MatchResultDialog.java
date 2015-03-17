package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import data.match.Match;

public class MatchResultDialog extends JDialog {
	private static final long serialVersionUID = 6107543507015426423L;
	private static final String FORFEIT = "Forfeit";
	private static final String WITHDRAW = "Withdraw";
	private Match match;
	private JCheckBox forfeitTeam1, forfeitTeam2;
	private JCheckBox withdrawTeam1, withdrawTeam2;
	private JTextField team1, team2;
	
	public MatchResultDialog(JFrame owner) {
		super(owner, true);
		setTitle("Match Result");
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		JPanel result = new JPanel(new GridBagLayout());
		team1 = new JTextField(30);
		team1.setEditable(false);
		result.add(team1, new GridBagConstraints(0, 0, 1, 1, 0.1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
		team2 = new JTextField(30);
		team2.setEditable(false);
		result.add(team2, new GridBagConstraints(0, 1, 1, 1, 0.1, 0.5, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
		forfeitTeam1 = new JCheckBox(FORFEIT);
		forfeitTeam1.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				withdrawTeam1.setEnabled(forfeitTeam1.isSelected());
				if(!forfeitTeam1.isSelected()) {
					withdrawTeam1.setSelected(false);
				}
			}
		});
		result.add(forfeitTeam1, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		forfeitTeam2 = new JCheckBox(FORFEIT);
		forfeitTeam2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				withdrawTeam2.setEnabled(forfeitTeam2.isSelected());
				if(!forfeitTeam2.isSelected()) {
					withdrawTeam2.setSelected(false);
				}
			}
		});
		result.add(forfeitTeam2, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		withdrawTeam1 = new JCheckBox(WITHDRAW);
		result.add(withdrawTeam1, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		withdrawTeam2 = new JCheckBox(WITHDRAW);
		result.add(withdrawTeam2, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 5, 0, 5), 0, 0));
		getContentPane().add(result, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				match.setT1OnStart(forfeitTeam1.isSelected(), withdrawTeam1.isSelected());
				match.setT2OnStart(forfeitTeam2.isSelected(), withdrawTeam2.isSelected());
				dispose();
			}
		});
		buttons.add(save);
		JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		buttons.add(cancel);
		getContentPane().add(buttons, BorderLayout.PAGE_END);
		pack();
		setLocationRelativeTo(null);
	}
	
	public void setMatch(Match match) {
		this.match = match;
		team1.setText(match.getTeam1() == null ? Match.TBD : match.getTeam1().getName());
		team2.setText(match.getTeam2() == null ? Match.TBD : match.getTeam2().getName());
		forfeitTeam1.setSelected(true);
		forfeitTeam1.doClick();
		if(match.getT1ForfeitOnStart()) {
			forfeitTeam1.doClick();
		}
		forfeitTeam2.setSelected(true);
		forfeitTeam2.doClick();
		if(match.getT2ForfeitOnStart()) {
			forfeitTeam2.doClick();
		}
		withdrawTeam1.setSelected(match.getT1WithdrawalOnStart());
		withdrawTeam2.setSelected(match.getT2WithdrawalOnStart());
	}
}
