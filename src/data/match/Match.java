package data.match;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ui.main.TournamentViewManager;
import data.event.Event;
import data.player.Player;
import data.team.Team;
import data.tournament.Court;
import data.tournament.TournamentUtils;

public class Match implements Serializable {
	private static final long serialVersionUID = -1820147720593874501L;
	public static final String TBD = "TBD";
	private transient Date estimatedDate;
	private transient boolean recalculating;
	private int index;
	private int matchLevel;
	private String level, matchDescription;
	private Event event;
	private Team t1, t2, winner, loser;
	private ProxyMatch proxyMatch; // this is used to handle the relationship between matches
	private Match winnerMatch, mirrorMatch;
	private Map<String, List<Match>> potentialLoserMatches;
	private Date start, end, requestedDate;
	private List<Game> games;
	private Court court;
	private boolean complete;
	private Team oldT1, oldT2;
	private int t1MatchesPlayed, t1MatchesWon, t1MatchesLost;
	private int t2MatchesPlayed, t2MatchesWon, t2MatchesLost;
	private Match selectedLoserMatch;
	private boolean t1Forfeit, t2Forfeit;
	private int nextAvailableCourtOrder;
	private boolean setT1Forfeit, setT1Withdrawal;
	private boolean setT2Forfeit, setT2Withdrawal;
	private boolean ignoreMatch;
	
	// these fields are here to maintain backwards compatibility
	private Map<String, Match> loserMatches;
	
	public Match(int minScore, int maxScore, int winBy, int bestOf) {
		if(minScore < 0 || maxScore < minScore || winBy < 1 || bestOf < 1 || (t1 == t2 && t1 != null)) {
			throw new RuntimeException("Invalid paramters entered.");
		}
		index = TournamentUtils.getNextMatchIndex();
		games = new ArrayList<Game>();
		for(int i = 0; i < bestOf; ++i) {
			games.add(generateGame(minScore, maxScore, winBy, bestOf, i));
		}
		potentialLoserMatches = new HashMap<String, List<Match>>();
		proxyMatch = new ProxyMatch(this);
	}
	
	public Match(Team t1, Team t2, int minScore, int maxScore, int winBy, int bestOf) {
		this(minScore, maxScore, winBy, bestOf);
		setTeam1(t1);
		setTeam2(t2);
	}
	
	// subclasses should override this function to change the type of game being returned
	public Game generateGame(int minScore, int maxScore, int winBy, int bestOf, int gameIndex) {
		return new Game(minScore, maxScore, winBy);
	}
	
	// this function should only be called after the match is complete
	public Set<Match> recalculate() {
		// return null if we don't have an end date
		if(end == null) {
			return null;
		}
		// save all the old values
		Match oldLoserMatch = selectedLoserMatch;
		Team oldWinnerTeam = winner;
		Team oldLoserTeam = loser;
		int oldWinnerMatchesPlayed = -1;
		if(winnerMatch != null && winnerMatch.isComplete()) {
			if(equals(winnerMatch.getToTeam1Match(true))) {
				oldWinnerMatchesPlayed = winnerMatch.t1MatchesPlayed;
			}
			else {
				oldWinnerMatchesPlayed = winnerMatch.t2MatchesPlayed;
			}
		}
		Team loserTeam1 = null;
		Team loserTeam2 = null;
		if(selectedLoserMatch != null) {
			loserTeam1 = selectedLoserMatch.getTeam1();
			loserTeam2 = selectedLoserMatch.getTeam2();
		}
		// reset the match status and the team statuses
		complete = false;
		recalculating = true;
		if(winnerMatch != null) {
			winnerMatch.proxyMatch.resetFromMatch(this);
		}
		if(selectedLoserMatch != null) {
			selectedLoserMatch.proxyMatch.resetFromMatch(this);
		}
		// reset the match
		if(oldT1 != null && oldT1.canResetMatch()) {
			oldT1.setMatchesPlayed(t1MatchesPlayed);
			oldT1.setMatchesWon(t1MatchesWon);
			oldT1.setMatchesLost(t1MatchesLost);
			oldT1.setResetMatch(true);
		}
		if(oldT2 != null && oldT2.canResetMatch()) {
			oldT2.setMatchesPlayed(t2MatchesPlayed);
			oldT2.setMatchesWon(t2MatchesWon);
			oldT2.setMatchesLost(t2MatchesLost);
			oldT2.setResetMatch(true);
		}
		// return the other matches which need to be recalculated
		Set<Match> matches = finish();
		if(matches == null) {
			return null;
		}
		if(oldWinnerTeam == winner && winnerMatch != null && (oldWinnerMatchesPlayed == -1 || winner.getMatchesPlayed() == oldWinnerMatchesPlayed)) {
			matches.remove(winnerMatch);
		}
		if(oldLoserTeam == loser && selectedLoserMatch != null && oldLoserMatch == selectedLoserMatch) {
			if(selectedLoserMatch.getTeam1() == loserTeam1 && selectedLoserMatch.getTeam2() == loserTeam2) {
				matches.remove(selectedLoserMatch);
			}
		}
		else if(oldLoserMatch != null) {
			matches.add(oldLoserMatch);
		}
		recalculating = false;
		return matches;
	}
	
