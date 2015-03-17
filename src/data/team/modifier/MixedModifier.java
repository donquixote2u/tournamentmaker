package data.team.modifier;

import data.player.Player;
import data.team.Team;

@CreatableTeamModifier(displayName = "Mixed", minSize = 2, maxSize = 2)
public class MixedModifier extends TeamModifier {
	private static final long serialVersionUID = -3791702836491383836L;

	public boolean isValidTeam(Team team) {
		if(team == null) {
			return false;
		}
		boolean male = false;
		boolean female = false;
		for(Player player : team.getPlayers()) {
			if(player.isMale()) {
				male = true;
			}
			else {
				female = true;
			}
		}
		return male && female;
	}
}
