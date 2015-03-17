package data.team.singles;

import java.io.IOException;

import data.player.Player;
import data.team.BaseModifierTeam;

@Deprecated
public class OnePlayerTeam extends BaseModifierTeam {
	private static final long serialVersionUID = -4962010950139788791L;
	protected Player player;
	protected boolean isNew;
	
	public OnePlayerTeam() {
		super(1, "One");
		isNew = true;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(!getClass().getSimpleName().equals(OnePlayerTeam.class.getSimpleName())) {
			return;
		}
		if(isNew) {
			return;
		}
		setDataFromOldTeam(1, "Unrestricted One Player");
		setPlayer(0, player);
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