	public boolean isRecalculating() {
		return recalculating;
	}
	
	public Set<Match> finish() {
		// return null if the match isn't complete
		Set<Match> updatedMatches = new HashSet<Match>();
		if(!isComplete()) {
			if(t1 != null && setT1Forfeit) {
				t1Forfeit = true;
			}
			if(t2 != null && setT2Forfeit) {
				t2Forfeit = true;
			}
			if(calculateResult()) {
				complete = true;
				if(t1 != null && setT1Forfeit && setT1Withdrawal) {
					t1.setIsWithdrawn(true);
				}
				if(t2 != null && setT2Forfeit && setT2Withdrawal) {
					t2.setIsWithdrawn(true);
				}
			}
			else {
				if(t1 != null && setT1Forfeit) {
					t1Forfeit = false;
				}
				if(t2 != null && setT2Forfeit) {
					t2Forfeit = false;
				}
				oldT1 = null;
				oldT2 = null;
				return null;
			}
		}
		else {
			return updatedMatches;
		}
		// keep track of the current stats in case we have to recalculate the match later
		oldT1 = t1;
		if(t1 != null) {
			t1MatchesPlayed = t1.getMatchesPlayed();
			t1MatchesWon = t1.getMatchesWon();
			t1MatchesLost = t1.getMatchesLost();
		}
		oldT2 = t2;
		if(t2 != null) {
			t2MatchesPlayed = t2.getMatchesPlayed();
			t2MatchesWon = t2.getMatchesWon();
			t2MatchesLost = t2.getMatchesLost();
		}
		// updating the winner and checking to see where they should be going next
		if(winner != null) {
			// don't count this as a won match if it was a bye
			if(loser != null) {
				winner.setMatchesWon(winner.getMatchesWon() + 1);
			}
			// only increment the matches played if the winner actually got to play
			if(shouldCountMatch(loser, t1 == winner ? getTeam2Forfeit() : getTeam1Forfeit())) {
				winner.setMatchesPlayed(winner.getMatchesPlayed() + 1);
			}
		}
		if(this.winnerMatch != null) {
			if(this.winnerMatch.proxyMatch.forwardTeam(this, winner, shouldForwardTeam(this, winner))) {
				updatedMatches.add(this.winnerMatch);
			}
			else {
				throw new RuntimeException("Unable to forward the winner team");
			}
		}
		// updating the loser and checking to see where they should be going next
		selectedLoserMatch = null;
		if(loser != null) {
			loser.setMatchesPlayed(loser.getMatchesPlayed() + 1);
			loser.setMatchesLost(loser.getMatchesLost() + 1);
			List<Match> potentialMatches = potentialLoserMatches.get(getLoserMatchId(loser, 0, 0));
			if(potentialMatches != null) {
				for(Match loserMatch : potentialMatches) {
					if(loserMatch.proxyMatch.forwardTeam(this, loser, shouldForwardTeam(this, loser))) {
						selectedLoserMatch = loserMatch;
						break;
					}
				}
				if(selectedLoserMatch != null) {
					updatedMatches.add(selectedLoserMatch);
				}
				else {
					throw new RuntimeException("Unable to forward the loser team");
				}
			}
		}
		// grabbing all the non-complete potential loser matches that aren't playing
		for(List<Match> list : potentialLoserMatches.values()) {
			for(Match match : list) {
				if(!match.isComplete() && match.getStart() == null) {
					updatedMatches.add(match);
				}
			}
		}
		return updatedMatches;
	}
	
