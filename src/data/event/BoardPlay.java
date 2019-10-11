/*  This is an experimental event type for Club Board Play added 23/4/19 bvw */
package data.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.event.result.EventResult;
import data.event.result.SingleEliminationEventResult;
import data.match.Match;
import data.team.Team;

@CreatableEvent(displayName = "Board Play")
public class BoardPlay extends Event {
	private static final long serialVersionUID = 6291742635954664185L;
	private List<Match> matches;
	
	public BoardPlay(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
		matches = new ArrayList<Match>();
		if(numberOfTeams < 2 ) {
			throw new IllegalArgumentException("The number of teams for this event must be at least 2.");
		}
	}

	protected List<Match> startEvent() {
		String level = getDisplayLevels().get(0);
		List<Team> teams = getTeams();
                matches.addAll(EventUtils.createSingleLevelBoard(teams.size() / 2, this, level));
		EventUtils.setTeamsInMatches(teams, matches);
		return getMatches(level);
	}
	
	protected void undoEvent() {
		matches.clear();
	}
	
	protected List<String> generateDisplayLevels(List<String> levels) {
		List<String> displayLevels = new ArrayList<String>();
		String displayLevel  = generateSingleDisplayLevelString();
		displayLevels.add(displayLevel);
		return displayLevels;
	}

	public EventResult getWinners(String level) {
		if(!getDisplayLevels().contains(level)) {
			return null;
		}
		return new SingleEliminationEventResult(level, getMatches(level));
	}

	public List<Match> getMatches(String level) {
		if(!getDisplayLevels().contains(level)) {
			return null;
		}
		return Collections.unmodifiableList(matches);
	}
}
