package data.event;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import data.event.result.EventResult;
import data.match.Match;
import data.player.Player;
import data.team.Team;
import data.team.singles.OnePlayerTeam;

public class RoundRobinEventResultTest {
	private Team t1, t2, t3, t4;
	private RoundRobinEvent event;
	
	@Before
	public void setUpBeforeTest() {
		List<String> levels = new ArrayList<String>();
		levels.add("A");
		event = new RoundRobinEvent("Event", levels, new OnePlayerTeam(), 4, 21, 30, 2, 3);
		List<Team> teams = new ArrayList<Team>();
		t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));		
		teams.add(t1);
		t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Jim", true));
		teams.add(t2);
		t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Bob", true));
		teams.add(t3);
		t4 = new OnePlayerTeam();
		t4.setPlayer(0, new Player("Bill", true));
		teams.add(t4);
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
	}
	
	@Test
	public void testMatchComp() {
		// going to set this up so the teams are 3-0, 2-1, 1-2, 0-3.
		List<Match> matches = event.start();
		// set jack to go 3-0
		Match match = matches.get(0);
		match.getGames().get(0).setTeam2Score(11);
		match.getGames().get(1).setTeam2Score(16);
		match = matches.get(1);
		match.getGames().get(0).setTeam2Score(17);
		match.getGames().get(1).setTeam2Score(21);
		match = matches.get(2);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(0);
		// set jim to go 2-1
		match = matches.get(3);
		match.getGames().get(0).setTeam2Score(4);
		match.getGames().get(1).setTeam2Score(19);
		match = matches.get(4);
		match.getGames().get(0).setTeam2Score(21);
		match.getGames().get(1).setTeam2Score(22);
		// set bob to go 1-2
		match = matches.get(5);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(1);
		assertFalse(event.isComplete("B"));
		for(Match cur : matches) {
			cur.finish();
		}
		assertTrue(event.isComplete("A"));
		assertNull(event.getWinners("B"));
		EventResult winners = event.getWinners("A");
		assertNull(winners.getWinners(-1));
		assertNull(winners.getWinners(4));
		assertEquals(winners.getWinners(0).size(), 1);
		assertEquals(winners.getWinners(1).size(), 1);
		assertEquals(winners.getWinners(2).size(), 1);
		assertEquals(winners.getWinners(3).size(), 1);
		assertEquals(winners.getWinners(0).get(0), t1);
		assertEquals(winners.getWinners(1).get(0), t2);
		assertEquals(winners.getWinners(2).get(0), t3);
		assertEquals(winners.getWinners(3).get(0), t4);
	}
	
	@Test
	public void testTiedComp() {
		// going to set this up so team one gets 3-0 and all the other teams get 1-2 in order to test the tiebreakers
		// this one is going to test a completely tied result
		List<Match> matches = event.start();
		// set jack to go 3-0
		Match match = matches.get(0);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		match = matches.get(1);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		match = matches.get(2);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		// set everyone else to 1-2
		match = matches.get(3);
		match.getGames().get(0).setTeam1Score(10);
		match.getGames().get(1).setTeam1Score(10);
		match = matches.get(4);
		match.getGames().get(0).setTeam2Score(10);
		match.getGames().get(1).setTeam2Score(10);
		match = matches.get(5);
		match.getGames().get(0).setTeam1Score(10);
		match.getGames().get(1).setTeam1Score(10);
		for(Match cur : matches) {
			cur.finish();
		}
		assertTrue(event.isComplete("A"));
		EventResult winners = event.getWinners("A");
		assertEquals(winners.getWinners(0).size(), 1);
		assertEquals(winners.getWinners(1).size(), 3);
		assertEquals(winners.getWinners(2).size(), 3);
		assertEquals(winners.getWinners(3).size(), 3);
		assertEquals(winners.getWinners(0).get(0), t1);
		assertEquals(winners.getWinners(1), winners.getWinners(2));
		assertEquals(winners.getWinners(2), winners.getWinners(3));
	}
	
	@Test
	public void testGameComp() {
		// going to set this up so team one gets 3-0 and all the other teams get 1-2 in order to test the tiebreakers
		// this one is going to test the game tiebreaker
		List<Match> matches = event.start();
		// set jack to go 3-0
		Match match = matches.get(0);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		match = matches.get(1);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam1Score(10);
		match.getGames().get(2).setTeam2Score(12);
		match = matches.get(2);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		// set everyone else to 1-2
		match = matches.get(3);
		match.getGames().get(0).setTeam1Score(10);
		match.getGames().get(1).setTeam1Score(10);
		match = matches.get(4);
		match.getGames().get(0).setTeam2Score(10);
		match.getGames().get(1).setTeam2Score(10);
		match = matches.get(5);
		match.getGames().get(0).setTeam1Score(10);
		match.getGames().get(1).setTeam1Score(10);
		for(Match cur : matches) {
			cur.finish();
		}
		assertTrue(event.isComplete("A"));
		EventResult winners = event.getWinners("A");
		assertEquals(winners.getWinners(0).size(), 1);
		assertEquals(winners.getWinners(1).size(), 1);
		assertEquals(winners.getWinners(2).size(), 1);
		assertEquals(winners.getWinners(3).size(), 1);
		assertEquals(winners.getWinners(0).get(0), t1);
		assertEquals(winners.getWinners(1).get(0), t3);
		assertEquals(winners.getWinners(2).get(0), t2);
	}
	
	@Test
	public void testPointsComp() {
		// going to set this up so team one gets 3-0 and all the other teams get 1-2 in order to test the tiebreakers
		// this one is going to test the points tiebreaker
		List<Match> matches = event.start();
		// set jack to go 3-0
		Match match = matches.get(0);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		match = matches.get(1);
		match.getGames().get(0).setTeam2Score(12);
		match.getGames().get(1).setTeam2Score(12);
		match = matches.get(2);
		match.getGames().get(0).setTeam2Score(19);
		match.getGames().get(1).setTeam2Score(12);
		// set everyone else to 1-2
		match = matches.get(3);
		match.getGames().get(0).setTeam1Score(10);
		match.getGames().get(1).setTeam1Score(10);
		match = matches.get(4);
		match.getGames().get(0).setTeam2Score(10);
		match.getGames().get(1).setTeam2Score(10);
		match = matches.get(5);
		match.getGames().get(0).setTeam1Score(10);
		match.getGames().get(1).setTeam1Score(10);
		for(Match cur : matches) {
			cur.finish();
		}
		assertTrue(event.isComplete("A"));
		EventResult winners = event.getWinners("A");
		assertEquals(winners.getWinners(0).size(), 1);
		assertEquals(winners.getWinners(1).size(), 1);
		assertEquals(winners.getWinners(2).size(), 1);
		assertEquals(winners.getWinners(3).size(), 1);
		assertEquals(winners.getWinners(0).get(0), t1);
		assertEquals(winners.getWinners(1).get(0), t4);
		assertEquals(winners.getWinners(2).get(0), t3);
	}
	
	@Test
	public void testForfeit() {
		List<Match> matches = event.start();
		Match match = matches.get(0);
		match.setTeam2Forfeit(true, false);
		match = matches.get(1);
		match.setTeam2Forfeit(true, false);
		match = matches.get(2);
		match.setTeam1Forfeit(true, false);
		match.setTeam2Forfeit(true, false);
		match = matches.get(3);
		match.setTeam1Forfeit(true, false);
		match = matches.get(4);
		match.setTeam2Forfeit(true, false);
		match = matches.get(5);
		match.setTeam2Forfeit(true, false);
		for(Match cur : matches) {
			cur.finish();
		}
		assertTrue(event.isComplete("A"));
		EventResult winners = event.getWinners("A");
		assertEquals(winners.getWinners(0).size(), 1);
		assertEquals(winners.getWinners(1).size(), 1);
		assertEquals(winners.getWinners(2).size(), 1);
		assertEquals(winners.getWinners(3).size(), 1);
		assertEquals(winners.getWinners(2).get(0), t2);
		assertEquals(winners.getWinners(3).get(0), t4);
	}
	
	@Test
	public void testWithdraw() {
		List<Match> matches = event.start();
		Match match = matches.get(0);
		match.getGames().get(0).setTeam1Score(12);
		match.getGames().get(0).setTeam2Score(19);
		match.setTeam1Forfeit(true, true);
		match.setTeam2Forfeit(true, true);
		match = matches.get(1);
		match.setTeam2Forfeit(true, true);
		match = matches.get(2);
		match.setTeam2Forfeit(true, true);
		for(Match cur : matches) {
			cur.finish();
		}
		assertTrue(event.isComplete("A"));
		EventResult winners = event.getWinners("A");
		assertEquals(winners.getWinners(0).size(), 2);
		assertEquals(winners.getWinners(1).size(), 2);
		assertEquals(winners.getWinners(2).size(), 1);
		assertEquals(winners.getWinners(3).size(), 1);
		assertEquals(winners.getWinners(0).get(0), t4);
		assertEquals(winners.getWinners(0).get(1), t3);
		assertEquals(winners.getWinners(2).get(0), t2);
		match = matches.get(0);
		match.setTeam1Forfeit(true, false);
		for(Match cur : matches) {
			cur.end();
			cur.recalculate();
		}
		assertTrue(event.isComplete("A"));
		winners = event.getWinners("A");
		assertEquals(winners.getWinners(0).size(), 1);
		assertEquals(winners.getWinners(1).size(), 2);
		assertEquals(winners.getWinners(2).size(), 2);
		assertEquals(winners.getWinners(3).size(), 1);
		assertEquals(winners.getWinners(0).get(0), t1);
		assertEquals(winners.getWinners(1), winners.getWinners(2));
		assertEquals(winners.getWinners(3).get(0), t2);
	}
}
