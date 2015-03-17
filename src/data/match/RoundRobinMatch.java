package data.match;

import data.team.Team;

public class RoundRobinMatch extends Match {
	private static final long serialVersionUID = 300972883593711232L;
	
	public RoundRobinMatch(int minScore, int maxScore, int winBy, int bestOf) {
		super(minScore, maxScore, winBy, bestOf);
	}
	
	public RoundRobinMatch(Team t1, Team t2, int minScore, int maxScore, int winBy, int bestOf) {
		super(t1, t2, minScore, maxScore, winBy, bestOf);
	}
	
	public boolean getTeam1Forfeit() {
		Team team = getTeam1();
		if(team == null) {
			return super.getTeam1Forfeit();
		}
		return team.isWithdrawn() || super.getTeam1Forfeit();
	}
	
	public boolean getTeam2Forfeit() {
		Team team = getTeam2();
		if(team == null) {
			return super.getTeam2Forfeit();
		}
		return team.isWithdrawn() || super.getTeam2Forfeit();
	}
}
