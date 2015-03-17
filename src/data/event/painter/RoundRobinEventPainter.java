package data.event.painter;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.HashMap;
import java.util.List;

import data.event.Event;
import data.match.Match;
import data.team.Team;

public class RoundRobinEventPainter extends EventPainter {
	private Dimension dimension;
	private static final String TEAM_MESSAGE = "Bold team's score shown first";
	private static final String WINNER = "Won";
	private static final String LOSER = "Lost";
	
	public RoundRobinEventPainter(Event event, String level) {
		super(event, level);
	}
	
	public Dimension getCanvasSize() {
		return dimension;
	}
	
	public void paint(int matchHeight, int xPadding, int yPadding, float fontSize, int textPadding, Graphics g) {
		Font titleFont = g.getFont().deriveFont(fontSize + 4.0f).deriveFont(Font.BOLD);
		FontMetrics metrics = g.getFontMetrics(titleFont);
		String title = getEvent().getName() + " - " + getLevel();
		int titleHeight = metrics.getHeight();
		int titleWidth = metrics.stringWidth(title);
		g.setFont(g.getFont().deriveFont(fontSize));
		metrics = g.getFontMetrics(g.getFont());
		int teamMessageWidth = metrics.stringWidth(TEAM_MESSAGE);
		int matchWidth = teamMessageWidth;
		// calculating the match width
		metrics = g.getFontMetrics(g.getFont().deriveFont(Font.BOLD));
		HashMap<Team, Match[]> matches = new HashMap<Team, Match[]>();
		List<Team> teams = getEvent().getTeams();
		for(int i = 0; i < teams.size(); ++i) {
			matches.put(teams.get(i), new Match[teams.size()]);
			int width = metrics.stringWidth(getTeamString(teams.get(i)));
			if(width > matchWidth) {
				matchWidth = width;
			}
		}
		metrics = g.getFontMetrics(g.getFont());
		for(Match match : getEvent().getAllMatches()) {
			matches.get(match.getTeam1())[teams.indexOf(match.getTeam2())] = match;
			matches.get(match.getTeam2())[teams.indexOf(match.getTeam1())] = match;
			int width = metrics.stringWidth(getScoreString(match));
			if(width > matchWidth) {
				matchWidth = width;
			}
		}
		matchWidth += 2 * textPadding;
		dimension = new Dimension(Math.max((teams.size() + 1) * matchWidth, titleWidth) + (2 * xPadding), ((teams.size() + 1) * matchHeight) + (3 * yPadding) + titleHeight);
		// draw the title
		Font font = g.getFont();
		g.setFont(titleFont);
		g.drawString(title, (dimension.width / 2) - (titleWidth / 2), yPadding + titleHeight);
		g.setFont(font);
		// draw the grid
		titleHeight *= 1.5;
		for(int i = 0; i <= teams.size(); ++i) {
			int xStart = (i * matchWidth) + xPadding;
			int xEnd = ((i + 1) * matchWidth) + xPadding;
			for(int j = 0; j <= teams.size(); ++j) {
				int yStart = (j * matchHeight) + (2 * yPadding) + titleHeight;
				int yEnd = ((j + 1) * matchHeight) + (2 * yPadding) + titleHeight;
				g.drawLine(xStart, yEnd, xEnd, yEnd);
				g.drawLine(xEnd, yStart, xEnd, yEnd);
			}
		}
		// draw the teams
		int fontHeight = metrics.getHeight();
		int y0 = ((matchHeight + (4 * yPadding) + (2 * titleHeight)) / 2) + (fontHeight / 2) - textPadding;
		g.drawString(TEAM_MESSAGE, ((matchWidth + (2 * xPadding)) / 2) - (teamMessageWidth / 2), y0);
		g.setFont(font.deriveFont(Font.BOLD));
		metrics = g.getFontMetrics(g.getFont());
		int boldFontHeight = metrics.getHeight();
		for(int i = 0; i < teams.size(); ++i) {
			int y1 = ((i + 1) * matchHeight) + (2 * yPadding) + titleHeight;
			int y2 = ((i + 2) * matchHeight) + (2 * yPadding) + titleHeight;
			int half = (y1 + y2) / 2;
			g.drawString(getTeamString(teams.get(i)), xPadding + textPadding, half + (boldFontHeight / 2) - textPadding);
		}
		g.setFont(font);
		for(int i = 0; i < teams.size(); ++i) {
			g.drawString(getTeamString(teams.get(i)), ((i + 1) * matchWidth) + xPadding + (2 * textPadding), y0); 
		}
		// draw the matches
		for(int i = 0; i < teams.size(); ++i) {
			Team team = teams.get(i);
			int y = y0 + ((i + 1) * matchHeight);
			for(int j = 0; j < teams.size(); ++j) {
				if(i == j) {
					continue;
				}
				int x = ((j + 1) * matchWidth) + xPadding + (2 * textPadding);
				g.drawString(getScoreString(matches.get(team)[j], team), x, y);
			}
		}
	}
	
	protected String getScoreString(Match match, Team team) {
		if(match == null) {
			return "";
		}
		String scoreString = super.getScoreString(match, team);
		if(match.getTeam1Forfeit() || match.getTeam2Forfeit()) {
			scoreString = (team.equals(match.getWinner()) ? WINNER : LOSER) + " - " + scoreString;
		}
		return scoreString;
	}
}
