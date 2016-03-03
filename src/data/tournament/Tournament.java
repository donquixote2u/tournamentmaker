package data.tournament;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import ui.util.GenericUtils;
import data.event.Event;
import data.match.Match;
import data.player.Player;
import data.team.Team;

public class Tournament implements Serializable {
	private static final long serialVersionUID = 5748474555295789647L;
	public static final int VERSION = 8;
	public static final int COMPATIBLE = 5;
	private transient List<Player> newPlayers;
	private transient TreeMap<Team, List<Team>> newTeams;
	private transient Map<Integer, MatchInfo> averageMatchTimes;
	private transient Set<Match> userActionMatches;
	private int version;
	private String name, filePath;
	private List<Event> events;
	private List<Player> players;
	private TreeMap<Team, List<Team>> createdTeams;
	private Set<Match> matches, completedMatches;
	private List<String> levels;
	private List<Court> courts;
	private int timeBetweenMatches;
	private boolean ignorePlayerStatus, showAllMatches, useDefaultPrinter, autoPrintMatches, disableBracketPooling;
	
	// this is for backwards compatibility
	private Map<Class<? extends Team>, List<Team>> teams;
	
	public Tournament(String name, List<String> levels, int numberOfCourts, int timeBetweenMatches) {
		if(numberOfCourts < 1) {
			throw new RuntimeException("invalid parameters");
		}
		setName(name);
		this.levels = levels;
		if(this.levels == null) {
			this.levels = new ArrayList<String>();
		}
		setTimeBetweenMatches(timeBetweenMatches);
		events = new ArrayList<Event>();
		players = new ArrayList<Player>();
		createdTeams = new TreeMap<Team, List<Team>>();
		matches = new HashSet<Match>();
		completedMatches = new HashSet<Match>();
		courts = new ArrayList<Court>();
		for(int i = 0; i < numberOfCourts; ++i) {
			courts.add(new Court(Integer.toString(i + 1)));
		}
		initTransientFields();
		version = VERSION;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		if(name == null) {
			throw new RuntimeException("invalid parameters");
		}
		this.name = name;
	}
	
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public String getFilePath() {
		return filePath;
	}
	
	public boolean getIgnorePlayerStatus() {
		return ignorePlayerStatus;
	}
	
	public void setIgnorePlayerStatus(boolean ignorePlayerStatus) {
		this.ignorePlayerStatus = ignorePlayerStatus;
		for(Player player : players) {
			player.setIgnoreCheckedIn(ignorePlayerStatus);
		}
	}
	
	public boolean getShowAllMatches() {
		return showAllMatches;
	}
	
	public void setShowAllMatches(boolean showAllMatches) {
		this.showAllMatches = showAllMatches;
	}
	
	public boolean getUseDefaultPrinter() {
		return useDefaultPrinter;
	}
	
	public void setUseDefaultPrinter(boolean useDefaultPrinter) {
		this.useDefaultPrinter = useDefaultPrinter;
	}
	
	public boolean getAutoPrintMatches() {
		return autoPrintMatches;
	}
	
	public void setAutoPrintMatches(boolean autoPrintMatches) {
		this.autoPrintMatches = autoPrintMatches;
	}
	
	public boolean getDisableBracketPooling() {
		return disableBracketPooling;
	}
	
	public void setDisableBracketPooling(boolean disableBracketPooling) {
		this.disableBracketPooling = disableBracketPooling;
	}
	
	/**
	 * @return the desired time between matches in milliseconds
	 */
	public long getLongTimeBetweenMatches() {
		// this was originally the number of minutes desired between matches. we multiply it by 60000 here instead of when it's stored for backwards compatibility 
		return timeBetweenMatches * 60000L;
	}
	
	public int getTimeBetweenMatches() {
		return timeBetweenMatches;
	}
	
	public void setTimeBetweenMatches(int timeBetweenMatches) {
		if(timeBetweenMatches < 0) {
			throw new RuntimeException("invalid parameters");
		}
		this.timeBetweenMatches = timeBetweenMatches;
	}
	
