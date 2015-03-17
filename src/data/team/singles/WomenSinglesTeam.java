package data.team.singles;

import java.io.IOException;

import data.team.modifier.FemaleModifier;

@Deprecated
public class WomenSinglesTeam extends OnePlayerTeam {
	private static final long serialVersionUID = -7522189851667544219L;
	
	public WomenSinglesTeam() {
		super();
		setDataFromOldTeam(1, "Women's Singles");
		addModifier(new FemaleModifier());
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(isNew) {
			return;
		}
		setDataFromOldTeam(1, "Women's Singles");
		setPlayer(0, player);
		addModifier(new FemaleModifier());
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
