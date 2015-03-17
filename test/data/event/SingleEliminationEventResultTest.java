package data.event;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import data.event.result.EventResult;
import data.match.Match;
import data.player.Player;
import data.team.Team;
import data.team.singles.OnePlayerTeam;

public class SingleEliminationEventResultTest {
	@Test
	public void test() {
		SingleEliminationEvent event = new SingleEliminationEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 8, 21, 30, 2, 1);
		List<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < 8; ++i) {
			OnePlayerTeam team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));
			teams.add(team);
		}
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		assertEquals(matches.size(), 4);
		assertFalse(event.isComplete("A"));
		matches = new ArrayList<Match>();
		for(Match match : event.getMatches("A")) {
			match.setTeam1Forfeit(true, false);
			Match winnerMatch = match.finish().iterator().next();
			if(!matches.contains(winnerMatch)) {
				matches.add(winnerMatch);
			}
			assertEquals(match.getMatchDescription(), EventUtils.QUARTER_FINALS);
		}
		assertEquals(matches.size(), 2);
		assertEquals(matches.get(0).getMatchDescription(), EventUtils.SEMI_FINALS);
		matches.get(0).setTeam1Forfeit(true, false);
		matches.get(0).finish();
		matches.get(1).setTeam2Forfeit(true, false);
		matches.get(1).finish();
		Match finals = matches.get(0).getWinnerMatch();
		assertEquals(finals.getMatchDescription(), EventUtils.FINALS);
		assertEquals(finals.getTeam1().getPlayer(0).getName(), "3");
		assertEquals(finals.getTeam2().getPlayer(0).getName(), "5");
		finals.setTeam1Forfeit(true, true);
		assertEquals(finals.finish().size(), 0);
		assertEquals(finals.getWinner().getPlayer(0).getName(), "5");
		assertTrue(event.isComplete("A"));
		EventResult result = event.getWinners("A");
		assertEquals(result.getWinners(0).size(), 1);
		assertEquals(result.getWinners(1).size(), 1);
		assertEquals(result.getWinners(2).size(), 2);
		assertEquals(result.getWinners(3).size(), 2);
		assertEquals(result.getWinners(4).size(), 4);
		assertEquals(result.getWinners(5).size(), 4);
		assertEquals(result.getWinners(6).size(), 4);
		assertEquals(result.getWinners(7).size(), 4);
		assertEquals(result.getWinners(0).get(0).getPlayer(0).getName(), "5");
		assertEquals(result.getWinners(1).get(0).getPlayer(0).getName(), "3");
		assertEquals(result.getWinners(2).get(0).getPlayer(0).getName(), "1");
		assertEquals(result.getWinners(2).get(1).getPlayer(0).getName(), "7");
		assertEquals(result.getWinners(2), result.getWinners(3));
		assertEquals(result.getWinners(4).get(0).getPlayer(0).getName(), "0");
		assertEquals(result.getWinners(4).get(1).getPlayer(0).getName(), "2");
		assertEquals(result.getWinners(4).get(2).getPlayer(0).getName(), "4");
		assertEquals(result.getWinners(4).get(3).getPlayer(0).getName(), "6");
		assertEquals(result.getWinners(4), result.getWinners(5));
		assertEquals(result.getWinners(4), result.getWinners(6));
		assertEquals(result.getWinners(4), result.getWinners(7));
	}
	
	@Test
	public void test1() {
		SingleEliminationEvent event = new SingleEliminationEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 2, 21, 30, 2, 1);
		List<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < 2; ++i) {
			OnePlayerTeam team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));
			teams.add(team);
		}
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		assertEquals(matches.size(), 1);
		matches.get(0).setTeam1Forfeit(true, false);
		matches.get(0).setTeam2Forfeit(true, false);
		assertEquals(matches.get(0).finish().size(), 0);
		assertTrue(event.isComplete("A"));
		EventResult result = event.getWinners("A");
		assertEquals(result.getWinners(0).size(), 2);
		assertEquals(result.getWinners(1).size(), 2);
		assertEquals(result.getWinners(0), result.getWinners(1));
	}
	
	@Test
	public void test2() {
		SingleEliminationEvent event = new SingleEliminationEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 1);
		List<Team> teams = new ArrayList<Team>();
		teams.add(null);
		teams.add(null);
		for(int i = 2; i < 4; ++i) {
			OnePlayerTeam team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));
			teams.add(team);
		}
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		assertEquals(matches.size(), 2);
		matches.get(0).finish();
		matches.get(1).setTeam2Forfeit(true, false);
		matches.get(1).finish();
		matches.get(0).getWinnerMatch().finish();
		assertTrue(event.isComplete("A"));
		EventResult result = event.getWinners("A");
		assertEquals(result.getWinners(0).get(0).getPlayer(0).getName(), "2");
		assertEquals(result.getWinners(1).get(0).getPlayer(0).getName(), "3");
		assertNull(result.getWinners(2));
	}
}
