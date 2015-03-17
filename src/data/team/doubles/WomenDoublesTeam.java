package data.team.doubles;

import java.io.IOException;

import data.team.modifier.FemaleModifier;

@Deprecated
public class WomenDoublesTeam extends TwoPlayerTeam {
	private static final long serialVersionUID = -5536559951814974851L;
	
	public WomenDoublesTeam() {
		super();
		setDataFromOldTeam(2, "Women's Doubles");
		addModifier(new FemaleModifier());
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(isNew) {
			return;
		}
		setDataFromOldTeam(2, "Women's Doubles");
		setPlayer(0, p1);
		setPlayer(1, p2);
		addModifier(new FemaleModifier());
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