	public boolean canStartMatch() {
		if(t1 == null || t2 == null || t1.equals(t2) || isComplete() || !Collections.disjoint(t1.getPlayers(), t2.getPlayers())) {
			return false;
		}
		return t1.canStartMatch() && t2.canStartMatch();
	}
	
	public List<Game> getGames() {
		return Collections.unmodifiableList(games);
	}
	
	public List<Player> getPlayers() {
		ArrayList<Player> players = new ArrayList<Player>();
		if(getTeam1() != null) {
			players.addAll(getTeam1().getPlayers());
		}
		if(getTeam2() != null) {
			players.addAll(getTeam2().getPlayers());
		}
		return players;
	}

	public Date getStart() {
		return start;
	}

	public boolean start() {
		if(!canStartMatch()) {
			return false;
		}
		start = new Date();
		t1.startMatch();
		t2.startMatch();
		return true;
	}

	public Date getEnd() {
		return end;
	}

	public void end() {
		end = new Date();
		// if either team is null or if we had to forfeit/withdrawal a team on start, that means this match was a bye
		if(t1 != null && t2 != null && !setT1Forfeit && !setT2Forfeit) {
			t1.endMatch(end);
			t2.endMatch(end);
		}
		nextAvailableCourtOrder = 0;
		requestedDate = null;
		setT1Forfeit = false;
		setT1Withdrawal = false;
		setT2Forfeit = false;
		setT2Withdrawal = false;
	}
	
	public int getIndex() {
		return index;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}

	public Court getCourt() {
		return court;
	}

	public void setCourt(Court court) {
		this.court = court;
		if(court == null) {
			// this means we are undoing the match
			reset();
			if(t1 != null && start != null && end == null) {
				t1.endMatch(null);
			}
			if(t2 != null && start != null && end == null) {
				t2.endMatch(null);
			}
			start = null;
			end = null;
		}
	}
	
	public void reset() {
		for(Game game : games) {
			game.setTeam1Score(-1);
			game.setTeam2Score(-1);
		}
		setTeam1Forfeit(false, false);
		setTeam2Forfeit(false, false);
	}
	
	/**
	 * @return Description of the match we are getting team 1 from.
	 */
	public String getToTeam1MatchDescription() {
		return getMatchDescription(getToTeam1Match(false));
	}
	
	public Team getTeam1() {
		return t1;
	}
	
	public void setTeam(Team team, boolean setToT1) {
		if(setToT1) {
			setTeam1(team);
		}
		else {
			setTeam2(team);
		}
	}
	
	public void setTeam1(Team team) {
		if(team != null && team.equals(t2)) {
			throw new RuntimeException("invalid team");
		}
		if(t1 != null && start != null && end == null) {
			t1.endMatch(null);
		}
		t1 = team;
		if(t1 != null && start != null && end == null) {
			t1.startMatch();
		}
		for(Game game : games) {
			game.setTeam1(t1);
		}
	}
	
	/**
	 * @return Description of the match we are getting team 2 from.
	 */
	public String getToTeam2MatchDescription() {
		return getMatchDescription(getToTeam2Match(false));
	}
	
	private String getMatchDescription(Match match) {
		if(match == null || match.isComplete()) {
			return TournamentViewManager.BYE;
		}
		String desc;
		if(equals(match.getWinnerMatch())) {
			desc = "W";
		}
		else {
			desc = "L";
		}
		if(match.getCourt() != null) {
			desc += "C" + match.getCourt().getId();
		}
		else {
			desc += match.getIndex();
		}
		return desc;
	}
	
