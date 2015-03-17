package data.team.singles;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import data.player.Player;

public class OnePlayerTeamTest {
	private OnePlayerTeam team;
	private Player player;
	
	@Before
	public void setupBeforeTest() {
		team = new OnePlayerTeam();
		player = new Player("Bob", true);
		team.setPlayer(0, player);
	}
	
	@Test
	public void test() {
		assertFalse(team.setPlayer(-1, null));
		assertFalse(team.setPlayer(1, null));
		assertNull(team.getPlayer(1));
		assertEquals(team.getPlayer(0), player);
		assertTrue(team.isValid());
		assertTrue(team.setPlayer(0, new Player("Bill", true)));
		assertFalse(team.setPlayer(0, null));
		assertTrue(team.isValid());
	}
}
