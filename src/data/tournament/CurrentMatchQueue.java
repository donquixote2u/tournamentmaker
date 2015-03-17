package data.tournament;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeSet;

import ui.util.Pair;
import data.match.Match;
import data.player.Player;

/**
 * This class wraps the list of current matches and provides functions for interacting with them.
 * @author Jason Young
 *
 */
public class CurrentMatchQueue {
	private long count;
	private TreeSet<Pair<Match, Long>> matches;
	private HashMap<Match, Date> endDates;
	private int numCourts;
	private Date currentDate;
	private HashSet<Player> playing;
	private HashMap<Player, Date> playerFinishDate;
	private HashSet<Match> playedMatches;
	private Tournament tournament;
	private boolean hasUsableCourts;
	
	public CurrentMatchQueue(Tournament tournament) {
		this.tournament = tournament;
		currentDate = new Date();
		endDates = new HashMap<Match, Date>();
		matches = new TreeSet<Pair<Match, Long>>(new Comparator<Pair<Match, Long>>() {
			public int compare(Pair<Match, Long> p1, Pair<Match, Long> p2) {
				int compare = TournamentUtils.compareDates(endDates.get(p1.getKey()), endDates.get(p2.getKey()));
				if(compare != 0) {
					return compare;
				}
				return p1.getValue().compareTo(p2.getValue());
			}
		});
		playing = new HashSet<Player>();
		playerFinishDate = new HashMap<Player, Date>();
		playedMatches = new HashSet<Match>();
		this.numCourts = -1;
		int numCourts = 0;
		for(Court court : tournament.getCourts()) {
			if(court.isUsable()) {
				++numCourts;
			}
			Match match = court.getCurrentMatch();
			if(match != null) {
				long duration = tournament.getEstimatedDuration(match);
				Date endDate = new Date(match.getStart().getTime() + (duration > 0 ? duration : (tournament.getTimeBetweenMatches() + 1)));
				if(currentDate.after(endDate)) {
					endDate = currentDate;
				}
				add(match, endDate);
			}
			for(Match finishedMatch : court.getPreviousMatches()) {
				playedMatches.add(finishedMatch);
				for(Player player : finishedMatch.getPlayers()) {
					playerFinishDate.put(player, getlastPlayedDate(player));
				}
			}
		}
		hasUsableCourts = numCourts > 0;
		this.numCourts = Math.max(numCourts, 1);
		while(this.numCourts < matches.size()) {
			removeFirstMatch();
		}
	}
	
	public boolean hasUsableCourts() {
		return hasUsableCourts;
	}
	
	public int size() {
		return matches.size();
	}
	
	public int getNumCourts() {
		return numCourts;
	}
	
	public Date getCurrentDate() {
		return currentDate;
	}
	
	public boolean contains(Match match) {
		return endDates.containsKey(match);
	}
	
	public boolean isPlaying(Player player) {
		return playing.contains(player);
	}
	
	public boolean isPlaying(Collection<? extends Player> players) {
		return !Collections.disjoint(playing, players);
	}
	
	public boolean isComplete(Match match) {
		if(match == null) {
			return false;
		}
		return match.isComplete() || playedMatches.contains(match);
	}
	
	public Date getlastPlayedDate(Player player) {
		if(player == null) {
			return null;
		}
		Date date = playerFinishDate.get(player);
		if(date == null) {
			date = player.getLastMatchTime();
		}
		return date;
	}
	
	public Date add(Match match) {
		long duration = tournament.getEstimatedDuration(match);
		Date finishDate = new Date(currentDate.getTime() + (duration > 0 ? duration : (tournament.getTimeBetweenMatches() + 1)));
		add(match, finishDate);
		return finishDate;
	}
	
	public void removeFirstMatch() {
		if(matches.isEmpty()) {
			return;
		}
		Match match = matches.pollFirst().getKey();
		Date endDate = endDates.remove(match);
		playing.removeAll(match.getPlayers());
		if(endDate.after(currentDate)) {
			currentDate = endDate;
		}
		playedMatches.add(match);
	}
	
	public void incrementCurrentDate() {
		currentDate = new Date(currentDate.getTime() + Math.max(tournament.getTimeBetweenMatches(), tournament.getAverageMatchTime()) + 1);
	}
	
	private void add(Match match, Date endDate) {
		if(numCourts == matches.size()) {
			removeFirstMatch();
		}
		for(Player player : match.getPlayers()) {
			playing.add(player);
			playerFinishDate.put(player, endDate);
		}
		endDates.put(match, endDate);
		matches.add(new Pair<Match, Long>(match, count++));
	}
}
