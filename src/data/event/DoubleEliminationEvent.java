package data.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import data.event.painter.DoubleEliminationWinnersEventPainter;
import data.event.painter.EventPainter;
import data.event.painter.SingleEliminationEventPainter;
import data.event.result.DoubleEliminationEventResult;
import data.event.result.EmptyEventResult;
import data.event.result.EventResult;
import data.match.DoubleEliminationMatch;
import data.match.Match;
import data.team.Team;

@CreatableEvent(displayName = "Double Elimination")
public class DoubleEliminationEvent extends Event {
	private static final long serialVersionUID = -2588632747474817366L;
	private static final String WINNERS = "Winners";
	private static final String LOSERS = "Losers";
	private Map<String, List<Match>> matches;

	public DoubleEliminationEvent(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
		matches = new HashMap<String, List<Match>>();
		double log2 = Math.log(numberOfTeams) / Math.log(2.0);
		if(numberOfTeams < 2 || log2 > Math.floor(log2)) {
			throw new IllegalArgumentException("The number of teams for this event must be at least 2 and a power of 2.");
		}
	}

	public EventResult getWinners(String level) {
		int index = getDisplayLevels().indexOf(level);
		if(index == -1) {
			return null;
		}
		if(index == 0) {
			return new DoubleEliminationEventResult(this);
		}
		return new EmptyEventResult();
	}

	public List<Match> getMatches(String level) {
		if(WINNERS.equals(level)) {
			level = getDisplayLevels().get(0);
		}
		if(LOSERS.equals(level)) {
			level = getDisplayLevels().get(1);
		}
		List<Match> list = matches.get(level);
		if(list == null) {
			return null;
		}
		return Collections.unmodifiableList(list);
	}
	
	public Match generateMatch(int minScore, int maxScore, int winBy, int bestOf) {
		return new DoubleEliminationMatch(minScore, maxScore, winBy, bestOf);
	}

	protected EventPainter getEventPainter(String level) {
		int index = getDisplayLevels().indexOf(level);
		if(index == -1) {
			return null;
		}
		if(index == 0) {
			return new DoubleEliminationWinnersEventPainter(this, level);
		}
		return new SingleEliminationEventPainter(this, level);
	}
	
	protected EventPainter getEventPainter(String description, String level, List<Match> matches, boolean isFinals) {
		int index = getDisplayLevels().indexOf(level);
		if(index == -1) {
			return null;
		}
		if(index == 0 && isFinals) {
			return new DoubleEliminationWinnersEventPainter(this, description, matches);
		}
		return new SingleEliminationEventPainter(this, description, matches);
	}

	protected List<Match> startEvent() {
		List<String> levels = getDisplayLevels();
		matches.put(levels.get(0), generateWinnersBracket());
		matches.put(levels.get(1), EventUtils.createSingleEliminationBracket(Math.max((int) (Math.pow(getNumberOfTeams() / 2, 2) / 2), 1), this, LOSERS));
		setLoserDropDowns();
		EventUtils.setTeamsInMatches(getTeams(), matches.get(levels.get(0)));
		return getMatches(levels.get(0));
	}

	protected void undoEvent() {
		matches.clear();
	}

	protected List<String> generateDisplayLevels(List<String> levels) {
		List<String> displayLevels = new ArrayList<String>();
		String levelString = generateSingleDisplayLevelString();
		displayLevels.add("Winners (" + levelString + ")");
		displayLevels.add("Losers (" + levelString + ")");
		return displayLevels;
	}
	
	private List<Match> generateWinnersBracket() {
		// create the final matches between the winners of the winners and losers brackets
		Match finals = createMatch(WINNERS);
		finals.setMatchLevel(1);
		finals.setMatchDescription(EventUtils.FINALS);
		Match winnerFinals = createMatch(WINNERS);
		winnerFinals.setMatchLevel(1);
		winnerFinals.setMatchDescription(EventUtils.FINALS);
		winnerFinals.setWinnerMatch(finals, true);
		winnerFinals.addLoserMatch("1", finals);
		// create the rest of the winner's bracket
		List<Match> matches = new ArrayList<Match>();
		Match semiFinals = createMatch(WINNERS);
		semiFinals.setMatchLevel(2);
		semiFinals.setMatchDescription(EventUtils.SEMI_FINALS);
		semiFinals.setWinnerMatch(winnerFinals, true);
		matches.add(semiFinals);
		if(getNumberOfTeams() == 2) {
			return matches;
		}
		int index = 2;
		while(matches.size() < getNumberOfTeams() / 2) {
			List<Match> currentMatches = matches;
			matches = new ArrayList<Match>();
			for(Match match : currentMatches) {
				Match m1 = createMatch(WINNERS);
				Match m2 = createMatch(WINNERS);
				m1.setMatchLevel(index + 1);
				m2.setMatchLevel(index + 1);
				if(index == 2) {
					m1.setMatchDescription(EventUtils.QUARTER_FINALS);
					m2.setMatchDescription(EventUtils.QUARTER_FINALS);
				}
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
	
	private void setLoserDropDowns() {
		// we want to work backwards so we need to find the finals match for the winners and the losers
		List<Match> winners = getMatches(getDisplayLevels().get(0));
		while(winners.size() > 1) {
			winners = EventUtils.getWinnerMatches(winners);
		}
		List<Match> losers = getMatches(getDisplayLevels().get(1));
		while(losers.size() > 1) {
			losers = EventUtils.getWinnerMatches(losers);
		}
		// set the loser's finals match to forward to the winner's final match
		losers.get(0).setWinnerMatch(winners.get(0).getWinnerMatch(), false);
		if(getNumberOfTeams() == 2) {
			winners.get(0).addLoserMatch("1", losers.get(0));
			return;
		}
		// traverse the bracket backwards
		while(!losers.isEmpty()) {
			// add drops from winners to losers and then move to the next set of matches
			ArrayList<Match> preLosers = new ArrayList<Match>();
			for(int i = 0; i < winners.size(); ++i) {
				Match match = winners.get(i);
				Match loser = losers.get(i);
				match.addLoserMatch("1", loser);
				loser.getDefaultToTeam1Match().setIgnoreMatch(true);
				preLosers.add(loser.getDefaultToTeam2Match());
			}
			winners = EventUtils.getDefaultFeederMatches(winners);
			losers = EventUtils.getDefaultFeederMatches(preLosers);
			if(losers.isEmpty()) {
				// this should only happen once we've hit the initial matches
				for(int i = 0; i < winners.size(); ++i) {
					Match match = winners.get(i);
					match.addLoserMatch("1", preLosers.get(i / 2));
				}
			}
		}
	}
}
