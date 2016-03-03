package ui.component.dialog;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import data.event.Event;
import data.player.Player;
import data.team.Team;
import data.tournament.Tournament;
import ui.main.TournamentUI;
import ui.main.TournamentViewManager;
import ui.util.GenericUtils;

public class TournamentImportDialog extends ImportDialog {
	private static final long serialVersionUID = 5742268526813092554L;
	private Tournament tournament;
	private JCheckBox importPlayers;
	private JPanel teamFields;
	private JPanel eventFields;
	
	public TournamentImportDialog(JFrame owner, TournamentViewManager manager, File file) {
		super(owner, manager, "Import from Tournament Maker Data File", file);
	}
	
	protected Component getDisplay() {
		JPanel display = new JPanel();
		display.setLayout(new BoxLayout(display, BoxLayout.PAGE_AXIS));
		// players
		JPanel fields = new JPanel(new GridBagLayout());
		fields.add(new JLabel("Players"), GenericUtils.createGridBagConstraint(0, 0, 1.0));
		fields.add(new JSeparator(JSeparator.HORIZONTAL), GenericUtils.createGridBagConstraint(0, 1, 1.0));
		importPlayers = new JCheckBox("Import All Players");
		if(tournament.getPlayers().isEmpty()) {
			fields.add(new JLabel("No players were found in the data file."), GenericUtils.createGridBagConstraint(0, 2, 1.0));
		}
		else {
			fields.add(importPlayers, GenericUtils.createGridBagConstraint(0, 2, 1.0));
		}
		display.add(fields);
		display.add(Box.createVerticalStrut(10));
		// teams
		fields = new JPanel(new GridBagLayout());
		fields.add(new JLabel("Teams"), GenericUtils.createGridBagConstraint(0, 0, 1.0));
		fields.add(new JSeparator(JSeparator.HORIZONTAL), GenericUtils.createGridBagConstraint(0, 1, 1.0));
		display.add(fields);
		teamFields = new JPanel(new GridBagLayout());
		List<String> teamTypes = tournament.getTeamTypes();
		if(teamTypes.isEmpty()) {
			fields.add(new JLabel("No teams were found in the data file."), GenericUtils.createGridBagConstraint(0, 2, 1.0));
		}
		else {
			for(int i = 0; i < teamTypes.size(); ++i) {
				teamFields.add(new JCheckBox(teamTypes.get(i)), GenericUtils.createGridBagConstraint(i % 3, i / 3, 0.33));
			}
		}
		display.add(teamFields);
		display.add(Box.createVerticalStrut(10));
		// events
		fields = new JPanel(new GridBagLayout());
		fields.add(new JLabel("Events"), GenericUtils.createGridBagConstraint(0, 0, 1.0));
		fields.add(new JSeparator(JSeparator.HORIZONTAL), GenericUtils.createGridBagConstraint(0, 1, 1.0));
		display.add(fields);
		eventFields = new JPanel(new GridBagLayout());
		List<Event> events = tournament.getEvents();
		if(events.isEmpty()) {
			fields.add(new JLabel("No events were found in the data file."), GenericUtils.createGridBagConstraint(0, 2, 1.0));
		}
		else {
			for(int i = 0; i < events.size(); ++i) {
				eventFields.add(new JCheckBox(events.get(i).getName()), GenericUtils.createGridBagConstraint(i % 3, i / 3, 0.33));
			}
		}
		display.add(eventFields);
		display.add(Box.createVerticalStrut(10));
		return display;
	}
	
	protected boolean isValid(File file) {
		ObjectInputStream reader = null;
		try {
			reader = new ObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
			tournament = (Tournament) reader.readObject();
			if(tournament == null) {
				return false;
			}
		}
		catch(Exception e) {
			return false;
		}
		finally {
			if(reader != null) {
				try {
					reader.close();
				}
				catch(Exception e) {}
			}
		}
		return true;
	}
	
	public String checkImportCompatibility() {
		if(tournament != null && tournament.getVersion() > Tournament.VERSION) {
			return "The data file was created by a newer version of " + TournamentUI.APP_NAME + ".\nPlease open your current data file with the newer version and import again.";
		}
		return super.checkImportCompatibility();
	}
	
