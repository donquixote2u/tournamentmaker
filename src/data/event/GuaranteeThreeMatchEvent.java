package data.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.event.result.EventResult;
import data.event.result.SingleEliminationEventResult;
import data.match.Match;
import data.team.Team;

@CreatableEvent(displayName = "Guarantee Three Match")
public class GuaranteeThreeMatchEvent extends Event {
	private static final long serialVersionUID = -6807263840130466051L;
	private Map<String, List<Match>> matches;
	
	public GuaranteeThreeMatchEvent(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
		matches = new HashMap<String, List<Match>>();
		double log2 = Math.log(numberOfTeams) / Math.log(2.0);
		if(numberOfTeams < 8 || log2 > Math.floor(log2)) {
			throw new IllegalArgumentException("The number of teams for this event must be at least 8 and a power of 2.");
		}
	}

	protected List<Match> startEvent() {
		List<Team> teams = getTeams();
		List<String> levels = getDisplayLevels();
		matches.put(levels.get(0), EventUtils.createSingleEliminationBracket(getNumberOfTeams() / 2, this, levels.get(0)));
		matches.put(levels.get(1), EventUtils.createSingleEliminationBracket(getNumberOfTeams() / 4, this, levels.get(1)));
		matches.put(levels.get(2), EventUtils.createSingleEliminationBracket(getNumberOfTeams() / 4, this, levels.get(2)));
		matches.put(levels.get(3), EventUtils.createSingleEliminationBracket(getNumberOfTeams() / 8, this, levels.get(3)));
		// 1st level drops to 3rd level on first match
		EventUtils.setDropDowns(matches.get(levels.get(0)), matches.get(levels.get(2)), 0);
		// 1st level drops to 2nd level on second match
		setSecondRoundLossDropDowns();
		// 3rd level drops to 4th level on first match
		EventUtils.setDropDowns(matches.get(levels.get(2)), matches.get(levels.get(3)), 0);
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
				displayLevels.add("3");
				displayLevels.add("4");
				break;
			case 1:
				displayLevels.add(levels.get(0));
				displayLevels.add(levels.get(0) + " Cons");
				displayLevels.add("3");
				displayLevels.add("4");
				break;
			case 2:
				displayLevels.add(levels.get(0));
				displayLevels.add(levels.get(0) + " Cons");
				displayLevels.add(levels.get(1));
				displayLevels.add(levels.get(1) + " Cons");
				break;
			case 3:
				displayLevels.add(levels.get(0));
				displayLevels.add(levels.get(1));
				displayLevels.add(levels.get(2));
				displayLevels.add(levels.get(2) + " Cons");
				break;
			default:
				displayLevels.add(levels.get(0));
				displayLevels.add(levels.get(1));
				displayLevels.add(levels.get(2));
				displayLevels.add(levels.get(3));
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
	
	private void setSecondRoundLossDropDowns() {
		String id = String.valueOf(1);
		List<String> levels = getDisplayLevels();
		List<Match> drops = matches.get(levels.get(1));
		// calculate the expected drops
		boolean inc = false;
		List<Match> originalDrops = new ArrayList<Match>();
		for(int i = 1; i < drops.size(); i += 4) {
			originalDrops.add(drops.get(i));
			if(drops.size() > i + 1) {
				originalDrops.add(drops.get(i + 1));
			}
		}
		List<Match> currentMatches = EventUtils.getWinnerMatches(matches.get(levels.get(0)));
		List<Match> currentDrops = new ArrayList<Match>(originalDrops);
		while(!currentMatches.isEmpty()) {
			for(int i = 0; i < currentMatches.size(); ++i) {
				Collection<Match> initialMatches = EventUtils.getInitialMatches(currentDrops.get(inc ? i : i / 2));
				initialMatches.retainAll(originalDrops);
				for(Match drop : initialMatches) {
					currentMatches.get(i).addLoserMatch(id, drop);
				}
			}
			currentMatches = EventUtils.getWinnerMatches(currentMatches);
			if(inc) {
				currentDrops = EventUtils.getWinnerMatches(currentDrops);
			}
			inc = true;
		}
		// calculate the unexpected drops
		originalDrops = new ArrayList<Match>();
		originalDrops.add(drops.get(0));
		for(int i = 3; i < drops.size(); i += 4) {
			originalDrops.add(drops.get(i));
			if(drops.size() > i + 1) {
				originalDrops.add(drops.get(i + 1));
			}
		}
		for(Match drop : originalDrops) {
			drop.setIgnoreMatch(true);
		}
		currentMatches = EventUtils.getWinnerMatches(EventUtils.getWinnerMatches(matches.get(levels.get(0))));
		currentDrops = EventUtils.getWinnerMatches(originalDrops);
		while(!currentMatches.isEmpty()) {
			int div = currentDrops.size() / currentMatches.size();
			for(int i = 0; i < currentMatches.size(); ++i) {
				for(int j = div * i; j < div * (i + 1); ++j) {
					currentMatches.get(i).addLoserMatch(id, currentDrops.get(j));
				}
			}
			currentMatches = EventUtils.getWinnerMatches(currentMatches);
		}
	}
}
