package data.team.singles;

import java.io.IOException;

import data.team.modifier.MaleModifier;

@Deprecated
public class MenSinglesTeam extends OnePlayerTeam {
	private static final long serialVersionUID = -8497091295026528398L;
	
	public MenSinglesTeam() {
		super();
		setDataFromOldTeam(1, "Men's Singles");
		addModifier(new MaleModifier());
	}
	
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		if(isNew) {
			return;
		}
		setDataFromOldTeam(1, "Men's Singles");
		setPlayer(0, player);
		addModifier(new MaleModifier());
		setMatchesWon(getMatchesPlayed());
		isNew = true;
	}
}
