package data.event.result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import data.team.Team;

public abstract class EventResult {
	private String description;
	private List<List<Team>> winners;
	
	public EventResult(String description) {
		this.description = description;
		winners = new ArrayList<List<Team>>();
	}
	
	public List<Team> getWinners(int index) {
		if(index < 0 || index >= winners.size()) {
			return null;
		}
		return Collections.unmodifiableList(winners.get(index));
	}
	
	public int getNumberOfWinners() {
		return winners.size();
	}
	
	public void clearWinners() {
		winners.clear();
	}
	
	public String getDescription() {
		return description;
	}
	
	protected void addWinners(List<Team> teams) {
		// checking for nulls and duplicates
		HashSet<Team> set = new HashSet<Team>(teams);
		set.remove(null);
		for(List<Team> list : winners) {
			for(Team team : list) {
				set.remove(team);
			}
		}
		ArrayList<Team> newTeams = new ArrayList<Team>(set);
		Collections.sort(newTeams, new Comparator<Team>() {
			public int compare(Team t1, Team t2) {
				if(t1 == null && t2 == null) {
					return 0;
				}
				if(t1 == null) {
					return -1;
				}
				if(t2 == null) {
					return 1;
				}
				String t1Name = t1.getName();
				String t2Name = t2.getName();
				if(t1Name == null && t2Name == null) {
					return 0;
				}
				if(t1Name == null) {
					return -1;
				}
				if(t2Name == null) {
					return 1;
				}
				return t1Name.compareTo(t2Name);
			}
		});
		for(int i = 0; i < newTeams.size(); ++i) {
			winners.add(newTeams);
		}
	}
}
