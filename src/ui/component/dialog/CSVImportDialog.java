package ui.component.dialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

import data.event.Event;
import data.player.Player;
import data.team.Team;
import ui.component.textfield.TextFieldWithDropDown;
import ui.main.TournamentUI;
import ui.main.TournamentViewManager;
import ui.util.GenericUtils;

public class CSVImportDialog extends ImportDialog {
	private static final long serialVersionUID = 4289510798693343332L;
	public static final String FILE_EXTENSION = ".csv";
	private static final String PLAYERS = "Import Players";
	private static final String TEAMS = "Import Teams";
	private File dataFile;
	private TreeMap<String, Integer> columns;
	private JComboBox<String> dataType;
	private JPanel teamFields, playerFields;
	private JComboBox<String> teamType;
	private JComboBox<String> eventName;
	private TextFieldWithDropDown teamName, seed;
	private TextFieldWithDropDown playerName, gender, address, email, phone, member, club, level, dob, paid, due, events;
	
	public CSVImportDialog(TournamentUI owner, TournamentViewManager manager, File file) {
		super(owner, manager, "Import from CSV Data File", file);
	}
	
	protected Component getDisplay() {
		JPanel display = new JPanel();
		display.setLayout(new BorderLayout());
		dataType = new JComboBox<String>();
		display.add(dataType, BorderLayout.PAGE_START);
		JPanel root = new JPanel();
		root.setLayout(new BoxLayout(root, BoxLayout.PAGE_AXIS));
		display.add(root, BorderLayout.CENTER);
		JPanel description = new JPanel(new GridBagLayout());
		description.add(new JLabel("Each drop down field contains the column headers from the selected CSV file. You can either selected a column header, or directly type into the drop down."), GenericUtils.createGridBagConstraint(0, 0, 1.0));
		description.add(new JLabel("Selecting a column header will insert it at your cursor's position. For each row, every column header will be converted to the value of the column in that row."), GenericUtils.createGridBagConstraint(0, 1, 1.0));
		description.add(new JLabel("All other text will be directly inserted as part of the field value. \"[\" and \"]\" are reserved for column headers and can not be used for anything else."), GenericUtils.createGridBagConstraint(0, 2, 1.0));
		description.add(new JLabel("Example:"), GenericUtils.createGridBagConstraint(0, 3, 1.0));
		description.add(new JLabel("Name = [Firstname] [Lastname]"), GenericUtils.createGridBagConstraint(0, 4, 1.0));
		description.add(new JLabel("For each row in the CSV file, the name is going to be the value in the \"Firstname\" column followed by a space and then the value in the \"Lastname\" column."), GenericUtils.createGridBagConstraint(0, 5, 1.0));
		root.add(description);
		teamFields = new JPanel(new GridBagLayout());
		teamFields.add(new JLabel("Type of Team"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
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
				while(teamFields.getComponentCount() > 8) {
					teamFields.remove(8);
				}
				Team team = getTournamentViewManager().getTournament().getTeamByType(previouslySelectedType);
				for(int i = 0; i < team.getNumberOfPlayers(); ++i) {
					teamFields.add(new JLabel("Player " + (i + 1) + " Name (Required)"), GenericUtils.createGridBagConstraint(0, 4 + i, 0.3));
					teamFields.add(new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 4 + i, 0.7));
				}
				pack();
			}
		});
		teamFields.add(teamType, GenericUtils.createGridBagConstraint(1, 0, 0.7));
		teamFields.add(new JLabel("Event Name"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		List<String> eventNames = new ArrayList<String>();
		for(Event event : getTournamentViewManager().getTournament().getEvents()) {
			eventNames.add(event.getName());
		}
		Collections.sort(eventNames);
		eventNames.add(0, "");
		eventName = new JComboBox<String>(eventNames.toArray(new String[0]));
		teamFields.add(eventName, GenericUtils.createGridBagConstraint(1, 1, 0.7));
		teamFields.add(new JLabel("Name"), GenericUtils.createGridBagConstraint(0, 2, 0.3));
		teamFields.add(teamName = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 2, 0.7));
		teamFields.add(new JLabel("Seed"), GenericUtils.createGridBagConstraint(0, 3, 0.3));
		teamFields.add(seed = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 3, 0.7));
		root.add(teamFields);
		playerFields = new JPanel(new GridBagLayout());
		playerFields.add(new JLabel("Name (Required)"), GenericUtils.createGridBagConstraint(0, 0, 0.3));
		playerFields.add(playerName = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 0, 0.7));
		playerFields.add(new JLabel("Gender (Male/Female, Required)"), GenericUtils.createGridBagConstraint(0, 1, 0.3));
		playerFields.add(gender = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 1, 0.7));
		playerFields.add(new JLabel("Address"), GenericUtils.createGridBagConstraint(0, 2, 0.3));
		playerFields.add(address = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 2, 0.7));
		playerFields.add(new JLabel("Email"), GenericUtils.createGridBagConstraint(0, 3, 0.3));
		playerFields.add(email = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 3, 0.7));
		playerFields.add(new JLabel("Phone Number"), GenericUtils.createGridBagConstraint(0, 4, 0.3));
		playerFields.add(phone = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 4, 0.7));
		playerFields.add(new JLabel("Membership ID"), GenericUtils.createGridBagConstraint(0, 5, 0.3));
		playerFields.add(member = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 5, 0.7));
		playerFields.add(new JLabel("Club"), GenericUtils.createGridBagConstraint(0, 6, 0.3));
		playerFields.add(club = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 6, 0.7));
		playerFields.add(new JLabel("Level"), GenericUtils.createGridBagConstraint(0, 7, 0.3));
		playerFields.add(level = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 7, 0.7));
		playerFields.add(new JLabel("Date of Birth (MM/DD/YYYY)"), GenericUtils.createGridBagConstraint(0, 8, 0.3));
		playerFields.add(dob = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 8, 0.7));
		playerFields.add(new JLabel("Amount Paid (Number)"), GenericUtils.createGridBagConstraint(0, 9, 0.3));
		playerFields.add(paid = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 9, 0.7));
		playerFields.add(new JLabel("Amount Due (Number)"), GenericUtils.createGridBagConstraint(0, 10, 0.3));
		playerFields.add(due = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 10, 0.7));
		playerFields.add(new JLabel("Event Names"), GenericUtils.createGridBagConstraint(0, 11, 0.3));
		playerFields.add(events = new TextFieldWithDropDown(columns.keySet()), GenericUtils.createGridBagConstraint(1, 11, 0.7));
		root.add(playerFields);
		dataType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if(PLAYERS.equals(dataType.getSelectedItem())) {
					teamFields.setVisible(false);
					playerFields.setVisible(true);
					pack();
				}
				else if(TEAMS.equals(dataType.getSelectedItem())) {
					boolean needRefresh = teamType.getItemCount() == 0 || getTournamentViewManager().getTournament().getTeamTypes().isEmpty();
					if(!((TournamentUI) getOwner()).checkForExistingTeamTypes()) {
						dataType.setSelectedItem(PLAYERS);
						return;
					}
					if(needRefresh) {
						refreshTeamTypes();
					}
					teamFields.setVisible(true);
					playerFields.setVisible(false);
					pack();
				}
			}
		});
		dataType.addItem(PLAYERS);
		dataType.addItem(TEAMS);
		return display;
	}
	
	protected boolean isValid(File file) {
		if(!file.getName().toLowerCase().endsWith(FILE_EXTENSION)) {
			return false;
		}
		dataFile = file;
		ICsvListReader listReader = null;
		try {
			listReader = new CsvListReader(new FileReader(dataFile), CsvPreference.STANDARD_PREFERENCE);
			columns = new TreeMap<String, Integer>();
			String[] header = listReader.getHeader(true);
			for(int i = 0; i < header.length; ++i) {
				if(header[i].trim().isEmpty()) {
					continue;
				}
				columns.put(header[i].replace('[', '{').replace(']', '}').trim(), i);
			}
			if(columns.keySet().isEmpty()) {
				return false;
			}
		}
		catch(Exception e) {
			return false;
		}
		finally {
			if(listReader != null) {
				try {
					listReader.close();
				}
				catch(Exception e) {}
			}
		}
		return true;
	}
	
	protected String importData() {
		String messages = "";
		int imported = 0, updated = 0, failed = 0;
		ICsvListReader listReader = null;
		try {
			List<Event> tournamentEvents = new ArrayList<Event>(getTournamentViewManager().getTournament().getEvents());
			Collections.sort(tournamentEvents, new Comparator<Event>() {
				public int compare(Event e1, Event e2) {
					return e2.getName().length() - e1.getName().length();
				}
			});
			Team newTeamType = null;
			ArrayList<Player> players = new ArrayList<Player>();
			if(TEAMS.equals(dataType.getSelectedItem())) {
				newTeamType = getTournamentViewManager().getTournament().getTeamByType((String) teamType.getSelectedItem()).newInstance();
				for(Player player : getTournamentViewManager().getTournament().getPlayers()) {
					if(newTeamType.isValidPlayer(player)) {
						players.add(player);
					}
				}
			}
			listReader = new CsvListReader(new FileReader(dataFile), CsvPreference.STANDARD_PREFERENCE);
			// throw away the header
			listReader.getHeader(true);
			// read the data and build the player/team object
			List<String> values;
			while((values = listReader.read()) != null) {
				boolean isNew = false;
				if(PLAYERS.equals(dataType.getSelectedItem())) {
					String name = getValueFromData(playerName.getText(), values, true);
					if(name.isEmpty()) {
						messages += "Unable to get a valid name value from row " + listReader.getRowNumber() + ".\n";
						++failed;
						continue;
					}
					String genderString = getValueFromData(gender.getText(), values, true);
					boolean isMale;
					if(genderString.equalsIgnoreCase("male") || genderString.toLowerCase().startsWith("m")) {
						isMale = true;
					}
					else if(genderString.equalsIgnoreCase("female") || genderString.toLowerCase().startsWith("f")) {
						isMale = false;
					}
					else {
						messages += "Unable to get a valid gender value from row " + listReader.getRowNumber() + ".\n";
						++failed;
						continue;
					}
					// try to find a matching player
					Player newPlayer = null;
					for(Player player : getTournamentViewManager().getTournament().getPlayers()) {
						if(player.isMale() == isMale && player.getName().equals(name)) {
							++updated;
							newPlayer = player;
							break;
						}
					}
					if(newPlayer == null) {
						++imported;
						newPlayer = new Player(name, isMale);
						isNew = true;
					}
					// grab all the player fields
					newPlayer.setAddress(getValueFromData(address.getText(), values, true));
					newPlayer.setEmail(getValueFromData(email.getText(), values, true));
					newPlayer.setPhoneNumber(getValueFromData(phone.getText(), values, true));
					newPlayer.setMembershipNumber(getValueFromData(member.getText(), values, false));
					newPlayer.setClub(getValueFromData(club.getText(), values, false));
					boolean setLevel = false;
					String levelValue = getValueFromData(level.getText(), values, false).trim();
					if(levelValue.isEmpty()) {
						newPlayer.setLevel(null);
						setLevel = true;
					}
					else {
						for(String level : getTournamentViewManager().getTournament().getLevels()) {
							if(level.equalsIgnoreCase(levelValue)) {
								newPlayer.setLevel(level);
								setLevel = true;
								break;
							}
						}
					}
					if(!setLevel) {
						messages += "\"" + levelValue + "\" is not a valid level (player \"" + name + "\" at row " + listReader.getRowNumber() + ").\n";
					}
					String dateString = getValueFromData(dob.getText(), values, true);
					Date date = GenericUtils.stringToDate(dateString, "MM/dd/yyyy");
					if(date == null) {
						date = GenericUtils.stringToDate(dateString, "MM/dd/yy");
					}
					if(date == null && !dateString.isEmpty()) {
						messages += "\"" + dateString + "\" is not a valid date (player \"" + name + "\" at row " + listReader.getRowNumber() + ").\n";
					}
					else {
						newPlayer.setDateOfBirth(date);
					}
					String amount = getValueFromData(paid.getText(), values, true);
					try {
						double amountPaid = Double.parseDouble(amount);
						newPlayer.setAmountPaid(amountPaid);
					}
					catch(Exception e) {
						if(!amount.isEmpty()) {
							messages += "\"" + amount + "\" is not valid for amount paid (player \"" + name + "\" at row " + listReader.getRowNumber() + ").\n";
						}
					}
					amount = getValueFromData(due.getText(), values, true);
					try {
						double amountDue = Double.parseDouble(amount);
						newPlayer.setAmountDue(amountDue);
					}
					catch(Exception e) {
						if(!amount.isEmpty()) {
							messages += "\"" + amount + "\" is not valid for amount due (player \"" + name + "\" at row " + listReader.getRowNumber() + ").\n";
						}
					}
					String eventString = getValueFromData(events.getText(), values, false);
					String extractedEvents = eventString;
					HashSet<String> eventNames = new HashSet<String>();
					for(Event event : tournamentEvents) {
						if(!event.getTeamFilter().isValidPlayer(newPlayer)) {
							continue;
						}
						String newString = GenericUtils.extractValueFromString(event.getName(), extractedEvents);
						if(newString != null) {
							extractedEvents = newString;
							eventNames.add(event.getName());
						}
					}
					newPlayer.setEvents(eventNames);
					if(!extractedEvents.trim().isEmpty() && !eventString.trim().isEmpty()) {
						messages += "Invalid event names (\"" + extractedEvents + "\") were detected (player \"" + name + "\" at row " + listReader.getRowNumber() + ").\n";
					}
					if(isNew) {
						getTournamentViewManager().getTournament().addPlayer(newPlayer);
					}
				}
				else if(TEAMS.equals(dataType.getSelectedItem())) {
					// find the matching players and create the team
					Team newTeam = newTeamType.newInstance();
					boolean matchedPlayer = false;
					for(int i = 9; i < teamFields.getComponentCount(); i += 2) {
						matchedPlayer = false;
						String playerName = getValueFromData(((TextFieldWithDropDown) teamFields.getComponent(i)).getText(), values, true);
						if(!playerName.isEmpty()) {
							for(Player player : players) {
								if(player.getName().equals(playerName)) {
									matchedPlayer = true;
									newTeam.setPlayer((i - 8) / 2, player);
									break;
								}
							}
						}
						if(!matchedPlayer) {
							++failed;
							messages += "Unable to find a player named \"" + playerName + "\" (row " + listReader.getRowNumber() + ").\n";
							break;
						}
					}
					if(!matchedPlayer) {
						continue;
					}
					if(!newTeam.isValid()) {
						++failed;
						messages += "The data from row " + listReader.getRowNumber() + " does not create a valid team.\n";
						continue;
					}
					isNew = true;
					for(Team team : getTournamentViewManager().getTournament().getTeams(newTeamType)) {
						if((new HashSet<Player>(team.getPlayers())).equals(new HashSet<Player>(newTeam.getPlayers()))) {
							++updated;
							newTeam = team;
							isNew = false;
							break;
						}
					}
					newTeam.setName(getValueFromData(teamName.getText(), values, true));
					newTeam.setSeed(getValueFromData(seed.getText(), values, false));
					String event = (String) eventName.getSelectedItem();
					if(event != null && !event.isEmpty()) {
						for(Player player : newTeam.getPlayers()) {
							HashSet<String> events = new HashSet<String>(player.getEvents());
							events.add(event);
							player.setEvents(events);
						}
					}
					if(isNew) {
						++imported;
						getTournamentViewManager().getTournament().addTeam(newTeam);
					}
				}
				else {
					throw new IllegalArgumentException("Invalid data type selected.");
				}
			}
		}
		catch(Exception e) {
			return "Error encountered: \"" + e.getMessage() + "\"";
		}
		finally {
			if(listReader != null) {
				try {
					listReader.close();
				}
				catch(Exception e) {}
			}
		}
		if(PLAYERS.equals(dataType.getSelectedItem())) {
			if(imported > 0) {
				messages += "Imported " + imported + " players.\n";
			}
			if(updated > 0) {
				messages += "Updated " + updated + " players.\n";
			}
			if(failed > 0) {
				messages += "Failed to import " + failed + " players.\n";
			}
		}
		else if(TEAMS.equals(dataType.getSelectedItem())) {
			if(imported > 0) {
				messages += "Imported " + imported + " teams.\n";
			}
			if(updated > 0) {
				messages += "Updated " + updated + " teams.\n";
			}
			if(failed > 0) {
				messages += "Failed to import " + failed + " teams.\n";
			}
		}
		return messages;
	}
	
	protected String validateImportData() {
		String errors = "";
		if(PLAYERS.equals(dataType.getSelectedItem())) {
			if(!isValidText(playerName.getText())) {
				errors += "Name is invalid.\n";
			}
			else if(playerName.getText().isEmpty() || playerName.getText().indexOf('[') == -1) {
				errors += "Name can not be empty and must contain at least one column header.\n";
			}
			if(!isValidText(gender.getText())) {
				errors += "Gender is invalid.\n";
			}
			else if(gender.getText().isEmpty()) {
				errors += "Gender can not be empty.\n";
			}
			if(!isValidText(address.getText())) {
				errors += "Address is invalid.\n";
			}
			if(!isValidText(email.getText())) {
				errors += "Email is invalid.\n";
			}
			if(!isValidText(phone.getText())) {
				errors += "Phone Number is invalid.\n";
			}
			if(!isValidText(member.getText())) {
				errors += "Membership ID is invalid.\n";
			}
			if(!isValidText(club.getText())) {
				errors += "Club is invalid.\n";
			}
			if(!isValidText(level.getText())) {
				errors += "Level is invalid.\n";
			}
			if(!isValidText(dob.getText())) {
				errors += "Date of Birth is invalid.\n";
			}
			if(!isValidText(paid.getText())) {
				errors += "Amount Paid is invalid.\n";
			}
			if(!isValidText(due.getText())) {
				errors += "Amount Due is invalid.\n";
			}
			if(!isValidText(events.getText())) {
				errors += "Event Names is invalid.\n";
			}
		}
		else if(TEAMS.equals(dataType.getSelectedItem())) {
			if(!isValidText(teamName.getText())) {
				errors += "Name is invalid.\n";
			}
			if(!isValidText(seed.getText())) {
				errors += "Seed is invalid.\n";
			}
			for(int i = 9; i < teamFields.getComponentCount(); i += 2) {
				TextFieldWithDropDown field = (TextFieldWithDropDown) teamFields.getComponent(i);
				if(!isValidText(field.getText())) {
					errors += "Player " + (((i - 8) / 2) + 1) + " Name is invalid.\n";
				}
				else if(field.getText().isEmpty() || field.getText().indexOf('[') == -1) {
					errors += "Player " + (((i - 8) / 2) + 1) + " Name can not be empty and must contain at least one column header.\n";
				}
			}
		}
		else {
			errors = "Invalid data type selected.";
		}
		return errors;
	}
	
	private boolean isValidText(String text) {
		if(text == null) {
			return false;
		}
		if(text.isEmpty()) {
			return true;
		}
		int start, end = 0;
		do {
			start = text.indexOf('[', end);
			end = text.indexOf(']', end);
			if(start == -1 && end == -1) {
				continue;
			}
			if((start == -1 && end != -1) || (start != -1 && end == -1) || start > end) {
				return false;
			}
			if(!columns.containsKey(text.substring(++start, end++))) {
				return false;
			}
		}
		while(end != -1 && end < text.length());
		return true;
	}
	
	private String getValueFromData(String text, List<String> values, boolean removeDoubleSpaces) {
		if(text == null || text.isEmpty()) {
			return "";
		}
		int start, end = 0;
		do {
			start = text.indexOf('[', end);
			end = text.indexOf(']', end);
			if(start == -1 && end == -1) {
				continue;
			}
			int index = columns.get(text.substring(start + 1, end));
			String value = null;
			if(index >= 0 && index < values.size()) {
				value = values.get(index);
			}
			if(value == null) {
				value = "";
			}
			text = text.substring(0, start) + value + text.substring(end + 1);
			end -= end - start;
			end += value.length();
		}
		while(end != -1 && end < text.length());
		if(removeDoubleSpaces) {
			text = text.replaceAll("\\s+", " ").trim();
		}
		return text;
	}
	
	private void refreshTeamTypes() {
		teamType.removeAllItems();
		List<String> types = getTournamentViewManager().getTournament().getTeamTypes();
		Collections.sort(types);
		types.add(TournamentUI.NEW_DISPLAY_TEXT);
		for(String type : types) {
			teamType.addItem(type);
		}
	}
}
