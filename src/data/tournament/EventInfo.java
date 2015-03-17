package data.tournament;

import java.util.List;
import java.util.Set;

import data.event.EventUtils;
import data.match.Match;

public class EventInfo {
	private int complete;
	private int total;
	private double percent;
	
	public EventInfo(List<Match> matches) {
		Set<Match> totalMatches = EventUtils.getAllMatches(matches);
		total = Math.max(totalMatches.size(), 1);
		complete = 0;
		for(Match match : totalMatches) {
			if(match.isComplete()) {
				++complete;
			}
		}
		percent = Math.round(((float) complete / (float) total) * 10000) / 100.0;
	}
	
	public int getNumberOfMatches() {
		return total;
	}
	
	public int getNumberOfCompleteMatches() {
		return complete;
	}
	
	public double getPercentComplete() {
		return percent;
	}
}
