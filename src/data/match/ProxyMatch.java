package data.match;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import data.event.EventUtils;
import data.team.Team;

public class ProxyMatch implements Serializable {
	private static final long serialVersionUID = -1900897681476874262L;
	public static final String DEFAULT_T1_ID = "DEFAULT_T1";
	public static final String DEFAULT_T2_ID = "DEFAULT_T2";
	private static final Comparator<Match> comp = new MatchComparator();
	private Map<String, Set<Match>> feederMatches;
	private Match match, actualT1, actualT2;
	
	// these fields are here to maintain backwards compatibility
	private static final String DEFAULT_ID = "DEFAULT";
	private Map<String, List<List<Match>>> toT1Matches, toT2Matches;
	private Map<String, List<List<Match>>> toMatches;
	
	public ProxyMatch(Match match) {
		if(match == null) {
			throw new RuntimeException("match cannot be null");
		}
		this.match = match;
		feederMatches = new HashMap<String, Set<Match>>();
	}
	
	public void addMatch(String id, Match match) {
		if(DEFAULT_T1_ID.equals(id) || DEFAULT_T2_ID.equals(id)) {
			throw new RuntimeException("id can't be equal to the default id");
		}
		Set<Match> set = feederMatches.get(id);
		if(set == null) {
			set = new TreeSet<Match>(comp);
			feederMatches.put(id, set);
		}
		set.add(match);
	}
	
	public void resetFromMatch(Match match) {
		// because we allow undos, you have to account for future matches sending teams to this one
		if(EventUtils.isFeederMatch(match, actualT1)) {
			actualT1 = null;
			this.match.setTeam1(null);
		}
		else if(EventUtils.isFeederMatch(match, actualT2)) {
			actualT2 = null;
			this.match.setTeam2(null);
		}
	}
	
