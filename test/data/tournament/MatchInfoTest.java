package data.tournament;

import static org.junit.Assert.*;

import org.junit.Test;

public class MatchInfoTest {
	@Test
	public void getTimeTest() {
		MatchInfo matchInfo = new MatchInfo();
		assertEquals(matchInfo.getTime(), 0);
		matchInfo.addTime(500);
		assertEquals(matchInfo.getTime(), 500);
		matchInfo.addTime(1000);
		assertEquals(matchInfo.getTime(), 750);
		matchInfo.addTime(500);
		assertEquals(matchInfo.getTime(), 667);
		matchInfo.addTime(141);
		assertEquals(matchInfo.getTime(), 535);
	}
	
	@Test
	public void averageTest() {
		MatchInfo m1 = new MatchInfo();
		assertEquals(m1.average(null), 0);
		MatchInfo m2 = new MatchInfo();
		assertEquals(m1.average(m2), 0);
		m1.addTime(500);
		assertEquals(m1.average(null), 500);
		assertEquals(m1.average(m2), 500);
		m1.addTime(1000);
		m1.addTime(500);
		assertEquals(m1.average(m2), 667);
		assertEquals(m1.average(m1), 667);
		m2.addTime(500);
		assertEquals(m1.average(m2), 584);
		assertEquals(m2.average(m1), 584);
	}
}
