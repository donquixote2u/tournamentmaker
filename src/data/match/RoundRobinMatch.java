package data.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
	
	public List<Match> getDependentMatchesForUndo() {
		List<Match> matches = new ArrayList<Match>();
		for(Match match : getEvent().getAllMatches()) {
			if(!(match instanceof RoundRobinMatch) || this == match || !match.isComplete()) {
				continue;
			}
			if(!((RoundRobinMatch) match).hasValidResult()) {
				matches.add(match);
			}
		}
		return matches;
	}
	
	public Set<Match> recalculate() {
		if(getEnd() == null) {
			return null;
		}
		// if we undo a round robin match, we have to recalculate all the other completed matches so the game info for the teams are correct
		Team winner = getWinner();
		if(winner != null) {
			winner.setMatchesWon(winner.getMatchesWon() - 1);
			winner.setMatchesPlayed(winner.getMatchesPlayed() - 1);
			winner.setResetMatch(true);
		}
		Team loser = getLoser();
		if(loser != null) {
			loser.setMatchesLost(loser.getMatchesLost() - 1);
			loser.setMatchesPlayed(loser.getMatchesPlayed() - 1);
			loser.setResetMatch(true);
		}
		Set<Match> matches = super.recalculate();
		if(matches == null) {
			return matches;
		}
		matches.addAll(getDependentMatchesForUndo());
		return matches;
	}
	
	private boolean hasValidResult() {
		// check to see if the values in the match are valid
		List<Game> games = new ArrayList<Game>(getGames());
		boolean hasWinner = true;
		for(Game game : games) {
			if(hasWinner && hasWinner != (game.getWinner() != null)) {
				hasWinner = false;
			}
			else if(!hasWinner && (game.getWinner() != null)) {
				return false;
			}
		}
		// if the teams have not been set yet, the match is not complete
		Match toT1 = getToTeam1Match(false);
		Match toT2 = getToTeam2Match(false);
		if(getTeam1() == null && getTeam2() == null) {
			// test to make sure this isn't a match with 2 byes
			if((toT1 == null || toT1.isComplete()) && (toT2 == null || toT2.isComplete())) {
				return true;
			}
			return false;
		}
		// if there is not a previous match or if it is complete and no team was forwarded, this is a bye
		if(getTeam1() == null) {
			if(toT1 == null || toT1.isComplete()) {
				return true;
			}
			return false;
		}
		if(getTeam2() == null) {
			if(toT2 == null || toT2.isComplete()) {
				return true;
			}
			return false;
		}
		// this is a forfeit so the match is complete
		if(getTeam1Forfeit() || getTeam2Forfeit()) {
			return true;
		}
		int t1Count = 0;
		int t2Count = 0;
		int indexFinished = -1;
		int gamesToWin = (int) Math.ceil(games.size() / 2.0);
		for(int i = 0; i < games.size(); ++i) {
			Game game = games.get(i);
			Team winner = game.getWinner();
			if(winner == getTeam1()) {
				++t1Count;
			}
			else if(winner == getTeam2()) {
				++t2Count;
			}
			else if(winner == null) {
				if(game.getTeam1Score() >= 0 || game.getTeam2Score() >= 0) {
					indexFinished = -1;
				}
				break;
			}
			if(indexFinished == -1 && (t1Count == gamesToWin || t2Count == gamesToWin)) {
				indexFinished = i;
			}
		}
		if(indexFinished == -1) {
			return false;
		}
		if(t1Count + t2Count != indexFinished + 1) {
			return false;
		}
		return true;
	}
}
