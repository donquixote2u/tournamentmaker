package data.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.match.Match;
import data.team.Team;

public class EventUtils {
	public static final String FINALS = "F";
	public static final String SEMI_FINALS = "SF";
	public static final String QUARTER_FINALS = "QF";
	
	/**
	 * Gets all matches in a level. Ignores matches with ignoreMatch set to true.
	 * @param matches
	 */
	public static Set<Match> getAllMatches(List<Match> matches) {
		return getAllMatches(matches, true);
	}
	
	/**
	 * Gets all matches in a level.
	 * @param matches
	 */
	public static Set<Match> getAllMatches(List<Match> matches, boolean honorIgnoreMatch) {
		Set<Match> allMatches = new HashSet<Match>();
		for(Match match : matches) {
			// get to the final match and work backwards to grab all the matches
			String level = match.getLevel();
			while(match.getWinnerMatch() != null && !allMatches.contains(match.getWinnerMatch()) && level.equals(match.getWinnerMatch().getLevel())) {
				match = match.getWinnerMatch();
			}
			ArrayList<Match> curMatches = new ArrayList<Match>();
			curMatches.add(match);
			while(!curMatches.isEmpty()) {
				Match cur = curMatches.remove(0);
				if(cur == null || (honorIgnoreMatch && cur.getIgnoreMatch())) {
					continue;
				}
				if(level.equals(cur.getLevel()) && allMatches.add(cur)) {
					curMatches.add(cur.getDefaultToTeam1Match());
					curMatches.add(cur.getDefaultToTeam2Match());
				}
			}
		}
		return allMatches;
	}
	
	/**
	 * Returns true if the given match can be reached from the starting match by traversing the winning matches.
	 * @param startingMatch
	 * @param match
	 */
	public static boolean isFeederMatch(Match startingMatch, Match match) {
		while(startingMatch != null && !startingMatch.equals(match)) {
			startingMatch = startingMatch.getWinnerMatch();
		}
		return startingMatch != null;
	}
	
	/**
	 * Returns the list of winner matches.
	 * @param matches
	 */
	public static List<Match> getWinnerMatches(List<Match> matches) {
		ArrayList<Match> winners = new ArrayList<Match>();
		for(Match match : matches) {
			if(match == null) {
				continue;
			}
			Match winner = match.getWinnerMatch();
			if(winner == null || winners.contains(winner)) {
				continue;
			}
			winners.add(winner);
		}
		return winners;
	}
	
	/**
	 * Returns the list of matches that feed into these matches.
	 * @param matches
	 * @return
	 */
	public static List<Match> getDefaultFeederMatches(List<Match> matches) {
		ArrayList<Match> feeders = new ArrayList<Match>();
		for(Match match : matches) {
			if(match == null) {
				continue;
			}
			Match pre = match.getDefaultToTeam1Match();
			if(pre != null) {
				feeders.add(pre);
			}
			pre = match.getDefaultToTeam2Match();
			if(pre != null) {
				feeders.add(pre);
			}
		}
		return feeders;
	}
	
	/**
	 * Returns all the starting matches that feed into this one. Ignores matches that feed into this one by dropping into it. 
	 * @param end
	 */
	public static Collection<Match> getInitialMatches(Match end) {
		HashSet<Match> dependents = new HashSet<Match>();
		if(end == null) {
			return dependents;
		}
		Match m1 = end.getDefaultToTeam1Match();
		Match m2 = end.getDefaultToTeam2Match();
		if(m1 == null && m2 == null) {
			dependents.add(end);
			return dependents;
		}
		if(m1 != null) {
			dependents.addAll(getInitialMatches(m1));
		}
		if(m2 != null) {
			dependents.addAll(getInitialMatches(m2));
		}
		return dependents;
	}
	