	public Team getTeam2() {
		return t2;
	}
	
	public void setTeam2(Team team) {
		if(team != null && team.equals(t1)) {
			throw new RuntimeException("invalid team");
		}
		if(t2 != null && start != null && end == null) {
			t2.endMatch(null);
		}
		t2 = team;
		if(t2 != null && start != null && end == null) {
			t2.startMatch();
		}
		for(Game game : games) {
			game.setTeam2(t2);
		}
	}
	
	public String getToTeam1MatchLongDiscription(String description) {
		return getMatchLongDescription(getToTeam1Match(false), (description == null || description.isEmpty()) ? ' ' : description.charAt(0));
	}
	
	public String getToTeam2MatchLongDiscription(String description) {
		return getMatchLongDescription(getToTeam2Match(false), (description == null || description.isEmpty()) ? ' ' : description.charAt(0));
	}
	
	private String getMatchLongDescription(Match match, char type) {
		if(match == null || match.isComplete()) {
			return TournamentViewManager.BYE;
		}
		String value = "Waiting for " + (type == 'W' ? "winner" : "loser") + " of " + match.toString();
		if(match.getCourt() != null) {
			value += " (Court " + match.getCourt().getId() + ")";
		}
		return value + ".";
	}

	public Match getToTeam1Match(boolean ignorePotentialMatches) {
		return proxyMatch.getToTeam1Match(ignorePotentialMatches);
	}
	
	public Match getDefaultToTeam1Match() {
		return proxyMatch.getDefaultMatch(true);
	}

	public Match getToTeam2Match(boolean ignorePotentialMatches) {
		return proxyMatch.getToTeam2Match(ignorePotentialMatches);
	}
	
	public Match getDefaultToTeam2Match() {
		return proxyMatch.getDefaultMatch(false);
	}
	
	public List<Match> getRelatedMatches() {
		List<Match> matches = new ArrayList<Match>();
		if(t1 == null || t2 == null) {
			if(t1 == null) {
				Match match = getToTeam1Match(false);
				if(match != null) {
					matches.addAll(match.getRelatedMatches());
				}
			}
			if(t2 == null) {
				Match match = getToTeam2Match(false);
				if(match != null) {
					matches.addAll(match.getRelatedMatches());
				}
			}
		}
		matches.add(this);
		return matches;
	}
	
	// override this function for matches that need different processing to determine where the loser goes
	public String getLoserMatchId(Team team, int numberOfAdditionalWins, int numberOfAdditionalLosses) {
		if(team == null) {
			return null;
		}
		return Integer.toString((team.getMatchesPlayed() + numberOfAdditionalWins + numberOfAdditionalLosses) - (team.getMatchesLost() + numberOfAdditionalLosses));
	}

	public Map<String, List<Match>> getLoserMatches() {
		return Collections.unmodifiableMap(potentialLoserMatches);
	}

	public void addLoserMatch(String id, Match loserMatch) {
		if(loserMatch == null) {
			return;
		}
		List<Match> matches = potentialLoserMatches.get(id);
		if(matches == null) {
			matches = new ArrayList<Match>();
			potentialLoserMatches.put(id, matches);
		}
		if(!matches.contains(loserMatch)) {
			matches.add(loserMatch);
			loserMatch.proxyMatch.addMatch(id, this);
		}
	}

	public Match getMirrorMatch() {
		return mirrorMatch;
	}

	public void setMirrorMatch(Match mirrorMatch) {
		this.mirrorMatch = mirrorMatch;
		mirrorMatch.mirrorMatch = this;
	}
	
	public Match getWinnerMatch() {
		return winnerMatch;
	}

	public void setWinnerMatch(Match winnerMatch, boolean setToT1) {
		this.winnerMatch = winnerMatch;
		winnerMatch.proxyMatch.setToTeamXMatch(this, setToT1);
	}
	
