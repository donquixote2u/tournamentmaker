package ui.component.editor;

import java.awt.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;

import ui.component.table.GenericTableModel;

import data.player.Player;
import data.team.Team;
import data.tournament.TournamentUtils;

public class TeamEditor extends GenericEditor {
	private static final long serialVersionUID = -3983470324818767555L;
	private Map<String, Player> players;
	private JPanel panel;
	private int playerCount;
	private String emptyValueMessage;
	private boolean displayUpdated;
	private Team team;

	public TeamEditor(JFrame owner, String emptyValueMessage) {
		super(owner, true);
		this.emptyValueMessage = emptyValueMessage;
		players = new HashMap<String, Player>();
		playerCount = 0;
	}
	
	public void setPlayers(List<Player> players) {
		this.players.clear();
		if(players != null) {
			for(Player player : players) {
				if(player != null) {
					this.players.put(player.getName(), player);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public Object getCellEditorValue() {
		List<Player> selectedPlayers = new ArrayList<Player>();
		for(int i = 0; i < panel.getComponentCount(); i += 2) {
			selectedPlayers.add(players.get(((JComboBox<String>) panel.getComponent(i)).getSelectedItem()));
		}
		return selectedPlayers;
	}
	
	@SuppressWarnings("unchecked")
	protected boolean validateEditorValue() {
		Team team = this.team.newInstance();
		for(int i = 0; i < panel.getComponentCount(); i += 2) {
			if(!team.setPlayer(i / 2, players.get(((JComboBox<String>) panel.getComponent(i)).getSelectedItem()))) {
				return false;
			}
		}
		return team.isValid();
	}

	@SuppressWarnings("unchecked")
	protected void setEditorValue(JTable table, Object value, int row, int column) {
		if(!(value instanceof List)) {
			buildPanel(null);
			return;
		}
		List<Player> list = (List<Player>) value;
		buildPanel(list);
		team = ((GenericTableModel<Team>) table.getModel()).getData(table.convertRowIndexToModel(row));
		List<String> playerNames = TournamentUtils.getValidPlayers(team, players.values());
		for(int i = 0; i < panel.getComponentCount(); i += 2) {
			JComboBox<String> box = (JComboBox<String>) panel.getComponent(i);
			box.removeAllItems();
			for(String name : playerNames) {
				box.addItem(name);
			}
			String selected = "";
			if(list.get(i / 2) != null && list.get(i / 2).getName() != null) {
				selected = list.get(i / 2).getName();
			}
			box.setSelectedItem(selected);
			box.setEnabled(!team.getInEvent());
		}
	}

	protected Component getDisplay() {
		panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        return panel;
	}

	protected boolean updateDisplay() {
		return displayUpdated;
	}
	
	private void buildPanel(List<?> list) {
		if((list == null || list.isEmpty()) && playerCount != 0) {
			playerCount = 0;
			displayUpdated = true;
			panel.removeAll();
			panel.add(new JLabel(emptyValueMessage));
		}
		else if(playerCount != list.size()) {
			playerCount = list.size();
			displayUpdated = true;
			panel.removeAll();
			for(int i = 0; i < playerCount; ++i) {
				JComboBox<String> box = new JComboBox<String>();
				box.setEditable(false);
				panel.add(box);
				panel.add(Box.createVerticalStrut(5));
			}
		}
	}
}