	/**
	 * Creates a bracket and returns the starting matches. The list of matches will be greater than or equal to the numberOfMatches.
	 * @param numberOfMatches
	 * @param event
	 * @param level
	 */
	public static List<Match> createSingleEliminationBracket(int numberOfMatches, Event event, String level) {
		// checking for illegal values
		if(numberOfMatches < 1) {
			throw new RuntimeException("illegal number of matches");
		}
		List<Match> matches = new ArrayList<Match>();
		Match finals = event.createMatch(level);
		finals.setMatchLevel(1);
		finals.setMatchDescription(FINALS);
		matches.add(finals);
		int index = 1;
		while(matches.size() < numberOfMatches) {
			List<Match> currentMatches = matches;
			matches = new ArrayList<Match>();
			// for each match, create the 2 that feed into it
			for(Match match : currentMatches) {
				Match m1 = event.createMatch(level);
				Match m2 = event.createMatch(level);
				m1.setMatchLevel(index + 1);
				m2.setMatchLevel(index + 1);
				// check to see if we need to set a description
				if(index == 1) {
					m1.setMatchDescription(SEMI_FINALS);
					m2.setMatchDescription(SEMI_FINALS);
				}
				if(index == 2) {
					m1.setMatchDescription(QUARTER_FINALS);
					m2.setMatchDescription(QUARTER_FINALS);
				}
				// set up the connections and add the new matches to the list
				m1.setWinnerMatch(match, true);
				m2.setWinnerMatch(match, false);
				m1.setMirrorMatch(m2);
				matches.add(m1);
				matches.add(m2);
			}
			++index;
		}
		return matches;
	}
	
	/**
	 * Sets the loser matches for a bracket (only guaranteed to work for brackets created using the createSingleEliminationBracket function).
	 * @param matches
	 * @param drops
	 */
	public static void setDropDowns(List<Match> matches, List<Match> drops, Object id) {
		if(matches == null || drops == null || matches.size() != drops.size() * 2) {
			return;
		}
		boolean inc = false;
		String loserId = String.valueOf(id);
		List<Match> currentMatches = new ArrayList<Match>(matches);
		List<Match> currentDrops = new ArrayList<Match>(drops);
		while(!currentMatches.isEmpty()) {
			for(int i = 0; i < currentMatches.size(); ++i) {
				for(Match drop : getInitialMatches(currentDrops.get(inc ? i : i / 2))) {
					currentMatches.get(i).addLoserMatch(loserId, drop);
				}
			}
			currentMatches = getWinnerMatches(currentMatches);
			if(inc) {
				currentDrops = getWinnerMatches(currentDrops);
			}
			inc = true;
		}
	}
	
	/**
	 * Takes a list of teams and sets them into the list of matches. There has to be twice as many teams as there are matches. Null team equals a bye.
	 * @param teams
	 * @param matches
	 */
	public static void setTeamsInMatches(List<Team> teams, List<Match> matches) {
		if(teams == null || matches == null || teams.size() % 2 != 0 || teams.size() / 2 != matches.size()) {
                        System.out.println("teams:"+teams.size());
                        System.out.println("matches:"+matches.size());
			throw new RuntimeException("invalid parameters for team and/or matches");
		}
		for(int i = 0; i < teams.size(); ++i) {
			matches.get(i / 2).setTeam(teams.get(i), i % 2 == 0);
		}
	}
	
	/**
	 * Returns true if the event is paused.
	 * @param event
	 */
	public static boolean eventIsPaused(Event event) {
		if(event.getPausedLevels() == null) {
			return false;
		}
		for(String level : event.getDisplayLevels()) {
			if(event.getPausedLevels().contains(level)) {
				for(Match match : getAllMatches(event.getMatches(level))) {
					if(event.getPausedMatchLevel() < match.getMatchLevel() && match.canStartMatch()) {
						return false;
					}
				}
			}
			else {
				for(Match match : getAllMatches(event.getMatches(level))) {
					if(match.canStartMatch()) {
						return false;
					}
				}
			}
		}
		return true;
	}
public static List<Match> createSingleLevelBoard(int numberOfMatches, Event event, String level) {
		// checking for illegal values
		if(numberOfMatches < 1) {
			throw new RuntimeException("illegal number of matches");
		}
		List<Match> matches = new ArrayList<Match>();
		while(matches.size() < numberOfMatches) {
                    Match m1 = event.createMatch(level);
                    m1.setMatchLevel(1);
                    // m1.setWinnerMatch(m1, true);
                    matches.add(m1);
		}
		return matches;
	}
}