	/**
	 * @return returns the average match time in milliseconds
	 */
	public long getAverageMatchTime() {
		return averageMatchTimes.get(0).getTime();
	}
	
	/**
	 * @param match
	 * @return returns the estimated duration for this match in milliseconds
	 */
	public long getEstimatedDuration(Match match) {
		if(match == null) {
			return 0;
		}
		return averageMatchTimes.get(0).average(averageMatchTimes.get(match.getMatchLevel()));
	}
	
	public boolean isCloseForMultiDayTournament() {
		for(Court court : courts) {
			if(court.getCurrentMatch() != null || !court.getPreviousMatches().isEmpty()) {
				return false;
			}
		}
		for(Event event : events) {
			if(!event.isComplete()) {
				return true;
			}
		}
		return false;
	}
	
	public void checkOutAllPlayers() {
		// when checking out all the players, make sure to reset the last played times
		for(Player player : players) {
			player.setCheckedIn(false);
			player.setLastMatchTime(null);
		}
	}
	
	public List<Court> getCourts() {
		return Collections.unmodifiableList(courts);
	}
	
	public List<String> getLevels() {
		return Collections.unmodifiableList(levels);
	}
	
	public void addCompletedMatch(Match match) {
		if(match == null || match.getIgnoreMatch()) {
			return;
		}
		if(match.isComplete() && completedMatches.add(match)) {
			updateAverageMatchTime(match);
		}
	}
	
	public List<Match> getCompletedMatches() {
		return TournamentUtils.sortCompletedMatches(Collections.unmodifiableSet(completedMatches));
	}
	
	public void removeMatch(Match match) {
		matches.remove(match);
	}
	
	/**
	 * This function will set the order for the given match. If another match has the same order value, the previously
	 * assigned match's order will be set to order + 1. If the order value is greater than 9, remove the order.
	 * @param match
	 * @param order
	 */
	public void setMatchOrder(Match match, int order) {
		// don't do anything if this is outside the expect range of values
		if(match == null || order < 0 || order > 9 || match.getNextAvailableCourtOrder() == order) {
			return;
		}
		int oldOrder = match.getNextAvailableCourtOrder();
		match.setNextAvailableCourtOrder(0);
		HashMap<Integer, Match> orderedMatches = new HashMap<Integer, Match>();
		for(Match cur : matches) {
			if(oldOrder > 0 && cur.getNextAvailableCourtOrder() > oldOrder) {
				cur.setNextAvailableCourtOrder(cur.getNextAvailableCourtOrder() - 1);
			}
			if(cur.getNextAvailableCourtOrder() > 0) {
				orderedMatches.put(cur.getNextAvailableCourtOrder(), cur);
			}
		}
		if(order > 0) {
			match.setNextAvailableCourtOrder(order);
			while(orderedMatches.containsKey(order)) {
				orderedMatches.get(order++).setNextAvailableCourtOrder(order);
			}
			if(order > 9) {
				orderedMatches.get(9).setNextAvailableCourtOrder(0);
			}
		}
	}
	
	/**
	 * This function should be called when a match without order has been played.
	 */
	public void decrementMatchOrder() {
		HashMap<Integer, Match> orderedMatches = new HashMap<Integer, Match>();
		for(Match match : matches) {
			if(match.getNextAvailableCourtOrder() > 0) {
				orderedMatches.put(match.getNextAvailableCourtOrder(), match);
			}
		}
		for(int i = 9; i > 1; --i) {
			if(orderedMatches.containsKey(i) && !orderedMatches.containsKey(i - 1)) {
				orderedMatches.get(i).setNextAvailableCourtOrder(i - 1);
			}
		}
	}
	