	public boolean getIgnoreMatch() {
		return ignoreMatch;
	}
	
	public void setIgnoreMatch(boolean ignoreMatch) {
		this.ignoreMatch = ignoreMatch;
		Match match = getDefaultToTeam1Match();
		if(match != null) {
			match.setIgnoreMatch(ignoreMatch);
		}
		match = getDefaultToTeam2Match();
		if(match != null) {
			match.setIgnoreMatch(ignoreMatch);
		}
	}

	public String getMatchDescription() {
		return matchDescription;
	}

	public void setMatchDescription(String matchDescription) {
		this.matchDescription = matchDescription;
	}
	
	public String getLevel() {
		return level;
	}
	
	public void setLevel(String level) {
		this.level = level;
	}
	
	public Event getEvent() {
		return event;
	}
	
	public void setEvent(Event event) {
		this.event = event;
	}
	
	public boolean isComplete() {
		return complete || ignoreMatch;
	}
	
	public Team getWinner() {
		return winner;
	}
	
	public Team getLoser() {
		return loser;
	}
	
	public void setTeam1Forfeit(boolean forfeit, boolean withdrawTeam) {
		t1Forfeit = forfeit;
		if(t1 != null) {
			if(forfeit) {
				t1.setIsWithdrawn(withdrawTeam);
			}
			else {
				t1.setIsWithdrawn(false);
			}
		}
	}
	
	public boolean getTeam1Forfeit() {
		return t1Forfeit;
	}
	
	public void setT1OnStart(boolean forfeit, boolean withdrawal) {
		setT1Forfeit = forfeit;
		setT1Withdrawal = withdrawal;
	}
	
	public boolean getT1ForfeitOnStart() {
		return setT1Forfeit;
	}
	
	public boolean getT1WithdrawalOnStart() {
		return setT1Withdrawal;
	}
	
	public void setTeam2Forfeit(boolean forfeit, boolean withdrawTeam) {
		t2Forfeit = forfeit;
		if(t2 != null) {
			if(forfeit) {
				t2.setIsWithdrawn(withdrawTeam);
			}
			else {
				t2.setIsWithdrawn(false);
			}
		}
	}
	
	public boolean getTeam2Forfeit() {
		return t2Forfeit;
	}
	
	public void setT2OnStart(boolean forfeit, boolean withdrawal) {
		setT2Forfeit = forfeit;
		setT2Withdrawal = withdrawal;
	}
	
	public boolean getT2ForfeitOnStart() {
		return setT2Forfeit;
	}
	
	public boolean getT2WithdrawalOnStart() {
		return setT2Withdrawal;
	}
	
	public int getMatchLevel() {
		return matchLevel;
	}
	
	public void setMatchLevel(int matchLevel) {
		if(matchLevel < 0) {
			matchLevel = 0;
		}
		this.matchLevel = matchLevel;
	}
	
	// override this function for matches that need a different method to calculate if they should forward a team
	public boolean shouldForwardTeam(Match match, Team team) {
		if(team == null || team.isWithdrawn()) {
			return false;
		}
		return true;
	}
	
	// override this function for matches that need a different method to calculate if a match should count
	public boolean shouldCountMatch(Team opponent, boolean opponentForfeit) {
		if(opponent == null) {
			return false;
		}
		if(opponent.isWithdrawn()) {
			// count the match if the opponent withdrew, but played part of the match
			for(Game game : games) {
				if(game.getTeam1Score() >= 0 && game.getTeam2Score() >= 0) {
					return true;
				}
			}
			return false;
		}
		return true;
	}
	
	public void setEstimatedDate(Date estimatedDate) {
		if(this.estimatedDate == null || TournamentUtils.compareDates(this.estimatedDate, estimatedDate) > 0) {
			this.estimatedDate = estimatedDate;
		}
	}
	
	public Date getEstimatedDate() {
		return estimatedDate;
	}
	
	public void setRequestedDate(Date requestedDate) {
		this.requestedDate = requestedDate;
	}
	
