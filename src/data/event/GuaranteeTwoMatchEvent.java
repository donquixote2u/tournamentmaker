package data.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.event.result.EventResult;
import data.event.result.SingleEliminationEventResult;
import data.match.Match;
import data.team.Team;

@CreatableEvent(displayName = "Guarantee Two Match")
public class GuaranteeTwoMatchEvent extends Event {
	private static final long serialVersionUID = 2422391775101120062L;
	private Map<String, List<Match>> matches;
	
	public GuaranteeTwoMatchEvent(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
		matches = new HashMap<String, List<Match>>();
		double log2 = Math.log(numberOfTeams) / Math.log(2.0);
		if(numberOfTeams < 4 || log2 > Math.floor(log2)) {
			throw new IllegalArgumentException("The number of teams for this event must be at least 4 and a power of 2.");
		}
	}

	protected List<Match> startEvent() {
		List<Team> teams = getTeams();
		List<String> levels = getDisplayLevels();
		matches.put(levels.get(0), EventUtils.createSingleEliminationBracket(getNumberOfTeams() / 2, this, levels.get(0)));
		matches.put(levels.get(1), EventUtils.createSingleEliminationBracket(getNumberOfTeams() / 4, this, levels.get(1)));
		// 1st level drops to 2nd level on first match
		EventUtils.setDropDowns(matches.get(levels.get(0)), matches.get(levels.get(1)), 0);
		EventUtils.setTeamsInMatches(teams, matches.get(levels.get(0)));
		return getMatches(levels.get(0));
	}
	
	protected void undoEvent() {
		matches.clear();
	}
	
	protected List<String> generateDisplayLevels(List<String> levels) {
		List<String> displayLevels = new ArrayList<String>();
		switch(levels.size()) {
			case 0:
				displayLevels.add("1");
				displayLevels.add("2");
				break;
			case 1:
				displayLevels.add(levels.get(0));
				displayLevels.add(levels.get(0) + " Cons");
				break;
			default:
				displayLevels.add(levels.get(0));
				displayLevels.add(levels.get(1));
				break;
		}
		return displayLevels;
	}

	public EventResult getWinners(String level) {
		List<Match> matches = getMatches(level);
		if(matches == null) {
			return null;
		}
		return new SingleEliminationEventResult(level, getMatches(level));
	}

	public List<Match> getMatches(String level) {
		List<Match> list = matches.get(level);
		if(list == null) {
			return null;
		}
		return Collections.unmodifiableList(list);
	}
}