	/**
	 * Returns the first available court reservation. Returns 0 if there are none available (maximum of 9).
	 */
	public int getNextFreeCourt() {
		TreeSet<Integer> reservedCourts = new TreeSet<Integer>();
		for(Match match : matches) {
			reservedCourts.add(match.getNextAvailableCourtOrder());
		}
		reservedCourts.remove(0);
		int pre = 0;
		for(Integer cur : reservedCourts) {
			if(cur - pre > 1) {
				return pre + 1;
			}
			pre = cur;
		}
		return pre < 9 ? pre + 1 : 0;
	}
	
	/**
	 * This function will recalculate the result of the given match based on the current values.
	 * @param match
	 * @return The set of matches that are affected from this recalculation.
	 */
	public Set<Match> recalculateMatch(Match match) {
		HashSet<Match> matches = new HashSet<Match>();
		if(match == null) {
			return matches;
		}
		Set<Match> changedMatches = match.recalculate();
		if(changedMatches == null) {
			// we are undoing a match and sending it and all affected matches back to the set of matches
			addMatches(undoMatch(match));
		}
		else {
			// we are just changing the score for this match
			ArrayList<Match> currentMatches = new ArrayList<Match>(changedMatches);
			while(!currentMatches.isEmpty()) {
				Match currentMatch = currentMatches.remove(0);
				if(currentMatch.isComplete()) {
					if(currentMatch.getTeam1() != null && currentMatch.getTeam2() != null) {
						matches.add(currentMatch);
					}
					else {
						// this should be a bye so reset everything
						currentMatch.reset();
						Set<Match> newMatches = currentMatch.recalculate();
						if(newMatches == null) {
							addMatches(undoMatch(currentMatch));
						}
						else {
							currentMatches.addAll(newMatches);
						}
					}
				}
				else {
					// check to see if we can auto complete this match (looking for byes)
					ArrayList<Match> newMatches = new ArrayList<Match>();
					newMatches.add(currentMatch);
					while(!newMatches.isEmpty()) {
						Match newMatch = newMatches.remove(0);
						if(newMatch.getTeam1() != null && newMatch.getTeam2() != null) {
							continue;
						}
						// remove the match from whatever court it is on
						if(newMatch.getCourt() != null) {
							newMatch.getCourt().undoMatch(newMatch);
						}
						Set<Match> finishedMatches = newMatch.finish();
						if(finishedMatches != null) {
							newMatch.end();
							addCompletedMatch(newMatch);
							for(Match finishedMatch : finishedMatches) {
								if(!newMatches.contains(finishedMatch)) {
									newMatches.add(finishedMatch);
								}
							}
							removeMatch(newMatch);
						}
						else {
							// if we are unable to auto complete the match, we should add it back into the queue
							addMatches(undoMatch(newMatch));
						}
					}
				}
			}
		}
		return matches;
	}
	
	private void undoMatchTime(Match match) {
		Date start = match.getStart();
		Date end = match.getEnd();
		long matchTime = 0;
		// ignore matches with no start, no end time, or invalid/empty times
		if(start != null && end != null) {
			matchTime = end.getTime() - start.getTime();
		}
		if(matchTime > 0) {
			averageMatchTimes.get(0).removeTime(matchTime);
			if(match.getMatchLevel() > 0) {
				MatchInfo levelAverage = averageMatchTimes.get(match.getMatchLevel());
				if(levelAverage != null) {
					levelAverage.removeTime(matchTime);
				}
			}
		}
	}
	
	/**
	 * Undo the given match and all dependent matches.
	 * @param match
	 */
	private Set<Match> undoMatch(Match match) {
		HashSet<Match> matches = new HashSet<Match>();
		if(match == null) {
			return matches;
		}
		completedMatches.remove(match);
		undoMatchTime(match);
		// removing the court from the match and adding the match back into the matches queue
		if(match.getCourt() == null || !match.getCourt().undoMatch(match)) {
			match.setCourt(null);
		}
		matches.add(match);
		// undo all dependent matches
		ArrayList<Match> dependents = new ArrayList<Match>(match.getDependentMatchesForUndo());
		if(match.getWinnerMatch() != null) {
			dependents.add(match.getWinnerMatch());
		}
		Map<String, List<Match>> loserMatches = match.getLoserMatches();
		for(String key : loserMatches.keySet()) {
			for(Match loserMatch : loserMatches.get(key)) {
				dependents.add(loserMatch);
			}
		}
		while(!dependents.isEmpty()) {
			Match dependent = dependents.remove(0);
			Set<Match> nextMatches = dependent.recalculate();
			if(nextMatches == null) {
				matches.addAll(undoMatch(dependent));
			}
			else {
				dependents.addAll(0, nextMatches);
			}
		}
		return matches;
	}
	
