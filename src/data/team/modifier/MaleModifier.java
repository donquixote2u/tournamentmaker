package data.team.modifier;

import data.player.Player;

@CreatableTeamModifier(displayName = "Male Only")
public class MaleModifier extends TeamModifier {
	private static final long serialVersionUID = -9126487676585782341L;

	public boolean isValidPlayer(Player player) {
		if(player == null) {
			return false;
		}
		return player.isMale();
	}
}
