package data.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.event.painter.EventPainter;
import data.event.painter.SingleEliminationEventPainter;
import data.event.result.EventResult;
import data.event.result.SingleEliminationEventResult;
import data.match.Match;
import data.team.Team;

@CreatableEvent(displayName = "Single Elimination")
public class SingleEliminationEvent extends Event {
	private static final long serialVersionUID = 6291742635954664185L;
	private List<Match> matches;
	
	public SingleEliminationEvent(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
		matches = new ArrayList<Match>();
		double log2 = Math.log(numberOfTeams) / Math.log(2.0);
		if(numberOfTeams < 2 || log2 > Math.floor(log2)) {
			throw new IllegalArgumentException("The number of teams for this event must be at least 2 and a power of 2.");
		}
	}

	protected List<Match> startEvent() {
		String level = getDisplayLevels().get(0);
		List<Team> teams = getTeams();
		matches.addAll(EventUtils.createSingleEliminationBracket(teams.size() / 2, this, level));
		EventUtils.setTeamsInMatches(teams, matches);
		return getMatches(level);
	}
	
	protected void undoEvent() {
		matches.clear();
	}
	
	protected List<String> generateDisplayLevels(List<String> levels) {
		List<String> displayLevels = new ArrayList<String>();
		String displayLevel  = "";
		for(String level : levels) {
			displayLevel += level + ", ";
		}
		if(displayLevel.isEmpty()) {
			displayLevel = "All";
		}
		else {
			displayLevel = displayLevel.substring(0, displayLevel.length() - 2);
		}
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
	
	public EventPainter getEventPainter(String level) {
		return new SingleEliminationEventPainter(this, level);
	}
}
