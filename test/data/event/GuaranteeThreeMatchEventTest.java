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

public class GuaranteeThreeMatchEventTest {
	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() {
		new GuaranteeThreeMatchEvent("Event", null, new OnePlayerTeam(), 8, 21, 30, 2, 1);
	}
	
	@Test
	public void testConstructor1() {
		new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C"), new OnePlayerTeam(), 8, 21, 30, 2, 1);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructor2() {
		new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C", "D"), new OnePlayerTeam(), 7, 21, 30, 2, 1);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void testConstructor3() {
		new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C", "D"), new OnePlayerTeam(), 12, 21, 30, 2, 1);
	}
	
	public void testStart() {
		GuaranteeThreeMatchEvent event = new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C", "D"), new OnePlayerTeam(), 8, 21, 30, 2, 1);
		assertEquals(event.start().size(), 0);
	}
	
	@Test
	public void testAToCDrop() {
		List<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < 8; ++i) {
			Team team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));		
			teams.add(team);
		}
		GuaranteeThreeMatchEvent event = new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C", "D"), new OnePlayerTeam(), 8, 21, 30, 2, 1);
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		assertEquals(matches.size(), teams.size() / 2);
		assertEquals(EventUtils.getAllMatches(matches).size(), 7);
		assertNull(event.getMatches("F"));
		assertNull(event.getMatches(null));
		assertNull(event.start());
		matches.get(0).setTeam2Forfeit(true, true);
		assertEquals(matches.get(0).finish().size(), 2);
		matches.get(1).setTeam2Forfeit(true, false);
		assertEquals(matches.get(1).finish().size(), 2);
		matches.get(0).getWinnerMatch().getGames().get(0).setTeam1Score(11);
		assertEquals(matches.get(0).getWinnerMatch().finish().size(), 3);
		matches = event.getMatches("C");
		assertEquals(matches.get(0).getTeam1().getPlayer(0).getName(), "3");
		assertEquals(matches.get(0).getTeam2().getPlayer(0).getName(), "0");
	}
	
	@Test
	public void testAToBDrop() {
		List<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < 8; ++i) {
			Team team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));		
			teams.add(team);
		}
		GuaranteeThreeMatchEvent event = new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C", "D"), new OnePlayerTeam(), 8, 21, 30, 2, 1);
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		matches.get(0).setTeam2Forfeit(true, false);
		assertEquals(matches.get(0).finish().size(), 2);
		matches.get(1).setTeam2Forfeit(true, false);
		assertEquals(matches.get(1).finish().size(), 2);
		matches.get(0).getWinnerMatch().getGames().get(0).setTeam1Score(11);
		assertEquals(matches.get(0).getWinnerMatch().finish().size(), 3);
		assertEquals(event.getMatches("B").get(1).getTeam1().getPlayer(0).getName(), "0");
	}
	
	@Test
	public void testCToDDrop() {
		List<Team> teams = new ArrayList<Team>();
		for(int i = 0; i < 8; ++i) {
			Team team = new OnePlayerTeam();
			team.setPlayer(0, new Player(i + "", true));		
			teams.add(team);
		}
		GuaranteeThreeMatchEvent event = new GuaranteeThreeMatchEvent("Event", Arrays.asList("A", "B", "C", "D"), new OnePlayerTeam(), 8, 21, 30, 2, 1);
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		matches.get(0).setTeam2Forfeit(true, false);
		assertEquals(matches.get(0).finish().size(), 2);
		matches.get(1).setTeam2Forfeit(true, false);
		assertEquals(matches.get(1).finish().size(), 2);
		matches = event.getMatches("C");
		matches.get(0).setTeam1Forfeit(true, false);
		assertEquals(matches.get(0).finish().size(), 2);
		assertEquals(event.getMatches("D").get(0).getTeam1().getPlayer(0).getName(), "1");
	}
}
