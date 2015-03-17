package data.team.singles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import data.player.Player;

public class WomenSinglesTeamTest {
	Player player;
	OnePlayerTeam team;
	
	@Before
	public void setUpBeforeTest() {
		player = new Player("Jill", false);
		player.setIsMale(false);
		team = new WomenSinglesTeam();
		team.setPlayer(0, player);
	}
	
	@Test
	public void test() {
		assertTrue(team.isValid());
	}
	
	@Test
	public void test1() {
		player.setIsMale(true);
		assertFalse(team.isValid());
	}
}
