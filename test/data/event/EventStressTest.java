package data.event;

import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import org.junit.Before;
import org.junit.Test;

import ui.component.panel.EventBracketCanvas;
import data.match.Match;
import data.player.Player;
import data.team.BaseModifierTeam;
import data.team.Team;
import data.tournament.Tournament;

public class EventStressTest {
	private int numberOfLosses;
	
	@Before
	public void setUpBeforeTest() {
		numberOfLosses = 0;
	}
	
	@Test
	public void GuaranteeThreeMatchEventStressTest() {
		runStressTest(new GuaranteeThreeMatchEvent("Test", new ArrayList<String>(), new BaseModifierTeam(1, "test"), 32, 21, 30, 2, 3), 3);
	}
	
	@Test
	public void GuaranteeTwoMatchEventStressTest() {
		runStressTest(new GuaranteeTwoMatchEvent("Test", new ArrayList<String>(), new BaseModifierTeam(1, "test"), 32, 21, 30, 2, 3), 2);
	}
	
	@Test
	public void SingleEliminationEventStressTest() {
		runStressTest(new SingleEliminationEvent("Test", new ArrayList<String>(), new BaseModifierTeam(1, "test"), 32, 21, 30, 2, 3), 1);
	}
	
	@Test
	public void RoundRobinEventStressTest() {
		runStressTest(new RoundRobinEvent("Test", new ArrayList<String>(), new BaseModifierTeam(1, "test"), 6, 21, 30, 2, 3), 5);
	}
	
	@Test
	public void DoubleRoundRobinEventStressTest() {
		runStressTest(new DoubleRoundRobinEvent("Test", new ArrayList<String>(), new BaseModifierTeam(1, "test"), 6, 21, 30, 2, 3), 10);
	}
	
	@Test
	public void DoubleEliminationEventStressTest() {
		numberOfLosses = 2;
		runStressTest(new DoubleEliminationEvent("Test", new ArrayList<String>(), new BaseModifierTeam(1, "test"), 32, 21, 30, 2, 3), 2);
	}
	
	private void runStressTest(final Event event, int numberOfGames) {
		try {
			stressTestEvent(event, numberOfGames);
		}
		catch(Exception e) {
			for(final String level : event.getDisplayLevels()) {
				JDialog dialog = new JDialog((JFrame) null, "Debug", true);
				dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
				JScrollPane scrollPane = new JScrollPane();
				scrollPane.setPreferredSize(new Dimension(1200, 800));
				final EventBracketCanvas eventCanvas = new EventBracketCanvas(50, 10, 10, 16.0f, 5, scrollPane);
				eventCanvas.setBackground(Color.WHITE);
				scrollPane.setViewportView(eventCanvas);
				scrollPane.setBorder(BorderFactory.createEmptyBorder());
				scrollPane.getVerticalScrollBar().setUnitIncrement(50);
				scrollPane.getHorizontalScrollBar().setUnitIncrement(50);
				dialog.getContentPane().add(scrollPane);
				eventCanvas.setEventPainter(event.getEventPainter(level));
				dialog.pack();
				dialog.setVisible(true);
			}
			throw new RuntimeException(e);
		}
	}
	
	private void stressTestEvent(Event event, int numberOfGames) {
		// set up the tournament
		Tournament tournament = new Tournament("Test", new ArrayList<String>(), 1, 0);
		assertTrue(tournament.addEvent(event));
		for(int i = 0; i < 1000; ++i) {
			// set up the event
			assertFalse(event.isComplete());
			ArrayList<Team> teams = new ArrayList<Team>();
			for(int j = 0; j < event.getNumberOfTeams(); ++j) {
				if(((int) (Math.random() * 100)) < 5) {
					teams.add(null);
					continue;
				}
				Team team = event.getTeamFilter().newInstance();
				for(int k = 0; k < team.getNumberOfPlayers(); ++k) {
					Player player = new Player(String.valueOf(j), true);
					player.setCheckedIn(true);
					team.setPlayer(k, player);
				}
				teams.add(team);
			}
			event.setTeams(teams);
			event.setFilterTeamByLevel(false);
			assertTrue(event.canStart());
			tournament.startEvent(event);
			int notNullTeamCount = 0;
			for(Team team : event.getTeams()) {
				if(team != null) {
					++notNullTeamCount;
				}
			}
			if(notNullTeamCount > 1) {
				assertFalse(event.isComplete());
				// run through all the matches
				List<Match> matches;
				while(!(matches = tournament.getMatches()).isEmpty()) {
					Match match;
					int index = ((int) (Math.random() * 100)) % matches.size();
					while(!(match = matches.get(index % matches.size())).canStartMatch()) {
						if(++index > 2 * matches.size()) {
							throw new RuntimeException("Unable to complete event.");
						}
					}
					assertTrue(match.canStartMatch());
					tournament.getCourts().get(0).setMatch(match);
					tournament.removeMatch(match);
					if(((int) (Math.random() * 100)) < 8) {
						boolean withdrawTeam = ((int) (Math.random() * 100)) < 30;
						if(((int) (Math.random() * 100)) < 50) {
							match.setTeam1Forfeit(true, withdrawTeam);
						}
						else {
							match.setTeam2Forfeit(true, withdrawTeam);
						}
					}
					else {
						for(int j = 0; j < match.getGames().size(); ++j) {
							if(j % 2 == 0) {
								match.getGames().get(j).setTeam1Score(((int) (Math.random() * 100)) % event.getMaxScore());
							}
							else {
								match.getGames().get(j).setTeam2Score(((int) (Math.random() * 100)) % event.getMaxScore());
							}
						}
					}
					tournament.getCourts().get(0).setMatch(null);
					tournament.getCourts().get(0).getPreviousMatches().remove(match);
					Set<Match> newMatches;
					try {
						newMatches = match.finish();
					}
					catch(Exception e) {
						System.out.println(match + ", id: " + match.getIndex() + " on cycle #" + i);
						throw new RuntimeException(e);
					}
					assertNotNull(newMatches);
					tournament.addCompletedMatch(match);
					tournament.addMatches(newMatches);
				}
			}
			// make sure every team played the required number of games
			boolean hasFewer = false;
			for(Team team : event.getTeams()) {
				if(team == null || team.isWithdrawn()) {
					continue;
				}
				if(team.getMatchesPlayed() >= numberOfGames) {
					continue;
				}
				if(team.getMatchesWon() + team.getMatchesLost() >= numberOfGames) {
					continue;
				}
				// look through all the matches to account for byes and withdrawals
				int count = 0;
				for(Match match : event.getAllMatches()) {
					if(team.equals(match.getTeam1()) || team.equals(match.getTeam2())) {
						++count;
					}
				}
				assertTrue(count >= numberOfGames);
				if(numberOfLosses > 0) {
					if(!hasFewer && team.getMatchesLost() < numberOfLosses) {
						hasFewer = true;
						continue;
					}
					assertEquals(team.getMatchesLost(), numberOfLosses);
				}
			}
			assertTrue(event.isComplete());
			// reset everything
			tournament.undoEvent(event);
		}
	}
}
