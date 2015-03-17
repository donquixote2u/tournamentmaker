package data.tournament;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ui.util.Pair;
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
			// don't sort matches that are missing both teams
			if(match.getTeam1() == null && match.getTeam2() == null) {
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
		LinkedHashSet<Match> unsorted = new LinkedHashSet<Match>(matchesToSort);
		// separating the matches into different groups
		final CurrentMatchQueue current = new CurrentMatchQueue(tournament);
		// TODO figure out when to play scheduled matches?
		final HashMap<Player, Set<Player>> playerToTeammates = new HashMap<Player, Set<Player>>();
		HashSet<Match> initial = new HashSet<Match>();
		final HashSet<Match> waiting = new HashSet<Match>();
		PlayerQueue playerQueue = new PlayerQueue();
		HashMap<Player, List<Match>> playerToMatches = new HashMap<Player, List<Match>>();
		for(Match match : unsorted) {
			for(Player player : match.getPlayers()) {
				List<Match> playerMatches = playerToMatches.get(player);
				if(playerMatches == null) {
					playerMatches = new ArrayList<Match>();
					playerToMatches.put(player, playerMatches);
				}
				playerMatches.add(match);
				playerQueue.add(current.getlastPlayedDate(player), player);
				Set<Player> teammates = playerToTeammates.get(player);
				if(teammates == null) {
					teammates = new HashSet<Player>();
					playerToTeammates.put(player, teammates);
				}
				ArrayList<Player> list = new ArrayList<Player>(match.getTeam1() != null ? match.getTeam1().getPlayers() : match.getTeam2().getPlayers());
				if(!list.remove(player)) {
					list = new ArrayList<Player>(match.getTeam2().getPlayers());
					list.remove(player);
				}
				teammates.addAll(list);
			}
			if(isInitialMatch(match)) {
				initial.add(match);
			}
			if(match.getTeam1() == null || match.getTeam2() == null) {
				waiting.add(match);
			}
		}
		// this comparator sorts the matches by if the match can start, then by the status, then by event progress, then by match tier
		final Comparator<Match> matchComp = new Comparator<Match>() {
			private HashMap<String, EventInfo> eventMap = new HashMap<String, EventInfo>();
			
			public int compare(Match m1, Match m2) {
				// null check
				if(m1 == null && m2 == null) {
					return 0;
				}
				if(m1 == null) {
					return 1;
				}
				if(m2 == null) {
					return -1;
				}
				// waiting matches
				boolean m1Status = waiting.contains(m1);
				boolean m2Status = waiting.contains(m2);
				int compare = m1Status == m2Status ? 0 : (m1Status ? -1 : 1);
				if(compare != 0) {
					return compare;
				}
				// matches whose mirror matches are playing or are complete
				Match m1Mirror = m1.getMirrorMatch();
				Match m2Mirror = m2.getMirrorMatch();
				m1Status = current.contains(m1Mirror) || current.isComplete(m1Mirror);
				m2Status = current.contains(m2Mirror) || current.isComplete(m2Mirror);
				compare = m1Status == m2Status ? 0 : (m1Status ? -1 : 1);
				if(compare != 0) {
					return compare;
				}
				// matches with players whose team mates are already playing
				m1Status = false;
				for(Player player : m1.getPlayers()) {
					if(current.isPlaying(playerToTeammates.get(player))) {
						m1Status = true;
						break;
					}
				}
				m2Status = false;
				for(Player player : m2.getPlayers()) {
					if(current.isPlaying(playerToTeammates.get(player))) {
						m2Status = true;
						break;
					}
				}
				compare = m1Status == m2Status ? 0 : (m1Status ? -1 : 1);
				if(compare != 0) {
					return compare;
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
				compare = Math.round((float) info2.getPercentComplete()) - Math.round((float) info1.getPercentComplete());
				if(compare == 0) {
					compare = info1.getNumberOfMatches() - info2.getNumberOfMatches();
				}
				// compare match placement in the bracket
				return compare == 0 ? m2.getMatchLevel() - m1.getMatchLevel() : compare;
			}
		};
		// sort the matches
		boolean canIncrementTime = true;
		HashMap<Match, List<Match>> feederMatches = new HashMap<Match, List<Match>>();
		ArrayList<Match> sorted = new ArrayList<Match>();
		while(!playerQueue.isEmpty()) {
			// add all the playable matches
			while(!playerQueue.isEmpty()) {
				int index = 0;
				Pair<Match, Match> selectedMatch = null;
				Pair<Date, LinkedHashSet<Player>> entry = playerQueue.get(index++);
				// get the most important match
				while(entry != null) {
					boolean playerNeedsRest = needsRest(tournament.getTimeBetweenMatches(), entry.getKey(), current.getCurrentDate());
					for(Player player : entry.getValue()) {
						// look through this player's potential matches and try to find one we can play
						for(Match match : playerToMatches.get(player)) {
							// if this match has already been selected, we should continue looking
							if(selectedMatch != null && (selectedMatch.getKey().equals(match) || selectedMatch.getValue().equals(match))) {
								continue;
							}
							// if this match isn't waiting for other matches and we have a player who can't play/needs rest, skip this match
							if((playerNeedsRest || current.isPlaying(player)) && !waiting.contains(match)) {
								continue;
							}
							// select the more important match
							Match matchToPlay = getMatchToPlay(match, tournament.getTimeBetweenMatches(), unsorted, current, feederMatches, matchComp);
							if(matchToPlay == null) {
								continue;
							}
							// if one of the players has requested a delay, then we can't play this match
							if(hasRequestedDelay(matchToPlay, current)) {
								continue;
							}
							if(selectedMatch == null) {
								selectedMatch = new Pair<Match, Match>(match, matchToPlay);
							}
							else {
								int compare = matchComp.compare(selectedMatch.getValue(), matchToPlay);
								if(compare > 0 || (compare == 0 && matchComp.compare(selectedMatch.getKey(), match) > 0)) {
									selectedMatch = new Pair<Match, Match>(match, matchToPlay);
								}
							}
						}
					}
					entry = playerQueue.get(index++);
				}
				// if we couldn't find a match, exit the loop and make some changes to our sorting algorithm
				if(selectedMatch == null) {
					break;
				}
				canIncrementTime = true;
				// check the mirror match
				LinkedHashSet<Match> selectedMatches = new LinkedHashSet<Match>();
				selectedMatches.add(selectedMatch.getValue());
				Match mirror = getMatchToPlay(selectedMatch.getValue().getMirrorMatch(), tournament.getTimeBetweenMatches(), unsorted, current, feederMatches, matchComp);
				if(mirror != null && !hasRequestedDelay(mirror, current)) {
					selectedMatches.add(mirror);
				}
				// adding the selected matches to the list of current matches
				for(Match match : selectedMatches) {
					sorted.add(match);
					// update the player queue
					for(Player player : match.getPlayers()) {
						Date key = current.getlastPlayedDate(player);
						LinkedHashSet<Player> players = playerQueue.get(key).getValue();
						players.remove(player);
						if(players.isEmpty()) {
							playerQueue.remove(key);
						}
					}
					Date endDate = current.add(match);
					// add the remaining matches back into the player queue
					for(Player player : match.getPlayers()) {
						List<Match> possibleMatches = playerToMatches.get(player);
						possibleMatches.remove(match);
						if(!possibleMatches.isEmpty()) {
							playerQueue.add(endDate, player);
						}
					}
					// remove the match from any sort groups they might be in
					initial.remove(match);
					waiting.remove(match);
				}
			}
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
				// there's nothing else we can do, just add all the rest of the matches
				while(!playerQueue.isEmpty()) {
					Pair<Date, LinkedHashSet<Player>> entry = playerQueue.poll();
					for(Player player : entry.getValue()) {
						List<Match> playerMatches = playerToMatches.get(player);
						while(!playerMatches.isEmpty()) {
							Match match = playerMatches.get(0);
							for(Player matchPlayer : match.getPlayers()) {
								playerToMatches.get(matchPlayer).remove(match);
							}
							sorted.add(match);
						}
					}
				}
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
		tournament.updateEstimatedTimes(sorted);
		return sorted;
	}
	
	private static Match getMatchToPlay(Match match, long waitTime, LinkedHashSet<Match> unsorted, CurrentMatchQueue current, HashMap<Match, List<Match>> feederMatches, Comparator<Match> matchComp) {
		Match matchToPlay = null;
		if(match == null || current.isComplete(match)) {
			return matchToPlay;
		}
		int previousCount = 0;
		List<Match> previousMatches = feederMatches.get(match);
		if(previousMatches == null) {
			previousMatches = new ArrayList<Match>();
			feederMatches.put(match, previousMatches);
		}
		if(match.getTeam1() == null) {
			Match toT1 = previousMatches.size() == previousCount ? match.getToTeam1Match(false) : previousMatches.get(previousCount);
			if(toT1 != null && previousMatches.size() == previousCount) {
				previousMatches.add(toT1);
			}
			++previousCount;
			matchToPlay = getMatchToPlay(toT1, waitTime, unsorted, current, feederMatches, matchComp);
		}
		if(match.getTeam2() == null) {
			Match toT2 = previousMatches.size() == previousCount ? match.getToTeam2Match(false) : previousMatches.get(previousCount);
			if(toT2 != null && previousMatches.size() == previousCount) {
				previousMatches.add(toT2);
			}
			++previousCount;
			Match other = getMatchToPlay(toT2, waitTime, unsorted, current, feederMatches, matchComp);
			matchToPlay = matchComp.compare(matchToPlay, other) <= 0 ? matchToPlay : other;
		}
		for(Match previous : previousMatches) {
			if(!current.isComplete(previous) || current.isPlaying(previous.getPlayers()) || needsRest(waitTime, getLastPlayedTime(previous, current), current.getCurrentDate())) {
				return matchToPlay;
			}
		}
		if(unsorted.contains(match) && !current.isPlaying(match.getPlayers()) && !needsRest(waitTime, getLastPlayedTime(match, current), current.getCurrentDate())) {
			return match;
		}
		return matchToPlay;
	}
	
	private static boolean isInitialMatch(Match match) {
		boolean isInitial = true;
		if(match.getTeam1() != null && match.getTeam1().getMatchesPlayed() > 0) {
			isInitial = false;
		}
		if(match.getTeam2() != null && match.getTeam2().getMatchesPlayed() > 0) {
			isInitial = false;
		}
		return isInitial;
	}
	
	private static boolean hasRequestedDelay(Match match, CurrentMatchQueue current) {
		for(Player player : match.getPlayers()) {
			if(player.getRequestedDelay() != null && player.getRequestedDelay().after(current.getCurrentDate())) {
				return true;
			}
		}
		return false;
	}
	
	private static Date getLastPlayedTime(Match match, CurrentMatchQueue current) {
		Date lastPlayed = null;
		for(Player player : match.getPlayers()) {
			Date lastMatchTime = current.getlastPlayedDate(player);
			if((lastPlayed == null && lastMatchTime != null) || (lastPlayed != null && lastMatchTime != null && lastPlayed.before(lastMatchTime))) {
				lastPlayed = lastMatchTime;
			}
		}
		return lastPlayed;
	}
	
	private static boolean needsRest(long waitTime, Date lastPlayed, Date currentDate) {
		if(lastPlayed == null) {
			return false;
		}
		if(waitTime <= 0) {
			return false;
		}
		return currentDate.getTime() - lastPlayed.getTime() < waitTime;
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
