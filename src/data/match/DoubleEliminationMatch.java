package data.match;

import data.team.Team;

public class DoubleEliminationMatch extends Match {
	private static final long serialVersionUID = -7097177482985048749L;

	public DoubleEliminationMatch(int minScore, int maxScore, int winBy, int bestOf) {
		super(minScore, maxScore, winBy, bestOf);
	}
	
	public DoubleEliminationMatch(Team t1, Team t2, int minScore, int maxScore, int winBy, int bestOf) {
		super(t1, t2, minScore, maxScore, winBy, bestOf);
	}
	
	public String getLoserMatchId(Team team, int numberOfAdditionalWins, int numberOfAdditionalLosses) {
		if(team == null) {
			return null;
		}
		return Integer.toString(team.getMatchesLost() + numberOfAdditionalLosses);
	}
}
