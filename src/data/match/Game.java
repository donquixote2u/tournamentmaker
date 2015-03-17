package data.match;

import java.io.Serializable;

import data.team.Team;

public class Game implements Serializable {
	private static final long serialVersionUID = 3013453702512389260L;
	private Team t1, t2;
	private int minScore, maxScore, winBy;
	private int t1Score, t2Score;
	private Team winner;
	
	public Game(int minScore, int maxScore, int winBy) {
		this.minScore = minScore;
		this.maxScore = maxScore;
		this.winBy = winBy;
		t1Score = -1;
		t2Score = -1;
	}

	public Team getTeam1() {
		return t1;
	}

	public void setTeam1(Team t1) {
		if(winner != null && winner == this.t1) {
			winner = t1;
		}
		this.t1 = t1;
	}

	public Team getTeam2() {
		return t2;
	}

	public void setTeam2(Team t2) {
		if(winner != null && winner == this.t2) {
			winner = t2;
		}
		this.t2 = t2;
	}

	public Team getWinner() {
		return winner;
	}
	
	public int getTeam1Score() {
		return t1Score;
	}
	
	public void setTeam1Score(int score) {
		t1Score = score;
		if(score >= 0) {
			validateScore();
		}
		else {
			winner = null;
		}
	}
	
	public int getTeam2Score() {
		return t2Score;
	}
	
	public void setTeam2Score(int score) {
		t2Score = score;
		if(score >= 0) {
			validateScore();
		}
		else {
			winner = null;
		}
	}
	
	public int getWinnerScore() {
		Team winner = getWinner();
		if(winner == null) {
			return 0;
		}
		return winner == t1 ? getTeam1Score() : getTeam2Score();
	}
	
	public int getLoserScore() {
		Team winner = getWinner();
		if(winner == null) {
			return 0;
		}
		return winner == t1 ? getTeam2Score() : getTeam1Score();
	}
	
	private void validateScore() {
		if(maxScore > 0) {
			if(t1Score > maxScore) {
				t1Score = maxScore;
			}
			if(t2Score > maxScore) {
				t2Score = maxScore;
			}
		}
		if(minScore == 0 && maxScore == 0) {
			if(t1Score < 0) {
				t1Score = 0;
			}
			if(t2Score < 0) {
				t2Score = 0;
			}
			if(t1Score - winBy >= t2Score) {
				winner = t1;
			}
			else if(t2Score - winBy >= t1Score) {
				winner = t2;
			}
			else {
				winner = null;
			}
		}
		else if((t1Score >= 0 && t2Score < 0) || (t2Score >= 0 && t1Score < 0)) {
			if(t1Score >= 0) {
				if(t1Score == maxScore) {
					winner = t1;
					// defaulting the other team's score to a valid score
					t2Score = maxScore - winBy; 
				}
				else {
					winner = t2;
					t2Score = Math.max(Math.min(maxScore, t1Score + winBy), minScore);
				}
			}
			else {
				if(t2Score == maxScore) {
					winner = t2;
					// defaulting the other team's score to a valid score
					t1Score = maxScore - winBy; 
				}
				else {
					winner = t1;
					t1Score = Math.max(Math.min(maxScore, t2Score + winBy), minScore);
				}
			}
		}
		else {
			if(t1Score == maxScore && t2Score == maxScore) {
				winner = null;
			}
			else if(t1Score == maxScore && maxScore != minScore) {
				if(t1Score - t2Score <= winBy) {
					winner = t1;
				}
				else {
					winner = null;
				}
			}
			else if(t2Score == maxScore && maxScore != minScore) {
				if(t2Score - t1Score <= winBy) {
					winner = t2;
				}
				else {
					winner = null;
				}
			}
			else if((t1Score == minScore && t1Score - t2Score >= winBy) || (t1Score > minScore && t1Score - t2Score == winBy)) {
				winner = t1;
			}
			else if((t2Score == minScore && t2Score - t1Score >= winBy) || (t2Score > minScore && t2Score - t1Score == winBy)) {
				winner = t2;
			}
			else {
				winner = null;
			}
		}
	}
}
