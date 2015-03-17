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

public class RoundRobinEventTest {
	@Test(expected = IllegalArgumentException.class)
	public void testConstructor() {
		new RoundRobinEvent("Event", null, new OnePlayerTeam(), 4, 21, 30, 2, 3);
	}
	
	@Test
	public void testConstructor1() {
		new RoundRobinEvent("Event", new ArrayList<String>(), new OnePlayerTeam(), 4, 21, 30, 2, 3);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSetTeams() {
		RoundRobinEvent event = new RoundRobinEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 3);
		event.setTeams(null);
	}
	
	@Test(expected = RuntimeException.class)
	public void testSetTeams2() {
		RoundRobinEvent event = new RoundRobinEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 3);
		event.setTeams(new ArrayList<Team>());
	}
	
	@Test
	public void testStart() {
		List<Team> teams = new ArrayList<Team>();
		Team team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Jack", true));		
		teams.add(team);
		teams.add(null);
		team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Bob", true));
		teams.add(team);
		team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Bill", true));
		teams.add(team);
		RoundRobinEvent event = new RoundRobinEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 3);
		event.setTeams(Arrays.asList(team, null, team, team));
		assertFalse(event.canStart());
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		assertEquals(matches.size(), 6);
		assertNull(event.start());
	}
	
	public void testStart2() {
		RoundRobinEvent event = new RoundRobinEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 3);
		assertEquals(event.start().size(), 0);
	}
	
	@Test
	public void testStart3() {
		List<Team> teams = new ArrayList<Team>();
		Team team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Jack", true));		
		teams.add(team);
		team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Rob", true));
		teams.add(team);
		team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Bob", true));
		teams.add(team);
		team = new OnePlayerTeam();
		team.setPlayer(0, new Player("Bill", true));
		teams.add(team);
		RoundRobinEvent event = new RoundRobinEvent("Event", Arrays.asList("A"), new OnePlayerTeam(), 4, 21, 30, 2, 3);
		event.setTeams(teams);
		event.setFilterTeamByLevel(false);
		List<Match> matches = event.start();
		assertEquals(matches.size(), 6);
	}
}
