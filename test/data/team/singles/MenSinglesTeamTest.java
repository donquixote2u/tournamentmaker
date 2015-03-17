package data.team.singles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import data.player.Player;

public class MenSinglesTeamTest {
	Player player;
	OnePlayerTeam team;
	
	@Before
	public void setUpBeforeTest() {
		player = new Player("Bob", true);
		player.setIsMale(true);
		team = new MenSinglesTeam();
		team.setPlayer(0, player);
	}
	
	@Test
	public void test() {
		assertTrue(team.isValid());
	}
	
	@Test
	public void test1() {
		player.setIsMale(false);
		assertFalse(team.isValid());
	}
}
