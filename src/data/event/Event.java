package data.event;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.event.painter.EventPainter;
import data.event.painter.SingleEliminationEventPainter;
import data.event.result.EventResult;
import data.match.Match;
import data.player.Player;
import data.team.Team;

public abstract class Event implements Serializable {
	private static final long serialVersionUID = -4350019506547575220L;
	// number of starting matches in each pool
	private static final int POOL_SIZE = 8;
	private int numberOfTeams, minScore, maxScore, winBy, bestOf;
	private String name;
	private List<String> levels;
	private List<Team> teams;
	private Team teamFilter;
	private boolean started, filterTeamByLevel;
	private Set<String> pausedLevels;
	private int pausedMatchLevel;
	private List<String> displayLevels;
	// this is for backwards compatibility
	private Class<? extends Team> teamType;
	
	public Event(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		if(name == null || teamFilter == null || levels == null || numberOfTeams < 1 || minScore < 0 || maxScore - minScore < winBy || winBy < 1 || bestOf < 1) {
			throw new IllegalArgumentException("Invalid paramters.");
		}
		setName(name);
		this.levels = new ArrayList<String>(levels);
		this.teamFilter = teamFilter;
		this.numberOfTeams = numberOfTeams;
		this.maxScore = maxScore;
		this.minScore = minScore;
		this.winBy = winBy;
		this.bestOf = bestOf;
		filterTeamByLevel = !levels.isEmpty();
		teams = new ArrayList<Team>();
		for(int i = 0; i < numberOfTeams; ++i) {
			teams.add(null);
		}
		pausedMatchLevel = -1;
		displayLevels = generateDisplayLevels(this.levels);
	}
	
	public final int getNumberOfTeams() {
		return numberOfTeams;
	}
	
	public final String getName() {
		return name;
	}
	
	public final void setName(String name) {
		this.name = name;
	}
	
	public final Team getTeamFilter() {
		return teamFilter;
	}
	
	public final int getMinScore() {
		return minScore;
	}
	
	public final int getMaxScore() {
		return maxScore;
	}
	
	public final int getWinBy() {
		return winBy;
	}
	
	public final int getBestOf() {
		return bestOf;
	}
	
	public final List<String> getDisplayLevels() {
		return Collections.unmodifiableList(displayLevels);
	}
	
	public final List<String> getLevels() {
		return Collections.unmodifiableList(levels);
	}
	
	public final Match createMatch(String level) {
		Match match = generateMatch(minScore, maxScore, winBy, bestOf);
		match.setLevel(level);
		match.setEvent(this);
		return match;
	}
	
	public final Match createMatch(Team t1, Team t2, String level) {
		Match match = createMatch(level);
		match.setTeam1(t1);
		match.setTeam2(t2);
		return match;
	}
	
	// subclasses should override this function to change the type of match being returned
	public Match generateMatch(int minScore, int maxScore, int winBy, int bestOf) {
		return new Match(minScore, maxScore, winBy, bestOf);
	}
	
	// subclasses should override this function to change the validation
	public boolean isValid() {
		List<Team> teams = getTeams();
		if(getNumberOfTeams() != teams.size()) {
			return false;
		}
		HashSet<Player> playerSet = new HashSet<Player>();
		ArrayList<Player> playerList = new ArrayList<Player>();
		HashSet<Team> set = new HashSet<Team>();
		ArrayList<Team> list = new ArrayList<Team>();
		for(Team team : teams) {
			if(team != null) {
				if(team.getInEvent()) {
					return false;
				}
				if(filterTeamByLevel && Collections.disjoint(levels, team.getLevels())) {
					return false;
				}
				if(teamFilter.isValidTeam(team)) {
					set.add(team);
					list.add(team);
					playerSet.addAll(team.getPlayers());
					playerList.addAll(team.getPlayers());
				}
				else {
					return false;
				}
			}
		}
		return set.size() == list.size() && playerSet.size() == playerList.size();
	}
	
