package data.event;

import java.util.List;

import data.team.Team;

@CreatableEvent(displayName = "Double Round Robin")
public class DoubleRoundRobinEvent extends RoundRobinEvent {
	private static final long serialVersionUID = 2605778103136462583L;

	public DoubleRoundRobinEvent(String name, List<String> levels, Team teamFilter, int numberOfTeams, int minScore, int maxScore, int winBy, int bestOf) {
		super(name, levels, teamFilter, numberOfTeams, minScore, maxScore, winBy, bestOf);
	}
	
	public int getNumberOfDuplicateMatches() {
		return 2;
	}
}
