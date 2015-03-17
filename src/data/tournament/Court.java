package data.tournament;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import data.match.Match;

public class Court implements Serializable {
	private static final long serialVersionUID = 669894440631570195L;
	private String id;
	private Match currentMatch;
	private List<Match> previousMatches;
	private boolean usable;
	
	public Court(String id) {
		this.id = id;
		previousMatches = new ArrayList<Match>();
		usable = true;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	public void setUsable(boolean usable) {
		this.usable = usable;
	}
	
	public boolean isUsable() {
		return usable;
	}

	public boolean isAvailable() {
		return currentMatch == null;
	}

	public Match getCurrentMatch() {
		return currentMatch;
	}
	
	/**
	 * Get the previous matches that played on this court but don't have scores set yet.
	 * @return a modifiable list of the previous matches
	 */
	public List<Match> getPreviousMatches() {
		return previousMatches;
	}

	public void setMatch(Match match) {
		if(currentMatch != null) {
			currentMatch.end();
			previousMatches.add(0, currentMatch);
		}
		currentMatch = match;
		if(currentMatch != null) {
			currentMatch.start();
			currentMatch.setCourt(this);
		}
	}
	
	public boolean undoMatch(Match match) {
		if(match == null) {
			return false;
		}
		if(match.equals(currentMatch)) {
			currentMatch.setCourt(null);
			if(previousMatches.size() > 0) {
				currentMatch = previousMatches.remove(0);
			}
			else {
				currentMatch = null;
			}
			return true;
		}
		int index = previousMatches.indexOf(match);
		if(index != -1) {
			previousMatches.remove(index).setCourt(null);
			return true;
		}
		return false;
	}
	
	public boolean swapCurrentMatch(Court court) {
		if(court == null || equals(court)) {
			return false;
		}
		Match m1 = currentMatch;
		Match m2 = court.currentMatch;
		currentMatch = m2;
		court.currentMatch = m1;
		if(currentMatch != null) {
			currentMatch.setCourt(this);
		}
		if(court.currentMatch != null) {
			court.currentMatch.setCourt(court);
		}
		return true;
	}
}
