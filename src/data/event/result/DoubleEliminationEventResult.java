package data.event.result;

import java.util.ArrayList;
import java.util.List;

import data.event.DoubleEliminationEvent;
import data.event.EventUtils;
import data.match.Match;
import data.team.Team;

public class DoubleEliminationEventResult extends EventResult {
	public DoubleEliminationEventResult(DoubleEliminationEvent event) {
		super("");
		calculateWinners(event);
	}
	
	private void calculateWinners(DoubleEliminationEvent event) {
		clearWinners();
		// get the loser matches
		List<Match> matches = event.getMatches(event.getDisplayLevels().get(1));
		if(matches == null || matches.size() == 0) {
			return;
		}
		// get the finals match
		while(matches.size() > 1) {
			matches = EventUtils.getWinnerMatches(matches);
		}
		Match finals = matches.get(0);
		ArrayList<Team> teams = new ArrayList<Team>();
		// add the first and second place
		Match winnersFinals = finals.getWinnerMatch();
		if(winnersFinals.getTeam1() == null && winnersFinals.getTeam2() == null) {
			// looks like everyone withdrew
			return;
		}
		// check to see if we need to look at the 2nd finals match
		if(winnersFinals.getWinnerMatch().getTeam1() != null && winnersFinals.getWinnerMatch().getTeam2() != null) {
			teams.add(winnersFinals.getWinnerMatch().getWinner());
			addWinners(teams);
			teams.clear();
			teams.add(winnersFinals.getWinnerMatch().getLoser());
			addWinners(teams);
			teams.clear();
		}
		else {
			teams.add(winnersFinals.getWinner());
			addWinners(teams);
			teams.clear();
			teams.add(winnersFinals.getLoser());
			addWinners(teams);
			teams.clear();
		}
		// add the rest of the matches
		ArrayList<Match> currentMatches = new ArrayList<Match>();
		ArrayList<Match> nextMatches = new ArrayList<Match>();
		currentMatches.add(finals);
		while(!currentMatches.isEmpty()) {
			Match match = currentMatches.remove(0);
			teams.add(match.getLoser());
			Match pre = match.getDefaultToTeam1Match();
			if(pre != null && !pre.getIgnoreMatch()) {
				nextMatches.add(pre);
			}
			pre = match.getDefaultToTeam2Match();
			if(pre != null && !pre.getIgnoreMatch()) {
				nextMatches.add(pre);
			}
			if(currentMatches.isEmpty()) {
				addWinners(teams);
				teams.clear();
				currentMatches = nextMatches;
				nextMatches = new ArrayList<Match>();
			}
		}
	}
}
