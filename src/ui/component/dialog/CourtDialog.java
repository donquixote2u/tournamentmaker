package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Set;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import ui.component.panel.MatchPanel;
import ui.main.TournamentViewManager;
import ui.util.GenericUtils;
import data.match.Match;
import data.tournament.Court;

public class CourtDialog extends JDialog {
	private static final long serialVersionUID = 6276355085134417278L;
	private JComboBox<Match> filter;
	private MatchPanel matchPanel;
	private Court court;
	
	@SuppressWarnings("serial")
	public CourtDialog(JFrame owner, final TournamentViewManager manager) {
		super(owner, true);
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		filter = new JComboBox<Match>();
		filter.setRenderer(new DefaultListCellRenderer() {
			@SuppressWarnings("rawtypes")
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				Match match = (Match) value;
				String description = match.getEvent().getName() + " ";
				if(match.getEvent().showDisplayLevel()) {
					description += "- " + match.getLevel() + " ";
				}
				if(match.getIndex() > 0) {
					description += "#" + match.getIndex() + " ";
				}
				if(!description.isEmpty()) {
					description = GenericUtils.italics("[" + description.trim() + "] ");
				}
				if(match.getMatchDescription() != null) {
					description += GenericUtils.bold(match.getMatchDescription()) + " ";
				}
				description = (description + match.toString()).trim();
				setText(GenericUtils.html(description));
				return this;
			}
		});
		filter.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Match match = (Match) filter.getSelectedItem();
				if(match == null) {
					return;
				}
				matchPanel.setMatch(match);
			}
		});
		getContentPane().add(filter, BorderLayout.PAGE_START);
		matchPanel = new MatchPanel(this);
		getContentPane().add(matchPanel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton finish = new JButton("Finish Match");
		matchPanel.setFinishButton(finish);
		finish.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(finishMatch(manager)) {
					removeMatch();
				}
			}
		});
		buttons.add(finish);
		JButton undo = new JButton("Undo Match");
		undo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(undoMatch(manager)) {
					removeMatch();
				}
			}
		});
		buttons.add(undo);
		JButton print = new JButton("Print Match");
		print.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				manager.printMatch((Match) filter.getSelectedItem());
				processWindowEvent(new WindowEvent(CourtDialog.this, WindowEvent.WINDOW_CLOSING));
			}
		});
		buttons.add(print);
		getContentPane().add(buttons, BorderLayout.PAGE_END);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent event) {
				court = null;
				dispose();
				manager.switchToTab(TournamentViewManager.TOURNAMENT_TAB, true);
			}
		});
	}
	
	public void show(Court court) {
		this.court = court;
		String title = "Court " + court.getId() + " Results";
		filter.removeAllItems();
		for(Match match : court.getPreviousMatches()) {
			filter.addItem(match);
		}
		if(court.getCurrentMatch() != null) {
			filter.addItem(court.getCurrentMatch());
		}
		if(filter.getItemCount() == 0) {
			JOptionPane.showMessageDialog(getOwner(), "There are no matches to display.", title, JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		setLocationRelativeTo(getOwner());
		setTitle(title);
		setVisible(true);
	}
	
	private boolean finishMatch(TournamentViewManager manager) {
		Match match = (Match) filter.getSelectedItem();
		Set<Match> newMatches = match.finish();
		if(newMatches == null) {
			JOptionPane.showMessageDialog(this, MatchPanel.INVALID_VALUES, "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if(match.equals(court.getCurrentMatch())) {
			court.setMatch(null);
		}
		court.getPreviousMatches().remove(match);
		manager.getTournament().addCompletedMatch(match);
		manager.getTournament().addMatches(newMatches);
		manager.refreshCurrentTab();
		return true;
	}
	
	private boolean undoMatch(TournamentViewManager manager) {
		Match match = (Match) filter.getSelectedItem();
		court.undoMatch(match);
		manager.getTournament().addMatches(Arrays.asList(match));
		manager.refreshCurrentTab();
		return true;
	}
	
	private void removeMatch() {
		Match match = (Match) filter.getSelectedItem();
		filter.removeItem(match);
		if(filter.getItemCount() == 0) {
			processWindowEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
		}
	}
}
