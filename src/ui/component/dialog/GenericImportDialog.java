package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import data.player.Player;
import data.team.Team;
import ui.main.TournamentUI;
import ui.main.TournamentViewManager;
import ui.util.GenericUtils;

public class GenericImportDialog extends ImportDialog {
	private static final long serialVersionUID = 5103472598222170840L;
	private File dataFile;
	private JComboBox<String> teamType;

	public GenericImportDialog(TournamentUI owner, TournamentViewManager manager, File file) {
		super(owner, manager, "Import from Generic Data File", file);
	}
	
	protected Component getDisplay() {
		JPanel display = new JPanel();
		display.setLayout(new BorderLayout());
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
		display.add(root, BorderLayout.CENTER);
		JPanel description = new JPanel(new GridBagLayout());
		description.add(new JLabel("This tool will scan the file you selected for player names. It will try to create teams from these players."), GenericUtils.createGridBagConstraint(0, 0, 1.0));
		root.add(description);
		JPanel fields = new JPanel(new GridBagLayout());
		fields.add(new JLabel("Type of Team"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		teamType = new JComboBox<String>();
		teamType.addActionListener(new ActionListener() {
			private String previouslySelectedType;
			public void actionPerformed(ActionEvent event) {
				String type = (String) teamType.getSelectedItem();
				if(TournamentUI.NEW_DISPLAY_TEXT.equals(type)) {
					String team = ((TournamentUI) getOwner()).openNewTeamTypeDialog();
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
		fields.add(teamType, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		List<String> types = getTournamentViewManager().getTournament().getTeamTypes();
		Collections.sort(types);
		types.add(TournamentUI.NEW_DISPLAY_TEXT);
		for(String type : types) {
			teamType.addItem(type);
		}
		root.add(fields);
		return display;
	}
	
	protected boolean isValid(File file) {
		if(!((TournamentUI) getOwner()).checkForExistingTeamTypes()) {
			return false;
		}
		dataFile = file;
		return true;
	}
	
	protected String importData() {
		String messages = "";
		int imported = 0, duplicated = 0;
		LineNumberReader reader = null;
		try {
			// grab the players and sort the from longest name to shortest
			Team newTeamType = getTournamentViewManager().getTournament().getTeamByType((String) teamType.getSelectedItem()).newInstance();
			ArrayList<Player> players = new ArrayList<Player>();
			for(Player player : getTournamentViewManager().getTournament().getPlayers()) {
				if(newTeamType.isValidPlayer(player)) {
					players.add(player);
				}
			}
			Collections.sort(players, new Comparator<Player>() {
				public int compare(Player p1, Player p2) {
					return p2.getName().length() - p1.getName().length();
				}
			});
			// find the players and create teams
			reader = new LineNumberReader(new InputStreamReader(new FileInputStream(dataFile)));
			Team newTeam = newTeamType.newInstance();
			int index = 0;
			String currentLine;
			while((currentLine = reader.readLine()) != null) {
				boolean foundPlayer;
				do {
					foundPlayer = false;
					for(Player player : players) {
						String newValue = GenericUtils.extractValueFromString(player.getName(), currentLine);
						if(newValue != null && !currentLine.equals(newValue)) {
							currentLine = newValue;
							foundPlayer = true;
							newTeam.setPlayer(index++, player);
							if(index == newTeam.getNumberOfPlayers()) {
								break;
							}
						}
					}
					if(index == newTeam.getNumberOfPlayers()) {
						if(newTeam.isValid()) {
							boolean isNew = true;
							for(Team team : getTournamentViewManager().getTournament().getTeams(newTeamType)) {
								if((new HashSet<Player>(team.getPlayers())).equals(new HashSet<Player>(newTeam.getPlayers()))) {
									++duplicated;
									isNew = false;
									break;
								}
							}
							if(isNew) {
								++imported;
								getTournamentViewManager().getTournament().addTeam(newTeam);
							}
						}
						index = 0;
						newTeam = newTeamType.newInstance();
					}
				}
				while(foundPlayer);
			}
		}
		catch(Exception e) {
			return "Error encountered: \"" + e.getMessage() + "\"";
		}
		finally {
			if(reader != null) {
				try {
					reader.close();
				}
				catch (Exception e) {}
			}
		}
		if(imported > 0) {
			messages += "Imported " + imported + " teams.\n";
		}
		if(duplicated > 0) {
			messages += "Ignored " + duplicated + " duplicate teams.\n";
		}
		if(messages.isEmpty()) {
			messages = "No valid teams were detected.\n";
		}
		return messages;
	}
	
	protected String validateImportData() {
		return null;
	}
}
