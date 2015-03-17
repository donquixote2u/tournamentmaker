package data.event;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import data.match.Match;
import data.player.Player;
import data.team.Team;
import data.team.singles.OnePlayerTeam;

public class SingleEliminationEventTest {
	@Test
	public void testStart() {
		SingleEliminationEvent event = new SingleEliminationEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 1);
		List<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < 4; ++i) {
			OnePlayerTeam team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));
			teams.add(team);
		}
		event.setTeams(teams);
		assertFalse(event.canStart());
		event.setFilterTeamByLevel(false);
		assertTrue(event.canStart());
		List<Match> matches = event.start();
		assertFalse(event.canStart());
		assertEquals(matches.size(), 2);
		matches.get(0).setTeam1Forfeit(true, false);
		matches.get(1).setTeam2Forfeit(true, false);
		assertEquals(matches.get(0).finish().size(), 1);
		assertEquals(matches.get(1).finish().iterator().next(), matches.get(0).getWinnerMatch());
		assertNull(matches.get(0).getWinnerMatch().getWinnerMatch());
		assertEquals(EventUtils.getAllMatches(matches).size(), 3);
		assertEquals(matches.get(0).getMatchDescription(), EventUtils.SEMI_FINALS);
		assertEquals(matches.get(1).getMatchDescription(), EventUtils.SEMI_FINALS);
		assertEquals(matches.get(0).getWinnerMatch().getMatchDescription(), EventUtils.FINALS);
	}
}
