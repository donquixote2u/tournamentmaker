package data.event.painter;

import java.awt.Dimension;
import java.awt.Graphics;
import java.util.List;

import ui.main.TournamentViewManager;
import data.event.Event;
import data.match.Game;
import data.match.Match;
import data.team.Team;

public abstract class EventPainter {
	private Event event;
	private String value;
	private List<Match> matches;
	
	public EventPainter(Event event, String level) {
		if(event == null || !event.getDisplayLevels().contains(level)) {
			throw new RuntimeException("illegal parameters");
		}
		this.event = event;
		value = level;
	}
	
	public EventPainter(Event event, String description, List<Match> matches) {
		if(event == null || matches == null) {
			throw new RuntimeException("illegal parameters");
		}
		this.event = event;
		this.matches = matches;
		value = description;
	}
	
	public boolean equals(Object other) {
		if(other == null || !(other instanceof EventPainter)) {
			return false;
		}
		return toString().equals(other.toString());
	}
	
	public int hashCode() {
		return toString().hashCode();
	}
	
	public String toString() {
		if(matches == null) {
			return event.getName() + (event.showDisplayLevel() ? " - " + value : "");
		}
		return event.getName() + value;
	}
	
	protected final Event getEvent() {
		return event;
	}
	
	protected final String getTitle() {
		return event.getName() + (matches == null ? " - " : "") + value;
	}
	
	protected final List<Match> getMatches() {
		if(matches == null) {
			return event.getMatches(value);
		}
		return matches;
	}
	
	protected String getTeamString(Team team) {
		if(team == null) {
			return "";
		}
		String name = team.getName();
		if(name == null) {
			name = " ";
		}
		else {
			name += " ";
		}
		if(team.getSeed() != null) {
			name += "[" + team.getSeed() + "]";
		}
		return name.trim();
	}
	
	protected String getScoreString(Match match) {
		if(match == null || match.getWinner() == null) {
			return "";
		}
		return getScoreString(match, match.getWinner());
	}
	
	protected String getScoreString(Match match, Team team) {
		String scores = "";
		for(Game game : match.getGames()) {
			if(game.getTeam1Score() >= 0 && game.getTeam2Score() >= 0) {
				if(team.equals(game.getTeam1())) {
					scores += game.getTeam1Score() + "-" + game.getTeam2Score() + ", ";
				}
				else {
					scores += game.getTeam2Score() + "-" + game.getTeam1Score() + ", ";
				}
			}
		}
		if(scores.length() > 0) {
			scores = scores.substring(0, scores.length() - 2);
		}
		if(match.getTeam1Forfeit() || match.getTeam2Forfeit()) {
			scores += " " + TournamentViewManager.WALKOVER;
		}
		return scores.trim();
	}
	
	public abstract Dimension getCanvasSize();
	public abstract void paint(int matchHeight, int xPadding, int yPadding, float fontSize, int textPadding, Graphics g);
}
