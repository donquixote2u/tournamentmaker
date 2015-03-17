package ui.component.editor;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;

import data.match.Match;
import ui.component.panel.MatchPanel;
import ui.main.TournamentViewManager;

public class MatchEditor extends EditorButton {
	private static final long serialVersionUID = 7993873546111005614L;
	private JFrame owner;
	private JDialog editor;
	private MatchPanel matchPanel;
	private ArrayList<Match> matches;
	private JLabel label;
	private TournamentViewManager manager;
	
	public MatchEditor(final JFrame owner, TournamentViewManager manager) {
		this.owner = owner;
		this.manager = manager;
		label = new JLabel("");
		matches = new ArrayList<Match>();
		editor = new JDialog(owner);
		editor.setUndecorated(true);
		editor.setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
		editor.getContentPane().setLayout(new BorderLayout());
		matchPanel = new MatchPanel(editor);
		editor.getContentPane().add(matchPanel, BorderLayout.CENTER);
		JPanel buttons = new JPanel(new FlowLayout());
		JButton save = new JButton("Save");
		save.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				Match match = matchPanel.getMatch();
				// warn them if they are undoing a match without set winners
				if(!matchPanel.hasWinner() && JOptionPane.showConfirmDialog(editor,
						"This will undo all dependent matches. Do you want to continue?", "Unable to Detect Winner",
						JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.NO_OPTION) {
					return;
				}
				Set<Match> newMatches = MatchEditor.this.manager.getTournament().recalculateMatch(match);
				while(matches.remove(match));
				matches.addAll(newMatches);
				if(matches.isEmpty()) {
					fireEditingStopped();
				}
				else {
					matchPanel.setMatch(matches.get(0));
				}
			}
		});
		buttons.add(save);
		editor.getContentPane().add(buttons, BorderLayout.PAGE_END);
		editor.addWindowListener(new WindowAdapter() {
			public void windowActivated(WindowEvent event) {
				owner.getGlassPane().setVisible(true);
			}
			
			public void windowClosed(WindowEvent event) {
				owner.getGlassPane().setVisible(false);
			}
		});
	}
	
	protected final void fireEditingStopped() {
		editor.dispose();
		super.fireEditingStopped();
	}
	
	protected final void fireEditingCanceled() {
		// users aren't allowed to cancel
	}
	
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		Match match = (Match) value;
		if(match == null || match.getTeam1() == null || match.getTeam2() == null) {
			super.fireEditingCanceled();
			return label;
		}
		return super.getTableCellEditorComponent(table, value, isSelected, row, column);
	}
	
	public Object getCellEditorValue() {
		return null;
	}

	protected ActionListener getButtonAction() {
		return new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				editor.setLocationRelativeTo(owner);
				editor.setVisible(true);
			}
		};
	}

	protected String generateButtonText(Object value) {
		return "";
	}

	protected void setEditorValue(JTable table, Object value, int row, int column) {
		matches.clear();
		matches.add((Match) value);
		matchPanel.setMatch((Match) value);
	}
}