	public Date getRequestedDate() {
		return requestedDate;
	}
	
	public int getNextAvailableCourtOrder() {
		return nextAvailableCourtOrder;
	}
	
	public void setNextAvailableCourtOrder(int nextAvailableCourtOrder) {
		this.nextAvailableCourtOrder = nextAvailableCourtOrder;
	}
	
	public boolean hasUserAction() {
		return getRequestedDate() != null || getNextAvailableCourtOrder() > 0 || getT1ForfeitOnStart() || getT2ForfeitOnStart();
	}
	
	public String toString() {
		String value = "";
		if(t1 != null) {
			value += t1.getName();
		}
		else {
			value += TBD;
		}
		value += " vs ";
		if(t2 != null) {
			value += t2.getName();
		}
		else {
			value += TBD;
		}
		return value;
	}
	
	private boolean calculateResult() {
		winner = null;
		loser = null;
		// check to see if the values in the games are ok
		boolean hasWinner = true;
		for(Game game : games) {
			if(hasWinner && hasWinner != (game.getWinner() != null)) {
				hasWinner = false;
			}
			else if(!hasWinner && (game.getWinner() != null)) {
				return false;
			}
		}
		// if the teams have not been set yet, the match is not complete
		Match toT1 = getToTeam1Match(false);
		Match toT2 = getToTeam2Match(false);
		if(t1 == null && t2 == null) {
			// test to make sure this isn't a match with 2 byes
			if((toT1 == null || toT1.isComplete()) && (toT2 == null || toT2.isComplete())) {
				return true;
			}
			return false;
		}
		// if there is not a previous match or if it is complete and no team was forwarded, this is a bye
		if(t1 == null) {
			if(toT1 == null || toT1.isComplete()) {
				winner = t2;
				return true;
			}
			return false;
		}
		if(t2 == null) {
			if(toT2 == null || toT2.isComplete()) {
				winner = t1;
				return true;
			}
			return false;
		}
		// this is a double forfeit so the match is complete, but we don't have a winner or a loser
		boolean t1Forfeit = getTeam1Forfeit();
		boolean t2Forfeit = getTeam2Forfeit();
		if(t1Forfeit && t2Forfeit) {
			return true;
		}
		if(t1Forfeit) {
			winner = t2;
			loser = t1;
			return true;
		}
		if(t2Forfeit) {
			winner = t1;
			loser = t2;
			return true;
		}
		int t1Count = 0;
		int t2Count = 0;
		int indexFinished = -1;
		int gamesToWin = (int) Math.ceil(games.size() / 2.0);
		for(int i = 0; i < games.size(); ++i) {
			Game game = games.get(i);
			Team winner = game.getWinner();
			if(winner == t1) {
				++t1Count;
			}
			else if(winner == t2) {
				++t2Count;
			}
			else if(winner == null) {
				if(game.getTeam1Score() >= 0 || game.getTeam2Score() >= 0) {
					indexFinished = -1;
				}
				break;
			}
			if(indexFinished == -1 && (t1Count == gamesToWin || t2Count == gamesToWin)) {
				indexFinished = i;
			}
		}
		if(indexFinished == -1) {
			return false;
		}
		if(t1Count + t2Count != indexFinished + 1) {
			return false;
		}
		if(gamesToWin == t1Count) {
			winner = t1;
			loser = t2;
		}
		else {
			winner = t2;
			loser = t1;
		}
		return true;
	}
	
	// this is to maintain backwards compatibility and to update the match index in tournament utils
	private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
		in.defaultReadObject();
		// always update the match index
		TournamentUtils.updateMatchIndex(index);
		// if potentialLostMatches isn't null, we have a newer object and can skip the conversion
		if(potentialLoserMatches != null) {
			return;
		}
		potentialLoserMatches = new HashMap<String, List<Match>>();
		for(String key : loserMatches.keySet()) {
			potentialLoserMatches.put(key, Arrays.asList(loserMatches.get(key)));
		}
		// reset this field so we don't have duplicate data
		loserMatches = null;
	}
}