	public void addMatches(Collection<Match> matches) {
		for(Match match : matches) {
			if(!match.isComplete() && match.getStart() == null) {
				this.matches.add(match);
			}
		}
		// since the matches can affect each other, we will run all the matches until none of them are potentially completable
		TreeSet<Match> currentMatches = new TreeSet<Match>(new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				// sorting by level and then match index
				int comp = m1.getEvent().getDisplayLevels().indexOf(m1.getLevel()) - m2.getEvent().getDisplayLevels().indexOf(m2.getLevel());
				if(comp != 0) {
					return comp;
				}
				return m2.getIndex() - m1.getIndex();
			}
		});
		currentMatches.addAll(this.matches);
		Iterator<Match> iter = currentMatches.iterator();
		while(iter.hasNext()) {
			Match match = iter.next();
			Set<Match> set = match.finish();
			if(set != null) {
				match.end();
				addCompletedMatch(match);
				iter.remove();
				removeMatch(match);
				for(Match nextMatch : set) {
					if(!nextMatch.isComplete() && nextMatch.getStart() == null) {
						currentMatches.add(nextMatch);
						this.matches.add(nextMatch);
					}
				}
				iter = currentMatches.iterator();
			}
		}
	}
	
	public void addUserActionMatch(Match match) {
		if(match.hasUserAction()) {
			userActionMatches.add(match);
		}
		else {
			userActionMatches.remove(match);
		}
	}
	
	public synchronized List<Match> getMatches() {
		Set<Match> matches = new HashSet<Match>();
		if(showAllMatches) {
			for(Event event : events) {
				matches.addAll(event.getAllMatches());
			}
		}
		else {
			matches.addAll(this.matches);
			matches.addAll(userActionMatches);
		}
		return TournamentUtils.sortMatches(this, matches);
	}
	
	/**
	 * Set the estimated times for all the given matches.
	 * @param matches
	 * @return
	 */
	public synchronized void updateEstimatedTimes(Collection<Match> matches) {
		if(matches == null) {
			return;
		}
		ArrayList<Match> curMatches = new ArrayList<Match>(matches);
		// clear all the previously set estimated times
		for(Player player : players) {
			player.setEstimatedDate(null);
		}
		for(Team key : createdTeams.keySet()) {
			for(Team team : createdTeams.get(key)) {
				team.setEstimatedDate(null);
			}
		}
		for(Match match : this.matches) {
			match.setEstimatedDate(null);
		}
		// check to see if we can do any kind of time estimation
		if(getAverageMatchTime() == 0) {
			return;
		}
		// set the estimated time for all the matches
		CurrentMatchQueue queue = new CurrentMatchQueue(this);
		if(!queue.hasUsableCourts()) {
			return;
		}
		while(!curMatches.isEmpty()) {
			boolean jumped = false;
			Iterator<Match> iter = curMatches.iterator();
			while(iter.hasNext()) {
				Match match = iter.next();
				List<Player> players = match.getPlayers();
				if(queue.isPlaying(players)) {
					jumped = true;
					continue;
				}
				iter.remove();
				queue.add(match);
				Date currentDate = queue.getCurrentDate();
				match.setEstimatedDate(currentDate);
				if(match.getTeam1() != null) {
					match.getTeam1().setEstimatedDate(currentDate);
				}
				if(match.getTeam2() != null) {
					match.getTeam2().setEstimatedDate(currentDate);
				}
				for(Player player : players) {
					player.setEstimatedDate(currentDate);
				}
				if(jumped) {
					break;
				}
			}
			if(jumped) {
				queue.removeFirstMatch();
			}
		}
	}
	
	public void startEvent(Event event) {
		if(!events.contains(event) || event.isStarted() || !event.canStart()) {
			return;
		}
		addMatches(event.start());
	}
	
	public void undoEvent(Event event) {
		if(event == null) {
			return;
		}
		Set<Match> matches = event.getAllMatches();
		int start = Integer.MAX_VALUE;
		int end = Integer.MIN_VALUE; 
		for(Match match : matches) {
			if(match.getIndex() < start) {
				start = match.getIndex();
			}
			if(match.getIndex() > end) {
				end = match.getIndex();
			}
			match.setRequestedDate(null);
			setMatchOrder(match, 0);
			match.setT1OnStart(false, false);
			match.setT2OnStart(false, false);
			addUserActionMatch(match);
		}
		int difference = end - (--start);
		for(Court court : courts) {
			for(Match match : new ArrayList<Match>(court.getPreviousMatches())) {
				if(matches.remove(match)) {
					court.undoMatch(match);
				}
			}
			if(court.getCurrentMatch() != null && matches.remove(court.getCurrentMatch())) {
				court.undoMatch(court.getCurrentMatch());
			}
		}
		for(Match match : new ArrayList<Match>(completedMatches)) {
			if(matches.remove(match)) {
				completedMatches.remove(match);
				undoMatchTime(match);
			}
		}
		for(Match match : new ArrayList<Match>(matches)) {
			if(matches.remove(match)) {
				removeMatch(match);
			}
		}
		event.undo();
		TournamentUtils.decreaseMatchIndex(difference);
		for(Event cur : events) {
			if(cur.isStarted()) {
				for(Match match : cur.getAllMatches()) {
					if(match.getIndex() > end) {
						match.setIndex(match.getIndex() - difference);
					}
				}
			}
		}
	}
	
	public boolean addEvent(Event event) {
		if(event == null) {
			return false;
		}
		if(getEvent(event.getName()) != null) {
			return false;
		}
		events.add(event);
		Collections.sort(events, new Comparator<Event>() {
			public int compare(Event event1, Event event2) {
				return event1.getName().compareTo(event2.getName());
			}
		});
		return true;
	}
	
	public void renameEvent(Event event, String name) {
		if(getEvent(name) != null) {
			return;
		}
		for(Player player : players) {
			Set<String> events = new HashSet<String>(player.getEvents());
			if(events.remove(event.getName())) {
				events.add(name);
				player.setEvents(events);
			}
		}
		event.setName(name);
		Collections.sort(events, new Comparator<Event>() {
			public int compare(Event event1, Event event2) {
				return event1.getName().compareTo(event2.getName());
			}
		});
	}
	
	public String removeEvent(Event event) {
		if(event == null) {
			return "Can not remove a null event.";
		}
		if(event.isStarted()) {
			return "Can not remove an event that has started.";
		}
		if(events.remove(event)) {
			for(Player player : getPlayers()) {
				Set<String> events = new HashSet<String>(player.getEvents());
				if(events.remove(event.getName())) {
					player.setEvents(events);
				}
			}
			return null;
		}
		return "Event not found.";
	}
	
	public boolean replaceEvent(Event newEvent) {
		if(newEvent == null || newEvent.isStarted()) {
			return false;
		}
		boolean replacedEvent = false;
		for(int i = 0; i < events.size(); ++i) {
			Event event = events.get(i);
			if(!event.getName().equals(newEvent.getName())) {
				continue;
			}
			if(!event.isStarted()) {
				replacedEvent = true;
				events.remove(i);
				events.add(i, newEvent);
			}
			break;
		}
		return replacedEvent;
	}
	
	public Event getEvent(String name) {
		for(Event event : events) {
			if(event.getName().equals(name)) {
				return event;
			}
		}
		return null;
	}
	
	public List<Event> getEvents() {
		return Collections.unmodifiableList(events);
	}
	
	public void addPlayer(Player player) {
		if(player == null) {
			return;
		}
		players.add(player);
		newPlayers.add(player);
		player.setIgnoreCheckedIn(ignorePlayerStatus);
	}
	
	public String removePlayer(Player player) {
		if(player == null) {
			return "Can not remove a null player.";
		}
		for(Team team : getTeams()) {
			if(team.getPlayers().contains(player)) {
				return "Can not remove a player who is on a team (" + team.getName() + ").";
			}
		}
		if(players.remove(player)) {
			return null;
		}
		return "Player not found.";
	}
	
	public List<Player> getPlayers() {
		return Collections.unmodifiableList(players);
	}
	
	public List<Player> getNewPlayers() {
		List<Player> list = newPlayers;
		newPlayers = new ArrayList<Player>();
		return list;
	}
	
	public boolean hasNewPlayers() {
		return !newPlayers.isEmpty();
	}
	
	public String getUpcomingMatchesForPlayer(Player player) {
		if(player == null) {
			return null;
		}
		ArrayList<Match> upcoming = new ArrayList<Match>();
		for(Match match : matches) {
			if(match == null) {
				continue;
			}
			Event event = match.getEvent();
			if(event.getPausedLevels() != null && event.getPausedLevels().contains(match.getLevel()) && event.getPausedMatchLevel() >= match.getMatchLevel()) {
				continue;
			}
			if(match.getPlayers().contains(player)) {
				upcoming.add(match);
			}
		}
		if(upcoming.isEmpty()) {
			return "No upcoming matches.";
		}
		Collections.sort(upcoming, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return TournamentUtils.compareDates(m1.getEstimatedDate(), m2.getEstimatedDate());
			}
		});
		String info = "";
		for(Match match : upcoming) {
			if(match.getTeam1() == null) {
				info += match.getToTeam1MatchLongDiscription(match.getToTeam1MatchDescription());
				info = info.substring(0, info.length() - 1);
			}
			else if(match.getTeam2() == null) {
				info += match.getToTeam2MatchLongDiscription(match.getToTeam2MatchDescription());
				info = info.substring(0, info.length() - 1);
			}
			else {
				info += "Playing against ";
				if(match.getTeam1().getPlayers().contains(player)) {
					info += match.getTeam2().getName();
				}
				else {
					info += match.getTeam1().getName();
				}
			}
			info += " in " + match.getEvent().getPoolName(match, disableBracketPooling);
			if(match.getEstimatedDate() != null) {
				info += " (est. " + GenericUtils.dateToString(match.getEstimatedDate(), "hh:mm a") + ")";
			}
			info += "\n";
		}
		return info;
	}
	
	public boolean addTeam(Team team) {
		if(team == null) {
			return false;
		}
		List<Team> list = createdTeams.get(team);
		if(list == null) {
			list = new ArrayList<Team>();
			createdTeams.put(team.newInstance(), list);
		}
		list.add(team);
		list = newTeams.get(team);
		if(list == null) {
			list = new ArrayList<Team>();
			newTeams.put(team.newInstance(), list);
		}
		list.add(team);
		return true;
	}
	
	public String removeTeam(Team team) {
		if(team == null) {
			return "Can not remove a null team.";
		}
		boolean inUse = false;
		for(Event event : events) {
			if(event.getTeamFilter().getTeamType().equals(team.getTeamType())) {
				inUse = true;
			}
			for(Team eventTeam : event.getTeams()) {
				// we don't use contains here because we want to see if the actual team is in the event
				if(eventTeam == team) {
					return "Can not remove a team playing in an event (" + event.getName() + ").";
				}
			}
		}
		List<Team> list = createdTeams.get(team);
		if(list == null || !list.remove(team)) {
			return "Team not found.";
		}
		if(!inUse && list.isEmpty()) {
			createdTeams.remove(team);
		}
		return null;
	}
	
	public boolean addTeamType(Team team) {
		if(team == null) {
			return false;
		}
		if(!createdTeams.containsKey(team)) {
			createdTeams.put(team, new ArrayList<Team>());
			return true;
		}
		return false;
	}
	
	public List<String> getTeamTypes() {
		ArrayList<String> keys = new ArrayList<String>();
		for(Team team : createdTeams.keySet()) {
			keys.add(team.getTeamType());
		}
		Collections.sort(keys);
		return keys;
	}
	
	public Team getTeamByType(String teamType) {
		for(Team team : createdTeams.keySet()) {
			if(team.getTeamType().equals(teamType)) {
				return team;
			}
		}
		return null;
	}
	
	public List<Team> getTeams() {
		ArrayList<Team> list = new ArrayList<Team>();
		for(List<Team> value : createdTeams.values()) {
			list.addAll(value);
		}
		return list;
	}
	
	public List<Team> getTeams(Team team) {
		ArrayList<Team> list = new ArrayList<Team>();
		if(createdTeams.containsKey(team)) {
			list.addAll(createdTeams.get(team));
		}
		return list;
	}
	
	public List<Team> getNewTeams() {
		ArrayList<Team> list = new ArrayList<Team>();
		for(List<Team> value : newTeams.values()) {
			list.addAll(value);
		}
		newTeams.clear();
		return list;
	}
	
	public boolean hasNewTeams() {
		return !newTeams.isEmpty();
	}
	
	public int getVersion() {
		return version;
	}
	
	public void updateVersion() {
		if(version >= COMPATIBLE) {
			version = VERSION;
		}
	}
	
	private void updateAverageMatchTime(Match match) {
		// ignore matches with forfeits
		if(match == null || match.getTeam1Forfeit() || match.getTeam2Forfeit()) {
			return;
		}
		Date start = match.getStart();
		Date end = match.getEnd();
		// ignore matches with no start or end time
		if(start == null || end == null) {
			return;
		}
		long matchTime = end.getTime() - start.getTime();
		// ignore matches with invalid/empty times
		if(matchTime <= 0) {
			return;
		}
		averageMatchTimes.get(0).addTime(matchTime);
		if(match.getMatchLevel() == 0) {
			return;
		}
		MatchInfo levelAverage = averageMatchTimes.get(match.getMatchLevel());
		if(levelAverage == null) {
			levelAverage = new MatchInfo();
			averageMatchTimes.put(match.getMatchLevel(), levelAverage);
		}
		levelAverage.addTime(matchTime);
	}
	
	// for initializing our transient fields and backwards compatibility
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		initTransientFields();
		// check to see if we are dealing with an old tournament file
		if(teams != null) {
			createdTeams = new TreeMap<Team, List<Team>>();
			for(List<Team> list : teams.values()) {
				for(Team team : list) {
					addTeam(team);
				}
			}
			teams = null;
			newPlayers.clear();
			newTeams.clear();
		}
		// data files before version 4 didn't have display levels
		if(version < 4) {
			for(Event event : events) {
				if(!event.isStarted()) {
					continue;
				}
				for(String displayLevel : event.getDisplayLevels()) {
					for(Match match : event.getMatches(displayLevel)) {
						do {
							match.setLevel(displayLevel);
							match.setEvent(event);
							match = match.getWinnerMatch();
						}
						while(match != null);
					}
				}
			}
		}
	}
	
	private void initTransientFields() {
		newPlayers = new ArrayList<Player>();
		newTeams = new TreeMap<Team, List<Team>>();
		averageMatchTimes = new HashMap<Integer, MatchInfo>();
		averageMatchTimes.put(0, new MatchInfo());
		for(Match match : completedMatches) {
			updateAverageMatchTime(match);
		}
		userActionMatches = new HashSet<Match>();
		for(Event event : events) {
			for(Match match : event.getAllMatches()) {
				if(match.hasUserAction()) {
					userActionMatches.add(match);
				}
			}
		}
		setIgnorePlayerStatus(ignorePlayerStatus);
	}
}
