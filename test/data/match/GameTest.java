package data.match;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import data.player.Player;
import data.team.Team;
import data.team.doubles.TwoPlayerTeam;
import data.team.singles.OnePlayerTeam;

public class GameTest {
	private Game game;
	
	@Before
	public void setUpBeforeTest() {
		Team t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		Team t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bob", true));
		t2.setPlayer(1, new Player("Jane", false));
		game = new Game(21, 30, 2);
		game.setTeam1(t1);
		game.setTeam2(t2);
	}
	
	@Test
	public void testSetup() {
		assertEquals(game.getWinner(), null);
		assertEquals(game.getWinnerScore(), 0);
		assertEquals(game.getLoserScore(), 0);
	}
	
	@Test
	public void testMinScore() {
		game.setTeam1Score(11);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Bob");
		assertEquals(game.getWinnerScore(), 21);
		assertEquals(game.getLoserScore(), 11);
	}
	
	@Test
	public void testMidScore() {
		game.setTeam2Score(21);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Jack");
		assertEquals(game.getWinnerScore(), 23);
		assertEquals(game.getLoserScore(), 21);
	}
	
	@Test
	public void testSetMaxScore1() {
		game.setTeam1Score(28);
		assertEquals(game.getWinnerScore(), 30);
		assertEquals(game.getLoserScore(), 28);
	}
	
	@Test
	public void testSetMaxScore2() {
		game.setTeam1Score(29);
		assertEquals(game.getWinnerScore(), 30);
		assertEquals(game.getLoserScore(), 29);
	}
	
	@Test
	public void testSetScore1() {
		game.setTeam1Score(11);
		assertEquals(game.getWinnerScore(), 21);
		assertEquals(game.getLoserScore(), 11);
		game.setTeam2Score(13);
		assertEquals(game.getWinner(), null);
		game.setTeam1Score(21);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Jack");
		assertEquals(game.getWinnerScore(), 21);
		assertEquals(game.getLoserScore(), 13);
	}
	
	@Test
	public void testSetScore2() {
		game.setTeam1Score(30);
		assertEquals(game.getWinnerScore(), 30);
		assertEquals(game.getLoserScore(), 28);
		game.setTeam2Score(13);
		assertEquals(game.getWinner(), null);
		game.setTeam2Score(21);
		assertEquals(game.getWinner(), null);
		game.setTeam1Score(20);
		assertEquals(game.getWinner(), null);
		game.setTeam1Score(19);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Bob");
	}
	
	@Test
	public void testSetScore3() {
		game.setTeam1Score(30);
		game.setTeam2Score(29);
		assertEquals(game.getWinnerScore(), 30);
		assertEquals(game.getLoserScore(), 29);
		game.setTeam1Score(13);
		assertEquals(game.getWinner(), null);
		game.setTeam2Score(15);
		assertEquals(game.getWinner(), null);
		game.setTeam1Score(21);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Jack");
		game.setTeam2Score(21);
		assertEquals(game.getWinner(), null);
	}
	
	@Test
	public void testSetScore4() {
		game.setTeam1Score(0);
		assertEquals(game.getWinnerScore(), 21);
		assertEquals(game.getLoserScore(), 0);
		game.setTeam2Score(30);
		assertEquals(game.getWinner(), null);
		game.setTeam1Score(30);
		assertEquals(game.getWinner(), null);
		game.setTeam1Score(28);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Bob");
	}
	
	@Test
	public void testSetScore5() {
		Team t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		Team t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bob", true));
		t2.setPlayer(1, new Player("Jane", false));
		game = new Game(0, 0, 1);
		game.setTeam1(t1);
		game.setTeam2(t2);
		assertNull(game.getWinner());
		game.setTeam1Score(5);
		assertEquals(game.getTeam2Score(), 0);
		assertEquals(game.getWinner(), t1);
		game.setTeam2Score(6);
		assertEquals(game.getTeam1Score(), 5);
		assertEquals(game.getWinner(), t2);
		game.setTeam1Score(6);
		assertNull(game.getWinner());
	}
	
	@Test
	public void testSetTeams1() {
		game.setTeam2Score(24);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Jack");
		Team team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Bill", true));
		game.setTeam1(team);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Bill");
	}
	
	@Test
	public void testSetTeams2() {
		Team team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Sandy", false));
		game.setTeam2(team);
		game.setTeam1Score(11);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Sandy");
		team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Bill", true));
		game.setTeam1(team);
		assertEquals(game.getWinner().getPlayers().get(0).getName(), "Sandy");
	}
}