	public boolean forwardTeam(Match match, Team team, boolean forwardTeam) {
		if(this.match.isComplete() && !match.isRecalculating()) {
			return false;
		}
		List<Match> matches = getPotentialFeederMatches();
		Match defaultMatch = getToTeam1Match(true);
		if(defaultMatch == null || defaultMatch.isComplete() || match.equals(defaultMatch)) {
			Match potential = null;
			if(!match.equals(defaultMatch) && matches.size() > 0) {
				potential = matches.get(0);
			}
			if((potential == null || comp.compare(match, potential) <= 0) && setFromTeam1Match(match, team, forwardTeam)) {
				return true;
			}
		}
		defaultMatch = getToTeam2Match(true);
		if(defaultMatch == null || defaultMatch.isComplete() || match.equals(defaultMatch)) {
			Match potential = null;
			if(!match.equals(defaultMatch)) {
				if(matches.size() > 1) {
					potential = matches.get(1);
				}
				else if(matches.size() > 0) {
					potential = matches.get(0);
				}
			}
			if((potential == null || comp.compare(match, potential) <= 0) && setFromTeam2Match(match, team, forwardTeam)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean setFromTeam1Match(Match match, Team team, boolean setMatch) {
		if(actualT1 == null && setFromTeamXMatch(match, DEFAULT_T1_ID)) {
			if(setMatch) {
				actualT1 = match;
				this.match.setTeam1(team);
			}
			else {
				actualT1 = null;
				this.match.setTeam1(null);
			}
			return true;
		}
		return false;
	}
	
	public boolean setFromTeam2Match(Match match, Team team, boolean setMatch) {
		if(actualT2 == null && setFromTeamXMatch(match, DEFAULT_T2_ID)) {
			if(setMatch) {
				actualT2 = match;
				this.match.setTeam2(team);
			}
			else {
				actualT2 = null;
				this.match.setTeam2(null);
			}
			return true;
		}
		return false;
	}
	
	private boolean setFromTeamXMatch(Match match, String default_id) {
		if(match == null) {
			return false;
		}
		// check the default match
		if(feederMatches.containsKey(default_id) && feederMatches.get(default_id).contains(match)) {
			return true;
		}
		// check the list of potential matches
		for(String id : feederMatches.keySet()) {
			if(DEFAULT_T1_ID.equals(id) || DEFAULT_T2_ID.equals(id)) {
				continue;
			}
			if(feederMatches.get(id).contains(match)) {
				return true;
			}
		}
		return false;
	}
	
	public void setToTeamXMatch(Match toTXMatch, boolean setToT1) {
		if(setToT1) {
			setToTeam1Match(toTXMatch);
		}
		else {
			setToTeam2Match(toTXMatch);
		}
	}
	
	public void setToTeam1Match(Match toT1Match) {
		if(toT1Match == null) {
			feederMatches.remove(DEFAULT_T1_ID);
		}
		else {
			TreeSet<Match> set = new TreeSet<Match>(comp);
			set.add(toT1Match);
			feederMatches.put(DEFAULT_T1_ID, set);
		}
	}
	
	public Match getDefaultMatch(boolean toT1Match) {
		Set<Match> defaultMatch = feederMatches.get(toT1Match ? DEFAULT_T1_ID : DEFAULT_T2_ID);
		if(defaultMatch != null) {
			return defaultMatch.iterator().next();
		}
		return null;
	}
	
	public Match getToTeam1Match(boolean ignorePotentialMatches) {
		if(actualT1 != null) {
			return actualT1;
		}
		Match match = getDefaultMatch(true);
		// default to the winner match
		if(ignorePotentialMatches || (!ignorePotentialMatches && match != null && !match.isComplete())) {
			return match;
		}
		// go through the other matches and look for an unfinished one
		List<Match> matches = getPotentialFeederMatches();
		if(matches.size() > 0) {
			return matches.get(0);
		}
		return match;
	}
	
	public void setToTeam2Match(Match toT2Match) {
		if(toT2Match == null) {
			feederMatches.remove(DEFAULT_T2_ID);
		}
		else {
			TreeSet<Match> set = new TreeSet<Match>(comp);
			set.add(toT2Match);
			feederMatches.put(DEFAULT_T2_ID, set);
		}
	}
	
	public Match getToTeam2Match(boolean ignorePotentialMatches) {
		if(actualT2 != null) {
			return actualT2;
		}
		Match match = getDefaultMatch(false);
		// default to the winner match
		if(ignorePotentialMatches || (!ignorePotentialMatches && match != null && !match.isComplete())) {
			return match;
		}
		// go through the other matches and look for an unfinished one
		List<Match> matches = getPotentialFeederMatches();
		if(getToTeam1Match(true) != null && matches.size() > 0) {
			return matches.get(0);
		}
		if(matches.size() > 1) {
			return matches.get(1);
		}
		return match;
	}
	
	private List<Match> getPotentialFeederMatches() {
		LinkedHashSet<Match> set = new LinkedHashSet<Match>();
		for(String id : feederMatches.keySet()) {
			if(DEFAULT_T1_ID.equals(id) || DEFAULT_T2_ID.equals(id)) {
				continue;
			}
			// grab all the matches that can potentially send a team to this one
			for(Match match : feederMatches.get(id)) {
				if(match.isComplete()) {
					continue;
				}
				Map<String, List<Match>> map = match.getLoserMatches();
				Map<Team, WinLossPair> teams = calculateWinLossPair(this.match, match, getPotentialFeederTeams(match));
				for(Team team : teams.keySet()) {
					WinLossPair pair = teams.get(team);
					boolean addedMatch = false;
					for(int i = pair.wins; !addedMatch && i >= 0; --i) {
						// stop looking at teams as soon as we have determined this is a potential match
						List<Match> loserMatches = map.get(match.getLoserMatchId(team, i, pair.losses));
						if(loserMatches == null) {
							continue;
						}
						for(Match loserMatch : loserMatches) {
							if(this.match.equals(loserMatch)) {
								set.add(match);
								addedMatch = true;
								break;
							}
						}
					}
					if(addedMatch) {
						break;
					}
				}
			}
		}
		// attempt to sort the potential matches by match level and then by match index
		ArrayList<Match> matches = new ArrayList<Match>(set);
		Collections.sort(matches, comp);
		return matches;
	}
	
	private Map<Team, WinLossPair> getPotentialFeederTeams(Match match) {
		HashMap<Team, WinLossPair> map = new HashMap<Team, WinLossPair>();
		if(match == null || match.isComplete()) {
			return map;
		}
		// check to see if we have teams
		Team t1 = match.getTeam1();
		Team t2 = match.getTeam2();
		if(t1 != null && t2 != null) {
			map.put(t1, new WinLossPair(0, 0, false));
			map.put(t2, new WinLossPair(0, 0, false));
			return map;
		}
		// we don't have both teams, so time to check the feeder matches
		Match m1 = match.getToTeam1Match(false);
		Match m2 = match.getToTeam2Match(false);
		Map<Team, WinLossPair> m1Map = getPotentialFeederTeams(m1);
		Map<Team, WinLossPair> m2Map = getPotentialFeederTeams(m2);
		// check to see if we have one team
		if(t1 != null) {
			map.put(t1, new WinLossPair(0, 0, m2Map.isEmpty()));
			map.putAll(calculateWinLossPair(match, m2, m2Map));
			return map;
		}
		if(t2 != null) {
			map.put(t2, new WinLossPair(0, 0, m1Map.isEmpty()));
			map.putAll(calculateWinLossPair(match, m1, m1Map));
			return map;
		}
		// we don't have any set teams, check the potential teams from feeder matches
		if(m1Map.isEmpty() && m2Map.isEmpty()) {
			return map;
		}
		if(!m1Map.isEmpty() && !m2Map.isEmpty()) {
			map.putAll(calculateWinLossPair(match, m1, m1Map));
			map.putAll(calculateWinLossPair(match, m2, m2Map));
			return map;
		}
		Map<Team, WinLossPair> potentialTeams = m1Map.isEmpty() ? calculateWinLossPair(match, m2, m2Map) : calculateWinLossPair(match, m1, m1Map);
		for(Team key : potentialTeams.keySet()) {
			WinLossPair value = potentialTeams.get(key);
			value.isBye = true;
			map.put(key, value);
		}
		return map;
	}
	
	private Map<Team, WinLossPair> calculateWinLossPair(Match toMatch, Match fromMatch, Map<Team, WinLossPair> potentialTeams) {
		HashMap<Team, WinLossPair> map = new HashMap<Team, WinLossPair>();
		if(toMatch == null || fromMatch == null || potentialTeams == null || potentialTeams.isEmpty()) {
			return map;
		}
		boolean wonMatch = toMatch.equals(fromMatch.getWinnerMatch());
		for(Team key : potentialTeams.keySet()) {
			WinLossPair value = potentialTeams.get(key);
			if(value.isBye) {
				value.isBye = false;
				// only winner matches can get teams from a bye
				if(wonMatch) {
					map.put(key, value);
				}
			}
			// not a bye, just increment this as usual
			else if(wonMatch) {
				++value.wins;
				map.put(key, value);
			}
			else {
				++value.losses;
				map.put(key, value);
			}
		}
		return map;
	}
	
	// this is to maintain backwards compatibility
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		boolean newObject = true;
		// check for older objects to convert
		if(toT1Matches != null && toT2Matches != null) {
			feederMatches = new HashMap<String, Set<Match>>();
			TreeSet<Match> set = new TreeSet<Match>(comp);
			if(toT1Matches.containsKey(DEFAULT_ID)) {
				for(List<Match> list : toT1Matches.get(DEFAULT_ID)) {
					set.addAll(list);
				}
				feederMatches.put(DEFAULT_T1_ID, set);
				toT1Matches.remove(DEFAULT_ID);
			}
			for(String key : toT1Matches.keySet()) {
				set = new TreeSet<Match>(comp);
				for(List<Match> list : toT1Matches.get(key)) {
					set.addAll(list);
				}
				feederMatches.put(key, set);
			}
			set = new TreeSet<Match>(comp);
			if(toT2Matches.containsKey(DEFAULT_ID)) {
				for(List<Match> list : toT2Matches.get(DEFAULT_ID)) {
					set.addAll(list);
				}
				feederMatches.put(DEFAULT_T2_ID, set);
				toT2Matches.remove(DEFAULT_ID);
			}
			for(String key : toT2Matches.keySet()) {
				set = new TreeSet<Match>(comp);
				for(List<Match> list : toT2Matches.get(key)) {
					set.addAll(list);
				}
				feederMatches.put(key, set);
			}
			// reset these fields so we don't have duplicate data
			toT1Matches = null;
			toT2Matches = null;
			newObject = false;
		}
		if(toMatches != null) {
			feederMatches = new HashMap<String, Set<Match>>();
			for(String key : toMatches.keySet()) {
				TreeSet<Match> set = new TreeSet<Match>(comp);
				for(List<Match> list : toMatches.get(key)) {
					set.addAll(list);
				}
				feederMatches.put(key, set);
			}
			// reset this field so we don't have duplicate data
			toMatches = null;
			newObject = false;
		}
		// recreate every tree set in case the comparator changed
		if(newObject) {
			Map<String, Set<Match>> temp = feederMatches;
			feederMatches = new HashMap<String, Set<Match>>();
			for(String key : temp.keySet()) {
				TreeSet<Match> value = new TreeSet<Match>(comp);
				value.addAll(temp.get(key));
				feederMatches.put(key, value);
			}
		}
	}
	
	private class WinLossPair {
		boolean isBye;
		int wins, losses;
		
		public WinLossPair(int wins, int losses, boolean isBye) {
			this.wins = wins;
			this.losses = losses;
			this.isBye = isBye;
		}
	}
	
	private static class MatchComparator implements Comparator<Match>, Serializable {
		private static final long serialVersionUID = -6875049031117497927L;

		public int compare(Match m1, Match m2) {
			int comp = m2.getMatchLevel() - m1.getMatchLevel();
			if(comp == 0) {
				return m1.getIndex() - m2.getIndex();
			}
			return comp;
		}
	}
}
