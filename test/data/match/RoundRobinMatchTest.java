package data.match;

import static org.junit.Assert.*;

import org.junit.Test;

import data.player.Player;
import data.team.Team;
import data.team.singles.OnePlayerTeam;

public class RoundRobinMatchTest {
	@Test
	public void testSetForfeit() {
		Team t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Bob", true));
		Team t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		Match match = new RoundRobinMatch(t1, t2, 21, 30, 2, 1);
		match.setTeam1Forfeit(true, true);
		assertTrue(t1.isWithdrawn());
		assertFalse(t2.isWithdrawn());
		match.setTeam1Forfeit(false, true);
		assertFalse(t1.isWithdrawn());
		match.setTeam2Forfeit(true, false);
		assertFalse(t2.isWithdrawn());
	}
	
	@Test
	public void testFinish() {
		Team t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Bob", true));
		Team t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		Team t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		Team t4 = new OnePlayerTeam();
		t4.setPlayer(0, new Player("Jim", true));
		Match m1 = new RoundRobinMatch(t1, t2, 21, 30, 2, 1);
		Match m2 = new RoundRobinMatch(t1, t3, 21, 30, 2, 1);
		Match m3 = new RoundRobinMatch(t1, t4, 21, 30, 2, 1);
		Match m4 = new RoundRobinMatch(t2, t3, 21, 30, 2, 1);
		Match m5 = new RoundRobinMatch(t2, t4, 21, 30, 2, 1);
		Match m6 = new RoundRobinMatch(t3, t4, 21, 30, 2, 1);
		m1.setTeam1Forfeit(true, true);
		assertFalse(m1.isComplete());
		assertEquals(m1.finish().size(), 0);
		assertEquals(m2.finish().size(), 0);
		assertEquals(m3.finish().size(), 0);
		m4.setTeam2Forfeit(true, true);
		assertEquals(m4.finish().size(), 0);
		assertTrue(m4.isComplete());
		assertFalse(m6.isComplete());
		assertEquals(m6.finish().size(), 0);
		assertEquals(m6.getWinner(), t4);
		m4.setTeam2Forfeit(true, false);
		assertNull(m6.recalculate());
		m5.setTeam1Forfeit(true, false);
		m6.setTeam2Forfeit(true, true);
		assertEquals(m5.finish().size(), 0);
		assertTrue(m5.isComplete());
		assertNull(m5.getWinner());
		assertNull(m5.getLoser());
	}
}
