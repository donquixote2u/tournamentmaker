package data.event.painter;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import data.event.RoundRobinEvent;
import data.match.Match;
import data.team.Team;

public class RoundRobinEventPainter extends EventPainter {
	private Dimension dimension;
	private static final String TEAM_MESSAGE = "Bold team's score shown first";
	private static final String WINNER = "Won";
	private static final String LOSER = "Lost";
	private int numberOfDuplicateMatches;
	
	public RoundRobinEventPainter(RoundRobinEvent event, String level) {
		super(event, level);
		numberOfDuplicateMatches = event.getNumberOfDuplicateMatches();
	}
	
	public Dimension getCanvasSize() {
		return dimension;
	}
	
	public void paint(int matchHeight, int xPadding, int yPadding, float fontSize, int textPadding, Graphics g) {
		Font titleFont = g.getFont().deriveFont(fontSize + 4.0f).deriveFont(Font.BOLD);
		FontMetrics metrics = g.getFontMetrics(titleFont);
		String title = getTitle();
		int titleHeight = metrics.getHeight();
		int titleWidth = metrics.stringWidth(title);
		g.setFont(g.getFont().deriveFont(fontSize));
		metrics = g.getFontMetrics(g.getFont());
		int teamMessageWidth = metrics.stringWidth(TEAM_MESSAGE);
		int matchWidth = teamMessageWidth;
		// calculating the match width
		metrics = g.getFontMetrics(g.getFont().deriveFont(Font.BOLD));
		List<Team> teams = getEvent().getTeams();
		Match[][][] matches = new Match[teams.size()][teams.size()][numberOfDuplicateMatches];
		for(int i = 0; i < teams.size(); ++i) {
			int width = metrics.stringWidth(getTeamString(teams.get(i)));
			if(width > matchWidth) {
				matchWidth = width;
			}
		}
		metrics = g.getFontMetrics(g.getFont());
		// take advantage of the fact round robin events always generate their matches in order
		ArrayList<Match> allMatches = new ArrayList<Match>(getEvent().getAllMatches());
		Collections.sort(allMatches, new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				return m1.getIndex() - m2.getIndex();
			}
		});
		Comparator<Match> sortByFinished = new Comparator<Match>() {
			public int compare(Match m1, Match m2) {
				int m1Finished = m1.getWinner() != null ? 1 : 0;
				int m2Finished = m2.getWinner() != null ? 1 : 0;
				return m2Finished - m1Finished;
			}
		};
		int index = 0;
		for(int i = 0; i < teams.size(); ++i) {
			for(int j = i + 1; j < teams.size(); ++j) {
				ArrayList<Match> headToHeadMatches = new ArrayList<Match>();
				for(int count = 0; count < numberOfDuplicateMatches; ++count) {
					Match match = allMatches.get(index++);
					int width = metrics.stringWidth(getScoreString(match));
					if(width > matchWidth) {
						matchWidth = width;
					}
					headToHeadMatches.add(match);
				}
				Collections.sort(headToHeadMatches, sortByFinished);
				for(int count = 0; count < numberOfDuplicateMatches; ++count) {
					matches[i][j][count] = headToHeadMatches.get(count);
				}
			}
		}
		matchWidth += 2 * textPadding;
		dimension = new Dimension(Math.max((teams.size() + 1) * matchWidth, titleWidth) + ((teams.size() + 1) * 2 * xPadding), ((teams.size() + 1) * matchHeight * numberOfDuplicateMatches) + (4 * yPadding) + titleHeight);
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
				int yStart = (j * matchHeight * numberOfDuplicateMatches) + (2 * yPadding) + titleHeight;
				int yEnd = ((j + 1) * matchHeight * numberOfDuplicateMatches) + (2 * yPadding) + titleHeight;
				g.drawLine(xStart, yEnd, xEnd, yEnd);
				g.drawLine(xEnd, yStart, xEnd, yEnd);
			}
		}
		// draw the teams
		int fontHeight = metrics.getHeight();
		int y0 = ((matchHeight * numberOfDuplicateMatches + (4 * yPadding) + (2 * titleHeight)) / 2) + (fontHeight / 2) - textPadding;
		g.drawString(TEAM_MESSAGE, ((matchWidth + (2 * xPadding)) / 2) - (teamMessageWidth / 2), y0);
		g.setFont(font.deriveFont(Font.BOLD));
		metrics = g.getFontMetrics(g.getFont());
		int boldFontHeight = metrics.getHeight();
		for(int i = 0; i < teams.size(); ++i) {
			int y1 = ((i + 1) * matchHeight * numberOfDuplicateMatches) + (2 * yPadding) + titleHeight;
			int y2 = ((i + 2) * matchHeight * numberOfDuplicateMatches) + (2 * yPadding) + titleHeight;
			int half = (y1 + y2) / 2;
			g.drawString(getTeamString(teams.get(i)), xPadding + textPadding, half + (boldFontHeight / 2) - textPadding);
		}
		g.setFont(font);
		for(int i = 0; i < teams.size(); ++i) {
			g.drawString(getTeamString(teams.get(i)), ((i + 1) * matchWidth) + xPadding + (2 * textPadding), y0); 
		}
		// draw the matches
		for(int i = 0; i < teams.size(); ++i) {
			Team team1 = teams.get(i);
			if(team1 == null) {
				continue;
			}
			for(int j = i + 1; j < teams.size(); ++j) {
				Team team2 = teams.get(j);
				if(team2 == null) {
					continue;
				}
				int x1 = ((j + 1) * matchWidth) + xPadding + (2 * textPadding);
				int x2 = ((i + 1) * matchWidth) + xPadding + (2 * textPadding);
				for(int count = 0; count < numberOfDuplicateMatches; ++count) {
					int yStart = ((i + 1) * matchHeight * numberOfDuplicateMatches) + (2 * yPadding) + titleHeight + (matchHeight * count);
					int yEnd = yStart + matchHeight;
					g.drawString(getScoreString(matches[i][j][count], team1), x1, ((yStart + yEnd) / 2) + (fontHeight / 2) - textPadding);
					yStart = ((j + 1) * matchHeight * numberOfDuplicateMatches) + (2 * yPadding) + titleHeight + (matchHeight * count);
					yEnd = yStart + matchHeight;
					g.drawString(getScoreString(matches[i][j][count], team2), x2, ((yStart + yEnd) / 2) + (fontHeight / 2) - textPadding);
				}
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
