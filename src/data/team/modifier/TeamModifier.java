package data.team.modifier;

import java.io.Serializable;

import data.player.Player;
import data.team.Team;

public class TeamModifier implements Serializable {
	private static final long serialVersionUID = 7552784768382347279L;

	public boolean isValidPlayer(Player player) {
		if(player == null) {
			return false;
		}
		return true;
	}
	
	public boolean isValidTeam(Team team) {
		if(team == null) {
			return false;
		}
		for(Player player : team.getPlayers()) {
			if(!isValidPlayer(player)) {
				return false;
			}
		}
		return true;
	}
	
	public String[] getParameters() {
		return new String[0];
	}
	
	public static boolean isApplicableForTeamSize(int numberOfPlayers, Class<? extends TeamModifier> modifierClass) {
		if(modifierClass == null) {
			return false;
		}
		CreatableTeamModifier annotation = modifierClass.getAnnotation(CreatableTeamModifier.class);
		if(annotation == null) {
			return false;
		}
		return numberOfPlayers >= annotation.minSize() && numberOfPlayers <= annotation.maxSize();
	}
}
