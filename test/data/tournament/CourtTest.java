package data.tournament;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import data.match.Match;
import data.player.Player;
import data.team.Team;
import data.team.singles.MenSinglesTeam;
import data.team.singles.WomenSinglesTeam;

public class CourtTest {
	private Court court;
	
	@Before
	public void setUpBeforeTest() {
		court = new Court("test");
	}
	
	@Test
	public void testId() {
		assertEquals(court.getId(), "test");
	}
	
	@Test
	public void testIsAvailable() {
		assertTrue(court.isAvailable());
		assertEquals(court.getCurrentMatch(), null);
		assertTrue(court.getPreviousMatches().isEmpty());
	}
	
	@Test
	public void testSetMatch() {
		Team t1 = new MenSinglesTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.getPlayers().get(0).setCheckedIn(true);
		Team t2 = new MenSinglesTeam();
		t2.setPlayer(0, new Player("Bob", true));
		t2.getPlayers().get(0).setCheckedIn(true);
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		court.setMatch(m1);
		assertFalse(court.isAvailable());
		assertEquals(m1.getCourt(), court);
		assertNotNull(m1.getStart());
		assertEquals(court.getCurrentMatch(), m1);
		Team t3 = new WomenSinglesTeam();
		t3.setPlayer(0, new Player("Jill", false));
		t3.getPlayers().get(0).setCheckedIn(true);
		Team t4 = new WomenSinglesTeam();
		t4.setPlayer(0, new Player("Jane", false));
		t4.getPlayers().get(0).setCheckedIn(true);
		Match m2 = new Match(t3, t4, 21, 30, 2, 3);
		court.setMatch(m2);
		assertEquals(m1.getCourt(), court);
		assertEquals(m2.getCourt(), court);
		assertEquals(court.getCurrentMatch(), m2);
		assertEquals(court.getPreviousMatches().size(), 1);
		assertNotNull(m1.getEnd());
		assertFalse(t1.getPlayers().get(0).isInGame());
		assertTrue(t3.getPlayers().get(0).isInGame());
		assertEquals(court.getPreviousMatches().get(0), m1);
		court.setMatch(null);
		assertTrue(court.isAvailable());
		assertEquals(court.getPreviousMatches().size(), 2);
		assertEquals(court.getPreviousMatches().get(0), m2);
		assertNotNull(m2.getEnd());
		assertFalse(t3.getPlayers().get(0).isInGame());
	}
	
	@Test
	public void testUndoPreviousMatch() {
		Team t1 = new MenSinglesTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.getPlayers().get(0).setCheckedIn(true);
		Team t2 = new MenSinglesTeam();
		t2.setPlayer(0, new Player("Bob", true));
		t2.getPlayers().get(0).setCheckedIn(true);
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		court.setMatch(m1);
		assertNotNull(m1.getCourt());
		Match m2 = new Match(null, null, 21, 30, 2, 3);
		court.setMatch(m2);
		assertEquals(court.getCurrentMatch(), m2);
		assertTrue(court.undoMatch(m1));
		assertEquals(court.getCurrentMatch(), m2);
		assertNull(m1.getCourt());
		assertNull(m1.getEnd());
		assertFalse(t1.getPlayers().get(0).isInGame());
		assertNotNull(t2.getPlayers().get(0).getLastMatchTime());
	}
	
	@Test
	public void testUndoMatch() {
		Team t1 = new MenSinglesTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.getPlayers().get(0).setCheckedIn(true);
		Team t2 = new MenSinglesTeam();
		t2.setPlayer(0, new Player("Bob", true));
		t2.getPlayers().get(0).setCheckedIn(true);
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		court.setMatch(m1);
		assertNotNull(m1.getCourt());
		court.undoMatch(m1);
		assertNull(m1.getCourt());
		assertNull(court.getCurrentMatch());
		court.setMatch(m1);
		assertNull(m1.getEnd());
		court.setMatch(null);
		assertNotNull(m1.getEnd());
		assertEquals(m1.getCourt(), court);
		assertEquals(court.getPreviousMatches().size(), 1);
		court.undoMatch(m1);
		assertTrue(court.getPreviousMatches().isEmpty());
		court.setMatch(m1);
		assertEquals(court.getCurrentMatch(), m1);
		Date end = m1.getEnd();
		court.setMatch(null);
		assertNotSame(m1.getEnd(), end);
	}
}
