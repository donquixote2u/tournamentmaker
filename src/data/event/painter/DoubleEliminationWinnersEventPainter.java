package data.event.painter;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import ui.main.TournamentViewManager;
import data.event.Event;
import data.match.Match;
import data.team.Team;

public class DoubleEliminationWinnersEventPainter extends EventPainter {
	private Dimension dimension;
	
	public DoubleEliminationWinnersEventPainter(Event event, String level) {
		super(event, level);
	}
	
	public DoubleEliminationWinnersEventPainter(Event event, String description, List<Match> matches) {
		super(event, description, matches);
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
		int fontHeight = metrics.getHeight();
		// getting the matches
		int levels = 0;
		int matchWidth = 0;
		List<Match> matches = getMatches();
		ArrayList<Match> currentMatches = new ArrayList<Match>();
		ArrayList<Match> nextMatches = new ArrayList<Match>(matches);
		while(!nextMatches.isEmpty()) {
			++levels;
			currentMatches = nextMatches;
			nextMatches = new ArrayList<Match>();
			for(Match match : currentMatches) {
				// update the matchWidth so it is large enough to fit the longest score
				int width = metrics.stringWidth(getScoreString(match));
				if(width > matchWidth) {
					matchWidth = width;
				}
				Match winnerMatch = match.getWinnerMatch();
				if(winnerMatch != null && !nextMatches.contains(winnerMatch)) {
					nextMatches.add(winnerMatch);
				}
			}
		}
		// update the matchWidth so it is large enough to fit the largest name
		for(Team team : getEvent().getTeams()) {
			String name = team == null ? TournamentViewManager.BYE : getTeamString(team);
			int width = metrics.stringWidth(name);
			if(width > matchWidth) {
				matchWidth = width;
			}
		}
		matchWidth += (2 * xPadding) + textPadding;
		// calculate the finals for the winner's bracket
		Match winnerFinal = currentMatches.get(0).getDefaultToTeam1Match();
		currentMatches.clear();
		currentMatches.add(winnerFinal.getDefaultToTeam1Match());
		boolean drawBackupFinal = winnerFinal.isComplete() && winnerFinal.getWinnerMatch().getTeam1() != null && winnerFinal.getWinnerMatch().getTeam2() != null;
		if(!drawBackupFinal) {
			--levels;
		}
		dimension = new Dimension(Math.max((levels + 1) * matchWidth, titleWidth) + (2 * xPadding), (matches.size() * 4 * matchHeight) + (2 * yPadding) + titleHeight);
		levels -= drawBackupFinal ? 2 : 1;
		int finalLevel = levels + 1;
		// draw the title
		Font font = g.getFont();
		g.setFont(titleFont);
		g.drawString(title, (dimension.width / 2) - (titleWidth / 2), yPadding + titleHeight);
		g.setFont(font);
		// going through all the matches in reverse order and drawing them
		int index = 1;
		int size = (matches.size() * 2 * matchHeight) / (currentMatches.size() * 2);
		while(!currentMatches.isEmpty()) {
			int x0 = ((levels - 1) * matchWidth) + xPadding, x1 = (levels * matchWidth) + xPadding, x2 = ((levels + 1) * matchWidth) + xPadding;
			int y1 = (index * size) + yPadding + titleHeight, y2 = ((index + 2) * size) + yPadding + titleHeight;
			int half = (y1 + y2) / 2;
			Match match = currentMatches.remove(0);
			boolean isInitial = matches.contains(match);
			// draw the winner of the match and the score if we forwarded this match
			Match winnerMatch = match.getWinnerMatch();
			if(winnerMatch == null || match.equals(winnerMatch.getToTeam1Match(false)) || match.equals(winnerMatch.getToTeam2Match(false))) {
				if(match.isComplete() && match.getWinner() == null) {
					g.drawString(TournamentViewManager.BYE, x1 + textPadding, half - textPadding);
				}
				else {
					g.drawString(getTeamString(match.getWinner()), x1 + textPadding, half - textPadding);
				}
			}
			g.drawLine(x1, half, x2, half);
			g.drawString(getScoreString(match), x1 + textPadding, half + fontHeight);
			// draw the line connecting the previous teams
			g.drawLine(x1, y1, x1, y2);
			// if the match is not complete, draw the match index
			if(!match.isComplete()) {
				String description = String.valueOf(match.getIndex());
				int width = metrics.stringWidth(description);
				g.drawString(description, x1 - textPadding - width, half + (fontHeight / 3));
			}
			// check if we are at the end so we draw the teams or if there are previous matches so we add them and let them draw themselves
			Match pre = match.getDefaultToTeam1Match();
			if(pre != null && !isInitial && match.getLevel().equals(pre.getLevel())) {
				nextMatches.add(pre);
			}
			if(match.getTeam1() != null) {
				g.drawString(getTeamString(match.getTeam1()), x0 + textPadding, y1 - textPadding);
				g.drawLine(x0, y1, x1, y1);
			}
			else if(pre == null || isInitial || !pre.equals(match.getToTeam1Match(false))) {
				String description = match.getToTeam1MatchDescription();
				if(description == null) {
					description = TournamentViewManager.BYE;
				}
				g.drawString(description, x0 + textPadding, y1 - textPadding);
				g.drawLine(x0, y1, x1, y1);
			}
			pre = match.getDefaultToTeam2Match();
			if(pre != null && !isInitial && match.getLevel().equals(pre.getLevel())) {
				nextMatches.add(pre);
			}
			if(match.getTeam2() != null) {
				g.drawString(getTeamString(match.getTeam2()), x0 + textPadding, y2 - textPadding);
				g.drawLine(x0, y2, x1, y2);
			}
			else if(pre == null || isInitial || !pre.equals(match.getToTeam2Match(false))) {
				String description = match.getToTeam2MatchDescription();
				if(description == null) {
					description = TournamentViewManager.BYE;
				}
				g.drawString(description, x0 + textPadding, y2 - textPadding);
				g.drawLine(x0, y2, x1, y2);
			}
			index += 4;
			// moving back to the previous set of matches
			if(currentMatches.isEmpty() && !nextMatches.isEmpty()) {
				index = 1;
				--levels;
				currentMatches = nextMatches;
				nextMatches = new ArrayList<Match>();
				size = (matches.size() * 2 * matchHeight) / (currentMatches.size() * 2);
			}
		}
		// draw the final matches
		size = matches.size() * matchHeight;
		int x0 = ((finalLevel - 1) * matchWidth) + xPadding, x1 = (finalLevel * matchWidth) + xPadding, x2 = ((finalLevel + 1) * matchWidth) + xPadding;
		int y1 = (size + (3 * size) + (2 * yPadding) + (2 * titleHeight)) / 2, y2 = (7 * (size / 2)) + yPadding + titleHeight;
		if(y2 - y1 < 2 * matchHeight) {
			y2 = y1 + (2 * matchHeight);
		}
		int half = (y1 + y2) / 2;
		g.drawLine(x0, y2, x1, y2);
		g.drawLine(x1, y1, x1, y2);
		g.drawLine(x1, half, x2, half);
		if(!winnerFinal.getDefaultToTeam2Match().isComplete()) {
			g.drawString(winnerFinal.getToTeam2MatchDescription(), x0 + textPadding, y2 - textPadding);
		}
		else {
			if(winnerFinal.getTeam2() == null) {
				g.drawString(TournamentViewManager.BYE, x0 + textPadding, y2 - textPadding);
			}
			else {
				g.drawString(getTeamString(winnerFinal.getTeam2()), x0 + textPadding, y2 - textPadding);
				g.drawString(getScoreString(winnerFinal.getDefaultToTeam2Match()), x0 + textPadding, y2 + fontHeight);
			}
		}
		if(winnerFinal.isComplete()) {
			if(winnerFinal.getWinner() == null) {
				g.drawString(TournamentViewManager.BYE, x1 + textPadding, half - textPadding);
			}
			else {
				g.drawString(getTeamString(winnerFinal.getWinner()), x1 + textPadding, half - textPadding);
				g.drawString(getScoreString(winnerFinal), x1 + textPadding, half + fontHeight);
			}
		}
		else {
			String description = String.valueOf(winnerFinal.getIndex());
			int width = metrics.stringWidth(description);
			g.drawString(description, x1 - textPadding - width, half + (fontHeight / 3));
		}
		// check to see if we need to draw the backup finals
		if(drawBackupFinal) {
			winnerFinal = winnerFinal.getWinnerMatch();
			x0 = x1;
			x1 = x2;
			x2 = ((finalLevel + 2) * matchWidth) + xPadding;
			y1 = half;
			y2 = (15 * (size / 4)) + yPadding + titleHeight;
			if(y2 - y1 < 2 * matchHeight) {
				y2 = y1 + (2 * matchHeight);
			}
			half = (y1 + y2) / 2;
			g.drawLine(x0, y2, x1, y2);
			g.drawLine(x1, y1, x1, y2);
			g.drawLine(x1, half, x2, half);
			g.drawString(getTeamString(winnerFinal.getTeam2()), x0 + textPadding, y2 - textPadding);
			if(winnerFinal.isComplete()) {
				if(winnerFinal.getWinner() == null) {
					g.drawString(TournamentViewManager.BYE, x1 + textPadding, half - textPadding);
				}
				else {
					g.drawString(getTeamString(winnerFinal.getWinner()), x1 + textPadding, half - textPadding);
					g.drawString(getScoreString(winnerFinal), x1 + textPadding, half + fontHeight);
				}
			}
			else {
				String description = String.valueOf(winnerFinal.getIndex());
				int width = metrics.stringWidth(description);
				g.drawString(description, x1 - textPadding - width, half + (fontHeight / 3));
			}
		}
	}
}
