package data.event.result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.event.EventUtils;
import data.match.Game;
import data.match.Match;
import data.team.Team;

public class RoundRobinEventResult extends EventResult {
	public RoundRobinEventResult(String description, List<Match> matches) {
		super(description);
		calculateWinners(matches);
	}
	
	public void calculateWinners(List<Match> matches) {
		clearWinners();
		if(matches == null || matches.isEmpty()) {
			return;
		}
		Set<Match> set = EventUtils.getAllMatches(matches);
		// generating the map of teams to results
		HashMap<Team, ResultData> map = new HashMap<Team, ResultData>();
		for(Match match : set) {
			Team winner = match.getWinner();
			Team loser = match.getLoser();
			if(winner == null || loser == null) {
				// is this a double withdraw
				Team t1 = match.getTeam1();
				Team t2 = match.getTeam2();
				if(t1 != null) {
					ResultData data = map.get(t1);
					if(data == null) {
						data = new ResultData(t1);
						map.put(t1, data);
					}
					data.addMatch(match, false);
				}
				if(t2 != null) {
					ResultData data = map.get(t2);
					if(data == null) {
						data = new ResultData(t2);
						map.put(t2, data);
					}
					data.addMatch(match, false);
				}
				// either way, we can ignore this match
				continue;
			}
			ResultData winResult = map.get(winner);
			if(winResult == null) {
				winResult = new ResultData(winner);
				map.put(winner, winResult);
			}
			ResultData loseResult = map.get(loser);
			if(loseResult == null) {
				loseResult = new ResultData(loser);
				map.put(loser, loseResult);
			}
			winResult.addMatch(match, true);
			loseResult.addMatch(match, false);
		}
		// sort by matches won
		// if 2 teams are tied, the team who won their head to head match wins
		// if more than 2 teams are tied, sort by total games won - total games lost
		// if 2 teams are tied at this point, sort by the head to head matchup
		// if more than 2 teams are tied, sort by total points won - total points lost
		// if 2 teams are tied at this point, sort by the head to head matchup
		// if more than 2 teams are tied, they will remain tied
		Comparator<ResultData> matchComp = new Comparator<ResultData>() {
			public int compare(ResultData r1, ResultData r2) {
				return r2.matchesWon - r1.matchesWon;
			}
		};
		Comparator<ResultData> gameComp = new Comparator<ResultData>() {
			public int compare(ResultData r1, ResultData r2) {
				return (r2.gamesWon - (r2.gamesPlayed - r2.gamesWon)) - (r1.gamesWon - (r1.gamesPlayed - r1.gamesWon));
			}
		};
		Comparator<ResultData> pointComp = new Comparator<ResultData>() {
			public int compare(ResultData r1, ResultData r2) {
				return (r2.pointsWon - (r2.pointsPlayed - r2.pointsWon)) - (r1.pointsWon - (r1.pointsPlayed - r1.pointsWon));
			}
		};
		// generating the list of winners
		for(List<ResultData> results : sort(map.values(), matchComp)) {
			if(results.size() == 1) {
				addWinners(Arrays.asList(results.get(0).team));
				continue;
			}
			for(List<ResultData> gameTiebreak : sort(results, gameComp)) {
				if(gameTiebreak.size() == 1) {
					addWinners(Arrays.asList(gameTiebreak.get(0).team));
					continue;
				}
				for(List<ResultData> pointTiebreak : sort(gameTiebreak, pointComp)) {
					ArrayList<Team> currentWinners = new ArrayList<Team>();
					for(ResultData resultData : pointTiebreak) {
						currentWinners.add(resultData.team);
					}
					addWinners(currentWinners);
				}
			}
		}
	}
	
	private List<List<ResultData>> sort(Collection<ResultData> data, Comparator<ResultData> comp) {
		ArrayList<ResultData> list = new ArrayList<ResultData>(data);
		Collections.sort(list, comp);
		int inc = 0;
		ArrayList<List<ResultData>> results = new ArrayList<List<ResultData>>();
		for(int i = 0; i < list.size(); i += inc) {
			ArrayList<ResultData> currentWinners = new ArrayList<ResultData>();
			currentWinners.add(list.get(i));
			for(int j = i + 1; j < list.size() && comp.compare(list.get(i), list.get(j)) == 0; ++j) {
				currentWinners.add(list.get(j));
			}
			inc = 1;
			if(currentWinners.size() == 2) {
				inc = 2;
				// try to do tie breaks based on the head to head matchup result
				ResultData r1 = currentWinners.get(0);
				ResultData r2 = currentWinners.get(1);
				if(r1.teamsBeaten.contains(r2.team)) {
					results.add(Arrays.asList(r1));
					results.add(Arrays.asList(r2));
				}
				else if(r2.teamsBeaten.contains(r1.team)) {
					results.add(Arrays.asList(r2));
					results.add(Arrays.asList(r1));
				}
				else {
					inc = 1;
					results.add(currentWinners);
				}
			}
			else {
				results.add(currentWinners);
			}
		}
		return results;
	}
	
	@SuppressWarnings("unused")
	private class ResultData {
		Team team;		
		int matchesWon, matchesPlayed, gamesWon, gamesPlayed, pointsWon, pointsPlayed;
		Set<Team> teamsBeaten;
		
		public ResultData(Team team) {
			this.team = team;
			teamsBeaten = new HashSet<Team>();
		}
		
		public void addMatch(Match match, boolean isWinner) {
			if(isWinner) {
				teamsBeaten.add(match.getLoser());
				++matchesWon;
			}
			++matchesPlayed;
			for(Game game : match.getGames()) {
				if(game.getWinner() != null) {
					++gamesPlayed;
					pointsPlayed += game.getWinnerScore() + game.getLoserScore();
					if(game.getWinner().equals(team)) {
						++gamesWon;
						pointsWon += game.getWinnerScore();
					}
					else {
						pointsWon += game.getLoserScore();
					}
				}
				else if(game.getTeam1() != null && game.getTeam2() != null && game.getTeam1Score() >= 0 && game.getTeam2Score() >= 0) {
					++gamesPlayed;
					pointsPlayed += game.getTeam1Score() + game.getTeam2Score();
					if(team.equals(game.getTeam1())) {
						pointsWon += game.getTeam1Score();
						if(match.getTeam2Forfeit() && !match.getTeam1Forfeit()) {
							++gamesWon;
						}
					}
					else {
						pointsWon += game.getTeam2Score();
						if(match.getTeam1Forfeit() && !match.getTeam2Forfeit()) {
							++gamesWon;
						}
					}
				}
			}
		}
	}
}
