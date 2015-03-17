package data.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import data.event.painter.EventPainter;
import data.event.painter.RoundRobinEventPainter;
import data.event.result.EventResult;
import data.event.result.RoundRobinEventResult;
import data.match.Match;
import data.match.RoundRobinMatch;
import data.team.Team;

@CreatableEvent(displayName = "Round Robin")
public class RoundRobinEvent extends Event {
	private static final long serialVersionUID = -1746797107601759992L;
	private List<Match> matches;
	
	public RoundRobinEvent(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
		if(numberOfTeams < 2) {
			throw new IllegalArgumentException("The number of teams for this event must be at least 2.");
		}
		matches = new ArrayList<Match>();
	}

	protected List<Match> startEvent() {
		String level = getDisplayLevels().get(0);
		List<Team> teams = getTeams();
		for(int i = 0; i < teams.size(); ++i) {
			for(int j = i + 1; j < teams.size(); ++j) {
				Match match = createMatch(teams.get(i), teams.get(j), level);
				matches.add(match);
			}
		}
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
		return new RoundRobinEventResult(level, getMatches(level));
	}
	
	public List<Match> getMatches(String level) {
		if(!getDisplayLevels().contains(level)) {
			return null;
		}
		return Collections.unmodifiableList(matches);
	}
	
	public Match generateMatch(int minScore, int maxScore, int winBy, int bestOf) {
		return new RoundRobinMatch(minScore, maxScore, winBy, bestOf);
	}
	
	public EventPainter getEventPainter(String level) {
		return new RoundRobinEventPainter(this, level);
	}
}
