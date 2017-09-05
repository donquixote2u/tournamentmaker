package data.tournament;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.event.Event;
import data.match.Match;
import data.player.Player;
import data.team.Team;

public class TournamentUtils {
	private static int matchIndex;
	
	public static List<Match> sortMatches(Tournament tournament, Collection<Match> matches) {
		// only add the matches we want to sort
		ArrayList<Match> hasOrder = new ArrayList<Match>();
		ArrayList<Match> hasTime = new ArrayList<Match>();
		ArrayList<Match> hasForfeit = new ArrayList<Match>();
		ArrayList<Match> matchesToSort = new ArrayList<Match>();
		ArrayList<Match> futureMatches = new ArrayList<Match>();
		ArrayList<Match> notScheduled = new ArrayList<Match>();
		for(Match match : matches) {
			// don't sort completed or started matches
			if(match.isComplete() || match.getStart() != null) {
				continue;
			}
			// don't sort ordered matches
			if(match.getNextAvailableCourtOrder() > 0) {
				hasOrder.add(match);
				continue;
			}
			// don't sort scheduled matches
			if(match.getRequestedDate() != null) {
				hasTime.add(match);
				continue;
			}
			// don't sort paused matches
			Event event = match.getEvent();
			if(event.getPausedLevels() != null && event.getPausedLevels().contains(match.getLevel()) && event.getPausedMatchLevel() >= match.getMatchLevel()) {
				continue;
			}
			// keep track of matches with forfeits
			if(match.getT1ForfeitOnStart() || match.getT2ForfeitOnStart()) {
				hasForfeit.add(match);
			}
			// if we are disabling the scheduler, we want to sort the matches by index
			if(tournament.getDisableScheduler()) {
				notScheduled.add(match);
				continue;
			}
			// don't sort matches that are missing both teams
			if(match.getTeam1() == null && match.getTeam2() == null) {
				if(tournament.getShowAllMatches()) {
					futureMatches.add(match);
				}
				continue;
			}
			matchesToSort.add(match);
		}
		// sort the matches with court order by court order
		Collections.sort(hasOrder, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m1.getNextAvailableCourtOrder() - m2.getNextAvailableCourtOrder();
			}
		});
		// apply an initial sort by name so we will always get the same results each time
		Collections.sort(matchesToSort, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m1.toString().compareTo(m2.toString());
			}
		});
		// get a mapping of players to team mates and the number of players each match impacts
		final CurrentMatchQueue current = new CurrentMatchQueue(tournament);
		final HashMap<Match, Double> matchImpact = new HashMap<Match, Double>();
		final HashMap<Player, Integer> playerMatchCount = new HashMap<Player, Integer>();
		for(Match match : matchesToSort) {
			// update the player match count
			for(Player player : match.getPlayers()) {
				Integer count = playerMatchCount.get(player);
				count = count == null ? Integer.valueOf(1) : Integer.valueOf(count.intValue() + 1);
				playerMatchCount.put(player, count);
			}
			// update the impact of the feeder matches
			if(match.getTeam1() == null) {
				updateFeederMatchImpact(match.getToTeam1Match(false), match.getTeam2().getPlayers().size() * 2, 0, matchImpact);
			}
			if(match.getTeam2() == null) {
				updateFeederMatchImpact(match.getToTeam2Match(false), match.getTeam1().getPlayers().size() * 2, 0, matchImpact);
			}
			// calculate the number of other players this match impacts
			Double currentImpact = matchImpact.get(match);
			double impact = currentImpact != null ? currentImpact.doubleValue() : 0;
			if(match.getTeam1() != null && match.getTeam2() != null && match.getMirrorMatch() != null && match.getMirrorMatch().getStart() != null && !match.getMirrorMatch().isComplete()) {
				impact += match.getMirrorMatch().getPlayers().size() / 2.0;
			}
			matchImpact.put(match, Double.valueOf(impact));
		}
		// calculate the player to match impact and then sort everything
		final HashMap<Match, Double> playerMatchImpact = new HashMap<Match, Double>();
		for(Match match: matchesToSort) {
			double playerImpact = 0;
			for(Player player : match.getPlayers()) {
				playerImpact += playerMatchCount.get(player).intValue();
			}
			playerImpact /= match.getPlayers().size();
			playerMatchImpact.put(match, playerImpact);
		}
		Collections.sort(matchesToSort, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				int compare = playerMatchImpact.get(m2).compareTo(playerMatchImpact.get(m1));
				if(compare != 0) {
					return compare;
				}
				return matchImpact.get(m2).compareTo(matchImpact.get(m1));
			}
		});
		// this comparator sorts the matches by if the match can start, then by event progress, and then the last played time
		final Comparator<Match> matchComp = new Comparator<Match>() {
			private HashMap<String, EventInfo> eventMap = new HashMap<String, EventInfo>();
			
			private Comparator<Player> lastPlayedComp = new Comparator<Player>() {
				public int compare(Player p1, Player p2) {
					if(p1 == null && p2 == null) {
						return 0;
					}
					if(p1 == null) {
						return 1;
					}
					if(p2 == null) {
						return -1;
					}
					Date d1 = current.getlastPlayedDate(p1);
					Date d2 = current.getlastPlayedDate(p2);
					if(d1 == null && d2 == null) {
						return 0;
					}
					if(d1 == null) {
						return -1;
					}
					if(d2 == null) {
						return 1;
					}
					return d1.compareTo(d2);
				}
			};
			
			public int compare(Match m1, Match m2) {
				// waiting matches
				boolean m1Waiting = m1.getTeam1() == null || m1.getTeam2() == null;
				boolean m2Waiting = m2.getTeam1() == null || m2.getTeam2() == null;
				if(m1Waiting != m2Waiting) {
					return m1Waiting ? 1 : -1;
				}
				// compare the percent complete for a level in an event
				String id1 = m1.getEvent().getName() + " - " + m1.getLevel();
				String id2 = m2.getEvent().getName() + " - " + m2.getLevel();
				EventInfo info1 = eventMap.get(id1);
				if(info1 == null) {
					info1 = new EventInfo(m1.getEvent().getMatches(m1.getLevel()));
					eventMap.put(id1, info1);
				}
				EventInfo info2 = eventMap.get(id2);
				if(info2 == null) {
					info2 = new EventInfo(m2.getEvent().getMatches(m2.getLevel()));
					eventMap.put(id2, info2);
				}
				// we want to play the matches from the level that's closer to being complete first
				int m1Percent = Math.round((float) info1.getPercentComplete());
				int m2Percent = Math.round((float) info2.getPercentComplete());
				if(m1Percent != m2Percent) {
					return m2Percent - m1Percent;
				}
				// last played time
				List<Player> m1Players = m1.getPlayers();
				List<Player> m2Players = m2.getPlayers();
				if(m1Players.isEmpty() && m2Players.isEmpty()) {
					return 0;
				}
				if(m1Players.isEmpty()) {
					return 1;
				}
				if(m2Players.isEmpty()) {
					return -1;
				}
				Collections.sort(m1Players, lastPlayedComp);
				Collections.sort(m2Players, lastPlayedComp);
				Player p1 = m1Players.get(0);
				Player p2 = m2Players.get(0);
				if(p1 == null && p2 == null) {
					return 0;
				}
				if(p1 == null) {
					return 1;
				}
				if(p2 == null) {
					return -1;
				}
				// players who have not played yet (null dates) should be treated as the earlier one
				Date d1 = p1.getLastMatchTime();
				Date d2 = p2.getLastMatchTime();
				if(d1 == null && d2 == null) {
					return 0;
				}
				if(d1 == null) {
					return -1;
				}
				if(d2 == null) {
					return 1;
				}
				return d1.compareTo(d2);
			}
		};
		// sort the matches
		boolean canIncrementTime = true;
		ArrayList<Match> sorted = new ArrayList<Match>();
		while(!matchesToSort.isEmpty()) {
			// find the most important match that can currently be played
			boolean addedMatch = false;
			for(int i = 0; i < matchesToSort.size(); ++i) {
				Match match = matchesToSort.get(i);
				if(current.isPlaying(match.getPlayers()) || hasRequestedDelay(match, current)) {
					continue;
				}
				addedMatch = true;
				Double playerImpact = playerMatchImpact.get(match);
				Double impact = matchImpact.get(match);
				for(int j = i + 1; j < matchesToSort.size() && ((playerImpact.compareTo(Double.valueOf(1.0)) > 0 && playerImpact.equals(playerMatchImpact.get(matchesToSort.get(j)))) || (playerImpact.equals(Double.valueOf(1.0)) && impact.equals(matchImpact.get(matchesToSort.get(j))))); ++j) {
					Match next = matchesToSort.get(j);
					if(current.isPlaying(next.getPlayers()) || hasRequestedDelay(next, current)) {
						continue;
					}
					if(matchComp.compare(match, next) > 0) {
						match = next;
						i = j;
					}
				}
				match = matchesToSort.remove(i);
				sorted.add(match);
				current.add(match);
				break;
			}
			if(!addedMatch) {
				// we couldn't add any more matches regularly, so we'll have to make some changes
				if(current.size() > 0) {
					// increment the time by removing the first match
					current.removeFirstMatch();
				}
				else if(canIncrementTime) {
					// try to add more matches by incrementing the time
					canIncrementTime = false;
					current.incrementCurrentDate();
				}
				else {
					// nothing else we can do so just add all the remaining matches
					sorted.addAll(matchesToSort);
					matchesToSort.clear();
				}
			}
			else {
				canIncrementTime = true;
			}
		}
		// attempt to honor the requested time
		tournament.updateEstimatedTimes(sorted);
		sorted.addAll(hasTime);
		Collections.sort(sorted, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				Date d1 = m1.getRequestedDate();
				if(d1 == null) {
					d1 = m1.getEstimatedDate();
				}
				Date d2 = m2.getRequestedDate();
				if(d2 == null) {
					d2 = m2.getEstimatedDate();
				}
				return compareDates(d1, d2);
			}
		});
		// make sure all matches with forfeits are shown and the winner match for a forfeited match is shown
		HashSet<Match> forfeitSet = new HashSet<Match>();
		for(Match match : hasForfeit) {
			if(!sorted.contains(match)) {
				forfeitSet.add(match);
			}
			if(match.getWinnerMatch() != null && !sorted.contains(match.getWinnerMatch())) {
				forfeitSet.add(match.getWinnerMatch());
			}
		}
		hasForfeit = new ArrayList<Match>(forfeitSet);
		Collections.sort(hasForfeit, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m2.getIndex() - m1.getIndex();
			}
		});
		sorted.addAll(hasForfeit);
		// adding the ordered matches back in the correct order
		for(Match match : hasOrder) {
			if(match.getNextAvailableCourtOrder() > sorted.size()) {
				sorted.add(match);
			}
			else {
				sorted.add(match.getNextAvailableCourtOrder() - 1, match);
			}
		}
		// add all the future matches
		Collections.sort(futureMatches, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m2.getIndex() - m1.getIndex();
			}
		});
		sorted.addAll(futureMatches);
		// add all the matches where we aren't using the scheduler
		Collections.sort(notScheduled, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m1.getIndex() - m2.getIndex();
			}
		});
		sorted.addAll(notScheduled);
		tournament.updateEstimatedTimes(sorted);
		return sorted;
	}
	
	private static boolean hasRequestedDelay(Match match, CurrentMatchQueue current) {
		for(Player player : match.getPlayers()) {
			if(player.getRequestedDelay() != null && player.getRequestedDelay().after(current.getCurrentDate())) {
				return true;
			}
		}
		return false;
	}
	
	private static void updateFeederMatchImpact(Match match, int incAmount, int additionalImpact, HashMap<Match, Double> matchImpact) {
		if(match == null) {
			return;
		}
		Double currentImpact = matchImpact.get(match);
		double newImpact = currentImpact != null ? currentImpact.doubleValue() : 0;
		newImpact += incAmount + additionalImpact;
		matchImpact.put(match, Double.valueOf(newImpact));
		if(match.getTeam1() == null) {
			updateFeederMatchImpact(match.getDefaultToTeam1Match(), incAmount, additionalImpact + incAmount, matchImpact);
		}
		if(match.getTeam2() == null) {
			updateFeederMatchImpact(match.getDefaultToTeam2Match(), incAmount, additionalImpact + incAmount, matchImpact);
		}
	}
	
	/**
	 * Sorts the dates by natural ordering. Null dates are considered to be the smallest values.
	 * @param d1 the first date
	 * @param d2 the second date
	 * @return returns -1 if the first date is earlier, 0 if the dates are the same, and 1 if the second date is earlier.
	 */
	public static int compareDates(Date d1, Date d2) {
		if(d1 == null && d2 == null) {
			return 0;
		}
		if(d1 == null) {
			return -1;
		}
		if(d2 == null) {
			return 1;
		}
		return d1.compareTo(d2);
	}
	
	public static List<Match> sortCompletedMatches(Set<Match> matches) {
		List<Match> list = new ArrayList<Match>(matches);
		Collections.sort(list, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return compareDates(m2.getEnd(), m1.getEnd());
			}
		});
		return list;
	}
	
	public static List<String> getValidEventNames(Player player, Collection<Event> events) {
		List<String> list = new ArrayList<String>();
		for(Event event : events) {
			if(event.getTeamFilter().isValidPlayer(player)) {
				list.add(event.getName());
			}
		}
		return list;
	}
	
	public static List<String> getValidPlayers(Team team, Collection<Player> players) {
		List<String> playerNames = new ArrayList<String>();
		for(Player player : players) {
			if(team.isValidPlayer(player)) {
				if(player.getName() != null && !player.getName().trim().isEmpty()) {
					playerNames.add(player.getName());
				}
			}
		}
		Collections.sort(playerNames);
		return playerNames;
	}
	
	public static List<int[]> getPairings(int numberOfTeams) {
		ArrayList<int[]> pairs = new ArrayList<int[]>();
		double log2 = Math.log(numberOfTeams) / Math.log(2.0);
		if(numberOfTeams < 2 || log2 > Math.floor(log2)) {
			// for events that have a non-power of 2 number of teams
			for(int i = 0; i < numberOfTeams; i += 2) {
				pairs.add(new int[]{i + 1, i + 2});
			}
		}
		else {
			// for events that have a power of 2 number of teams we build the pairs backwards from the final match
			pairs.add(new int[]{1, 2});
			while(pairs.size() * 2 < numberOfTeams) {
				int lowerSeeds = pairs.size() * 4;
				int[] seeds = new int[pairs.size() * 2];
				for(int i = 0; i < seeds.length; ++i) {
					seeds[i] = pairs.get(i / 2)[i % 2];
				}
				int[][] newPairs = new int[seeds.length][];
				for(int index = 1; index <= pairs.size() * 2; ++index) {
					for(int i = 0; i < seeds.length; ++i) {
						if(seeds[i] != index) {
							continue;
						}
						if(i < seeds.length / 2) {
							newPairs[i] = new int[]{index, lowerSeeds--};
						}
						else {
							newPairs[i] = new int[]{lowerSeeds--, index};
						}
						break;
					}
				}
				pairs.clear();
				for(int i = 0; i < newPairs.length; ++i) {
					pairs.add(newPairs[i]);
				}
			}
		}
		return pairs;
	}
	
	public static int getNextMatchIndex() {
		return ++matchIndex;
	}
	
	public static void updateMatchIndex(int matchIndex) {
		TournamentUtils.matchIndex = Math.max(TournamentUtils.matchIndex, matchIndex);
	}
	
	public static void decreaseMatchIndex(int amount) {
		matchIndex -= amount;
	}
	
	public static void resetMatchIndex() {
		matchIndex = 0;
	}
}