	// this should be called to set the list of teams (should be in order with empty spaces for the byes)
	public final void setTeams(List<Team> teams) {
		if(isStarted()) {
			throw new RuntimeException("Unable to set teams for an in-progress/completed event.");
		}
		if(teams == null || teams.size() != numberOfTeams) {
			throw new RuntimeException("Invalid list of teams given.");
		}
		this.teams.clear();
		this.teams.addAll(teams);
	}
	
	public final List<Team> getTeams() {
		return Collections.unmodifiableList(teams);
	}
	
	public final boolean isComplete(String level) {
		if(!isStarted()) {
			return false;
		}
		List<Match> matches = getMatches(level);
		if(matches == null) {
			return false;
		}
		for(Match match : EventUtils.getAllMatches(matches)) {
			if(!match.isComplete()) {
				return false;
			}
		}
		return true;
	}
	
	public final boolean isComplete() {
		for(String level : getDisplayLevels()) {
			if(!isComplete(level)) {
				return false;
			}
		}
		return true;
	}
	
	public final Set<String> getPausedLevels() {
		return pausedLevels;
	}
	
	public final int getPausedMatchLevel() {
		return pausedMatchLevel;
	}
	
	public final void setPaused(Set<String> levels, int matchLevel) {
		if(matchLevel < 0) {
			pausedMatchLevel = -1;
			pausedLevels = null;
		}
		else {
			pausedMatchLevel = matchLevel;
			pausedLevels = levels;
			if(pausedLevels == null) {
				pausedLevels = new HashSet<String>();
			}
		}
	}
	
	public final boolean getFilterTeamByLevel() {
		return filterTeamByLevel;
	}
	
	public final void setFilterTeamByLevel(boolean filterTeamByLevel) {
		this.filterTeamByLevel = !levels.isEmpty() && filterTeamByLevel;
	}
	
	public final boolean canStart() {
		return !isStarted() && isValid();
	}
	
	public final boolean isStarted() {
		return started;
	}
	
	public final List<Match> start() {
		if(!canStart()) {
			return null;
		}
		started = true;
		for(Team team : teams) {
			if(team != null) {
				team.setInEvent(true);
			}
		}
		return startEvent();
	}
	
	public final void undo() {
		started = false;
		undoEvent();
		for(Team team : teams) {
			if(team != null) {
				team.setInEvent(false);
				team.setIsWithdrawn(false);
				team.setMatchesLost(0);
				team.setMatchesPlayed(0);
				team.setMatchesWon(0);
			}
		}
		setPaused(null, -1);
	}
	
	/**
	 * Returns a list of event painters for the given level.
	 * @param level the display level of the match you want to draw
	 * @param disablePooling true to draw all the matches together
	 * @return
	 */
	public final List<EventPainter> getEventPainters(String level, boolean disablePooling) {
		List<EventPainter> eventPainters = new ArrayList<EventPainter>();
		List<Match> matches = getMatches(level);
		if(matches == null) {
			return eventPainters;
		}
		if(disablePooling || !canPoolMatches() || matches.size() <= POOL_SIZE) {
			eventPainters.add(getEventPainter(level));
			return eventPainters;
		}
		String levelString = " - ";
		if(showDisplayLevel()) {
			levelString += level + ", ";
		}
		int poolIndex = 1;
		while(matches.size() > POOL_SIZE) {
			int inc = (int) (Math.log(POOL_SIZE) / Math.log(2.0));
			List<Match> nextMatches = new ArrayList<Match>(matches);
			for(int i = 0; i < inc; ++i) {
				nextMatches = EventUtils.getWinnerMatches(nextMatches);
			}
			int poolCount = matches.size() / POOL_SIZE;
			for(int i = 0; i < poolCount; ++i) {
				if(nextMatches.get(i).getIgnoreMatch()) {
					continue;
				}
				eventPainters.add(getEventPainter(levelString + "Pool " + poolIndex++, level, matches.subList(i * POOL_SIZE, (i + 1) * POOL_SIZE), false));
			}
			matches = nextMatches;
		}
		eventPainters.add(getEventPainter(levelString + "Finals Pool", level, matches, true));
		return eventPainters;
	}
	
