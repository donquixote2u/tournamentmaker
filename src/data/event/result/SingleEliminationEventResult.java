package data.event.result;

import java.util.ArrayList;
import java.util.List;

import data.match.Match;
import data.team.Team;

public class SingleEliminationEventResult extends EventResult {
	public SingleEliminationEventResult(String description, List<Match> matches) {
		super(description);
		calculateWinners(matches);
	}

	private void calculateWinners(List<Match> matches) {
		clearWinners();
		if(matches == null || matches.size() == 0) {
			return;
		}
		// getting to the finals game
		Match finals = matches.get(0);
		while(finals.getWinnerMatch() != null) {
			finals = finals.getWinnerMatch();
		}
		if(finals.getTeam1() == null && finals.getTeam2() == null) {
			// looks like everyone withdrew
			return;
		}
		// adding the first place team
		ArrayList<Team> currentTeams = new ArrayList<Team>();
		Team winner = finals.getWinner();
		if(winner != null) {
			currentTeams.add(winner);
			addWinners(currentTeams);
			currentTeams.clear();
		}
		// go through the remaining matches and rank the rest of the teams by traversing the bracket in reverse
		String level = finals.getLevel();
		ArrayList<Match> currentMatches = new ArrayList<Match>();
		ArrayList<Match> nextMatches = new ArrayList<Match>();
		currentMatches.add(finals);
		while(!currentMatches.isEmpty()) {
			Match match = currentMatches.remove(0);
			if(match.getLoser() != null) {
				currentTeams.add(match.getLoser());
			}
			else if(match.getWinner() == null) {
				if(match.getTeam1() != null) {
					currentTeams.add(match.getTeam1());
				}
				if(match.getTeam2() != null) {
					currentTeams.add(match.getTeam2());
				}
			}
			Match pre = match.getToTeam1Match(true);
			if(pre != null && level.equals(pre.getLevel())) {
				nextMatches.add(pre);
			}
			pre = match.getToTeam2Match(true);
			if(pre != null && level.equals(pre.getLevel())) {
				nextMatches.add(pre);
			}
			// rank the current teams and move on to the next group of matches
			if(currentMatches.isEmpty()) {
				// don't bother with tie breaks in this format
				addWinners(currentTeams);
				currentTeams.clear();
				currentMatches = nextMatches;
				nextMatches = new ArrayList<Match>();
			}
		}
	}
}
