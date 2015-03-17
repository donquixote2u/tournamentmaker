package data.team;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import data.player.Player;
import data.tournament.TournamentUtils;

public abstract class Team implements Serializable, Comparable<Team> {
	private static final long serialVersionUID = -6032723529164295799L;
	private transient Date estimatedDate;
	private transient boolean resetMatch;
	private String seed;
	private boolean isWithdrawn, inMatch, inEvent;
	private int matchesPlayed, matchesWon, matchesLost;
	private String name;
	
	public boolean getInEvent() {
		return inEvent;
	}
	
	public void setInEvent(boolean inEvent) {
		this.inEvent = inEvent;
	}
	
	public String getSeed() {
		if(seed == null || seed.trim().isEmpty()) {
			return null;
		}
		return seed.trim();
	}
	
	public void setSeed(String seed) {
		this.seed = seed;
	}
	
	public int getMatchesPlayed() {
		return matchesPlayed;
	}
	
	public void setMatchesPlayed(int matchesPlayed) {
		this.matchesPlayed = matchesPlayed;
	}
	
	public int getMatchesWon() {
		return matchesWon;
	}
	
	public void setMatchesWon(int matchesWon) {
		this.matchesWon = matchesWon;
	}
	
	public int getMatchesLost() {
		return matchesLost;
	}
	
	public void setMatchesLost(int matchesLost) {
		this.matchesLost = matchesLost;
	}
	
	public boolean isCheckedIn() {
		for(Player player : getPlayers()) {
			if(player == null || !player.isCheckedIn()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean canStartMatch() {
		if(inMatch) {
			return false;
		}
		for(Player player : getPlayers()) {
			if(player == null || player.isInGame() || !player.isCheckedIn()) {
				return false;
			}
		}
		return true;
	}
	
	public boolean isInMatch() {
		return inMatch;
	}
	
	public boolean startMatch() {
		if(!canStartMatch() || !isValid()) {
			return false;
		}
		inMatch = true;
		for(Player player : getPlayers()) {
			player.setInGame(true);
			player.setLastMatchTime(null);
		}
		return true;
	}
	
	public void endMatch(Date date) {
		inMatch = false;
		for(Player player : getPlayers()) {
			if(date != null) {
				player.setLastMatchTime(date);
			}
			else {
				player.undoSetLastMatchTime();
			}
			player.setInGame(false);
		}
	}
	
	public Set<String> getLevels() {
		Set<String> levels = new HashSet<String>();
		for(Player player : getPlayers()) {
			if(player != null) {
				String level = player.getLevel();
				if(level != null && !level.trim().isEmpty()) {
					levels.add(level.trim());
				}
			}
		}
		return levels;
	}
	
	public boolean isWithdrawn() {
		return isWithdrawn;
	}
	
	public void setIsWithdrawn(boolean isWithdrawn) {
		this.isWithdrawn = isWithdrawn;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getClub() {
		HashSet<String> set = new HashSet<String>();
		for(Player player : getPlayers()) {
			if(player != null) {
				set.add(player.getClub());
			}
		}
		ArrayList<String> list = new ArrayList<String>();
		for(String club : set) {
			if(club != null && !club.trim().isEmpty()) {
				list.add(club.trim());
			}
		}
		Collections.sort(list);
		String clubName = "";
		for(String club : list) {
			clubName += club + " / ";
		}
		if(!clubName.isEmpty()) {
			clubName = clubName.substring(0, clubName.length() - 3);
		}
		return clubName;
	}
	
	public void setEstimatedDate(Date estimatedDate) {
		if(this.estimatedDate == null || TournamentUtils.compareDates(this.estimatedDate, estimatedDate) > 0) {
			this.estimatedDate = estimatedDate;
		}
	}
	
	public Date getEstimatedDate() {
		return estimatedDate;
	}
	
	public boolean canResetMatch() {
		return !resetMatch;
	}
	
	public void setResetMatch(boolean resetMatch) {
		this.resetMatch = resetMatch;
	}
	
	public String getTeamType() {
		return getClass().getSimpleName();
	}
	
	public String getTeamTypeDescription() {
		return getTeamType();
	}
	
	public boolean isValidTeam(Team team) {
		if(team == null) {
			return false;
		}
		return getClass().equals(team.getClass());
	}
	
	public int compareTo(Team team) {
		if(team == null) {
			return 1;
		}
		return getTeamType().compareTo(team.getTeamType());
	}
	
	public String toString() {
		String seed = getSeed();
		if(seed == null) {
			seed = "";
		}
		else {
			seed = "[" + seed + "] ";
		}
		ArrayList<String> levels = new ArrayList<String>(getLevels());
		Collections.sort(levels);
		String club = getClub();
		if(!club.isEmpty()) {
			club = " - " + club;
		}
		return seed + getName() + (levels.isEmpty() ? "" : " " + levels) + club;
	}
	
	public abstract List<Player> getPlayers();
	public abstract Player getPlayer(int index);
	public abstract boolean setPlayer(int index, Player player);
	public abstract boolean isValid();
	public abstract int getNumberOfPlayers();
	public abstract boolean isValidPlayer(Player player);
	public abstract Team newInstance();
}