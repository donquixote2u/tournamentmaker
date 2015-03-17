package data.team.doubles;

import java.io.IOException;

import data.team.modifier.MixedModifier;

@Deprecated
public class MixDoublesTeam extends TwoPlayerTeam {
	private static final long serialVersionUID = -8311038301354402630L;
	
	public MixDoublesTeam() {
		super();
		setDataFromOldTeam(2, "Mixed Doubles");
		addModifier(new MixedModifier());
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(isNew) {
			return;
		}
		setDataFromOldTeam(2, "Mixed Doubles");
		setPlayer(0, p1);
		setPlayer(1, p2);
		addModifier(new MixedModifier());
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
