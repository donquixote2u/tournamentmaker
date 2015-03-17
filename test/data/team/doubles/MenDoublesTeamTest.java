package data.team.doubles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import data.player.Player;

public class MenDoublesTeamTest {
	private Player p1, p2;
	private TwoPlayerTeam team;
	
	@Before
	public void setUpBeforeTest() {
		p1 = new Player("Bob", true);
		p2 = new Player("Bill", true);
		p1.setIsMale(true);
		p2.setIsMale(true);
		team = new MenDoublesTeam();
		team.setPlayer(0, p1);
		team.setPlayer(1, p2);
	}
	
	@Test
	public void test() {
		assertTrue(team.isValid());
	}
	
	@Test
	public void test1() {
		p1.setIsMale(false);
		assertFalse(team.isValid());
	}
	
	@Test
	public void test2() {
		p2.setIsMale(false);
		assertFalse(team.isValid());
	}
	
	@Test
	public void test3() {
		team.setPlayer(1, null);
		assertTrue(team.isValid());
	}
	
	@Test
	public void test4() {
		team.setPlayer(0, null);
		assertTrue(team.isValid());
	}
	
	@Test
	public void test5() {
		team.setPlayer(1, p1);
		assertFalse(team.isValid());
	}
}
