package data.match;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

import data.player.Player;
import data.team.doubles.TwoPlayerTeam;
import data.team.singles.OnePlayerTeam;

public class MatchTest {
	@Test(expected = RuntimeException.class)
	public void testConstructor1() {
		new Match(null, null, 1, 2, 1, 0);
	}
	
	@Test(expected = RuntimeException.class)
	public void testConstructor2() {
		new Match(null, null, 2, 1, 1, 1);
	}
	
	@Test(expected = RuntimeException.class)
	public void testConstructor3() {
		new Match(null, null, 1, 2, 0, 1);
	}
	
	@Test(expected = RuntimeException.class)
	public void testConstructor4() {
		new Match(null, null, -1, 2, 1, 1);
	}
	
	@Test(expected = RuntimeException.class)
	public void testConstructor5() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		new Match(t1, t1, 21, 30, 2, 3);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSetTeam1() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		(new Match(t1, t2, 21, 30, 2, 3)).setTeam1(t2);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSetTeam2() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		(new Match(t1, t2, 21, 30, 2, 3)).setTeam2(t1);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSetTeam3() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		Match match = new Match(null, null, 21, 30, 2, 3);
		match.setTeam1(t1);
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		match.setTeam2(t2);
		match.setTeam1(null);
		match.setTeam2(null);
		match.setTeam1(t1);
		match.setTeam2(t1);
	}
	
	@Test
	public void testStartAndStop() {
		Match match = new Match(null, null, 21, 30, 2, 3);
		assertFalse(match.canStartMatch());
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		match = new Match(t1, null, 21, 30, 2, 3);
		assertFalse(match.canStartMatch());
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		match = new Match(null, t2, 21, 30, 2, 3);
		assertFalse(match.canStartMatch());
		match = new Match(t1, t2, 21, 30, 2, 3);
		assertFalse(match.canStartMatch());
		t1.getPlayer(0).setCheckedIn(true);
		assertFalse(match.canStartMatch());
		t1.getPlayer(1).setCheckedIn(true);
		assertFalse(match.canStartMatch());
		t2.getPlayer(0).setCheckedIn(true);
		assertFalse(match.canStartMatch());
		assertFalse(match.start());
		t2.getPlayer(1).setCheckedIn(true);
		assertTrue(match.canStartMatch());
		assertEquals(match.getGames().size(), 3);
		assertTrue(match.start());
		assertTrue(t1.getPlayer(0).isInGame());
		assertTrue(t1.getPlayer(1).isInGame());
		assertTrue(t2.getPlayer(0).isInGame());
		assertTrue(t2.getPlayer(1).isInGame());
		match.end();
		assertEquals(match.getEnd(), t1.getPlayer(0).getLastMatchTime());
	}
	
	@Test
	public void testSetMatch() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		Match m2 = new Match(t1, t2, 21, 30, 2, 3);
		m1.addLoserMatch("0", m2);
		assertEquals(m2.getToTeam1Match(false), m1);
		m1.setWinnerMatch(m2, false);
		assertEquals(m2.getToTeam2Match(false), m1);
		m2.setMirrorMatch(m1);
		assertEquals(m1.getMirrorMatch(), m2);
	}
	
	@Test
	public void testSetMatch1() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		OnePlayerTeam t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		Match m2 = new Match(t3, null, 21, 30, 2, 3);
		Match m3 = new Match(null, null, 21, 30, 2, 3);
		Match m4 = new Match(null, null, 21, 30, 2, 3);
		m1.setWinnerMatch(m3, true);
		m2.setWinnerMatch(m3, false);
		m1.addLoserMatch("0", m4);
		m3.addLoserMatch("0", m4);
		m2.addLoserMatch("0", m4);
		assertFalse(m1.isComplete());
		assertNull(m4.getToTeam1Match(true));
		assertEquals(m4.getToTeam1Match(false), m1);
		assertEquals(m4.getToTeam2Match(false), m3);
		m1.setTeam2Forfeit(true, true);
		assertFalse(m1.isComplete());
		assertEquals(m1.finish().size(), 2);
		assertTrue(m1.isComplete());
		assertEquals(m2.finish().size(), 2);
		assertEquals(m4.getToTeam1Match(false), m3);
	}
	
	@Test
	public void testSetMatch2() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		OnePlayerTeam t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		Match m2 = new Match(t3, null, 21, 30, 2, 3);
		Match m3 = new Match(null, null, 21, 30, 2, 3);
		Match m4 = new Match(null, null, 21, 30, 2, 3);
		m1.setWinnerMatch(m3, true);
		m2.setWinnerMatch(m3, false);
		m1.addLoserMatch("0", m4);
		m3.addLoserMatch("0", m4);
		assertNull(m4.getToTeam1Match(true));
		assertEquals(m4.getToTeam1Match(false), m1);
		m1.setTeam1Forfeit(true, false);
		assertFalse(m1.isComplete());
		assertEquals(m1.finish().size(), 2);
		assertTrue(m1.isComplete());
		assertEquals(m2.finish().size(), 1);
		assertEquals(m4.getToTeam1Match(false), m1);
		assertEquals(m4.getTeam1(), t1);
	}
	
	@Test
	public void testSetMatch3() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		OnePlayerTeam t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		OnePlayerTeam t4 = new OnePlayerTeam();
		t4.setPlayer(0, new Player("Bob", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		Match m2 = new Match(null, t3, 21, 30, 2, 3);
		Match m3 = new Match(t4, null, 21, 30, 2, 3);
		Match m4 = new Match(null, null, 21, 30, 2, 3);
		Match m5 = new Match(null, null, 21, 30, 2, 3);
		m1.setWinnerMatch(m2, true);
		m2.setWinnerMatch(m3, false);
		m3.setWinnerMatch(m5, false);
		m1.addLoserMatch("0", m4);
		m2.addLoserMatch("0", m4);
		m3.addLoserMatch("0", m4);
		assertEquals(m4.getToTeam1Match(false), m1);
		assertFalse(m2.isComplete());
		m1.setTeam2Forfeit(true, true);
		assertEquals(m1.finish().size(), 2);
		assertTrue(m1.isComplete());
		assertEquals(m2.getTeam1(), t1);
		assertEquals(m4.getToTeam1Match(false), m2);
		m2.setTeam2Forfeit(true, true);
		assertEquals(m2.finish().size(), 2);
		assertTrue(m2.isComplete());
		assertEquals(m3.getTeam2(), t1);
		assertEquals(m4.getToTeam1Match(false), m3);
		m3.setTeam1Forfeit(true, false);
		assertEquals(m3.finish().size(), 2);
		assertTrue(m3.isComplete());
		assertEquals(m4.getToTeam1Match(false), m3);
		assertEquals(m4.getTeam1(), t4);
		assertFalse(m5.isComplete());
		assertEquals(m5.finish().size(), 0);
	}
	
	@Test
	public void testSetMatch4() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		OnePlayerTeam t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		OnePlayerTeam t4 = new OnePlayerTeam();
		t4.setPlayer(0, new Player("Bob", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		Match m2 = new Match(null, t3, 21, 30, 2, 3);
		Match m3 = new Match(t4, null, 21, 30, 2, 3);
		Match m4 = new Match(null, null, 21, 30, 2, 3);
		Match m5 = new Match(null, null, 21, 30, 2, 3);
		m1.setWinnerMatch(m2, true);
		m2.setWinnerMatch(m3, false);
		m3.setWinnerMatch(m5, false);
		m1.addLoserMatch("0", m4);
		m2.addLoserMatch("0", m4);
		m3.addLoserMatch("0", m4);
		assertEquals(m4.getToTeam1Match(false), m1);
		m1.setTeam2Forfeit(true, true);
		assertEquals(m1.finish().size(), 2);
		assertEquals(m2.getTeam1(), t1);
		assertEquals(m4.getToTeam1Match(false), m2);
		m2.setTeam2Forfeit(true, false);
		assertEquals(m2.finish().size(), 2);
		assertEquals(m3.getTeam2(), t1);
		assertEquals(m4.getToTeam1Match(false), m2);
		assertNull(m4.finish());
		m3.setTeam2Forfeit(true, false);
		assertEquals(m3.finish().size(), 2);
		assertEquals(m4.finish().size(), 0);
		assertTrue(m4.isComplete());
		m2.setTeam2Forfeit(true, true);
		m2.end();
		assertEquals(m2.recalculate().size(), 1);
		assertNull(m4.getToTeam1Match(false));
	}
	
	@Test
	public void testRecalculate() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		OnePlayerTeam t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		OnePlayerTeam t4 = new OnePlayerTeam();
		t4.setPlayer(0, new Player("Bob", true));
		OnePlayerTeam t5 = new OnePlayerTeam();
		t5.setPlayer(0, new Player("Sean", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 1);
		Match m2 = new Match(t3, null, 21, 30, 2, 1);
		Match m3 = new Match(null, t4, 21, 30, 2, 1);
		Match m4 = new Match(null, t5, 21, 30, 2, 1);
		Match m5 = new Match(null, t5, 21, 30, 2, 1);
		Match m6 = new Match(null, t5, 21, 30, 2, 1);
		m1.setWinnerMatch(m2, false);
		m1.addLoserMatch("0", m3);
		m2.addLoserMatch("0", m5);
		m2.addLoserMatch("1", m4);
		m2.setWinnerMatch(m6, true);
		m1.setTeam1Forfeit(true, false);
		assertEquals(m1.finish().size(), 2);
		m1.end();
		assertEquals(m1.recalculate().size(), 0);
		m2.setTeam1Forfeit(true, false);
		m2.finish();
		m3.setTeam1Forfeit(true, false);
		m3.finish();
		m1.setTeam1Forfeit(false, false);
		m1.setTeam2Forfeit(true, false);
		assertEquals(m1.recalculate().size(), 2);
		m4.setTeam2Forfeit(true, false);
		m4.finish();
		m5.setTeam2Forfeit(true, false);
		m5.finish();
		m6.setTeam2Forfeit(true, false);
		m6.finish();
		m2.setTeam1Forfeit(false, false);
		m2.setTeam2Forfeit(true, false);
		m2.end();
		assertEquals(m2.recalculate().size(), 3);
		assertEquals(m4.getTeam1(), t1);
		assertEquals(m5.getTeam1(), null);
	}
	
	@Test
	public void testRecalculateFinish() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		OnePlayerTeam t3 = new OnePlayerTeam();
		t3.setPlayer(0, new Player("Jon", true));
		OnePlayerTeam t4 = new OnePlayerTeam();
		t4.setPlayer(0, new Player("Bob", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		Match m2 = new Match(null, t3, 21, 30, 2, 3);
		Match m3 = new Match(t4, null, 21, 30, 2, 3);
		Match m4 = new Match(null, null, 21, 30, 2, 3);
		Match m5 = new Match(null, null, 21, 30, 2, 3);
		m1.setWinnerMatch(m2, true);
		m2.setWinnerMatch(m3, false);
		m3.setWinnerMatch(m5, false);
		m1.addLoserMatch("0", m4);
		m2.addLoserMatch("0", m4);
		m3.addLoserMatch("0", m4);
		assertNull(m1.recalculate());
		assertEquals(m4.getToTeam1Match(false), m1);
		m1.setTeam2Forfeit(true, true);
		assertEquals(m1.finish().size(), 2);
		assertEquals(m2.getTeam1(), t1);
		assertEquals(m4.getToTeam1Match(false), m2);
		m2.setTeam2Forfeit(true, true);
		assertEquals(m2.finish().size(), 2);
		assertEquals(m3.getTeam2(), t1);
		assertEquals(m4.getToTeam1Match(false), m3);
		m3.setTeam2Forfeit(true, false);
		assertEquals(m3.finish().size(), 2);
		assertEquals(m4.finish().size(), 0);
		assertEquals(m5.finish().size(), 0);
		m1.setTeam2Forfeit(true, false);
		m1.end();
		assertEquals(m1.recalculate().size(), 1);
		assertTrue(m2.isComplete());
		assertTrue(m3.isComplete());
		assertTrue(m4.isComplete());
		assertTrue(m5.isComplete());
		assertEquals(m4.getToTeam1Match(false), m1);
		assertEquals(m4.finish().size(), 0);
		assertTrue(m4.isComplete());
		assertEquals(m2.finish().size(), 0);
		m3.setTeam2Forfeit(true, true);
		assertEquals(m3.finish().size(), 0);
		assertEquals(m5.finish().size(), 0);
		assertEquals(m4.getToTeam1Match(false), m1);
	}
	
	@Test
	public void testFinish() {
		OnePlayerTeam t1 = new OnePlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		OnePlayerTeam t2 = new OnePlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		Match m1 = new Match(t1, t2, 21, 30, 2, 3);
		m1.getGames().get(0).setTeam1Score(12);
		m1.getGames().get(1).setTeam1Score(15);
		m1.getGames().get(1).setTeam2Score(11);
		assertNull(m1.finish());
		m1.getGames().get(1).setTeam2Score(21);
		m1.getGames().get(2).setTeam2Score(15);
		assertNull(m1.finish());
	}
	
	@Test
	public void testCalculateResult1() {
		Match match = new Match(null, null, 21, 30, 2, 3);
		assertEquals(match.finish().size(), 0);
		assertTrue(match.isComplete());
		assertEquals(match.getWinner(), null);
		assertEquals(match.getLoser(), null);
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		match = new Match(t1, t2, 21, 30, 2, 3);
		match.setTeam1Forfeit(true, true);
		match.setTeam2Forfeit(true, true);
		assertEquals(match.finish().size(), 0);
		assertTrue(match.isComplete());
		assertEquals(match.getWinner(), null);
		assertEquals(match.getLoser(), null);
		match = new Match(t1, t2, 21, 30, 2, 3);
		match.setTeam1Forfeit(true, true);
		match.setTeam2Forfeit(false, false);
		assertEquals(match.finish().size(), 0);
		assertTrue(match.isComplete());
		assertEquals(match.getWinner(), t2);
		assertEquals(match.getLoser(), t1);
		match = new Match(t1, t2, 21, 30, 2, 3);
		match.setTeam1Forfeit(false, false);
		match.setTeam2Forfeit(true, true);
		assertEquals(match.finish().size(), 0);
		assertTrue(match.isComplete());
		assertEquals(match.getWinner(), t1);
		assertEquals(match.getLoser(), t2);
	}
	
	@Test
	public void testCalculateResult2() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		Match winner = new Match(null, null, 21, 30, 2, 3);
		Match match = new Match(t1, t2, 21, 30, 2, 3);
		match.setWinnerMatch(winner, true);
		assertFalse(winner.isComplete());
		assertNull(match.finish());
		match.setTeam1Forfeit(true, true);
		assertNull(winner.finish());
		assertEquals(match.finish().size(), 1);
		assertEquals(winner.finish().size(), 0);
		assertTrue(winner.isComplete());
		match.end();
		Set<Match> matches = match.recalculate();
		assertEquals(matches.size(), 0);
		assertEquals(winner.getTeam1(), t2);
		assertTrue(winner.isComplete());
		assertEquals(winner.finish().size(), 0);
		match.setTeam1Forfeit(false, false);
		match.setTeam2Forfeit(true, true);
		match.recalculate();
		assertEquals(winner.getTeam1(), t1);
		Match loser = new Match(null, null, 21 ,30, 2, 3);
		match.setTeam2Forfeit(true, false);
		match.addLoserMatch("0", loser);
		assertEquals(match.recalculate().size(), 1);
		loser.finish();
		assertTrue(loser.isComplete());
		match.setTeam2Forfeit(true, true);
		assertEquals(match.recalculate().size(), 1);
		assertEquals(loser.getTeam2(), null);
	}
	
	@Test
	public void testCalculateResult3() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		Match match = new Match(t1, t2, 21, 30, 2, 3);
		Match loser = new Match(null, null, 21, 30, 2, 3);
		Match winner = new Match(null, null, 21, 30, 2, 3);
		match.addLoserMatch("0", loser);
		match.setWinnerMatch(winner, true);
		match.getGames().get(0).setTeam1Score(11);
		match.getGames().get(1).setTeam2Score(19);
		match.getGames().get(2).setTeam1Score(13);
		assertEquals(match.finish().size(), 2);
		assertTrue(match.isComplete());
		assertEquals(match.finish().size(), 0);
		assertEquals(t1.getMatchesPlayed(), 1);
		assertEquals(t1.getMatchesLost(), 1);
		assertEquals(t2.getMatchesPlayed(), 1);
		assertEquals(t2.getMatchesLost(), 0);
		assertEquals(winner.getTeam1(), t2);
		winner.finish();
		assertTrue(winner.isComplete());
		assertEquals(loser.getTeam1(), t1);
		assertEquals(loser.finish().size(), 0);
		assertTrue(loser.isComplete());
		assertEquals(loser.getWinner(), t1);
		match.setTeam2Forfeit(true, false);
		match.end();
		match.recalculate();
		assertEquals(loser.getTeam1(), t2);
		assertEquals(t1.getMatchesPlayed(), 1);
		assertEquals(t1.getMatchesLost(), 0);
		assertEquals(t2.getMatchesPlayed(), 1);
		assertEquals(t2.getMatchesLost(), 1);
	}
	
	@Test
	public void testCalculateResult4() {
		TwoPlayerTeam t1 = new TwoPlayerTeam();
		t1.setPlayer(0, new Player("Jack", true));
		t1.setPlayer(1, new Player("Jill", false));
		TwoPlayerTeam t2 = new TwoPlayerTeam();
		t2.setPlayer(0, new Player("Bill", true));
		t2.setPlayer(1, new Player("Jane", false));
		Match match = new Match(t1, t2, 21, 30, 2, 3);
		Match loser1 = new Match(null, null, 21, 30, 2, 3);
		Match loser2 = new Match(null, null, 21, 30, 2, 3);
		match.addLoserMatch("0", loser1);
		match.addLoserMatch("1", loser2);
		t1.setMatchesPlayed(1);
		match.getGames().get(0).setTeam1Score(11);
		match.getGames().get(1).setTeam2Score(19);
		match.getGames().get(2).setTeam1Score(13);
		assertEquals(match.finish().size(), 2);
		assertEquals(loser2.getTeam1(), t1);
		assertEquals(t1.getMatchesPlayed(), 2);
		assertEquals(t1.getMatchesLost(), 1);
		assertEquals(t2.getMatchesPlayed(), 1);
		assertEquals(t2.getMatchesLost(), 0);
		match.getGames().get(2).setTeam1Score(21);
		match.getGames().get(2).setTeam2Score(13);
		match.end();
		match.recalculate();
		assertNull(loser2.getTeam1());
		assertEquals(loser1.getTeam1(), t2);
	}
}
