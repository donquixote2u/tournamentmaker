package data.team.modifier;

import data.player.Player;

@CreatableTeamModifier(displayName = "Female Only")
public class FemaleModifier extends TeamModifier {
	private static final long serialVersionUID = 878380346332724569L;

	public boolean isValidPlayer(Player player) {
		if(player == null) {
			return false;
		}
		return !player.isMale();
	}
}
