package data.team.doubles;

import java.io.IOException;

import data.team.modifier.MaleModifier;

@Deprecated
public class MenDoublesTeam extends TwoPlayerTeam {
	private static final long serialVersionUID = -7422611599917104458L;
	
	public MenDoublesTeam() {
		super();
		setDataFromOldTeam(2, "Men's Doubles");
		addModifier(new MaleModifier());
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(isNew) {
			return;
		}
		setDataFromOldTeam(2, "Men's Doubles");
		setPlayer(0, p1);
		setPlayer(1, p2);
		addModifier(new MaleModifier());
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
