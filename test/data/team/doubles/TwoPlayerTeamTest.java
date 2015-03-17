package data.team.doubles;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import data.player.Player;

public class TwoPlayerTeamTest {
	private TwoPlayerTeam t1, t2;
	private Player p1, p2, p3, p4;
	
	@Before
	public void setupBeforeTest() {
		p1 = new Player("Bob", true);
		p2 = new Player("Bill", true);
		t1 = new TwoPlayerTeam();
		t1.setPlayer(0, p1);
		t1.setPlayer(1, p2);
		p3 = new Player("Jill", false);
		p4 = new Player("Jane", false);
		t2 = new TwoPlayerTeam();
		t2.setPlayer(0, p3);
		t2.setPlayer(1, p4);
	}

	@Test
	public void testGetPlayerOne() {
		assertEquals(t1.getPlayer(0), p1);
	}

	@Test
	public void testSetPlayerOne() {
		assertFalse(t1.setPlayer(-1, null));
		assertTrue(t1.setPlayer(0, p2));
		assertEquals(t1.getPlayer(0), t1.getPlayer(1));
	}

	@Test
	public void testGetPlayerTwo() {
		assertEquals(t2.getPlayer(1), p4);
	}

	@Test
	public void testSetPlayerTwo() {
		assertFalse(t2.setPlayer(3, null));
		assertTrue(t2.setPlayer(1, p3));
		assertEquals(t2.getPlayer(0), t2.getPlayer(0));
	}

	@Test
	public void testCanStartMatch() {
		assertFalse(t1.canStartMatch());
		p1.setCheckedIn(true);
		assertFalse(t1.canStartMatch());
		p2.setCheckedIn(true);
		assertTrue(t1.canStartMatch());
		p1.setInGame(true);
		assertFalse(t1.canStartMatch());
	}

	@Test
	public void testStartMatch() {
		p1.setCheckedIn(true);
		p2.setCheckedIn(true);
		p3.setCheckedIn(true);
		p4.setCheckedIn(true);
		assertFalse(p1.isInGame());
		assertFalse(p2.isInGame());
		assertTrue(t1.startMatch());
		assertTrue(p1.isInGame());
		assertTrue(p2.isInGame());
		t2.setPlayer(0, p1);
		assertFalse(t2.startMatch());
	}

	@Test
	public void testEndMatch() {
		p1.setCheckedIn(true);
		p2.setCheckedIn(true);
		assertTrue(t1.startMatch());
		assertTrue(p1.isInGame());
		assertTrue(p2.isInGame());
		t1.endMatch(null);
		assertFalse(p1.isInGame());
		assertFalse(p2.isInGame());
	}

	@Test
	public void testEndMatchDate() {
		p1.setCheckedIn(true);
		p2.setCheckedIn(true);
		assertTrue(t1.startMatch());
		Date date = new Date();
		t1.endMatch(date);
		assertEquals(p1.getLastMatchTime(), date);
		assertEquals(p2.getLastMatchTime(), date);
	}
}