	/**
	 * Returns the name of the pool the match is in.
	 * @param match
	 * @return
	 */
	public final String getPoolName(Match match, boolean disablePooling) {
		if(match == null) {
			return null;
		}
		List<Match> matches = getMatches(match.getLevel());
		if(disablePooling || !canPoolMatches() || matches.size() <= POOL_SIZE) {
			return getName() + (showDisplayLevel() ? " - " + match.getLevel() : "");
		}
		String poolName = getName() + " - ";
		if(showDisplayLevel()) {
			poolName += match.getLevel() + ", ";
		}
		int poolIndex = 1;
		while(matches.size() > POOL_SIZE) {
			int inc = (int) (Math.log(POOL_SIZE) / Math.log(2.0));
			List<Match> nextMatches = new ArrayList<Match>(matches);
			for(int i = 0; i < inc; ++i) {
				nextMatches = EventUtils.getWinnerMatches(nextMatches);
			}
			int poolCount = matches.size() / POOL_SIZE;
			for(int i = 0; i < poolCount; ++i) {
				if(nextMatches.get(i).getIgnoreMatch()) {
					continue;
				}
				List<Match> pool = matches.subList(i * POOL_SIZE, (i + 1) * POOL_SIZE);
				while(pool.size() > 1) {
					for(Match cur : pool) {
						if(match.equals(cur)) {
							return poolName + "Pool " + poolIndex;
						}
					}
					pool = EventUtils.getWinnerMatches(pool);
				}
				++poolIndex;
			}
			matches = nextMatches;
		}
		return poolName + "Finals Pool";
	}
	
	public Set<Match> getAllMatches() {
		Set<Match> matches = new HashSet<Match>();
		if(!started) {
			return matches;
		}
		for(String level : getDisplayLevels()) {
			matches.addAll(EventUtils.getAllMatches(getMatches(level), false));
		}
		return matches;
	}
	
	public boolean showDisplayLevel() {
		return displayLevels.size() > 1;
	}
	
	/**
	 * Returns if the matches in this event can be broken up and displayed in different event painters.
	 * Events that don't use single elimination style brackets should return false.
	 * @return
	 */
	public boolean canPoolMatches() {
		return true;
	}
	
	protected EventPainter getEventPainter(String level) {
		return new SingleEliminationEventPainter(this, level);
	}
	
	protected EventPainter getEventPainter(String description, String level, List<Match> matches, boolean isFinals) {
		return new SingleEliminationEventPainter(this, description, matches);
	}
	
	protected String generateSingleDisplayLevelString() {
		String levelString = "";
		for(String level : levels) {
			levelString += level + ", ";
		}
		if(levelString.isEmpty()) {
			levelString = "All";
		}
		else {
			levelString = levelString.substring(0, levelString.length() - 2);
		}
		return levelString;
	}
	
	// for backwards compatibility
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		in.defaultReadObject();
		// check to see if we are dealing with an old tournament file
		if(teamType != null) {
			teamFilter = teamType.newInstance();
			teamType = null;
		}
		if(displayLevels == null) {
			displayLevels = generateDisplayLevels(levels);
		}
	}
	
	// this returns a list of all the winners for the event at the given level
	public abstract EventResult getWinners(String level);
	// this returns a list of all the matches for the event at the given level
	public abstract List<Match> getMatches(String level);
	// this should be called to start the event. it will handle any initial calculations and return the list of available matches
	protected abstract List<Match> startEvent();
	// this should be called to undo an event
	protected abstract void undoEvent();
	// this returns the list of strings for visually representing levels
	protected abstract List<String> generateDisplayLevels(List<String> levels);
}