	protected String importData() {
		int imported = 0, updated = 0;
		HashMap<String, Player> males = new HashMap<String, Player>();
		HashMap<String, Player> females = new HashMap<String, Player>();
		for(Player player : getTournamentViewManager().getTournament().getPlayers()) {
			if(player.isMale()) {
				if(!males.containsKey(player.getName())) {
					males.put(player.getName(), player);
				}
			}
			else {
				if(!females.containsKey(player.getName())) {
					females.put(player.getName(), player);
				}
			}
		}
		HashSet<String> eventNames = new HashSet<String>();
		for(Event event : getTournamentViewManager().getTournament().getEvents()) {
			eventNames.add(event.getName());
		}
		for(Event event : tournament.getEvents()) {
			for(Component comp : eventFields.getComponents()) {
				if(!(comp instanceof JCheckBox)) {
					continue;
				}
				JCheckBox box = (JCheckBox) comp;
				if(box.isSelected() && box.getText().equals(event.getName())) {
					eventNames.add(event.getName());
					break;
				}
			}
		}
		String messages = "";
		// import players
		if(importPlayers.isSelected()) {
			for(Player player : tournament.getPlayers()) {
				Player current;
				if(player.isMale()) {
					current = males.get(player.getName());
				}
				else {
					current = females.get(player.getName());
				}
				if(current == null) {
					current = new Player(player.getName(), player.isMale());
					getTournamentViewManager().getTournament().addPlayer(current);
					if(current.isMale()) {
						males.put(current.getName(), current);
					}
					else {
						females.put(current.getName(), current);
					}
					++imported;
				}
				else {
					++updated;
				}
				current.setAddress(player.getAddress());
				current.setEmail(player.getEmail());
				current.setPhoneNumber(player.getPhoneNumber());
				current.setMembershipNumber(player.getMembershipNumber());
				current.setClub(player.getClub());
				if(player.getLevel() == null || player.getLevel().isEmpty() || getTournamentViewManager().getTournament().getLevels().contains(player.getLevel())) {
					current.setLevel(player.getLevel());
				}
				else {
					messages += "\"" + player.getLevel() + "\" is not a valid level (player \"" + player.getName() + "\").\n";
				}
				current.setDateOfBirth(player.getDateOfBirth());
				current.setAmountPaid(player.getAmountPaid());
				current.setAmountDue(player.getAmountDue());
				String invalidEvents = "";
				HashSet<String> events = new HashSet<String>();
				for(String event : player.getEvents()) {
					if(eventNames.contains(event)) {
						events.add(event);
					}
					else {
						invalidEvents += invalidEvents.isEmpty() ? "" : ", " + event;
					}
				}
				if(!invalidEvents.isEmpty()) {
					messages += "Invalid event names (\"" + invalidEvents + "\") were detected (player \"" + player.getName() + "\").\n";
				}
				if(player.getEvents().isEmpty() || !events.isEmpty()) {
					current.setEvents(events);
				}
			}
			if(imported > 0) {
				messages += "Imported " + imported + " players.\n";
			}
			if(updated > 0) {
				messages += "Updated " + updated + " players.\n";
			}
		}
		// import teams
		ArrayList<Team> teamTypes = new ArrayList<Team>();
		for(Component comp : teamFields.getComponents()) {
			if(!(comp instanceof JCheckBox)) {
				continue;
			}
			JCheckBox box = (JCheckBox) comp;
			if(!box.isSelected()) {
				continue;
			}
			Team newTeam = tournament.getTeamByType(box.getText());
			Team oldTeam = getTournamentViewManager().getTournament().getTeamByType(box.getText());
			if(oldTeam != null && !oldTeam.getTeamTypeDescription().equals(newTeam.getTeamTypeDescription())) {
				messages += "Unable to import non-unique team type (" + newTeam.getTeamTypeDescription() + ").\n";
				continue;
			}
			teamTypes.add(newTeam);
		}
		imported = 0;
		for(Team team : teamTypes) {
			if(getTournamentViewManager().getTournament().addTeamType(team.newInstance())) {
				++imported;
			}
		}
		if(imported > 0) {
			messages += "Imported " + imported + " new team types.\n";
		}
		imported = 0;
		updated = 0;
		int failed = 0;
		for(Team type : teamTypes) {
			for(Team team : tournament.getTeams(type)) {
				Team newTeam = team.newInstance();
				for(int i = 0; i < newTeam.getNumberOfPlayers(); ++i) {
					Player original = team.getPlayer(i);
					Player player;
					if(original.isMale()) {
						player = males.get(original.getName());
					}
					else {
						player = females.get(original.getName());
					}
					newTeam.setPlayer(i, player);
				}
				if(!newTeam.isValid()) {
					++failed;
					continue;
				}
				boolean isNew = true;
				for(Team cur : getTournamentViewManager().getTournament().getTeams(type)) {
					if((new HashSet<Player>(newTeam.getPlayers())).equals(new HashSet<Player>(cur.getPlayers()))) {
						isNew = false;
						newTeam = cur;
						break;
					}
				}
				newTeam.setSeed(team.getSeed());
				newTeam.setName(team.getName());
				if(isNew) {
					++imported;
					getTournamentViewManager().getTournament().addTeam(newTeam);
				}
				else {
					++updated;
				}
			}
		}
		if(imported > 0) {
			messages += "Imported " + imported + " teams.\n";
		}
		if(updated > 0) {
			messages += "Updated " + updated + " teams.\n";
		}
		if(failed > 0) {
			messages += "Failed to import " + failed + " teams.\n";
		}
		// import events
		imported = 0;
		updated = 0;
		failed = 0;
		ArrayList<Event> events = new ArrayList<Event>();
		for(Event event : tournament.getEvents()) {
			for(Component comp : eventFields.getComponents()) {
				if(!(comp instanceof JCheckBox)) {
					continue;
				}
				JCheckBox box = (JCheckBox) comp;
				if(box.isSelected() && box.getText().equals(event.getName())) {
					if(getTournamentViewManager().getTournament().getTeamByType(event.getTeamFilter().getTeamType()) == null) {
						messages += "Unable to find team type (" + event.getTeamFilter().getTeamTypeDescription() + ") for event \"" + event.getName() + "\".\n";
						++failed;
					}
					else {
						Event cur = getTournamentViewManager().getTournament().getEvent(event.getName());
						if(cur != null && cur.isStarted()) {
							messages += "Unabled to update in-progress/completed event (\"" + event.getName() + "\").\n";
							++failed;
						}
						else {
							events.add(event);
						}
					}
					break;
				}
			}
		}
		for(Event event : events) {
			String invalidLevels = "";
			List<String> levels = new ArrayList<String>();
			for(String level : event.getLevels()) {
				if(getTournamentViewManager().getTournament().getLevels().contains(level)) {
					levels.add(level);
				}
				else {
					invalidLevels += level + ", ";
				}
			}
			if(!invalidLevels.isEmpty()) {
				invalidLevels = invalidLevels.substring(0, invalidLevels.length() - 2);
				messages += "\"" + invalidLevels + "\" are not valid levels for event \"" + event.getName() + "\".\n";
			}
			Event newEvent = null;
			try {
				Constructor<? extends Event> constructor = event.getClass().getConstructor(String.class, List.class, Team.class, int.class, int.class, int.class, int.class, int.class);
				newEvent = constructor.newInstance(event.getName(), levels, event.getTeamFilter().newInstance(), event.getNumberOfTeams(), event.getMinScore(), event.getMaxScore(), event.getWinBy(), event.getBestOf());
			}
			catch(Exception e) {
				++failed;
			}
			if(newEvent == null) {
				continue;
			}
			newEvent.setFilterTeamByLevel(event.getFilterTeamByLevel());
			List<Team> teams = new ArrayList<Team>();
			for(Team team : event.getTeams()) {
				if(team == null) {
					teams.add(null);
					continue;
				}
				HashSet<Player> players = new HashSet<Player>();
				for(Player player : team.getPlayers()) {
					if(player.isMale()) {
						players.add(males.get(player.getName()));
					}
					else {
						players.add(females.get(player.getName()));
					}
				}
				boolean foundTeam = false;
				for(Team cur : getTournamentViewManager().getTournament().getTeams(newEvent.getTeamFilter())) {
					if((new HashSet<Player>(cur.getPlayers())).equals(players)) {
						foundTeam = true;
						teams.add(cur);
						break;
					}
				}
				if(!foundTeam) {
					messages += "Unable to find team (\"" + team.getName() + "\") for event \"" + newEvent.getName() + ".\n";
					teams.add(null);
				}
			}
			newEvent.setTeams(teams);
			if(getTournamentViewManager().getTournament().getEvent(newEvent.getName()) == null) {
				++imported;
				getTournamentViewManager().getTournament().addEvent(newEvent);
			}
			else {
				++updated;
				getTournamentViewManager().getTournament().replaceEvent(newEvent);
			}
		}
		if(imported > 0) {
			messages += "Imported " + imported + " events.\n";
		}
		if(updated > 0) {
			messages += "Updated " + updated + " events.\n";
		}
		if(failed > 0) {
			messages += "Failed to import " + failed + " events.\n";
		}
		return messages;
	}
	
	protected String validateImportData() {
		return null;
	}
}
