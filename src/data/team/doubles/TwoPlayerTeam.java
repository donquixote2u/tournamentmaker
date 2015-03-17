package data.team.doubles;

import java.io.IOException;

import data.player.Player;
import data.team.BaseModifierTeam;

@Deprecated
public class TwoPlayerTeam extends BaseModifierTeam {
	private static final long serialVersionUID = 2285538455094782693L;
	protected Player p1, p2;
	protected boolean isNew;
	
	public TwoPlayerTeam() {
		super(2, "Two");
		isNew = true;
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(!getClass().getSimpleName().equals(TwoPlayerTeam.class.getSimpleName())) {
			return;
		}
		if(isNew) {
			return;
		}
		setDataFromOldTeam(2, "Unrestricted Two Player");
		setPlayer(0, p1);
		setPlayer(1, p2);
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
