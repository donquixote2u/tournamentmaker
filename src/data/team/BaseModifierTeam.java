package data.team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import data.player.Player;
import data.team.modifier.CreatableTeamModifier;
import data.team.modifier.TeamModifier;

public class BaseModifierTeam extends Team {
	private static final long serialVersionUID = -5816332302614045712L;
	private Player[] players;
	private String teamType;
	private List<TeamModifier> modifiers;
	
	public BaseModifierTeam(int numberOfPlayers, String teamType) {
		if(teamType == null || teamType.trim().isEmpty() || numberOfPlayers <= 0) {
			throw new RuntimeException("invalid parameters");
		}
		this.teamType = teamType.trim();
		players = new Player[numberOfPlayers];
		modifiers = new ArrayList<TeamModifier>();
	}
	
	public final List<Player> getPlayers() {
		return Arrays.asList(players);
	}
	
	public final Player getPlayer(int index) {
		if(index < 0 || index >= players.length) {
			return null;
		}
		return players[index];
	}
	
	public final boolean setPlayer(int index, Player player) {
		if(!isValidPlayer(player)) {
			return false;
		}
		if(index >= 0 && index < players.length) {
			if(players[index] != null && isInMatch()) {
				players[index].setInGame(false);
			}
			players[index] = player;
			if(isInMatch()) {
				player.setInGame(true);
			}
			return true;
		}
		return false;
	}
	
	public final boolean isValid() {
		for(int i = 0; i < players.length; ++i) {
			if(players[i] == null) {
				return false;
			}
			for(int j = i + 1; j < players.length; ++j) {
				if(players[i].equals(players[j])) {
					return false;
				}
			}
		}
		for(TeamModifier modifier : modifiers) {
			if(!modifier.isValidTeam(this)) {
				return false;
			}
		}
		return true;
	}
	
	public final int getNumberOfPlayers() {
		return players.length;
	}

	public final boolean isValidPlayer(Player player) {
		if(player == null) {
			return false;
		}
		for(TeamModifier modifier : modifiers) {
			if(!modifier.isValidPlayer(player)) {
				return false;
			}
		}
		return true;
	}
	
	public final void addModifier(TeamModifier modifier) {
		if(modifier == null || !TeamModifier.isApplicableForTeamSize(getNumberOfPlayers(), modifier.getClass())) {
			return;
		}
		modifiers.add(modifier);
		Collections.sort(modifiers, new Comparator<TeamModifier>() {
			public int compare(TeamModifier mod1, TeamModifier mod2) {
				CreatableTeamModifier a1 = mod1.getClass().getAnnotation(CreatableTeamModifier.class);
				CreatableTeamModifier a2 = mod2.getClass().getAnnotation(CreatableTeamModifier.class);
				int result = a1.parameters().length - a2.parameters().length;
				if(result != 0) {
					return result;
				}
				return a1.displayName().compareTo(a2.displayName());
			}
		});
	}
	
	public final String getTeamType() {
		return teamType;
	}
	
	public final String getTeamTypeDescription() {
		String description = teamType + " - " + Integer.toString(getNumberOfPlayers()) + " ";
		if(getNumberOfPlayers() == 1) {
			description += "player";
		}
		else {
			description += "players";
		}
		description += ", ";
		for(TeamModifier modifier : modifiers) {
			CreatableTeamModifier annoation = modifier.getClass().getAnnotation(CreatableTeamModifier.class);
			description += annoation.displayName();
			String[] parameters = modifier.getParameters();
			String[] parameterDescriptions = annoation.parameters();
			if(parameters.length > 0) {
				description += " (";
			}
			for(int i = 0; i < parameters.length; ++i) {
				description += parameterDescriptions[i] + ": ";
				description += parameters[i] + ", ";
			}
			if(parameters.length > 0) {
				description = description.substring(0, description.length() - 2);
				description += ")";
			}
			description += ", ";
		}
		description = description.substring(0, description.length() - 2);
		return description;
	}
	
	public boolean isValidTeam(Team team) {
		if(team == null) {
			return false;
		}
		if(!getTeamType().equals(team.getTeamType())) {
			return false;
		}
		return team.isValid();
	}
	
	public BaseModifierTeam newInstance() {
		BaseModifierTeam team = new BaseModifierTeam(getNumberOfPlayers(), getTeamType());
		for(TeamModifier modifier : modifiers) {
			team.addModifier(modifier);
		}
		return team;
	}
	
	public String getName() {
		String teamName = super.getName();
		if(teamName == null || teamName.trim().isEmpty()) {
			if(isValid()) {
				teamName = "";
				for(Player player : players) {
					teamName += player.getName() + " / ";
				}
				if(!teamName.isEmpty()) {
					teamName = teamName.substring(0, teamName.length() - 3);
				}
				return teamName;
			}
		}
		return teamName;
	}
	
	// this is for backwards compatibility
	protected void setDataFromOldTeam(int numberOfPlayer, String teamType) {
		this.teamType = teamType;
		players = new Player[numberOfPlayer];
		modifiers = new ArrayList<TeamModifier>();
	}
}